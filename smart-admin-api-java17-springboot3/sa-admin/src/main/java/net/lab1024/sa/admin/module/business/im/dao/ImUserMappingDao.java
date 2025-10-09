package net.lab1024.sa.admin.module.business.im.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.admin.module.business.im.domain.entity.ImUserMappingEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * IM用户映射DAO
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Mapper
public interface ImUserMappingDao extends BaseMapper<ImUserMappingEntity> {
}
