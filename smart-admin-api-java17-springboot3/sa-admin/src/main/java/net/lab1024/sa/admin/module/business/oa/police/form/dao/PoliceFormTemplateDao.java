package net.lab1024.sa.admin.module.business.oa.police.form.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.admin.module.business.oa.police.form.domain.entity.PoliceFormTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 警情表单模板DAO
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-22
 * @Copyright 1024创新实验室 （ https://1024lab.net ），Since 2012
 */
@Mapper
public interface PoliceFormTemplateDao extends BaseMapper<PoliceFormTemplateEntity> {

    /**
     * 根据警情类型和组织ID获取模板
     * 优先获取组织模板，如果没有则获取全局默认模板
     */
    PoliceFormTemplateEntity getTemplateByTypeAndOrg(@Param("reportType") Integer reportType,
                                                    @Param("organizationId") Long organizationId);
}