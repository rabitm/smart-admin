package net.lab1024.sa.admin.module.support.im.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * IM Token 请求表单
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-10
 * @Copyright 1024创新实验室
 */
@Data
@Schema(description = "IM Token请求")
public class IMTokenForm {

    @Schema(description = "员工ID")
    @NotNull(message = "员工ID不能为空")
    private Long employeeId;
}
