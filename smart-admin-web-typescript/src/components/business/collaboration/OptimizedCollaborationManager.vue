<!--
  * 高性能协作管理器组件 - 优化版本
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-24
  * @Copyright 1024创新实验室
-->
<template>
  <div class="optimized-collaboration-manager">
    <!-- 虚拟化光标层 - 只渲染可见光标 -->
    <CollaborationCursor
      v-for="cursor in visibleCursors"
      :key="`cursor-${cursor.user.id}-${cursor.fieldName}`"
      :user="cursor.user"
      :field-element="cursor.fieldElement"
      :field-label="cursor.fieldLabel"
      :is-editing="cursor.isEditing"
      :show-label="cursor.showLabel"
      :visible="cursor.visible"
      :position="cursor.position"
    />

    <!-- 协作状态面板 - 使用虚拟滚动 -->
    <div v-if="showCollaborationPanel && collaborators.length > 0" class="collaboration-panel">
      <div class="panel-header">
        <span class="panel-title">🤝 协作中</span>
        <span class="collaborator-count">{{ collaborators.length }}人</span>
        <div class="performance-stats" v-if="showPerformanceStats">
          <span class="stat-item">渲染: {{ renderStats.cursors }}/{{ totalCursors }}</span>
          <span class="stat-item">FPS: {{ Math.round(performanceStats.fps) }}</span>
        </div>
      </div>

      <!-- 虚拟滚动容器 -->
      <div
        ref="collaboratorListRef"
        class="collaborator-list"
        @scroll="handleListScroll"
      >
        <div :style="{ height: `${totalListHeight}px` }" class="virtual-list-spacer">
          <div
            v-for="user in visibleCollaborators"
            :key="user.id"
            class="collaborator-item"
            :style="{
              borderLeftColor: user.color,
              transform: `translateY(${user.virtualOffset}px)`
            }"
          >
            <div class="user-avatar" :style="{ backgroundColor: user.color }">
              <canvas
                v-if="user.avatarCanvas"
                ref="avatarCanvas"
                :width="32"
                :height="32"
              ></canvas>
              <span v-else class="avatar-text">{{ getAvatarText(user.name) }}</span>
            </div>

            <div class="user-info">
              <div class="user-name">{{ user.name }}</div>
              <div v-if="user.currentField" class="current-field">
                正在编辑: {{ user.currentField }}
              </div>
              <div v-else class="user-status">在线</div>
            </div>

            <div class="user-actions">
              <a-tooltip title="定位到用户">
                <a-button
                  type="text"
                  size="small"
                  @click="locateUser(user)"
                >
                  <template #icon>📍</template>
                </a-button>
              </a-tooltip>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 性能监控面板（开发模式） -->
    <div v-if="isDevelopment && showPerformancePanel" class="performance-panel">
      <h4>协作系统性能监控</h4>
      <div class="performance-metrics">
        <div>总光标数: {{ totalCursors }}</div>
        <div>可见光标: {{ renderStats.cursors }}</div>
        <div>FPS: {{ Math.round(performanceStats.fps) }}</div>
        <div>内存使用: {{ Math.round(performanceStats.memory / 1024 / 1024) }}MB</div>
        <div>头像缓存: {{ avatarCacheStats.size }}/{{ avatarCacheStats.maxSize }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick, reactive } from 'vue';
import CollaborationCursor from './CollaborationCursor.vue';
import { avatarManager } from '/@/utils/avatar-manager';

interface CollaborationUser {
  id: string | number;
  name: string;
  avatar?: string;
  color: string;
  currentField?: string;
  lastSeen?: Date;
  isOnline: boolean;
  avatarCanvas?: HTMLCanvasElement;
  virtualOffset?: number;
}

interface CursorInfo {
  user: CollaborationUser;
  fieldElement?: HTMLElement;
  fieldName: string;
  fieldLabel?: string;
  position: { x: number; y: number };
  isEditing: boolean;
  showLabel: boolean;
  visible: boolean;
}

const props = defineProps<{
  collaborators: CollaborationUser[];
  activeCursors: CursorInfo[];
  showCollaborationPanel?: boolean;
  showPerformanceStats?: boolean;
  showPerformancePanel?: boolean;
  maxVisibleCursors?: number;
  virtualScrollItemHeight?: number;
}>();

const emit = defineEmits<{
  userLocated: [user: CollaborationUser];
  performanceWarning: [stats: any];
}>();

// 性能相关状态
const performanceStats = reactive({
  fps: 60,
  memory: 0,
  renderTime: 0,
  lastFrameTime: Date.now()
});

const renderStats = reactive({
  cursors: 0,
  collaborators: 0
});

// 虚拟滚动相关
const collaboratorListRef = ref<HTMLElement>();
const listScrollTop = ref(0);
const itemHeight = computed(() => props.virtualScrollItemHeight || 60);
const visibleItemCount = computed(() => Math.ceil(300 / itemHeight.value) + 2); // 容器高度300px

// 开发模式检测
const isDevelopment = computed(() => process.env.NODE_ENV === 'development');

// 性能优化：限制最大可见光标数
const maxCursors = computed(() => props.maxVisibleCursors || 50);

// 虚拟化可见光标
const visibleCursors = computed(() => {
  const cursors = props.activeCursors
    .filter(cursor => cursor.visible)
    .sort((a, b) => {
      // 优先显示正在编辑的光标
      if (a.isEditing && !b.isEditing) return -1;
      if (!a.isEditing && b.isEditing) return 1;
      return 0;
    })
    .slice(0, maxCursors.value);

  renderStats.cursors = cursors.length;
  return cursors;
});

// 计算总光标数
const totalCursors = computed(() => props.activeCursors.length);

// 虚拟滚动 - 可见协作者
const visibleCollaborators = computed(() => {
  const startIndex = Math.floor(listScrollTop.value / itemHeight.value);
  const endIndex = Math.min(
    startIndex + visibleItemCount.value,
    props.collaborators.length
  );

  const visible = props.collaborators.slice(startIndex, endIndex).map((user, index) => ({
    ...user,
    virtualOffset: (startIndex + index) * itemHeight.value
  }));

  renderStats.collaborators = visible.length;
  return visible;
});

// 虚拟滚动总高度
const totalListHeight = computed(() => props.collaborators.length * itemHeight.value);

// 头像缓存统计
const avatarCacheStats = computed(() => avatarManager.getCacheStats());

// 处理列表滚动
const handleListScroll = (event: Event) => {
  const target = event.target as HTMLElement;
  listScrollTop.value = target.scrollTop;
};

// 生成头像文本
const getAvatarText = (name: string): string => {
  return avatarManager.getAvatarText(name);
};

// 定位用户
const locateUser = (user: CollaborationUser) => {
  emit('userLocated', user);
};

// 性能监控
let frameCount = 0;
let lastFpsUpdate = Date.now();

const updatePerformanceStats = () => {
  const now = Date.now();
  frameCount++;

  // 更新FPS（每秒计算一次）
  if (now - lastFpsUpdate >= 1000) {
    performanceStats.fps = frameCount;
    frameCount = 0;
    lastFpsUpdate = now;

    // 更新内存使用（如果可用）
    if ('memory' in performance) {
      performanceStats.memory = (performance as any).memory.usedJSHeapSize;
    }

    // 性能警告
    if (performanceStats.fps < 30) {
      emit('performanceWarning', {
        fps: performanceStats.fps,
        cursors: totalCursors.value,
        memory: performanceStats.memory
      });
    }
  }

  performanceStats.lastFrameTime = now;
  requestAnimationFrame(updatePerformanceStats);
};

// 预加载头像
const preloadAvatars = async () => {
  if (props.collaborators.length > 0) {
    await avatarManager.preloadAvatars(
      props.collaborators.map(user => ({
        id: String(user.id),
        name: user.name,
        color: user.color
      }))
    );
  }
};

// 监听协作者变化，预加载头像
watch(() => props.collaborators, async (newCollaborators) => {
  await preloadAvatars();

  // 为可见协作者设置Canvas头像
  nextTick(() => {
    newCollaborators.forEach(user => {
      const canvas = avatarManager.getOptimizedAvatar(
        String(user.id),
        user.name,
        user.color
      );
      user.avatarCanvas = canvas;
    });
  });
}, { immediate: true });

onMounted(() => {
  // 开始性能监控
  if (props.showPerformanceStats || isDevelopment.value) {
    updatePerformanceStats();
  }

  // 预加载头像
  preloadAvatars();
});

// 暴露组件方法
defineExpose({
  getPerformanceStats: () => performanceStats,
  getRenderStats: () => renderStats,
  clearAvatarCache: () => avatarManager.clearCache(),
  preloadAvatars
});
</script>

<style scoped>
.optimized-collaboration-manager {
  position: relative;
}

.collaboration-panel {
  position: fixed;
  top: 20px;
  right: 20px;
  width: 300px;
  max-height: 400px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.3);
  z-index: 1001;
  overflow: hidden;
}

.panel-header {
  padding: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  font-weight: 600;
  font-size: 16px;
  color: #1a1a1a;
}

.collaborator-count {
  background: #1890ff;
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.performance-stats {
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: #666;
}

.stat-item {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 4px;
}

.collaborator-list {
  max-height: 300px;
  overflow-y: auto;
  position: relative;
}

.virtual-list-spacer {
  position: relative;
}

.collaborator-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-left: 3px solid transparent;
  transition: background-color 0.2s ease;
  position: absolute;
  left: 0;
  right: 0;
  height: 60px;
  will-change: transform;
}

.collaborator-item:hover {
  background: rgba(24, 144, 255, 0.04);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  flex-shrink: 0;
}

.user-avatar canvas {
  border-radius: 50%;
}

.avatar-text {
  color: white;
  font-weight: 600;
  font-size: 14px;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-weight: 500;
  font-size: 14px;
  color: #1a1a1a;
  margin-bottom: 2px;
}

.current-field {
  font-size: 12px;
  color: #1890ff;
}

.user-status {
  font-size: 12px;
  color: #52c41a;
}

.user-actions {
  margin-left: 8px;
  flex-shrink: 0;
}

.performance-panel {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 12px;
  border-radius: 8px;
  font-size: 12px;
  z-index: 1002;
}

.performance-panel h4 {
  margin: 0 0 8px 0;
  color: white;
  font-size: 14px;
}

.performance-metrics div {
  margin: 4px 0;
}

/* 滚动条优化 */
.collaborator-list::-webkit-scrollbar {
  width: 4px;
}

.collaborator-list::-webkit-scrollbar-track {
  background: transparent;
}

.collaborator-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 2px;
}

.collaborator-list::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}
</style>