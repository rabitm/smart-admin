package net.lab1024.sa.admin.module.support.im.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 发送消息表单
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@Schema(description = "发送消息表单")
public class SendMessageForm {

    @Schema(description = "警情ID")
    @NotNull(message = "警情ID不能为空")
    private Long reportId;

    @Schema(description = "群组ID")
    private String groupId;

    @Schema(description = "消息内容")
    @NotBlank(message = "消息内容不能为空")
    private String content;

    @Schema(description = "消息类型: 101-文本")
    private Integer contentType = 101;
}
