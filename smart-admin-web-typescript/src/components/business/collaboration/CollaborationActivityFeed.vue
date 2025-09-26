<!--
  * 协作活动流 - 显示实时协作动态
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-25
  * @Copyright 1024创新实验室
-->
<template>
  <div class="collaboration-activity-feed">
    <div class="activity-header">
      <HistoryOutlined />
      <span>实时协作动态</span>
    </div>

    <div class="activity-list">
      <div
        v-for="activity in activities"
        :key="activity.id"
        class="activity-item"
        :class="activity.type"
      >
        <div class="activity-user">
          <div
            class="user-avatar"
            :style="{ backgroundColor: activity.user.color }"
          >
            <img v-if="activity.user.avatar" :src="activity.user.avatar" :alt="activity.user.name" />
            <span v-else class="avatar-text">{{ getAvatarText(activity.user.name) }}</span>
          </div>
        </div>

        <div class="activity-content">
          <div class="activity-text">
            <span class="user-name">{{ activity.user.name }}</span>
            <span class="action-text">{{ getActionText(activity) }}</span>
          </div>
          <div class="activity-time">{{ getTimeAgo(activity.timestamp) }}</div>
        </div>

        <div class="activity-status">
          <div
            class="status-indicator"
            :class="activity.type"
          ></div>
        </div>
      </div>

      <div v-if="activities.length === 0" class="empty-activities">
        <span>暂无协作动态</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { HistoryOutlined } from '@ant-design/icons-vue';
import globalCollaborationManager from '/@/utils/global-collaboration-manager';

interface CollaborationUser {
  id: string;
  name: string;
  avatar?: string;
  color: string;
  departmentName?: string;
}

interface Activity {
  id: string;
  type: 'field_focus' | 'field_edit' | 'field_save' | 'user_join' | 'user_leave';
  user: CollaborationUser;
  fieldName?: string;
  fieldLabel?: string;
  content?: string;
  timestamp: number;
}

const activities = ref<Activity[]>([]);

// 字段名称映射
const fieldLabels: Record<string, string> = {
  'reportNumber': '报告编号',
  'incidentType': '事件类型',
  'incidentTime': '事件时间',
  'location': '事件地点',
  'reporter': '报告人',
  'description': '事件描述',
  'evidenceList': '证据清单',
  'witnessInfo': '证人信息',
  'suspectInfo': '嫌疑人信息',
  'basicInfo': '基本信息',
  'professionalInfo': '专业信息'
};

onMounted(() => {
  // 监听协作事件
  globalCollaborationManager.on('user_joined', handleUserJoined);
  globalCollaborationManager.on('user_left', handleUserLeft);
  globalCollaborationManager.on('field_focus', handleFieldFocus);
  globalCollaborationManager.on('field_edit', handleFieldEdit);
  globalCollaborationManager.on('field_save', handleFieldSave);
});

onUnmounted(() => {
  globalCollaborationManager.off('user_joined', handleUserJoined);
  globalCollaborationManager.off('user_left', handleUserLeft);
  globalCollaborationManager.off('field_focus', handleFieldFocus);
  globalCollaborationManager.off('field_edit', handleFieldEdit);
  globalCollaborationManager.off('field_save', handleFieldSave);
});

const handleUserJoined = (user: CollaborationUser) => {
  addActivity({
    id: `join_${user.id}_${Date.now()}`,
    type: 'user_join',
    user,
    timestamp: Date.now()
  });
};

const handleUserLeft = (user: CollaborationUser) => {
  addActivity({
    id: `leave_${user.id}_${Date.now()}`,
    type: 'user_leave',
    user,
    timestamp: Date.now()
  });
};

const handleFieldFocus = (data: any) => {
  addActivity({
    id: `focus_${data.userId}_${data.fieldName}_${Date.now()}`,
    type: 'field_focus',
    user: data.user,
    fieldName: data.fieldName,
    fieldLabel: fieldLabels[data.fieldName] || data.fieldName,
    timestamp: Date.now()
  });
};

const handleFieldEdit = (data: any) => {
  addActivity({
    id: `edit_${data.userId}_${data.fieldName}_${Date.now()}`,
    type: 'field_edit',
    user: data.user,
    fieldName: data.fieldName,
    fieldLabel: fieldLabels[data.fieldName] || data.fieldName,
    timestamp: Date.now()
  });
};

const handleFieldSave = (data: any) => {
  addActivity({
    id: `save_${data.userId}_${data.fieldName}_${Date.now()}`,
    type: 'field_save',
    user: data.user,
    fieldName: data.fieldName,
    fieldLabel: fieldLabels[data.fieldName] || data.fieldName,
    content: data.content,
    timestamp: Date.now()
  });
};

const addActivity = (activity: Activity) => {
  activities.value.unshift(activity);

  // 保持最多20条记录
  if (activities.value.length > 20) {
    activities.value = activities.value.slice(0, 20);
  }

  // 清理5分钟前的旧记录
  const fiveMinutesAgo = Date.now() - 5 * 60 * 1000;
  activities.value = activities.value.filter(a => a.timestamp > fiveMinutesAgo);
};

const getActionText = (activity: Activity): string => {
  switch (activity.type) {
    case 'user_join':
      return '加入了协作';
    case 'user_leave':
      return '离开了协作';
    case 'field_focus':
      return `开始编辑 ${activity.fieldLabel}`;
    case 'field_edit':
      return `正在编辑 ${activity.fieldLabel}`;
    case 'field_save':
      return `保存了 ${activity.fieldLabel}`;
    default:
      return '进行了操作';
  }
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

const getTimeAgo = (timestamp: number): string => {
  const now = Date.now();
  const diff = now - timestamp;

  if (diff < 10000) return '刚刚';
  if (diff < 60000) return `${Math.floor(diff / 1000)}秒前`;
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  return `${Math.floor(diff / 3600000)}小时前`;
};
</script>

<style scoped>
.collaboration-activity-feed {
  background: white;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  height: 120px; /* 减小高度，更适合顶部显示 */
  display: flex;
  flex-direction: column;
  margin: 0 auto; /* 居中显示 */
  max-width: 800px; /* 限制最大宽度 */
}

.activity-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px; /* 减小padding */
  border-bottom: 1px solid #f0f0f0;
  font-weight: 500;
  color: #666;
  font-size: 12px; /* 减小字体 */
}

.activity-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px; /* 减小padding */
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 8px; /* 减小间隔 */
  padding: 4px 6px; /* 减小padding */
  border-radius: 4px; /* 减小圆角 */
  margin-bottom: 2px; /* 减小间隔 */
  transition: background-color 0.2s ease;
  animation: fadeIn 0.5s ease-out;
}

.activity-item:hover {
  background-color: #f5f5f5;
}

.activity-item.user_join {
  background-color: rgba(82, 196, 26, 0.1);
}

.activity-item.user_leave {
  background-color: rgba(255, 77, 79, 0.1);
}

.activity-item.field_focus {
  background-color: rgba(24, 144, 255, 0.1);
}

.activity-item.field_edit {
  background-color: rgba(250, 140, 22, 0.1);
}

.activity-item.field_save {
  background-color: rgba(82, 196, 26, 0.1);
}

.activity-user {
  flex-shrink: 0;
}

.user-avatar {
  width: 18px; /* 缩小头像 */
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 8px; /* 缩小头像字体 */
  font-weight: 600;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.activity-content {
  flex: 1;
  min-width: 0;
}

.activity-text {
  font-size: 11px; /* 缩小文字 */
  line-height: 1.3; /* 减小行高 */
  margin-bottom: 1px; /* 减小间距 */
}

.user-name {
  font-weight: 500;
  color: #333;
  margin-right: 4px;
}

.action-text {
  color: #666;
}

.activity-time {
  font-size: 9px; /* 进一步缩小时间字体 */
  color: #999;
}

.activity-status {
  flex-shrink: 0;
}

.status-indicator {
  width: 6px; /* 缩小状态指示器 */
  height: 6px;
  border-radius: 50%;
}

.status-indicator.user_join {
  background-color: #52c41a;
}

.status-indicator.user_leave {
  background-color: #ff4d4f;
}

.status-indicator.field_focus {
  background-color: #1890ff;
}

.status-indicator.field_edit {
  background-color: #fa8c16;
  animation: pulse 2s ease-in-out infinite;
}

.status-indicator.field_save {
  background-color: #52c41a;
}

.empty-activities {
  padding: 20px;
  text-align: center;
  color: #999;
  font-size: 12px;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>