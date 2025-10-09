package net.lab1024.sa.admin.module.business.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IM自动拉人规则实体
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Data
@TableName("t_im_auto_invite_rule")
public class ImAutoInviteRuleEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型: CREATOR=创建人, OWNER=负责人, DEPARTMENT=部门, ROLE=角色
     */
    private String ruleType;

    /**
     * 业务类型: POLICE=警情
     */
    private String businessType;

    /**
     * 条件配置(JSON格式)
     */
    private String conditionConfig;

    /**
     * 是否启用: 1=启用, 0=禁用
     */
    private Integer enabled;

    /**
     * 优先级 (数字越小优先级越高)
     */
    private Integer priority;

    /**
     * 规则说明
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除标志: 0=未删除, 1=已删除
     */
    private Integer deletedFlag;
}
