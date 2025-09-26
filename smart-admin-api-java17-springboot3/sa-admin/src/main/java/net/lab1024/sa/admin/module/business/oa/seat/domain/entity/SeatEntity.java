package net.lab1024.sa.admin.module.business.oa.seat.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 席位信息实体类
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-25
 * @Copyright 1024创新实验室
 */
@Data
@TableName("t_oa_seat")
public class SeatEntity {

    @TableId(type = IdType.AUTO)
    private Long seatId;

    /**
     * 席位编码
     */
    private String seatCode;

    /**
     * 席位名称
     */
    private String seatName;

    /**
     * 席位类型: 1-接警席 2-处警席 3-督导席
     */
    private Integer seatType;

    /**
     * 席位X坐标
     */
    private Integer positionX;

    /**
     * 席位Y坐标
     */
    private Integer positionY;

    /**
     * 楼层
     */
    private Integer floorNumber;

    /**
     * 区域
     */
    private String area;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * MAC地址
     */
    private String macAddress;

    /**
     * 状态: 0-空闲 1-忙碌 2-离线 3-维护
     */
    private Integer seatStatus;

    /**
     * 启用状态: 0-禁用 1-启用
     */
    private Boolean enabledFlag;

    /**
     * 备注
     */
    private String remark;

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