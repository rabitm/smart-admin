# ChatPanel 事件监听器方法修复

## 修复时间
**2025-10-10**

## 问题描述

### 连续发现的错误

#### 错误 1: `getSelfUserInfo is not a function`
```
TypeError: openIMClient.getSelfUserInfo is not a function
```

#### 错误 2: `on is not a function`
```
TypeError: openIMClient.on is not a function
```

#### 错误 3: `off is not a function`
```
TypeError: openIMClient.off is not a function
```

### 根本原因

我在重构 `ChatPanel.vue` 时，错误地使用了**不存在的方法**！

正确的 OpenIM Client API 应该是：
- ❌ `openIMClient.on()` - 不存在
- ❌ `openIMClient.off()` - 不存在
- ✅ `openIMClient.onMessage(conversationID, listener)` - 正确
- ✅ `openIMClient.offMessage(conversationID, listener)` - 正确

## 解决方案总结

### 修复 1: 获取当前用户ID (Line 184-190)

```typescript
// ❌ 旧代码 - 使用不存在的方法
const userInfo = await openIMClient.getSelfUserInfo();
currentUserID.value = userInfo.userID;

// ✅ 新代码 - 使用正确的属性
if (!openIMClient.isLoggedIn || !openIMClient.currentUserId) {
  throw new Error('OpenIM 未登录，请刷新页面重试');
}

currentUserID.value = openIMClient.currentUserId;
console.log('📡 [聊天面板] 当前用户ID:', currentUserID.value);
```

### 修复 2: 注册消息监听器 (Line 197)

```typescript
// ❌ 旧代码 - 使用不存在的方法
openIMClient.on('onRecvNewMessage', handleNewMessage);

// ✅ 新代码 - 使用正确的方法
openIMClient.onMessage(conversationID.value, handleNewMessage);
```

### 修复 3: 消息处理函数签名 (Line 216)

```typescript
// ❌ 旧代码 - 错误的签名
function handleNewMessage(data: { data: MessageItem }) {
  const messageItem = data.data;
  // ...
}

// ✅ 新代码 - 正确的签名
function handleNewMessage(messageItem: MessageItem) {
  console.log('📨 [聊天面板] 收到新消息:', messageItem);
  // ...
}
```

### 修复 4: 移除消息监听器 (Line 393-394)

```typescript
// ❌ 旧代码 - 使用不存在的方法
if (openIMClient) {
  openIMClient.off('onRecvNewMessage', handleNewMessage);
}

// ✅ 新代码 - 使用正确的方法
if (openIMClient && conversationID.value) {
  openIMClient.offMessage(conversationID.value, handleNewMessage);
}
```

## OpenIM Client API 参考

### 正确的方法和属性

根据 `openim-client.ts` 的实现：

#### 属性
```typescript
openIMClient.currentUserId: string     // 当前登录用户ID
openIMClient.isLoggedIn: boolean       // 登录状态
```

#### 消息监听方法
```typescript
// 添加监听器
openIMClient.onMessage(conversationID: string, listener: (message: MessageItem) => void): void

// 移除监听器
openIMClient.offMessage(conversationID: string, listener: Function): void
```

#### 发送消息方法
```typescript
openIMClient.sendTextMessage(conversationID: string, text: string): Promise<any>
openIMClient.sendImageMessage(conversationID: string, file: File): Promise<any>
openIMClient.sendFileMessage(conversationID: string, file: File): Promise<any>
// ... 等等
```

#### 历史消息方法
```typescript
openIMClient.getHistoryMessageList(
  conversationID: string,
  count: number,
  startClientMsgID?: string
): Promise<MessageItem[]>
```

## 工作原理说明

### OpenIM Client 内部实现

`openim-client.ts` 的内部结构：

```typescript
class OpenIMClient {
  private sdk: any;  // OpenIM SDK 实例
  private messageListeners: Map<string, Function[]> = new Map();  // 消息监听器映射

  private setupEventListeners(): void {
    // 内部监听 OpenIM SDK 的事件
    this.sdk.on('onRecvNewMessage', (data: MessageItem) => {
      // 通知所有订阅者
      this.notifyMessageListeners(data);
    });
  }

  // 对外暴露的 API
  onMessage(conversationID: string, listener: (message: MessageItem) => void): void {
    if (!this.messageListeners.has(conversationID)) {
      this.messageListeners.set(conversationID, []);
    }
    this.messageListeners.get(conversationID)!.push(listener);
  }

  private notifyMessageListeners(message: MessageItem): void {
    const conversationID = message.conversationID;
    const listeners = this.messageListeners.get(conversationID);
    if (listeners) {
      listeners.forEach(listener => listener(message));
    }
  }
}
```

### 消息流程

```
OpenIM Server
    ↓ WebSocket
this.sdk.on('onRecvNewMessage')  ← 内部监听
    ↓
notifyMessageListeners(message)
    ↓
根据 conversationID 查找监听器
    ↓
调用所有订阅了该会话的监听器
    ↓
ChatPanel.handleNewMessage(messageItem)  ← 我们的组件接收消息
    ↓
更新 UI 显示新消息
```

## 修复的完整代码

### ChatPanel.vue 完整的初始化流程

```typescript
async function initializeChat() {
  try {
    loading.value = true;

    console.log('📡 [聊天面板] 初始化, reportId:', props.reportId, 'groupId:', props.groupId);

    // 1. 检查 OpenIM 客户端
    if (!openIMClient) {
      throw new Error('OpenIM 客户端未初始化');
    }

    // 2. 检查是否已登录并获取当前用户ID
    if (!openIMClient.isLoggedIn || !openIMClient.currentUserId) {
      throw new Error('OpenIM 未登录，请刷新页面重试');
    }

    currentUserID.value = openIMClient.currentUserId;
    console.log('📡 [聊天面板] 当前用户ID:', currentUserID.value);

    // 3. 构造会话ID (群聊: sg_ + groupID)
    conversationID.value = `sg_${props.groupId}`;
    console.log('📡 [聊天面板] 会话ID:', conversationID.value);

    // 4. 注册消息监听器
    openIMClient.onMessage(conversationID.value, handleNewMessage);

    isSDKReady.value = true;

    // 5. 加载历史消息
    await loadHistoryMessages();

  } catch (error) {
    console.error('❌ [聊天面板] 初始化失败:', error);
    antMessage.error('聊天功能初始化失败: ' + (error as Error).message);
    smartSentry.captureError(error, { tags: { module: 'ChatPanel', action: 'initialize' } });
  } finally {
    loading.value = false;
  }
}

function handleNewMessage(messageItem: MessageItem) {
  console.log('📨 [聊天面板] 收到新消息:', messageItem);

  // 只处理当前会话的消息
  if (messageItem.groupID !== props.groupId) {
    console.log('⚠️ [聊天面板] 消息不属于当前群组,跳过');
    return;
  }

  // 避免重复添加
  const exists = messages.value.some((m) => m.messageId === messageItem.clientMsgID);
  if (exists) {
    console.log('⚠️ [聊天面板] 消息已存在,跳过');
    return;
  }

  // 添加到消息列表
  const message = convertMessageItem(messageItem);
  messages.value.push(message);

  // 如果不是自己发的消息且面板未展开,增加未读数
  if (!message.isSelf && !expanded.value) {
    newMessageCount.value++;
  }

  // 滚动到底部
  if (expanded.value) {
    scrollToBottom();
  }
}

function cleanup() {
  console.log('🧹 [聊天面板] 清理资源');

  // 移除消息监听器
  if (openIMClient && conversationID.value) {
    openIMClient.offMessage(conversationID.value, handleNewMessage);
  }

  isSDKReady.value = false;
}
```

## 文件修改清单

| 文件 | 修改内容 | 行号 | 状态 |
|------|---------|------|------|
| `ChatPanel.vue` | 修复 `getSelfUserInfo()` → 使用 `currentUserId` | 184-190 | ✅ 完成 |
| `ChatPanel.vue` | 修复 `on()` → 使用 `onMessage()` | 197 | ✅ 完成 |
| `ChatPanel.vue` | 修复 `handleNewMessage` 签名 | 216 | ✅ 完成 |
| `ChatPanel.vue` | 修复 `off()` → 使用 `offMessage()` | 393-394 | ✅ 完成 |

## 经验教训

### 1. 不要假设 API 存在

```typescript
// ❌ 错误的假设
// "这个IM库应该有 on/off 方法，就像 EventEmitter 一样"
openIMClient.on('onRecvNewMessage', handler);

// ✅ 正确的做法
// "先查看源代码，确认实际的 API"
openIMClient.onMessage(conversationID, handler);
```

### 2. 阅读源代码是最好的文档

当第三方库文档不清楚时：
1. 直接阅读源代码（`openim-client.ts`）
2. 查看导出的公共方法
3. 理解内部实现逻辑

### 3. TypeScript 类型提示的重要性

如果 `openIMClient` 有明确的类型定义，TypeScript 会在编译时发现这些错误：

```typescript
// 建议添加接口定义
export interface IOpenIMClient {
  currentUserId: string;
  isLoggedIn: boolean;
  onMessage(conversationID: string, listener: (message: MessageItem) => void): void;
  offMessage(conversationID: string, listener: Function): void;
  sendTextMessage(conversationID: string, text: string): Promise<any>;
  getHistoryMessageList(conversationID: string, count: number): Promise<MessageItem[]>;
}

// 使用时会有类型检查
const client: IOpenIMClient = openIMClient;
client.on(); // ❌ TypeScript 编译错误：方法不存在
```

## 测试验证

### 测试步骤

1. **刷新页面**
```bash
Ctrl + Shift + R (硬刷新)
```

2. **访问警情编辑页面**
```
http://localhost:8081/oa/police/emergency-intake?id=5
```

3. **切换到即时聊天Tab**

4. **观察控制台输出**

### 预期成功输出

```
📡 [聊天面板] 初始化, reportId: 5, groupId: sg_1234567890
📡 [聊天面板] 当前用户ID: emp_cf1e361fd46741f5b2a09335cef50db8
📡 [聊天面板] 会话ID: sg_sg_1234567890
📥 [聊天面板] 加载历史消息, conversationID: sg_sg_1234567890
📥 [聊天面板] 获取到历史消息: [...]
✅ [聊天面板] 历史消息加载成功, 数量: 10
```

### 发送消息测试

1. 输入消息内容："测试消息"
2. 点击"发送"按钮
3. 观察控制台输出

**预期输出**:
```
📤 [聊天面板] 发送消息: 测试消息
✅ [聊天面板] 消息发送成功: {...}
📨 [聊天面板] 收到新消息: {...}
```

### 多用户测试

1. 打开两个浏览器窗口
2. 两个窗口都访问同一警情
3. 在窗口A发送消息
4. 窗口B应该实时收到消息

## 潜在问题

### 会话ID双重前缀问题

如前面文档所述，需要检查 `props.groupId` 的格式：

```typescript
// 当前代码
conversationID.value = `sg_${props.groupId}`;

// 如果 props.groupId 已包含 sg_ 前缀，结果会是:
// sg_sg_1234567890  ← 错误

// 建议的修复
conversationID.value = props.groupId.startsWith('sg_')
  ? props.groupId
  : `sg_${props.groupId}`;
```

**待验证**: 检查后端返回的 `groupId` 格式。

---

**修复完成时间**: 2025-10-10
**修复人员**: Claude Code Assistant
**问题类型**: API 方法调用错误（方法不存在）
**修复次数**: 3次（getSelfUserInfo → on/off → offMessage）
**测试状态**: 待用户验证
