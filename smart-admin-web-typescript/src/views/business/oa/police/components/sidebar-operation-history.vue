<!--
  * 侧边栏操作历史组件
  * 超紧凑版本，适合在侧边栏中显示
  *
  * @Author:    Claude Code Assistant
  * @Date:      2025-09-28
  * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
-->
<template>
  <div class="sidebar-operation-history">
    <!-- 标题栏 -->
    <div class="history-header">
      <h4 class="history-title">
        <HistoryOutlined />
        操作历史
      </h4>
      <a-tooltip title="刷新">
        <a-button type="text" size="small" @click="refreshData">
          <template #icon><ReloadOutlined /></template>
        </a-button>
      </a-tooltip>
    </div>

    <!-- 快速筛选 -->
    <div class="quick-filters">
      <a-radio-group
        v-model:value="filterType"
        button-style="solid"
        size="small"
        @change="handleFilterChange"
      >
        <a-radio-button value="all">全部</a-radio-button>
        <a-radio-button value="today">今日</a-radio-button>
        <a-radio-button value="update">更新</a-radio-button>
      </a-radio-group>
    </div>

    <!-- 历史列表 -->
    <div class="history-list">
      <a-spin :spinning="loading" size="small">
        <div v-if="filteredData.length > 0" class="timeline-mini">
          <div
            v-for="item in displayData"
            :key="item.id"
            class="timeline-mini-item"
            @click="showDetails(item)"
          >
            <div class="item-dot" :class="`dot-${getOperationType(item.content)}`"></div>

            <div class="item-body">
              <div class="item-summary">
                <span class="item-user">{{ item.operateUserName }}</span>
                <span class="item-time">{{ formatTimeShort(item.createTime) }}</span>
              </div>

              <div class="item-content">
                <template v-if="item.fieldChanges && Object.keys(item.fieldChanges).length > 0">
                  <div class="content-changes">
                    <span class="change-count">{{ Object.keys(item.fieldChanges).length }}个字段</span>
                    <div class="change-preview">
                      <span
                        v-for="(change, fieldKey, index) in item.fieldChanges"
                        :key="fieldKey"
                        class="field-tag"
                        v-show="index < 2"
                      >
                        {{ change.field }}
                      </span>
                      <span v-if="Object.keys(item.fieldChanges).length > 2" class="more-fields">
                        +{{ Object.keys(item.fieldChanges).length - 2 }}
                      </span>
                    </div>
                  </div>
                </template>
                <template v-else>
                  <span class="simple-action">{{ getOperationTypeText(item.content) }}</span>
                </template>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="empty-mini">
          <a-empty :image="Empty.PRESENTED_IMAGE_SIMPLE" description="暂无记录" />
        </div>
      </a-spin>
    </div>

    <!-- 查看更多 -->
    <div class="view-more" v-if="filteredData.length > displayLimit">
      <a-button type="link" size="small" block @click="expandList">
        查看更多 ({{ filteredData.length - displayLimit }})
      </a-button>
    </div>

    <!-- 详情弹窗 -->
    <a-drawer
      v-model:open="detailVisible"
      title="操作详情"
      placement="right"
      :width="400"
    >
      <div v-if="selectedItem" class="operation-detail">
        <a-descriptions :column="1" size="small" bordered>
          <a-descriptions-item label="操作人">
            {{ selectedItem.operateUserName }}
          </a-descriptions-item>
          <a-descriptions-item label="操作时间">
            {{ formatTimeFull(selectedItem.createTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="操作类型">
            {{ getOperationTypeText(selectedItem.content) }}
          </a-descriptions-item>
        </a-descriptions>

        <div v-if="selectedItem.fieldChanges" class="detail-changes">
          <h5>变更详情</h5>
          <div class="change-list">
            <div
              v-for="(change, fieldKey) in selectedItem.fieldChanges"
              :key="fieldKey"
              class="change-detail-item"
            >
              <div class="change-field">{{ change.field }}</div>
              <div class="change-values">
                <div class="old-value">
                  <span class="label">旧值:</span>
                  <span class="value">{{ formatValue(change.oldValue) }}</span>
                </div>
                <div class="new-value">
                  <span class="label">新值:</span>
                  <span class="value">{{ formatValue(change.newValue) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted, watch } from 'vue';
  import { Empty } from 'ant-design-vue';
  import { HistoryOutlined, ReloadOutlined } from '@ant-design/icons-vue';
  import { operateLogApi } from '/@/api/support/operate-log-api';
  import { policeFormConfigApi } from '/@/api/business/oa/police-form-config-api';
  import { policeReportApi } from '/@/api/business/oa/police-report-api';

  // 组件属性
  const props = defineProps<{
    reportId: string | number;
    maxItems?: number;
  }>();

  // 响应式数据
  const loading = ref(false);
  const timelineData = ref([]);
  const fieldDisplayNames = ref<Record<string, string>>({});
  const filterType = ref('all');
  const displayLimit = ref(props.maxItems || 10);
  const expanded = ref(false);
  const detailVisible = ref(false);
  const selectedItem = ref(null);

  // 计算属性
  const filteredData = computed(() => {
    if (filterType.value === 'all') {
      return timelineData.value;
    }

    if (filterType.value === 'today') {
      const today = new Date().toDateString();
      return timelineData.value.filter(item => {
        const itemDate = new Date(item.createTime).toDateString();
        return itemDate === today;
      });
    }

    if (filterType.value === 'update') {
      return timelineData.value.filter(item =>
        item.content?.includes('字段更新')
      );
    }

    return timelineData.value;
  });

  const displayData = computed(() => {
    const limit = expanded.value ? filteredData.value.length : displayLimit.value;
    return filteredData.value.slice(0, limit);
  });

  // 解析字段变更
  function parseFieldChanges(paramStr: string) {
    if (!paramStr) return {};

    try {
      const param = JSON.parse(paramStr);
      const changes = {};

      for (const [fieldName, changeData] of Object.entries(param)) {
        if (typeof changeData === 'object' && changeData !== null) {
          const data = changeData as any;
          if (data.old !== undefined || data.new !== undefined) {
            changes[fieldName] = {
              field: fieldDisplayNames.value[fieldName] || fieldName,
              oldValue: data.old,
              newValue: data.new,
              hasOldValue: data.old !== undefined && data.old !== null && data.old !== ''
            };
          }
        }
      }

      return changes;
    } catch (error) {
      return {};
    }
  }

  // 格式化时间轴数据
  function formatTimelineData(logs: any[]) {
    return logs.map(log => ({
      ...log,
      fieldChanges: parseFieldChanges(log.requestParam)
    }));
  }

  // 获取操作类型
  function getOperationType(content: string): string {
    if (content?.includes('字段更新')) return 'update';
    if (content?.includes('创建')) return 'create';
    if (content?.includes('删除')) return 'delete';
    return 'other';
  }

  // 获取操作类型文本
  function getOperationTypeText(content: string): string {
    if (content?.includes('字段更新')) return '字段更新';
    if (content?.includes('创建')) return '创建';
    if (content?.includes('删除')) return '删除';
    return '其他操作';
  }

  // 格式化时间 - 短格式
  function formatTimeShort(time: string): string {
    if (!time) return '';
    const date = new Date(time);
    const now = new Date();
    const diff = now.getTime() - date.getTime();

    if (diff < 60 * 1000) return '刚刚';
    if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}分钟前`;
    if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))}小时前`;

    return date.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  // 格式化时间 - 完整格式
  function formatTimeFull(time: string): string {
    if (!time) return '';
    return new Date(time).toLocaleString('zh-CN');
  }

  // 格式化值
  function formatValue(value: any): string {
    if (value === null || value === undefined || value === '') {
      return '空';
    }
    return String(value);
  }

  // 获取数据
  async function fetchTimelineData() {
    try {
      loading.value = true;

      // 初始化字段名称映射
      await initFieldDisplayNames();

      const response = await operateLogApi.queryList({
        pageNum: 1,
        pageSize: 50, // 限制数量，保持性能
      });

      if (response.data?.list) {
        // 筛选当前警情相关的操作
        const filteredLogs = response.data.list.filter(log => {
          if (!log.url) return false;
          return log.url.includes(`/${props.reportId}`) ||
                 log.url.includes(`reportId=${props.reportId}`) ||
                 (log.requestParam && log.requestParam.includes(`"reportId":"${props.reportId}"`));
        });

        timelineData.value = formatTimelineData(filteredLogs);
      }
    } catch (error) {
      console.error('获取操作历史失败:', error);
    } finally {
      loading.value = false;
    }
  }

  // 初始化字段显示名称
  async function initFieldDisplayNames() {
    try {
      const reportResponse = await policeReportApi.getDetail(props.reportId);
      if (!reportResponse.data) return;

      const reportType = reportResponse.data.reportType;
      if (reportType) {
        const configResponse = await policeFormConfigApi.getFormConfig(reportType);
        if (configResponse.data) {
          const fieldMap: Record<string, string> = {};
          [...configResponse.data.step2Fields, ...configResponse.data.step3Fields].forEach(field => {
            fieldMap[field.key] = field.label;
          });

          // 基础字段映射
          Object.assign(fieldMap, {
            reporterName: '报警人姓名',
            reporterPhone: '报警人电话',
            incidentLocation: '事发地点',
            description: '警情描述',
            reportType: '警情类型',
            reportLevel: '紧急程度',
            status: '处理状态'
          });

          fieldDisplayNames.value = fieldMap;
        }
      }
    } catch (error) {
      console.warn('初始化字段名称失败:', error);
    }
  }

  // 事件处理
  function handleFilterChange() {
    expanded.value = false;
  }

  function expandList() {
    expanded.value = true;
  }

  function showDetails(item: any) {
    selectedItem.value = item;
    detailVisible.value = true;
  }

  function refreshData() {
    fetchTimelineData();
  }

  // 监听reportId变化
  watch(() => props.reportId, () => {
    if (props.reportId) {
      fetchTimelineData();
    }
  }, { immediate: true });

  // 组件挂载
  onMounted(() => {
    fetchTimelineData();
  });
</script>

<style scoped>
.sidebar-operation-history {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fafafa;
  border-radius: 6px;
  overflow: hidden;
}

/* 标题栏 */
.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.history-title {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 快速筛选 */
.quick-filters {
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.quick-filters .ant-radio-group {
  width: 100%;
}

.quick-filters .ant-radio-button-wrapper {
  flex: 1;
  text-align: center;
}

/* 历史列表 */
.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.timeline-mini {
  position: relative;
}

.timeline-mini-item {
  position: relative;
  display: flex;
  align-items: flex-start;
  padding: 8px 12px;
  margin-bottom: 8px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.2s;
}

.timeline-mini-item:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 4px rgba(24, 144, 255, 0.1);
}

.timeline-mini-item:last-child {
  margin-bottom: 0;
}

.item-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-top: 6px;
  margin-right: 8px;
  flex-shrink: 0;
}

.dot-update { background-color: #1890ff; }
.dot-create { background-color: #52c41a; }
.dot-delete { background-color: #ff4d4f; }
.dot-other { background-color: #d9d9d9; }

.item-body {
  flex: 1;
  min-width: 0;
}

.item-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.item-user {
  font-weight: 500;
  font-size: 12px;
  color: #262626;
}

.item-time {
  font-size: 11px;
  color: #8c8c8c;
}

.item-content {
  font-size: 11px;
}

.content-changes {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.change-count {
  color: #1890ff;
  font-weight: 500;
}

.change-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
}

.field-tag {
  background: #f0f8ff;
  color: #1890ff;
  padding: 1px 4px;
  border-radius: 2px;
  font-size: 10px;
}

.more-fields {
  color: #8c8c8c;
  font-size: 10px;
}

.simple-action {
  color: #595959;
}

/* 空状态 */
.empty-mini {
  padding: 20px;
  text-align: center;
}

/* 查看更多 */
.view-more {
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

/* 详情弹窗 */
.operation-detail {
  padding: 0;
}

.detail-changes {
  margin-top: 24px;
}

.detail-changes h5 {
  margin-bottom: 12px;
  color: #262626;
  font-weight: 500;
}

.change-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.change-detail-item {
  background: #fafafa;
  border-radius: 6px;
  padding: 12px;
}

.change-field {
  font-weight: 500;
  color: #262626;
  margin-bottom: 8px;
}

.change-values {
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

.old-value .label {
  color: #ff7875;
  font-size: 12px;
  width: 40px;
}

.new-value .label {
  color: #73d13d;
  font-size: 12px;
  width: 40px;
}

.old-value .value {
  background: #fff1f0;
  color: #a8071a;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  text-decoration: line-through;
}

.new-value .value {
  background: #f6ffed;
  color: #389e0d;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  font-weight: 500;
}

/* 滚动条优化 */
.history-list::-webkit-scrollbar {
  width: 4px;
}

.history-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 2px;
}

.history-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 2px;
}

.history-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>