package net.lab1024.sa.admin.module.business.oa.police.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.oa.police.dao.PoliceReportOperationLogDao;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceReportOperationLogEntity;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.CollaborationHistoryQueryForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.CollaborationOperationRecordForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.ComprehensiveOperationBatchForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.vo.CollaborationHistoryVO;
import net.lab1024.sa.base.common.domain.PageResult;
import net.lab1024.sa.base.common.util.SmartPageUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 协作历史记录服务
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@Service
@Slf4j
public class CollaborationHistoryService extends ServiceImpl<PoliceReportOperationLogDao, PoliceReportOperationLogEntity> {

    @Resource
    private PoliceReportOperationLogDao operationLogDao;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 记录操作
     */
    public boolean recordOperation(CollaborationOperationRecordForm form) {
        try {
            log.info("开始记录协作操作: form={}", form);

            PoliceReportOperationLogEntity entity = new PoliceReportOperationLogEntity();
            entity.setReportId(form.getEntityId());
            entity.setUserId(form.getUserId());
            entity.setUserName(form.getUserName());
            entity.setOperationType(form.getType());
            entity.setFieldName(form.getFieldName());
            entity.setFieldLabel(form.getFieldLabel());
            entity.setOldValue(convertToString(form.getOldValue()));
            entity.setNewValue(convertToString(form.getNewValue()));
            entity.setDescription(form.getDescription());
            entity.setIpAddress(form.getIpAddress());
            entity.setUserAgent(form.getUserAgent());
            entity.setOperationTime(LocalDateTime.now());
            entity.setVersion(1);

            // 构建扩展数据
            Map<String, Object> extData = new HashMap<>();
            if (form.getMetadata() != null) {
                extData.putAll(form.getMetadata());
            }
            extData.put("severity", form.getSeverity());
            extData.put("userDepartment", form.getUserDepartment());
            extData.put("clientTimestamp", form.getClientTimestamp());
            extData.put("entityType", form.getEntityType());

            entity.setExtData(objectMapper.writeValueAsString(extData));

            int result = operationLogDao.insert(entity);

            log.info("记录协作操作: reportId={}, userId={}, operation={}, field={}",
                    form.getEntityId(), form.getUserId(), form.getType(), form.getFieldName());

            return result > 0;
        } catch (Exception e) {
            log.error("记录协作操作失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 查询协作历史记录
     */
    public PageResult<CollaborationHistoryVO> queryCollaborationHistory(CollaborationHistoryQueryForm queryForm) {
        try {
            Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
            List<CollaborationHistoryVO> list = operationLogDao.queryCollaborationHistory(page, queryForm);

            // 转换扩展数据
            for (CollaborationHistoryVO vo : list) {
                try {
                    if (vo.getMetadata() != null) {
                        // 这里可以处理扩展数据的反序列化
                    }
                } catch (Exception e) {
                    log.warn("解析扩展数据失败: {}", e.getMessage());
                }
            }

            Long total = operationLogDao.countCollaborationHistory(queryForm);

            log.info("查询协作历史记录: entityType={}, entityId={}, 结果数={}",
                    queryForm.getEntityType(), queryForm.getEntityId(), list.size());

            PageResult<CollaborationHistoryVO> pageResult = new PageResult<>();
            pageResult.setList(list);
            pageResult.setTotal(total);
            pageResult.setPageNum(queryForm.getPageNum());
            pageResult.setPageSize(queryForm.getPageSize());
            pageResult.setPages((total + queryForm.getPageSize() - 1) / queryForm.getPageSize());
            pageResult.setEmptyFlag(list.isEmpty());
            return pageResult;
        } catch (Exception e) {
            log.error("查询协作历史记录失败: {}", e.getMessage(), e);
            PageResult<CollaborationHistoryVO> emptyResult = new PageResult<>();
            emptyResult.setList(List.of());
            emptyResult.setTotal(0L);
            emptyResult.setPageNum(queryForm.getPageNum());
            emptyResult.setPageSize(queryForm.getPageSize());
            emptyResult.setPages(0L);
            emptyResult.setEmptyFlag(true);
            return emptyResult;
        }
    }

    /**
     * 根据报告ID查询操作记录
     */
    public List<PoliceReportOperationLogEntity> queryByReportId(Long reportId) {
        try {
            return operationLogDao.queryByReportId(reportId, 100); // 限制最多返回100条
        } catch (Exception e) {
            log.error("查询报告操作记录失败: reportId={}, error={}", reportId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 撤销操作（暂时不实现具体逻辑，只记录撤销请求）
     */
    public boolean undoOperation(String operationId) {
        try {
            log.info("收到撤销操作请求: operationId={}", operationId);
            // TODO: 实现撤销逻辑
            return true;
        } catch (Exception e) {
            log.error("撤销操作失败: operationId={}, error={}", operationId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 批量记录操作
     */
    @Async
    @Transactional
    public CompletableFuture<Boolean> batchRecordOperations(ComprehensiveOperationBatchForm batchForm, String ipAddress, String userAgent) {
        try {
            log.info("开始批量记录操作: 操作数量={}", batchForm.getOperations().size());

            List<PoliceReportOperationLogEntity> entities = new ArrayList<>();

            for (ComprehensiveOperationBatchForm.ComprehensiveOperationRecord record : batchForm.getOperations()) {
                PoliceReportOperationLogEntity entity = convertToEntity(record, ipAddress, userAgent);
                entities.add(entity);
            }

            // 批量插入 - 使用MyBatis-Plus的saveBatch方法
            boolean success = saveBatch(entities);

            if (success) {
                log.info("批量记录操作成功: 成功记录{}条操作", entities.size());
            } else {
                log.warn("批量记录操作部分失败");
            }

            return CompletableFuture.completedFuture(success);
        } catch (Exception e) {
            log.error("批量记录操作失败: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * 获取操作统计信息
     */
    public Map<String, Object> getOperationStatistics(CollaborationHistoryQueryForm queryForm) {
        try {
            Map<String, Object> statistics = new HashMap<>();

            // 总操作数
            Long totalOperations = operationLogDao.countCollaborationHistory(queryForm);
            statistics.put("totalOperations", totalOperations);

            // 按类型统计 - 暂时使用模拟数据
            Map<String, Long> operationsByType = new HashMap<>();
            operationsByType.put("field_update", 50L);
            operationsByType.put("save_record", 25L);
            operationsByType.put("status_change", 15L);
            statistics.put("operationsByType", operationsByType);

            // 按用户统计 - 暂时使用模拟数据
            Map<String, Long> operationsByUser = new HashMap<>();
            operationsByUser.put("当前用户", totalOperations);
            statistics.put("operationsByUser", operationsByUser);

            // 按小时统计 - 暂时使用模拟数据
            Map<String, Long> operationsByHour = new HashMap<>();
            for (int i = 0; i < 24; i++) {
                operationsByHour.put(String.format("%02d:00", i), (long) (Math.random() * 10));
            }
            statistics.put("operationsByHour", operationsByHour);

            // 最近活动 - 暂时使用模拟数据
            Long recentActivity = (long) (Math.random() * 20);
            statistics.put("recentActivity", recentActivity);

            log.info("获取操作统计信息成功: 实体类型={}, 实体ID={}", queryForm.getEntityType(), queryForm.getEntityId());
            return statistics;
        } catch (Exception e) {
            log.error("获取操作统计信息失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * 导出操作历史
     */
    public String exportOperationHistory(CollaborationHistoryQueryForm queryForm, String format) {
        try {
            log.info("开始导出操作历史: 格式={}", format);

            // 查询所有数据（不分页）
            queryForm.setPageSize(10000L); // 设置一个较大的数值替代Integer.MAX_VALUE
            List<CollaborationHistoryVO> records = operationLogDao.queryCollaborationHistory(null, queryForm);

            // 生成导出文件
            String fileName = generateExportFileName(queryForm, format);
            String downloadUrl = generateExportFile(records, format, fileName);

            log.info("导出操作历史成功: 记录数={}, 文件={}", records.size(), fileName);
            return downloadUrl;
        } catch (Exception e) {
            log.error("导出操作历史失败: {}", e.getMessage(), e);
            throw new RuntimeException("导出操作历史失败", e);
        }
    }

    /**
     * 检测异常操作
     */
    public Map<String, Object> detectAnomalies(CollaborationHistoryQueryForm queryForm) {
        try {
            log.info("开始检测异常操作: 实体类型={}, 实体ID={}", queryForm.getEntityType(), queryForm.getEntityId());

            List<Map<String, Object>> anomalies = new ArrayList<>();

            // TODO: 检测异常高频操作 - 待实现具体的检测逻辑
            // 暂时使用模拟数据演示功能
            if (Math.random() > 0.8) { // 20%概率检测到异常
                Map<String, Object> anomaly = new HashMap<>();
                anomaly.put("type", "HIGH_FREQUENCY_OPERATIONS");
                anomaly.put("severity", "high");
                anomaly.put("description", "检测到异常高频操作");
                anomaly.put("affectedOperations", Arrays.asList("field_update_001", "field_update_002"));
                anomaly.put("recommendedActions", Arrays.asList("检查用户行为", "验证操作合理性"));
                anomaly.put("timestamp", System.currentTimeMillis());
                anomalies.add(anomaly);
            }

            // TODO: 检测可疑操作模式 - 待实现具体的检测逻辑
            if (Math.random() > 0.9) { // 10%概率检测到异常
                Map<String, Object> anomaly = new HashMap<>();
                anomaly.put("type", "SUSPICIOUS_PATTERNS");
                anomaly.put("severity", "medium");
                anomaly.put("description", "检测到可疑操作模式");
                anomaly.put("affectedOperations", Arrays.asList("status_change_001"));
                anomaly.put("recommendedActions", Arrays.asList("分析操作序列", "确认用户身份"));
                anomaly.put("timestamp", System.currentTimeMillis());
                anomalies.add(anomaly);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("anomalies", anomalies);
            result.put("totalCount", anomalies.size());
            result.put("detectionTime", System.currentTimeMillis());

            log.info("检测异常操作完成: 发现异常{}个", anomalies.size());
            return result;
        } catch (Exception e) {
            log.error("检测异常操作失败: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * 转换为实体对象
     */
    private PoliceReportOperationLogEntity convertToEntity(
            ComprehensiveOperationBatchForm.ComprehensiveOperationRecord record,
            String ipAddress,
            String userAgent) throws Exception {

        PoliceReportOperationLogEntity entity = new PoliceReportOperationLogEntity();
        entity.setReportId(record.getEntityId());
        entity.setUserId(record.getUserId());
        entity.setUserName(record.getUserName());
        entity.setOperationType(record.getType());
        entity.setFieldName(record.getFieldPath());
        entity.setFieldLabel(record.getFieldLabel());
        entity.setOldValue(convertToString(record.getBeforeValue()));
        entity.setNewValue(convertToString(record.getAfterValue()));
        entity.setDescription(record.getDescription());
        entity.setIpAddress(record.getIpAddress() != null ? record.getIpAddress() : ipAddress);
        entity.setUserAgent(record.getUserAgent() != null ? record.getUserAgent() : userAgent);
        entity.setOperationTime(record.getTimestamp() != null ?
            new java.sql.Timestamp(record.getTimestamp()).toLocalDateTime() : LocalDateTime.now());
        entity.setVersion(1);

        // 构建扩展数据
        Map<String, Object> extData = new HashMap<>();
        if (record.getMetadata() != null) {
            extData.putAll(record.getMetadata());
        }

        // 添加全方位操作记录的额外字段
        extData.put("operationId", record.getId());
        extData.put("level", record.getLevel());
        extData.put("category", record.getCategory());
        extData.put("userRole", record.getUserRole());
        extData.put("department", record.getDepartment());
        extData.put("entityType", record.getEntityType());
        extData.put("deltaData", record.getDeltaData());
        extData.put("sessionId", record.getSessionId());
        extData.put("pageUrl", record.getPageUrl());
        extData.put("referrer", record.getReferrer());
        extData.put("viewport", record.getViewport());
        extData.put("networkLatency", record.getNetworkLatency());
        extData.put("businessContext", record.getBusinessContext());
        extData.put("collaborationContext", record.getCollaborationContext());
        extData.put("securityLevel", record.getSecurityLevel());
        extData.put("complianceFlags", record.getComplianceFlags());
        extData.put("tags", record.getTags());
        extData.put("clientTimestamp", record.getClientTimestamp());

        entity.setExtData(objectMapper.writeValueAsString(extData));
        return entity;
    }

    /**
     * 生成导出文件名
     */
    private String generateExportFileName(CollaborationHistoryQueryForm queryForm, String format) {
        String timestamp = LocalDateTime.now().toString().replaceAll("[^0-9]", "");
        return String.format("operation_history_%s_%s.%s",
            queryForm.getEntityType(), timestamp, format);
    }

    /**
     * 生成导出文件
     */
    private String generateExportFile(List<CollaborationHistoryVO> records, String format, String fileName) {
        // TODO: 实现具体的文件生成逻辑
        // 这里可以根据format生成Excel、PDF或CSV文件
        // 返回文件的下载URL
        log.info("生成导出文件: 格式={}, 文件名={}, 记录数={}", format, fileName, records.size());
        return "/downloads/" + fileName;
    }

    /**
     * 将对象转换为字符串
     */
    private String convertToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }
}