package net.lab1024.sa.admin.module.business.oa.police.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.oa.police.constant.PoliceReportStatusEnum;
import net.lab1024.sa.admin.module.business.oa.police.dao.PoliceReportDao;
import net.lab1024.sa.admin.module.business.oa.police.dao.PoliceReportFieldDataDao;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceReportEntity;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceReportFieldDataEntity;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.PoliceReportAddForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.PoliceReportQueryForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.PoliceReportUpdateForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.vo.PoliceReportVO;
import net.lab1024.sa.base.common.domain.PageResult;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.common.util.SmartBeanUtil;
import net.lab1024.sa.base.common.util.SmartPageUtil;
import net.lab1024.sa.base.module.support.datatracer.constant.DataTracerConst;
import net.lab1024.sa.base.module.support.datatracer.constant.DataTracerTypeEnum;
import net.lab1024.sa.base.module.support.datatracer.service.DataTracerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 警情录入Service
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-18
 */
@Service
@Slf4j
public class PoliceReportService {

    @Resource
    private PoliceReportDao policeReportDao;

    @Resource
    private PoliceReportFieldDataDao policeReportFieldDataDao;

    @Resource
    private DataTracerService dataTracerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 分页查询警情信息
     */
    public ResponseDTO<PageResult<PoliceReportVO>> queryByPage(PoliceReportQueryForm queryForm) {
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<PoliceReportVO> policeReportList = policeReportDao.queryPage(page, queryForm);
        PageResult<PoliceReportVO> pageResult = SmartPageUtil.convert2PageResult(page, policeReportList);
        return ResponseDTO.ok(pageResult);
    }

    /**
     * 查询警情信息详情
     */
    public ResponseDTO<PoliceReportVO> getDetail(Long reportId) {
        // 校验警情信息是否存在
        PoliceReportVO policeReportVO = policeReportDao.getDetail(reportId, Boolean.FALSE);
        if (Objects.isNull(policeReportVO)) {
            return ResponseDTO.userErrorParam("警情信息不存在");
        }
        return ResponseDTO.ok(policeReportVO);
    }

    /**
     * 新增警情信息
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> addPoliceReport(PoliceReportAddForm addForm) {
        // 生成警情编号
        String reportNumber = generateReportNumber();

        // 验证警情编号是否重复（理论上不应该重复，但保险起见）
        PoliceReportEntity existingReport = policeReportDao.queryByReportNumber(reportNumber, null, Boolean.FALSE);
        if (Objects.nonNull(existingReport)) {
            return ResponseDTO.userErrorParam("警情编号已存在，请重试");
        }

        // 数据插入
        PoliceReportEntity policeReportEntity = SmartBeanUtil.copy(addForm, PoliceReportEntity.class);
        policeReportEntity.setReportNumber(reportNumber);
        policeReportEntity.setStatus(PoliceReportStatusEnum.PENDING.getValue()); // 默认待处理状态
        policeReportEntity.setDeletedFlag(Boolean.FALSE);

        policeReportDao.insert(policeReportEntity);

        // 保存专业字段数据
        saveProfessionalFields(policeReportEntity.getReportId(), addForm.getProfessionalFields());

        // 数据追踪
        dataTracerService.addTrace(policeReportEntity.getReportId(), DataTracerTypeEnum.OA_ENTERPRISE,
            "新增警情:" + DataTracerConst.HTML_BR + dataTracerService.getChangeContent(policeReportEntity));

        return ResponseDTO.ok();
    }

    /**
     * 更新警情信息
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> updatePoliceReport(PoliceReportUpdateForm updateForm) {
        Long reportId = updateForm.getReportId();

        // 校验警情信息是否存在
        PoliceReportEntity oldPoliceReport = policeReportDao.selectById(reportId);
        if (Objects.isNull(oldPoliceReport) || oldPoliceReport.getDeletedFlag()) {
            return ResponseDTO.userErrorParam("警情信息不存在");
        }

        // 数据更新
        PoliceReportEntity updateEntity = SmartBeanUtil.copy(updateForm, PoliceReportEntity.class);
        policeReportDao.updateById(updateEntity);

        // 更新专业字段数据
        if (updateForm.getProfessionalFields() != null) {
            // 先删除旧的专业字段数据
            policeReportFieldDataDao.deleteByReportId(reportId);
            // 保存新的专业字段数据
            saveProfessionalFields(reportId, updateForm.getProfessionalFields());
        }

        // 数据追踪
        dataTracerService.addTrace(reportId, DataTracerTypeEnum.OA_ENTERPRISE,
            "更新警情:" + DataTracerConst.HTML_BR + dataTracerService.getChangeContent(oldPoliceReport, updateEntity));

        return ResponseDTO.ok();
    }

    /**
     * 删除警情信息
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> deletePoliceReport(Long reportId) {
        // 校验警情信息是否存在
        PoliceReportEntity policeReportEntity = policeReportDao.selectById(reportId);
        if (Objects.isNull(policeReportEntity) || policeReportEntity.getDeletedFlag()) {
            return ResponseDTO.userErrorParam("警情信息不存在");
        }

        policeReportDao.deletePoliceReport(reportId, Boolean.TRUE);

        // 数据追踪
        dataTracerService.addTrace(reportId, DataTracerTypeEnum.OA_ENTERPRISE,
            "删除警情:" + DataTracerConst.HTML_BR + dataTracerService.getChangeContent(policeReportEntity));

        return ResponseDTO.ok();
    }

    /**
     * 根据报警人电话查询警情列表
     */
    public ResponseDTO<List<PoliceReportVO>> queryByReporterPhone(String reporterPhone) {
        List<PoliceReportVO> policeReportList = policeReportDao.queryByReporterPhone(reporterPhone, Boolean.FALSE);
        return ResponseDTO.ok(policeReportList);
    }

    /**
     * 根据处理人员查询警情列表
     */
    public ResponseDTO<List<PoliceReportVO>> queryByHandler(Long handlerId) {
        List<PoliceReportVO> policeReportList = policeReportDao.queryByHandler(handlerId, Boolean.FALSE);
        return ResponseDTO.ok(policeReportList);
    }

    /**
     * 获取各状态警情统计
     */
    public ResponseDTO<List<PoliceReportVO>> getStatusStatistics() {
        List<PoliceReportVO> statistics = policeReportDao.getStatusStatistics(Boolean.FALSE);
        return ResponseDTO.ok(statistics);
    }

    /**
     * 生成警情编号
     * 格式：POLICE-YYYYMMDD-XXX（XXX为当天的序号，从001开始）
     */
    private String generateReportNumber() {
        LocalDateTime now = LocalDateTime.now();
        String datePrefix = "POLICE-" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 获取当天最大编号
        String maxReportNumber = policeReportDao.getMaxReportNumberByDate(datePrefix);

        int sequence = 1;
        if (maxReportNumber != null && maxReportNumber.length() > datePrefix.length()) {
            try {
                String sequenceStr = maxReportNumber.substring(datePrefix.length() + 1);
                sequence = Integer.parseInt(sequenceStr) + 1;
            } catch (NumberFormatException e) {
                log.warn("解析警情编号序号失败: {}", maxReportNumber, e);
                // 发生异常时使用当前时间戳后3位作为序号，避免重复
                sequence = (int) (System.currentTimeMillis() % 1000);
            }
        }

        return datePrefix + "-" + String.format("%03d", sequence);
    }

    /**
     * 保存专业字段数据
     */
    private void saveProfessionalFields(Long reportId, Map<String, Object> professionalFields) {
        log.info("开始保存专业字段数据 - reportId: {}, professionalFields: {}", reportId, professionalFields);

        if (professionalFields == null || professionalFields.isEmpty()) {
            log.warn("专业字段数据为空 - reportId: {}", reportId);
            return;
        }

        List<PoliceReportFieldDataEntity> fieldDataList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : professionalFields.entrySet()) {
            String fieldKey = entry.getKey();
            Object fieldValue = entry.getValue();

            log.debug("处理字段 - key: {}, value: {}, isBasicField: {}", fieldKey, fieldValue, isBasicField(fieldKey));

            // 跳过基本字段，只保存专业字段
            if (isBasicField(fieldKey)) {
                log.debug("跳过基本字段: {}", fieldKey);
                continue;
            }

            // 跳过空值
            if (fieldValue == null || (fieldValue instanceof String && ((String) fieldValue).trim().isEmpty())) {
                log.debug("跳过空值字段: {}", fieldKey);
                continue;
            }

            PoliceReportFieldDataEntity fieldData = new PoliceReportFieldDataEntity();
            fieldData.setReportId(reportId);
            fieldData.setFieldKey(fieldKey);

            // 处理数组类型的字段值，将其序列化为JSON字符串
            String fieldValueStr;
            if (fieldValue instanceof List || fieldValue instanceof ArrayList) {
                try {
                    fieldValueStr = objectMapper.writeValueAsString(fieldValue);
                } catch (Exception e) {
                    log.warn("序列化数组字段值失败: {} = {}", fieldKey, fieldValue, e);
                    fieldValueStr = fieldValue.toString();
                }
            } else {
                fieldValueStr = fieldValue.toString();
            }

            fieldData.setFieldValue(fieldValueStr);
            fieldDataList.add(fieldData);

            log.info("准备保存专业字段: {} = {}", fieldKey, fieldValue);
        }

        if (!fieldDataList.isEmpty()) {
            log.info("批量保存 {} 个专业字段数据", fieldDataList.size());
            policeReportFieldDataDao.batchInsertFieldData(fieldDataList);
            log.info("专业字段数据保存成功");
        } else {
            log.warn("没有有效的专业字段数据需要保存");
        }
    }

    /**
     * 判断是否为基本字段
     */
    private boolean isBasicField(String fieldKey) {
        return "reportType".equals(fieldKey) ||
               "reportLevel".equals(fieldKey) ||
               "reporterName".equals(fieldKey) ||
               "reporterPhone".equals(fieldKey) ||
               "reporterIdCard".equals(fieldKey) ||
               "reportTime".equals(fieldKey) ||
               "incidentLocation".equals(fieldKey) ||
               "description".equals(fieldKey) ||
               "handlerId".equals(fieldKey) ||
               "handlerName".equals(fieldKey) ||
               "attachments".equals(fieldKey) ||
               "remark".equals(fieldKey);
    }

    /**
     * 搜索地址建议（历史 + 模拟）
     */
    public ResponseDTO<List<String>> searchLocationSuggestions(String keyword) {
        if (keyword == null || keyword.trim().length() < 2) {
            return ResponseDTO.ok(new java.util.ArrayList<>());
        }

        try {
            List<String> suggestions = new java.util.ArrayList<>();

            // 获取历史地址建议
            try {
                List<String> historySuggestions = policeReportDao.getLocationSuggestions(keyword.trim(), Boolean.FALSE);
                if (historySuggestions != null && !historySuggestions.isEmpty()) {
                    suggestions.addAll(historySuggestions);
                }
            } catch (Exception e) {
                log.warn("获取历史地址建议失败: {}", e.getMessage());
            }

            // 添加模拟地址建议
            String trimmedKeyword = keyword.trim();
            suggestions.add(trimmedKeyword + "街道");
            suggestions.add(trimmedKeyword + "大道");
            suggestions.add(trimmedKeyword + "路");
            suggestions.add(trimmedKeyword + "小区");
            suggestions.add(trimmedKeyword + "广场");
            suggestions.add(trimmedKeyword + "商场");

            // 去重并限制返回数量
            List<String> result = suggestions.stream()
                .distinct()
                .limit(6)
                .collect(java.util.stream.Collectors.toList());

            log.info("地址建议搜索 - 关键字: {}, 结果数量: {}", keyword, result.size());
            return ResponseDTO.ok(result);

        } catch (Exception e) {
            log.error("搜索地址建议出现异常: {}", e.getMessage(), e);
            return ResponseDTO.ok(new java.util.ArrayList<>());
        }
    }

    /**
     * 获取警情专业字段数据
     */
    public ResponseDTO<Map<String, Object>> getPoliceReportFieldData(Long reportId) {
        try {
            List<PoliceReportFieldDataEntity> fieldDataList = policeReportFieldDataDao.getFieldDataByReportId(reportId);
            Map<String, Object> fieldDataMap = new HashMap<>();

            for (PoliceReportFieldDataEntity fieldData : fieldDataList) {
                String fieldValue = fieldData.getFieldValue();
                Object parsedValue;

                // 尝试将JSON数组字符串解析为List对象
                if (fieldValue != null && fieldValue.startsWith("[") && fieldValue.endsWith("]")) {
                    try {
                        parsedValue = objectMapper.readValue(fieldValue, List.class);
                        log.debug("成功解析数组字段: {} = {}", fieldData.getFieldKey(), parsedValue);
                    } catch (Exception e) {
                        log.debug("解析数组字段失败，使用原始值: {} = {}", fieldData.getFieldKey(), fieldValue);
                        parsedValue = fieldValue;
                    }
                } else {
                    parsedValue = fieldValue;
                }

                fieldDataMap.put(fieldData.getFieldKey(), parsedValue);
            }

            return ResponseDTO.ok(fieldDataMap);
        } catch (Exception e) {
            log.error("获取警情专业字段数据失败: reportId={}", reportId, e);
            return ResponseDTO.ok(new HashMap<>());
        }
    }

    /**
     * 高级查询 - 支持专业字段条件查询
     * 适用于复杂的统计分析和业务查询
     */
    public ResponseDTO<PageResult<PoliceReportVO>> advancedQuery(PoliceReportQueryForm queryForm) {
        try {
            Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);

            // 构建动态查询条件
            StringBuilder sql = new StringBuilder();
            Map<String, Object> params = new HashMap<>();

            sql.append("SELECT DISTINCT pr.* FROM t_police_report pr ");

            // 根据查询条件决定是否需要JOIN专业字段表
            boolean needFieldJoin = hasFieldQueryConditions(queryForm);
            if (needFieldJoin) {
                sql.append("LEFT JOIN t_police_report_field_data fd ON pr.report_id = fd.report_id ");
            }

            sql.append("WHERE pr.deleted_flag = 0 ");

            // 基本查询条件
            addBasicQueryConditions(sql, params, queryForm);

            // 专业字段查询条件
            if (needFieldJoin) {
                addFieldQueryConditions(sql, params, queryForm);
            }

            sql.append("ORDER BY pr.report_time DESC, pr.create_time DESC");

            log.info("高级查询SQL: {}", sql.toString());
            log.info("查询参数: {}", params);

            // 执行查询
            List<PoliceReportVO> policeReportList = policeReportDao.advancedQuery(sql.toString(), params, page);
            PageResult<PoliceReportVO> pageResult = SmartPageUtil.convert2PageResult(page, policeReportList);

            return ResponseDTO.ok(pageResult);

        } catch (Exception e) {
            log.error("高级查询失败", e);
            return ResponseDTO.userErrorParam("查询失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否有专业字段查询条件
     */
    private boolean hasFieldQueryConditions(PoliceReportQueryForm queryForm) {
        return queryForm.getTrappedCountMin() != null ||
               queryForm.getTrappedCountMax() != null ||
               queryForm.getCasualtiesCountMin() != null ||
               queryForm.getCasualtiesCountMax() != null ||
               queryForm.getVehicleCountMin() != null ||
               queryForm.getVehicleCountMax() != null ||
               queryForm.getDangerLevel() != null ||
               queryForm.getRescueType() != null ||
               queryForm.getAccidentType() != null ||
               queryForm.getFireFloor() != null ||
               queryForm.getFireScale() != null;
    }

    /**
     * 添加基本查询条件
     */
    private void addBasicQueryConditions(StringBuilder sql, Map<String, Object> params, PoliceReportQueryForm queryForm) {
        if (queryForm.getReportNumber() != null) {
            sql.append("AND pr.report_number LIKE :reportNumber ");
            params.put("reportNumber", "%" + queryForm.getReportNumber() + "%");
        }

        if (queryForm.getReportType() != null) {
            sql.append("AND pr.report_type = :reportType ");
            params.put("reportType", queryForm.getReportType());
        }

        if (queryForm.getReportLevel() != null) {
            sql.append("AND pr.report_level = :reportLevel ");
            params.put("reportLevel", queryForm.getReportLevel());
        }

        if (queryForm.getStatus() != null) {
            sql.append("AND pr.status = :status ");
            params.put("status", queryForm.getStatus());
        }

        if (queryForm.getReportTimeStart() != null) {
            sql.append("AND pr.report_time >= :reportTimeStart ");
            params.put("reportTimeStart", queryForm.getReportTimeStart());
        }

        if (queryForm.getReportTimeEnd() != null) {
            sql.append("AND pr.report_time <= :reportTimeEnd ");
            params.put("reportTimeEnd", queryForm.getReportTimeEnd());
        }
    }

    /**
     * 添加专业字段查询条件
     */
    private void addFieldQueryConditions(StringBuilder sql, Map<String, Object> params, PoliceReportQueryForm queryForm) {
        List<String> fieldConditions = new ArrayList<>();

        // 被困人数查询
        if (queryForm.getTrappedCountMin() != null || queryForm.getTrappedCountMax() != null) {
            StringBuilder trappedCondition = new StringBuilder();
            trappedCondition.append("(fd.field_key = 'trappedCount' AND ");

            if (queryForm.getTrappedCountMin() != null && queryForm.getTrappedCountMax() != null) {
                trappedCondition.append("CAST(fd.field_value AS UNSIGNED) BETWEEN :trappedCountMin AND :trappedCountMax");
                params.put("trappedCountMin", queryForm.getTrappedCountMin());
                params.put("trappedCountMax", queryForm.getTrappedCountMax());
            } else if (queryForm.getTrappedCountMin() != null) {
                trappedCondition.append("CAST(fd.field_value AS UNSIGNED) >= :trappedCountMin");
                params.put("trappedCountMin", queryForm.getTrappedCountMin());
            } else {
                trappedCondition.append("CAST(fd.field_value AS UNSIGNED) <= :trappedCountMax");
                params.put("trappedCountMax", queryForm.getTrappedCountMax());
            }

            trappedCondition.append(")");
            fieldConditions.add(trappedCondition.toString());
        }

        // 伤亡人数查询
        if (queryForm.getCasualtiesCountMin() != null || queryForm.getCasualtiesCountMax() != null) {
            StringBuilder casualtiesCondition = new StringBuilder();
            casualtiesCondition.append("(fd.field_key = 'casualties' AND ");

            if (queryForm.getCasualtiesCountMin() != null && queryForm.getCasualtiesCountMax() != null) {
                casualtiesCondition.append("CAST(fd.field_value AS UNSIGNED) BETWEEN :casualtiesCountMin AND :casualtiesCountMax");
                params.put("casualtiesCountMin", queryForm.getCasualtiesCountMin());
                params.put("casualtiesCountMax", queryForm.getCasualtiesCountMax());
            } else if (queryForm.getCasualtiesCountMin() != null) {
                casualtiesCondition.append("CAST(fd.field_value AS UNSIGNED) >= :casualtiesCountMin");
                params.put("casualtiesCountMin", queryForm.getCasualtiesCountMin());
            } else {
                casualtiesCondition.append("CAST(fd.field_value AS UNSIGNED) <= :casualtiesCountMax");
                params.put("casualtiesCountMax", queryForm.getCasualtiesCountMax());
            }

            casualtiesCondition.append(")");
            fieldConditions.add(casualtiesCondition.toString());
        }

        // 危险等级查询
        if (queryForm.getDangerLevel() != null) {
            fieldConditions.add("(fd.field_key = 'dangerLevel' AND fd.field_value = :dangerLevel)");
            params.put("dangerLevel", queryForm.getDangerLevel().toString());
        }

        // 救援类型查询
        if (queryForm.getRescueType() != null) {
            fieldConditions.add("(fd.field_key = 'rescueType' AND fd.field_value LIKE :rescueType)");
            params.put("rescueType", "%" + queryForm.getRescueType() + "%");
        }

        // 添加字段条件到SQL
        if (!fieldConditions.isEmpty()) {
            sql.append("AND pr.report_id IN (");
            sql.append("SELECT DISTINCT fd2.report_id FROM t_police_report_field_data fd2 WHERE ");
            sql.append(String.join(" OR ", fieldConditions));
            sql.append(") ");
        }
    }

    /**
     * 专业字段统计查询
     * 例如：按警情类型统计被困人数、伤亡情况等
     */
    public ResponseDTO<List<Map<String, Object>>> getFieldStatistics(String fieldKey, Integer reportType) {
        try {
            List<Map<String, Object>> statistics = policeReportDao.getFieldStatistics(fieldKey, reportType);
            return ResponseDTO.ok(statistics);
        } catch (Exception e) {
            log.error("获取专业字段统计数据失败: fieldKey={}, reportType={}", fieldKey, reportType, e);
            return ResponseDTO.ok(new ArrayList<>());
        }
    }
}