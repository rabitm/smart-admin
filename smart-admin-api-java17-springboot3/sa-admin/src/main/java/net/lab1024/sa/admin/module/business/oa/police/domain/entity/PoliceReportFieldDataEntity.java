package net.lab1024.sa.admin.module.business.oa.police.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import net.lab1024.sa.base.module.support.datatracer.annoation.DataTracerFieldLabel;

import java.time.LocalDateTime;

/**
 * 警情专业字段数据实体
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-23
 * @Copyright 1024创新实验室 （ https://1024lab.net ），Since 2012
 */
@Data
@TableName("t_police_report_field_data")
public class PoliceReportFieldDataEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @DataTracerFieldLabel("数据ID")
    private Long id;

    /**
     * 警情ID
     */
    @DataTracerFieldLabel("警情ID")
    private Long reportId;

    /**
     * 字段标识
     */
    @DataTracerFieldLabel("字段标识")
    private String fieldKey;

    /**
     * 字段值
     */
    @DataTracerFieldLabel("字段值")
    private String fieldValue;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}