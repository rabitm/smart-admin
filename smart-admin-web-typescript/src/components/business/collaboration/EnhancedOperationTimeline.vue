<!--
  * 增强版操作时间轴组件 - 丰富的操作类型和筛选功能
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-24
  * @Copyright 1024创新实验室
-->
<template>
  <div class="enhanced-operation-timeline">
    <!-- 工具栏 -->
    <div class="timeline-toolbar">
      <div class="toolbar-left">
        <span class="timeline-title">📊 操作时间轴</span>
        <div class="timeline-stats">
          <a-statistic-countdown
            v-if="isLiveMode"
            :value="Date.now() + 30000"
            format="mm:ss"
            @finish="refreshTimeline"
            title="自动刷新"
            value-style="font-size: 12px; color: #52c41a"
          />
          <span class="stat-item">
            <span class="stat-value">{{ filteredOperations.length }}</span>
            <span class="stat-label">个操作</span>
          </span>
          <span class="stat-item">
            <span class="stat-value">{{ uniqueUsers.length }}</span>
            <span class="stat-label">位用户</span>
          </span>
          <span class="stat-item" v-if="timeRange">
            <span class="stat-value">{{ formatDuration(timeRange.duration) }}</span>
            <span class="stat-label">时间跨度</span>
          </span>
        </div>
      </div>

      <div class="toolbar-right">
        <a-space>
          <!-- 实时模式切换 -->
          <a-switch
            v-model:checked="isLiveMode"
            size="small"
            @change="toggleLiveMode"
          >
            <template #checkedChildren>🔴 实时</template>
            <template #unCheckedChildren>⏸️ 暂停</template>
          </a-switch>

          <!-- 视图模式 -->
          <a-segmented
            v-model:value="viewMode"
            :options="[
              { label: '📋 列表', value: 'list' },
              { label: '📈 图表', value: 'chart' },
              { label: '🗂️ 分组', value: 'group' }
            ]"
            size="small"
          />

          <!-- 导出按钮 -->
          <a-button
            type="primary"
            size="small"
            @click="exportOperations"
            :loading="exportLoading"
          >
            📤 导出
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- 高级筛选面板 -->
    <div class="filter-panel" :class="{ 'filter-panel-expanded': showAdvancedFilters }">
      <div class="basic-filters">
        <a-space wrap>
          <!-- 操作类型筛选 -->
          <a-select
            v-model:value="filters.type"
            size="small"
            style="width: 140px"
            mode="multiple"
            placeholder="操作类型"
            :max-tag-count="2"
          >
            <a-select-option
              v-for="type in operationTypes"
              :key="type.value"
              :value="type.value"
            >
              {{ type.icon }} {{ type.label }}
            </a-select-option>
          </a-select>

          <!-- 用户筛选 -->
          <a-select
            v-model:value="filters.users"
            size="small"
            style="width: 140px"
            mode="multiple"
            placeholder="用户"
            :max-tag-count="2"
          >
            <a-select-option
              v-for="user in uniqueUsers"
              :key="user.id"
              :value="user.id"
            >
              <div class="user-option">
                <div class="user-avatar" :style="{ backgroundColor: user.color }">
                  {{ getAvatarText(user.name) }}
                </div>
                {{ user.name }}
              </div>
            </a-select-option>
          </a-select>

          <!-- 时间范围筛选 -->
          <a-range-picker
            v-model:value="filters.timeRange"
            size="small"
            style="width: 240px"
            show-time
            format="YYYY-MM-DD HH:mm"
            placeholder="['开始时间', '结束时间']"
          />

          <!-- 更多筛选按钮 -->
          <a-button
            size="small"
            type="text"
            @click="toggleAdvancedFilters"
          >
            {{ showAdvancedFilters ? '🔼 收起' : '🔽 更多筛选' }}
          </a-button>

          <!-- 清空筛选 -->
          <a-button
            size="small"
            type="text"
            @click="clearFilters"
            :disabled="!hasActiveFilters"
          >
            🗑️ 清空
          </a-button>
        </a-space>
      </div>

      <!-- 高级筛选选项 -->
      <div v-if="showAdvancedFilters" class="advanced-filters">
        <a-space wrap>
          <!-- 字段名筛选 -->
          <a-select
            v-model:value="filters.fieldNames"
            size="small"
            style="width: 160px"
            mode="multiple"
            placeholder="字段名"
            :max-tag-count="2"
          >
            <a-select-option
              v-for="field in uniqueFieldNames"
              :key="field"
              :value="field"
            >
              {{ getFieldDisplayName(field) }}
            </a-select-option>
          </a-select>

          <!-- 影响等级筛选 -->
          <a-select
            v-model:value="filters.impactLevel"
            size="small"
            style="width: 120px"
            placeholder="影响等级"
          >
            <a-select-option value="high">🔴 高影响</a-select-option>
            <a-select-option value="medium">🟡 中影响</a-select-option>
            <a-select-option value="low">🟢 低影响</a-select-option>
          </a-select>

          <!-- 包含冲突 -->
          <a-checkbox v-model:checked="filters.hasConflict">
            ⚠️ 包含冲突
          </a-checkbox>

          <!-- 搜索关键字 -->
          <a-input
            v-model:value="filters.keyword"
            size="small"
            style="width: 160px"
            placeholder="搜索关键字"
            :suffix="filters.keyword ? '🔍' : ''"
            @pressEnter="applyFilters"
          />
        </a-space>
      </div>
    </div>

    <!-- 时间轴内容 -->
    <div class="timeline-content" ref="timelineContentRef">
      <!-- 列表视图 -->
      <div v-if="viewMode === 'list'" class="timeline-list">
        <div
          v-for="(operation, index) in paginatedOperations"
          :key="operation.id"
          class="timeline-item"
          :class="{
            'timeline-item-selected': selectedOperation?.id === operation.id,
            'timeline-item-conflict': operation.hasConflict,
            'timeline-item-important': operation.impactLevel === 'high'
          }"
          @click="selectOperation(operation)"
        >
          <!-- 时间线 -->
          <div class="timeline-line">
            <div
              class="timeline-dot"
              :class="`timeline-dot-${operation.type}`"
              :style="{ backgroundColor: getOperationColor(operation) }"
            >
              <span class="operation-icon">{{ getOperationIcon(operation) }}</span>
            </div>
            <div
              v-if="index < paginatedOperations.length - 1"
              class="timeline-connector"
            ></div>
          </div>

          <!-- 操作内容 -->
          <div class="timeline-content-item">
            <div class="operation-header">
              <div class="operation-info">
                <span class="operation-type-badge" :style="{ color: getOperationColor(operation) }">
                  {{ getOperationTypeName(operation.type) }}
                </span>
                <span class="operation-time">
                  {{ formatTime(operation.timestamp) }}
                </span>
                <span v-if="operation.hasConflict" class="conflict-badge">
                  ⚠️ 冲突
                </span>
                <span class="impact-badge" :class="`impact-${operation.impactLevel}`">
                  {{ getImpactLevelText(operation.impactLevel) }}
                </span>
              </div>

              <div class="operation-actions">
                <a-tooltip title="查看详情">
                  <a-button
                    type="text"
                    size="small"
                    @click.stop="showOperationDetail(operation)"
                  >
                    👁️
                  </a-button>
                </a-tooltip>
                <a-tooltip title="回滚到此操作" v-if="canRollback(operation)">
                  <a-button
                    type="text"
                    size="small"
                    @click.stop="rollbackToOperation(operation)"
                  >
                    ↩️
                  </a-button>
                </a-tooltip>
              </div>
            </div>

            <div class="operation-content">
              <div class="operation-description">
                {{ getOperationDescription(operation) }}
              </div>

              <div class="operation-meta">
                <div class="user-info">
                  <div class="user-avatar" :style="{ backgroundColor: operation.user?.color || '#666' }">
                    {{ getAvatarText(operation.user?.name || '未知') }}
                  </div>
                  <span class="user-name">{{ operation.user?.name || '未知用户' }}</span>
                </div>

                <div v-if="operation.fieldName" class="field-info">
                  <span class="field-label">字段:</span>
                  <span class="field-name">{{ getFieldDisplayName(operation.fieldName) }}</span>
                </div>

                <div v-if="operation.changes" class="change-details">
                  <a-tag
                    v-if="operation.changes.oldValue !== undefined"
                    color="volcano"
                    size="small"
                  >
                    旧值: {{ formatValue(operation.changes.oldValue) }}
                  </a-tag>
                  <span v-if="operation.changes.oldValue !== undefined && operation.changes.newValue !== undefined"> → </span>
                  <a-tag
                    v-if="operation.changes.newValue !== undefined"
                    color="green"
                    size="small"
                  >
                    新值: {{ formatValue(operation.changes.newValue) }}
                  </a-tag>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMoreOperations" class="load-more">
          <a-button
            type="dashed"
            block
            @click="loadMoreOperations"
            :loading="loadingMore"
          >
            📥 加载更多操作
          </a-button>
        </div>
      </div>

      <!-- 图表视图 -->
      <div v-else-if="viewMode === 'chart'" class="timeline-chart">
        <div class="chart-container">
          <!-- 操作频率图表 -->
          <div class="chart-item">
            <h4>📈 操作频率趋势</h4>
            <div class="frequency-chart" ref="frequencyChartRef"></div>
          </div>

          <!-- 用户活跃度图表 -->
          <div class="chart-item">
            <h4>👥 用户活跃度</h4>
            <div class="user-activity-chart" ref="userActivityChartRef"></div>
          </div>

          <!-- 操作类型分布 -->
          <div class="chart-item">
            <h4>🥧 操作类型分布</h4>
            <div class="operation-pie-chart" ref="operationPieChartRef"></div>
          </div>
        </div>
      </div>

      <!-- 分组视图 -->
      <div v-else-if="viewMode === 'group'" class="timeline-groups">
        <div
          v-for="group in groupedOperations"
          :key="group.key"
          class="operation-group"
        >
          <div class="group-header">
            <div class="group-title">
              <span class="group-icon">{{ group.icon }}</span>
              <span class="group-name">{{ group.name }}</span>
              <span class="group-count">({{ group.operations.length }})</span>
            </div>
            <a-button
              type="text"
              size="small"
              @click="toggleGroupExpanded(group.key)"
            >
              {{ expandedGroups.includes(group.key) ? '🔼' : '🔽' }}
            </a-button>
          </div>

          <div v-if="expandedGroups.includes(group.key)" class="group-content">
            <div
              v-for="operation in group.operations"
              :key="operation.id"
              class="group-operation-item"
              @click="selectOperation(operation)"
            >
              <span class="operation-time">{{ formatTime(operation.timestamp) }}</span>
              <span class="operation-desc">{{ getOperationDescription(operation) }}</span>
              <span class="operation-user">{{ operation.user?.name }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 操作详情弹窗 -->
    <a-modal
      v-model:open="showDetailModal"
      title="操作详情"
      width="600px"
      :footer="null"
    >
      <div v-if="selectedOperation" class="operation-detail">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="操作ID">
            {{ selectedOperation.id }}
          </a-descriptions-item>
          <a-descriptions-item label="操作类型">
            {{ getOperationTypeName(selectedOperation.type) }}
          </a-descriptions-item>
          <a-descriptions-item label="操作时间">
            {{ formatDetailTime(selectedOperation.timestamp) }}
          </a-descriptions-item>
          <a-descriptions-item label="操作用户">
            {{ selectedOperation.user?.name || '未知用户' }}
          </a-descriptions-item>
          <a-descriptions-item label="字段名称" v-if="selectedOperation.fieldName">
            {{ getFieldDisplayName(selectedOperation.fieldName) }}
          </a-descriptions-item>
          <a-descriptions-item label="影响等级">
            {{ getImpactLevelText(selectedOperation.impactLevel) }}
          </a-descriptions-item>
          <a-descriptions-item label="操作描述" :span="2">
            {{ getOperationDescription(selectedOperation) }}
          </a-descriptions-item>
        </a-descriptions>

        <!-- 变更详情 -->
        <div v-if="selectedOperation.changes" class="change-detail">
          <h4>变更详情</h4>
          <a-table
            :columns="changeColumns"
            :data-source="formatChangesForTable(selectedOperation.changes)"
            :pagination="false"
            size="small"
          />
        </div>

        <!-- 冲突详情 -->
        <div v-if="selectedOperation.conflictInfo" class="conflict-detail">
          <h4>冲突详情</h4>
          <a-alert
            :message="selectedOperation.conflictInfo.message"
            :type="selectedOperation.conflictInfo.resolved ? 'success' : 'warning'"
            show-icon
          />
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue';
import { format } from 'date-fns';
import { zhCN } from 'date-fns/locale';

// 操作类型定义
interface OperationUser {
  id: string;
  name: string;
  avatar?: string;
  color: string;
}

interface OperationChanges {
  oldValue?: any;
  newValue?: any;
  fieldPath?: string;
  changeType: 'create' | 'update' | 'delete';
}

interface ConflictInfo {
  message: string;
  resolved: boolean;
  resolution?: string;
  conflictedWith?: string[];
}

interface TimelineOperation {
  id: string;
  type: 'field_update' | 'field_create' | 'field_delete' | 'user_join' | 'user_leave' | 'conflict_resolve' | 'rollback' | 'sync' | 'export' | 'import';
  timestamp: number;
  user?: OperationUser;
  fieldName?: string;
  description: string;
  changes?: OperationChanges;
  hasConflict: boolean;
  conflictInfo?: ConflictInfo;
  impactLevel: 'high' | 'medium' | 'low';
  metadata?: Record<string, any>;
}

const props = defineProps<{
  operations: TimelineOperation[];
  autoRefresh?: boolean;
  maxDisplayItems?: number;
  enableExport?: boolean;
  enableRollback?: boolean;
}>();

const emit = defineEmits<{
  operationSelected: [operation: TimelineOperation];
  rollbackRequested: [operation: TimelineOperation];
  exportRequested: [operations: TimelineOperation[]];
  refreshRequested: [];
}>();

// 状态管理
const isLiveMode = ref(props.autoRefresh ?? true);
const viewMode = ref<'list' | 'chart' | 'group'>('list');
const showAdvancedFilters = ref(false);
const showDetailModal = ref(false);
const selectedOperation = ref<TimelineOperation | null>(null);
const exportLoading = ref(false);
const loadingMore = ref(false);
const currentPage = ref(1);
const pageSize = 20;

// 筛选状态
const filters = ref({
  type: [] as string[],
  users: [] as string[],
  timeRange: null as [any, any] | null,
  fieldNames: [] as string[],
  impactLevel: undefined as string | undefined,
  hasConflict: false,
  keyword: ''
});

const expandedGroups = ref<string[]>(['field_operations', 'user_operations']);

// 操作类型配置
const operationTypes = [
  { value: 'field_update', label: '字段更新', icon: '✏️' },
  { value: 'field_create', label: '字段创建', icon: '➕' },
  { value: 'field_delete', label: '字段删除', icon: '🗑️' },
  { value: 'user_join', label: '用户加入', icon: '👋' },
  { value: 'user_leave', label: '用户离开', icon: '👋' },
  { value: 'conflict_resolve', label: '冲突解决', icon: '⚡' },
  { value: 'rollback', label: '回滚操作', icon: '↩️' },
  { value: 'sync', label: '同步操作', icon: '🔄' },
  { value: 'export', label: '导出操作', icon: '📤' },
  { value: 'import', label: '导入操作', icon: '📥' }
];

// 变更详情表格列
const changeColumns = [
  { title: '属性', dataIndex: 'property', key: 'property' },
  { title: '旧值', dataIndex: 'oldValue', key: 'oldValue' },
  { title: '新值', dataIndex: 'newValue', key: 'newValue' }
];

// 计算属性
const uniqueUsers = computed(() => {
  const userMap = new Map();
  props.operations.forEach(op => {
    if (op.user && !userMap.has(op.user.id)) {
      userMap.set(op.user.id, op.user);
    }
  });
  return Array.from(userMap.values());
});

const uniqueFieldNames = computed(() => {
  return [...new Set(props.operations.map(op => op.fieldName).filter(Boolean))];
});

const timeRange = computed(() => {
  if (props.operations.length === 0) return null;

  const timestamps = props.operations.map(op => op.timestamp);
  const start = Math.min(...timestamps);
  const end = Math.max(...timestamps);

  return {
    start: format(new Date(start), 'MM-dd HH:mm', { locale: zhCN }),
    end: format(new Date(end), 'MM-dd HH:mm', { locale: zhCN }),
    duration: end - start
  };
});

const hasActiveFilters = computed(() => {
  return filters.value.type.length > 0 ||
         filters.value.users.length > 0 ||
         filters.value.timeRange ||
         filters.value.fieldNames.length > 0 ||
         filters.value.impactLevel ||
         filters.value.hasConflict ||
         filters.value.keyword;
});

const filteredOperations = computed(() => {
  let result = [...props.operations];

  // 类型筛选
  if (filters.value.type.length > 0) {
    result = result.filter(op => filters.value.type.includes(op.type));
  }

  // 用户筛选
  if (filters.value.users.length > 0) {
    result = result.filter(op => op.user && filters.value.users.includes(op.user.id));
  }

  // 时间范围筛选
  if (filters.value.timeRange && filters.value.timeRange[0] && filters.value.timeRange[1]) {
    const [start, end] = filters.value.timeRange;
    result = result.filter(op =>
      op.timestamp >= start.valueOf() && op.timestamp <= end.valueOf()
    );
  }

  // 字段名筛选
  if (filters.value.fieldNames.length > 0) {
    result = result.filter(op =>
      op.fieldName && filters.value.fieldNames.includes(op.fieldName)
    );
  }

  // 影响等级筛选
  if (filters.value.impactLevel) {
    result = result.filter(op => op.impactLevel === filters.value.impactLevel);
  }

  // 冲突筛选
  if (filters.value.hasConflict) {
    result = result.filter(op => op.hasConflict);
  }

  // 关键字搜索
  if (filters.value.keyword) {
    const keyword = filters.value.keyword.toLowerCase();
    result = result.filter(op =>
      op.description.toLowerCase().includes(keyword) ||
      op.user?.name.toLowerCase().includes(keyword) ||
      op.fieldName?.toLowerCase().includes(keyword)
    );
  }

  // 按时间倒序排序
  return result.sort((a, b) => b.timestamp - a.timestamp);
});

const paginatedOperations = computed(() => {
  const maxItems = props.maxDisplayItems || (currentPage.value * pageSize);
  return filteredOperations.value.slice(0, maxItems);
});

const hasMoreOperations = computed(() => {
  const maxItems = props.maxDisplayItems || (currentPage.value * pageSize);
  return filteredOperations.value.length > maxItems;
});

const groupedOperations = computed(() => {
  const groups = [
    {
      key: 'field_operations',
      name: '字段操作',
      icon: '📝',
      operations: filteredOperations.value.filter(op =>
        ['field_update', 'field_create', 'field_delete'].includes(op.type)
      )
    },
    {
      key: 'user_operations',
      name: '用户操作',
      icon: '👥',
      operations: filteredOperations.value.filter(op =>
        ['user_join', 'user_leave'].includes(op.type)
      )
    },
    {
      key: 'system_operations',
      name: '系统操作',
      icon: '⚙️',
      operations: filteredOperations.value.filter(op =>
        ['sync', 'rollback', 'export', 'import'].includes(op.type)
      )
    },
    {
      key: 'conflict_operations',
      name: '冲突处理',
      icon: '⚠️',
      operations: filteredOperations.value.filter(op =>
        op.type === 'conflict_resolve' || op.hasConflict
      )
    }
  ].filter(group => group.operations.length > 0);

  return groups;
});

// 方法
const getAvatarText = (name: string): string => {
  if (!name) return '?';
  const cleanName = name.trim();
  if (/[\u4e00-\u9fa5]/.test(cleanName)) {
    return cleanName.charAt(cleanName.length - 1);
  } else {
    return cleanName.charAt(0).toUpperCase();
  }
};

const getOperationIcon = (operation: TimelineOperation): string => {
  const typeConfig = operationTypes.find(t => t.value === operation.type);
  return typeConfig?.icon || '❓';
};

const getOperationColor = (operation: TimelineOperation): string => {
  const colors = {
    field_update: '#1890ff',
    field_create: '#52c41a',
    field_delete: '#ff4d4f',
    user_join: '#722ed1',
    user_leave: '#fa8c16',
    conflict_resolve: '#faad14',
    rollback: '#eb2f96',
    sync: '#13c2c2',
    export: '#2f54eb',
    import: '#a0d911'
  };
  return colors[operation.type as keyof typeof colors] || '#666';
};

const getOperationTypeName = (type: string): string => {
  const typeConfig = operationTypes.find(t => t.value === type);
  return typeConfig?.label || type;
};

const getOperationDescription = (operation: TimelineOperation): string => {
  return operation.description || '无描述';
};

const getImpactLevelText = (level: string): string => {
  const levels = {
    high: '🔴 高影响',
    medium: '🟡 中影响',
    low: '🟢 低影响'
  };
  return levels[level as keyof typeof levels] || level;
};

const getFieldDisplayName = (fieldName: string): string => {
  // 这里可以添加字段名映射逻辑
  return fieldName || '未知字段';
};

const formatTime = (timestamp: number): string => {
  return format(new Date(timestamp), 'HH:mm:ss', { locale: zhCN });
};

const formatDetailTime = (timestamp: number): string => {
  return format(new Date(timestamp), 'yyyy-MM-dd HH:mm:ss', { locale: zhCN });
};

const formatDuration = (duration: number): string => {
  const minutes = Math.floor(duration / 60000);
  const seconds = Math.floor((duration % 60000) / 1000);
  return `${minutes}分${seconds}秒`;
};

const formatValue = (value: any): string => {
  if (value === null || value === undefined) return '空';
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
};

const formatChangesForTable = (changes: OperationChanges) => {
  return [
    {
      property: '字段值',
      oldValue: formatValue(changes.oldValue),
      newValue: formatValue(changes.newValue)
    }
  ];
};

const selectOperation = (operation: TimelineOperation) => {
  selectedOperation.value = operation;
  emit('operationSelected', operation);
};

const showOperationDetail = (operation: TimelineOperation) => {
  selectedOperation.value = operation;
  showDetailModal.value = true;
};

const canRollback = (operation: TimelineOperation): boolean => {
  return props.enableRollback && ['field_update', 'field_delete'].includes(operation.type);
};

const rollbackToOperation = (operation: TimelineOperation) => {
  emit('rollbackRequested', operation);
};

const toggleLiveMode = (checked: boolean) => {
  isLiveMode.value = checked;
  if (checked) {
    startAutoRefresh();
  } else {
    stopAutoRefresh();
  }
};

const toggleAdvancedFilters = () => {
  showAdvancedFilters.value = !showAdvancedFilters.value;
};

const clearFilters = () => {
  filters.value = {
    type: [],
    users: [],
    timeRange: null,
    fieldNames: [],
    impactLevel: undefined,
    hasConflict: false,
    keyword: ''
  };
};

const applyFilters = () => {
  currentPage.value = 1;
};

const loadMoreOperations = () => {
  loadingMore.value = true;
  currentPage.value++;
  setTimeout(() => {
    loadingMore.value = false;
  }, 500);
};

const toggleGroupExpanded = (groupKey: string) => {
  const index = expandedGroups.value.indexOf(groupKey);
  if (index > -1) {
    expandedGroups.value.splice(index, 1);
  } else {
    expandedGroups.value.push(groupKey);
  }
};

const exportOperations = async () => {
  exportLoading.value = true;
  try {
    emit('exportRequested', filteredOperations.value);
  } finally {
    exportLoading.value = false;
  }
};

const refreshTimeline = () => {
  emit('refreshRequested');
};

// 自动刷新
let refreshInterval: NodeJS.Timeout | null = null;

const startAutoRefresh = () => {
  if (refreshInterval) return;
  refreshInterval = setInterval(() => {
    refreshTimeline();
  }, 30000); // 30秒刷新一次
};

const stopAutoRefresh = () => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
    refreshInterval = null;
  }
};

onMounted(() => {
  if (isLiveMode.value) {
    startAutoRefresh();
  }
});

onUnmounted(() => {
  stopAutoRefresh();
});

// 监听筛选条件变化
watch(filters, () => {
  currentPage.value = 1;
}, { deep: true });

// 暴露组件方法
defineExpose({
  refreshTimeline,
  exportOperations,
  clearFilters,
  selectOperation
});
</script>

<style scoped>
.enhanced-operation-timeline {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.timeline-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.timeline-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.timeline-stats {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-value {
  font-weight: 600;
  color: #1890ff;
}

.stat-label {
  font-size: 12px;
  color: #666;
}

.filter-panel {
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
  transition: all 0.3s ease;
}

.basic-filters {
  padding: 12px 16px;
}

.advanced-filters {
  padding: 0 16px 12px 16px;
  border-top: 1px solid #f0f0f0;
}

.user-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  font-weight: 600;
}

.timeline-content {
  flex: 1;
  overflow: auto;
  padding: 16px;
}

.timeline-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.timeline-item:hover {
  background: #f5f5f5;
  border-color: #d9d9d9;
}

.timeline-item-selected {
  background: #e6f7ff;
  border-color: #1890ff;
}

.timeline-item-conflict {
  border-left: 3px solid #faad14;
}

.timeline-item-important {
  border-left: 3px solid #ff4d4f;
}

.timeline-line {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}

.timeline-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.timeline-connector {
  width: 2px;
  height: 20px;
  background: #d9d9d9;
  margin: 4px 0;
}

.timeline-content-item {
  flex: 1;
  min-width: 0;
}

.operation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.operation-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.operation-type-badge {
  font-weight: 600;
  font-size: 13px;
}

.operation-time {
  font-size: 12px;
  color: #666;
}

.conflict-badge {
  background: #faad14;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
}

.impact-badge {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
}

.impact-high { background: #ff4d4f; color: white; }
.impact-medium { background: #faad14; color: white; }
.impact-low { background: #52c41a; color: white; }

.operation-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.operation-description {
  font-size: 14px;
  color: #1a1a1a;
}

.operation-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-name {
  font-size: 12px;
  color: #666;
}

.field-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.field-label {
  color: #666;
}

.field-name {
  color: #1890ff;
  font-weight: 500;
}

.change-details {
  display: flex;
  align-items: center;
  gap: 8px;
}

.load-more {
  margin-top: 16px;
}

.operation-group {
  margin-bottom: 16px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  overflow: hidden;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
  border-bottom: 1px solid #d9d9d9;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.group-count {
  color: #666;
  font-weight: normal;
}

.group-content {
  background: #fff;
}

.group-operation-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.group-operation-item:hover {
  background: #f5f5f5;
}

.operation-detail {
  max-height: 500px;
  overflow-y: auto;
}

.change-detail,
.conflict-detail {
  margin-top: 16px;
}

.chart-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: auto auto;
  gap: 16px;
}

.chart-item {
  padding: 16px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
}

.chart-item h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
}

.frequency-chart,
.user-activity-chart,
.operation-pie-chart {
  height: 200px;
  background: #f5f5f5;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
}

/* 滚动条样式 */
.timeline-content::-webkit-scrollbar {
  width: 6px;
}

.timeline-content::-webkit-scrollbar-track {
  background: #f0f0f0;
}

.timeline-content::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 3px;
}

.timeline-content::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>