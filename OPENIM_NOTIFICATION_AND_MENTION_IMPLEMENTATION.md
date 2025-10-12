# OpenIM消息通知与@提及功能实现方案

## 📋 目录
1. [Phase 4: 消息通知功能](#phase-4-消息通知功能)
2. [Phase 5: @提及功能](#phase-5-提及功能)
3. [实施优先级](#实施优先级)
4. [测试方案](#测试方案)

---

## Phase 4: 消息通知功能

### 需求分析

#### 业务场景
- **场景1**: 用户正在查看其他警情,收到新警情的消息通知
- **场景2**: 用户最小化浏览器,收到紧急消息需要桌面通知
- **场景3**: 用户正在编辑警情,收到新消息需要声音提示
- **场景4**: 用户切换标签页,需要在标题栏显示未读消息数

#### 产品需求

| 功能 | 优先级 | 说明 |
|-----|--------|------|
| 浏览器通知 (Notification API) | P0 | 桌面弹窗通知 |
| 声音提示 | P0 | 新消息提示音 |
| 未读消息数徽标 | P0 | Tab标题显示 (3) |
| 系统托盘通知 | P1 | Electron环境下 |
| 消息免打扰 | P1 | 用户可设置 |

### 技术实现

#### 1. 浏览器通知 (Notification API)

**文件**: `src/utils/notification-manager.ts`

```typescript
/**
 * 浏览器通知管理器
 *
 * 功能:
 * 1. 请求通知权限
 * 2. 发送桌面通知
 * 3. 通知点击处理
 * 4. 通知音效
 */

import { message as antMessage } from 'ant-design-vue';

export interface NotificationOptions {
  title: string;
  body: string;
  icon?: string;
  tag?: string;
  requireInteraction?: boolean;
  silent?: boolean;
  onClick?: () => void;
}

class NotificationManager {
  private permission: NotificationPermission = 'default';
  private enabled: boolean = true;
  private soundEnabled: boolean = true;
  private notificationSound: HTMLAudioElement | null = null;

  constructor() {
    this.checkPermission();
    this.initNotificationSound();
  }

  /**
   * 检查通知权限
   */
  private checkPermission(): void {
    if (!('Notification' in window)) {
      console.warn('⚠️ [通知管理器] 浏览器不支持通知功能');
      return;
    }

    this.permission = Notification.permission;
    console.log('🔔 [通知管理器] 当前权限:', this.permission);
  }

  /**
   * 请求通知权限
   */
  async requestPermission(): Promise<boolean> {
    if (!('Notification' in window)) {
      antMessage.warning('当前浏览器不支持桌面通知');
      return false;
    }

    if (this.permission === 'granted') {
      return true;
    }

    if (this.permission === 'denied') {
      antMessage.warning('通知权限已被拒绝,请在浏览器设置中允许通知');
      return false;
    }

    try {
      const result = await Notification.requestPermission();
      this.permission = result;

      if (result === 'granted') {
        antMessage.success('已开启桌面通知');
        return true;
      } else {
        antMessage.info('已拒绝桌面通知');
        return false;
      }
    } catch (error) {
      console.error('❌ [通知管理器] 请求权限失败:', error);
      return false;
    }
  }

  /**
   * 初始化通知音效
   */
  private initNotificationSound(): void {
    try {
      // 使用系统默认提示音或自定义音频文件
      this.notificationSound = new Audio('/sounds/notification.mp3');
      this.notificationSound.volume = 0.5;
    } catch (error) {
      console.error('❌ [通知管理器] 初始化音效失败:', error);
    }
  }

  /**
   * 播放通知音效
   */
  private playNotificationSound(): void {
    if (!this.soundEnabled || !this.notificationSound) {
      return;
    }

    try {
      // 重置音频到开始位置
      this.notificationSound.currentTime = 0;
      this.notificationSound.play().catch((error) => {
        console.warn('⚠️ [通知管理器] 播放音效失败:', error);
      });
    } catch (error) {
      console.error('❌ [通知管理器] 播放音效异常:', error);
    }
  }

  /**
   * 发送通知
   *
   * @param options 通知选项
   * @returns 通知实例
   */
  async sendNotification(options: NotificationOptions): Promise<Notification | null> {
    // 1. 检查是否启用通知
    if (!this.enabled) {
      console.log('⚠️ [通知管理器] 通知已禁用');
      return null;
    }

    // 2. 检查浏览器支持
    if (!('Notification' in window)) {
      console.warn('⚠️ [通知管理器] 浏览器不支持通知');
      return null;
    }

    // 3. 检查权限
    if (this.permission !== 'granted') {
      console.warn('⚠️ [通知管理器] 没有通知权限');
      return null;
    }

    // 4. 检查页面可见性 (只在后台时发送通知)
    if (document.visibilityState === 'visible' && !options.requireInteraction) {
      console.log('⚠️ [通知管理器] 页面可见,跳过通知');
      // 仅播放音效
      this.playNotificationSound();
      return null;
    }

    try {
      // 5. 创建通知
      const notification = new Notification(options.title, {
        body: options.body,
        icon: options.icon || '/favicon.ico',
        tag: options.tag || 'smart-admin-message',
        requireInteraction: options.requireInteraction || false,
        silent: options.silent || false,
      });

      // 6. 播放音效 (如果通知不静音)
      if (!options.silent) {
        this.playNotificationSound();
      }

      // 7. 点击事件处理
      notification.onclick = () => {
        console.log('🖱️ [通知管理器] 通知被点击');

        // 聚焦窗口
        window.focus();

        // 执行自定义回调
        if (options.onClick) {
          options.onClick();
        }

        // 关闭通知
        notification.close();
      };

      // 8. 自动关闭 (5秒后)
      setTimeout(() => {
        notification.close();
      }, 5000);

      console.log('✅ [通知管理器] 通知已发送:', options.title);
      return notification;
    } catch (error) {
      console.error('❌ [通知管理器] 发送通知失败:', error);
      return null;
    }
  }

  /**
   * 发送消息通知
   */
  async sendMessageNotification(params: {
    senderName: string;
    messageContent: string;
    conversationId: string;
    onClick?: () => void;
  }): Promise<void> {
    const { senderName, messageContent, conversationId, onClick } = params;

    // 截断过长的消息内容
    const truncatedContent = messageContent.length > 50
      ? messageContent.substring(0, 50) + '...'
      : messageContent;

    await this.sendNotification({
      title: `${senderName} 发来新消息`,
      body: truncatedContent,
      tag: `message-${conversationId}`,
      requireInteraction: false,
      silent: false,
      onClick,
    });
  }

  /**
   * 启用/禁用通知
   */
  setEnabled(enabled: boolean): void {
    this.enabled = enabled;
    console.log(`🔔 [通知管理器] 通知已${enabled ? '启用' : '禁用'}`);
  }

  /**
   * 启用/禁用音效
   */
  setSoundEnabled(enabled: boolean): void {
    this.soundEnabled = enabled;
    console.log(`🔊 [通知管理器] 音效已${enabled ? '启用' : '禁用'}`);
  }

  /**
   * 获取通知权限状态
   */
  getPermission(): NotificationPermission {
    return this.permission;
  }

  /**
   * 是否已授权
   */
  isGranted(): boolean {
    return this.permission === 'granted';
  }
}

// 导出单例
export const notificationManager = new NotificationManager();
```

#### 2. 未读消息数徽标 (Tab标题)

**文件**: `src/utils/unread-badge-manager.ts`

```typescript
/**
 * 未读消息徽标管理器
 *
 * 功能:
 * 1. 更新Tab标题显示未读数
 * 2. 更新Favicon显示未读数
 * 3. 标题闪烁提醒
 */

class UnreadBadgeManager {
  private originalTitle: string = '';
  private unreadCount: number = 0;
  private isBlinking: boolean = false;
  private blinkTimer: number | null = null;

  constructor() {
    this.originalTitle = document.title;
    console.log('📛 [徽标管理器] 初始化, 原始标题:', this.originalTitle);
  }

  /**
   * 设置未读消息数
   */
  setUnreadCount(count: number): void {
    this.unreadCount = count;

    if (count > 0) {
      this.updateTitle();
      this.startBlinking();
    } else {
      this.resetTitle();
      this.stopBlinking();
    }
  }

  /**
   * 更新标题显示未读数
   */
  private updateTitle(): void {
    const prefix = this.unreadCount > 99 ? '(99+) ' : `(${this.unreadCount}) `;
    document.title = prefix + this.originalTitle;
  }

  /**
   * 重置标题
   */
  private resetTitle(): void {
    document.title = this.originalTitle;
  }

  /**
   * 开始标题闪烁
   */
  private startBlinking(): void {
    if (this.isBlinking) {
      return;
    }

    this.isBlinking = true;
    let showUnread = true;

    this.blinkTimer = window.setInterval(() => {
      if (showUnread) {
        this.updateTitle();
      } else {
        document.title = this.originalTitle;
      }
      showUnread = !showUnread;
    }, 1000);
  }

  /**
   * 停止标题闪烁
   */
  private stopBlinking(): void {
    if (!this.isBlinking) {
      return;
    }

    this.isBlinking = false;

    if (this.blinkTimer) {
      clearInterval(this.blinkTimer);
      this.blinkTimer = null;
    }

    this.resetTitle();
  }

  /**
   * 增加未读数
   */
  incrementUnreadCount(amount: number = 1): void {
    this.setUnreadCount(this.unreadCount + amount);
  }

  /**
   * 清空未读数
   */
  clearUnreadCount(): void {
    this.setUnreadCount(0);
  }

  /**
   * 获取当前未读数
   */
  getUnreadCount(): number {
    return this.unreadCount;
  }
}

// 导出单例
export const unreadBadgeManager = new UnreadBadgeManager();
```

#### 3. 在ChatPanel中集成通知

**文件**: `ChatPanel.vue` (修改)

```typescript
import { notificationManager } from '/@/utils/notification-manager';
import { unreadBadgeManager } from '/@/utils/unread-badge-manager';

// 在 setup() 中

// 处理新消息
async function handleNewMessage(messageItem: MessageItem) {
  console.log('📨 [聊天面板] 收到新消息:', messageItem);

  // 只处理当前会话的消息
  if (messageItem.groupID !== props.groupId) {
    return;
  }

  // 避免重复添加
  const exists = messages.value.some((m) => m.messageId === messageItem.clientMsgID);
  if (exists) {
    return;
  }

  const message = convertMessageItem(messageItem);
  messages.value.push(message);

  // 🔔 新增: 如果是别人发的消息且面板未展开,发送通知
  if (!message.isSelf && !expanded.value) {
    // 增加未读数
    newMessageCount.value++;

    // 更新未读徽标
    unreadBadgeManager.incrementUnreadCount(1);

    // 发送桌面通知
    await notificationManager.sendMessageNotification({
      senderName: messageItem.senderNickname || '未知用户',
      messageContent: messageItem.textElem?.content || '[非文本消息]',
      conversationId: conversationID.value,
      onClick: () => {
        // 点击通知时,展开聊天面板
        expanded.value = true;
        scrollToBottom();
      },
    });
  }

  // 如果面板已展开,滚动到底部
  if (expanded.value) {
    await scrollToBottom();
    await markConversationAsRead();
  }
}

// 展开/收起面板
async function togglePanel() {
  expanded.value = !expanded.value;

  if (expanded.value) {
    // 清空未读数
    newMessageCount.value = 0;

    // 清空未读徽标
    unreadBadgeManager.clearUnreadCount();

    // 滚动到底部
    await scrollToBottom();

    // 标记消息为已读
    await markConversationAsRead();
  } else {
    // 收起时也关闭成员面板
    showMemberPanel.value = false;
  }
}

// 组件挂载时请求通知权限
onMounted(() => {
  // 请求通知权限
  notificationManager.requestPermission();
});
```

#### 4. 添加通知音效文件

**文件**: `public/sounds/notification.mp3`

可以使用以下几种方式获取通知音效:

1. **系统默认音效** (推荐):
   ```bash
   # Windows: C:\Windows\Media\notify.wav
   # macOS: /System/Library/Sounds/
   # Linux: /usr/share/sounds/
   ```

2. **在线免费音效**:
   - [Notification Sound Effect](https://pixabay.com/sound-effects/)
   - [Freesound](https://freesound.org/)

3. **自定义音效** (使用Audacity制作):
   - 简短 (0.5-1秒)
   - 音量适中
   - 格式: MP3或OGG

---

## Phase 5: @提及功能

### 需求分析

#### 业务场景
- **场景1**: 指挥官@特定警员发布任务指令
- **场景2**: 警员@指挥官报告现场情况
- **场景3**: 用户查看有哪些消息@了自己
- **场景4**: 点击@的用户名,查看用户详情

#### 产品需求

| 功能 | 优先级 | 说明 |
|-----|--------|------|
| 输入框@人员选择 | P0 | 输入@弹出人员列表 |
| @全体成员 | P0 | @all功能 |
| 消息中高亮显示@ | P0 | @张三 蓝色高亮 |
| @我的消息特殊标记 | P0 | 背景色高亮 |
| 点击@跳转 | P1 | 查看用户详情 |
| @未读消息统计 | P1 | 显示"有3条@你的消息" |

### 技术实现

#### 1. @人员选择组件

**文件**: `src/components/mention/MentionSelector.vue`

```vue
<template>
  <div class="mention-selector">
    <!-- 人员列表弹窗 -->
    <div v-if="visible" class="mention-popup" :style="popupStyle">
      <div class="mention-list">
        <!-- @全体成员 -->
        <div
          class="mention-item mention-all"
          :class="{ active: selectedIndex === 0 }"
          @click="selectMention({ userId: 'all', userName: '全体成员' })"
        >
          <UserOutlined />
          <span>全体成员</span>
        </div>

        <!-- 群成员列表 -->
        <div
          v-for="(member, index) in filteredMembers"
          :key="member.userId"
          class="mention-item"
          :class="{ active: selectedIndex === index + 1 }"
          @click="selectMention(member)"
        >
          <Avatar :src="member.avatar" :size="24">
            {{ member.userName.charAt(0) }}
          </Avatar>
          <span>{{ member.userName }}</span>
        </div>

        <!-- 无匹配结果 -->
        <div v-if="filteredMembers.length === 0" class="mention-empty">
          无匹配结果
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue';
import { Avatar } from 'ant-design-vue';
import { UserOutlined } from '@ant-design/icons-vue';

interface Member {
  userId: string;
  userName: string;
  avatar?: string;
}

interface Props {
  visible: boolean;
  members: Member[];
  searchText: string;
  position: { x: number; y: number };
}

interface Emits {
  (e: 'select', member: Member): void;
  (e: 'close'): void;
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  members: () => [],
  searchText: '',
  position: () => ({ x: 0, y: 0 }),
});

const emit = defineEmits<Emits>();

// 选中的索引
const selectedIndex = ref<number>(0);

// 过滤后的成员列表
const filteredMembers = computed(() => {
  if (!props.searchText) {
    return props.members;
  }

  const search = props.searchText.toLowerCase();
  return props.members.filter((member) =>
    member.userName.toLowerCase().includes(search)
  );
});

// 弹窗位置
const popupStyle = computed(() => ({
  left: `${props.position.x}px`,
  top: `${props.position.y}px`,
}));

// 选择成员
function selectMention(member: Member) {
  emit('select', member);
  selectedIndex.value = 0;
}

// 键盘导航
function handleKeyDown(event: KeyboardEvent) {
  if (!props.visible) {
    return;
  }

  const maxIndex = filteredMembers.value.length;

  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault();
      selectedIndex.value = Math.min(selectedIndex.value + 1, maxIndex);
      break;

    case 'ArrowUp':
      event.preventDefault();
      selectedIndex.value = Math.max(selectedIndex.value - 1, 0);
      break;

    case 'Enter':
      event.preventDefault();
      if (selectedIndex.value === 0) {
        selectMention({ userId: 'all', userName: '全体成员' });
      } else {
        const member = filteredMembers.value[selectedIndex.value - 1];
        if (member) {
          selectMention(member);
        }
      }
      break;

    case 'Escape':
      event.preventDefault();
      emit('close');
      break;
  }
}

// 监听键盘事件
watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      window.addEventListener('keydown', handleKeyDown);
    } else {
      window.removeEventListener('keydown', handleKeyDown);
      selectedIndex.value = 0;
    }
  }
);
</script>

<style scoped lang="less">
.mention-popup {
  position: fixed;
  z-index: 1000;
  background: white;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  max-height: 300px;
  overflow-y: auto;
  min-width: 200px;
}

.mention-list {
  padding: 4px 0;
}

.mention-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover,
  &.active {
    background-color: #f5f5f5;
  }

  span {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.mention-all {
  font-weight: 500;
  color: #1890ff;
  border-bottom: 1px solid #f0f0f0;
}

.mention-empty {
  padding: 12px;
  text-align: center;
  color: #999;
}
</style>
```

#### 2. 在ChatPanel中集成@功能

**文件**: `ChatPanel.vue` (修改消息输入部分)

```vue
<template>
  <!-- 消息输入框 -->
  <div class="message-input-container">
    <a-textarea
      ref="messageInputRef"
      v-model:value="messageInput"
      :placeholder="mentionMode ? `正在@: ${mentionSearch}` : '输入消息...'"
      :auto-size="{ minRows: 2, maxRows: 4 }"
      @keydown="handleInputKeyDown"
      @input="handleInputChange"
    />

    <!-- @人员选择器 -->
    <MentionSelector
      :visible="showMentionSelector"
      :members="groupMembers"
      :search-text="mentionSearch"
      :position="mentionPopupPosition"
      @select="handleMentionSelect"
      @close="closeMentionSelector"
    />

    <!-- 发送按钮 -->
    <a-button type="primary" @click="sendMessage">
      发送
    </a-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import MentionSelector from '/@/components/mention/MentionSelector.vue';

// 消息输入框引用
const messageInputRef = ref<any>(null);

// @功能相关状态
const mentionMode = ref<boolean>(false);
const mentionSearch = ref<string>('');
const showMentionSelector = ref<boolean>(false);
const mentionPopupPosition = ref<{ x: number; y: number }>({ x: 0, y: 0 });
const mentionStartIndex = ref<number>(-1);

// 群成员列表 (从GroupMemberPanel获取)
const groupMembers = computed(() => {
  // TODO: 从GroupMemberPanel或API获取
  return [];
});

/**
 * 处理输入框键盘事件
 */
function handleInputKeyDown(event: KeyboardEvent) {
  // 检测@符号
  if (event.key === '@' || event.key === 'Shift') {
    // 在下一个tick检查是否输入了@
    nextTick(() => {
      checkMentionTrigger();
    });
  }

  // Enter发送消息
  if (event.key === 'Enter' && !event.shiftKey && !showMentionSelector.value) {
    event.preventDefault();
    sendMessage();
  }
}

/**
 * 处理输入变化
 */
function handleInputChange() {
  if (mentionMode.value) {
    updateMentionSearch();
  } else {
    checkMentionTrigger();
  }
}

/**
 * 检查是否触发@提及
 */
function checkMentionTrigger() {
  const textarea = messageInputRef.value?.$el?.querySelector('textarea');
  if (!textarea) {
    return;
  }

  const cursorPos = textarea.selectionStart;
  const text = messageInput.value;

  // 查找光标前最近的@符号
  const beforeCursor = text.substring(0, cursorPos);
  const lastAtIndex = beforeCursor.lastIndexOf('@');

  if (lastAtIndex === -1) {
    closeMentionSelector();
    return;
  }

  // 检查@后面是否有空格或换行 (如果有,则不触发)
  const afterAt = beforeCursor.substring(lastAtIndex + 1);
  if (afterAt.includes(' ') || afterAt.includes('\n')) {
    closeMentionSelector();
    return;
  }

  // 触发@提及
  mentionMode.value = true;
  mentionStartIndex.value = lastAtIndex;
  mentionSearch.value = afterAt;

  // 计算弹窗位置
  calculateMentionPopupPosition(textarea, cursorPos);

  // 显示选择器
  showMentionSelector.value = true;
}

/**
 * 更新@搜索关键词
 */
function updateMentionSearch() {
  const textarea = messageInputRef.value?.$el?.querySelector('textarea');
  if (!textarea) {
    return;
  }

  const cursorPos = textarea.selectionStart;
  const text = messageInput.value;

  // 提取@后的搜索词
  const searchText = text.substring(mentionStartIndex.value + 1, cursorPos);

  // 如果包含空格或换行,关闭选择器
  if (searchText.includes(' ') || searchText.includes('\n')) {
    closeMentionSelector();
    return;
  }

  mentionSearch.value = searchText;
}

/**
 * 计算@选择器弹窗位置
 */
function calculateMentionPopupPosition(textarea: HTMLTextAreaElement, cursorPos: number) {
  // 获取光标位置 (相对于textarea)
  const rect = textarea.getBoundingClientRect();

  // 简化计算: 弹窗显示在输入框下方
  mentionPopupPosition.value = {
    x: rect.left,
    y: rect.bottom + 5,
  };
}

/**
 * 处理@选择
 */
function handleMentionSelect(member: { userId: string; userName: string }) {
  const textarea = messageInputRef.value?.$el?.querySelector('textarea');
  if (!textarea) {
    return;
  }

  const cursorPos = textarea.selectionStart;
  const text = messageInput.value;

  // 替换@xxx为@用户名
  const before = text.substring(0, mentionStartIndex.value);
  const after = text.substring(cursorPos);
  const mentionText = `@${member.userName} `;

  messageInput.value = before + mentionText + after;

  // 移动光标到@后面
  nextTick(() => {
    const newCursorPos = before.length + mentionText.length;
    textarea.setSelectionRange(newCursorPos, newCursorPos);
    textarea.focus();
  });

  // 关闭选择器
  closeMentionSelector();
}

/**
 * 关闭@选择器
 */
function closeMentionSelector() {
  mentionMode.value = false;
  showMentionSelector.value = false;
  mentionSearch.value = '';
  mentionStartIndex.value = -1;
}

/**
 * 发送消息 (支持@)
 */
async function sendMessage() {
  if (!messageInput.value.trim()) {
    return;
  }

  try {
    // 解析@提及
    const mentions = parseMentions(messageInput.value);

    console.log('📤 [聊天面板] 发送消息:', messageInput.value);
    console.log('📤 [聊天面板] @提及:', mentions);

    // 发送OpenIM消息
    const result = await openIMClient.sendGroupTextMessage(props.groupId, messageInput.value);

    // 如果有@提及,发送扩展信息
    if (mentions.length > 0) {
      // TODO: 调用后端API记录@提及
      // await policeReportApi.recordMentions({
      //   messageId: result.clientMsgID,
      //   mentions,
      // });
    }

    console.log('✅ [OpenIM] 群组消息发送成功:', result);

    // 清空输入框
    messageInput.value = '';
  } catch (error) {
    console.error('❌ [聊天面板] 消息发送失败:', error);
    message.error('消息发送失败');
  }
}

/**
 * 解析消息中的@提及
 */
function parseMentions(text: string): Array<{ userId: string; userName: string }> {
  const mentions: Array<{ userId: string; userName: string }> = [];

  // 匹配@用户名 (空格或结尾作为分隔)
  const mentionRegex = /@([^\s@]+)/g;
  let match;

  while ((match = mentionRegex.exec(text)) !== null) {
    const userName = match[1];

    // 特殊处理@全体成员
    if (userName === '全体成员') {
      mentions.push({ userId: 'all', userName: '全体成员' });
      continue;
    }

    // 从群成员中查找用户ID
    const member = groupMembers.value.find((m) => m.userName === userName);
    if (member) {
      mentions.push({ userId: member.userId, userName: member.userName });
    }
  }

  return mentions;
}
</script>
```

#### 3. 消息显示中高亮@

**文件**: `ChatMessageItem.vue` (新建)

```vue
<template>
  <div class="chat-message-item" :class="{ mentioned: isMentioned }">
    <!-- 用户头像 -->
    <Avatar :src="message.avatar" :size="32">
      {{ message.senderName.charAt(0) }}
    </Avatar>

    <!-- 消息内容 -->
    <div class="message-content">
      <div class="message-header">
        <span class="sender-name">{{ message.senderName }}</span>
        <span class="message-time">{{ formatTime(message.sendTime) }}</span>
      </div>

      <div class="message-body" v-html="formattedContent"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Avatar } from 'ant-design-vue';
import { userStore } from '/@/store/modules/system/user';

interface Message {
  messageId: string;
  senderName: string;
  avatar?: string;
  content: string;
  sendTime: number;
  mentions?: Array<{ userId: string; userName: string }>;
}

interface Props {
  message: Message;
}

const props = defineProps<Props>();

const user = userStore();

// 是否@了当前用户
const isMentioned = computed(() => {
  if (!props.message.mentions) {
    return false;
  }

  const currentUserId = user.employeeId;
  return props.message.mentions.some(
    (m) => m.userId === currentUserId || m.userId === 'all'
  );
});

// 格式化消息内容 (高亮@)
const formattedContent = computed(() => {
  let content = props.message.content;

  // 转义HTML
  content = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

  // 高亮@提及
  content = content.replace(/@([^\s@]+)/g, (match, userName) => {
    return `<span class="mention">@${userName}</span>`;
  });

  // 转换换行
  content = content.replace(/\n/g, '<br>');

  return content;
});

// 格式化时间
function formatTime(timestamp: number): string {
  const date = new Date(timestamp);
  const now = new Date();

  // 今天: 显示时:分
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }

  // 昨天: 显示"昨天 时:分"
  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  if (date.toDateString() === yesterday.toDateString()) {
    return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }

  // 其他: 显示月-日 时:分
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}
</script>

<style scoped lang="less">
.chat-message-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 4px;
  transition: background-color 0.2s;

  &:hover {
    background-color: #f5f5f5;
  }

  // @了我的消息
  &.mentioned {
    background-color: #e6f7ff;
    border-left: 3px solid #1890ff;
    padding-left: 9px;
  }
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.sender-name {
  font-weight: 500;
  color: #262626;
}

.message-time {
  font-size: 12px;
  color: #8c8c8c;
}

.message-body {
  color: #595959;
  word-wrap: break-word;
  word-break: break-all;

  :deep(.mention) {
    color: #1890ff;
    font-weight: 500;
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }
  }
}
</style>
```

---

## 实施优先级

### 第一周: Phase 4 - 消息通知功能

#### Day 1-2: 基础通知功能
- [x] 创建 `notification-manager.ts`
- [ ] 实现浏览器通知权限请求
- [ ] 实现桌面通知发送
- [ ] 添加通知音效

#### Day 3-4: 未读消息徽标
- [ ] 创建 `unread-badge-manager.ts`
- [ ] 实现Tab标题更新
- [ ] 实现标题闪烁效果
- [ ] 集成到ChatPanel

#### Day 5: 测试与优化
- [ ] 浏览器兼容性测试
- [ ] 性能优化
- [ ] 用户体验优化

### 第二周: Phase 5 - @提及功能

#### Day 6-7: @选择组件
- [ ] 创建 `MentionSelector.vue`
- [ ] 实现人员列表展示
- [ ] 实现搜索过滤
- [ ] 实现键盘导航

#### Day 8-9: @功能集成
- [ ] ChatPanel集成@输入
- [ ] 消息解析@提及
- [ ] 后端API对接 (可选)

#### Day 10-11: @消息显示
- [ ] 创建 `ChatMessageItem.vue`
- [ ] 实现@高亮显示
- [ ] 实现@我的消息高亮

#### Day 12: 测试与优化
- [ ] 功能测试
- [ ] 性能优化
- [ ] 用户体验优化

---

## 测试方案

### Phase 4: 消息通知测试

#### 测试用例1: 浏览器通知
```
前置条件: 用户已授权通知权限

步骤:
1. 用户A在警情A的聊天中
2. 用户B打开警情B的聊天
3. 用户A发送消息"测试通知"

预期结果:
- 用户B收到桌面通知
- 通知标题: "用户A 发来新消息"
- 通知内容: "测试通知"
- 播放提示音
```

#### 测试用例2: 未读消息徽标
```
前置条件: 用户已收到3条未读消息

步骤:
1. 观察浏览器Tab标题

预期结果:
- 标题显示: (3) SmartAdmin
- 标题每秒闪烁一次
- 打开聊天面板后,标题恢复正常
```

### Phase 5: @提及测试

#### 测试用例3: @人员选择
```
步骤:
1. 在消息输入框输入"@"
2. 观察弹出的人员列表
3. 输入"张"进行搜索
4. 选择"张三"

预期结果:
- 输入"@"后弹出人员列表
- 列表包含"全体成员"和所有群成员
- 搜索"张"后只显示名字包含"张"的成员
- 选择后输入框显示"@张三 "
```

#### 测试用例4: @消息高亮
```
前置条件: 用户A的用户名是"张三"

步骤:
1. 用户B发送消息"@张三 请查看报告"
2. 用户A查看聊天记录

预期结果:
- 用户A看到消息背景高亮 (淡蓝色)
- "@张三"文字显示为蓝色
- 左侧有蓝色竖线标记
```

---

**文档版本**: v1.0
**最后更新**: 2025-10-11
**作者**: Claude Code Assistant
**状态**: ✅ 待实施
