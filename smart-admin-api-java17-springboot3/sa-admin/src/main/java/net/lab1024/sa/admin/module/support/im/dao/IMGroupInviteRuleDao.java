package net.lab1024.sa.admin.module.support.im.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.admin.module.support.im.domain.entity.IMGroupInviteRuleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IM群组邀请规则Dao
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface IMGroupInviteRuleDao extends BaseMapper<IMGroupInviteRuleEntity> {

    /**
     * 根据规则编码查询
     */
    IMGroupInviteRuleEntity selectByRuleCode(@Param("ruleCode") String ruleCode);

    /**
     * 查询所有启用的规则(按优先级降序)
     */
    List<IMGroupInviteRuleEntity> selectEnabledRules();

    /**
     * 根据规则类型查询启用的规则
     */
    List<IMGroupInviteRuleEntity> selectEnabledRulesByType(@Param("ruleType") Integer ruleType);

    /**
     * 更新规则启用状态
     */
    int updateEnabledFlag(@Param("id") Long id, @Param("enabledFlag") Boolean enabledFlag);

    /**
     * 更新规则优先级
     */
    int updatePriority(@Param("id") Long id, @Param("priority") Integer priority);
}
