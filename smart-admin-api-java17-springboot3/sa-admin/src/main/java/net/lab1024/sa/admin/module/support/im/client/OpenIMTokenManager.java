package net.lab1024.sa.admin.module.support.im.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.im.config.OpenIMConfig;
import net.lab1024.sa.admin.module.support.im.constant.IMConstant;
import net.lab1024.sa.admin.module.support.im.constant.IMErrorCodeEnum;
import net.lab1024.sa.base.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * OpenIM Token管理器
 * 负责获取、缓存和刷新OpenIM管理员Token
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Component
public class OpenIMTokenManager {

    @Resource
    private OpenIMConfig openIMConfig;

    @Resource
    @Qualifier("openimRestTemplate")
    private RestTemplate restTemplate;

    /**
     * Token缓存
     */
    private volatile TokenCache tokenCache;

    /**
     * 读写锁
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    /**
     * Token缓存内部类
     */
    @Data
    private static class TokenCache {
        private String token;
        private Long expireTime;
        private Long createTime;

        public boolean isExpired() {
            if (expireTime == null) {
                return true;
            }
            // 提前5分钟刷新
            long refreshBuffer = 5 * 60 * 1000;
            return System.currentTimeMillis() > (expireTime - refreshBuffer);
        }
    }

    /**
     * 获取Token(带缓存)
     */
    public String getToken() {
        // 先尝试读锁
        readLock.lock();
        try {
            if (tokenCache != null && !tokenCache.isExpired()) {
                log.debug("📋 [Token] 使用缓存的Token, 剩余有效期: {}秒",
                        (tokenCache.getExpireTime() - System.currentTimeMillis()) / 1000);
                return tokenCache.getToken();
            }
        } finally {
            readLock.unlock();
        }

        // Token过期或不存在,获取写锁
        writeLock.lock();
        try {
            // 双重检查
            if (tokenCache != null && !tokenCache.isExpired()) {
                return tokenCache.getToken();
            }

            // 获取新Token
            log.info("🔑 [Token] Token不存在或已过期,重新获取");
            return refreshToken();

        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 刷新Token
     */
    public String refreshToken() {
        writeLock.lock();
        try {
            String url = openIMConfig.getFullApiUrl(IMConstant.API_ADMIN_TOKEN);

            // 构建请求体 (根据 OpenIM v3.x 官方文档)
            // 文档: https://docs.openim.io/restapi/apis/authenticationmanagement/getadmintoken
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userID", openIMConfig.getAdminUserId());
            requestBody.put("secret", openIMConfig.getAdminSecret());

            // 准备请求头 (OpenIM v3.x 只需要 operationID)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("operationID", "ADMIN_TOKEN_" + System.currentTimeMillis());

            HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(requestBody), headers);

            log.info("📤 [Token请求] URL: {}, UserID: {}",
                    url, openIMConfig.getAdminUserId());

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            // 解析响应
            JSONObject jsonResponse = JSON.parseObject(response.getBody());
            Integer errCode = jsonResponse.getInteger("errCode");

            if (errCode == null || errCode != 0) {
                String errMsg = jsonResponse.getString("errMsg");
                log.error("❌ [Token错误] errCode: {}, errMsg: {}", errCode, errMsg);
                throw new BusinessException(errMsg);
            }

            // 提取token
            JSONObject data = jsonResponse.getJSONObject("data");
            String token = data.getString("token");
            Integer expireTime = data.getInteger("expireTimeSeconds");

            if (token == null || token.isEmpty()) {
                log.error("❌ [Token错误] 响应中未包含token字段");
                throw new BusinessException(IMErrorCodeEnum.IM_TOKEN_EXPIRED);
            }

            // 缓存Token
            TokenCache newCache = new TokenCache();
            newCache.setToken(token);
            newCache.setCreateTime(System.currentTimeMillis());

            // 计算过期时间
            if (expireTime != null && expireTime > 0) {
                newCache.setExpireTime(System.currentTimeMillis() + (expireTime * 1000L));
            } else {
                // 使用配置的默认过期时间
                newCache.setExpireTime(System.currentTimeMillis() +
                        (openIMConfig.getTokenExpireSeconds() * 1000L));
            }

            tokenCache = newCache;

            log.info("✅ [Token获取成功] Token: {}..., 有效期: {}秒",
                    token.substring(0, Math.min(20, token.length())),
                    (tokenCache.getExpireTime() - System.currentTimeMillis()) / 1000);

            return token;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ [Token获取失败]", e);
            throw new BusinessException("Token获取失败: " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 清除Token缓存
     */
    public void clearToken() {
        writeLock.lock();
        try {
            tokenCache = null;
            log.info("🗑️ [Token] Token缓存已清除");
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 获取Token缓存信息(用于监控)
     */
    public Map<String, Object> getTokenCacheInfo() {
        readLock.lock();
        try {
            Map<String, Object> info = new HashMap<>();
            if (tokenCache != null) {
                info.put("exists", true);
                info.put("createTime", tokenCache.getCreateTime());
                info.put("expireTime", tokenCache.getExpireTime());
                info.put("remainingSeconds", (tokenCache.getExpireTime() - System.currentTimeMillis()) / 1000);
                info.put("isExpired", tokenCache.isExpired());
            } else {
                info.put("exists", false);
            }
            return info;
        } finally {
            readLock.unlock();
        }
    }
}
