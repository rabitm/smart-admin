package net.lab1024.sa.admin.module.support.rocketmq.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketMessage;
import org.springframework.beans.BeanUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * RocketMQ消息协议
 * 继承WebSocket消息协议，增加RocketMQ特有字段
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-29
 * @Copyright 1024创新实验室
 */
@Data
@Accessors(chain = true)
public class RocketMQMessage {

    // WebSocket消息的所有字段
    private String messageId;
    private String type;
    private String module;
    private Long fromUserId;
    private String fromUserName;
    private Long toUserId;
    private String room;
    private Map<String, Object> data;
    private String content;
    private Integer priority = 2;
    private Boolean needAck = false;
    private Instant timestamp;
    private Long expireAfter;
    private Integer retryCount = 0;
    private Integer maxRetries = 3;
    private Map<String, Object> metadata;

    /**
     * RocketMQ主题
     */
    private String topic;

    /**
     * RocketMQ标签，用于消息过滤
     */
    private String tag;

    /**
     * 消息关键字，用于查询和去重
     */
    private String keys;

    /**
     * 是否顺序消息
     */
    private Boolean orderly = false;

    /**
     * 延迟级别 (0=不延迟, 1=1s, 2=5s, 3=10s, 4=30s, 5=1m, 6=2m, 7=3m, 8=4m, 9=5m, 10=6m, 11=7m, 12=8m, 13=9m, 14=10m, 15=20m, 16=30m, 17=1h, 18=2h)
     */
    private Integer delayLevel = 0;

    /**
     * 目标用户ID列表
     */
    private List<Long> targetUsers;

    /**
     * 目标部门ID列表
     */
    private List<Long> targetDepts;

    /**
     * 目标角色列表
     */
    private List<String> targetRoles;

    /**
     * 消息分区键，用于顺序消息
     */
    private String shardingKey;

    /**
     * 创建RocketMQ消息
     */
    public static RocketMQMessage create(String topic, String tag, String type, Map<String, Object> data) {
        RocketMQMessage message = new RocketMQMessage();
        message.setMessageId(generateMessageId())
               .setTopic(topic)
               .setTag(tag)
               .setType(type)
               .setData(data)
               .setKeys(message.getMessageId())
               .setTimestamp(Instant.now());
        return message;
    }

    /**
     * 从WebSocket消息转换
     */
    public static RocketMQMessage fromWebSocketMessage(WebSocketMessage wsMessage, String topic, String tag) {
        RocketMQMessage rocketMessage = new RocketMQMessage();
        BeanUtils.copyProperties(wsMessage, rocketMessage);
        rocketMessage.setTopic(topic)
                    .setTag(tag)
                    .setKeys(wsMessage.getMessageId());
        return rocketMessage;
    }

    /**
     * 转换为WebSocket消息
     */
    public WebSocketMessage toWebSocketMessage() {
        WebSocketMessage wsMessage = new WebSocketMessage();
        BeanUtils.copyProperties(this, wsMessage);
        return wsMessage;
    }

    /**
     * 生成消息ID
     */
    private static String generateMessageId() {
        return System.currentTimeMillis() + "-" + Math.random();
    }

    /**
     * 构建目标地址 (topic:tag)
     */
    public String getDestination() {
        if (tag != null) {
            return topic + ":" + tag;
        }
        return topic;
    }

    /**
     * 设置用户标签
     */
    public RocketMQMessage forUser(Long userId) {
        this.tag = "USER_" + userId;
        return this;
    }

    /**
     * 设置房间标签
     */
    public RocketMQMessage forRoom(String room) {
        this.tag = "ROOM_" + room;
        this.room = room;
        return this;
    }

    /**
     * 设置部门标签
     */
    public RocketMQMessage forDepartment(Long departmentId) {
        this.tag = "DEPT_" + departmentId;
        return this;
    }

    /**
     * 设置角色标签
     */
    public RocketMQMessage forRole(String role) {
        this.tag = "ROLE_" + role;
        return this;
    }

    /**
     * 设置广播标签
     */
    public RocketMQMessage forBroadcast() {
        this.tag = "BROADCAST";
        return this;
    }

    /**
     * 设置顺序消息
     */
    public RocketMQMessage ordered(String shardingKey) {
        this.orderly = true;
        this.shardingKey = shardingKey;
        return this;
    }

    /**
     * 设置延迟消息
     */
    public RocketMQMessage delayed(Integer delayLevel) {
        this.delayLevel = delayLevel;
        return this;
    }
}