package net.lab1024.sa.admin.module.support.im.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.im.dao.IMGroupMemberDao;
import net.lab1024.sa.admin.module.support.im.domain.dto.OpenIMWebhookDTO;
import net.lab1024.sa.admin.module.support.im.domain.entity.IMGroupMemberEntity;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Map;

/**
 * IM Webhook 服务
 *
 * 职责:
 * 1. 接收 OpenIM Server 的 Webhook 回调
 * 2. 同步 OpenIM 事件到业务数据库
 * 3. 触发业务逻辑 (如通知、日志记录等)
 *
 * OpenIM Webhook 配置:
 * 在 OpenIM Server 配置文件中添加:
 * ```yaml
 * webhook:
 *   url: "http://your-backend-domain/api/im/webhook"
 *   enable: true
 * ```
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-10
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service
public class IMWebhookService {

    @Resource
    private IMGroupMemberDao imGroupMemberDao;

    @Resource
    private IMOperationLogService imOperationLogService;

    /**
     * 处理 OpenIM Webhook 事件
     *
     * @param webhook Webhook事件数据
     */
    public void handleWebhook(OpenIMWebhookDTO webhook) {
        if (webhook == null || webhook.getEvent() == null) {
            log.warn("⚠️ [Webhook] 收到无效的Webhook事件");
            return;
        }

        String event = webhook.getEvent();
        log.info("📥 [Webhook] 收到事件: {}, requestId: {}", event, webhook.getRequestId());

        try {
            // 解析事件数据
            JSONObject data = JSON.parseObject(webhook.getData());

            // 根据事件类型分发处理
            switch (event) {
                case "group.member.add":
                    handleGroupMemberAdd(data);
                    break;
                case "group.member.delete":
                    handleGroupMemberDelete(data);
                    break;
                case "group.disband":
                    handleGroupDisband(data);
                    break;
                case "message.send.after":
                    handleMessageSent(data);
                    break;
                default:
                    log.debug("🔔 [Webhook] 忽略事件: {}", event);
                    break;
            }

        } catch (Exception e) {
            log.error("❌ [Webhook] 处理事件失败: {}", event, e);
        }
    }

    /**
     * 处理成员加入群组事件
     *
     * 同步目的: 即使前端直接邀请成员,后端也能知道群组成员变化
     */
    private void handleGroupMemberAdd(JSONObject data) {
        try {
            String groupId = data.getString("groupID");
            String userId = data.getString("userID");
            Integer roleInGroup = data.getInteger("roleLevel");

            log.info("👥 [Webhook] 成员加入群组: groupId={}, userId={}, role={}",
                    groupId, userId, roleInGroup);

            // TODO: 同步成员信息到数据库
            // 1. 根据 groupId 查询对应的警情ID
            // 2. 根据 userId 查询对应的员工ID
            // 3. 保存成员记录

        } catch (Exception e) {
            log.error("❌ [Webhook] 处理成员加入事件失败", e);
        }
    }

    /**
     * 处理成员退出群组事件
     */
    private void handleGroupMemberDelete(JSONObject data) {
        try {
            String groupId = data.getString("groupID");
            String userId = data.getString("userID");

            log.info("👋 [Webhook] 成员退出群组: groupId={}, userId={}", groupId, userId);

            // TODO: 软删除成员记录

        } catch (Exception e) {
            log.error("❌ [Webhook] 处理成员退出事件失败", e);
        }
    }

    /**
     * 处理群组解散事件
     */
    private void handleGroupDisband(JSONObject data) {
        try {
            String groupId = data.getString("groupID");

            log.info("💥 [Webhook] 群组解散: groupId={}", groupId);

            // TODO: 更新群组状态为已解散

        } catch (Exception e) {
            log.error("❌ [Webhook] 处理群组解散事件失败", e);
        }
    }

    /**
     * 处理消息发送事件
     *
     * 用途: 记录消息日志,触发业务逻辑 (如消息审计、敏感词检测等)
     */
    private void handleMessageSent(JSONObject data) {
        try {
            String messageId = data.getString("clientMsgID");
            String senderId = data.getString("sendID");
            String groupId = data.getString("groupID");
            Integer contentType = data.getInteger("contentType");

            log.debug("💬 [Webhook] 消息发送: messageId={}, senderId={}, groupId={}, type={}",
                    messageId, senderId, groupId, contentType);

            // TODO: 记录消息日志

        } catch (Exception e) {
            log.error("❌ [Webhook] 处理消息发送事件失败", e);
        }
    }

    /**
     * 验证 Webhook 签名
     *
     * OpenIM 支持配置签名密钥,确保 Webhook 请求来自 OpenIM Server
     *
     * @param signature 请求签名
     * @param data 请求数据
     * @return 签名是否有效
     */
    public boolean verifySignature(String signature, String data) {
        // TODO: 实现签名验证逻辑
        // 1. 从配置读取签名密钥
        // 2. 使用相同算法计算签名
        // 3. 比较签名是否一致

        return true;  // 暂时不验证
    }
}
