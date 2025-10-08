package net.lab1024.sa.admin.module.support.rocketmq.test;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.rocketmq.monitor.RocketMQMonitorService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RocketMQ 负载压力测试服务
 *
 * 功能:
 * 1. 模拟100-500并发用户发送消息
 * 2. 测试消息发送性能和稳定性
 * 3. 统计测试结果和性能指标
 * 4. 支持可配置的测试场景
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class RocketMQLoadTestService {

    private final RocketMQTemplate rocketMQTemplate;
    private final RocketMQMonitorService monitorService;

    // 线程池配置
    private static final int CORE_POOL_SIZE = 50;
    private static final int MAX_POOL_SIZE = 500;
    private static final int QUEUE_CAPACITY = 10000;

    // 测试线程池
    private ExecutorService testExecutor;

    // 测试状态
    private volatile boolean testRunning = false;
    private volatile LocalDateTime testStartTime;
    private volatile LocalDateTime testEndTime;

    // 测试统计
    private final AtomicLong totalTestMessages = new AtomicLong(0);
    private final AtomicLong successTestMessages = new AtomicLong(0);
    private final AtomicLong failedTestMessages = new AtomicLong(0);
    private final AtomicLong totalTestTime = new AtomicLong(0);
    private final AtomicInteger activeThreads = new AtomicInteger(0);

    /**
     * 启动负载测试
     *
     * @param config 测试配置
     * @return 测试任务ID
     */
    public String startLoadTest(LoadTestConfig config) {
        if (testRunning) {
            throw new IllegalStateException("已有测试正在运行中,请等待完成后再启动新测试");
        }

        // 重置统计
        resetTestStats();
        testRunning = true;
        testStartTime = LocalDateTime.now();

        // 创建线程池
        testExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                Math.min(config.getConcurrentUsers(), MAX_POOL_SIZE),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "RocketMQ-LoadTest-" + threadNumber.getAndIncrement());
                        thread.setDaemon(false);
                        return thread;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 异步执行测试
        CompletableFuture.runAsync(() -> executeLoadTest(config), testExecutor)
                .whenComplete((result, throwable) -> {
                    testRunning = false;
                    testEndTime = LocalDateTime.now();
                    shutdownExecutor();
                    if (throwable != null) {
                        log.error("🧪 [负载测试] 测试执行失败", throwable);
                    }
                });

        String testId = "LOAD_TEST_" + System.currentTimeMillis();
        log.info("🧪 [负载测试] 测试已启动 - ID: {}, 并发用户: {}, 总消息数: {}",
                testId, config.getConcurrentUsers(), config.getTotalMessages());

        return testId;
    }

    /**
     * 执行负载测试
     */
    private void executeLoadTest(LoadTestConfig config) {
        int concurrentUsers = config.getConcurrentUsers();
        int messagesPerUser = config.getTotalMessages() / concurrentUsers;
        int remainingMessages = config.getTotalMessages() % concurrentUsers;

        log.info("🧪 [负载测试] 开始执行 - 并发用户: {}, 每用户消息数: {}, 剩余消息: {}",
                concurrentUsers, messagesPerUser, remainingMessages);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 为每个并发用户创建任务
        for (int i = 0; i < concurrentUsers; i++) {
            int userId = i + 1;
            int messageCount = messagesPerUser + (i < remainingMessages ? 1 : 0);

            CompletableFuture<Void> future = CompletableFuture.runAsync(
                    () -> sendMessagesForUser(userId, messageCount, config),
                    testExecutor
            );
            futures.add(future);
        }

        // 等待所有任务完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            log.info("🧪 [负载测试] 所有消息发送完成");
        } catch (Exception e) {
            log.error("🧪 [负载测试] 等待任务完成时出错", e);
        }
    }

    /**
     * 为单个用户发送消息
     */
    private void sendMessagesForUser(int userId, int messageCount, LoadTestConfig config) {
        activeThreads.incrementAndGet();
        try {
            log.debug("🧪 [负载测试] 用户 {} 开始发送 {} 条消息", userId, messageCount);

            for (int i = 0; i < messageCount; i++) {
                if (!testRunning) {
                    log.warn("🧪 [负载测试] 测试已停止,用户 {} 终止发送", userId);
                    break;
                }

                sendTestMessage(userId, i + 1, config);

                // 发送间隔控制
                if (config.getMessageIntervalMs() > 0) {
                    try {
                        Thread.sleep(config.getMessageIntervalMs());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            log.debug("🧪 [负载测试] 用户 {} 完成发送", userId);
        } finally {
            activeThreads.decrementAndGet();
        }
    }

    /**
     * 发送测试消息
     */
    private void sendTestMessage(int userId, int messageSeq, LoadTestConfig config) {
        long startTime = System.currentTimeMillis();
        totalTestMessages.incrementAndGet();

        try {
            // 构建测试消息
            Map<String, Object> message = Map.of(
                    "userId", userId,
                    "messageSeq", messageSeq,
                    "timestamp", System.currentTimeMillis(),
                    "testId", config.getTestId(),
                    "payload", generateTestPayload(config.getMessageSize())
            );

            // 发送消息
            String topic = config.getTopic();
            rocketMQTemplate.syncSend(
                    topic,
                    MessageBuilder.withPayload(message).build(),
                    config.getSendTimeout()
            );

            long elapsed = System.currentTimeMillis() - startTime;
            totalTestTime.addAndGet(elapsed);
            successTestMessages.incrementAndGet();

            // 记录到监控服务
            monitorService.recordSendSuccess(topic, elapsed);

            if (totalTestMessages.get() % 1000 == 0) {
                log.info("🧪 [负载测试] 已发送 {} 条消息, 成功率: {:.2f}%",
                        totalTestMessages.get(), getSuccessRate());
            }

        } catch (Exception e) {
            failedTestMessages.incrementAndGet();
            monitorService.recordSendFailure(config.getTopic(), e.getMessage());
            log.error("🧪 [负载测试] 用户 {} 消息 {} 发送失败: {}",
                    userId, messageSeq, e.getMessage());
        }
    }

    /**
     * 生成测试负载数据
     */
    private String generateTestPayload(int sizeInBytes) {
        StringBuilder payload = new StringBuilder(sizeInBytes);
        for (int i = 0; i < sizeInBytes; i++) {
            payload.append((char) ('A' + (i % 26)));
        }
        return payload.toString();
    }

    /**
     * 停止负载测试
     */
    public void stopLoadTest() {
        if (!testRunning) {
            log.warn("🧪 [负载测试] 没有正在运行的测试");
            return;
        }

        log.info("🧪 [负载测试] 正在停止测试...");
        testRunning = false;
        shutdownExecutor();
    }

    /**
     * 关闭线程池
     */
    private void shutdownExecutor() {
        if (testExecutor != null && !testExecutor.isShutdown()) {
            testExecutor.shutdown();
            try {
                if (!testExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    testExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                testExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 获取测试报告
     */
    public LoadTestReport getTestReport() {
        LoadTestReport report = new LoadTestReport();
        report.setTestRunning(testRunning);
        report.setTestStartTime(testStartTime);
        report.setTestEndTime(testEndTime);
        report.setTotalMessages(totalTestMessages.get());
        report.setSuccessMessages(successTestMessages.get());
        report.setFailedMessages(failedTestMessages.get());
        report.setSuccessRate(getSuccessRate());
        report.setAverageTimeMs(getAverageTime());
        report.setActiveThreads(activeThreads.get());
        report.setThroughputPerSecond(calculateThroughput());

        return report;
    }

    /**
     * 重置测试统计
     */
    private void resetTestStats() {
        totalTestMessages.set(0);
        successTestMessages.set(0);
        failedTestMessages.set(0);
        totalTestTime.set(0);
        activeThreads.set(0);
        testStartTime = null;
        testEndTime = null;
    }

    /**
     * 计算成功率
     */
    private double getSuccessRate() {
        long total = totalTestMessages.get();
        if (total == 0) return 0.0;
        return (double) successTestMessages.get() / total * 100;
    }

    /**
     * 计算平均耗时
     */
    private long getAverageTime() {
        long success = successTestMessages.get();
        if (success == 0) return 0;
        return totalTestTime.get() / success;
    }

    /**
     * 计算吞吐量 (消息/秒)
     */
    private double calculateThroughput() {
        if (testStartTime == null) return 0.0;

        LocalDateTime endTime = testEndTime != null ? testEndTime : LocalDateTime.now();
        long durationSeconds = java.time.Duration.between(testStartTime, endTime).getSeconds();

        if (durationSeconds == 0) return 0.0;
        return (double) totalTestMessages.get() / durationSeconds;
    }

    /**
     * 负载测试配置
     */
    @Data
    public static class LoadTestConfig {
        /**
         * 测试ID
         */
        private String testId = "DEFAULT";

        /**
         * 主题名称
         */
        private String topic = "police-sync-topic";

        /**
         * 并发用户数 (100-500)
         */
        private int concurrentUsers = 100;

        /**
         * 总消息数
         */
        private int totalMessages = 10000;

        /**
         * 消息大小 (字节)
         */
        private int messageSize = 1024;

        /**
         * 消息发送间隔 (毫秒, 0表示无间隔)
         */
        private long messageIntervalMs = 0;

        /**
         * 发送超时 (毫秒)
         */
        private long sendTimeout = 3000;
    }

    /**
     * 负载测试报告
     */
    @Data
    public static class LoadTestReport {
        private boolean testRunning;
        private LocalDateTime testStartTime;
        private LocalDateTime testEndTime;
        private long totalMessages;
        private long successMessages;
        private long failedMessages;
        private double successRate;
        private long averageTimeMs;
        private int activeThreads;
        private double throughputPerSecond;
    }
}
