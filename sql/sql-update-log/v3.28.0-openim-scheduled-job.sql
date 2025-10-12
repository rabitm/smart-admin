-- ======================================================
-- OpenIM定时任务配置
-- 版本: v3.28.0
-- 日期: 2025-10-11
-- 说明: 将OpenIM用户同步任务迁移到SmartJob定时任务模块
-- ======================================================

-- ----------------------------
-- 添加 OpenIM用户增量同步 定时任务
-- ----------------------------
INSERT INTO `t_smart_job` (`job_name`, `job_class`, `trigger_type`, `trigger_value`, `enabled_flag`, `param`, `sort`, `remark`, `deleted_flag`, `update_name`, `create_time`, `update_time`)
VALUES
(
    'OpenIM用户增量同步',
    'net.lab1024.sa.admin.module.support.im.job.IMUserIncrementalSyncJob',
    'cron',
    '0 0 * * * ?',
    1,
    NULL,
    10,
    '定时增量同步SmartAdmin员工到OpenIM系统。默认每小时执行一次。可通过修改trigger_value调整执行频率。
示例cron表达式:
- 每小时: 0 0 * * * ?
- 每30分钟: 0 0/30 * * * ?
- 每15分钟: 0 0/15 * * * ?
- 每天凌晨2点: 0 0 2 * * ?',
    0,
    '系统',
    NOW(),
    NOW()
);

-- ----------------------------
-- 添加 OpenIM用户同步每日报告 定时任务
-- ----------------------------
INSERT INTO `t_smart_job` (`job_name`, `job_class`, `trigger_type`, `trigger_value`, `enabled_flag`, `param`, `sort`, `remark`, `deleted_flag`, `update_name`, `create_time`, `update_time`)
VALUES
(
    'OpenIM用户同步每日报告',
    'net.lab1024.sa.admin.module.support.im.job.IMUserDailySyncReportJob',
    'cron',
    '0 0 2 * * ?',
    1,
    NULL,
    11,
    '每日凌晨2点打印OpenIM用户同步统计报告。详细统计数据可查看定时任务执行日志。',
    0,
    '系统',
    NOW(),
    NOW()
);

-- ======================================================
-- 说明:
-- 1. 增量同步任务默认每小时执行一次 (0 0 * * * ?)
-- 2. 每日报告任务默认每天凌晨2点执行 (0 0 2 * * ?)
-- 3. 可在系统管理 -> 定时任务 界面中修改执行频率和启用状态
-- 4. 任务执行日志可在 定时任务 -> 执行记录 中查看
-- 5. 如需手动执行,可在定时任务列表中点击"执行"按钮
-- ======================================================
