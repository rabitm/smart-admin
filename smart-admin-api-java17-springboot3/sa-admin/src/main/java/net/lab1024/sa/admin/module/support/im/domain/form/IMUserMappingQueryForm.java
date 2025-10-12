package net.lab1024.sa.admin.module.support.im.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * IM 用户映射批量查询表单
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-10
 * @Copyright 1024创新实验室
 */
@Data
@Schema(description = "IM用户映射批量查询")
public class IMUserMappingQueryForm {

    @Schema(description = "员工ID列表")
    @NotEmpty(message = "员工ID列表不能为空")
    private List<Long> employeeIds;
}
