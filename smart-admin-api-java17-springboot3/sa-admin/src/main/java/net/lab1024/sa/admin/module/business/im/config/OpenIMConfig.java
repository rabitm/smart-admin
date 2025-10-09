package net.lab1024.sa.admin.module.business.im.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * OpenIM 配置类
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "openim", name = "api-url")
public class OpenIMConfig {

    private final OpenIMProperties openIMProperties;

    /**
     * 配置RestTemplate用于调用OpenIM API
     */
    @Bean(name = "openIMRestTemplate")
    public RestTemplate openIMRestTemplate() {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());

        log.info("📱 [OpenIM配置] RestTemplate已初始化");
        log.info("📱 [OpenIM配置] API地址: {}", openIMProperties.getApiUrl());
        log.info("📱 [OpenIM配置] WebSocket地址: {}", openIMProperties.getWsUrl());
        log.info("📱 [OpenIM配置] 平台ID: {}", openIMProperties.getPlatformId());
        log.info("📱 [OpenIM配置] 自动创建群组: {}", openIMProperties.getGroup().getAutoCreate());

        return restTemplate;
    }

    /**
     * 配置HTTP请求工厂
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 连接超时 10秒
        factory.setReadTimeout(30000);     // 读取超时 30秒
        return factory;
    }
}
