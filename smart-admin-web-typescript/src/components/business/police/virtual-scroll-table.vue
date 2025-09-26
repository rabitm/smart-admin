<!--
  * 高性能虚拟滚动表格组件
  * 支持200-500个并发端的实时数据更新
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-26
  * @Copyright 1024创新实验室
-->
<template>
  <div class="virtual-scroll-table" ref="containerRef">
    <!-- 表头 -->
    <div class="table-header" :style="{ width: totalWidth + 'px' }">
      <table>
        <thead>
          <tr>
            <th
              v-for="column in columns"
              :key="column.key"
              :style="{ width: column.width + 'px' }"
              @click="handleSort(column)"
            >
              <div class="header-cell">
                <span>{{ column.title }}</span>
                <span v-if="column.sortable" class="sort-icon">
                  <CaretUpOutlined
                    :class="{ active: sortField === column.key && sortOrder === 'ascend' }"
                  />
                  <CaretDownOutlined
                    :class="{ active: sortField === column.key && sortOrder === 'descend' }"
                  />
                </span>
              </div>
            </th>
          </tr>
        </thead>
      </table>
    </div>

    <!-- 虚拟滚动容器 -->
    <div
      class="scroll-container"
      ref="scrollRef"
      @scroll="handleScroll"
      :style="{ height: containerHeight + 'px' }"
    >
      <!-- 占位元素，撑开滚动高度 -->
      <div class="scroll-spacer" :style="{ height: totalHeight + 'px' }"></div>

      <!-- 可见行渲染 -->
      <div
        class="visible-content"
        :style="{
          transform: `translateY(${offsetY}px)`,
          width: totalWidth + 'px'
        }"
      >
        <table>
          <tbody>
            <tr
              v-for="(row, index) in visibleRows"
              :key="row.id"
              :class="getRowClass(row)"
              :style="{ height: rowHeight + 'px' }"
              @click="handleRowClick(row)"
              @dblclick="handleRowDoubleClick(row)"
            >
              <td
                v-for="column in columns"
                :key="`${row.id}-${column.key}`"
                :style="{ width: column.width + 'px' }"
                :class="getCellClass(row, column)"
              >
                <div class="cell-content">
                  <!-- 自定义插槽 -->
                  <slot
                    v-if="column.slot"
                    :name="column.slot"
                    :record="row"
                    :column="column"
                    :value="row[column.dataIndex || column.key]"
                  />

                  <!-- 默认渲染 -->
                  <template v-else>
                    <span
                      v-if="isFieldUpdating(row, column)"
                      class="updating-indicator"
                    >
                      <LoadingOutlined />
                    </span>
                    {{ formatCellValue(row, column) }}
                  </template>

                  <!-- 更新标记 -->
                  <transition name="update-flash">
                    <span
                      v-if="isFieldUpdated(row, column)"
                      class="update-badge"
                    ></span>
                  </transition>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 性能监控面板 -->
    <div v-if="showPerformancePanel" class="performance-panel">
      <div class="stat-item">
        <span class="label">FPS:</span>
        <span class="value" :class="{ good: fps >= 50, warning: fps < 30 }">
          {{ fps }}
        </span>
      </div>
      <div class="stat-item">
        <span class="label">渲染耗时:</span>
        <span class="value">{{ renderTime }}ms</span>
      </div>
      <div class="stat-item">
        <span class="label">可见行:</span>
        <span class="value">{{ visibleRows.length }}/{{ totalRows }}</span>
      </div>
      <div class="stat-item">
        <span class="label">更新数:</span>
        <span class="value">{{ updateCount }}</span>
      </div>
      <div class="stat-item">
        <span class="label">内存:</span>
        <span class="value">{{ memoryUsage }}MB</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue';
import {
  CaretUpOutlined,
  CaretDownOutlined,
  LoadingOutlined
} from '@ant-design/icons-vue';
import { debounce, throttle } from 'lodash-es';
import type { PoliceReportData } from '/@/utils/police-list-update-manager';

/**
 * 列定义
 */
interface TableColumn {
  key: string;
  title: string;
  dataIndex?: string;
  width: number;
  sortable?: boolean;
  slot?: string;
  formatter?: (value: any, record: any) => string;
}

/**
 * 组件属性
 */
interface Props {
  data: PoliceReportData[];
  columns: TableColumn[];
  rowHeight?: number;
  containerHeight?: number;
  bufferSize?: number;
  showPerformancePanel?: boolean;
  highlightUpdates?: boolean;
  updateDuration?: number;
}

const props = withDefaults(defineProps<Props>(), {
  rowHeight: 50,
  containerHeight: 600,
  bufferSize: 10,
  showPerformancePanel: false,
  highlightUpdates: true,
  updateDuration: 2000
});

const emit = defineEmits<{
  rowClick: [row: PoliceReportData];
  rowDoubleClick: [row: PoliceReportData];
  sort: [field: string, order: string];
}>();

// DOM引用
const containerRef = ref<HTMLElement>();
const scrollRef = ref<HTMLElement>();

// 滚动状态
const scrollTop = ref(0);
const offsetY = ref(0);

// 排序状态
const sortField = ref<string>('');
const sortOrder = ref<'ascend' | 'descend'>('');

// 可见行计算
const visibleRange = computed(() => {
  const start = Math.floor(scrollTop.value / props.rowHeight);
  const visibleCount = Math.ceil(props.containerHeight / props.rowHeight);
  const end = start + visibleCount;
  return {
    start: Math.max(0, start - props.bufferSize),
    end: Math.min(props.data.length, end + props.bufferSize)
  };
});

// 可见行数据
const visibleRows = computed(() => {
  const { start, end } = visibleRange.value;
  return props.data.slice(start, end);
});

// 总行数
const totalRows = computed(() => props.data.length);

// 总高度
const totalHeight = computed(() => props.data.length * props.rowHeight);

// 总宽度
const totalWidth = computed(() => {
  return props.columns.reduce((sum, col) => sum + col.width, 0);
});

// 更新追踪
const updatedFields = ref(new Map<string, Set<string>>());
const updatingRows = ref(new Set<number>());

// 性能监控
const fps = ref(60);
const renderTime = ref(0);
const updateCount = ref(0);
const memoryUsage = ref(0);

let lastFrameTime = performance.now();
let frameCount = 0;

/**
 * 处理滚动事件（节流）
 */
const handleScroll = throttle((event: Event) => {
  const target = event.target as HTMLElement;
  scrollTop.value = target.scrollTop;

  // 计算偏移量
  const { start } = visibleRange.value;
  offsetY.value = start * props.rowHeight;

  // 性能监控
  measurePerformance();
}, 16); // 60fps

/**
 * 处理排序
 */
const handleSort = (column: TableColumn) => {
  if (!column.sortable) return;

  if (sortField.value === column.key) {
    sortOrder.value = sortOrder.value === 'ascend' ? 'descend' : 'ascend';
  } else {
    sortField.value = column.key;
    sortOrder.value = 'ascend';
  }

  emit('sort', sortField.value, sortOrder.value);
};

/**
 * 处理行点击
 */
const handleRowClick = (row: PoliceReportData) => {
  emit('rowClick', row);
};

/**
 * 处理行双击
 */
const handleRowDoubleClick = (row: PoliceReportData) => {
  emit('rowDoubleClick', row);
};

/**
 * 获取行样式类
 */
const getRowClass = (row: PoliceReportData) => {
  const classes: string[] = ['table-row'];

  // 更新高亮
  if (props.highlightUpdates && row.updateFields && row.updateFields.size > 0) {
    classes.push('row-updated');
  }

  // 状态样式
  if (row.status === 'COMPLETED') {
    classes.push('row-completed');
  } else if (row.status === 'IN_PROGRESS') {
    classes.push('row-in-progress');
  }

  // 等级样式
  if (row.reportLevel === 'HIGH') {
    classes.push('row-high-priority');
  }

  return classes.join(' ');
};

/**
 * 获取单元格样式类
 */
const getCellClass = (row: PoliceReportData, column: TableColumn) => {
  const classes: string[] = ['table-cell'];

  if (isFieldUpdated(row, column)) {
    classes.push('cell-updated');
  }

  return classes.join(' ');
};

/**
 * 检查字段是否正在更新
 */
const isFieldUpdating = (row: PoliceReportData, column: TableColumn) => {
  return updatingRows.value.has(row.id);
};

/**
 * 检查字段是否已更新
 */
const isFieldUpdated = (row: PoliceReportData, column: TableColumn) => {
  if (!props.highlightUpdates) return false;

  const key = column.dataIndex || column.key;
  return row.updateFields?.has(key) || false;
};

/**
 * 格式化单元格值
 */
const formatCellValue = (row: PoliceReportData, column: TableColumn) => {
  const key = column.dataIndex || column.key;
  const value = row[key];

  if (column.formatter) {
    return column.formatter(value, row);
  }

  // 默认格式化
  if (value === null || value === undefined) {
    return '-';
  }

  if (value instanceof Date) {
    return value.toLocaleString('zh-CN');
  }

  return String(value);
};

/**
 * 性能测量
 */
const measurePerformance = () => {
  const now = performance.now();
  const delta = now - lastFrameTime;

  frameCount++;

  // 每秒计算一次FPS
  if (delta >= 1000) {
    fps.value = Math.round((frameCount * 1000) / delta);
    frameCount = 0;
    lastFrameTime = now;
  }

  // 测量渲染时间
  const renderStart = performance.now();
  nextTick(() => {
    renderTime.value = Math.round(performance.now() - renderStart);
  });

  // 内存使用（如果支持）
  if ('memory' in performance) {
    const memInfo = (performance as any).memory;
    memoryUsage.value = Math.round(memInfo.usedJSHeapSize / 1048576);
  }
};

/**
 * 处理数据更新
 */
watch(() => props.data, (newData, oldData) => {
  // 检测更新
  if (oldData && newData.length === oldData.length) {
    updateCount.value++;

    // 标记更新的行
    newData.forEach((row, index) => {
      if (oldData[index] && row.version > oldData[index].version) {
        // 临时标记为正在更新
        updatingRows.value.add(row.id);

        // 延迟清除标记
        setTimeout(() => {
          updatingRows.value.delete(row.id);
        }, props.updateDuration);
      }
    });
  }
}, { deep: false });

/**
 * 滚动到指定行
 */
const scrollToRow = (rowId: number) => {
  const index = props.data.findIndex(row => row.id === rowId);
  if (index >= 0) {
    const targetScrollTop = index * props.rowHeight;
    if (scrollRef.value) {
      scrollRef.value.scrollTop = targetScrollTop;
    }
  }
};

/**
 * 刷新视图
 */
const refresh = () => {
  // 强制重新计算
  scrollTop.value = scrollRef.value?.scrollTop || 0;
  const { start } = visibleRange.value;
  offsetY.value = start * props.rowHeight;
};

// 定期刷新性能数据
let performanceTimer: NodeJS.Timeout;

onMounted(() => {
  if (props.showPerformancePanel) {
    performanceTimer = setInterval(measurePerformance, 1000);
  }
});

onUnmounted(() => {
  if (performanceTimer) {
    clearInterval(performanceTimer);
  }
});

// 暴露方法
defineExpose({
  scrollToRow,
  refresh
});
</script>

<style scoped lang="less">
.virtual-scroll-table {
  position: relative;
  background: white;
  border: 1px solid #f0f0f0;
  border-radius: 4px;

  .table-header {
    position: sticky;
    top: 0;
    z-index: 10;
    background: white;
    border-bottom: 2px solid #f0f0f0;

    table {
      width: 100%;
      table-layout: fixed;
    }

    th {
      padding: 12px 8px;
      text-align: left;
      font-weight: 600;
      color: #333;
      background: #fafafa;
      border-right: 1px solid #f0f0f0;
      cursor: pointer;
      user-select: none;

      &:last-child {
        border-right: none;
      }

      .header-cell {
        display: flex;
        align-items: center;
        justify-content: space-between;

        .sort-icon {
          display: flex;
          flex-direction: column;
          margin-left: 4px;

          .anticon {
            font-size: 10px;
            color: #999;
            transition: color 0.2s;

            &.active {
              color: #1890ff;
            }
          }
        }
      }
    }
  }

  .scroll-container {
    position: relative;
    overflow-y: auto;
    overflow-x: hidden;

    &::-webkit-scrollbar {
      width: 8px;
    }

    &::-webkit-scrollbar-track {
      background: #f1f1f1;
    }

    &::-webkit-scrollbar-thumb {
      background: #888;
      border-radius: 4px;

      &:hover {
        background: #555;
      }
    }
  }

  .scroll-spacer {
    position: absolute;
    top: 0;
    left: 0;
    width: 1px;
    pointer-events: none;
  }

  .visible-content {
    position: absolute;
    top: 0;
    left: 0;

    table {
      width: 100%;
      table-layout: fixed;
      border-collapse: collapse;
    }

    .table-row {
      transition: background-color 0.2s;
      cursor: pointer;

      &:hover {
        background-color: #f5f5f5;
      }

      &.row-updated {
        animation: updateFlash 1s ease;
      }

      &.row-completed {
        opacity: 0.7;
      }

      &.row-in-progress {
        background-color: #e6f7ff;
      }

      &.row-high-priority {
        border-left: 3px solid #ff4d4f;
      }

      td {
        padding: 8px;
        border-bottom: 1px solid #f0f0f0;
        border-right: 1px solid #f0f0f0;

        &:last-child {
          border-right: none;
        }

        .cell-content {
          position: relative;
          display: flex;
          align-items: center;

          .updating-indicator {
            margin-right: 4px;
            color: #1890ff;
            animation: spin 1s linear infinite;
          }

          .update-badge {
            position: absolute;
            top: 2px;
            right: 2px;
            width: 6px;
            height: 6px;
            background: #52c41a;
            border-radius: 50%;
            animation: pulse 2s ease infinite;
          }
        }

        &.cell-updated {
          background-color: #f6ffed;
          animation: cellUpdateFlash 2s ease;
        }
      }
    }
  }

  .performance-panel {
    position: fixed;
    top: 100px;
    right: 20px;
    padding: 12px;
    background: rgba(0, 0, 0, 0.8);
    border-radius: 4px;
    color: white;
    font-size: 12px;
    font-family: 'Courier New', monospace;
    z-index: 1000;

    .stat-item {
      display: flex;
      justify-content: space-between;
      margin-bottom: 4px;
      min-width: 120px;

      .label {
        margin-right: 8px;
        opacity: 0.8;
      }

      .value {
        font-weight: bold;

        &.good {
          color: #52c41a;
        }

        &.warning {
          color: #faad14;
        }
      }
    }
  }
}

// 动画定义
@keyframes updateFlash {
  0% {
    background-color: transparent;
  }
  25% {
    background-color: #fff3e0;
  }
  100% {
    background-color: transparent;
  }
}

@keyframes cellUpdateFlash {
  0% {
    background-color: #f6ffed;
  }
  100% {
    background-color: transparent;
  }
}

@keyframes pulse {
  0% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.2);
  }
  100% {
    opacity: 0;
    transform: scale(1.5);
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.update-flash-enter-active {
  transition: all 0.3s ease;
}

.update-flash-leave-active {
  transition: all 2s ease;
}

.update-flash-enter-from {
  opacity: 0;
  transform: scale(0);
}

.update-flash-leave-to {
  opacity: 0;
  transform: scale(2);
}
</style>