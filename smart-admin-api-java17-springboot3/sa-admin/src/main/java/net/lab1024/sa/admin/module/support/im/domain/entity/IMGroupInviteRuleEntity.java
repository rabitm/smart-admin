package net.lab1024.sa.admin.module.support.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IM群组邀请规则实体
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@TableName("t_im_group_invite_rule")
public class IMGroupInviteRuleEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则编码
     */
    private String ruleCode;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型:1-按警情类型,2-按部门,3-按角色,4-按警情等级,5-固定人员
     */
    private Integer ruleType;

    /**
     * 规则配置(JSON格式)
     */
    private String ruleConfig;

    /**
     * 是否启用
     */
    private Boolean enabledFlag;

    /**
     * 优先级(数字越大优先级越高)
     */
    private Integer priority;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 删除标识
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
