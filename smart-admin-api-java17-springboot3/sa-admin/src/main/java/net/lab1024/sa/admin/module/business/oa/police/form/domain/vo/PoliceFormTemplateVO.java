package net.lab1024.sa.admin.module.business.oa.police.form.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 警情表单模板VO
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-22
 * @Copyright 1024创新实验室 （ https://1024lab.net ），Since 2012
 */
@Data
@Schema(description = "警情表单模板")
public class PoliceFormTemplateVO {

    @Schema(description = "模板ID")
    private Long id;

    @Schema(description = "警情类型")
    private Integer reportType;

    @Schema(description = "警情类型名称")
    private String reportTypeName;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板描述")
    private String description;

    @Schema(description = "是否默认模板")
    private Boolean isDefault;

    @Schema(description = "状态")
    private Boolean status;

    @Schema(description = "所属组织ID")
    private Long organizationId;

    @Schema(description = "所属组织名称")
    private String organizationName;

    @Schema(description = "字段数量")
    private Integer fieldCount;

    @Schema(description = "创建人")
    private Long createUserId;

    @Schema(description = "创建人姓名")
    private String createUserName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}