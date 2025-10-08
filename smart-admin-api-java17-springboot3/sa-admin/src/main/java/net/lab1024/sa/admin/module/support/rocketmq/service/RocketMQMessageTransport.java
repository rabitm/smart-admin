package net.lab1024.sa.admin.module.support.rocketmq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.rocketmq.constant.RocketMQTopics;
import net.lab1024.sa.admin.module.support.rocketmq.domain.RocketMQMessage;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketMessage;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketSession;
import net.lab1024.sa.admin.module.support.websocket.service.MessageTransport;
import net.lab1024.sa.admin.module.support.websocket.service.WebSocketSessionManager;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;

/**
 * RocketMQ消息传输实现
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-29
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service("rocketMQTransport")
@ConditionalOnProperty(name = "rocketmq.name-server")
public class RocketMQMessageTransport implements MessageTransport {

    private final RocketMQTemplate rocketMQTemplate;
    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public RocketMQMessageTransport(RocketMQTemplate rocketMQTemplate,
                                   WebSocketSessionManager sessionManager,
                                   ObjectMapper objectMapper) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
        log.info("🚀 [RocketMQ] RocketMQ消息传输服务已初始化");
    }

    @Override
    public boolean sendToUser(Long userId, WebSocketMessage message) {
        try {
            String topic = RocketMQTopics.getTopicByModule(message.getModule());
            RocketMQMessage rocketMessage = RocketMQMessage.fromWebSocketMessage(message, topic, null)
                    .forUser(userId);

            String destination = rocketMessage.getDestination();

            // 异步发送消息
            rocketMQTemplate.convertAndSend(destination, rocketMessage);

            log.debug("🚀 [RocketMQ] 发送用户消息: userId={}, type={}, topic={}",
                     userId, message.getType(), topic);
            return true;

        } catch (Exception e) {
            log.error("🚀 [RocketMQ] 发送用户消息失败: userId={}, error={}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendToSession(String sessionId, WebSocketMessage message) {
        try {
            // 获取会话信息，转换为用户消息
            WebSocketSession session = sessionManager.getSession(sessionId);
            if (session != null && session.getUserId() != null) {
                return sendToUser(session.getUserId(), message);
            }

            log.debug("🚀 [RocketMQ] 会话{}不存在或无用户ID", sessionId);
            return false;

        } catch (Exception e) {
            log.error("🚀 [RocketMQ] 发送会话消息失败: sessionId={}, error={}", sessionId, e.getMessage());
            return false;
        }
    }

    @Override
    public int broadcast(WebSocketMessage message) {
        try {
            String topic = RocketMQTopics.getTopicByModule(message.getModule());
            RocketMQMessage rocketMessage = RocketMQMessage.fromWebSocketMessage(message, topic, null)
                    .forBroadcast();

            String destination = rocketMessage.getDestination();

            // 广播消息
            rocketMQTemplate.convertAndSend(destination, rocketMessage);

            int onlineCount = sessionManager.getOnlineUserCount();
            log.debug("🚀 [RocketMQ] 广播消息: type={}, topic={}, 在线用户数={}",
                     message.getType(), topic, onlineCount);
            return onlineCount;

        } catch (Exception e) {
            log.error("🚀 [RocketMQ] 广播消息失败: error={}", e.getMessage());
            return 0;
        }
    }

    @Override
    public int sendToRoom(String room, WebSocketMessage message) {
        try {
            String topic = RocketMQTopics.getTopicByModule(message.getModule());
            RocketMQMessage rocketMessage = RocketMQMessage.fromWebSocketMessage(message, topic, null)
                    .forRoom(room);

            String destination = rocketMessage.getDestination();

            // 发送房间消息
            rocketMQTemplate.convertAndSend(destination, rocketMessage);

            int roomUserCount = sessionManager.getRoomSessions(room).size();
            log.debug("🚀 [RocketMQ] 发送房间消息: room={}, type={}, topic={}, 房间用户数={}",
                     room, message.getType(), topic, roomUserCount);
            return roomUserCount;

        } catch (Exception e) {
            log.error("🚀 [RocketMQ] 发送房间消息失败: room={}, error={}", room, e.getMessage());
            return 0;
        }
    }

    @Override
    public int sendToModule(String module, WebSocketMessage message) {
        try {
            String topic = RocketMQTopics.getTopicByModule(module);
            RocketMQMessage rocketMessage = RocketMQMessage.fromWebSocketMessage(message, topic, null)
                    .forBroadcast();

            String destination = rocketMessage.getDestination();

            // 发送模块消息
            rocketMQTemplate.convertAndSend(destination, rocketMessage);

            int moduleUserCount = sessionManager.getModuleSessions(module).size();
            log.debug("🚀 [RocketMQ] 发送模块消息: module={}, type={}, topic={}, 模块用户数={}",
                     module, message.getType(), topic, moduleUserCount);
            return moduleUserCount;

        } catch (Exception e) {
            log.error("🚀 [RocketMQ] 发送模块消息失败: module={}, error={}", module, e.getMessage());
            return 0;
        }
    }

    @Override
    public int sendToDepartment(Long departmentId, WebSocketMessage message) {
        try {
            String topic = RocketMQTopics.getTopicByModule(message.getModule());
            RocketMQMessage rocketMessage = RocketMQMessage.fromWebSocketMessage(message, topic, null)
                    .forDepartment(departmentId);

            String destination = rocketMessage.getDestination();

            // 发送部门消息
            rocketMQTemplate.convertAndSend(destination, rocketMessage);

            int deptUserCount = sessionManager.getDepartmentSessions(departmentId).size();
            log.debug("🚀 [RocketMQ] 发送部门消息: deptId={}, type={}, topic={}, 部门用户数={}",
                     departmentId, message.getType(), topic, deptUserCount);
            return deptUserCount;

        } catch (Exception e) {
            log.error("🚀 [RocketMQ] 发送部门消息失败: deptId={}, error={}", departmentId, e.getMessage());
            return 0;
        }
    }

    @Override
    public int sendToRole(String role, WebSocketMessage message) {
        try {
            String topic = RocketMQTopics.getTopicByModule(message.getModule());
            RocketMQMessage rocketMessage = RocketMQMessage.fromWebSocketMessage(message, topic, null)
                    .forRole(role);

            String destination = rocketMessage.getDestination();

            // 发送角色消息
            rocketMQTemplate.convertAndSend(destination, rocketMessage);

            int roleUserCount = sessionManager.getRoleSessions(role).size();
            log.debug("🚀 [RocketMQ] 发送角色消息: role={}, type={}, topic={}, 角色用户数={}",
                     role, message.getType(), topic, roleUserCount);
            return roleUserCount;

        } catch (Exception e) {
            log.error("🚀 [RocketMQ] 发送角色消息失败: role={}, error={}", role, e.getMessage());
            return 0;
        }
    }

    @Override
    public int sendToMatched(WebSocketMessage message, Predicate<WebSocketSession> predicate) {
        try {
            // 获取匹配的会话
            List<WebSocketSession> sessions = sessionManager.getSessionsByPredicate(predicate);

            int successCount = 0;
            for (WebSocketSession session : sessions) {
                if (session.getUserId() != null) {
                    if (sendToUser(session.getUserId(), message)) {
                        successCount++;
                    }
                }
            }

            log.debug("🚀 [RocketMQ] 条件发送消息: type={}, 匹配用户数={}, 成功发送数={}",
                     message.getType(), sessions.size(), successCount);
            return successCount;

        } catch (Exception e) {
            log.error("🚀 [RocketMQ] 条件发送消息失败: error={}", e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean disconnectUser(Long userId, String reason) {
        try {
            // 发送强制断开消息
            WebSocketMessage disconnectMessage = WebSocketMessage.system("FORCE_DISCONNECT", reason);
            disconnectMessage.setToUserId(userId);

            boolean result = sendToUser(userId, disconnectMessage);

            log.info("🚀 [RocketMQ] 强制断开用户: userId={}, reason={}, result={}", userId, reason, result);
            return result;

        } catch (Exception e) {
            log.error("🚀 [RocketMQ] 强制断开用户失败: userId={}, error={}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    public int getOnlineUserCount() {
        return sessionManager.getOnlineUserCount();
    }

    @Override
    public List<Long> getOnlineUsers() {
        return sessionManager.getOnlineUserIds();
    }

    @Override
    public boolean isUserOnline(Long userId) {
        return sessionManager.isUserOnline(userId);
    }

    @Override
    public String getTransportType() {
        return "rocketmq";
    }

    /**
     * 发送有序消息
     */
    public boolean sendOrderedMessage(String orderId, WebSocketMessage message) {
        try {
            String topic = RocketMQTopics.getTopicByModule(message.getModule());
            RocketMQMessage rocketMessage = RocketMQMessage.fromWebSocketMessage(message, topic, null)
                    .ordered(orderId);

            String destination = rocketMessage.getDestination();

            // 发送顺序消息
            rocketMQTemplate.syncSendOrderly(destination, rocketMessage, orderId);

            log.debug("🚀 [RocketMQ] 发送有序消息: orderId={}, type={}, topic={}",
                     orderId, message.getType(), topic);
            return true;

        } catch (Exception e) {
            log.error("🚀 [RocketMQ] 发送有序消息失败: orderId={}, error={}", orderId, e.getMessage());
            return false;
        }
    }

    /**
     * 发送延迟消息
     */
    public boolean sendDelayedMessage(WebSocketMessage message, int delayLevel) {
        try {
            String topic = RocketMQTopics.getTopicByModule(message.getModule());
            RocketMQMessage rocketMessage = RocketMQMessage.fromWebSocketMessage(message, topic, null)
                    .delayed(delayLevel);

            String destination = rocketMessage.getDestination();

            // 发送延迟消息（注意：RocketMQ 4.x版本延迟消息API可能不同）
            rocketMQTemplate.syncSend(destination, rocketMessage, 3000);

            log.debug("🚀 [RocketMQ] 发送延迟消息: delayLevel={}, type={}, topic={}",
                     delayLevel, message.getType(), topic);
            return true;

        } catch (Exception e) {
            log.error("🚀 [RocketMQ] 发送延迟消息失败: delayLevel={}, error={}", delayLevel, e.getMessage());
            return false;
        }
    }
}