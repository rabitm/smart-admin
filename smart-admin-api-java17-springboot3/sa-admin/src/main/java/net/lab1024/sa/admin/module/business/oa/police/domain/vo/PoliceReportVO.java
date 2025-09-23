package net.lab1024.sa.admin.module.business.oa.police.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 警情录入视图对象
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-18
 */
@Data
public class PoliceReportVO {

    @Schema(description = "警情ID")
    private Long reportId;

    @Schema(description = "警情编号")
    private String reportNumber;

    @Schema(description = "警情类型")
    private Integer reportType;

    @Schema(description = "警情类型名称")
    private String reportTypeName;

    @Schema(description = "警情等级")
    private Integer reportLevel;

    @Schema(description = "警情等级名称")
    private String reportLevelName;

    @Schema(description = "报警人姓名")
    private String reporterName;

    @Schema(description = "报警人电话")
    private String reporterPhone;

    @Schema(description = "报警人身份证号")
    private String reporterIdCard;

    @Schema(description = "报警时间")
    private LocalDateTime reportTime;

    @Schema(description = "事发地点")
    private String incidentLocation;

    @Schema(description = "警情描述")
    private String description;

    @Schema(description = "处理状态")
    private Integer status;

    @Schema(description = "处理状态名称")
    private String statusName;

    @Schema(description = "处理人员ID")
    private Long handlerId;

    @Schema(description = "处理人员姓名")
    private String handlerName;

    @Schema(description = "处理结果")
    private String handleResult;

    @Schema(description = "处理完成时间")
    private LocalDateTime handleTime;

    @Schema(description = "附件信息")
    private String attachments;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "创建人姓名")
    private String createUserName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}