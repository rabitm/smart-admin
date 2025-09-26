<!--
  * 极简版生产级协作管理器 - 用于测试
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-25
  * @Copyright 1024创新实验室
-->
<template>
  <div class="minimal-production-collaboration-manager">
    <!-- 完全移除ProductionCursorRenderer进行测试 -->

    <!-- 简化的协作状态指示器 -->
    <div v-if="showStatusIndicator" class="collaboration-status">
      <div class="status-header">
        <span>👥 协作状态 (无CursorRenderer)</span>
        <div class="connection-indicator disconnected"></div>
      </div>
      <div class="simple-info">
        <span>测试阶段 - 无ProductionCursorRenderer</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

interface Props {
  cursors?: any[];
  users?: any[];
  showUserLabels?: boolean;
  maxVisibleCursors?: number;
  enableHighlights?: boolean;
  showStatusIndicator?: boolean;
  showPerformancePanel?: boolean;
  websocketUrl?: string;
  autoReconnect?: boolean;
  maxReconnectAttempts?: number;
  offlineManager?: any;
}

const props = withDefaults(defineProps<Props>(), {
  cursors: () => [],
  users: () => [],
  showUserLabels: true,
  maxVisibleCursors: 20,
  enableHighlights: true,
  showStatusIndicator: true,
  showPerformancePanel: false,
  websocketUrl: '',
  autoReconnect: true,
  maxReconnectAttempts: 5
});

const emit = defineEmits<{
  cursorClick: [cursor: any];
  userHover: [userId: string];
  userClick: [user: any];
  connectionChange: [status: string];
  conflictDetected: [conflicts: any[]];
  performanceAlert: [metric: string, value: number];
}>();

// 最简单的响应式状态
const connectionStatus = ref('disconnected');

// 事件处理
const handleCursorClick = (cursor: any) => {
  emit('cursorClick', cursor);
};

const handleUserHover = (userId: string) => {
  emit('userHover', userId);
};

console.log('✅ MinimalProductionCollaborationManager 组件已加载 (无ProductionCursorRenderer)');
</script>

<style scoped>
.minimal-production-collaboration-manager {
  position: relative;
  width: 100%;
  height: 100%;
}

.collaboration-status {
  position: fixed;
  top: 80px;
  right: 20px;
  width: 280px;
  background: white;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 16px;
  z-index: 1002;
}

.status-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 500;
}

.connection-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-left: auto;
  transition: background-color 0.3s ease;
}

.connection-indicator.connected {
  background-color: #52c41a;
}

.connection-indicator.disconnected {
  background-color: #ff4d4f;
}

.connection-indicator.connecting {
  background-color: #fa8c16;
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.active-users {
  max-height: 200px;
  overflow-y: auto;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.user-item:hover {
  background-color: #f5f5f5;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  font-weight: 600;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-activity {
  font-size: 12px;
  color: #666;
}

.user-status {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.activity-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.activity-indicator.active {
  background-color: #52c41a;
}

.activity-indicator.idle {
  background-color: #fa8c16;
}

.activity-indicator.inactive {
  background-color: #d9d9d9;
}
</style>