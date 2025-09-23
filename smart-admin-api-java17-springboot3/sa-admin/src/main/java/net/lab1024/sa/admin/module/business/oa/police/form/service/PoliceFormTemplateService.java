package net.lab1024.sa.admin.module.business.oa.police.form.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import net.lab1024.sa.admin.module.business.oa.police.form.dao.PoliceFormFieldDao;
import net.lab1024.sa.admin.module.business.oa.police.form.dao.PoliceFormTemplateDao;
import net.lab1024.sa.admin.module.business.oa.police.form.domain.entity.PoliceFormFieldEntity;
import net.lab1024.sa.admin.module.business.oa.police.form.domain.entity.PoliceFormTemplateEntity;
import net.lab1024.sa.admin.module.business.oa.police.form.domain.form.PoliceFormFieldSaveForm;
import net.lab1024.sa.admin.module.business.oa.police.form.domain.form.PoliceFormTemplateAddForm;
import net.lab1024.sa.admin.module.business.oa.police.form.domain.vo.PoliceFormConfigVO;
import net.lab1024.sa.admin.module.business.oa.police.form.domain.vo.PoliceFormTemplateVO;
import net.lab1024.sa.admin.module.business.oa.police.form.manager.PoliceFormConfigManager;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.common.util.SmartBeanUtil;
import net.lab1024.sa.base.common.util.SmartRequestUtil;
import net.lab1024.sa.base.common.util.SmartStringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 警情表单模板服务
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-22
 * @Copyright 1024创新实验室 （ https://1024lab.net ），Since 2012
 */
@Service
public class PoliceFormTemplateService {

    @Autowired
    private PoliceFormTemplateDao policeFormTemplateDao;

    @Autowired
    private PoliceFormFieldDao policeFormFieldDao;

    @Autowired
    private PoliceFormConfigManager policeFormConfigManager;

    /**
     * 获取模板列表
     */
    public ResponseDTO<List<PoliceFormTemplateVO>> getTemplateList() {
        LambdaQueryWrapper<PoliceFormTemplateEntity> query = new LambdaQueryWrapper<>();
        query.orderByDesc(PoliceFormTemplateEntity::getCreateTime);

        List<PoliceFormTemplateEntity> templates = policeFormTemplateDao.selectList(query);
        List<PoliceFormTemplateVO> result = SmartBeanUtil.copyList(templates, PoliceFormTemplateVO.class);

        // 设置警情类型名称和字段数量
        for (PoliceFormTemplateVO templateVO : result) {
            templateVO.setReportTypeName(getReportTypeName(templateVO.getReportType()));

            // 统计字段数量
            LambdaQueryWrapper<PoliceFormFieldEntity> fieldQuery = new LambdaQueryWrapper<>();
            fieldQuery.eq(PoliceFormFieldEntity::getTemplateId, templateVO.getId())
                     .eq(PoliceFormFieldEntity::getStatus, true);
            Long fieldCountLong = policeFormFieldDao.selectCount(fieldQuery);
            int fieldCount = fieldCountLong != null ? fieldCountLong.intValue() : 0;
            templateVO.setFieldCount(fieldCount);
        }

        return ResponseDTO.ok(result);
    }

    /**
     * 创建模板
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> createTemplate(PoliceFormTemplateAddForm form) {
        // 检查是否已存在该类型的模板
        LambdaQueryWrapper<PoliceFormTemplateEntity> query = new LambdaQueryWrapper<>();
        query.eq(PoliceFormTemplateEntity::getReportType, form.getReportType())
             .eq(PoliceFormTemplateEntity::getOrganizationId, form.getOrganizationId());

        PoliceFormTemplateEntity existing = policeFormTemplateDao.selectOne(query);
        if (existing != null) {
            return ResponseDTO.userErrorParam("该警情类型已存在模板配置");
        }

        PoliceFormTemplateEntity template = SmartBeanUtil.copy(form, PoliceFormTemplateEntity.class);
        template.setStatus(true);
        template.setCreateUserId(SmartRequestUtil.getRequestUserId());
        template.setCreateTime(LocalDateTime.now());

        policeFormTemplateDao.insert(template);

        return ResponseDTO.ok();
    }

    /**
     * 获取模板详情
     */
    public ResponseDTO<PoliceFormConfigVO> getTemplateDetail(Long templateId) {
        PoliceFormTemplateEntity template = policeFormTemplateDao.selectById(templateId);
        if (template == null) {
            return ResponseDTO.userErrorParam("模板不存在");
        }

        PoliceFormConfigVO config = policeFormConfigManager.getFormConfig(template.getReportType(), template.getOrganizationId());
        return ResponseDTO.ok(config);
    }

    /**
     * 保存字段配置
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> saveFields(PoliceFormFieldSaveForm form) {
        PoliceFormTemplateEntity template = policeFormTemplateDao.selectById(form.getTemplateId());
        if (template == null) {
            return ResponseDTO.userErrorParam("模板不存在");
        }

        // 删除原有字段
        policeFormFieldDao.deleteByTemplateId(form.getTemplateId());

        // 保存新字段
        if (form.getFields() != null && !form.getFields().isEmpty()) {
            List<PoliceFormFieldEntity> fieldEntities = new ArrayList<>();

            // 处理字段层级关系
            Map<String, Long> fieldKeyToIdMap = new HashMap<>();

            // 第一轮：保存顶级字段
            for (PoliceFormFieldSaveForm.FieldItem field : form.getFields()) {
                if (field.getParentFieldId() == null) { // 顶级字段
                    PoliceFormFieldEntity entity = convertToEntity(field, form.getTemplateId());
                    fieldEntities.add(entity);
                }
            }

            // 批量插入顶级字段
            if (!fieldEntities.isEmpty()) {
                policeFormFieldDao.batchInsertFields(fieldEntities);

                // 建立key到ID的映射
                for (int i = 0; i < fieldEntities.size(); i++) {
                    PoliceFormFieldEntity entity = fieldEntities.get(i);
                    fieldKeyToIdMap.put(entity.getFieldKey(), entity.getId());
                }
            }

            // 第二轮：保存子字段
            List<PoliceFormFieldEntity> childFieldEntities = new ArrayList<>();
            for (PoliceFormFieldSaveForm.FieldItem field : form.getFields()) {
                if (field.getChildren() != null && !field.getChildren().isEmpty()) {
                    Long parentId = fieldKeyToIdMap.get(field.getFieldKey());
                    for (PoliceFormFieldSaveForm.FieldItem childField : field.getChildren()) {
                        PoliceFormFieldEntity childEntity = convertToEntity(childField, form.getTemplateId());
                        childEntity.setParentFieldId(parentId);
                        childFieldEntities.add(childEntity);
                    }
                }
            }

            if (!childFieldEntities.isEmpty()) {
                policeFormFieldDao.batchInsertFields(childFieldEntities);
            }
        }

        return ResponseDTO.ok();
    }

    /**
     * 删除模板
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> deleteTemplate(Long templateId) {
        PoliceFormTemplateEntity template = policeFormTemplateDao.selectById(templateId);
        if (template == null) {
            return ResponseDTO.userErrorParam("模板不存在");
        }

        // 删除模板和关联字段
        policeFormTemplateDao.deleteById(templateId);
        policeFormFieldDao.deleteByTemplateId(templateId);

        return ResponseDTO.ok();
    }

    /**
     * 转换字段表单到实体
     */
    private PoliceFormFieldEntity convertToEntity(PoliceFormFieldSaveForm.FieldItem field, Long templateId) {
        PoliceFormFieldEntity entity = new PoliceFormFieldEntity();
        entity.setTemplateId(templateId);
        entity.setFieldKey(field.getFieldKey());
        entity.setFieldLabel(field.getFieldLabel());
        entity.setFieldType(field.getFieldType());
        entity.setIsRequired(field.getIsRequired());
        entity.setFieldIcon(field.getFieldIcon());
        entity.setPlaceholder(field.getPlaceholder());
        entity.setSortOrder(field.getSortOrder());
        entity.setStepNumber(field.getStepNumber());
        entity.setValidationRules(field.getValidationRules());
        entity.setDefaultValue(field.getDefaultValue());
        entity.setStatus(true);
        entity.setCreateUserId(SmartRequestUtil.getRequestUserId());
        entity.setCreateTime(LocalDateTime.now());

        // 处理选项
        if (field.getFieldOptions() != null && !field.getFieldOptions().isEmpty()) {
            entity.setFieldOptions(JSON.toJSONString(field.getFieldOptions()));
        }
        if (field.getQuickOptions() != null && !field.getQuickOptions().isEmpty()) {
            entity.setQuickOptions(JSON.toJSONString(field.getQuickOptions()));
        }

        // 设置数据字典编码
        entity.setDictCode(field.getDictCode());

        return entity;
    }

    /**
     * 获取警情类型名称
     */
    private String getReportTypeName(Integer reportType) {
        Map<Integer, String> typeNames = new HashMap<>();
        typeNames.put(1, "火灾事故");
        typeNames.put(2, "抢险救援");
        typeNames.put(3, "医疗急救");
        typeNames.put(4, "交通事故");
        typeNames.put(5, "治安事件");
        typeNames.put(6, "刑事案件");
        typeNames.put(7, "自然灾害");
        typeNames.put(99, "其他事件");

        return typeNames.getOrDefault(reportType, "未知类型");
    }
}