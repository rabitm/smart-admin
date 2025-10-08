package net.lab1024.sa.admin.module.support.rocketmq.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.rocketmq.monitor.RocketMQMonitorService;
import net.lab1024.sa.base.common.code.SystemErrorCode;
import net.lab1024.sa.base.common.code.UserErrorCode;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * RocketMQ 监控 API 控制器
 *
 * 功能:
 * 1. 获取RocketMQ整体监控指标
 * 2. 获取单个Topic的监控指标
 * 3. 获取RocketMQ健康状态
 * 4. 重置监控指标 (管理员功能)
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Slf4j
@RestController
@RequestMapping("/api/rocketmq/monitor")
@Tag(name = "RocketMQ监控")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class RocketMQMonitorController {

    private final RocketMQMonitorService monitorService;

    /**
     * 获取RocketMQ整体监控指标
     */
    @Operation(summary = "获取整体监控指标")
    @GetMapping("/metrics")
    public ResponseDTO<RocketMQMonitorService.MonitorMetrics> getMetrics() {
        try {
            RocketMQMonitorService.MonitorMetrics metrics = monitorService.getMetrics();
            return ResponseDTO.ok(metrics);
        } catch (Exception e) {
            log.error("📊 [监控API] 获取监控指标失败", e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "获取监控指标失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定Topic的监控指标
     */
    @Operation(summary = "获取Topic监控指标")
    @GetMapping("/topics/{topic}")
    public ResponseDTO<RocketMQMonitorService.TopicMetrics> getTopicMetrics(@PathVariable String topic) {
        try {
            RocketMQMonitorService.TopicMetrics topicMetrics = monitorService.getTopicMetrics(topic);
            if (topicMetrics == null) {
                return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST, "Topic不存在或暂无统计数据: " + topic);
            }
            return ResponseDTO.ok(topicMetrics);
        } catch (Exception e) {
            log.error("📊 [监控API] 获取Topic监控指标失败: topic={}", topic, e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "获取Topic监控指标失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有Topic的监控指标列表
     */
    @Operation(summary = "获取所有Topic监控指标")
    @GetMapping("/topics")
    public ResponseDTO<Map<String, RocketMQMonitorService.TopicMetrics>> getAllTopicMetrics() {
        try {
            Map<String, RocketMQMonitorService.TopicMetrics> allTopics = monitorService.getAllTopicMetrics();
            return ResponseDTO.ok(allTopics);
        } catch (Exception e) {
            log.error("📊 [监控API] 获取所有Topic监控指标失败", e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "获取所有Topic监控指标失败: " + e.getMessage());
        }
    }

    /**
     * 获取RocketMQ健康状态
     */
    @Operation(summary = "获取健康状态")
    @GetMapping("/health")
    public ResponseDTO<Map<String, Object>> getHealthStatus() {
        try {
            RocketMQMonitorService.MonitorMetrics metrics = monitorService.getMetrics();

            Map<String, Object> health = new HashMap<>();
            health.put("status", determineHealthStatus(metrics));
            health.put("successRate", metrics.getSuccessRate());
            health.put("avgSendTimeMs", metrics.getAvgSendTimeMs());
            health.put("totalSent", metrics.getTotalSent());
            health.put("totalFailed", metrics.getTotalFailed());
            health.put("lastMonitorTime", metrics.getLastMonitorTime());

            // 健康检查告警信息
            if (metrics.getSuccessRate() < 95.0 && metrics.getTotalSent() > 10) {
                health.put("warning", "消息发送成功率低于95%");
            }
            if (metrics.getAvgSendTimeMs() > 500) {
                health.put("warning", "消息发送平均耗时超过500ms");
            }

            return ResponseDTO.ok(health);
        } catch (Exception e) {
            log.error("📊 [监控API] 获取健康状态失败", e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "获取健康状态失败: " + e.getMessage());
        }
    }

    /**
     * 重置监控指标 (管理员功能)
     */
    @Operation(summary = "重置监控指标")
    @PostMapping("/reset")
    public ResponseDTO<Void> resetMetrics() {
        try {
            monitorService.resetMetrics();
            log.info("📊 [监控API] 监控指标已重置");
            return ResponseDTO.ok();
        } catch (Exception e) {
            log.error("📊 [监控API] 重置监控指标失败", e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "重置监控指标失败: " + e.getMessage());
        }
    }

    /**
     * 获取监控摘要 (简化版指标，用于首页展示)
     */
    @Operation(summary = "获取监控摘要")
    @GetMapping("/summary")
    public ResponseDTO<Map<String, Object>> getSummary() {
        try {
            RocketMQMonitorService.MonitorMetrics metrics = monitorService.getMetrics();

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalSent", metrics.getTotalSent());
            summary.put("successRate", String.format("%.2f%%", metrics.getSuccessRate()));
            summary.put("avgSendTime", metrics.getAvgSendTimeMs() + "ms");
            summary.put("status", determineHealthStatus(metrics));
            summary.put("topicCount", monitorService.getAllTopicMetrics().size());

            return ResponseDTO.ok(summary);
        } catch (Exception e) {
            log.error("📊 [监控API] 获取监控摘要失败", e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "获取监控摘要失败: " + e.getMessage());
        }
    }

    /**
     * 判断健康状态
     */
    private String determineHealthStatus(RocketMQMonitorService.MonitorMetrics metrics) {
        if (metrics.getTotalSent() == 0) {
            return "UNKNOWN";
        }

        // 成功率低于95%为不健康
        if (metrics.getSuccessRate() < 95.0) {
            return "UNHEALTHY";
        }

        // 平均耗时超过500ms为警告
        if (metrics.getAvgSendTimeMs() > 500) {
            return "WARNING";
        }

        // 最大耗时超过2000ms为警告
        if (metrics.getMaxSendTimeMs() > 2000) {
            return "WARNING";
        }

        return "HEALTHY";
    }
}
