package net.lab1024.sa.admin.module.support.im.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.admin.module.support.im.domain.entity.IMGroupMemberEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IM群组成员Dao
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface IMGroupMemberDao extends BaseMapper<IMGroupMemberEntity> {

    /**
     * 查询群组成员列表
     */
    List<IMGroupMemberEntity> selectByGroupMappingId(@Param("groupMappingId") Long groupMappingId);

    /**
     * 查询群组中的指定员工
     */
    IMGroupMemberEntity selectByGroupAndEmployee(@Param("groupMappingId") Long groupMappingId,
                                                  @Param("employeeId") Long employeeId);

    /**
     * 查询员工加入的所有群组
     */
    List<IMGroupMemberEntity> selectByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * 查询群组成员数量
     */
    Integer countByGroupMappingId(@Param("groupMappingId") Long groupMappingId);

    /**
     * 查询指定角色的成员
     */
    List<IMGroupMemberEntity> selectByGroupAndRole(@Param("groupMappingId") Long groupMappingId,
                                                    @Param("roleInGroup") Integer roleInGroup);

    /**
     * 批量插入成员
     */
    int batchInsert(@Param("members") List<IMGroupMemberEntity> members);

    /**
     * 更新成员角色
     */
    int updateMemberRole(@Param("id") Long id, @Param("roleInGroup") Integer roleInGroup);

    /**
     * 软删除成员(退群)
     */
    int softDelete(@Param("id") Long id);
}
