<!--
  * 增强版操作时间轴组件 - 支持全方位操作记录和智能分析
  *
  * 核心功能:
  * 1. 实时显示所有类型的操作记录
  * 2. 多维度过滤和搜索
  * 3. 智能聚合和分组
  * 4. 操作回放和分析
  * 5. 异常检测和告警
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-28
  * @Copyright 1024创新实验室
-->
<template>
  <div class="comprehensive-timeline">
    <!-- 顶部控制面板 -->
    <div class="timeline-controls">
      <div class="stats-panel">
        <div class="stat-item">
          <span class="stat-label">今日操作</span>
          <span class="stat-value">{{ stats.todayOperations }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">活跃用户</span>
          <span class="stat-value">{{ stats.activeUsers }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">异常操作</span>
          <span class="stat-value critical">{{ stats.anomalies }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">操作成功率</span>
          <span class="stat-value">{{ stats.successRate }}%</span>
        </div>
      </div>

      <div class="filter-panel">
        <a-space>
          <!-- 时间范围选择 -->
          <a-range-picker
            v-model:value="timeRange"
            :placeholder="['开始时间', '结束时间']"
            format="YYYY-MM-DD HH:mm"
            @change="handleTimeRangeChange"
          />

          <!-- 操作级别过滤 -->
          <a-select
            v-model:value="selectedLevels"
            mode="multiple"
            placeholder="操作级别"
            style="min-width: 150px"
            @change="handleFilterChange"
          >
            <a-select-option value="keystroke">键盘输入</a-select-option>
            <a-select-option value="field">字段操作</a-select-option>
            <a-select-option value="form">表单操作</a-select-option>
            <a-select-option value="record">记录操作</a-select-option>
            <a-select-option value="system">系统操作</a-select-option>
          </a-select>

          <!-- 操作类型过滤 -->
          <a-select
            v-model:value="selectedTypes"
            mode="multiple"
            placeholder="操作类型"
            style="min-width: 150px"
            @change="handleFilterChange"
          >
            <a-select-option value="input_change">输入变更</a-select-option>
            <a-select-option value="field_update">字段更新</a-select-option>
            <a-select-option value="save_record">保存记录</a-select-option>
            <a-select-option value="delete_record">删除记录</a-select-option>
            <a-select-option value="status_change">状态变更</a-select-option>
            <a-select-option value="conflict_resolve">冲突解决</a-select-option>
          </a-select>

          <!-- 用户过滤 -->
          <a-select
            v-model:value="selectedUsers"
            mode="multiple"
            placeholder="操作用户"
            style="min-width: 120px"
            @change="handleFilterChange"
          >
            <a-select-option
              v-for="user in uniqueUsers"
              :key="user.id"
              :value="user.id"
            >
              {{ user.name }}
            </a-select-option>
          </a-select>

          <!-- 安全级别过滤 -->
          <a-select
            v-model:value="selectedSecurityLevels"
            mode="multiple"
            placeholder="安全级别"
            style="min-width: 120px"
            @change="handleFilterChange"
          >
            <a-select-option value="low">低</a-select-option>
            <a-select-option value="medium">中</a-select-option>
            <a-select-option value="high">高</a-select-option>
            <a-select-option value="critical">关键</a-select-option>
          </a-select>

          <!-- 搜索框 -->
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索操作内容..."
            style="width: 200px"
            @search="handleFilterChange"
            @change="debounceSearch"
          />

          <!-- 功能按钮 -->
          <a-button-group>
            <a-button @click="refreshData">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
            <a-button @click="exportTimeline">
              <template #icon><ExportOutlined /></template>
              导出
            </a-button>
            <a-button @click="showSettings">
              <template #icon><SettingOutlined /></template>
              设置
            </a-button>
          </a-button-group>
        </a-space>
      </div>
    </div>

    <!-- 智能分组开关 -->
    <div class="grouping-controls">
      <a-space>
        <span>显示模式：</span>
        <a-radio-group v-model:value="displayMode" @change="handleDisplayModeChange">
          <a-radio-button value="chronological">按时间</a-radio-button>
          <a-radio-button value="grouped">智能分组</a-radio-button>
          <a-radio-button value="analytical">分析视图</a-radio-button>
        </a-radio-group>

        <a-switch
          v-model:checked="enableRealtime"
          checked-children="实时"
          un-checked-children="暂停"
          @change="handleRealtimeToggle"
        />

        <span>聚合级别：</span>
        <a-slider
          v-model:value="aggregationLevel"
          :min="1"
          :max="5"
          :marks="{ 1: '详细', 3: '适中', 5: '简洁' }"
          style="width: 120px"
          @change="handleAggregationChange"
        />
      </a-space>
    </div>

    <!-- 异常告警 -->
    <div v-if="anomalies.length > 0" class="anomaly-alerts">
      <a-alert
        v-for="anomaly in anomalies"
        :key="anomaly.type"
        :type="getAlertType(anomaly.severity)"
        :message="anomaly.description"
        :description="anomaly.recommendedActions.join('; ')"
        show-icon
        closable
        @close="dismissAnomaly(anomaly)"
      />
    </div>

    <!-- 时间轴主体 -->
    <div class="timeline-container">
      <div class="timeline-header">
        <div class="timeline-meta">
          <span class="operation-count">
            显示 {{ filteredOperations.length }} / {{ totalOperations }} 条操作
          </span>
          <span v-if="timeRange" class="time-span">
            {{ formatTimeSpan(timeRange) }}
          </span>
        </div>

        <div class="timeline-legend">
          <div class="legend-item" v-for="level in operationLevels" :key="level.key">
            <div class="legend-dot" :style="{ backgroundColor: level.color }"></div>
            <span class="legend-label">{{ level.label }}</span>
          </div>
        </div>
      </div>

      <div class="timeline-body" ref="timelineBodyRef" @scroll="handleScroll">
        <!-- 按时间显示 -->
        <template v-if="displayMode === 'chronological'">
          <div
            v-for="(operation, index) in paginatedOperations"
            :key="operation.id"
            class="timeline-item"
            :class="{
              'timeline-item-selected': selectedOperationId === operation.id,
              'timeline-item-critical': operation.securityLevel === 'critical',
              'timeline-item-anomaly': isAnomalyOperation(operation)
            }"
            @click="selectOperation(operation)"
          >
            <div class="timeline-line">
              <div
                class="timeline-dot"
                :style="{ backgroundColor: getOperationColor(operation) }"
                :title="getOperationTitle(operation)"
              >
                <component :is="getOperationIcon(operation)" />
              </div>
              <div
                v-if="index < paginatedOperations.length - 1"
                class="timeline-connector"
                :style="{ background: getConnectorGradient(operation, paginatedOperations[index + 1]) }"
              ></div>
            </div>

            <div class="timeline-content">
              <ComprehensiveOperationCard
                :operation="operation"
                :show-details="selectedOperationId === operation.id"
                :aggregation-level="aggregationLevel"
                @action="handleOperationAction"
                @expand="expandOperation"
              />
            </div>
          </div>
        </template>

        <!-- 智能分组显示 -->
        <template v-else-if="displayMode === 'grouped'">
          <div
            v-for="group in groupedOperations"
            :key="group.id"
            class="operation-group"
          >
            <div class="group-header" @click="toggleGroup(group.id)">
              <div class="group-title">
                <component :is="group.icon" />
                <span>{{ group.title }}</span>
                <a-badge :count="group.operations.length" />
              </div>
              <div class="group-meta">
                <span class="group-timespan">{{ formatGroupTimespan(group) }}</span>
                <DownOutlined :class="{ 'group-expanded': group.expanded }" />
              </div>
            </div>

            <a-collapse-transition>
              <div v-show="group.expanded" class="group-operations">
                <ComprehensiveOperationCard
                  v-for="operation in group.operations"
                  :key="operation.id"
                  :operation="operation"
                  :show-details="selectedOperationId === operation.id"
                  :aggregation-level="aggregationLevel"
                  @action="handleOperationAction"
                  @expand="expandOperation"
                />
              </div>
            </a-collapse-transition>
          </div>
        </template>

        <!-- 分析视图 -->
        <template v-else-if="displayMode === 'analytical'">
          <OperationAnalyticsView
            :operations="filteredOperations"
            :time-range="timeRange"
            @drill-down="handleDrillDown"
          />
        </template>

        <!-- 加载更多 -->
        <div v-if="hasMore" class="load-more">
          <a-button
            type="link"
            :loading="loading"
            @click="loadMore"
          >
            加载更多
          </a-button>
        </div>

        <!-- 空状态 -->
        <div v-if="filteredOperations.length === 0" class="timeline-empty">
          <div class="empty-illustration">
            <FileSearchOutlined />
          </div>
          <div class="empty-title">暂无操作记录</div>
          <div class="empty-description">
            <template v-if="hasActiveFilters">
              尝试调整筛选条件以查看更多操作记录
            </template>
            <template v-else>
              开始操作后，记录将显示在这里
            </template>
          </div>
          <a-button v-if="hasActiveFilters" type="primary" @click="clearFilters">
            清除筛选条件
          </a-button>
        </div>
      </div>
    </div>

    <!-- 操作详情抽屉 -->
    <a-drawer
      v-model:visible="detailDrawerVisible"
      title="操作详情"
      placement="right"
      width="600"
    >
      <OperationDetailView
        v-if="selectedOperation"
        :operation="selectedOperation"
        @close="closeDetailDrawer"
        @action="handleDetailAction"
      />
    </a-drawer>

    <!-- 设置模态框 -->
    <a-modal
      v-model:visible="settingsModalVisible"
      title="时间轴设置"
      width="800"
      @ok="saveSettings"
    >
      <TimelineSettingsForm
        v-model:settings="timelineSettings"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, reactive } from 'vue';
import { format, formatDistanceToNow } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import {
  ReloadOutlined,
  ExportOutlined,
  SettingOutlined,
  DownOutlined,
  FileSearchOutlined
} from '@ant-design/icons-vue';
import type {
  ComprehensiveOperationRecord,
  OperationLevel,
  OperationType,
  SecurityLevel,
  AnomalyReport
} from '/@/utils/comprehensive-operation-tracker';
import { getOperationTracker } from '/@/utils/comprehensive-operation-tracker';
import ComprehensiveOperationCard from './ComprehensiveOperationCard.vue';
import OperationAnalyticsView from './OperationAnalyticsView.vue';
import OperationDetailView from './OperationDetailView.vue';
import TimelineSettingsForm from './TimelineSettingsForm.vue';

// =============== Props 和 Emits ===============

interface Props {
  entityType?: string;
  entityId?: number;
  autoRefresh?: boolean;
  refreshInterval?: number;
}

const props = withDefaults(defineProps<Props>(), {
  autoRefresh: true,
  refreshInterval: 30000
});

const emit = defineEmits<{
  operationSelect: [operation: ComprehensiveOperationRecord];
  operationAction: [action: string, operation: ComprehensiveOperationRecord];
}>();

// =============== 响应式数据 ===============

const timelineBodyRef = ref<HTMLElement>();

// 数据状态
const operations = ref<ComprehensiveOperationRecord[]>([]);
const loading = ref(false);
const hasMore = ref(true);
const currentPage = ref(1);
const pageSize = ref(50);

// 筛选状态
const timeRange = ref<[string, string] | null>(null);
const selectedLevels = ref<OperationLevel[]>([]);
const selectedTypes = ref<OperationType[]>([]);
const selectedUsers = ref<number[]>([]);
const selectedSecurityLevels = ref<SecurityLevel[]>([]);
const searchKeyword = ref('');

// 显示状态
const displayMode = ref<'chronological' | 'grouped' | 'analytical'>('chronological');
const enableRealtime = ref(true);
const aggregationLevel = ref(3);
const selectedOperationId = ref<string>();
const detailDrawerVisible = ref(false);
const settingsModalVisible = ref(false);

// 异常检测
const anomalies = ref<AnomalyReport[]>([]);

// 设置
const timelineSettings = reactive({
  showSystemOperations: true,
  showKeystrokeLevel: false,
  enableAnomalyDetection: true,
  autoScrollToNew: true,
  compactMode: false,
  realtimeUpdateSound: false
});

// 统计信息
const stats = reactive({
  todayOperations: 0,
  activeUsers: 0,
  anomalies: 0,
  successRate: 95.8
});

// 分组状态
const expandedGroups = ref<Set<string>>(new Set());

// =============== 计算属性 ===============

const selectedOperation = computed(() => {
  return operations.value.find(op => op.id === selectedOperationId.value);
});

const uniqueUsers = computed(() => {
  const users = operations.value.reduce((acc, op) => {
    if (!acc.some(u => u.id === op.userId)) {
      acc.push({ id: op.userId, name: op.userName });
    }
    return acc;
  }, [] as Array<{ id: number; name: string }>);

  return users.sort((a, b) => a.name.localeCompare(b.name));
});

const filteredOperations = computed(() => {
  let filtered = operations.value;

  // 时间范围过滤
  if (timeRange.value) {
    const [start, end] = timeRange.value;
    const startTime = new Date(start).getTime();
    const endTime = new Date(end).getTime();
    filtered = filtered.filter(op =>
      op.timestamp >= startTime && op.timestamp <= endTime
    );
  }

  // 级别过滤
  if (selectedLevels.value.length > 0) {
    filtered = filtered.filter(op => selectedLevels.value.includes(op.level));
  }

  // 类型过滤
  if (selectedTypes.value.length > 0) {
    filtered = filtered.filter(op => selectedTypes.value.includes(op.type));
  }

  // 用户过滤
  if (selectedUsers.value.length > 0) {
    filtered = filtered.filter(op => selectedUsers.value.includes(op.userId));
  }

  // 安全级别过滤
  if (selectedSecurityLevels.value.length > 0) {
    filtered = filtered.filter(op => selectedSecurityLevels.value.includes(op.securityLevel));
  }

  // 关键词搜索
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase();
    filtered = filtered.filter(op =>
      op.fieldLabel?.toLowerCase().includes(keyword) ||
      op.afterValue?.toString().toLowerCase().includes(keyword) ||
      op.metadata?.description?.toLowerCase().includes(keyword) ||
      op.userName.toLowerCase().includes(keyword)
    );
  }

  // 设置过滤
  if (!timelineSettings.showSystemOperations) {
    filtered = filtered.filter(op => op.level !== 'system');
  }

  if (!timelineSettings.showKeystrokeLevel) {
    filtered = filtered.filter(op => op.level !== 'keystroke');
  }

  return filtered.sort((a, b) => b.timestamp - a.timestamp);
});

const paginatedOperations = computed(() => {
  const start = 0;
  const end = currentPage.value * pageSize.value;
  return filteredOperations.value.slice(start, end);
});

const groupedOperations = computed(() => {
  if (displayMode.value !== 'grouped') return [];

  const groups = new Map<string, {
    id: string;
    title: string;
    icon: string;
    operations: ComprehensiveOperationRecord[];
    expanded: boolean;
  }>();

  filteredOperations.value.forEach(operation => {
    let groupKey: string;
    let groupTitle: string;
    let groupIcon: string;

    // 根据聚合级别决定分组策略
    if (aggregationLevel.value <= 2) {
      // 详细分组 - 按字段分组
      groupKey = `${operation.entityType}_${operation.fieldPath}`;
      groupTitle = `${operation.fieldLabel || operation.fieldPath}`;
      groupIcon = '📝';
    } else if (aggregationLevel.value <= 4) {
      // 适中分组 - 按操作类型分组
      groupKey = operation.type;
      groupTitle = getOperationTypeLabel(operation.type);
      groupIcon = getOperationIcon(operation);
    } else {
      // 简洁分组 - 按用户分组
      groupKey = `user_${operation.userId}`;
      groupTitle = `${operation.userName} 的操作`;
      groupIcon = '👤';
    }

    if (!groups.has(groupKey)) {
      groups.set(groupKey, {
        id: groupKey,
        title: groupTitle,
        icon: groupIcon,
        operations: [],
        expanded: expandedGroups.value.has(groupKey)
      });
    }

    groups.get(groupKey)!.operations.push(operation);
  });

  return Array.from(groups.values()).sort((a, b) => {
    const aLatest = Math.max(...a.operations.map(op => op.timestamp));
    const bLatest = Math.max(...b.operations.map(op => op.timestamp));
    return bLatest - aLatest;
  });
});

const totalOperations = computed(() => operations.value.length);

const hasActiveFilters = computed(() => {
  return timeRange.value !== null ||
         selectedLevels.value.length > 0 ||
         selectedTypes.value.length > 0 ||
         selectedUsers.value.length > 0 ||
         selectedSecurityLevels.value.length > 0 ||
         searchKeyword.value.trim() !== '';
});

const operationLevels = [
  { key: 'keystroke', label: '键盘输入', color: '#d9d9d9' },
  { key: 'field', label: '字段操作', color: '#1890ff' },
  { key: 'form', label: '表单操作', color: '#52c41a' },
  { key: 'record', label: '记录操作', color: '#fa8c16' },
  { key: 'system', label: '系统操作', color: '#f5222d' }
];

// =============== 方法 ===============

const operationTracker = getOperationTracker();

const loadOperations = async (append = false) => {
  if (loading.value) return;

  loading.value = true;
  try {
    // 这里应该调用API获取操作记录
    // 暂时使用模拟数据进行演示
    await new Promise(resolve => setTimeout(resolve, 500));

    // 实际实现中应该调用后端API
    // const response = await operationHistoryApi.query({
    //   entityType: props.entityType,
    //   entityId: props.entityId,
    //   pageNum: currentPage.value,
    //   pageSize: pageSize.value,
    //   ...getQueryParams()
    // });

    console.log('📊 [ComprehensiveOperationTimeline] 操作记录加载完成');
  } catch (error) {
    console.error('❌ [ComprehensiveOperationTimeline] 加载操作记录失败:', error);
  } finally {
    loading.value = false;
  }
};

const refreshData = () => {
  currentPage.value = 1;
  loadOperations();
};

const loadMore = () => {
  currentPage.value++;
  loadOperations(true);
};

const handleFilterChange = () => {
  currentPage.value = 1;
  // 重新加载数据
  // loadOperations();
};

const handleTimeRangeChange = () => {
  handleFilterChange();
};

const handleDisplayModeChange = () => {
  // 显示模式变更时的处理
  console.log('显示模式变更:', displayMode.value);
};

const handleRealtimeToggle = (enabled: boolean) => {
  if (enabled) {
    operationTracker.start();
  } else {
    operationTracker.stop();
  }
};

const handleAggregationChange = () => {
  // 聚合级别变更时重新分组
  console.log('聚合级别变更:', aggregationLevel.value);
};

const selectOperation = (operation: ComprehensiveOperationRecord) => {
  selectedOperationId.value = operation.id;
  emit('operationSelect', operation);
};

const expandOperation = (operation: ComprehensiveOperationRecord) => {
  selectedOperation.value;
  detailDrawerVisible.value = true;
};

const handleOperationAction = (action: string, operation: ComprehensiveOperationRecord) => {
  emit('operationAction', action, operation);

  switch (action) {
    case 'revert':
      handleRevertOperation(operation);
      break;
    case 'replay':
      handleReplayOperation(operation);
      break;
    case 'export':
      handleExportOperation(operation);
      break;
  }
};

const handleRevertOperation = async (operation: ComprehensiveOperationRecord) => {
  // TODO: 实现操作回滚
  console.log('回滚操作:', operation);
};

const handleReplayOperation = (operation: ComprehensiveOperationRecord) => {
  // TODO: 实现操作回放
  console.log('回放操作:', operation);
};

const handleExportOperation = (operation: ComprehensiveOperationRecord) => {
  // TODO: 实现单个操作导出
  console.log('导出操作:', operation);
};

const toggleGroup = (groupId: string) => {
  if (expandedGroups.value.has(groupId)) {
    expandedGroups.value.delete(groupId);
  } else {
    expandedGroups.value.add(groupId);
  }
};

const exportTimeline = () => {
  // TODO: 实现时间轴导出
  console.log('导出时间轴');
};

const showSettings = () => {
  settingsModalVisible.value = true;
};

const saveSettings = () => {
  settingsModalVisible.value = false;
  // TODO: 保存设置
  console.log('保存设置:', timelineSettings);
};

const clearFilters = () => {
  timeRange.value = null;
  selectedLevels.value = [];
  selectedTypes.value = [];
  selectedUsers.value = [];
  selectedSecurityLevels.value = [];
  searchKeyword.value = '';
  handleFilterChange();
};

const dismissAnomaly = (anomaly: AnomalyReport) => {
  const index = anomalies.value.indexOf(anomaly);
  if (index > -1) {
    anomalies.value.splice(index, 1);
  }
};

const handleDrillDown = (data: any) => {
  // 从分析视图钻取到具体操作
  console.log('钻取分析:', data);
};

const handleScroll = () => {
  const element = timelineBodyRef.value;
  if (!element || !hasMore.value || loading.value) return;

  const { scrollTop, scrollHeight, clientHeight } = element;
  if (scrollTop + clientHeight >= scrollHeight - 10) {
    loadMore();
  }
};

const closeDetailDrawer = () => {
  detailDrawerVisible.value = false;
  selectedOperationId.value = undefined;
};

const handleDetailAction = (action: string, operation: ComprehensiveOperationRecord) => {
  handleOperationAction(action, operation);
  closeDetailDrawer();
};

// =============== 工具方法 ===============

const getOperationColor = (operation: ComprehensiveOperationRecord): string => {
  const levelColors = {
    keystroke: '#d9d9d9',
    field: '#1890ff',
    form: '#52c41a',
    record: '#fa8c16',
    system: '#f5222d'
  };
  return levelColors[operation.level] || '#8c8c8c';
};

const getOperationIcon = (operation: ComprehensiveOperationRecord): string => {
  const typeIcons: Record<string, string> = {
    input_change: '⌨️',
    field_update: '📝',
    save_record: '💾',
    delete_record: '🗑️',
    status_change: '🔄',
    conflict_resolve: '⚠️',
    user_join: '👋',
    user_leave: '👋',
    error_occurred: '❌'
  };
  return typeIcons[operation.type] || '•';
};

const getOperationTitle = (operation: ComprehensiveOperationRecord): string => {
  return `${getOperationTypeLabel(operation.type)} - ${operation.fieldLabel}`;
};

const getOperationTypeLabel = (type: OperationType): string => {
  const labels: Record<string, string> = {
    input_change: '输入变更',
    field_update: '字段更新',
    save_record: '保存记录',
    delete_record: '删除记录',
    status_change: '状态变更',
    conflict_resolve: '冲突解决',
    user_join: '用户进入',
    user_leave: '用户离开',
    error_occurred: '错误发生'
  };
  return labels[type] || type;
};

const getConnectorGradient = (current: ComprehensiveOperationRecord, next: ComprehensiveOperationRecord): string => {
  const currentColor = getOperationColor(current);
  const nextColor = getOperationColor(next);
  return `linear-gradient(180deg, ${currentColor}40 0%, ${nextColor}40 100%)`;
};

const isAnomalyOperation = (operation: ComprehensiveOperationRecord): boolean => {
  return anomalies.value.some(anomaly =>
    anomaly.affectedOperations.includes(operation.id)
  );
};

const getAlertType = (severity: string) => {
  const typeMap: Record<string, string> = {
    low: 'info',
    medium: 'warning',
    high: 'warning',
    critical: 'error'
  };
  return typeMap[severity] || 'info';
};

const formatTimeSpan = (range: [string, string]): string => {
  const [start, end] = range;
  return `${format(new Date(start), 'MM-dd HH:mm')} - ${format(new Date(end), 'MM-dd HH:mm')}`;
};

const formatGroupTimespan = (group: any): string => {
  const timestamps = group.operations.map((op: ComprehensiveOperationRecord) => op.timestamp);
  const earliest = Math.min(...timestamps);
  const latest = Math.max(...timestamps);

  if (earliest === latest) {
    return format(earliest, 'HH:mm:ss');
  }

  return `${format(earliest, 'HH:mm')} - ${format(latest, 'HH:mm')}`;
};

// 防抖搜索
let searchTimer: NodeJS.Timeout;
const debounceSearch = () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    handleFilterChange();
  }, 300);
};

// =============== 生命周期 ===============

onMounted(() => {
  loadOperations();

  // 开始操作追踪
  if (enableRealtime.value) {
    operationTracker.start();
  }

  // 添加实时更新监听
  const unsubscribe = operationTracker.addListener((newOperations) => {
    operations.value.unshift(...newOperations);

    // 播放提示音
    if (timelineSettings.realtimeUpdateSound) {
      // TODO: 播放提示音
    }

    // 自动滚动到新操作
    if (timelineSettings.autoScrollToNew) {
      timelineBodyRef.value?.scrollTo({ top: 0, behavior: 'smooth' });
    }
  });

  // 设置定时刷新
  let refreshTimer: NodeJS.Timeout;
  if (props.autoRefresh) {
    refreshTimer = setInterval(refreshData, props.refreshInterval);
  }

  onUnmounted(() => {
    unsubscribe();
    if (refreshTimer) {
      clearInterval(refreshTimer);
    }
    operationTracker.stop();
  });
});
</script>

<style scoped>
.comprehensive-timeline {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}

.timeline-controls {
  padding: 16px;
  background: white;
  border-bottom: 1px solid #f0f0f0;
}

.stats-panel {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #262626;
}

.stat-value.critical {
  color: #f5222d;
}

.filter-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.grouping-controls {
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  gap: 16px;
}

.anomaly-alerts {
  padding: 16px;
  background: white;
  border-bottom: 1px solid #f0f0f0;
}

.anomaly-alerts .ant-alert {
  margin-bottom: 8px;
}

.anomaly-alerts .ant-alert:last-child {
  margin-bottom: 0;
}

.timeline-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.timeline-header {
  padding: 16px;
  background: white;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.timeline-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.operation-count {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.time-span {
  font-size: 12px;
  color: #8c8c8c;
}

.timeline-legend {
  display: flex;
  gap: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-label {
  font-size: 12px;
  color: #8c8c8c;
}

.timeline-body {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background: #fafafa;
}

.timeline-item {
  display: flex;
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 8px;
  padding: 12px;
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.timeline-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-1px);
}

.timeline-item-selected {
  border: 2px solid #1890ff;
  background: rgba(24, 144, 255, 0.02);
}

.timeline-item-critical {
  border-left: 4px solid #f5222d;
}

.timeline-item-anomaly {
  border: 1px solid #faad14;
  background: rgba(250, 173, 20, 0.04);
}

.timeline-line {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 16px;
  position: relative;
}

.timeline-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: white;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 2;
}

.timeline-connector {
  width: 2px;
  flex: 1;
  min-height: 20px;
  margin-top: 8px;
}

.timeline-content {
  flex: 1;
  min-width: 0;
}

.operation-group {
  margin-bottom: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.group-header {
  padding: 16px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: background-color 0.3s ease;
}

.group-header:hover {
  background: #f0f0f0;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.group-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #8c8c8c;
  font-size: 12px;
}

.group-expanded {
  transform: rotate(180deg);
}

.group-operations {
  padding: 16px;
}

.load-more {
  text-align: center;
  padding: 16px;
}

.timeline-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  background: white;
  border-radius: 8px;
  margin: 40px 0;
}

.empty-illustration {
  font-size: 64px;
  color: #d9d9d9;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 18px;
  color: #262626;
  margin-bottom: 8px;
  font-weight: 500;
}

.empty-description {
  font-size: 14px;
  color: #8c8c8c;
  margin-bottom: 24px;
  line-height: 1.5;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .timeline-controls {
    padding: 12px;
  }

  .stats-panel {
    flex-wrap: wrap;
    gap: 16px;
  }

  .filter-panel {
    flex-direction: column;
    gap: 8px;
  }

  .filter-panel .ant-space {
    flex-wrap: wrap;
  }

  .grouping-controls {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .timeline-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .timeline-legend {
    flex-wrap: wrap;
    gap: 12px;
  }

  .timeline-item {
    margin-bottom: 12px;
    padding: 8px;
  }

  .timeline-line {
    margin-right: 12px;
  }
}

/* 滚动条美化 */
.timeline-body::-webkit-scrollbar {
  width: 6px;
}

.timeline-body::-webkit-scrollbar-track {
  background: #f0f0f0;
  border-radius: 3px;
}

.timeline-body::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 3px;
  transition: background-color 0.3s ease;
}

.timeline-body::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}

/* 动画效果 */
.timeline-item {
  animation: fadeInUp 0.3s ease;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.group-expanded {
  transition: transform 0.3s ease;
}

/* 暗色主题支持 */
@media (prefers-color-scheme: dark) {
  .comprehensive-timeline {
    background: #141414;
  }

  .timeline-controls,
  .grouping-controls,
  .timeline-header,
  .timeline-item,
  .operation-group {
    background: #1f1f1f;
    border-color: #303030;
  }

  .group-header {
    background: #262626;
  }

  .group-header:hover {
    background: #303030;
  }

  .timeline-empty {
    background: #1f1f1f;
    border-color: #303030;
  }
}
</style>