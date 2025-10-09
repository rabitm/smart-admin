package net.lab1024.sa.admin.module.business.im.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.im.config.OpenIMProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OpenIM健康检查服务
 *
 * 功能：
 * 1. 定期检查OpenIM服务器连接状态
 * 2. 熔断器机制，连续失败时自动熔断
 * 3. 健康状态缓存，供其他服务查询
 * 4. 自动恢复检测
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-09
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenIMHealthCheckService {

    private final OpenIMProperties openIMProperties;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    // 健康状态标志
    private final AtomicBoolean isHealthy = new AtomicBoolean(true);

    // 连续失败计数
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    // 熔断器状态
    private final AtomicBoolean circuitBreakerOpen = new AtomicBoolean(false);

    // 最后检查时间
    private volatile LocalDateTime lastCheckTime;

    // 最后成功时间
    private volatile LocalDateTime lastSuccessTime;

    // Redis缓存键
    private static final String HEALTH_STATUS_KEY = "openim:health:status";
    private static final String CIRCUIT_BREAKER_KEY = "openim:circuit:breaker";

    // 配置参数
    private static final int MAX_CONSECUTIVE_FAILURES = 3; // 连续失败3次后熔断
    private static final long CIRCUIT_BREAKER_TIMEOUT_MS = 30000; // 熔断30秒后尝试恢复
    private static final long HEALTH_CHECK_TIMEOUT_MS = 5000; // 健康检查超时5秒

    @PostConstruct
    public void init() {
        log.info("🏥 [健康检查] OpenIM健康检查服务已启动");
        // 立即执行一次健康检查
        checkHealth();
    }

    /**
     * 定期健康检查（每30秒执行一次）
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void scheduledHealthCheck() {
        checkHealth();
    }

    /**
     * 执行健康检查
     */
    public boolean checkHealth() {
        lastCheckTime = LocalDateTime.now();

        // 如果熔断器开启，检查是否可以尝试恢复
        if (circuitBreakerOpen.get()) {
            if (shouldAttemptRecovery()) {
                log.info("🔄 [健康检查] 熔断器超时，尝试恢复检查");
                circuitBreakerOpen.set(false);
                consecutiveFailures.set(0);
            } else {
                log.warn("⚡ [健康检查] 熔断器开启中，跳过健康检查");
                return false;
            }
        }

        try {
            // 尝试获取管理员Token作为健康检查
            String adminToken = getAdminTokenForHealthCheck();

            if (adminToken != null && !adminToken.isEmpty()) {
                // 健康检查成功
                onHealthCheckSuccess();
                return true;
            } else {
                // 健康检查失败
                onHealthCheckFailure(new Exception("获取管理员Token失败"));
                return false;
            }

        } catch (Exception e) {
            onHealthCheckFailure(e);
            return false;
        }
    }

    /**
     * 健康检查成功处理
     */
    private void onHealthCheckSuccess() {
        boolean wasUnhealthy = !isHealthy.get();

        isHealthy.set(true);
        lastSuccessTime = LocalDateTime.now();
        consecutiveFailures.set(0);

        // 更新Redis缓存
        redisTemplate.opsForValue().set(HEALTH_STATUS_KEY, "healthy", 60, TimeUnit.SECONDS);
        redisTemplate.delete(CIRCUIT_BREAKER_KEY);

        if (wasUnhealthy) {
            log.info("✅ [健康检查] OpenIM服务恢复正常");
        } else {
            log.debug("✅ [健康检查] OpenIM服务健康");
        }
    }

    /**
     * 健康检查失败处理
     */
    private void onHealthCheckFailure(Exception e) {
        isHealthy.set(false);
        int failures = consecutiveFailures.incrementAndGet();

        log.error("❌ [健康检查] OpenIM服务健康检查失败 (连续失败: {}次)", failures, e);

        // 更新Redis缓存
        redisTemplate.opsForValue().set(HEALTH_STATUS_KEY, "unhealthy", 60, TimeUnit.SECONDS);

        // 达到阈值，开启熔断器
        if (failures >= MAX_CONSECUTIVE_FAILURES && !circuitBreakerOpen.get()) {
            openCircuitBreaker();
        }
    }

    /**
     * 开启熔断器
     */
    private void openCircuitBreaker() {
        circuitBreakerOpen.set(true);
        redisTemplate.opsForValue().set(CIRCUIT_BREAKER_KEY, System.currentTimeMillis(),
                CIRCUIT_BREAKER_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        log.error("⚡ [熔断器] OpenIM服务熔断器已开启，{}秒后尝试恢复",
                CIRCUIT_BREAKER_TIMEOUT_MS / 1000);
    }

    /**
     * 判断是否应该尝试恢复
     */
    private boolean shouldAttemptRecovery() {
        Object breakerTime = redisTemplate.opsForValue().get(CIRCUIT_BREAKER_KEY);
        if (breakerTime == null) {
            return true;
        }

        long openTime = ((Number) breakerTime).longValue();
        long elapsed = System.currentTimeMillis() - openTime;

        return elapsed >= CIRCUIT_BREAKER_TIMEOUT_MS;
    }

    /**
     * 获取管理员Token用于健康检查
     */
    private String getAdminTokenForHealthCheck() {
        try {
            String url = openIMProperties.getApiUrl() + "/auth/get_admin_token";

            Map<String, String> request = new HashMap<>();
            request.put("secret", openIMProperties.getAdmin().getSecret());
            request.put("userID", openIMProperties.getAdmin().getUserId());  // OpenIM需要userID

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("operationID", java.util.UUID.randomUUID().toString());  // OpenIM需要operationID头部

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            // 使用短超时时间
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body != null && body.get("errCode") != null) {
                int errCode = ((Number) body.get("errCode")).intValue();
                if (errCode == 0) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    if (data != null) {
                        return (String) data.get("token");
                    }
                }
            }

            return null;

        } catch (Exception e) {
            log.debug("🔍 [健康检查] 获取管理员Token异常: {}", e.getMessage());
            throw new RuntimeException("健康检查失败", e);
        }
    }

    /**
     * 获取当前健康状态
     */
    public boolean isHealthy() {
        return isHealthy.get();
    }

    /**
     * 获取熔断器状态
     */
    public boolean isCircuitBreakerOpen() {
        return circuitBreakerOpen.get();
    }

    /**
     * 获取连续失败次数
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    /**
     * 获取最后检查时间
     */
    public LocalDateTime getLastCheckTime() {
        return lastCheckTime;
    }

    /**
     * 获取最后成功时间
     */
    public LocalDateTime getLastSuccessTime() {
        return lastSuccessTime;
    }

    /**
     * 获取健康状态报告
     */
    public Map<String, Object> getHealthReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("healthy", isHealthy.get());
        report.put("circuitBreakerOpen", circuitBreakerOpen.get());
        report.put("consecutiveFailures", consecutiveFailures.get());
        report.put("lastCheckTime", lastCheckTime);
        report.put("lastSuccessTime", lastSuccessTime);
        report.put("openimApiUrl", openIMProperties.getApiUrl());

        return report;
    }
}
