package net.lab1024.sa.admin.module.business.oa.police.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 操作历史缓存服务 - 确保不影响主业务流程
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-28
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationHistoryCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 缓存配置
    private static final String CACHE_PREFIX = "police:operation:";
    private static final Duration CACHE_TTL = Duration.ofHours(24); // 24小时过期
    private static final Duration CACHE_TTL_SHORT = Duration.ofMinutes(5); // 短期缓存
    private static final int MAX_BATCH_SIZE = 50; // 最大批处理大小

    /**
     * 异步添加操作记录到缓存
     * 使用有序集合按时间排序，不影响主业务
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> addOperationAsync(Long reportId, Map<String, Object> operation) {
        return CompletableFuture.runAsync(() -> {
            try {
                String key = buildCacheKey(reportId);
                double score = System.currentTimeMillis(); // 使用时间戳作为score排序

                // 使用Redis有序集合存储，按时间自动排序
                redisTemplate.opsForZSet().add(key, operation, score);

                // 设置过期时间
                redisTemplate.expire(key, CACHE_TTL);

                // 保持最近100条记录，删除旧记录
                Long count = redisTemplate.opsForZSet().count(key, 0, Double.MAX_VALUE);
                if (count != null && count > 100) {
                    redisTemplate.opsForZSet().removeRange(key, 0, count - 100 - 1);
                }

                log.debug("✅ 操作记录已缓存: reportId={}", reportId);
            } catch (Exception e) {
                // 缓存失败不应影响主业务，只记录警告日志
                log.warn("⚠️ 操作记录缓存失败: reportId={}, error={}", reportId, e.getMessage());
            }
        });
    }

    /**
     * 批量异步添加操作记录
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> addOperationsBatchAsync(Long reportId, List<Map<String, Object>> operations) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (CollectionUtils.isEmpty(operations)) {
                    return;
                }

                String key = buildCacheKey(reportId);

                // 分批处理，避免大数据量影响性能
                for (int i = 0; i < operations.size(); i += MAX_BATCH_SIZE) {
                    int end = Math.min(i + MAX_BATCH_SIZE, operations.size());
                    List<Map<String, Object>> batch = operations.subList(i, end);

                    // 批量添加到有序集合
                    for (int j = 0; j < batch.size(); j++) {
                        Map<String, Object> operation = batch.get(j);
                        double score = System.currentTimeMillis() + j; // 保证顺序
                        redisTemplate.opsForZSet().add(key, operation, score);
                    }
                }

                // 设置过期时间
                redisTemplate.expire(key, CACHE_TTL);

                log.debug("✅ 批量操作记录已缓存: reportId={}, count={}", reportId, operations.size());
            } catch (Exception e) {
                log.warn("⚠️ 批量操作记录缓存失败: reportId={}, error={}", reportId, e.getMessage());
            }
        });
    }

    /**
     * 获取缓存的操作历史
     * 带熔断保护，失败时返回空列表而不是异常
     */
    public List<Map<String, Object>> getCachedOperations(Long reportId) {
        try {
            String key = buildCacheKey(reportId);

            // 从有序集合获取，按时间倒序
            Set<Object> operations = redisTemplate.opsForZSet()
                .reverseRange(key, 0, -1);

            if (CollectionUtils.isEmpty(operations)) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Object operation : operations) {
                if (operation instanceof Map) {
                    result.add((Map<String, Object>) operation);
                }
            }

            log.debug("📖 从缓存获取操作历史: reportId={}, count={}", reportId, result.size());
            return result;
        } catch (Exception e) {
            log.warn("⚠️ 获取缓存操作历史失败: reportId={}, error={}", reportId, e.getMessage());
            return Collections.emptyList(); // 返回空列表，不影响业务
        }
    }

    /**
     * 分页获取缓存的操作历史
     */
    public Map<String, Object> getCachedOperationsPaged(Long reportId, int pageNum, int pageSize) {
        try {
            String key = buildCacheKey(reportId);

            // 获取总数
            Long total = redisTemplate.opsForZSet().count(key, 0, Double.MAX_VALUE);
            if (total == null || total == 0) {
                return buildEmptyPageResult();
            }

            // 计算分页范围（倒序）
            int start = (pageNum - 1) * pageSize;
            int end = start + pageSize - 1;

            Set<Object> operations = redisTemplate.opsForZSet()
                .reverseRange(key, start, end);

            List<Map<String, Object>> result = new ArrayList<>();
            if (!CollectionUtils.isEmpty(operations)) {
                for (Object operation : operations) {
                    if (operation instanceof Map) {
                        result.add((Map<String, Object>) operation);
                    }
                }
            }

            Map<String, Object> pageResult = new HashMap<>();
            pageResult.put("list", result);
            pageResult.put("total", total);
            pageResult.put("pageNum", pageNum);
            pageResult.put("pageSize", pageSize);

            log.debug("📄 分页获取缓存操作历史: reportId={}, page={}, size={}, total={}",
                     reportId, pageNum, pageSize, total);
            return pageResult;
        } catch (Exception e) {
            log.warn("⚠️ 分页获取缓存操作历史失败: reportId={}, error={}", reportId, e.getMessage());
            return buildEmptyPageResult();
        }
    }

    /**
     * 清除指定警情的操作历史缓存
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> clearCacheAsync(Long reportId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String key = buildCacheKey(reportId);
                redisTemplate.delete(key);
                log.debug("🗑️ 已清除操作历史缓存: reportId={}", reportId);
            } catch (Exception e) {
                log.warn("⚠️ 清除操作历史缓存失败: reportId={}, error={}", reportId, e.getMessage());
            }
        });
    }

    /**
     * 预热缓存 - 在警情详情页面加载时异步预热
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> preloadCacheAsync(Long reportId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String key = buildCacheKey(reportId);

                // 检查缓存是否存在
                Boolean exists = redisTemplate.hasKey(key);
                if (Boolean.TRUE.equals(exists)) {
                    return; // 缓存已存在
                }

                // 异步从数据库加载并缓存
                // 这里可以调用数据库服务加载历史数据
                log.debug("🔄 开始预热操作历史缓存: reportId={}", reportId);

            } catch (Exception e) {
                log.warn("⚠️ 预热操作历史缓存失败: reportId={}, error={}", reportId, e.getMessage());
            }
        });
    }

    /**
     * 健康检查 - 检查Redis连接状态
     */
    public boolean isHealthy() {
        try {
            redisTemplate.opsForValue().set("health:check", "ok", Duration.ofSeconds(1));
            return true;
        } catch (Exception e) {
            log.warn("⚠️ Redis健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    // ================== 私有方法 ==================

    private String buildCacheKey(Long reportId) {
        return CACHE_PREFIX + "history:" + reportId;
    }

    private Map<String, Object> buildEmptyPageResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("list", Collections.emptyList());
        result.put("total", 0);
        result.put("pageNum", 1);
        result.put("pageSize", 10);
        return result;
    }
}