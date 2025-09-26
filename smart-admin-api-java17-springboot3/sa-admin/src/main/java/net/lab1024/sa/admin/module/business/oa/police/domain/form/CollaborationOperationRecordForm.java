package net.lab1024.sa.admin.module.business.oa.police.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 协作操作记录表单
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@Data
@Schema(description = "协作操作记录表单")
public class CollaborationOperationRecordForm {

    @Schema(description = "操作类型")
    @NotBlank(message = "操作类型不能为空")
    private String type;

    @Schema(description = "实体类型")
    @NotBlank(message = "实体类型不能为空")
    private String entityType;

    @Schema(description = "实体ID")
    @NotNull(message = "实体ID不能为空")
    private Long entityId;

    @Schema(description = "字段名称")
    private String fieldName;

    @Schema(description = "字段显示名称")
    private String fieldLabel;

    @Schema(description = "旧值")
    private Object oldValue;

    @Schema(description = "新值")
    private Object newValue;

    @Schema(description = "操作用户ID")
    @NotNull(message = "操作用户ID不能为空")
    private Long userId;

    @Schema(description = "操作用户姓名")
    @NotBlank(message = "操作用户姓名不能为空")
    private String userName;

    @Schema(description = "用户部门")
    private String userDepartment;

    @Schema(description = "客户端时间戳")
    private Long clientTimestamp;

    @Schema(description = "IP地址")
    private String ipAddress;

    @Schema(description = "用户代理")
    private String userAgent;

    @Schema(description = "操作描述")
    private String description;

    @Schema(description = "扩展元数据")
    private Map<String, Object> metadata;

    @Schema(description = "严重程度")
    private String severity;
}