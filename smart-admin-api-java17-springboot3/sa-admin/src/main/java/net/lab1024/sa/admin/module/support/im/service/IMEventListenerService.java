package net.lab1024.sa.admin.module.support.im.service;

import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceReportEntity;
import net.lab1024.sa.admin.module.support.im.config.OpenIMConfig;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * IM事件监听服务
 * 监听业务事件并触发IM相关操作
 *
 * 注意: 当前版本的事件监听器已禁用,因为具体的事件类尚未创建
 * 要启用自动监听功能,需要:
 * 1. 创建对应的事件类 (如 PoliceReportCreatedEvent)
 * 2. 在业务代码中发布事件
 * 3. 取消下面方法的注释并修改 @EventListener 参数
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Service
public class IMEventListenerService {

    @Resource
    private OpenIMConfig openIMConfig;

    // 已移除: IMGroupManagementService (已废弃，使用新架构的 IMBusinessService)
    // @Resource
    // private IMGroupManagementService imGroupManagementService;

    @Resource
    private IMInviteRuleService imInviteRuleService;

    /**
     * 监听警情创建事件 - 当前已禁用
     *
     * 要启用此功能:
     * 1. 创建 PoliceReportCreatedEvent 事件类
     * 2. 在警情创建成功后发布事件: applicationContext.publishEvent(new PoliceReportCreatedEvent(...))
     * 3. 修改 @EventListener 注解参数为具体事件类: @EventListener(PoliceReportCreatedEvent.class)
     * 4. 取消方法体的注释
     *
     * @param event 警情创建事件
     */
    /*
    @Async("imAsyncExecutor")
    @EventListener(PoliceReportCreatedEvent.class)
    public void onPoliceReportCreated(PoliceReportCreatedEvent event) {
        log.info("📨 [事件监听] 收到警情创建事件: reportId={}", event.getReportId());

        try {
            // 检查IM功能是否启用
            if (!openIMConfig.getEnabled()) {
                log.info("⚠️ [事件监听] IM功能未启用,跳过处理");
                return;
            }

            // 检查是否自动创建群组
            if (!openIMConfig.getAutoCreateGroup()) {
                log.info("⚠️ [事件监听] 自动创建群组功能未启用,跳过处理");
                return;
            }

            Long reportId = event.getReportId();
            Long creatorId = event.getCreatorId();
            PoliceReportEntity report = event.getReport();

            // 1. 创建IM群组
            log.info("👥 [事件监听] 开始为警情{}创建IM群组", reportId);
            String groupId = imGroupManagementService.createGroupForReport(reportId, creatorId);
            log.info("✅ [事件监听] 警情{}的IM群组创建成功: {}", reportId, groupId);

            // 2. 执行自动邀请规则
            if (openIMConfig.getAutoInvite()) {
                log.info("🎯 [事件监听] 开始执行自动邀请规则");
                List<Long> targetEmployees = imInviteRuleService.calculateTargetEmployees(report);

                if (!targetEmployees.isEmpty()) {
                    log.info("📤 [事件监听] 邀请{}个目标人员入群", targetEmployees.size());
                    imGroupManagementService.inviteMembers(reportId, targetEmployees, "自动邀请规则");
                    log.info("✅ [事件监听] 自动邀请完成");
                } else {
                    log.info("⚠️ [事件监听] 没有匹配的目标人员");
                }
            }

        } catch (Exception e) {
            log.error("❌ [事件监听] 处理警情创建事件失败", e);
            // 不抛出异常,避免影响主业务流程
        }
    }
    */

    /**
     * 监听员工创建事件 - 当前已禁用
     *
     * 要启用此功能:
     * 1. 创建 EmployeeCreatedEvent 事件类
     * 2. 在员工创建成功后发布事件
     * 3. 修改 @EventListener 注解参数
     * 4. 取消方法体的注释
     */
    /*
    @Async("imAsyncExecutor")
    @EventListener(EmployeeCreatedEvent.class)
    public void onEmployeeCreated(EmployeeCreatedEvent event) {
        log.info("📨 [事件监听] 收到员工创建事件: employeeId={}", event.getEmployeeId());

        try {
            if (!openIMConfig.getEnabled()) {
                log.info("⚠️ [事件监听] IM功能未启用,跳过处理");
                return;
            }

            Long employeeId = event.getEmployeeId();
            imUserSyncService.syncUser(employeeId);
            log.info("✅ [事件监听] 员工{}已自动同步到OpenIM", employeeId);

        } catch (Exception e) {
            log.error("❌ [事件监听] 处理员工创建事件失败", e);
        }
    }
    */

    /**
     * 监听警情状态变更事件 - 当前已禁用
     *
     * 要启用此功能:
     * 1. 创建 PoliceReportStatusChangedEvent 事件类
     * 2. 在警情状态变更后发布事件
     * 3. 修改 @EventListener 注解参数
     * 4. 取消方法体的注释
     */
    /*
    @Async("imAsyncExecutor")
    @EventListener(PoliceReportStatusChangedEvent.class)
    public void onPoliceReportStatusChanged(PoliceReportStatusChangedEvent event) {
        log.info("📨 [事件监听] 收到警情状态变更事件: reportId={}", event.getReportId());

        try {
            if (!openIMConfig.getEnabled()) {
                log.info("⚠️ [事件监听] IM功能未启用,跳过处理");
                return;
            }

            Long reportId = event.getReportId();
            Integer oldStatus = event.getOldStatus();
            Integer newStatus = event.getNewStatus();

            // 发送状态变更通知到群组
            String message = String.format("警情状态已更新: %s -> %s", oldStatus, newStatus);
            imMessageService.sendGroupMessage(reportId, message);
            log.info("✅ [事件监听] 已发送状态变更通知到群组");

        } catch (Exception e) {
            log.error("❌ [事件监听] 处理警情状态变更事件失败", e);
        }
    }
    */
}
