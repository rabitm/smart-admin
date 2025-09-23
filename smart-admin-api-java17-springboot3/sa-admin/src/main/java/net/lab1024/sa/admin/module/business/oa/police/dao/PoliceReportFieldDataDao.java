package net.lab1024.sa.admin.module.business.oa.police.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceReportFieldDataEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 警情专业字段数据DAO
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-23
 * @Copyright 1024创新实验室 （ https://1024lab.net ），Since 2012
 */
@Mapper
public interface PoliceReportFieldDataDao extends BaseMapper<PoliceReportFieldDataEntity> {

    /**
     * 批量插入字段数据
     */
    void batchInsertFieldData(@Param("fieldDataList") List<PoliceReportFieldDataEntity> fieldDataList);

    /**
     * 根据警情ID删除所有字段数据
     */
    void deleteByReportId(@Param("reportId") Long reportId);

    /**
     * 根据警情ID获取字段数据
     */
    List<PoliceReportFieldDataEntity> getFieldDataByReportId(@Param("reportId") Long reportId);
}