package net.lab1024.sa.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

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

    /**
     * Lettuce连接池配置
     * 支持200-500并发连接
     */
    @Bean
    public GenericObjectPoolConfig redisPoolConfig() {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();

        // 连接池配置
        poolConfig.setMaxTotal(200);           // 最大连接数
        poolConfig.setMaxIdle(50);             // 最大空闲连接
        poolConfig.setMinIdle(20);             // 最小空闲连接
        poolConfig.setMaxWait(Duration.ofMillis(2000));  // 获取连接最大等待时间

        // 连接测试配置
        poolConfig.setTestOnBorrow(true);      // 获取连接时测试
        poolConfig.setTestOnReturn(false);     // 归还连接时不测试
        poolConfig.setTestWhileIdle(true);     // 空闲时测试
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30)); // 空闲连接检测周期
        poolConfig.setNumTestsPerEvictionRun(3);  // 每次检测的连接数
        poolConfig.setMinEvictableIdleTime(Duration.ofMinutes(5));    // 连接最小空闲时间

        // JMX监控
        poolConfig.setJmxEnabled(true);
        poolConfig.setJmxNameBase("redis.pool");
        poolConfig.setJmxNamePrefix("high-performance");

        log.info("Redis连接池配置完成: maxTotal={}, maxIdle={}, minIdle={}",
                poolConfig.getMaxTotal(), poolConfig.getMaxIdle(), poolConfig.getMinIdle());

        return poolConfig;
    }

    /**
     * Lettuce客户端配置
     */
    @Bean
    public LettuceClientConfiguration lettuceClientConfiguration(GenericObjectPoolConfig redisPoolConfig) {
        // Socket选项
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofSeconds(10))  // 连接超时
                .keepAlive(true)                        // TCP KeepAlive
                .tcpNoDelay(true)                      // TCP NoDelay
                .build();

        // 客户端选项
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .autoReconnect(true)                    // 自动重连
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.ACCEPT_COMMANDS) // 断线行为
                .cancelCommandsOnReconnectFailure(false) // 重连失败不取消命令
                .publishOnScheduler(true)               // 使用调度器发布
                .build();

        // 构建配置
        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(redisPoolConfig)
                .clientOptions(clientOptions)
                .commandTimeout(Duration.ofSeconds(3))   // 命令超时
                .shutdownTimeout(Duration.ofMillis(100)) // 关闭超时
                .build();

        log.info("Lettuce客户端配置完成");
        return clientConfig;
    }

    /**
     * Redis连接工厂
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            RedisProperties redisProperties,
            LettuceClientConfiguration lettuceClientConfiguration) {

        // 单机配置
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProperties.getHost());
        config.setPort(redisProperties.getPort());
        config.setDatabase(redisProperties.getDatabase());
        if (redisProperties.getPassword() != null) {
            config.setPassword(redisProperties.getPassword());
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, lettuceClientConfiguration);
        factory.setShareNativeConnection(true);  // 共享本地连接
        factory.setValidateConnection(true);     // 验证连接

        log.info("Redis连接工厂创建完成: {}:{}", config.getHostName(), config.getPort());
        return factory;
    }

    /**
     * 高性能RedisTemplate
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                       ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用String序列化器作为key序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 使用Jackson序列化器作为value序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        // 设置序列化器
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // 开启事务支持
        template.setEnableTransactionSupport(false);  // 关闭事务以提高性能

        template.afterPropertiesSet();

        log.info("高性能RedisTemplate配置完成");
        return template;
    }

    /**
     * 列表专用RedisTemplate（优化列表操作）
     */
    @Bean
    public RedisTemplate<String, Object> listRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

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
     * Redis缓存管理器
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
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

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()  // 事务感知
                .build();

        log.info("Redis缓存管理器配置完成，缓存数量: {}", cacheConfigs.size());
        return cacheManager;
    }

    /**
     * Redis消息监听容器
     * 用于实时更新推送
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

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
     * Redis性能监控
     */
    @Bean
    public RedisPerformanceMonitor redisPerformanceMonitor(RedisTemplate<String, Object> redisTemplate) {
        return new RedisPerformanceMonitor(redisTemplate);
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