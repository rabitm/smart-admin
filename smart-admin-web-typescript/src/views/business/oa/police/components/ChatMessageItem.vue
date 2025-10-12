<!--
  聊天消息项组件
  @Author Claude Code Assistant
  @Date 2025-10-09
  @Copyright 1024创新实验室
-->

<template>
  <div :class="['chat-message-item', isCurrentUser ? 'own-message' : 'other-message']">
    <!-- 发送者头像 -->
    <div v-if="!isCurrentUser" class="message-avatar">
      <a-avatar :size="32">
        {{ message.senderNickname?.charAt(0) || '?' }}
      </a-avatar>
    </div>

    <div class="message-content-wrapper">
      <!-- 发送者昵称 -->
      <div v-if="!isCurrentUser" class="message-sender">
        {{ message.senderNickname || '未知用户' }}
      </div>

      <!-- 消息内容 -->
      <div class="message-content">
        <!-- 文本消息 -->
        <div v-if="message.contentType === 101" class="message-text">
          {{ getTextContent() }}
        </div>

        <!-- 图片消息 -->
        <div v-else-if="message.contentType === 102" class="message-image">
          <a-image :src="getImageUrl()" :width="200" />
        </div>

        <!-- 语音消息 -->
        <div v-else-if="message.contentType === 103" class="message-voice">
          <a-button size="small" @click="playVoice">
            <template #icon><SoundOutlined /></template>
            {{ getVoiceDuration() }}秒
          </a-button>
        </div>

        <!-- 视频消息 -->
        <div v-else-if="message.contentType === 104" class="message-video">
          <video :src="getVideoUrl()" controls style="max-width: 300px;" />
        </div>

        <!-- 文件消息 -->
        <div v-else-if="message.contentType === 106" class="message-file">
          <a-button @click="downloadFile">
            <template #icon><DownloadOutlined /></template>
            {{ getFileName() }}
          </a-button>
        </div>

        <!-- 位置消息 -->
        <div v-else-if="message.contentType === 107" class="message-location">
          <EnvironmentOutlined />
          {{ getLocationInfo() }}
        </div>

        <!-- 其他类型消息 -->
        <div v-else class="message-unsupported">
          [不支持的消息类型]
        </div>
      </div>

      <!-- 消息时间 -->
      <div class="message-time">
        {{ formatTime(message.sendTime) }}
      </div>
    </div>

    <!-- 当前用户头像 -->
    <div v-if="isCurrentUser" class="message-avatar">
      <a-avatar :size="32">
        {{ message.senderNickname?.charAt(0) || '我' }}
      </a-avatar>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { SoundOutlined, DownloadOutlined, EnvironmentOutlined } from '@ant-design/icons-vue';
import type { MessageItem } from '/@/utils/openim-client';
import { format } from 'date-fns';

// 组件属性
const props = defineProps<{
  message: MessageItem;
  currentUserId: string;
}>();

// 是否为当前用户发送的消息
const isCurrentUser = computed(() => {
  return props.message.sendID === props.currentUserId;
});

/**
 * 获取文本消息内容
 */
function getTextContent(): string {
  try {
    const content = JSON.parse(props.message.content);
    return content.content || props.message.content;
  } catch {
    return props.message.content;
  }
}

/**
 * 获取图片URL
 */
function getImageUrl(): string {
  try {
    const content = JSON.parse(props.message.content);
    return content.sourcePicture?.url || content.bigPicture?.url || '';
  } catch {
    return '';
  }
}

/**
 * 获取语音时长
 */
function getVoiceDuration(): number {
  try {
    const content = JSON.parse(props.message.content);
    return content.duration || 0;
  } catch {
    return 0;
  }
}

/**
 * 播放语音
 */
function playVoice() {
  try {
    const content = JSON.parse(props.message.content);
    const audioUrl = content.sourceUrl;
    if (audioUrl) {
      const audio = new Audio(audioUrl);
      audio.play();
    }
  } catch (error) {
    console.error('播放语音失败:', error);
  }
}

/**
 * 获取视频URL
 */
function getVideoUrl(): string {
  try {
    const content = JSON.parse(props.message.content);
    return content.videoUrl || '';
  } catch {
    return '';
  }
}

/**
 * 获取文件名
 */
function getFileName(): string {
  try {
    const content = JSON.parse(props.message.content);
    return content.fileName || '未知文件';
  } catch {
    return '未知文件';
  }
}

/**
 * 下载文件
 */
function downloadFile() {
  try {
    const content = JSON.parse(props.message.content);
    const fileUrl = content.sourceUrl;
    if (fileUrl) {
      window.open(fileUrl, '_blank');
    }
  } catch (error) {
    console.error('下载文件失败:', error);
  }
}

/**
 * 获取位置信息
 */
function getLocationInfo(): string {
  try {
    const content = JSON.parse(props.message.content);
    return content.description || `${content.latitude}, ${content.longitude}`;
  } catch {
    return '位置信息';
  }
}

/**
 * 格式化时间
 */
function formatTime(timestamp: number): string {
  return format(new Date(timestamp), 'HH:mm');
}
</script>

<style scoped lang="less">
.chat-message-item {
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

  .message-time {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
  }

  .message-text {
    line-height: 1.5;
  }

  .message-image,
  .message-video {
    :deep(img),
    :deep(video) {
      border-radius: 4px;
      display: block;
    }
  }

  .message-voice,
  .message-file,
  .message-location {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .message-unsupported {
    color: #999;
    font-style: italic;
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
    background-color: #f0f0f0;
    color: #333;
  }

  .message-time {
    text-align: left;
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

  .message-time {
    text-align: right;
  }
}
</style>
