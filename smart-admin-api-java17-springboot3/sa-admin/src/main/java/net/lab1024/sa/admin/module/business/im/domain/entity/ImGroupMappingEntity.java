package net.lab1024.sa.admin.module.business.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IM群组映射实体
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Data
@TableName("t_im_group_mapping")
public class ImGroupMappingEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * OpenIM群组ID
     */
    private String groupId;

    /**
     * 业务类型: POLICE=警情群
     */
    private String businessType;

    /**
     * 业务ID (如警情ID)
     */
    private Long businessId;

    /**
     * 群组名称
     */
    private String groupName;

    /**
     * 群主OpenIM用户ID
     */
    private String ownerUserId;

    /**
     * 群主员工ID
     */
    private Long ownerEmployeeId;

    /**
     * 成员数量
     */
    private Integer memberCount;

    /**
     * 群组状态: 0=创建中, 1=同步中, 2=验证中, 3=正常, 4=异常, 5=修复中, 9=已删除
     * 参考: ImGroupStatusEnum
     */
    private Integer status;

    /**
     * 自动拉人规则配置(JSON格式)
     */
    private String autoInviteRule;

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
