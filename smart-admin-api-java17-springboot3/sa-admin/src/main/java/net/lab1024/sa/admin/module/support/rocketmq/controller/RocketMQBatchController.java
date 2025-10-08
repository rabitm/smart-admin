package net.lab1024.sa.admin.module.support.rocketmq.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.rocketmq.batch.RocketMQBatchProcessor;
import net.lab1024.sa.base.common.code.SystemErrorCode;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * RocketMQ 批处理 API 控制器
 *
 * 功能:
 * 1. 获取批处理统计信息
 * 2. 强制刷新待处理消息
 * 3. 批处理配置管理
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Slf4j
@RestController
@RequestMapping("/api/rocketmq/batch")
@Tag(name = "RocketMQ批处理")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class RocketMQBatchController {

    private final RocketMQBatchProcessor batchProcessor;

    /**
     * 获取批处理统计信息
     */
    @Operation(summary = "获取批处理统计信息")
    @GetMapping("/stats")
    public ResponseDTO<RocketMQBatchProcessor.BatchStats> getStats() {
        try {
            RocketMQBatchProcessor.BatchStats stats = batchProcessor.getStats();
            return ResponseDTO.ok(stats);
        } catch (Exception e) {
            log.error("📦 [批处理API] 获取统计信息失败", e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 强制刷新所有待处理消息
     */
    @Operation(summary = "强制刷新所有待处理消息")
    @PostMapping("/flush")
    public ResponseDTO<Void> flushAll() {
        try {
            batchProcessor.flushAll();
            return ResponseDTO.ok();
        } catch (Exception e) {
            log.error("📦 [批处理API] 强制刷新失败", e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "强制刷新失败: " + e.getMessage());
        }
    }
}
