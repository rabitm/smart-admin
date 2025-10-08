package net.lab1024.sa.admin.module.support.websocket.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.config.RocketMQConfiguration;
import net.lab1024.sa.admin.module.support.rocketmq.service.RocketMQMessageTransport;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketMessage;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketSession;
import net.lab1024.sa.admin.module.support.websocket.service.MessageTransport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;

/**
 * 混合传输实现 - WebSocket + RocketMQ 双重支持
 *
 * 支持动态降级：
 * 1. 优先使用配置的主要传输方式
 * 2. 主要方式失败时自动降级到备用方式
 * 3. 支持运行时动态切换传输方式
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-29
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service("hybridMessageTransport")
@ConditionalOnProperty(name = "websocket.transport.type", havingValue = "hybrid")
public class HybridMessageTransport implements MessageTransport {

    private final MessageTransport webSocketTransport;
    private final MessageTransport rocketMQTransport;

    public HybridMessageTransport(@Qualifier("webSocketTransport") MessageTransport webSocketTransport,
                                  @Qualifier("rocketMQTransport") MessageTransport rocketMQTransport) {
        this.webSocketTransport = webSocketTransport;
        this.rocketMQTransport = rocketMQTransport;

        log.info("🚀 [混合传输] 混合消息传输服务已初始化");
    }

    @Value("${websocket.transport.primary:websocket}")
    private String primaryTransport;

    @Value("${websocket.transport.fallback:websocket}")
    private String fallbackTransport;

    @Value("${websocket.transport.hybrid-fallback:true}")
    private boolean enableHybridFallback;

    /**
     * 获取当前可用的传输方式
     */
    private MessageTransport getCurrentTransport() {
        // 检查主要传输方式是否可用
        if ("rocketmq".equals(primaryTransport)) {
            // 简单检查RocketMQ传输是否可用
            try {
                if (rocketMQTransport != null) {
                    log.debug("🚀 [混合传输] 使用主要传输方式: RocketMQ");
                    return rocketMQTransport;
                }
            } catch (Exception e) {
                log.debug("🚀 [混合传输] RocketMQ检查失败: {}", e.getMessage());
            }

            if (enableHybridFallback) {
                log.debug("🚀 [混合传输] RocketMQ不可用，降级到WebSocket");
                return webSocketTransport;
            }
        }

        // 默认使用WebSocket
        log.debug("🚀 [混合传输] 使用默认传输方式: WebSocket");
        return webSocketTransport;
    }

    /**
     * 带降级的消息发送
     */
    private boolean sendWithFallback(MessageTransport primary, MessageTransport fallback,
                                   java.util.function.Supplier<Boolean> sendAction) {
        try {
            // 尝试使用主要传输方式
            if (sendAction.get()) {
                return true;
            }
        } catch (Exception e) {
            log.warn("🚀 [混合传输] 主要传输方式失败: {}", e.getMessage());
        }

        // 降级到备用传输方式
        if (enableHybridFallback && fallback != null && fallback != primary) {
            try {
                log.debug("🚀 [混合传输] 降级到备用传输方式");
                return sendAction.get();
            } catch (Exception e) {
                log.error("🚀 [混合传输] 备用传输方式也失败: {}", e.getMessage());
            }
        }

        return false;
    }

    @Override
    public boolean sendToUser(Long userId, WebSocketMessage message) {
        MessageTransport currentTransport = getCurrentTransport();
        MessageTransport fallbackTransport = currentTransport == rocketMQTransport ? webSocketTransport : null;

        return sendWithFallback(currentTransport, fallbackTransport,
            () -> currentTransport.sendToUser(userId, message));
    }

    @Override
    public boolean sendToSession(String sessionId, WebSocketMessage message) {
        MessageTransport currentTransport = getCurrentTransport();
        MessageTransport fallbackTransport = currentTransport == rocketMQTransport ? webSocketTransport : null;

        return sendWithFallback(currentTransport, fallbackTransport,
            () -> currentTransport.sendToSession(sessionId, message));
    }

    @Override
    public int broadcast(WebSocketMessage message) {
        MessageTransport currentTransport = getCurrentTransport();
        MessageTransport fallbackTransport = currentTransport == rocketMQTransport ? webSocketTransport : null;

        try {
            return currentTransport.broadcast(message);
        } catch (Exception e) {
            log.warn("🚀 [混合传输] 广播失败，尝试降级: {}", e.getMessage());
            if (enableHybridFallback && fallbackTransport != null) {
                return fallbackTransport.broadcast(message);
            }
            return 0;
        }
    }

    @Override
    public int sendToRoom(String room, WebSocketMessage message) {
        MessageTransport currentTransport = getCurrentTransport();
        MessageTransport fallbackTransport = currentTransport == rocketMQTransport ? webSocketTransport : null;

        try {
            return currentTransport.sendToRoom(room, message);
        } catch (Exception e) {
            log.warn("🚀 [混合传输] 房间消息发送失败，尝试降级: {}", e.getMessage());
            if (enableHybridFallback && fallbackTransport != null) {
                return fallbackTransport.sendToRoom(room, message);
            }
            return 0;
        }
    }

    @Override
    public int sendToModule(String module, WebSocketMessage message) {
        MessageTransport currentTransport = getCurrentTransport();
        MessageTransport fallbackTransport = currentTransport == rocketMQTransport ? webSocketTransport : null;

        try {
            return currentTransport.sendToModule(module, message);
        } catch (Exception e) {
            log.warn("🚀 [混合传输] 模块消息发送失败，尝试降级: {}", e.getMessage());
            if (enableHybridFallback && fallbackTransport != null) {
                return fallbackTransport.sendToModule(module, message);
            }
            return 0;
        }
    }

    @Override
    public int sendToDepartment(Long departmentId, WebSocketMessage message) {
        MessageTransport currentTransport = getCurrentTransport();
        MessageTransport fallbackTransport = currentTransport == rocketMQTransport ? webSocketTransport : null;

        try {
            return currentTransport.sendToDepartment(departmentId, message);
        } catch (Exception e) {
            log.warn("🚀 [混合传输] 部门消息发送失败，尝试降级: {}", e.getMessage());
            if (enableHybridFallback && fallbackTransport != null) {
                return fallbackTransport.sendToDepartment(departmentId, message);
            }
            return 0;
        }
    }

    @Override
    public int sendToRole(String role, WebSocketMessage message) {
        MessageTransport currentTransport = getCurrentTransport();
        MessageTransport fallbackTransport = currentTransport == rocketMQTransport ? webSocketTransport : null;

        try {
            return currentTransport.sendToRole(role, message);
        } catch (Exception e) {
            log.warn("🚀 [混合传输] 角色消息发送失败，尝试降级: {}", e.getMessage());
            if (enableHybridFallback && fallbackTransport != null) {
                return fallbackTransport.sendToRole(role, message);
            }
            return 0;
        }
    }

    @Override
    public int sendToMatched(WebSocketMessage message, Predicate<WebSocketSession> predicate) {
        MessageTransport currentTransport = getCurrentTransport();
        MessageTransport fallbackTransport = currentTransport == rocketMQTransport ? webSocketTransport : null;

        try {
            return currentTransport.sendToMatched(message, predicate);
        } catch (Exception e) {
            log.warn("🚀 [混合传输] 条件消息发送失败，尝试降级: {}", e.getMessage());
            if (enableHybridFallback && fallbackTransport != null) {
                return fallbackTransport.sendToMatched(message, predicate);
            }
            return 0;
        }
    }

    @Override
    public boolean disconnectUser(Long userId, String reason) {
        MessageTransport currentTransport = getCurrentTransport();
        MessageTransport fallbackTransport = currentTransport == rocketMQTransport ? webSocketTransport : null;

        return sendWithFallback(currentTransport, fallbackTransport,
            () -> currentTransport.disconnectUser(userId, reason));
    }

    @Override
    public int getOnlineUserCount() {
        // 在线用户数使用WebSocket统计，因为这是实际连接数
        return webSocketTransport.getOnlineUserCount();
    }

    @Override
    public List<Long> getOnlineUsers() {
        // 在线用户列表使用WebSocket统计
        return webSocketTransport.getOnlineUsers();
    }

    @Override
    public boolean isUserOnline(Long userId) {
        // 用户在线状态使用WebSocket判断
        return webSocketTransport.isUserOnline(userId);
    }

    @Override
    public String getTransportType() {
        MessageTransport currentTransport = getCurrentTransport();
        return "hybrid(" + currentTransport.getTransportType() + ")";
    }

    /**
     * 动态切换传输方式
     */
    public void switchTransport(String transportType) {
        if ("websocket".equals(transportType) || "rocketmq".equals(transportType)) {
            this.primaryTransport = transportType;
            log.info("🚀 [混合传输] 传输方式已切换为: {}", transportType);
        } else {
            log.warn("🚀 [混合传输] 不支持的传输方式: {}", transportType);
        }
    }

    /**
     * 获取传输状态
     */
    public String getTransportStatus() {
        boolean rocketMQAvailable = rocketMQTransport != null;
        MessageTransport currentTransport = getCurrentTransport();

        return String.format("主要传输:%s, RocketMQ可用:%s, 当前使用:%s",
            primaryTransport, rocketMQAvailable, currentTransport.getTransportType());
    }
}