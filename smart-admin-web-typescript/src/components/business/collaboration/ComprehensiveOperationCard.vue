<!--
  * 操作记录卡片组件
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-28
  * @Copyright 1024创新实验室
-->
<template>
  <div
    class="operation-card"
    :class="{
      'operation-card-expanded': showDetails,
      'operation-card-critical': operation.securityLevel === 'critical',
      'operation-card-high': operation.securityLevel === 'high',
      'operation-card-medium': operation.securityLevel === 'medium'
    }"
  >
    <!-- 卡片头部 -->
    <div class="operation-header">
      <div class="user-info">
        <div
          class="user-avatar"
          :style="{ backgroundColor: getUserColor(operation.userName) }"
        >
          <img
            v-if="operation.metadata?.avatar"
            :src="operation.metadata.avatar"
            :alt="operation.userName"
          />
          <span v-else class="avatar-text">{{ getAvatarText(operation.userName) }}</span>
        </div>
        <div class="user-details">
          <span class="user-name">{{ operation.userName }}</span>
          <span v-if="operation.department" class="user-department">{{ operation.department }}</span>
        </div>
      </div>

      <div class="operation-meta">
        <div class="operation-time">
          <a-tooltip :title="formatFullTime(operation.timestamp)">
            {{ formatRelativeTime(operation.timestamp) }}
          </a-tooltip>
        </div>
        <div class="operation-level">
          <a-tag :color="getLevelColor(operation.level)" size="small">
            {{ getLevelLabel(operation.level) }}
          </a-tag>
        </div>
        <div v-if="operation.securityLevel !== 'low'" class="security-level">
          <a-tag :color="getSecurityColor(operation.securityLevel)" size="small">
            {{ getSecurityLabel(operation.securityLevel) }}
          </a-tag>
        </div>
      </div>
    </div>

    <!-- 操作描述 -->
    <div class="operation-description">
      <div class="operation-title">
        <component :is="getOperationIcon(operation.type)" class="operation-icon" />
        <span class="operation-text">{{ getOperationDescription(operation) }}</span>
        <a-badge
          v-if="operation.collaborationContext?.conflictUsers?.length"
          :count="operation.collaborationContext.conflictUsers.length"
          :title="`涉及${operation.collaborationContext.conflictUsers.length}个用户冲突`"
        />
      </div>

      <!-- 字段信息 -->
      <div v-if="operation.fieldLabel" class="field-info">
        <span class="field-label">{{ operation.fieldLabel }}</span>
        <span v-if="operation.fieldPath !== operation.fieldLabel" class="field-path">
          ({{ operation.fieldPath }})
        </span>
      </div>
    </div>

    <!-- 数据变更详情 -->
    <div
      v-if="hasValueChange && aggregationLevel <= 3"
      class="value-change-section"
    >
      <div class="value-change-header">
        <span class="section-title">数据变更</span>
        <a-button
          type="text"
          size="small"
          @click="toggleValueDetails"
        >
          <template #icon>
            <DownOutlined :class="{ 'rotate-180': showValueDetails }" />
          </template>
        </a-button>
      </div>

      <a-collapse-transition>
        <div v-show="showValueDetails" class="value-change-content">
          <div v-if="operation.beforeValue !== undefined" class="value-item old-value">
            <span class="value-label">修改前:</span>
            <div class="value-content">
              <ValueDisplay :value="operation.beforeValue" type="before" />
            </div>
          </div>

          <div class="value-item new-value">
            <span class="value-label">
              {{ operation.beforeValue !== undefined ? '修改后:' : '设置为:' }}
            </span>
            <div class="value-content">
              <ValueDisplay :value="operation.afterValue" type="after" />
            </div>
          </div>

          <!-- 增量数据展示 -->
          <div v-if="operation.deltaData && aggregationLevel <= 2" class="delta-section">
            <span class="value-label">变更详情:</span>
            <div class="delta-content">
              <pre>{{ JSON.stringify(operation.deltaData, null, 2) }}</pre>
            </div>
          </div>
        </div>
      </a-collapse-transition>
    </div>

    <!-- 业务上下文 -->
    <div
      v-if="hasBusinessContext && aggregationLevel <= 3"
      class="business-context-section"
    >
      <div class="context-header">
        <span class="section-title">业务信息</span>
      </div>
      <div class="context-content">
        <div
          v-for="(value, key) in operation.businessContext"
          :key="key"
          class="context-item"
        >
          <span class="context-label">{{ getBusinessContextLabel(key) }}:</span>
          <span class="context-value">{{ value }}</span>
        </div>
      </div>
    </div>

    <!-- 协作信息 -->
    <div
      v-if="operation.collaborationContext && aggregationLevel <= 2"
      class="collaboration-section"
    >
      <div class="section-header">
        <span class="section-title">协作信息</span>
      </div>
      <div class="collaboration-content">
        <div v-if="operation.collaborationContext.conflictUsers?.length" class="conflict-info">
          <span class="conflict-label">冲突用户:</span>
          <a-tag
            v-for="user in operation.collaborationContext.conflictUsers"
            :key="user"
            size="small"
            color="orange"
          >
            {{ user }}
          </a-tag>
        </div>
        <div v-if="operation.collaborationContext.lockDuration" class="lock-info">
          <span class="lock-label">锁定时长:</span>
          <span class="lock-duration">{{ formatDuration(operation.collaborationContext.lockDuration) }}</span>
        </div>
        <div v-if="operation.collaborationContext.resolutionStrategy" class="resolution-info">
          <span class="resolution-label">解决策略:</span>
          <span class="resolution-strategy">{{ operation.collaborationContext.resolutionStrategy }}</span>
        </div>
      </div>
    </div>

    <!-- 技术详情 -->
    <div
      v-if="showDetails && aggregationLevel <= 1"
      class="technical-details-section"
    >
      <div class="section-header">
        <span class="section-title">技术详情</span>
      </div>
      <div class="technical-content">
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">会话ID:</span>
            <span class="detail-value">{{ operation.sessionId }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">IP地址:</span>
            <span class="detail-value">{{ operation.ipAddress || '未知' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">页面URL:</span>
            <span class="detail-value">{{ operation.pageUrl }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">视口大小:</span>
            <span class="detail-value">
              {{ operation.viewport.width }}x{{ operation.viewport.height }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 合规标记 -->
    <div v-if="operation.complianceFlags?.length" class="compliance-section">
      <a-tag
        v-for="flag in operation.complianceFlags"
        :key="flag"
        color="purple"
        size="small"
      >
        {{ getComplianceLabel(flag) }}
      </a-tag>
    </div>

    <!-- 操作按钮 -->
    <div class="operation-actions">
      <a-button-group size="small">
        <a-button
          type="text"
          @click="handleAction('expand')"
          :disabled="showDetails"
        >
          <template #icon><EyeOutlined /></template>
          详情
        </a-button>

        <a-button
          v-if="canRevert"
          type="text"
          @click="handleAction('revert')"
        >
          <template #icon><UndoOutlined /></template>
          回滚
        </a-button>

        <a-button
          v-if="canReplay"
          type="text"
          @click="handleAction('replay')"
        >
          <template #icon><PlayCircleOutlined /></template>
          回放
        </a-button>

        <a-button
          type="text"
          @click="handleAction('export')"
        >
          <template #icon><DownloadOutlined /></template>
          导出
        </a-button>

        <a-dropdown>
          <a-button type="text">
            <template #icon><MoreOutlined /></template>
          </a-button>
          <template #overlay>
            <a-menu @click="handleMenuAction">
              <a-menu-item key="copy">
                <CopyOutlined /> 复制信息
              </a-menu-item>
              <a-menu-item key="report">
                <FlagOutlined /> 标记异常
              </a-menu-item>
              <a-menu-item key="analyze">
                <BarChartOutlined /> 分析模式
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-button-group>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { format, formatDistanceToNow } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import {
  DownOutlined,
  EyeOutlined,
  UndoOutlined,
  PlayCircleOutlined,
  DownloadOutlined,
  MoreOutlined,
  CopyOutlined,
  FlagOutlined,
  BarChartOutlined
} from '@ant-design/icons-vue';
import type { ComprehensiveOperationRecord, OperationType } from '/@/utils/comprehensive-operation-tracker';
import ValueDisplay from './ValueDisplay.vue';

// =============== Props 和 Emits ===============

interface Props {
  operation: ComprehensiveOperationRecord;
  showDetails?: boolean;
  aggregationLevel?: number;
}

const props = withDefaults(defineProps<Props>(), {
  showDetails: false,
  aggregationLevel: 3
});

const emit = defineEmits<{
  action: [action: string, operation: ComprehensiveOperationRecord];
  expand: [operation: ComprehensiveOperationRecord];
}>();

// =============== 响应式数据 ===============

const showValueDetails = ref(false);

// =============== 计算属性 ===============

const hasValueChange = computed(() => {
  return props.operation.beforeValue !== undefined ||
         props.operation.afterValue !== undefined;
});

const hasBusinessContext = computed(() => {
  return props.operation.businessContext &&
         Object.keys(props.operation.businessContext).length > 0;
});

const canRevert = computed(() => {
  // 只有字段更新和记录更新操作可以回滚
  const revertableTypes = ['field_update', 'save_record', 'status_change'];
  return revertableTypes.includes(props.operation.type) &&
         props.operation.securityLevel !== 'critical';
});

const canReplay = computed(() => {
  // 所有用户操作都可以回放
  return props.operation.level !== 'system';
});

// =============== 方法 ===============

const toggleValueDetails = () => {
  showValueDetails.value = !showValueDetails.value;
};

const handleAction = (action: string) => {
  if (action === 'expand') {
    emit('expand', props.operation);
  } else {
    emit('action', action, props.operation);
  }
};

const handleMenuAction = ({ key }: { key: string }) => {
  switch (key) {
    case 'copy':
      copyOperationInfo();
      break;
    case 'report':
      reportAnomaly();
      break;
    case 'analyze':
      analyzeOperation();
      break;
  }
};

const copyOperationInfo = () => {
  const info = `
操作类型: ${getOperationDescription(props.operation)}
操作用户: ${props.operation.userName}
操作时间: ${formatFullTime(props.operation.timestamp)}
字段: ${props.operation.fieldLabel}
${hasValueChange.value ? `变更: ${props.operation.beforeValue} → ${props.operation.afterValue}` : ''}
`.trim();

  navigator.clipboard.writeText(info).then(() => {
    console.log('操作信息已复制到剪贴板');
  });
};

const reportAnomaly = () => {
  emit('action', 'report_anomaly', props.operation);
};

const analyzeOperation = () => {
  emit('action', 'analyze', props.operation);
};

// =============== 工具方法 ===============

const getAvatarText = (name: string): string => {
  if (!name) return '?';

  if (/[\u4e00-\u9fa5]/.test(name)) {
    return name.slice(-1);
  }

  const words = name.split(' ');
  return words.map(word => word.charAt(0)).join('').substring(0, 2).toUpperCase();
};

const getUserColor = (name: string): string => {
  const colors = [
    '#f56a00', '#7265e6', '#ffbf00', '#00a2ae',
    '#f56a00', '#52c41a', '#1890ff', '#722ed1'
  ];

  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }

  return colors[Math.abs(hash) % colors.length];
};

const getOperationIcon = (type: OperationType): string => {
  const icons: Record<string, string> = {
    input_start: '✏️',
    input_change: '⌨️',
    input_end: '✅',
    select_option: '📋',
    checkbox_toggle: '☑️',
    radio_select: '🔘',
    focus_field: '👁️',
    blur_field: '👁️‍🗨️',
    scroll_page: '📜',
    resize_window: '🪟',
    save_record: '💾',
    delete_record: '🗑️',
    status_change: '🔄',
    field_update: '📝',
    field_lock: '🔒',
    field_unlock: '🔓',
    conflict_resolve: '⚖️',
    user_join: '👋',
    user_leave: '👋',
    page_load: '🌐',
    page_unload: '🌐',
    error_occurred: '❌'
  };

  return icons[type] || '•';
};

const getOperationDescription = (operation: ComprehensiveOperationRecord): string => {
  const descriptions: Record<string, string> = {
    input_start: '开始输入',
    input_change: '输入内容',
    input_end: '结束输入',
    select_option: '选择选项',
    checkbox_toggle: '切换复选框',
    radio_select: '选择单选项',
    focus_field: '聚焦字段',
    blur_field: '离开字段',
    scroll_page: '滚动页面',
    resize_window: '调整窗口',
    save_record: '保存记录',
    delete_record: '删除记录',
    status_change: '变更状态',
    field_update: '更新字段',
    field_lock: '锁定字段',
    field_unlock: '解锁字段',
    conflict_resolve: '解决冲突',
    user_join: '用户加入',
    user_leave: '用户离开',
    page_load: '页面加载',
    page_unload: '页面卸载',
    error_occurred: '发生错误'
  };

  return descriptions[operation.type] || operation.type;
};

const getLevelColor = (level: string): string => {
  const colors: Record<string, string> = {
    keystroke: 'default',
    field: 'blue',
    form: 'green',
    record: 'orange',
    system: 'red'
  };
  return colors[level] || 'default';
};

const getLevelLabel = (level: string): string => {
  const labels: Record<string, string> = {
    keystroke: '键盘',
    field: '字段',
    form: '表单',
    record: '记录',
    system: '系统'
  };
  return labels[level] || level;
};

const getSecurityColor = (level: string): string => {
  const colors: Record<string, string> = {
    low: 'default',
    medium: 'warning',
    high: 'orange',
    critical: 'red'
  };
  return colors[level] || 'default';
};

const getSecurityLabel = (level: string): string => {
  const labels: Record<string, string> = {
    low: '低',
    medium: '中',
    high: '高',
    critical: '关键'
  };
  return labels[level] || level;
};

const getBusinessContextLabel = (key: string): string => {
  const labels: Record<string, string> = {
    reportType: '报告类型',
    priority: '优先级',
    status: '状态',
    assignee: '指派人'
  };
  return labels[key] || key;
};

const getComplianceLabel = (flag: string): string => {
  const labels: Record<string, string> = {
    'data-deletion': '数据删除',
    'personal-data': '个人数据',
    'critical-operation': '关键操作'
  };
  return labels[flag] || flag;
};

const formatRelativeTime = (timestamp: number): string => {
  return formatDistanceToNow(timestamp, {
    addSuffix: true,
    locale: zhCN
  });
};

const formatFullTime = (timestamp: number): string => {
  return format(timestamp, 'yyyy-MM-dd HH:mm:ss', { locale: zhCN });
};

const formatDuration = (milliseconds: number): string => {
  const seconds = Math.floor(milliseconds / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);

  if (hours > 0) {
    return `${hours}小时${minutes % 60}分钟`;
  } else if (minutes > 0) {
    return `${minutes}分钟${seconds % 60}秒`;
  } else {
    return `${seconds}秒`;
  }
};
</script>

<style scoped>
.operation-card {
  background: white;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 8px;
  transition: all 0.3s ease;
  position: relative;
}

.operation-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #d9d9d9;
}

.operation-card-expanded {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
}

.operation-card-critical {
  border-left: 4px solid #f5222d;
}

.operation-card-high {
  border-left: 4px solid #fa8c16;
}

.operation-card-medium {
  border-left: 4px solid #faad14;
}

.operation-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
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

.user-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  line-height: 1.4;
}

.user-department {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.4;
}

.operation-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.operation-time {
  font-size: 12px;
  color: #8c8c8c;
}

.operation-description {
  margin-bottom: 12px;
}

.operation-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.operation-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.operation-text {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  flex: 1;
}

.field-info {
  font-size: 12px;
  color: #8c8c8c;
}

.field-label {
  color: #1890ff;
  font-weight: 500;
}

.field-path {
  margin-left: 4px;
}

.value-change-section,
.business-context-section,
.collaboration-section,
.technical-details-section {
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
}

.value-change-header,
.context-header,
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: #595959;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.rotate-180 {
  transform: rotate(180deg);
}

.value-change-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.value-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.value-label {
  font-size: 11px;
  color: #8c8c8c;
  min-width: 60px;
  margin-top: 2px;
  flex-shrink: 0;
}

.value-content {
  flex: 1;
  min-width: 0;
}

.delta-section {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.delta-content {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 8px;
  font-family: 'Courier New', monospace;
  font-size: 11px;
  color: #595959;
  max-height: 120px;
  overflow: auto;
}

.delta-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.context-content {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.context-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.context-label {
  color: #8c8c8c;
}

.context-value {
  color: #262626;
  font-weight: 500;
}

.collaboration-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.conflict-info,
.lock-info,
.resolution-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.conflict-label,
.lock-label,
.resolution-label {
  color: #8c8c8c;
  min-width: 60px;
}

.lock-duration,
.resolution-strategy {
  color: #262626;
  font-weight: 500;
}

.technical-content {
  font-size: 12px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 8px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-label {
  color: #8c8c8c;
  font-weight: 500;
}

.detail-value {
  color: #262626;
  font-family: monospace;
  word-break: break-all;
  text-align: right;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.compliance-section {
  margin-bottom: 12px;
}

.operation-actions {
  display: flex;
  justify-content: flex-end;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .operation-card {
    padding: 12px;
  }

  .operation-header {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }

  .operation-meta {
    flex-direction: row;
    align-items: center;
    gap: 8px;
  }

  .user-info {
    gap: 8px;
  }

  .user-avatar {
    width: 28px;
    height: 28px;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 2px;
  }

  .detail-value {
    max-width: none;
    text-align: left;
  }

  .value-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .value-label {
    min-width: auto;
    margin-top: 0;
  }

  .context-content {
    flex-direction: column;
    gap: 6px;
  }

  .context-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 2px;
  }
}

/* 动画效果 */
.operation-card {
  animation: slideInUp 0.3s ease;
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 暗色主题支持 */
@media (prefers-color-scheme: dark) {
  .operation-card {
    background: #1f1f1f;
    border-color: #303030;
  }

  .operation-card:hover {
    border-color: #404040;
  }

  .value-change-section,
  .business-context-section,
  .collaboration-section,
  .technical-details-section {
    background: #262626;
    border-color: #303030;
  }

  .delta-content {
    background: #1f1f1f;
    border-color: #303030;
    color: #d9d9d9;
  }
}
</style>