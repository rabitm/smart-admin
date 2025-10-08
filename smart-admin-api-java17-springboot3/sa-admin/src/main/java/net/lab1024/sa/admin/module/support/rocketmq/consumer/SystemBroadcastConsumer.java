package net.lab1024.sa.admin.module.support.rocketmq.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.rocketmq.constant.RocketMQTopics;
import net.lab1024.sa.admin.module.support.rocketmq.domain.RocketMQMessage;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketMessage;
import net.lab1024.sa.admin.module.support.websocket.service.MessageTransport;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 系统广播消费者
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-29
 * @Copyright 1024创新实验室
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rocketmq.name-server")
@RocketMQMessageListener(
    topic = RocketMQTopics.SYSTEM_BROADCAST,
    consumerGroup = "system-broadcast-consumer"
)
public class SystemBroadcastConsumer implements RocketMQListener<RocketMQMessage> {

    @Qualifier("webSocketTransport")
    private final MessageTransport webSocketTransport;

    @Override
    public void onMessage(RocketMQMessage message) {
        try {
            log.debug("🚀 [系统广播] 收到消息: type={}, tag={}", message.getType(), message.getTag());

            // 转发到WebSocket
            forwardToWebSocket(message);

        } catch (Exception e) {
            log.error("🚀 [系统广播] 消息处理失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 转发消息到WebSocket
     */
    private void forwardToWebSocket(RocketMQMessage rocketMessage) {
        try {
            WebSocketMessage wsMessage = rocketMessage.toWebSocketMessage();
            String tag = rocketMessage.getTag();

            if (tag == null || tag.equals("BROADCAST")) {
                // 广播消息
                webSocketTransport.broadcast(wsMessage);
            } else if (tag.startsWith("USER_")) {
                // 用户消息
                Long userId = Long.parseLong(tag.substring(5));
                webSocketTransport.sendToUser(userId, wsMessage);
            } else if (tag.startsWith("DEPT_")) {
                // 部门消息
                Long departmentId = Long.parseLong(tag.substring(5));
                webSocketTransport.sendToDepartment(departmentId, wsMessage);
            } else if (tag.startsWith("ROLE_")) {
                // 角色消息
                String role = tag.substring(5);
                webSocketTransport.sendToRole(role, wsMessage);
            } else {
                // 默认广播
                webSocketTransport.broadcast(wsMessage);
            }

        } catch (Exception e) {
            log.error("🚀 [RocketMQ] 转发消息到WebSocket失败: {}", e.getMessage(), e);
        }
    }
}