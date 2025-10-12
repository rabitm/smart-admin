package net.lab1024.sa.admin.module.support.im.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.im.config.OpenIMConfig;
import net.lab1024.sa.admin.module.support.im.constant.IMConstant;
import net.lab1024.sa.admin.module.support.im.constant.IMErrorCodeEnum;
import net.lab1024.sa.admin.module.support.im.constant.IMOperationTypeEnum;
import net.lab1024.sa.base.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OpenIM HTTP客户端
 * 封装所有与OpenIM服务器的HTTP通信
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Component
public class OpenIMClient {

    @Resource
    private OpenIMConfig openIMConfig;

    @Resource
    @Qualifier("openimRestTemplate")
    private RestTemplate restTemplate;

    @Resource
    private OpenIMTokenManager tokenManager;

    // 熔断器相关
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong circuitBreakerOpenTime = new AtomicLong(0);
    private volatile boolean circuitBreakerOpen = false;

    /**
     * 发送POST请求(带认证)
     */
    public <T> T post(String endpoint, Object requestBody, Class<T> responseType, IMOperationTypeEnum operationType) {
        return post(endpoint, requestBody, responseType, operationType, true);
    }

    /**
     * 发送POST请求
     *
     * @param endpoint 端点路径
     * @param requestBody 请求体
     * @param responseType 响应类型
     * @param operationType 操作类型
     * @param needAuth 是否需要认证
     */
    public <T> T post(String endpoint, Object requestBody, Class<T> responseType,
                      IMOperationTypeEnum operationType, boolean needAuth) {
        // 检查熔断器状态
        checkCircuitBreaker();

        // 配置检查
        if (!openIMConfig.isConfigValid()) {
            throw new BusinessException(IMErrorCodeEnum.IM_CONFIG_ERROR);
        }

        String url = openIMConfig.getFullApiUrl(endpoint);
        long startTime = System.currentTimeMillis();

        try {
            // 准备请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 添加Token认证
            if (needAuth) {
                String token = tokenManager.getToken();
                headers.set("token", token);
                headers.set("operationID", generateOperationId());
            }

            // 创建请求实体
            HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(requestBody), headers);

            log.info("📤 [OpenIM请求] {} - URL: {}, Body: {}", operationType.getDescription(), url, JSON.toJSONString(requestBody));

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("📥 [OpenIM响应] {} - 耗时: {}ms, Status: {}, Body: {}",
                    operationType.getDescription(), executionTime, response.getStatusCode().value(), response.getBody());

            // 解析响应
            JSONObject jsonResponse = JSON.parseObject(response.getBody());
            Integer errCode = jsonResponse.getInteger("errCode");

            if (errCode == null || errCode != 0) {
                String errMsg = jsonResponse.getString("errMsg");

                // 特殊处理: 用户已注册不视为错误
                if (errCode == 1102 && "RegisteredAlreadyError".equals(errMsg)) {
                    log.info("ℹ️ [OpenIM] {} - 用户已存在,跳过注册", operationType.getDescription());
                    resetCircuitBreaker();
                    // 返回空对象表示成功但无需处理
                    return null;
                }

                log.error("❌ [OpenIM错误] {} - errCode: {}, errMsg: {}", operationType.getDescription(), errCode, errMsg);
                handleFailure();
                throw new BusinessException(errMsg);
            }

            // 重置失败计数
            resetCircuitBreaker();

            // 返回data字段
            String dataStr = jsonResponse.getString("data");
            if (dataStr == null || dataStr.isEmpty() || "null".equals(dataStr)) {
                return null;
            }

            return JSON.parseObject(dataStr, responseType);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("❌ [OpenIM HTTP错误] {} - 耗时: {}ms, Status: {}, Body: {}",
                    operationType.getDescription(), executionTime, e.getStatusCode(), e.getResponseBodyAsString(), e);
            handleFailure();
            throw new BusinessException("HTTP错误: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());

        } catch (ResourceAccessException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("❌ [OpenIM连接超时] {} - 耗时: {}ms", operationType.getDescription(), executionTime, e);
            handleFailure();
            throw new BusinessException("连接超时: " + e.getMessage());

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("❌ [OpenIM未知错误] {} - 耗时: {}ms", operationType.getDescription(), executionTime, e);
            handleFailure();
            throw new BusinessException(e.getMessage());
        }
    }

    /**
     * 带重试的POST请求
     */
    public <T> T postWithRetry(String endpoint, Object requestBody, Class<T> responseType, IMOperationTypeEnum operationType) {
        int maxRetries = openIMConfig.getRetryMaxCount();
        int attempt = 0;

        while (attempt <= maxRetries) {
            try {
                return post(endpoint, requestBody, responseType, operationType);
            } catch (BusinessException e) {
                attempt++;
                if (attempt > maxRetries) {
                    log.error("❌ [OpenIM重试失败] {} - 已达到最大重试次数: {}", operationType.getDescription(), maxRetries);
                    throw e;
                }

                // 指数退避
                long backoffTime = (long) Math.pow(2, attempt - 1) * 1000;
                log.warn("⚠️ [OpenIM重试] {} - 第{}次重试, 等待{}ms", operationType.getDescription(), attempt, backoffTime);

                try {
                    Thread.sleep(backoffTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }

        throw new BusinessException(IMErrorCodeEnum.IM_API_CALL_FAILED);
    }

    /**
     * 检查熔断器状态
     */
    private void checkCircuitBreaker() {
        if (circuitBreakerOpen) {
            long openTime = circuitBreakerOpenTime.get();
            long timeout = openIMConfig.getCircuitBreakerTimeoutSeconds() * 1000L;

            if (System.currentTimeMillis() - openTime > timeout) {
                // 熔断器超时,尝试关闭
                log.info("🔄 [熔断器] 超时时间已过,尝试关闭熔断器");
                circuitBreakerOpen = false;
                consecutiveFailures.set(0);
            } else {
                log.warn("⚡ [熔断器] 熔断器开启中,拒绝请求");
                throw new BusinessException(IMErrorCodeEnum.IM_CIRCUIT_BREAKER_OPEN);
            }
        }
    }

    /**
     * 处理失败情况
     */
    private void handleFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        int threshold = openIMConfig.getCircuitBreakerThreshold();

        if (failures >= threshold && !circuitBreakerOpen) {
            circuitBreakerOpen = true;
            circuitBreakerOpenTime.set(System.currentTimeMillis());
            log.error("🔥 [熔断器] 连续失败{}次,达到阈值{},开启熔断器", failures, threshold);
        }
    }

    /**
     * 重置熔断器
     */
    private void resetCircuitBreaker() {
        if (consecutiveFailures.get() > 0) {
            log.info("✅ [熔断器] 请求成功,重置失败计数");
            consecutiveFailures.set(0);
        }
        if (circuitBreakerOpen) {
            circuitBreakerOpen = false;
            log.info("✅ [熔断器] 关闭熔断器");
        }
    }

    /**
     * 生成操作ID
     */
    private String generateOperationId() {
        return "OP_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }

    /**
     * 构建请求体
     */
    public Map<String, Object> buildRequestBody() {
        return new HashMap<>();
    }

    /**
     * 构建请求体并添加字段
     */
    public Map<String, Object> buildRequestBody(String key, Object value) {
        Map<String, Object> body = new HashMap<>();
        body.put(key, value);
        return body;
    }
}
