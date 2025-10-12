package net.lab1024.sa.admin.module.support.im.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.lab1024.sa.admin.module.support.im.domain.entity.IMOperationLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * IM操作日志Dao
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface IMOperationLogDao extends BaseMapper<IMOperationLogEntity> {

    /**
     * 分页查询操作日志
     */
    Page<IMOperationLogEntity> selectPage(Page<IMOperationLogEntity> page,
                                          @Param("operationType") String operationType,
                                          @Param("successFlag") Boolean successFlag,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 根据目标查询日志
     */
    List<IMOperationLogEntity> selectByTarget(@Param("targetType") String targetType,
                                              @Param("targetId") String targetId,
                                              @Param("limit") Integer limit);

    /**
     * 根据操作人查询日志
     */
    List<IMOperationLogEntity> selectByOperator(@Param("operatorId") Long operatorId,
                                                @Param("limit") Integer limit);

    /**
     * 统计操作类型分布
     */
    List<java.util.Map<String, Object>> countByOperationType(@Param("startTime") LocalDateTime startTime,
                                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 统计成功率
     */
    java.util.Map<String, Object> countSuccessRate(@Param("operationType") String operationType,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 删除过期日志
     */
    int deleteExpiredLogs(@Param("expireTime") LocalDateTime expireTime);
}
