package net.lab1024.sa.admin.module.support.im.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.im.domain.dto.OpenIMWebhookDTO;
import net.lab1024.sa.admin.module.support.im.service.IMWebhookService;
import net.lab1024.sa.base.common.annoation.NoNeedLogin;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * IM Webhook 控制器
 *
 * 接收 OpenIM Server 的 Webhook 回调
 *
 * 配置方法:
 * 1. 在 OpenIM Server 配置文件中添加:
 *    webhook:
 *      url: "http://your-backend-domain/api/im/webhook"
 *      enable: true
 *
 * 2. 确保此接口可以被 OpenIM Server 访问 (添加到白名单)
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-10
 * @Copyright 1024创新实验室
 */
@Slf4j
@Tag(name = "IM Webhook接收")
@RestController
@RequestMapping("/api/im/webhook")
public class IMWebhookController {

    @Resource
    private IMWebhookService imWebhookService;

    /**
     * 接收 OpenIM Webhook 事件
     *
     * 注意: 此接口不需要登录认证,因为是 OpenIM Server 调用
     */
    @NoNeedLogin
    @Operation(summary = "接收OpenIM Webhook事件")
    @PostMapping
    public ResponseDTO<Void> receiveWebhook(
            @RequestHeader(value = "X-OpenIM-Signature", required = false) String signature,
            @RequestBody OpenIMWebhookDTO webhook
    ) {
        log.info("📥 [Webhook] 收到事件: {}, requestId: {}", webhook.getEvent(), webhook.getRequestId());

        // 验证签名 (可选,根据安全需求)
        if (signature != null && !imWebhookService.verifySignature(signature, webhook.getData())) {
            log.warn("⚠️ [Webhook] 签名验证失败, requestId: {}", webhook.getRequestId());
            return ResponseDTO.userErrorParam("签名验证失败");
        }

        // 异步处理 Webhook 事件
        try {
            imWebhookService.handleWebhook(webhook);
            return ResponseDTO.ok();
        } catch (Exception e) {
            log.error("❌ [Webhook] 处理失败, requestId: {}", webhook.getRequestId(), e);
            return ResponseDTO.userErrorParam("处理失败");
        }
    }

    /**
     * Webhook 健康检查
     *
     * OpenIM Server 可能会定期调用此接口检查 Webhook 是否可用
     */
    @NoNeedLogin
    @Operation(summary = "Webhook健康检查")
    @GetMapping("/health")
    public ResponseDTO<String> health() {
        return ResponseDTO.ok("OK");
    }
}
