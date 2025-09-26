<!--
  * 协作字段指示器 - 显示其他用户的实时操作
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-25
  * @Copyright 1024创新实验室
-->
<template>
  <div class="collaboration-field-indicator">
    <!-- 字段锁定指示器 -->
    <div
      v-if="safeFieldState.isLocked && safeFieldState.lockedBy && shouldShowIndicator(safeFieldState.lockedBy)"
      class="field-locked-indicator"
    >
      <div class="locked-user">
        <div
          class="user-avatar"
          :style="{ backgroundColor: safeFieldState.lockedBy.color }"
          :title="`${safeFieldState.lockedBy.name} 正在编辑此字段`"
        >
          <img v-if="safeFieldState.lockedBy.avatar" :src="safeFieldState.lockedBy.avatar" :alt="safeFieldState.lockedBy.name" />
          <span v-else class="avatar-text">{{ getAvatarText(safeFieldState.lockedBy.name) }}</span>
        </div>
        <span class="editing-text">{{ safeFieldState.lockedBy.name }} 正在编辑</span>
      </div>
    </div>

    <!-- 最近操作指示器 -->
    <div
      v-if="safeFieldState.lastModifiedBy && shouldShowIndicator(safeFieldState.lastModifiedBy)"
      class="field-modified-indicator"
    >
      <span class="modified-text">
        {{ safeFieldState.lastModifiedBy.name }} {{ getTimeAgo(safeFieldState.lastModifiedAt) }}更新
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useUserStore } from '/@/store/modules/system/user';

interface CollaborationUser {
  id: string;
  name: string;
  avatar?: string;
  color: string;
}

interface FieldState {
  isLocked: boolean;
  lockedBy?: CollaborationUser;
  lastModifiedBy?: CollaborationUser;
  lastModifiedAt?: number;
}

interface Props {
  fieldName: string;
  fieldState: FieldState;
}

const props = defineProps<Props>();
const userStore = useUserStore();

// 安全的字段状态，带默认值
const safeFieldState = computed(() => {
  if (!props.fieldState) {
    return {
      isLocked: false,
      lockedBy: null,
      lastModifiedBy: null,
      lastModifiedAt: null
    };
  }
  return props.fieldState;
});

const currentUserId = computed(() => {
  // 尝试多种方式获取用户ID
  let userId = '';

  // 方式1: 从userStore.employeeId获取 (这是SmartAdmin的标准字段)
  if (userStore.employeeId) {
    userId = userStore.employeeId.toString();
  }
  // 方式2: 从userStore.userInfo获取
  else if (userStore.userInfo?.userId) {
    userId = userStore.userInfo.userId.toString();
  }
  // 方式3: 从userStore.userInfo的employeeId获取
  else if (userStore.userInfo?.employeeId) {
    userId = userStore.userInfo.employeeId.toString();
  }
  // 方式4: 从localStorage获取
  else if (typeof window !== 'undefined') {
    try {
      const storedUser = localStorage.getItem('userInfo') || localStorage.getItem('LOGIN_USER_DATA') || localStorage.getItem('user-token');
      if (storedUser) {
        const user = JSON.parse(storedUser);
        userId = (user.userId || user.employeeId || user.id || '').toString();
      }
    } catch (e) {
      console.warn('🔧 [CollaborationFieldIndicator] 无法从localStorage获取用户信息:', e);
    }
  }

  console.log('🔧 [CollaborationFieldIndicator] 当前用户信息调试:', {
    userId,
    userStoreEmployeeId: userStore.employeeId,
    userStoreInfo: userStore.userInfo,
    hasUserInfo: !!userStore.userInfo,
    userStoreUserId: userStore.userInfo?.userId,
    userStoreUserEmployeeId: userStore.userInfo?.employeeId
  });

  return userId;
});

// 判断是否应该显示指示器
const shouldShowIndicator = (user: CollaborationUser | null): boolean => {
  if (!user || !user.id) {
    console.log('🔧 [CollaborationFieldIndicator] 用户信息不完整，不显示指示器:', user);
    return false;
  }

  const show = user.id !== currentUserId.value;
  console.log('🔧 [CollaborationFieldIndicator] 用户比较:', {
    userId: user.id,
    userName: user.name,
    currentUserId: currentUserId.value,
    shouldShow: show
  });
  return show;
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

const getTimeAgo = (timestamp?: number): string => {
  if (!timestamp) return '';

  const now = Date.now();
  const diff = now - timestamp;

  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
  return `${Math.floor(diff / 86400000)}天前`;
};
</script>

<style scoped>
.collaboration-field-indicator {
  position: absolute;
  top: -8px;
  right: -8px;
  z-index: 1000;
}

.field-locked-indicator {
  background: #1890ff;
  color: white;
  border: 1px solid #1890ff;
  border-radius: 6px;
  padding: 6px 12px;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
  margin-bottom: 4px;
  min-width: 120px;
  animation: pulse 2s ease-in-out infinite;
}

.locked-user {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 10px;
  font-weight: 600;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.editing-text {
  font-size: 11px;
  color: white;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.field-modified-indicator {
  background: #fa8c16;
  color: white;
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 10px;
  white-space: nowrap;
  box-shadow: 0 2px 6px rgba(250, 140, 22, 0.3);
  min-width: 80px;
  text-align: center;
}

.modified-text {
  opacity: 0.8;
}

/* 动画效果 */
.field-locked-indicator {
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
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
    transform: scale(1);
    box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 6px 16px rgba(24, 144, 255, 0.5);
  }
}
</style>