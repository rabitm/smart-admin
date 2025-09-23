package net.lab1024.sa.admin.module.business.oa.police.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceReportEntity;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.PoliceReportQueryForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.vo.PoliceReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 警情录入DAO
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-18
 */
@Mapper
public interface PoliceReportDao extends BaseMapper<PoliceReportEntity> {

    /**
     * 根据警情编号查询
     */
    PoliceReportEntity queryByReportNumber(@Param("reportNumber") String reportNumber, @Param("excludeReportId") Long excludeReportId, @Param("deletedFlag") Boolean deletedFlag);

    /**
     * 删除警情信息
     */
    void deletePoliceReport(@Param("reportId") Long reportId, @Param("deletedFlag") Boolean deletedFlag);

    /**
     * 警情信息分页查询
     */
    List<PoliceReportVO> queryPage(Page page, @Param("queryForm") PoliceReportQueryForm queryForm);

    /**
     * 查询警情信息详情
     */
    PoliceReportVO getDetail(@Param("reportId") Long reportId, @Param("deletedFlag") Boolean deletedFlag);

    /**
     * 根据报警人电话查询警情列表
     */
    List<PoliceReportVO> queryByReporterPhone(@Param("reporterPhone") String reporterPhone, @Param("deletedFlag") Boolean deletedFlag);

    /**
     * 根据处理人员查询警情列表
     */
    List<PoliceReportVO> queryByHandler(@Param("handlerId") Long handlerId, @Param("deletedFlag") Boolean deletedFlag);

    /**
     * 统计各状态警情数量
     */
    List<PoliceReportVO> getStatusStatistics(@Param("deletedFlag") Boolean deletedFlag);

    /**
     * 生成警情编号（获取当天最大编号）
     */
    String getMaxReportNumberByDate(@Param("datePrefix") String datePrefix);

    /**
     * 获取历史地址建议
     */
    List<String> getLocationSuggestions(@Param("keyword") String keyword, @Param("deletedFlag") Boolean deletedFlag);
}