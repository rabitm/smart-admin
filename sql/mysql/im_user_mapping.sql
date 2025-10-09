-- =============================================
-- IM用户映射表
-- 用于SmartAdmin用户与OpenIM用户的映射关系
-- =============================================

CREATE TABLE IF NOT EXISTS t_im_user_mapping (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  employee_id BIGINT NOT NULL COMMENT 'SmartAdmin员工ID',
  open_im_user_id VARCHAR(64) NOT NULL COMMENT 'OpenIM用户ID',
  sync_status TINYINT DEFAULT 1 COMMENT '同步状态: 1=已同步, 0=待同步',
  last_sync_time DATETIME COMMENT '最后同步时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted_flag TINYINT DEFAULT 0 COMMENT '删除标志: 0=未删除, 1=已删除',

  UNIQUE KEY uk_employee_id (employee_id),
  UNIQUE KEY uk_open_im_user_id (open_im_user_id),
  KEY idx_sync_status (sync_status),
  KEY idx_deleted_flag (deleted_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM用户映射表';

-- 创建索引
CREATE INDEX idx_create_time ON t_im_user_mapping(create_time);
CREATE INDEX idx_last_sync_time ON t_im_user_mapping(last_sync_time);
