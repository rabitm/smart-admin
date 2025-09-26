package net.lab1024.sa.admin.module.business.oa.seat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 座位管理WebSocket控制器
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@Slf4j
@Component
public class SeatWebSocketController implements WebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 座位管理相关的会话映射
    private final Map<String, WebSocketSession> seatSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String seatId = getSeatId(session);
        String userId = getUserId(session);

        log.info("座位WebSocket连接建立: seatId={}, userId={}, sessionId={}", seatId, userId, session.getId());

        // 添加到座位会话映射
        seatSessions.put(session.getId(), session);

        // 发送连接成功消息
        sendMessage(session, Map.of(
            "type", "connection_established",
            "seatId", seatId,
            "userId", userId,
            "message", "座位WebSocket连接成功"
        ));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            String payload = message.getPayload().toString();
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String type = (String) data.get("type");
            String seatId = getSeatId(session);
            String userId = getUserId(session);

            log.debug("收到座位WebSocket消息: type={}, seatId={}, userId={}", type, seatId, userId);

            switch (type) {
                case "ping":
                    // 心跳响应
                    sendMessage(session, Map.of(
                        "type", "pong",
                        "timestamp", data.get("timestamp")
                    ));
                    break;

                case "seat_status_query":
                    // 查询座位状态
                    handleSeatStatusQuery(session, data);
                    break;

                case "seat_operation":
                    // 座位操作（预约、占用、释放等）
                    handleSeatOperation(session, data);
                    break;

                default:
                    log.debug("未处理的消息类型: {}", type);
                    break;
            }

        } catch (Exception e) {
            log.error("处理座位WebSocket消息失败: sessionId={}", session.getId(), e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("座位WebSocket传输错误: sessionId={}", session.getId(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String seatId = getSeatId(session);
        String userId = getUserId(session);

        log.info("座位WebSocket连接关闭: seatId={}, userId={}, sessionId={}, status={}",
                seatId, userId, session.getId(), closeStatus);

        // 从会话映射移除
        seatSessions.remove(session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 处理座位状态查询
     */
    private void handleSeatStatusQuery(WebSocketSession session, Map<String, Object> data) {
        try {
            String seatId = (String) data.get("seatId");

            // 这里可以查询数据库获取实际座位状态
            // 目前返回模拟数据
            Map<String, Object> response = Map.of(
                "type", "seat_status_response",
                "seatId", seatId,
                "status", "available",
                "currentUser", "",
                "reservedUser", "",
                "timestamp", System.currentTimeMillis()
            );

            sendMessage(session, response);
        } catch (Exception e) {
            log.error("处理座位状态查询失败", e);
        }
    }

    /**
     * 处理座位操作
     */
    private void handleSeatOperation(WebSocketSession session, Map<String, Object> data) {
        try {
            String operation = (String) data.get("operation");
            String seatId = (String) data.get("seatId");

            log.info("处理座位操作: operation={}, seatId={}", operation, seatId);

            // 这里可以调用座位服务处理实际操作
            // 目前返回模拟响应
            Map<String, Object> response = Map.of(
                "type", "seat_operation_response",
                "operation", operation,
                "seatId", seatId,
                "result", "success",
                "message", "操作成功",
                "timestamp", System.currentTimeMillis()
            );

            sendMessage(session, response);
        } catch (Exception e) {
            log.error("处理座位操作失败", e);
        }
    }

    /**
     * 广播座位状态变更给所有连接
     */
    public void broadcastSeatStatusChange(String seatId, String oldStatus, String newStatus,
                                        String userId, String userName) {
        Map<String, Object> message = Map.of(
            "type", "seat_status_changed",
            "seatId", seatId,
            "oldStatus", oldStatus,
            "newStatus", newStatus,
            "userId", userId,
            "userName", userName,
            "timestamp", System.currentTimeMillis()
        );

        broadcastToAll(message);
    }

    /**
     * 广播座位预约变更给所有连接
     */
    public void broadcastSeatReservationChange(String seatId, String action, String userId, String userName) {
        Map<String, Object> message = Map.of(
            "type", "seat_reservation_changed",
            "seatId", seatId,
            "action", action,
            "userId", userId,
            "userName", userName,
            "timestamp", System.currentTimeMillis()
        );

        broadcastToAll(message);
    }

    /**
     * 广播消息给所有连接的客户端
     */
    private void broadcastToAll(Map<String, Object> message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("序列化消息失败", e);
            return;
        }

        int sentCount = 0;
        for (WebSocketSession session : seatSessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                    sentCount++;
                } catch (IOException e) {
                    log.error("发送广播消息失败: sessionId={}", session.getId(), e);
                }
            }
        }

        log.debug("广播座位消息完成: {} 个连接", sentCount);
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
            log.error("发送座位WebSocket消息失败: sessionId={}", session.getId(), e);
        }
    }

    /**
     * 从WebSocket会话中获取座位ID
     */
    private String getSeatId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] kv = param.split("=");
                if (kv.length == 2 && "seatId".equals(kv[0])) {
                    return kv[1];
                }
            }
        }
        return "unknown";
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
                if (kv.length == 2 && ("userId".equals(kv[0]) || "employeeId".equals(kv[0]))) {
                    return kv[1];
                }
            }
        }
        return "anonymous";
    }
}