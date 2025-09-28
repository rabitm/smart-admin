package net.lab1024.sa.admin.module.business.oa.police.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.oa.police.service.PoliceOperationHistoryService;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.common.util.SmartRequestUtil;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 警情操作历史控制器 - 高性能、非阻塞
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-28
 * @Copyright 1024创新实验室
 */
@Slf4j
@Tag(name = "警情操作历史")
@RestController
@RequestMapping("/oa/police/operation-history")
@RequiredArgsConstructor
public class PoliceOperationHistoryController {

    private final PoliceOperationHistoryService operationHistoryService;

    /**
     * 获取操作历史 - 分页查询
     */
    @Operation(summary = "获取操作历史")
    @GetMapping("/{reportId}")
    public ResponseDTO<Map<String, Object>> getOperationHistory(
            @PathVariable Long reportId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        // 限制查询范围，防止大查询影响性能
        int limitedPageSize = Math.min(Math.max(pageSize, 1), 100);
        int limitedPageNum = Math.max(pageNum, 1);

        try {
            Map<String, Object> result = operationHistoryService.getOperationHistory(
                reportId, limitedPageNum, limitedPageSize
            );

            log.debug("📖 操作历史查询完成: reportId={}, page={}, size={}",
                     reportId, limitedPageNum, limitedPageSize);

            return ResponseDTO.ok(result);
        } catch (Exception e) {
            log.warn("⚠️ 操作历史查询异常: reportId={}, error={}", reportId, e.getMessage());

            // 返回空结果而不是错误，保证前端正常显示
            Map<String, Object> emptyResult = Map.of(
                "list", java.util.Collections.emptyList(),
                "total", 0,
                "pageNum", limitedPageNum,
                "pageSize", limitedPageSize
            );
            return ResponseDTO.ok(emptyResult);
        }
    }

    /**
     * 异步记录操作 - 火焰式提交，不等待响应
     */
    @Operation(summary = "记录操作历史")
    @PostMapping("/{reportId}/record")
    public ResponseDTO<String> recordOperation(
            @PathVariable Long reportId,
            @RequestParam String operation,
            @RequestBody(required = false) Map<String, Object> details) {

        try {
            Long userId = SmartRequestUtil.getRequestUserId();
            String userName = SmartRequestUtil.getRequestUser().getUserName();

            // 异步提交，立即返回，不等待处理结果
            operationHistoryService.recordOperationAsync(reportId, userId, userName, operation, details);

            log.debug("🔥 操作历史记录已提交: reportId={}, operation={}", reportId, operation);
            return ResponseDTO.ok("已提交");
        } catch (Exception e) {
            log.warn("⚠️ 操作历史记录提交失败: reportId={}, operation={}, error={}",
                     reportId, operation, e.getMessage());

            // 即使失败也返回成功，不影响主业务
            return ResponseDTO.ok("已提交");
        }
    }

    /**
     * 预热缓存 - 警情详情页面加载时调用
     */
    @Operation(summary = "预热操作历史缓存")
    @PostMapping("/{reportId}/preload")
    public ResponseDTO<String> preloadCache(@PathVariable Long reportId) {
        try {
            // 异步预热，立即返回
            operationHistoryService.preloadOperationHistoryAsync(reportId);

            log.debug("🔄 操作历史缓存预热已启动: reportId={}", reportId);
            return ResponseDTO.ok("预热已启动");
        } catch (Exception e) {
            log.warn("⚠️ 操作历史缓存预热失败: reportId={}, error={}", reportId, e.getMessage());
            return ResponseDTO.ok("预热已启动"); // 失败也返回成功
        }
    }

    /**
     * 清理缓存 - 警情删除时调用
     */
    @Operation(summary = "清理操作历史缓存")
    @PostMapping("/{reportId}/cache/clear")
    public ResponseDTO<String> clearCache(@PathVariable Long reportId) {
        try {
            // 异步清理，立即返回
            operationHistoryService.clearOperationHistoryAsync(reportId);

            log.debug("🗑️ 操作历史缓存清理已启动: reportId={}", reportId);
            return ResponseDTO.ok("清理已启动");
        } catch (Exception e) {
            log.warn("⚠️ 操作历史缓存清理失败: reportId={}, error={}", reportId, e.getMessage());
            return ResponseDTO.ok("清理已启动");
        }
    }

    /**
     * 健康检查
     */
    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public ResponseDTO<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = operationHistoryService.healthCheck();
            return ResponseDTO.ok(health);
        } catch (Exception e) {
            log.warn("⚠️ 健康检查失败: {}", e.getMessage());
            return ResponseDTO.userErrorParam("服务暂时不可用");
        }
    }
}