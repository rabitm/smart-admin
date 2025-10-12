package net.lab1024.sa.admin.module.support.im.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.im.constant.IMOperationTypeEnum;
import net.lab1024.sa.admin.module.support.im.dao.IMOperationLogDao;
import net.lab1024.sa.admin.module.support.im.domain.entity.IMOperationLogEntity;
import net.lab1024.sa.base.common.domain.RequestUser;
import net.lab1024.sa.base.common.util.SmartRequestUtil;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * IM操作日志服务
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Service
public class IMOperationLogService {

    @Resource
    private IMOperationLogDao imOperationLogDao;

    /**
     * 异步记录操作日志
     */
    @Async("imAsyncExecutor")
    public void logAsync(IMOperationTypeEnum operationType, String targetType, String targetId,
                         Boolean success, Object requestData, Object responseData,
                         Integer executionTime, String errorMessage) {
        try {
            IMOperationLogEntity logEntity = buildLogEntity(
                    operationType, targetType, targetId, success,
                    requestData, responseData, executionTime, errorMessage
            );
            imOperationLogDao.insert(logEntity);
            log.debug("📝 [操作日志] 记录成功 - {}: {}", operationType.getDescription(), targetId);
        } catch (Exception e) {
            log.error("❌ [操作日志] 记录失败", e);
        }
    }

    /**
     * 记录成功操作
     */
    @Async("imAsyncExecutor")
    public void logSuccess(IMOperationTypeEnum operationType, String targetType, String targetId,
                          Object requestData, Object responseData, Integer executionTime) {
        logAsync(operationType, targetType, targetId, true, requestData, responseData, executionTime, null);
    }

    /**
     * 记录失败操作
     */
    @Async("imAsyncExecutor")
    public void logFailure(IMOperationTypeEnum operationType, String targetType, String targetId,
                          Object requestData, String errorMessage, Integer executionTime) {
        logAsync(operationType, targetType, targetId, false, requestData, null, executionTime, errorMessage);
    }

    /**
     * 构建日志实体
     */
    private IMOperationLogEntity buildLogEntity(IMOperationTypeEnum operationType,
                                                String targetType, String targetId,
                                                Boolean success, Object requestData,
                                                Object responseData, Integer executionTime,
                                                String errorMessage) {
        IMOperationLogEntity logEntity = new IMOperationLogEntity();

        // 基本信息
        logEntity.setOperationType(operationType.getCode());
        logEntity.setOperationDetail(operationType.getDescription());
        logEntity.setTargetType(targetType);
        logEntity.setTargetId(targetId);
        logEntity.setSuccessFlag(success);
        logEntity.setExecutionTime(executionTime);
        logEntity.setErrorMessage(errorMessage);

        // 数据信息(脱敏处理)
        if (requestData != null) {
            logEntity.setRequestData(JSON.toJSONString(requestData));
        }
        if (responseData != null) {
            String responseStr = JSON.toJSONString(responseData);
            // 限制响应数据长度
            if (responseStr.length() > 5000) {
                responseStr = responseStr.substring(0, 5000) + "...(truncated)";
            }
            logEntity.setResponseData(responseStr);
        }

        // 操作人信息
        try {
            RequestUser requestUser = SmartRequestUtil.getRequestUser();
            if (requestUser != null) {
                logEntity.setOperatorId(requestUser.getUserId());
                logEntity.setOperatorName(requestUser.getUserName());
            }
        } catch (Exception e) {
            log.debug("无法获取操作人信息: {}", e.getMessage());
        }

        // 请求信息
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                logEntity.setIpAddress(getClientIpAddress(request));
                logEntity.setUserAgent(request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            log.debug("无法获取请求信息: {}", e.getMessage());
        }

        logEntity.setCreateTime(LocalDateTime.now());

        return logEntity;
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 对于多级代理,取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 清理过期日志(保留30天)
     */
    public int cleanExpiredLogs() {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(30);
        int count = imOperationLogDao.deleteExpiredLogs(expireTime);
        log.info("🗑️ [操作日志] 清理过期日志完成,删除{}条记录", count);
        return count;
    }
}
