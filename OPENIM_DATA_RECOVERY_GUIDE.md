# OpenIM 数据恢复和初始化指南

## 问题分析

### 重启后数据丢失原因

**不是真的丢失!** 重启 OpenIM 后,数据实际上还在,但需要重新同步:

1. ✅ **MongoDB 数据持久化**: `/home/lai/openim-docker/components/mongodb/data/db`
2. ✅ **MinIO 数据持久化**: `/home/lai/openim-docker/components/mnt/data`
3. ⚠️ **OpenIM 内存缓存丢失**: 用户会话、群组缓存等内存数据重启后清空
4. ⚠️ **MySQL 同步映射存在**: `t_im_user_mapping`、`t_im_group_mapping` 数据还在

### 自动修复机制

我们的后端代码已经实现了**自动验证和修复**机制:

```java
// IMUserSyncService.java:368-421
public String getOpenIMUserId(Long employeeId) {
    IMUserMappingEntity mapping = imUserMappingDao.selectByEmployeeId(employeeId);

    if (mapping != null && IMConstant.SYNC_STATUS_SUCCESS.equals(mapping.getSyncStatus())) {
        // 🔧 验证用户在OpenIM服务器上是否真实存在
        if (verifyUserExistsOnServer(mapping.getOpenimUserId())) {
            return mapping.getOpenimUserId();  // ✅ 用户存在,直接返回
        } else {
            log.warn("⚠️ OpenIM服务器上不存在用户,删除旧映射重新同步");
            imUserMappingDao.deleteById(mapping.getId());  // ❌ 用户不存在,触发重新同步
        }
    }

    // 自动重新同步
    return syncUser(employeeId);
}
```

**这意味着**: 只要访问一次功能,系统会自动检测并重新同步数据!

---

## 数据恢复步骤

### 方案 1: 自动恢复 (推荐)

**原理**: 访问功能时,后端自动检测并重新同步数据

#### 步骤 1: 访问警情详情页

```
http://localhost:8081/oa/police/report-detail?reportId=5
```

#### 步骤 2: 切换到 "即时聊天" Tab

系统会自动执行:
1. 检查当前用户是否在 OpenIM 中存在
2. 如果不存在,自动调用 `syncUser()` 重新同步
3. 检查群组是否存在
4. 如果不存在,自动调用 `createGroupForReport()` 重新创建

#### 步骤 3: 查看后端日志

**预期日志**:
```
⚠️ [用户验证] 数据库中存在用户映射,但OpenIM服务器上不存在,删除旧映射重新同步
📤 [用户同步] 开始同步员工1 -> OpenIM用户emp_xxx
✅ [用户同步] 员工1同步成功,OpenIM用户ID: emp_xxx, 耗时: 150ms

⚠️ [群组验证] 数据库中存在群组映射,但OpenIM服务器上不存在,删除旧映射重新创建
📤 [群组创建] 为警情5创建群组
✅ [群组创建] 警情5的群组创建成功: group_report_5
```

**验证成功**: 聊天功能恢复正常!

---

### 方案 2: 手动批量恢复

如果需要恢复所有数据,可以使用批量同步接口。

#### 步骤 1: 启动后端服务

确保 SmartAdmin 后端正在运行:
```bash
cd smart-admin-api-java17-springboot3
mvn spring-boot:run
```

#### 步骤 2: 调用增量同步接口

**使用 Swagger UI**:
```
http://localhost:1024/swagger-ui/index.html
```

找到 **"IM 用户同步管理"** → **"增量同步用户"**

或者使用 curl:
```bash
curl -X POST http://localhost:1024/api/support/im/user/incremental-sync \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**预期响应**:
```json
{
  "code": 1,
  "msg": "操作成功",
  "data": {
    "total": 10,
    "successCount": 10,
    "failureCount": 0,
    "executionTime": 2500
  }
}
```

#### 步骤 3: 同步群组

访问任何警情详情页的聊天 Tab,系统会自动重新创建群组。

或者通过后端接口批量创建:
```bash
# 为所有警情创建群组
curl -X POST http://localhost:1024/api/business/oa/police/batch-create-groups \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### 方案 3: 完全重置 (慎用)

如果数据完全混乱,可以选择完全重置。

#### ⚠️ 警告

**会丢失所有聊天消息!** 只在测试环境使用!

#### 步骤 1: 清空 OpenIM 数据

```bash
# 进入 WSL
wsl

# 停止服务
cd /home/lai/openim-docker  # 根据实际路径调整
docker-compose down

# 清空 MongoDB 数据
sudo rm -rf components/mongodb/data/db/*

# 清空 MinIO 数据
sudo rm -rf components/mnt/data/openim/*

# 重启服务
docker-compose up -d
```

#### 步骤 2: 清空 MySQL 映射表

```sql
-- 连接到 SmartAdmin 数据库
USE smart_admin_v3;

-- 清空用户映射
TRUNCATE TABLE t_im_user_mapping;

-- 清空群组映射
TRUNCATE TABLE t_im_group_mapping;

-- 清空操作日志(可选)
TRUNCATE TABLE t_im_operation_log;
```

#### 步骤 3: 重新同步

访问系统功能,数据会自动重新同步。

---

## 验证数据恢复

### 检查用户同步状态

```sql
-- 查询用户映射
SELECT
    employee_id,
    openim_user_id,
    nickname,
    sync_status,
    sync_time,
    error_message
FROM t_im_user_mapping
ORDER BY sync_time DESC
LIMIT 20;
```

**预期结果**:
- `sync_status = 1` (成功)
- `sync_time` 是最近时间
- `error_message` 为空

### 检查群组同步状态

```sql
-- 查询群组映射
SELECT
    police_report_id,
    openim_group_id,
    group_name,
    sync_status,
    sync_time,
    error_message
FROM t_im_group_mapping
ORDER BY sync_time DESC
LIMIT 20;
```

### 检查 MongoDB 数据

```bash
# 进入 MongoDB 容器
wsl docker exec -it mongo mongosh

# 切换数据库
use openIM

# 查询用户
db.user.find({}, {userID: 1, nickname: 1, createTime: 1}).limit(5)

# 查询群组
db.super_group.find({}, {groupID: 1, groupName: 1, createTime: 1}).limit(5)

# 查询消息
db.sg_group_report_5.find({}, {sendID: 1, contentType: 1, sendTime: 1}).limit(10)
```

### 前端功能测试

1. **登录系统**
2. **访问警情列表**: `http://localhost:8081/oa/police/report-list`
3. **点击任意警情** → 切换到 "即时聊天" Tab
4. **检查聊天功能**:
   - ✅ 可以发送消息
   - ✅ 可以看到历史消息(如果有)
   - ✅ 可以发送图片(MinIO 配置正确的情况下)
   - ✅ 实时消息推送正常

---

## 数据持久化配置检查

### 检查当前挂载

```bash
# 检查 MongoDB 挂载
wsl docker inspect mongo --format='{{range .Mounts}}{{.Type}}: {{.Source}} -> {{.Destination}}{{println}}{{end}}'

# 检查 MinIO 挂载
wsl docker inspect minio --format='{{range .Mounts}}{{.Type}}: {{.Source}} -> {{.Destination}}{{println}}{{end}}'
```

**预期输出**:
```
# MongoDB
bind: /home/lai/openim-docker/components/mongodb/data/db -> /data/db  ✅

# MinIO
bind: /home/lai/openim-docker/components/mnt/data -> /data  ✅
```

### 确保数据目录权限

```bash
wsl

cd /home/lai/openim-docker

# 检查数据目录所有者
ls -la components/mongodb/data/
ls -la components/mnt/data/

# 如果权限不对,修复权限
sudo chown -R $(whoami):$(whoami) components/
```

---

## 预防措施

### 1. 定期备份 MongoDB

```bash
# 创建备份脚本
cat > /home/lai/openim-docker/backup-mongodb.sh << 'EOF'
#!/bin/bash
BACKUP_DIR=/home/lai/openim-docker/backups/mongodb
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR
docker exec mongo mongodump --out /tmp/backup
docker cp mongo:/tmp/backup $BACKUP_DIR/backup_$DATE
docker exec mongo rm -rf /tmp/backup

# 保留最近7天的备份
find $BACKUP_DIR -type d -mtime +7 -exec rm -rf {} +

echo "✅ MongoDB backup completed: $BACKUP_DIR/backup_$DATE"
EOF

chmod +x /home/lai/openim-docker/backup-mongodb.sh

# 设置定时任务(每天凌晨2点备份)
crontab -e
# 添加: 0 2 * * * /home/lai/openim-docker/backup-mongodb.sh
```

### 2. 配置健康检查

在 `docker-compose.yml` 中添加:
```yaml
services:
  openim-server:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:10002/"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  mongo:
    healthcheck:
      test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
      interval: 10s
      timeout: 5s
      retries: 5
```

### 3. 监控同步状态

创建一个定时任务,监控同步状态:
```sql
-- 查询同步失败的用户
SELECT COUNT(*) as failed_users
FROM t_im_user_mapping
WHERE sync_status = 0
AND update_time > DATE_SUB(NOW(), INTERVAL 1 HOUR);

-- 查询同步失败的群组
SELECT COUNT(*) as failed_groups
FROM t_im_group_mapping
WHERE sync_status = 0
AND update_time > DATE_SUB(NOW(), INTERVAL 1 HOUR);
```

---

## 常见问题

### Q1: 重启后仍然无法同步

**检查清单**:
1. OpenIM 服务器是否正常运行
   ```bash
   curl http://localhost:10002/
   ```
2. MongoDB 是否正常运行
   ```bash
   wsl docker logs mongo | tail -20
   ```
3. 后端日志是否有错误
   ```bash
   tail -f logs/smart-admin.log | grep -E "IM|OpenIM"
   ```

### Q2: 自动同步失败

**查看错误日志**:
```sql
SELECT
    operation_type,
    resource_type,
    resource_id,
    error_message,
    create_time
FROM t_im_operation_log
WHERE success = 0
ORDER BY create_time DESC
LIMIT 20;
```

**常见错误**:
- `Connection refused`: OpenIM 服务器未启动
- `Unauthorized`: 认证配置错误
- `Timeout`: 网络问题或服务器负载过高

### Q3: 消息历史丢失

**检查消息表**:
```bash
wsl docker exec mongo mongosh openIM --eval "db.getCollectionNames()" | grep sg_
```

**如果消息表存在**: 消息还在,只是前端没有正确加载
**如果消息表不存在**: 消息确实丢失,无法恢复

**解决**: 配置 MongoDB 定期备份(见"预防措施")

### Q4: MinIO 图片丢失

**检查文件**:
```bash
wsl ls -lh /home/lai/openim-docker/components/mnt/data/openim/
```

**如果文件存在**: 配置问题,检查 MinIO 访问地址
**如果文件不存在**: 文件丢失,无法恢复

---

## 总结

### ✅ 好消息

1. **数据持久化已配置**: MongoDB 和 MinIO 数据不会因重启丢失
2. **自动修复机制已实现**: 访问功能时自动检测并修复
3. **MySQL 映射表保留**: 可以追溯历史同步记录

### 🔧 恢复步骤

**最简单的方式**:
1. 启动后端服务
2. 访问警情详情页 → 即时聊天 Tab
3. 系统自动检测并重新同步
4. 恢复完成! ✅

### 📋 后续优化

1. 配置 MongoDB 定期备份
2. 添加健康检查和监控
3. 实现数据同步状态面板
4. 配置告警通知

---

**文档创建时间**: 2025-10-10
**适用版本**: SmartAdmin v3.27.0+
**OpenIM 版本**: v3.8.3

**需要帮助?** 检查日志并联系技术支持!
