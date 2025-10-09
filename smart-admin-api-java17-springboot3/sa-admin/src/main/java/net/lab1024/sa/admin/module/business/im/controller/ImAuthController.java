package net.lab1024.sa.admin.module.business.im.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.im.domain.vo.ImTokenVO;
import net.lab1024.sa.admin.module.business.im.service.ImUserSyncService;
import net.lab1024.sa.base.common.code.SystemErrorCode;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.common.util.SmartRequestUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * IM认证控制器
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Slf4j
@RestController
@RequestMapping("/api/im/auth")
@Tag(name = "IM认证")
@RequiredArgsConstructor
// 临时移除条件注解以便调试
// @ConditionalOnProperty(prefix = "openim", name = "api-url")
public class ImAuthController {

    private final ImUserSyncService imUserSyncService;

    /**
     * 获取IM Token
     */
    @Operation(summary = "获取IM Token")
    @GetMapping("/token")
    public ResponseDTO<ImTokenVO> getToken() {
        try {
            // 获取当前登录用户ID
            Long employeeId = SmartRequestUtil.getRequestUserId();

            // 获取IM Token
            ImTokenVO tokenVO = imUserSyncService.getImToken(employeeId);

            return ResponseDTO.ok(tokenVO);

        } catch (Exception e) {
            log.error("📱 [IM认证] 获取Token失败", e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "获取Token失败: " + e.getMessage());
        }
    }

    /**
     * 刷新IM Token
     */
    @Operation(summary = "刷新IM Token")
    @PostMapping("/refresh")
    public ResponseDTO<ImTokenVO> refreshToken() {
        try {
            // 获取当前登录用户ID
            Long employeeId = SmartRequestUtil.getRequestUserId();

            // 重新获取Token (效果同getToken)
            ImTokenVO tokenVO = imUserSyncService.getImToken(employeeId);

            return ResponseDTO.ok(tokenVO);

        } catch (Exception e) {
            log.error("📱 [IM认证] 刷新Token失败", e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "刷新Token失败: " + e.getMessage());
        }
    }
}
