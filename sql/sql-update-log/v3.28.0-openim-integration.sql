-- ============================================================
-- SmartAdmin v3.28.0 - OpenIM 即时聊天集成
-- 功能: 为警情管理系统集成OpenIM即时通讯功能
-- 作者: Claude Code Assistant
-- 日期: 2025-10-09
-- ============================================================

-- ============================================================
-- 1. IM用户映射表
-- 功能: 维护SmartAdmin员工与OpenIM用户的映射关系
-- ============================================================
CREATE TABLE `t_im_user_mapping` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id` BIGINT NOT NULL COMMENT '员工ID',
    `openim_user_id` VARCHAR(64) NOT NULL COMMENT 'OpenIM用户ID',
    `nickname` VARCHAR(100) COMMENT '用户昵称',
    `face_url` VARCHAR(500) COMMENT '头像URL',
    `sync_status` TINYINT NOT NULL DEFAULT 1 COMMENT '同步状态:1-已同步,2-同步失败,3-待同步',
    `sync_time` DATETIME COMMENT '同步时间',
    `error_message` VARCHAR(500) COMMENT '错误信息',
    `deleted_flag` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '删除标识',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_employee_id` (`employee_id`),
    UNIQUE KEY `uk_openim_user_id` (`openim_user_id`),
    INDEX `idx_sync_status` (`sync_status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM用户映射表';

-- ============================================================
-- 2. IM群组映射表
-- 功能: 维护警情与OpenIM群组的映射关系
-- ============================================================
CREATE TABLE `t_im_group_mapping` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `report_id` BIGINT NOT NULL COMMENT '警情ID',
    `openim_group_id` VARCHAR(64) NOT NULL COMMENT 'OpenIM群组ID',
    `group_name` VARCHAR(100) NOT NULL COMMENT '群组名称',
    `group_type` TINYINT NOT NULL DEFAULT 2 COMMENT '群组类型:2-工作群',
    `owner_user_id` VARCHAR(64) NOT NULL COMMENT '群主OpenIM用户ID',
    `owner_employee_id` BIGINT NOT NULL COMMENT '群主员工ID',
    `group_status` TINYINT NOT NULL DEFAULT 1 COMMENT '群组状态:1-正常,2-已解散,3-已归档',
    `member_count` INT NOT NULL DEFAULT 0 COMMENT '成员数量',
    `max_member_count` INT NOT NULL DEFAULT 1000 COMMENT '最大成员数量',
    `notification` VARCHAR(500) COMMENT '群公告',
    `introduction` VARCHAR(500) COMMENT '群简介',
    `face_url` VARCHAR(500) COMMENT '群头像URL',
    `need_verification` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否需要验证',
    `deleted_flag` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '删除标识',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_id` (`report_id`),
    UNIQUE KEY `uk_openim_group_id` (`openim_group_id`),
    INDEX `idx_group_status` (`group_status`),
    INDEX `idx_owner_employee_id` (`owner_employee_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM群组映射表';

-- ============================================================
-- 3. IM群组成员表
-- 功能: 记录群组成员详细信息
-- ============================================================
CREATE TABLE `t_im_group_member` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `group_mapping_id` BIGINT NOT NULL COMMENT '群组映射ID',
    `employee_id` BIGINT NOT NULL COMMENT '员工ID',
    `openim_user_id` VARCHAR(64) NOT NULL COMMENT 'OpenIM用户ID',
    `role_in_group` TINYINT NOT NULL DEFAULT 1 COMMENT '群内角色:1-普通成员,2-管理员,3-群主',
    `join_type` TINYINT NOT NULL DEFAULT 1 COMMENT '加入方式:1-自动拉入,2-手动邀请,3-主动申请',
    `join_source` VARCHAR(100) COMMENT '加入来源(规则名称或邀请人)',
    `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `mute_end_time` DATETIME COMMENT '禁言结束时间',
    `ex` VARCHAR(500) COMMENT '扩展字段',
    `deleted_flag` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '删除标识(退群)',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_employee` (`group_mapping_id`, `employee_id`, `deleted_flag`),
    INDEX `idx_employee_id` (`employee_id`),
    INDEX `idx_openim_user_id` (`openim_user_id`),
    INDEX `idx_role_in_group` (`role_in_group`),
    INDEX `idx_join_time` (`join_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM群组成员表';

-- ============================================================
-- 4. IM群组邀请规则配置表
-- 功能: 配置自动邀请成员的规则
-- ============================================================
CREATE TABLE `t_im_group_invite_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `rule_code` VARCHAR(50) NOT NULL COMMENT '规则编码',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `rule_type` TINYINT NOT NULL COMMENT '规则类型:1-按警情类型,2-按部门,3-按角色,4-按警情等级,5-固定人员',
    `rule_config` JSON NOT NULL COMMENT '规则配置(JSON格式)',
    `enabled_flag` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级(数字越大优先级越高)',
    `description` VARCHAR(500) COMMENT '规则描述',
    `deleted_flag` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '删除标识',
    `create_user_id` BIGINT COMMENT '创建人ID',
    `create_user_name` VARCHAR(50) COMMENT '创建人姓名',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_code` (`rule_code`),
    INDEX `idx_rule_type` (`rule_type`),
    INDEX `idx_enabled_priority` (`enabled_flag`, `priority` DESC),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM群组邀请规则配置表';

-- ============================================================
-- 5. IM操作日志表
-- 功能: 记录所有IM相关操作,用于审计和问题排查
-- ============================================================
CREATE TABLE `t_im_operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型:USER_SYNC,GROUP_CREATE,GROUP_INVITE,GROUP_KICK,MSG_SEND等',
    `operation_detail` VARCHAR(500) COMMENT '操作详情',
    `target_type` VARCHAR(50) COMMENT '目标类型:USER,GROUP,MESSAGE',
    `target_id` VARCHAR(100) COMMENT '目标ID',
    `operator_id` BIGINT COMMENT '操作人ID',
    `operator_name` VARCHAR(50) COMMENT '操作人姓名',
    `success_flag` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否成功',
    `error_code` VARCHAR(50) COMMENT '错误码',
    `error_message` TEXT COMMENT '错误信息',
    `request_data` TEXT COMMENT '请求数据',
    `response_data` TEXT COMMENT '响应数据',
    `execution_time` INT COMMENT '执行时间(ms)',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `user_agent` VARCHAR(500) COMMENT 'User Agent',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_operation_type` (`operation_type`),
    INDEX `idx_target_type_id` (`target_type`, `target_id`),
    INDEX `idx_operator_id` (`operator_id`),
    INDEX `idx_success_flag` (`success_flag`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM操作日志表';

-- ============================================================
-- 6. IM配置表
-- 功能: 存储OpenIM服务器配置和系统参数
-- ============================================================
CREATE TABLE `t_im_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` TEXT NOT NULL COMMENT '配置值',
    `config_type` VARCHAR(50) NOT NULL COMMENT '配置类型:SERVER,SYSTEM,BUSINESS',
    `value_type` VARCHAR(20) NOT NULL DEFAULT 'STRING' COMMENT '值类型:STRING,NUMBER,BOOLEAN,JSON',
    `description` VARCHAR(500) COMMENT '配置说明',
    `is_encrypted` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否加密',
    `editable_flag` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否可编辑',
    `deleted_flag` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '删除标识',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`),
    INDEX `idx_config_type` (`config_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM配置表';

-- ============================================================
-- 7. 插入默认配置数据
-- ============================================================
INSERT INTO `t_im_config` (`config_key`, `config_value`, `config_type`, `value_type`, `description`, `is_encrypted`, `editable_flag`) VALUES
('openim.server.api.url', 'http://localhost:10002', 'SERVER', 'STRING', 'OpenIM API服务地址', FALSE, TRUE),
('openim.server.ws.url', 'ws://localhost:10001', 'SERVER', 'STRING', 'OpenIM WebSocket服务地址', FALSE, TRUE),
('openim.admin.user.id', 'imAdmin', 'SERVER', 'STRING', 'OpenIM管理员用户ID', FALSE, TRUE),
('openim.admin.secret', '', 'SERVER', 'STRING', 'OpenIM管理员密钥(需配置)', TRUE, TRUE),
('openim.platform.id', '1', 'SYSTEM', 'NUMBER', '平台ID: 1-iOS,2-Android,3-Windows,4-OSX,5-Web,6-MiniWeb,7-Linux,8-AndroidPad,9-iPad,10-Admin', FALSE, TRUE),
('openim.token.expire.seconds', '7200', 'SYSTEM', 'NUMBER', 'Token有效期(秒),默认2小时', FALSE, TRUE),
('openim.api.timeout.seconds', '10', 'SYSTEM', 'NUMBER', 'API调用超时时间(秒)', FALSE, TRUE),
('openim.api.retry.max.count', '3', 'SYSTEM', 'NUMBER', 'API调用最大重试次数', FALSE, TRUE),
('openim.group.auto.create', 'true', 'BUSINESS', 'BOOLEAN', '警情创建时自动创建IM群组', FALSE, TRUE),
('openim.group.auto.invite', 'true', 'BUSINESS', 'BOOLEAN', '自动邀请成员入群', FALSE, TRUE),
('openim.group.max.members', '500', 'BUSINESS', 'NUMBER', '群组最大成员数', FALSE, TRUE),
('openim.group.name.template', '警情-{reportNumber}', 'BUSINESS', 'STRING', '群组名称模板', FALSE, TRUE),
('openim.user.sync.batch.size', '50', 'SYSTEM', 'NUMBER', '用户批量同步每批数量', FALSE, TRUE),
('openim.group.invite.batch.size', '30', 'SYSTEM', 'NUMBER', '群组邀请每批数量', FALSE, TRUE),
('openim.circuit.breaker.threshold', '5', 'SYSTEM', 'NUMBER', '熔断器失败阈值', FALSE, TRUE),
('openim.circuit.breaker.timeout.seconds', '30', 'SYSTEM', 'NUMBER', '熔断器超时时间(秒)', FALSE, TRUE);

-- ============================================================
-- 8. 插入默认邀请规则
-- ============================================================
INSERT INTO `t_im_group_invite_rule` (`rule_code`, `rule_name`, `rule_type`, `rule_config`, `enabled_flag`, `priority`, `description`) VALUES
('RULE_CREATOR', '警情创建人自动入群', 5, '{"type": "creator", "role": 3}', TRUE, 100, '警情创建人自动作为群主加入群组'),
('RULE_HANDLER', '警情处理人自动入群', 5, '{"type": "handler", "role": 2}', TRUE, 90, '警情指派的处理人自动作为管理员加入群组'),
('RULE_DEPT_HEAD', '部门负责人自动入群', 2, '{"target": "department_head", "departments": [], "role": 2}', TRUE, 80, '相关部门负责人自动作为管理员加入群组'),
('RULE_HIGH_LEVEL', '高等级警情通知上级', 4, '{"report_levels": [1, 2], "notify_superiors": true, "role": 1}', TRUE, 70, '高等级警情自动通知上级领导');

-- ============================================================
-- 9. 更新警情表,添加IM群组ID字段(可选)
-- ============================================================
ALTER TABLE `t_oa_police_report`
ADD COLUMN `im_group_id` VARCHAR(64) COMMENT 'IM群组ID' AFTER `remark`,
ADD INDEX `idx_im_group_id` (`im_group_id`);

-- ============================================================
-- 版本记录
-- ============================================================
-- v3.28.0: 2025-10-09
-- - 新增OpenIM即时聊天集成功能
-- - 支持用户自动同步
-- - 支持警情群组自动创建
-- - 支持基于规则的自动邀请
-- - 完整的操作日志和审计功能
-- ============================================================
