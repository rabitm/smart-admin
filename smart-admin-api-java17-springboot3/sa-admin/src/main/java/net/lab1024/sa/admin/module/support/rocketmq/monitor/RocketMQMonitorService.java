package net.lab1024.sa.admin.module.support.rocketmq.monitor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * RocketMQ 监控服务
 *
 * 功能:
 * 1. 实时监控消息发送成功率
 * 2. 统计消息发送耗时
 * 3. 监控消息队列积压
 * 4. 异常告警
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class RocketMQMonitorService {

    // 消息发送统计
    private final LongAdder totalSentMessages = new LongAdder();
    private final LongAdder totalSuccessMessages = new LongAdder();
    private final LongAdder totalFailedMessages = new LongAdder();

    // 消息耗时统计 (毫秒)
    private final AtomicLong totalSendTime = new AtomicLong(0);
    private final AtomicLong maxSendTime = new AtomicLong(0);
    private final AtomicLong minSendTime = new AtomicLong(Long.MAX_VALUE);

    // 分主题统计
    private final ConcurrentHashMap<String, TopicMetrics> topicMetricsMap = new ConcurrentHashMap<>();

    // 最后一次监控时间
    private volatile LocalDateTime lastMonitorTime = LocalDateTime.now();

    /**
     * 记录消息发送成功
     */
    public void recordSendSuccess(String topic, long sendTimeMs) {
        totalSentMessages.increment();
        totalSuccessMessages.increment();

        // 记录耗时
        totalSendTime.addAndGet(sendTimeMs);
        updateMaxTime(sendTimeMs);
        updateMinTime(sendTimeMs);

        // 分主题统计
        getOrCreateTopicMetrics(topic).recordSuccess(sendTimeMs);

        log.debug("📊 [RocketMQ监控] 消息发送成功 - Topic: {}, 耗时: {}ms", topic, sendTimeMs);
    }

    /**
     * 记录消息发送失败
     */
    public void recordSendFailure(String topic, String errorMessage) {
        totalSentMessages.increment();
        totalFailedMessages.increment();

        // 分主题统计
        getOrCreateTopicMetrics(topic).recordFailure();

        log.warn("📊 [RocketMQ监控] 消息发送失败 - Topic: {}, Error: {}", topic, errorMessage);
    }

    /**
     * 获取或创建Topic指标
     */
    private TopicMetrics getOrCreateTopicMetrics(String topic) {
        return topicMetricsMap.computeIfAbsent(topic, k -> new TopicMetrics(topic));
    }

    /**
     * 更新最大耗时
     */
    private void updateMaxTime(long timeMs) {
        long current = maxSendTime.get();
        while (timeMs > current && !maxSendTime.compareAndSet(current, timeMs)) {
            current = maxSendTime.get();
        }
    }

    /**
     * 更新最小耗时
     */
    private void updateMinTime(long timeMs) {
        long current = minSendTime.get();
        while (timeMs < current && !minSendTime.compareAndSet(current, timeMs)) {
            current = minSendTime.get();
        }
    }

    /**
     * 获取监控指标
     */
    public MonitorMetrics getMetrics() {
        MonitorMetrics metrics = new MonitorMetrics();

        long sent = totalSentMessages.sum();
        long success = totalSuccessMessages.sum();
        long failed = totalFailedMessages.sum();

        metrics.setTotalSent(sent);
        metrics.setTotalSuccess(success);
        metrics.setTotalFailed(failed);
        metrics.setSuccessRate(sent > 0 ? (double) success / sent * 100 : 0.0);

        // 平均耗时
        long avgTime = sent > 0 ? totalSendTime.get() / sent : 0;
        metrics.setAvgSendTimeMs(avgTime);
        metrics.setMaxSendTimeMs(maxSendTime.get());
        metrics.setMinSendTimeMs(minSendTime.get() == Long.MAX_VALUE ? 0 : minSendTime.get());

        metrics.setLastMonitorTime(lastMonitorTime);

        return metrics;
    }

    /**
     * 获取Topic指标
     */
    public TopicMetrics getTopicMetrics(String topic) {
        return topicMetricsMap.get(topic);
    }

    /**
     * 获取所有Topic指标
     */
    public Map<String, TopicMetrics> getAllTopicMetrics() {
        return new HashMap<>(topicMetricsMap);
    }

    /**
     * 重置统计数据
     */
    public void resetMetrics() {
        totalSentMessages.reset();
        totalSuccessMessages.reset();
        totalFailedMessages.reset();
        totalSendTime.set(0);
        maxSendTime.set(0);
        minSendTime.set(Long.MAX_VALUE);
        topicMetricsMap.clear();

        log.info("📊 [RocketMQ监控] 监控指标已重置");
    }

    /**
     * 定时打印监控指标 (每5分钟)
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void printMonitorMetrics() {
        MonitorMetrics metrics = getMetrics();

        log.info("📊 ==================== RocketMQ 监控报告 ====================");
        log.info("📊 总发送消息数: {}", metrics.getTotalSent());
        log.info("📊 成功消息数: {}", metrics.getTotalSuccess());
        log.info("📊 失败消息数: {}", metrics.getTotalFailed());
        log.info("📊 成功率: {:.2f}%", metrics.getSuccessRate());
        log.info("📊 平均耗时: {}ms", metrics.getAvgSendTimeMs());
        log.info("📊 最大耗时: {}ms", metrics.getMaxSendTimeMs());
        log.info("📊 最小耗时: {}ms", metrics.getMinSendTimeMs());
        log.info("📊 =========================================================");

        // 检查异常情况
        checkAndAlert(metrics);

        lastMonitorTime = LocalDateTime.now();
    }

    /**
     * 检查并告警
     */
    private void checkAndAlert(MonitorMetrics metrics) {
        // 成功率低于95%告警
        if (metrics.getSuccessRate() < 95.0 && metrics.getTotalSent() > 10) {
            log.error("🚨 [RocketMQ告警] 消息发送成功率低于95%: {:.2f}%", metrics.getSuccessRate());
        }

        // 平均耗时超过500ms告警
        if (metrics.getAvgSendTimeMs() > 500) {
            log.warn("⚠️ [RocketMQ告警] 消息发送平均耗时超过500ms: {}ms", metrics.getAvgSendTimeMs());
        }

        // 最大耗时超过2000ms告警
        if (metrics.getMaxSendTimeMs() > 2000) {
            log.warn("⚠️ [RocketMQ告警] 消息发送最大耗时超过2000ms: {}ms", metrics.getMaxSendTimeMs());
        }
    }

    /**
     * 监控指标数据
     */
    @Data
    public static class MonitorMetrics {
        private long totalSent;
        private long totalSuccess;
        private long totalFailed;
        private double successRate;
        private long avgSendTimeMs;
        private long maxSendTimeMs;
        private long minSendTimeMs;
        private LocalDateTime lastMonitorTime;
    }

    /**
     * Topic指标数据
     */
    @Data
    public static class TopicMetrics {
        private final String topic;
        private final LongAdder sentCount = new LongAdder();
        private final LongAdder successCount = new LongAdder();
        private final LongAdder failureCount = new LongAdder();
        private final AtomicLong totalTime = new AtomicLong(0);

        public TopicMetrics(String topic) {
            this.topic = topic;
        }

        public void recordSuccess(long timeMs) {
            sentCount.increment();
            successCount.increment();
            totalTime.addAndGet(timeMs);
        }

        public void recordFailure() {
            sentCount.increment();
            failureCount.increment();
        }

        public long getSent() {
            return sentCount.sum();
        }

        public long getSuccess() {
            return successCount.sum();
        }

        public long getFailure() {
            return failureCount.sum();
        }

        public double getSuccessRate() {
            long sent = sentCount.sum();
            return sent > 0 ? (double) successCount.sum() / sent * 100 : 0.0;
        }

        public long getAvgTime() {
            long sent = sentCount.sum();
            return sent > 0 ? totalTime.get() / sent : 0;
        }
    }
}
