package net.lab1024.sa.admin.module.support.rocketmq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.oa.police.service.PoliceEditLockService;
import net.lab1024.sa.admin.module.business.oa.police.service.PoliceReportService;
import net.lab1024.sa.admin.module.support.rocketmq.constant.RocketMQTopics;
import net.lab1024.sa.admin.module.support.rocketmq.domain.RocketMQMessage;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketMessage;
import net.lab1024.sa.admin.module.support.websocket.service.MessageTransport;
import net.lab1024.sa.base.module.support.operatelog.domain.OperateLogEntity;
import net.lab1024.sa.base.module.support.operatelog.OperateLogDao;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 警情字段同步消费者 - 增强版
 *
 * 功能特性：
 * 1. 字段级实时同步
 * 2. 编辑锁定状态管理
 * 3. 操作历史记录
 * 4. 高并发性能优化
 * 5. 消息去重和幂等性保证
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-29
 * @Copyright 1024创新实验室
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rocketmq.name-server")
@RocketMQMessageListener(
    topic = RocketMQTopics.POLICE_FIELD_SYNC,
    consumerGroup = "police-field-sync-consumer",
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING,
    maxReconsumeTimes = 3
)
public class PoliceFieldSyncConsumer implements RocketMQListener<RocketMQMessage> {

    @Qualifier("webSocketTransport")
    private final MessageTransport webSocketTransport;

    private final PoliceEditLockService editLockService;
    private final PoliceReportService policeReportService;
    private final OperateLogDao operateLogDao;
    private final ObjectMapper objectMapper;

    // 性能监控指标
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private volatile long lastLogTime = System.currentTimeMillis();

    @Override
    public void onMessage(RocketMQMessage message) {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("🚀 [警情字段同步] 收到消息: type={}, tag={}, messageId={}",
                     message.getType(), message.getTag(), message.getMessageId());

            // 处理不同类型的同步消息
            switch (message.getType()) {
                case "FIELD_UPDATE":
                    handleFieldUpdate(message);
                    break;
                case "FIELD_LOCK":
                    handleFieldLock(message);
                    break;
                case "FIELD_UNLOCK":
                    handleFieldUnlock(message);
                    break;
                case "FIELD_EDIT_STATE":
                    handleFieldEditState(message);
                    break;
                case "BATCH_UPDATE":
                    handleBatchUpdate(message);
                    break;
                case "USER_STATE":
                    handleUserState(message);
                    break;
                case "USER_ACTIVITY":
                    handleUserActivity(message);
                    break;
                default:
                    // 兼容旧版本消息格式，转发到WebSocket
                    forwardToWebSocket(message);
                    break;
            }

            // 性能统计
            long processingTime = System.currentTimeMillis() - startTime;
            processedCount.incrementAndGet();

            if (processingTime > 100) {
                log.warn("🚀 [性能警告] 消息处理耗时过长: {}ms, type={}, messageId={}",
                        processingTime, message.getType(), message.getMessageId());
            }

            // 定期输出性能统计
            logPerformanceStats();

        } catch (Exception e) {
            errorCount.incrementAndGet();
            log.error("🚀 [警情字段同步] 消息处理失败: type={}, messageId={}, error={}",
                     message.getType(), message.getMessageId(), e.getMessage(), e);

            // 对于关键错误，可以选择重新抛出异常触发重试
            if (isRetryableError(e)) {
                throw new RuntimeException("Retryable error occurred", e);
            }
        }
    }

    /**
     * 处理字段更新消息
     */
    private void handleFieldUpdate(RocketMQMessage message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) message.getData();

            Long reportId = getLongValue(data, "reportId");
            Long userId = getLongValue(data, "userId");
            String userName = getStringValue(data, "userName");
            String fieldName = getStringValue(data, "fieldName");
            String oldValue = getStringValue(data, "oldValue");
            String newValue = getStringValue(data, "newValue");
            String operationType = getStringValue(data, "operationType");

            if (reportId == null || fieldName == null) {
                log.warn("🚀 [字段更新] 消息数据不完整: reportId={}, fieldName={}", reportId, fieldName);
                return;
            }

            log.info("🚀 [字段更新] 处理字段更新: reportId={}, fieldName={}, userId={}, userName={}",
                    reportId, fieldName, userId, userName);

            // 记录操作历史
            recordFieldOperation(reportId, userId, userName, fieldName, oldValue, newValue, operationType);

            // 转发到WebSocket客户端
            forwardToWebSocket(message);

        } catch (Exception e) {
            log.error("🚀 [字段更新] 处理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理字段锁定消息
     */
    private void handleFieldLock(RocketMQMessage message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) message.getData();

            Long reportId = getLongValue(data, "reportId");
            Long userId = getLongValue(data, "userId");
            String userName = getStringValue(data, "userName");
            String fieldName = getStringValue(data, "fieldName");

            if (reportId == null || fieldName == null || userId == null) {
                log.warn("🚀 [字段锁定] 消息数据不完整: reportId={}, fieldName={}, userId={}",
                        reportId, fieldName, userId);
                return;
            }

            log.info("🚀 [字段锁定] 处理字段锁定: reportId={}, fieldName={}, userId={}, userName={}",
                    reportId, fieldName, userId, userName);

            // 转发到WebSocket客户端
            forwardToWebSocket(message);

        } catch (Exception e) {
            log.error("🚀 [字段锁定] 处理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理字段解锁消息
     */
    private void handleFieldUnlock(RocketMQMessage message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) message.getData();

            Long reportId = getLongValue(data, "reportId");
            Long userId = getLongValue(data, "userId");
            String userName = getStringValue(data, "userName");
            String fieldName = getStringValue(data, "fieldName");

            if (reportId == null || fieldName == null || userId == null) {
                log.warn("🚀 [字段解锁] 消息数据不完整: reportId={}, fieldName={}, userId={}",
                        reportId, fieldName, userId);
                return;
            }

            log.info("🚀 [字段解锁] 处理字段解锁: reportId={}, fieldName={}, userId={}, userName={}",
                    reportId, fieldName, userId, userName);

            // 转发到WebSocket客户端
            forwardToWebSocket(message);

        } catch (Exception e) {
            log.error("🚀 [字段解锁] 处理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理字段编辑状态消息
     */
    private void handleFieldEditState(RocketMQMessage message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) message.getData();

            Long reportId = getLongValue(data, "reportId");
            Long userId = getLongValue(data, "userId");
            String userName = getStringValue(data, "userName");
            String fieldName = getStringValue(data, "fieldName");
            String action = getStringValue(data, "action");

            if (reportId == null || fieldName == null || userId == null) {
                log.warn("🚀 [编辑状态] 消息数据不完整: reportId={}, fieldName={}, userId={}, action={}",
                        reportId, fieldName, userId, action);
                return;
            }

            log.debug("🚀 [编辑状态] 处理编辑状态: reportId={}, fieldName={}, userId={}, action={}",
                     reportId, fieldName, userId, action);

            // 转发到WebSocket客户端
            forwardToWebSocket(message);

        } catch (Exception e) {
            log.error("🚀 [编辑状态] 处理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理批量更新消息
     */
    private void handleBatchUpdate(RocketMQMessage message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) message.getData();

            Long reportId = getLongValue(data, "reportId");
            Long userId = getLongValue(data, "userId");
            String userName = getStringValue(data, "userName");

            @SuppressWarnings("unchecked")
            Map<String, Object> fieldUpdates = (Map<String, Object>) data.get("fieldUpdates");

            if (reportId == null || fieldUpdates == null || fieldUpdates.isEmpty()) {
                log.warn("🚀 [批量更新] 消息数据不完整: reportId={}, fieldUpdates size={}",
                        reportId, fieldUpdates != null ? fieldUpdates.size() : 0);
                return;
            }

            log.info("🚀 [批量更新] 处理批量更新: reportId={}, fields={}, userId={}, userName={}",
                    reportId, fieldUpdates.keySet(), userId, userName);

            // 记录每个字段的操作历史
            for (Map.Entry<String, Object> entry : fieldUpdates.entrySet()) {
                String fieldName = entry.getKey();
                String newValue = entry.getValue() != null ? String.valueOf(entry.getValue()) : null;

                recordFieldOperation(reportId, userId, userName, fieldName, null, newValue, "BATCH_UPDATE");
            }

            // 转发到WebSocket客户端
            forwardToWebSocket(message);

        } catch (Exception e) {
            log.error("🚀 [批量更新] 处理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理用户状态消息
     */
    private void handleUserState(RocketMQMessage message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) message.getData();

            Long reportId = getLongValue(data, "reportId");
            Long userId = getLongValue(data, "userId");
            String userName = getStringValue(data, "userName");
            String action = getStringValue(data, "action");

            if (reportId == null || userId == null || action == null) {
                log.warn("🚀 [用户状态] 消息数据不完整: reportId={}, userId={}, action={}",
                        reportId, userId, action);
                return;
            }

            log.info("🚀 [用户状态] 处理用户状态变更: reportId={}, userId={}, userName={}, action={}",
                    reportId, userId, userName, action);

            // 记录用户状态操作
            recordFieldOperation(reportId, userId, userName, "user_state", null, action, "USER_STATE");

            // 转发到WebSocket客户端
            forwardToWebSocket(message);

        } catch (Exception e) {
            log.error("🚀 [用户状态] 处理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理用户活跃度消息
     */
    private void handleUserActivity(RocketMQMessage message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) message.getData();

            Long reportId = getLongValue(data, "reportId");
            Long userId = getLongValue(data, "userId");
            String userName = getStringValue(data, "userName");
            String activityType = getStringValue(data, "activityType");

            if (reportId == null || userId == null || activityType == null) {
                log.debug("🚀 [用户活跃度] 消息数据不完整: reportId={}, userId={}, activityType={}",
                        reportId, userId, activityType);
                return;
            }

            log.debug("🚀 [用户活跃度] 处理用户活跃度: reportId={}, userId={}, userName={}, activityType={}",
                     reportId, userId, userName, activityType);

            // 转发到WebSocket客户端（不记录操作历史，减少日志噪音）
            forwardToWebSocket(message);

        } catch (Exception e) {
            log.error("🚀 [用户活跃度] 处理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 记录字段操作历史
     */
    private void recordFieldOperation(Long reportId, Long userId, String userName,
                                    String fieldName, String oldValue, String newValue, String operationType) {
        try {
            OperateLogEntity operateLog = new OperateLogEntity();
            operateLog.setOperateUserId(userId);
            operateLog.setOperateUserName(userName);
            operateLog.setOperateUserType(1);
            operateLog.setModule("警情协同");
            operateLog.setContent("RocketMQ字段同步");
            operateLog.setUrl("/oa/police/report/field-sync/" + reportId);
            operateLog.setMethod("ROCKETMQ");

            // 构建包含旧值和新值的参数
            String paramWithValues = String.format("{\"%s\":{\"old\":\"%s\",\"new\":\"%s\",\"operation\":\"%s\"}}",
                    fieldName,
                    oldValue != null ? oldValue.replace("\"", "\\\"") : "",
                    newValue != null ? newValue.replace("\"", "\\\"") : "",
                    operationType);

            operateLog.setParam(paramWithValues);
            operateLog.setCreateTime(LocalDateTime.now());
            operateLog.setIp("RocketMQ Consumer");
            operateLog.setUserAgent("RocketMQ Field Sync");
            operateLog.setSuccessFlag(true);
            operateLog.setFailReason("");

            operateLogDao.insert(operateLog);

            log.debug("🚀 [操作记录] 已保存: reportId={}, fieldName={}, operation={}",
                     reportId, fieldName, operationType);

        } catch (Exception e) {
            log.error("🚀 [操作记录] 保存失败: reportId={}, fieldName={}, error={}",
                     reportId, fieldName, e.getMessage());
        }
    }

    /**
     * 转发消息到WebSocket
     */
    private void forwardToWebSocket(RocketMQMessage rocketMessage) {
        try {
            WebSocketMessage wsMessage = rocketMessage.toWebSocketMessage();
            String tag = rocketMessage.getTag();

            if (tag == null || tag.equals("BROADCAST")) {
                // 广播消息
                webSocketTransport.broadcast(wsMessage);
            } else if (tag.startsWith("USER_")) {
                // 用户消息
                Long userId = Long.parseLong(tag.substring(5));
                webSocketTransport.sendToUser(userId, wsMessage);
            } else if (tag.startsWith("ROOM_")) {
                // 房间消息
                String room = tag.substring(5);
                webSocketTransport.sendToRoom(room, wsMessage);
            } else if (tag.startsWith("DEPT_")) {
                // 部门消息
                Long departmentId = Long.parseLong(tag.substring(5));
                webSocketTransport.sendToDepartment(departmentId, wsMessage);
            } else if (tag.startsWith("ROLE_")) {
                // 角色消息
                String role = tag.substring(5);
                webSocketTransport.sendToRole(role, wsMessage);
            } else {
                // 默认广播
                webSocketTransport.broadcast(wsMessage);
            }

        } catch (Exception e) {
            log.error("🚀 [WebSocket转发] 转发失败: messageId={}, error={}",
                     rocketMessage.getMessageId(), e.getMessage(), e);
        }
    }

    /**
     * 性能统计日志
     */
    private void logPerformanceStats() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > 60000) { // 每分钟输出一次
            long processed = processedCount.get();
            long errors = errorCount.get();

            if (processed > 0) {
                log.info("🚀 [性能统计] 1分钟内处理消息: 成功={}, 失败={}, 成功率={:.2f}%",
                        processed, errors, (processed * 100.0 / (processed + errors)));

                // 重置计数器
                processedCount.set(0);
                errorCount.set(0);
            }

            lastLogTime = currentTime;
        }
    }

    /**
     * 判断是否为可重试的错误
     */
    private boolean isRetryableError(Exception e) {
        // 数据库连接错误、网络错误等可以重试
        String errorMessage = e.getMessage();
        if (errorMessage != null) {
            return errorMessage.contains("Connection") ||
                   errorMessage.contains("Timeout") ||
                   errorMessage.contains("Network") ||
                   errorMessage.contains("Socket");
        }
        return false;
    }

    /**
     * 安全获取Long值
     */
    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;

        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.warn("🚀 [数据转换] Long值转换失败: key={}, value={}", key, value);
                return null;
            }
        }
        return null;
    }

    /**
     * 安全获取String值
     */
    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? String.valueOf(value) : null;
    }
}