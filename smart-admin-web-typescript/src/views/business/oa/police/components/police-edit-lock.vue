<!--
  * 警情编辑锁组件
  *
  * @Author:    Claude Code Assistant
  * @Date:      2025-09-24
  * @Copyright  1024创新实验室
-->
<template>
  <div v-if="showLockWarning" class="police-edit-lock-warning">
    <a-alert
      :message="lockMessage"
      type="warning"
      :show-icon="true"
      :closable="false"
      class="mb-3"
    >
      <template #description>
        <div>{{ lockDescription }}</div>
        <div class="lock-actions mt-2">
          <a-button type="primary" size="small" @click="requestEditAccess" :loading="requesting">
            尝试获取编辑权限
          </a-button>
          <a-button size="small" @click="refreshLockStatus" :loading="checking">
            刷新状态
          </a-button>
        </div>
      </template>
    </a-alert>
  </div>

  <div v-if="showEditingIndicator" class="police-editing-indicator">
    <a-alert
      message="正在编辑"
      description="您当前拥有该警情的编辑权限，其他用户无法同时编辑。"
      type="success"
      :show-icon="true"
      :closable="false"
      class="mb-3"
    />
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted, onUnmounted, watch } from 'vue';
  import { message } from 'ant-design-vue';
  import { policeReportApi } from '/@/api/business/oa/police-report-api';
  import { getWebSocketClient } from '/@/utils/websocket-manager';

  // Props
  interface Props {
    reportId: number;
    seatId?: number;
    autoLock?: boolean; // 是否自动尝试获取锁定
    disabled?: boolean; // 是否禁用编辑锁功能
  }

  const props = withDefaults(defineProps<Props>(), {
    autoLock: true,
    disabled: false
  });

  // Emits
  const emit = defineEmits<{
    lockStatusChange: [locked: boolean];
    editAccessGranted: [];
    editAccessDenied: [reason: string];
  }>();

  // Refs
  const showLockWarning = ref(false);
  const showEditingIndicator = ref(false);
  const lockMessage = ref('');
  const lockDescription = ref('');
  const requesting = ref(false);
  const checking = ref(false);
  const lockCheckInterval = ref<NodeJS.Timeout | null>(null);
  const currentLockStatus = ref(false);
  const hasEditAccess = ref(false);

  // Methods
  async function checkLockStatus() {
    if (props.disabled || !props.reportId) return;

    try {
      checking.value = true;
      const response = await policeReportApi.checkLock(props.reportId);

      if (response.data) {
        // 被锁定
        currentLockStatus.value = true;
        showLockWarning.value = true;
        showEditingIndicator.value = false;
        hasEditAccess.value = false;
        lockMessage.value = '警情正在被其他用户编辑';
        lockDescription.value = '该警情目前正在被其他用户编辑中，您暂时无法进行修改。请稍后再试或联系当前编辑用户。';
        emit('lockStatusChange', true);
        emit('editAccessDenied', '警情正在被其他用户编辑');
      } else {
        // 未被锁定
        currentLockStatus.value = false;
        showLockWarning.value = false;

        if (props.autoLock) {
          await requestEditAccess();
        } else {
          hasEditAccess.value = false;
          showEditingIndicator.value = false;
          emit('lockStatusChange', false);
        }
      }
    } catch (error) {
      console.error('检查锁定状态失败:', error);
      message.error('检查编辑状态失败');
    } finally {
      checking.value = false;
    }
  }

  async function requestEditAccess() {
    if (props.disabled || !props.reportId) return;

    try {
      requesting.value = true;
      const response = await policeReportApi.lock(props.reportId, props.seatId);

      if (response.code === 1) {
        hasEditAccess.value = true;
        showEditingIndicator.value = true;
        showLockWarning.value = false;
        emit('lockStatusChange', false);
        emit('editAccessGranted');
        message.success('获取编辑权限成功');
      } else {
        hasEditAccess.value = false;
        showEditingIndicator.value = false;
        showLockWarning.value = true;
        lockMessage.value = '获取编辑权限失败';
        lockDescription.value = response.msg || '无法获取编辑权限，请稍后再试';
        emit('editAccessDenied', response.msg || '无法获取编辑权限');
        message.error(response.msg || '获取编辑权限失败');
      }
    } catch (error) {
      console.error('请求编辑权限失败:', error);
      message.error('请求编辑权限失败');
      hasEditAccess.value = false;
      showEditingIndicator.value = false;
      emit('editAccessDenied', '请求编辑权限失败');
    } finally {
      requesting.value = false;
    }
  }

  async function releaseEditAccess() {
    if (props.disabled || !props.reportId || !hasEditAccess.value) return;

    try {
      const response = await policeReportApi.unlock(props.reportId);

      if (response.code === 1) {
        hasEditAccess.value = false;
        showEditingIndicator.value = false;
        emit('lockStatusChange', false);
      } else {
        console.error('释放编辑权限失败:', response.msg);
      }
    } catch (error) {
      console.error('释放编辑权限失败:', error);
    }
  }

  function refreshLockStatus() {
    checkLockStatus();
  }

  function startLockCheck() {
    if (lockCheckInterval.value) {
      clearInterval(lockCheckInterval.value);
    }

    // 每30秒检查一次锁定状态
    lockCheckInterval.value = setInterval(() => {
      if (!hasEditAccess.value) {
        checkLockStatus();
      }
    }, 30000);
  }

  function stopLockCheck() {
    if (lockCheckInterval.value) {
      clearInterval(lockCheckInterval.value);
      lockCheckInterval.value = null;
    }
  }

  // WebSocket 事件处理
  function setupWebSocketHandlers() {
    const wsClient = getWebSocketClient();

    wsClient.on('POLICE_CASE_LOCK', handlePoliceCaseLock);
    wsClient.on('POLICE_CASE_UNLOCK', handlePoliceCaseUnlock);
  }

  function cleanupWebSocketHandlers() {
    const wsClient = getWebSocketClient();

    wsClient.off('POLICE_CASE_LOCK', handlePoliceCaseLock);
    wsClient.off('POLICE_CASE_UNLOCK', handlePoliceCaseUnlock);
  }

  function handlePoliceCaseLock(message: SeatSyncMessage) {
    if (message.policeCaseId === props.reportId) {
      // 警情被锁定
      if (!hasEditAccess.value) {
        checkLockStatus();
      }
    }
  }

  function handlePoliceCaseUnlock(message: SeatSyncMessage) {
    if (message.policeCaseId === props.reportId) {
      // 警情被解锁
      checkLockStatus();
    }
  }

  // 监听 reportId 变化
  watch(() => props.reportId, (newReportId, oldReportId) => {
    if (newReportId !== oldReportId) {
      // 释放之前的编辑权限
      if (oldReportId && hasEditAccess.value) {
        releaseEditAccess();
      }

      // 检查新的锁定状态
      if (newReportId) {
        checkLockStatus();
      } else {
        showLockWarning.value = false;
        showEditingIndicator.value = false;
        hasEditAccess.value = false;
      }
    }
  });

  // Lifecycle
  onMounted(() => {
    setupWebSocketHandlers();
    if (props.reportId) {
      checkLockStatus();
      startLockCheck();
    }
  });

  onUnmounted(() => {
    cleanupWebSocketHandlers();
    stopLockCheck();

    // 组件销毁时释放编辑权限
    if (hasEditAccess.value) {
      releaseEditAccess();
    }
  });

  // 暴露方法给父组件
  defineExpose({
    checkLockStatus,
    requestEditAccess,
    releaseEditAccess,
    hasEditAccess: () => hasEditAccess.value,
    isLocked: () => currentLockStatus.value
  });
</script>

<style scoped>
.police-edit-lock-warning {
  margin-bottom: 16px;
}

.police-editing-indicator {
  margin-bottom: 16px;
}

.lock-actions {
  display: flex;
  gap: 8px;
}

.lock-actions .ant-btn {
  height: 28px;
}
</style>