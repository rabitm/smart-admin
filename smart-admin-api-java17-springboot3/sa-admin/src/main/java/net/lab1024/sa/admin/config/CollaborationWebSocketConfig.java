package net.lab1024.sa.admin.config;

import net.lab1024.sa.admin.module.business.collaboration.controller.CollaborationWebSocketController;
import net.lab1024.sa.admin.module.business.oa.seat.controller.SeatWebSocketController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 协作WebSocket配置
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@Configuration
@EnableWebSocket
public class CollaborationWebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private CollaborationWebSocketController collaborationWebSocketController;

    @Autowired
    private SeatWebSocketController seatWebSocketController;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册协作WebSocket处理器
        registry.addHandler(collaborationWebSocketController, "/api/collaboration/ws")
                .setAllowedOriginPatterns("*") // 使用allowedOriginPatterns替代allowedOrigins
                .withSockJS(); // 支持SockJS降级

        // 注册座位管理WebSocket处理器
        registry.addHandler(seatWebSocketController, "/api/websocket/seat")
                .setAllowedOriginPatterns("*") // 使用allowedOriginPatterns替代allowedOrigins
                .withSockJS(); // 支持SockJS降级
    }
}