package net.lab1024.sa.admin.module.support.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IM消息镜像实体
 *
 * 用途: 在后端数据库存储所有 IM 消息副本
 * 解决: 无痕模式首次登录无法加载历史消息的问题
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-11
 * @Copyright 1024创新实验室
 */
@Data
@TableName("t_im_message")
public class IMMessageEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 警情ID
     */
    private Long reportId;

    /**
     * OpenIM群组ID (格式: group_report_5)
     */
    private String groupId;

    /**
     * OpenIM消息ID (clientMsgID)
     */
    private String messageId;

    /**
     * OpenIM服务器消息ID (serverMsgID)
     */
    private String serverMessageId;

    /**
     * 会话ID (格式: sg_group_report_5)
     */
    private String conversationId;

    // ========== 发送者信息 ==========

    /**
     * 发送者OpenIM用户ID (格式: emp_1)
     */
    private String senderId;

    /**
     * 发送者员工ID
     */
    private Long senderEmployeeId;

    /**
     * 发送者姓名
     */
    private String senderName;

    /**
     * 发送者头像URL
     */
    private String senderAvatar;

    // ========== 消息内容 ==========

    /**
     * 消息类型
     * 101-文本, 102-图片, 103-语音, 104-视频, 105-文件
     */
    private Integer contentType;

    /**
     * 消息内容 (文本消息的文本内容)
     */
    private String content;

    /**
     * 完整消息内容JSON (用于复杂消息类型)
     */
    private String contentJson;

    // ========== 时间信息 ==========

    /**
     * 发送时间戳(毫秒)
     */
    private Long sendTime;

    /**
     * 消息序号
     */
    private Long seq;

    // ========== 消息状态 ==========

    /**
     * 消息状态
     * 1-正常, 2-已撤回, 3-已删除
     */
    private Integer status;

    /**
     * 是否已读
     * 0-未读, 1-已读
     */
    private Integer isRead;

    // ========== 系统字段 ==========

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除标记
     * 0-未删除, 1-已删除
     */
    private Integer deletedFlag;
}
