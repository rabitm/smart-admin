<!--
  * 离线状态管理器组件 - 显示离线操作和同步状态
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-24
  * @Copyright 1024创新实验室
-->
<template>
  <div class="offline-status-manager">
    <!-- 离线状态指示器 -->
    <div
      class="offline-indicator"
      :class="[
        `status-${networkStatus.online ? 'online' : 'offline'}`,
        `quality-${networkQuality.quality}`
      ]"
      @click="toggleDetailPanel"
    >
      <div class="indicator-icon">
        <span v-if="networkStatus.online">🟢</span>
        <span v-else class="offline-icon">🔴</span>
      </div>

      <div class="indicator-text">
        <div class="status-line">
          {{ networkStatus.online ? '在线' : '离线' }}
          <span v-if="pendingCount > 0" class="pending-badge">{{ pendingCount }}</span>
        </div>
        <div v-if="networkStatus.online" class="quality-line">
          {{ getQualityText(networkQuality.quality) }}
        </div>
      </div>

      <div v-if="isSyncing" class="sync-spinner">
        <div class="spinner"></div>
      </div>
    </div>

    <!-- 详细状态面板 -->
    <div v-if="showDetailPanel" class="detail-panel">
      <!-- 网络状态 -->
      <div class="panel-section">
        <h4>网络状态</h4>
        <div class="network-info">
          <div class="info-row">
            <span class="label">连接状态:</span>
            <span :class="`status-${networkStatus.online ? 'online' : 'offline'}`">
              {{ networkStatus.online ? '在线' : '离线' }}
            </span>
          </div>

          <div v-if="networkStatus.online" class="info-row">
            <span class="label">网络质量:</span>
            <span :class="`quality-${networkQuality.quality}`">
              {{ getQualityText(networkQuality.quality) }}
            </span>
          </div>

          <div v-if="networkQuality.bandwidth" class="info-row">
            <span class="label">带宽:</span>
            <span>{{ networkQuality.bandwidth.toFixed(1) }}Mbps</span>
          </div>

          <div v-if="networkQuality.latency" class="info-row">
            <span class="label">延迟:</span>
            <span>{{ networkQuality.latency.toFixed(0) }}ms</span>
          </div>

          <div class="info-row">
            <span class="label">最后在线:</span>
            <span>{{ formatTime(networkStatus.lastOnlineTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 同步队列状态 -->
      <div class="panel-section">
        <h4>同步队列</h4>
        <div class="queue-stats">
          <div class="stat-group">
            <div class="stat-item critical">
              <div class="stat-value">{{ queueStats.byPriority[1] || 0 }}</div>
              <div class="stat-label">紧急</div>
            </div>
            <div class="stat-item high">
              <div class="stat-value">{{ queueStats.byPriority[2] || 0 }}</div>
              <div class="stat-label">重要</div>
            </div>
            <div class="stat-item normal">
              <div class="stat-value">{{ queueStats.byPriority[3] || 0 }}</div>
              <div class="stat-label">普通</div>
            </div>
            <div class="stat-item low">
              <div class="stat-value">{{ queueStats.byPriority[4] || 0 }}</div>
              <div class="stat-label">低优先级</div>
            </div>
          </div>

          <div class="queue-actions">
            <a-button
              type="primary"
              :loading="isSyncing"
              :disabled="!networkStatus.online || pendingCount === 0"
              @click="forceSyncAll"
            >
              <template #icon>🔄</template>
              强制同步
            </a-button>

            <a-button
              type="default"
              :disabled="queueStats.total === 0"
              @click="showQueueDetails = !showQueueDetails"
            >
              <template #icon>📋</template>
              {{ showQueueDetails ? '隐藏' : '详情' }}
            </a-button>

            <a-button
              type="default"
              @click="cleanup"
            >
              <template #icon>🗑️</template>
              清理
            </a-button>
          </div>
        </div>
      </div>

      <!-- 性能指标 -->
      <div class="panel-section">
        <h4>性能指标</h4>
        <div class="performance-metrics">
          <div class="metric-row">
            <span class="metric-label">同步成功率:</span>
            <div class="metric-value">
              <div class="progress-bar">
                <div
                  class="progress-fill"
                  :style="{ width: `${syncSuccessRate}%` }"
                ></div>
              </div>
              <span class="progress-text">{{ syncSuccessRate.toFixed(1) }}%</span>
            </div>
          </div>

          <div class="metric-row">
            <span class="metric-label">平均同步时间:</span>
            <span class="metric-value">{{ performanceMetrics.averageSyncTime.toFixed(0) }}ms</span>
          </div>

          <div class="metric-row">
            <span class="metric-label">网络效率:</span>
            <span class="metric-value">{{ (performanceMetrics.networkEfficiency * 100).toFixed(1) }}%</span>
          </div>

          <div class="metric-row">
            <span class="metric-label">压缩比率:</span>
            <span class="metric-value">{{ (performanceMetrics.compressionRatio * 100).toFixed(1) }}%</span>
          </div>
        </div>
      </div>

      <!-- 队列详情 -->
      <div v-if="showQueueDetails" class="panel-section">
        <h4>队列详情</h4>
        <div class="queue-details">
          <div class="queue-list" v-if="recentOperations.length > 0">
            <div
              v-for="operation in recentOperations"
              :key="operation.id"
              class="operation-item"
              :class="`priority-${operation.priority} status-${operation.status}`"
            >
              <div class="operation-header">
                <span class="operation-field">{{ operation.fieldName }}</span>
                <span class="operation-time">{{ formatTime(operation.timestamp) }}</span>
              </div>

              <div class="operation-details">
                <span class="operation-user">{{ operation.userName }}</span>
                <span class="operation-status" :class="`status-${operation.status}`">
                  {{ getStatusText(operation.status) }}
                </span>
              </div>

              <div v-if="operation.error" class="operation-error">
                {{ operation.error }}
              </div>

              <div v-if="operation.status === 'FAILED' || operation.status === 'CONFLICTED'" class="operation-actions">
                <a-button size="small" type="link" @click="retryOperation(operation)">
                  重试
                </a-button>
                <a-button size="small" type="link" danger @click="removeOperation(operation)">
                  移除
                </a-button>
              </div>
            </div>
          </div>

          <div v-else class="empty-queue">
            <div class="empty-icon">📭</div>
            <div class="empty-text">暂无待处理操作</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, reactive } from 'vue';
import type {
  EnhancedOfflineManager,
  NetworkQualityInfo,
  SyncPerformanceMetrics,
  NetworkQuality,
  Priority
} from '/@/utils/enhanced-offline-manager';
import type { SyncStatus, OfflineOperation } from '/@/utils/offline-sync-queue';

interface Props {
  offlineManager?: EnhancedOfflineManager;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  syncCompleted: [results: Map<string, any>];
  operationRetried: [operation: OfflineOperation];
  operationRemoved: [operation: OfflineOperation];
}>();

// 响应式状态
const showDetailPanel = ref(false);
const showQueueDetails = ref(false);
const isSyncing = ref(false);

const networkStatus = reactive({
  online: navigator.onLine,
  lastOnlineTime: Date.now(),
  lastOfflineTime: 0
});

const networkQuality = ref<NetworkQualityInfo>({
  quality: 'good' as NetworkQuality,
  bandwidth: 1,
  latency: 100,
  packetLoss: 0,
  timestamp: Date.now(),
  connectionType: 'unknown'
});

const performanceMetrics = ref<SyncPerformanceMetrics>({
  totalOperations: 0,
  successfulSyncs: 0,
  failedSyncs: 0,
  conflictedSyncs: 0,
  averageSyncTime: 0,
  totalDataSynced: 0,
  compressionRatio: 0.7,
  cacheHitRate: 0.8,
  networkEfficiency: 0.75
});

const queueStats = ref({
  total: 0,
  byPriority: {} as Record<Priority, number>,
  byStatus: {} as Record<SyncStatus, number>
});

const recentOperations = ref<OfflineOperation[]>([]);

// 计算属性
const pendingCount = computed(() => {
  return (queueStats.value.byStatus['PENDING'] || 0) + (queueStats.value.byStatus['FAILED'] || 0);
});

const syncSuccessRate = computed(() => {
  const total = performanceMetrics.value.totalOperations;
  if (total === 0) return 100;
  return (performanceMetrics.value.successfulSyncs / total) * 100;
});

// 方法
const toggleDetailPanel = () => {
  showDetailPanel.value = !showDetailPanel.value;
};

const getQualityText = (quality: NetworkQuality): string => {
  const qualityMap = {
    'excellent': '优秀',
    'good': '良好',
    'fair': '一般',
    'poor': '较差'
  };
  return qualityMap[quality] || '未知';
};

const getStatusText = (status: SyncStatus): string => {
  const statusMap = {
    'PENDING': '待同步',
    'SYNCING': '同步中',
    'SYNCED': '已同步',
    'FAILED': '失败',
    'CONFLICTED': '冲突'
  };
  return statusMap[status] || status;
};

const formatTime = (timestamp: number): string => {
  if (!timestamp) return '从未';
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now.getTime() - timestamp;

  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
  return date.toLocaleDateString();
};

const forceSyncAll = async () => {
  if (isSyncing.value) return;

  isSyncing.value = true;
  try {
    const results = await props.offlineManager.forceSyncAll();
    emit('syncCompleted', results);
    updateStats();
  } catch (error) {
    console.error('强制同步失败:', error);
  } finally {
    isSyncing.value = false;
  }
};

const cleanup = async () => {
  await props.offlineManager.cleanup();
  updateStats();
};

const retryOperation = async (operation: OfflineOperation) => {
  // 实现重试逻辑
  emit('operationRetried', operation);
  updateStats();
};

const removeOperation = (operation: OfflineOperation) => {
  // 实现移除逻辑
  emit('operationRemoved', operation);
  updateStats();
};

const updateStats = () => {
  if (!props.offlineManager) {
    console.warn('[OfflineStatusManager] offlineManager prop is undefined');
    return;
  }

  networkQuality.value = props.offlineManager.getNetworkQuality();
  performanceMetrics.value = props.offlineManager.getPerformanceMetrics();
  queueStats.value = props.offlineManager.getQueueStats();

  // 更新最近操作（模拟获取）
  // recentOperations.value = props.offlineManager.getRecentOperations();
};

const handleOnline = () => {
  networkStatus.online = true;
  networkStatus.lastOnlineTime = Date.now();
  updateStats();
};

const handleOffline = () => {
  networkStatus.online = false;
  networkStatus.lastOfflineTime = Date.now();
};

// 生命周期
onMounted(() => {
  window.addEventListener('online', handleOnline);
  window.addEventListener('offline', handleOffline);

  // 定期更新状态
  const updateInterval = setInterval(updateStats, 5000);

  onUnmounted(() => {
    window.removeEventListener('online', handleOnline);
    window.removeEventListener('offline', handleOffline);
    clearInterval(updateInterval);
  });

  // 初始状态更新
  updateStats();
});

// 监听网络状态变化
watch(() => navigator.onLine, (isOnline) => {
  if (isOnline) {
    handleOnline();
  } else {
    handleOffline();
  }
});
</script>

<style scoped>
.offline-status-manager {
  position: relative;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.offline-indicator {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 120px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.offline-indicator:hover {
  background: rgba(255, 255, 255, 1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.indicator-icon {
  margin-right: 8px;
  font-size: 12px;
}

.offline-icon {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.indicator-text {
  flex: 1;
  font-size: 12px;
}

.status-line {
  font-weight: 500;
  color: #1a1a1a;
  display: flex;
  align-items: center;
  gap: 6px;
}

.quality-line {
  color: #666;
  margin-top: 2px;
  font-size: 11px;
}

.pending-badge {
  background: #ff4d4f;
  color: white;
  font-size: 10px;
  padding: 1px 4px;
  border-radius: 8px;
  font-weight: 600;
  min-width: 16px;
  text-align: center;
}

.sync-spinner {
  margin-left: 8px;
}

.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid #f0f0f0;
  border-top: 2px solid #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 状态样式 */
.status-online {
  border-left: 3px solid #52c41a;
}

.status-offline {
  border-left: 3px solid #ff4d4f;
}

.quality-excellent { color: #52c41a; }
.quality-good { color: #1890ff; }
.quality-fair { color: #fa8c16; }
.quality-poor { color: #ff4d4f; }

/* 详细面板 */
.detail-panel {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  width: 400px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(0, 0, 0, 0.06);
  z-index: 1000;
  max-height: 600px;
  overflow-y: auto;
}

.panel-section {
  padding: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.panel-section:last-child {
  border-bottom: none;
}

.panel-section h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

/* 网络信息 */
.network-info .info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
}

.network-info .label {
  color: #666;
}

/* 队列统计 */
.stat-group {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.stat-item {
  text-align: center;
  padding: 8px;
  border-radius: 6px;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.stat-item.critical { background: rgba(255, 77, 79, 0.1); border-color: rgba(255, 77, 79, 0.2); }
.stat-item.high { background: rgba(250, 140, 22, 0.1); border-color: rgba(250, 140, 22, 0.2); }
.stat-item.normal { background: rgba(24, 144, 255, 0.1); border-color: rgba(24, 144, 255, 0.2); }
.stat-item.low { background: rgba(82, 196, 26, 0.1); border-color: rgba(82, 196, 26, 0.2); }

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.stat-label {
  font-size: 11px;
  color: #666;
  margin-top: 2px;
}

.queue-actions {
  display: flex;
  gap: 8px;
}

.queue-actions .ant-btn {
  font-size: 12px;
  height: 28px;
  padding: 0 8px;
}

/* 性能指标 */
.performance-metrics .metric-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 12px;
}

.metric-label {
  color: #666;
}

.metric-value {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-bar {
  width: 80px;
  height: 4px;
  background: #f0f0f0;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #1890ff;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 11px;
  color: #666;
  min-width: 30px;
}

/* 队列详情 */
.queue-details {
  max-height: 300px;
  overflow-y: auto;
}

.operation-item {
  padding: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 6px;
  margin-bottom: 8px;
  font-size: 12px;
}

.operation-item.priority-1 { border-left: 3px solid #ff4d4f; }
.operation-item.priority-2 { border-left: 3px solid #fa8c16; }
.operation-item.priority-3 { border-left: 3px solid #1890ff; }
.operation-item.priority-4 { border-left: 3px solid #52c41a; }

.operation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
  margin-bottom: 4px;
}

.operation-field {
  color: #1a1a1a;
}

.operation-time {
  color: #999;
  font-size: 11px;
}

.operation-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.operation-user {
  color: #666;
}

.operation-status {
  font-size: 11px;
  padding: 1px 4px;
  border-radius: 2px;
}

.operation-status.status-PENDING { background: #f0f0f0; color: #666; }
.operation-status.status-SYNCING { background: #e6f7ff; color: #1890ff; }
.operation-status.status-SYNCED { background: #f6ffed; color: #52c41a; }
.operation-status.status-FAILED { background: #fff2f0; color: #ff4d4f; }
.operation-status.status-CONFLICTED { background: #fff7e6; color: #fa8c16; }

.operation-error {
  color: #ff4d4f;
  font-size: 11px;
  margin-top: 4px;
  background: #fff2f0;
  padding: 4px;
  border-radius: 4px;
}

.operation-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.empty-queue {
  text-align: center;
  padding: 32px 16px;
  color: #999;
}

.empty-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.empty-text {
  font-size: 14px;
}

/* 滚动条样式 */
.detail-panel::-webkit-scrollbar,
.queue-details::-webkit-scrollbar {
  width: 4px;
}

.detail-panel::-webkit-scrollbar-track,
.queue-details::-webkit-scrollbar-track {
  background: transparent;
}

.detail-panel::-webkit-scrollbar-thumb,
.queue-details::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 2px;
}
</style>