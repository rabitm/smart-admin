# OpenIM 会话类型修复 - SuperGroup vs Group

## 修复状态

✅ **会话类型修复完成** - ChatPanel.vue:194 使用正确的 `sessionType: 3` (超级群组)
⏳ **等待验证** - 需要刷新浏览器并测试历史消息加载

---

## 问题分析

### 症状

```
📡 [聊天面板] 会话ID: undefined
📥 [聊天面板] 加载历史消息, conversationID: undefined
✅ [OpenIM] 获取历史消息成功, 数量: 0
```

### 根本原因

**OpenIM SDK v3.x 有两种群组类型**:

1. **Group (群组)** - `sessionType: 2`, conversationID格式: `g_<groupID>`
2. **SuperGroup (超级群组)** - `sessionType: 3`, conversationID格式: `sg_<groupID>`

**我们的错误**:
- 后端创建的是 **SuperGroup** (`groupType: 2`)
- SDK在数据库中存储的会话是 `sg_group_report_5`
- 但前端使用 `sessionType: 2` 调用 `getConversation`
- SDK返回的是新创建的普通群组会话 `g_group_report_5`
- **两个不同的会话!历史消息在 `sg_` 中,查询的却是 `g_`**

### SDK日志证据

```javascript
// 前端调用 getConversation(props.groupId, 2)
SDK => run getOneConversation with args ["xxx", 2, "group_report_5"]

// SDK返回的是普通群组会话（注意conversationID是 g_ 开头）
{
  "conversationID":"g_group_report_5",  // ❌ 错误!应该是 sg_
  "conversationType":2,                 // 普通群组
  "groupID":"group_report_5",
  ...
}

// 但数据库中实际存在的会话是超级群组
SDK => getAllConversations return
{
  "conversationID":"sg_group_report_5",  // ✅ 正确的超级群组会话
  "conversationType":3,                  // 超级群组
  "groupID":"group_report_5",
  ...
}
```

---

## 修复详情

### 文件: ChatPanel.vue

**位置**: `smart-admin-web-typescript/src/views/business/oa/police/components/ChatPanel.vue:194`

**修改前 (错误)**:
```typescript
// 3. 获取群组会话（让SDK返回正确的会话ID）
console.log('📡 [聊天面板] 获取群组会话, groupId:', props.groupId);
const conversation = await openIMClient.getConversation(props.groupId, 2); // ❌ 2表示群聊
conversationID.value = conversation.conversationID;
console.log('📡 [聊天面板] 会话ID:', conversationID.value);
```

**修改后 (正确)**:
```typescript
// 3. 获取群组会话（让SDK返回正确的会话ID）
console.log('📡 [聊天面板] 获取群组会话, groupId:', props.groupId);
const conversation = await openIMClient.getConversation(props.groupId, 3); // ✅ 3表示超级群组
conversationID.value = conversation.conversationID;
console.log('📡 [聊天面板] 会话ID:', conversationID.value);
```

**关键改动**: `sessionType: 2` → `sessionType: 3`

---

## OpenIM 群组类型对照表

| 类型 | sessionType | conversationType | groupType | conversationID格式 | 用途 |
|------|-------------|------------------|-----------|-------------------|------|
| **单聊** | 1 | 1 | - | `si_<recvID>_<sendID>` | 一对一聊天 |
| **Group 群组** | 2 | 2 | 0 | `g_<groupID>` | 普通群组 |
| **SuperGroup 超级群组** | 3 | 3 | 2 | `sg_<groupID>` | 大型群组,支持更多成员 |
| **Notification 通知** | 4 | 4 | - | `sn_<groupID>` | 系统通知 |

### 我们的配置

**后端创建群组** (`IMGroupSyncService.java`):
```java
requestBody.put("groupType", 2);  // ← groupType=2 表示SuperGroup
```

**前端调用** (修复后):
```typescript
await openIMClient.getConversation(props.groupId, 3);  // ← sessionType=3 表示SuperGroup
```

**一致性**: 后端 `groupType: 2` → 前端 `sessionType: 3` ✅

---

## 验证步骤

### 步骤 1: 硬刷新浏览器

```bash
# Windows/Linux
Ctrl + Shift + R

# Mac
Cmd + Shift + R
```

### 步骤 2: 访问警情详情页

```
http://localhost:8081/oa/police/report-detail?reportId=5
```

切换到 **"即时聊天"** Tab

### 步骤 3: 检查前端控制台

#### ✅ 预期成功日志

```
📡 [聊天面板] 初始化, reportId: 5, groupId: group_report_5
📡 [聊天面板] 当前用户ID: emp_cf1e361fd46741f5b2a09335cef50db8
📡 [聊天面板] 获取群组会话, groupId: group_report_5
SDK => run getOneConversation with args ["xxx", 3, "group_report_5"]  // ← sessionType=3
📡 [聊天面板] 会话ID: sg_group_report_5  // ✅ 正确!是 sg_ 开头
📥 [聊天面板] 加载历史消息, conversationID: sg_group_report_5
SDK => run getAdvancedHistoryMessageList with args ["xxx", "sg_group_report_5", ...]
✅ [OpenIM] 获取历史消息成功, 数量: X  // ← 应该 > 0
✅ [聊天面板] 历史消息加载成功, 数量: X
```

#### ❌ 不应该再出现

```
📡 [聊天面板] 会话ID: undefined  // ❌
📡 [聊天面板] 会话ID: g_group_report_5  // ❌ 错误的会话类型
SDK => no conversation with id g_group_report_5  // ❌
```

### 步骤 4: 验证UI

- ✅ 聊天面板显示历史消息
- ✅ 消息按时间顺序排列
- ✅ 发送者昵称和头像正确
- ✅ 可以发送新消息
- ✅ 新消息出现在历史记录中

---

## 技术细节

### OpenIM SDK v3.x 群组架构

#### 普通群组 (Group)
- **适用场景**: 小型群组(100人以下)
- **会话格式**: `g_<groupID>`
- **创建方式**: `groupType: 0`
- **性能特点**: 轻量级,所有成员信息同步到客户端

#### 超级群组 (SuperGroup)
- **适用场景**: 大型群组(支持上千人)
- **会话格式**: `sg_<groupID>`
- **创建方式**: `groupType: 2`
- **性能特点**: 高性能,成员信息按需加载

### 为什么使用SuperGroup?

警情协作群组选择SuperGroup的原因:

1. **扩展性**: 支持更多成员(应急指挥可能涉及多个部门)
2. **性能**: 消息分发更高效
3. **功能**: 支持更多高级特性(消息撤回、@提及等)

---

## 相关代码位置

### 后端群组创建

**文件**: `IMGroupSyncService.java:163-228`

```java
/**
 * 在OpenIM服务器上创建群组
 */
private void createGroupOnOpenIM(Long reportId, Map<String, Object> requestBody) {
    // ...
    requestBody.put("groupType", 2);  // ← SuperGroup
    // ...
}
```

### 前端会话获取

**文件**: `ChatPanel.vue:194`

```typescript
const conversation = await openIMClient.getConversation(props.groupId, 3); // ← SuperGroup
```

### SDK封装

**文件**: `openim-client.ts:69-79`

```typescript
async getConversation(sourceID: string, sessionType: number): Promise<ConversationItem> {
  try {
    return await this.sdk.getOneConversation({
      sourceID,
      sessionType,  // 必须传入正确的 sessionType
    });
  } catch (error) {
    console.error('❌ [OpenIM] 获取会话失败:', error);
    throw error;
  }
}
```

---

## 常见问题

### Q1: 为什么会话ID会是 undefined?

**原因**:
```typescript
const conversation = await openIMClient.getConversation(props.groupId, 2);
conversationID.value = conversation.conversationID;  // ← 可能为 undefined
```

当 `sessionType` 不匹配时,SDK可能返回一个不完整的会话对象。

**解决**: 使用正确的 `sessionType: 3`

### Q2: 如何判断应该使用哪个sessionType?

**检查后端创建群组时的 groupType**:

```java
// 后端代码
requestBody.put("groupType", 2);  // ← groupType

// 对照表
groupType: 0 → sessionType: 2 (Group)
groupType: 2 → sessionType: 3 (SuperGroup)
```

### Q3: 已经创建了错误类型的会话怎么办?

**方案A**: 删除旧会话,重新获取

```typescript
// SDK会自动创建正确类型的会话
const conversation = await openIMClient.getConversation(props.groupId, 3);
```

**方案B**: 清除SDK本地缓存

```bash
# 清除浏览器 IndexedDB 中的 OpenIM 数据
# 开发者工具 -> Application -> IndexedDB -> 删除相关数据库
```

### Q4: 为什么消息发送成功但历史记录为空?

**诊断步骤**:

1. 检查发送消息时使用的会话ID
2. 检查历史消息查询时使用的会话ID
3. 确认两者是否一致

```typescript
// 发送消息
await openIMClient.sendGroupTextMessage(props.groupId, text);
// ↑ 内部会用 groupID 获取会话

// 查询历史
await openIMClient.getHistoryMessages(conversationID.value, 50);
// ↑ 必须使用相同的 conversationID
```

---

## 测试清单

### 基础功能测试

- [ ] 刷新页面后,会话ID显示为 `sg_group_report_5`
- [ ] 历史消息正常加载(数量 > 0)
- [ ] 消息按时间排序正确
- [ ] 发送者信息显示正确
- [ ] 可以发送新消息
- [ ] 新消息立即显示在聊天面板
- [ ] 刷新页面后新消息仍在历史记录中

### 多用户协作测试

**两个浏览器窗口**:

- [ ] 用户A发送消息
- [ ] 用户B实时接收
- [ ] 用户B刷新页面,消息仍在历史记录
- [ ] 两个用户看到相同的消息列表

### SDK日志验证

```javascript
// 检查以下关键日志
✅ run getOneConversation with args ["xxx", 3, "group_report_5"]
✅ conversationID: "sg_group_report_5"
✅ run getAdvancedHistoryMessageList with args ["xxx", "sg_group_report_5", ...]
✅ messageList: [{...}, {...}]  // 数组不为空
```

---

## 后续优化

### 1. 类型安全

在 `openim-client.ts` 中添加枚举:

```typescript
export enum SessionType {
  Single = 1,
  Group = 2,
  SuperGroup = 3,
  Notification = 4,
}

// 使用时
await openIMClient.getConversation(props.groupId, SessionType.SuperGroup);
```

### 2. 会话类型验证

添加类型检查函数:

```typescript
function validateConversationType(conversation: ConversationItem, expectedType: number): void {
  if (conversation.conversationType !== expectedType) {
    throw new Error(
      `会话类型不匹配: 期望 ${expectedType}, 实际 ${conversation.conversationType}`
    );
  }
}
```

### 3. 文档完善

更新所有相关文档,明确说明:
- 后端使用 `groupType: 2` (SuperGroup)
- 前端使用 `sessionType: 3` (SuperGroup)
- conversationID格式: `sg_<groupID>`

---

## 相关文档

- [OpenIM用户同步和消息发送修复指南](OPENIM_USER_SYNC_AND_MESSAGING_FIX.md)
- [历史消息加载修复验证指南](HISTORICAL_MESSAGE_FIX_VERIFICATION.md)
- [群组同步修复测试指南](TEST_GROUP_SYNC_FIX.md)

---

**修复负责人**: Claude Code Assistant
**修复时间**: 2025-10-10 17:35
**问题级别**: 🔴 Critical (导致核心功能完全不可用)
**测试状态**: 等待用户刷新浏览器并验证

---

## 总结

这是一个 **类型映射错误**,症状严重但原因明确:

**一句话总结**: 后端创建的是SuperGroup(`groupType:2`),前端必须用`sessionType:3`来获取会话,而不是`sessionType:2`。

**修复方式**: 一行代码改动 - `ChatPanel.vue:194` 将 `2` 改为 `3`。

**影响范围**: 所有警情聊天功能都无法加载历史消息。

**验证方式**: 刷新浏览器,检查 `conversationID` 是否为 `sg_group_report_5` 且历史消息数量 > 0。

**重要性**: ⭐⭐⭐⭐⭐ 核心功能修复,必须验证成功!
