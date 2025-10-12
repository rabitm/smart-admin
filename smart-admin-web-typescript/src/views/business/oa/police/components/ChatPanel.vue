<!--
  聊天面板组件 - WebSocket实时版本
  集成群成员面板 - 飞书/钉钉风格
  @Author Claude Code Assistant
  @Date 2025-10-09
  @Copyright 1024创新实验室
-->

<template>
  <div class="chat-panel">
    <!-- 聊天头部 -->
    <div class="chat-header" @click="togglePanel">
      <div class="chat-title">
        <CommentOutlined />
        <span>{{ groupName || '警情讨论组' }}</span>
        <!-- SDK 就绪状态指示器 -->
        <a-badge :status="isSDKReady ? 'success' : 'error'" :text="isSDKReady ? '就绪' : '未就绪'" />
      </div>
      <div class="chat-actions">
        <!-- 群成员面板切换按钮 -->
        <a-button
          v-if="expanded"
          type="text"
          size="small"
          @click.stop="toggleMemberPanel"
          :class="{ 'active-btn': showMemberPanel }"
        >
          <template #icon><TeamOutlined /></template>
        </a-button>
        <a-badge :count="newMessageCount" :offset="[-5, 5]">
          <a-button type="text" size="small">
            <template #icon>
              <UpOutlined v-if="expanded" />
              <DownOutlined v-else />
            </template>
          </a-button>
        </a-badge>
      </div>
    </div>

    <!-- 聊天内容区域 -->
    <div v-show="expanded" class="chat-body" :class="{ 'with-member-panel': showMemberPanel }">
      <!-- 主聊天区域 -->
      <div class="main-chat-area">
        <!-- 消息列表 -->
        <div ref="messageListRef" class="message-list">
        <a-spin :spinning="loading">
          <a-empty v-if="!loading && messages.length === 0" description="暂无消息" />

          <div v-else class="message-container">
            <div
              v-for="message in messages"
              :key="message.messageId"
              :class="['message-item', message.isSelf ? 'own-message' : 'other-message']"
            >
              <!-- 发送者头像 -->
              <div v-if="!message.isSelf" class="message-avatar">
                <a-avatar :size="32">
                  {{ message.senderName?.charAt(0) || '?' }}
                </a-avatar>
              </div>

              <div class="message-content-wrapper">
                <!-- 发送者昵称 -->
                <div v-if="!message.isSelf" class="message-sender">
                  {{ message.senderName || '未知用户' }}
                </div>

                <!-- 消息内容 -->
                <div class="message-content">
                  <!-- 文本消息 -->
                  <div v-if="message.contentType === 101" class="message-text">
                    <!-- @ 提及标签 (Phase 5) -->
                    <div v-if="message.isAtMe || message.isAtAll" class="mention-badge">
                      <a-tag color="blue" size="small">
                        <template #icon><BellOutlined /></template>
                        {{ message.isAtAll ? '@所有人' : '@我' }}
                      </a-tag>
                    </div>

                    <!-- Markdown 消息内容 (Modern Editor) -->
                    <div v-if="message.messageType === 'markdown'" class="markdown-message-content" v-html="renderMarkdown(message.content)"></div>
                    <!-- 富文本消息内容 (Modern Editor) -->
                    <div v-else-if="message.html" class="rich-message-content" v-html="message.html"></div>
                    <!-- 普通文本消息内容 (高亮 @ 文本) -->
                    <span v-else v-html="renderMessageWithMention(message)"></span>
                  </div>

                  <!-- 图片消息 -->
                  <div v-else-if="message.contentType === 102 && message.pictureElem" class="message-image">
                    <a-image
                      :src="message.pictureElem.sourcePicture.url"
                      :width="Math.min(message.pictureElem.sourcePicture.width, 300)"
                      :height="Math.min(message.pictureElem.sourcePicture.height, 300)"
                      :preview="{src: message.pictureElem.sourcePicture.url}"
                      style="border-radius: 4px; cursor: pointer"
                    />
                  </div>

                  <!-- 文件消息 -->
                  <div v-else-if="message.contentType === 106 && message.fileElem" class="message-file">
                    <div class="file-info" @click="downloadFile(message.fileElem.sourceUrl, message.fileElem.fileName)">
                      <PaperClipOutlined style="font-size: 20px; margin-right: 8px" />
                      <div class="file-details">
                        <div class="file-name">{{ message.fileElem.fileName }}</div>
                        <div class="file-size">{{ formatFileSize(message.fileElem.fileSize) }}</div>
                      </div>
                    </div>
                  </div>

                  <!-- 视频消息 -->
                  <div v-else-if="message.contentType === 104 && message.videoElem" class="message-video">
                    <video
                      :src="message.videoElem.videoUrl"
                      :poster="message.videoElem.snapshotUrl"
                      controls
                      style="max-width: 300px; max-height: 300px; border-radius: 4px"
                    />
                    <div class="video-info">
                      <span>{{ formatDuration(message.videoElem.duration) }}</span>
                      <span>{{ formatFileSize(message.videoElem.videoSize) }}</span>
                    </div>
                  </div>

                  <!-- 其他类型消息 -->
                  <div v-else class="message-text">
                    {{ message.content }}
                  </div>
                </div>

                <!-- 消息时间和已读状态 -->
                <div class="message-meta">
                  <span class="message-time">{{ formatTime(message.sendTime) }}</span>
                  <!-- 已读状态 (仅显示自己发送的消息) -->
                  <span v-if="message.isSelf" class="message-read-status">
                    <!-- 群聊: 显示已读人数 -->
                    <template v-if="message.hasReadCount !== undefined">
                      <span class="read-count" @click="showReadMembers(message)">
                        已读 {{ message.hasReadCount }}/{{ (message.hasReadCount || 0) + (message.unreadCount || 0) }}
                      </span>
                    </template>
                    <!-- 单聊: 显示已读未读状态 -->
                    <template v-else-if="message.isRead !== undefined">
                      <span v-if="message.isRead" class="read-indicator">✓✓</span>
                      <span v-else class="unread-indicator">✓</span>
                    </template>
                  </span>
                </div>
              </div>

              <!-- 当前用户头像 -->
              <div v-if="message.isSelf" class="message-avatar">
                <a-avatar :size="32">
                  {{ message.senderName?.charAt(0) || '我' }}
                </a-avatar>
              </div>
            </div>
          </div>
        </a-spin>
      </div>

      <!-- 输入区域 - 现代化富文本编辑器 -->
      <div class="input-area">
        <ModernMessageEditor
          ref="editorRef"
          :members="groupMembers"
          :sending="sending"
          @send="handleEditorSend"
          @upload-image="handleEditorUploadImage"
          @upload-video="handleEditorUploadVideo"
          @upload-file="handleEditorUploadFile"
          @mention="handleEditorMention"
        />
      </div>
      </div>

      <!-- 群成员面板 -->
      <GroupMemberPanel
        v-if="showMemberPanel"
        :group-id="groupId"
        :report-id="reportId"
        @invite="handleInviteMembers"
        @memberRemoved="handleMemberRemoved"
        @memberUpdated="handleMemberUpdated"
      />
    </div>

    <!-- 已读成员列表弹窗 -->
    <MessageReadMemberList
      v-model="showReadMemberModal"
      :message-id="selectedMessage?.messageId || ''"
      :conversation-i-d="conversationID"
      :has-read-count="selectedMessage?.hasReadCount"
      :unread-count="selectedMessage?.unreadCount"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { message as antMessage } from 'ant-design-vue';
import {
  CommentOutlined,
  UpOutlined,
  DownOutlined,
  TeamOutlined,
  BellOutlined,
} from '@ant-design/icons-vue';
import { smartSentry } from '/@/lib/smart-sentry';
import { format } from 'date-fns';
import { openIMClient } from '/@/utils/openim-client';
import type { MessageItem, ConversationType } from '@openim/wasm-client-sdk';
import GroupMemberPanel from './GroupMemberPanel.vue';
import MessageReadMemberList from './MessageReadMemberList.vue';
import ModernMessageEditor from './ModernMessageEditor.vue';
import { notificationManager } from '/@/utils/notification-manager';
import { unreadBadgeManager } from '/@/utils/unread-badge-manager';
import { imBusinessApi } from '/@/api/business/oa/im-business-api';

// 定义成员接口
interface Member {
  userId: string;
  nickname: string;
  isAll?: boolean;
}

// 定义消息接口
interface Message {
  messageId: string;
  senderId: string;
  senderName: string;
  senderAvatar?: string;
  contentType: number;
  content: string;
  sendTime: number;
  isSelf: boolean;
  // 已读状态 (Phase 2)
  hasReadCount?: number;  // 已读人数
  unreadCount?: number;   // 未读人数
  isRead?: boolean;       // 是否已读(单聊)
  // @ 提及相关 (Phase 5)
  atUserList?: string[];      // 被 @ 的用户ID列表
  isAtMe?: boolean;            // 是否 @ 了当前用户
  isAtAll?: boolean;           // 是否 @ 所有人
  // 富文本消息 (Modern Editor)
  html?: string;               // 富文本HTML内容
  messageType?: 'text' | 'markdown' | 'rich';  // 消息类型
  // 图片消息额外属性
  pictureElem?: {
    sourcePicture: {
      url: string;
      width: number;
      height: number;
    };
  };
  // 文件消息额外属性
  fileElem?: {
    fileName: string;
    fileSize: number;
    sourceUrl: string;
  };
  // 视频消息额外属性
  videoElem?: {
    videoUrl: string;
    snapshotUrl: string;
    videoSize: number;
    duration: number;
  };
}

// 组件属性
const props = defineProps<{
  reportId: number;
  groupId: string; // 必须提供 groupId (格式: group_report_5, 由后端直接返回正确格式)
  groupName?: string;
}>();

// 状态定义
const expanded = ref(false);
const loading = ref(false);
const sending = ref(false);
const messages = ref<Message[]>([]);
const newMessageCount = ref(0);
const messageListRef = ref<HTMLDivElement>();
const isSDKReady = ref(false);
const conversationID = ref<string>('');
const currentUserID = ref<string>('');
const showMemberPanel = ref(false); // 群成员面板显示状态
const selectedMessage = ref<Message | null>(null); // 当前选中的消息(用于查看已读列表)
const showReadMemberModal = ref(false); // 已读成员弹窗显示状态

// ==================== 现代化编辑器 ====================
const editorRef = ref(); // 编辑器引用
const groupMembers = ref<Member[]>([]); // 群成员列表
const selectedMembers = ref<string[]>([]); // 被 @ 的成员ID列表

/**
 * 初始化聊天
 */
onMounted(async () => {
  await initializeChat();
});

/**
 * 清理资源
 */
onUnmounted(() => {
  cleanup();
});

/**
 * 初始化聊天功能
 */
async function initializeChat() {
  try {
    loading.value = true;

    console.log('📡 [聊天面板] 初始化, reportId:', props.reportId, 'groupId:', props.groupId);

    // 🔧 优化: 不在初始化时请求通知权限,改为在首次需要通知时请求
    // 这样可以避免用户在页面加载时习惯性拒绝权限
    console.log('💡 [聊天面板] 当前通知权限状态:', notificationManager.getPermissionStatus());

    // 1. 检查 OpenIM 客户端
    if (!openIMClient) {
      throw new Error('OpenIM 客户端未初始化');
    }

    // 2. 检查是否已登录并获取当前用户ID
    if (!openIMClient.isLoggedIn || !openIMClient.currentUserId) {
      console.warn('⚠️ [聊天面板] OpenIM未登录,尝试重新登录...');

      try {
        // 尝试重新登录 OpenIM
        await openIMClient.loginWithSmartAdmin();
        console.log('✅ [聊天面板] OpenIM重新登录成功');

        // 再次检查登录状态
        if (!openIMClient.isLoggedIn || !openIMClient.currentUserId) {
          throw new Error('OpenIM 重新登录失败');
        }
      } catch (loginError) {
        console.error('❌ [聊天面板] OpenIM重新登录失败:', loginError);
        throw new Error('OpenIM 未登录，请刷新页面重试');
      }
    }

    currentUserID.value = openIMClient.currentUserId;
    console.log('📡 [聊天面板] 当前用户ID:', currentUserID.value);

    // 3. 获取群组会话（让SDK返回正确的会话ID）
    console.log('📡 [聊天面板] 获取群组会话, groupId:', props.groupId);
    const conversation = await openIMClient.getConversation(props.groupId, 3); // 3表示超级群组
    conversationID.value = conversation.conversationID;
    console.log('📡 [聊天面板] 会话ID:', conversationID.value);

    // 4. 注册消息监听器
    openIMClient.onMessage(conversationID.value, handleNewMessage);

    // 5. 注册已读回执监听器 (Phase 2)
    openIMClient.onGroupReadReceiptReceived(handleGroupReadReceipt);
    openIMClient.onC2CReadReceiptReceived(handleC2CReadReceipt);

    // 6. 🔧 关键修复: 注册会话变化监听器 (包括新会话创建和会话更新)
    // 注意: SDK的onNewConversation和onConversationChanged都触发同一个监听器
    openIMClient.onConversationChanged(handleConversationChanged);

    isSDKReady.value = true;

    // 7. 加载历史消息
    await loadHistoryMessages();

    // 8. 加载自己发送消息的已读状态
    await loadMessagesReadStatus();

    // 9. 加载群成员列表 (用于 @ 功能) - Phase 5
    await loadGroupMembers();

  } catch (error) {
    console.error('❌ [聊天面板] 初始化失败:', error);
    antMessage.error('聊天功能初始化失败: ' + (error as Error).message);
    smartSentry.captureError(error, { tags: { module: 'ChatPanel', action: 'initialize' } });
  } finally {
    loading.value = false;
  }
}

/**
 * 处理新消息
 */
async function handleNewMessage(messageItem: MessageItem) {
  console.log('📨 [聊天面板] 收到新消息:', messageItem);

  // 检查消息是否属于当前群组
  if (messageItem.groupID !== props.groupId) {
    console.log('⚠️ [聊天面板] 消息不属于当前群组,跳过');
    console.log('🔍 [DEBUG] messageItem.groupID:', messageItem.groupID, 'vs props.groupId:', props.groupId);
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

  // 💾 保存消息到数据库 (异步,不阻塞UI)
  saveMessageToDatabase(messageItem).catch(err => {
    console.warn('⚠️ [聊天面板] 保存消息到数据库失败:', err);
    // 不影响正常聊天功能
  });

  // 如果不是自己发的消息
  if (!message.isSelf) {
    // 增加未读数
    if (!expanded.value) {
      newMessageCount.value++;
    }

    // 更新未读徽标
    unreadBadgeManager.incrementUnreadCount(1);

    // ==================== Phase 5: 被 @ 时发送特殊通知 ====================
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
        // 点击通知时展开面板并滚动到底部
        expanded.value = true;
        scrollToBottom();
      },
    });

    console.log('🔔 [聊天面板] 已发送消息通知');
  }

  // 滚动到底部
  if (expanded.value) {
    await scrollToBottom();

    // 🔧 关键修复: 如果面板已展开,自动标记为已读
    await markConversationAsRead();
  }
}

/**
 * 处理会话变化和新会话创建
 * 🔧 关键修复: 当会话被创建或更新时,重新加载历史消息
 * 解决新客户端首次收到消息不显示的问题
 *
 * SDK事件说明:
 * - onNewConversation: 新会话创建时触发(首次收到消息时会话不存在)
 * - onConversationChanged: 会话信息变化时触发
 * 两个事件都会调用此处理函数
 */
async function handleConversationChanged(data: any) {
  console.log('🔔 [聊天面板] 会话发生变化:', data);

  try {
    // 检查是否是当前群组的会话
    if (Array.isArray(data)) {
      const hasCurrentConversation = data.some(conv =>
        conv.groupID === props.groupId || conv.conversationID === conversationID.value
      );

      if (hasCurrentConversation) {
        console.log('✅ [聊天面板] 检测到当前群组会话变化,立即重新加载历史消息');
        console.log('🔍 [DEBUG] 匹配的会话 groupID:', props.groupId);

        // 🔧 关键: 等待200ms确保SDK已经完成会话初始化和消息保存
        // 这对于新会话创建场景特别重要
        await new Promise(resolve => setTimeout(resolve, 200));

        // 重新加载历史消息
        await loadHistoryMessages();

        // 如果面板已展开,滚动到底部
        if (expanded.value) {
          await scrollToBottom();
        }
      }
    }
  } catch (error) {
    console.error('❌ [聊天面板] 处理会话变化失败:', error);
  }
}

/**
 * 转换数据库MessageVO到OpenIM MessageItem格式
 *
 * 用途: 将后端数据库返回的MessageVO转换为OpenIM SDK兼容的MessageItem格式
 * 场景: 无痕模式首次登录,SDK的IndexedDB为空时,从数据库加载历史消息
 */
function convertMessageVOToMessageItem(vo: any): MessageItem {
  // 解析contentJson获取完整消息内容
  let parsedContent = {};
  try {
    if (vo.contentJson) {
      parsedContent = JSON.parse(vo.contentJson);
    }
  } catch (error) {
    console.warn('⚠️ [消息转换] 解析contentJson失败:', error);
  }

  // 构建MessageItem对象
  const messageItem: MessageItem = {
    clientMsgID: vo.messageId,
    serverMsgID: vo.serverMessageId || '',
    createTime: vo.sendTime,
    sendTime: vo.sendTime,
    sessionType: 3, // 超级群组
    sendID: vo.senderId,
    recvID: props.groupId,
    msgFrom: 100,
    contentType: vo.contentType,
    platformID: 5, // Web平台
    senderNickname: vo.senderName || '',
    senderFaceUrl: vo.senderAvatar || '',
    groupID: props.groupId,
    content: vo.contentJson || vo.content || '',
    seq: vo.seq || 0,
    isRead: false,
    status: 2, // 已发送
    // 根据消息类型添加对应的elem
    textElem: vo.contentType === 101 ? parsedContent : undefined,
    pictureElem: vo.contentType === 102 ? parsedContent : undefined,
    fileElem: vo.contentType === 106 ? parsedContent : undefined,
    videoElem: vo.contentType === 104 ? parsedContent : undefined,
  } as any;

  return messageItem;
}

/**
 * 转换 MessageItem 到本地 Message 格式
 */
function convertMessageItem(item: MessageItem): Message {
  // 解析消息内容
  let content = '';
  let pictureElem;
  let fileElem;
  let videoElem;

  console.log('🔍 [DEBUG] 转换消息:', {
    clientMsgID: item.clientMsgID,
    contentType: item.contentType,
    content: item.content,
    textElem: (item as any).textElem,
    pictureElem: (item as any).pictureElem,
    fileElem: (item as any).fileElem,
    videoElem: (item as any).videoElem,
    sendTime: item.sendTime
  });

  // ==================== Phase 5: 提取 @ 提及信息 (只从文本消息提取) ====================
  let atUserList: string[] = [];
  let isAtAll = false;
  let isAtMe = false;

  if (item.contentType === 101) {
    // 🔧 Phase 5 Fix: @ 信息可能存储在 atTextElem 或 textElem 中
    const atTextElem = (item as any).atTextElem;
    const textElem = (item as any).textElem;

    atUserList = atTextElem?.atUserIDList || textElem?.atUserIDList || [];
    isAtAll = atUserList.includes('all');
    isAtMe = atUserList.includes(currentUserID.value);
  }

  try {
    if (item.contentType === 101) {
      // 101 - 文本消息
      // 🔧 Phase 5 Fix: @ 提及消息的内容存储在 atTextElem.text 中,而非 textElem.content
      const atTextElem = (item as any).atTextElem;
      const textElem = (item as any).textElem;

      if (atTextElem && atTextElem.text) {
        // @ 提及消息 - 内容在 atTextElem.text
        content = atTextElem.text;
        console.log('✅ [DEBUG] @ 提及消息:', content);
      } else if (textElem && textElem.content) {
        // 普通文本消息 - 内容在 textElem.content
        content = textElem.content;
        console.log('✅ [DEBUG] 普通文本消息:', content);
      } else if (item.content && item.content !== 'undefined' && item.content !== 'null') {
        // Fallback: 尝试从 item.content 解析
        try {
          content = JSON.parse(item.content).content;
        } catch {
          content = item.content;
        }
        console.log('✅ [DEBUG] Fallback文本消息:', content);
      }
    } else if (item.contentType === 102) {
      // 102 - 图片消息
      const picElem = (item as any).pictureElem;
      if (picElem && picElem.sourcePicture) {
        pictureElem = picElem;
        content = '[图片]';
        console.log('✅ [DEBUG] 图片消息:', picElem.sourcePicture.url);
      }
    } else if (item.contentType === 106) {
      // 106 - 文件消息
      const fElem = (item as any).fileElem;
      if (fElem) {
        fileElem = {
          fileName: fElem.fileName,
          fileSize: fElem.fileSize,
          sourceUrl: fElem.sourceUrl || fElem.filePath,
        };
        content = `[文件] ${fElem.fileName}`;
        console.log('✅ [DEBUG] 文件消息:', fElem.fileName);
      }
    } else if (item.contentType === 103) {
      // 103 - 语音消息
      content = '[语音消息]';
    } else if (item.contentType === 104) {
      // 104 - 视频消息
      const vElem = (item as any).videoElem;
      if (vElem) {
        videoElem = {
          videoUrl: vElem.videoUrl || vElem.videoPath,
          snapshotUrl: vElem.snapshotUrl || vElem.snapshotPath || '',
          videoSize: vElem.videoSize || 0,
          duration: vElem.duration || 0,
        };
        content = '[视频消息]';
        console.log('✅ [DEBUG] 视频消息:', vElem);
      } else {
        content = '[视频消息]';
      }
    } else if (item.contentType >= 1500 && item.contentType < 2000) {
      // 系统通知消息 (1501-1999)
      content = '[系统通知]';
    } else {
      // 其他类型消息
      content = item.content || `[未知消息类型:${item.contentType}]`;
    }
  } catch (error) {
    console.warn('⚠️ [聊天面板] 消息内容解析失败:', item, error);
    content = '[消息解析失败]';
  }

  const message: Message = {
    messageId: item.clientMsgID,
    senderId: item.sendID,
    senderName: item.senderNickname || '未知用户',
    senderAvatar: item.senderFaceUrl,
    contentType: item.contentType,
    content: content,
    sendTime: item.sendTime,
    isSelf: item.sendID === currentUserID.value,
    pictureElem,
    fileElem,
    videoElem,
    // Phase 5: @ 提及信息
    atUserList,
    isAtMe,
    isAtAll,
  };

  console.log('✅ [DEBUG] 转换后的消息:', message);

  return message;
}

/**
 * 加载历史消息
 * 🔧 关键修复: 添加REST API fallback方案,解决无痕模式首次登录历史消息不显示的问题
 */
async function loadHistoryMessages() {
  try {
    console.log('📥 [聊天面板] 加载历史消息, conversationID:', conversationID.value);

    if (!openIMClient) {
      console.warn('⚠️ [聊天面板] OpenIM 客户端未初始化');
      return;
    }

    // 1. 首先尝试从SDK获取历史消息 (从本地IndexedDB)
    let messageList = await openIMClient.getHistoryMessages(conversationID.value, 50);

    console.log('📥 [聊天面板] SDK获取到历史消息:', messageList);

    // 2. 🔧 如果SDK返回空消息,使用数据库 fallback
    if (messageList.length === 0) {
      console.log('🔄 [聊天面板] SDK返回空消息,尝试从数据库加载...');

      try {
        // 调用后端REST API,后端从数据库查询历史消息
        const response = await imBusinessApi.getGroupHistoryMessages(props.groupId, 50);

        console.log('📥 [聊天面板] 数据库API返回数据:', response);

        // 检查响应格式
        if (response && response.data) {
          const historyData = response.data;

          // 判断返回数据格式(后端返回的是MessageVO数组)
          if (Array.isArray(historyData)) {
            // 转换MessageVO为MessageItem格式
            messageList = historyData.map(convertMessageVOToMessageItem);
            console.log(`✅ [聊天面板] 数据库获取到 ${messageList.length} 条历史消息`);
          } else if (historyData.messageList && Array.isArray(historyData.messageList)) {
            // 嵌套在 messageList 字段中
            messageList = historyData.messageList.map(convertMessageVOToMessageItem);
            console.log(`✅ [聊天面板] 数据库获取到 ${messageList.length} 条历史消息`);
          } else {
            console.warn('⚠️ [聊天面板] 数据库API返回数据格式未知:', historyData);
          }
        } else {
          console.warn('⚠️ [聊天面板] 数据库API返回空数据');
        }

      } catch (apiError) {
        console.error('❌ [聊天面板] 数据库fallback失败:', apiError);
        // API失败也不抛出异常,继续使用空消息列表
      }
    }

    // 3. 转换并排序消息（过滤掉系统通知消息）
    messages.value = messageList
      .filter(item => item.contentType < 1500) // 只保留用户消息，过滤系统通知
      .map(convertMessageItem)
      .sort((a, b) => a.sendTime - b.sendTime);

    console.log(`✅ [聊天面板] 历史消息加载成功, 数量: ${messages.value.length}`);

    // 4. 滚动到底部
    await scrollToBottom();

  } catch (error) {
    console.error('❌ [聊天面板] 加载历史消息失败:', error);
    // 不显示错误消息,因为可能是群组刚创建还没有消息
    smartSentry.captureError(error, { tags: { module: 'ChatPanel', action: 'loadHistory' } });
  }
}

// ==================== 现代化编辑器事件处理 ====================

/**
 * 处理编辑器发送消息
 */
async function handleEditorSend(content: string, type: 'text' | 'markdown' | 'rich', metadata?: any) {
  if (!content.trim()) {
    return;
  }

  try {
    sending.value = true;

    console.log('📤 [聊天面板] 发送消息:', { content, type, metadata });

    if (!openIMClient) {
      throw new Error('OpenIM 客户端未初始化');
    }

    // 🔧 关键修复: 记录发送前消息列表长度
    const messageCountBefore = messages.value.length;
    console.log('🔍 [DEBUG] 发送前消息数量:', messageCountBefore);

    let result;

    // 检查是否有 @ 提及
    if (selectedMembers.value.length > 0) {
      // 发送带 @ 提及的消息
      console.log('📌 [@提及] 发送带 @ 提及的消息, 被 @ 成员:', selectedMembers.value);
      result = await openIMClient.sendGroupTextMessageWithMention(
        props.groupId,
        content,
        selectedMembers.value
      );
    } else {
      // 发送普通消息
      console.log('📝 [普通消息] 发送普通文本消息');
      result = await openIMClient.sendGroupTextMessage(props.groupId, content);
    }

    console.log('✅ [聊天面板] 消息发送成功:', result);

    // 🔧 手动添加消息到列表（自己发送的消息不会触发onRecvNewMessage回调）
    const messageData = result.data || result;
    const newMessage = convertMessageItem(messageData);

    // 添加富文本HTML内容 (Modern Editor)
    if (metadata && metadata.html) {
      newMessage.html = metadata.html;
      newMessage.messageType = type;
      console.log('✅ [聊天面板] 保存富文本HTML内容');
    }

    // 检查是否已存在（避免重复）
    const exists = messages.value.some((m) => m.messageId === newMessage.messageId);
    if (!exists) {
      messages.value.push(newMessage);
      console.log('✅ [聊天面板] 消息已添加到列表');

      // 💾 保存消息到数据库 (异步,不阻塞UI)
      saveMessageToDatabase(messageData).catch(err => {
        console.warn('⚠️ [聊天面板] 保存消息到数据库失败:', err);
      });

      // 滚动到底部
      await scrollToBottom();

      // ✨ 立即加载新消息的已读状态
      console.log('📖 [聊天面板] 为新发送的消息初始化已读状态');
      await loadMessagesReadStatus();
    }

    // 🔧 关键修复: 如果这是首条消息,等待会话创建并重新加载所有历史消息
    if (messageCountBefore === 0) {
      console.log('🔄 [聊天面板] 首条消息发送成功,等待2秒后重新加载历史消息...');
      setTimeout(async () => {
        try {
          console.log('🔄 [聊天面板] 开始重新加载历史消息...');
          await loadHistoryMessages();
          console.log('✅ [聊天面板] 历史消息重新加载完成');
        } catch (error) {
          console.error('❌ [聊天面板] 重新加载历史消息失败:', error);
        }
      }, 2000);
    }

    // 清空 @ 成员列表
    selectedMembers.value = [];

    // 重置编辑器
    editorRef.value?.reset();

  } catch (error) {
    console.error('❌ [聊天面板] 消息发送失败:', error);
    antMessage.error('消息发送失败: ' + (error as Error).message);
    smartSentry.captureError(error, { tags: { module: 'ChatPanel', action: 'sendMessage' } });
  } finally {
    sending.value = false;
  }
}

/**
 * 处理编辑器上传图片
 */
async function handleEditorUploadImage(file: File) {
  await sendImageMessage(file);
}

/**
 * 处理编辑器上传视频
 */
async function handleEditorUploadVideo(file: File) {
  await sendVideoMessage(file);
}

/**
 * 处理编辑器上传文件
 */
async function handleEditorUploadFile(file: File) {
  await sendFileMessage(file);
}

/**
 * 处理编辑器 @ 提及
 */
function handleEditorMention(userIds: string[]) {
  selectedMembers.value = userIds;
  console.log('📌 [@提及] 更新 selectedMembers:', selectedMembers.value);
}

/**
 * 滚动到底部
 */
async function scrollToBottom() {
  await nextTick();

  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
  }
}

/**
 * 切换面板展开/收起
 */
async function togglePanel() {
  expanded.value = !expanded.value;

  if (expanded.value) {
    // 展开时清空未读数
    newMessageCount.value = 0;

    // 清空未读徽标
    unreadBadgeManager.clearUnreadCount();

    // 滚动到底部
    await scrollToBottom();

    // 🔧 关键修复: 标记消息为已读
    await markConversationAsRead();
  } else {
    // 收起时也关闭成员面板
    showMemberPanel.value = false;
  }
}

/**
 * 切换群成员面板
 */
function toggleMemberPanel() {
  showMemberPanel.value = !showMemberPanel.value;
  console.log('👥 [聊天面板] 切换群成员面板:', showMemberPanel.value ? '显示' : '隐藏');
}

/**
 * 处理邀请成员
 */
function handleInviteMembers() {
  console.log('➕ [聊天面板] 处理邀请成员');
  antMessage.info('成员邀请功能开发中...');
  // TODO: Phase 3 - 实现邀请成员功能
}

/**
 * 处理成员移除
 */
function handleMemberRemoved(member: any) {
  console.log('🚫 [聊天面板] 成员已移除:', member.nickname);
  antMessage.success(`${member.nickname} 已被移出群聊`);
}

/**
 * 处理成员更新
 */
function handleMemberUpdated(member: any) {
  console.log('🔄 [聊天面板] 成员信息已更新:', member.nickname);
}

/**
 * 格式化时间
 */
function formatTime(timestamp: number): string {
  return format(new Date(timestamp), 'HH:mm');
}

/**
 * 格式化文件大小
 */
function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}

/**
 * 格式化视频时长
 */
function formatDuration(seconds: number): string {
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}

/**
 * 显示表情选择器
 */
function showEmojiPicker() {
  antMessage.info('表情功能开发中...');
}

/**
 * 显示图片上传
 */
function showImageUpload() {
  // 创建文件选择器
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*';
  input.onchange = async (e: Event) => {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (file) {
      await sendImageMessage(file);
    }
  };
  input.click();
}

/**
 * 发送图片消息
 */
async function sendImageMessage(file: File) {
  try {
    sending.value = true;
    console.log('🖼️ [聊天面板] 发送图片消息, 文件名:', file.name, '大小:', file.size);

    if (!openIMClient) {
      throw new Error('OpenIM 客户端未初始化');
    }

    // 使用 OpenIM SDK 发送群组图片消息
    const result = await openIMClient.sendGroupImageMessage(props.groupId, file);

    console.log('✅ [聊天面板] 图片消息发送成功:', result);

    // 手动添加消息到列表
    const messageData = result.data || result;
    const newMessage = convertMessageItem(messageData);

    const exists = messages.value.some((m) => m.messageId === newMessage.messageId);
    if (!exists) {
      messages.value.push(newMessage);
      await scrollToBottom();
    }

  } catch (error) {
    console.error('❌ [聊天面板] 图片消息发送失败:', error);
    antMessage.error('图片发送失败: ' + (error as Error).message);
    smartSentry.captureError(error, { tags: { module: 'ChatPanel', action: 'sendImageMessage' } });
  } finally {
    sending.value = false;
  }
}

/**
 * 显示文件上传
 */
function showFileUpload() {
  // 创建文件选择器
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = '*/*';
  input.onchange = async (e: Event) => {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (file) {
      await sendFileMessage(file);
    }
  };
  input.click();
}

/**
 * 发送文件消息
 */
async function sendFileMessage(file: File) {
  try {
    sending.value = true;
    console.log('📎 [聊天面板] 发送文件消息, 文件名:', file.name, '大小:', file.size);

    if (!openIMClient) {
      throw new Error('OpenIM 客户端未初始化');
    }

    // 检查文件大小 (限制为 100MB)
    const maxSize = 100 * 1024 * 1024;
    if (file.size > maxSize) {
      throw new Error('文件大小不能超过 100MB');
    }

    // 使用 OpenIM SDK 发送群组文件消息
    const result = await openIMClient.sendGroupFileMessage(props.groupId, file);

    console.log('✅ [聊天面板] 文件消息发送成功:', result);

    // 手动添加消息到列表
    const messageData = result.data || result;
    const newMessage = convertMessageItem(messageData);

    const exists = messages.value.some((m) => m.messageId === newMessage.messageId);
    if (!exists) {
      messages.value.push(newMessage);
      await scrollToBottom();
    }

  } catch (error) {
    console.error('❌ [聊天面板] 文件消息发送失败:', error);
    antMessage.error('文件发送失败: ' + (error as Error).message);
    smartSentry.captureError(error, { tags: { module: 'ChatPanel', action: 'sendFileMessage' } });
  } finally {
    sending.value = false;
  }
}

/**
 * 显示视频上传
 */
function showVideoUpload() {
  // 创建文件选择器
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'video/*';
  input.onchange = async (e: Event) => {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (file) {
      await sendVideoMessage(file);
    }
  };
  input.click();
}

/**
 * 发送视频消息
 */
async function sendVideoMessage(file: File) {
  try {
    sending.value = true;
    console.log('🎬 [聊天面板] 发送视频消息, 文件名:', file.name, '大小:', file.size);

    if (!openIMClient) {
      throw new Error('OpenIM 客户端未初始化');
    }

    // 检查文件大小 (限制为 200MB)
    const maxSize = 200 * 1024 * 1024;
    if (file.size > maxSize) {
      throw new Error('视频文件大小不能超过 200MB');
    }

    // 提取视频时长
    const duration = await extractVideoDuration(file);
    console.log('📹 [聊天面板] 视频时长:', duration, '秒');

    // 生成视频缩略图 (可选)
    let snapshotFile: File | undefined;
    try {
      snapshotFile = await generateVideoSnapshot(file);
      console.log('🖼️ [聊天面板] 视频缩略图生成成功');
    } catch (error) {
      console.warn('⚠️ [聊天面板] 视频缩略图生成失败，将不使用缩略图:', error);
    }

    // 使用 OpenIM SDK 发送群组视频消息
    const result = await openIMClient.sendGroupVideoMessage(props.groupId, file, duration, snapshotFile);

    console.log('✅ [聊天面板] 视频消息发送成功:', result);

    // 手动添加消息到列表
    const messageData = result.data || result;
    const newMessage = convertMessageItem(messageData);

    const exists = messages.value.some((m) => m.messageId === newMessage.messageId);
    if (!exists) {
      messages.value.push(newMessage);
      await scrollToBottom();
    }

  } catch (error) {
    console.error('❌ [聊天面板] 视频消息发送失败:', error);
    antMessage.error('视频发送失败: ' + (error as Error).message);
    smartSentry.captureError(error, { tags: { module: 'ChatPanel', action: 'sendVideoMessage' } });
  } finally {
    sending.value = false;
  }
}

/**
 * 提取视频时长
 */
function extractVideoDuration(file: File): Promise<number> {
  return new Promise((resolve, reject) => {
    const video = document.createElement('video');
    video.preload = 'metadata';

    video.onloadedmetadata = () => {
      window.URL.revokeObjectURL(video.src);
      resolve(Math.floor(video.duration));
    };

    video.onerror = () => {
      window.URL.revokeObjectURL(video.src);
      reject(new Error('无法读取视频文件'));
    };

    video.src = URL.createObjectURL(file);
  });
}

/**
 * 生成视频缩略图
 */
function generateVideoSnapshot(file: File): Promise<File> {
  return new Promise((resolve, reject) => {
    const video = document.createElement('video');
    const canvas = document.createElement('canvas');
    const context = canvas.getContext('2d');

    if (!context) {
      reject(new Error('无法创建Canvas上下文'));
      return;
    }

    video.preload = 'metadata';

    video.onloadedmetadata = () => {
      // 跳转到视频的第1秒
      video.currentTime = 1;
    };

    video.onseeked = () => {
      // 设置画布大小
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;

      // 绘制视频帧
      context.drawImage(video, 0, 0, canvas.width, canvas.height);

      // 转换为Blob
      canvas.toBlob((blob) => {
        window.URL.revokeObjectURL(video.src);

        if (blob) {
          const snapshotFile = new File([blob], 'snapshot.jpg', { type: 'image/jpeg' });
          resolve(snapshotFile);
        } else {
          reject(new Error('无法生成缩略图'));
        }
      }, 'image/jpeg', 0.8);
    };

    video.onerror = () => {
      window.URL.revokeObjectURL(video.src);
      reject(new Error('无法读取视频文件'));
    };

    video.src = URL.createObjectURL(file);
  });
}

/**
 * 下载文件
 */
function downloadFile(url: string, fileName: string) {
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
}

// ==================== 已读回执功能 (Phase 2) ====================

/**
 * 标记会话为已读
 * 🔧 关键修复: 当用户查看消息时,主动发送已读回执给OpenIM
 * ⚠️ 注意: 超级群组不支持已读回执功能
 */
async function markConversationAsRead() {
  try {
    if (!openIMClient || !conversationID.value) {
      console.warn('⚠️ [聊天面板] OpenIM客户端或会话ID未初始化,无法标记已读');
      return;
    }

    // ⚠️ 超级群组不支持已读回执功能
    if (conversationID.value.startsWith('sg_')) {
      console.log('⚠️ [聊天面板] 超级群组不支持已读回执,跳过标记');
      return;
    }

    console.log('✅ [聊天面板] 标记会话为已读, conversationID:', conversationID.value);

    // 调用OpenIM SDK标记消息为已读
    await openIMClient.markMessageAsRead(conversationID.value);

    console.log('✅ [聊天面板] 会话已标记为已读');

    // 标记成功后,刷新自己发送消息的已读状态
    // 使用 setTimeout 延迟一点时间,让OpenIM服务器有时间处理
    setTimeout(async () => {
      await loadMessagesReadStatus();
    }, 500);

  } catch (error) {
    console.error('❌ [聊天面板] 标记会话为已读失败:', error);
    // 不显示错误提示,因为这不是关键功能
  }
}

/**
 * 加载消息已读状态
 */
async function loadMessagesReadStatus() {
  try {
    // 只获取自己发送的消息
    const selfMessages = messages.value.filter(m => m.isSelf);

    if (selfMessages.length === 0) {
      console.log('📖 [聊天面板] 没有自己发送的消息,跳过已读状态加载');
      return;
    }

    const messageIDs = selfMessages.map(m => m.messageId);
    console.log('📖 [聊天面板] 加载已读状态, 消息数:', messageIDs.length);
    console.log('📖 [聊天面板] 消息ID列表:', messageIDs);
    console.log('📖 [聊天面板] 会话ID:', conversationID.value);

    // ⚠️ 超级群组不支持已读回执功能
    // OpenIM SDK的 getGroupMessageReaderList API 对超级群组(conversationID以'sg_'开头)返回空错误
    // 参考: https://docs.openim.io/guides/gettingStarted/super-group
    if (conversationID.value.startsWith('sg_')) {
      console.warn('⚠️ [聊天面板] 超级群组不支持已读回执功能,跳过已读状态加载');
      return;
    }

    // 获取群组消息已读状态(仅支持普通群组)
    const readStatusMap = await openIMClient.getGroupMessageReadReceipt(conversationID.value, messageIDs);

    console.log('📖 [聊天面板] 收到的已读状态Map:', readStatusMap);
    console.log('📖 [聊天面板] Map大小:', readStatusMap.size);

    // 获取群成员总数用于计算未读人数
    let totalMembers = 0;
    try {
      const members = await openIMClient.getGroupMembers(props.groupId);
      totalMembers = members.length;
      console.log('📖 [聊天面板] 群成员总数:', totalMembers);
    } catch (error) {
      console.warn('⚠️ [聊天面板] 获取群成员总数失败:', error);
    }

    // 如果只有一个成员(自己),跳过已读状态显示
    if (totalMembers <= 1) {
      console.log('📖 [聊天面板] 群组只有一个成员,跳过已读状态显示');
      return;
    }

    // 更新消息的已读状态
    let updatedCount = 0;
    messages.value.forEach(message => {
      if (message.isSelf) {
        if (readStatusMap.has(message.messageId)) {
          // 有已读回执数据
          const status = readStatusMap.get(message.messageId)!;
          message.hasReadCount = status.hasReadCount;
          // 计算未读人数 = 总成员数 - 已读人数 - 1(自己)
          message.unreadCount = Math.max(0, totalMembers - status.hasReadCount - 1);
          updatedCount++;
          console.log(`✅ [聊天面板] 更新消息 ${message.messageId.substring(0, 8)}..., 已读:${status.hasReadCount}, 未读:${message.unreadCount}`);
        } else {
          // 没有已读回执数据,初始化为0已读
          message.hasReadCount = 0;
          message.unreadCount = totalMembers - 1; // 减去自己
          console.log(`⚠️ [聊天面板] 消息 ${message.messageId.substring(0, 8)}... 无已读回执,初始化为 0/${message.unreadCount}`);
        }
      }
    });

    console.log(`✅ [聊天面板] 已读状态加载完成, 更新了 ${updatedCount} 条消息, 初始化了 ${selfMessages.length - updatedCount} 条消息`);
  } catch (error) {
    console.error('❌ [聊天面板] 加载已读状态失败:', error);
    // 不显示错误提示,因为这不是关键功能
  }
}

/**
 * 处理群聊已读回执
 */
function handleGroupReadReceipt(data: any) {
  console.log('✓✓ [聊天面板] 收到群聊已读回执:', data);

  try {
    // 更新消息的已读状态
    if (data && data.groupMessageReadInfo) {
      data.groupMessageReadInfo.forEach((info: any) => {
        const message = messages.value.find(m => m.messageId === info.clientMsgID);
        if (message && message.isSelf) {
          message.hasReadCount = info.hasReadCount || 0;
          message.unreadCount = info.unreadCount || 0;
          console.log(`✅ [聊天面板] 更新消息已读状态: ${message.messageId}, 已读:${message.hasReadCount}, 未读:${message.unreadCount}`);
        }
      });
    }
  } catch (error) {
    console.error('❌ [聊天面板] 处理群聊已读回执失败:', error);
  }
}

/**
 * 处理单聊已读回执
 */
function handleC2CReadReceipt(data: any[]) {
  console.log('✓✓ [聊天面板] 收到单聊已读回执:', data);

  try {
    data.forEach((receipt: any) => {
      if (receipt.msgIDList) {
        receipt.msgIDList.forEach((msgID: string) => {
          const message = messages.value.find(m => m.messageId === msgID);
          if (message && message.isSelf) {
            message.isRead = true;
            console.log(`✅ [聊天面板] 标记消息已读: ${msgID}`);
          }
        });
      }
    });
  } catch (error) {
    console.error('❌ [聊天面板] 处理单聊已读回执失败:', error);
  }
}

/**
 * 显示已读成员列表
 */
function showReadMembers(message: Message) {
  console.log('👀 [聊天面板] 查看已读成员:', message.messageId);
  selectedMessage.value = message;
  showReadMemberModal.value = true;
}

// ==================== 消息持久化 (Database Storage) ====================

/**
 * 保存消息到数据库
 *
 * 用途: 解决无痕模式首次登录无法加载历史消息的问题
 * 原理: 前端通过OpenIM SDK发送/接收消息后,同时保存到后端数据库
 * 特性: 异步保存,不阻塞UI;自动去重,相同messageId只保存一次
 *
 * @param messageItem OpenIM消息对象
 */
async function saveMessageToDatabase(messageItem: MessageItem) {
  try {
    console.log('💾 [消息保存] 开始保存消息到数据库:', messageItem.clientMsgID);

    // 1. 提取消息内容
    let content = '';
    let contentJson = '';

    // 根据消息类型解析内容
    if (messageItem.contentType === 101) {
      // 文本消息 - 需要同时检查 atTextElem 和 textElem (Phase 5)
      const atTextElem = (messageItem as any).atTextElem;
      const textElem = (messageItem as any).textElem;

      if (atTextElem && atTextElem.text) {
        // @ 提及消息 - 内容在 atTextElem.text
        content = atTextElem.text;
        contentJson = JSON.stringify(atTextElem);
      } else if (textElem && textElem.content) {
        // 普通文本消息 - 内容在 textElem.content
        content = textElem.content;
        contentJson = JSON.stringify(textElem);
      }
    } else if (messageItem.contentType === 102) {
      // 图片消息
      content = '[图片]';
      contentJson = JSON.stringify((messageItem as any).pictureElem || {});
    } else if (messageItem.contentType === 106) {
      // 文件消息
      const fileElem = (messageItem as any).fileElem;
      content = `[文件] ${fileElem?.fileName || ''}`;
      contentJson = JSON.stringify(fileElem || {});
    } else if (messageItem.contentType === 104) {
      // 视频消息
      content = '[视频]';
      contentJson = JSON.stringify((messageItem as any).videoElem || {});
    } else {
      // 其他类型
      content = messageItem.content || `[消息类型:${messageItem.contentType}]`;
      contentJson = messageItem.content || '{}';
    }

    // 2. 构建请求数据
    const messageData = {
      reportId: props.reportId,
      groupId: props.groupId,
      messageId: messageItem.clientMsgID,
      serverMessageId: messageItem.serverMsgID,
      conversationId: conversationID.value,
      senderId: messageItem.sendID,
      senderName: messageItem.senderNickname || '未知用户',
      senderAvatar: messageItem.senderFaceUrl,
      contentType: messageItem.contentType,
      content: content,
      contentJson: contentJson,
      sendTime: messageItem.sendTime,
      seq: messageItem.seq,
    };

    console.log('💾 [消息保存] 请求数据:', messageData);

    // 3. 调用后端API保存
    await imBusinessApi.saveMessage(messageData);

    console.log('✅ [消息保存] 消息保存成功:', messageItem.clientMsgID);

  } catch (error) {
    console.error('❌ [消息保存] 保存失败:', error);
    // 不抛出异常,不影响正常聊天功能
  }
}

// ==================== @ 提及功能 (Phase 5) ====================

/**
 * 加载群成员列表
 */
async function loadGroupMembers() {
  try {
    if (!openIMClient) {
      console.warn('⚠️ [聊天面板] OpenIM 客户端未初始化');
      return;
    }

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

/**
 * 渲染带 @ 高亮的消息文本
 */
function renderMessageWithMention(message: Message): string {
  let content = message.content;

  // 正则匹配 @昵称 模式
  const mentionRegex = /@([^\s]+)/g;

  content = content.replace(mentionRegex, (match, nickname) => {
    return `<span class="mention-highlight">@${nickname}</span>`;
  });

  return content;
}

/**
 * 渲染 Markdown 消息
 * 🔧 修复: 用户要求 Markdown 消息在历史记录中显示为格式化的 HTML，而非原始 Markdown 语法
 */
function renderMarkdown(content: string): string {
  if (!content) return '';

  let html = content
    // 标题
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    // 加粗
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    // 斜体
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    // 删除线
    .replace(/~~(.*?)~~/g, '<del>$1</del>')
    // 代码
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    // 引用
    .replace(/^> (.*$)/gim, '<blockquote>$1</blockquote>')
    // 无序列表
    .replace(/^\- (.*$)/gim, '<li>$1</li>')
    .replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>')
    // 链接
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>')
    // 换行
    .replace(/\n/g, '<br>');

  return html;
}

/**
 * 清理资源
 */
function cleanup() {
  console.log('🧹 [聊天面板] 清理资源');

  // 移除消息监听器
  if (openIMClient && conversationID.value) {
    openIMClient.offMessage(conversationID.value, handleNewMessage);
  }

  // 移除会话变化监听器 (包括新会话创建事件)
  if (openIMClient) {
    openIMClient.offConversationChanged(handleConversationChanged);
  }

  isSDKReady.value = false;
}
</script>

<style scoped lang="less">
.chat-panel {
  display: flex;
  flex-direction: column;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  background-color: #fff;
  overflow: hidden;

  .chat-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #e8e8e8;
    background-color: #fafafa;
    cursor: pointer;
    user-select: none;

    &:hover {
      background-color: #f0f0f0;
    }

    .chat-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 500;
      font-size: 14px;
    }

    .chat-actions {
      display: flex;
      gap: 8px;

      .active-btn {
        background-color: #e6f7ff;
        color: #1890ff;
      }
    }
  }

  .chat-body {
    display: flex;
    flex-direction: row; // 改为横向布局
    height: 500px;
    position: relative;

    // 主聊天区容器
    .main-chat-area {
      flex: 1;
      display: flex;
      flex-direction: column;
      min-width: 0; // 防止内容溢出
      transition: all 0.3s ease;
    }

    .message-list {
      flex: 1;
      overflow-y: auto;
      padding: 16px 0;
      background-color: #f5f5f5;

      .message-container {
        display: flex;
        flex-direction: column;
      }

      .message-item {
        display: flex;
        margin-bottom: 16px;
        padding: 0 16px;

        .message-avatar {
          flex-shrink: 0;
        }

        .message-content-wrapper {
          display: flex;
          flex-direction: column;
          max-width: 60%;
        }

        .message-sender {
          font-size: 12px;
          color: #999;
          margin-bottom: 4px;
        }

        .message-content {
          padding: 8px 12px;
          border-radius: 8px;
          word-wrap: break-word;
        }

        .message-meta {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-top: 4px;
        }

        .message-time {
          font-size: 12px;
          color: #999;
        }

        .message-read-status {
          font-size: 12px;
          color: #999;

          .read-count {
            cursor: pointer;
            color: #1890ff;
            transition: all 0.3s;

            &:hover {
              color: #40a9ff;
              text-decoration: underline;
            }
          }

          .read-indicator {
            color: #52c41a;
            font-weight: bold;
          }

          .unread-indicator {
            color: #999;
          }
        }

        .message-text {
          line-height: 1.5;
          white-space: pre-wrap;

          // Phase 5: @ 提及样式
          .mention-badge {
            margin-bottom: 4px;
          }

          :deep(.mention-highlight) {
            color: #1890ff;
            background-color: #e6f7ff;
            padding: 0 4px;
            border-radius: 2px;
            font-weight: 500;
          }

          // Modern Editor: 富文本样式
          .rich-message-content {
            :deep(strong) {
              font-weight: bold;
            }

            :deep(em) {
              font-style: italic;
            }

            :deep(code) {
              background: rgba(0, 0, 0, 0.1);
              padding: 2px 6px;
              border-radius: 3px;
              font-family: 'Courier New', monospace;
              font-size: 13px;
            }

            :deep(blockquote) {
              border-left: 3px solid rgba(255, 255, 255, 0.3);
              margin: 8px 0;
              padding-left: 12px;
            }

            :deep(ul), :deep(ol) {
              margin-left: 20px;
              margin-top: 8px;
              margin-bottom: 8px;
            }

            :deep(p) {
              margin: 0;
            }

            :deep(.mention) {
              color: inherit;
              background: rgba(255, 255, 255, 0.2);
              padding: 0 4px;
              border-radius: 2px;
              font-weight: 500;
            }
          }

          // Modern Editor: Markdown 消息样式
          .markdown-message-content {
            :deep(h1) {
              font-size: 20px;
              font-weight: bold;
              margin: 8px 0 4px 0;
            }

            :deep(h2) {
              font-size: 18px;
              font-weight: bold;
              margin: 6px 0 4px 0;
            }

            :deep(h3) {
              font-size: 16px;
              font-weight: bold;
              margin: 4px 0 2px 0;
            }

            :deep(strong) {
              font-weight: bold;
            }

            :deep(em) {
              font-style: italic;
            }

            :deep(del) {
              text-decoration: line-through;
            }

            :deep(code) {
              background: rgba(0, 0, 0, 0.1);
              padding: 2px 6px;
              border-radius: 3px;
              font-family: 'Courier New', monospace;
              font-size: 13px;
            }

            :deep(blockquote) {
              border-left: 3px solid rgba(255, 255, 255, 0.3);
              margin: 8px 0;
              padding-left: 12px;
              font-style: italic;
              opacity: 0.9;
            }

            :deep(ul) {
              margin-left: 20px;
              margin-top: 8px;
              margin-bottom: 8px;
              list-style-type: disc;
            }

            :deep(li) {
              margin: 4px 0;
            }

            :deep(a) {
              color: inherit;
              text-decoration: underline;
            }
          }
        }

        // 其他用户消息的富文本样式调整
        &.other-message .rich-message-content {
          :deep(code) {
            background: #f5f5f5;
          }

          :deep(blockquote) {
            border-left-color: #1890ff;
          }

          :deep(.mention) {
            background: #e6f7ff;
            color: #1890ff;
          }
        }

        // 其他用户消息的 Markdown 样式调整
        &.other-message .markdown-message-content {
          :deep(code) {
            background: #f5f5f5;
          }

          :deep(blockquote) {
            border-left-color: #1890ff;
          }

          :deep(a) {
            color: #1890ff;
          }
        }

        .message-image {
          :deep(.ant-image) {
            display: block;
          }
        }

        .message-file {
          .file-info {
            display: flex;
            align-items: center;
            padding: 8px 12px;
            border-radius: 4px;
            cursor: pointer;
            transition: all 0.3s;

            &:hover {
              background-color: rgba(0, 0, 0, 0.05);
            }

            .file-details {
              flex: 1;

              .file-name {
                font-size: 14px;
                margin-bottom: 4px;
                word-break: break-all;
              }

              .file-size {
                font-size: 12px;
                color: #999;
              }
            }
          }
        }

        .message-video {
          video {
            display: block;
            border-radius: 4px;
          }

          .video-info {
            display: flex;
            justify-content: space-between;
            margin-top: 4px;
            font-size: 12px;
            color: #999;
          }
        }
      }

      // 其他用户的消息
      .other-message {
        justify-content: flex-start;

        .message-avatar {
          margin-right: 8px;
        }

        .message-content-wrapper {
          align-items: flex-start;
        }

        .message-content {
          background-color: #fff;
          color: #333;
          box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
        }

      }

      // 当前用户的消息
      .own-message {
        justify-content: flex-end;

        .message-avatar {
          margin-left: 8px;
        }

        .message-content-wrapper {
          align-items: flex-end;
        }

        .message-content {
          background-color: #1890ff;
          color: #fff;
        }

      }
    }

    .input-area {
      border-top: 1px solid #e8e8e8;
      padding: 12px 16px;
      background-color: #fff;

      .input-actions {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 8px;
      }
    }

    // 群成员面板样式
    :deep(.group-member-panel) {
      width: 300px;
      flex-shrink: 0;
      height: 100%;
      animation: slideIn 0.3s ease;
    }

    @keyframes slideIn {
      from {
        transform: translateX(100%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }
  }
}

// 滚动条样式
.message-list::-webkit-scrollbar {
  width: 6px;
}

.message-list::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.message-list::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.3);
}
</style>
