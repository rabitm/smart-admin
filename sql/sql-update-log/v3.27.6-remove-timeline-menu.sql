-- =============================================
-- 移除独立操作时间轴菜单脚本
-- @Author: Claude Code Assistant
-- @Date: 2025-09-28
-- 理由：操作时间轴应该是针对特定警情记录的，不应该作为独立菜单存在
-- =============================================

-- 查找操作时间轴菜单ID
SET @timeline_menu_id = (SELECT menu_id FROM t_menu WHERE menu_name = '操作时间轴' LIMIT 1);

-- 检查是否找到操作时间轴菜单
SELECT CASE
    WHEN @timeline_menu_id IS NOT NULL THEN CONCAT('✅ 找到操作时间轴菜单ID: ', @timeline_menu_id)
    ELSE '⚠️  操作时间轴菜单不存在，无需移除'
END as check_result;

-- 移除角色菜单关联
DELETE FROM t_role_menu WHERE menu_id = @timeline_menu_id;

-- 移除菜单记录
DELETE FROM t_menu WHERE menu_id = @timeline_menu_id;

-- 输出结果
SELECT CASE
    WHEN @timeline_menu_id IS NULL THEN '⚠️  操作时间轴菜单不存在，无需移除'
    WHEN ROW_COUNT() > 0 THEN CONCAT('✅ 成功移除操作时间轴菜单，ID: ', @timeline_menu_id)
    ELSE '❌ 移除失败'
END as final_result;

-- 验证移除结果
SELECT
    COUNT(*) as remaining_timeline_menus,
    CASE
        WHEN COUNT(*) = 0 THEN '✅ 操作时间轴菜单已完全移除'
        ELSE '❌ 仍有残留的操作时间轴菜单'
    END as verification_result
FROM t_menu
WHERE menu_name = '操作时间轴';

-- 验证角色权限清理
SELECT
    COUNT(*) as remaining_role_permissions,
    CASE
        WHEN COUNT(*) = 0 THEN '✅ 相关角色权限已完全清理'
        ELSE '❌ 仍有残留的角色权限'
    END as permission_cleanup_result
FROM t_role_menu rm
JOIN t_menu m ON rm.menu_id = m.menu_id
WHERE m.menu_name = '操作时间轴';