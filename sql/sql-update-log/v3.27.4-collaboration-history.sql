-- 协作历史记录表创建脚本
-- v3.27.4 新增协作历史记录功能
-- Author: Claude Code Assistant
-- Date: 2025-09-25

-- 创建警情操作记录表
CREATE TABLE IF NOT EXISTS `t_police_report_operation_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id` bigint(20) NOT NULL COMMENT '警情ID',
  `user_id` bigint(20) NOT NULL COMMENT '操作用户ID',
  `user_name` varchar(50) NOT NULL COMMENT '操作用户名称',
  `operation_type` varchar(50) NOT NULL COMMENT '操作类型（FIELD_UPDATE、FIELD_DELETE、STATUS_CHANGE、CREATE、DELETE等）',
  `field_name` varchar(100) DEFAULT NULL COMMENT '字段名称',
  `field_label` varchar(100) DEFAULT NULL COMMENT '字段显示名称（中文）',
  `old_value` text DEFAULT NULL COMMENT '旧值',
  `new_value` text DEFAULT NULL COMMENT '新值',
  `description` varchar(500) DEFAULT NULL COMMENT '操作描述',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `version` int(11) DEFAULT 1 COMMENT '版本号（用于并发控制）',
  `ext_data` text DEFAULT NULL COMMENT '扩展数据（JSON格式）',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_field_name` (`field_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='警情操作记录表';

-- 创建索引优化查询性能
CREATE INDEX `idx_report_user` ON `t_police_report_operation_log` (`report_id`, `user_id`);
CREATE INDEX `idx_report_time` ON `t_police_report_operation_log` (`report_id`, `operation_time` DESC);
CREATE INDEX `idx_user_time` ON `t_police_report_operation_log` (`user_id`, `operation_time` DESC);

-- 插入一些示例数据（可选）
INSERT INTO `t_police_report_operation_log` (
    `report_id`, `user_id`, `user_name`, `operation_type`,
    `field_name`, `field_label`, `old_value`, `new_value`,
    `description`, `ip_address`, `operation_time`
) VALUES
(1, 44, '系统管理员', 'FIELD_UPDATE', 'reportType', '警情类型', '1', '2', '修改警情类型', '127.0.0.1', NOW()),
(1, 44, '系统管理员', 'FIELD_UPDATE', 'reporterName', '报警人姓名', '张三', '李四', '修改报警人姓名', '127.0.0.1', NOW()),
(2, 44, '系统管理员', 'CREATE', NULL, NULL, NULL, NULL, '创建新警情', '127.0.0.1', NOW());

-- 添加表注释
ALTER TABLE `t_police_report_operation_log` COMMENT = '警情操作记录表 - 记录所有警情相关操作，支持协作历史追踪';

-- 验证表创建
SELECT
    TABLE_NAME as '表名',
    TABLE_COMMENT as '表注释',
    TABLE_ROWS as '数据行数'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 't_police_report_operation_log';

-- 验证索引创建
SHOW INDEX FROM `t_police_report_operation_log`;