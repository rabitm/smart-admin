# ChatPanel 组件 OpenIM SDK 迁移完成报告

## 修复时间
**2025-10-10**

## 问题背景

### 用户报告的错误
```
接口http://127.0.0.1:1024/api/im/message/history?reportId=5&count=50返回
{
  "code": 10001,
  "level": "system",
  "msg": "org.springframework.web.servlet.resource.NoResourceFoundException: No static resource api/im/message/history.",
  "ok": false,
  "data": null
}
```

### 根本原因分析

**问题不是 API 路径缺失 `/api` 前缀，而是：**

1. **后端从未实现消息相关的 API 端点**
   - `/api/im/message/history` - 获取消息历史
   - `/api/im/message/send` - 发送消息
   - `/api/im/message/new` - 获取新消息
   - `/api/im/subscription/*` - 订阅相关接口

2. **架构设计错误**
   - 前端组件 `ChatPanel.vue` 试图通过后端 REST API 来操作 IM 消息
   - 正确的架构应该是：**前端直接使用 OpenIM SDK 进行消息操作**

3. **现有的后端 Controller**
   - ✅ `IMTokenController.java` - Token 管理（正确）
   - ✅ `IMBusinessController.java` - 业务集成（群组管理）
   - ✅ `IMUserMappingController.java` - 用户映射
   - ❌ **没有** `IMMessageController.java` - 消息管理

## 解决方案

### 架构原则

根据 OpenIM 官方架构设计：

```
┌──────────────────────────────────────────────────────────┐
│                       前端应用                             │
│  ┌────────────────┐              ┌───────────────────┐  │
│  │  OpenIM SDK    │◄────────────►│   OpenIM Server   │  │
│  │ (直接通信)      │  WebSocket   │                   │  │
│  └────────────────┘              └───────────────────┘  │
│         ▲                                                │
│         │                                                │
│         │ Token / GroupID                                │
│         ▼                                                │
│  ┌────────────────┐              ┌───────────────────┐  │
│  │  后端 REST API  │◄────────────►│  SmartAdmin 后端  │  │
│  │ (业务逻辑)      │     HTTP     │                   │  │
│  └────────────────┘              └───────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

**职责划分：**
- **前端 OpenIM SDK**: 直接处理所有 IM 消息操作（发送、接收、历史记录）
- **后端 REST API**: 仅处理业务逻辑（群组创建、用户映射、Token 生成）

### 修改内容

#### 文件：`ChatPanel.vue`

**修改摘要：**
- ❌ 移除：`imApi.getMessageHistory()` - 通过后端获取历史消息
- ✅ 使用：`imClient.getHistoryMessageList()` - 直接从 OpenIM SDK 获取
- ❌ 移除：`imApi.sendMessage()` - 通过后端发送消息
- ✅ 使用：`imClient.sendTextMessage()` - 直接通过 OpenIM SDK 发送
- ❌ 移除：`imWebSocketService.subscribeReport()` - 自定义 WebSocket 订阅
- ✅ 使用：`imClient.on('onRecvNewMessage')` - OpenIM SDK 事件监听

#### 关键代码变更

**1. 导入依赖变更**

```typescript
// ❌ 旧代码
import { imApi } from '/@/api/business/oa/im-api';
import { imWebSocketService, type IMMessage } from '/@/services/im-websocket.service';
import { getWebSocketClient } from '/@/utils/websocket-manager';

// ✅ 新代码
import { getOpenIMClient } from '/@/utils/openim-client';
import type { MessageItem, ConversationType } from '@openim/wasm-client-sdk';
```

**2. 组件属性变更**

```typescript
// ❌ 旧代码
const props = defineProps<{
  reportId: number;
  groupId?: string;  // 可选
  groupName?: string;
}>();

// ✅ 新代码
const props = defineProps<{
  reportId: number;
  groupId: string;   // 必须提供（从业务接口获取）
  groupName?: string;
}>();
```

**3. 状态管理变更**

```typescript
// ❌ 旧代码
const wsConnected = ref(false);
const subscribed = ref(false);

// ✅ 新代码
const isSDKReady = ref(false);
const conversationID = ref<string>('');  // sg_${groupId}
const currentUserID = ref<string>('');
```

**4. 初始化流程变更**

```typescript
// ❌ 旧代码
async function initializeChat() {
  // 1. 初始化自定义 WebSocket 服务
  imWebSocketService.initialize();

  // 2. 订阅 WebSocket 消息
  subscribeMessages();

  // 3. 调用后端订阅接口
  await subscribeReportOnBackend();

  // 4. 从后端加载历史消息
  await loadHistoryMessages();
}

// ✅ 新代码
async function initializeChat() {
  // 1. 获取 OpenIM 客户端
  const imClient = getOpenIMClient();

  // 2. 获取当前用户ID
  const userInfo = await imClient.getSelfUserInfo();
  currentUserID.value = userInfo.userID;

  // 3. 构造会话ID (群聊: sg_ + groupID)
  conversationID.value = `sg_${props.groupId}`;

  // 4. 注册消息监听器
  imClient.on('onRecvNewMessage', handleNewMessage);

  isSDKReady.value = true;

  // 5. 直接从 OpenIM 加载历史消息
  await loadHistoryMessages();
}
```

**5. 加载历史消息变更**

```typescript
// ❌ 旧代码
async function loadHistoryMessages() {
  const response = await imApi.getMessageHistory(props.reportId, 50);

  if (response.data && Array.isArray(response.data)) {
    messages.value = response.data;
  }
}

// ✅ 新代码
async function loadHistoryMessages() {
  const imClient = getOpenIMClient();

  // 直接从 OpenIM SDK 获取历史消息
  const messageList = await imClient.getHistoryMessageList(conversationID.value, 50);

  // 转换并排序消息
  messages.value = messageList
    .map(convertMessageItem)
    .sort((a, b) => a.sendTime - b.sendTime);
}
```

**6. 发送消息变更**

```typescript
// ❌ 旧代码
async function sendMessage() {
  const response = await imApi.sendMessage(props.reportId, inputText.value.trim());

  if (response.data) {
    console.log('消息发送成功, messageId:', response.data);
    inputText.value = '';
  }
}

// ✅ 新代码
async function sendMessage() {
  const imClient = getOpenIMClient();

  // 直接使用 OpenIM SDK 发送文本消息
  const result = await imClient.sendTextMessage(conversationID.value, inputText.value.trim());

  console.log('消息发送成功:', result);
  inputText.value = '';

  // SDK 会自动触发 onRecvNewMessage 回调,无需手动添加到列表
}
```

**7. 消息接收变更**

```typescript
// ❌ 旧代码
function handleNewMessage(message: IMMessage) {
  messages.value.push({
    messageId: message.messageId,
    senderId: message.senderId,
    senderName: message.senderName,
    contentType: message.contentType,
    content: message.content,
    sendTime: message.sendTime,
    isSelf: message.isSelf,
  });
}

// ✅ 新代码
function handleNewMessage(data: { data: MessageItem }) {
  const messageItem = data.data;

  // 只处理当前会话的消息
  if (messageItem.groupID !== props.groupId) {
    return;
  }

  // 避免重复添加
  const exists = messages.value.some((m) => m.messageId === messageItem.clientMsgID);
  if (exists) {
    return;
  }

  // 转换并添加到消息列表
  const message = convertMessageItem(messageItem);
  messages.value.push(message);
}
```

**8. 清理资源变更**

```typescript
// ❌ 旧代码
async function cleanup() {
  // 取消自定义 WebSocket 订阅
  imWebSocketService.unsubscribeReport(props.reportId);

  // 取消后端订阅
  await imApi.unsubscribeReport(props.reportId);

  subscribed.value = false;
}

// ✅ 新代码
function cleanup() {
  // 移除 OpenIM SDK 消息监听器
  const imClient = getOpenIMClient();
  if (imClient) {
    imClient.off('onRecvNewMessage', handleNewMessage);
  }

  isSDKReady.value = false;
}
```

## 技术优势

### 修改前的问题

1. **不必要的后端依赖**
   - 每次发送/接收消息都要经过后端 API
   - 增加后端服务器负载
   - 需要维护额外的消息 API 端点

2. **性能问题**
   - HTTP 请求延迟高于 WebSocket 直连
   - 后端需要转发消息到 OpenIM，再返回给前端

3. **架构复杂性**
   - 需要同时维护自定义 WebSocket 和 HTTP API
   - 消息流转路径过长：前端 → 后端 → OpenIM → 后端 → 前端

### 修改后的优势

1. **直接通信**
   - 前端直接与 OpenIM Server 通信
   - 减少消息延迟
   - 降低后端负载

2. **简化架构**
   - 消息流转路径：前端 ↔ OpenIM Server
   - 后端只处理业务逻辑（群组管理、Token 生成）

3. **官方最佳实践**
   - 符合 OpenIM 官方推荐架构
   - 利用 SDK 的完整功能（消息撤回、引用、已读回执等）

## 遗留问题和注意事项

### 1. `im-api.ts` 文件状态

**文件路径**: `smart-admin-web-typescript/src/api/business/oa/im-api.ts`

**状态**: 已废弃，但保留

**原因**:
- 该文件定义的所有消息相关 API 都没有后端实现
- `ChatPanel.vue` 已迁移到使用 OpenIM SDK

**建议**:
- [ ] 删除 `im-api.ts` 中的消息相关方法（`sendMessage`, `getMessageHistory`, `getNewMessages`, `subscribeReport`, `unsubscribeReport`, `getSubscriberCount`）
- [ ] 保留群组管理相关方法（`createGroupForReport`, `inviteMembers`, `kickMembers`, `disbandGroup`, `getGroupInfo`）
- [ ] 或者完全废弃 `im-api.ts`，使用新的 `im-business-api.ts`

### 2. 调用 ChatPanel 组件的方式

**重要**:  组件现在需要 `groupId` 属性（必填）

```vue
<!-- ❌ 旧的调用方式 -->
<ChatPanel :reportId="reportId" />

<!-- ✅ 新的调用方式 -->
<ChatPanel
  :reportId="reportId"
  :groupId="groupId"     <!-- 必须提供！ -->
  :groupName="groupName"
/>
```

**获取 groupId 的方式**:

```typescript
// 方法1: 从业务 API 获取
import { imBusinessApi } from '/@/api/business/oa/im-business-api';

const groupId = ref<string>('');

// 警情详情页加载时获取群组ID
onMounted(async () => {
  try {
    const groupInfo = await imBusinessApi.getGroupByReportId(reportId.value);
    groupId.value = groupInfo.groupId;
  } catch (error) {
    console.error('获取群组ID失败:', error);
    // 如果群组不存在，创建群组
    const newGroupInfo = await imBusinessApi.createGroupForReport(reportId.value);
    groupId.value = newGroupInfo.groupId;
  }
});
```

### 3. 消息格式转换

OpenIM SDK 返回的 `MessageItem` 结构与自定义消息格式不同，需要转换：

```typescript
function convertMessageItem(item: MessageItem): Message {
  return {
    messageId: item.clientMsgID,
    senderId: item.sendID,
    senderName: item.senderNickname || '未知用户',
    senderAvatar: item.senderFaceUrl,
    contentType: item.contentType,
    // 文本消息内容在 content 字段中，需要 JSON 解析
    content: item.contentType === 101 ? JSON.parse(item.content).content : item.content,
    sendTime: item.sendTime,
    isSelf: item.sendID === currentUserID.value,
  };
}
```

### 4. 会话ID格式

OpenIM 要求特定的会话ID格式：
- **单聊**: `si_${receiverUserID}`
- **群聊**: `sg_${groupID}`

确保传入正确的 `groupId`（不包含 `sg_` 前缀），组件内部会自动添加。

## 测试检查清单

- [ ] 前端编译无错误
- [ ] 警情详情页正确传入 `groupId` 参数
- [ ] 聊天面板能够成功初始化（SDK 就绪状态显示"就绪"）
- [ ] 能够加载历史消息
- [ ] 能够发送文本消息
- [ ] 能够实时接收其他用户的消息
- [ ] 消息列表正确显示发送者信息
- [ ] 自己的消息和他人的消息样式区分正确
- [ ] 消息时间戳显示正确
- [ ] 组件卸载时正确清理监听器

## 下一步工作

### 短期任务

1. **更新警情详情页**
   - 确保传入正确的 `groupId` 参数给 `ChatPanel` 组件
   - 处理群组不存在的情况（调用创建群组 API）

2. **清理遗留代码**
   - 审查并删除/重构 `im-api.ts` 中的废弃方法
   - 移除不再使用的 `imWebSocketService` 和 `im-websocket.service.ts`（如果仅用于聊天）

### 长期优化

1. **功能增强**
   - 实现图片上传和发送（使用 `imClient.sendImageMessage()`）
   - 实现文件上传和发送（使用 `imClient.sendFileMessage()`）
   - 实现表情选择器

2. **用户体验优化**
   - 实现消息已读回执
   - 实现消息撤回功能
   - 实现消息引用/回复
   - 实现@成员功能

3. **性能优化**
   - 实现消息分页加载（上拉加载更多历史消息）
   - 实现虚拟滚动（大量消息时）

---

**修复完成时间**: 2025-10-10
**修复人员**: Claude Code Assistant
**架构调整**: 从后端 API 模式迁移到 OpenIM SDK 直连模式
**审核状态**: 待用户验证测试
