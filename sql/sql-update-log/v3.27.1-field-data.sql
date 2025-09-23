-- 专业字段数据存储表
-- @Author: Claude Code Assistant
-- @Date: 2025-09-23

-- 警情报告专业字段数据表 - 存储实际录入的专业字段数据
CREATE TABLE `t_police_report_field_data` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `report_id` bigint(20) NOT NULL COMMENT '警情报告ID',
  `field_key` varchar(100) NOT NULL COMMENT '字段标识',
  `field_value` text COMMENT '字段值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_field_key` (`field_key`),
  UNIQUE KEY `uk_report_field` (`report_id`, `field_key`),
  CONSTRAINT `fk_field_data_report` FOREIGN KEY (`report_id`) REFERENCES `t_police_report` (`report_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='警情报告专业字段数据';