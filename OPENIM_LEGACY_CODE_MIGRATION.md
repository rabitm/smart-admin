# OpenIM 旧代码迁移记录

## 📋 迁移说明

本文档记录了从旧的三层代理架构迁移到新的前端直连架构时，对旧代码的处理方式。

**迁移原则**:
1. **不直接删除** - 将旧代码重命名为 `*.deprecated.java`，方便回滚
2. **保留文档** - 在文件头部添加 `@Deprecated` 注解和说明
3. **提供替代方案** - 在注释中说明新的实现方式
4. **逐步移除** - 确认新架构稳定后再彻底删除

---

## 🗑️ 待移除/弃用的文件

### 1. 群组代理控制器

**文件**: `IMGroupController.java`
**原因**: 前端直接调用 OpenIM SDK 进行群组操作
**替代方案**:
- 创建群组: `IMBusinessController.createGroupForReport()` (仅业务触发)
- 邀请成员: 前端直接调用 `openIMSDK.inviteUserToGroup()`
- 移除成员: 前端直接调用 `openIMSDK.kickGroupMember()`
- 解散群组: `IMBusinessController.disbandGroupForReport()` (仅业务触发)

**操作**: 重命名为 `IMGroupController.deprecated.java`

---

### 2. 消息代理控制器

**文件**: `IMMessageController.java`
**原因**: 前端直接调用 OpenIM SDK 进行消息操作
**替代方案**:
- 发送消息: 前端直接调用 `openIMSDK.sendMessage()`
- 查询历史: 前端直接调用 `openIMSDK.getHistoryMessageList()`
- 获取新消息: 前端监听 `onRecvNewMessages` 回调

**操作**: 重命名为 `IMMessageController.deprecated.java`

---

### 3. 群组管理服务 (部分方法)

**文件**: `IMGroupManagementService.java`
**原因**: 邀请/移除成员改为前端直连
**保留方法**:
- ✅ `createGroupForReport()` - 业务创建群组时调用
- ✅ 私有辅助方法 (generateGroupId, buildGroupInfo等)

**弃用方法**:
- ❌ `inviteMembers()` - 改为前端直接调用SDK
- ❌ `kickMembers()` - 改为前端直接调用SDK
- ❌ `disbandGroup()` - 已在 `IMBusinessService` 中重新实现

**操作**: 创建新的简化版服务，旧文件重命名

---

### 4. 消息服务

**文件**: `IMMessageService.java`
**原因**: 前端直接调用 OpenIM SDK 进行消息操作
**替代方案**:
- 发送消息: 前端 `openIMSDK.sendMessage()`
- 查询历史: 前端 `openIMSDK.getHistoryMessageList()`
- 实时接收: 前端监听 SDK 回调

**操作**: 重命名为 `IMMessageService.deprecated.java`

---

### 5. WebSocket 代理处理器

**文件**: `IMWebSocketHandler.java`
**原因**: 前端直接连接 OpenIM WebSocket
**替代方案**: 前端直接与 OpenIM Server 建立 WebSocket 连接

**操作**: 重命名为 `IMWebSocketHandler.deprecated.java`

---

### 6. WebSocket 监听器

**文件**: `IMWebSocketListener.java`
**原因**: 前端直接连接 OpenIM WebSocket
**替代方案**: 前端使用 OpenIM SDK 的事件监听机制

**操作**: 重命名为 `IMWebSocketListener.deprecated.java`

---

### 7. 订阅控制器

**文件**: `IMSubscriptionController.java`
**原因**: 前端直接通过 OpenIM SDK 订阅消息
**替代方案**: 前端使用 SDK 的 `setConversationListener()` 等方法

**操作**: 重命名为 `IMSubscriptionController.deprecated.java`

---

### 8. 用户控制器 (如果存在代理功能)

**文件**: `IMUserController.java`
**原因**: 用户信息查询可能需要保留部分功能
**检查方式**: 查看是否有前端不应直接调用的用户管理功能

**操作**: 待检查后决定

---

## 📦 保留的文件

### 核心服务 (必须保留)

✅ `IMTokenService.java` - Token生成服务
✅ `IMUserSyncService.java` - 用户同步服务
✅ `IMBusinessService.java` - 业务集成服务 (新)
✅ `IMWebhookService.java` - Webhook处理服务 (新)
✅ `IMOperationLogService.java` - 操作日志服务
✅ `IMInviteRuleService.java` - 邀请规则服务
✅ `IMEventListenerService.java` - 事件监听服务

### 核心控制器 (必须保留)

✅ `IMTokenController.java` - Token API
✅ `IMUserMappingController.java` - 用户映射API
✅ `IMBusinessController.java` - 业务集成API (新)
✅ `IMWebhookController.java` - Webhook接收API (新)
✅ `IMConfigController.java` - 配置管理API

### 基础设施 (必须保留)

✅ `OpenIMClient.java` - OpenIM HTTP客户端
✅ `OpenIMTokenManager.java` - Token管理器
✅ `OpenIMConfig.java` - OpenIM配置
✅ 所有 DAO、Entity、Enum、Constant

---

## 🔄 迁移步骤

### Step 1: 备份旧代码
```bash
# 创建备份分支
git checkout -b backup/openim-legacy-code

# 提交当前状态
git add .
git commit -m "备份: OpenIM旧架构代码"

# 切回开发分支
git checkout feature/police-management-system-v3.27.0
```

### Step 2: 重命名旧文件

逐个重命名以下文件为 `*.deprecated.java`:
1. `IMGroupController.java` → `IMGroupController.deprecated.java`
2. `IMMessageController.java` → `IMMessageController.deprecated.java`
3. `IMMessageService.java` → `IMMessageService.deprecated.java`
4. `IMWebSocketHandler.java` → `IMWebSocketHandler.deprecated.java`
5. `IMWebSocketListener.java` → `IMWebSocketListener.deprecated.java`
6. `IMSubscriptionController.java` → `IMSubscriptionController.deprecated.java`

### Step 3: 处理 IMGroupManagementService

由于 `IMBusinessService` 已经重新实现了 `createGroupForReport()`，可以直接弃用旧的 `IMGroupManagementService`：

```bash
mv IMGroupManagementService.java IMGroupManagementService.deprecated.java
```

### Step 4: 更新依赖引用

检查是否有其他服务依赖这些被弃用的类：
```bash
# 搜索引用
grep -r "IMGroupController" --include="*.java"
grep -r "IMMessageController" --include="*.java"
grep -r "IMGroupManagementService" --include="*.java"
```

如果有引用，需要更新为新的服务。

### Step 5: 添加弃用注解

在重命名的文件头部添加:
```java
/**
 * @deprecated 此类已弃用，请使用新的前端直连架构
 *
 * 迁移说明:
 * - 群组操作: 前端直接调用 OpenIM SDK
 * - 业务触发: 使用 IMBusinessService
 *
 * @see IMBusinessService
 * @see IMBusinessController
 */
@Deprecated
public class IMGroupController {
    // ...
}
```

### Step 6: 测试验证

1. 编译项目: `mvn clean compile`
2. 确认没有编译错误
3. 运行单元测试
4. 手动测试新API

### Step 7: 文档更新

更新以下文档:
- README.md - 更新架构说明
- API文档 - 标记旧API为已弃用
- 开发文档 - 添加迁移指南

---

## 📊 迁移前后对比

### 代码量变化

| 类型 | 迁移前 | 迁移后 | 变化 |
|------|--------|--------|------|
| Controller | 7个 | 4个 | ↓ 43% |
| Service | 8个 | 6个 | ↓ 25% |
| 代码行数 | ~3000行 | ~1500行 | ↓ 50% |

### 文件数量

**移除/弃用**: 6-7个文件
**新增**: 7个文件
**保留**: 20+个基础文件

---

## ⚠️ 注意事项

1. **不要立即删除**: 先重命名为 `.deprecated.java`，确认新架构稳定后再删除
2. **保留数据库表**: 所有数据库表保留不变，继续用于数据同步
3. **API向后兼容**: 如果有外部系统调用旧API，需要保留一段时间
4. **逐步迁移**: 先在测试环境验证，再部署到生产环境
5. **回滚计划**: 保留备份分支，如有问题可快速回滚

---

## 🧪 测试检查清单

### 功能测试
- [ ] Token生成和刷新正常
- [ ] 用户映射查询正常
- [ ] 警情创建自动创建群组
- [ ] Webhook事件接收正常
- [ ] 业务操作日志记录正常

### 性能测试
- [ ] 响应时间符合预期 (50-100ms)
- [ ] CPU使用率降低 (~20%)
- [ ] 内存使用降低 (~1GB)
- [ ] 并发支持提升 (500+用户)

### 兼容性测试
- [ ] 现有前端功能不受影响
- [ ] 数据库数据完整性
- [ ] 现有群组正常工作
- [ ] 现有用户映射有效

---

## 📅 时间线

| 日期 | 里程碑 | 状态 |
|------|--------|------|
| 2025-10-10 | Phase 1.1-1.4 后端开发完成 | ✅ 完成 |
| 2025-10-10 | Phase 1.5 开始移除旧代码 | 🔄 进行中 |
| 待定 | Phase 1.6-1.8 前端开发 | ⏳ 待开始 |
| 待定 | Phase 1.9 测试验证 | ⏳ 待开始 |
| 待定 | 生产环境部署 | ⏳ 待开始 |

---

**文档版本**: v1.0
**创建时间**: 2025-10-10
**维护人**: Claude Code Assistant
