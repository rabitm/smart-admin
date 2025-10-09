package net.lab1024.sa.admin.module.business.im.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.im.service.OpenIMHealthCheckService;
import net.lab1024.sa.base.common.code.SystemErrorCode;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * IM健康检查控制器
 *
 * 提供OpenIM服务健康状态监控端点
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-09
 * @Copyright 1024创新实验室
 */
@Slf4j
@RestController
@RequestMapping("/api/im/health")
@Tag(name = "IM健康检查")
@RequiredArgsConstructor
public class ImHealthController {

    private final OpenIMHealthCheckService healthCheckService;

    /**
     * 获取OpenIM服务健康状态
     */
    @Operation(summary = "获取OpenIM服务健康状态")
    @GetMapping("/status")
    public ResponseDTO<Map<String, Object>> getHealthStatus() {
        try {
            Map<String, Object> report = healthCheckService.getHealthReport();
            boolean healthy = (boolean) report.get("healthy");

            if (healthy) {
                return ResponseDTO.ok(report);
            } else {
                return ResponseDTO.errorData(SystemErrorCode.SYSTEM_ERROR, report);
            }

        } catch (Exception e) {
            log.error("❌ [健康检查] 获取健康状态失败", e);
            Map<String, Object> errorReport = new HashMap<>();
            errorReport.put("healthy", false);
            errorReport.put("error", e.getMessage());
            return ResponseDTO.errorData(SystemErrorCode.SYSTEM_ERROR, errorReport);
        }
    }

    /**
     * 立即执行健康检查
     */
    @Operation(summary = "立即执行健康检查")
    @GetMapping("/check")
    public ResponseDTO<Map<String, Object>> performHealthCheck() {
        try {
            boolean result = healthCheckService.checkHealth();

            Map<String, Object> response = new HashMap<>();
            response.put("checkResult", result);
            response.put("message", result ? "健康检查通过" : "健康检查失败");
            response.put("report", healthCheckService.getHealthReport());

            return ResponseDTO.ok(response);

        } catch (Exception e) {
            log.error("❌ [健康检查] 执行健康检查失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("checkResult", false);
            errorResponse.put("error", e.getMessage());
            return ResponseDTO.errorData(SystemErrorCode.SYSTEM_ERROR, errorResponse);
        }
    }

    /**
     * 获取熔断器状态
     */
    @Operation(summary = "获取熔断器状态")
    @GetMapping("/circuit-breaker")
    public ResponseDTO<Map<String, Object>> getCircuitBreakerStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("circuitBreakerOpen", healthCheckService.isCircuitBreakerOpen());
            status.put("consecutiveFailures", healthCheckService.getConsecutiveFailures());
            status.put("lastCheckTime", healthCheckService.getLastCheckTime());
            status.put("lastSuccessTime", healthCheckService.getLastSuccessTime());

            return ResponseDTO.ok(status);

        } catch (Exception e) {
            log.error("❌ [健康检查] 获取熔断器状态失败", e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "获取熔断器状态失败");
        }
    }
}
