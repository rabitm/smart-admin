<!--
  * 操作时间轴组件 - 显示协作编辑的操作历史
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-24
  * @Copyright 1024创新实验室
-->
<template>
  <div class="operation-timeline">
    <div class="timeline-container">
      <!-- 时间轴头部 -->
      <div class="timeline-header">
        <div class="timeline-stats">
          <span class="total-operations">{{ operations.length }} 个操作</span>
          <span class="time-range" v-if="timeRange">
            {{ timeRange.start }} - {{ timeRange.end }}
          </span>
        </div>

        <div class="timeline-actions">
          <a-select
            v-model:value="filterType"
            size="small"
            style="width: 120px"
            @change="handleFilterChange"
          >
            <a-select-option value="all">全部操作</a-select-option>
            <a-select-option value="field">字段变更</a-select-option>
            <a-select-option value="user">用户操作</a-select-option>
            <a-select-option value="conflict">冲突处理</a-select-option>
          </a-select>
        </div>
      </div>

      <!-- 时间轴主体 -->
      <div class="timeline-body" ref="timelineBodyRef">
        <div
          v-for="(operation, index) in filteredOperations"
          :key="operation.id"
          class="timeline-item"
          :class="{
            'timeline-item-selected': selectedOperationId === operation.id,
            'timeline-item-conflict': operation.type === 'conflict'
          }"
          @click="selectOperation(operation)"
        >
          <!-- 时间轴线条 -->
          <div class="timeline-line">
            <div
              class="timeline-dot"
              :style="{ backgroundColor: getOperationColor(operation) }"
            >
              <component :is="getOperationIcon(operation)" />
            </div>
            <div
              v-if="index < filteredOperations.length - 1"
              class="timeline-connector"
            ></div>
          </div>

          <!-- 操作内容 -->
          <div class="timeline-content">
            <!-- 操作头部 -->
            <div class="operation-header">
              <div class="user-info">
                <div class="user-avatar" :style="{ backgroundColor: operation.user.color }">
                  <img v-if="operation.user.avatar" :src="operation.user.avatar" :alt="operation.user.name" />
                  <span v-else class="avatar-text">{{ getAvatarText(operation.user.name) }}</span>
                </div>
                <span class="user-name">{{ operation.user.name }}</span>
              </div>

              <div class="operation-time">
                <a-tooltip :title="formatFullTime(operation.timestamp)">
                  {{ formatRelativeTime(operation.timestamp) }}
                </a-tooltip>
              </div>
            </div>

            <!-- 操作描述 -->
            <div class="operation-description">
              <span class="operation-action">{{ getOperationDescription(operation) }}</span>
            </div>

            <!-- 字段变更详情 -->
            <div v-if="operation.type === 'field_update'" class="field-change-detail">
              <div class="field-name">
                <span class="field-label">{{ operation.fieldLabel || operation.fieldName }}</span>
              </div>

              <div class="value-change">
                <div v-if="operation.oldValue" class="old-value">
                  <span class="value-label">修改前:</span>
                  <span class="value-content">{{ formatValue(operation.oldValue) }}</span>
                </div>
                <div class="new-value">
                  <span class="value-label">{{ operation.oldValue ? '修改后:' : '设置为:' }}</span>
                  <span class="value-content">{{ formatValue(operation.newValue) }}</span>
                </div>
              </div>
            </div>

            <!-- 冲突详情 -->
            <div v-if="operation.type === 'conflict'" class="conflict-detail">
              <div class="conflict-info">
                <span class="conflict-field">字段: {{ operation.fieldLabel }}</span>
                <span class="conflict-strategy">解决方案: {{ operation.resolutionStrategy }}</span>
              </div>
              <div class="conflict-values">
                <div class="conflict-value">
                  <span class="value-label">用户A:</span>
                  <span class="value-content">{{ formatValue(operation.valueA) }}</span>
                </div>
                <div class="conflict-value">
                  <span class="value-label">用户B:</span>
                  <span class="value-content">{{ formatValue(operation.valueB) }}</span>
                </div>
                <div class="conflict-result">
                  <span class="value-label">最终值:</span>
                  <span class="value-content">{{ formatValue(operation.finalValue) }}</span>
                </div>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="operation-actions" v-if="operation.type === 'field_update'">
              <a-button
                type="text"
                size="small"
                :disabled="!canRevert(operation)"
                @click.stop="revertOperation(operation)"
              >
                <template #icon>↶</template>
                回滚
              </a-button>
              <a-button
                type="text"
                size="small"
                @click.stop="showOperationDetail(operation)"
              >
                <template #icon>🔍</template>
                详情
              </a-button>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="filteredOperations.length === 0" class="timeline-empty">
          <div class="empty-icon">📝</div>
          <div class="empty-text">暂无操作历史</div>
          <div class="empty-tip">开始编辑表单字段后，操作记录将显示在这里</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { formatDistanceToNow, format } from 'date-fns';
import { zhCN } from 'date-fns/locale';

// 操作类型定义
interface OperationUser {
  id: number;
  name: string;
  avatar?: string;
  color: string;
}

interface BaseOperation {
  id: string;
  type: string;
  user: OperationUser;
  timestamp: number;
  reportId: number;
}

interface FieldUpdateOperation extends BaseOperation {
  type: 'field_update';
  fieldName: string;
  fieldLabel: string;
  oldValue: any;
  newValue: any;
}

interface ConflictOperation extends BaseOperation {
  type: 'conflict';
  fieldName: string;
  fieldLabel: string;
  valueA: any;
  valueB: any;
  finalValue: any;
  resolutionStrategy: string;
}

interface UserActionOperation extends BaseOperation {
  type: 'user_action';
  action: string;
  description: string;
}

type Operation = FieldUpdateOperation | ConflictOperation | UserActionOperation;

interface Props {
  reportId: number;
  operations: Operation[];
}

const props = defineProps<Props>();

// 响应式数据
const selectedOperationId = ref<string>();
const filterType = ref<string>('all');
const timelineBodyRef = ref<HTMLElement>();

// 过滤后的操作列表
const filteredOperations = computed(() => {
  let filtered = props.operations;

  switch (filterType.value) {
    case 'field':
      filtered = filtered.filter(op => op.type === 'field_update');
      break;
    case 'user':
      filtered = filtered.filter(op => op.type === 'user_action');
      break;
    case 'conflict':
      filtered = filtered.filter(op => op.type === 'conflict');
      break;
  }

  return filtered.sort((a, b) => b.timestamp - a.timestamp);
});

// 时间范围
const timeRange = computed(() => {
  if (props.operations.length === 0) return null;

  const timestamps = props.operations.map(op => op.timestamp);
  const start = Math.min(...timestamps);
  const end = Math.max(...timestamps);

  return {
    start: format(start, 'HH:mm'),
    end: format(end, 'HH:mm')
  };
});

// Emit 事件
const emit = defineEmits<{
  operationSelect: [operation: Operation];
  operationRevert: [operation: Operation];
}>();

// 获取用户头像文字
const getAvatarText = (name: string): string => {
  if (!name) return '?';

  if (/[\u4e00-\u9fa5]/.test(name)) {
    return name.slice(-1);
  }

  const words = name.split(' ');
  return words.map(word => word.charAt(0)).join('').substring(0, 2).toUpperCase();
};

// 获取操作颜色
const getOperationColor = (operation: Operation): string => {
  switch (operation.type) {
    case 'field_update':
      return '#1890ff';
    case 'conflict':
      return '#f5222d';
    case 'user_action':
      return '#52c41a';
    default:
      return '#8c8c8c';
  }
};

// 获取操作图标
const getOperationIcon = (operation: Operation): string => {
  switch (operation.type) {
    case 'field_update':
      return '📝';
    case 'conflict':
      return '⚠️';
    case 'user_action':
      return '👤';
    default:
      return '•';
  }
};

// 获取操作描述
const getOperationDescription = (operation: Operation): string => {
  switch (operation.type) {
    case 'field_update':
      return `修改了 ${operation.fieldLabel || operation.fieldName}`;
    case 'conflict':
      return `解决了字段冲突 - ${operation.fieldLabel}`;
    case 'user_action':
      return operation.description;
    default:
      return '未知操作';
  }
};

// 格式化相对时间
const formatRelativeTime = (timestamp: number): string => {
  return formatDistanceToNow(timestamp, {
    addSuffix: true,
    locale: zhCN
  });
};

// 格式化完整时间
const formatFullTime = (timestamp: number): string => {
  return format(timestamp, 'yyyy-MM-dd HH:mm:ss', { locale: zhCN });
};

// 格式化值
const formatValue = (value: any): string => {
  if (value === null || value === undefined) return '空';
  if (typeof value === 'boolean') return value ? '是' : '否';
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
};

// 选择操作
const selectOperation = (operation: Operation) => {
  selectedOperationId.value = operation.id;
  emit('operationSelect', operation);
};

// 处理过滤变更
const handleFilterChange = () => {
  selectedOperationId.value = undefined;
};

// 检查是否可以回滚
const canRevert = (operation: Operation): boolean => {
  if (operation.type !== 'field_update') return false;

  // 检查是否是最新的操作
  const fieldOperations = props.operations
    .filter(op => op.type === 'field_update' && (op as FieldUpdateOperation).fieldName === operation.fieldName)
    .sort((a, b) => b.timestamp - a.timestamp);

  return fieldOperations[0]?.id === operation.id;
};

// 回滚操作
const revertOperation = (operation: Operation) => {
  emit('operationRevert', operation);
};

// 显示操作详情
const showOperationDetail = (operation: Operation) => {
  console.log('显示操作详情:', operation);
  // TODO: 实现操作详情弹窗
};

// 监听操作变化，自动滚动到最新
watch(() => props.operations.length, (newLength, oldLength) => {
  if (newLength > oldLength && timelineBodyRef.value) {
    // 新操作添加后滚动到顶部
    setTimeout(() => {
      timelineBodyRef.value?.scrollTo({
        top: 0,
        behavior: 'smooth'
      });
    }, 100);
  }
});

// 暴露方法
defineExpose({
  selectOperation,
  scrollToOperation: (operationId: string) => {
    const element = timelineBodyRef.value?.querySelector(`[data-operation-id="${operationId}"]`);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }
});
</script>

<style scoped>
.operation-timeline {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.timeline-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.timeline-header {
  padding: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fafafa;
}

.timeline-stats {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.total-operations {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.time-range {
  font-size: 12px;
  color: #8c8c8c;
}

.timeline-body {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  position: relative;
}

.timeline-item {
  display: flex;
  margin-bottom: 24px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 8px;
  padding: 8px;
  margin-left: -8px;
  margin-right: -8px;
}

.timeline-item:hover {
  background: rgba(24, 144, 255, 0.04);
}

.timeline-item-selected {
  background: rgba(24, 144, 255, 0.08);
  border: 1px solid rgba(24, 144, 255, 0.2);
}

.timeline-item-conflict {
  background: rgba(245, 34, 45, 0.04);
}

.timeline-line {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 16px;
  position: relative;
}

.timeline-dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: #1890ff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.1);
  z-index: 2;
}

.timeline-connector {
  width: 2px;
  background: linear-gradient(180deg, rgba(24, 144, 255, 0.3) 0%, rgba(24, 144, 255, 0.1) 100%);
  flex: 1;
  min-height: 40px;
  margin-top: 8px;
}

.timeline-content {
  flex: 1;
  min-width: 0;
}

.operation-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: #1890ff;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-text {
  color: white;
  font-size: 10px;
  font-weight: bold;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

.operation-time {
  font-size: 12px;
  color: #8c8c8c;
}

.operation-description {
  margin-bottom: 8px;
}

.operation-action {
  font-size: 13px;
  color: #262626;
  font-weight: 500;
}

.field-change-detail {
  background: #fafafa;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 6px;
  padding: 8px 12px;
  margin-bottom: 8px;
}

.field-name {
  margin-bottom: 8px;
}

.field-label {
  font-size: 12px;
  font-weight: 600;
  color: #1890ff;
  background: #e6f7ff;
  padding: 2px 6px;
  border-radius: 3px;
}

.value-change {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.old-value,
.new-value {
  display: flex;
  align-items: center;
  gap: 8px;
}

.value-label {
  font-size: 11px;
  color: #8c8c8c;
  min-width: 48px;
}

.value-content {
  font-size: 12px;
  color: #262626;
  background: white;
  padding: 2px 6px;
  border-radius: 3px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conflict-detail {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 6px;
  padding: 8px 12px;
  margin-bottom: 8px;
}

.conflict-info {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
  font-size: 12px;
}

.conflict-field {
  color: #f5222d;
  font-weight: 500;
}

.conflict-strategy {
  color: #8c8c8c;
}

.conflict-values {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.conflict-value,
.conflict-result {
  display: flex;
  align-items: center;
  gap: 8px;
}

.conflict-result {
  border-top: 1px solid #ffccc7;
  padding-top: 4px;
  margin-top: 4px;
}

.conflict-result .value-label {
  font-weight: 600;
  color: #f5222d;
}

.operation-actions {
  display: flex;
  gap: 4px;
  margin-top: 8px;
}

.timeline-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  min-height: 200px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-text {
  font-size: 16px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.empty-tip {
  font-size: 14px;
  color: #bfbfbf;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .timeline-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .timeline-item {
    margin-bottom: 16px;
  }

  .timeline-line {
    margin-right: 12px;
  }

  .operation-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .value-change {
    gap: 2px;
  }

  .conflict-info {
    flex-direction: column;
    gap: 4px;
  }
}

/* 滚动条样式 */
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
}

.timeline-body::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}
</style>