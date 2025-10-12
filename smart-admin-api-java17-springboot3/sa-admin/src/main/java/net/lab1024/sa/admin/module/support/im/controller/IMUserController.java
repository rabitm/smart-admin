package net.lab1024.sa.admin.module.support.im.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.lab1024.sa.admin.module.support.im.service.IMUserSyncService;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * IM用户管理Controller
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Tag(name = "IM用户管理")
@RestController
@RequestMapping("/im/user")
public class IMUserController {

    @Resource
    private IMUserSyncService imUserSyncService;

    @Operation(summary = "同步单个用户到OpenIM")
    @PostMapping("/sync/{employeeId}")
    public ResponseDTO<String> syncUser(@PathVariable Long employeeId) {
        String openimUserId = imUserSyncService.syncUser(employeeId);
        return ResponseDTO.ok(openimUserId);
    }

    @Operation(summary = "批量同步用户")
    @PostMapping("/sync/batch")
    public ResponseDTO<Map<String, Object>> batchSync(@RequestBody List<Long> employeeIds) {
        Map<String, Object> result = imUserSyncService.batchSyncUsers(employeeIds);
        return ResponseDTO.ok(result);
    }

    @Operation(summary = "同步所有用户(异步)")
    @PostMapping("/sync/all")
    public ResponseDTO<String> syncAll() {
        imUserSyncService.syncAllUsersAsync();
        return ResponseDTO.ok("同步任务已提交,请稍后查看结果");
    }

    @Operation(summary = "增量同步(仅同步未同步或失败的用户)")
    @PostMapping("/sync/incremental")
    public ResponseDTO<Map<String, Object>> incrementalSync() {
        Map<String, Object> result = imUserSyncService.incrementalSync();
        return ResponseDTO.ok(result);
    }

    /**
     * @deprecated 请使用 IMUserMappingController.getUserMapping() 替代
     * @see net.lab1024.sa.admin.module.support.im.controller.IMUserMappingController#getUserMapping(Long)
     */
    @Deprecated
    @Operation(summary = "根据员工ID获取OpenIM用户ID (已弃用,请使用 /api/im/user-mapping/{employeeId})")
    @GetMapping("/openim-id/{employeeId}")
    public ResponseDTO<String> getOpenIMUserId(@PathVariable Long employeeId) {
        String openimUserId = imUserSyncService.getOpenIMUserId(employeeId);
        return ResponseDTO.ok(openimUserId);
    }
}
