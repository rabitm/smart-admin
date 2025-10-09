<template>
  <div class="im-message-list" ref="messageListRef">
    <!-- 加载更多按钮 -->
    <div v-if="hasMore && !isLoading" class="load-more-wrapper">
      <a-button type="link" size="small" @click="handleLoadMore">加载更多消息</a-button>
    </div>

    <!-- 加载中 -->
    <div v-if="isLoading" class="loading-wrapper">
      <a-spin size="small" />
      <span style="margin-left: 8px">加载中...</span>
    </div>

    <!-- 消息列表为空 -->
    <div v-if="messages.length === 0 && !isLoading" class="empty-wrapper">
      <a-empty description="暂无消息" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
    </div>

    <!-- 消息列表 -->
    <div class="messages-wrapper">
      <im-message-item
        v-for="message in messages"
        :key="message.clientMsgID"
        :message="message"
        :current-user-id="currentUserID"
      />
    </div>

    <!-- 滚动到底部按钮 -->
    <transition name="fade">
      <div v-show="showScrollToBottom" class="scroll-to-bottom" @click="scrollToBottom">
        <down-outlined />
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onBeforeUnmount } from 'vue';
import { Empty } from 'ant-design-vue';
import { DownOutlined } from '@ant-design/icons-vue';
import ImMessageItem from './im-message-item.vue';
import type { ImMessage } from '/@/types/im';

interface Props {
  messages: ImMessage[];
  currentUserID?: string;
  isLoading?: boolean;
  hasMore?: boolean;
}

interface Emits {
  (e: 'load-more'): void;
}

const props = withDefaults(defineProps<Props>(), {
  currentUserID: '0',
  isLoading: false,
  hasMore: true,
});

const emit = defineEmits<Emits>();

const messageListRef = ref<HTMLElement | null>(null);
const showScrollToBottom = ref(false);
const isUserScrolling = ref(false);
const scrollThreshold = 100; // 距离底部多少像素时不显示"滚动到底部"按钮

/**
 * 滚动到底部
 */
const scrollToBottom = (smooth = true) => {
  if (!messageListRef.value) {
    return;
  }

  const scrollOptions: ScrollToOptions = {
    top: messageListRef.value.scrollHeight,
    behavior: smooth ? 'smooth' : 'auto',
  };

  messageListRef.value.scrollTo(scrollOptions);
};

/**
 * 处理滚动事件
 */
const handleScroll = () => {
  if (!messageListRef.value) {
    return;
  }

  const { scrollTop, scrollHeight, clientHeight } = messageListRef.value;
  const distanceToBottom = scrollHeight - scrollTop - clientHeight;

  // 判断是否显示"滚动到底部"按钮
  showScrollToBottom.value = distanceToBottom > scrollThreshold;

  // 判断用户是否在手动滚动
  isUserScrolling.value = distanceToBottom > scrollThreshold;
};

/**
 * 加载更多消息
 */
const handleLoadMore = () => {
  if (props.isLoading || !props.hasMore) {
    return;
  }

  // 记录当前滚动位置
  const scrollTop = messageListRef.value?.scrollTop || 0;

  emit('load-more');

  // 加载完成后恢复滚动位置 (避免跳动)
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = scrollTop;
    }
  });
};

/**
 * 监听消息列表变化,自动滚动到底部 (仅当用户未手动滚动时)
 */
watch(
  () => props.messages.length,
  (newLength, oldLength) => {
    // 只有新增消息时才自动滚动
    if (newLength > oldLength && !isUserScrolling.value) {
      nextTick(() => {
        scrollToBottom(true);
      });
    }
  }
);

/**
 * 组件挂载后滚动到底部
 */
onMounted(() => {
  if (messageListRef.value) {
    messageListRef.value.addEventListener('scroll', handleScroll);
  }

  nextTick(() => {
    scrollToBottom(false);
  });
});

/**
 * 组件卸载前移除事件监听
 */
onBeforeUnmount(() => {
  if (messageListRef.value) {
    messageListRef.value.removeEventListener('scroll', handleScroll);
  }
});

// 暴露给父组件的方法
defineExpose({
  scrollToBottom,
});
</script>

<style scoped lang="less">
.im-message-list {
  position: relative;
  height: 100%;
  overflow-y: auto;
  padding: 16px;
  background: #fafafa;

  // 自定义滚动条样式
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: #d9d9d9;
    border-radius: 3px;

    &:hover {
      background: #bfbfbf;
    }
  }
}

.load-more-wrapper {
  text-align: center;
  padding: 12px 0;
}

.loading-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 0;
  color: #00000073;
  font-size: 13px;
}

.empty-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 200px;
}

.messages-wrapper {
  display: flex;
  flex-direction: column;
}

.scroll-to-bottom {
  position: absolute;
  right: 24px;
  bottom: 24px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  transition: all 0.3s;
  z-index: 10;

  &:hover {
    background: #f5f5f5;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  }

  &:active {
    transform: translateY(0);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
