<!--
  * 生产级光标渲染器 - 精确跟踪DOM元素位置
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-24
  * @Copyright 1024创新实验室
-->
<template>
  <div class="production-cursor-renderer">
    <!-- 渲染每个用户的光标 -->
    <div
      v-for="cursor in visibleCursors"
      :key="`cursor-${cursor.userId}-${cursor.fieldName}`"
      class="user-cursor"
      :class="{
        'cursor-active': cursor.isActive,
        'cursor-inactive': !cursor.isActive
      }"
      :style="getCursorStyle(cursor)"
    >
      <!-- 光标线 -->
      <div
        class="cursor-line"
        :style="{
          backgroundColor: getUserColor(cursor.userId),
          height: `${cursor.position.height - 4}px`
        }"
      ></div>

      <!-- 用户标签 -->
      <div
        v-if="cursor.isActive && showUserLabels"
        class="cursor-label"
        :style="{
          backgroundColor: getUserColor(cursor.userId),
          transform: getLabelTransform(cursor)
        }"
      >
        <div class="label-content">
          <img
            v-if="getUserAvatar(cursor.userId)"
            :src="getUserAvatar(cursor.userId)"
            :alt="getUserName(cursor.userId)"
            class="label-avatar"
          />
          <span v-else class="label-avatar-text">
            {{ getAvatarText(getUserName(cursor.userId)) }}
          </span>
          <span class="label-name">{{ getUserName(cursor.userId) }}</span>
        </div>
      </div>

      <!-- 光标动画效果 -->
      <div
        v-if="cursor.isActive"
        class="cursor-pulse"
        :style="{
          backgroundColor: getUserColor(cursor.userId)
        }"
      ></div>
    </div>

    <!-- 字段高亮显示 -->
    <div
      v-for="highlight in fieldHighlights"
      :key="`highlight-${highlight.fieldName}`"
      class="field-highlight"
      :style="getHighlightStyle(highlight)"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';

interface UserCursor {
  userId: string;
  fieldName: string;
  position: {
    x: number;
    y: number;
    width: number;
    height: number;
    element: HTMLElement;
  };
  caretPosition?: number;
  isActive: boolean;
  timestamp: number;
}

interface ActiveUser {
  id: string;
  name: string;
  avatar?: string;
  color: string;
  sessionId: string;
  lastActivity: number;
}

interface FieldHighlight {
  fieldName: string;
  position: {
    x: number;
    y: number;
    width: number;
    height: number;
  };
  userId: string;
  type: 'editing' | 'focused' | 'conflict';
}

interface Props {
  cursors: UserCursor[];
  users: ActiveUser[];
  showUserLabels?: boolean;
  maxVisibleCursors?: number;
  enableHighlights?: boolean;
  cursorBlinkInterval?: number;
}

const props = withDefaults(defineProps<Props>(), {
  showUserLabels: true,
  maxVisibleCursors: 20,
  enableHighlights: true,
  cursorBlinkInterval: 1000
});

const emit = defineEmits<{
  cursorClick: [cursor: UserCursor];
  userHover: [userId: string];
}>();

// 响应式状态
const renderFrame = ref(0);
const isBlinking = ref(true);

// 计算属性
const visibleCursors = computed(() => {
  return props.cursors
    .filter(cursor => {
      // 过滤掉过期的光标
      const now = Date.now();
      const maxAge = 30000; // 30秒
      return now - cursor.timestamp < maxAge;
    })
    .sort((a, b) => {
      // 活跃的光标优先显示
      if (a.isActive && !b.isActive) return -1;
      if (!a.isActive && b.isActive) return 1;
      return b.timestamp - a.timestamp;
    })
    .slice(0, props.maxVisibleCursors);
});

const fieldHighlights = computed(() => {
  if (!props.enableHighlights) return [];

  const highlights: FieldHighlight[] = [];
  const fieldUsers = new Map<string, UserCursor[]>();

  // 按字段分组光标
  props.cursors.forEach(cursor => {
    if (!cursor.isActive) return;

    const fieldCursors = fieldUsers.get(cursor.fieldName) || [];
    fieldCursors.push(cursor);
    fieldUsers.set(cursor.fieldName, fieldCursors);
  });

  // 为每个有活跃光标的字段创建高亮
  fieldUsers.forEach((cursors, fieldName) => {
    const activeCursors = cursors.filter(c => c.isActive);
    if (activeCursors.length === 0) return;

    const firstCursor = activeCursors[0];
    const type = activeCursors.length > 1 ? 'conflict' : 'editing';

    highlights.push({
      fieldName,
      position: firstCursor.position,
      userId: firstCursor.userId,
      type
    });
  });

  return highlights;
});

// 方法
const getCursorStyle = (cursor: UserCursor) => {
  const { position, caretPosition } = cursor;

  // 计算光标在字段内的精确位置
  let offsetX = 2; // 默认左边距

  if (caretPosition !== undefined && position.element) {
    // 对于输入框，根据光标位置计算偏移
    if (position.element instanceof HTMLInputElement || position.element instanceof HTMLTextAreaElement) {
      const element = position.element;

      // 创建临时元素计算文本宽度
      const tempSpan = document.createElement('span');
      tempSpan.style.visibility = 'hidden';
      tempSpan.style.position = 'absolute';
      tempSpan.style.whiteSpace = 'pre';
      tempSpan.style.font = window.getComputedStyle(element).font;

      const textBeforeCaret = element.value.substring(0, caretPosition);
      tempSpan.textContent = textBeforeCaret;

      document.body.appendChild(tempSpan);
      offsetX = tempSpan.offsetWidth + 2;
      document.body.removeChild(tempSpan);

      // 处理滚动偏移
      offsetX -= element.scrollLeft;
    }
  }

  return {
    position: 'absolute',
    left: `${position.x + offsetX}px`,
    top: `${position.y + 2}px`,
    zIndex: 1000,
    pointerEvents: cursor.isActive ? 'auto' : 'none',
    opacity: cursor.isActive ? 1 : 0.6,
    transition: 'all 0.2s ease'
  };
};

const getLabelTransform = (cursor: UserCursor) => {
  // 计算标签位置，避免超出视口
  const viewportWidth = window.innerWidth;
  const viewportHeight = window.innerHeight;
  const labelWidth = 120; // 估算标签宽度
  const labelHeight = 32; // 估算标签高度

  let translateX = 0;
  let translateY = -labelHeight - 8; // 默认显示在光标上方

  // 如果标签会超出右边界，向左偏移
  if (cursor.position.x + labelWidth > viewportWidth) {
    translateX = -(cursor.position.x + labelWidth - viewportWidth + 20);
  }

  // 如果标签会超出上边界，显示在下方
  if (cursor.position.y + translateY < 0) {
    translateY = cursor.position.height + 8;
  }

  return `translate(${translateX}px, ${translateY}px)`;
};

const getHighlightStyle = (highlight: FieldHighlight) => {
  const { position, type } = highlight;

  let borderColor = '#1890ff';
  let backgroundColor = 'rgba(24, 144, 255, 0.1)';

  if (type === 'conflict') {
    borderColor = '#ff4d4f';
    backgroundColor = 'rgba(255, 77, 79, 0.1)';
  }

  return {
    position: 'absolute',
    left: `${position.x - 2}px`,
    top: `${position.y - 2}px`,
    width: `${position.width + 4}px`,
    height: `${position.height + 4}px`,
    border: `2px solid ${borderColor}`,
    backgroundColor,
    borderRadius: '4px',
    pointerEvents: 'none',
    zIndex: 999,
    animation: type === 'conflict' ? 'conflictPulse 1s ease-in-out infinite alternate' : 'none'
  };
};

const getUserColor = (userId: string): string => {
  const user = props.users.find(u => u.id === userId);
  return user?.color || '#1890ff';
};

const getUserName = (userId: string): string => {
  const user = props.users.find(u => u.id === userId);
  return user?.name || '未知用户';
};

const getUserAvatar = (userId: string): string | undefined => {
  const user = props.users.find(u => u.id === userId);
  return user?.avatar;
};

const getAvatarText = (name: string): string => {
  if (!name) return '?';

  // 提取中文姓名的最后一个字，或英文名的首字母
  const trimmed = name.trim();
  if (/[\u4e00-\u9fff]/.test(trimmed)) {
    return trimmed.charAt(trimmed.length - 1);
  } else {
    return trimmed.charAt(0).toUpperCase();
  }
};

// 光标闪烁动画
let blinkInterval: NodeJS.Timeout | null = null;

const startBlinking = () => {
  if (blinkInterval) return;

  blinkInterval = setInterval(() => {
    isBlinking.value = !isBlinking.value;
  }, props.cursorBlinkInterval);
};

const stopBlinking = () => {
  if (blinkInterval) {
    clearInterval(blinkInterval);
    blinkInterval = null;
  }
};

// 渲染帧更新
let animationFrame: number | null = null;

const updateRenderFrame = () => {
  renderFrame.value++;
  animationFrame = requestAnimationFrame(updateRenderFrame);
};

const stopRenderFrame = () => {
  if (animationFrame) {
    cancelAnimationFrame(animationFrame);
    animationFrame = null;
  }
};

// 生命周期
onMounted(() => {
  startBlinking();
  updateRenderFrame();
});

onUnmounted(() => {
  stopBlinking();
  stopRenderFrame();
});

// 监听光标变化
watch(() => props.cursors, (newCursors) => {
  // 当有活跃光标时开始闪烁动画
  const hasActiveCursors = newCursors.some(cursor => cursor.isActive);

  if (hasActiveCursors && !blinkInterval) {
    startBlinking();
  } else if (!hasActiveCursors && blinkInterval) {
    stopBlinking();
    isBlinking.value = true; // 重置为可见状态
  }
}, { immediate: true });

// 暴露组件方法
defineExpose({
  refreshPositions: () => {
    renderFrame.value++;
  },
  getCursorCount: () => visibleCursors.value.length,
  getHighlightCount: () => fieldHighlights.value.length
});
</script>

<style scoped>
.production-cursor-renderer {
  position: relative;
  pointer-events: none;
}

.user-cursor {
  position: absolute;
  pointer-events: none;
  transition: opacity 0.3s ease;
}

.cursor-active {
  opacity: 1;
}

.cursor-inactive {
  opacity: 0.6;
}

.cursor-line {
  width: 2px;
  border-radius: 1px;
  animation: cursorBlink 1s ease-in-out infinite alternate;
  position: relative;
}

@keyframes cursorBlink {
  0% { opacity: 1; }
  100% { opacity: 0.3; }
}

.cursor-label {
  position: absolute;
  left: 4px;
  background: var(--cursor-color, #1890ff);
  color: white;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 1001;
  pointer-events: auto;
  transition: transform 0.2s ease;
}

.cursor-label::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 8px;
  width: 0;
  height: 0;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 4px solid var(--cursor-color, #1890ff);
}

.label-content {
  display: flex;
  align-items: center;
  gap: 6px;
}

.label-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
}

.label-avatar-text {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
}

.label-name {
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cursor-pulse {
  position: absolute;
  top: -2px;
  left: -2px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  opacity: 0.8;
  animation: cursorPulse 2s ease-in-out infinite;
}

@keyframes cursorPulse {
  0% {
    transform: scale(1);
    opacity: 0.8;
  }
  50% {
    transform: scale(1.5);
    opacity: 0.4;
  }
  100% {
    transform: scale(1);
    opacity: 0.8;
  }
}

.field-highlight {
  position: absolute;
  pointer-events: none;
  border-radius: 4px;
  transition: all 0.2s ease;
}

@keyframes conflictPulse {
  0% {
    border-color: #ff4d4f;
    background-color: rgba(255, 77, 79, 0.1);
  }
  100% {
    border-color: #ff7875;
    background-color: rgba(255, 77, 79, 0.2);
  }
}

/* 响应式优化 */
@media (max-width: 768px) {
  .cursor-label {
    padding: 2px 6px;
    font-size: 11px;
  }

  .label-avatar,
  .label-avatar-text {
    width: 16px;
    height: 16px;
  }

  .label-name {
    max-width: 60px;
  }
}

/* 高分辨率屏幕优化 */
@media (min-resolution: 2dppx) {
  .cursor-line {
    width: 1.5px;
  }
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .cursor-label {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  }

  .field-highlight {
    background-color: rgba(24, 144, 255, 0.15);
  }
}
</style>