package net.lab1024.sa.admin.module.support.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IM群组映射实体
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@TableName("t_im_group_mapping")
public class IMGroupMappingEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 警情ID
     */
    private Long reportId;

    /**
     * OpenIM群组ID
     */
    private String openimGroupId;

    /**
     * 群组名称
     */
    private String groupName;

    /**
     * 群组类型:2-工作群
     */
    private Integer groupType;

    /**
     * 群主OpenIM用户ID
     */
    private String ownerUserId;

    /**
     * 群主员工ID
     */
    private Long ownerEmployeeId;

    /**
     * 群组状态:1-正常,2-已解散,3-已归档
     */
    private Integer groupStatus;

    /**
     * 成员数量
     */
    private Integer memberCount;

    /**
     * 最大成员数量
     */
    private Integer maxMemberCount;

    /**
     * 群公告
     */
    private String notification;

    /**
     * 群简介
     */
    private String introduction;

    /**
     * 群头像URL
     */
    private String faceUrl;

    /**
     * 是否需要验证
     */
    private Boolean needVerification;

    /**
     * 删除标识
     */
    private Boolean deletedFlag;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
