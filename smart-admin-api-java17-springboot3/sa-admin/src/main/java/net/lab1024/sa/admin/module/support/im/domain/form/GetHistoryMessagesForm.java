package net.lab1024.sa.admin.module.support.im.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 获取历史消息请求表单
 *
 * 用于REST API fallback方案,当前端SDK无法从IndexedDB获取消息时使用
 * 直接从OpenIM Server拉取历史消息
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-11
 * @Copyright 1024创新实验室
 */
@Data
@Schema(description = "获取历史消息请求")
public class GetHistoryMessagesForm {

    @Schema(description = "群组ID (格式: group_report_5)", required = true)
    @NotBlank(message = "群组ID不能为空")
    private String groupId;

    @Schema(description = "消息数量 (默认50条)", example = "50")
    @Min(value = 1, message = "消息数量至少为1")
    private Integer count = 50;

    @Schema(description = "起始消息ID (空字符串表示从最新消息开始)")
    private String startClientMsgID = "";
}
