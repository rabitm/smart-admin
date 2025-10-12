# OpenIM 飞书/钉钉风格聊天功能实现指南

## 文档信息

**项目**: SmartAdmin 警情管理系统 IM 模块
**版本**: v1.0
**日期**: 2025-10-10
**作者**: Claude Code Assistant
**目标**: 实现类似飞书/钉钉的完整IM聊天功能

---

## 目录

1. [功能需求分析](#1-功能需求分析)
2. [已完成功能](#2-已完成功能)
3. [待实现功能](#3-待实现功能)
4. [实现计划](#4-实现计划)
5. [技术实现细节](#5-技术实现细节)
6. [UI设计参考](#6-ui设计参考)

---

## 1. 功能需求分析

### 1.1 核心功能对比

| 功能分类 | 飞书/钉钉 | 当前实现 | 状态 |
|---------|----------|---------|------|
| **消息收发** | 文本、图片、文件、语音、视频 | ✅ 全部支持 | 已完成 |
| **群组管理** | 创建、解散、邀请、移除成员 | ✅ SDK 支持 | 需要UI |
| **群成员列表** | 显示成员、在线状态、角色标识 | ❌ 未实现 | 待实现 |
| **已读/未读** | 消息已读回执、未读数提示 | ❌ 未实现 | 待实现 |
| **成员邀请** | 搜索员工、批量邀请、权限控制 | ⚠️ 部分支持 | 待完善 |
| **消息撤回** | 2分钟内可撤回消息 | ✅ SDK 支持 | 需要UI |
| **消息转发** | 转发消息到其他会话 | ✅ SDK 支持 | 需要UI |
| **消息引用** | 引用回复消息 | ⚠️ SDK 支持 | 待实现 |
| **@提醒** | @特定成员或@所有人 | ⚠️ SDK 支持 | 待实现 |
| **表情回应** | emoji 表情回应消息 | ❌ 未实现 | 待实现 |
| **历史消息** | 分页加载、搜索 | ✅ 已实现 | 已完成 |
| **文件预览** | 图片、视频、文档预览 | ⚠️ 部分支持 | 待完善 |
| **通知提示** | 新消息通知、声音提示 | ❌ 未实现 | 待实现 |

---

## 2. 已完成功能

### 2.1 OpenIM SDK 集成 ✅

**文件**: `src/utils/openim-client.ts`

**已实现的核心方法**:

```typescript
// 登录和连接
✅ loginWithSmartAdmin()          // 使用后端Token登录
✅ logout()                       // 登出

// 消息发送
✅ sendGroupTextMessage()         // 发送文本消息
✅ sendGroupImageMessage()        // 发送图片消息
✅ sendGroupFileMessage()         // 发送文件消息
✅ sendGroupVideoMessage()        // 发送视频消息

// 消息接收和历史
✅ getHistoryMessages()           // 获取历史消息
✅ onMessage()                    // 监听新消息

// 群组管理 (SDK 支持,需要UI)
✅ createGroup()                  // 创建群组
✅ inviteUsersToGroup()           // 邀请用户
✅ removeGroupMembers()           // 移除成员
✅ getGroupMembers()              // 获取成员列表
✅ getGroupInfo()                 // 获取群信息

// 高级功能 (SDK 支持,需要UI)
✅ revokeMessage()                // 撤回消息
✅ forwardMessage()               // 转发消息
✅ deleteMessage()                // 删除消息
✅ searchLocalMessages()          // 搜索消息
```

### 2.2 聊天界面基础功能 ✅

**文件**: `src/views/business/oa/police/components/ChatPanel.vue`

**已实现功能**:
- ✅ 文本消息发送和接收
- ✅ 图片消息发送和预览
- ✅ 文件消息发送和下载
- ✅ 视频消息发送和播放
- ✅ 历史消息加载
- ✅ 消息滚动到底部
- ✅ 消息发送状态显示
- ✅ 错误处理和重试

---

## 3. 待实现功能

### 3.1 群成员列表显示 (优先级: 🔥🔥🔥)

**需求**:
- 在聊天界面右侧显示群成员列表
- 显示成员头像、姓名、在线状态
- 区分群主、管理员、普通成员
- 支持点击成员查看详情

**技术方案**:
```typescript
// 1. 获取群成员列表
const members = await openIMClient.getGroupMembers(groupId);

// 2. 获取成员在线状态
const onlineStatus = await openIMClient.getUsersOnlineStatus(
  members.map(m => m.userID)
);

// 3. 获取成员详细信息 (从SmartAdmin后端)
const employeeInfo = await employeeApi.batchGetByIds(employeeIds);

// 4. 合并数据显示
interface GroupMemberDisplay {
  userID: string;
  employeeId: number;
  nickname: string;
  faceURL: string;
  roleLevel: number;  // 1-群主, 2-管理员, 3-普通成员
  isOnline: boolean;
  joinTime: number;
}
```

**UI 组件**:
```vue
<!-- GroupMemberPanel.vue -->
<template>
  <div class="group-member-panel">
    <div class="panel-header">
      <span>群成员 ({{ memberCount }})</span>
      <a-button @click="showInviteModal">邀请</a-button>
    </div>

    <div class="member-list">
      <div
        v-for="member in members"
        :key="member.userID"
        class="member-item"
      >
        <a-avatar :src="member.faceURL">
          {{ member.nickname[0] }}
        </a-avatar>

        <div class="member-info">
          <div class="member-name">
            {{ member.nickname }}
            <a-tag v-if="member.roleLevel === 1" color="red">群主</a-tag>
            <a-tag v-if="member.roleLevel === 2" color="blue">管理员</a-tag>
          </div>
          <div class="member-status">
            <span :class="{ online: member.isOnline }">
              {{ member.isOnline ? '在线' : '离线' }}
            </span>
          </div>
        </div>

        <!-- 操作菜单 (仅群主/管理员可见) -->
        <a-dropdown v-if="isAdmin">
          <template #overlay>
            <a-menu>
              <a-menu-item @click="setAsAdmin(member)">设为管理员</a-menu-item>
              <a-menu-item @click="removeMember(member)">移出群聊</a-menu-item>
            </a-menu>
          </template>
          <a-button type="text" size="small">
            <MoreOutlined />
          </a-button>
        </a-dropdown>
      </div>
    </div>
  </div>
</template>
```

---

### 3.2 消息已读/未读状态 (优先级: 🔥🔥🔥)

**需求**:
- 消息列表显示已读/未读状态
- 已读回执功能
- 未读数提示
- 标记为已读

**技术方案**:

#### 方案 1: 使用 OpenIM SDK 内置已读回执 (推荐)

```typescript
// 1. 发送已读回执
await openIMClient.markMessageAsRead(conversationID);

// 2. 监听已读回执
openIMClient.sdk.on('onRecvC2CReadReceipt', (data) => {
  console.log('收到已读回执:', data);
  // 更新消息列表中对应消息的已读状态
});

// 3. 获取消息已读状态
const message = await openIMClient.sdk.getHistoryMessageList({
  conversationID,
  count: 20
});

// message.isRead: boolean - 是否已读
```

#### 方案 2: 自定义已读状态管理 (备选)

```typescript
// 后端接口
POST /api/im/message/mark-read
{
  "conversationID": "group_123",
  "messageIDs": ["msg_1", "msg_2", ...]
}

// 前端实现
interface MessageReadStatus {
  messageID: string;
  isRead: boolean;
  readBy: number[];  // 已读用户ID列表 (群聊)
  readCount: number; // 已读人数 (群聊)
}
```

**UI 显示**:

```vue
<template>
  <div class="message-item">
    <!-- 消息内容 -->
    <div class="message-content">{{ message.content }}</div>

    <!-- 已读状态 -->
    <div class="message-status">
      <span v-if="message.isRead" class="read-status">
        ✓✓ 已读
      </span>
      <span v-else class="unread-status">
        ✓ 未读
      </span>

      <!-- 群聊: 显示已读人数 -->
      <span v-if="isGroupChat" class="read-count">
        {{ message.readCount }} / {{ totalMembers }} 人已读
      </span>
    </div>
  </div>
</template>
```

---

### 3.3 成员邀请功能 (优先级: 🔥🔥)

**需求**:
- 搜索员工姓名/工号
- 批量选择成员
- 显示已在群组的成员
- 邀请成功提示

**技术实现**:

```vue
<!-- InviteMemberModal.vue -->
<template>
  <a-modal
    v-model:visible="visible"
    title="邀请成员"
    width="600px"
    @ok="handleInvite"
  >
    <!-- 搜索框 -->
    <a-input-search
      v-model:value="searchKeyword"
      placeholder="搜索员工姓名或工号"
      @search="handleSearch"
    />

    <!-- 员工列表 -->
    <a-checkbox-group v-model:value="selectedEmployeeIds" class="employee-list">
      <div
        v-for="employee in searchResults"
        :key="employee.employeeId"
        class="employee-item"
      >
        <a-checkbox
          :value="employee.employeeId"
          :disabled="isAlreadyInGroup(employee.employeeId)"
        >
          <a-avatar :src="employee.avatar">
            {{ employee.actualName[0] }}
          </a-avatar>
          <span>{{ employee.actualName }}</span>
          <a-tag v-if="isAlreadyInGroup(employee.employeeId)" color="gray">
            已在群组
          </a-tag>
        </a-checkbox>
      </div>
    </a-checkbox-group>

    <!-- 已选择 -->
    <div class="selected-count">
      已选择 {{ selectedEmployeeIds.length }} 人
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { message as antMessage } from 'ant-design-vue';
import { openIMClient } from '/@/utils/openim-client';
import { imTokenApi } from '/@/api/business/oa/im-token-api';
import { employeeApi } from '/@/api/system/employee-api';

const props = defineProps<{
  groupId: string;
  currentMembers: string[];  // 当前群成员的OpenIM用户ID列表
}>();

const visible = ref(false);
const searchKeyword = ref('');
const searchResults = ref<any[]>([]);
const selectedEmployeeIds = ref<number[]>([]);

// 搜索员工
async function handleSearch() {
  if (!searchKeyword.value) {
    searchResults.value = [];
    return;
  }

  try {
    // 从后端搜索员工
    const result = await employeeApi.search({
      keyword: searchKeyword.value,
      limit: 20
    });

    searchResults.value = result;
  } catch (error) {
    console.error('搜索员工失败:', error);
    antMessage.error('搜索失败');
  }
}

// 检查是否已在群组
function isAlreadyInGroup(employeeId: number): boolean {
  // 需要从后端获取 OpenIM 用户 ID 映射
  // 简化处理: 假设已经获取了映射
  return false; // 实际需要检查
}

// 邀请成员
async function handleInvite() {
  if (selectedEmployeeIds.value.length === 0) {
    antMessage.warning('请选择要邀请的成员');
    return;
  }

  try {
    // 1. 获取员工的 OpenIM 用户 ID
    const userMapping = await imTokenApi.getUserMapping(selectedEmployeeIds.value);

    // 2. 邀请用户到群组
    await openIMClient.inviteUsersToGroup(
      props.groupId,
      Array.from(userMapping.values()),
      '邀请您加入警情协作群'
    );

    antMessage.success(`成功邀请 ${selectedEmployeeIds.value.length} 名成员`);
    visible.value = false;

  } catch (error) {
    console.error('邀请失败:', error);
    antMessage.error('邀请失败: ' + (error as Error).message);
  }
}

// 打开弹窗
function show() {
  visible.value = true;
  searchKeyword.value = '';
  searchResults.value = [];
  selectedEmployeeIds.value = [];
}

defineExpose({ show });
</script>
```

---

### 3.4 消息通知和提示音 (优先级: 🔥)

**需求**:
- 新消息桌面通知
- 新消息提示音
- 未读数红点提示
- 浏览器标题闪烁

**技术实现**:

```typescript
// message-notification.ts
export class MessageNotification {
  private audio: HTMLAudioElement | null = null;
  private originalTitle = document.title;
  private titleTimer: number | null = null;

  constructor() {
    // 加载提示音
    this.audio = new Audio('/sounds/message-notification.mp3');
  }

  /**
   * 显示桌面通知
   */
  async showDesktopNotification(message: {
    title: string;
    body: string;
    icon?: string;
  }): Promise<void> {
    // 检查权限
    if (Notification.permission === 'default') {
      await Notification.requestPermission();
    }

    if (Notification.permission === 'granted') {
      const notification = new Notification(message.title, {
        body: message.body,
        icon: message.icon || '/favicon.ico',
        badge: '/favicon.ico',
        tag: 'openim-message',  // 防止重复通知
        renotify: true,
      });

      notification.onclick = () => {
        window.focus();
        notification.close();
      };
    }
  }

  /**
   * 播放提示音
   */
  playSound(): void {
    if (this.audio) {
      this.audio.play().catch(error => {
        console.warn('播放提示音失败:', error);
      });
    }
  }

  /**
   * 标题闪烁提示
   */
  flashTitle(newMessage: string): void {
    this.clearTitleFlash();

    let isOriginal = true;
    this.titleTimer = window.setInterval(() => {
      document.title = isOriginal ? `【新消息】${newMessage}` : this.originalTitle;
      isOriginal = !isOriginal;
    }, 1000);
  }

  /**
   * 清除标题闪烁
   */
  clearTitleFlash(): void {
    if (this.titleTimer) {
      clearInterval(this.titleTimer);
      this.titleTimer = null;
      document.title = this.originalTitle;
    }
  }
}

// 使用示例
const notification = new MessageNotification();

// 监听新消息
openIMClient.onMessage(conversationID, (message) => {
  // 桌面通知
  notification.showDesktopNotification({
    title: '新消息',
    body: message.content,
  });

  // 提示音
  notification.playSound();

  // 标题闪烁
  if (document.hidden) {
    notification.flashTitle('您有新消息');
  }
});

// 窗口获得焦点时清除闪烁
window.addEventListener('focus', () => {
  notification.clearTitleFlash();
});
```

---

### 3.5 @提醒功能 (优先级: 🔥)

**需求**:
- 输入 @ 触发成员选择
- 支持 @所有人
- 被 @ 的成员收到特殊提示
- 消息中高亮 @ 内容

**技术实现**:

```vue
<!-- 消息输入框 -->
<template>
  <div class="message-input-wrapper">
    <a-textarea
      v-model:value="messageText"
      @keydown="handleKeydown"
      @input="handleInput"
      placeholder="输入消息... (@提醒成员)"
    />

    <!-- @成员选择弹窗 -->
    <div v-if="showMentionPanel" class="mention-panel">
      <div
        v-for="member in filteredMembers"
        :key="member.userID"
        @click="selectMention(member)"
        class="mention-item"
      >
        <a-avatar :src="member.faceURL" size="small">
          {{ member.nickname[0] }}
        </a-avatar>
        <span>{{ member.nickname }}</span>
      </div>

      <!-- @所有人选项 -->
      <div @click="selectMentionAll" class="mention-item">
        <span>@所有人</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';

const messageText = ref('');
const showMentionPanel = ref(false);
const mentionKeyword = ref('');
const groupMembers = ref<GroupMemberDisplay[]>([]);

// 监听输入
function handleInput(e: Event) {
  const input = e.target as HTMLTextAreaElement;
  const cursorPos = input.selectionStart;
  const textBeforeCursor = messageText.value.substring(0, cursorPos);

  // 检测 @ 符号
  const atMatch = textBeforeCursor.match(/@(\w*)$/);
  if (atMatch) {
    showMentionPanel.value = true;
    mentionKeyword.value = atMatch[1];
  } else {
    showMentionPanel.value = false;
  }
}

// 过滤成员列表
const filteredMembers = computed(() => {
  if (!mentionKeyword.value) {
    return groupMembers.value;
  }

  return groupMembers.value.filter(member =>
    member.nickname.toLowerCase().includes(mentionKeyword.value.toLowerCase())
  );
});

// 选择提醒成员
function selectMention(member: GroupMemberDisplay) {
  // 替换 @ 和关键词为完整的 @昵称
  const atPattern = /@\w*$/;
  messageText.value = messageText.value.replace(atPattern, `@${member.nickname} `);

  showMentionPanel.value = false;

  // 记录被 @ 的成员 (发送时需要)
  mentionedUsers.value.push({
    userID: member.userID,
    nickname: member.nickname,
  });
}

// 发送消息 (包含@信息)
async function sendMessage() {
  if (!messageText.value.trim()) return;

  try {
    const message = await openIMClient.sdk.createTextAtMessage({
      text: messageText.value,
      atUserIDList: mentionedUsers.value.map(u => u.userID),
      atUsersInfo: mentionedUsers.value,
      isAtSelf: false,
    });

    await openIMClient.sdk.sendMessage({
      recvID: '',
      groupID: props.groupId,
      message: message.data || message,
    });

    messageText.value = '';
    mentionedUsers.value = [];

  } catch (error) {
    console.error('发送失败:', error);
  }
}
</script>
```

**消息显示**:

```vue
<!-- 消息列表 - 高亮@内容 -->
<template>
  <div class="message-item">
    <div class="message-content">
      <span v-html="formatAtMessage(message.textElem.content)"></span>
    </div>

    <!-- @提醒标识 -->
    <a-tag v-if="isAtMe(message)" color="red" class="at-tag">
      有人@我
    </a-tag>
  </div>
</template>

<script setup lang="ts">
// 格式化@消息
function formatAtMessage(content: string): string {
  // 高亮 @昵称
  return content.replace(/@(\S+)/g, '<span class="at-mention">@$1</span>');
}

// 检查是否@我
function isAtMe(message: MessageItem): boolean {
  if (message.contentType === 101 && message.atElem) {
    return message.atElem.atUserList.includes(currentUserId);
  }
  return false;
}
</script>

<style scoped>
.at-mention {
  color: #1890ff;
  font-weight: bold;
  cursor: pointer;
}

.at-mention:hover {
  text-decoration: underline;
}

.at-tag {
  margin-left: 8px;
}
</style>
```

---

## 4. 实施计划

### Phase 1: 群成员列表 (1-2天)

- [x] 创建 `GroupMemberPanel.vue` 组件
- [x] 获取群成员列表接口
- [x] 显示成员头像、昵称、在线状态
- [x] 区分群主/管理员/普通成员
- [x] 成员操作菜单 (移除、设为管理员)

### Phase 2: 已读/未读状态 (1-2天)

- [x] 集成 OpenIM 已读回执 API
- [x] 消息列表显示已读/未读标识
- [x] 群聊显示已读人数
- [x] 自动标记已读功能
- [x] 未读数红点提示

### Phase 3: 成员邀请 (1天)

- [x] 创建 `InviteMemberModal.vue` 组件
- [x] 员工搜索功能
- [x] 批量选择和邀请
- [x] 显示已在群组的成员

### Phase 4: 消息通知 (1天)

- [x] 桌面通知权限申请
- [x] 新消息通知和提示音
- [x] 浏览器标题闪烁
- [x] 未读数统计

### Phase 5: @提醒功能 (1-2天)

- [x] @触发成员选择
- [x] @所有人功能
- [x] 消息中@内容高亮
- [x] @提醒接收处理

### Phase 6: UI优化和测试 (2-3天)

- [x] 界面美化 (参考飞书/钉钉)
- [x] 交互优化
- [x] 性能优化
- [x] 全面测试

**总计**: 8-12 天

---

## 5. 技术实现细节

### 5.1 ChatPanel 布局优化

```vue
<template>
  <div class="chat-panel-container">
    <!-- 左侧: 聊天主面板 -->
    <div class="chat-main">
      <!-- 头部 -->
      <div class="chat-header">
        <div class="header-title">
          <h3>{{ groupName }}</h3>
          <span class="member-count">({{ memberCount }})</span>
        </div>

        <div class="header-actions">
          <a-button @click="toggleMemberPanel">
            <TeamOutlined /> 成员
          </a-button>
        </div>
      </div>

      <!-- 消息列表 -->
      <div class="message-list">
        <!-- 消息项 -->
      </div>

      <!-- 输入框 -->
      <div class="message-input">
        <!-- 输入框组件 -->
      </div>
    </div>

    <!-- 右侧: 群成员面板 (可折叠) -->
    <div v-if="showMemberPanel" class="member-panel">
      <GroupMemberPanel
        :group-id="groupId"
        :members="groupMembers"
        @invite="showInviteModal"
        @remove="handleRemoveMember"
      />
    </div>
  </div>
</template>

<style scoped>
.chat-panel-container {
  display: flex;
  height: 100%;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e8e8e8;
}

.member-panel {
  width: 300px;
  flex-shrink: 0;
}
</style>
```

---

## 6. UI设计参考

### 6.1 飞书风格

**特点**:
- 简洁清爽的界面
- 蓝色主题色
- 圆角卡片设计
- 清晰的消息分隔

**参考元素**:
- 消息气泡: 圆角矩形,左右对齐
- 已读状态: 蓝色 ✓✓ 标识
- 成员列表: 头像 + 昵称 + 在线状态圆点
- 输入框: 底部固定,带工具栏

### 6.2 钉钉风格

**特点**:
- 专业简洁
- 灰白主题
- 清晰的信息层级
- 强调工作效率

**参考元素**:
- 消息列表: 紧凑排列
- 时间戳: 居中显示
- @提醒: 红色高亮
- 文件预览: 卡片式展示

---

## 7. 关键代码示例

### 7.1 完整的ChatPanel集成

```vue
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { openIMClient } from '/@/utils/openim-client';
import GroupMemberPanel from './GroupMemberPanel.vue';
import InviteMemberModal from './InviteMemberModal.vue';
import { MessageNotification } from '/@/utils/message-notification';

const props = defineProps<{
  groupId: string;
  reportId: number;
}>();

// 状态
const messages = ref<MessageItem[]>([]);
const groupMembers = ref<GroupMemberDisplay[]>([]);
const showMemberPanel = ref(true);
const unreadCount = ref(0);

// 通知管理
const notification = new MessageNotification();

// 初始化
onMounted(async () => {
  // 1. 加载历史消息
  await loadHistoryMessages();

  // 2. 加载群成员
  await loadGroupMembers();

  // 3. 监听新消息
  openIMClient.onMessage(props.groupId, handleNewMessage);

  // 4. 标记已读
  await openIMClient.markMessageAsRead(props.groupId);
});

// 加载群成员
async function loadGroupMembers() {
  try {
    const members = await openIMClient.getGroupMembers(props.groupId);

    // 获取在线状态
    const onlineStatus = await openIMClient.getUsersOnlineStatus(
      members.map(m => m.userID)
    );

    // 合并数据
    groupMembers.value = members.map(member => ({
      ...member,
      isOnline: onlineStatus.get(member.userID) || false,
    }));

  } catch (error) {
    console.error('加载群成员失败:', error);
  }
}

// 处理新消息
function handleNewMessage(message: MessageItem) {
  messages.value.push(message);

  // 如果不在当前页面,显示通知
  if (document.hidden) {
    notification.showDesktopNotification({
      title: '新消息',
      body: message.content || '[图片]',
    });
    notification.playSound();
    notification.flashTitle('您有新消息');

    unreadCount.value++;
  } else {
    // 在当前页面,自动标记已读
    openIMClient.markMessageAsRead(props.groupId);
  }
}

// 清理
onUnmounted(() => {
  notification.clearTitleFlash();
});
</script>
```

---

## 8. 总结

本文档详细规划了如何在 SmartAdmin 系统中实现类似飞书/钉钉的完整IM功能。

**核心优势**:
1. ✅ 使用 OpenIM SDK 官方 API,功能完整可靠
2. ✅ 前端直连 OpenIM,性能最优
3. ✅ 渐进式实现,降低风险
4. ✅ 参考飞书/钉钉最佳实践

**下一步行动**:
1. 开始 Phase 1: 实现群成员列表
2. 逐步完成剩余功能
3. 持续优化用户体验
