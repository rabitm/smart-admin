package net.lab1024.sa.admin.module.business.oa.police.service;

import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceReportEntity;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketMessage;
import net.lab1024.sa.admin.module.support.websocket.service.WebSocketSessionManager;
import net.lab1024.sa.admin.module.support.websocket.service.impl.WebSocketTransport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 高性能警情列表更新服务
 * 支持200-500个并发端的实时列表更新
 *
 * 核心特性:
 * 1. 增量更新推送
 * 2. 消息批量合并
 * 3. 智能限流控制
 * 4. Delta压缩传输
 * 5. Redis缓存加速
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-26
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service
public class PoliceListUpdateService {

    private final WebSocketSessionManager sessionManager;
    private final WebSocketTransport webSocketTransport;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ThreadPoolExecutor broadcastExecutor;

    public PoliceListUpdateService(
            WebSocketSessionManager sessionManager,
            WebSocketTransport webSocketTransport,
            @Qualifier("policeRedisTemplate") RedisTemplate<String, Object> redisTemplate,
            @Qualifier("webSocketBroadcastExecutor") ThreadPoolExecutor broadcastExecutor) {
        this.sessionManager = sessionManager;
        this.webSocketTransport = webSocketTransport;
        this.redisTemplate = redisTemplate;
        this.broadcastExecutor = broadcastExecutor;
    }

    // 更新缓冲区
    private final Map<Long, UpdateBuffer> updateBuffers = new ConcurrentHashMap<>();

    // 列表订阅管理
    private final Map<String, Set<String>> listSubscribers = new ConcurrentHashMap<>();

    // 性能统计
    private final AtomicInteger totalUpdates = new AtomicInteger(0);
    private final AtomicInteger batchedUpdates = new AtomicInteger(0);
    private final AtomicInteger droppedUpdates = new AtomicInteger(0);

    // 高并发配置常量（针对200-500终端优化）
    private static final int BATCH_SIZE = 50; // 增大批量大小
    private static final int BUFFER_TIME_MS = 50; // 降低延迟
    private static final int MAX_BUFFER_SIZE = 200; // 增大缓冲区
    private static final int MAX_CONCURRENT_BROADCASTS = 100; // 最大并发广播数
    private static final int CIRCUIT_BREAKER_THRESHOLD = 1000; // 熔断阈值
    private static final String REDIS_LIST_KEY_PREFIX = "police:list:";
    private static final String REDIS_UPDATE_CHANNEL = "police:list:updates";

    // 并发控制和性能监控
    private final Semaphore broadcastSemaphore = new Semaphore(MAX_CONCURRENT_BROADCASTS);
    private final AtomicInteger activeBroadcasts = new AtomicInteger(0);
    private final AtomicInteger circuitBreakerCounter = new AtomicInteger(0);
    private volatile boolean circuitBreakerOpen = false;

    /**
     * 更新缓冲区
     */
    private static class UpdateBuffer {
        final Long reportId;
        final Map<String, Object> changes = new ConcurrentHashMap<>();
        final Set<String> changedFields = ConcurrentHashMap.newKeySet();
        long lastUpdateTime = System.currentTimeMillis();
        int version = 0;

        UpdateBuffer(Long reportId) {
            this.reportId = reportId;
        }

        void addChange(String field, Object value) {
            changes.put(field, value);
            changedFields.add(field);
            lastUpdateTime = System.currentTimeMillis();
            version++;
        }
    }

    /**
     * 处理警情更新
     */
    @Async
    public void handleReportUpdate(Long reportId, Map<String, Object> changes, String userId, String userName) {
        try {
            // 添加到更新缓冲区
            UpdateBuffer buffer = updateBuffers.computeIfAbsent(reportId, UpdateBuffer::new);

            synchronized (buffer) {
                changes.forEach(buffer::addChange);
            }

            totalUpdates.incrementAndGet();

            // 如果缓冲区过大，立即刷新
            if (buffer.changes.size() > MAX_BUFFER_SIZE) {
                flushUpdateBuffer(reportId);
            }

            // 更新Redis缓存
            updateRedisCache(reportId, changes);

            log.debug("警情更新已缓冲: reportId={}, fields={}, user={}",
                reportId, changes.keySet(), userName);

        } catch (Exception e) {
            log.error("处理警情更新失败: reportId={}", reportId, e);
        }
    }

    /**
     * 定时刷新更新缓冲区
     */
    @Scheduled(fixedDelay = BUFFER_TIME_MS)
    public void flushAllBuffers() {
        if (updateBuffers.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        List<Long> toFlush = new ArrayList<>();

        // 找出需要刷新的缓冲区
        updateBuffers.forEach((reportId, buffer) -> {
            if (now - buffer.lastUpdateTime >= BUFFER_TIME_MS) {
                toFlush.add(reportId);
            }
        });

        // 批量刷新
        if (!toFlush.isEmpty()) {
            batchFlushBuffers(toFlush);
        }
    }

    /**
     * 批量刷新缓冲区
     */
    private void batchFlushBuffers(List<Long> reportIds) {
        // 分批处理
        List<List<Long>> batches = partition(reportIds, BATCH_SIZE);

        for (List<Long> batch : batches) {
            try {
                // 构建批量更新消息
                List<Map<String, Object>> updates = new ArrayList<>();

                for (Long reportId : batch) {
                    UpdateBuffer buffer = updateBuffers.remove(reportId);
                    if (buffer != null && !buffer.changes.isEmpty()) {
                        Map<String, Object> update = new HashMap<>();
                        update.put("reportId", reportId);
                        update.put("data", new HashMap<>(buffer.changes));
                        update.put("fields", new ArrayList<>(buffer.changedFields));
                        update.put("version", buffer.version);
                        update.put("timestamp", buffer.lastUpdateTime);
                        updates.add(update);
                    }
                }

                if (!updates.isEmpty()) {
                    // 创建批量消息
                    Map<String, Object> batchMessage = new HashMap<>();
                    batchMessage.put("type", "BATCH");
                    batchMessage.put("updates", updates);
                    batchMessage.put("count", updates.size());
                    batchMessage.put("timestamp", System.currentTimeMillis());

                    // 广播批量更新
                    broadcastListUpdate(batchMessage);
                    batchedUpdates.addAndGet(updates.size());

                    log.info("批量刷新警情更新: count={}", updates.size());
                }

            } catch (Exception e) {
                log.error("批量刷新失败: batch={}", batch, e);
            }
        }
    }

    /**
     * 刷新单个更新缓冲区
     */
    private void flushUpdateBuffer(Long reportId) {
        UpdateBuffer buffer = updateBuffers.remove(reportId);

        if (buffer != null && !buffer.changes.isEmpty()) {
            Map<String, Object> updateMessage = new HashMap<>();
            updateMessage.put("type", "UPDATE");
            updateMessage.put("reportId", reportId);
            updateMessage.put("data", new HashMap<>(buffer.changes));
            updateMessage.put("fields", new ArrayList<>(buffer.changedFields));
            updateMessage.put("version", buffer.version);
            updateMessage.put("timestamp", buffer.lastUpdateTime);

            broadcastListUpdate(updateMessage);

            log.debug("单个更新已刷新: reportId={}, fields={}",
                reportId, buffer.changedFields);
        }
    }

    /**
     * 广播列表更新（高并发优化版本）
     */
    private void broadcastListUpdate(Map<String, Object> updateData) {
        // 熔断器检查
        if (circuitBreakerOpen) {
            droppedUpdates.incrementAndGet();
            log.warn("🔥 [熔断器] 广播被熔断器阻止，丢弃更新");
            return;
        }

        // 获取所有订阅列表更新的会话
        Set<String> subscribers = listSubscribers.getOrDefault("all", new HashSet<>());

        if (subscribers.isEmpty()) {
            return;
        }

        // 限流控制
        try {
            if (!broadcastSemaphore.tryAcquire(10, TimeUnit.MILLISECONDS)) {
                droppedUpdates.incrementAndGet();
                log.warn("📊 [限流] 广播队列已满，丢弃更新. 活跃广播数: {}", activeBroadcasts.get());
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        int currentBroadcasts = activeBroadcasts.incrementAndGet();
        long startTime = System.currentTimeMillis();

        try {
            // 创建WebSocket消息
            WebSocketMessage message = WebSocketMessage.business("police", "LIST_UPDATE", updateData);

            // 批量分组发送（每组最多100个会话）
            List<List<String>> subscriberBatches = partition(new ArrayList<>(subscribers), 100);

            List<CompletableFuture<Void>> batchTasks = subscriberBatches.stream()
                .map(batch -> CompletableFuture.runAsync(() -> {
                    sendToBatch(batch, message);
                }, broadcastExecutor))
                .collect(Collectors.toList());

            // 等待发送完成（动态超时：基于订阅者数量）
            int timeoutMs = Math.min(Math.max(subscribers.size() / 10, 50), 500);
            CompletableFuture.allOf(batchTasks.toArray(new CompletableFuture[0]))
                .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .exceptionally(throwable -> {
                    log.warn("📡 [广播超时] 部分批次发送超时: {}ms, 订阅者数: {}", timeoutMs, subscribers.size());
                    return null;
                });

            // 性能监控
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 100) {
                log.warn("🐌 [性能警告] 广播耗时过长: {}ms, 订阅者数: {}", duration, subscribers.size());
                circuitBreakerCounter.incrementAndGet();
            }

            // 熔断器逻辑
            if (circuitBreakerCounter.get() > CIRCUIT_BREAKER_THRESHOLD) {
                circuitBreakerOpen = true;
                log.error("⚡ [熔断器] 触发熔断，暂停广播 30秒");
                // 30秒后重置熔断器
                CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS).execute(() -> {
                    circuitBreakerOpen = false;
                    circuitBreakerCounter.set(0);
                    log.info("🔄 [熔断器] 熔断器已重置");
                });
            }

        } finally {
            activeBroadcasts.decrementAndGet();
            broadcastSemaphore.release();
        }
    }

    /**
     * 批量发送到一组订阅者
     */
    private void sendToBatch(List<String> sessionIds, WebSocketMessage message) {
        List<String> failedSessions = new ArrayList<>();

        for (String sessionId : sessionIds) {
            try {
                webSocketTransport.sendToSession(sessionId, message);
            } catch (Exception e) {
                log.debug("💔 [会话失效] sessionId={}", sessionId);
                failedSessions.add(sessionId);
            }
        }

        // 批量移除失效会话
        if (!failedSessions.isEmpty()) {
            failedSessions.forEach(this::removeSubscriber);
            log.info("🧹 [会话清理] 移除失效会话数: {}", failedSessions.size());
        }
    }

    /**
     * 处理新增警情
     */
    public void handleReportInsert(PoliceReportEntity report, String userId, String userName) {
        Map<String, Object> insertMessage = new HashMap<>();
        insertMessage.put("type", "INSERT");
        insertMessage.put("reportId", report.getReportId());
        insertMessage.put("data", convertToUpdateData(report));
        insertMessage.put("timestamp", System.currentTimeMillis());
        insertMessage.put("userId", userId);
        insertMessage.put("userName", userName);

        // 立即广播新增
        broadcastListUpdate(insertMessage);

        // 更新Redis
        updateRedisCache(report.getReportId(), convertToUpdateData(report));

        log.info("新增警情已广播: reportId={}, user={}", report.getReportId(), userName);
    }

    /**
     * 处理删除警情
     */
    public void handleReportDelete(Long reportId, String userId, String userName) {
        Map<String, Object> deleteMessage = new HashMap<>();
        deleteMessage.put("type", "DELETE");
        deleteMessage.put("reportId", reportId);
        deleteMessage.put("timestamp", System.currentTimeMillis());
        deleteMessage.put("userId", userId);
        deleteMessage.put("userName", userName);

        // 立即广播删除
        broadcastListUpdate(deleteMessage);

        // 从Redis删除
        removeFromRedisCache(reportId);

        log.info("删除警情已广播: reportId={}, user={}", reportId, userName);
    }

    /**
     * 订阅列表更新
     */
    public void subscribeListUpdates(String sessionId) {
        listSubscribers.computeIfAbsent("all", k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        log.debug("会话订阅列表更新: sessionId={}", sessionId);
    }

    /**
     * 取消订阅列表更新
     */
    public void unsubscribeListUpdates(String sessionId) {
        Set<String> subscribers = listSubscribers.get("all");
        if (subscribers != null) {
            subscribers.remove(sessionId);
        }
        log.debug("会话取消订阅列表更新: sessionId={}", sessionId);
    }

    /**
     * 移除订阅者
     */
    private void removeSubscriber(String sessionId) {
        listSubscribers.values().forEach(subscribers -> subscribers.remove(sessionId));
    }

    /**
     * 更新Redis缓存
     */
    private void updateRedisCache(Long reportId, Map<String, Object> changes) {
        try {
            String key = REDIS_LIST_KEY_PREFIX + reportId;
            redisTemplate.opsForHash().putAll(key, changes);
            redisTemplate.expire(key, 1, TimeUnit.HOURS);

            // 发布更新通知
            Map<String, Object> notification = new HashMap<>();
            notification.put("reportId", reportId);
            notification.put("changes", changes);
            notification.put("timestamp", System.currentTimeMillis());

            redisTemplate.convertAndSend(REDIS_UPDATE_CHANNEL, notification);
        } catch (Exception e) {
            log.error("更新Redis缓存失败: reportId={}", reportId, e);
        }
    }

    /**
     * 从Redis删除
     */
    private void removeFromRedisCache(Long reportId) {
        try {
            String key = REDIS_LIST_KEY_PREFIX + reportId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("从Redis删除失败: reportId={}", reportId, e);
        }
    }

    /**
     * 转换实体为更新数据
     */
    private Map<String, Object> convertToUpdateData(PoliceReportEntity report) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", report.getReportId()); // 前端期望的 id 字段
        data.put("reportId", report.getReportId()); // 后端的主键字段
        data.put("reportNumber", report.getReportNumber());
        data.put("reportType", report.getReportType());
        data.put("reportLevel", report.getReportLevel());
        data.put("status", report.getStatus());
        data.put("reporterName", report.getReporterName());
        data.put("reporterPhone", report.getReporterPhone());
        data.put("reporterIdCard", report.getReporterIdCard());
        data.put("incidentLocation", report.getIncidentLocation());
        data.put("description", report.getDescription()); // 后端字段名
        data.put("incidentDescription", report.getDescription()); // 前端期望的字段名
        data.put("reportTime", report.getReportTime());
        data.put("handlerName", report.getHandlerName());
        data.put("handlerId", report.getHandlerId());
        data.put("handleResult", report.getHandleResult());
        data.put("handleTime", report.getHandleTime());
        data.put("attachments", report.getAttachments());
        data.put("remark", report.getRemark());
        data.put("createUserId", report.getCreateUserId());
        data.put("createUserName", report.getCreateUserName());
        data.put("createTime", report.getCreateTime());
        data.put("updateTime", report.getUpdateTime());
        data.put("updatedAt", System.currentTimeMillis()); // 前端管理的时间戳
        data.put("version", 1); // 版本控制
        return data;
    }

    /**
     * 列表分区工具
     */
    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    /**
     * 获取性能统计
     */
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUpdates", totalUpdates.get());
        stats.put("batchedUpdates", batchedUpdates.get());
        stats.put("droppedUpdates", droppedUpdates.get());
        stats.put("bufferSize", updateBuffers.size());
        stats.put("subscriberCount", listSubscribers.getOrDefault("all", Collections.emptySet()).size());
        return stats;
    }

    /**
     * 清理过期缓冲区
     */
    @Scheduled(fixedDelay = 60000) // 每分钟清理
    public void cleanupExpiredBuffers() {
        long now = System.currentTimeMillis();
        long expireTime = 5 * 60 * 1000; // 5分钟过期

        updateBuffers.entrySet().removeIf(entry -> {
            UpdateBuffer buffer = entry.getValue();
            if (now - buffer.lastUpdateTime > expireTime) {
                droppedUpdates.incrementAndGet();
                log.warn("清理过期缓冲区: reportId={}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}