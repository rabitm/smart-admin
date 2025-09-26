package net.lab1024.sa.admin.module.business.oa.seat.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 席位VO
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-25
 * @Copyright 1024创新实验室
 */
@Data
@Schema(description = "席位VO")
public class SeatVO {

    @Schema(description = "席位ID")
    private Long seatId;

    @Schema(description = "席位编码")
    private String seatCode;

    @Schema(description = "席位名称")
    private String seatName;

    @Schema(description = "席位类型: 1-接警席 2-处警席 3-督导席")
    private Integer seatType;

    @Schema(description = "席位类型名称")
    private String seatTypeName;

    @Schema(description = "席位X坐标")
    private Integer positionX;

    @Schema(description = "席位Y坐标")
    private Integer positionY;

    @Schema(description = "楼层")
    private Integer floorNumber;

    @Schema(description = "区域")
    private String area;

    @Schema(description = "IP地址")
    private String ipAddress;

    @Schema(description = "MAC地址")
    private String macAddress;

    @Schema(description = "席位状态: 0-空闲 1-忙碌 2-离线 3-维护")
    private Integer seatStatus;

    @Schema(description = "席位状态名称")
    private String seatStatusName;

    @Schema(description = "启用状态")
    private Boolean enabledFlag;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "删除标识")
    private Boolean deletedFlag;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "创建人姓名")
    private String createUserName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}