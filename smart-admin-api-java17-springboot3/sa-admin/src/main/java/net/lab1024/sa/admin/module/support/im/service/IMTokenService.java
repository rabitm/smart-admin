package net.lab1024.sa.admin.module.support.im.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.im.client.OpenIMClient;
import net.lab1024.sa.admin.module.support.im.config.OpenIMConfig;
import net.lab1024.sa.admin.module.support.im.constant.IMConstant;
import net.lab1024.sa.admin.module.support.im.constant.IMErrorCodeEnum;
import net.lab1024.sa.admin.module.support.im.constant.IMOperationTypeEnum;
import net.lab1024.sa.admin.module.support.im.dao.IMUserMappingDao;
import net.lab1024.sa.admin.module.support.im.domain.entity.IMUserMappingEntity;
import net.lab1024.sa.admin.module.support.im.domain.vo.IMTokenVO;
import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.base.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * IM Token 生成服务
 * 核心职责：为前端用户生成 OpenIM Token
 *
 * v2.0 优化:
 * - 双层缓存 (Caffeine + Redis) 提升性能
 * - 自动刷新机制避免 Token 过期
 * - 完整的错误处理和日志记录
 *
 * 流程:
 * 1. 检查本地缓存 (Caffeine) - 亚毫秒级
 * 2. 检查 Redis 缓存 - 毫秒级
 * 3. 检查用户映射，如果不存在则先注册 OpenIM 用户
 * 4. 调用 OpenIM API 生成新 Token
 * 5. 双层缓存 Token
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-10
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service
public class IMTokenService {

    @Resource
    private OpenIMClient openIMClient;

    @Resource
    private OpenIMConfig openIMConfig;

    @Resource
    private IMUserMappingDao imUserMappingDao;

    @Resource
    private EmployeeDao employeeDao;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    @Qualifier("openimRedisTemplate")
    private RedisTemplate<String, String> openimRedisTemplate;

    /**
     * 本地缓存 (Caffeine)
     * 提供亚毫秒级访问速度
     * 设置:
     * - 最大容量 10000 个 Token
     * - 过期时间 50 分钟 (提前 10 分钟过期，确保自动刷新)
     */
    private Cache<Long, IMTokenVO> localTokenCache;

    /**
     * 初始化本地缓存
     */
    @PostConstruct
    public void initLocalCache() {
        localTokenCache = Caffeine.newBuilder()
                .maximumSize(10000)  // 最多缓存 10000 个用户的 Token
                .expireAfterWrite(50, TimeUnit.MINUTES)  // 写入后 50 分钟过期
                .recordStats()  // 记录缓存统计信息
                .build();

        log.info("✅ [Token缓存] 本地缓存初始化完成, 最大容量: 10000, 过期时间: 50分钟");
    }

    /**
     * 为员工生成 OpenIM Token
     * v2.0 优化: 双层缓存 (Caffeine + Redis)
     *
     * @param employeeId 员工ID
     * @return Token信息
     */
    public IMTokenVO generateToken(Long employeeId) {
        if (employeeId == null) {
            throw new BusinessException(IMErrorCodeEnum.EMPLOYEE_ID_REQUIRED);
        }

        log.info("🎫 [Token生成] 开始为员工{}生成Token", employeeId);

        // 1. 检查本地缓存 (Caffeine) - 亚毫秒级
        IMTokenVO cachedTokenVO = localTokenCache.getIfPresent(employeeId);
        if (cachedTokenVO != null && !isTokenNearExpiry(cachedTokenVO)) {
            log.info("✅ [Token缓存] 命中本地缓存, employeeId: {}", employeeId);
            return cachedTokenVO;
        }

        // 2. 获取或创建 OpenIM 用户映射
        String openimUserId = getOrCreateOpenIMUser(employeeId);

        // 3. 检查 Redis 缓存
        String cacheKey = buildTokenCacheKey(employeeId);
        String cachedToken = redisTemplate.opsForValue().get(cacheKey);

        if (cachedToken != null) {
            log.info("✅ [Token缓存] 命中Redis缓存, employeeId: {}", employeeId);

            // 获取缓存过期时间
            Long expireTime = getTokenExpireTime(cacheKey);

            IMTokenVO tokenVO = buildTokenVO(openimUserId, cachedToken, expireTime, employeeId);

            // 回写到本地缓存
            localTokenCache.put(employeeId, tokenVO);

            return tokenVO;
        }

        // 4. 调用 OpenIM API 生成 Token
        Object[] tokenResult = generateTokenFromOpenIM(openimUserId);
        String newToken = (String) tokenResult[0];
        Long expireTimeSeconds = (Long) tokenResult[1];

        // 5. 缓存 Token (提前 5 分钟过期，确保前端能及时刷新)
        long cacheExpireSeconds = expireTimeSeconds - 300;
        redisTemplate.opsForValue().set(cacheKey, newToken, cacheExpireSeconds, TimeUnit.SECONDS);

        // 使用OpenIM返回的实际过期时间
        long expireTime = System.currentTimeMillis() / 1000 + expireTimeSeconds;

        IMTokenVO tokenVO = buildTokenVO(openimUserId, newToken, expireTime, employeeId);

        // 6. 写入本地缓存
        localTokenCache.put(employeeId, tokenVO);

        log.info("✅ [Token生成] Token生成成功, employeeId: {}, expireTime: {}, 有效期: {}秒 ({}小时)",
                employeeId, expireTime, expireTimeSeconds, expireTimeSeconds / 3600);

        // 7. 输出缓存统计信息
        logCacheStats();

        return tokenVO;
    }

    /**
     * 检查 Token 是否临近过期
     * 如果距离过期时间少于 10 分钟,返回 true
     */
    private boolean isTokenNearExpiry(IMTokenVO tokenVO) {
        if (tokenVO == null || tokenVO.getExpireTime() == null) {
            return true;
        }

        long currentTime = System.currentTimeMillis() / 1000;
        long timeToExpire = tokenVO.getExpireTime() - currentTime;

        return timeToExpire < 600;  // 少于 10 分钟
    }

    /**
     * 输出缓存统计信息
     */
    private void logCacheStats() {
        if (localTokenCache != null) {
            var stats = localTokenCache.stats();
            log.debug("📊 [缓存统计] 命中率: {:.2f}%, 请求数: {}, 命中数: {}, 未命中数: {}, 加载成功数: {}, 加载失败数: {}",
                    stats.hitRate() * 100,
                    stats.requestCount(),
                    stats.hitCount(),
                    stats.missCount(),
                    stats.loadSuccessCount(),
                    stats.loadFailureCount());
        }
    }

    /**
     * 刷新 Token
     * v2.0 优化: 清除双层缓存
     *
     * @param employeeId 员工ID
     * @return 新Token信息
     */
    public IMTokenVO refreshToken(Long employeeId) {
        log.info("🔄 [Token刷新] 刷新员工{}的Token", employeeId);

        // 1. 清除本地缓存
        localTokenCache.invalidate(employeeId);

        // 2. 清除 Redis 缓存
        String cacheKey = buildTokenCacheKey(employeeId);
        redisTemplate.delete(cacheKey);

        // 3. 重新生成
        return generateToken(employeeId);
    }

    /**
     * 获取缓存统计信息
     * 用于监控和性能分析
     */
    public Map<String, Object> getCacheStats() {
        if (localTokenCache == null) {
            return Collections.emptyMap();
        }

        var stats = localTokenCache.stats();
        Map<String, Object> result = new HashMap<>();
        result.put("hitRate", stats.hitRate());
        result.put("requestCount", stats.requestCount());
        result.put("hitCount", stats.hitCount());
        result.put("missCount", stats.missCount());
        result.put("loadSuccessCount", stats.loadSuccessCount());
        result.put("loadFailureCount", stats.loadFailureCount());
        result.put("evictionCount", stats.evictionCount());
        result.put("estimatedSize", localTokenCache.estimatedSize());

        return result;
    }

    /**
     * 获取或创建 OpenIM 用户
     */
    private String getOrCreateOpenIMUser(Long employeeId) {
        // 1. 查询映射
        IMUserMappingEntity mapping = imUserMappingDao.selectByEmployeeId(employeeId);

        if (mapping != null && mapping.getSyncStatus() != null && mapping.getSyncStatus() == 1) {
            log.info("ℹ️ [用户映射] 用户已存在映射: employeeId={}, openimUserId={}",
                    employeeId, mapping.getOpenimUserId());

            // 确保用户在 OpenIM 中实际存在（防止数据不一致）
            // 如果用户不存在，registerOpenIMUser 会重新注册
            ensureUserExistsInOpenIM(employeeId, mapping.getOpenimUserId());

            return mapping.getOpenimUserId();
        }

        // 2. 不存在则注册
        return registerOpenIMUser(employeeId);
    }

    /**
     * 确保用户在 OpenIM 中存在
     * 如果不存在则尝试注册
     */
    private void ensureUserExistsInOpenIM(Long employeeId, String openimUserId) {
        try {
            // 查询员工信息
            EmployeeEntity employee = employeeDao.selectById(employeeId);
            if (employee == null) {
                log.warn("⚠️ [用户检查] 员工不存在: {}", employeeId);
                return;
            }

            // 构建用户信息
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userID", openimUserId);
            userInfo.put("nickname", employee.getActualName());
            userInfo.put("faceURL", employee.getAvatar() != null ? employee.getAvatar() : "");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("users", Collections.singletonList(userInfo));

            // 尝试注册（如果已存在会被忽略）
            openIMClient.post(
                IMConstant.API_USER_REGISTER,
                requestBody,
                JSONObject.class,
                IMOperationTypeEnum.USER_REGISTER
            );

            log.info("✅ [用户检查] 用户在OpenIM中已注册: {}", openimUserId);

        } catch (Exception e) {
            // 如果是用户已存在错误，说明用户正常，忽略即可
            if (e.getMessage() != null && e.getMessage().contains("RegisteredAlreadyError")) {
                log.debug("✅ [用户检查] 用户已存在于OpenIM: {}", openimUserId);
            } else {
                // 其他错误记录日志但不抛出，让后续的 Token 生成尝试去发现问题
                log.warn("⚠️ [用户检查] 检查用户失败: {}, 错误: {}", openimUserId, e.getMessage());
            }
        }
    }

    /**
     * 注册 OpenIM 用户
     */
    private String registerOpenIMUser(Long employeeId) {
        log.info("📝 [用户注册] 开始注册OpenIM用户, employeeId: {}", employeeId);

        // 1. 查询员工信息
        EmployeeEntity employee = employeeDao.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException("员工不存在: " + employeeId);
        }

        // 2. 生成 OpenIM 用户 ID (使用常量定义的前缀，优先使用employeeUid)
        String openimUserId;
        if (employee.getEmployeeUid() != null && !employee.getEmployeeUid().isEmpty()) {
            openimUserId = IMConstant.USER_ID_PREFIX + employee.getEmployeeUid();
        } else {
            openimUserId = IMConstant.USER_ID_PREFIX + employeeId;
        }

        // 3. 构建用户信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userID", openimUserId);
        userInfo.put("nickname", employee.getActualName());
        userInfo.put("faceURL", employee.getAvatar() != null ? employee.getAvatar() : "");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("users", Collections.singletonList(userInfo));

        // 4. 调用 OpenIM 注册 API
        try {
            openIMClient.post(
                IMConstant.API_USER_REGISTER,
                requestBody,
                JSONObject.class,
                IMOperationTypeEnum.USER_REGISTER
            );

            log.info("✅ [用户注册] OpenIM用户注册成功: {}", openimUserId);

        } catch (Exception e) {
            // 如果是用户已存在错误（1102），不抛出异常
            if (e.getMessage() != null && e.getMessage().contains("RegisteredAlreadyError")) {
                log.info("ℹ️ [用户注册] 用户已存在，跳过注册: {}", openimUserId);
            } else {
                log.error("❌ [用户注册] OpenIM用户注册失败", e);
                throw e;
            }
        }

        // 5. 保存映射
        saveUserMapping(employeeId, openimUserId, employee);

        return openimUserId;
    }

    /**
     * 保存用户映射
     */
    private void saveUserMapping(Long employeeId, String openimUserId, EmployeeEntity employee) {
        IMUserMappingEntity mapping = new IMUserMappingEntity();
        mapping.setEmployeeId(employeeId);
        mapping.setOpenimUserId(openimUserId);
        mapping.setSyncStatus(1);  // 1-已同步
        mapping.setSyncTime(LocalDateTime.now());
        mapping.setDeletedFlag(false);

        imUserMappingDao.insert(mapping);

        log.info("✅ [用户映射] 映射关系保存成功: employeeId={}, openimUserId={}",
                employeeId, openimUserId);
    }

    /**
     * 从 OpenIM 生成 Token
     *
     * @return [0]=token, [1]=expireTimeSeconds
     */
    private Object[] generateTokenFromOpenIM(String openimUserId) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userID", openimUserId);
        requestBody.put("platformID", IMConstant.DEFAULT_PLATFORM_ID);  // 5 = Web

        JSONObject response = openIMClient.post(
            IMConstant.API_USER_TOKEN,
            requestBody,
            JSONObject.class,
            IMOperationTypeEnum.TOKEN_GENERATE
        );

        if (response == null) {
            throw new BusinessException("生成Token失败: OpenIM返回空响应");
        }

        String token = response.getString("token");
        if (token == null || token.isEmpty()) {
            throw new BusinessException("生成Token失败: Token为空");
        }

        // 提取过期时间(秒)
        Long expireTimeSeconds = response.getLong("expireTimeSeconds");
        if (expireTimeSeconds == null || expireTimeSeconds <= 0) {
            log.warn("⚠️ [Token生成] OpenIM未返回有效的过期时间，使用配置默认值: {}秒", openIMConfig.getTokenExpireSeconds());
            expireTimeSeconds = openIMConfig.getTokenExpireSeconds().longValue();
        }

        // ✅ 关键修复: 将 Token 存储到 OpenIM Redis 中
        // 解决 1507 TokenNotExistError 问题
        storeTokenInOpenIMRedis(openimUserId, token, expireTimeSeconds);

        return new Object[]{token, expireTimeSeconds};
    }

    /**
     * 将 Token 存储到 OpenIM Redis
     * 模拟 OpenIM 内部 Token 存储逻辑
     *
     * OpenIM v3.x 使用双重验证:
     * 1. JWT 签名验证
     * 2. Redis 存在性检查 ← 这一步需要 Token 在 Redis 中存在
     *
     * Redis Key 格式: openim:token:{userID}:{platformID}
     * 例如: openim:token:emp_cf1e361fd46741f5b2a09335cef50db8:5
     *
     * @param openimUserId OpenIM 用户ID
     * @param token JWT Token
     * @param expireTimeSeconds 过期时间(秒)
     */
    private void storeTokenInOpenIMRedis(String openimUserId, String token, Long expireTimeSeconds) {
        try {
            // OpenIM v3.x Redis Key 格式
            String redisKey = String.format("openim:token:%s:%d", openimUserId, IMConstant.DEFAULT_PLATFORM_ID);

            // 存储 Token,过期时间与 Token 本身一致
            openimRedisTemplate.opsForValue().set(redisKey, token, expireTimeSeconds, TimeUnit.SECONDS);

            log.info("✅ [Token存储] Token已存储到OpenIM Redis: {}, 过期时间: {}秒 ({}天)",
                    redisKey, expireTimeSeconds, expireTimeSeconds / 86400);

        } catch (Exception e) {
            log.error("❌ [Token存储] 存储Token到Redis失败", e);
            // 不抛出异常,避免影响正常流程
            // 即使存储失败,前端仍可能通过其他方式工作
        }
    }

    /**
     * 构建 Token VO
     */
    private IMTokenVO buildTokenVO(String openimUserId, String token, Long expireTime, Long employeeId) {
        // 查询员工信息
        EmployeeEntity employee = employeeDao.selectById(employeeId);

        return IMTokenVO.builder()
            .openimUserId(openimUserId)
            .token(token)
            .expireTime(expireTime)
            .nickname(employee != null ? employee.getActualName() : "")
            .faceURL(employee != null && employee.getAvatar() != null ? employee.getAvatar() : "")
            .build();
    }

    /**
     * 构建 Token 缓存键
     */
    private String buildTokenCacheKey(Long employeeId) {
        return "im:token:" + employeeId;
    }

    /**
     * 获取 Token 过期时间
     * 基于缓存的剩余TTL计算
     */
    private Long getTokenExpireTime(String cacheKey) {
        Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        if (ttl == null || ttl < 0) {
            // 缓存不存在或已过期，使用默认值(不应该到这里)
            log.warn("⚠️ [Token缓存] 缓存Key不存在或已过期: {}", cacheKey);
            return System.currentTimeMillis() / 1000 + openIMConfig.getTokenExpireSeconds();
        }
        // TTL + 当前时间 = 过期时间戳
        return System.currentTimeMillis() / 1000 + ttl;
    }
}
