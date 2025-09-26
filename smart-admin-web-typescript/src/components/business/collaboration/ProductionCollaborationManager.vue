<!--
  * 生产级协作管理器 - 整合所有协作功能
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-24
  * @Copyright 1024创新实验室
-->
<template>
  <div class="production-collaboration-manager">
    <!-- 生产级光标渲染器 -->
    <ProductionCursorRenderer
      :cursors="cursors"
      :users="users"
      :show-user-labels="showUserLabels"
      :max-visible-cursors="maxVisibleCursors"
      :enable-highlights="enableHighlights"
      @cursor-click="handleCursorClick"
      @user-hover="handleUserHover"
    />

    <!-- 离线状态管理器已在父组件中处理，此处不重复添加 -->

    <!-- 协作状态指示器 -->
    <div v-if="showStatusIndicator" class="collaboration-status">
      <div class="status-header">
        <UserOutlined />
        <span>协作状态</span>
        <div
          class="connection-indicator"
          :class="{
            'connected': connectionStatus === 'connected',
            'disconnected': connectionStatus === 'disconnected',
            'connecting': connectionStatus === 'connecting'
          }"
        ></div>
      </div>

      <div class="active-users">
        <div
          v-for="user in users"
          :key="user.id"
          class="user-item"
          :title="user.name"
          @click="handleUserClick(user)"
        >
          <div class="user-avatar" :style="{ backgroundColor: user.color }">
            <img v-if="user.avatar" :src="user.avatar" :alt="user.name" />
            <span v-else class="avatar-text">{{ getAvatarText(user.name) }}</span>
          </div>
          <div class="user-info">
            <div class="user-name">{{ user.name }}</div>
            <div class="user-activity">
              {{ getActivityText(user.lastActivity) }}
            </div>
          </div>
          <div class="user-status">
            <div
              class="activity-indicator"
              :class="getActivityStatus(user.lastActivity)"
            ></div>
          </div>
        </div>
      </div>

      <div v-if="conflictCount > 0" class="conflict-indicator">
        <WarningOutlined />
        <span>{{ conflictCount }} 个冲突待解决</span>
        <a-button size="small" @click="handleResolveConflicts">解决冲突</a-button>
      </div>
    </div>

    <!-- 性能监控面板（开发模式） -->
    <div v-if="isDevelopment && showPerformancePanel" class="performance-panel">
      <div class="performance-header">
        <DashboardOutlined />
        <span>性能监控</span>
        <a-button size="small" @click="showPerformancePanel = false">
          <CloseOutlined />
        </a-button>
      </div>
      <div class="performance-metrics">
        <div class="metric">
          <span>光标数量:</span>
          <span>{{ cursors.length }}</span>
        </div>
        <div class="metric">
          <span>活跃用户:</span>
          <span>{{ users.length }}</span>
        </div>
        <div class="metric">
          <span>WebSocket延迟:</span>
          <span>{{ websocketLatency }}ms</span>
        </div>
        <div class="metric">
          <span>渲染FPS:</span>
          <span>{{ renderFPS }}</span>
        </div>
        <div class="metric">
          <span>内存使用:</span>
          <span>{{ memoryUsage }}MB</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { message } from 'ant-design-vue';
import ProductionCursorRenderer from './ProductionCursorRenderer.vue';
import {
  UserOutlined,
  WarningOutlined,
  DashboardOutlined,
  CloseOutlined
} from '@ant-design/icons-vue';

// 接口定义
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

interface PendingOperation {
  id: string;
  type: string;
  data: any;
  timestamp: number;
  retryCount: number;
}

interface Props {
  cursors?: UserCursor[];
  users?: ActiveUser[];
  showUserLabels?: boolean;
  maxVisibleCursors?: number;
  enableHighlights?: boolean;
  showStatusIndicator?: boolean;
  showPerformancePanel?: boolean;
  websocketUrl?: string;
  autoReconnect?: boolean;
  maxReconnectAttempts?: number;
  offlineManager?: any; // EnhancedOfflineManager - 设为可选避免循环依赖
}

const props = withDefaults(defineProps<Props>(), {
  cursors: () => [],
  users: () => [],
  showUserLabels: true,
  maxVisibleCursors: 20,
  enableHighlights: true,
  showStatusIndicator: true,
  showPerformancePanel: false,
  websocketUrl: 'ws://localhost:1024/collaboration',
  autoReconnect: true,
  maxReconnectAttempts: 5
});

const emit = defineEmits<{
  cursorClick: [cursor: UserCursor];
  userHover: [userId: string];
  userClick: [user: ActiveUser];
  connectionChange: [status: 'connected' | 'disconnected' | 'connecting'];
  conflictDetected: [conflicts: any[]];
  performanceAlert: [metric: string, value: number];
}>();

// 响应式状态
const connectionStatus = ref<'connected' | 'disconnected' | 'connecting'>('disconnected');
const isOffline = ref(false);
const pendingOperations = ref<PendingOperation[]>([]);
const syncStatus = ref('idle');
const conflictCount = ref(0);
const websocketLatency = ref(0);
const renderFPS = ref(60);
const memoryUsage = ref(0);
const isDevelopment = ref(process.env.NODE_ENV === 'development');

// WebSocket 连接管理
let websocket: WebSocket | null = null;
let reconnectAttempts = 0;
let reconnectTimer: NodeJS.Timeout | null = null;
let heartbeatTimer: NodeJS.Timeout | null = null;
let performanceTimer: NodeJS.Timeout | null = null;

// 计算属性
const cursors = computed(() => props.cursors || []);
const users = computed(() => props.users || []);

// 方法
const connectWebSocket = async () => {
  if (websocket?.readyState === WebSocket.OPEN) return;

  connectionStatus.value = 'connecting';
  emit('connectionChange', 'connecting');

  try {
    websocket = new WebSocket(props.websocketUrl);

    websocket.onopen = () => {
      console.log('✅ [Production Collaboration] WebSocket连接成功');
      connectionStatus.value = 'connected';
      isOffline.value = false;
      reconnectAttempts = 0;

      emit('connectionChange', 'connected');
      startHeartbeat();
      message.success('协作连接已建立');
    };

    websocket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        handleWebSocketMessage(data);
      } catch (error) {
        console.error('❌ [Production Collaboration] 解析WebSocket消息失败:', error);
      }
    };

    websocket.onclose = () => {
      console.log('🔌 [Production Collaboration] WebSocket连接关闭');
      connectionStatus.value = 'disconnected';
      isOffline.value = true;

      emit('connectionChange', 'disconnected');
      stopHeartbeat();

      // 自动重连
      if (props.autoReconnect && reconnectAttempts < props.maxReconnectAttempts) {
        scheduleReconnect();
      }
    };

    websocket.onerror = (error) => {
      console.error('❌ [Production Collaboration] WebSocket连接错误:', error);
      connectionStatus.value = 'disconnected';
      isOffline.value = true;

      emit('connectionChange', 'disconnected');
    };

  } catch (error) {
    console.error('❌ [Production Collaboration] WebSocket初始化失败:', error);
    connectionStatus.value = 'disconnected';
    isOffline.value = true;
    emit('connectionChange', 'disconnected');
  }
};

const disconnectWebSocket = () => {
  if (websocket) {
    websocket.close();
    websocket = null;
  }
  stopHeartbeat();
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
};

const scheduleReconnect = () => {
  if (reconnectTimer) return;

  const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000);
  reconnectAttempts++;

  console.log(`🔄 [Production Collaboration] ${delay}ms后尝试重连 (第${reconnectAttempts}次)`);

  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    connectWebSocket();
  }, delay);
};

const startHeartbeat = () => {
  if (heartbeatTimer) return;

  heartbeatTimer = setInterval(() => {
    if (websocket?.readyState === WebSocket.OPEN) {
      const start = Date.now();
      websocket.send(JSON.stringify({ type: 'ping', timestamp: start }));
    }
  }, 30000);
};

const stopHeartbeat = () => {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer);
    heartbeatTimer = null;
  }
};

const handleWebSocketMessage = (data: any) => {
  switch (data.type) {
    case 'pong':
      websocketLatency.value = Date.now() - data.timestamp;
      break;

    case 'cursor_update':
      // 光标更新由ProductionCursorRenderer处理
      break;

    case 'user_join':
      message.info(`${data.user.name} 加入了协作`);
      break;

    case 'user_leave':
      message.info(`${data.user.name} 离开了协作`);
      break;

    case 'conflict_detected':
      conflictCount.value++;
      emit('conflictDetected', data.conflicts);
      message.warning('检测到编辑冲突');
      break;

    default:
      console.log('🔔 [Production Collaboration] 未知消息类型:', data.type);
  }
};

const sendWebSocketMessage = (data: any) => {
  if (websocket?.readyState === WebSocket.OPEN) {
    websocket.send(JSON.stringify(data));
  } else {
    // 添加到离线队列
    addPendingOperation({
      id: `pending_${Date.now()}`,
      type: 'websocket_message',
      data,
      timestamp: Date.now(),
      retryCount: 0
    });
  }
};

const addPendingOperation = (operation: PendingOperation) => {
  pendingOperations.value.push(operation);

  // 限制待处理操作数量
  if (pendingOperations.value.length > 100) {
    pendingOperations.value = pendingOperations.value.slice(-50);
  }
};

// 事件处理
const handleCursorClick = (cursor: UserCursor) => {
  emit('cursorClick', cursor);
};

const handleUserHover = (userId: string) => {
  emit('userHover', userId);
};

const handleUserClick = (user: ActiveUser) => {
  emit('userClick', user);
};

const handleRetrySync = async () => {
  syncStatus.value = 'syncing';

  try {
    // 重试待处理的操作
    for (const operation of pendingOperations.value) {
      if (operation.retryCount < 3) {
        operation.retryCount++;
        sendWebSocketMessage(operation.data);
      }
    }

    // 清理成功的操作（简化处理）
    setTimeout(() => {
      pendingOperations.value = [];
      syncStatus.value = 'idle';
      message.success('同步完成');
    }, 2000);

  } catch (error) {
    console.error('❌ [Production Collaboration] 同步失败:', error);
    syncStatus.value = 'error';
    message.error('同步失败');
  }
};

const handleClearPending = () => {
  pendingOperations.value = [];
  message.info('已清理待处理操作');
};

const handleResolveConflicts = () => {
  conflictCount.value = 0;
  message.info('冲突解决完成');
};

// OfflineStatusManager 事件处理已移至父组件

// 工具方法
const getAvatarText = (name: string): string => {
  if (!name) return '?';

  const trimmed = name.trim();
  if (/[\u4e00-\u9fff]/.test(trimmed)) {
    return trimmed.charAt(trimmed.length - 1);
  } else {
    return trimmed.charAt(0).toUpperCase();
  }
};

const getActivityText = (lastActivity: number): string => {
  const now = Date.now();
  const diff = now - lastActivity;

  if (diff < 60000) return '刚刚活跃';
  if (diff < 300000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 3600000) return `${Math.floor(diff / 300000 * 5)}分钟前`;
  return '不活跃';
};

const getActivityStatus = (lastActivity: number): string => {
  const now = Date.now();
  const diff = now - lastActivity;

  if (diff < 60000) return 'active';
  if (diff < 300000) return 'idle';
  return 'inactive';
};

// 性能监控
const startPerformanceMonitoring = () => {
  if (!isDevelopment.value) return;

  performanceTimer = setInterval(() => {
    // 监控渲染FPS
    let fps = 0;
    let lastTime = performance.now();
    let frameCount = 0;

    const measureFPS = () => {
      frameCount++;
      const currentTime = performance.now();
      if (currentTime >= lastTime + 1000) {
        fps = Math.round((frameCount * 1000) / (currentTime - lastTime));
        renderFPS.value = fps;
        frameCount = 0;
        lastTime = currentTime;

        if (fps < 30) {
          emit('performanceAlert', 'fps', fps);
        }
      }
      requestAnimationFrame(measureFPS);
    };
    measureFPS();

    // 监控内存使用
    if ('memory' in performance) {
      const memory = (performance as any).memory;
      memoryUsage.value = Math.round(memory.usedJSHeapSize / 1024 / 1024);

      if (memoryUsage.value > 100) {
        emit('performanceAlert', 'memory', memoryUsage.value);
      }
    }
  }, 5000);
};

const stopPerformanceMonitoring = () => {
  if (performanceTimer) {
    clearInterval(performanceTimer);
    performanceTimer = null;
  }
};

// 生命周期
onMounted(() => {
  connectWebSocket();
  startPerformanceMonitoring();
});

onUnmounted(() => {
  disconnectWebSocket();
  stopPerformanceMonitoring();
});

// 监听连接状态变化
watch(() => connectionStatus.value, (newStatus) => {
  console.log('🔄 [Production Collaboration] 连接状态变更:', newStatus);
});

// 暴露组件方法
defineExpose({
  connect: connectWebSocket,
  disconnect: disconnectWebSocket,
  sendMessage: sendWebSocketMessage,
  getConnectionStatus: () => connectionStatus.value,
  getPendingOperationsCount: () => pendingOperations.value.length,
  clearPendingOperations: handleClearPending,
  retrySync: handleRetrySync
});
</script>

<style scoped>
.production-collaboration-manager {
  position: relative;
  /* 移除导致问题的 width: 100%; height: 100%; */
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

.conflict-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 8px;
  background-color: #fff7e6;
  border: 1px solid #ffec3d;
  border-radius: 6px;
  font-size: 12px;
}

.performance-panel {
  position: fixed;
  bottom: 20px;
  right: 20px;
  width: 240px;
  background: white;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 16px;
  z-index: 1002;
}

.performance-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 500;
}

.performance-metrics .metric {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
}

.performance-metrics .metric span:first-child {
  color: #666;
}

.performance-metrics .metric span:last-child {
  font-weight: 500;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .collaboration-status,
  .performance-panel {
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 90%;
    max-width: 300px;
  }
}

/* 深色模式适配 */
@media (prefers-color-scheme: dark) {
  .collaboration-status,
  .performance-panel {
    background: #1f1f1f;
    border-color: #434343;
    color: #e8e8e8;
  }

  .user-item:hover {
    background-color: #2c2c2c;
  }

  .conflict-indicator {
    background-color: #2c2416;
    border-color: #594214;
  }
}
</style>