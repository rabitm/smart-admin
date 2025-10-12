<!--
  消息已读成员列表组件
  @Author Claude Code Assistant
  @Date 2025-10-11
  @Copyright 1024创新实验室
-->

<template>
  <a-modal
    v-model:visible="visible"
    title="消息详情"
    width="500px"
    :footer="null"
    @cancel="handleClose"
  >
    <a-tabs v-model:activeKey="activeTab">
      <!-- 已读成员 -->
      <a-tab-pane key="read" :tab="`已读 (${readMembers.length})`">
        <div class="member-list">
          <a-empty v-if="readMembers.length === 0" description="暂无已读成员" />

          <div
            v-for="member in readMembers"
            :key="member.userID"
            class="member-item"
          >
            <a-avatar :size="32" :src="member.faceURL">
              {{ member.nickname?.charAt(0) || '?' }}
            </a-avatar>

            <div class="member-info">
              <div class="member-name">{{ member.nickname || '未知用户' }}</div>
              <div class="member-read-time">{{ formatReadTime(member.readTime) }}</div>
            </div>

            <div class="member-status">
              <span class="read-badge">✓✓</span>
            </div>
          </div>
        </div>
      </a-tab-pane>

      <!-- 未读成员 -->
      <a-tab-pane key="unread" :tab="`未读 (${unreadMembers.length})`">
        <div class="member-list">
          <a-empty v-if="unreadMembers.length === 0" description="全部成员已读" />

          <div
            v-for="member in unreadMembers"
            :key="member.userID"
            class="member-item"
          >
            <a-avatar :size="32" :src="member.faceURL">
              {{ member.nickname?.charAt(0) || '?' }}
            </a-avatar>

            <div class="member-info">
              <div class="member-name">{{ member.nickname || '未知用户' }}</div>
              <div class="member-status-text">未读</div>
            </div>

            <div class="member-status">
              <span class="unread-badge">✓</span>
            </div>
          </div>
        </div>
      </a-tab-pane>
    </a-tabs>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { format } from 'date-fns';
import type { GroupMemberItem } from '@openim/wasm-client-sdk';
import { openIMClient } from '/@/utils/openim-client';

interface Props {
  modelValue: boolean;
  messageId: string;
  conversationID: string;
  hasReadCount?: number;
  unreadCount?: number;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
}>();

const visible = ref(false);
const activeTab = ref('read');
const readMembers = ref<any[]>([]);
const unreadMembers = ref<any[]>([]);
const loading = ref(false);

// 监听显示状态变化
watch(
  () => props.modelValue,
  async (newValue) => {
    visible.value = newValue;

    if (newValue) {
      // 打开弹窗时加载成员列表
      await loadMembers();
    }
  },
  { immediate: true }
);

// 监听弹窗关闭
watch(visible, (newValue) => {
  if (!newValue) {
    emit('update:modelValue', false);
  }
});

/**
 * 加载已读/未读成员列表
 */
async function loadMembers() {
  try {
    loading.value = true;

    console.log('📖 [已读列表] 加载成员列表, messageId:', props.messageId);

    // 获取群组消息已读详情
    const readStatusMap = await openIMClient.getGroupMessageReadReceipt(
      props.conversationID,
      [props.messageId]
    );

    const status = readStatusMap.get(props.messageId);

    if (status) {
      // 已读成员列表
      readMembers.value = status.readMembers || [];

      console.log('✅ [已读列表] 已读成员数:', readMembers.value.length);

      // 获取群组所有成员
      const groupID = extractGroupIDFromConversation(props.conversationID);
      if (groupID) {
        const allMembers = await openIMClient.getGroupMembers(groupID);

        // 计算未读成员 (所有成员 - 已读成员)
        const readUserIDs = new Set(readMembers.value.map(m => m.userID));
        unreadMembers.value = allMembers.filter(m => !readUserIDs.has(m.userID));

        console.log('✅ [已读列表] 未读成员数:', unreadMembers.value.length);
      }
    }
  } catch (error) {
    console.error('❌ [已读列表] 加载成员列表失败:', error);
  } finally {
    loading.value = false;
  }
}

/**
 * 从会话ID中提取群组ID
 */
function extractGroupIDFromConversation(conversationID: string): string | null {
  // conversationID 格式: sg_groupID 或 si_groupID
  const match = conversationID.match(/^s[gi]_(.+)$/);
  return match ? match[1] : null;
}

/**
 * 格式化已读时间
 */
function formatReadTime(timestamp: number): string {
  if (!timestamp) return '';

  const now = Date.now();
  const diff = now - timestamp;

  // 1分钟内
  if (diff < 60 * 1000) {
    return '刚刚';
  }

  // 1小时内
  if (diff < 60 * 60 * 1000) {
    const minutes = Math.floor(diff / (60 * 1000));
    return `${minutes}分钟前`;
  }

  // 24小时内
  if (diff < 24 * 60 * 60 * 1000) {
    return format(new Date(timestamp), 'HH:mm');
  }

  // 超过24小时
  return format(new Date(timestamp), 'MM-dd HH:mm');
}

/**
 * 关闭弹窗
 */
function handleClose() {
  visible.value = false;
}
</script>

<style scoped lang="less">
.member-list {
  max-height: 400px;
  overflow-y: auto;

  .member-item {
    display: flex;
    align-items: center;
    padding: 12px;
    border-bottom: 1px solid #f0f0f0;
    transition: all 0.3s;

    &:hover {
      background-color: #fafafa;
    }

    &:last-child {
      border-bottom: none;
    }

    .member-info {
      flex: 1;
      margin-left: 12px;

      .member-name {
        font-size: 14px;
        font-weight: 500;
        color: #333;
        margin-bottom: 4px;
      }

      .member-read-time {
        font-size: 12px;
        color: #999;
      }

      .member-status-text {
        font-size: 12px;
        color: #999;
      }
    }

    .member-status {
      .read-badge {
        color: #52c41a;
        font-weight: bold;
        font-size: 16px;
      }

      .unread-badge {
        color: #d9d9d9;
        font-size: 16px;
      }
    }
  }
}

// 滚动条样式
.member-list::-webkit-scrollbar {
  width: 6px;
}

.member-list::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.member-list::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.3);
}
</style>
