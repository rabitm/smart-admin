package net.lab1024.sa.admin.module.business.oa.police.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.constant.AdminSwaggerTagConst;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.CollaborationHistoryQueryForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.CollaborationOperationRecordForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.ComprehensiveOperationBatchForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.vo.CollaborationHistoryVO;
import net.lab1024.sa.admin.module.business.oa.police.service.CollaborationHistoryService;
import net.lab1024.sa.base.common.domain.PageResult;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.common.util.SmartRequestUtil;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.concurrent.CompletableFuture;

/**
 * 协作历史记录控制器
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@RestController
@RequestMapping("/api/collaboration/history")
@Tag(name = AdminSwaggerTagConst.Business.POLICE_COLLABORATION_HISTORY)
@Slf4j
public class CollaborationHistoryController {

    @Resource
    private CollaborationHistoryService collaborationHistoryService;

    @Operation(summary = "记录协作操作")
    @PostMapping("/record")
    public ResponseDTO<Boolean> recordOperation(@RequestBody @Valid CollaborationOperationRecordForm form,
                                               HttpServletRequest request) {
        try {
            // 补充IP地址和用户代理信息
            if (form.getIpAddress() == null) {
                form.setIpAddress(request.getRemoteAddr());
            }
            if (form.getUserAgent() == null) {
                form.setUserAgent(request.getHeader("User-Agent"));
            }

            boolean success = collaborationHistoryService.recordOperation(form);
            return ResponseDTO.ok(success);
        } catch (Exception e) {
            log.error("记录协作操作失败: {}", e.getMessage(), e);
            return ResponseDTO.userErrorParam("记录操作失败");
        }
    }

    @Operation(summary = "查询协作历史记录")
    @GetMapping("/query")
    public ResponseDTO<PageResult<CollaborationHistoryVO>> queryHistory(@Valid CollaborationHistoryQueryForm queryForm) {
        try {
            PageResult<CollaborationHistoryVO> result = collaborationHistoryService.queryCollaborationHistory(queryForm);
            return ResponseDTO.ok(result);
        } catch (Exception e) {
            log.error("查询协作历史记录失败: {}", e.getMessage(), e);
            return ResponseDTO.userErrorParam("查询历史记录失败");
        }
    }

    @Operation(summary = "撤销操作")
    @PostMapping("/undo/{operationId}")
    public ResponseDTO<Boolean> undoOperation(@PathVariable String operationId) {
        try {
            boolean success = collaborationHistoryService.undoOperation(operationId);
            return ResponseDTO.ok(success);
        } catch (Exception e) {
            log.error("撤销操作失败: operationId={}, error={}", operationId, e.getMessage(), e);
            return ResponseDTO.userErrorParam("撤销操作失败");
        }
    }

    @Operation(summary = "批量记录操作")
    @PostMapping("/batch-record")
    public ResponseDTO<Boolean> batchRecordOperations(@RequestBody @Valid ComprehensiveOperationBatchForm batchForm,
                                                     HttpServletRequest request) {
        try {
            // 补充通用信息
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            CompletableFuture<Boolean> future = collaborationHistoryService.batchRecordOperations(batchForm, ipAddress, userAgent);
            boolean success = future.get(); // 等待异步操作完成
            return ResponseDTO.ok(success);
        } catch (Exception e) {
            log.error("批量记录操作失败: {}", e.getMessage(), e);
            return ResponseDTO.userErrorParam("批量记录操作失败");
        }
    }

    @Operation(summary = "获取操作统计信息")
    @PostMapping("/statistics")
    public ResponseDTO<Object> getStatistics(@RequestBody @Valid CollaborationHistoryQueryForm queryForm) {
        try {
            Object statistics = collaborationHistoryService.getOperationStatistics(queryForm);
            return ResponseDTO.ok(statistics);
        } catch (Exception e) {
            log.error("获取统计信息失败: {}", e.getMessage(), e);
            return ResponseDTO.userErrorParam("获取统计信息失败");
        }
    }

    @Operation(summary = "导出操作历史")
    @PostMapping("/export")
    public ResponseDTO<String> exportHistory(@RequestBody @Valid CollaborationHistoryQueryForm queryForm,
                                           @RequestParam(defaultValue = "excel") String format) {
        try {
            String downloadUrl = collaborationHistoryService.exportOperationHistory(queryForm, format);
            return ResponseDTO.ok(downloadUrl);
        } catch (Exception e) {
            log.error("导出操作历史失败: {}", e.getMessage(), e);
            return ResponseDTO.userErrorParam("导出操作历史失败");
        }
    }

    @Operation(summary = "检测异常操作")
    @PostMapping("/detect-anomalies")
    public ResponseDTO<Object> detectAnomalies(@RequestBody @Valid CollaborationHistoryQueryForm queryForm) {
        try {
            Object anomalies = collaborationHistoryService.detectAnomalies(queryForm);
            return ResponseDTO.ok(anomalies);
        } catch (Exception e) {
            log.error("检测异常操作失败: {}", e.getMessage(), e);
            return ResponseDTO.userErrorParam("检测异常操作失败");
        }
    }
}