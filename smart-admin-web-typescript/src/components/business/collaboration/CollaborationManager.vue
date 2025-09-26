<!--
  * 协作管理器组件 - 管理所有协作功能
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-24
  * @Copyright 1024创新实验室
-->
<template>
  <div class="collaboration-manager">
    <!-- 协作光标层 -->
    <CollaborationCursor
      v-for="cursor in activeCursors"
      :key="`cursor-${cursor.user.id}-${cursor.fieldName}`"
      :user="cursor.user"
      :field-element="cursor.fieldElement"
      :field-label="cursor.fieldLabel"
      :is-editing="cursor.isEditing"
      :show-label="cursor.showLabel"
      :visible="cursor.visible"
      :position="cursor.position"
    />

    <!-- 协作状态面板 -->
    <div v-if="showCollaborationPanel && collaborators.length > 0" class="collaboration-panel">
      <div class="panel-header">
        <span class="panel-title">🤝 协作中</span>
        <span class="collaborator-count">{{ collaborators.length }}人</span>
      </div>

      <div class="collaborator-list">
        <div
          v-for="user in collaborators"
          :key="user.id"
          class="collaborator-item"
          :style="{ borderLeftColor: user.color }"
        >
          <div class="user-avatar" :style="{ backgroundColor: user.color }">
            <img v-if="user.avatar" :src="user.avatar" :alt="user.name" />
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

    <!-- 操作历史侧边栏 -->
    <div v-if="showHistoryPanel" class="history-panel">
      <div class="panel-header">
        <span class="panel-title">📝 操作历史</span>
        <a-button type="text" size="small" @click="showHistoryPanel = false">
          ✕
        </a-button>
      </div>

      <OperationTimeline
        :report-id="reportId"
        :operations="operationHistory"
        @operation-select="handleOperationSelect"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import CollaborationCursor from './CollaborationCursor.vue';
import OperationTimeline from './OperationTimeline.vue';

interface CollaborationUser {
  id: number;
  name: string;
  avatar?: string;
  color: string;
  currentField?: string;
  lastActivity?: number;
}

interface CollaborationCursor {
  user: CollaborationUser;
  fieldName: string;
  fieldLabel: string;
  fieldElement?: HTMLElement;
  isEditing: boolean;
  showLabel: boolean;
  visible: boolean;
  position: { x: number; y: number };
}

interface Props {
  reportId: number;
  currentUserId: number;
  showCollaborationPanel?: boolean;
  showHistoryPanel?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  showCollaborationPanel: true,
  showHistoryPanel: false
});

// 协作用户列表
const collaborators = ref<CollaborationUser[]>([]);
const activeCursors = ref<CollaborationCursor[]>([]);
const operationHistory = ref<any[]>([]);

// 用户颜色池
const USER_COLORS = [
  '#1890ff', '#52c41a', '#faad14', '#f5222d',
  '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16'
];

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

// 分配用户颜色
const assignUserColor = (userId: number): string => {
  return USER_COLORS[userId % USER_COLORS.length];
};

// 添加协作者
const addCollaborator = (user: Omit<CollaborationUser, 'color'>) => {
  if (user.id === props.currentUserId) return; // 不显示当前用户

  const existingIndex = collaborators.value.findIndex(c => c.id === user.id);
  const userWithColor = {
    ...user,
    color: assignUserColor(user.id),
    lastActivity: Date.now()
  };

  if (existingIndex >= 0) {
    collaborators.value[existingIndex] = userWithColor;
  } else {
    collaborators.value.push(userWithColor);
  }
};

// 移除协作者
const removeCollaborator = (userId: number) => {
  const index = collaborators.value.findIndex(c => c.id === userId);
  if (index >= 0) {
    collaborators.value.splice(index, 1);
  }

  // 同时移除相关光标
  activeCursors.value = activeCursors.value.filter(c => c.user.id !== userId);
};

// 更新用户编辑状态
const updateUserEditState = (userId: number, fieldName: string, fieldLabel: string, action: string) => {
  const user = collaborators.value.find(c => c.id === userId);
  if (!user) return;

  // 更新用户当前字段
  if (action === 'FIELD_FOCUS') {
    user.currentField = fieldLabel;
    user.lastActivity = Date.now();

    // 添加或更新光标
    updateCursor(user, fieldName, fieldLabel, true);
  } else if (action === 'FIELD_BLUR') {
    user.currentField = undefined;

    // 移除光标
    removeCursor(user.id, fieldName);
  }
};

// 更新光标
const updateCursor = (user: CollaborationUser, fieldName: string, fieldLabel: string, isEditing: boolean) => {
  const existingIndex = activeCursors.value.findIndex(
    c => c.user.id === user.id && c.fieldName === fieldName
  );

  const fieldElement = document.querySelector(`[data-field="${fieldName}"]`) as HTMLElement ||
                      document.querySelector(`input[name="${fieldName}"]`) as HTMLElement ||
                      document.querySelector(`textarea[name="${fieldName}"]`) as HTMLElement;

  const cursor: CollaborationCursor = {
    user,
    fieldName,
    fieldLabel,
    fieldElement,
    isEditing,
    showLabel: true,
    visible: true,
    position: { x: 0, y: 0 }
  };

  if (existingIndex >= 0) {
    activeCursors.value[existingIndex] = cursor;
  } else {
    activeCursors.value.push(cursor);
  }

  // 3秒后隐藏标签
  setTimeout(() => {
    const currentIndex = activeCursors.value.findIndex(
      c => c.user.id === user.id && c.fieldName === fieldName
    );
    if (currentIndex >= 0 && activeCursors.value[currentIndex].isEditing) {
      activeCursors.value[currentIndex].showLabel = false;
    }
  }, 3000);
};

// 移除光标
const removeCursor = (userId: number, fieldName: string) => {
  const index = activeCursors.value.findIndex(
    c => c.user.id === userId && c.fieldName === fieldName
  );
  if (index >= 0) {
    activeCursors.value.splice(index, 1);
  }
};

// 定位到用户
const locateUser = (user: CollaborationUser) => {
  const userCursor = activeCursors.value.find(c => c.user.id === user.id);
  if (userCursor?.fieldElement) {
    userCursor.fieldElement.scrollIntoView({
      behavior: 'smooth',
      block: 'center'
    });

    // 临时高亮字段
    userCursor.fieldElement.style.boxShadow = `0 0 0 2px ${user.color}`;
    setTimeout(() => {
      if (userCursor.fieldElement) {
        userCursor.fieldElement.style.boxShadow = '';
      }
    }, 2000);
  }
};

// 处理操作选择
const handleOperationSelect = (operation: any) => {
  console.log('选择操作:', operation);
  // TODO: 实现操作回溯功能
};

// 清理无效光标
const cleanupCursors = () => {
  activeCursors.value = activeCursors.value.filter(cursor => {
    // 检查用户是否还在协作列表中
    const userExists = collaborators.value.some(c => c.id === cursor.user.id);

    // 检查字段元素是否还存在
    const elementExists = cursor.fieldElement && document.contains(cursor.fieldElement);

    return userExists && (elementExists || cursor.position.x !== 0 || cursor.position.y !== 0);
  });
};

// 定期清理
let cleanupTimer: NodeJS.Timeout;

onMounted(() => {
  cleanupTimer = setInterval(cleanupCursors, 5000);
});

onUnmounted(() => {
  if (cleanupTimer) {
    clearInterval(cleanupTimer);
  }
});

// 暴露方法给父组件
defineExpose({
  addCollaborator,
  removeCollaborator,
  updateUserEditState,
  locateUser
});
</script>

<style scoped>
.collaboration-manager {
  position: relative;
  pointer-events: none;
}

.collaboration-panel {
  position: fixed;
  top: 120px;
  right: 20px;
  width: 280px;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  z-index: 1001;
  pointer-events: auto;
  backdrop-filter: blur(8px);
}

.history-panel {
  position: fixed;
  top: 80px;
  right: 20px;
  width: 360px;
  max-height: 70vh;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  z-index: 1002;
  pointer-events: auto;
  overflow: hidden;
}

.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fafafa;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.collaborator-count {
  font-size: 12px;
  color: #8c8c8c;
  background: #f0f0f0;
  padding: 2px 8px;
  border-radius: 10px;
}

.collaborator-list {
  max-height: 300px;
  overflow-y: auto;
}

.collaborator-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  border-left: 3px solid transparent;
  transition: background-color 0.2s ease;
}

.collaborator-item:hover {
  background: #fafafa;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #1890ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  overflow: hidden;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-text {
  color: white;
  font-size: 12px;
  font-weight: bold;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  margin-bottom: 2px;
}

.current-field {
  font-size: 12px;
  color: #1890ff;
  background: #e6f7ff;
  padding: 2px 6px;
  border-radius: 3px;
  display: inline-block;
}

.user-status {
  font-size: 12px;
  color: #52c41a;
}

.user-actions {
  display: flex;
  gap: 4px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .collaboration-panel,
  .history-panel {
    width: calc(100vw - 40px);
    right: 20px;
  }

  .history-panel {
    max-height: 60vh;
  }
}
</style>