package net.lab1024.sa.admin.module.support.websocket.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * WebSocket统一消息协议
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@Data
@Accessors(chain = true)
public class WebSocketMessage {

    /**
     * 消息ID，用于追踪和去重
     */
    private String messageId;

    /**
     * 消息类型
     */
    private String type;

    /**
     * 业务模块（seat、police、collaboration等）
     */
    private String module;

    /**
     * 发送者用户ID
     */
    private Long fromUserId;

    /**
     * 发送者用户名
     */
    private String fromUserName;

    /**
     * 接收者用户ID（为空表示广播）
     */
    private Long toUserId;

    /**
     * 接收者房间/频道
     */
    private String room;

    /**
     * 消息数据
     */
    private Map<String, Object> data;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 优先级：1-高，2-中，3-低
     */
    private Integer priority = 2;

    /**
     * 是否需要确认接收
     */
    private Boolean needAck = false;

    /**
     * 消息发送时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    /**
     * 消息过期时间（秒）
     */
    private Long expireAfter;

    /**
     * 重试次数
     */
    private Integer retryCount = 0;

    /**
     * 最大重试次数
     */
    private Integer maxRetries = 3;

    /**
     * 额外元数据
     */
    private Map<String, Object> metadata;

    /**
     * 创建系统消息
     */
    public static WebSocketMessage system(String type, String content) {
        return new WebSocketMessage()
                .setMessageId(generateMessageId())
                .setType(type)
                .setModule("system")
                .setContent(content)
                .setTimestamp(Instant.now());
    }

    /**
     * 创建业务消息
     */
    public static WebSocketMessage business(String module, String type, Map<String, Object> data) {
        return new WebSocketMessage()
                .setMessageId(generateMessageId())
                .setType(type)
                .setModule(module)
                .setData(data)
                .setTimestamp(Instant.now());
    }

    /**
     * 创建广播消息
     */
    public static WebSocketMessage broadcast(String module, String type, Map<String, Object> data) {
        return business(module, type, data).setRoom("all");
    }

    /**
     * 创建点对点消息
     */
    public static WebSocketMessage direct(String module, String type, Long toUserId, Map<String, Object> data) {
        return business(module, type, data).setToUserId(toUserId);
    }

    /**
     * 生成消息ID
     */
    private static String generateMessageId() {
        return System.currentTimeMillis() + "-" + Math.random();
    }

    /**
     * 检查消息是否过期
     */
    public boolean isExpired() {
        if (expireAfter == null || timestamp == null) {
            return false;
        }
        return Instant.now().isAfter(timestamp.plusSeconds(expireAfter));
    }

    /**
     * 是否可以重试
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    /**
     * 增加重试次数
     */
    public WebSocketMessage incrementRetry() {
        this.retryCount++;
        return this;
    }
}