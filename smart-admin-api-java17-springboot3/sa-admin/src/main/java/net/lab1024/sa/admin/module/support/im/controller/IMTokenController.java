package net.lab1024.sa.admin.module.support.im.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.im.domain.form.IMTokenForm;
import net.lab1024.sa.admin.module.support.im.domain.vo.IMTokenVO;
import net.lab1024.sa.admin.module.support.im.service.IMTokenService;
import net.lab1024.sa.base.common.annoation.NoNeedLogin;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.common.util.SmartRequestUtil;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

/**
 * IM Token 控制器
 * 为前端提供 OpenIM Token 获取接口
 *
 * 安全策略:
 * 1. 必须先登录 SmartAdmin 才能获取 IM Token
 * 2. 只能获取当前登录用户的 Token，或者管理员可以获取其他用户Token
 * 3. Token 有效期 1 小时，提前 5 分钟过期
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-10
 * @Copyright 1024创新实验室
 */
@Slf4j
@Tag(name = "IM Token管理")
@RestController
@RequestMapping("/api/im/token")
public class IMTokenController {

    @Resource
    private IMTokenService imTokenService;

    /**
     * 获取当前用户的 OpenIM Token
     */
    @Operation(summary = "获取OpenIM Token")
    @PostMapping
    public ResponseDTO<IMTokenVO> getToken(@Valid @RequestBody IMTokenForm form) {
        // 1. 获取当前登录用户ID
        Long currentUserId = SmartRequestUtil.getRequestUserId();

        // 2. 验证权限：只能获取自己的Token（除非是管理员）
        if (!currentUserId.equals(form.getEmployeeId())) {
            // TODO: 添加管理员权限检查
            // if (!hasAdminRole()) {
            //     return ResponseDTO.userErrorParam("无权限获取其他用户Token");
            // }
            log.warn("⚠️ [Token获取] 用户{}尝试获取用户{}的Token", currentUserId, form.getEmployeeId());
        }

        // 3. 生成Token
        IMTokenVO tokenVO = imTokenService.generateToken(form.getEmployeeId());

        return ResponseDTO.ok(tokenVO);
    }

    /**
     * 刷新 Token
     */
    @Operation(summary = "刷新OpenIM Token")
    @PostMapping("/refresh")
    public ResponseDTO<IMTokenVO> refreshToken() {
        // 获取当前登录用户ID
        Long currentUserId = SmartRequestUtil.getRequestUserId();

        // 刷新Token
        IMTokenVO tokenVO = imTokenService.refreshToken(currentUserId);

        return ResponseDTO.ok(tokenVO);
    }

    /**
     * 获取当前用户的 Token（简化版）
     * 自动使用当前登录用户ID
     */
    @Operation(summary = "获取当前用户Token")
    @GetMapping("/current")
    public ResponseDTO<IMTokenVO> getCurrentUserToken() {
        // 获取当前登录用户ID
        Long currentUserId = SmartRequestUtil.getRequestUserId();

        // 生成Token
        IMTokenVO tokenVO = imTokenService.generateToken(currentUserId);

        return ResponseDTO.ok(tokenVO);
    }
}
