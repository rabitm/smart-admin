<!--
  * 最简测试版本 - 步骤6：移除v-if测试
-->
<template>
  <div class="minimal-production-collaboration-manager">
    <!-- 移除v-if条件，直接显示 -->
    <div class="collaboration-status">
      <div class="status-header">
        <span>👥 协作状态 (无v-if)</span>
        <div class="connection-indicator disconnected"></div>
      </div>
      <div class="simple-info">
        <span>连接状态: {{ connectionStatus }}</span>
        <span>cursors: {{ cursors?.length || 0 }}</span>
        <span>users: {{ users?.length || 0 }}</span>
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

// 添加响应式变量
const connectionStatus = ref<'connected' | 'disconnected' | 'connecting'>('disconnected');
const isOffline = ref(false);
const conflictCount = ref(0);
const websocketLatency = ref(0);
const renderFPS = ref(60);
const memoryUsage = ref(0);

console.log('✅ 测试组件加载成功 - 步骤5：添加v-if模板结构');
</script>

<style scoped>
/* 步骤1：添加position和width/height */
.minimal-production-collaboration-manager {
  position: relative;
  width: 100%;
  height: 100%;
}

.collaboration-status {
  background: lightblue;
  padding: 10px;
  border: 1px solid blue;
}
</style>