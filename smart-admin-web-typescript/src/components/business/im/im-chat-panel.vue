<template>
  <div class="im-chat-panel">
    <!-- IM未连接提示 -->
    <div v-if="connectionStatus === 3 && currentUserID === '0'" class="connection-error">
      <a-result status="warning" title="IM服务未连接" sub-title="正在尝试连接OpenIM服务,请稍候...">
        <template #extra>
          <a-button type="primary" @click="handleReconnect">重新连接</a-button>
        </template>
      </a-result>
    </div>

    <!-- 正常聊天界面 -->
    <template v-else>
      <!-- 头部 -->
      <div class="chat-header">
        <div class="header-left">
          <team-outlined style="font-size: 18px; margin-right: 8px" />
          <div class="group-info">
            <div class="group-name">{{ groupInfo?.groupName || '群聊' }}</div>
            <div class="group-members">{{ groupInfo?.memberCount || 0 }} 人</div>
          </div>
        </div>

        <div class="header-right">
          <!-- 连接状态 -->
          <a-tag v-if="connectionStatus === 2" color="success">
            <check-circle-outlined />
            已连接
          </a-tag>
          <a-tag v-else-if="connectionStatus === 1" color="processing">
            <loading-outlined />
            连接中
          </a-tag>
          <a-tag v-else-if="connectionStatus === 4" color="warning">
            <sync-outlined spin />
            重连中
          </a-tag>
          <a-tag v-else color="error">
            <close-circle-outlined />
            未连接
          </a-tag>

        <!-- 更多操作 -->
        <a-dropdown>
          <a-button type="text" size="small">
            <more-outlined />
          </a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item key="invite" @click="handleInviteMembers">
                <user-add-outlined />
                邀请成员
              </a-menu-item>
              <a-menu-item key="info" @click="handleShowGroupInfo">
                <info-circle-outlined />
                群组信息
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="chat-body">
      <im-message-list
        ref="messageListRef"
        :messages="messages"
        :current-user-id="currentUserID"
        :is-loading="isLoadingHistory"
        :has-more="hasMoreHistory"
        @load-more="handleLoadMore"
      />
    </div>

    <!-- 输入区域 -->
    <div class="chat-footer">
      <im-input-area
        ref="inputAreaRef"
        :disabled="!isConnected || !groupInfo"
        @send-text="handleSendText"
        @send-image="handleSendImage"
        @send-file="handleSendFile"
      />
    </div>
    </template>

    <!-- 邀请成员弹窗 -->
    <a-modal
      v-model:open="inviteModalVisible"
      title="邀请成员"
      :width="600"
      @ok="handleInviteConfirm"
      @cancel="inviteModalVisible = false"
    >
      <!-- TODO: 实现员工选择器 -->
      <a-empty description="员工选择器开发中..." />
    </a-modal>

    <!-- 群组信息弹窗 -->
    <a-modal
      v-model:open="groupInfoModalVisible"
      title="群组信息"
      :width="500"
      :footer="null"
      @cancel="groupInfoModalVisible = false"
    >
      <a-descriptions :column="1" bordered>
        <a-descriptions-item label="群组ID">{{ groupInfo?.groupID }}</a-descriptions-item>
        <a-descriptions-item label="群组名称">{{ groupInfo?.groupName }}</a-descriptions-item>
        <a-descriptions-item label="成员数量">{{ groupInfo?.memberCount }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">
          {{ groupInfo?.createTime ? formatDateTime(groupInfo.createTime) : '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="群组简介">
          {{ groupInfo?.introduction || '暂无简介' }}
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue';
import { message as antMessage } from 'ant-design-vue';
import {
  TeamOutlined,
  CheckCircleOutlined,
  LoadingOutlined,
  SyncOutlined,
  CloseCircleOutlined,
  MoreOutlined,
  UserAddOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons-vue';
import ImMessageList from './im-message-list.vue';
import ImInputArea from './im-input-area.vue';
import { useIMConnection, useIMGroup, getOpenIMSDK } from '/@/services/im.service';
import type { ImGroupInfo, ImMessage } from '/@/types/im';
import { format } from 'date-fns';
import { SdkEvent } from '/@/utils/openim-sdk-wrapper';
import { imLogger } from '/@/utils/im-logger';

interface Props {
  groupID: string;
  reportId?: number;
}

const props = defineProps<Props>();

// IM连接管理
const { connectionStatus, currentUserID, connect, isConnected, onConnectionStatusChange } = useIMConnection();

// 直接使用SDK，确保conversationID总是使用最新的props.groupID
const sdk = getOpenIMSDK();

// 消息列表
const messages = ref<ImMessage[]>([]);
const isLoadingHistory = ref(false);
const hasMoreHistory = ref(true);

// 群组管理
const { getGroupInfo, getPoliceGroup } = useIMGroup();

// 组件引用
const messageListRef = ref<any>(null);
const inputAreaRef = ref<any>(null);

// 群组信息
const groupInfo = ref<ImGroupInfo | null>(null);

// 弹窗状态
const inviteModalVisible = ref(false);
const groupInfoModalVisible = ref(false);

/**
 * 初始化
 */
const init = async () => {
  imLogger.debug('聊天面板开始初始化', { groupID: props.groupID, reportId: props.reportId });

  // 1. 连接IM
  const connected = await connect();
  if (!connected) {
    antMessage.error('IM连接失败,无法加载聊天记录');
    return;
  }

  // 2. 加载群组信息
  await loadGroupInfo();

  // 3. 加载历史消息
  await loadHistory();

  // 4. 监听新消息
  const handleNewMessage = (message: ImMessage) => {
    imLogger.debug('收到新消息', message);

    // 添加到消息列表
    messages.value.push(message);

    // 滚动到底部
    messageListRef.value?.scrollToBottom();

    // 播放提示音 (可选)
    // playNotificationSound();
  };

  sdk.on(SdkEvent.NEW_MESSAGE, handleNewMessage);

  // 5. 监听连接状态变化
  const cleanupStatus = onConnectionStatusChange((status) => {
    imLogger.debug('连接状态变更', status);
  });

  // 保存清理函数
  cleanupFunctions.push(
    () => sdk.off(SdkEvent.NEW_MESSAGE, handleNewMessage),
    cleanupStatus
  );
};

/**
 * 重新连接IM
 */
const handleReconnect = async () => {
  imLogger.info('手动重新连接IM');
  await init();
};

/**
 * 加载群组信息
 */
const loadGroupInfo = async () => {
  try {
    // 如果有reportId,优先从后端获取群组映射信息（会自动邀请当前用户加入群组）
    if (props.reportId) {
      imLogger.debug('获取警情群组信息并自动加入群组');
      const policeGroup = await getPoliceGroup(props.reportId);
      if (policeGroup) {
        imLogger.debug('获取警情群组信息成功:', policeGroup);
        imLogger.debug('等待OpenIM服务器处理群组成员更新...');

        // 重要：轮询验证用户是否成功加入群组
        // 最多重试10次,每次间隔500ms
        let retryCount = 0;
        const maxRetries = 10;
        let joinedSuccessfully = false;

        while (retryCount < maxRetries && !joinedSuccessfully) {
          await new Promise(resolve => setTimeout(resolve, 500));
          retryCount++;

          imLogger.debug(`验证群组成员身份 (${retryCount}/${maxRetries})...`);

          // 尝试获取群组信息,如果能获取到说明已经加入
          const info = await getGroupInfo(props.groupID);
          if (info) {
            joinedSuccessfully = true;
            groupInfo.value = info;
            imLogger.debug(`成功加入群组! 群组信息:`, info);
          }
        }

        if (!joinedSuccessfully) {
          imLogger.error('加入群组超时,请稍后重试');
          antMessage.error('加入群组超时,请刷新页面重试');
          return;
        }

        imLogger.debug('群组成员验证完成,现在可以发送消息了');
        return; // 如果通过轮询已经获取到群组信息,直接返回
      }
    }

    // 从OpenIM获取群组详细信息
    const info = await getGroupInfo(props.groupID);
    if (info) {
      groupInfo.value = info;
      imLogger.debug('获取群组信息成功:', info);
    } else {
      imLogger.warn('未能获取群组信息,可能不在此群组中');
      antMessage.warning('您可能不在此群组中,无法发送消息');
    }
  } catch (error) {
    imLogger.error('获取群组信息失败:', error);
  }
};

/**
 * 加载历史消息
 */
const loadHistory = async (count = 20): Promise<void> => {
  if (isLoadingHistory.value || !hasMoreHistory.value || !props.groupID) {
    imLogger.debug('跳过加载历史消息', {
      isLoadingHistory: isLoadingHistory.value,
      hasMoreHistory: hasMoreHistory.value,
      groupID: props.groupID,
    });
    return;
  }

  try {
    isLoadingHistory.value = true;
    imLogger.debug('加载历史消息...', { groupID: props.groupID, count });

    // ✅ 修复: 使用标准的conversationID格式 "sg_<groupID>"
    const conversationID = `sg_${props.groupID}`;
    const startClientMsgID = messages.value.length > 0 ? messages.value[0].clientMsgID : '';
    imLogger.debug('调用SDK获取历史消息...', {
      conversationID,
      startClientMsgID,
      count,
      currentMessagesCount: messages.value.length,
    });

    const historyMessages = await sdk.getHistoryMessages(conversationID, count, startClientMsgID);
    imLogger.debug('SDK返回历史消息:', historyMessages.length, '条');

    // 打印SDK返回消息的顺序
    if (historyMessages.length > 0) {
      imLogger.debug('第一条 seq:', historyMessages[0].seq, '最后一条 seq:', historyMessages[historyMessages.length - 1].seq);
    }

    if (historyMessages.length === 0) {
      hasMoreHistory.value = false;
      imLogger.debug('没有更多历史消息');
    } else {
      // ✅ 修复: SDK返回的消息已经是正序(seq从小到大,最旧在前)
      // 直接添加到现有消息前面即可,不需要反转
      // 这样整体顺序就是: 最旧消息(seq最小) -> 最新消息(seq最大)
      messages.value = [...historyMessages, ...messages.value];
      imLogger.debug('加载历史消息成功:', historyMessages.length, '条, 当前总数:', messages.value.length);

      // 打印最终消息列表的顺序
      if (messages.value.length > 0) {
        imLogger.debug('第一条消息 seq:', messages.value[0].seq, '| 最后一条消息 seq:', messages.value[messages.value.length - 1].seq);
        imLogger.debug('应该是 seq 从小到大,最新消息在底部');
      }
    }
  } catch (error) {
    imLogger.error('加载历史消息失败:', error);
    antMessage.error('加载历史消息失败');
  } finally {
    isLoadingHistory.value = false;
  }
};

/**
 * 加载更多历史消息
 */
const handleLoadMore = async () => {
  await loadHistory(20);
};

/**
 * 发送文本消息
 */
const handleSendText = async (text: string) => {
  if (!text.trim() || !props.groupID) {
    imLogger.warn('发送消息失败: 文本为空或groupID未定义', { text, groupID: props.groupID });
    return;
  }

  // ✅ 检查用户是否已成功加入群组
  if (!groupInfo.value) {
    imLogger.error('发送消息失败: 用户未成功加入群组', { groupID: props.groupID });
    antMessage.error('您还未加入该群组,无法发送消息。请刷新页面重试');
    return;
  }

  try {
    imLogger.debug('发送文本消息...', { groupID: props.groupID, text });

    // ✅ 使用标准的conversationID格式 "sg_<groupID>"
    const conversationID = `sg_${props.groupID}`;
    const message = await sdk.sendTextMessage(conversationID, text.trim());

    imLogger.debug('SDK返回的消息:', message);

    if (message) {
      imLogger.debug('添加消息到列表前, 当前消息数:', messages.value.length);
      messages.value.push(message);
      imLogger.debug('文本消息发送成功, 当前消息数:', messages.value.length);
      imLogger.debug('当前消息列表:', messages.value);

      // 滚动到底部
      messageListRef.value?.scrollToBottom();
    } else {
      imLogger.warn('SDK返回的消息为null');
    }
  } catch (error) {
    imLogger.error('发送文本消息失败:', error);
    antMessage.error('发送文本消息失败');
  }
};

/**
 * 发送图片消息
 */
const handleSendImage = async (file: File) => {
  if (!props.groupID) {
    imLogger.warn('发送图片失败: groupID未定义', { groupID: props.groupID });
    return;
  }

  // ✅ 检查用户是否已成功加入群组
  if (!groupInfo.value) {
    imLogger.error('发送图片失败: 用户未成功加入群组', { groupID: props.groupID });
    antMessage.error('您还未加入该群组,无法发送图片。请刷新页面重试');
    return;
  }

  try {
    imLogger.debug('发送图片消息...', { groupID: props.groupID, file });

    // ✅ 使用标准的conversationID格式 "sg_<groupID>"
    const conversationID = `sg_${props.groupID}`;
    const message = await sdk.sendImageMessage(conversationID, file);

    imLogger.debug('SDK返回的图片消息:', message);

    if (message) {
      imLogger.debug('添加图片消息到列表前, 当前消息数:', messages.value.length);
      messages.value.push(message);
      imLogger.debug('图片消息发送成功, 当前消息数:', messages.value.length);
      imLogger.debug('当前消息列表:', messages.value);

      // 滚动到底部
      messageListRef.value?.scrollToBottom();
    } else {
      imLogger.warn('SDK返回的图片消息为null');
    }
  } catch (error) {
    imLogger.error('发送图片消息失败:', error);
    antMessage.error('发送图片失败');
  }
};

/**
 * 发送文件消息
 */
const handleSendFile = async (file: File) => {
  if (!props.groupID) {
    imLogger.warn('发送文件失败: groupID未定义', { groupID: props.groupID });
    return;
  }

  // ✅ 检查用户是否已成功加入群组
  if (!groupInfo.value) {
    imLogger.error('发送文件失败: 用户未成功加入群组', { groupID: props.groupID });
    antMessage.error('您还未加入该群组,无法发送文件。请刷新页面重试');
    return;
  }

  try {
    imLogger.debug('发送文件消息...', { groupID: props.groupID, file });

    // ✅ 使用标准的conversationID格式 "sg_<groupID>"
    const conversationID = `sg_${props.groupID}`;
    const message = await sdk.sendFileMessage(conversationID, file);

    imLogger.debug('SDK返回的文件消息:', message);

    if (message) {
      imLogger.debug('添加文件消息到列表前, 当前消息数:', messages.value.length);
      messages.value.push(message);
      imLogger.debug('文件消息发送成功, 当前消息数:', messages.value.length);
      imLogger.debug('当前消息列表:', messages.value);

      // 滚动到底部
      messageListRef.value?.scrollToBottom();
    } else {
      imLogger.warn('SDK返回的文件消息为null');
    }
  } catch (error) {
    imLogger.error('发送文件消息失败:', error);
    antMessage.error('发送文件失败');
  }
};

/**
 * 邀请成员
 */
const handleInviteMembers = () => {
  inviteModalVisible.value = true;
};

/**
 * 确认邀请成员
 */
const handleInviteConfirm = async () => {
  // TODO: 实现邀请成员逻辑
  antMessage.info('邀请成员功能开发中...');
  inviteModalVisible.value = false;
};

/**
 * 显示群组信息
 */
const handleShowGroupInfo = () => {
  groupInfoModalVisible.value = true;
};

/**
 * 格式化日期时间
 */
const formatDateTime = (timestamp: number): string => {
  return format(new Date(timestamp * 1000), 'yyyy-MM-dd HH:mm:ss');
};

// 清理函数列表
const cleanupFunctions: Array<() => void> = [];

/**
 * 组件挂载
 */
onMounted(() => {
  init();
});

/**
 * 组件卸载前清理
 */
onBeforeUnmount(() => {
  imLogger.debug('组件卸载,清理资源');
  cleanupFunctions.forEach(cleanup => cleanup());
});

/**
 * 监听groupID变化,重新初始化
 */
watch(
  () => props.groupID,
  () => {
    imLogger.debug('groupID变化,重新初始化');
    // 清理旧的监听器
    cleanupFunctions.forEach(cleanup => cleanup());
    cleanupFunctions.length = 0;
    // 重新初始化
    init();
  }
);
</script>

<style scoped lang="less">
.im-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.connection-error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 40px 20px;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;

  .header-left {
    display: flex;
    align-items: center;

    .group-info {
      .group-name {
        font-size: 14px;
        font-weight: 500;
        margin-bottom: 2px;
      }

      .group-members {
        font-size: 12px;
        color: #00000073;
      }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

.chat-body {
  flex: 1;
  overflow: hidden;
}

.chat-footer {
  flex-shrink: 0;
}
</style>
