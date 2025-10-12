# OpenIM 即时通讯入口位置和使用说明

## 📍 即时通讯入口位置

### 方式一：警情详情页面（新增✨）

**路径**: `业务管理 -> 警情管理 -> 警情列表`

**步骤**:
1. 进入警情列表页面
2. 点击任意警情记录的"查看"按钮
3. 在警情详情页面，点击 **"即时聊天"** 标签页（位于"操作历史"标签页旁边）
4. 系统会自动加载聊天界面

**特点**:
- ✅ 与操作历史并列，方便快速切换
- ✅ 查看警情详情的同时可以沟通
- ✅ 适合协作讨论和沟通

**文件位置**: `police-report-detail.vue` (第115-124行)

### 方式二：警情接警页面

**路径**: `业务管理 -> 警情管理 -> 警情接警`

**步骤**:
1. 进入警情接警页面
2. 创建新警情或编辑现有警情
3. 在右侧面板，点击 **"即时聊天"** 标签页（与"专业信息"标签页并列）
4. 系统会自动加载聊天界面

**特点**:
- ✅ 录入警情时可以同步沟通
- ✅ 实时协作录入信息
- ✅ 适合多人协同接警

**文件位置**: `emergency-intake.vue` (第297行)

---

## 🔧 后端事件监听器修复说明

### 问题描述

后端启动时出现以下警告日志（每次事件触发时都会打印）:

```
[2025-10-09 18:08:15,483][INFO] 📨 [事件监听] 收到警情创建事件
[2025-10-09 18:08:15,483][INFO] ⚠️ [事件监听] 警情创建事件监听功能待完善
```

### 问题原因

事件监听器使用 `@EventListener` 监听所有 `Object` 类型的事件，导致：
- 监听到不相关的Spring内部事件
- 打印大量无用的日志信息
- 对性能有轻微影响

### 解决方案

已将所有事件监听器方法注释掉，因为：
1. **具体的事件类尚未创建**（如 `PoliceReportCreatedEvent`）
2. **核心功能不依赖事件监听**（手动调用 API 创建群组完全可用）
3. **避免日志污染**

### 修复文件

`IMEventListenerService.java` - 所有监听方法已注释

修改内容：
- 禁用 `onPoliceReportCreated()` - 警情创建事件监听
- 禁用 `onEmployeeCreated()` - 员工创建事件监听
- 禁用 `onPoliceReportStatusChanged()` - 警情状态变更事件监听

### 当前状态

✅ **不影响核心功能**:
- 手动创建群组 API: `/im/group/create/{reportId}` 完全可用
- 用户同步 API: `/im/user/sync/{employeeId}` 完全可用
- 所有 REST API 接口正常工作

⚠️ **暂不支持自动化**:
- 创建警情时不会自动创建IM群组
- 创建员工时不会自动同步到OpenIM
- 警情状态变更时不会自动发送通知

---

## 🚀 如何使用即时通讯功能

### 1. 首次使用前的准备

#### 步骤 1: 启动 OpenIM 服务器

```bash
# 使用 Docker Compose
docker-compose up -d

# 验证服务
curl http://localhost:10002/healthz
```

#### 步骤 2: 同步用户到 OpenIM

**方式 A: 自动全量同步（推荐首次使用）**

访问 Swagger 接口文档: http://localhost:1024/doc.html

找到 **IM用户管理** 分组，执行:
- `POST /im/user/incremental-sync` - 增量同步（只同步未同步的用户）

**方式 B: 手动同步单个用户**

```bash
# 同步指定员工
curl -X POST http://localhost:1024/im/user/sync/{employeeId}
```

### 2. 使用聊天功能

#### 在警情详情页使用

1. 进入任意警情详情页
2. 点击 **"即时聊天"** 标签页
3. 首次打开时，系统会自动：
   - 创建 IM 群组
   - 加载聊天界面
   - 连接 OpenIM 服务器
4. 发送消息测试

#### 在警情接警页使用

1. 进入警情接警页面（新增或编辑）
2. 填写警情基本信息并保存
3. 点击右侧 **"即时聊天"** 标签页
4. 开始沟通

### 3. 多人协作聊天

1. **用户A**: 打开警情详情页 -> 点击"即时聊天"
2. **用户B**: 打开同一警情详情页 -> 点击"即时聊天"
3. 双方可以实时收发消息

### 4. 查看群组成员

查看数据库:
```sql
-- 查看群组信息
SELECT * FROM t_im_group_mapping WHERE report_id = ?;

-- 查看群组成员
SELECT * FROM t_im_group_member WHERE group_id = ?;
```

---

## 📊 即时通讯功能状态

### ✅ 已完成

- [x] 前端聊天界面集成（警情详情页 + 警情接警页）
- [x] 后端 REST API 完整实现
- [x] OpenIM 客户端封装
- [x] 用户自动同步机制
- [x] 群组创建和管理
- [x] 消息发送和接收
- [x] Token 自动刷新
- [x] 熔断器和重试机制
- [x] 完整操作日志

### ⚠️ 待完善（可选）

- [ ] 自动事件监听（需要创建事件类）
- [ ] 消息历史持久化
- [ ] 文件分享功能
- [ ] @提醒功能
- [ ] 消息已读状态
- [ ] 离线消息推送

---

## 🔍 调试和排查

### 检查聊天功能是否正常

#### 1. 检查前端连接

打开浏览器开发者工具（F12），查看 Console:

**正常日志**:
```
🔌 [OpenIM] SDK 初始化中...
✅ [OpenIM] SDK 初始化成功
🔐 [OpenIM] 用户登录中...
✅ [OpenIM] 用户登录成功
📤 [OpenIM] 发送消息...
✅ [OpenIM] 消息发送成功
```

**错误日志**:
```
❌ [OpenIM] 连接失败: Connection refused
❌ [OpenIM] Token获取失败
```

#### 2. 检查后端 API

**测试用户同步**:
```bash
curl http://localhost:1024/im/user/sync/1
```

**测试创建群组**:
```bash
curl -X POST http://localhost:1024/im/group/create/1
```

**检查 Token 状态**:
```bash
curl http://localhost:1024/im/config/token-cache
```

#### 3. 检查数据库

**查看用户同步状态**:
```sql
SELECT
  COUNT(*) as total,
  SUM(CASE WHEN sync_status = 1 THEN 1 ELSE 0 END) as success,
  SUM(CASE WHEN sync_status = 0 THEN 1 ELSE 0 END) as failed
FROM t_im_user_mapping;
```

**查看群组创建记录**:
```sql
SELECT
  g.report_id,
  g.group_id,
  g.group_name,
  COUNT(m.member_id) as member_count
FROM t_im_group_mapping g
LEFT JOIN t_im_group_member m ON g.group_id = m.group_id
GROUP BY g.report_id, g.group_id, g.group_name;
```

### 常见问题

#### 问题 1: 点击"即时聊天"标签页后无内容

**可能原因**:
- OpenIM 服务未启动
- 用户未同步到 OpenIM
- 网络连接问题

**解决方案**:
1. 检查 OpenIM 服务: `docker-compose ps`
2. 同步当前用户: `POST /im/user/sync/{employeeId}`
3. 查看浏览器 Console 错误信息

#### 问题 2: 消息发送失败

**可能原因**:
- WebSocket 连接断开
- Token 过期
- 群组不存在

**解决方案**:
1. 刷新页面重新连接
2. 检查 Token 缓存: `GET /im/config/token-cache`
3. 手动创建群组: `POST /im/group/create/{reportId}`

#### 问题 3: 看不到其他人的消息

**可能原因**:
- 其他用户未加入群组
- WebSocket 连接问题
- 浏览器缓存问题

**解决方案**:
1. 查看群组成员: `SELECT * FROM t_im_group_member WHERE group_id = ?`
2. 手动添加成员: `POST /im/group/add-members`
3. 清除浏览器缓存并刷新

---

## 💡 使用技巧

### 1. 快速切换视图

- 在警情详情页，可以快速在"基本信息"、"操作历史"、"即时聊天"之间切换
- 使用键盘快捷键（如果有配置）

### 2. 协作沟通

- 在录入警情时，打开聊天面板与同事讨论
- 使用聊天记录作为沟通历史参考

### 3. 性能优化

- 聊天界面会自动连接到 OpenIM
- 切换到其他标签页时，WebSocket 连接保持
- 刷新页面会重新建立连接

### 4. 数据同步

- 新员工入职时，手动调用同步接口
- 定期执行增量同步，确保用户数据最新

---

## 📞 技术支持

### 查看完整文档

- **快速启动**: `OPENIM_QUICK_START.md`
- **完整文档**: `OPENIM_INTEGRATION_COMPLETE.md`

### 常用命令

**查看后端日志**:
```bash
tail -f logs/smart_admin_v3/sa-admin/dev/smart-admin.log | grep -i "openim\|IM"
```

**重启 OpenIM 服务**:
```bash
cd open-im-server
docker-compose restart
```

**清除数据重新开始**:
```sql
TRUNCATE TABLE t_im_user_mapping;
TRUNCATE TABLE t_im_group_mapping;
TRUNCATE TABLE t_im_group_member;
TRUNCATE TABLE t_im_operation_log;
```

---

## 📝 更新日志

### 2025-10-09

**新增功能**:
- ✅ 在警情详情页添加"即时聊天"入口
- ✅ 修复事件监听器日志污染问题

**位置变更**:
- ✨ 警情详情页: 添加"即时聊天"标签页（操作历史旁边）
- ✨ 警情接警页: 已有"即时聊天"标签页（专业信息旁边）

**Bug 修复**:
- 🐛 禁用未实现的事件监听器，避免日志污染
- 🐛 注释掉占位符事件监听方法

**文件修改**:
- `police-report-detail.vue` - 添加即时聊天标签页
- `IMEventListenerService.java` - 注释事件监听器

---

**文档更新时间**: 2025-10-09
**SmartAdmin 版本**: v3.28.0
**OpenIM SDK**: @openim/wasm-client-sdk v3.8.3-patch.10
