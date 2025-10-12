package net.lab1024.sa.admin.module.support.im.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.admin.module.support.im.domain.entity.IMMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IM消息镜像 DAO
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-11
 * @Copyright 1024创新实验室
 */
@Mapper
public interface IMMessageDao extends BaseMapper<IMMessageEntity> {

    /**
     * 根据消息ID查询消息
     *
     * @param messageId OpenIM消息ID
     * @return 消息实体
     */
    IMMessageEntity selectByMessageId(@Param("messageId") String messageId);

    /**
     * 根据警情ID查询消息列表(按时间倒序)
     *
     * @param reportId 警情ID
     * @param limit 数量限制
     * @return 消息列表
     */
    List<IMMessageEntity> selectByReportId(@Param("reportId") Long reportId, @Param("limit") Integer limit);

    /**
     * 根据警情ID和时间范围查询消息列表
     *
     * @param reportId 警情ID
     * @param beforeTime 时间戳(毫秒),查询此时间之前的消息
     * @param limit 数量限制
     * @return 消息列表
     */
    List<IMMessageEntity> selectByReportIdAndTime(
            @Param("reportId") Long reportId,
            @Param("beforeTime") Long beforeTime,
            @Param("limit") Integer limit
    );

    /**
     * 根据群组ID查询消息列表(按时间倒序)
     *
     * @param groupId OpenIM群组ID
     * @param limit 数量限制
     * @return 消息列表
     */
    List<IMMessageEntity> selectByGroupId(@Param("groupId") String groupId, @Param("limit") Integer limit);

    /**
     * 统计警情的消息总数
     *
     * @param reportId 警情ID
     * @return 消息总数
     */
    Integer countByReportId(@Param("reportId") Long reportId);

    /**
     * 批量插入消息(用于历史数据导入)
     *
     * @param messageList 消息列表
     * @return 插入数量
     */
    Integer batchInsert(@Param("list") List<IMMessageEntity> messageList);
}
