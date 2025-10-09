-- =============================================
-- IM群组映射表
-- 用于OpenIM群组与业务实体(如警情)的映射关系
-- =============================================

CREATE TABLE IF NOT EXISTS t_im_group_mapping (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  group_id VARCHAR(64) NOT NULL COMMENT 'OpenIM群组ID',
  business_type VARCHAR(32) NOT NULL COMMENT '业务类型: POLICE=警情群',
  business_id BIGINT NOT NULL COMMENT '业务ID (如警情ID)',
  group_name VARCHAR(128) COMMENT '群组名称',
  owner_user_id VARCHAR(64) COMMENT '群主OpenIM用户ID',
  owner_employee_id BIGINT COMMENT '群主员工ID',
  member_count INT DEFAULT 0 COMMENT '成员数量',
  auto_invite_rule TEXT COMMENT '自动拉人规则配置(JSON格式)',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted_flag TINYINT DEFAULT 0 COMMENT '删除标志: 0=未删除, 1=已删除',

  UNIQUE KEY uk_group_id (group_id),
  UNIQUE KEY uk_business (business_type, business_id),
  KEY idx_owner_employee_id (owner_employee_id),
  KEY idx_business_type (business_type),
  KEY idx_business_id (business_id),
  KEY idx_deleted_flag (deleted_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM群组映射表';

-- 创建索引
CREATE INDEX idx_create_time ON t_im_group_mapping(create_time);
CREATE INDEX idx_group_name ON t_im_group_mapping(group_name);
