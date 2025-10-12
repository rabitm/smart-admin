package net.lab1024.sa.admin.module.support.im.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.admin.module.support.im.domain.entity.IMGroupMappingEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IM群组映射Dao
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface IMGroupMappingDao extends BaseMapper<IMGroupMappingEntity> {

    /**
     * 根据警情ID查询群组映射
     */
    IMGroupMappingEntity selectByReportId(@Param("reportId") Long reportId);

    /**
     * 根据OpenIM群组ID查询映射
     */
    IMGroupMappingEntity selectByOpenimGroupId(@Param("openimGroupId") String openimGroupId);

    /**
     * 批量查询警情的群组映射
     */
    List<IMGroupMappingEntity> selectByReportIds(@Param("reportIds") List<Long> reportIds);

    /**
     * 查询指定状态的群组
     */
    List<IMGroupMappingEntity> selectByStatus(@Param("groupStatus") Integer groupStatus,
                                               @Param("limit") Integer limit);

    /**
     * 更新成员数量
     */
    int updateMemberCount(@Param("id") Long id, @Param("memberCount") Integer memberCount);

    /**
     * 更新群组状态
     */
    int updateGroupStatus(@Param("id") Long id, @Param("groupStatus") Integer groupStatus);
}
