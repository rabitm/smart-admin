package net.lab1024.sa.admin.module.business.oa.police.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import net.lab1024.sa.base.common.domain.PageParam;

import jakarta.validation.constraints.NotNull;

/**
 * 协作历史查询表单
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@Data
@Schema(description = "协作历史查询表单")
public class CollaborationHistoryQueryForm extends PageParam {

    @Schema(description = "实体类型")
    @NotNull(message = "实体类型不能为空")
    private String entityType;

    @Schema(description = "实体ID")
    @NotNull(message = "实体ID不能为空")
    private Long entityId;

    @Schema(description = "操作用户ID")
    private Long userId;

    @Schema(description = "字段名称")
    private String fieldName;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "开始时间戳")
    private Long startTime;

    @Schema(description = "结束时间戳")
    private Long endTime;

    @Schema(description = "严重程度")
    private String severity;
}