package net.lab1024.sa.admin.module.support.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IM配置实体
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@TableName("t_im_config")
public class IMConfigEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置类型:SERVER,SYSTEM,BUSINESS
     */
    private String configType;

    /**
     * 值类型:STRING,NUMBER,BOOLEAN,JSON
     */
    private String valueType;

    /**
     * 配置说明
     */
    private String description;

    /**
     * 是否加密
     */
    private Boolean isEncrypted;

    /**
     * 是否可编辑
     */
    private Boolean editableFlag;

    /**
     * 删除标识
     */
    private Boolean deletedFlag;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
