-- 座位管理系统数据库脚本
-- Author: Claude Code Assistant
-- Date: 2025-09-25
-- Version: v3.27.3

-- 创建座位表
CREATE TABLE `t_seat` (
  `seat_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '座位ID',
  `seat_number` varchar(50) NOT NULL COMMENT '座位编号',
  `seat_name` varchar(100) NOT NULL COMMENT '座位名称',
  `seat_type` tinyint(4) NOT NULL DEFAULT '1' COMMENT '座位类型：1-普通座位 2-VIP座位 3-无障碍座位',
  `seat_status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '座位状态：1-空闲 2-使用中 3-预约中 4-维护中',
  `area_id` bigint(20) DEFAULT NULL COMMENT '区域ID',
  `area_name` varchar(100) DEFAULT NULL COMMENT '区域名称',
  `floor` int(11) DEFAULT NULL COMMENT '楼层',
  `location_desc` varchar(255) DEFAULT NULL COMMENT '座位位置描述',
  `current_user_id` bigint(20) DEFAULT NULL COMMENT '当前使用者ID',
  `current_user_name` varchar(100) DEFAULT NULL COMMENT '当前使用者姓名',
  `reserved_user_id` bigint(20) DEFAULT NULL COMMENT '预约者ID',
  `reserved_user_name` varchar(100) DEFAULT NULL COMMENT '预约者姓名',
  `reserve_start_time` datetime DEFAULT NULL COMMENT '预约开始时间',
  `reserve_end_time` datetime DEFAULT NULL COMMENT '预约结束时间',
  `use_start_time` datetime DEFAULT NULL COMMENT '使用开始时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `deleted_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `create_user_id` bigint(20) DEFAULT NULL COMMENT '创建人ID',
  `create_user_name` varchar(100) DEFAULT NULL COMMENT '创建人姓名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user_id` bigint(20) DEFAULT NULL COMMENT '更新人ID',
  `update_user_name` varchar(100) DEFAULT NULL COMMENT '更新人姓名',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`seat_id`),
  UNIQUE KEY `uk_seat_number` (`seat_number`),
  KEY `idx_seat_type` (`seat_type`),
  KEY `idx_seat_status` (`seat_status`),
  KEY `idx_area_id` (`area_id`),
  KEY `idx_floor` (`floor`),
  KEY `idx_current_user_id` (`current_user_id`),
  KEY `idx_reserved_user_id` (`reserved_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='座位管理表';

-- 插入示例数据
INSERT INTO `t_seat` (`seat_number`, `seat_name`, `seat_type`, `seat_status`, `area_id`, `area_name`, `floor`, `location_desc`, `remark`) VALUES
('A001', 'A区1号座位', 1, 1, 1, 'A区阅读区', 1, '靠窗位置，采光良好', '普通座位'),
('A002', 'A区2号座位', 1, 1, 1, 'A区阅读区', 1, '靠近书架', '普通座位'),
('A003', 'A区3号座位', 2, 1, 1, 'A区阅读区', 1, '独立隔间，安静舒适', 'VIP座位'),
('B001', 'B区1号座位', 1, 1, 2, 'B区自习区', 2, '开放式座位', '普通座位'),
('B002', 'B区2号座位', 3, 1, 2, 'B区自习区', 2, '无障碍设计，方便轮椅使用', '无障碍座位'),
('C001', 'C区1号座位', 1, 2, 3, 'C区讨论区', 1, '适合小组讨论', '使用中'),
('C002', 'C区2号座位', 1, 3, 3, 'C区讨论区', 1, '预约座位', '预约中'),
('D001', 'D区1号座位', 1, 4, 4, 'D区休息区', 3, '设备维护中', '维护中');

-- 座位使用记录表（可选，用于记录历史使用情况）
CREATE TABLE `t_seat_usage_log` (
  `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `seat_id` bigint(20) NOT NULL COMMENT '座位ID',
  `seat_number` varchar(50) NOT NULL COMMENT '座位编号',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `user_name` varchar(100) NOT NULL COMMENT '用户姓名',
  `action_type` tinyint(4) NOT NULL COMMENT '操作类型：1-预约 2-使用 3-释放 4-取消预约',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` int(11) DEFAULT NULL COMMENT '使用时长（分钟）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_seat_id` (`seat_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_action_type` (`action_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='座位使用记录表';