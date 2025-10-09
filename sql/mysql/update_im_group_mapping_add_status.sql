-- ==============================================
-- OpenIM群组映射表 - 添加状态字段
-- 作者: Claude Code Assistant
-- 日期: 2025-10-09
-- 用途: 支持群组生命周期状态管理
-- ==============================================

-- 添加status字段
ALTER TABLE `t_im_group_mapping`
ADD COLUMN `status` INT(2) DEFAULT 3 COMMENT '群组状态: 0=创建中, 1=同步中, 2=验证中, 3=正常, 4=异常, 5=修复中, 9=已删除'
AFTER `member_count`;

-- 为现有记录设置默认状态为正常(3)
UPDATE `t_im_group_mapping`
SET `status` = 3
WHERE `status` IS NULL AND `deleted_flag` = 0;

-- 添加状态字段索引以提升查询性能
CREATE INDEX `idx_status` ON `t_im_group_mapping`(`status`);

-- 添加组合索引用于业务查询优化
CREATE INDEX `idx_business_status` ON `t_im_group_mapping`(`business_type`, `business_id`, `status`);

-- 验证修改
SELECT
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM
    information_schema.COLUMNS
WHERE
    TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 't_im_group_mapping'
    AND COLUMN_NAME = 'status';

-- 统计各状态的群组数量
SELECT
    status,
    CASE status
        WHEN 0 THEN '创建中'
        WHEN 1 THEN '同步中'
        WHEN 2 THEN '验证中'
        WHEN 3 THEN '正常'
        WHEN 4 THEN '异常'
        WHEN 5 THEN '修复中'
        WHEN 9 THEN '已删除'
        ELSE '未知'
    END as status_name,
    COUNT(*) as count
FROM
    t_im_group_mapping
WHERE
    deleted_flag = 0
GROUP BY
    status
ORDER BY
    status;
