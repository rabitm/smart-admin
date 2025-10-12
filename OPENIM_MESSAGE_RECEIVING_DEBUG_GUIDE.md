# OpenIM消息接收问题调试指南

## 问题描述

### 现象
重启前端服务后,消息无法被接收方看到:
- ✅ 发送方可以成功发送消息
- ✅ WebSocket连接正常 (ping/pong成功)
- ❌ 接收方的ChatPanel不显示消息
- ❌ SDK日志显示 `newMessagesList is empty` 警告

### 时间线
1. **修复前**: 消息可以正常发送和接收,但已读状态不同步
2. **修复已读回执**: 添加了 `markConversationAsRead()` 调用
3. **重启前端**: 用户重启了前端开发服务器
4. **问题出现**: 消息不再显示给接收方

---

## 已添加的调试日志

### 文件: `openim-client.ts` (line 589-616)

在 `notifyMessageListeners()` 方法中添加了详细的调试日志:

```typescript
private notifyMessageListeners(message: MessageItem): void {
  const conversationID = message.conversationID;

  console.log('🔔 [OpenIM] 通知消息监听器:', {
    conversationID,
    groupID: message.groupID,
    clientMsgID: message.clientMsgID,
    sendID: message.sendID,
    registeredConversations: Array.from(this.messageListeners.keys()),
    listenerCount: this.messageListeners.get(conversationID)?.length || 0,
  });

  const listeners = this.messageListeners.get(conversationID);
  if (listeners && listeners.length > 0) {
    console.log(`✅ [OpenIM] 找到 ${listeners.length} 个监听器，开始通知`);
    // ... notify listeners
  } else {
    console.warn('⚠️ [OpenIM] 未找到会话的消息监听器:', conversationID);
    console.warn('⚠️ [OpenIM] 已注册的会话列表:', Array.from(this.messageListeners.keys()));
  }
}
```

这些日志将帮助诊断:
1. SDK的 `onRecvNewMessage` 事件是否被触发
2. 接收到的消息的conversationID是什么
3. 是否有监听器注册到该conversationID
4. 如果没有找到监听器,显示所有已注册的conversationID

---

## 测试步骤

### 准备工作

1. **清除浏览器缓存和刷新**
   ```
   1. 关闭所有相关的浏览器标签
   2. 清除浏览器缓存 (Ctrl + Shift + Delete)
   3. 重新打开浏览器
   ```

2. **确认前端代码已更新**
   ```bash
   # 确认修改已保存
   # 重启前端开发服务器 (如果还没重启)
   cd smart-admin-web-typescript
   npm run dev
   ```

### 测试场景: 消息发送和接收

#### 步骤1: 打开发送方浏览器

1. 打开浏览器A (例如Chrome)
2. 打开开发者工具 (F12)
3. 登录为 **用户A**
4. 进入警情详情页
5. 展开聊天面板
6. **复制控制台的所有日志** (特别是包含 `[OpenIM]` 的日志)

**期望看到的日志**:
```
📡 [聊天面板] 初始化, reportId: xxx, groupId: xxx
📡 [聊天面板] 当前用户ID: emp_xxx
📡 [聊天面板] 获取群组会话, groupId: xxx
📡 [聊天面板] 会话ID: sg_group_report_5
✅ [OpenIM] SDK初始化成功
✅ [OpenIM] 登录成功, UserID: emp_xxx
✅ [OpenIM] 连接成功
```

#### 步骤2: 打开接收方浏览器

1. 打开浏览器B (例如Firefox或Chrome隐身模式)
2. 打开开发者工具 (F12)
3. 登录为 **用户B**
4. 进入同一个警情详情页
5. 展开聊天面板
6. **复制控制台的所有日志**

**期望看到的日志**:
```
📡 [聊天面板] 初始化, reportId: xxx, groupId: xxx
📡 [聊天面板] 当前用户ID: emp_yyy
📡 [聊天面板] 会话ID: sg_group_report_5
✅ [OpenIM] 登录成功, UserID: emp_yyy
✅ [OpenIM] 连接成功
```

#### 步骤3: 发送测试消息

1. 在 **浏览器A (用户A)** 中:
   - 在聊天面板输入框输入: `测试消息1`
   - 点击"发送"按钮
   - **立即复制控制台的所有新日志**

**期望看到的日志**:
```
📤 [聊天面板] 发送消息: 测试消息1
📤 [OpenIM] 发送群组文本消息, groupID: xxx
✅ [OpenIM] 消息创建成功
✅ [OpenIM] 群组消息发送成功
✅ [聊天面板] 消息发送成功
✅ [聊天面板] 消息已添加到列表
```

#### 步骤4: 检查接收方

1. 立即切换到 **浏览器B (用户B)**
2. 观察聊天面板是否显示 `测试消息1`
3. **立即复制控制台的所有新日志**

**如果消息正常接收,期望看到**:
```
📨 [OpenIM] 收到新消息: {...}
🔔 [OpenIM] 通知消息监听器: {
  conversationID: "sg_group_report_5",
  groupID: "xxx",
  clientMsgID: "...",
  sendID: "emp_xxx",
  registeredConversations: ["sg_group_report_5"],
  listenerCount: 1
}
✅ [OpenIM] 找到 1 个监听器，开始通知
📨 [聊天面板] 收到新消息: {...}
✅ [聊天面板] 消息已添加到列表
```

**如果消息未接收,可能看到**:
```
(没有任何日志)

或者:

📨 [OpenIM] 收到新消息: {...}
⚠️ [OpenIM] 未找到会话的消息监听器: sg_group_report_5
⚠️ [OpenIM] 已注册的会话列表: []

或者:

WARN [conversation_msg.go:737] newMessagesList is empty
```

---

## 可能的原因和解决方案

### 原因1: SDK的 `onRecvNewMessage` 事件未触发

**症状**: 接收方控制台没有任何 `📨 [OpenIM] 收到新消息` 日志

**诊断**:
- SDK内部的消息监听器没有被触发
- 可能是WebSocket连接状态问题
- 可能是SDK配置问题

**解决方案**:
1. 检查OpenIM服务器日志,确认消息是否成功推送
2. 检查WebSocket连接状态:
   ```javascript
   // 在接收方浏览器控制台执行
   openIMClient.status
   // 应该返回 'connected'
   ```
3. 尝试重新登录:
   ```javascript
   // 在接收方浏览器控制台执行
   await openIMClient.logout()
   await openIMClient.loginWithSmartAdmin()
   ```

### 原因2: ConversationID不匹配

**症状**:
- 接收方看到 `📨 [OpenIM] 收到新消息`
- 但看到 `⚠️ [OpenIM] 未找到会话的消息监听器`
- `registeredConversations` 和 `conversationID` 不一致

**诊断**:
- 发送方和接收方的conversationID可能不同
- 监听器注册在错误的conversationID上

**解决方案**:
1. 对比发送方和接收方的conversationID:
   ```javascript
   // 在两个浏览器控制台分别执行
   console.log('My conversationID:', conversationID.value)
   ```
2. 检查群组ID是否一致:
   ```javascript
   // 在两个浏览器控制台分别执行
   console.log('My groupID:', props.groupId)
   ```
3. 如果不一致,检查警情详情页面传递的groupId是否正确

### 原因3: 监听器注册失败

**症状**:
- 看到 `registeredConversations: []`
- 监听器没有被注册

**诊断**:
- `initializeChat()` 过程中某个步骤失败
- 监听器注册代码没有执行

**解决方案**:
1. 在ChatPanel.vue的 `initializeChat()` 中添加更多日志:
   ```typescript
   // line 323
   console.log('📡 [DEBUG] 准备注册消息监听器, conversationID:', conversationID.value);
   openIMClient.onMessage(conversationID.value, handleNewMessage);
   console.log('📡 [DEBUG] 消息监听器注册完成');
   ```

### 原因4: `markConversationAsRead()` 干扰

**症状**: 消息接收在添加已读回执功能后停止工作

**诊断**:
- `markConversationAsRead()` 可能影响了SDK的消息处理
- 调用时机不正确

**解决方案**:
1. **临时禁用** `markConversationAsRead()` 调用,测试消息接收是否恢复:
   ```typescript
   // ChatPanel.vue line 598 - 临时注释掉
   async function togglePanel() {
     expanded.value = !expanded.value;

     if (expanded.value) {
       newMessageCount.value = 0;
       await scrollToBottom();

       // 🔧 临时禁用,测试消息接收
       // await markConversationAsRead();
     } else {
       showMemberPanel.value = false;
     }
   }

   // ChatPanel.vue line 379 - 临时注释掉
   async function handleNewMessage(messageItem: MessageItem) {
     // ... existing code ...

     if (expanded.value) {
       await scrollToBottom();

       // 🔧 临时禁用,测试消息接收
       // await markConversationAsRead();
     }
   }
   ```

2. 如果禁用后消息接收恢复,说明 `markConversationAsRead()` 有问题
3. 可能需要调整调用时机或添加条件判断

### 原因5: SDK WASM缓存问题

**症状**: 重启前端后出现问题

**诊断**:
- OpenIM SDK的WASM文件可能被浏览器缓存
- 旧版本的SDK缓存导致行为异常

**解决方案**:
1. 清除浏览器缓存 (包括应用缓存)
2. 强制刷新页面 (Ctrl + Shift + R 或 Cmd + Shift + R)
3. 尝试使用无痕模式/隐私模式测试
4. 检查 `public/openIM.wasm` 文件是否正确

---

## 收集诊断信息

请按照以下步骤收集完整的诊断信息:

### 1. 发送方日志

在发送方浏览器控制台执行:
```javascript
// 显示当前状态
console.log('=== 发送方状态 ===');
console.log('登录状态:', openIMClient.isLoggedIn);
console.log('用户ID:', openIMClient.currentUserId);
console.log('连接状态:', openIMClient.status);
console.log('ConversationID:', conversationID.value);
console.log('GroupID:', props.groupId);
```

然后发送测试消息,复制所有相关日志。

### 2. 接收方日志

在接收方浏览器控制台执行:
```javascript
// 显示当前状态
console.log('=== 接收方状态 ===');
console.log('登录状态:', openIMClient.isLoggedIn);
console.log('用户ID:', openIMClient.currentUserId);
console.log('连接状态:', openIMClient.status);
console.log('ConversationID:', conversationID.value);
console.log('GroupID:', props.groupId);

// 检查消息监听器
console.log('已注册的会话:', Array.from(openIMClient.messageListeners.keys()));
console.log('当前会话的监听器数量:', openIMClient.messageListeners.get(conversationID.value)?.length);
```

### 3. Network标签

在接收方浏览器:
1. 打开开发者工具
2. 切换到 "Network" (网络) 标签
3. 筛选 "WS" (WebSocket)
4. 观察WebSocket连接
5. 点击WebSocket连接,查看 "Messages" (消息) 标签
6. 截图或复制WebSocket消息

### 4. OpenIM服务器日志

如果可以访问OpenIM服务器:
```bash
# 查看OpenIM服务器的消息发送日志
docker logs openim-server --tail=100 -f

# 或者查看特定的日志文件
tail -f /path/to/openim/logs/msg_gateway.log
```

---

## 应急回滚方案

如果问题紧急,可以临时回滚已读回执功能:

### 回滚步骤

1. **注释掉 `markConversationAsRead()` 调用**

   **文件**: `ChatPanel.vue`

   ```typescript
   // line 598 - togglePanel函数
   async function togglePanel() {
     expanded.value = !expanded.value;

     if (expanded.value) {
       newMessageCount.value = 0;
       await scrollToBottom();

       // 🔧 临时禁用已读回执功能
       // await markConversationAsRead();
     } else {
       showMemberPanel.value = false;
     }
   }

   // line 379 - handleNewMessage函数
   async function handleNewMessage(messageItem: MessageItem) {
     // ... existing code ...

     if (expanded.value) {
       await scrollToBottom();

       // 🔧 临时禁用已读回执功能
       // await markConversationAsRead();
     }
   }
   ```

2. **注释掉已读回执监听器注册**

   ```typescript
   // line 326-327 - initializeChat函数
   // 🔧 临时禁用已读回执监听
   // openIMClient.onGroupReadReceiptReceived(handleGroupReadReceipt);
   // openIMClient.onC2CReadReceiptReceived(handleC2CReadReceipt);
   ```

3. **保存并重启前端**
   ```bash
   # 前端会自动热重载
   # 或者手动重启: Ctrl+C 然后 npm run dev
   ```

4. **重新测试消息发送和接收**

5. **如果消息接收恢复**:
   - 说明问题确实与已读回执功能有关
   - 需要重新设计已读回执的调用时机

---

## 后续优化方向

一旦确定了根本原因,可以考虑以下优化:

### 1. 延迟调用 `markConversationAsRead()`

确保消息完全加载后再标记为已读:

```typescript
async function handleNewMessage(messageItem: MessageItem) {
  // ... 添加消息到列表 ...

  if (expanded.value) {
    await scrollToBottom();

    // 延迟500ms再标记为已读,让SDK有时间处理消息
    setTimeout(async () => {
      await markConversationAsRead();
    }, 500);
  }
}
```

### 2. 添加条件判断

只在消息列表不为空时标记为已读:

```typescript
async function markConversationAsRead() {
  // 新增: 检查是否有消息
  if (messages.value.length === 0) {
    console.log('⚠️ [聊天面板] 消息列表为空,跳过标记已读');
    return;
  }

  // ... existing code ...
}
```

### 3. 使用防抖

避免频繁调用 `markConversationAsRead()`:

```typescript
import { debounce } from 'lodash-es';

// 创建防抖版本的标记已读函数
const debouncedMarkAsRead = debounce(async () => {
  await markConversationAsRead();
}, 1000); // 1秒内只执行一次

// 在handleNewMessage中使用
async function handleNewMessage(messageItem: MessageItem) {
  // ... existing code ...

  if (expanded.value) {
    await scrollToBottom();
    debouncedMarkAsRead(); // 使用防抖版本
  }
}
```

---

## 检查清单

在提供日志之前,请确认:

- [ ] 已清除浏览器缓存并强制刷新
- [ ] 已重启前端开发服务器
- [ ] 两个用户都已成功登录
- [ ] 两个用户都已进入相同的警情详情页
- [ ] 两个用户都已展开聊天面板
- [ ] 已复制发送方的完整控制台日志
- [ ] 已复制接收方的完整控制台日志
- [ ] 已检查WebSocket连接状态 (Network标签)
- [ ] 已执行诊断命令并记录结果

---

## 下一步

请按照 **测试步骤** 部分的指示,收集完整的诊断信息:

1. 执行步骤1-4的测试流程
2. 复制发送方和接收方的**完整控制台日志**
3. 执行 **收集诊断信息** 部分的所有命令
4. 将所有日志和截图提供给开发团队

特别注意:
- ✅ 包含 `[OpenIM]` 前缀的所有日志
- ✅ 包含 `[聊天面板]` 前缀的所有日志
- ✅ 任何错误或警告信息
- ✅ WebSocket消息内容 (Network标签)

有了这些信息,我们就能准确定位问题所在。

---

**最后更新**: 2025-10-11
**相关文件**: `ChatPanel.vue`, `openim-client.ts`
**相关修复**: `OPENIM_READ_RECEIPT_FIX.md`, `OPENIM_INCREMENTAL_SYNC_FIX.md`
