package net.lab1024.sa.admin.module.business.oa.seat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.lab1024.sa.admin.module.business.oa.seat.domain.entity.SeatEntity;
import net.lab1024.sa.admin.module.business.oa.seat.domain.form.SeatQueryForm;
import net.lab1024.sa.admin.module.business.oa.seat.domain.vo.SeatVO;
import net.lab1024.sa.admin.module.business.oa.seat.service.SeatService;
import net.lab1024.sa.base.common.domain.PageResult;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.common.domain.ValidateList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 座位管理控制器
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-25
 * @Copyright 1024创新实验室
 */
@RestController
@RequestMapping("/seat")
@Tag(name = "座位管理")
public class SeatController {

    @Autowired
    private SeatService seatService;

    @Operation(summary = "分页查询座位列表")
    @PostMapping("/query")
    public ResponseDTO<PageResult<SeatVO>> queryPage(@RequestBody SeatQueryForm queryForm) {
        return ResponseDTO.ok(seatService.queryPage(queryForm));
    }

    @Operation(summary = "获取座位详情")
    @GetMapping("/get/{seatId}")
    public ResponseDTO<SeatVO> getDetail(@PathVariable Long seatId) {
        return seatService.getDetail(seatId);
    }

    @Operation(summary = "添加座位")
    @PostMapping("/add")
    public ResponseDTO<String> add(@RequestBody SeatEntity seatEntity) {
        return seatService.add(seatEntity);
    }

    @Operation(summary = "更新座位")
    @PostMapping("/update")
    public ResponseDTO<String> update(@RequestBody SeatEntity seatEntity) {
        return seatService.update(seatEntity);
    }

    @Operation(summary = "删除座位")
    @PostMapping("/delete/{seatId}")
    public ResponseDTO<String> delete(@PathVariable Long seatId) {
        return seatService.delete(seatId);
    }

    @Operation(summary = "批量删除座位")
    @PostMapping("/batchDelete")
    public ResponseDTO<String> batchDelete(@RequestBody ValidateList<Long> seatIdList) {
        ResponseDTO<String> result = ResponseDTO.ok();
        for (Long seatId : seatIdList) {
            ResponseDTO<String> deleteResult = seatService.delete(seatId);
            if (!deleteResult.getOk()) {
                result = deleteResult;
                break;
            }
        }
        return result;
    }

    @Operation(summary = "预约座位")
    @PostMapping("/reserve")
    public ResponseDTO<String> reserve(@RequestParam Long seatId,
                                       @RequestParam Long userId,
                                       @RequestParam String userName,
                                       @RequestParam LocalDateTime startTime,
                                       @RequestParam LocalDateTime endTime) {
        return seatService.reserve(seatId, userId, userName, startTime, endTime);
    }

    @Operation(summary = "取消预约")
    @PostMapping("/cancelReservation/{seatId}")
    public ResponseDTO<String> cancelReservation(@PathVariable Long seatId) {
        return seatService.release(seatId);
    }

    @Operation(summary = "占用座位")
    @PostMapping("/occupy/{seatId}")
    public ResponseDTO<String> occupy(@PathVariable Long seatId,
                                      @RequestParam Long userId,
                                      @RequestParam String userName) {
        return seatService.occupy(seatId, userId, userName);
    }

    @Operation(summary = "释放座位")
    @PostMapping("/release/{seatId}")
    public ResponseDTO<String> release(@PathVariable Long seatId) {
        return seatService.release(seatId);
    }
}