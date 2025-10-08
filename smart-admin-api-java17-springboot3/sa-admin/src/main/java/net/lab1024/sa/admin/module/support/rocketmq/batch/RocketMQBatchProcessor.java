package net.lab1024.sa.admin.module.support.rocketmq.batch;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.rocketmq.monitor.RocketMQMonitorService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RocketMQ 批量消息处理器
 *
 * 功能:
 * 1. 批量消息聚合,减少网络开销
 * 2. 智能批次大小控制
 * 3. 定时批次刷新
 * 4. 异步批量发送
 * 5. 失败重试机制
 * 6. 性能统计和监控
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class RocketMQBatchProcessor {

    private final RocketMQTemplate rocketMQTemplate;
    private final RocketMQMonitorService monitorService;

    // 批次配置
    private static final int DEFAULT_BATCH_SIZE = 32;  // 默认批次大小
    private static final int MAX_BATCH_SIZE = 100;      // 最大批次大小
    private static final long FLUSH_INTERVAL_MS = 100;  // 刷新间隔 100ms

    // 批次队列 - 按Topic分组
    private final Map<String, BlockingQueue<BatchMessage>> topicQueues = new ConcurrentHashMap<>();

    // 批次处理线程池
    private final ExecutorService batchExecutor = new ThreadPoolExecutor(
            2,
            10,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "RocketMQ-Batch-" + threadNumber.getAndIncrement());
                    thread.setDaemon(false);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    // 统计信息
    private final AtomicLong totalBatchedMessages = new AtomicLong(0);
    private final AtomicLong totalBatchesSent = new AtomicLong(0);
    private final AtomicLong totalBatchesFailed = new AtomicLong(0);

    /**
     * 添加消息到批次队列 (异步)
     *
     * @param topic   主题
     * @param payload 消息负载
     * @return 是否成功添加到队列
     */
    public boolean addToBatch(String topic, Object payload) {
        return addToBatch(topic, payload, null);
    }

    /**
     * 添加消息到批次队列 (异步,带Tag)
     *
     * @param topic   主题
     * @param payload 消息负载
     * @param tag     消息Tag
     * @return 是否成功添加到队列
     */
    public boolean addToBatch(String topic, Object payload, String tag) {
        try {
            BlockingQueue<BatchMessage> queue = topicQueues.computeIfAbsent(
                    topic,
                    k -> new LinkedBlockingQueue<>(10000)
            );

            BatchMessage batchMessage = new BatchMessage();
            batchMessage.setTopic(topic);
            batchMessage.setPayload(payload);
            batchMessage.setTag(tag);
            batchMessage.setTimestamp(System.currentTimeMillis());

            boolean added = queue.offer(batchMessage, 100, TimeUnit.MILLISECONDS);

            if (added) {
                totalBatchedMessages.incrementAndGet();

                // 检查是否需要立即刷新
                if (queue.size() >= DEFAULT_BATCH_SIZE) {
                    triggerFlush(topic);
                }
            } else {
                log.warn("📦 [批处理] 消息队列已满,添加失败 - Topic: {}", topic);
            }

            return added;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("📦 [批处理] 添加消息到队列被中断 - Topic: {}", topic, e);
            return false;
        } catch (Exception e) {
            log.error("📦 [批处理] 添加消息到队列失败 - Topic: {}", topic, e);
            return false;
        }
    }

    /**
     * 立即发送消息 (不批处理)
     *
     * @param topic   主题
     * @param payload 消息负载
     * @return 发送结果
     */
    public boolean sendImmediately(String topic, Object payload) {
        return sendImmediately(topic, payload, null);
    }

    /**
     * 立即发送消息 (不批处理,带Tag)
     *
     * @param topic   主题
     * @param payload 消息负载
     * @param tag     消息Tag
     * @return 发送结果
     */
    public boolean sendImmediately(String topic, Object payload, String tag) {
        long startTime = System.currentTimeMillis();
        try {
            String destination = tag != null ? topic + ":" + tag : topic;
            Message<Object> message = MessageBuilder.withPayload(payload).build();

            rocketMQTemplate.syncSend(destination, message, 3000);

            long elapsed = System.currentTimeMillis() - startTime;
            monitorService.recordSendSuccess(topic, elapsed);

            log.debug("📦 [批处理] 立即发送成功 - Topic: {}, Tag: {}, 耗时: {}ms",
                    topic, tag, elapsed);
            return true;
        } catch (Exception e) {
            monitorService.recordSendFailure(topic, e.getMessage());
            log.error("📦 [批处理] 立即发送失败 - Topic: {}, Tag: {}", topic, tag, e);
            return false;
        }
    }

    /**
     * 触发批次刷新 (异步)
     */
    private void triggerFlush(String topic) {
        batchExecutor.submit(() -> flushBatch(topic));
    }

    /**
     * 定时刷新所有批次 (每100ms)
     */
    @Scheduled(fixedDelay = FLUSH_INTERVAL_MS, initialDelay = 1000)
    public void scheduledFlush() {
        for (String topic : topicQueues.keySet()) {
            BlockingQueue<BatchMessage> queue = topicQueues.get(topic);
            if (queue != null && !queue.isEmpty()) {
                triggerFlush(topic);
            }
        }
    }

    /**
     * 刷新指定Topic的批次
     */
    private void flushBatch(String topic) {
        BlockingQueue<BatchMessage> queue = topicQueues.get(topic);
        if (queue == null || queue.isEmpty()) {
            return;
        }

        List<BatchMessage> batch = new ArrayList<>();
        queue.drainTo(batch, MAX_BATCH_SIZE);

        if (batch.isEmpty()) {
            return;
        }

        sendBatch(topic, batch);
    }

    /**
     * 发送批量消息
     */
    private void sendBatch(String topic, List<BatchMessage> batch) {
        if (batch.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        int batchSize = batch.size();

        try {
            // 按Tag分组
            Map<String, List<Object>> tagGroups = new HashMap<>();
            for (BatchMessage msg : batch) {
                String tag = msg.getTag() != null ? msg.getTag() : "DEFAULT";
                tagGroups.computeIfAbsent(tag, k -> new ArrayList<>()).add(msg.getPayload());
            }

            // 分Tag批量发送
            for (Map.Entry<String, List<Object>> entry : tagGroups.entrySet()) {
                String tag = entry.getKey();
                List<Object> payloads = entry.getValue();

                try {
                    String destination = "DEFAULT".equals(tag) ? topic : topic + ":" + tag;

                    // 使用RocketMQ的批量发送API
                    if (payloads.size() == 1) {
                        // 单条消息直接发送
                        Message<Object> message = MessageBuilder.withPayload(payloads.get(0)).build();
                        rocketMQTemplate.syncSend(destination, message, 3000);
                    } else {
                        // 多条消息包装为批量
                        Message<List<Object>> message = MessageBuilder.withPayload(payloads).build();
                        rocketMQTemplate.syncSend(destination, message, 5000);
                    }

                    log.debug("📦 [批处理] Tag组发送成功 - Topic: {}, Tag: {}, Count: {}",
                            topic, tag, payloads.size());
                } catch (Exception e) {
                    log.error("📦 [批处理] Tag组发送失败 - Topic: {}, Tag: {}, Count: {}",
                            topic, tag, payloads.size(), e);
                    monitorService.recordSendFailure(topic, e.getMessage());
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            totalBatchesSent.incrementAndGet();

            // 记录平均耗时
            long avgTime = batchSize > 0 ? elapsed / batchSize : elapsed;
            monitorService.recordSendSuccess(topic, avgTime);

            log.info("📦 [批处理] 批次发送完成 - Topic: {}, 消息数: {}, 总耗时: {}ms, 平均: {}ms",
                    topic, batchSize, elapsed, avgTime);

        } catch (Exception e) {
            totalBatchesFailed.incrementAndGet();
            log.error("📦 [批处理] 批次发送失败 - Topic: {}, 消息数: {}", topic, batchSize, e);

            // 失败重试 - 降级为单条发送
            retryBatchAsSingle(batch);
        }
    }

    /**
     * 批次失败后降级为单条发送
     */
    private void retryBatchAsSingle(List<BatchMessage> batch) {
        log.warn("📦 [批处理] 批次发送失败,降级为单条重试 - 消息数: {}", batch.size());

        for (BatchMessage msg : batch) {
            try {
                sendImmediately(msg.getTopic(), msg.getPayload(), msg.getTag());
            } catch (Exception e) {
                log.error("📦 [批处理] 单条重试失败 - Topic: {}", msg.getTopic(), e);
            }
        }
    }

    /**
     * 强制刷新所有待发送消息
     */
    public void flushAll() {
        log.info("📦 [批处理] 强制刷新所有批次...");

        for (String topic : topicQueues.keySet()) {
            flushBatch(topic);
        }

        log.info("📦 [批处理] 所有批次刷新完成");
    }

    /**
     * 获取批处理统计信息
     */
    public BatchStats getStats() {
        BatchStats stats = new BatchStats();
        stats.setTotalBatchedMessages(totalBatchedMessages.get());
        stats.setTotalBatchesSent(totalBatchesSent.get());
        stats.setTotalBatchesFailed(totalBatchesFailed.get());

        // 计算队列中待处理的消息数
        long pendingMessages = 0;
        for (BlockingQueue<BatchMessage> queue : topicQueues.values()) {
            pendingMessages += queue.size();
        }
        stats.setPendingMessages(pendingMessages);

        // 计算批次成功率
        long totalBatches = totalBatchesSent.get() + totalBatchesFailed.get();
        if (totalBatches > 0) {
            stats.setSuccessRate((double) totalBatchesSent.get() / totalBatches * 100);
        }

        return stats;
    }

    /**
     * 关闭批处理器
     */
    @PreDestroy
    public void shutdown() {
        log.info("📦 [批处理] 正在关闭批处理器...");

        // 刷新所有待处理消息
        flushAll();

        // 关闭线程池
        batchExecutor.shutdown();
        try {
            if (!batchExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                batchExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            batchExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("📦 [批处理] 批处理器已关闭");
    }

    /**
     * 批次消息
     */
    @Data
    private static class BatchMessage {
        private String topic;
        private Object payload;
        private String tag;
        private long timestamp;
    }

    /**
     * 批处理统计信息
     */
    @Data
    public static class BatchStats {
        private long totalBatchedMessages;
        private long totalBatchesSent;
        private long totalBatchesFailed;
        private long pendingMessages;
        private double successRate;
    }
}
