package net.lab1024.sa.admin.module.support.websocket.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketMessage;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketSession;
import net.lab1024.sa.admin.module.support.websocket.service.MessageTransport;
import net.lab1024.sa.admin.module.support.websocket.service.WebSocketSessionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * WebSocket传输实现
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "websocket.transport.type", havingValue = "websocket", matchIfMissing = true)
public class WebSocketTransport implements MessageTransport {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    /**
     * Spring WebSocket会话映射
     */
    private final Map<String, org.springframework.web.socket.WebSocketSession> springWebSocketSessions = new ConcurrentHashMap<>();

    /**
     * 添加Spring WebSocket会话
     */
    public void addSpringWebSocketSession(String sessionId, org.springframework.web.socket.WebSocketSession springSession) {
        springWebSocketSessions.put(sessionId, springSession);
    }

    /**
     * 移除Spring WebSocket会话
     */
    public void removeSpringWebSocketSession(String sessionId) {
        springWebSocketSessions.remove(sessionId);
    }

    @Override
    public boolean sendToUser(Long userId, WebSocketMessage message) {
        WebSocketSession session = sessionManager.getSessionByUserId(userId);
        if (session != null) {
            return sendToSession(session.getSessionId(), message);
        }
        log.debug("用户{}不在线，无法发送消息", userId);
        return false;
    }

    @Override
    public boolean sendToSession(String sessionId, WebSocketMessage message) {
        org.springframework.web.socket.WebSocketSession springSession = springWebSocketSessions.get(sessionId);
        if (springSession == null || !springSession.isOpen()) {
            log.debug("会话{}不存在或已关闭", sessionId);
            return false;
        }

        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            springSession.sendMessage(new TextMessage(jsonMessage));

            // 更新会话活跃时间
            sessionManager.updateSessionActivity(sessionId);

            log.debug("消息发送成功: sessionId={}, type={}", sessionId, message.getType());
            return true;
        } catch (JsonProcessingException e) {
            log.error("序列化消息失败: sessionId={}, error={}", sessionId, e.getMessage());
            return false;
        } catch (IOException e) {
            log.error("发送消息到会话{}失败: {}", sessionId, e.getMessage());
            return false;
        }
    }

    @Override
    public int broadcast(WebSocketMessage message) {
        List<WebSocketSession> sessions = sessionManager.getAllActiveSessions();
        int successCount = 0;

        for (WebSocketSession session : sessions) {
            if (sendToSession(session.getSessionId(), message)) {
                successCount++;
            }
        }

        log.debug("广播消息{}: 成功发送给{}个用户", message.getType(), successCount);
        return successCount;
    }

    @Override
    public int sendToRoom(String room, WebSocketMessage message) {
        List<WebSocketSession> sessions = sessionManager.getRoomSessions(room);
        int successCount = 0;

        for (WebSocketSession session : sessions) {
            if (sendToSession(session.getSessionId(), message)) {
                successCount++;
            }
        }

        log.debug("房间{}广播消息{}: 成功发送给{}个用户", room, message.getType(), successCount);
        return successCount;
    }

    @Override
    public int sendToModule(String module, WebSocketMessage message) {
        List<WebSocketSession> sessions = sessionManager.getModuleSessions(module);
        int successCount = 0;

        for (WebSocketSession session : sessions) {
            if (sendToSession(session.getSessionId(), message)) {
                successCount++;
            }
        }

        log.debug("模块{}广播消息{}: 成功发送给{}个用户", module, message.getType(), successCount);
        return successCount;
    }

    @Override
    public int sendToMatched(WebSocketMessage message, Predicate<WebSocketSession> predicate) {
        List<WebSocketSession> allSessions = sessionManager.getAllActiveSessions();
        int successCount = 0;

        for (WebSocketSession session : allSessions) {
            if (predicate.test(session)) {
                if (sendToSession(session.getSessionId(), message)) {
                    successCount++;
                }
            }
        }

        log.debug("条件广播消息{}: 成功发送给{}个用户", message.getType(), successCount);
        return successCount;
    }

    public int sendToUserList(List<Long> userIds, WebSocketMessage message) {
        int successCount = 0;

        for (Long userId : userIds) {
            if (sendToUser(userId, message)) {
                successCount++;
            }
        }

        log.debug("批量发送消息{}: 成功发送给{}个用户", message.getType(), successCount);
        return successCount;
    }

    public int sendToSessionList(List<String> sessionIds, WebSocketMessage message) {
        int successCount = 0;

        for (String sessionId : sessionIds) {
            if (sendToSession(sessionId, message)) {
                successCount++;
            }
        }

        log.debug("批量发送消息{}: 成功发送给{}个会话", message.getType(), successCount);
        return successCount;
    }

    @Override
    public boolean disconnectUser(Long userId, String reason) {
        WebSocketSession session = sessionManager.getSessionByUserId(userId);
        if (session != null) {
            org.springframework.web.socket.WebSocketSession springSession = springWebSocketSessions.get(session.getSessionId());
            if (springSession != null && springSession.isOpen()) {
                try {
                    // 发送断开通知
                    WebSocketMessage disconnectMessage = WebSocketMessage.system("DISCONNECT", reason);
                    String jsonMessage = objectMapper.writeValueAsString(disconnectMessage);
                    springSession.sendMessage(new TextMessage(jsonMessage));

                    // 延迟关闭连接
                    Thread.sleep(100);
                    springSession.close();

                    log.info("用户{}连接已断开: {}", userId, reason);
                    return true;
                } catch (Exception e) {
                    log.error("断开用户{}连接失败", userId, e);
                }
            }
        }
        return false;
    }

    public int getActiveConnectionCount() {
        return springWebSocketSessions.size();
    }

    public boolean isConnected(String sessionId) {
        org.springframework.web.socket.WebSocketSession springSession = springWebSocketSessions.get(sessionId);
        return springSession != null && springSession.isOpen();
    }

    @Override
    public int sendToDepartment(Long departmentId, WebSocketMessage message) {
        List<WebSocketSession> allSessions = sessionManager.getAllActiveSessions();
        int successCount = 0;

        for (WebSocketSession session : allSessions) {
            if (departmentId.equals(session.getDepartmentId())) {
                if (sendToSession(session.getSessionId(), message)) {
                    successCount++;
                }
            }
        }

        log.debug("部门{}广播消息{}: 成功发送给{}个用户", departmentId, message.getType(), successCount);
        return successCount;
    }

    @Override
    public int sendToRole(String role, WebSocketMessage message) {
        List<WebSocketSession> allSessions = sessionManager.getAllActiveSessions();
        int successCount = 0;

        for (WebSocketSession session : allSessions) {
            if (role.equals(session.getRole())) {
                if (sendToSession(session.getSessionId(), message)) {
                    successCount++;
                }
            }
        }

        log.debug("角色{}广播消息{}: 成功发送给{}个用户", role, message.getType(), successCount);
        return successCount;
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
        WebSocketSession session = sessionManager.getSessionByUserId(userId);
        return session != null;
    }

    @Override
    public String getTransportType() {
        return "websocket";
    }
}