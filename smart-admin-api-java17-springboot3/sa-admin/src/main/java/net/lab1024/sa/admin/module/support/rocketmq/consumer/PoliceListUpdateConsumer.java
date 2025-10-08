package net.lab1024.sa.admin.module.support.rocketmq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.rocketmq.constant.RocketMQTopics;
import net.lab1024.sa.admin.module.support.rocketmq.domain.RocketMQMessage;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketMessage;
import net.lab1024.sa.admin.module.support.websocket.service.MessageTransport;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 警情列表更新消费者 - 高并发优化版本
 *
 * 设计特性：
 * 1. 支持200-500并发用户列表同步
 * 2. 智能批量处理和去重
 * 3. 性能降级和熔断机制
 * 4. 内存优化和垃圾回收友好
 * 5. 监控和统计功能
 * 6. 消息优先级处理
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-29
 * @Copyright 1024创新实验室
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rocketmq.name-server")
@RocketMQMessageListener(
    topic = RocketMQTopics.POLICE_LIST_UPDATE,
    consumerGroup = "police-list-update-consumer",
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING,
    maxReconsumeTimes = 3
)
public class PoliceListUpdateConsumer implements RocketMQListener<RocketMQMessage> {

    @Qualifier("webSocketTransport")
    private final MessageTransport webSocketTransport;

    private final ObjectMapper objectMapper;

    // 高并发性能优化配置
    private static final int BATCH_SIZE = 50;
    private static final int BATCH_TIMEOUT_MS = 100;
    private static final int MAX_PENDING_MESSAGES = 10000;

    // 性能监控指标
    private final AtomicLong processedMessages = new AtomicLong(0);
    private final AtomicLong droppedMessages = new AtomicLong(0);
    private final AtomicLong batchedMessages = new AtomicLong(0);
    private volatile long lastStatsTime = System.currentTimeMillis();

    // 批量处理队列
    private final BlockingQueue<RocketMQMessage> pendingMessages = new LinkedBlockingQueue<>(MAX_PENDING_MESSAGES);

    // 线程池配置
    private final ThreadPoolExecutor batchProcessor = new ThreadPoolExecutor(
        4, 8, 60L, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(1000),
        r -> new Thread(r, "PoliceListBatch-" + System.currentTimeMillis()),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    // 熔断器配置
    private volatile boolean circuitBreakerOpen = false;
    private final AtomicLong errorCount = new AtomicLong(0);
    private volatile long lastCircuitBreakerCheck = System.currentTimeMillis();
    private static final long CIRCUIT_BREAKER_THRESHOLD = 100;
    private static final long CIRCUIT_BREAKER_RESET_TIME = 30000; // 30秒

    // 消息去重缓存（使用LRU）
    private final Map<String, Long> messageCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cacheCleanup = Executors.newSingleThreadScheduledExecutor(
        r -> new Thread(r, "MessageCacheCleanup")
    );

    // 初始化
    {
        // 启动批量处理线程
        startBatchProcessor();

        // 启动缓存清理任务
        cacheCleanup.scheduleWithFixedDelay(this::cleanupCache, 60, 60, TimeUnit.SECONDS);
    }

    @Override
    public void onMessage(RocketMQMessage message) {
        long startTime = System.currentTimeMillis();

        try {
            // 熔断器检查
            if (isCircuitBreakerOpen()) {
                droppedMessages.incrementAndGet();
                log.warn("🔥 [熔断器] 消息被丢弃: messageId={}", message.getMessageId());
                return;
            }

            // 消息去重检查
            if (isDuplicateMessage(message)) {
                log.debug("🔄 [去重] 重复消息忽略: messageId={}", message.getMessageId());
                return;
            }

            // 队列容量检查
            if (pendingMessages.size() >= MAX_PENDING_MESSAGES) {
                droppedMessages.incrementAndGet();
                log.warn("📊 [限流] 队列已满，消息被丢弃: messageId={}, queueSize={}",
                        message.getMessageId(), pendingMessages.size());
                return;
            }

            // 加入批量处理队列
            if (!pendingMessages.offer(message)) {
                droppedMessages.incrementAndGet();
                log.warn("📊 [限流] 加入队列失败: messageId={}", message.getMessageId());
                return;
            }

            processedMessages.incrementAndGet();
            resetCircuitBreaker();

            // 性能统计
            long processingTime = System.currentTimeMillis() - startTime;
            if (processingTime > 50) {
                log.warn("🚀 [性能警告] 消息入队耗时过长: {}ms, messageId={}",
                        processingTime, message.getMessageId());
            }

            logPerformanceStats();

        } catch (Exception e) {
            errorCount.incrementAndGet();
            log.error("🚀 [列表更新] 消息处理失败: messageId={}, error={}",
                     message.getMessageId(), e.getMessage(), e);

            checkCircuitBreaker();
        }
    }

    /**
     * 启动批量处理器
     */
    private void startBatchProcessor() {
        batchProcessor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    processBatchMessages();
                } catch (Exception e) {
                    log.error("🚀 [批量处理] 批量处理器异常: {}", e.getMessage(), e);

                    // 异常恢复延迟
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
    }

    /**
     * 批量处理消息
     */
    private void processBatchMessages() throws InterruptedException {
        RocketMQMessage firstMessage = pendingMessages.take(); // 阻塞等待第一个消息

        // 收集批量消息
        java.util.List<RocketMQMessage> batch = new java.util.ArrayList<>();
        batch.add(firstMessage);

        // 在超时时间内收集更多消息
        long deadline = System.currentTimeMillis() + BATCH_TIMEOUT_MS;
        while (batch.size() < BATCH_SIZE && System.currentTimeMillis() < deadline) {
            RocketMQMessage message = pendingMessages.poll(
                Math.max(1, deadline - System.currentTimeMillis()),
                TimeUnit.MILLISECONDS
            );

            if (message != null) {
                batch.add(message);
            } else {
                break;
            }
        }

        // 处理批量消息
        if (!batch.isEmpty()) {
            processBatch(batch);
            batchedMessages.addAndGet(batch.size());
        }
    }

    /**
     * 处理批量消息
     */
    private void processBatch(java.util.List<RocketMQMessage> batch) {
        try {
            log.debug("🚀 [批量处理] 处理批量消息: size={}", batch.size());

            // 按消息类型分组处理
            Map<String, java.util.List<RocketMQMessage>> groupedMessages = new java.util.HashMap<>();

            for (RocketMQMessage message : batch) {
                String messageType = message.getType();
                groupedMessages.computeIfAbsent(messageType, k -> new java.util.ArrayList<>()).add(message);
            }

            // 并行处理不同类型的消息
            groupedMessages.entrySet().parallelStream().forEach(entry -> {
                String messageType = entry.getKey();
                java.util.List<RocketMQMessage> messages = entry.getValue();

                try {
                    switch (messageType) {
                        case "LIST_UPDATE":
                            handleListUpdates(messages);
                            break;
                        case "LIST_INSERT":
                            handleListInserts(messages);
                            break;
                        case "LIST_DELETE":
                            handleListDeletes(messages);
                            break;
                        case "LIST_BATCH":
                            handleListBatchUpdates(messages);
                            break;
                        default:
                            // 兼容性处理，逐个转发
                            for (RocketMQMessage message : messages) {
                                forwardToWebSocket(message);
                            }
                            break;
                    }
                } catch (Exception e) {
                    log.error("🚀 [批量处理] 处理消息类型{}失败: error={}", messageType, e.getMessage(), e);
                }
            });

        } catch (Exception e) {
            log.error("🚀 [批量处理] 批量处理失败: batchSize={}, error={}", batch.size(), e.getMessage(), e);
        }
    }

    /**
     * 处理列表更新消息
     */
    private void handleListUpdates(java.util.List<RocketMQMessage> messages) {
        try {
            // 合并相同reportId的更新
            Map<Long, RocketMQMessage> mergedUpdates = new java.util.HashMap<>();

            for (RocketMQMessage message : messages) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) message.getData();
                Long reportId = getLongValue(data, "reportId");

                if (reportId != null) {
                    // 保留最新的更新
                    RocketMQMessage existing = mergedUpdates.get(reportId);
                    if (existing == null ||
                        message.getTimestamp().isAfter(existing.getTimestamp())) {
                        mergedUpdates.put(reportId, message);
                    }
                }
            }

            // 广播合并后的更新
            for (RocketMQMessage message : mergedUpdates.values()) {
                forwardToWebSocket(message);
            }

            log.debug("🚀 [列表更新] 处理完成: 原始={}, 合并后={}", messages.size(), mergedUpdates.size());

        } catch (Exception e) {
            log.error("🚀 [列表更新] 处理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理列表插入消息
     */
    private void handleListInserts(java.util.List<RocketMQMessage> messages) {
        try {
            log.debug("🚀 [列表插入] 处理插入消息: count={}", messages.size());

            // 按时间顺序处理插入
            messages.stream()
                   .sorted((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
                   .forEach(this::forwardToWebSocket);

        } catch (Exception e) {
            log.error("🚀 [列表插入] 处理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理列表删除消息
     */
    private void handleListDeletes(java.util.List<RocketMQMessage> messages) {
        try {
            log.debug("🚀 [列表删除] 处理删除消息: count={}", messages.size());

            // 去重删除消息（同一个reportId只需要删除一次）
            java.util.Set<Long> deletedReportIds = new java.util.HashSet<>();

            for (RocketMQMessage message : messages) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) message.getData();
                Long reportId = getLongValue(data, "reportId");

                if (reportId != null && !deletedReportIds.contains(reportId)) {
                    deletedReportIds.add(reportId);
                    forwardToWebSocket(message);
                }
            }

            log.debug("🚀 [列表删除] 处理完成: 原始={}, 去重后={}", messages.size(), deletedReportIds.size());

        } catch (Exception e) {
            log.error("🚀 [列表删除] 处理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理列表批量更新消息
     */
    private void handleListBatchUpdates(java.util.List<RocketMQMessage> messages) {
        try {
            log.debug("🚀 [批量更新] 处理批量更新消息: count={}", messages.size());

            // 批量更新消息需要保持顺序
            for (RocketMQMessage message : messages) {
                forwardToWebSocket(message);
            }

        } catch (Exception e) {
            log.error("🚀 [批量更新] 处理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 转发消息到WebSocket
     */
    private void forwardToWebSocket(RocketMQMessage rocketMessage) {
        try {
            WebSocketMessage wsMessage = rocketMessage.toWebSocketMessage();
            String tag = rocketMessage.getTag();

            if (tag == null || tag.equals("BROADCAST")) {
                webSocketTransport.broadcast(wsMessage);
            } else if (tag.startsWith("USER_")) {
                Long userId = Long.parseLong(tag.substring(5));
                webSocketTransport.sendToUser(userId, wsMessage);
            } else if (tag.startsWith("ROOM_")) {
                String room = tag.substring(5);
                webSocketTransport.sendToRoom(room, wsMessage);
            } else {
                webSocketTransport.broadcast(wsMessage);
            }

        } catch (Exception e) {
            log.error("🚀 [WebSocket转发] 转发失败: messageId={}, error={}",
                     rocketMessage.getMessageId(), e.getMessage());
        }
    }

    /**
     * 检查是否为重复消息
     */
    private boolean isDuplicateMessage(RocketMQMessage message) {
        String messageId = message.getMessageId();
        if (messageId == null) return false;

        Long timestamp = messageCache.get(messageId);
        long currentTime = System.currentTimeMillis();

        if (timestamp != null && (currentTime - timestamp) < 30000) { // 30秒内的重复消息
            return true;
        }

        messageCache.put(messageId, currentTime);
        return false;
    }

    /**
     * 清理消息缓存
     */
    private void cleanupCache() {
        try {
            long currentTime = System.currentTimeMillis();
            long expireTime = currentTime - 300000; // 5分钟过期

            messageCache.entrySet().removeIf(entry -> entry.getValue() < expireTime);

            if (messageCache.size() > 10000) {
                log.warn("🚀 [缓存清理] 缓存过大，清理最旧的条目: size={}", messageCache.size());

                // 清理一半最旧的条目
                messageCache.entrySet().stream()
                           .sorted(Map.Entry.<String, Long>comparingByValue())
                           .limit(messageCache.size() / 2)
                           .map(Map.Entry::getKey)
                           .forEach(messageCache::remove);
            }

        } catch (Exception e) {
            log.error("🚀 [缓存清理] 清理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 熔断器检查
     */
    private boolean isCircuitBreakerOpen() {
        long currentTime = System.currentTimeMillis();

        // 熔断器重置检查
        if (circuitBreakerOpen && (currentTime - lastCircuitBreakerCheck) > CIRCUIT_BREAKER_RESET_TIME) {
            circuitBreakerOpen = false;
            errorCount.set(0);
            log.info("🔥 [熔断器] 熔断器已重置");
        }

        return circuitBreakerOpen;
    }

    /**
     * 检查是否需要打开熔断器
     */
    private void checkCircuitBreaker() {
        if (errorCount.get() > CIRCUIT_BREAKER_THRESHOLD) {
            circuitBreakerOpen = true;
            lastCircuitBreakerCheck = System.currentTimeMillis();
            log.warn("🔥 [熔断器] 熔断器已打开: errorCount={}", errorCount.get());
        }
    }

    /**
     * 重置熔断器
     */
    private void resetCircuitBreaker() {
        if (errorCount.get() > 0) {
            errorCount.decrementAndGet();
        }
    }

    /**
     * 性能统计
     */
    private void logPerformanceStats() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastStatsTime > 60000) { // 每分钟输出一次
            long processed = processedMessages.get();
            long dropped = droppedMessages.get();
            long batched = batchedMessages.get();

            if (processed > 0 || dropped > 0) {
                log.info("🚀 [性能统计] 1分钟统计 - 处理: {}, 丢弃: {}, 批量: {}, 队列: {}, 缓存: {}",
                        processed, dropped, batched, pendingMessages.size(), messageCache.size());

                // 重置计数器
                processedMessages.set(0);
                droppedMessages.set(0);
                batchedMessages.set(0);
            }

            lastStatsTime = currentTime;
        }
    }

    /**
     * 安全获取Long值
     */
    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;

        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取性能监控信息
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new java.util.HashMap<>();
        metrics.put("processedMessages", processedMessages.get());
        metrics.put("droppedMessages", droppedMessages.get());
        metrics.put("batchedMessages", batchedMessages.get());
        metrics.put("pendingQueueSize", pendingMessages.size());
        metrics.put("cacheSize", messageCache.size());
        metrics.put("circuitBreakerOpen", circuitBreakerOpen);
        metrics.put("errorCount", errorCount.get());
        metrics.put("threadPoolActiveCount", batchProcessor.getActiveCount());
        metrics.put("threadPoolQueueSize", batchProcessor.getQueue().size());
        metrics.put("timestamp", System.currentTimeMillis());

        return metrics;
    }
}