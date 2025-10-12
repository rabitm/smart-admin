-- ========================================
-- SmartAdmin v3.28.1 OpenIM 消息镜像存储
-- ========================================
-- 功能: 解决无痕模式首次登录无法加载历史消息的问题
-- 方案: 在后端数据库存储所有 IM 消息副本
-- 日期: 2025-10-11
-- 作者: Claude Code Assistant
-- ========================================

-- 创建 IM 消息镜像表
DROP TABLE IF EXISTS `t_im_message`;
CREATE TABLE `t_im_message` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `report_id` BIGINT(20) NOT NULL COMMENT '警情ID',
    `group_id` VARCHAR(100) NOT NULL COMMENT 'OpenIM群组ID (格式: group_report_5)',
    `message_id` VARCHAR(100) NOT NULL COMMENT 'OpenIM消息ID (clientMsgID)',
    `server_message_id` VARCHAR(100) NULL COMMENT 'OpenIM服务器消息ID (serverMsgID)',
    `conversation_id` VARCHAR(100) NULL COMMENT '会话ID (格式: sg_group_report_5)',

    -- 发送者信息
    `sender_id` VARCHAR(100) NOT NULL COMMENT '发送者OpenIM用户ID (格式: emp_1)',
    `sender_employee_id` BIGINT(20) NULL COMMENT '发送者员工ID',
    `sender_name` VARCHAR(100) NULL COMMENT '发送者姓名',
    `sender_avatar` VARCHAR(500) NULL COMMENT '发送者头像URL',

    -- 消息内容
    `content_type` INT(11) NOT NULL DEFAULT 101 COMMENT '消息类型 (101-文本, 102-图片, 103-语音, 104-视频, 105-文件)',
    `content` TEXT NULL COMMENT '消息内容 (文本消息的文本内容)',
    `content_json` TEXT NULL COMMENT '完整消息内容JSON (用于复杂消息类型)',

    -- 时间信息
    `send_time` BIGINT(20) NOT NULL COMMENT '发送时间戳(毫秒)',
    `seq` BIGINT(20) NULL COMMENT '消息序号',

    -- 消息状态
    `status` TINYINT(4) NOT NULL DEFAULT 1 COMMENT '消息状态 (1-正常, 2-已撤回, 3-已删除)',
    `is_read` TINYINT(4) NOT NULL DEFAULT 0 COMMENT '是否已读 (0-未读, 1-已读)',

    -- 系统字段
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_flag` TINYINT(4) NOT NULL DEFAULT 0 COMMENT '删除标记 (0-未删除, 1-已删除)',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_id` (`message_id`) COMMENT '消息ID唯一索引',
    KEY `idx_report_id` (`report_id`) COMMENT '警情ID索引',
    KEY `idx_group_id` (`group_id`) COMMENT '群组ID索引',
    KEY `idx_send_time` (`send_time`) COMMENT '发送时间索引',
    KEY `idx_sender_id` (`sender_id`) COMMENT '发送者ID索引',
    KEY `idx_create_time` (`create_time`) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IM消息镜像表';

-- ========================================
-- 索引说明
-- ========================================
-- 1. uk_message_id: 防止重复保存同一条消息
-- 2. idx_report_id: 按警情ID查询消息
-- 3. idx_group_id: 按群组ID查询消息
-- 4. idx_send_time: 按时间范围查询消息(分页)
-- 5. idx_sender_id: 按发送者查询消息
-- 6. idx_create_time: 按创建时间查询(用于数据维护)

-- ========================================
-- 使用场景
-- ========================================
-- 1. 无痕模式首次登录: 从数据库加载历史消息
-- 2. 消息搜索: 全文搜索消息内容
-- 3. 消息统计: 统计群组活跃度、消息数量等
-- 4. 审计合规: 保留完整的消息记录用于追溯
-- 5. 数据备份: 避免依赖 OpenIM 的数据持久化

-- ========================================
-- 数据维护建议
-- ========================================
-- 1. 定期归档: 归档 3 个月前的消息到历史表
-- 2. 定期清理: 清理已删除的消息(deleted_flag=1)
-- 3. 索引优化: 根据实际查询情况调整索引
-- 4. 分区表: 如果数据量大,可以按 report_id 或 send_time 分区

-- ========================================
-- 版本信息
-- ========================================
-- 版本: v3.28.1
-- 更新内容: 新增 IM 消息镜像存储功能
-- 依赖版本: v3.27.0+ (OpenIM 基础集成)
