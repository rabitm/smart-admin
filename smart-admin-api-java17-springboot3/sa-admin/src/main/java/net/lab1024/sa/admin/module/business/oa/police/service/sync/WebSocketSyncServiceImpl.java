package net.lab1024.sa.admin.module.business.oa.police.service.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceReportOperationLogEntity;
import net.lab1024.sa.admin.module.business.oa.seat.service.SeatSyncService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于WebSocket的实时同步服务实现
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-24
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSyncServiceImpl implements SyncService {

    private final SeatSyncService seatSyncService;

    @Override
    public void syncFieldUpdate(Long reportId, Long userId, String userName,
                               String fieldName, String fieldValue, String operationType) {
        try {
            log.info("同步字段更新: reportId={}, userId={}, fieldName={}, operationType={}",
                    reportId, userId, fieldName, operationType);

            // 1. 通过WebSocket同步到其他用户
            seatSyncService.notifyPoliceCaseFieldSync(reportId, userId, userName, fieldName, fieldValue);

            // 2. 记录操作历史
            // TODO: 当前简化实现，后续可以异步处理
            recordOperation(reportId, userId, userName, fieldName, null, fieldValue, operationType);

        } catch (Exception e) {
            log.error("同步字段更新失败", e);
        }
    }

    @Override
    public void syncFieldEditState(Long reportId, Long userId, String userName,
                                  String fieldName, String action) {
        try {
            log.info("同步字段编辑状态: reportId={}, userId={}, fieldName={}, action={}",
                    reportId, userId, fieldName, action);

            // 构建编辑状态消息
            Map<String, Object> data = new HashMap<>();
            data.put("fieldName", fieldName);
            data.put("action", action);
            data.put("employeeId", userId);
            data.put("employeeName", userName);
            data.put("timestamp", System.currentTimeMillis());

            // 通过WebSocket发送编辑状态
            seatSyncService.sendMessage("FIELD_EDIT_STATE", data, userId, null, action);

        } catch (Exception e) {
            log.error("同步字段编辑状态失败", e);
        }
    }

    @Override
    public void recordOperation(Long reportId, Long userId, String userName,
                               String fieldName, String oldValue, String newValue, String operationType) {
        try {
            log.info("记录操作历史: reportId={}, userId={}, fieldName={}, operationType={}",
                    reportId, userId, fieldName, operationType);

            // 构建操作记录
            PoliceReportOperationLogEntity logEntity = new PoliceReportOperationLogEntity();
            logEntity.setReportId(reportId);
            logEntity.setUserId(userId);
            logEntity.setUserName(userName);
            logEntity.setOperationType(operationType);
            logEntity.setFieldName(fieldName);
            logEntity.setOldValue(oldValue);
            logEntity.setNewValue(newValue);
            logEntity.setOperationTime(LocalDateTime.now());

            // 获取请求信息（WebSocket上下文中无法获取HTTP请求信息）
            logEntity.setIpAddress("WebSocket");
            logEntity.setUserAgent("WebSocket Connection");

            // 生成操作描述
            String description = generateOperationDescription(operationType, fieldName, oldValue, newValue);
            logEntity.setDescription(description);

            // TODO: 保存到数据库（后续实现DAO层）
            log.info("操作记录已生成: {}", description);

        } catch (Exception e) {
            log.error("记录操作历史失败", e);
        }
    }

    @Override
    public Object getOperationHistory(Long reportId) {
        try {
            log.info("获取操作历史: reportId={}", reportId);
            // TODO: 从数据库查询操作历史
            return new HashMap<String, Object>() {{
                put("reportId", reportId);
                put("message", "操作历史查询功能正在开发中");
            }};
        } catch (Exception e) {
            log.error("获取操作历史失败", e);
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            return seatSyncService != null;
        } catch (Exception e) {
            log.error("检查服务可用性失败", e);
            return false;
        }
    }

    /**
     * 生成操作描述
     */
    private String generateOperationDescription(String operationType, String fieldName,
                                              String oldValue, String newValue) {
        switch (operationType) {
            case "FIELD_UPDATE":
                if (oldValue != null && !oldValue.equals(newValue)) {
                    return String.format("修改字段 %s: %s → %s", fieldName, oldValue, newValue);
                } else {
                    return String.format("设置字段 %s: %s", fieldName, newValue);
                }
            case "FIELD_DELETE":
                return String.format("清空字段 %s", fieldName);
            case "CREATE":
                return "创建警情记录";
            case "DELETE":
                return "删除警情记录";
            default:
                return String.format("执行操作 %s", operationType);
        }
    }
}