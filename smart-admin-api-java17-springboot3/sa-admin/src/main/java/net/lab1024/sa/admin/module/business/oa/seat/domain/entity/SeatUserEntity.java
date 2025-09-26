package net.lab1024.sa.admin.module.business.oa.seat.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 席位用户关联实体类
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-25
 * @Copyright 1024创新实验室
 */
@Data
@TableName("t_oa_seat_user")
public class SeatUserEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 席位ID
     */
    private Long seatId;

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 在线状态: 0-离线 1-在线 2-忙碌 3-离开
     */
    private Integer onlineStatus;

    /**
     * 当前处理的警情ID
     */
    private Long currentCaseId;

    /**
     * 处理数量
     */
    private Integer handleCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}