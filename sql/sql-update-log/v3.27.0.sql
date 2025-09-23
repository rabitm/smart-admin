-- --------------------------------------------------------
-- 版本更新脚本 v3.27.0
-- 新增警情录入功能
-- --------------------------------------------------------

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_oa_police_report
-- ----------------------------
DROP TABLE IF EXISTS `t_oa_police_report`;
CREATE TABLE `t_oa_police_report` (
  `report_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '警情ID',
  `report_number` varchar(50) NOT NULL COMMENT '警情编号',
  `report_type` int(11) NOT NULL COMMENT '警情类型：1-刑事案件，2-交通事故，3-治安案件，4-火灾事故，5-医疗急救，6-民事纠纷，99-其他',
  `report_level` int(11) NOT NULL COMMENT '警情等级：1-紧急，2-高，3-中，4-低',
  `reporter_name` varchar(50) NOT NULL COMMENT '报警人姓名',
  `reporter_phone` varchar(20) NOT NULL COMMENT '报警人电话',
  `reporter_id_card` varchar(18) DEFAULT NULL COMMENT '报警人身份证号',
  `report_time` datetime NOT NULL COMMENT '报警时间',
  `incident_location` varchar(500) NOT NULL COMMENT '事发地点',
  `description` text NOT NULL COMMENT '警情描述',
  `status` int(11) NOT NULL DEFAULT 1 COMMENT '处理状态：1-待处理，2-处理中，3-已完成，4-已关闭',
  `handler_id` bigint(20) DEFAULT NULL COMMENT '处理人员ID',
  `handler_name` varchar(50) DEFAULT NULL COMMENT '处理人员姓名',
  `handle_result` text DEFAULT NULL COMMENT '处理结果',
  `handle_time` datetime DEFAULT NULL COMMENT '处理完成时间',
  `attachments` varchar(1000) DEFAULT NULL COMMENT '附件信息',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `deleted_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '删除状态：0-未删除，1-已删除',
  `create_user_id` bigint(20) NOT NULL COMMENT '创建人ID',
  `create_user_name` varchar(50) NOT NULL COMMENT '创建人姓名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`report_id`) USING BTREE,
  UNIQUE KEY `uk_report_number` (`report_number`) USING BTREE,
  KEY `idx_report_type` (`report_type`) USING BTREE,
  KEY `idx_report_level` (`report_level`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_reporter_phone` (`reporter_phone`) USING BTREE,
  KEY `idx_report_time` (`report_time`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  KEY `idx_deleted_flag` (`deleted_flag`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='警情录入表' ROW_FORMAT=Dynamic;

-- ----------------------------
-- 初始化数据（示例数据）
-- ----------------------------
INSERT INTO `t_oa_police_report` VALUES
(1, 'POLICE-20250918-001', 3, 2, '张三', '13800138001', '110101199001011234', '2025-09-18 10:30:00', '朝阳区某小区门口', '发生口角纠纷，双方情绪激动', 1, NULL, NULL, NULL, NULL, NULL, '初次报警', 0, 1, '系统管理员', '2025-09-18 10:35:00', '2025-09-18 10:35:00'),
(2, 'POLICE-20250918-002', 2, 1, '李四', '13900139002', '110101199002022345', '2025-09-18 11:15:00', '西三环某路段', '两车追尾事故，有人员受伤', 2, 2, '王警官', '已到现场处理，伤者送医', '2025-09-18 12:30:00', NULL, '事故现场已清理', 0, 1, '系统管理员', '2025-09-18 11:20:00', '2025-09-18 12:30:00');

-- ----------------------------
-- 菜单和权限配置
-- ----------------------------

-- 查找当前最大的菜单ID
SET @max_menu_id = (SELECT IFNULL(MAX(menu_id), 0) FROM t_menu);

-- 添加OA办公主菜单（如果不存在）
INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `context_menu_id`, `frame_flag`, `frame_url`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_user_id`, `update_time`)
SELECT @max_menu_id + 1, 'OA办公', 1, 0, 4, '/oa', NULL, NULL, NULL, NULL, 'PartitionOutlined', NULL, 0, NULL, 0, 1, 0, 0, 1, NOW(), 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_menu WHERE menu_name = 'OA办公' AND parent_id = 0);

-- 更新最大菜单ID
SET @oa_menu_id = (SELECT menu_id FROM t_menu WHERE menu_name = 'OA办公' AND parent_id = 0);
SET @max_menu_id = (SELECT IFNULL(MAX(menu_id), @max_menu_id) FROM t_menu);

-- 添加警情管理目录菜单
INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `context_menu_id`, `frame_flag`, `frame_url`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_user_id`, `update_time`)
VALUES (@max_menu_id + 1, '警情管理', 1, @oa_menu_id, 1, '/oa/police', NULL, NULL, NULL, NULL, 'AlertOutlined', NULL, 0, NULL, 0, 1, 0, 0, 1, NOW(), 1, NOW());

SET @police_menu_id = @max_menu_id + 1;
SET @max_menu_id = @max_menu_id + 1;

-- 添加警情录入页面菜单
INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `context_menu_id`, `frame_flag`, `frame_url`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_user_id`, `update_time`)
VALUES (@max_menu_id + 1, '警情录入', 2, @police_menu_id, 1, '/oa/police/report-list', '/business/oa/police/police-report-list.vue', NULL, NULL, NULL, 'FileTextOutlined', NULL, 0, NULL, 1, 1, 0, 0, 1, NOW(), 1, NOW());

SET @police_report_menu_id = @max_menu_id + 1;
SET @max_menu_id = @max_menu_id + 1;

-- 添加权限功能点
-- 查询权限
INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `context_menu_id`, `frame_flag`, `frame_url`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_user_id`, `update_time`)
VALUES (@max_menu_id + 1, '查询', 3, @police_report_menu_id, 1, NULL, NULL, 1, 'oa:police:query', 'oa:police:query', NULL, @police_report_menu_id, 0, NULL, 0, 1, 0, 0, 1, NOW(), 1, NOW());

-- 新增权限
INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `context_menu_id`, `frame_flag`, `frame_url`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_user_id`, `update_time`)
VALUES (@max_menu_id + 2, '新增', 3, @police_report_menu_id, 2, NULL, NULL, 1, 'oa:police:add', 'oa:police:add', NULL, @police_report_menu_id, 0, NULL, 0, 1, 0, 0, 1, NOW(), 1, NOW());

-- 编辑权限
INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `context_menu_id`, `frame_flag`, `frame_url`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_user_id`, `update_time`)
VALUES (@max_menu_id + 3, '编辑', 3, @police_report_menu_id, 3, NULL, NULL, 1, 'oa:police:update', 'oa:police:update', NULL, @police_report_menu_id, 0, NULL, 0, 1, 0, 0, 1, NOW(), 1, NOW());

-- 删除权限
INSERT INTO `t_menu` (`menu_id`, `menu_name`, `menu_type`, `parent_id`, `sort`, `path`, `component`, `perms_type`, `api_perms`, `web_perms`, `icon`, `context_menu_id`, `frame_flag`, `frame_url`, `cache_flag`, `visible_flag`, `disabled_flag`, `deleted_flag`, `create_user_id`, `create_time`, `update_user_id`, `update_time`)
VALUES (@max_menu_id + 4, '删除', 3, @police_report_menu_id, 4, NULL, NULL, 1, 'oa:police:delete', 'oa:police:delete', NULL, @police_report_menu_id, 0, NULL, 0, 1, 0, 0, 1, NOW(), 1, NOW());

-- ----------------------------
-- 为管理员角色分配权限（假设管理员角色ID为1）
-- ----------------------------

-- 获取新增的菜单ID范围
SET @start_menu_id = @oa_menu_id;
SET @end_menu_id = @max_menu_id + 4;

-- 为管理员角色（通常ID为1）分配所有新增的菜单权限
INSERT INTO `t_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`)
SELECT 1, menu_id, NOW(), NOW()
FROM `t_menu`
WHERE menu_id BETWEEN @start_menu_id AND @end_menu_id
AND NOT EXISTS (
    SELECT 1 FROM `t_role_menu`
    WHERE role_id = 1 AND menu_id = `t_menu`.menu_id
);

SET FOREIGN_KEY_CHECKS = 1;