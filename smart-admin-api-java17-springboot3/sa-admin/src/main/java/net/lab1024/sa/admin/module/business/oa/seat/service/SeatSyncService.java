package net.lab1024.sa.admin.module.business.oa.seat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.collaboration.controller.CollaborationWebSocketController;
import net.lab1024.sa.admin.module.business.oa.seat.controller.SeatWebSocketController;
import net.lab1024.sa.admin.util.AdminRequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 座位同步服务
 * 处理警情与座位管理系统的实时同步
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-25
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service
public class SeatSyncService {

    @Autowired
    private CollaborationWebSocketController collaborationWebSocketController;

    @Autowired
    private SeatWebSocketController seatWebSocketController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 通知警情案例更新
     */
    public void notifyPoliceCaseUpdate(Long caseId, Long userId, String userName, Object updateData) {
        log.info("通知警情案例更新: caseId={}, userId={}, userName={}", caseId, userId, userName);

        Map<String, Object> message = new HashMap<>();
        message.put("type", "police_case_update");
        message.put("caseId", caseId);
        message.put("userId", userId);
        message.put("userName", userName);
        message.put("updateData", updateData);
        message.put("timestamp", System.currentTimeMillis());

        // 广播到所有相关房间（基于警情ID创建房间）
        String roomId = "police_case_" + caseId;
        broadcastToRoom(roomId, message, null);
    }

    /**
     * 通知警情列表刷新
     */
    public void notifyPoliceListRefresh(Long userId, String userName, String message) {
        log.info("通知警情列表刷新: userId={}, userName={}, message={}", userId, userName, message);

        Map<String, Object> wsMessage = new HashMap<>();
        wsMessage.put("type", "police_list_refresh");
        wsMessage.put("userId", userId);
        wsMessage.put("userName", userName);
        wsMessage.put("message", message);
        wsMessage.put("timestamp", System.currentTimeMillis());

        // 广播到警情列表房间
        String roomId = "police_list";
        broadcastToRoom(roomId, wsMessage, null);
    }

    /**
     * 通知警情案例锁定
     */
    public void notifyPoliceCaseLock(Long caseId, Long userId, String userName, Long seatId) {
        log.info("通知警情案例锁定: caseId={}, userId={}, userName={}, seatId={}", caseId, userId, userName, seatId);

        Map<String, Object> message = new HashMap<>();
        message.put("type", "police_case_lock");
        message.put("caseId", caseId);
        message.put("lockUserId", userId);
        message.put("lockUserName", userName);
        message.put("seatId", seatId);
        message.put("lockTime", LocalDateTime.now());
        message.put("timestamp", System.currentTimeMillis());

        // 广播到警情案例房间
        String roomId = "police_case_" + caseId;
        broadcastToRoom(roomId, message, null);

        // 同时广播到座位房间
        if (seatId != null) {
            String seatRoomId = "seat_" + seatId;
            broadcastToRoom(seatRoomId, message, null);
        }
    }

    /**
     * 通知警情案例解锁
     */
    public void notifyPoliceCaseUnlock(Long caseId, Long userId, String userName, Long seatId) {
        log.info("通知警情案例解锁: caseId={}, userId={}, userName={}, seatId={}", caseId, userId, userName, seatId);

        Map<String, Object> message = new HashMap<>();
        message.put("type", "police_case_unlock");
        message.put("caseId", caseId);
        message.put("unlockUserId", userId);
        message.put("unlockUserName", userName);
        message.put("seatId", seatId);
        message.put("unlockTime", LocalDateTime.now());
        message.put("timestamp", System.currentTimeMillis());

        // 广播到警情案例房间
        String roomId = "police_case_" + caseId;
        broadcastToRoom(roomId, message, null);

        // 同时广播到座位房间
        if (seatId != null) {
            String seatRoomId = "seat_" + seatId;
            broadcastToRoom(seatRoomId, message, null);
        }
    }

    /**
     * 通知警情案例字段同步
     */
    public void notifyPoliceCaseFieldSync(Long caseId, Long userId, String userName, String fieldName, String fieldValue) {
        log.info("通知警情案例字段同步: caseId={}, userId={}, fieldName={}, fieldValue={}",
                 caseId, userId, fieldName, fieldValue);

        Map<String, Object> message = new HashMap<>();
        message.put("type", "police_case_field_sync");
        message.put("caseId", caseId);
        message.put("userId", userId);
        message.put("userName", userName);
        message.put("fieldName", fieldName);
        message.put("fieldValue", fieldValue);
        message.put("timestamp", System.currentTimeMillis());

        // 广播到警情案例房间，但排除发起人
        String roomId = "police_case_" + caseId;
        String excludeSessionId = findUserSessionId(userId);
        broadcastToRoom(roomId, message, excludeSessionId);
    }

    /**
     * 发送WebSocket消息
     */
    public void sendMessage(String messageType, Map<String, Object> data, Long fromUserId, Long toUserId, String action) {
        log.info("发送WebSocket消息: messageType={}, fromUserId={}, toUserId={}, action={}",
                 messageType, fromUserId, toUserId, action);

        Map<String, Object> message = new HashMap<>();
        message.put("type", messageType.toLowerCase());
        message.put("fromUserId", fromUserId);
        message.put("toUserId", toUserId);
        message.put("action", action);
        message.put("data", data);
        message.put("timestamp", System.currentTimeMillis());

        if (toUserId != null) {
            // 点对点消息
            sendToUser(toUserId, message);
        } else {
            // 广播消息，根据数据中的caseId决定房间
            String roomId = "default";
            if (data != null && data.containsKey("caseId")) {
                roomId = "police_case_" + data.get("caseId");
            } else if (data != null && data.containsKey("reportId")) {
                roomId = "police_case_" + data.get("reportId");
            }

            String excludeSessionId = findUserSessionId(fromUserId);
            broadcastToRoom(roomId, message, excludeSessionId);
        }
    }

    /**
     * 广播消息到指定房间
     */
    private void broadcastToRoom(String roomId, Map<String, Object> message, String excludeSessionId) {
        try {
            // 通过反射访问CollaborationWebSocketController的私有字段和方法
            Field roomSessionsField = CollaborationWebSocketController.class.getDeclaredField("roomSessions");
            roomSessionsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Map<String, WebSocketSession>> roomSessions =
                (Map<String, Map<String, WebSocketSession>>) roomSessionsField.get(collaborationWebSocketController);

            Map<String, WebSocketSession> roomSessionMap = roomSessions.get(roomId);
            if (roomSessionMap == null || roomSessionMap.isEmpty()) {
                log.debug("房间 {} 中没有活跃连接", roomId);
                return;
            }

            String json = objectMapper.writeValueAsString(message);
            int sentCount = 0;

            for (Map.Entry<String, WebSocketSession> entry : roomSessionMap.entrySet()) {
                String sessionId = entry.getKey();
                WebSocketSession session = entry.getValue();

                if (excludeSessionId != null && sessionId.equals(excludeSessionId)) {
                    continue;
                }

                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(json));
                        sentCount++;
                    } catch (IOException e) {
                        log.error("发送消息到会话失败: sessionId={}, roomId={}", sessionId, roomId, e);
                    }
                }
            }

            log.info("消息已发送到房间 {}: {} 个连接", roomId, sentCount);

        } catch (Exception e) {
            log.error("广播消息到房间失败: roomId={}", roomId, e);
        }
    }

    /**
     * 发送消息给指定用户
     */
    private void sendToUser(Long userId, Map<String, Object> message) {
        try {
            // 通过反射访问CollaborationWebSocketController的私有字段
            Field roomSessionsField = CollaborationWebSocketController.class.getDeclaredField("roomSessions");
            roomSessionsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Map<String, WebSocketSession>> roomSessions =
                (Map<String, Map<String, WebSocketSession>>) roomSessionsField.get(collaborationWebSocketController);

            Field sessionUsersField = CollaborationWebSocketController.class.getDeclaredField("sessionUsers");
            sessionUsersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionUsers = (Map<String, Object>) sessionUsersField.get(collaborationWebSocketController);

            String json = objectMapper.writeValueAsString(message);
            int sentCount = 0;

            // 遍历所有会话，找到目标用户的连接
            for (Map.Entry<String, Map<String, WebSocketSession>> roomEntry : roomSessions.entrySet()) {
                for (Map.Entry<String, WebSocketSession> sessionEntry : roomEntry.getValue().entrySet()) {
                    String sessionId = sessionEntry.getKey();
                    WebSocketSession session = sessionEntry.getValue();

                    // 检查会话对应的用户ID
                    Object userInfo = sessionUsers.get(sessionId);
                    if (userInfo != null) {
                        try {
                            Field userIdField = userInfo.getClass().getDeclaredField("userId");
                            userIdField.setAccessible(true);
                            String sessionUserId = (String) userIdField.get(userInfo);

                            if (userId.toString().equals(sessionUserId) && session.isOpen()) {
                                session.sendMessage(new TextMessage(json));
                                sentCount++;
                            }
                        } catch (Exception e) {
                            log.debug("检查用户会话失败: sessionId={}", sessionId, e);
                        }
                    }
                }
            }

            log.info("消息已发送给用户 {}: {} 个连接", userId, sentCount);

        } catch (Exception e) {
            log.error("发送消息给用户失败: userId={}", userId, e);
        }
    }

    /**
     * 查找用户的会话ID（用于排除发送者）
     */
    private String findUserSessionId(Long userId) {
        if (userId == null) {
            return null;
        }

        try {
            // 通过反射访问CollaborationWebSocketController的私有字段
            Field sessionUsersField = CollaborationWebSocketController.class.getDeclaredField("sessionUsers");
            sessionUsersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionUsers = (Map<String, Object>) sessionUsersField.get(collaborationWebSocketController);

            // 遍历所有会话，找到匹配的用户ID
            for (Map.Entry<String, Object> entry : sessionUsers.entrySet()) {
                String sessionId = entry.getKey();
                Object userInfo = entry.getValue();

                if (userInfo != null) {
                    try {
                        Field userIdField = userInfo.getClass().getDeclaredField("userId");
                        userIdField.setAccessible(true);
                        String sessionUserId = (String) userIdField.get(userInfo);

                        if (userId.toString().equals(sessionUserId)) {
                            return sessionId;
                        }
                    } catch (Exception e) {
                        log.debug("获取用户会话信息失败: sessionId={}", sessionId, e);
                    }
                }
            }

        } catch (Exception e) {
            log.error("查找用户会话ID失败: userId={}", userId, e);
        }

        return null;
    }

    /**
     * 通知座位状态变更
     */
    public void notifySeatStatusChange(Long seatId, String oldStatus, String newStatus, Long userId, String userName) {
        log.info("通知座位状态变更: seatId={}, {} -> {}, userId={}", seatId, oldStatus, newStatus, userId);

        // 使用专门的座位WebSocket控制器进行广播
        seatWebSocketController.broadcastSeatStatusChange(
            seatId.toString(),
            oldStatus,
            newStatus,
            userId != null ? userId.toString() : "unknown",
            userName != null ? userName : "unknown"
        );

        // 同时通过协作WebSocket广播（兼容现有系统）
        Map<String, Object> message = new HashMap<>();
        message.put("type", "seat_status_change");
        message.put("seatId", seatId);
        message.put("oldStatus", oldStatus);
        message.put("newStatus", newStatus);
        message.put("userId", userId);
        message.put("userName", userName);
        message.put("changeTime", LocalDateTime.now());
        message.put("timestamp", System.currentTimeMillis());

        // 广播到座位管理房间
        broadcastToRoom("seat_management", message, null);

        // 同时广播到具体座位房间
        String seatRoomId = "seat_" + seatId;
        broadcastToRoom(seatRoomId, message, null);
    }

    /**
     * 通知座位预约状态变更
     */
    public void notifySeatReservationChange(Long seatId, String action, Long userId, String userName,
                                          LocalDateTime startTime, LocalDateTime endTime) {
        log.info("通知座位预约变更: seatId={}, action={}, userId={}", seatId, action, userId);

        // 使用专门的座位WebSocket控制器进行广播
        seatWebSocketController.broadcastSeatReservationChange(
            seatId.toString(),
            action,
            userId != null ? userId.toString() : "unknown",
            userName != null ? userName : "unknown"
        );

        // 同时通过协作WebSocket广播（兼容现有系统）
        Map<String, Object> message = new HashMap<>();
        message.put("type", "seat_reservation_change");
        message.put("seatId", seatId);
        message.put("action", action); // reserve, cancel, expire
        message.put("userId", userId);
        message.put("userName", userName);
        message.put("startTime", startTime);
        message.put("endTime", endTime);
        message.put("timestamp", System.currentTimeMillis());

        // 广播到座位管理房间
        broadcastToRoom("seat_management", message, null);

        // 同时广播到具体座位房间
        String seatRoomId = "seat_" + seatId;
        broadcastToRoom(seatRoomId, message, null);
    }
}