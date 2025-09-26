package net.lab1024.sa.admin.module.business.collaboration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协作WebSocket控制器
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@Slf4j
@Component
public class CollaborationWebSocketController implements WebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 房间 -> 会话映射
    private final Map<String, Map<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    // 会话 -> 用户信息映射
    private final Map<String, UserInfo> sessionUsers = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = getRoomId(session);
        String userId = getUserId(session);

        log.info("WebSocket连接建立: roomId={}, userId={}, sessionId={}", roomId, userId, session.getId());

        // 添加到房间
        roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(session.getId(), session);

        // 记录用户信息
        sessionUsers.put(session.getId(), new UserInfo(userId, getCurrentUserName(userId), roomId));

        // 通知房间内其他用户
        broadcastUserJoined(roomId, userId, session.getId());

        // 发送当前房间用户列表给新用户
        sendActiveUsers(session, roomId);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            String payload = message.getPayload().toString();
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String type = (String) data.get("type");
            String roomId = getRoomId(session);
            String userId = getUserId(session);

            log.debug("收到WebSocket消息: type={}, roomId={}, userId={}", type, roomId, userId);

            switch (type) {
                case "ping":
                    // 心跳响应
                    sendMessage(session, Map.of(
                        "type", "pong",
                        "timestamp", data.get("timestamp")
                    ));
                    break;

                case "cursor_update":
                    // 广播光标位置更新
                    broadcastToRoom(roomId, data, session.getId());
                    break;

                case "join_room":
                    // 用户主动加入房间消息
                    log.info("用户主动加入房间: userId={}, roomId={}", userId, roomId);
                    break;

                default:
                    // 其他消息类型直接广播
                    broadcastToRoom(roomId, data, session.getId());
                    break;
            }

        } catch (Exception e) {
            log.error("处理WebSocket消息失败: sessionId={}", session.getId(), e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: sessionId={}", session.getId(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String roomId = getRoomId(session);
        String userId = getUserId(session);

        log.info("WebSocket连接关闭: roomId={}, userId={}, sessionId={}, status={}",
                roomId, userId, session.getId(), closeStatus);

        // 从房间移除
        Map<String, WebSocketSession> roomSessionMap = roomSessions.get(roomId);
        if (roomSessionMap != null) {
            roomSessionMap.remove(session.getId());
            if (roomSessionMap.isEmpty()) {
                roomSessions.remove(roomId);
            }
        }

        // 移除用户信息
        UserInfo userInfo = sessionUsers.remove(session.getId());

        // 通知房间内其他用户
        if (userInfo != null) {
            broadcastUserLeft(roomId, userInfo.userId, session.getId());
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 广播用户加入消息
     */
    private void broadcastUserJoined(String roomId, String userId, String sessionId) {
        Map<String, Object> message = Map.of(
            "type", "user_joined",
            "user", Map.of(
                "id", userId,
                "name", getCurrentUserName(userId),
                "color", generateUserColor(userId),
                "sessionId", sessionId,
                "lastActivity", System.currentTimeMillis(),
                "isOnline", true
            )
        );
        broadcastToRoom(roomId, message, sessionId);
    }

    /**
     * 广播用户离开消息
     */
    private void broadcastUserLeft(String roomId, String userId, String sessionId) {
        Map<String, Object> message = Map.of(
            "type", "user_left",
            "user", Map.of(
                "id", userId,
                "name", getCurrentUserName(userId),
                "sessionId", sessionId
            )
        );
        broadcastToRoom(roomId, message, sessionId);
    }

    /**
     * 发送当前房间活跃用户列表
     */
    private void sendActiveUsers(WebSocketSession session, String roomId) {
        Map<String, WebSocketSession> roomSessionMap = roomSessions.get(roomId);
        if (roomSessionMap == null) return;

        Map<String, Object> users = new ConcurrentHashMap<>();
        roomSessionMap.forEach((sessionId, ws) -> {
            UserInfo userInfo = sessionUsers.get(sessionId);
            if (userInfo != null && !sessionId.equals(session.getId())) {
                users.put(userInfo.userId, Map.of(
                    "id", userInfo.userId,
                    "name", userInfo.userName,
                    "color", generateUserColor(userInfo.userId),
                    "sessionId", sessionId,
                    "lastActivity", System.currentTimeMillis(),
                    "isOnline", true
                ));
            }
        });

        Map<String, Object> message = Map.of(
            "type", "users_list",
            "users", users.values()
        );
        sendMessage(session, message);
    }

    /**
     * 广播消息到房间内所有用户（除了发送者）
     */
    private void broadcastToRoom(String roomId, Map<String, Object> message, String excludeSessionId) {
        Map<String, WebSocketSession> roomSessionMap = roomSessions.get(roomId);
        if (roomSessionMap == null) return;

        roomSessionMap.forEach((sessionId, session) -> {
            if (!sessionId.equals(excludeSessionId) && session.isOpen()) {
                sendMessage(session, message);
            }
        });
    }

    /**
     * 发送消息到指定会话
     */
    private void sendMessage(WebSocketSession session, Map<String, Object> message) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.error("发送WebSocket消息失败: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 从WebSocket会话中获取房间ID
     */
    private String getRoomId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] kv = param.split("=");
                if (kv.length == 2 && "roomId".equals(kv[0])) {
                    return kv[1];
                }
            }
        }
        return "default";
    }

    /**
     * 从WebSocket会话中获取用户ID
     */
    private String getUserId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] kv = param.split("=");
                if (kv.length == 2 && "userId".equals(kv[0])) {
                    return kv[1];
                }
            }
        }
        return "anonymous";
    }

    /**
     * 获取当前用户名（这里简化处理，实际应该从用户服务获取）
     */
    private String getCurrentUserName(String userId) {
        // TODO: 从用户服务获取真实用户名
        return "用户" + userId;
    }

    /**
     * 生成用户颜色
     */
    private String generateUserColor(String userId) {
        String[] colors = {"#1890ff", "#52c41a", "#fa8c16", "#eb2f96", "#722ed1", "#13c2c2"};
        int hash = Math.abs(userId.hashCode());
        return colors[hash % colors.length];
    }

    /**
     * 用户信息内部类
     */
    private static class UserInfo {
        public final String userId;
        public final String userName;
        public final String roomId;

        public UserInfo(String userId, String userName, String roomId) {
            this.userId = userId;
            this.userName = userName;
            this.roomId = roomId;
        }
    }
}