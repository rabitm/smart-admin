package net.lab1024.sa.admin.module.business.oa.police.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceReportOperationLogEntity;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.CollaborationHistoryQueryForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.vo.CollaborationHistoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 警情操作记录DAO
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@Mapper
public interface PoliceReportOperationLogDao extends BaseMapper<PoliceReportOperationLogEntity> {

    /**
     * 分页查询协作历史记录
     */
    List<CollaborationHistoryVO> queryCollaborationHistory(Page<?> page, @Param("query") CollaborationHistoryQueryForm queryForm);

    /**
     * 查询协作历史记录总数
     */
    Long countCollaborationHistory(@Param("query") CollaborationHistoryQueryForm queryForm);

    /**
     * 根据报告ID查询操作记录
     */
    List<PoliceReportOperationLogEntity> queryByReportId(@Param("reportId") Long reportId, @Param("limit") Integer limit);

    /**
     * 根据用户ID查询操作记录
     */
    List<PoliceReportOperationLogEntity> queryByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 删除指定报告的所有操作记录
     */
    void deleteByReportId(@Param("reportId") Long reportId);

    /**
     * 统计操作记录数量
     */
    Long countOperationsByReportId(@Param("reportId") Long reportId);

    /**
     * 查询最近的操作记录
     */
    List<PoliceReportOperationLogEntity> queryRecentOperations(@Param("reportId") Long reportId, @Param("hours") Integer hours);

    // TODO: 后续实现高级统计和异常检测功能
    // 暂时注释掉未实现的方法，避免编译错误
}