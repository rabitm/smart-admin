package net.lab1024.sa.admin.module.support.im.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.im.domain.form.IMUserMappingQueryForm;
import net.lab1024.sa.admin.module.support.im.service.IMUserSyncService;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.Map;

/**
 * IM 用户映射控制器
 * 为前端提供用户映射查询接口
 *
 * 用途:
 * - 前端邀请成员到群组时，需要先获取成员的 OpenIM 用户 ID
 * - 批量查询可以减少网络请求次数
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-10
 * @Copyright 1024创新实验室
 */
@Slf4j
@Tag(name = "IM 用户映射管理")
@RestController
@RequestMapping("/api/im/user-mapping")
public class IMUserMappingController {

    @Resource
    private IMUserSyncService imUserSyncService;

    /**
     * 批量获取用户映射
     * 前端在邀请成员时调用此接口获取 OpenIM 用户 ID
     */
    @Operation(summary = "批量获取用户映射")
    @PostMapping("/batch")
    public ResponseDTO<Map<Long, String>> batchGetUserMapping(@Valid @RequestBody IMUserMappingQueryForm form) {
        log.info("📋 [用户映射] 批量查询用户映射, 数量: {}", form.getEmployeeIds().size());

        // 批量获取映射
        Map<Long, String> mappingMap = imUserSyncService.batchGetOpenIMUserIds(form.getEmployeeIds());

        log.info("✅ [用户映射] 查询成功, 返回数量: {}", mappingMap.size());

        return ResponseDTO.ok(mappingMap);
    }

    /**
     * 获取单个用户映射
     */
    @Operation(summary = "获取单个用户映射")
    @GetMapping("/{employeeId}")
    public ResponseDTO<String> getUserMapping(@PathVariable Long employeeId) {
        log.info("🔍 [用户映射] 查询用户映射, employeeId: {}", employeeId);

        String openimUserId = imUserSyncService.getOpenIMUserId(employeeId);

        return ResponseDTO.ok(openimUserId);
    }
}
