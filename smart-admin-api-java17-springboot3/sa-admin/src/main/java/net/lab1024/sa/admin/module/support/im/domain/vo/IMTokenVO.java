package net.lab1024.sa.admin.module.support.im.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IM Token 响应对象
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-10
 * @Copyright 1024创新实验室
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "IM Token响应")
public class IMTokenVO {

    @Schema(description = "OpenIM用户ID")
    private String openimUserId;

    @Schema(description = "OpenIM Token")
    private String token;

    @Schema(description = "Token过期时间(Unix时间戳，秒)")
    private Long expireTime;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "用户头像")
    private String faceURL;
}
