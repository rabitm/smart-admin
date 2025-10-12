package net.lab1024.sa.admin.module.support.im.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.admin.module.support.im.domain.entity.IMConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IM配置Dao
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface IMConfigDao extends BaseMapper<IMConfigEntity> {

    /**
     * 根据配置键查询
     */
    IMConfigEntity selectByConfigKey(@Param("configKey") String configKey);

    /**
     * 根据配置类型查询所有配置
     */
    List<IMConfigEntity> selectByConfigType(@Param("configType") String configType);

    /**
     * 查询所有可编辑的配置
     */
    List<IMConfigEntity> selectEditableConfigs();

    /**
     * 更新配置值
     */
    int updateConfigValue(@Param("configKey") String configKey,
                         @Param("configValue") String configValue);

    /**
     * 批量更新配置
     */
    int batchUpdateConfigValues(@Param("configs") List<IMConfigEntity> configs);
}
