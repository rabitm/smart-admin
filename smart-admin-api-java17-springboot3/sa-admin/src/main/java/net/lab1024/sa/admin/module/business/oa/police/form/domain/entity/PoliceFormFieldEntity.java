package net.lab1024.sa.admin.module.business.oa.police.form.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 警情录入表单字段实体
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-22
 * @Copyright 1024创新实验室 （ https://1024lab.net ），Since 2012
 */
@Data
@TableName("t_police_form_field")
public class PoliceFormFieldEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 字段标识
     */
    private String fieldKey;

    /**
     * 字段名称
     */
    private String fieldLabel;

    /**
     * 字段类型：input,select,textarea,number,checkbox,compact-group,checkbox-compact
     */
    private String fieldType;

    /**
     * 是否必填
     */
    private Boolean isRequired;

    /**
     * 字段图标
     */
    private String fieldIcon;

    /**
     * 占位符提示
     */
    private String placeholder;

    /**
     * 字段选项JSON
     */
    private String fieldOptions;

    /**
     * 快捷选项JSON
     */
    private String quickOptions;

    /**
     * 数据字典编码
     */
    private String dictCode;

    /**
     * 父字段ID，用于组合字段
     */
    private Long parentFieldId;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 步骤号：2或3
     */
    private Integer stepNumber;

    /**
     * 验证规则JSON
     */
    private String validationRules;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 状态：1启用，0禁用
     */
    private Boolean status;

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