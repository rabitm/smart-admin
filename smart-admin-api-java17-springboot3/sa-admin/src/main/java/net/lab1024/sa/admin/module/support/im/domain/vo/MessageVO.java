package net.lab1024.sa.admin.module.support.im.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * IM消息VO
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@Schema(description = "IM消息")
public class MessageVO {

    @Schema(description = "消息ID (clientMsgID)")
    private String messageId;

    @Schema(description = "服务器消息ID (serverMsgID)")
    private String serverMessageId;

    @Schema(description = "会话ID")
    private String conversationId;

    @Schema(description = "发送者ID")
    private String senderId;

    @Schema(description = "发送者昵称")
    private String senderName;

    @Schema(description = "发送者头像")
    private String senderAvatar;

    @Schema(description = "消息内容类型: 101-文本, 102-图片, 103-语音, 104-视频, 105-文件")
    private Integer contentType;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "完整消息内容JSON (用于复杂消息类型)")
    private String contentJson;

    @Schema(description = "消息发送时间戳(毫秒)")
    private Long sendTime;

    @Schema(description = "消息序列号")
    private Long seq;

    @Schema(description = "是否是自己发送的消息")
    private Boolean isSelf;
}
