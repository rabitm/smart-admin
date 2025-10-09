# OpenIM集成 - 快速测试指南

## 📅 测试日期
2025-10-08

## ✅ 前置条件检查

### 1. OpenIM服务状态
```bash
# 检查OpenIM Docker容器状态
docker ps | findstr openim

# 应该看到以下服务运行中:
# - openim-server (API服务)
# - openim-chat (聊天服务)
# - mongo (MongoDB)
# - redis (Redis)
# - kafka (Kafka - 可选)
```

### 2. 后端服务状态
```bash
# 检查Spring Boot应用是否启动
curl http://localhost:1024/api/health

# 或查看日志
tail -f logs/smart-admin.log
```

### 3. 前端服务状态
```bash
# 启动前端开发服务器
cd smart-admin-web-typescript
npm run dev

# 访问: http://localhost:8081
```

## 🧪 测试流程

### 阶段1: 数据库验证 (5分钟)

#### 1.1 检查IM相关表
```sql
-- 连接MySQL
mysql -u root -p smart_admin

-- 检查表是否创建
SHOW TABLES LIKE 't_im_%';

-- 应该看到3个表:
-- t_im_user_mapping
-- t_im_group_mapping
-- t_im_auto_invite_rule

-- 检查自动拉人规则
SELECT * FROM t_im_auto_invite_rule;

-- 应该看到4条规则:
-- 1. 创建人自动加入 (启用)
-- 2. 处理人自动加入 (启用)
-- 3. 部门成员自动加入 (禁用)
-- 4. 角色成员自动加入 (禁用)
```

### 阶段2: 后端API测试 (10分钟)

#### 2.1 登录获取Token
```bash
curl -X POST http://localhost:1024/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456"
  }'

# 保存返回的token到环境变量
# export TOKEN="YOUR_TOKEN_HERE"
```

#### 2.2 测试IM Token接口
```bash
curl -X GET http://localhost:1024/api/im/auth/token \
  -H "x-access-token: $TOKEN"

# 预期返回:
{
  "code": 1,
  "msg": "success",
  "data": {
    "userToken": "...",
    "userID": "1",
    "expireTime": 1696809600000,
    "apiUrl": "http://localhost:10002",
    "wsUrl": "ws://localhost:10001",
    "platformID": 5
  }
}
```

如果返回错误,检查:
- OpenIM服务是否启动
- 配置文件中的OpenIM地址是否正确
- 用户是否已同步到OpenIM

#### 2.3 创建测试警情
```bash
# 通过SmartAdmin前端UI创建警情
# 或使用API创建
curl -X POST http://localhost:1024/api/oa/police/report \
  -H "x-access-token: $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reportNumber": "JQ202510080001",
    "reportType": 1,
    "reportLevel": 1,
    "reporterName": "张三",
    "reporterPhone": "13800138000",
    "reportTime": "2025-10-08 10:00:00",
    "incidentLocation": "测试地点",
    "description": "这是一个测试警情",
    "status": 1
  }'
```

#### 2.4 验证群组自动创建
```sql
-- 查询是否创建了群组映射
SELECT * FROM t_im_group_mapping WHERE business_type = 'POLICE';

-- 应该看到新创建的群组记录
```

```bash
# 查看后端日志,应该看到类似输出:
# 📱 [警情群组监听器] 收到警情创建事件 - 警情ID: 1, 警情编号: JQ202510080001
# 📱 [警情群组] 警情群组创建成功 - 警情ID: 1, 警情编号: JQ202510080001, GroupID: xxx, 成员数: 1
```

#### 2.5 测试获取警情群组接口
```bash
curl -X GET http://localhost:1024/api/im/group/police/1 \
  -H "x-access-token: $TOKEN"

# 预期返回:
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "groupId": "xxx",
    "businessType": "POLICE",
    "businessId": 1,
    "groupName": "警情 JQ202510080001 - 这是一个测试警情",
    "ownerUserId": "1",
    "memberCount": 1,
    "createTime": "2025-10-08 10:00:00"
  }
}
```

### 阶段3: 前端UI测试 (15分钟)

#### 3.1 登录系统
1. 访问 http://localhost:8081
2. 使用admin/123456登录
3. 检查是否成功登录

#### 3.2 查看警情列表
1. 进入"警情管理" → "警情列表"
2. 找到刚创建的测试警情
3. 点击"查看详情"

#### 3.3 测试IM面板
1. 在警情详情页,点击"群组沟通"Tab
2. 检查IM面板是否显示

**预期状态**:
- ✅ IM面板正常加载
- ✅ 显示连接状态 (应该是"已连接"或"连接中")
- ✅ 显示群组名称和成员数
- ✅ 可以看到输入框

**常见问题排查**:

**问题1: 显示"该警情尚未创建IM群组"**
- 原因: 群组创建失败或还在创建中
- 解决: 检查后端日志,查看是否有错误

**问题2: 显示"加载失败"**
- 原因: API调用失败
- 解决: 打开浏览器开发者工具 → Network,查看API请求是否成功

**问题3: 连接状态一直是"连接中"**
- 原因: OpenIM WebSocket连接失败
- 解决:
  1. 检查OpenIM服务是否启动
  2. 检查WebSocket地址配置
  3. 查看浏览器控制台错误

#### 3.4 测试发送消息

**发送文本消息**:
1. 在输入框输入: "这是一条测试消息"
2. 按Enter发送
3. 检查消息是否出现在列表中

**预期结果**:
- ✅ 消息立即出现在消息列表
- ✅ 消息气泡样式正确 (蓝色背景,右侧对齐)
- ✅ 显示发送状态 (✓)
- ✅ 显示发送时间

**发送图片消息**:
1. 点击图片按钮
2. 选择一张图片 (< 10MB)
3. 等待上传和发送

**预期结果**:
- ✅ 图片消息出现在列表
- ✅ 图片正常显示
- ✅ 可以点击查看大图

**发送文件消息**:
1. 点击文件按钮
2. 选择一个文件 (< 100MB)
3. 等待上传和发送

**预期结果**:
- ✅ 文件消息出现在列表
- ✅ 显示文件名和大小
- ✅ 可以下载文件

#### 3.5 测试消息接收

**多用户测试**:
1. 打开另一个浏览器 (或隐身窗口)
2. 使用另一个账号登录
3. 进入同一个警情详情页
4. 在第一个浏览器发送消息
5. 在第二个浏览器查看是否收到

**预期结果**:
- ✅ 消息实时同步
- ✅ 消息气泡样式正确 (灰色背景,左侧对齐)
- ✅ 显示发送人昵称

### 阶段4: 功能完整性测试 (10分钟)

#### 4.1 测试历史消息加载
1. 发送20条以上消息
2. 刷新页面
3. 检查历史消息是否加载

**预期结果**:
- ✅ 历史消息正确显示
- ✅ 消息顺序正确 (旧的在上,新的在下)
- ✅ 可以滚动到顶部加载更多

#### 4.2 测试连接断开重连
1. 停止OpenIM服务
   ```bash
   docker stop openim-server
   ```
2. 检查连接状态是否变为"未连接"
3. 启动OpenIM服务
   ```bash
   docker start openim-server
   ```
4. 检查是否自动重连

**预期结果**:
- ✅ 断开时状态更新为"未连接"
- ✅ 恢复后自动重连
- ✅ 重连后消息可以正常发送

#### 4.3 测试快捷键
1. 聚焦到输入框
2. 输入文字
3. 按Enter → 应该发送消息
4. 按Ctrl+Enter → 应该换行

**预期结果**:
- ✅ Enter发送消息
- ✅ Ctrl+Enter换行
- ✅ 输入框支持多行

#### 4.4 测试粘贴图片
1. 从电脑复制一张图片
2. 在输入框按Ctrl+V粘贴
3. 检查图片是否发送

**预期结果**:
- ✅ 图片自动发送
- ✅ 图片正常显示

## 📊 测试用例清单

### 后端测试用例

| 测试项 | 测试方法 | 预期结果 | 状态 |
|--------|----------|----------|------|
| IM Token获取 | GET /api/im/auth/token | 返回有效Token | ⬜ |
| 用户自动同步 | 首次获取Token | 创建用户映射 | ⬜ |
| 警情创建 | POST /api/oa/police/report | 创建成功 | ⬜ |
| 群组自动创建 | 监听警情创建事件 | 创建群组映射 | ⬜ |
| 获取警情群组 | GET /api/im/group/police/{id} | 返回群组信息 | ⬜ |
| 邀请成员 | POST /api/im/group/police/invite | 邀请成功 | ⬜ |

### 前端测试用例

| 测试项 | 测试方法 | 预期结果 | 状态 |
|--------|----------|----------|------|
| IM面板加载 | 打开警情详情 | 显示IM面板 | ⬜ |
| IM连接 | 自动连接 | 状态"已连接" | ⬜ |
| 发送文本消息 | 输入+Enter | 消息发送成功 | ⬜ |
| 发送图片消息 | 上传图片 | 图片发送成功 | ⬜ |
| 发送文件消息 | 上传文件 | 文件发送成功 | ⬜ |
| 接收消息 | 其他用户发送 | 实时收到消息 | ⬜ |
| 历史消息加载 | 刷新页面 | 显示历史消息 | ⬜ |
| 滚动加载更多 | 滚动到顶部 | 加载更多消息 | ⬜ |
| 自动重连 | 断开后恢复 | 自动重连成功 | ⬜ |
| 快捷键 | Enter/Ctrl+Enter | 发送/换行 | ⬜ |
| 粘贴图片 | Ctrl+V | 图片发送成功 | ⬜ |

## 🐛 常见问题和解决方案

### 问题1: OpenIM服务连接失败

**现象**:
```
❌ [OpenIM SDK] 连接失败: Connection refused
```

**解决方案**:
```bash
# 1. 检查OpenIM服务状态
docker ps | findstr openim

# 2. 如果没有运行,启动服务
cd ~/openim
docker-compose up -d

# 3. 检查端口是否监听
netstat -an | findstr "10002"
netstat -an | findstr "10001"
```

### 问题2: Token获取失败

**现象**:
```
❌ [IM服务] 获取IM Token失败
```

**解决方案**:
1. 检查后端配置文件`sa-base.yaml`中的OpenIM配置
2. 检查OpenIM管理员凭据是否正确
3. 检查后端日志是否有错误

### 问题3: 群组创建失败

**现象**:
数据库中没有群组映射记录

**解决方案**:
1. 检查后端日志,查找错误信息
2. 确认OpenIM API地址可访问
3. 确认用户已同步到OpenIM
4. 检查自动拉人规则是否启用

### 问题4: 消息发送失败

**现象**:
```
❌ [OpenIM SDK] 发送消息失败
```

**解决方案**:
1. 检查WebSocket连接状态
2. 检查用户Token是否有效
3. 刷新Token重试
4. 检查网络连接

### 问题5: 历史消息不显示

**现象**:
刷新页面后看不到历史消息

**解决方案**:
1. 检查OpenIM服务是否正常
2. 检查群组ID是否正确
3. 打开开发者工具查看API调用
4. 检查SDK初始化是否成功

## ✅ 测试通过标准

### 必须通过的测试
- ✅ 后端编译成功,无错误
- ✅ 前端编译成功,无错误
- ✅ 数据库表创建成功
- ✅ OpenIM服务正常运行
- ✅ 警情创建时自动创建群组
- ✅ IM面板正常显示
- ✅ 消息可以正常发送和接收
- ✅ 历史消息正常加载

### 建议通过的测试
- ⭕ 图片消息发送成功
- ⭕ 文件消息发送成功
- ⭕ 多用户消息同步
- ⭕ 自动重连功能正常
- ⭕ 快捷键功能正常

## 📝 测试报告模板

```markdown
# OpenIM集成测试报告

## 测试信息
- 测试人员: ___________
- 测试日期: 2025-10-08
- 测试环境: 开发环境

## 测试结果
- 后端API测试: ⬜ 通过 ⬜ 失败
- 前端UI测试: ⬜ 通过 ⬜ 失败
- 功能完整性: ⬜ 通过 ⬜ 失败

## 发现的问题
1. 问题描述: ___________
   严重程度: ⬜ 高 ⬜ 中 ⬜ 低
   状态: ⬜ 已修复 ⬜ 待修复

2. 问题描述: ___________
   严重程度: ⬜ 高 ⬜ 中 ⬜ 低
   状态: ⬜ 已修复 ⬜ 待修复

## 总体评价
⬜ 可以上线
⬜ 需要修复后上线
⬜ 不建议上线

## 备注
___________
```

---

**文档版本**: v1.0
**创建日期**: 2025-10-08
**作者**: Claude Code Assistant
**状态**: ✅ 准备测试
