package net.lab1024.sa.admin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * WebSocket性能优化配置 - 支持200人并发
 *
 * 优化内容:
 * 1. 连接池配置优化
 * 2. 消息处理线程池
 * 3. 心跳检测优化
 * 4. 内存使用优化
 */
@Slf4j
@Configuration
@EnableWebSocket
public class WebSocketOptimizationConfig implements WebSocketConfigurer {

    /**
     * WebSocket消息处理线程池
     * 核心线程: CPU核心数 * 2
     * 最大线程: 200 (支持200人并发)
     * 队列长度: 1000 (缓冲突发流量)
     */
    @Bean(name = "webSocketMessageExecutor")
    public ThreadPoolExecutor webSocketMessageExecutor() {
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        int maximumPoolSize = Math.max(200, corePoolSize * 4);

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            60L, // 空闲线程保活时间
            java.util.concurrent.TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(1000),
            r -> {
                Thread thread = new Thread(r);
                thread.setName("websocket-msg-" + thread.getId());
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // 队列满时由调用线程执行
        );

        log.info("WebSocket消息处理线程池已创建: core={}, max={}, queue=1000",
                corePoolSize, maximumPoolSize);

        return executor;
    }

    /**
     * WebSocket广播专用线程池
     * 用于并行广播消息到多个客户端
     */
    @Bean(name = "webSocketBroadcastExecutor")
    public ThreadPoolExecutor webSocketBroadcastExecutor() {
        int corePoolSize = 50; // 固定50个核心线程用于广播
        int maximumPoolSize = 200; // 最大200线程支持并发广播

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            30L,
            java.util.concurrent.TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(500),
            r -> {
                Thread thread = new Thread(r);
                thread.setName("websocket-broadcast-" + thread.getId());
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy() // 丢弃无法处理的消息
        );

        log.info("WebSocket广播线程池已创建: core={}, max={}", corePoolSize, maximumPoolSize);

        return executor;
    }

    /**
     * 定时任务线程池 - 用于心跳检测和清理
     */
    @Bean(name = "webSocketScheduledExecutor")
    public ScheduledExecutorService webSocketScheduledExecutor() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread thread = new Thread(r);
                thread.setName("websocket-scheduled-" + thread.getId());
                thread.setDaemon(true);
                return thread;
            }
        );

        log.info("WebSocket定时任务线程池已创建");
        return executor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // WebSocket配置将在其他配置文件中处理
        log.info("WebSocket优化配置已加载");
    }
}

/**
 * WebSocket连接参数优化配置
 */
@Slf4j
@Configuration
class WebSocketTuningConfig {

    /**
     * WebSocket连接参数调优
     * 针对200人并发优化
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        ServerEndpointExporter exporter = new ServerEndpointExporter();

        // 设置系统属性进行调优
        System.setProperty("org.apache.tomcat.websocket.DEFAULT_BUFFER_SIZE", "8192"); // 8KB缓冲
        System.setProperty("org.apache.tomcat.websocket.DEFAULT_PROCESS_PERIOD", "10"); // 10ms处理周期
        System.setProperty("tomcat.websocket.executorCoreSize", "50"); // 核心线程数
        System.setProperty("tomcat.websocket.executorMaxSize", "200"); // 最大线程数

        log.info("WebSocket Tomcat参数已优化: bufferSize=8KB, 核心线程=50, 最大线程=200");

        return exporter;
    }
}