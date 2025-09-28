package net.lab1024.sa.admin.module.business.oa.police.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 全方位操作记录批量表单
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-28
 * @Copyright 1024创新实验室
 */
@Data
@Schema(description = "全方位操作记录批量表单")
public class ComprehensiveOperationBatchForm {

    @Schema(description = "操作记录列表")
    @Valid
    @NotEmpty(message = "操作记录列表不能为空")
    private List<ComprehensiveOperationRecord> operations;

    /**
     * 全方位操作记录
     */
    @Data
    @Schema(description = "全方位操作记录")
    public static class ComprehensiveOperationRecord {

        @Schema(description = "操作ID")
        @NotNull(message = "操作ID不能为空")
        private String id;

        @Schema(description = "时间戳")
        @NotNull(message = "时间戳不能为空")
        private Long timestamp;

        @Schema(description = "客户端时间戳")
        private Long clientTimestamp;

        @Schema(description = "操作类型")
        @NotNull(message = "操作类型不能为空")
        private String type;

        @Schema(description = "操作级别")
        private String level;

        @Schema(description = "操作分类")
        private String category;

        @Schema(description = "用户ID")
        @NotNull(message = "用户ID不能为空")
        private Long userId;

        @Schema(description = "用户姓名")
        @NotNull(message = "用户姓名不能为空")
        private String userName;

        @Schema(description = "用户角色")
        private String userRole;

        @Schema(description = "部门")
        private String department;

        @Schema(description = "实体类型")
        @NotNull(message = "实体类型不能为空")
        private String entityType;

        @Schema(description = "实体ID")
        @NotNull(message = "实体ID不能为空")
        private Long entityId;

        @Schema(description = "字段路径")
        private String fieldPath;

        @Schema(description = "字段标签")
        private String fieldLabel;

        @Schema(description = "修改前值")
        private Object beforeValue;

        @Schema(description = "修改后值")
        private Object afterValue;

        @Schema(description = "增量数据")
        private Object deltaData;

        @Schema(description = "会话ID")
        private String sessionId;

        @Schema(description = "页面URL")
        private String pageUrl;

        @Schema(description = "来源页面")
        private String referrer;

        @Schema(description = "视口大小")
        private Map<String, Object> viewport;

        @Schema(description = "用户代理")
        private String userAgent;

        @Schema(description = "IP地址")
        private String ipAddress;

        @Schema(description = "网络延迟")
        private Integer networkLatency;

        @Schema(description = "业务上下文")
        private Map<String, Object> businessContext;

        @Schema(description = "协作上下文")
        private Map<String, Object> collaborationContext;

        @Schema(description = "安全级别")
        private String securityLevel;

        @Schema(description = "合规标记")
        private List<String> complianceFlags;

        @Schema(description = "元数据")
        private Map<String, Object> metadata;

        @Schema(description = "标签")
        private List<String> tags;

        @Schema(description = "操作描述")
        private String description;
    }
}