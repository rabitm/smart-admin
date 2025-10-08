package net.lab1024.sa.admin.module.business.oa.police.service.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceReportEntity;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceReportOperationLogEntity;
import net.lab1024.sa.admin.module.business.oa.police.dao.PoliceReportDao;
import net.lab1024.sa.admin.module.business.oa.seat.service.SeatSyncService;
import net.lab1024.sa.base.module.support.operatelog.domain.OperateLogEntity;
import net.lab1024.sa.base.module.support.operatelog.OperateLogDao;
import net.lab1024.sa.base.common.util.SmartRequestUtil;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import java.lang.reflect.Field;
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
@Service("webSocketSyncService")
@RequiredArgsConstructor
@ConditionalOnMissingBean(name = "rocketMQSyncService")
public class WebSocketSyncServiceImpl implements SyncService {

    private final SeatSyncService seatSyncService;
    private final PoliceReportDao policeReportDao;
    private final OperateLogDao operateLogDao;

    // 使用 @Lazy 避免循环依赖
    @Autowired
    @Lazy
    private net.lab1024.sa.admin.module.business.oa.police.service.PoliceReportService policeReportService;

    @Override
    public void syncFieldUpdate(Long reportId, Long userId, String userName,
                               String fieldName, String fieldValue, String operationType) {
        try {
            log.info("同步字段更新: reportId={}, userId={}, fieldName={}, operationType={}",
                    reportId, userId, fieldName, operationType);

            // 1. 先获取旧值
            String oldValue = getFieldOldValue(reportId, fieldName);
            log.debug("获取到字段旧值: fieldName={}, oldValue={}, newValue={}", fieldName, oldValue, fieldValue);

            // 2. 通过WebSocket同步到其他用户
            seatSyncService.notifyPoliceCaseFieldSync(reportId, userId, userName, fieldName, fieldValue);

            // 3. 记录操作历史（现在包含旧值和新值）
            recordOperation(reportId, userId, userName, fieldName, oldValue, fieldValue, operationType);

        } catch (Exception e) {
            log.error("同步字段更新失败", e);
        }
    }

    @Override
    public void syncFieldUpdate(Long reportId, Long userId, String userName,
                               String fieldName, String oldValue, String fieldValue, String operationType) {
        try {
            log.info("同步字段更新（含旧值）: reportId={}, userId={}, fieldName={}, operationType={}",
                    reportId, userId, fieldName, operationType);

            log.debug("字段更新详情: fieldName={}, oldValue={}, newValue={}", fieldName, oldValue, fieldValue);

            // 1. 通过WebSocket同步到其他用户
            seatSyncService.notifyPoliceCaseFieldSync(reportId, userId, userName, fieldName, fieldValue);

            // 2. 记录操作历史（直接使用传入的旧值，不重新获取）
            recordOperation(reportId, userId, userName, fieldName, oldValue, fieldValue, operationType);

        } catch (Exception e) {
            log.error("同步字段更新失败", e);
        }
    }

    /**
     * 获取字段的旧值
     */
    private String getFieldOldValue(Long reportId, String fieldName) {
        try {
            // 查询当前警情信息
            PoliceReportEntity entity = policeReportDao.selectById(reportId);
            if (entity == null) {
                log.warn("未找到警情信息: reportId={}", reportId);
                return null;
            }

            // 使用反射获取字段值
            return getFieldValue(entity, fieldName);
        } catch (Exception e) {
            log.error("获取字段旧值失败: fieldName={}", fieldName, e);
            return null;
        }
    }

    /**
     * 使用反射获取孟体字段值
     */
    private String getFieldValue(Object entity, String fieldName) {
        try {
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(entity);
            return value != null ? String.valueOf(value) : null;
        } catch (NoSuchFieldException e) {
            // 如果在主实体中没找到，可能是专业字段，返回null
            log.debug("字段不在主实体中: fieldName={}", fieldName);
            return null;
        } catch (Exception e) {
            log.error("获取字段值失败: fieldName={}", fieldName, e);
            return null;
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

            // 使用系统的操作日志表保存记录
            OperateLogEntity operateLog = new OperateLogEntity();
            operateLog.setOperateUserId(userId);
            operateLog.setOperateUserName(userName);
            operateLog.setOperateUserType(1); // 假设用户类型为1
            operateLog.setModule("警情管理");
            operateLog.setContent("同步警情字段更新");
            operateLog.setUrl("/oa/police/report/sync-field/" + reportId);
            operateLog.setMethod("POST");

            // 构建包含旧值和新值的参数
            String paramWithOldValue = String.format("{\"%s\":{\"old\":\"%s\",\"new\":\"%s\"}}",
                fieldName,
                oldValue != null ? oldValue.replace("\"", "\\\"") : "",
                newValue != null ? newValue.replace("\"", "\\\"") : "");

            operateLog.setParam(paramWithOldValue);
            operateLog.setCreateTime(LocalDateTime.now());
            operateLog.setIp("协作系统");
            operateLog.setUserAgent("WebSocket Connection");
            operateLog.setSuccessFlag(true);
            operateLog.setFailReason("");

            // 保存到数据库
            operateLogDao.insert(operateLog);

            log.info("操作记录已保存: reportId={}, fieldName={}, oldValue={}, newValue={}",
                reportId, fieldName, oldValue, newValue);

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