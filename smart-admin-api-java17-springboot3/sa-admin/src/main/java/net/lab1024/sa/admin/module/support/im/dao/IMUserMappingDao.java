package net.lab1024.sa.admin.module.support.im.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.admin.module.support.im.domain.entity.IMUserMappingEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IM用户映射Dao
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface IMUserMappingDao extends BaseMapper<IMUserMappingEntity> {

    /**
     * 根据员工ID查询映射
     */
    IMUserMappingEntity selectByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * 根据OpenIM用户ID查询映射
     */
    IMUserMappingEntity selectByOpenimUserId(@Param("openimUserId") String openimUserId);

    /**
     * 批量查询员工的映射关系
     */
    List<IMUserMappingEntity> selectByEmployeeIds(@Param("employeeIds") List<Long> employeeIds);

    /**
     * 查询待同步的用户
     */
    List<IMUserMappingEntity> selectPendingSync(@Param("limit") Integer limit);

    /**
     * 查询同步失败的用户
     */
    List<IMUserMappingEntity> selectFailedSync(@Param("limit") Integer limit);

    /**
     * 更新同步状态
     */
    int updateSyncStatus(@Param("id") Long id,
                        @Param("syncStatus") Integer syncStatus,
                        @Param("errorMessage") String errorMessage);
}
