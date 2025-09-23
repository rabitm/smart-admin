package net.lab1024.sa.admin.module.business.oa.police.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import net.lab1024.sa.base.module.support.datatracer.annoation.DataTracerFieldLabel;

import java.time.LocalDateTime;

/**
 * 警情录入实体
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-18
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@TableName("t_oa_police_report")
public class PoliceReportEntity {

    /**
     * 警情ID
     */
    @TableId(type = IdType.AUTO)
    @DataTracerFieldLabel("警情ID")
    private Long reportId;

    /**
     * 警情编号
     */
    @DataTracerFieldLabel("警情编号")
    private String reportNumber;

    /**
     * 警情类型
     */
    @DataTracerFieldLabel("警情类型")
    private Integer reportType;

    /**
     * 警情等级
     */
    @DataTracerFieldLabel("警情等级")
    private Integer reportLevel;

    /**
     * 报警人姓名
     */
    @DataTracerFieldLabel("报警人姓名")
    private String reporterName;

    /**
     * 报警人电话
     */
    @DataTracerFieldLabel("报警人电话")
    private String reporterPhone;

    /**
     * 报警人身份证号
     */
    @DataTracerFieldLabel("报警人身份证号")
    private String reporterIdCard;

    /**
     * 报警时间
     */
    @DataTracerFieldLabel("报警时间")
    private LocalDateTime reportTime;

    /**
     * 事发地点
     */
    @DataTracerFieldLabel("事发地点")
    private String incidentLocation;

    /**
     * 警情描述
     */
    @DataTracerFieldLabel("警情描述")
    private String description;

    /**
     * 处理状态：1-待处理，2-处理中，3-已完成，4-已关闭
     */
    @DataTracerFieldLabel("处理状态")
    private Integer status;

    /**
     * 处理人员ID
     */
    @DataTracerFieldLabel("处理人员ID")
    private Long handlerId;

    /**
     * 处理人员姓名
     */
    @DataTracerFieldLabel("处理人员姓名")
    private String handlerName;

    /**
     * 处理结果
     */
    @DataTracerFieldLabel("处理结果")
    private String handleResult;

    /**
     * 处理完成时间
     */
    @DataTracerFieldLabel("处理完成时间")
    private LocalDateTime handleTime;

    /**
     * 附件信息
     */
    @DataTracerFieldLabel("附件信息")
    private String attachments;

    /**
     * 备注
     */
    @DataTracerFieldLabel("备注")
    private String remark;

    /**
     * 删除状态
     */
    private Boolean deletedFlag;

    /**
     * 创建人ID
     */
    private Long createUserId;

    /**
     * 创建人姓名
     */
    private String createUserName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}