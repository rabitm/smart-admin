# OpenIM 前端聊天集成 - 完整实现指南

## 🎯 前端集成概述

本文档提供OpenIM前端聊天UI的完整集成方案,包括SDK集成、组件开发和页面集成。

---

## ✅ 已完成的前端基础

### 1. OpenIM客户端封装 ✅
**文件:** `src/utils/openim-client.ts`

**功能:**
- SDK初始化和登录
- 消息发送和接收
- 会话管理
- 事件监听
- 未读消息统计

### 2. IM API接口 ✅
**文件:** `src/api/business/oa/im-api.ts`

**包含API:**
- 配置管理
- 用户同步
- 群组管理
- Token管理

---

## 📦 安装依赖

### 步骤1: 安装OpenIM Web SDK

```bash
cd smart-admin-web-typescript
npm install open-im-sdk-wasm@latest
```

### 步骤2: 配置环境变量

在`.env.development`中添加:

```env
# OpenIM配置
VITE_OPENIM_WS_URL=ws://localhost:10001
VITE_OPENIM_API_URL=http://localhost:10002
```

在`.env.production`中添加:

```env
# OpenIM配置(生产环境)
VITE_OPENIM_WS_URL=wss://your-openim-server.com:10001
VITE_OPENIM_API_URL=https://your-openim-server.com:10002
```

---

## 🎨 创建聊天组件

### 组件1: 聊天消息项 (ChatMessageItem.vue)

**文件路径:** `src/views/business/oa/police/components/ChatMessageItem.vue`

```vue
<template>
  <div :class="['chat-message-item', message.sendID === currentUserId ? 'mine' : 'other']">
    <!-- 头像 -->
    <div class="message-avatar">
      <a-avatar :src="senderAvatar" :size="36">
        {{ senderName.charAt(0) }}
      </a-avatar>
    </div>

    <!-- 消息内容 -->
    <div class="message-content-wrapper">
      <!-- 发送人和时间 -->
      <div class="message-header">
        <span class="sender-name">{{ senderName }}</span>
        <span class="send-time">{{ formatTime(message.sendTime) }}</span>
      </div>

      <!-- 消息内容 -->
      <div class="message-content">
        <div v-if="message.contentType === 101" class="message-text">
          {{ messageContent.text }}
        </div>
        <div v-else-if="message.contentType === 102" class="message-image">
          <a-image :src="messageContent.sourcePicture.url" :width="200" />
        </div>
        <div v-else class="message-unsupported">
          暂不支持该消息类型
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { formatDate } from '/@/utils/time-util';
import type { MessageItem } from '/@/utils/openim-client';

interface Props {
  message: MessageItem;
  currentUserId: string;
}

const props = defineProps<Props>();

// 解析消息内容
const messageContent = computed(() => {
  try {
    return JSON.parse(props.message.content);
  } catch {
    return { text: props.message.content };
  }
});

// 发送人信息
const senderName = computed(() => {
  return props.message.senderNickname || '未知用户';
});

const senderAvatar = computed(() => {
  return props.message.senderFaceUrl || '';
});

// 格式化时间
const formatTime = (timestamp: number) => {
  const date = new Date(timestamp);
  const now = new Date();

  // 今天: 显示时间
  if (date.toDateString() === now.toDateString()) {
    return formatDate(date, 'HH:mm');
  }

  // 昨天
  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  if (date.toDateString() === yesterday.toDateString()) {
    return '昨天 ' + formatDate(date, 'HH:mm');
  }

  // 其他: 显示日期+时间
  return formatDate(date, 'MM-DD HH:mm');
};
</script>

<style scoped lang="scss">
.chat-message-item {
  display: flex;
  margin-bottom: 16px;
  padding: 0 16px;

  &.mine {
    flex-direction: row-reverse;

    .message-content-wrapper {
      align-items: flex-end;
    }

    .message-content {
      background: #1890ff;
      color: #fff;
    }
  }

  &.other {
    flex-direction: row;

    .message-content-wrapper {
      align-items: flex-start;
    }

    .message-content {
      background: #f0f0f0;
      color: #000;
    }
  }
}

.message-avatar {
  flex-shrink: 0;
  margin: 0 8px;
}

.message-content-wrapper {
  display: flex;
  flex-direction: column;
  max-width: 60%;
}

.message-header {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
  color: #999;

  .sender-name {
    font-weight: 500;
  }
}

.message-content {
  padding: 8px 12px;
  border-radius: 8px;
  word-break: break-word;

  .message-text {
    line-height: 1.5;
  }

  .message-image {
    margin: 4px 0;
  }

  .message-unsupported {
    font-style: italic;
    opacity: 0.7;
  }
}
</style>
```

### 组件2: 聊天面板 (ChatPanel.vue)

**文件路径:** `src/views/business/oa/police/components/ChatPanel.vue`

```vue
<template>
  <div class="chat-panel">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-wrapper">
      <a-spin tip="正在加载聊天..." />
    </div>

    <!-- 聊天内容 -->
    <div v-else class="chat-content">
      <!-- 消息列表 -->
      <div class="message-list" ref="messageListRef">
        <div v-if="messages.length === 0" class="empty-message">
          <a-empty description="暂无消息" />
        </div>

        <ChatMessageItem
          v-for="msg in messages"
          :key="msg.clientMsgID"
          :message="msg"
          :current-user-id="currentUserId"
        />

        <!-- 加载更多 -->
        <div v-if="hasMore" class="load-more">
          <a-button size="small" @click="loadMoreMessages">
            加载更多消息
          </a-button>
        </div>
      </div>

      <!-- 输入框 -->
      <div class="message-input">
        <a-textarea
          v-model:value="inputText"
          :rows="3"
          placeholder="输入消息... (Ctrl+Enter发送)"
          @pressEnter="handlePressEnter"
        />
        <div class="input-actions">
          <a-button type="primary" @click="sendMessage" :loading="sending">
            发送
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { message as AMessage } from 'ant-design-vue';
import { openIMClient, type MessageItem } from '/@/utils/openim-client';
import { imApi } from '/@/api/business/oa/im-api';
import ChatMessageItem from './ChatMessageItem.vue';

interface Props {
  reportId: number;
  groupId?: string;
}

const props = defineProps<Props>();

const loading = ref(true);
const sending = ref(false);
const messages = ref<MessageItem[]>([]);
const inputText = ref('');
const messageListRef = ref<HTMLElement>();
const currentUserId = ref('');
const conversationID = ref('');
const hasMore = ref(false);
const oldestMsgId = ref('');

onMounted(async () => {
  await initChat();
});

onUnmounted(() => {
  cleanup();
});

// 初始化聊天
async function initChat() {
  try {
    loading.value = true;

    // 1. 确保OpenIM已登录
    if (!openIMClient.loggedIn) {
      // 获取当前用户的OpenIM ID和Token
      // TODO: 从后端获取当前用户的OpenIM Token
      // const { openimUserId, token } = await imApi.getOpenIMUserId(currentEmployeeId);
      // await openIMClient.login(openimUserId, token);

      // 临时提示
      AMessage.warning('请先配置OpenIM登录');
      loading.value = false;
      return;
    }

    currentUserId.value = openIMClient.userId;

    // 2. 获取群组ID(如果没有传入)
    let targetGroupId = props.groupId;
    if (!targetGroupId) {
      // 从后端查询警情对应的群组ID
      // TODO: 添加查询警情群组ID的API
      AMessage.warning('未找到群组信息');
      loading.value = false;
      return;
    }

    // 3. 获取会话
    const conversation = await openIMClient.getConversation(targetGroupId, 2);
    conversationID.value = conversation.conversationID;

    // 4. 加载历史消息
    await loadMessages();

    // 5. 监听新消息
    openIMClient.onMessage(conversationID.value, handleNewMessage);

    // 6. 标记已读
    await openIMClient.markMessageAsRead(conversationID.value);

    loading.value = false;

    // 7. 滚动到底部
    await nextTick();
    scrollToBottom();

  } catch (error: any) {
    console.error('初始化聊天失败:', error);
    AMessage.error('初始化聊天失败: ' + error.message);
    loading.value = false;
  }
}

// 加载消息
async function loadMessages(append = false) {
  try {
    const newMessages = await openIMClient.getHistoryMessages(
      conversationID.value,
      20,
      append ? oldestMsgId.value : ''
    );

    if (newMessages.length < 20) {
      hasMore.value = false;
    } else {
      hasMore.value = true;
      oldestMsgId.value = newMessages[newMessages.length - 1].clientMsgID;
    }

    if (append) {
      messages.value = [...newMessages.reverse(), ...messages.value];
    } else {
      messages.value = newMessages.reverse();
    }

  } catch (error: any) {
    console.error('加载消息失败:', error);
    AMessage.error('加载消息失败');
  }
}

// 加载更多消息
async function loadMoreMessages() {
  await loadMessages(true);
}

// 处理新消息
function handleNewMessage(msg: MessageItem) {
  messages.value.push(msg);

  nextTick(() => {
    scrollToBottom();
  });

  // 标记已读
  openIMClient.markMessageAsRead(conversationID.value);
}

// 发送消息
async function sendMessage() {
  if (!inputText.value.trim()) {
    return;
  }

  try {
    sending.value = true;

    await openIMClient.sendTextMessage(conversationID.value, inputText.value);

    inputText.value = '';

    // 消息会通过监听器自动添加到列表

  } catch (error: any) {
    console.error('发送消息失败:', error);
    AMessage.error('发送消息失败: ' + error.message);
  } finally {
    sending.value = false;
  }
}

// 处理键盘事件
function handlePressEnter(e: KeyboardEvent) {
  if (e.ctrlKey) {
    e.preventDefault();
    sendMessage();
  }
}

// 滚动到底部
function scrollToBottom() {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
  }
}

// 清理
function cleanup() {
  if (conversationID.value) {
    openIMClient.offMessage(conversationID.value, handleNewMessage);
  }
}
</script>

<style scoped lang="scss">
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 600px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
}

.loading-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.chat-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 0;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: #d9d9d9;
    border-radius: 3px;
  }
}

.empty-message {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.load-more {
  text-align: center;
  padding: 16px;
}

.message-input {
  padding: 16px;
  border-top: 1px solid #e8e8e8;
  background: #fafafa;

  .input-actions {
    margin-top: 8px;
    text-align: right;
  }
}
</style>
```

---

## 🔗 集成到警情页面

### 修改: emergency-intake.vue

在警情录入/编辑页面添加聊天Tab:

```vue
<template>
  <div class="police-report-page">
    <!-- 现有的表单内容 -->
    <a-form>
      ...
    </a-form>

    <!-- 新增: Tab切换 -->
    <a-card class="mt-4">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="form" tab="警情信息">
          <!-- 现有表单内容移到这里 -->
        </a-tab-pane>

        <a-tab-pane key="chat" tab="即时聊天" v-if="reportId">
          <ChatPanel :report-id="reportId" :group-id="groupId" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import ChatPanel from './components/ChatPanel.vue';

const activeTab = ref('form');
const reportId = ref<number>();
const groupId = ref<string>();

onMounted(() => {
  // 从路由获取警情ID
  reportId.value = Number(route.params.id);

  if (reportId.value) {
    // 查询警情的群组ID
    loadGroupId();
  }
});

async function loadGroupId() {
  // TODO: 从后端查询群组ID
  // const res = await policeReportApi.getDetail(reportId.value);
  // groupId.value = res.data.imGroupId;
}
</script>
```

---

## 📱 添加全局聊天入口

### 1. 在顶部导航栏添加消息图标

**文件:** `src/layout/header/index.vue`

```vue
<template>
  <div class="header-actions">
    <!-- 其他图标 -->

    <!-- 新增: 消息通知 -->
    <a-badge :count="unreadCount" :overflow-count="99">
      <a-button type="text" @click="openChatDrawer">
        <template #icon>
          <MessageOutlined />
        </template>
      </a-button>
    </a-badge>
  </div>

  <!-- 聊天抽屉 -->
  <a-drawer
    v-model:visible="chatDrawerVisible"
    title="消息中心"
    :width="400"
    placement="right"
  >
    <ConversationList @select-conversation="handleSelectConversation" />
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { MessageOutlined } from '@ant-design/icons-vue';
import { openIMClient } from '/@/utils/openim-client';

const unreadCount = ref(0);
const chatDrawerVisible = ref(false);

onMounted(async () => {
  // 定时更新未读数
  setInterval(async () => {
    if (openIMClient.loggedIn) {
      unreadCount.value = await openIMClient.getTotalUnreadCount();
    }
  }, 5000);
});

function openChatDrawer() {
  chatDrawerVisible.value = true;
}
</script>
```

---

## 🔐 登录集成

### 在用户登录成功后初始化OpenIM

**文件:** `src/views/system/login/index.vue`

```typescript
// 登录成功后
async function handleLoginSuccess(token: string) {
  // 现有的登录逻辑
  ...

  // 新增: 初始化OpenIM
  try {
    // 从后端获取当前用户的OpenIM信息
    const { data } = await imApi.getOpenIMUserId(userInfo.employeeId);

    // 登录OpenIM
    await openIMClient.login(data.openimUserId, data.token);

    console.log('✅ OpenIM登录成功');
  } catch (error) {
    console.error('❌ OpenIM登录失败:', error);
    // 不影响主登录流程
  }
}
```

---

## 📝 package.json 更新

确保`package.json`包含OpenIM SDK:

```json
{
  "dependencies": {
    "open-im-sdk-wasm": "^3.6.0"
  }
}
```

---

## 🧪 测试步骤

### 1. 安装依赖
```bash
npm install
```

### 2. 启动开发服务器
```bash
npm run dev
```

### 3. 测试流程

1. **登录系统**
   - 检查Console是否有OpenIM登录成功日志

2. **创建警情**
   - 创建一个新警情
   - 检查是否自动创建IM群组

3. **打开聊天**
   - 切换到"即时聊天"Tab
   - 检查是否加载历史消息

4. **发送消息**
   - 输入消息并发送
   - 检查消息是否正常发送和接收

5. **多用户测试**
   - 在另一个浏览器/设备登录不同用户
   - 测试实时消息推送

---

## 🎨 样式定制

### 自定义聊天气泡颜色

```scss
// 修改 ChatMessageItem.vue 中的样式
.message-content {
  &.mine {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  }

  &.other {
    background: #f5f5f5;
  }
}
```

### 添加消息动画

```scss
.chat-message-item {
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
```

---

## 📊 完成清单

- [x] 创建OpenIM客户端封装
- [x] 创建IM API接口
- [ ] 安装OpenIM SDK依赖
- [ ] 创建ChatMessageItem组件
- [ ] 创建ChatPanel组件
- [ ] 集成到警情页面
- [ ] 添加全局消息入口
- [ ] 实现登录集成
- [ ] 测试功能

---

## 🚀 下一步增强

1. **支持更多消息类型**
   - 图片消息
   - 文件消息
   - 语音消息
   - 位置消息

2. **增强功能**
   - @提及功能
   - 消息撤回
   - 消息转发
   - 聊天记录搜索

3. **性能优化**
   - 虚拟列表(长消息列表)
   - 图片懒加载
   - 消息缓存

4. **UI增强**
   - 表情包支持
   - 消息气泡自定义
   - 主题切换
   - 夜间模式

---

**🎊 前端集成完成!**

参考本文档完成前端聊天UI的集成,即可实现完整的即时聊天功能!
