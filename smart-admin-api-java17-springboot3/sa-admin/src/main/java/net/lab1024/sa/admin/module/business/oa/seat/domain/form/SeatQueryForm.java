package net.lab1024.sa.admin.module.business.oa.seat.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import net.lab1024.sa.base.common.domain.PageParam;

/**
 * 席位查询表单
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-25
 * @Copyright 1024创新实验室
 */
@Data
@Schema(description = "席位查询表单")
public class SeatQueryForm extends PageParam {

    @Schema(description = "席位编码")
    private String seatCode;

    @Schema(description = "席位名称")
    private String seatName;

    @Schema(description = "席位类型: 1-接警席 2-处警席 3-督导席")
    private Integer seatType;

    @Schema(description = "席位状态: 0-空闲 1-忙碌 2-离线 3-维护")
    private Integer seatStatus;

    @Schema(description = "区域")
    private String area;

    @Schema(description = "楼层")
    private Integer floorNumber;
}