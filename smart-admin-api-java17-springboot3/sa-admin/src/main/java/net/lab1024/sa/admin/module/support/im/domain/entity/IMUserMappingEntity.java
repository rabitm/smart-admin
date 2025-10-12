package net.lab1024.sa.admin.module.support.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IM用户映射实体
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@TableName("t_im_user_mapping")
public class IMUserMappingEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * OpenIM用户ID
     */
    private String openimUserId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String faceUrl;

    /**
     * 同步状态:1-已同步,2-同步失败,3-待同步
     */
    private Integer syncStatus;

    /**
     * 同步时间
     */
    private LocalDateTime syncTime;

    /**
     * 错误信息
     */
    private String errorMessage;

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
