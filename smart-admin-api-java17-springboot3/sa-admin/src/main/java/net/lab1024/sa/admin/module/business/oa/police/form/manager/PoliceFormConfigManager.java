package net.lab1024.sa.admin.module.business.oa.police.form.manager;

import com.alibaba.fastjson2.JSON;
import net.lab1024.sa.admin.module.business.oa.police.form.dao.PoliceFormFieldDao;
import net.lab1024.sa.admin.module.business.oa.police.form.dao.PoliceFormTemplateDao;
import net.lab1024.sa.admin.module.business.oa.police.form.domain.entity.PoliceFormFieldEntity;
import net.lab1024.sa.admin.module.business.oa.police.form.domain.entity.PoliceFormTemplateEntity;
import net.lab1024.sa.admin.module.business.oa.police.form.domain.vo.PoliceFormConfigVO;
import net.lab1024.sa.base.common.util.SmartStringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 警情表单配置管理器
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-22
 * @Copyright 1024创新实验室 （ https://1024lab.net ），Since 2012
 */
@Service
public class PoliceFormConfigManager {

    @Autowired
    private PoliceFormTemplateDao policeFormTemplateDao;

    @Autowired
    private PoliceFormFieldDao policeFormFieldDao;

    /**
     * 获取表单配置
     */
    public PoliceFormConfigVO getFormConfig(Integer reportType, Long organizationId) {
        // 1. 获取模板
        PoliceFormTemplateEntity template = policeFormTemplateDao.getTemplateByTypeAndOrg(reportType, organizationId);
        if (template == null) {
            return null;
        }

        // 2. 获取字段列表
        List<PoliceFormFieldEntity> allFields = policeFormFieldDao.getFieldsByTemplateId(template.getId());
        if (CollectionUtils.isEmpty(allFields)) {
            return createEmptyConfig(reportType, template);
        }

        // 3. 构建配置
        return buildFormConfig(reportType, template, allFields);
    }

    /**
     * 构建表单配置
     */
    private PoliceFormConfigVO buildFormConfig(Integer reportType, PoliceFormTemplateEntity template,
                                             List<PoliceFormFieldEntity> allFields) {
        PoliceFormConfigVO config = new PoliceFormConfigVO();
        config.setReportType(reportType);
        config.setTemplateId(template.getId());
        config.setTemplateName(template.getTemplateName());

        // 按步骤分组 - 过滤掉 stepNumber 为 null 的字段
        Map<Integer, List<PoliceFormFieldEntity>> stepGroups = allFields.stream()
                .filter(field -> field.getStepNumber() != null) // 过滤 null 值
                .collect(Collectors.groupingBy(PoliceFormFieldEntity::getStepNumber));

        // 构建第二步字段
        config.setStep2Fields(buildFieldList(stepGroups.get(2), allFields));

        // 构建第三步字段
        config.setStep3Fields(buildFieldList(stepGroups.get(3), allFields));

        return config;
    }

    /**
     * 构建字段列表
     */
    private List<PoliceFormConfigVO.PoliceFormFieldVO> buildFieldList(List<PoliceFormFieldEntity> stepFields,
                                                                     List<PoliceFormFieldEntity> allFields) {
        if (CollectionUtils.isEmpty(stepFields)) {
            return new ArrayList<>();
        }

        // 创建字段映射
        Map<Long, PoliceFormFieldEntity> fieldMap = allFields.stream()
                .collect(Collectors.toMap(PoliceFormFieldEntity::getId, f -> f));

        // 获取顶级字段（没有父字段的）
        List<PoliceFormFieldEntity> topLevelFields = stepFields.stream()
                .filter(f -> f.getParentFieldId() == null)
                .sorted(Comparator.comparing(PoliceFormFieldEntity::getSortOrder))
                .collect(Collectors.toList());

        return topLevelFields.stream()
                .map(field -> buildFieldVO(field, fieldMap, allFields))
                .collect(Collectors.toList());
    }

    /**
     * 构建字段VO
     */
    private PoliceFormConfigVO.PoliceFormFieldVO buildFieldVO(PoliceFormFieldEntity field,
                                                            Map<Long, PoliceFormFieldEntity> fieldMap,
                                                            List<PoliceFormFieldEntity> allFields) {
        PoliceFormConfigVO.PoliceFormFieldVO fieldVO = new PoliceFormConfigVO.PoliceFormFieldVO();

        fieldVO.setId(field.getId());
        fieldVO.setKey(field.getFieldKey());
        fieldVO.setLabel(field.getFieldLabel());
        fieldVO.setType(field.getFieldType());
        fieldVO.setRequired(field.getIsRequired());
        fieldVO.setIcon(field.getFieldIcon());
        fieldVO.setPlaceholder(field.getPlaceholder());
        fieldVO.setDefaultValue(field.getDefaultValue());
        fieldVO.setSortOrder(field.getSortOrder());
        fieldVO.setValidationRules(field.getValidationRules());
        fieldVO.setDictCode(field.getDictCode());

        // 解析选项
        if (SmartStringUtil.isNotBlank(field.getFieldOptions())) {
            try {
                fieldVO.setOptions(JSON.parseArray(field.getFieldOptions(), String.class));
            } catch (Exception e) {
                fieldVO.setOptions(new ArrayList<>());
            }
        }

        // 解析快捷选项
        if (SmartStringUtil.isNotBlank(field.getQuickOptions())) {
            try {
                fieldVO.setQuickOptions(JSON.parseArray(field.getQuickOptions(), String.class));
            } catch (Exception e) {
                fieldVO.setQuickOptions(new ArrayList<>());
            }
        }

        // 构建子字段（用于组合字段）
        if ("compact-group".equals(field.getFieldType())) {
            List<PoliceFormFieldEntity> childFields = allFields.stream()
                    .filter(f -> field.getId().equals(f.getParentFieldId()))
                    .sorted(Comparator.comparing(PoliceFormFieldEntity::getSortOrder))
                    .collect(Collectors.toList());

            List<PoliceFormConfigVO.PoliceFormFieldVO> childFieldVOs = childFields.stream()
                    .map(childField -> buildFieldVO(childField, fieldMap, allFields))
                    .collect(Collectors.toList());

            fieldVO.setFields(childFieldVOs);
        }

        return fieldVO;
    }

    /**
     * 创建空配置
     */
    private PoliceFormConfigVO createEmptyConfig(Integer reportType, PoliceFormTemplateEntity template) {
        PoliceFormConfigVO config = new PoliceFormConfigVO();
        config.setReportType(reportType);
        config.setTemplateId(template.getId());
        config.setTemplateName(template.getTemplateName());
        config.setStep2Fields(new ArrayList<>());
        config.setStep3Fields(new ArrayList<>());
        return config;
    }
}