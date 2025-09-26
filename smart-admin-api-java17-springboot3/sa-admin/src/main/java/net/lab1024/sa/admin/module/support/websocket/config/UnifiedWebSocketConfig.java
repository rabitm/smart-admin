package net.lab1024.sa.admin.module.support.websocket.config;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.module.support.websocket.handler.UnifiedWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 统一WebSocket配置
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class UnifiedWebSocketConfig implements WebSocketConfigurer {

    private final UnifiedWebSocketHandler unifiedWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册统一WebSocket处理器（原生WebSocket，不使用SockJS）
        registry.addHandler(unifiedWebSocketHandler, "/api/websocket/unified")
                .setAllowedOriginPatterns("*");

        // 为了兼容现有系统，保留原有路径
        registry.addHandler(unifiedWebSocketHandler, "/api/websocket/seat")
                .setAllowedOriginPatterns("*");

        registry.addHandler(unifiedWebSocketHandler, "/api/collaboration/ws")
                .setAllowedOriginPatterns("*");
    }
}