package net.lab1024.sa.admin.module.business.oa.police.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.oa.police.dao.PoliceReportOperationLogDao;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceReportOperationLogEntity;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.CollaborationHistoryQueryForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.CollaborationOperationRecordForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.vo.CollaborationHistoryVO;
import net.lab1024.sa.base.common.domain.PageResult;
import net.lab1024.sa.base.common.util.SmartPageUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 协作历史记录服务
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@Service
@Slf4j
public class CollaborationHistoryService {

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