# OpenIM Phase 5: @ 提及功能实现指南

## ✅ 已完成

### 1. MentionSelector 组件 ✅
**文件**: `smart-admin-web-typescript/src/views/business/oa/police/components/MentionSelector.vue`

**功能特性**:
- ✅ 成员列表展示
- ✅ @所有人选项
- ✅ 搜索过滤
- ✅ 键盘导航 (↑↓ + Enter + Esc)
- ✅ 鼠标悬停高亮

**使用方式**:
```vue
<MentionSelector
  :visible="showMentionSelector"
  :members="groupMembers"
  :position="{ top: 100, left: 20 }"
  :show-mention-all="true"
  @select="handleMentionSelect"
  @close="showMentionSelector = false"
/>
```

### 2. OpenIM 客户端扩展 ✅
**文件**: `smart-admin-web-typescript/src/utils/openim-client.ts`

**新增方法**:
```typescript
/**
 * 发送带 @ 提及的群组文本消息
 * @param groupID 群组ID
 * @param text 消息文本
 * @param atUserIDList 被 @ 的用户ID列表 (传入 ['all'] 表示 @所有人)
 */
async sendGroupTextMessageWithMention(
  groupID: string,
  text: string,
  atUserIDList: string[]
): Promise<MessageItem>
```

**OpenIM SDK API**:
- 使用 `createTextAtMessage()` 创建 @ 消息
- `atUserIDList`: 被 @ 的用户ID数组,`['all']` 表示 @所有人
- `atUsersInfo`: 用户信息,SDK会自动填充

## 🔄 待集成功能

### 3. ChatPanel 集成 @ 功能 🚧

**需要实现的功能**:

#### 3.1 输入框监听 @ 触发
```typescript
// 在 ChatPanel.vue <script> 部分添加:

// 状态定义
const showMentionSelector = ref(false);
const mentionSelectorPosition = ref({ top: 0, left: 0 });
const groupMembers = ref<Member[]>([]);
const selectedMembers = ref<string[]>([]); // 被 @ 的成员ID

// 监听输入框 @ 触发
function handleInputChange(e: Event) {
  const textarea = e.target as HTMLTextAreaElement;
  const text = textarea.value;
  const cursorPos = textarea.selectionStart;

  // 检查光标前是否有 @
  const textBeforeCursor = text.substring(0, cursorPos);
  const lastAtIndex = textBeforeCursor.lastIndexOf('@');

  if (lastAtIndex !== -1) {
    const textAfterAt = textBeforeCursor.substring(lastAtIndex + 1);

    // 如果 @ 后面没有空格,显示选择器
    if (!textAfterAt.includes(' ')) {
      showMentionSelector.value = true;

      // 计算选择器位置 (基于光标位置)
      const rect = textarea.getBoundingClientRect();
      mentionSelectorPosition.value = {
        top: rect.bottom + window.scrollY,
        left: rect.left + window.scrollX,
      };
    }
  } else {
    showMentionSelector.value = false;
  }
}
```

#### 3.2 加载群成员列表
```typescript
// 在 initializeChat() 中添加:
async function initializeChat() {
  // ... 现有代码 ...

  // 加载群成员列表 (用于 @ 功能)
  await loadGroupMembers();
}

// 加载群成员
async function loadGroupMembers() {
  try {
    const members = await openIMClient.getGroupMembers(props.groupId);

    groupMembers.value = members.map(member => ({
      userId: member.userID,
      nickname: member.nickname || member.userID,
    }));

    console.log('✅ [聊天面板] 群成员加载成功, 数量:', groupMembers.value.length);
  } catch (error) {
    console.error('❌ [聊天面板] 加载群成员失败:', error);
  }
}
```

#### 3.3 处理成员选择
```typescript
// 处理 @ 成员选择
function handleMentionSelect(member: { userId: string; nickname: string; isAll?: boolean }) {
  console.log('📌 [@提及] 选中成员:', member);

  // 1. 关闭选择器
  showMentionSelector.value = false;

  // 2. 找到最后一个 @ 的位置
  const lastAtIndex = inputText.value.lastIndexOf('@');

  // 3. 替换文本
  if (lastAtIndex !== -1) {
    const beforeAt = inputText.value.substring(0, lastAtIndex);
    const afterAt = inputText.value.substring(lastAtIndex + 1);
    const nextSpaceIndex = afterAt.indexOf(' ');
    const remainingText = nextSpaceIndex !== -1 ? afterAt.substring(nextSpaceIndex) : '';

    inputText.value = `${beforeAt}@${member.nickname} ${remainingText}`;
  }

  // 4. 记录被 @ 的用户ID
  if (member.isAll) {
    selectedMembers.value.push('all');
  } else {
    selectedMembers.value.push(member.userId);
  }
}
```

#### 3.4 修改发送消息逻辑
```typescript
// 修改 sendMessage() 函数:
async function sendMessage() {
  if (!inputText.value.trim()) {
    return;
  }

  try {
    sending.value = true;

    console.log('📤 [聊天面板] 发送消息:', inputText.value);

    if (!openIMClient) {
      throw new Error('OpenIM 客户端未初始化');
    }

    // 🔧 新增: 检查是否有 @ 提及
    let result;
    if (selectedMembers.value.length > 0) {
      // 发送带 @ 提及的消息
      console.log('📌 [@提及] 发送带 @ 提及的消息, 被 @ 成员:', selectedMembers.value);
      result = await openIMClient.sendGroupTextMessageWithMention(
        props.groupId,
        inputText.value.trim(),
        selectedMembers.value
      );
    } else {
      // 发送普通消息
      result = await openIMClient.sendGroupTextMessage(props.groupId, inputText.value.trim());
    }

    console.log('✅ [聊天面板] 消息发送成功:', result);

    // ... 其余现有代码 ...

    // 清空输入框和 @ 成员列表
    inputText.value = '';
    selectedMembers.value = [];

  } catch (error) {
    console.error('❌ [聊天面板] 消息发送失败:', error);
    antMessage.error('消息发送失败: ' + (error as Error).message);
    smartSentry.captureError(error, { tags: { module: 'ChatPanel', action: 'sendMessage' } });
  } finally {
    sending.value = false;
  }
}
```

#### 3.5 模板更新
```vue
<!-- 在 ChatPanel.vue <template> 部分修改: -->

<!-- 输入区域 -->
<div class="input-area">
  <a-textarea
    v-model:value="inputText"
    placeholder="输入消息... (输入 @ 提及成员)"
    :auto-size="{ minRows: 2, maxRows: 4 }"
    @input="handleInputChange"
    @keydown.enter.exact.prevent="sendMessage"
  />

  <!-- @ 提及选择器 -->
  <MentionSelector
    :visible="showMentionSelector"
    :members="groupMembers"
    :position="mentionSelectorPosition"
    :show-mention-all="true"
    @select="handleMentionSelect"
    @close="showMentionSelector = false"
  />

  <div class="input-actions">
    <!-- ... 现有按钮 ... -->
  </div>
</div>
```

#### 3.6 导入组件
```typescript
// 在 ChatPanel.vue <script> 部分添加:
import MentionSelector from './MentionSelector.vue';

// 定义成员接口
interface Member {
  userId: string;
  nickname: string;
  isAll?: boolean;
}
```

### 4. @ 高亮渲染 🚧

**需要实现的功能**:

#### 4.1 消息接口扩展
```typescript
// 在 ChatPanel.vue Message 接口中添加:
interface Message {
  messageId: string;
  // ... 现有字段 ...

  // @ 提及相关 (Phase 5)
  atUserList?: string[];      // 被 @ 的用户ID列表
  isAtMe?: boolean;            // 是否 @ 了当前用户
  isAtAll?: boolean;           // 是否 @ 所有人
}
```

#### 4.2 消息转换逻辑更新
```typescript
// 修改 convertMessageItem() 函数:
function convertMessageItem(item: MessageItem): Message {
  // ... 现有代码 ...

  // 提取 @ 信息
  const textElem = (item as any).textElem;
  const atUserList = textElem?.atUserIDList || [];
  const isAtAll = atUserList.includes('all');
  const isAtMe = atUserList.includes(currentUserID.value);

  const message: Message = {
    messageId: item.clientMsgID,
    // ... 现有字段 ...

    // @ 提及信息
    atUserList,
    isAtMe,
    isAtAll,
  };

  return message;
}
```

#### 4.3 @ 高亮渲染组件
```vue
<!-- 修改文本消息渲染部分: -->

<!-- 文本消息 -->
<div v-if="message.contentType === 101" class="message-text">
  <!-- @ 提及标签 -->
  <div v-if="message.isAtMe || message.isAtAll" class="mention-badge">
    <a-tag color="blue" size="small">
      <template #icon><BellOutlined /></template>
      {{ message.isAtAll ? '@所有人' : '@我' }}
    </a-tag>
  </div>

  <!-- 消息内容 (高亮 @ 文本) -->
  <span v-html="renderMessageWithMention(message)"></span>
</div>
```

#### 4.4 @ 文本高亮函数
```typescript
// 添加 @ 文本高亮渲染函数:
function renderMessageWithMention(message: Message): string {
  let content = message.content;

  // 正则匹配 @昵称 模式
  const mentionRegex = /@([^\s]+)/g;

  content = content.replace(mentionRegex, (match, nickname) => {
    return `<span class="mention-highlight">@${nickname}</span>`;
  });

  return content;
}
```

#### 4.5 样式定义
```less
// 在 ChatPanel.vue <style> 部分添加:

.mention-badge {
  margin-bottom: 4px;
}

.mention-highlight {
  color: #1890ff;
  background-color: #e6f7ff;
  padding: 0 4px;
  border-radius: 2px;
  font-weight: 500;
}
```

#### 4.6 导入图标
```typescript
// 在 ChatPanel.vue import 部分添加:
import { BellOutlined } from '@ant-design/icons-vue';
```

### 5. @ 通知增强 🚧

**需要实现的功能**:

#### 5.1 被 @ 消息的特殊通知
```typescript
// 修改 handleNewMessage() 函数:
async function handleNewMessage(messageItem: MessageItem) {
  // ... 现有代码 ...

  // 添加到消息列表
  const message = convertMessageItem(messageItem);
  messages.value.push(message);

  // 如果不是自己发的消息
  if (!message.isSelf) {
    // ... 现有未读数更新代码 ...

    // 🔧 新增: 被 @ 时发送特殊通知
    let notificationTitle = message.senderName || '未知用户';
    let notificationContent = message.content || '[非文本消息]';

    if (message.isAtMe) {
      notificationTitle = `${notificationTitle} @了你`;
    } else if (message.isAtAll) {
      notificationTitle = `${notificationTitle} @所有人`;
    }

    // 发送桌面通知
    await notificationManager.sendMessageNotification({
      senderName: notificationTitle,
      messageContent: notificationContent,
      conversationId: conversationID.value,
      avatar: message.senderAvatar,
      onClick: () => {
        expanded.value = true;
        scrollToBottom();
      },
    });
  }

  // ... 其余代码 ...
}
```

## 📝 完整集成清单

- [x] 创建 MentionSelector 组件
- [x] 扩展 OpenIM 客户端 @ 功能
- [ ] ChatPanel 输入框监听 @ 触发
- [ ] 加载群成员列表
- [ ] 处理成员选择逻辑
- [ ] 修改发送消息逻辑
- [ ] 消息接口扩展 (支持 @ 信息)
- [ ] 消息转换逻辑更新 (提取 @ 信息)
- [ ] @ 高亮渲染实现
- [ ] @ 通知增强
- [ ] 样式优化
- [ ] 测试验证

## 🔍 测试要点

1. **@ 触发测试**:
   - 输入 `@` 应弹出成员选择器
   - 键盘导航 (↑↓Enter) 正常工作
   - 搜索过滤功能正常

2. **消息发送测试**:
   - 发送 @单个成员 消息
   - 发送 @所有人 消息
   - 发送混合 @多个成员 消息

3. **渲染测试**:
   - @ 文本高亮显示
   - 被 @ 的消息显示标签
   - @我 和 @所有人 区分正确

4. **通知测试**:
   - 被 @ 时收到特殊通知
   - 通知标题正确显示 @ 信息

## 📚 参考资料

- **OpenIM SDK 文档**: https://docs.openim.io/
- **OpenIM @ 消息 API**: `createTextAtMessage()`
- **Ant Design Vue**: https://www.antdv.com/
- **Vue 3 Composition API**: https://vuejs.org/guide/

## 🎯 下一步

用户需要按照上述步骤,逐步集成 @ 功能到 ChatPanel 组件中。
