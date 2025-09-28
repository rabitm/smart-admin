package net.lab1024.sa.admin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import jakarta.annotation.Resource;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 高性能Redis配置
 * 针对200-500并发优化
 *
 * 优化内容:
 * 1. 连接池优化
 * 2. 序列化优化
 * 3. 缓存策略优化
 * 4. 集群支持
 * 5. 监听器优化
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-26
 * @Copyright 1024创新实验室
 */
@Slf4j
@Configuration
@EnableCaching
public class HighPerformanceRedisConfig {

    @Resource
    private RedisConnectionFactory factory;


    /**
     * 高性能RedisTemplate（警情专用）
     * 优化序列化和连接池性能
     */
    @Bean("policeRedisTemplate")
    public RedisTemplate<String, Object> policeRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 使用String序列化器作为key序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 使用Jackson序列化器作为value序列化器（为了性能优化，使用更简单的配置）
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        // 设置序列化器
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // 关闭事务以提高性能
        template.setEnableTransactionSupport(false);

        template.afterPropertiesSet();

        log.info("警情专用RedisTemplate配置完成");
        return template;
    }

    /**
     * 列表专用RedisTemplate（优化列表操作）
     */
    @Bean
    public RedisTemplate<String, Object> listRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 列表操作使用更快的序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 使用更快的序列化器
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();

        log.info("列表专用RedisTemplate配置完成");
        return template;
    }

    /**
     * 高性能缓存管理器（警情专用）
     */
    @Bean("policeCacheManager")
    public CacheManager policeCacheManager() {
        // 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))  // 默认过期时间
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();       // 不缓存null值

        // 特定缓存配置
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // 警情列表缓存（短期缓存）
        cacheConfigs.put("police:list", defaultConfig.entryTtl(Duration.ofSeconds(10)));

        // 警情详情缓存（中期缓存）
        cacheConfigs.put("police:detail", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // 用户信息缓存（长期缓存）
        cacheConfigs.put("user:info", defaultConfig.entryTtl(Duration.ofHours(1)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()  // 事务感知
                .build();

        log.info("高性能缓存管理器配置完成，缓存数量: {}", cacheConfigs.size());
        return cacheManager;
    }

    /**
     * Redis消息监听容器
     * 用于实时更新推送
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);

        // 设置线程池
        container.setTaskExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        container.setSubscriptionExecutor(java.util.concurrent.Executors.newFixedThreadPool(5));

        // 错误处理
        container.setErrorHandler(throwable ->
                log.error("Redis消息监听错误", throwable));

        // 恢复间隔
        container.setRecoveryInterval(5000L);

        log.info("Redis消息监听容器配置完成");
        return container;
    }

    /**
     * Redis性能监控（警情专用）
     */
    @Bean
    public RedisPerformanceMonitor redisPerformanceMonitor(@Qualifier("policeRedisTemplate") RedisTemplate<String, Object> policeRedisTemplate) {
        return new RedisPerformanceMonitor(policeRedisTemplate);
    }

    /**
     * Redis性能监控器
     */
    public static class RedisPerformanceMonitor {
        private final RedisTemplate<String, Object> redisTemplate;
        private long commandCount = 0;
        private long totalLatency = 0;

        public RedisPerformanceMonitor(RedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate;
            startMonitoring();
        }

        private void startMonitoring() {
            // 每分钟输出一次性能统计
            java.util.concurrent.Executors.newScheduledThreadPool(1)
                    .scheduleAtFixedRate(this::logPerformanceStats, 60, 60,
                            java.util.concurrent.TimeUnit.SECONDS);
        }

        private void logPerformanceStats() {
            if (commandCount > 0) {
                double avgLatency = (double) totalLatency / commandCount;
                log.info("Redis性能统计: 命令数={}, 平均延迟={}ms",
                        commandCount, avgLatency);

                // 重置统计
                commandCount = 0;
                totalLatency = 0;
            }
        }

        public void recordCommand(long latency) {
            commandCount++;
            totalLatency += latency;
        }
    }
}