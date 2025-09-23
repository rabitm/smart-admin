-- 添加警情表单配置菜单
-- @Author: Claude Code Assistant
-- @Date: 2025-09-22

-- 查找警情管理的菜单ID
SET @police_menu_id = (SELECT menu_id FROM t_menu WHERE menu_name = '警情管理' LIMIT 1);

-- 如果没有找到警情管理菜单，先创建一个
INSERT IGNORE INTO t_menu (
    menu_name, menu_type, parent_id, sort, path, component,
    perms_type, api_perms, web_perms, icon, context_menu_id,
    frame_flag, frame_url, cache_flag, visible_flag, disabled_flag,
    deleted_flag, create_user_id, create_time, update_time
) VALUES (
    '警情管理', 2,
    (SELECT menu_id FROM (SELECT menu_id FROM t_menu WHERE menu_name = 'OA办公' LIMIT 1) as oa_menu),
    30, '/oa/police', '',
    1, '', '', 'AlertOutlined', NULL,
    0, '', 0, 1, 0,
    0, 1, NOW(), NOW()
);

-- 获取警情管理菜单ID（创建后）
SET @police_menu_id = (SELECT menu_id FROM t_menu WHERE menu_name = '警情管理' LIMIT 1);

-- 插入表单配置菜单
INSERT INTO t_menu (
    menu_name, menu_type, parent_id, sort, path, component,
    perms_type, api_perms, web_perms, icon, context_menu_id,
    frame_flag, frame_url, cache_flag, visible_flag, disabled_flag,
    deleted_flag, create_user_id, create_time, update_time
) VALUES (
    '表单配置', 3, @police_menu_id, 40, '/oa/police/form-template-list', 'business/oa/police/form-template-list',
    1, '', '', 'SettingOutlined', NULL,
    0, '', 0, 1, 0,
    0, 1, NOW(), NOW()
);

-- 获取表单配置菜单ID
SET @form_config_menu_id = LAST_INSERT_ID();

-- 为表单配置菜单分配权限给超级管理员角色
INSERT INTO t_role_menu (
    role_id, menu_id, create_time, update_time
) VALUES (
    1, @form_config_menu_id, NOW(), NOW()
);

-- 输出结果
SELECT
    CONCAT('✅ 成功添加菜单: ', menu_name, ' (ID: ', menu_id, ')') as result
FROM t_menu
WHERE menu_id = @form_config_menu_id;