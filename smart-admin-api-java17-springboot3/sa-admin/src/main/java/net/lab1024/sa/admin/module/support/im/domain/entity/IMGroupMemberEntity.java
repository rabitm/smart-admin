package net.lab1024.sa.admin.module.support.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IM群组成员实体
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@TableName("t_im_group_member")
public class IMGroupMemberEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 群组映射ID
     */
    private Long groupMappingId;

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * OpenIM用户ID
     */
    private String openimUserId;

    /**
     * 群内角色:1-普通成员,2-管理员,3-群主
     */
    private Integer roleInGroup;

    /**
     * 加入方式:1-自动拉入,2-手动邀请,3-主动申请
     */
    private Integer joinType;

    /**
     * 加入来源(规则名称或邀请人)
     */
    private String joinSource;

    /**
     * 加入时间
     */
    private LocalDateTime joinTime;

    /**
     * 禁言结束时间
     */
    private LocalDateTime muteEndTime;

    /**
     * 扩展字段
     */
    private String ex;

    /**
     * 删除标识(退群)
     */
    private Boolean deletedFlag;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
