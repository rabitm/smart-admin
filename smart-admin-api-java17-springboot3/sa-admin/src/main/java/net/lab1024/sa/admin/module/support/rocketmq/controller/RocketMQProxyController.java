package net.lab1024.sa.admin.module.support.rocketmq.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.rocketmq.domain.RocketMQMessage;
import net.lab1024.sa.admin.module.support.rocketmq.service.RocketMQMessageTransport;
import net.lab1024.sa.admin.module.support.websocket.service.WebSocketSessionManager;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RocketMQ HTTP代理控制器
 * 为前端提供HTTP接口访问RocketMQ
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-29
 * @Copyright 1024创新实验室
 */
@Slf4j
@RestController
@RequestMapping("/rocketmq")
@ConditionalOnProperty(name = "rocketmq.name-server")
public class RocketMQProxyController {

    private final RocketMQTemplate rocketMQTemplate;
    private final RocketMQMessageTransport rocketMQTransport;
    private final WebSocketSessionManager sessionManager;

    public RocketMQProxyController(RocketMQTemplate rocketMQTemplate,
                                  RocketMQMessageTransport rocketMQTransport,
                                  WebSocketSessionManager sessionManager) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.rocketMQTransport = rocketMQTransport;
        this.sessionManager = sessionManager;
    }

    // SSE连接管理
    private final Map<String, SseEmitter> sseConnections = new ConcurrentHashMap<>();

    /**
     * 发送消息接口
     */
    @PostMapping("/send")
    public ResponseDTO<Map<String, Object>> sendMessage(@RequestBody RocketMQSendRequest request,
                                                       HttpServletRequest httpRequest) {
        try {
            // 获取用户信息
            String userIdHeader = httpRequest.getHeader("X-User-Id");
            String userNameHeader = httpRequest.getHeader("X-User-Name");
            String sessionId = httpRequest.getHeader("X-Session-Id");

            // 构建RocketMQ消息
            RocketMQMessage message = new RocketMQMessage();
            message.setMessageId(generateMessageId())
                   .setTopic(request.getTopic())
                   .setTag(request.getTag())
                   .setType(request.getType())
                   .setModule(request.getModule())
                   .setKeys(request.getKeys() != null ? request.getKeys() : message.getMessageId())
                   .setData(request.getData())
                   .setContent(request.getContent())
                   .setOrderly(request.getOrderly() != null ? request.getOrderly() : false)
                   .setDelayLevel(request.getDelayLevel() != null ? request.getDelayLevel() : 0)
                   .setTimestamp(Instant.now());

            // 设置发送者信息
            if (userIdHeader != null) {
                message.setFromUserId(Long.parseLong(userIdHeader));
            }
            if (userNameHeader != null) {
                message.setFromUserName(userNameHeader);
            }

            // 发送消息
            String destination = message.getDestination();

            if (message.getOrderly()) {
                // 顺序消息
                String orderId = request.getOrderId() != null ? request.getOrderId() : sessionId;
                rocketMQTemplate.syncSendOrderly(destination, message, orderId);
            } else if (message.getDelayLevel() > 0) {
                // 延迟消息
                rocketMQTemplate.syncSend(destination, message, 3000);
            } else {
                // 普通消息
                rocketMQTemplate.convertAndSend(destination, message);
            }

            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("messageId", message.getMessageId());
            result.put("topic", message.getTopic());
            result.put("tag", message.getTag());
            result.put("timestamp", message.getTimestamp());

            log.debug("🚀 [HTTP代理] 消息发送成功: topic={}, tag={}, type={}",
                     message.getTopic(), message.getTag(), message.getType());

            return ResponseDTO.ok(result);

        } catch (Exception e) {
            log.error("🚀 [HTTP代理] 消息发送失败: {}", e.getMessage(), e);
            return ResponseDTO.userErrorParam("消息发送失败: " + e.getMessage());
        }
    }

    /**
     * SSE订阅接口
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam List<String> topics,
                               HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        String sessionId = request.getHeader("X-Session-Id");
        String userName = request.getHeader("X-User-Name");

        // 创建SSE连接
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        String connectionId = sessionId != null ? sessionId : "anonymous-" + System.currentTimeMillis();

        try {
            // 注册SSE连接
            sseConnections.put(connectionId, emitter);

            // 注册到session管理器（用于消息路由）
            if (userId != null && sessionId != null) {
                sessionManager.registerSseConnection(sessionId, Long.parseLong(userId), emitter, topics);
            }

            // 发送连接确认
            Map<String, Object> connectData = new HashMap<>();
            connectData.put("status", "connected");
            connectData.put("connectionId", connectionId);
            connectData.put("topics", topics);
            connectData.put("userId", userId);
            connectData.put("userName", userName);

            emitter.send(SseEmitter.event()
                        .name("connected")
                        .data(connectData));

            // 设置连接关闭回调
            emitter.onCompletion(() -> {
                sseConnections.remove(connectionId);
                if (sessionId != null) {
                    sessionManager.removeSseConnection(sessionId);
                }
                log.debug("🚀 [SSE] 连接关闭: connectionId={}, userId={}", connectionId, userId);
            });

            emitter.onError((error) -> {
                sseConnections.remove(connectionId);
                if (sessionId != null) {
                    sessionManager.removeSseConnection(sessionId);
                }
                log.error("🚀 [SSE] 连接错误: connectionId={}, error={}", connectionId, error.getMessage());
            });

            emitter.onTimeout(() -> {
                sseConnections.remove(connectionId);
                if (sessionId != null) {
                    sessionManager.removeSseConnection(sessionId);
                }
                log.debug("🚀 [SSE] 连接超时: connectionId={}", connectionId);
            });

            log.info("🚀 [SSE] 订阅建立: connectionId={}, userId={}, topics={}", connectionId, userId, topics);

        } catch (Exception e) {
            log.error("🚀 [SSE] 订阅失败: connectionId={}, error={}", connectionId, e.getMessage());
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 获取在线用户数
     */
    @GetMapping("/online-count")
    public ResponseDTO<Map<String, Object>> getOnlineCount() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("onlineUserCount", sessionManager.getOnlineUserCount());
            result.put("sseConnectionCount", sseConnections.size());
            result.put("timestamp", Instant.now());

            return ResponseDTO.ok(result);

        } catch (Exception e) {
            log.error("🚀 [HTTP代理] 获取在线用户数失败: {}", e.getMessage());
            return ResponseDTO.userErrorParam("获取在线用户数失败");
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseDTO<Map<String, Object>> health() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "UP");
            result.put("transport", "rocketmq");
            result.put("onlineUsers", sessionManager.getOnlineUserCount());
            result.put("sseConnections", sseConnections.size());
            result.put("timestamp", Instant.now());

            return ResponseDTO.ok(result);

        } catch (Exception e) {
            log.error("🚀 [HTTP代理] 健康检查失败: {}", e.getMessage());
            return ResponseDTO.userErrorParam("健康检查失败");
        }
    }

    /**
     * 广播消息到所有SSE连接
     */
    public void broadcastToSse(RocketMQMessage message) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("messageId", message.getMessageId());
        messageData.put("type", message.getType());
        messageData.put("module", message.getModule());
        messageData.put("topic", message.getTopic());
        messageData.put("tag", message.getTag());
        messageData.put("data", message.getData());
        messageData.put("content", message.getContent());
        messageData.put("fromUserId", message.getFromUserId());
        messageData.put("fromUserName", message.getFromUserName());
        messageData.put("timestamp", message.getTimestamp());

        sseConnections.values().forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                            .name("message")
                            .data(messageData));
            } catch (Exception e) {
                log.warn("🚀 [SSE] 发送消息失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return "http-" + System.currentTimeMillis() + "-" + Math.random();
    }

    /**
     * RocketMQ发送请求
     */
    public static class RocketMQSendRequest {
        private String topic;
        private String tag;
        private String type;
        private String module;
        private String keys;
        private Map<String, Object> data;
        private String content;
        private Boolean orderly;
        private String orderId;
        private Integer delayLevel;

        // Getters and Setters
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }

        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getModule() { return module; }
        public void setModule(String module) { this.module = module; }

        public String getKeys() { return keys; }
        public void setKeys(String keys) { this.keys = keys; }

        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public Boolean getOrderly() { return orderly; }
        public void setOrderly(Boolean orderly) { this.orderly = orderly; }

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public Integer getDelayLevel() { return delayLevel; }
        public void setDelayLevel(Integer delayLevel) { this.delayLevel = delayLevel; }
    }
}