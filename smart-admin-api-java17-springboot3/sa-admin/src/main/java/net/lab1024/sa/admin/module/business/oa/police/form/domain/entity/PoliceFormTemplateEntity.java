package net.lab1024.sa.admin.module.business.oa.police.form.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 警情录入表单模板实体
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-22
 * @Copyright 1024创新实验室 （ https://1024lab.net ），Since 2012
 */
@Data
@TableName("t_police_form_template")
public class PoliceFormTemplateEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 警情类型
     */
    private Integer reportType;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 是否默认模板
     */
    private Boolean isDefault;

    /**
     * 状态：1启用，0禁用
     */
    private Boolean status;

    /**
     * 所属组织ID，为空表示全局模板
     */
    private Long organizationId;

    /**
     * 创建人
     */
    private Long createUserId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private Long updateUserId;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}