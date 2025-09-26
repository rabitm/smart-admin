<!--
  * 统一警务协作组件
  * 使用统一WebSocket架构实现实时协作功能
  *
  * @Author:    Claude Code Assistant
  * @Date:      2025-09-25
  * @Copyright  1024创新实验室
-->
<template>
  <div class="unified-police-collaboration">
    <!-- 协作用户状态 -->
    <div class="collaboration-status" v-if="collaborationUsers.length > 0">
      <div class="collab-users">
        <div
          v-for="user in collaborationUsers"
          :key="user.id"
          class="collab-user-avatar"
          :style="{ backgroundColor: user.color }"
          :title="user.name"
        >
          {{ user.name.charAt(0) }}
        </div>
      </div>
      <span class="collab-count">{{ collaborationUsers.length }}人协作中</span>
    </div>

    <!-- 字段编辑指示器 -->
    <div class="field-indicators">
      <div
        v-for="(editor, fieldName) in activeFieldEditors"
        :key="fieldName"
        class="field-indicator"
        :style="{ backgroundColor: editor.color }"
      >
        <span class="field-name">{{ getFieldLabel(fieldName) }}</span>
        <span class="editor-name">{{ editor.name }}</span>
      </div>
    </div>

    <!-- 连接状态 -->
    <div class="connection-status" :class="connectionStatusClass">
      <span class="status-indicator"></span>
      <span class="status-text">{{ connectionStatusText }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';
import {
  policeWebSocketService,
  initializeWebSocketService,
  type PoliceReportCollaboration,
  type CollaborationUser
} from '/@/services/websocket';

// Props
interface Props {
  reportId?: number;
  fieldLabelMapping?: Record<string, string>;
}

const props = withDefaults(defineProps<Props>(), {
  fieldLabelMapping: () => ({})
});

// Emits
const emit = defineEmits<{
  userJoin: [user: CollaborationUser];
  userLeave: [user: CollaborationUser];
  fieldEdit: [data: PoliceReportCollaboration];
  fieldFocus: [data: PoliceReportCollaboration];
  fieldBlur: [data: PoliceReportCollaboration];
  reportLock: [data: any];
  reportUnlock: [data: any];
}>();

// 状态
const collaborationUsers = ref<CollaborationUser[]>([]);
const activeFieldEditors = ref<Record<string, { name: string; color: string }>>({});
const isConnected = ref(false);
const isInitialized = ref(false);

// 计算属性
const connectionStatusClass = computed(() => ({
  'connected': isConnected.value,
  'disconnected': !isConnected.value
}));

const connectionStatusText = computed(() => {
  if (!isInitialized.value) return '初始化中...';
  return isConnected.value ? '已连接' : '连接断开';
});

// 方法
function getFieldLabel(fieldName: string): string {
  return props.fieldLabelMapping[fieldName] || fieldName;
}

// 初始化协作服务
async function initializeCollaboration(): Promise<void> {
  try {
    // 初始化警务WebSocket服务
    await initializeWebSocketService('police');
    isInitialized.value = true;
    isConnected.value = policeWebSocketService.isConnected();

    // 如果有报告ID，加入协作
    if (props.reportId) {
      await policeWebSocketService.joinReport(props.reportId);
    }

    // 设置事件监听器
    setupEventListeners();

    console.log('🚨 统一警务协作组件初始化完成');
  } catch (error) {
    console.error('❌ 初始化警务协作服务失败:', error);
  }
}

// 设置事件监听器
function setupEventListeners(): void {
  // 用户加入/离开
  policeWebSocketService.on('user_join', (data: any) => {
    const user: CollaborationUser = {
      id: data.userId,
      name: data.userName,
      color: generateUserColor(data.userId),
      isOnline: true,
      lastActiveTime: data.timestamp
    };

    const existingIndex = collaborationUsers.value.findIndex(u => u.id === user.id);
    if (existingIndex === -1) {
      collaborationUsers.value.push(user);
    }

    emit('userJoin', user);
  });

  policeWebSocketService.on('user_leave', (data: any) => {
    const userIndex = collaborationUsers.value.findIndex(u => u.id === data.userId);
    if (userIndex !== -1) {
      const user = collaborationUsers.value[userIndex];
      collaborationUsers.value.splice(userIndex, 1);
      emit('userLeave', user);
    }
  });

  // 字段编辑事件
  policeWebSocketService.on('field_edit', (data: PoliceReportCollaboration) => {
    emit('fieldEdit', data);
  });

  policeWebSocketService.on('field_focus', (data: PoliceReportCollaboration) => {
    if (data.fieldName) {
      activeFieldEditors.value[data.fieldName] = {
        name: data.userName,
        color: generateUserColor(data.userId)
      };
    }
    emit('fieldFocus', data);
  });

  policeWebSocketService.on('field_blur', (data: PoliceReportCollaboration) => {
    if (data.fieldName) {
      delete activeFieldEditors.value[data.fieldName];
    }
    emit('fieldBlur', data);
  });

  // 报告锁定事件
  policeWebSocketService.on('report_lock', (data: any) => {
    emit('reportLock', data);
  });

  policeWebSocketService.on('report_unlock', (data: any) => {
    emit('reportUnlock', data);
  });
}

// 生成用户颜色
function generateUserColor(userId: number): string {
  const colors = [
    '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4',
    '#FFEAA7', '#DDA0DD', '#98D8C8', '#F7DC6F',
    '#BB8FCE', '#85C1E9', '#F8C471', '#82E0AA'
  ];
  return colors[userId % colors.length];
}

// 发送字段编辑事件
async function sendFieldEdit(fieldName: string, value: any): Promise<void> {
  await policeWebSocketService.sendFieldEdit(fieldName, value);
}

// 发送字段焦点事件
async function sendFieldFocus(fieldName: string): Promise<void> {
  await policeWebSocketService.sendFieldFocus(fieldName);
}

// 发送字段失去焦点事件
async function sendFieldBlur(fieldName: string): Promise<void> {
  await policeWebSocketService.sendFieldBlur(fieldName);
}

// 切换报告
async function switchReport(newReportId: number): Promise<void> {
  if (policeWebSocketService.getCurrentReportId()) {
    await policeWebSocketService.leaveReport();
  }

  collaborationUsers.value = [];
  activeFieldEditors.value = {};

  await policeWebSocketService.joinReport(newReportId);
}

// 暴露方法给父组件
defineExpose({
  sendFieldEdit,
  sendFieldFocus,
  sendFieldBlur,
  switchReport
});

// 生命周期
onMounted(() => {
  initializeCollaboration();
});

onUnmounted(() => {
  if (policeWebSocketService.getCurrentReportId()) {
    policeWebSocketService.leaveReport();
  }
});
</script>

<style scoped>
.unified-police-collaboration {
  position: relative;
}

.collaboration-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.collab-users {
  display: flex;
  gap: 4px;
}

.collab-user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 12px;
  cursor: pointer;
  transition: transform 0.2s;
}

.collab-user-avatar:hover {
  transform: scale(1.1);
}

.collab-count {
  font-size: 12px;
  color: #666;
}

.field-indicators {
  position: fixed;
  top: 100px;
  right: 20px;
  z-index: 1000;
  max-width: 200px;
}

.field-indicator {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 4px;
  padding: 4px 8px;
  margin-bottom: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border-left: 3px solid;
}

.field-name {
  font-size: 12px;
  font-weight: bold;
  display: block;
}

.editor-name {
  font-size: 11px;
  color: #666;
}

.connection-status {
  position: fixed;
  bottom: 20px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 12px;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.connection-status.connected {
  color: #52c41a;
}

.connection-status.disconnected {
  color: #ff4d4f;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
}

.status-text {
  font-weight: 500;
}
</style>