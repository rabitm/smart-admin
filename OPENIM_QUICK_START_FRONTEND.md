# OpenIM 前端快速启动指南

## 🚀 5分钟快速上手

### 前置条件

1. ✅ OpenIM Server 已启动（端口 10001, 10002）
2. ✅ SmartAdmin 后端已部署（完成 Phase 1.1-1.5）
3. ✅ 前端依赖已安装（`npm install`）

---

## 📝 基础使用

### 1. 用户登录后自动连接 OpenIM ✅

**无需任何额外代码！** 用户登录 SmartAdmin 后会自动连接 OpenIM。

```typescript
// 这些代码已经集成到登录流程中，无需手动调用

// store/modules/system/user.ts
setUserLoginInfo(data) {
  // ... 设置用户信息 ...

  // 自动初始化 OpenIM 连接
  this.initOpenIMConnection(); // ✅ 已集成
}
```

**查看连接状态**:
```typescript
import { openIMClient } from '/@/utils/openim-client';

// 检查是否已登录
console.log('OpenIM 登录状态:', openIMClient.loggedIn);
console.log('当前用户ID:', openIMClient.userId);
```

---

### 2. 为警情创建群组

```vue
<script setup lang="ts">
import { imBusinessApi } from '/@/api/business/oa/im-business-api';

async function createReportGroup(reportId: number) {
  try {
    // 后端会自动创建群组
    const groupInfo = await imBusinessApi.createGroupForReport(reportId);

    console.log('✅ 群组创建成功:', groupInfo);
    // groupInfo.groupId: "POLICE_GROUP_12345"
    // groupInfo.groupName: "警情-12345"

  } catch (error) {
    console.error('❌ 群组创建失败:', error);
  }
}
</script>
```

---

### 3. 发送消息到群组

```vue
<script setup lang="ts">
import { openIMClient } from '/@/utils/openim-client';
import { imBusinessApi } from '/@/api/business/oa/im-business-api';

async function sendMessage(reportId: number, text: string) {
  try {
    // 1. 获取群组信息
    const groupInfo = await imBusinessApi.getGroupByReportId(reportId);

    // 2. 发送消息（前端直连 OpenIM）
    const message = await openIMClient.sendGroupTextMessage(
      groupInfo.groupId,
      text
    );

    console.log('✅ 消息发送成功:', message);

  } catch (error) {
    console.error('❌ 消息发送失败:', error);
  }
}
</script>
```

---

### 4. 接收实时消息

```vue
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { openIMClient } from '/@/utils/openim-client';
import { imBusinessApi } from '/@/api/business/oa/im-business-api';

const messages = ref<any[]>([]);
let conversationID = '';

// 消息监听器
const handleNewMessage = (message: any) => {
  console.log('📨 收到新消息:', message);
  messages.value.push(message);
};

onMounted(async () => {
  const reportId = 12345; // 从路由参数获取

  // 获取群组信息
  const groupInfo = await imBusinessApi.getGroupByReportId(reportId);

  // 获取会话ID
  const conversation = await openIMClient.getConversation(groupInfo.groupId, 2);
  conversationID = conversation.conversationID;

  // 添加消息监听器
  openIMClient.onMessage(conversationID, handleNewMessage);
});

onUnmounted(() => {
  // 清理监听器
  openIMClient.offMessage(conversationID, handleNewMessage);
});
</script>

<template>
  <div>
    <div v-for="msg in messages" :key="msg.clientMsgID">
      {{ msg.senderNickname }}: {{ msg.content }}
    </div>
  </div>
</template>
```

---

### 5. 邀请成员到群组

```vue
<script setup lang="ts">
import { openIMClient } from '/@/utils/openim-client';
import { imTokenApi } from '/@/api/business/oa/im-token-api';
import { imBusinessApi } from '/@/api/business/oa/im-business-api';

async function inviteMembers(reportId: number, employeeIds: number[]) {
  try {
    // 1. 批量获取员工的 OpenIM 用户 ID
    const userMapping = await imTokenApi.batchGetUserMapping(employeeIds);
    const openimUserIds = Object.values(userMapping);

    // 2. 获取群组信息
    const groupInfo = await imBusinessApi.getGroupByReportId(reportId);

    // 3. 前端直接调用 OpenIM SDK 邀请成员
    await openIMClient.inviteUsersToGroup(
      groupInfo.groupId,
      openimUserIds,
      '警情协作邀请'
    );

    console.log('✅ 成员邀请成功');

  } catch (error) {
    console.error('❌ 成员邀请失败:', error);
  }
}
</script>
```

---

### 6. 加载历史消息

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { openIMClient } from '/@/utils/openim-client';
import { imBusinessApi } from '/@/api/business/oa/im-business-api';

const messages = ref<any[]>([]);

async function loadHistory(reportId: number) {
  try {
    // 获取群组信息
    const groupInfo = await imBusinessApi.getGroupByReportId(reportId);

    // 获取会话
    const conversation = await openIMClient.getConversation(groupInfo.groupId, 2);

    // 加载最近 20 条消息
    const historyMessages = await openIMClient.getHistoryMessages(
      conversation.conversationID,
      20
    );

    messages.value = historyMessages.reverse(); // 反转顺序（最新的在下面）
    console.log('✅ 加载了', historyMessages.length, '条历史消息');

  } catch (error) {
    console.error('❌ 加载历史消息失败:', error);
  }
}

// 加载更多（滚动到顶部时）
async function loadMore() {
  const oldestMessage = messages.value[0];
  if (!oldestMessage) return;

  const moreMessages = await openIMClient.getHistoryMessages(
    conversationID,
    20,
    oldestMessage.clientMsgID
  );

  messages.value.unshift(...moreMessages.reverse());
}
</script>
```

---

## 🔧 完整组件示例

### 警情聊天组件

```vue
<template>
  <div class="police-chat-panel">
    <!-- 消息列表 -->
    <div class="message-list" ref="messageListRef">
      <div
        v-for="msg in messages"
        :key="msg.clientMsgID"
        :class="['message-item', msg.sendID === currentUserId ? 'self' : 'other']"
      >
        <div class="message-avatar">
          <img :src="msg.senderFaceUrl" alt="avatar" />
        </div>
        <div class="message-content">
          <div class="message-sender">{{ msg.senderNickname }}</div>
          <div class="message-text">{{ msg.textElem?.content }}</div>
          <div class="message-time">{{ formatTime(msg.sendTime) }}</div>
        </div>
      </div>
    </div>

    <!-- 消息输入框 -->
    <div class="message-input">
      <a-textarea
        v-model:value="messageText"
        placeholder="输入消息..."
        :rows="3"
        @keydown.enter.prevent="sendMessage"
      />
      <a-button type="primary" @click="sendMessage">发送</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { openIMClient } from '/@/utils/openim-client';
import { imBusinessApi } from '/@/api/business/oa/im-business-api';
import dayjs from 'dayjs';

const props = defineProps<{
  reportId: number;
}>();

const messages = ref<any[]>([]);
const messageText = ref('');
const messageListRef = ref<HTMLElement | null>(null);
let conversationID = '';
let currentUserId = '';

// 格式化时间
const formatTime = (timestamp: number) => {
  return dayjs(timestamp).format('HH:mm:ss');
};

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
    }
  });
};

// 发送消息
const sendMessage = async () => {
  if (!messageText.value.trim()) return;

  try {
    const groupInfo = await imBusinessApi.getGroupByReportId(props.reportId);
    await openIMClient.sendGroupTextMessage(groupInfo.groupId, messageText.value);

    messageText.value = ''; // 清空输入框
    scrollToBottom();

  } catch (error) {
    console.error('❌ 消息发送失败:', error);
  }
};

// 处理新消息
const handleNewMessage = (message: any) => {
  console.log('📨 收到新消息:', message);
  messages.value.push(message);
  scrollToBottom();
};

// 初始化
onMounted(async () => {
  try {
    // 获取当前用户ID
    currentUserId = openIMClient.userId;

    // 获取群组信息
    const groupInfo = await imBusinessApi.getGroupByReportId(props.reportId);

    // 获取会话
    const conversation = await openIMClient.getConversation(groupInfo.groupId, 2);
    conversationID = conversation.conversationID;

    // 加载历史消息
    const historyMessages = await openIMClient.getHistoryMessages(conversationID, 20);
    messages.value = historyMessages.reverse();

    // 添加消息监听器
    openIMClient.onMessage(conversationID, handleNewMessage);

    // 滚动到底部
    scrollToBottom();

  } catch (error) {
    console.error('❌ 初始化聊天面板失败:', error);
  }
});

// 清理
onUnmounted(() => {
  if (conversationID) {
    openIMClient.offMessage(conversationID, handleNewMessage);
  }
});
</script>

<style scoped lang="less">
.police-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;

  .message-list {
    flex: 1;
    overflow-y: auto;
    padding: 16px;

    .message-item {
      display: flex;
      margin-bottom: 16px;

      &.self {
        flex-direction: row-reverse;

        .message-content {
          align-items: flex-end;
          margin-right: 8px;
        }

        .message-text {
          background: #1890ff;
          color: white;
        }
      }

      &.other {
        .message-content {
          margin-left: 8px;
        }
      }
    }

    .message-avatar {
      img {
        width: 40px;
        height: 40px;
        border-radius: 50%;
      }
    }

    .message-content {
      display: flex;
      flex-direction: column;
      max-width: 60%;

      .message-sender {
        font-size: 12px;
        color: #999;
        margin-bottom: 4px;
      }

      .message-text {
        background: #f0f0f0;
        padding: 8px 12px;
        border-radius: 8px;
        word-break: break-word;
      }

      .message-time {
        font-size: 12px;
        color: #999;
        margin-top: 4px;
      }
    }
  }

  .message-input {
    padding: 16px;
    border-top: 1px solid #f0f0f0;

    .ant-btn {
      margin-top: 8px;
    }
  }
}
</style>
```

---

## 🐛 常见问题

### 1. OpenIM 连接失败

**问题**: 控制台显示 "OpenIM 连接初始化失败"

**解决**:
```bash
# 检查 OpenIM Server 状态
curl http://localhost:10002/healthz

# 检查环境变量
# .env.development
VITE_OPENIM_WS_URL=ws://localhost:10001
VITE_OPENIM_API_URL=http://localhost:10002
```

### 2. Token 获取失败

**问题**: 控制台显示 "Token 获取失败"

**解决**:
```typescript
// 检查后端 Token 接口
const response = await fetch('http://localhost:1024/api/im/token/get', {
  method: 'POST',
  headers: {
    'x-access-token': 'your-smartadmin-token'
  }
});

console.log(await response.json());
```

### 3. 消息发送失败

**问题**: 消息发送失败，显示 "未登录OpenIM"

**解决**:
```typescript
// 检查登录状态
console.log('OpenIM 登录状态:', openIMClient.loggedIn);

// 手动重新登录
await openIMClient.loginWithSmartAdmin();
```

---

## 📚 更多文档

- [OpenIM 前端集成指南](./OPENIM_FRONTEND_INTEGRATION_GUIDE.md)
- [Phase 1 完成总结](./OPENIM_PHASE1_FRONTEND_COMPLETE.md)
- [OpenIM 架构重构方案](./OPENIM_ARCHITECTURE_REFACTORING.md)

---

**版本**: v1.0
**日期**: 2025-10-10
**状态**: ✅ 可用
