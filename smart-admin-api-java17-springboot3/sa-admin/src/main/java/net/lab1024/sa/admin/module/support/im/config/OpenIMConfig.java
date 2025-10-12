package net.lab1024.sa.admin.module.support.im.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.im.constant.IMConstant;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * OpenIM配置类
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "openim")
public class OpenIMConfig {

    /**
     * OpenIM API服务地址
     */
    private String apiUrl = IMConstant.DEFAULT_API_URL;

    /**
     * OpenIM WebSocket服务地址
     */
    private String wsUrl = IMConstant.DEFAULT_WS_URL;

    /**
     * 管理员用户ID
     */
    private String adminUserId = IMConstant.DEFAULT_ADMIN_USER_ID;

    /**
     * 管理员密钥
     */
    private String adminSecret;

    /**
     * 平台ID
     */
    private Integer platformId = IMConstant.DEFAULT_PLATFORM_ID;

    /**
     * Token有效期(秒)
     */
    private Integer tokenExpireSeconds = IMConstant.DEFAULT_TOKEN_EXPIRE;

    /**
     * API调用超时时间(秒)
     */
    private Integer apiTimeoutSeconds = IMConstant.DEFAULT_API_TIMEOUT;

    /**
     * API调用最大重试次数
     */
    private Integer retryMaxCount = IMConstant.DEFAULT_RETRY_MAX;

    /**
     * 是否启用IM功能
     */
    private Boolean enabled = true;

    /**
     * 是否自动创建群组
     */
    private Boolean autoCreateGroup = true;

    /**
     * 是否自动邀请成员
     */
    private Boolean autoInvite = true;

    /**
     * 群组最大成员数
     */
    private Integer groupMaxMembers = IMConstant.DEFAULT_GROUP_MAX_MEMBERS;

    /**
     * 群组名称模板
     */
    private String groupNameTemplate = IMConstant.DEFAULT_GROUP_NAME_TEMPLATE;

    /**
     * 用户批量同步每批数量
     */
    private Integer userSyncBatchSize = IMConstant.DEFAULT_USER_SYNC_BATCH_SIZE;

    /**
     * 群组邀请每批数量
     */
    private Integer groupInviteBatchSize = IMConstant.DEFAULT_GROUP_INVITE_BATCH_SIZE;

    /**
     * 熔断器失败阈值
     */
    private Integer circuitBreakerThreshold = IMConstant.DEFAULT_CIRCUIT_BREAKER_THRESHOLD;

    /**
     * 熔断器超时时间(秒)
     */
    private Integer circuitBreakerTimeoutSeconds = IMConstant.DEFAULT_CIRCUIT_BREAKER_TIMEOUT;

    /**
     * 创建RestTemplate Bean用于OpenIM API调用
     */
    @Bean(name = "openimRestTemplate")
    public RestTemplate openimRestTemplate() {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());
        log.info("✅ OpenIM RestTemplate初始化完成 - API地址: {}", apiUrl);
        return restTemplate;
    }

    /**
     * 配置HTTP请求工厂
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 连接超时时间(毫秒)
        factory.setConnectTimeout(3000);
        // 读取超时时间(毫秒)
        factory.setReadTimeout(apiTimeoutSeconds * 1000);
        return factory;
    }

    /**
     * 获取完整的API URL
     */
    public String getFullApiUrl(String endpoint) {
        if (apiUrl.endsWith("/")) {
            return apiUrl + endpoint.substring(1);
        }
        return apiUrl + endpoint;
    }

    /**
     * 校验配置是否完整
     */
    public boolean isConfigValid() {
        if (!enabled) {
            log.warn("⚠️ OpenIM功能未启用");
            return false;
        }
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            log.error("❌ OpenIM API地址未配置");
            return false;
        }
        if (adminUserId == null || adminUserId.trim().isEmpty()) {
            log.error("❌ OpenIM管理员用户ID未配置");
            return false;
        }
        return true;
    }
}
