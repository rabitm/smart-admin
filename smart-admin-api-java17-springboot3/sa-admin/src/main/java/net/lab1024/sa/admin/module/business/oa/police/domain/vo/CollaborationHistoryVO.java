package net.lab1024.sa.admin.module.business.oa.police.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 协作历史记录VO
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@Data
@Schema(description = "协作历史记录")
public class CollaborationHistoryVO {

    @Schema(description = "记录ID")
    private String id;

    @Schema(description = "操作类型")
    private String type;

    @Schema(description = "实体类型")
    private String entityType;

    @Schema(description = "实体ID")
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
    private Long userId;

    @Schema(description = "操作用户姓名")
    private String userName;

    @Schema(description = "用户部门")
    private String userDepartment;

    @Schema(description = "操作时间戳")
    private Long timestamp;

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

    @Schema(description = "操作时间")
    private LocalDateTime operationTime;
}