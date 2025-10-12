# OpenIM定时任务调试指南

## 快速检查清单

### 1. 确认SQL已执行

检查数据库中是否有任务配置：

```sql
SELECT * FROM t_smart_job
WHERE job_class LIKE '%IMUser%'
ORDER BY job_id DESC;
```

应该看到两条记录：
- `IMUserIncrementalSyncJob` - OpenIM用户增量同步
- `IMUserDailySyncReportJob` - OpenIM用户同步每日报告

### 2. 重启应用

**重要**：定时任务配置需要重启应用才能生效！

```bash
# 1. 停止应用
# 2. 重新编译
cd smart-admin-api-java17-springboot3
mvn clean package -DskipTests

# 3. 启动应用
```

### 3. 检查启动日志

查找以下关键日志：

```
==== SmartJob ==== start/refresh job num:X->[..., OpenIM用户增量同步, ...]
```

如果看到 `OpenIM用户增量同步` 在任务列表中，说明任务已成功加载。

### 4. 手动执行任务

1. 登录SmartAdmin后台
2. 进入 **系统管理 -> 定时任务**
3. 找到 "OpenIM用户增量同步" 任务
4. 点击 **执行** 按钮

### 5. 查看执行日志

#### 方式1：界面查看
1. 点击任务右侧的 **日志** 按钮
2. 查看最新的执行记录
3. 查看 **执行结果** 和 **是否成功** 字段

#### 方式2：数据库查看
```sql
SELECT
    log_id,
    job_name,
    success_flag,
    execute_start_time,
    execute_time_millis,
    execute_result,
    create_name
FROM t_smart_job_log
WHERE job_name LIKE '%OpenIM%'
ORDER BY log_id DESC
LIMIT 10;
```

### 6. 查看应用日志

查找以下关键日志：

```
⏰ ==================== OpenIM用户增量同步任务开始 ====================
⏰ 执行时间: 2025-10-11 10:00:00
⏰ IMUserSyncService注入状态: 已注入
⏰ 开始调用 IMUserSyncService.incrementalSync()...
✅ ==================== OpenIM用户增量同步任务完成 ====================
✅ 本次同步: 总数=X, 成功=X, 失败=X
```

---

## 常见问题排查

### 问题1：任务列表中找不到OpenIM同步任务

**原因**：
- SQL脚本未执行
- 应用未重启
- Job类路径配置错误

**排查步骤**：
1. 检查数据库中是否有任务配置（见上面SQL）
2. 确认 `job_class` 字段值为：`net.lab1024.sa.admin.module.support.im.job.IMUserIncrementalSyncJob`
3. 检查 `enabled_flag` 是否为 `1`
4. 检查 `deleted_flag` 是否为 `0`
5. 重启应用

### 问题2：点击执行没有反应

**原因**：
- 任务未启用
- Redis连接问题（任务使用Redis锁）
- 应用日志中有错误

**排查步骤**：
1. 检查任务是否启用（开关是否打开）
2. 检查Redis连接是否正常
3. 查看应用日志中是否有异常

### 问题3：执行日志显示失败

**原因**：
- IMUserSyncService依赖注入失败
- OpenIM服务连接失败
- 数据库连接异常

**排查步骤**：

#### 步骤1：查看执行结果中的错误信息

在定时任务执行日志中，查看 **执行结果** 字段的错误堆栈。

#### 步骤2：查看应用日志

查找以下关键日志：

**依赖注入失败**：
```
❌ IMUserSyncService依赖注入失败，无法执行同步任务
```
**解决方案**：检查 `IMUserSyncService` 类是否正确标注 `@Service` 注解

**OpenIM连接失败**：
```
❌ OpenIM服务连接失败
```
**解决方案**：检查 OpenIM 配置和服务状态

#### 步骤3：手动测试同步功能

使用原有的API接口测试同步功能：

```bash
# 测试增量同步
curl -X POST http://localhost:1024/api/im/user/sync/incremental
```

如果API调用成功，说明同步功能本身没问题，问题在于定时任务配置。

### 问题4：任务执行了但用户没有同步到OpenIM

**原因**：
- 没有需要同步的用户（所有用户已同步）
- 同步条件不满足
- OpenIM服务未启动

**排查步骤**：

#### 步骤1：查看执行结果

如果显示：
```
同步完成: 无新增用户需要同步
```
说明所有用户已同步。

#### 步骤2：检查用户同步状态

```sql
SELECT
    e.employee_id,
    e.actual_name,
    e.login_name,
    m.openim_user_id,
    m.sync_status,
    m.last_sync_time,
    m.error_message
FROM t_employee e
LEFT JOIN t_im_user_mapping m ON e.employee_id = m.employee_id
WHERE e.deleted_flag = 0
ORDER BY e.create_time DESC
LIMIT 20;
```

查看字段说明：
- `openim_user_id` 为空：未同步
- `sync_status = 0`：同步失败
- `sync_status = 1`：同步成功
- `error_message`：失败原因

#### 步骤3：强制重新同步

如果需要重新同步所有用户：

```sql
-- 清空同步状态（慎用！会重新同步所有用户）
TRUNCATE TABLE t_im_user_mapping;
```

然后重新执行定时任务。

#### 步骤4：检查OpenIM数据库

连接到OpenIM的MySQL数据库，检查用户表：

```sql
USE openim_db;  -- 替换为你的OpenIM数据库名

SELECT * FROM users
ORDER BY create_time DESC
LIMIT 20;
```

---

## 调试技巧

### 技巧1：临时提高执行频率

如果需要快速测试，可以临时修改Cron表达式：

1. 进入 **系统管理 -> 定时任务**
2. 编辑 "OpenIM用户增量同步" 任务
3. 修改 **触发配置** 为：`0 0/1 * * * ?`（每分钟执行一次）
4. 保存
5. 等待1分钟后查看是否自动执行
6. 测试完成后改回原值：`0 0 * * * ?`

### 技巧2：查看详细的同步日志

在应用日志中搜索以下关键字：
- `OpenIM用户增量同步`
- `IMUserSyncService`
- `同步完成`
- `同步失败`

### 技巧3：使用数据库触发器监控

创建触发器监控 `t_im_user_mapping` 表的变化：

```sql
-- 查看最近的同步记录变化
SELECT * FROM t_im_user_mapping
ORDER BY last_sync_time DESC
LIMIT 20;
```

---

## 性能监控

### 监控任务执行统计

```sql
-- 查看最近30天的执行统计
SELECT
    DATE(execute_start_time) as date,
    COUNT(*) as total_executions,
    SUM(CASE WHEN success_flag = 1 THEN 1 ELSE 0 END) as success_count,
    SUM(CASE WHEN success_flag = 0 THEN 1 ELSE 0 END) as fail_count,
    AVG(execute_time_millis) as avg_time_ms,
    MAX(execute_time_millis) as max_time_ms
FROM t_smart_job_log
WHERE job_name = 'OpenIM用户增量同步'
  AND execute_start_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY DATE(execute_start_time)
ORDER BY date DESC;
```

### 监控同步成功率

```sql
-- 查看用户同步成功率
SELECT
    COUNT(*) as total_users,
    SUM(CASE WHEN openim_user_id IS NOT NULL THEN 1 ELSE 0 END) as synced_users,
    SUM(CASE WHEN openim_user_id IS NULL THEN 1 ELSE 0 END) as not_synced_users,
    ROUND(SUM(CASE WHEN openim_user_id IS NOT NULL THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2) as sync_rate
FROM t_employee
WHERE deleted_flag = 0;
```

---

## 紧急回滚方案

如果新的定时任务出现问题，需要回滚：

### 方案1：禁用新任务

```sql
-- 禁用OpenIM同步定时任务
UPDATE t_smart_job
SET enabled_flag = 0
WHERE job_class LIKE '%IMUser%';
```

### 方案2：恢复旧的@Scheduled方式

1. 还原 `IMUserScheduledSyncService.java` 文件
2. 恢复 `IMUserController.java` 中被移除的API
3. 重新编译部署

---

## 联系支持

如果以上方法都无法解决问题，请提供以下信息：

1. **任务配置**：`t_smart_job` 表中的任务记录
2. **执行日志**：`t_smart_job_log` 表中最近的执行记录
3. **应用日志**：包含 "OpenIM" 关键字的日志片段
4. **环境信息**：
   - Java版本
   - Spring Boot版本
   - Redis版本
   - OpenIM版本
   - 数据库版本

---

**更新日期**：2025-10-11
**文档版本**：v1.0.0
