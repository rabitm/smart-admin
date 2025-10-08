package net.lab1024.sa.admin.module.support.rocketmq.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.rocketmq.test.RocketMQLoadTestService;
import net.lab1024.sa.base.common.code.SystemErrorCode;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * RocketMQ 负载测试 API 控制器
 *
 * 功能:
 * 1. 启动负载压力测试
 * 2. 停止正在运行的测试
 * 3. 获取测试进度和报告
 * 4. 预设测试场景
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Slf4j
@RestController
@RequestMapping("/api/rocketmq/load-test")
@Tag(name = "RocketMQ负载测试")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class RocketMQLoadTestController {

    private final RocketMQLoadTestService loadTestService;

    /**
     * 启动自定义负载测试
     */
    @Operation(summary = "启动自定义负载测试")
    @PostMapping("/start")
    public ResponseDTO<String> startLoadTest(@RequestBody RocketMQLoadTestService.LoadTestConfig config) {
        try {
            // 验证参数
            if (config.getConcurrentUsers() < 1 || config.getConcurrentUsers() > 500) {
                return ResponseDTO.userErrorParam("并发用户数必须在1-500之间");
            }
            if (config.getTotalMessages() < 1) {
                return ResponseDTO.userErrorParam("总消息数必须大于0");
            }

            String testId = loadTestService.startLoadTest(config);
            return ResponseDTO.ok(testId);
        } catch (IllegalStateException e) {
            return ResponseDTO.userErrorParam(e.getMessage());
        } catch (Exception e) {
            log.error("🧪 [负载测试] 启动测试失败", e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "启动测试失败: " + e.getMessage());
        }
    }

    /**
     * 启动轻量级测试 (100并发用户, 10000消息)
     */
    @Operation(summary = "启动轻量级测试")
    @PostMapping("/start/light")
    public ResponseDTO<String> startLightTest() {
        RocketMQLoadTestService.LoadTestConfig config = new RocketMQLoadTestService.LoadTestConfig();
        config.setTestId("LIGHT_TEST");
        config.setConcurrentUsers(100);
        config.setTotalMessages(10000);
        config.setMessageSize(512);

        return startLoadTest(config);
    }

    /**
     * 启动中等负载测试 (200并发用户, 50000消息)
     */
    @Operation(summary = "启动中等负载测试")
    @PostMapping("/start/medium")
    public ResponseDTO<String> startMediumTest() {
        RocketMQLoadTestService.LoadTestConfig config = new RocketMQLoadTestService.LoadTestConfig();
        config.setTestId("MEDIUM_TEST");
        config.setConcurrentUsers(200);
        config.setTotalMessages(50000);
        config.setMessageSize(1024);

        return startLoadTest(config);
    }

    /**
     * 启动重度负载测试 (500并发用户, 100000消息)
     */
    @Operation(summary = "启动重度负载测试")
    @PostMapping("/start/heavy")
    public ResponseDTO<String> startHeavyTest() {
        RocketMQLoadTestService.LoadTestConfig config = new RocketMQLoadTestService.LoadTestConfig();
        config.setTestId("HEAVY_TEST");
        config.setConcurrentUsers(500);
        config.setTotalMessages(100000);
        config.setMessageSize(2048);

        return startLoadTest(config);
    }

    /**
     * 启动持久性测试 (100并发用户, 100000消息, 有间隔)
     */
    @Operation(summary = "启动持久性测试")
    @PostMapping("/start/endurance")
    public ResponseDTO<String> startEnduranceTest() {
        RocketMQLoadTestService.LoadTestConfig config = new RocketMQLoadTestService.LoadTestConfig();
        config.setTestId("ENDURANCE_TEST");
        config.setConcurrentUsers(100);
        config.setTotalMessages(100000);
        config.setMessageSize(1024);
        config.setMessageIntervalMs(10); // 10ms间隔,避免过载

        return startLoadTest(config);
    }

    /**
     * 停止负载测试
     */
    @Operation(summary = "停止负载测试")
    @PostMapping("/stop")
    public ResponseDTO<Void> stopLoadTest() {
        try {
            loadTestService.stopLoadTest();
            return ResponseDTO.ok();
        } catch (Exception e) {
            log.error("🧪 [负载测试] 停止测试失败", e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "停止测试失败: " + e.getMessage());
        }
    }

    /**
     * 获取测试报告
     */
    @Operation(summary = "获取测试报告")
    @GetMapping("/report")
    public ResponseDTO<RocketMQLoadTestService.LoadTestReport> getTestReport() {
        try {
            RocketMQLoadTestService.LoadTestReport report = loadTestService.getTestReport();
            return ResponseDTO.ok(report);
        } catch (Exception e) {
            log.error("🧪 [负载测试] 获取测试报告失败", e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "获取测试报告失败: " + e.getMessage());
        }
    }
}
