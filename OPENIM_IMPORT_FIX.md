# OpenIM Client 导入错误修复

## 错误信息
```
SyntaxError: The requested module '/src/utils/openim-client.ts' does not provide an export named 'getOpenIMClient'
```

## 问题原因

`openim-client.ts` 文件导出的是 **实例** 而不是 **工厂函数**：

```typescript
// openim-client.ts:1052
export const openIMClient = new OpenIMClient();  // ✅ 导出实例
```

但 `ChatPanel.vue` 中错误地导入了不存在的函数：

```typescript
// ❌ 错误的导入
import { getOpenIMClient } from '/@/utils/openim-client';
```

## 修复内容

### 修改文件
`smart-admin-web-typescript/src/views/business/oa/police/components/ChatPanel.vue`

### 修改 1: 导入语句 (Line 122)

```typescript
// ❌ 旧代码
import { getOpenIMClient } from '/@/utils/openim-client';

// ✅ 新代码
import { openIMClient } from '/@/utils/openim-client';
```

### 修改 2: initializeChat 函数 (Lines 180, 185, 193)

```typescript
// ❌ 旧代码
const imClient = getOpenIMClient();
if (!imClient) {
  throw new Error('OpenIM 客户端未初始化');
}
const userInfo = await imClient.getSelfUserInfo();
imClient.on('onRecvNewMessage', handleNewMessage);

// ✅ 新代码
if (!openIMClient) {
  throw new Error('OpenIM 客户端未初始化');
}
const userInfo = await openIMClient.getSelfUserInfo();
openIMClient.on('onRecvNewMessage', handleNewMessage);
```

### 修改 3: loadHistoryMessages 函数 (Lines 268, 274)

```typescript
// ❌ 旧代码
const imClient = getOpenIMClient();
if (!imClient) {
  console.warn('⚠️ [聊天面板] OpenIM 客户端未初始化');
  return;
}
const messageList = await imClient.getHistoryMessageList(conversationID.value, 50);

// ✅ 新代码
if (!openIMClient) {
  console.warn('⚠️ [聊天面板] OpenIM 客户端未初始化');
  return;
}
const messageList = await openIMClient.getHistoryMessageList(conversationID.value, 50);
```

### 修改 4: sendMessage 函数 (Lines 308, 313)

```typescript
// ❌ 旧代码
const imClient = getOpenIMClient();
if (!imClient) {
  throw new Error('OpenIM 客户端未初始化');
}
const result = await imClient.sendTextMessage(conversationID.value, inputText.value.trim());

// ✅ 新代码
if (!openIMClient) {
  throw new Error('OpenIM 客户端未初始化');
}
const result = await openIMClient.sendTextMessage(conversationID.value, inputText.value.trim());
```

### 修改 5: cleanup 函数 (Lines 391-392)

```typescript
// ❌ 旧代码
const imClient = getOpenIMClient();
if (imClient) {
  imClient.off('onRecvNewMessage', handleNewMessage);
}

// ✅ 新代码
if (openIMClient) {
  openIMClient.off('onRecvNewMessage', handleNewMessage);
}
```

## 代码模式对比

### 错误模式
```typescript
import { getOpenIMClient } from '/@/utils/openim-client';

const imClient = getOpenIMClient();  // ❌ getOpenIMClient 不存在
if (imClient) {
  await imClient.sendTextMessage(...);
}
```

### 正确模式
```typescript
import { openIMClient } from '/@/utils/openim-client';

if (openIMClient) {  // ✅ 直接使用单例实例
  await openIMClient.sendTextMessage(...);
}
```

## 为什么使用单例模式？

`openim-client.ts` 使用单例模式的原因：

1. **全局状态共享**: SDK 连接状态、Token、用户信息需要在整个应用中共享
2. **避免重复初始化**: 防止多次创建 SDK 实例导致的资源浪费
3. **简化使用**: 不需要每次都创建新实例或传递实例引用

```typescript
// openim-client.ts 的设计
class OpenIMClient {
  private sdk: any;
  private isInitialized: boolean = false;

  async initialize() {
    if (this.isInitialized) {
      console.log('⚠️ OpenIM 已经初始化，跳过重复初始化');
      return;
    }
    // ... 初始化逻辑 ...
    this.isInitialized = true;
  }
}

// 导出单例实例
export const openIMClient = new OpenIMClient();
```

## 验证修复

### 检查点 1: 编译成功
```bash
cd smart-admin-web-typescript
npm run dev
```

**预期**: 没有 TypeScript 编译错误

### 检查点 2: 页面加载成功
访问 `/oa/police/emergency-intake?id=5`

**预期**:
- 页面正常加载
- 控制台显示：`📡 [聊天面板] 初始化, reportId: 5, groupId: xxx`
- 没有导入错误

### 检查点 3: 聊天功能正常
切换到"即时聊天"Tab

**预期**:
- ChatPanel 组件正常显示
- 状态指示器显示"就绪"
- 能够发送和接收消息

## 总结

这是一个简单的**导入/导出不匹配**错误：

- **根本原因**: 导入了不存在的命名导出 `getOpenIMClient`
- **正确方式**: 导入实际存在的命名导出 `openIMClient`
- **修复范围**: 仅涉及 `ChatPanel.vue` 一个文件
- **修复时间**: < 5 分钟

---

**修复时间**: 2025-10-10
**影响范围**: `ChatPanel.vue`
**测试状态**: 待验证
