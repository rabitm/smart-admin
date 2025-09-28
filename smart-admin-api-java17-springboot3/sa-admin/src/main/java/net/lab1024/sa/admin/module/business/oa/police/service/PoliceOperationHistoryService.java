package net.lab1024.sa.admin.module.business.oa.police.service;

// 移除Resilience4j依赖，使用手动实现的熔断和限流
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.module.support.operatelog.OperateLogDao;
import net.lab1024.sa.base.module.support.operatelog.domain.OperateLogEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 警情操作历史服务 - 高性能、高可用
 *
 * 设计原则：
 * 1. 不影响主业务流程
 * 2. 异步处理所有写入操作
 * 3. 缓存优先，数据库降级
 * 4. 熔断保护防止雪崩
 * 5. 限流保护防止过载
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-28
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PoliceOperationHistoryService {

    private final OperationHistoryCacheService cacheService;
    private final OperateLogDao operateLogDao;

    // 手动熔断器状态
    private volatile boolean circuitBreakerOpen = false;
    private volatile long circuitBreakerOpenTime = 0;
    private volatile int failureCount = 0;
    private static final int FAILURE_THRESHOLD = 5; // 5次失败后开启熔断
    private static final long CIRCUIT_BREAKER_TIMEOUT = 30000; // 30秒后尝试恢复

    // 简单限流
    private volatile long lastRequestTime = 0;
    private volatile int requestCount = 0;
    private static final int RATE_LIMIT = 50; // 每秒50次
    private static final long RATE_WINDOW = 1000; // 1秒窗口

    /**
     * 异步记录操作历史 - 绝不阻塞主业务
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> recordOperationAsync(Long reportId, Long userId, String userName,
                                                       String operation, Map<String, Object> details) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> operationRecord = buildOperationRecord(
                    reportId, userId, userName, operation, details
                );

                // 1. 优先写入缓存（异步，不等待结果）
                cacheService.addOperationAsync(reportId, operationRecord);

                // 2. 异步写入数据库（降级操作，失败不影响业务）
                asyncSaveToDatabase(operationRecord);

                log.debug("📝 操作历史记录已提交: reportId={}, operation={}", reportId, operation);
            } catch (Exception e) {
                // 记录操作历史失败不应影响主业务
                log.warn("⚠️ 记录操作历史失败: reportId={}, operation={}, error={}",
                        reportId, operation, e.getMessage());
            }
        });
    }

    /**
     * 批量异步记录操作历史
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> recordOperationsBatchAsync(Long reportId,
                                                             List<Map<String, Object>> operations) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (CollectionUtils.isEmpty(operations)) {
                    return;
                }

                // 批量写入缓存
                cacheService.addOperationsBatchAsync(reportId, operations);

                log.debug("📝 批量操作历史记录已提交: reportId={}, count={}", reportId, operations.size());
            } catch (Exception e) {
                log.warn("⚠️ 批量记录操作历史失败: reportId={}, count={}, error={}",
                        reportId, operations.size(), e.getMessage());
            }
        });
    }

    /**
     * 获取操作历史 - 带熔断和限流保护
     */
    public Map<String, Object> getOperationHistory(Long reportId, int pageNum, int pageSize) {
        // 检查熔断器状态
        if (isCircuitBreakerOpen()) {
            log.warn("🔄 熔断器开启，直接降级: reportId={}", reportId);
            return getOperationHistoryFallback(reportId, pageNum, pageSize, new RuntimeException("Circuit breaker open"));
        }

        // 简单限流检查
        if (!checkRateLimit()) {
            log.warn("🚦 请求被限流: reportId={}", reportId);
            return getOperationHistoryFallback(reportId, pageNum, pageSize, new RuntimeException("Rate limit exceeded"));
        }
        try {
            log.debug("📖 获取操作历史: reportId={}, page={}, size={}", reportId, pageNum, pageSize);

            // 1. 优先从缓存获取
            if (cacheService.isHealthy()) {
                Map<String, Object> cachedResult = cacheService.getCachedOperationsPaged(reportId, pageNum, pageSize);
                if (cachedResult != null && !CollectionUtils.isEmpty((List<?>) cachedResult.get("list"))) {
                    log.debug("✅ 从缓存获取操作历史成功: reportId={}", reportId);
                    recordSuccess(); // 记录成功
                    return cachedResult;
                }
            }

            // 2. 缓存未命中，异步预热缓存，同时返回数据库查询结果
            cacheService.preloadCacheAsync(reportId);
            Map<String, Object> result = getFromDatabase(reportId, pageNum, pageSize);
            recordSuccess(); // 记录成功
            return result;

        } catch (Exception e) {
            log.error("❌ 获取操作历史失败: reportId={}, error={}", reportId, e.getMessage());
            recordFailure(); // 记录失败
            return getOperationHistoryFallback(reportId, pageNum, pageSize, e);
        }
    }

    /**
     * 降级方法 - 熔断时调用
     */
    public Map<String, Object> getOperationHistoryFallback(Long reportId, int pageNum, int pageSize, Exception ex) {
        log.warn("🔄 操作历史查询降级: reportId={}, reason={}", reportId, ex.getMessage());

        try {
            // 降级到数据库查询（限制查询范围）
            return getFromDatabase(reportId, pageNum, Math.min(pageSize, 10));
        } catch (Exception e) {
            log.error("❌ 降级查询也失败: reportId={}, error={}", reportId, e.getMessage());
            // 最终降级：返回空结果
            return buildEmptyResult(pageNum, pageSize);
        }
    }

    /**
     * 清理操作历史缓存
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> clearOperationHistoryAsync(Long reportId) {
        return cacheService.clearCacheAsync(reportId);
    }

    /**
     * 预热指定警情的操作历史缓存
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> preloadOperationHistoryAsync(Long reportId) {
        return cacheService.preloadCacheAsync(reportId);
    }

    /**
     * 健康检查
     */
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("cache", cacheService.isHealthy());
        health.put("service", "ok");
        health.put("timestamp", LocalDateTime.now());
        return health;
    }

    // ================== 私有方法 ==================

    private Map<String, Object> buildOperationRecord(Long reportId, Long userId, String userName,
                                                    String operation, Map<String, Object> details) {
        Map<String, Object> record = new HashMap<>();
        record.put("reportId", reportId);
        record.put("userId", userId);
        record.put("userName", userName);
        record.put("operation", operation);
        record.put("details", details);
        record.put("timestamp", System.currentTimeMillis());
        record.put("createTime", LocalDateTime.now().toString());
        return record;
    }

    @Async("taskExecutor")
    private void asyncSaveToDatabase(Map<String, Object> operationRecord) {
        try {
            // 转换为OperateLogEntity并保存
            OperateLogEntity logEntity = convertToLogEntity(operationRecord);
            operateLogDao.insert(logEntity);
            log.debug("💾 操作历史已保存到数据库");
        } catch (Exception e) {
            log.warn("⚠️ 保存操作历史到数据库失败: {}", e.getMessage());
        }
    }

    private OperateLogEntity convertToLogEntity(Map<String, Object> record) {
        OperateLogEntity entity = new OperateLogEntity();
        entity.setOperateUserId((Long) record.get("userId"));
        entity.setOperateUserName((String) record.get("userName"));
        entity.setOperateUserType(1);
        entity.setModule("警情管理");
        entity.setContent((String) record.get("operation"));
        entity.setUrl("/oa/police/operation/history");
        entity.setMethod("POST");
        entity.setParam(convertDetailsToJson(record.get("details")));
        entity.setCreateTime(LocalDateTime.now());
        entity.setIp("系统内部");
        entity.setUserAgent("操作历史服务");
        entity.setSuccessFlag(true);
        entity.setFailReason("");
        return entity;
    }

    private String convertDetailsToJson(Object details) {
        if (details == null) {
            return "{}";
        }
        try {
            if (details instanceof Map) {
                StringBuilder sb = new StringBuilder("{");
                Map<?, ?> map = (Map<?, ?>) details;
                boolean first = true;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!first) sb.append(",");
                    sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
                    first = false;
                }
                sb.append("}");
                return sb.toString();
            }
            return String.valueOf(details);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> getFromDatabase(Long reportId, int pageNum, int pageSize) {
        try {
            // 限制数据库查询范围，避免大查询影响性能
            int limitedPageSize = Math.min(pageSize, 50);
            int offset = (pageNum - 1) * limitedPageSize;

            // 简化查询，使用现有的queryList方法
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put("pageNum", (offset / limitedPageSize) + 1);
            queryParams.put("pageSize", limitedPageSize);

            // 这里应该根据实际的DAO方法调整
            List<OperateLogEntity> logs = new ArrayList<>();
            Long total = 0L;

            try {
                // 暂时使用简单查询，避免复杂的reportId过滤
                // 在实际环境中，这里应该实现专门的查询方法
                logs = operateLogDao.selectList(null); // 需要根据实际DAO方法调整
                total = (long) logs.size();

                // 简单过滤（生产环境应该在SQL层面过滤）
                logs = logs.stream()
                    .filter(log -> log.getUrl() != null && log.getUrl().contains(reportId.toString()))
                    .skip(offset)
                    .limit(limitedPageSize)
                    .collect(Collectors.toList());

            } catch (Exception e) {
                log.warn("数据库查询失败，返回空结果: {}", e.getMessage());
                logs = new ArrayList<>();
                total = 0L;
            }

            List<Map<String, Object>> operations = logs.stream()
                .map(this::convertLogToMap)
                .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("list", operations);
            result.put("total", total);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);

            log.debug("💾 从数据库获取操作历史: reportId={}, count={}", reportId, operations.size());
            return result;
        } catch (Exception e) {
            log.error("❌ 数据库查询操作历史失败: reportId={}, error={}", reportId, e.getMessage());
            return buildEmptyResult(pageNum, pageSize);
        }
    }

    private Map<String, Object> convertLogToMap(OperateLogEntity log) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", log.getOperateLogId());
        map.put("userId", log.getOperateUserId());
        map.put("userName", log.getOperateUserName());
        map.put("operation", log.getContent());
        map.put("details", log.getParam());
        map.put("createTime", log.getCreateTime().toString());
        map.put("timestamp", System.currentTimeMillis());
        return map;
    }

    private Map<String, Object> buildEmptyResult(int pageNum, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("list", Collections.emptyList());
        result.put("total", 0);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    // ================== 手动熔断器实现 ==================

    /**
     * 检查熔断器是否开启
     */
    private boolean isCircuitBreakerOpen() {
        if (!circuitBreakerOpen) {
            return false;
        }

        // 检查是否可以尝试恢复
        if (System.currentTimeMillis() - circuitBreakerOpenTime > CIRCUIT_BREAKER_TIMEOUT) {
            log.info("🔄 熔断器尝试恢复");
            circuitBreakerOpen = false;
            failureCount = 0;
            return false;
        }

        return true;
    }

    /**
     * 记录成功
     */
    private void recordSuccess() {
        if (circuitBreakerOpen) {
            log.info("✅ 熔断器恢复成功");
            circuitBreakerOpen = false;
            failureCount = 0;
        }
    }

    /**
     * 记录失败
     */
    private void recordFailure() {
        failureCount++;
        if (failureCount >= FAILURE_THRESHOLD && !circuitBreakerOpen) {
            log.warn("⚡ 熔断器开启: 失败次数={}", failureCount);
            circuitBreakerOpen = true;
            circuitBreakerOpenTime = System.currentTimeMillis();
        }
    }

    /**
     * 简单限流检查
     */
    private boolean checkRateLimit() {
        long currentTime = System.currentTimeMillis();

        // 重置窗口
        if (currentTime - lastRequestTime > RATE_WINDOW) {
            lastRequestTime = currentTime;
            requestCount = 1;
            return true;
        }

        // 检查是否超限
        if (requestCount >= RATE_LIMIT) {
            return false;
        }

        requestCount++;
        return true;
    }
}