package net.lab1024.sa.admin.module.business.collaboration.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketMessage;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketSession;
import net.lab1024.sa.admin.module.support.websocket.handler.UnifiedWebSocketHandler;
import net.lab1024.sa.admin.module.support.websocket.service.WebSocketSessionManager;
import net.lab1024.sa.admin.module.support.websocket.service.impl.WebSocketTransport;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 协作WebSocket处理器
 * 处理协作模块的实时消息
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-26
 * @Copyright 1024创新实验室
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollaborationWebSocketHandler implements UnifiedWebSocketHandler.MessageHandler {

    private final UnifiedWebSocketHandler unifiedWebSocketHandler;
    private final WebSocketSessionManager sessionManager;
    private final WebSocketTransport webSocketTransport;

    @PostConstruct
    public void init() {
        // 注册协作模块的消息处理器
        unifiedWebSocketHandler.registerMessageHandler("collaboration", this);
        log.info("协作WebSocket处理器已注册");
    }

    @Override
    public void handleMessage(String sessionId, WebSocketMessage message) throws Exception {
        String type = message.getType();
        Map<String, Object> data = message.getData();

        log.info("处理协作消息: type={}, sessionId={}, fromUser={}",
            type, sessionId, message.getFromUserName());

        switch (type) {
            case "JOIN_DOCUMENT":
                handleJoinDocument(sessionId, message, data);
                break;
            case "LEAVE_DOCUMENT":
                handleLeaveDocument(sessionId, message, data);
                break;
            case "FIELD_EDIT":
                handleFieldEdit(sessionId, message, data);
                break;
            case "FIELD_FOCUS":
                handleFieldFocus(sessionId, message, data);
                break;
            case "FIELD_BLUR":
                handleFieldBlur(sessionId, message, data);
                break;
            case "CURSOR_MOVE":
                handleCursorMove(sessionId, message, data);
                break;
            case "TEXT_SELECTION":
                handleTextSelection(sessionId, message, data);
                break;
            case "SAVE_DOCUMENT":
                handleSaveDocument(sessionId, message, data);
                break;
            default:
                log.warn("未知的协作消息类型: {}", type);
        }
    }

    /**
     * 处理加入文档协作
     */
    private void handleJoinDocument(String sessionId, WebSocketMessage message, Map<String, Object> data) {
        Object documentIdObj = data.get("documentId");
        String documentType = (String) data.getOrDefault("documentType", "document");

        if (documentIdObj == null) {
            log.warn("加入文档协作缺少documentId参数");
            return;
        }

        String collaborationRoom = "collaboration_" + documentType + "_" + documentIdObj;

        // 订阅协作房间
        sessionManager.subscribeRoom(sessionId, collaborationRoom);

        // 获取房间内现有用户列表
        List<WebSocketSession> roomSessions = sessionManager.getRoomSessions(collaborationRoom);
        List<Map<String, Object>> currentUsers = roomSessions.stream()
            .filter(s -> !s.getSessionId().equals(sessionId))
            .map(s -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("userId", s.getUserId());
                userMap.put("userName", s.getUsername());
                userMap.put("timestamp", System.currentTimeMillis());
                return userMap;
            })
            .toList();

        // 发送用户列表给新加入的用户
        if (!currentUsers.isEmpty()) {
            Map<String, Object> userListData = Map.of("users", currentUsers);
            WebSocketMessage userListMessage = WebSocketMessage.business("collaboration", "USER_LIST", userListData);
            webSocketTransport.sendToSession(sessionId, userListMessage);
        }

        // 通知房间内其他用户有新用户加入
        Map<String, Object> joinData = Map.of(
            "userId", message.getFromUserId(),
            "userName", message.getFromUserName(),
            "documentId", documentIdObj,
            "documentType", documentType,
            "timestamp", System.currentTimeMillis()
        );

        WebSocketMessage joinMessage = WebSocketMessage.business("collaboration", "USER_JOIN", joinData)
            .setFromUserId(message.getFromUserId())
            .setFromUserName(message.getFromUserName());

        // 广播给房间内其他用户（排除自己）
        sendToRoomExcludeSelf(collaborationRoom, sessionId, joinMessage);

        log.info("用户{}加入文档{}协作", message.getFromUserName(), documentIdObj);
    }

    /**
     * 处理离开文档协作
     */
    private void handleLeaveDocument(String sessionId, WebSocketMessage message, Map<String, Object> data) {
        Object documentIdObj = data.get("documentId");
        String documentType = (String) data.getOrDefault("documentType", "document");

        if (documentIdObj == null) {
            return;
        }

        String collaborationRoom = "collaboration_" + documentType + "_" + documentIdObj;

        // 通知房间内其他用户有用户离开
        Map<String, Object> leaveData = Map.of(
            "userId", message.getFromUserId(),
            "userName", message.getFromUserName(),
            "documentId", documentIdObj,
            "timestamp", System.currentTimeMillis()
        );

        WebSocketMessage leaveMessage = WebSocketMessage.business("collaboration", "USER_LEAVE", leaveData)
            .setFromUserId(message.getFromUserId())
            .setFromUserName(message.getFromUserName());

        // 广播给房间内其他用户（排除自己）
        sendToRoomExcludeSelf(collaborationRoom, sessionId, leaveMessage);

        // 取消订阅协作房间
        sessionManager.unsubscribeRoom(sessionId, collaborationRoom);

        log.info("用户{}离开文档{}协作", message.getFromUserName(), documentIdObj);
    }

    /**
     * 处理字段编辑
     */
    private void handleFieldEdit(String sessionId, WebSocketMessage message, Map<String, Object> data) {
        Object documentIdObj = data.get("documentId");
        String fieldName = (String) data.get("fieldName");

        if (documentIdObj == null || fieldName == null) {
            log.warn("字段编辑消息缺少必要参数: documentId={}, fieldName={}", documentIdObj, fieldName);
            return;
        }

        String collaborationRoom = "collaboration_document_" + documentIdObj;

        // 创建字段编辑消息
        Map<String, Object> editData = Map.of(
            "type", "edit",
            "userId", message.getFromUserId(),
            "userName", message.getFromUserName(),
            "targetId", fieldName,
            "targetType", "field",
            "data", Map.of(
                "value", data.get("value"),
                "selectionStart", data.get("selectionStart"),
                "selectionEnd", data.get("selectionEnd")
            ),
            "timestamp", data.getOrDefault("timestamp", System.currentTimeMillis())
        );

        WebSocketMessage editMessage = WebSocketMessage.business("collaboration", "FIELD_EDIT", editData)
            .setFromUserId(message.getFromUserId())
            .setFromUserName(message.getFromUserName());

        // 广播给房间内其他用户（排除自己）
        sendToRoomExcludeSelf(collaborationRoom, sessionId, editMessage);

        log.debug("用户{}编辑字段{}: {}", message.getFromUserName(), fieldName, data.get("value"));
    }

    /**
     * 处理字段聚焦
     */
    private void handleFieldFocus(String sessionId, WebSocketMessage message, Map<String, Object> data) {
        Object documentIdObj = data.get("documentId");
        String fieldName = (String) data.get("fieldName");

        if (documentIdObj == null || fieldName == null) {
            return;
        }

        String collaborationRoom = "collaboration_document_" + documentIdObj;

        Map<String, Object> focusData = Map.of(
            "type", "focus",
            "userId", message.getFromUserId(),
            "userName", message.getFromUserName(),
            "targetId", fieldName,
            "targetType", "field",
            "data", data.get("cursorPosition"),
            "timestamp", data.getOrDefault("timestamp", System.currentTimeMillis())
        );

        WebSocketMessage focusMessage = WebSocketMessage.business("collaboration", "FIELD_FOCUS", focusData)
            .setFromUserId(message.getFromUserId())
            .setFromUserName(message.getFromUserName());

        // 广播给房间内其他用户（排除自己）
        sendToRoomExcludeSelf(collaborationRoom, sessionId, focusMessage);

        log.debug("用户{}聚焦字段{}", message.getFromUserName(), fieldName);
    }

    /**
     * 处理字段失去焦点
     */
    private void handleFieldBlur(String sessionId, WebSocketMessage message, Map<String, Object> data) {
        Object documentIdObj = data.get("documentId");
        String fieldName = (String) data.get("fieldName");

        if (documentIdObj == null || fieldName == null) {
            return;
        }

        String collaborationRoom = "collaboration_document_" + documentIdObj;

        Map<String, Object> blurData = Map.of(
            "type", "blur",
            "userId", message.getFromUserId(),
            "userName", message.getFromUserName(),
            "targetId", fieldName,
            "targetType", "field",
            "timestamp", data.getOrDefault("timestamp", System.currentTimeMillis())
        );

        WebSocketMessage blurMessage = WebSocketMessage.business("collaboration", "FIELD_BLUR", blurData)
            .setFromUserId(message.getFromUserId())
            .setFromUserName(message.getFromUserName());

        // 广播给房间内其他用户（排除自己）
        sendToRoomExcludeSelf(collaborationRoom, sessionId, blurMessage);

        log.debug("用户{}离开字段{}", message.getFromUserName(), fieldName);
    }

    /**
     * 处理光标移动
     */
    private void handleCursorMove(String sessionId, WebSocketMessage message, Map<String, Object> data) {
        Object documentIdObj = data.get("documentId");
        String fieldName = (String) data.get("fieldName");

        if (documentIdObj == null || fieldName == null) {
            return;
        }

        String collaborationRoom = "collaboration_document_" + documentIdObj;

        @SuppressWarnings("unchecked")
        Map<String, Object> position = (Map<String, Object>) data.get("position");

        Map<String, Object> cursorData = Map.of(
            "userId", message.getFromUserId(),
            "userName", message.getFromUserName(),
            "targetId", fieldName,
            "x", position.get("x"),
            "y", position.get("y"),
            "timestamp", data.getOrDefault("timestamp", System.currentTimeMillis())
        );

        WebSocketMessage cursorMessage = WebSocketMessage.business("collaboration", "CURSOR_MOVE", cursorData)
            .setFromUserId(message.getFromUserId())
            .setFromUserName(message.getFromUserName());

        // 广播给房间内其他用户（排除自己）
        sendToRoomExcludeSelf(collaborationRoom, sessionId, cursorMessage);
    }

    /**
     * 处理文本选择
     */
    private void handleTextSelection(String sessionId, WebSocketMessage message, Map<String, Object> data) {
        Object documentIdObj = data.get("documentId");
        String fieldName = (String) data.get("fieldName");

        if (documentIdObj == null || fieldName == null) {
            return;
        }

        String collaborationRoom = "collaboration_document_" + documentIdObj;

        WebSocketMessage selectionMessage = WebSocketMessage.business("collaboration", "TEXT_SELECTION", data)
            .setFromUserId(message.getFromUserId())
            .setFromUserName(message.getFromUserName());

        // 广播给房间内其他用户（排除自己）
        sendToRoomExcludeSelf(collaborationRoom, sessionId, selectionMessage);
    }

    /**
     * 处理文档保存
     */
    private void handleSaveDocument(String sessionId, WebSocketMessage message, Map<String, Object> data) {
        Object documentIdObj = data.get("documentId");

        if (documentIdObj == null) {
            return;
        }

        String collaborationRoom = "collaboration_document_" + documentIdObj;

        WebSocketMessage saveMessage = WebSocketMessage.business("collaboration", "DOCUMENT_SAVE", data)
            .setFromUserId(message.getFromUserId())
            .setFromUserName(message.getFromUserName());

        // 广播给房间内其他用户（排除自己）
        sendToRoomExcludeSelf(collaborationRoom, sessionId, saveMessage);

        log.info("用户{}保存文档{}", message.getFromUserName(), documentIdObj);
    }

    /**
     * 向房间发送消息，排除指定会话
     */
    private void sendToRoomExcludeSelf(String room, String excludeSessionId, WebSocketMessage message) {
        List<WebSocketSession> sessions = sessionManager.getRoomSessions(room);
        for (WebSocketSession session : sessions) {
            if (!session.getSessionId().equals(excludeSessionId)) {
                webSocketTransport.sendToSession(session.getSessionId(), message);
            }
        }
    }
}