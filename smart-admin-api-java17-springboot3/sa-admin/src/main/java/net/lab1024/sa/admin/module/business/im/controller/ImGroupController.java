package net.lab1024.sa.admin.module.business.im.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.im.domain.entity.ImGroupMappingEntity;
import net.lab1024.sa.admin.module.business.im.service.ImPoliceGroupService;
import net.lab1024.sa.base.common.code.SystemErrorCode;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * IM群组管理控制器
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Slf4j
@RestController
@RequestMapping("/api/im/group")
@Tag(name = "IM群组管理")
@RequiredArgsConstructor
// 临时移除条件注解以便调试
// @ConditionalOnProperty(prefix = "openim", name = "api-url")
public class ImGroupController {

    private final ImPoliceGroupService imPoliceGroupService;

    /**
     * 获取警情群组信息（会自动邀请当前用户加入群组）
     */
    @Operation(summary = "获取警情群组信息")
    @GetMapping("/police/{reportId}")
    public ResponseDTO<ImGroupMappingEntity> getPoliceGroup(@PathVariable Long reportId) {
        try {
            ImGroupMappingEntity groupMapping = imPoliceGroupService.getPoliceGroup(reportId);

            if (groupMapping == null) {
                return ResponseDTO.userErrorParam("警情群组不存在");
            }

            // 自动邀请当前用户加入群组（如果还不是成员）
            boolean joinSuccess = imPoliceGroupService.autoJoinCurrentUser(reportId, groupMapping.getGroupId());

            // 如果加入失败，可能是群组在OpenIM中不存在（孤儿记录），自动重建
            if (!joinSuccess) {
                log.warn("📱 [群组管理] 检测到无效群组，自动重建 - 警情ID: {}, 旧GroupID: {}",
                        reportId, groupMapping.getGroupId());

                // 自动重建群组
                ImGroupMappingEntity newGroupMapping = imPoliceGroupService.rebuildPoliceGroup(reportId);

                if (newGroupMapping == null) {
                    return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "群组重建失败，请稍后重试");
                }

                // 重建成功后，自动邀请当前用户加入新群组
                boolean rejoinSuccess = imPoliceGroupService.autoJoinCurrentUser(reportId, newGroupMapping.getGroupId());

                if (!rejoinSuccess) {
                    log.error("📱 [群组管理] 重建后加入新群组失败 - 警情ID: {}", reportId);
                    return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "加入新群组失败，请刷新重试");
                }

                log.info("✅ [群组管理] 群组自动重建成功 - 警情ID: {}, 新GroupID: {}",
                        reportId, newGroupMapping.getGroupId());

                return ResponseDTO.ok(newGroupMapping);
            }

            return ResponseDTO.ok(groupMapping);

        } catch (Exception e) {
            log.error("📱 [群组管理] 获取警情群组失败 - 警情ID: {}", reportId, e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "获取警情群组失败: " + e.getMessage());
        }
    }

    /**
     * 手动创建警情群组
     */
    @Operation(summary = "手动创建警情群组")
    @PostMapping("/police/create/{reportId}")
    public ResponseDTO<ImGroupMappingEntity> createPoliceGroup(@PathVariable @NotNull Long reportId) {
        try {
            log.info("📱 [群组管理] 收到手动创建群组请求 - 警情ID: {}", reportId);

            ImGroupMappingEntity groupMapping = imPoliceGroupService.createOrGetPoliceGroup(reportId);

            if (groupMapping == null) {
                return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "群组创建失败");
            }

            return ResponseDTO.ok(groupMapping);

        } catch (Exception e) {
            log.error("📱 [群组管理] 创建警情群组失败 - 警情ID: {}", reportId, e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "创建警情群组失败: " + e.getMessage());
        }
    }

    /**
     * 重建警情群组（删除无效映射并重新创建）
     */
    @Operation(summary = "重建警情群组")
    @PostMapping("/police/rebuild/{reportId}")
    public ResponseDTO<ImGroupMappingEntity> rebuildPoliceGroup(@PathVariable @NotNull Long reportId) {
        try {
            log.info("📱 [群组管理] 收到重建群组请求 - 警情ID: {}", reportId);

            ImGroupMappingEntity newGroupMapping = imPoliceGroupService.rebuildPoliceGroup(reportId);

            if (newGroupMapping == null) {
                return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "群组重建失败");
            }

            return ResponseDTO.ok(newGroupMapping);

        } catch (Exception e) {
            log.error("📱 [群组管理] 重建警情群组失败 - 警情ID: {}", reportId, e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "重建警情群组失败: " + e.getMessage());
        }
    }

    /**
     * 邀请成员加入警情群组
     */
    @Operation(summary = "邀请成员加入警情群组")
    @PostMapping("/police/invite")
    public ResponseDTO<Void> inviteMembers(@RequestBody @Valid InviteMembersForm form) {
        try {
            boolean success = imPoliceGroupService.inviteMembers(form.getReportId(), form.getEmployeeIds());

            if (success) {
                return ResponseDTO.ok();
            } else {
                return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "邀请成员失败");
            }

        } catch (Exception e) {
            log.error("📱 [群组管理] 邀请成员失败 - 警情ID: {}", form.getReportId(), e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "邀请成员失败: " + e.getMessage());
        }
    }

    /**
     * 邀请成员表单
     */
    @Data
    public static class InviteMembersForm {
        @NotNull(message = "警情ID不能为空")
        private Long reportId;

        @NotEmpty(message = "成员列表不能为空")
        private List<Long> employeeIds;
    }
}
