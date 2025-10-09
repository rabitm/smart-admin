package net.lab1024.sa.admin.module.business.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IM用户映射实体
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Data
@TableName("t_im_user_mapping")
public class ImUserMappingEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * SmartAdmin员工ID
     */
    private Long employeeId;

    /**
     * OpenIM用户ID
     */
    private String openImUserId;

    /**
     * 同步状态: 1=已同步, 0=待同步
     */
    private Integer syncStatus;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;

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
