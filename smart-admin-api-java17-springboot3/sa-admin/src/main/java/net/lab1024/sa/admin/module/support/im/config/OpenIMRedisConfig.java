package net.lab1024.sa.admin.module.support.im.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * OpenIM 专用 Redis 配置
 * 用于存储 OpenIM Token,确保与 OpenIM Server 使用相同的 Redis 实例
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-10
 * @Copyright 1024创新实验室
 */
@Slf4j
@Configuration
public class OpenIMRedisConfig {

    @Value("${openim.redis.host:localhost}")
    private String host;

    @Value("${openim.redis.port:6379}")
    private Integer port;

    @Value("${openim.redis.password:openIM123}")
    private String password;

    @Value("${openim.redis.database:0}")
    private Integer database;

    @Value("${openim.redis.timeout:10000}")
    private Long timeout;

    /**
     * OpenIM 专用 Redis 连接工厂
     */
    @Bean("openimRedisConnectionFactory")
    public RedisConnectionFactory openimRedisConnectionFactory() {
        log.info("📡 [OpenIM Redis] 初始化连接工厂: {}:{}, database: {}", host, port, database);

        // Redis 单机配置
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(host);
        redisConfig.setPort(port);
        redisConfig.setDatabase(database);
        redisConfig.setPassword(password);

        // Lettuce 客户端资源配置
        ClientResources clientResources = DefaultClientResources.create();

        // Lettuce 客户端配置
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(ClientOptions.builder()
                        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                        .autoReconnect(true)
                        .build())
                .clientResources(clientResources)
                .commandTimeout(Duration.ofMillis(timeout))
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
        factory.afterPropertiesSet();

        log.info("✅ [OpenIM Redis] 连接工厂初始化成功");

        return factory;
    }

    /**
     * OpenIM 专用 RedisTemplate
     * 用于存储和管理 OpenIM Token
     */
    @Bean("openimRedisTemplate")
    public RedisTemplate<String, String> openimRedisTemplate(
            RedisConnectionFactory openimRedisConnectionFactory) {

        log.info("📋 [OpenIM Redis] 初始化 RedisTemplate");

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(openimRedisConnectionFactory);

        // 使用 String 序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 设置 key 序列化器
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 设置 value 序列化器
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();

        log.info("✅ [OpenIM Redis] RedisTemplate 初始化成功");

        return template;
    }
}
