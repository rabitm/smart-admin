package net.lab1024.sa.admin.module.business.oa.police.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 警情录入更新表单
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-18
 */
@Data
public class PoliceReportUpdateForm {

    @Schema(description = "警情ID")
    @NotNull(message = "警情ID不能为空")
    private Long reportId;

    @Schema(description = "警情类型")
    @NotNull(message = "警情类型不能为空")
    private Integer reportType;

    @Schema(description = "警情等级")
    @NotNull(message = "警情等级不能为空")
    private Integer reportLevel;

    @Schema(description = "报警人姓名")
    @NotBlank(message = "报警人姓名不能为空")
    @Length(max = 50, message = "报警人姓名最多50字符")
    private String reporterName;

    @Schema(description = "报警人电话")
    @NotBlank(message = "报警人电话不能为空")
    @Length(max = 20, message = "报警人电话最多20字符")
    private String reporterPhone;

    @Schema(description = "报警人身份证号")
    @Length(max = 18, message = "报警人身份证号最多18字符")
    private String reporterIdCard;

    @Schema(description = "报警时间")
    @NotNull(message = "报警时间不能为空")
    private LocalDateTime reportTime;

    @Schema(description = "事发地点")
    @NotBlank(message = "事发地点不能为空")
    @Length(max = 500, message = "事发地点最多500字符")
    private String incidentLocation;

    @Schema(description = "警情描述")
    @NotBlank(message = "警情描述不能为空")
    @Length(max = 2000, message = "警情描述最多2000字符")
    private String description;

    @Schema(description = "处理状态")
    @NotNull(message = "处理状态不能为空")
    private Integer status;

    @Schema(description = "处理人员ID")
    private Long handlerId;

    @Schema(description = "处理人员姓名")
    @Length(max = 50, message = "处理人员姓名最多50字符")
    private String handlerName;

    @Schema(description = "处理结果")
    @Length(max = 2000, message = "处理结果最多2000字符")
    private String handleResult;

    @Schema(description = "处理完成时间")
    private LocalDateTime handleTime;

    @Schema(description = "附件信息")
    @Length(max = 1000, message = "附件信息最多1000字符")
    private String attachments;

    @Schema(description = "备注")
    @Length(max = 500, message = "备注最多500字符")
    private String remark;

    @Schema(description = "专业字段数据")
    private Map<String, Object> professionalFields;
}