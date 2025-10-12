package net.lab1024.sa.admin.module.support.im.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 保存消息表单
 *
 * 用途: 前端发送或接收消息后调用此接口保存到数据库
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-11
 * @Copyright 1024创新实验室
 */
@Data
@Schema(description = "保存IM消息表单")
public class SaveMessageForm {

    @Schema(description = "警情ID", required = true)
    @NotNull(message = "警情ID不能为空")
    private Long reportId;

    @Schema(description = "OpenIM群组ID", required = true)
    @NotBlank(message = "群组ID不能为空")
    private String groupId;

    @Schema(description = "消息ID (clientMsgID)", required = true)
    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    @Schema(description = "服务器消息ID (serverMsgID)")
    private String serverMessageId;

    @Schema(description = "会话ID")
    private String conversationId;

    @Schema(description = "发送者ID", required = true)
    @NotBlank(message = "发送者ID不能为空")
    private String senderId;

    @Schema(description = "发送者昵称")
    private String senderName;

    @Schema(description = "发送者头像")
    private String senderAvatar;

    @Schema(description = "消息内容类型: 101-文本, 102-图片, 103-语音, 104-视频, 105-文件", required = true)
    @NotNull(message = "消息内容类型不能为空")
    private Integer contentType;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "完整消息内容JSON (用于复杂消息类型)")
    private String contentJson;

    @Schema(description = "消息发送时间戳(毫秒)", required = true)
    @NotNull(message = "发送时间不能为空")
    private Long sendTime;

    @Schema(description = "消息序列号")
    private Long seq;
}
