package net.lab1024.sa.admin.module.business.oa.seat.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.oa.seat.service.SeatService;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketMessage;
import net.lab1024.sa.admin.module.support.websocket.handler.UnifiedWebSocketHandler;
import net.lab1024.sa.admin.module.support.websocket.service.MessageTransport;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * 座位管理WebSocket消息处理器
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatWebSocketHandler implements UnifiedWebSocketHandler.MessageHandler {

    private final UnifiedWebSocketHandler unifiedWebSocketHandler;
    private final MessageTransport messageTransport;
    private final SeatService seatService;

    @PostConstruct
    public void init() {
        // 注册座位模块的消息处理器
        unifiedWebSocketHandler.registerMessageHandler("seat", this);
        log.info("座位管理WebSocket处理器已注册");
    }

    @Override
    public void handleMessage(String sessionId, WebSocketMessage message) throws Exception {
        String type = message.getType();
        Map<String, Object> data = message.getData();

        log.debug("处理座位模块消息: sessionId={}, type={}", sessionId, type);

        switch (type) {
            case "SEAT_STATUS_QUERY":
                handleSeatStatusQuery(sessionId, message);
                break;

            case "SEAT_RESERVE":
                handleSeatReserve(sessionId, message);
                break;

            case "SEAT_RELEASE":
                handleSeatRelease(sessionId, message);
                break;

            case "SEAT_OCCUPY":
                handleSeatOccupy(sessionId, message);
                break;

            case "SEAT_LIST_QUERY":
                handleSeatListQuery(sessionId, message);
                break;

            default:
                log.warn("未处理的座位消息类型: {}", type);
                sendErrorResponse(sessionId, message, "不支持的消息类型: " + type);
        }
    }

    /**
     * 处理座位状态查询
     */
    private void handleSeatStatusQuery(String sessionId, WebSocketMessage message) {
        try {
            Map<String, Object> data = message.getData();
            Long seatId = Long.valueOf(data.get("seatId").toString());

            // 查询座位状态（这里简化处理，实际应调用service）
            Map<String, Object> seatStatus = Map.of(
                "seatId", seatId,
                "status", "available",
                "currentUser", "",
                "reservedUser", "",
                "lastUpdateTime", System.currentTimeMillis()
            );

            // 发送响应
            WebSocketMessage response = WebSocketMessage.business("seat", "SEAT_STATUS_RESPONSE", seatStatus)
                .setFromUserId(message.getFromUserId())
                .setFromUserName(message.getFromUserName());

            messageTransport.sendToSession(sessionId, response);

            log.debug("座位状态查询完成: seatId={}", seatId);

        } catch (Exception e) {
            log.error("处理座位状态查询失败", e);
            sendErrorResponse(sessionId, message, "查询座位状态失败: " + e.getMessage());
        }
    }

    /**
     * 处理座位预约
     */
    private void handleSeatReserve(String sessionId, WebSocketMessage message) {
        try {
            Map<String, Object> data = message.getData();
            Long seatId = Long.valueOf(data.get("seatId").toString());
            Long userId = message.getFromUserId();
            String userName = message.getFromUserName();

            log.info("用户{}({})请求预约座位: {}", userName, userId, seatId);

            // TODO: 调用实际的座位预约业务逻辑
            // Result<Void> result = seatService.reserveSeat(seatId, userId);

            // 模拟预约成功
            boolean success = true;

            if (success) {
                // 发送预约成功响应
                Map<String, Object> responseData = Map.of(
                    "seatId", seatId,
                    "userId", userId,
                    "userName", userName,
                    "operation", "reserve",
                    "result", "success",
                    "timestamp", System.currentTimeMillis()
                );

                WebSocketMessage response = WebSocketMessage.business("seat", "SEAT_OPERATION_RESPONSE", responseData)
                    .setFromUserId(userId)
                    .setFromUserName(userName);

                messageTransport.sendToSession(sessionId, response);

                // 广播座位状态变更
                broadcastSeatStatusChange(seatId, "available", "reserved", userId, userName);

            } else {
                sendErrorResponse(sessionId, message, "座位预约失败，可能已被占用");
            }

        } catch (Exception e) {
            log.error("处理座位预约失败", e);
            sendErrorResponse(sessionId, message, "座位预约失败: " + e.getMessage());
        }
    }

    /**
     * 处理座位释放
     */
    private void handleSeatRelease(String sessionId, WebSocketMessage message) {
        try {
            Map<String, Object> data = message.getData();
            Long seatId = Long.valueOf(data.get("seatId").toString());
            Long userId = message.getFromUserId();
            String userName = message.getFromUserName();

            log.info("用户{}({})请求释放座位: {}", userName, userId, seatId);

            // TODO: 调用实际的座位释放业务逻辑
            // Result<Void> result = seatService.releaseSeat(seatId, userId);

            // 模拟释放成功
            boolean success = true;

            if (success) {
                // 发送释放成功响应
                Map<String, Object> responseData = Map.of(
                    "seatId", seatId,
                    "userId", userId,
                    "userName", userName,
                    "operation", "release",
                    "result", "success",
                    "timestamp", System.currentTimeMillis()
                );

                WebSocketMessage response = WebSocketMessage.business("seat", "SEAT_OPERATION_RESPONSE", responseData)
                    .setFromUserId(userId)
                    .setFromUserName(userName);

                messageTransport.sendToSession(sessionId, response);

                // 广播座位状态变更
                broadcastSeatStatusChange(seatId, "occupied", "available", userId, userName);

            } else {
                sendErrorResponse(sessionId, message, "座位释放失败，您可能没有权限");
            }

        } catch (Exception e) {
            log.error("处理座位释放失败", e);
            sendErrorResponse(sessionId, message, "座位释放失败: " + e.getMessage());
        }
    }

    /**
     * 处理座位占用
     */
    private void handleSeatOccupy(String sessionId, WebSocketMessage message) {
        try {
            Map<String, Object> data = message.getData();
            Long seatId = Long.valueOf(data.get("seatId").toString());
            Long userId = message.getFromUserId();
            String userName = message.getFromUserName();

            log.info("用户{}({})请求占用座位: {}", userName, userId, seatId);

            // TODO: 调用实际的座位占用业务逻辑
            // Result<Void> result = seatService.occupySeat(seatId, userId);

            // 模拟占用成功
            boolean success = true;

            if (success) {
                // 发送占用成功响应
                Map<String, Object> responseData = Map.of(
                    "seatId", seatId,
                    "userId", userId,
                    "userName", userName,
                    "operation", "occupy",
                    "result", "success",
                    "timestamp", System.currentTimeMillis()
                );

                WebSocketMessage response = WebSocketMessage.business("seat", "SEAT_OPERATION_RESPONSE", responseData)
                    .setFromUserId(userId)
                    .setFromUserName(userName);

                messageTransport.sendToSession(sessionId, response);

                // 广播座位状态变更
                broadcastSeatStatusChange(seatId, "reserved", "occupied", userId, userName);

            } else {
                sendErrorResponse(sessionId, message, "座位占用失败，座位可能不可用");
            }

        } catch (Exception e) {
            log.error("处理座位占用失败", e);
            sendErrorResponse(sessionId, message, "座位占用失败: " + e.getMessage());
        }
    }

    /**
     * 处理座位列表查询
     */
    private void handleSeatListQuery(String sessionId, WebSocketMessage message) {
        try {
            // TODO: 调用实际的座位列表查询业务逻辑
            // List<SeatVO> seats = seatService.getAllSeats();

            // 模拟座位列表数据
            Map<String, Object> seatList = Map.of(
                "seats", java.util.List.of(
                    Map.of("seatId", 1001, "seatCode", "A001", "status", "available", "currentUser", ""),
                    Map.of("seatId", 1002, "seatCode", "A002", "status", "occupied", "currentUser", "张三"),
                    Map.of("seatId", 1003, "seatCode", "A003", "status", "reserved", "currentUser", "李四")
                ),
                "total", 3,
                "timestamp", System.currentTimeMillis()
            );

            WebSocketMessage response = WebSocketMessage.business("seat", "SEAT_LIST_RESPONSE", seatList)
                .setFromUserId(message.getFromUserId())
                .setFromUserName(message.getFromUserName());

            messageTransport.sendToSession(sessionId, response);

            log.debug("座位列表查询完成");

        } catch (Exception e) {
            log.error("处理座位列表查询失败", e);
            sendErrorResponse(sessionId, message, "查询座位列表失败: " + e.getMessage());
        }
    }

    /**
     * 广播座位状态变更
     */
    private void broadcastSeatStatusChange(Long seatId, String oldStatus, String newStatus, Long userId, String userName) {
        Map<String, Object> data = Map.of(
            "seatId", seatId,
            "oldStatus", oldStatus,
            "newStatus", newStatus,
            "userId", userId,
            "userName", userName,
            "timestamp", System.currentTimeMillis()
        );

        WebSocketMessage broadcast = WebSocketMessage.broadcast("seat", "SEAT_STATUS_CHANGED", data)
            .setFromUserId(userId)
            .setFromUserName(userName);

        int count = messageTransport.sendToModule("seat", broadcast);
        log.debug("广播座位状态变更: seatId={}, {} -> {}, 发送给{}个用户",
                 seatId, oldStatus, newStatus, count);
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(String sessionId, WebSocketMessage originalMessage, String errorMsg) {
        WebSocketMessage errorResponse = WebSocketMessage.business("seat", "ERROR",
            Map.of("error", errorMsg, "originalType", originalMessage.getType()))
            .setFromUserId(originalMessage.getFromUserId())
            .setFromUserName(originalMessage.getFromUserName());

        messageTransport.sendToSession(sessionId, errorResponse);
    }
}