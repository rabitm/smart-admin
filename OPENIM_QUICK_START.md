# OpenIM 即时通讯快速启动指南

## 🚀 5分钟快速上手

本指南帮助你在 5 分钟内启动并测试 OpenIM 即时通讯功能。

---

## 前提条件

- ✅ Docker 和 Docker Compose 已安装
- ✅ MySQL 数据库已运行（smart_admin_v3）
- ✅ Redis 服务已运行
- ✅ Node.js 和 npm 已安装
- ✅ JDK 17 已安装
- ✅ Maven 已配置

---

## 步骤 1: 启动 OpenIM 服务器（2分钟）

### 使用 Docker Compose（推荐）

```bash
# 1. 克隆 OpenIM 仓库
git clone https://github.com/openimsdk/open-im-server.git
cd open-im-server

# 2. 启动所有服务
docker-compose up -d

# 3. 等待服务启动（约30秒）
docker-compose ps

# 4. 验证服务是否正常
curl http://localhost:10002/healthz
```

**预期输出**:
```json
{"status":"ok"}
```

如果看到 `{"status":"ok"}`，说明 OpenIM 服务已成功启动！

### 端口说明
- `10001`: WebSocket 服务（前端连接）
- `10002`: HTTP API 服务（后端调用）
- `10005`: 管理后台

---

## 步骤 2: 创建 OpenIM 管理员账号（1分钟）

```bash
# 使用 OpenIM API 创建管理员用户
curl -X POST http://localhost:10002/user/user_register \
  -H "Content-Type: application/json" \
  -d '{
    "secret": "openIM123",
    "users": [{
      "userID": "imAdmin",
      "nickname": "IM管理员",
      "faceURL": ""
    }]
  }'
```

**预期输出**:
```json
{
  "errCode": 0,
  "errMsg": "",
  "data": {}
}
```

如果 `errCode` 为 0，说明管理员账号创建成功！

---

## 步骤 3: 执行数据库脚本（30秒）

```bash
# 进入 SmartAdmin 目录
cd smart-admin

# 使用 MySQL 客户端执行脚本
mysql -h 101.36.125.254 -u root -pSmartAdmin666 smart_admin_v3 < sql/sql-update-log/v3.28.0-openim-integration.sql
```

或者使用 Navicat/DBeaver 等工具手动执行 `sql/sql-update-log/v3.28.0-openim-integration.sql`

**验证**:
```sql
-- 检查表是否创建成功
SHOW TABLES LIKE 't_im_%';
```

应该看到 5 个表：
- `t_im_user_mapping`
- `t_im_group_mapping`
- `t_im_group_member`
- `t_im_group_invite_rule`
- `t_im_operation_log`

---

## 步骤 4: 启动后端服务（1分钟）

```bash
cd smart-admin-api-java17-springboot3

# 编译项目
mvn clean compile

# 启动服务
mvn spring-boot:run
```

**等待启动完成**，看到以下日志表示成功：

```
✅ OpenIM RestTemplate初始化完成 - API地址: http://localhost:10002
Started SmartAdminApplication in 15.234 seconds
```

**验证 IM 接口**:

访问 Swagger 文档：http://localhost:1024/doc.html

在左侧菜单中查找 **IM模块**，应该能看到以下接口分组：
- IM用户管理
- IM群组管理
- IM配置管理

---

## 步骤 5: 启动前端服务（1分钟）

```bash
cd smart-admin-web-typescript

# 安装依赖（首次运行）
# npm install

# 启动开发服务器
npm run dev
```

**等待编译完成**，看到：

```
  ➜  Local:   http://localhost:8081/
  ➜  Network: http://192.168.x.x:8081/
```

---

## 步骤 6: 测试即时通讯功能（1分钟）

### 6.1 登录系统

1. 打开浏览器访问: http://localhost:8081
2. 使用默认账号登录：
   - 用户名: `admin`
   - 密码: `123456`

### 6.2 测试用户自动同步

1. 进入 **系统管理 -> 员工管理**
2. 查看员工列表
3. 打开浏览器开发者工具（F12），查看控制台

**预期日志**:
```
👤 [用户同步] 开始同步员工...
✅ [用户同步] 员工同步成功
```

**验证数据库**:
```sql
-- 查看已同步的用户
SELECT * FROM t_im_user_mapping WHERE sync_status = 1;
```

### 6.3 测试群组创建和即时聊天

1. 进入 **业务管理 -> 警情管理 -> 警情接警**
2. 点击"新增"按钮，创建一条新警情
3. 填写基本信息后保存
4. 点击右侧 **即时聊天** 标签页

**预期效果**:
- ✅ 系统自动创建群组
- ✅ 显示聊天界面
- ✅ 可以发送和接收消息

**验证数据库**:
```sql
-- 查看已创建的群组
SELECT * FROM t_im_group_mapping;

-- 查看群组成员
SELECT * FROM t_im_group_member;
```

### 6.4 测试多人聊天

1. 打开第二个浏览器窗口（或使用隐私模式）
2. 使用另一个账号登录
3. 打开同一条警情
4. 点击"即时聊天"标签页
5. 在两个窗口中互相发送消息

**预期效果**:
- ✅ 消息实时同步
- ✅ 两个窗口都能看到对方的消息

---

## 🎉 成功！

如果以上所有步骤都正常，恭喜你已经成功集成 OpenIM 即时通讯功能！

---

## 📊 查看运行状态

### 1. 检查 OpenIM 服务状态

```bash
# 查看 Docker 容器状态
docker-compose ps

# 查看 OpenIM 日志
docker-compose logs -f openim-server

# 测试 API 连接
curl http://localhost:10002/healthz
```

### 2. 检查后端 IM 状态

访问以下接口：

**Token 缓存信息**:
```bash
curl http://localhost:1024/im/config/token-cache
```

**预期输出**:
```json
{
  "code": 1,
  "data": {
    "exists": true,
    "remainingSeconds": 86340,
    "isExpired": false
  }
}
```

**用户同步统计**:
```sql
-- 查看同步状态统计
SELECT sync_status,
       CASE sync_status
           WHEN 1 THEN '成功'
           WHEN 0 THEN '失败'
           ELSE '未知'
       END AS status_text,
       COUNT(*) as count
FROM t_im_user_mapping
GROUP BY sync_status;
```

**操作日志**:
```sql
-- 查看最近的 IM 操作
SELECT operation_type, success_flag, COUNT(*) as count
FROM t_im_operation_log
WHERE create_time > DATE_SUB(NOW(), INTERVAL 1 HOUR)
GROUP BY operation_type, success_flag;
```

### 3. 检查前端连接状态

打开浏览器开发者工具（F12），在 Console 中应该能看到：

```
🔌 [OpenIM] SDK 初始化中...
✅ [OpenIM] SDK 初始化成功
🔐 [OpenIM] 用户登录中...
✅ [OpenIM] 用户登录成功
```

---

## 🐛 常见问题快速修复

### 问题 1: OpenIM 服务启动失败

**症状**: `docker-compose up -d` 失败或 `curl http://localhost:10002/healthz` 无响应

**解决方案**:
```bash
# 停止所有容器
docker-compose down

# 清理旧数据（谨慎操作）
docker-compose down -v

# 重新启动
docker-compose up -d

# 查看详细日志
docker-compose logs -f
```

### 问题 2: 管理员账号创建失败

**症状**: 注册接口返回错误

**解决方案**:
```bash
# 检查 OpenIM 是否正常运行
docker-compose ps

# 确认端口 10002 可访问
telnet localhost 10002

# 查看 OpenIM 日志
docker-compose logs openim-server | grep ERROR
```

### 问题 3: 后端 Token 获取失败

**症状**: 后端日志显示 "Token获取失败"

**解决方案**:

1. 确认 `sa-base.yaml` 中配置正确：
```yaml
openim:
  api-url: http://localhost:10002  # 确保地址正确
  admin-user-id: imAdmin           # 确保用户已创建
  admin-secret: openIM123          # 确保密钥正确
```

2. 手动测试 Token API：
```bash
curl -X POST http://localhost:10002/auth/user_token \
  -H "Content-Type: application/json" \
  -d '{
    "secret": "openIM123",
    "platformID": 10,
    "userID": "imAdmin"
  }'
```

3. 重启后端服务

### 问题 4: 前端连接 WebSocket 失败

**症状**: 浏览器控制台显示 WebSocket 连接失败

**解决方案**:

1. 检查 OpenIM WebSocket 端口：
```bash
netstat -an | grep 10001
```

2. 检查 `.env.development` 配置：
```env
VITE_OPENIM_WS_URL=ws://localhost:10001  # 确保地址正确
```

3. 清除浏览器缓存并刷新页面

### 问题 5: 用户同步失败

**症状**: `t_im_user_mapping` 表中 `sync_status = 0`

**解决方案**:

1. 查看错误信息：
```sql
SELECT employee_id, error_message
FROM t_im_user_mapping
WHERE sync_status = 0;
```

2. 手动重试同步：
```bash
# 调用重试接口
curl -X POST http://localhost:1024/im/user/sync-failed
```

3. 检查后端日志：
```bash
tail -f logs/smart_admin_v3/sa-admin/dev/smart-admin.log | grep "用户同步"
```

### 问题 6: 群组创建失败

**症状**: 点击"即时聊天"标签页显示错误

**解决方案**:

1. 确保用户已同步：
```sql
-- 检查当前登录用户是否已同步
SELECT * FROM t_im_user_mapping WHERE employee_id = ?;
```

2. 查看操作日志：
```sql
SELECT * FROM t_im_operation_log
WHERE operation_type = 'GROUP_CREATE'
AND success_flag = 0
ORDER BY create_time DESC
LIMIT 10;
```

3. 手动创建群组：
```bash
curl -X POST http://localhost:1024/im/group/create/{reportId}
```

---

## 📖 下一步

现在你已经成功运行了 OpenIM 集成，可以继续：

1. **配置群组邀请规则**
   - 进入数据库配置 `t_im_group_invite_rule` 表
   - 设置哪些部门/职位/角色的用户自动加入群组

2. **自定义群组名称模板**
   - 修改 `sa-base.yaml` 中的 `group-name-template`
   - 支持变量: `{incidentType}`, `{location}`, `{time}`

3. **监控 IM 运行状态**
   - 查看操作日志表 `t_im_operation_log`
   - 监控 Token 刷新情况
   - 检查熔断器状态

4. **性能优化**
   - 调整批量同步大小
   - 配置 Redis 缓存
   - 优化 WebSocket 连接池

5. **生产环境部署**
   - 参考 `OPENIM_INTEGRATION_COMPLETE.md` 中的生产环境配置
   - 配置 HTTPS 和域名
   - 设置监控和告警

---

## 💡 提示和技巧

1. **开发调试**
   - 使用 `VITE_MESSAGE_DEBUG=true` 开启前端消息调试
   - 查看浏览器 Network 面板的 WebSocket 连接
   - 使用 Postman 测试后端 IM 接口

2. **性能测试**
   - 使用 JMeter 测试并发创建群组
   - 监控数据库连接池使用情况
   - 检查 Redis 内存使用

3. **日志分析**
   - 使用 `grep` 过滤 IM 相关日志
   - 配置日志收集（如 ELK）
   - 设置日志告警规则

4. **数据备份**
   - 定期备份 IM 相关表
   - 备份 OpenIM 数据（如果需要持久化）
   - 导出操作日志用于分析

---

## 📞 获取帮助

如果遇到问题：

1. 查看完整文档: `OPENIM_INTEGRATION_COMPLETE.md`
2. 查看 OpenIM 官方文档: https://docs.openim.io
3. 检查 GitHub Issues: https://github.com/openimsdk/open-im-server/issues
4. 查看 SmartAdmin 文档: https://smartadmin.vip

---

**文档更新时间**: 2025-10-09
**SmartAdmin 版本**: v3.28.0
**OpenIM 版本**: latest (compatible with v3.x)
**OpenIM SDK 版本**: @openim/wasm-client-sdk v3.8.3-patch.10
