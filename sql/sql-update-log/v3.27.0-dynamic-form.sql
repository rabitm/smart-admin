-- 动态表单配置系统
-- @Author: Claude Code Assistant
-- @Date: 2025-09-22

-- 1. 表单模板表 - 存储不同警情类型的表单模板
CREATE TABLE `t_police_form_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `report_type` int(11) NOT NULL COMMENT '警情类型',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `description` varchar(500) COMMENT '模板描述',
  `is_default` tinyint(1) DEFAULT 1 COMMENT '是否默认模板',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态：1启用，0禁用',
  `organization_id` bigint(20) COMMENT '所属组织ID，为空表示全局模板',
  `create_user_id` bigint(20) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user_id` bigint(20) COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_org` (`report_type`, `organization_id`),
  KEY `idx_report_type` (`report_type`),
  KEY `idx_organization` (`organization_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='警情录入表单模板';

-- 2. 表单字段表 - 存储具体的字段配置
CREATE TABLE `t_police_form_field` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `template_id` bigint(20) NOT NULL COMMENT '模板ID',
  `field_key` varchar(100) NOT NULL COMMENT '字段标识',
  `field_label` varchar(100) NOT NULL COMMENT '字段名称',
  `field_type` varchar(50) NOT NULL COMMENT '字段类型：input,select,textarea,number,checkbox,compact-group,checkbox-compact',
  `is_required` tinyint(1) DEFAULT 0 COMMENT '是否必填',
  `field_icon` varchar(20) COMMENT '字段图标',
  `placeholder` varchar(200) COMMENT '占位符提示',
  `field_options` text COMMENT '字段选项JSON',
  `quick_options` text COMMENT '快捷选项JSON',
  `parent_field_id` bigint(20) COMMENT '父字段ID，用于组合字段',
  `sort_order` int(11) DEFAULT 0 COMMENT '排序',
  `step_number` tinyint(2) DEFAULT 2 COMMENT '步骤号：2或3',
  `validation_rules` text COMMENT '验证规则JSON',
  `default_value` varchar(500) COMMENT '默认值',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态：1启用，0禁用',
  `create_user_id` bigint(20) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_user_id` bigint(20) COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_parent_field` (`parent_field_id`),
  KEY `idx_sort` (`template_id`, `step_number`, `sort_order`),
  CONSTRAINT `fk_form_field_template` FOREIGN KEY (`template_id`) REFERENCES `t_police_form_template` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='警情录入表单字段';

-- 3. 用户表单配置表 - 存储用户个性化配置
CREATE TABLE `t_police_user_form_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `report_type` int(11) NOT NULL COMMENT '警情类型',
  `template_id` bigint(20) NOT NULL COMMENT '使用的模板ID',
  `custom_config` text COMMENT '用户自定义配置JSON',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_type` (`user_id`, `report_type`),
  KEY `idx_template_id` (`template_id`),
  CONSTRAINT `fk_user_config_template` FOREIGN KEY (`template_id`) REFERENCES `t_police_form_template` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户警情表单配置';

-- 4. 初始化默认模板数据 - 火灾事故
INSERT INTO `t_police_form_template` (`report_type`, `template_name`, `description`, `is_default`, `create_user_id`)
VALUES (1, '火灾事故默认模板', '火灾事故录入的默认字段配置', 1, 1);

SET @template_id = LAST_INSERT_ID();

-- 初始化火灾事故默认字段
INSERT INTO `t_police_form_field` (`template_id`, `field_key`, `field_label`, `field_type`, `is_required`, `field_icon`, `field_options`, `quick_options`, `sort_order`, `step_number`, `create_user_id`) VALUES
(@template_id, 'peopleInfo', '人员情况', 'compact-group', 1, '👥', NULL, NULL, 1, 2, 1),
(@template_id, 'trappedCount', '被困', 'select', 0, NULL, '["0", "1-3人", "4-10人", "10+人"]', '["0", "1-3人", "4-10人", "10+人"]', 1, 2, 1),
(@template_id, 'casualties', '伤亡', 'select', 0, NULL, '["无", "1人", "2-5人", "5+人"]', '["无", "1人", "2-5人", "5+人"]', 2, 2, 1),
(@template_id, 'fireInfo', '火情详情', 'compact-group', 1, '🔥', NULL, NULL, 2, 2, 1),
(@template_id, 'fireFloor', '楼层', 'select', 0, NULL, '["1层", "2层", "3-6层", "7+层", "地下"]', NULL, 1, 2, 1),
(@template_id, 'fireScale', '火势', 'select', 0, NULL, '["初起", "发展", "猛烈", "衰减"]', NULL, 2, 2, 1),
(@template_id, 'materialSmoke', '物质与烟雾', 'compact-group', 1, '💨', NULL, NULL, 3, 2, 1),
(@template_id, 'burningMaterial', '燃烧物', 'select', 0, NULL, '["木材", "塑料", "油类", "电器", "化学品", "其他"]', NULL, 1, 2, 1),
(@template_id, 'smokeCondition', '烟雾', 'select', 0, NULL, '["无烟", "轻微", "中等", "浓烟", "有毒"]', NULL, 2, 2, 1),
(@template_id, 'additionalInfo', '补充信息', 'compact-group', 0, '📋', NULL, NULL, 1, 3, 1),
(@template_id, 'burnArea', '面积', 'input', 0, NULL, NULL, NULL, 1, 3, 1),
(@template_id, 'fireSource', '原因', 'select', 0, NULL, '["电气", "用火不慎", "吸烟", "自燃", "纵火", "不明"]', NULL, 2, 3, 1),
(@template_id, 'rescueEquipment', '所需装备', 'checkbox-compact', 0, '🚒', '["消防车", "云梯车", "救护车", "抢险车", "供水车"]', NULL, 2, 3, 1);

-- 更新父子关系
UPDATE `t_police_form_field` SET `parent_field_id` = (SELECT id FROM (SELECT id FROM `t_police_form_field` WHERE `field_key` = 'peopleInfo' AND `template_id` = @template_id) as temp) WHERE `field_key` IN ('trappedCount', 'casualties') AND `template_id` = @template_id;
UPDATE `t_police_form_field` SET `parent_field_id` = (SELECT id FROM (SELECT id FROM `t_police_form_field` WHERE `field_key` = 'fireInfo' AND `template_id` = @template_id) as temp) WHERE `field_key` IN ('fireFloor', 'fireScale') AND `template_id` = @template_id;
UPDATE `t_police_form_field` SET `parent_field_id` = (SELECT id FROM (SELECT id FROM `t_police_form_field` WHERE `field_key` = 'materialSmoke' AND `template_id` = @template_id) as temp) WHERE `field_key` IN ('burningMaterial', 'smokeCondition') AND `template_id` = @template_id;
UPDATE `t_police_form_field` SET `parent_field_id` = (SELECT id FROM (SELECT id FROM `t_police_form_field` WHERE `field_key` = 'additionalInfo' AND `template_id` = @template_id) as temp) WHERE `field_key` IN ('burnArea', 'fireSource') AND `template_id` = @template_id;