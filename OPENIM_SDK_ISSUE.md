# OpenIM WASM SDK 问题说明和解决方案

## ❌ 当前问题

使用 `@openim/wasm-client-sdk` v3.8.3-patch.10 时出现以下错误：

```
Uncaught (in promise) ReferenceError: Go is not defined
    at initializeWasm (index.es.js:953:5)
    at new SDK (index.es.js:2100:39)
    at getSDK (index.es.js:2171:16)
```

## 🔍 问题原因

OpenIM WASM SDK 需要 Go WASM 运行时支持，但：

1. **复杂的环境依赖**：需要加载 `wasm_exec.js` 和 `.wasm` 文件
2. **浏览器兼容性问题**：不同浏览器对 WASM 的支持不同
3. **构建工具配置**：Vite 需要特殊配置才能正确处理 WASM 资源
4. **性能开销**：WASM 加载需要额外的网络请求和初始化时间

## 💡 解决方案选择

### 方案 A：修复 WASM SDK（复杂，不推荐）

**步骤**：
1. 安装 Go WASM 支持文件
2. 配置 Vite 支持 WASM
3. 手动复制 WASM 文件到 public 目录
4. 修改 HTML 添加 wasm_exec.js

**问题**：
- 配置复杂
- 维护成本高
- 可能影响其他功能

### ⭐ 方案 B：使用 HTTP REST API（推荐）

**优势**：
- ✅ 简单直接，无需复杂配置
- ✅ 与后端紧密集成
- ✅ 易于调试和维护
- ✅ 更好的错误处理
- ✅ 统一的认证机制

**实现思路**：
1. 后端提供消息收发的 REST API
2. 前端通过 HTTP 轮询或 WebSocket 获取新消息
3. 利用现有的 WebSocket 基础设施

### 方案 C：使用 OpenIM HTTP API（中等复杂度）

**说明**：
- 直接调用 OpenIM 的 HTTP API
- 不使用 WASM SDK
- 通过后端代理 OpenIM API

## 🚀 推荐实现：REST API 方案

### 1. 后端 API 设计

```java
@RestController
@RequestMapping("/im/message")
public class IMMessageController {

    /**
     * 发送群组消息
     */
    @PostMapping("/send")
    public ResponseDTO<Void> sendGroupMessage(@RequestBody SendMessageRequest request) {
        // 调用 OpenIM API 发送消息
        openIMClient.sendMessage(request.getGroupId(), request.getContent());
        return ResponseDTO.ok();
    }

    /**
     * 获取群组消息历史
     */
    @GetMapping("/history")
    public ResponseDTO<List<Message>> getMessageHistory(
            @RequestParam String groupId,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String lastMessageId
    ) {
        // 从 OpenIM 获取消息历史
        List<Message> messages = openIMClient.getGroupMessages(groupId, pageSize, lastMessageId);
        return ResponseDTO.ok(messages);
    }

    /**
     * 获取新消息（轮询）
     */
    @GetMapping("/new")
    public ResponseDTO<List<Message>> getNewMessages(
            @RequestParam String groupId,
            @RequestParam Long since  // 时间戳
    ) {
        // 获取指定时间之后的新消息
        List<Message> newMessages = openIMClient.getNewMessages(groupId, since);
        return ResponseDTO.ok(newMessages);
    }
}
```

### 2. 前端实现

```typescript
// im-message-api.ts
export const imMessageApi = {
  /**
   * 发送消息
   */
  sendMessage: (groupId: string, content: string) => {
    return postRequest('/im/message/send', { groupId, content });
  },

  /**
   * 获取消息历史
   */
  getHistory: (groupId: string, pageSize: number = 20, lastMessageId?: string) => {
    return getRequest('/im/message/history', { groupId, pageSize, lastMessageId });
  },

  /**
   * 获取新消息
   */
  getNewMessages: (groupId: string, since: number) => {
    return getRequest('/im/message/new', { groupId, since });
  },
};

// ChatPanel.vue
const pollNewMessages = () => {
  setInterval(async () => {
    const lastTimestamp = messages.value[messages.value.length - 1]?.timestamp || Date.now();
    const newMessages = await imMessageApi.getNewMessages(groupId, lastTimestamp);
    if (newMessages.length > 0) {
      messages.value.push(...newMessages);
    }
  }, 3000); // 每3秒轮询一次
};
```

### 3. 或者使用 WebSocket 推送

```typescript
// 复用现有的 WebSocket 基础设施
unifiedWebSocketClient.onModuleMessage('im', 'NEW_MESSAGE', (message) => {
  messages.value.push(message);
});

// 发送消息
const sendMessage = async (content: string) => {
  await imMessageApi.sendMessage(groupId, content);
  // 后端会通过 WebSocket 广播给所有在线用户
};
```

## 📊 方案对比

| 特性 | WASM SDK | REST API | HTTP API |
|------|----------|----------|----------|
| 实现难度 | ⭐⭐⭐⭐⭐ | ⭐ | ⭐⭐ |
| 维护成本 | 高 | 低 | 中 |
| 性能 | 好 | 中 | 中 |
| 兼容性 | 中 | 优 | 优 |
| 实时性 | 优 | 中（轮询） | 优（WebSocket） |
| 调试难度 | 高 | 低 | 中 |

## 🛠️ 临时解决方案：禁用聊天功能

在修复 WASM SDK 或实现 REST API 之前，可以临时禁用聊天功能：

### 前端

```vue
<!-- ChatPanel.vue -->
<template>
  <div class="chat-panel">
    <a-alert
      type="info"
      message="即时通讯功能开发中"
      description="当前版本暂不支持即时聊天，请使用其他沟通方式。"
      show-icon
    />
  </div>
</template>
```

### 或者隐藏聊天标签页

```vue
<!-- police-report-detail.vue -->
<!-- 暂时注释掉聊天标签页 -->
<!--
<a-tab-pane key="chat" tab="即时聊天">
  <ChatPanel ... />
</a-tab-pane>
-->
```

## ✅ 推荐行动计划

### 短期（立即）

1. **暂时注释聊天标签页**，避免用户看到报错
2. 保留后端 IM API，已经完全可用
3. 文档说明功能正在完善

### 中期（1-2周）

1. **实现 REST API 方案**
   - 后端添加消息收发 API
   - 前端使用 HTTP 轮询或 WebSocket
   - 测试基本聊天功能

2. **优化性能**
   - 使用 WebSocket 实时推送
   - 添加消息缓存
   - 优化轮询间隔

### 长期（1个月+）

1. **研究 WASM SDK 集成**
   - 配置 Vite 支持 WASM
   - 测试浏览器兼容性
   - 完整的功能测试

2. **增强功能**
   - 文件发送
   - 图片预览
   - @提醒
   - 表情支持

## 📝 代码示例：REST API 完整实现

### 后端实现

```java
// IMMessageService.java
@Service
public class IMMessageService {

    @Resource
    private OpenIMClient openIMClient;

    /**
     * 发送群组文本消息
     */
    public void sendGroupTextMessage(String groupId, String content, Long senderId) {
        Map<String, Object> request = new HashMap<>();
        request.put("groupID", groupId);
        request.put("contentType", 101); // 文本消息
        request.put("content", Map.of("text", content));
        request.put("senderID", "SA_EMP_" + senderId);

        openIMClient.post("/msg/send_msg", request, JSONObject.class, IMOperationTypeEnum.MESSAGE_SEND);
    }

    /**
     * 获取群组消息历史
     */
    public List<Message> getGroupMessageHistory(String groupId, int count, String startMsgId) {
        Map<String, Object> request = new HashMap<>();
        request.put("groupID", groupId);
        request.put("count", count);
        request.put("startClientMsgID", startMsgId);

        JSONObject response = openIMClient.post(
            "/msg/get_group_history_messages",
            request,
            JSONObject.class,
            IMOperationTypeEnum.MESSAGE_QUERY
        );

        // 解析消息列表
        return parseMessages(response);
    }
}
```

### 前端实现

```vue
<template>
  <div class="chat-panel">
    <!-- 消息列表 -->
    <div class="message-list" ref="messageList">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message-item"
        :class="{ 'own-message': msg.senderId === currentUserId }"
      >
        <div class="message-content">{{ msg.content }}</div>
        <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
      </div>
    </div>

    <!-- 输入框 -->
    <div class="input-area">
      <a-textarea
        v-model:value="inputText"
        :rows="3"
        placeholder="输入消息..."
        @keyup.enter="handleSendMessage"
      />
      <a-button type="primary" @click="handleSendMessage">发送</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { imMessageApi } from '/@/api/business/oa/im-message-api';

const props = defineProps<{
  groupId: string;
}>();

const messages = ref<Message[]>([]);
const inputText = ref('');
const pollingTimer = ref<number>();

// 加载消息历史
const loadHistory = async () => {
  const result = await imMessageApi.getHistory(props.groupId, 50);
  messages.value = result.data;
};

// 发送消息
const handleSendMessage = async () => {
  if (!inputText.value.trim()) return;

  await imMessageApi.sendMessage(props.groupId, inputText.value);
  inputText.value = '';

  // 立即刷新消息列表
  await loadHistory();
};

// 轮询新消息
const startPolling = () => {
  pollingTimer.value = setInterval(async () => {
    const lastTimestamp = messages.value[messages.value.length - 1]?.timestamp || Date.now();
    const newMessages = await imMessageApi.getNewMessages(props.groupId, lastTimestamp);
    if (newMessages.data.length > 0) {
      messages.value.push(...newMessages.data);
    }
  }, 3000);
};

onMounted(() => {
  loadHistory();
  startPolling();
});

onUnmounted(() => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value);
  }
});
</script>
```

## 🎯 结论

**建议使用 REST API 方案**，原因：

1. ✅ 可以立即开始开发，无需解决 WASM 问题
2. ✅ 与现有架构完美契合
3. ✅ 易于维护和扩展
4. ✅ 更好的用户体验
5. ✅ 生产环境更稳定

WASM SDK 可以作为长期优化目标，但不应阻塞当前功能上线。

---

**文档更新时间**: 2025-10-09
**建议优先级**: 高
**预计工作量**: 2-3 天（REST API 方案）
