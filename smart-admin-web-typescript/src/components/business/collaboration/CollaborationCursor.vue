<!--
  * 协作光标组件 - 类似飞书文档的实时光标显示
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-24
  * @Copyright 1024创新实验室
-->
<template>
  <div
    v-if="visible && isInViewport"
    ref="cursorRef"
    class="collaboration-cursor"
    :style="cursorStyle"
    @mouseenter="showTooltip = true"
    @mouseleave="showTooltip = false"
  >
    <!-- 光标指针 -->
    <div class="cursor-pointer" :style="{ borderLeftColor: user.color }">
      <div class="cursor-tail" :style="{ backgroundColor: user.color }"></div>
    </div>

    <!-- 用户标签 -->
    <div
      v-if="showLabel || showTooltip"
      class="cursor-label"
      :style="{ backgroundColor: user.color }"
    >
      <div class="user-avatar">
        <img v-if="user.avatar" :src="user.avatar" :alt="user.name" />
        <span v-else class="avatar-text">{{ getAvatarText(user.name) }}</span>
      </div>
      <span class="user-name">{{ user.name }}</span>
      <span v-if="fieldLabel" class="field-info">正在编辑: {{ fieldLabel }}</span>
    </div>

    <!-- 编辑提示动画 -->
    <div v-if="isEditing" class="editing-indicator">
      <div class="typing-dots">
        <span></span>
        <span></span>
        <span></span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue';

interface CollaborationUser {
  id: number;
  name: string;
  avatar?: string;
  color: string;
}

interface Props {
  user: CollaborationUser;
  fieldElement?: HTMLElement;
  fieldLabel?: string;
  isEditing?: boolean;
  showLabel?: boolean;
  visible?: boolean;
  position?: { x: number; y: number };
}

const props = withDefaults(defineProps<Props>(), {
  isEditing: false,
  showLabel: false,
  visible: true,
  position: () => ({ x: 0, y: 0 })
});

const cursorRef = ref<HTMLElement>();
const showTooltip = ref(false);
const cursorPosition = ref({ x: 0, y: 0 });

// 计算光标样式
const cursorStyle = computed(() => ({
  left: `${cursorPosition.value.x}px`,
  top: `${cursorPosition.value.y}px`,
  zIndex: 1000
}));

// 获取用户头像文字
const getAvatarText = (name: string): string => {
  if (!name) return '?';

  // 中文名取最后一个字
  if (/[\u4e00-\u9fa5]/.test(name)) {
    return name.slice(-1);
  }

  // 英文名取首字母
  const words = name.split(' ');
  return words.map(word => word.charAt(0)).join('').substring(0, 2).toUpperCase();
};

// 更新光标位置
const updateCursorPosition = () => {
  if (props.position.x !== 0 || props.position.y !== 0) {
    cursorPosition.value = props.position;
    return;
  }

  if (!props.fieldElement) return;

  const rect = props.fieldElement.getBoundingClientRect();
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
  const scrollLeft = window.pageXOffset || document.documentElement.scrollLeft;

  // 将光标定位在字段右上角
  cursorPosition.value = {
    x: rect.right + scrollLeft - 10,
    y: rect.top + scrollTop - 5
  };
};

// 监听字段元素变化
watch(() => props.fieldElement, () => {
  updateCursorPosition();
}, { immediate: true });

// 监听位置变化
watch(() => props.position, () => {
  updateCursorPosition();
}, { deep: true });

// 性能优化：节流处理窗口变化
const throttle = (func: Function, limit: number) => {
  let inThrottle: boolean;
  return function(...args: any[]) {
    if (!inThrottle) {
      func.apply(this, args);
      inThrottle = true;
      setTimeout(() => inThrottle = false, limit);
    }
  }
};

// 使用节流优化的窗口变化处理
const handleWindowChange = throttle(() => {
  updateCursorPosition();
}, 16); // 约60fps

// 可见性检测：只更新可见区域的光标
const isInViewport = computed(() => {
  if (!cursorPosition.value) return false;

  const viewportHeight = window.innerHeight;
  const viewportWidth = window.innerWidth;
  const scrollTop = window.pageYOffset;
  const scrollLeft = window.pageXOffset;

  return (
    cursorPosition.value.x >= scrollLeft - 100 &&
    cursorPosition.value.x <= scrollLeft + viewportWidth + 100 &&
    cursorPosition.value.y >= scrollTop - 100 &&
    cursorPosition.value.y <= scrollTop + viewportHeight + 100
  );
});

onMounted(() => {
  // 使用被动事件监听器优化性能
  window.addEventListener('scroll', handleWindowChange, { passive: true });
  window.addEventListener('resize', handleWindowChange, { passive: true });

  // 初始化位置
  nextTick(() => {
    updateCursorPosition();
  });
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleWindowChange);
  window.removeEventListener('resize', handleWindowChange);
});

// 暴露方法
defineExpose({
  updatePosition: updateCursorPosition
});
</script>

<style scoped>
.collaboration-cursor {
  position: fixed;
  pointer-events: auto;
  transition: all 0.2s ease;
  z-index: 1000;
}

.cursor-pointer {
  position: relative;
  width: 2px;
  height: 20px;
  border-left: 2px solid #1890ff;
  animation: cursorBlink 1s infinite;
}

.cursor-tail {
  position: absolute;
  top: 0;
  left: -1px;
  width: 8px;
  height: 8px;
  border-radius: 0 50% 50% 50%;
  transform: rotate(-45deg);
}

.cursor-label {
  position: absolute;
  top: -35px;
  left: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background-color: #1890ff;
  color: white;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  opacity: 0;
  animation: fadeInUp 0.3s ease forwards;
}

.cursor-label::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 8px;
  width: 0;
  height: 0;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 4px solid;
  border-top-color: inherit;
}

.user-avatar {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-text {
  font-size: 8px;
  font-weight: bold;
  color: white;
}

.user-name {
  font-weight: 500;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.field-info {
  opacity: 0.9;
  font-size: 11px;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.editing-indicator {
  position: absolute;
  top: 22px;
  left: 0;
}

.typing-dots {
  display: flex;
  gap: 2px;
}

.typing-dots span {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background-color: #1890ff;
  animation: typingDots 1.4s infinite;
}

.typing-dots span:nth-child(1) { animation-delay: 0s; }
.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes cursorBlink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes typingDots {
  0%, 80%, 100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  40% {
    opacity: 1;
    transform: scale(1);
  }
}

/* 不同颜色主题 */
.cursor-pointer[style*="border-left-color: #52c41a"] {
  border-left-color: #52c41a !important;
}

.cursor-pointer[style*="border-left-color: #faad14"] {
  border-left-color: #faad14 !important;
}

.cursor-pointer[style*="border-left-color: #f5222d"] {
  border-left-color: #f5222d !important;
}

.cursor-pointer[style*="border-left-color: #722ed1"] {
  border-left-color: #722ed1 !important;
}
</style>