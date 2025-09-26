<!--
  * 离线状态指示器组件 - 显示网络状态和同步进度
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-24
  * @Copyright 1024创新实验室
-->
<template>
  <div class="offline-status-indicator">
    <!-- 网络状态指示器 -->
    <div
      class="network-status"
      :class="getNetworkStatusClass()"
      @click="showDetails = !showDetails"
    >
      <div class="status-icon">
        {{ getNetworkIcon() }}
      </div>
      <div class="status-text">
        {{ getNetworkStatusText() }}
      </div>
      <div v-if="pendingCount > 0" class="pending-badge">
        {{ pendingCount }}
      </div>
    </div>

    <!-- 详情面板 -->
    <div v-if="showDetails" class="status-details">
      <div class="details-header">
        <span class="details-title">同步状态</span>
        <a-button type="text" size="small" @click="showDetails = false">
          ✕
        </a-button>
      </div>

      <!-- 网络信息 -->
      <div class="network-info">
        <div class="info-item">
          <span class="info-label">网络状态:</span>
          <span class="info-value" :class="networkStatus.online ? 'online' : 'offline'">
            {{ networkStatus.online ? '已连接' : '已断开' }}
          </span>
        </div>
        <div v-if="!networkStatus.online" class="info-item">
          <span class="info-label">断开时间:</span>
          <span class="info-value">{{ formatOfflineTime() }}</span>
        </div>
        <div v-if="networkStatus.online" class="info-item">
          <span class="info-label">连接时间:</span>
          <span class="info-value">{{ formatOnlineTime() }}</span>
        </div>
      </div>

      <!-- 同步统计 -->
      <div class="sync-stats">
        <div class="stat-item">
          <div class="stat-number pending">{{ pendingCount }}</div>
          <div class="stat-label">待同步</div>
        </div>
        <div class="stat-item">
          <div class="stat-number conflict">{{ conflictCount }}</div>
          <div class="stat-label">冲突</div>
        </div>
        <div class="stat-item">
          <div class="stat-number synced">{{ syncedCount }}</div>
          <div class="stat-label">已同步</div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="sync-actions">
        <a-button
          type="primary"
          size="small"
          :disabled="!networkStatus.online || syncing"
          :loading="syncing"
          @click="handleSyncAll"
        >
          {{ syncing ? '同步中...' : '立即同步' }}
        </a-button>
        <a-button
          size="small"
          :disabled="pendingCount === 0"
          @click="handleViewQueue"
        >
          查看队列
        </a-button>
        <a-dropdown v-if="conflictCount > 0 || failedCount > 0">
          <a-button size="small">
            更多操作
            <template #icon>▼</template>
          </a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item
                v-if="conflictCount > 0"
                key="conflicts"
                @click="handleViewConflicts"
              >
                处理冲突 ({{ conflictCount }})
              </a-menu-item>
              <a-menu-item
                v-if="failedCount > 0"
                key="retry"
                @click="handleRetryFailed"
              >
                重试失败操作 ({{ failedCount }})
              </a-menu-item>
              <a-menu-item
                key="clear"
                @click="handleClearSynced"
              >
                清除已同步操作
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>

      <!-- 最近操作 -->
      <div v-if="recentOperations.length > 0" class="recent-operations">
        <div class="recent-title">最近操作</div>
        <div class="operation-list">
          <div
            v-for="operation in recentOperations.slice(0, 3)"
            :key="operation.id"
            class="operation-item"
          >
            <div class="operation-icon">
              {{ getOperationIcon(operation.status) }}
            </div>
            <div class="operation-info">
              <div class="operation-field">{{ operation.fieldName }}</div>
              <div class="operation-time">{{ formatRelativeTime(operation.timestamp) }}</div>
            </div>
            <div class="operation-status" :class="getStatusClass(operation.status)">
              {{ getStatusText(operation.status) }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 同步进度条 -->
    <div v-if="syncing && syncProgress > 0" class="sync-progress">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: `${syncProgress}%` }"></div>
      </div>
      <div class="progress-text">
        同步进度: {{ Math.round(syncProgress) }}%
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { formatDistanceToNow } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import {
  OfflineSyncQueue,
  OfflineOperation,
  SyncStatus,
  NetworkStatus
} from '/@/utils/offline-sync-queue';

interface Props {
  syncQueue: OfflineSyncQueue;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  viewQueue: [];
  viewConflicts: [];
  syncCompleted: [results: Map<string, any>];
}>();

// 响应式数据
const showDetails = ref(false);
const syncing = ref(false);
const syncProgress = ref(0);
const networkStatus = ref<NetworkStatus>({ online: true, lastOnlineTime: Date.now(), lastOfflineTime: 0 });
const operations = ref<OfflineOperation[]>([]);

// 计算属性
const pendingCount = computed(() => {
  return operations.value.filter(op =>
    op.status === SyncStatus.PENDING || op.status === SyncStatus.FAILED
  ).length;
});

const conflictCount = computed(() => {
  return operations.value.filter(op => op.status === SyncStatus.CONFLICTED).length;
});

const failedCount = computed(() => {
  return operations.value.filter(op => op.status === SyncStatus.FAILED).length;
});

const syncedCount = computed(() => {
  return operations.value.filter(op => op.status === SyncStatus.SYNCED).length;
});

const recentOperations = computed(() => {
  return operations.value
    .sort((a, b) => b.timestamp - a.timestamp)
    .slice(0, 5);
});

// 获取网络状态样式类
const getNetworkStatusClass = (): string => {
  if (syncing.value) return 'status-syncing';
  if (!networkStatus.value.online) return 'status-offline';
  if (conflictCount.value > 0) return 'status-conflict';
  if (pendingCount.value > 0) return 'status-pending';
  return 'status-online';
};

// 获取网络图标
const getNetworkIcon = (): string => {
  if (syncing.value) return '🔄';
  if (!networkStatus.value.online) return '📵';
  if (conflictCount.value > 0) return '⚠️';
  if (pendingCount.value > 0) return '⏳';
  return '✅';
};

// 获取网络状态文本
const getNetworkStatusText = (): string => {
  if (syncing.value) return '同步中';
  if (!networkStatus.value.online) return '离线模式';
  if (conflictCount.value > 0) return '有冲突';
  if (pendingCount.value > 0) return '待同步';
  return '已连接';
};

// 获取操作图标
const getOperationIcon = (status: SyncStatus): string => {
  switch (status) {
    case SyncStatus.PENDING: return '⏳';
    case SyncStatus.SYNCING: return '🔄';
    case SyncStatus.SYNCED: return '✅';
    case SyncStatus.FAILED: return '❌';
    case SyncStatus.CONFLICTED: return '⚠️';
    default: return '•';
  }
};

// 获取状态样式类
const getStatusClass = (status: SyncStatus): string => {
  return `status-${status.toLowerCase()}`;
};

// 获取状态文本
const getStatusText = (status: SyncStatus): string => {
  const texts = {
    [SyncStatus.PENDING]: '等待',
    [SyncStatus.SYNCING]: '同步中',
    [SyncStatus.SYNCED]: '已同步',
    [SyncStatus.FAILED]: '失败',
    [SyncStatus.CONFLICTED]: '冲突'
  };
  return texts[status] || '未知';
};

// 格式化离线时间
const formatOfflineTime = (): string => {
  if (!networkStatus.value.lastOfflineTime) return '未知';
  return formatDistanceToNow(networkStatus.value.lastOfflineTime, {
    addSuffix: true,
    locale: zhCN
  });
};

// 格式化在线时间
const formatOnlineTime = (): string => {
  if (!networkStatus.value.lastOnlineTime) return '未知';
  return formatDistanceToNow(networkStatus.value.lastOnlineTime, {
    addSuffix: true,
    locale: zhCN
  });
};

// 格式化相对时间
const formatRelativeTime = (timestamp: number): string => {
  return formatDistanceToNow(timestamp, {
    addSuffix: true,
    locale: zhCN
  });
};

// 处理立即同步
const handleSyncAll = async () => {
  if (!networkStatus.value.online || syncing.value) return;

  syncing.value = true;
  syncProgress.value = 0;

  try {
    const pendingOps = operations.value.filter(op =>
      op.status === SyncStatus.PENDING || op.status === SyncStatus.FAILED
    );

    if (pendingOps.length === 0) {
      syncing.value = false;
      return;
    }

    // 模拟进度更新
    const progressInterval = setInterval(() => {
      if (syncProgress.value < 90) {
        syncProgress.value += Math.random() * 10;
      }
    }, 500);

    const results = await props.syncQueue.syncAll();

    clearInterval(progressInterval);
    syncProgress.value = 100;

    // 更新操作列表
    updateOperations();

    emit('syncCompleted', results);

    setTimeout(() => {
      syncing.value = false;
      syncProgress.value = 0;
    }, 1000);

  } catch (error) {
    console.error('同步失败:', error);
    syncing.value = false;
    syncProgress.value = 0;
  }
};

// 查看队列
const handleViewQueue = () => {
  emit('viewQueue');
  showDetails.value = false;
};

// 查看冲突
const handleViewConflicts = () => {
  emit('viewConflicts');
  showDetails.value = false;
};

// 重试失败操作
const handleRetryFailed = async () => {
  syncing.value = true;
  try {
    await props.syncQueue.retryFailedOperations();
    updateOperations();
  } catch (error) {
    console.error('重试失败:', error);
  } finally {
    syncing.value = false;
  }
};

// 清除已同步操作
const handleClearSynced = () => {
  props.syncQueue.clearSyncedOperations();
  updateOperations();
};

// 更新操作列表
const updateOperations = () => {
  operations.value = props.syncQueue.getAllOperations();
  networkStatus.value = props.syncQueue.getNetworkStatus();
};

// 设置事件监听
const setupEventListeners = () => {
  props.syncQueue.on('operationAdded', updateOperations);
  props.syncQueue.on('syncCompleted', updateOperations);
  props.syncQueue.on('networkOnline', (status: NetworkStatus) => {
    networkStatus.value = status;
    updateOperations();
  });
  props.syncQueue.on('networkOffline', (status: NetworkStatus) => {
    networkStatus.value = status;
    updateOperations();
  });
};

// 清除事件监听
const cleanupEventListeners = () => {
  props.syncQueue.off('operationAdded', updateOperations);
  props.syncQueue.off('syncCompleted', updateOperations);
  props.syncQueue.off('networkOnline');
  props.syncQueue.off('networkOffline');
};

// 生命周期钩子
onMounted(() => {
  updateOperations();
  setupEventListeners();
});

onUnmounted(() => {
  cleanupEventListeners();
});

// 定时更新
const updateTimer = setInterval(updateOperations, 5000);
onUnmounted(() => {
  clearInterval(updateTimer);
});
</script>

<style scoped>
.offline-status-indicator {
  position: relative;
  z-index: 1000;
}

.network-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  min-width: 120px;
}

.network-status:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.status-online {
  border-color: #52c41a;
  background: linear-gradient(45deg, #f6ffed 0%, #ffffff 100%);
}

.status-offline {
  border-color: #8c8c8c;
  background: linear-gradient(45deg, #f5f5f5 0%, #ffffff 100%);
}

.status-pending {
  border-color: #faad14;
  background: linear-gradient(45deg, #fffbe6 0%, #ffffff 100%);
}

.status-conflict {
  border-color: #f5222d;
  background: linear-gradient(45deg, #fff2f0 0%, #ffffff 100%);
}

.status-syncing {
  border-color: #1890ff;
  background: linear-gradient(45deg, #e6f7ff 0%, #ffffff 100%);
}

.status-icon {
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-text {
  font-size: 13px;
  font-weight: 500;
  color: #262626;
  white-space: nowrap;
}

.pending-badge {
  background: #f5222d;
  color: white;
  font-size: 10px;
  font-weight: bold;
  padding: 2px 6px;
  border-radius: 8px;
  min-width: 16px;
  text-align: center;
}

.status-details {
  position: absolute;
  top: 100%;
  right: 0;
  width: 320px;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 1001;
  margin-top: 8px;
  overflow: hidden;
}

.details-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: #fafafa;
}

.details-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.network-info {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  font-size: 12px;
  color: #8c8c8c;
}

.info-value {
  font-size: 12px;
  color: #262626;
  font-weight: 500;
}

.info-value.online {
  color: #52c41a;
}

.info-value.offline {
  color: #f5222d;
}

.sync-stats {
  padding: 12px 16px;
  display: flex;
  justify-content: space-around;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-number {
  font-size: 20px;
  font-weight: bold;
  line-height: 1;
}

.stat-number.pending { color: #faad14; }
.stat-number.conflict { color: #f5222d; }
.stat-number.synced { color: #52c41a; }

.stat-label {
  font-size: 11px;
  color: #8c8c8c;
}

.sync-actions {
  padding: 12px 16px;
  display: flex;
  gap: 8px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.recent-operations {
  padding: 12px 16px;
}

.recent-title {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
  font-weight: 600;
}

.operation-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.operation-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  background: #fafafa;
  border-radius: 4px;
}

.operation-icon {
  font-size: 12px;
  width: 16px;
  text-align: center;
}

.operation-info {
  flex: 1;
  min-width: 0;
}

.operation-field {
  font-size: 12px;
  font-weight: 500;
  color: #262626;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.operation-time {
  font-size: 10px;
  color: #8c8c8c;
}

.operation-status {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 3px;
  font-weight: 500;
}

.status-pending { background: #fff7e6; color: #d48806; }
.status-syncing { background: #e6f7ff; color: #1890ff; }
.status-synced { background: #f6ffed; color: #389e0d; }
.status-failed { background: #fff2f0; color: #cf1322; }
.status-conflicted { background: #fff0f6; color: #c41d7f; }

.sync-progress {
  position: absolute;
  bottom: -30px;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 6px;
  padding: 8px 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.progress-bar {
  width: 100%;
  height: 4px;
  background: #f0f0f0;
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 4px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #1890ff 0%, #40a9ff 100%);
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 11px;
  color: #8c8c8c;
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .status-details {
    width: calc(100vw - 40px);
    right: -20px;
  }

  .sync-actions {
    flex-direction: column;
  }

  .sync-stats {
    padding: 8px 12px;
  }

  .stat-number {
    font-size: 16px;
  }
}
</style>