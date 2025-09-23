package net.lab1024.sa.admin.module.business.oa.police.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.base.common.domain.PageParam;

import java.time.LocalDateTime;

/**
 * 警情录入查询表单
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-18
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PoliceReportQueryForm extends PageParam {

    @Schema(description = "警情编号")
    private String reportNumber;

    @Schema(description = "警情类型")
    private Integer reportType;

    @Schema(description = "警情等级")
    private Integer reportLevel;

    @Schema(description = "报警人姓名")
    private String reporterName;

    @Schema(description = "报警人电话")
    private String reporterPhone;

    @Schema(description = "处理状态")
    private Integer status;

    @Schema(description = "处理人员姓名")
    private String handlerName;

    @Schema(description = "事发地点")
    private String incidentLocation;

    @Schema(description = "报警开始时间")
    private LocalDateTime reportTimeStart;

    @Schema(description = "报警结束时间")
    private LocalDateTime reportTimeEnd;

    @Schema(description = "创建开始时间")
    private LocalDateTime createTimeStart;

    @Schema(description = "创建结束时间")
    private LocalDateTime createTimeEnd;

    // ==================== 专业字段查询参数 ====================

    @Schema(description = "被困人数(最小值)")
    private Integer trappedCountMin;

    @Schema(description = "被困人数(最大值)")
    private Integer trappedCountMax;

    @Schema(description = "伤亡人数(最小值)")
    private Integer casualtiesCountMin;

    @Schema(description = "伤亡人数(最大值)")
    private Integer casualtiesCountMax;

    @Schema(description = "车辆数量(最小值)")
    private Integer vehicleCountMin;

    @Schema(description = "车辆数量(最大值)")
    private Integer vehicleCountMax;

    @Schema(description = "危险等级")
    private Integer dangerLevel;

    @Schema(description = "救援类型")
    private String rescueType;

    @Schema(description = "事故类型")
    private String accidentType;

    @Schema(description = "火灾楼层")
    private String fireFloor;

    @Schema(description = "火势规模")
    private Integer fireScale;
}