package net.lab1024.sa.admin.module.business.oa.police.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 警情操作记录实体
 * 记录警情的所有操作历史，支持操作回溯
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-24
 * @Copyright 1024创新实验室
 */
@Data
@TableName("t_police_report_operation_log")
public class PoliceReportOperationLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 警情ID
     */
    private Long reportId;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作用户名称
     */
    private String userName;

    /**
     * 操作类型（FIELD_UPDATE、FIELD_DELETE、STATUS_CHANGE、CREATE、DELETE等）
     */
    private String operationType;

    /**
     * 字段名称
     */
    private String fieldName;

    /**
     * 字段显示名称（中文）
     */
    private String fieldLabel;

    /**
     * 旧值
     */
    private String oldValue;

    /**
     * 新值
     */
    private String newValue;

    /**
     * 操作描述
     */
    private String description;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 版本号（用于并发控制）
     */
    private Integer version;

    /**
     * 扩展数据（JSON格式）
     */
    private String extData;
}