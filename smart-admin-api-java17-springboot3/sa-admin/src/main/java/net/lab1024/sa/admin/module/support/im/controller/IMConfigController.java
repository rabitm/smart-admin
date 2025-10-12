package net.lab1024.sa.admin.module.support.im.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.lab1024.sa.admin.module.support.im.client.OpenIMTokenManager;
import net.lab1024.sa.admin.module.support.im.config.OpenIMConfig;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * IM配置管理Controller
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Tag(name = "IM配置管理")
@RestController
@RequestMapping("/im/config")
public class IMConfigController {

    @Resource
    private OpenIMConfig openIMConfig;

    @Resource
    private OpenIMTokenManager openIMTokenManager;

    @Operation(summary = "获取IM配置信息")
    @GetMapping("/info")
    public ResponseDTO<Map<String, Object>> getConfigInfo() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", openIMConfig.getEnabled());
        config.put("apiUrl", openIMConfig.getApiUrl());
        config.put("wsUrl", openIMConfig.getWsUrl());
        config.put("autoCreateGroup", openIMConfig.getAutoCreateGroup());
        config.put("autoInvite", openIMConfig.getAutoInvite());
        config.put("groupMaxMembers", openIMConfig.getGroupMaxMembers());

        return ResponseDTO.ok(config);
    }

    @Operation(summary = "获取Token缓存信息")
    @GetMapping("/token-info")
    public ResponseDTO<Map<String, Object>> getTokenInfo() {
        Map<String, Object> tokenInfo = openIMTokenManager.getTokenCacheInfo();
        return ResponseDTO.ok(tokenInfo);
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/token-refresh")
    public ResponseDTO<String> refreshToken() {
        String token = openIMTokenManager.refreshToken();
        return ResponseDTO.ok(token);
    }

    @Operation(summary = "清除Token缓存")
    @PostMapping("/token-clear")
    public ResponseDTO<Void> clearToken() {
        openIMTokenManager.clearToken();
        return ResponseDTO.ok();
    }

    @Operation(summary = "检查IM服务连接状态")
    @GetMapping("/health")
    public ResponseDTO<Map<String, Object>> checkHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            // 尝试获取Token
            String token = openIMTokenManager.getToken();
            health.put("status", "healthy");
            health.put("tokenExists", token != null && !token.isEmpty());
            health.put("configValid", openIMConfig.isConfigValid());
        } catch (Exception e) {
            health.put("status", "unhealthy");
            health.put("error", e.getMessage());
        }

        return ResponseDTO.ok(health);
    }
}
