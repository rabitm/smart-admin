package net.lab1024.sa.admin.module.support.im.domain.dto;

import lombok.Data;

/**
 * OpenIM Webhook 事件 DTO
 *
 * OpenIM v3.x Webhook 规范:
 * https://docs.openim.io/restapi/webhooks
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-10
 * @Copyright 1024创新实验室
 */
@Data
public class OpenIMWebhookDTO {

    /**
     * 事件类型
     *
     * 常见事件:
     * - "user.online": 用户上线
     * - "user.offline": 用户下线
     * - "message.send.before": 消息发送前
     * - "message.send.after": 消息发送后
     * - "group.create": 群组创建
     * - "group.member.add": 成员加入群组
     * - "group.member.delete": 成员退出群组
     */
    private String event;

    /**
     * 事件数据 (JSON字符串)
     */
    private String data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 请求ID (用于日志追踪)
     */
    private String requestId;
}
