package net.lab1024.sa.admin.config;

// 移除Resilience4j依赖，使用简单的线程池配置
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * 操作历史服务配置 - 高性能配置
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-28
 * @Copyright 1024创新实验室
 */
@Slf4j
@Configuration
@EnableAsync
public class OperationHistoryConfig {

    /**
     * 操作历史专用线程池 - 与主业务隔离
     */
    @Bean("operationHistoryExecutor")
    public Executor operationHistoryExecutor() {
        return createTaskExecutor();
    }

    /**
     * 通用任务执行器 - 兼容@Async("taskExecutor")
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        return createTaskExecutor();
    }

    /**
     * 创建任务执行器的通用方法
     */
    private ThreadPoolTaskExecutor createTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：保持活跃处理能力
        executor.setCorePoolSize(2);

        // 最大线程数：高峰期扩容能力
        executor.setMaxPoolSize(8);

        // 队列容量：缓冲突发请求
        executor.setQueueCapacity(1000);

        // 线程名称前缀
        executor.setThreadNamePrefix("operation-history-");

        // 拒绝策略：丢弃最老的任务（操作历史可以丢失）
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("⚠️ 操作历史任务队列已满，丢弃任务: {}", r.toString());
        });

        // 空闲线程存活时间
        executor.setKeepAliveSeconds(60);

        // 关闭时等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("✅ 操作历史线程池已初始化: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * 操作历史配置属性Bean - 替代熔断器和限流器
     */
    @Bean
    public OperationHistoryProperties operationHistoryProperties() {
        OperationHistoryProperties properties = new OperationHistoryProperties();
        properties.setFailureThreshold(5);
        properties.setCircuitBreakerTimeout(30000L);
        properties.setRateLimit(50);
        properties.setRateWindow(1000L);

        log.info("⚙️ 操作历史配置初始化: {}", properties);
        return properties;
    }

    /**
     * 配置属性类
     */
    public static class OperationHistoryProperties {
        private int failureThreshold = 5;
        private long circuitBreakerTimeout = 30000L;
        private int rateLimit = 50;
        private long rateWindow = 1000L;

        // getters and setters
        public int getFailureThreshold() { return failureThreshold; }
        public void setFailureThreshold(int failureThreshold) { this.failureThreshold = failureThreshold; }
        public long getCircuitBreakerTimeout() { return circuitBreakerTimeout; }
        public void setCircuitBreakerTimeout(long circuitBreakerTimeout) { this.circuitBreakerTimeout = circuitBreakerTimeout; }
        public int getRateLimit() { return rateLimit; }
        public void setRateLimit(int rateLimit) { this.rateLimit = rateLimit; }
        public long getRateWindow() { return rateWindow; }
        public void setRateWindow(long rateWindow) { this.rateWindow = rateWindow; }

        @Override
        public String toString() {
            return "OperationHistoryProperties{" +
                    "failureThreshold=" + failureThreshold +
                    ", circuitBreakerTimeout=" + circuitBreakerTimeout +
                    ", rateLimit=" + rateLimit +
                    ", rateWindow=" + rateWindow +
                    '}';
        }
    }
}