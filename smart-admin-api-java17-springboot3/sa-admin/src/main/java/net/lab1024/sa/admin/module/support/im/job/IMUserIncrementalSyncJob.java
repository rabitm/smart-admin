package net.lab1024.sa.admin.module.support.im.job;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.im.service.IMUserSyncService;
import net.lab1024.sa.base.module.support.job.core.SmartJob;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenIM用户增量同步定时任务
 *
 * 功能:
 * 1. 定时增量同步员工到OpenIM
 * 2. 监控同步状态和统计
 * 3. 异常告警
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-11
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Service
public class IMUserIncrementalSyncJob implements SmartJob {

    @Resource
    private IMUserSyncService imUserSyncService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 执行增量同步任务
     *
     * @param param 可选参数 (不使用)
     * @return 执行结果说明
     */
    @Override
    public String run(String param) {
        String currentTime = LocalDateTime.now().format(FORMATTER);

        log.info("⏰ ==================== OpenIM用户增量同步任务开始 ====================");
        log.info("⏰ 执行时间: {}", currentTime);
        log.info("⏰ IMUserSyncService注入状态: {}", imUserSyncService != null ? "已注入" : "未注入");

        // 检查依赖注入
        if (imUserSyncService == null) {
            String errorMsg = "IMUserSyncService依赖注入失败，无法执行同步任务";
            log.error("❌ {}", errorMsg);
            throw new RuntimeException(errorMsg);
        }

        long startTime = System.currentTimeMillis();
        Map<String, Object> resultSummary = new HashMap<>();

        try {
            // 执行增量同步
            log.info("⏰ 开始调用 IMUserSyncService.incrementalSync()...");
            Map<String, Object> result = imUserSyncService.incrementalSync();

            // 统计结果
            int successCount = (int) result.getOrDefault("successCount", 0);
            int failureCount = (int) result.getOrDefault("failureCount", 0);
            int totalCount = (int) result.getOrDefault("total", 0);
            int executionTime = (int) result.getOrDefault("executionTime", 0);

            // 构建结果摘要
            resultSummary.put("success", true);
            resultSummary.put("totalCount", totalCount);
            resultSummary.put("successCount", successCount);
            resultSummary.put("failureCount", failureCount);
            resultSummary.put("executionTime", executionTime);
            resultSummary.put("executeTime", currentTime);

            // 打印详细结果
            log.info("✅ ==================== OpenIM用户增量同步任务完成 ====================");
            log.info("✅ 本次同步: 总数={}, 成功={}, 失败={}", totalCount, successCount, failureCount);
            log.info("✅ 执行耗时: {}ms ({}秒)", executionTime, executionTime / 1000.0);
            log.info("✅ =========================================================");

            // 检查异常情况
            checkAndAlert(result);

            // 返回执行结果
            if (totalCount == 0) {
                return String.format("同步完成: 无新增用户需要同步 (耗时: %dms)", executionTime);
            } else if (failureCount == 0) {
                return String.format("同步完成: 成功同步 %d 个用户 (耗时: %dms)", successCount, executionTime);
            } else {
                return String.format("同步完成: 总数=%d, 成功=%d, 失败=%d (耗时: %dms)",
                        totalCount, successCount, failureCount, executionTime);
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            log.error("❌ ==================== OpenIM用户增量同步任务失败 ====================");
            log.error("❌ 错误信息: {}", e.getMessage());
            log.error("❌ 执行耗时: {}ms", executionTime);
            log.error("❌ =========================================================", e);

            // 构建错误结果
            resultSummary.put("success", false);
            resultSummary.put("error", e.getMessage());
            resultSummary.put("executionTime", executionTime);
            resultSummary.put("executeTime", currentTime);

            // 触发告警
            alertSyncFailure(e.getMessage());

            // 抛出异常以便记录到任务日志
            throw new RuntimeException("OpenIM用户增量同步失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查并告警
     */
    private void checkAndAlert(Map<String, Object> result) {
        int total = (int) result.getOrDefault("total", 0);
        int failureCount = (int) result.getOrDefault("failureCount", 0);

        // 如果有同步且失败率超过50%
        if (total > 0 && failureCount > 0) {
            double failureRate = (double) failureCount / total * 100;

            if (failureRate > 50) {
                log.error("🚨 [同步告警] 同步失败率过高: {:.2f}% (失败: {}/总数: {})",
                        failureRate, failureCount, total);
                alertHighFailureRate(failureRate, failureCount, total);
            } else if (failureRate > 20) {
                log.warn("⚠️ [同步警告] 同步失败率偏高: {:.2f}% (失败: {}/总数: {})",
                        failureRate, failureCount, total);
            }
        }

        // 检查执行时间
        int executionTime = (int) result.getOrDefault("executionTime", 0);
        if (executionTime > 60000) { // 超过60秒
            log.warn("⚠️ [同步警告] 同步耗时过长: {}ms ({}秒)", executionTime, executionTime / 1000.0);
        }
    }

    /**
     * 告警: 同步失败
     */
    private void alertSyncFailure(String errorMessage) {
        // TODO: 集成告警系统 (邮件、短信、企业微信、钉钉等)
        log.error("🚨 [告警] OpenIM用户增量同步任务失败: {}", errorMessage);
    }

    /**
     * 告警: 高失败率
     */
    private void alertHighFailureRate(double failureRate, int failureCount, int total) {
        // TODO: 集成告警系统
        log.error("🚨 [告警] OpenIM用户同步失败率过高: {:.2f}% (失败: {}/总数: {})",
                failureRate, failureCount, total);
    }
}
