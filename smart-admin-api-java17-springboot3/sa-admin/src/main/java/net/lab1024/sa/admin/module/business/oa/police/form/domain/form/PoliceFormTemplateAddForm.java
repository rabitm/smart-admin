package net.lab1024.sa.admin.module.business.oa.police.form.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 警情表单模板新增表单
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-22
 * @Copyright 1024创新实验室 （ https://1024lab.net ），Since 2012
 */
@Data
@Schema(description = "警情表单模板新增")
public class PoliceFormTemplateAddForm {

    @Schema(description = "警情类型")
    @NotNull(message = "警情类型不能为空")
    private Integer reportType;

    @Schema(description = "模板名称")
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @Schema(description = "模板描述")
    private String description;

    @Schema(description = "是否默认模板")
    private Boolean isDefault = false;

    @Schema(description = "所属组织ID")
    private Long organizationId;
}