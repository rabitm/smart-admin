package net.lab1024.sa.admin.module.business.im.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.im.service.ImPoliceGroupService;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceReportEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 警情群组自动创建监听器
 * 监听警情创建事件,自动创建IM群组
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "openim", name = "api-url")
public class PoliceGroupAutoCreateListener {

    private final ImPoliceGroupService imPoliceGroupService;

    /**
     * 监听警情创建事件
     * 注意: 需要在PoliceReportService中发布事件
     */
    @EventListener
    public void onPoliceReportCreated(PoliceReportCreatedEvent event) {
        try {
            log.info("📱 [警情群组监听器] 收到警情创建事件 - 警情ID: {}, 警情编号: {}",
                    event.getPoliceReport().getReportId(),
                    event.getPoliceReport().getReportNumber());

            // 异步创建群组
            imPoliceGroupService.createPoliceGroup(event.getPoliceReport());

        } catch (Exception e) {
            log.error("📱 [警情群组监听器] 处理警情创建事件失败 - 警情ID: {}",
                    event.getPoliceReport().getReportId(), e);
        }
    }

    /**
     * 监听警情负责人变更事件
     * 注意: 需要在PoliceReportService中发布事件
     */
    @EventListener
    public void onPoliceReportOwnerChanged(PoliceReportOwnerChangedEvent event) {
        try {
            log.info("📱 [警情群组监听器] 收到负责人变更事件 - 警情ID: {}, 新负责人ID: {}",
                    event.getReportId(),
                    event.getNewOwnerEmployeeId());

            // 异步邀请新负责人
            imPoliceGroupService.inviteNewOwner(event.getReportId(), event.getNewOwnerEmployeeId());

        } catch (Exception e) {
            log.error("📱 [警情群组监听器] 处理负责人变更事件失败 - 警情ID: {}",
                    event.getReportId(), e);
        }
    }

    /**
     * 警情创建事件
     */
    public static class PoliceReportCreatedEvent {
        private final PoliceReportEntity policeReport;

        public PoliceReportCreatedEvent(PoliceReportEntity policeReport) {
            this.policeReport = policeReport;
        }

        public PoliceReportEntity getPoliceReport() {
            return policeReport;
        }
    }

    /**
     * 警情负责人变更事件
     */
    public static class PoliceReportOwnerChangedEvent {
        private final Long reportId;
        private final Long newOwnerEmployeeId;

        public PoliceReportOwnerChangedEvent(Long reportId, Long newOwnerEmployeeId) {
            this.reportId = reportId;
            this.newOwnerEmployeeId = newOwnerEmployeeId;
        }

        public Long getReportId() {
            return reportId;
        }

        public Long getNewOwnerEmployeeId() {
            return newOwnerEmployeeId;
        }
    }
}
