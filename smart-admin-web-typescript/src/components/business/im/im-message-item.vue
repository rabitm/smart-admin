<template>
  <div class="im-message-item" :class="{ 'is-mine': isMine, 'is-other': !isMine }">
    <!-- 对方消息 - 头像在左边 -->
    <div v-if="!isMine" class="message-avatar">
      <a-avatar :src="message.senderFaceURL || undefined" :size="36">
        {{ message.senderNickname?.[0] || 'U' }}
      </a-avatar>
    </div>

    <!-- 消息内容区域 -->
    <div class="message-content-wrapper">
      <!-- 发送者昵称 (对方消息) -->
      <div v-if="!isMine" class="message-sender">{{ message.senderNickname }}</div>

      <!-- 消息气泡 -->
      <div class="message-bubble" :class="`message-type-${message.contentType}`">
        <!-- 文本消息 (101) -->
        <div v-if="message.contentType === 101" class="message-text">
          {{ getTextContent() }}
        </div>

        <!-- 图片消息 (102) -->
        <div v-else-if="message.contentType === 102" class="message-image">
          <img
            :src="getImageUrl()"
            alt="图片"
            @click="handlePreviewImage"
            style="max-width: 200px; max-height: 200px; cursor: pointer; border-radius: 4px"
          />
        </div>

        <!-- 文件消息 (105) -->
        <div v-else-if="message.contentType === 105" class="message-file">
          <file-outlined style="font-size: 24px; margin-right: 8px" />
          <div class="file-info">
            <div class="file-name">{{ getFileName() }}</div>
            <div class="file-size">{{ getFileSize() }}</div>
          </div>
          <a-button type="link" size="small" @click="handleDownloadFile">下载</a-button>
        </div>

        <!-- @ 消息 (106) -->
        <div v-else-if="message.contentType === 106" class="message-text message-at">
          {{ getTextContent() }}
        </div>

        <!-- 其他类型消息 -->
        <div v-else class="message-unsupported">
          <exclamation-circle-outlined />
          <span style="margin-left: 8px">不支持的消息类型 ({{ message.contentType }})</span>
        </div>
      </div>

      <!-- 消息时间和状态 -->
      <div class="message-meta">
        <span class="message-time">{{ formatTime(message.sendTime) }}</span>
        <span v-if="isMine && message.status === 1" class="message-status">
          <loading-outlined />
        </span>
        <span v-else-if="isMine && message.status === 2" class="message-status">
          <check-outlined style="color: #52c41a" />
        </span>
        <span v-else-if="isMine && message.status === 3" class="message-status">
          <close-circle-outlined style="color: #ff4d4f" />
        </span>
      </div>
    </div>

    <!-- 自己的消息 - 头像在右边 -->
    <div v-if="isMine" class="message-avatar">
      <a-avatar :src="message.senderFaceURL || undefined" :size="36">
        {{ message.senderNickname?.[0] || 'M' }}
      </a-avatar>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { message as antMessage } from 'ant-design-vue';
import {
  FileOutlined,
  ExclamationCircleOutlined,
  LoadingOutlined,
  CheckOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons-vue';
import type { ImMessage } from '/@/types/im';
import { format } from 'date-fns';

interface Props {
  message: ImMessage;
  currentUserID: string;
}

const props = defineProps<Props>();

// 是否是自己发送的消息
const isMine = computed(() => props.message.sendID === props.currentUserID);

/**
 * 获取文本内容
 */
const getTextContent = (): string => {
  try {
    // 优先使用 textElem (OpenIM SDK标准格式)
    if (props.message.textElem) {
      return props.message.textElem.content || '';
    }

    // 尝试解析 content 字段
    if (typeof props.message.content === 'string') {
      const content = JSON.parse(props.message.content);
      return content.content || content.text || props.message.content;
    }

    // 直接返回 content
    return props.message.content || '';
  } catch {
    return props.message.content || '';
  }
};

/**
 * 获取图片URL
 */
const getImageUrl = (): string => {
  try {
    // 优先使用 pictureElem (OpenIM SDK标准格式)
    if (props.message.pictureElem?.sourcePicture?.url) {
      return props.message.pictureElem.sourcePicture.url;
    }

    // 尝试解析 content 字段
    if (typeof props.message.content === 'string') {
      const content = JSON.parse(props.message.content);
      return content.sourcePicture?.url || content.url || '';
    }

    return '';
  } catch {
    return '';
  }
};

/**
 * 获取文件名
 */
const getFileName = (): string => {
  try {
    // 优先使用 fileElem (OpenIM SDK标准格式)
    if (props.message.fileElem?.fileName) {
      return props.message.fileElem.fileName;
    }

    // 尝试解析 content 字段
    if (typeof props.message.content === 'string') {
      const content = JSON.parse(props.message.content);
      return content.fileName || '未知文件';
    }

    return '未知文件';
  } catch {
    return '未知文件';
  }
};

/**
 * 获取文件大小
 */
const getFileSize = (): string => {
  try {
    let fileSize = 0;

    // 优先使用 fileElem (OpenIM SDK标准格式)
    if (props.message.fileElem?.fileSize) {
      fileSize = props.message.fileElem.fileSize;
    } else if (typeof props.message.content === 'string') {
      const content = JSON.parse(props.message.content);
      fileSize = content.fileSize || 0;
    }

    // 格式化文件大小
    if (fileSize < 1024) {
      return `${fileSize} B`;
    } else if (fileSize < 1024 * 1024) {
      return `${(fileSize / 1024).toFixed(2)} KB`;
    } else {
      return `${(fileSize / 1024 / 1024).toFixed(2)} MB`;
    }
  } catch {
    return '未知大小';
  }
};

/**
 * 格式化时间
 */
const formatTime = (timestamp: number): string => {
  if (!timestamp) {
    return '';
  }

  const date = new Date(timestamp);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);

  if (diffMins < 1) {
    return '刚刚';
  } else if (diffMins < 60) {
    return `${diffMins}分钟前`;
  } else if (diffMins < 1440) {
    return format(date, 'HH:mm');
  } else {
    return format(date, 'MM-dd HH:mm');
  }
};

/**
 * 预览图片
 */
const handlePreviewImage = () => {
  // TODO: 集成图片预览组件
  console.log('预览图片:', getImageUrl());
};

/**
 * 下载文件
 */
const handleDownloadFile = () => {
  try {
    let fileUrl = '';

    // 优先使用 fileElem (OpenIM SDK标准格式)
    if (props.message.fileElem?.sourceUrl) {
      fileUrl = props.message.fileElem.sourceUrl;
    } else if (typeof props.message.content === 'string') {
      const content = JSON.parse(props.message.content);
      fileUrl = content.sourceUrl || content.url || '';
    }

    if (fileUrl) {
      window.open(fileUrl, '_blank');
    } else {
      antMessage.error('文件链接不存在');
    }
  } catch (error) {
    console.error('下载文件失败:', error);
    antMessage.error('下载文件失败');
  }
};
</script>

<style scoped lang="less">
.im-message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;

  &.is-mine {
    flex-direction: row-reverse;

    .message-content-wrapper {
      align-items: flex-end;
    }

    .message-bubble {
      background: #1677ff;
      color: #fff;
    }

    .message-meta {
      flex-direction: row-reverse;
    }
  }

  &.is-other {
    flex-direction: row;

    .message-content-wrapper {
      align-items: flex-start;
    }

    .message-bubble {
      background: #f5f5f5;
      color: #000000d9;
    }
  }
}

.message-avatar {
  flex-shrink: 0;
}

.message-content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 60%;
}

.message-sender {
  font-size: 12px;
  color: #00000073;
  padding: 0 8px;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 8px;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.message-text {
  line-height: 1.5;
  white-space: pre-wrap;
}

.message-at {
  background: #fff7e6;
  border: 1px solid #ffd591;
}

.message-file {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  min-width: 200px;

  .file-info {
    flex: 1;

    .file-name {
      font-size: 14px;
      font-weight: 500;
      margin-bottom: 4px;
    }

    .file-size {
      font-size: 12px;
      color: #00000073;
    }
  }
}

.message-image {
  padding: 0;
  background: transparent;
}

.message-unsupported {
  display: flex;
  align-items: center;
  color: #00000073;
  font-size: 13px;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 8px;

  .message-time {
    font-size: 12px;
    color: #00000045;
  }

  .message-status {
    font-size: 12px;
  }
}
</style>
