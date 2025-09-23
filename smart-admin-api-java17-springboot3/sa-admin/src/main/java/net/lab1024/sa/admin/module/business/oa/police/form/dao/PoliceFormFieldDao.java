package net.lab1024.sa.admin.module.business.oa.police.form.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.admin.module.business.oa.police.form.domain.entity.PoliceFormFieldEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 警情表单字段DAO
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-22
 * @Copyright 1024创新实验室 （ https://1024lab.net ），Since 2012
 */
@Mapper
public interface PoliceFormFieldDao extends BaseMapper<PoliceFormFieldEntity> {

    /**
     * 根据模板ID获取字段列表
     */
    List<PoliceFormFieldEntity> getFieldsByTemplateId(@Param("templateId") Long templateId);

    /**
     * 根据模板ID和步骤号获取字段列表
     */
    List<PoliceFormFieldEntity> getFieldsByTemplateAndStep(@Param("templateId") Long templateId,
                                                           @Param("stepNumber") Integer stepNumber);

    /**
     * 批量保存字段
     */
    int batchInsertFields(@Param("fields") List<PoliceFormFieldEntity> fields);

    /**
     * 根据模板ID删除所有字段
     */
    int deleteByTemplateId(@Param("templateId") Long templateId);
}