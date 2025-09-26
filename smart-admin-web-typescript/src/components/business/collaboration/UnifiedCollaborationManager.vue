<!--
  * 统一协作管理组件 - 基于全局WebSocket管理器
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-25
  * @Copyright 1024创新实验室
-->
<template>
  <div class="unified-collaboration-manager">
    <!-- 协作状态指示器 -->
    <div v-if="showStatusIndicator" class="collaboration-status">
      <div class="status-header">
        <UserOutlined />
        <span>协作状态</span>
        <div
          class="connection-indicator"
          :class="{
            'connected': collaborationState.connectionStatus === 'connected',
            'disconnected': collaborationState.connectionStatus === 'disconnected',
            'connecting': collaborationState.connectionStatus === 'connecting'
          }"
        ></div>
      </div>

      <div class="active-users">
        <div
          v-for="user in collaborationState.activeUsers"
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
            <div class="user-department" v-if="user.departmentName">{{ user.departmentName }}</div>
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

      <div v-if="collaborationState.activeUsers.length === 0" class="empty-users">
        <span>暂无其他协作用户</span>
        <div class="tip-text">
          其他用户访问此页面时会自动显示在这里
        </div>
      </div>
    </div>

    <!-- 开发模式性能面板 -->
    <div v-if="isDevelopment && showPerformancePanel" class="performance-panel">
      <div class="performance-header">
        <DashboardOutlined />
        <span>协作状态</span>
      </div>
      <div class="performance-metrics">
        <div class="metric">
          <span>连接状态:</span>
          <span>{{ collaborationState.connectionStatus }}</span>
        </div>
        <div class="metric">
          <span>房间ID:</span>
          <span>{{ collaborationState.currentRoom || '未连接' }}</span>
        </div>
        <div class="metric">
          <span>活跃用户:</span>
          <span>{{ collaborationState.activeUsers.length }}</span>
        </div>
        <div class="metric">
          <span>光标数量:</span>
          <span>{{ collaborationState.userCursors.length }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { useUserStore } from '/@/store/modules/system/user';
import globalCollaborationManager from '/@/utils/global-collaboration-manager';
import { postRequest } from '/@/lib/axios';
import {
  UserOutlined,
  DashboardOutlined
} from '@ant-design/icons-vue';

interface Props {
  roomId?: string;
  showStatusIndicator?: boolean;
  showPerformancePanel?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  showStatusIndicator: true,
  showPerformancePanel: false
});

const emit = defineEmits<{
  userJoined: [user: any];
  userLeft: [user: any];
  connectionChange: [status: string];
}>();

const route = useRoute();
const userStore = useUserStore();

// 响应式状态
const collaborationState = globalCollaborationManager.state;
const isDevelopment = ref(process.env.NODE_ENV === 'development');

// 计算当前房间ID
const currentRoomId = computed(() => {
  return props.roomId || `police-report-${route.query.reportId}` || 'default-room';
});

// 计算当前用户信息
const currentUser = computed(() => {
  const userInfo = userStore.userInfo;
  return {
    id: userInfo?.userId || 'anonymous',
    name: userInfo?.userName || '匿名用户',
    avatar: userInfo?.avatar || '',
    color: generateUserColor(userInfo?.userId || 'anonymous'),
    sessionId: generateSessionId(),
    lastActivity: Date.now(),
    isOnline: true
  };
});

// 生命周期
onMounted(async () => {
  console.log('🚀 [Unified Collaboration] 组件挂载');

  // 初始化协作管理器
  globalCollaborationManager.initialize(currentUser.value);

  // 设置事件监听
  globalCollaborationManager.on('user_joined', handleUserJoined);
  globalCollaborationManager.on('user_left', handleUserLeft);
  globalCollaborationManager.on('connected', handleConnected);
  globalCollaborationManager.on('disconnected', handleDisconnected);

  // 加入协作房间
  await globalCollaborationManager.joinRoom(currentRoomId.value);
});

onUnmounted(() => {
  console.log('💀 [Unified Collaboration] 组件卸载');

  // 移除事件监听
  globalCollaborationManager.off('user_joined', handleUserJoined);
  globalCollaborationManager.off('user_left', handleUserLeft);
  globalCollaborationManager.off('connected', handleConnected);
  globalCollaborationManager.off('disconnected', handleDisconnected);

  // 离开协作房间
  globalCollaborationManager.leaveRoom();
});

// 事件处理
const handleUserJoined = (user: any) => {
  emit('userJoined', user);
};

const handleUserLeft = (user: any) => {
  emit('userLeft', user);
};

const handleConnected = (data: any) => {
  emit('connectionChange', 'connected');
};

const handleDisconnected = (data: any) => {
  emit('connectionChange', 'disconnected');
};

const handleUserClick = (user: any) => {
  console.log('👤 [Unified Collaboration] 用户点击:', user);
};


// 工具方法
const generateUserColor = (userId: string): string => {
  const colors = ['#1890ff', '#52c41a', '#fa8c16', '#eb2f96', '#722ed1', '#13c2c2'];
  const hash = userId.split('').reduce((a, b) => {
    a = ((a << 5) - a) + b.charCodeAt(0);
    return a & a;
  }, 0);
  return colors[Math.abs(hash) % colors.length];
};

const generateSessionId = (): string => {
  return `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
};

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

// 暴露方法
defineExpose({
  getCollaborationState: () => collaborationState,
  sendMessage: globalCollaborationManager.sendMessage.bind(globalCollaborationManager),
  sendCursorPosition: globalCollaborationManager.sendCursorPosition.bind(globalCollaborationManager)
});
</script>

<style scoped>
.unified-collaboration-manager {
  position: relative;
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

.user-department {
  font-size: 11px;
  color: #888;
  margin-bottom: 1px;
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

.empty-users {
  padding: 16px;
  text-align: center;
  color: #999;
  font-size: 12px;
}

.tip-text {
  margin-top: 8px;
  font-size: 11px;
  color: #bbb;
}

.test-actions {
  margin-top: 12px;
}

.test-button {
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.test-button:hover {
  background: #40a9ff;
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
</style>