-- 优化专业字段查询的存储结构
-- @Author: Claude Code Assistant
-- @Date: 2025-09-23

-- 方案1: 添加常用查询字段到主表（推荐）
-- 将经常用于查询的数值字段直接添加到主表中
ALTER TABLE `t_police_report`
ADD COLUMN `trapped_count` int DEFAULT NULL COMMENT '被困人数',
ADD COLUMN `casualties_count` int DEFAULT NULL COMMENT '伤亡人数',
ADD COLUMN `vehicle_count` int DEFAULT NULL COMMENT '车辆数量',
ADD COLUMN `affected_area_size` decimal(10,2) DEFAULT NULL COMMENT '影响面积(平方米)',
ADD COLUMN `danger_level` tinyint DEFAULT NULL COMMENT '危险等级(1-紧急,2-高,3-中,4-低)',
ADD COLUMN `fire_floor` varchar(50) DEFAULT NULL COMMENT '火灾楼层',
ADD COLUMN `fire_scale` tinyint DEFAULT NULL COMMENT '火势规模',
ADD COLUMN `rescue_type` varchar(100) DEFAULT NULL COMMENT '救援类型',
ADD COLUMN `accident_type` varchar(100) DEFAULT NULL COMMENT '事故类型';

-- 为查询字段添加索引
ALTER TABLE `t_police_report`
ADD INDEX `idx_trapped_count` (`trapped_count`),
ADD INDEX `idx_casualties_count` (`casualties_count`),
ADD INDEX `idx_danger_level` (`danger_level`),
ADD INDEX `idx_report_type_trapped` (`report_type`, `trapped_count`),
ADD INDEX `idx_report_type_casualties` (`report_type`, `casualties_count`);

-- 方案2: 创建专业字段查询视图
-- 为常用查询创建物化视图（如果数据库支持）
CREATE VIEW `v_police_report_queryable` AS
SELECT
    pr.report_id,
    pr.report_number,
    pr.report_type,
    pr.report_level,
    pr.reporter_name,
    pr.reporter_phone,
    pr.incident_location,
    pr.report_time,
    pr.status,
    pr.handler_name,
    -- 提取数值字段
    CAST(NULLIF(fd_trapped.field_value, '') AS UNSIGNED) as trapped_count,
    CAST(NULLIF(fd_casualties.field_value, '') AS UNSIGNED) as casualties_count,
    CAST(NULLIF(fd_vehicle.field_value, '') AS UNSIGNED) as vehicle_count,
    fd_danger.field_value as danger_level,
    fd_fire_floor.field_value as fire_floor,
    fd_fire_scale.field_value as fire_scale,
    fd_rescue_type.field_value as rescue_type,
    fd_accident_type.field_value as accident_type
FROM t_police_report pr
LEFT JOIN t_police_report_field_data fd_trapped
    ON pr.report_id = fd_trapped.report_id AND fd_trapped.field_key = 'trappedCount'
LEFT JOIN t_police_report_field_data fd_casualties
    ON pr.report_id = fd_casualties.report_id AND fd_casualties.field_key = 'casualties'
LEFT JOIN t_police_report_field_data fd_vehicle
    ON pr.report_id = fd_vehicle.report_id AND fd_vehicle.field_key = 'vehicleCount'
LEFT JOIN t_police_report_field_data fd_danger
    ON pr.report_id = fd_danger.report_id AND fd_danger.field_key = 'dangerLevel'
LEFT JOIN t_police_report_field_data fd_fire_floor
    ON pr.report_id = fd_fire_floor.report_id AND fd_fire_floor.field_key = 'fireFloor'
LEFT JOIN t_police_report_field_data fd_fire_scale
    ON pr.report_id = fd_fire_scale.report_id AND fd_fire_scale.field_key = 'fireScale'
LEFT JOIN t_police_report_field_data fd_rescue_type
    ON pr.report_id = fd_rescue_type.report_id AND fd_rescue_type.field_key = 'rescueType'
LEFT JOIN t_police_report_field_data fd_accident_type
    ON pr.report_id = fd_accident_type.report_id AND fd_accident_type.field_key = 'accidentType'
WHERE pr.deleted_flag = 0;

-- 示例查询SQL:
-- 查询被困人数大于5人的火灾或抢险救援
/*
-- 使用主表字段查询(方案1)
SELECT * FROM t_police_report
WHERE report_type IN (1, 2) -- 火灾、抢险救援
  AND trapped_count > 5
  AND deleted_flag = 0;

-- 使用视图查询(方案2)
SELECT * FROM v_police_report_queryable
WHERE report_type IN (1, 2)
  AND trapped_count > 5;

-- 复杂统计查询示例
SELECT
    report_type,
    COUNT(*) as total_cases,
    SUM(trapped_count) as total_trapped,
    AVG(trapped_count) as avg_trapped,
    MAX(trapped_count) as max_trapped
FROM v_police_report_queryable
WHERE trapped_count IS NOT NULL
GROUP BY report_type;
*/