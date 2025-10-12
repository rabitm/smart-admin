package net.lab1024.sa.admin.module.support.im.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * IM异步配置
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@EnableAsync
@Configuration
public class IMAsyncConfig {

    /**
     * IM异步任务线程池
     */
    @Bean(name = "imAsyncExecutor")
    public Executor imAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(5);

        // 最大线程数
        executor.setMaxPoolSize(10);

        // 队列容量
        executor.setQueueCapacity(200);

        // 线程空闲时间(秒)
        executor.setKeepAliveSeconds(60);

        // 线程名称前缀
        executor.setThreadNamePrefix("im-async-");

        // 拒绝策略:由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务完成后关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间(秒)
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("✅ IM异步任务线程池初始化完成 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }
}
