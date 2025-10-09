# OpenIM集成 - 生产级重构快速入门指南

**版本**: v3.28.0
**更新时间**: 2025-10-09

---

## 🚀 快速开始（5分钟部署）

### 步骤1: 数据库迁移（1分钟）

```bash
# 执行迁移脚本添加status字段
mysql -u root -p smart_admin_v3 < sql/mysql/update_im_group_mapping_add_status.sql

# 验证迁移成功
mysql -u root -p smart_admin_v3 -e "
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'smart_admin_v3'
AND TABLE_NAME = 't_im_group_mapping'
AND COLUMN_NAME = 'status';"
```

### 步骤2: 后端编译部署（2分钟）

```bash
cd smart-admin-api-java17-springboot3

# 编译
mvn clean compile

# 打包（跳过测试加速）
mvn clean package -DskipTests

# 启动
java -jar sa-admin/target/sa-admin.jar
```

### 步骤3: 验证部署（2分钟）

#### 3.1 检查健康状态

```bash
# 获取OpenIM健康状态
curl http://localhost:1024/api/im/health/status

# 期望输出:
{
  "code": 1,
  "msg": "成功",
  "data": {
    "healthy": true,
    "circuitBreakerOpen": false,
    "consecutiveFailures": 0,
    "lastCheckTime": "2025-10-09T10:30:00",
    "lastSuccessTime": "2025-10-09T10:30:00",
    "openimApiUrl": "http://localhost:10002"
  },
  "ok": true
}
```

#### 3.2 检查服务日志

查找以下关键日志确认服务正常：

```bash
tail -f logs/sa-admin.log | grep -E "健康检查|群组生命周期"
```

期望看到：
```
🏥 [健康检查] OpenIM健康检查服务已启动
✅ [健康检查] OpenIM服务健康
```

#### 3.3 测试群组创建

1. 登录系统：http://localhost:8081
2. 创建一个新警情
3. 观察日志应显示完整的群组创建流程：

```
🔨 [群组生命周期] 开始创建新群组 - BusinessType: POLICE, BusinessID: xxx
⏳ [群组生命周期] 群组创建成功，等待OpenIM同步 - GroupID: xxx
🔍 [群组生命周期] 验证群组成功 - GroupID: xxx
✅ [群组生命周期] 群组创建并激活成功 - GroupID: xxx
📦 [群组生命周期] 从缓存获取群组 - BusinessType: POLICE, BusinessID: xxx
```

---

## 📊 核心功能说明

### 1. 群组生命周期状态

| 状态值 | 状态名称 | 说明 | 是否可用 |
|-------|---------|------|---------|
| 0 | 创建中 | 群组正在OpenIM服务器创建 | ❌ |
| 1 | 同步中 | 等待OpenIM服务器数据同步完成 | ❌ |
| 2 | 验证中 | 正在验证群组是否在OpenIM中存在 | ❌ |
| 3 | 正常 | 群组已创建并验证成功，可正常使用 | ✅ |
| 4 | 异常 | 群组状态异常，需要修复 | ❌ |
| 5 | 修复中 | 正在自动修复群组 | ❌ |
| 9 | 已删除 | 群组已删除 | ❌ |

### 2. 健康检查机制

- **检查频率**: 每30秒自动执行
- **熔断阈值**: 连续失败3次开启熔断
- **熔断时长**: 30秒后自动尝试恢复
- **状态缓存**: Redis缓存60秒

### 3. 群组缓存策略

- **缓存时长**: 30分钟
- **缓存键格式**: `openim:group:POLICE:{reportId}`
- **自动更新**: 群组状态变更时自动更新
- **自动清除**: 群组修复时清除旧缓存

### 4. 重试机制

- **创建重试**: 最多3次，递增延迟（2s, 4s, 6s）
- **验证重试**: 最多3次，固定延迟（2s）
- **同步等待**: OpenIM同步3秒后验证

---

## 🔍 问题排查指南

### 问题1: 群组创建失败

**症状**:
```
❌ [群组生命周期] 创建群组失败，已重试3次
```

**排查步骤**:
1. 检查OpenIM服务是否运行：
   ```bash
   curl http://localhost:10002/auth/get_admin_token
   ```

2. 检查健康状态：
   ```bash
   curl http://localhost:1024/api/im/health/status
   ```

3. 检查熔断器状态：
   ```bash
   curl http://localhost:1024/api/im/health/circuit-breaker
   ```

**解决方案**:
- 如果OpenIM未运行，启动OpenIM服务
- 如果熔断器开启，等待30秒自动恢复或重启应用

### 问题2: 刷新后群组仍显示不存在

**症状**:
```
⚠️ [警情群组] 群组状态异常: 异常 - 警情ID: xxx
```

**排查步骤**:
1. 查看群组状态：
   ```sql
   SELECT * FROM t_im_group_mapping WHERE business_id = {reportId};
   ```

2. 检查是否为ERROR状态(status=4)

**解决方案**:
- 状态为ERROR会自动触发修复
- 如果自动修复失败，调用重建API：
  ```bash
  curl -X POST http://localhost:1024/api/im/group/police/rebuild/{reportId}
  ```

### 问题3: Redis缓存未命中

**症状**:
```
# 频繁看到创建日志而非缓存命中日志
```

**排查步骤**:
1. 检查Redis连接：
   ```bash
   redis-cli ping
   ```

2. 检查缓存键：
   ```bash
   redis-cli
   > KEYS openim:group:*
   ```

**解决方案**:
- 确保Redis服务正常运行
- 检查Redis配置（sa-base.yaml）
- 重启应用刷新连接池

### 问题4: OpenIM服务频繁超时

**症状**:
```
⚡ [健康检查] OpenIM服务熔断器已开启，30秒后尝试恢复
```

**排查步骤**:
1. 检查OpenIM服务器负载
2. 检查网络延迟

**解决方案**:
- 增加健康检查超时时间（修改源码）
- 优化OpenIM服务器配置
- 考虑部署在同一内网减少延迟

---

## 📈 性能监控

### 关键指标监控

1. **群组创建成功率**
   ```sql
   SELECT
       COUNT(CASE WHEN status = 3 THEN 1 END) * 100.0 / COUNT(*) as success_rate
   FROM t_im_group_mapping
   WHERE create_time > DATE_SUB(NOW(), INTERVAL 24 HOUR);
   ```

2. **缓存命中率**
   ```bash
   # Redis监控命中率
   redis-cli INFO stats | grep keyspace
   ```

3. **平均创建时间**
   ```sql
   SELECT
       AVG(TIMESTAMPDIFF(SECOND, create_time, update_time)) as avg_seconds
   FROM t_im_group_mapping
   WHERE status = 3
   AND update_time > DATE_SUB(NOW(), INTERVAL 24 HOUR);
   ```

4. **异常群组统计**
   ```sql
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
       END as status_name,
       COUNT(*) as count
   FROM t_im_group_mapping
   WHERE deleted_flag = 0
   GROUP BY status
   ORDER BY status;
   ```

---

## 🔧 配置调优

### 调整健康检查频率

修改 `OpenIMHealthCheckService.java`:

```java
// 从30秒改为60秒
@Scheduled(fixedDelay = 60000, initialDelay = 10000)
public void scheduledHealthCheck() {
    checkHealth();
}
```

### 调整熔断阈值

修改 `OpenIMHealthCheckService.java`:

```java
// 从3次改为5次
private static final int MAX_CONSECUTIVE_FAILURES = 5;

// 从30秒改为60秒
private static final long CIRCUIT_BREAKER_TIMEOUT_MS = 60000;
```

### 调整缓存过期时间

修改 `ImGroupLifecycleService.java`:

```java
// 从30分钟改为60分钟
private static final long GROUP_CACHE_EXPIRE_MINUTES = 60;
```

### 调整重试次数

修改 `ImGroupLifecycleService.java`:

```java
// 从3次改为5次
private static final int MAX_RETRY_ATTEMPTS = 5;

// 从2秒改为3秒
private static final long RETRY_DELAY_MS = 3000;
```

### 调整OpenIM同步等待时间

修改 `ImGroupLifecycleService.java`:

```java
// 从3秒改为5秒
private static final long OPENIM_SYNC_WAIT_MS = 5000;
```

---

## 🔐 生产环境检查清单

### 部署前检查

- [ ] 数据库迁移脚本已执行
- [ ] Redis服务正常运行
- [ ] OpenIM服务正常运行（端口10002可访问）
- [ ] 配置文件已更新（sa-base.yaml）
- [ ] 编译成功无错误
- [ ] 健康检查端点正常响应

### 部署后验证

- [ ] 健康检查服务正常工作
- [ ] 熔断器机制正常触发
- [ ] 群组创建成功率 > 99%
- [ ] 缓存命中率 > 80%
- [ ] 平均响应时间 < 2秒
- [ ] 异常群组自动修复成功

### 监控告警设置

1. **健康检查失败告警**
   ```bash
   # 连续3次健康检查失败时告警
   if [ consecutiveFailures >= 3 ]; then
       send_alert "OpenIM服务异常"
   fi
   ```

2. **群组创建失败率告警**
   ```sql
   -- 创建失败率 > 5% 时告警
   SELECT
       COUNT(CASE WHEN status != 3 THEN 1 END) * 100.0 / COUNT(*) as failure_rate
   FROM t_im_group_mapping
   WHERE create_time > DATE_SUB(NOW(), INTERVAL 1 HOUR)
   HAVING failure_rate > 5;
   ```

3. **熔断器开启告警**
   ```bash
   # 熔断器开启时立即告警
   curl http://localhost:1024/api/im/health/circuit-breaker | jq '.data.circuitBreakerOpen'
   ```

---

## 📚 API快速参考

### 健康检查API

```bash
# 获取健康状态
GET /api/im/health/status

# 立即执行健康检查
GET /api/im/health/check

# 获取熔断器状态
GET /api/im/health/circuit-breaker
```

### 群组管理API

```bash
# 获取警情群组（自动邀请当前用户）
GET /api/im/group/police/{reportId}

# 手动创建警情群组
POST /api/im/group/police/create/{reportId}

# 重建警情群组
POST /api/im/group/police/rebuild/{reportId}

# 邀请成员加入群组
POST /api/im/group/police/invite
{
  "reportId": 123,
  "employeeIds": [1, 2, 3]
}
```

---

## 🆘 紧急恢复步骤

### 场景1: 所有群组异常

```bash
# 1. 停止应用
kill -9 {pid}

# 2. 清理Redis缓存
redis-cli FLUSHDB

# 3. 重置群组状态
mysql -u root -p smart_admin_v3 -e "
UPDATE t_im_group_mapping
SET status = 4
WHERE deleted_flag = 0;"

# 4. 启动应用
java -jar sa-admin/target/sa-admin.jar

# 5. 批量重建群组（调用重建API）
```

### 场景2: OpenIM服务完全不可用

```bash
# 1. 开启熔断器（自动）
# 系统会自动检测并开启熔断器

# 2. 检查OpenIM服务
docker ps | grep openim
docker logs openim-server

# 3. 重启OpenIM服务
docker restart openim-server

# 4. 等待熔断器自动恢复（30秒）
# 或手动触发健康检查
curl http://localhost:1024/api/im/health/check
```

### 场景3: 数据库连接池耗尽

```bash
# 1. 检查数据库连接
mysql -u root -p -e "SHOW PROCESSLIST;"

# 2. 杀死慢查询
mysql -u root -p -e "KILL {process_id};"

# 3. 重启应用
kill -9 {pid}
java -jar sa-admin/target/sa-admin.jar
```

---

## 💡 最佳实践

### 1. 群组创建

```java
// ✅ 好的做法：使用生命周期服务
ImGroupMappingEntity group = groupLifecycleService.createOrGetGroup(
    "POLICE", reportId, groupName, ownerId, memberIds
);

// ❌ 不好的做法：直接调用OpenIM API
String groupId = openIMApiService.createGroup(groupName, ownerId, memberIds);
```

### 2. 群组状态检查

```java
// ✅ 好的做法：检查状态后再操作
ImGroupMappingEntity group = getPoliceGroup(reportId);
if (ImGroupStatusEnum.getByValue(group.getStatus()).isUsable()) {
    // 执行操作
}

// ❌ 不好的做法：假设群组总是可用
ImGroupMappingEntity group = getPoliceGroup(reportId);
imGroupManageService.inviteMembers(group.getGroupId(), members);
```

### 3. 错误处理

```java
// ✅ 好的做法：完整的错误处理
try {
    ImGroupMappingEntity group = createOrGetPoliceGroup(reportId);
    if (group == null || !ImGroupStatusEnum.ACTIVE.getValue().equals(group.getStatus())) {
        log.error("群组状态异常");
        return;
    }
} catch (Exception e) {
    log.error("创建群组失败", e);
    // 触发告警或通知
}

// ❌ 不好的做法：忽略错误
try {
    createOrGetPoliceGroup(reportId);
} catch (Exception ignored) {}
```

---

## 📞 技术支持

遇到问题？联系我们：

- 📧 Email: lab1024@163.com
- 💬 WeChat: zhuoda1024
- 🌐 Website: https://1024lab.net
- 📖 文档: https://smartadmin.vip

---

**版权所有 © 2025 1024创新实验室**
