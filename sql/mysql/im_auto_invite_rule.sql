-- =============================================
-- IM自动拉人规则表
-- 定义自动邀请成员加入群组的规则
-- =============================================

CREATE TABLE IF NOT EXISTS t_im_auto_invite_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  rule_name VARCHAR(64) NOT NULL COMMENT '规则名称',
  rule_type VARCHAR(32) NOT NULL COMMENT '规则类型: CREATOR=创建人, OWNER=负责人, DEPARTMENT=部门, ROLE=角色',
  business_type VARCHAR(32) NOT NULL COMMENT '业务类型: POLICE=警情',
  condition_config TEXT COMMENT '条件配置(JSON格式)',
  enabled TINYINT DEFAULT 1 COMMENT '是否启用: 1=启用, 0=禁用',
  priority INT DEFAULT 0 COMMENT '优先级 (数字越小优先级越高)',
  remark VARCHAR(255) COMMENT '规则说明',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted_flag TINYINT DEFAULT 0 COMMENT '删除标志: 0=未删除, 1=已删除',

  KEY idx_business_type (business_type),
  KEY idx_rule_type (rule_type),
  KEY idx_enabled (enabled),
  KEY idx_priority (priority),
  KEY idx_deleted_flag (deleted_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IM自动拉人规则表';

-- 插入默认规则
INSERT INTO t_im_auto_invite_rule (rule_name, rule_type, business_type, condition_config, enabled, priority, remark, create_time)
VALUES
  (
    '创建人自动加入',
    'CREATOR',
    'POLICE',
    '{"role": "OWNER", "description": "警情创建人自动成为群主"}',
    1,
    1,
    '警情创建时,创建人自动加入群组并成为群主',
    NOW()
  ),
  (
    '负责人自动加入',
    'OWNER',
    'POLICE',
    '{"role": "ADMIN", "description": "警情负责人自动成为管理员"}',
    1,
    2,
    '分配负责人时,负责人自动加入群组并成为管理员',
    NOW()
  ),
  (
    '部门成员自动加入',
    'DEPARTMENT',
    'POLICE',
    '{"includeManager": true, "description": "包含部门主管"}',
    0,
    3,
    '警情所属部门的成员自动加入群组 (默认禁用)',
    NOW()
  ),
  (
    '角色成员自动加入',
    'ROLE',
    'POLICE',
    '{"roles": ["消防队员", "应急指挥"], "description": "特定角色成员自动加入"}',
    0,
    4,
    '特定角色的成员自动加入群组 (默认禁用,需配置角色列表)',
    NOW()
  );

-- 创建索引
CREATE INDEX idx_create_time ON t_im_auto_invite_rule(create_time);
