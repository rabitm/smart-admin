package net.lab1024.sa.admin.module.business.oa.police.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 警情编辑锁实体类
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright: 1024创新实验室
 */
@Data
@TableName("t_police_edit_lock")
public class PoliceEditLockEntity {

    @TableId(type = IdType.AUTO)
    private Long lockId;

    /**
     * 警情ID
     */
    private Long policeCaseId;

    /**
     * 锁类型：1-读锁 2-写锁
     */
    private Integer lockType;

    /**
     * 锁定用户ID
     */
    private Long lockUserId;

    /**
     * 锁定用户名称
     */
    private String lockUserName;

    /**
     * 锁定用户座位ID
     */
    private Long lockSeatId;

    /**
     * 锁定时间
     */
    private LocalDateTime lockTime;

    /**
     * 锁过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 锁状态：0-已释放 1-锁定中
     */
    private Integer lockStatus;

    /**
     * 锁版本号，用于防止并发问题
     */
    private Integer lockVersion;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}