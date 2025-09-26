package net.lab1024.sa.admin.module.system.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@RestController
@RequestMapping("/api")
@Tag(name = "系统健康检查")
public class HealthController {

    @Operation(summary = "健康检查/心跳检测")
    @GetMapping("/ping")
    public ResponseDTO<Map<String, Object>> ping() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("message", "pong");
        result.put("timestamp", LocalDateTime.now());
        result.put("service", "SmartAdmin");
        return ResponseDTO.ok(result);
    }

    @Operation(summary = "服务健康状态")
    @GetMapping("/health")
    public ResponseDTO<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", LocalDateTime.now());
        result.put("service", "SmartAdmin API");
        result.put("version", "v3.27.0");
        return ResponseDTO.ok(result);
    }
}