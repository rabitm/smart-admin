-- ========================================
-- 修复 OpenIM 群组同步问题
-- 问题: 数据库中有群组映射，但 OpenIM 服务器上没有实际群组
-- 解决: 删除旧映射，让系统重新创建
-- ========================================

-- 1. 查看当前的群组映射
SELECT
    id,
    report_id,
    openim_group_id,
    group_name,
    group_status,
    create_time
FROM t_im_group_mapping
WHERE report_id = 5;

-- 2. 查看相关的群成员记录
SELECT
    gm.*
FROM t_im_group_member gm
INNER JOIN t_im_group_mapping gmap ON gm.group_mapping_id = gmap.id
WHERE gmap.report_id = 5;

-- 3. 删除群成员记录
DELETE FROM t_im_group_member
WHERE group_mapping_id IN (
    SELECT id FROM t_im_group_mapping WHERE report_id = 5
);

-- 4. 删除群组映射记录
DELETE FROM t_im_group_mapping
WHERE report_id = 5;

-- 5. 验证删除成功
SELECT COUNT(*) as remaining_mappings
FROM t_im_group_mapping
WHERE report_id = 5;

-- 预期结果: remaining_mappings = 0

-- ========================================
-- 执行完成后:
-- 1. 刷新浏览器页面
-- 2. 系统会自动调用 createGroupForReport API
-- 3. 后端会在 OpenIM 服务器上创建真实的群组
-- 4. 聊天功能应该可以正常工作
-- ========================================
