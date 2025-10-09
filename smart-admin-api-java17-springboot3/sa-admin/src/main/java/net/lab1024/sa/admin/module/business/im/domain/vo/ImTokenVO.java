package net.lab1024.sa.admin.module.business.im.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * IM Token响应VO
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Data
@Schema(description = "IM Token响应")
public class ImTokenVO {

    @Schema(description = "用户Token")
    private String userToken;

    @Schema(description = "OpenIM用户ID")
    private String userID;

    @Schema(description = "Token过期时间 (时间戳)")
    private Long expireTime;

    @Schema(description = "OpenIM API地址")
    private String apiUrl;

    @Schema(description = "OpenIM WebSocket地址")
    private String wsUrl;

    @Schema(description = "平台ID")
    private Integer platformID;
}
