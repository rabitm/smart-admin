# OpenIM 即时通讯集成完成文档

## 🎉 集成状态

✅ **前端集成完成** - TypeScript + Vue3 + OpenIM WASM SDK
✅ **后端集成完成** - Java 17 + Spring Boot 3 + OpenIM REST API
✅ **编译验证成功** - 前后端均无编译错误
⚙️ **配置待完成** - 需要配置 OpenIM 服务器连接信息

---

## 📋 已完成的功能模块

### 1. 前端功能 (TypeScript)

#### 核心组件
- ✅ `openim-client.ts` - OpenIM SDK 封装客户端
- ✅ `ChatPanel.vue` - 聊天面板主组件
- ✅ `ChatMessageItem.vue` - 消息列表项组件
- ✅ `emergency-intake.vue` - 警情接警页面集成聊天功能

#### API 接口
- ✅ `im-api.ts` - IM 相关 API 定义
  - `getTokenInfo()` - 获取用户 Token
  - `createGroupForReport()` - 为警情创建群组
  - `getGroupInfo()` - 获取群组信息

#### 功能特性
- ✅ OpenIM WASM SDK 集成 (v3.8.3-patch.10)
- ✅ 自动连接和登录
- ✅ 群组会话管理
- ✅ 消息发送和接收
- ✅ 消息列表显示
- ✅ 输入框和发送按钮
- ✅ 错误处理和重连机制
- ✅ 标签页切换（专业信息 ↔ 即时聊天）

### 2. 后端功能 (Java 17)

#### 数据库表结构
- ✅ `t_im_user_mapping` - 用户映射表
- ✅ `t_im_group_mapping` - 群组映射表
- ✅ `t_im_group_member` - 群组成员表
- ✅ `t_im_group_invite_rule` - 群组邀请规则表
- ✅ `t_im_operation_log` - IM 操作日志表
- ✅ `police_report` 新增字段 `im_group_id`

#### 核心服务层
```
net.lab1024.sa.admin.module.support.im
├── client/
│   ├── OpenIMClient.java              ✅ HTTP 客户端（熔断器 + 重试）
│   └── OpenIMTokenManager.java        ✅ Token 管理器（自动刷新）
├── service/
│   ├── IMUserSyncService.java         ✅ 用户同步服务
│   ├── IMGroupManagementService.java  ✅ 群组管理服务
│   ├── IMGroupInviteRuleService.java  ✅ 群组邀请规则服务
│   └── IMOperationLogService.java     ✅ 操作日志服务
├── controller/
│   ├── IMUserController.java          ✅ 用户接口
│   ├── IMGroupController.java         ✅ 群组接口
│   └── IMConfigController.java        ✅ 配置接口
├── config/
│   ├── OpenIMConfig.java              ✅ OpenIM 配置类
│   └── IMAsyncConfig.java             ✅ 异步任务配置
├── constant/
│   ├── IMConstant.java                ✅ 常量定义
│   ├── IMErrorCodeEnum.java           ✅ 错误码枚举
│   └── IMOperationTypeEnum.java       ✅ 操作类型枚举
├── dao/
│   ├── IMUserMappingDao.java          ✅ 用户映射 DAO
│   ├── IMGroupMappingDao.java         ✅ 群组映射 DAO
│   ├── IMGroupMemberDao.java          ✅ 群组成员 DAO
│   ├── IMGroupInviteRuleDao.java      ✅ 邀请规则 DAO
│   └── IMOperationLogDao.java         ✅ 操作日志 DAO
└── domain/entity/
    ├── IMUserMappingEntity.java       ✅ 用户映射实体
    ├── IMGroupMappingEntity.java      ✅ 群组映射实体
    ├── IMGroupMemberEntity.java       ✅ 群组成员实体
    ├── IMGroupInviteRuleEntity.java   ✅ 邀请规则实体
    └── IMOperationLogEntity.java      ✅ 操作日志实体
```

#### 核心功能特性
- ✅ 用户自动同步到 OpenIM
- ✅ 批量用户同步（支持分批处理）
- ✅ 增量同步（仅同步未同步或失败的用户）
- ✅ 自动创建群组并关联警情
- ✅ 智能邀请规则（按部门/职位/角色自动邀请）
- ✅ 群组成员管理（添加/移除）
- ✅ Token 自动刷新和缓存
- ✅ 熔断器保护（防止服务雪崩）
- ✅ 重试机制（指数退避）
- ✅ 完整操作日志记录
- ✅ 监听警情事件自动创建群组

---

## 🔧 配置步骤

### 1. 安装 OpenIM 服务器

#### 方式一：Docker Compose（推荐）

```bash
# 克隆 OpenIM 仓库
git clone https://github.com/openimsdk/open-im-server.git
cd open-im-server

# 启动服务
docker-compose up -d

# 查看服务状态
docker-compose ps
```

#### 方式二：二进制部署

参考官方文档：https://docs.openim.io/guides/gettingStarted/installation

### 2. 配置后端（Java）

在 `sa-base/src/main/resources/dev/sa-base.yaml` 文件末尾添加 OpenIM 配置：

```yaml
# OpenIM 即时通讯配置
openim:
  # 是否启用 IM 功能
  enabled: true

  # OpenIM API 服务地址
  api-url: http://localhost:10002

  # OpenIM WebSocket 服务地址（前端使用）
  ws-url: ws://localhost:10001

  # 管理员用户 ID（需要先在 OpenIM 中创建）
  admin-user-id: imAdmin

  # 管理员密钥（如果 OpenIM 启用了认证）
  admin-secret: openIM123

  # 平台 ID（1=iOS, 2=Android, 3=Windows, 4=OSX, 5=Web, 6=MiniWeb, 7=Linux, 8=AndroidPad, 9=iPad, 10=Admin）
  platform-id: 10

  # Token 有效期（秒）
  token-expire-seconds: 86400

  # API 调用超时时间（秒）
  api-timeout-seconds: 10

  # API 调用最大重试次数
  retry-max-count: 3

  # 是否自动创建群组
  auto-create-group: true

  # 是否自动邀请成员
  auto-invite: true

  # 群组最大成员数
  group-max-members: 200

  # 群组名称模板（{incidentType}=警情类型, {location}=地点, {time}=时间）
  group-name-template: "【{incidentType}】{location}"

  # 用户批量同步每批数量
  user-sync-batch-size: 50

  # 群组邀请每批数量
  group-invite-batch-size: 100

  # 熔断器失败阈值
  circuit-breaker-threshold: 5

  # 熔断器超时时间（秒）
  circuit-breaker-timeout-seconds: 60
```

### 3. 配置前端（TypeScript）

前端配置已存在于 `.env.development`：

```env
# OpenIM 配置
VITE_OPENIM_WS_URL=ws://localhost:10001
VITE_OPENIM_API_URL=http://localhost:10002
```

**注意**：如果 OpenIM 部署在其他服务器，请修改为实际地址。

### 4. 初始化数据库

执行 SQL 脚本创建 IM 相关表：

```bash
# 脚本位置
sql/sql-update-log/v3.28.0-openim-integration.sql
```

在 MySQL 中执行该脚本：

```sql
source /path/to/v3.28.0-openim-integration.sql;
```

### 5. 创建 OpenIM 管理员账号

使用 OpenIM 管理工具或 API 创建管理员账号：

```bash
# 使用 OpenIM CLI 创建用户（示例）
curl -X POST http://localhost:10002/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "users": [{
      "userID": "imAdmin",
      "nickname": "IM管理员",
      "secret": "openIM123"
    }]
  }'
```

---

## 🚀 启动和测试

### 1. 启动 OpenIM 服务

```bash
# 使用 Docker Compose
cd open-im-server
docker-compose up -d

# 验证服务状态
curl http://localhost:10002/healthz
```

### 2. 启动后端服务

```bash
cd smart-admin-api-java17-springboot3
mvn clean compile
mvn spring-boot:run
```

访问 Swagger 文档验证 IM 接口：
```
http://localhost:1024/doc.html
```

查看 IM 相关接口：
- `/im/user/*` - 用户管理接口
- `/im/group/*` - 群组管理接口
- `/im/config/*` - 配置信息接口

### 3. 启动前端服务

```bash
cd smart-admin-web-typescript
npm run dev
```

访问应用：
```
http://localhost:8081
```

### 4. 测试流程

#### 测试用户同步

1. 登录系统
2. 进入 **系统管理 -> 员工管理**
3. 查看员工列表
4. 后台会自动同步员工到 OpenIM（首次启动时）

验证同步结果：
```bash
# 查询数据库
SELECT * FROM t_im_user_mapping;
```

#### 测试群组创建

1. 进入 **业务管理 -> 警情管理 -> 警情接警**
2. 创建新警情或编辑现有警情
3. 点击 **即时聊天** 标签页
4. 系统自动创建群组并加载聊天界面

验证群组创建：
```bash
# 查询数据库
SELECT * FROM t_im_group_mapping;
SELECT * FROM t_im_group_member;
```

#### 测试即时通讯

1. 在聊天面板中输入消息
2. 点击发送
3. 消息应该出现在消息列表中
4. 打开多个浏览器窗口测试多人聊天

---

## 📊 监控和日志

### 1. 后端日志

查看 IM 操作日志：

```sql
-- 查看最近的 IM 操作
SELECT * FROM t_im_operation_log
ORDER BY create_time DESC
LIMIT 50;

-- 查看失败的操作
SELECT * FROM t_im_operation_log
WHERE success_flag = 0
ORDER BY create_time DESC;
```

查看应用日志：

```bash
# 日志文件位置
tail -f logs/smart_admin_v3/sa-admin/dev/smart-admin.log

# 搜索 IM 相关日志
grep -i "openim\|IM" logs/smart_admin_v3/sa-admin/dev/smart-admin.log
```

### 2. 前端日志

打开浏览器开发者工具（F12），查看 Console 日志：

```javascript
// OpenIM 连接日志
🔌 [OpenIM] SDK 初始化中...
✅ [OpenIM] SDK 初始化成功
🔐 [OpenIM] 用户登录中...
✅ [OpenIM] 用户登录成功

// 消息发送日志
📤 [OpenIM] 发送消息...
✅ [OpenIM] 消息发送成功

// 错误日志
❌ [OpenIM] 连接失败: Connection refused
```

### 3. 性能监控

查看 Token 缓存状态：

```bash
curl http://localhost:1024/im/config/token-cache
```

查看熔断器状态：

```bash
# 检查后端日志
grep "熔断器" logs/smart_admin_v3/sa-admin/dev/smart-admin.log
```

---

## 🔍 故障排查

### 常见问题

#### 1. 前端无法连接 OpenIM

**症状**：浏览器控制台显示 WebSocket 连接失败

**排查步骤**：
```bash
# 检查 OpenIM 服务是否运行
docker-compose ps

# 检查端口是否开放
netstat -an | grep 10001
telnet localhost 10001

# 检查防火墙
sudo ufw status
```

**解决方案**：
- 确保 OpenIM 服务正常运行
- 检查 `.env.development` 中的 WebSocket 地址配置
- 检查防火墙规则

#### 2. 后端 Token 获取失败

**症状**：后端日志显示 "Token获取失败"

**排查步骤**：
```bash
# 测试 OpenIM API
curl -X POST http://localhost:10002/auth/user_token \
  -H "Content-Type: application/json" \
  -d '{
    "userID": "imAdmin",
    "platformID": 10
  }'
```

**解决方案**：
- 检查 `admin-user-id` 配置是否正确
- 确认管理员账号已在 OpenIM 中创建
- 检查 `admin-secret` 是否配置正确

#### 3. 用户同步失败

**症状**：`t_im_user_mapping` 表中 `sync_status` 为 0

**排查步骤**：
```sql
-- 查看失败的用户
SELECT * FROM t_im_user_mapping
WHERE sync_status = 0;

-- 查看错误信息
SELECT employee_id, error_message
FROM t_im_user_mapping
WHERE sync_status = 0;
```

**解决方案**：
- 查看 `error_message` 字段了解失败原因
- 手动重试同步：调用 `/im/user/sync-failed` 接口
- 检查 OpenIM 服务是否正常

#### 4. 群组创建失败

**症状**：警情详情页聊天标签页显示错误

**排查步骤**：
```sql
-- 查看操作日志
SELECT * FROM t_im_operation_log
WHERE operation_type = 'GROUP_CREATE'
AND success_flag = 0
ORDER BY create_time DESC;
```

**解决方案**：
- 确保用户已同步到 OpenIM
- 检查群组邀请规则配置
- 查看操作日志中的错误详情

#### 5. 熔断器开启

**症状**：后端日志显示 "熔断器开启中,拒绝请求"

**排查步骤**：
```bash
# 查看最近的失败请求
grep "熔断器" logs/smart_admin_v3/sa-admin/dev/smart-admin.log | tail -20
```

**解决方案**：
- 检查 OpenIM 服务是否正常
- 等待熔断器超时（默认 60 秒）
- 修复 OpenIM 连接问题后熔断器会自动关闭

---

## 📚 API 接口文档

### 用户管理接口

#### 同步单个用户
```http
POST /im/user/sync/{employeeId}
```

#### 批量同步用户
```http
POST /im/user/batch-sync
Content-Type: application/json

{
  "employeeIds": [1, 2, 3]
}
```

#### 增量同步
```http
POST /im/user/incremental-sync
```

#### 同步失败的用户
```http
POST /im/user/sync-failed
```

### 群组管理接口

#### 为警情创建群组
```http
POST /im/group/create/{reportId}
```

#### 获取群组信息
```http
GET /im/group/{reportId}/info
```

#### 添加群组成员
```http
POST /im/group/add-members
Content-Type: application/json

{
  "groupId": "GROUP_xxx",
  "employeeIds": [1, 2, 3]
}
```

#### 移除群组成员
```http
POST /im/group/remove-members
Content-Type: application/json

{
  "groupId": "GROUP_xxx",
  "employeeIds": [1, 2]
}
```

### 配置管理接口

#### 获取用户 Token 信息
```http
GET /im/config/token-info
```

响应示例：
```json
{
  "code": 1,
  "data": {
    "openimUserId": "SA_EMP_123",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "wsUrl": "ws://localhost:10001",
    "apiUrl": "http://localhost:10002",
    "platformId": 10
  }
}
```

#### 获取 Token 缓存信息
```http
GET /im/config/token-cache
```

---

## 🎯 业务流程

### 1. 用户同步流程

```
员工创建/修改
    ↓
触发事件监听 (EmployeeEventListener)
    ↓
调用 IMUserSyncService.syncUser()
    ↓
生成 OpenIM 用户 ID (SA_EMP_{employeeId})
    ↓
调用 OpenIM API 注册用户
    ↓
保存映射关系到 t_im_user_mapping
    ↓
记录操作日志到 t_im_operation_log
```

### 2. 群组创建流程

```
警情创建/编辑 → 点击"即时聊天"标签页
    ↓
前端调用 /im/group/create/{reportId}
    ↓
检查群组是否已存在
    ↓
查询群组邀请规则
    ↓
根据规则筛选成员（部门/职位/角色）
    ↓
批量同步未注册的成员
    ↓
调用 OpenIM API 创建群组
    ↓
批量邀请成员加入群组
    ↓
保存群组映射关系
    ↓
保存成员关系
    ↓
更新警情表的 im_group_id 字段
    ↓
返回群组 ID 给前端
```

### 3. 消息发送流程

```
用户输入消息 → 点击发送
    ↓
调用 OpenIM SDK sendMessage()
    ↓
通过 WebSocket 发送到 OpenIM 服务器
    ↓
OpenIM 服务器分发消息到群组成员
    ↓
群组成员接收消息
    ↓
前端监听 onRecvNewMessage 事件
    ↓
更新消息列表显示
```

---

## 🔐 安全注意事项

1. **Token 安全**
   - Token 存储在内存中，不会持久化到本地存储
   - Token 自动刷新机制，提前 5 分钟刷新
   - 使用读写锁保证线程安全

2. **API 认证**
   - 所有 OpenIM API 调用都需要管理员 Token
   - Token 通过 HTTP Header 传输
   - 使用 HTTPS 加密传输（生产环境）

3. **数据隔离**
   - 每个警情创建独立群组
   - 群组成员根据规则自动邀请
   - 支持手动添加/移除成员

4. **日志审计**
   - 所有 IM 操作记录到 `t_im_operation_log`
   - 包含操作人、操作时间、操作内容、执行结果
   - 支持按操作类型、时间范围查询

---

## 📈 性能优化建议

1. **批量处理**
   - 用户同步使用批量 API（每批 50 个）
   - 群组邀请使用批量 API（每批 100 个）
   - 避免在循环中调用单个 API

2. **异步处理**
   - 全量用户同步使用 `@Async` 异步执行
   - 不阻塞主线程和用户请求
   - 配置独立的线程池 `imAsyncExecutor`

3. **缓存机制**
   - Token 缓存，避免频繁获取
   - OpenIM 用户 ID 映射缓存（考虑使用 Redis）
   - 群组信息缓存

4. **熔断保护**
   - 连续失败 5 次触发熔断器
   - 熔断 60 秒后自动恢复
   - 防止服务雪崩

5. **重试机制**
   - API 调用失败自动重试（最多 3 次）
   - 指数退避策略（1s、2s、4s）
   - 避免短时间内大量重试

---

## 🔄 后续优化方向

1. **功能增强**
   - [ ] 消息历史记录持久化
   - [ ] 群组公告功能
   - [ ] 文件分享功能
   - [ ] @提醒功能
   - [ ] 消息已读/未读状态
   - [ ] 消息撤回功能

2. **性能优化**
   - [ ] 引入 Redis 缓存 OpenIM 用户映射
   - [ ] 消息分页加载（虚拟滚动）
   - [ ] WebSocket 连接池优化
   - [ ] 离线消息同步

3. **监控告警**
   - [ ] IM 服务健康检查接口
   - [ ] 同步失败告警
   - [ ] 熔断器触发告警
   - [ ] Token 刷新失败告警

4. **管理功能**
   - [ ] IM 配置管理界面
   - [ ] 群组管理界面
   - [ ] 邀请规则配置界面
   - [ ] 操作日志查询界面

---

## 📞 技术支持

如遇到问题，请按以下顺序排查：

1. 查看本文档的 **故障排查** 章节
2. 查看后端日志文件
3. 查看数据库 `t_im_operation_log` 表
4. 查看浏览器控制台日志
5. 检查 OpenIM 服务状态和日志

---

## 📝 更新日志

### v3.28.0 (2025-10-09)

**新增功能**
- ✅ 完整的 OpenIM 集成（前端 + 后端）
- ✅ 用户自动同步机制
- ✅ 群组自动创建和成员邀请
- ✅ 聊天面板集成到警情接警页面
- ✅ 完整的操作日志和监控

**技术改进**
- ✅ 使用 `@openim/wasm-client-sdk` v3.8.3-patch.10
- ✅ 熔断器和重试机制
- ✅ Token 自动刷新
- ✅ 异步处理优化
- ✅ 批量处理性能优化

**Bug 修复**
- ✅ 修复前端路径别名问题（`@/` → `/@/`）
- ✅ 修复后端 TokenService 依赖问题
- ✅ 修复 BusinessException 构造函数问题
- ✅ 修复 Lambda 表达式变量作用域问题
- ✅ 添加 PoliceReportEntity.imGroupId 字段

---

**文档生成时间**: 2025-10-09
**集成版本**: SmartAdmin v3.28.0
**OpenIM SDK 版本**: v3.8.3-patch.10
**Spring Boot 版本**: 3.5.4
**Vue 版本**: 3.4.27
