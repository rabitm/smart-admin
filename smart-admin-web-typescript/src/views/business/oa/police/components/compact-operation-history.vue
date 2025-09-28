<!--
  * 紧凑型操作历史组件
  * 支持多种展示模式，提高信息密度和可读性
  *
  * @Author:    Claude Code Assistant
  * @Date:      2025-09-28
  * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
-->
<template>
  <div class="compact-operation-history">
    <!-- 工具栏 -->
    <div class="history-toolbar">
      <div class="toolbar-left">
        <a-radio-group v-model:value="viewMode" button-style="solid" size="small">
          <a-radio-button value="compact">紧凑视图</a-radio-button>
          <a-radio-button value="table">表格视图</a-radio-button>
          <a-radio-button value="card">卡片视图</a-radio-button>
        </a-radio-group>
      </div>
      <div class="toolbar-right">
        <a-select
          v-model:value="queryForm.operationType"
          placeholder="操作类型"
          allowClear
          size="small"
          style="width: 120px; margin-right: 8px;"
          @change="handleSearch"
        >
          <a-select-option value="FIELD_UPDATE">字段更新</a-select-option>
          <a-select-option value="CREATE">创建</a-select-option>
          <a-select-option value="DELETE">删除</a-select-option>
        </a-select>
        <a-input
          v-model:value="queryForm.userName"
          placeholder="操作人员"
          allowClear
          size="small"
          style="width: 120px; margin-right: 8px;"
          @change="handleSearch"
        />
        <a-button type="primary" size="small" @click="refreshData">
          <template #icon><ReloadOutlined /></template>
        </a-button>
      </div>
    </div>

    <!-- 紧凑时间轴视图 -->
    <div v-if="viewMode === 'compact'" class="compact-timeline-view">
      <a-spin :spinning="loading">
        <div v-if="paginatedTimelineData.length > 0" class="timeline-list">
          <div
            v-for="item in paginatedTimelineData"
            :key="item.uniqueId || `compact-${item.operateLogId || item.id || Math.random()}`"
            class="timeline-item-compact"
          >
            <div class="item-left">
              <div class="item-dot" :class="`dot-${getOperationType(item.content)}`"></div>
              <div class="item-time-compact">{{ formatCompactTime(item.createTime) }}</div>
            </div>
            <div class="item-content">
              <!-- 核心信息行：谁做了什么 -->
              <div class="item-summary">
                <span class="item-user">{{ item.operateUserName }}</span>
                <span class="item-action">{{ getOperationTypeText(item.content) }}</span>
                <span class="item-full-time">{{ formatFullTime(item.createTime) }}</span>
              </div>

              <!-- 详细变更信息：旧值→新值 -->
              <div class="item-changes" v-if="item.fieldChanges && Object.keys(item.fieldChanges).length > 0">
                <div
                  v-for="(change, fieldKey) in item.fieldChanges"
                  :key="`change-${item.operateLogId}-${fieldKey}`"
                  class="change-detail"
                >
                  <span class="field-label">{{ change.field }}:</span>
                  <div class="value-change">
                    <span class="old-value" :title="change.oldValue">{{ formatDisplayValue(change.oldValue) }}</span>
                    <span class="change-arrow">→</span>
                    <span class="new-value" :title="change.newValue">{{ formatDisplayValue(change.newValue) }}</span>
                  </div>
                </div>
              </div>

              <!-- 无字段变更时显示操作描述 -->
              <div v-else-if="item.content" class="item-description">
                {{ item.content }}
              </div>
            </div>
          </div>
        </div>
        <a-empty v-else description="暂无操作记录" />
      </a-spin>
    </div>

    <!-- 表格视图 -->
    <div v-if="viewMode === 'table'" class="table-view">
      <a-table
        :columns="tableColumns"
        :dataSource="paginatedTimelineData"
        :pagination="{
          current: queryForm.pageNum,
          pageSize: queryForm.pageSize,
          total: total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total, range) => `共 ${total} 条记录 第 ${range[0]}-${range[1]} 条`,
          size: 'small'
        }"
        size="small"
        :loading="loading"
        @change="handleTableChange"
        :scroll="{ y: 400 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'changes'">
            <div class="table-changes">
              <a-tag
                v-for="(change, fieldKey) in record.fieldChanges"
                :key="`table-${record.operateLogId}-${fieldKey}`"
                size="small"
                color="blue"
                class="change-tag-table"
              >
                {{ change.field }}: {{ formatValue(change.oldValue) }} → {{ formatValue(change.newValue) }}
              </a-tag>
            </div>
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
        </template>
      </a-table>
    </div>

    <!-- 卡片视图 -->
    <div v-if="viewMode === 'card'" class="card-view">
      <a-spin :spinning="loading">
        <div v-if="paginatedTimelineData.length > 0" class="card-list">
          <div
            v-for="item in paginatedTimelineData"
            :key="item.uniqueId || `card-${item.operateLogId || item.id || Math.random()}`"
            class="operation-card"
          >
            <div class="card-header">
              <a-avatar size="small" class="user-avatar">
                {{ item.operateUserName?.charAt(0) }}
              </a-avatar>
              <div class="card-info">
                <div class="card-user">{{ item.operateUserName }}</div>
                <div class="card-time">{{ formatTime(item.createTime) }}</div>
              </div>
              <a-tag size="small" :color="getOperationColor(item.content)">
                {{ getOperationTypeText(item.content) }}
              </a-tag>
            </div>
            <div class="card-content" v-if="item.fieldChanges">
              <div class="changes-grid">
                <div
                  v-for="(change, fieldKey) in item.fieldChanges"
                  :key="`card-change-${item.operateLogId}-${fieldKey}`"
                  class="change-item-card"
                >
                  <div class="field-name">{{ change.field }}</div>
                  <div class="field-change">
                    <span class="old-val">{{ formatValue(change.oldValue) }}</span>
                    <span class="arrow">→</span>
                    <span class="new-val">{{ formatValue(change.newValue) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <a-empty v-else description="暂无操作记录" />
      </a-spin>
    </div>

    <!-- 分页组件 -->
    <div class="pagination-container">
      <!-- 表格视图使用内置分页 -->
      <template v-if="viewMode === 'table'">
        <!-- 表格内置分页已处理 -->
      </template>
      <!-- 紧凑和卡片视图使用独立分页 -->
      <template v-else>
        <div class="pagination-debug" v-if="isDevelopment">
          <small style="color: #666; margin-bottom: 8px; display: block;">
            调试信息: 总数={{total}}, 每页={{queryForm.pageSize}}, 当前页={{queryForm.pageNum}}, 视图={{viewMode}}
          </small>
        </div>
        <a-pagination
          v-if="total > 0"
          v-model:current="queryForm.pageNum"
          v-model:page-size="queryForm.pageSize"
          :total="total"
          :page-size-options="['5', '10', '20', '50']"
          :show-size-changer="true"
          :show-quick-jumper="total > 50"
          :show-total="(total, range) => `共 ${total} 条记录 第 ${range[0]}-${range[1]} 条`"
          size="small"
          @change="handlePageChange"
          @showSizeChange="handlePageSizeChange"
          class="timeline-pagination"
        />
        <div v-else-if="!loading" class="no-data-pagination">
          <small style="color: #999;">暂无数据</small>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted, onUnmounted, reactive, watch, computed } from 'vue';
  import { ReloadOutlined } from '@ant-design/icons-vue';
  import { operateLogApi } from '/@/api/support/operate-log-api';
  import { policeOperationHistoryApi } from '/@/api/business/oa/police-operation-history-api';
  import { policeFormConfigApi } from '/@/api/business/oa/police-form-config-api';
  import { policeReportApi } from '/@/api/business/oa/police-report-api';

  // 组件属性
  const props = defineProps<{
    reportId: string | number;
    reportNumber?: string;
  }>();

  // 响应式数据
  const loading = ref(false);
  const timelineData = ref([]);
  const total = ref(0);
  const viewMode = ref('compact'); // compact | table | card
  const fieldDisplayNames = ref<Record<string, string>>({});

  // 分页数据计算
  const paginatedTimelineData = computed(() => {
    try {
      if (!timelineData.value || !Array.isArray(timelineData.value)) {
        return [];
      }

      if (viewMode.value === 'table') {
        // 表格视图使用内置分页
        return timelineData.value;
      }

      // 紧凑和卡片视图需要手动分页
      const start = Math.max(0, (queryForm.pageNum - 1) * queryForm.pageSize);
      const end = start + queryForm.pageSize;
      return timelineData.value.slice(start, end);
    } catch (error) {
      console.error('分页数据计算错误:', error);
      return [];
    }
  });

  const queryForm = reactive({
    pageNum: 1,
    pageSize: 5,  // 默认每页显示5条，方便测试分页
    operationType: undefined,
    userName: undefined,
  });

  // 表格列配置
  const tableColumns = [
    {
      title: '操作人',
      dataIndex: 'operateUserName',
      key: 'operateUserName',
      width: 100,
    },
    {
      title: '操作类型',
      dataIndex: 'content',
      key: 'content',
      width: 120,
      customRender: ({ text }) => getOperationTypeText(text),
    },
    {
      title: '变更内容',
      key: 'changes',
      ellipsis: true,
    },
    {
      title: '操作时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 160,
    },
  ];

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

  // 获取操作颜色
  function getOperationColor(content: string): string {
    const type = getOperationType(content);
    const colorMap = {
      update: 'blue',
      create: 'green',
      delete: 'red',
      other: 'default'
    };
    return colorMap[type] || 'default';
  }

  // 格式化时间
  function formatTime(time: string): string {
    if (!time) return '';
    const date = new Date(time);
    const now = new Date();
    const diff = now.getTime() - date.getTime();

    // 小于1分钟
    if (diff < 60 * 1000) {
      return '刚刚';
    }
    // 小于1小时
    if (diff < 60 * 60 * 1000) {
      return `${Math.floor(diff / (60 * 1000))}分钟前`;
    }
    // 小于1天
    if (diff < 24 * 60 * 60 * 1000) {
      return `${Math.floor(diff / (60 * 60 * 1000))}小时前`;
    }
    // 超过1天，显示具体时间
    return date.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  // 格式化紧凑时间（用于左侧显示）
  function formatCompactTime(time: string): string {
    if (!time) return '';
    const date = new Date(time);
    return date.toLocaleString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  // 格式化完整时间（用于详细信息）
  function formatFullTime(time: string): string {
    if (!time) return '';
    const date = new Date(time);
    return date.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }

  // 格式化值
  function formatValue(value: any): string {
    if (value === null || value === undefined || value === '') {
      return '空';
    }
    const str = String(value);
    return str.length > 20 ? str.substring(0, 20) + '...' : str;
  }

  // 格式化显示值（保留更多内容）
  function formatDisplayValue(value: any): string {
    if (value === null || value === undefined || value === '') {
      return '(空)';
    }
    const str = String(value);
    return str.length > 50 ? str.substring(0, 50) + '...' : str;
  }

  // 解析字段变更
  function parseFieldChanges(log: any) {
    console.log('🔍 [调试] 解析操作日志:', {
      requestParam: log.requestParam,
      param: log.param,
      content: log.content,
      url: log.url
    });

    // 尝试多个可能的数据源
    const paramSources = [
      log.param,           // 首先尝试 param 字段
      log.requestParam,    // 然后尝试 requestParam 字段
    ];

    for (const paramStr of paramSources) {
      if (!paramStr) continue;

      try {
        const param = JSON.parse(paramStr);
        console.log('📋 [调试] 解析JSON成功:', param);
        const changes = {};

        for (const [fieldName, changeData] of Object.entries(param)) {
          console.log('🔍 [调试] 处理字段:', fieldName, changeData);

          if (typeof changeData === 'object' && changeData !== null) {
            const data = changeData as any;
            if (data.old !== undefined || data.new !== undefined) {
              changes[fieldName] = {
                field: fieldDisplayNames.value[fieldName] || fieldName,
                oldValue: data.old,
                newValue: data.new,
                hasOldValue: data.old !== undefined && data.old !== null && data.old !== ''
              };
              console.log('✅ [调试] 成功解析字段变更:', fieldName, changes[fieldName]);
            }
          }
        }

        if (Object.keys(changes).length > 0) {
          console.log('🎉 [调试] 解析到字段变更:', changes);
          return changes;
        }
      } catch (error) {
        console.warn('❌ [调试] JSON解析失败:', error, paramStr);
        continue;
      }
    }

    // 如果没有找到JSON格式的变更，尝试从content解析
    if (log.content && log.content.includes('同步警情字段更新')) {
      console.log('🔍 [调试] 尝试从content解析字段更新');
      return parseFromContent(log.content);
    }

    console.log('❌ [调试] 无法解析字段变更');
    return {};
  }

  // 从content字符串解析字段更新
  function parseFromContent(content: string) {
    console.log('🔍 [调试] 从内容解析:', content);
    // 这里可以添加从content解析的逻辑，如果有特定格式的话
    return {};
  }

  // 格式化时间轴数据
  function formatTimelineData(logs: any[]) {
    if (!logs || !Array.isArray(logs)) {
      console.warn('无效的日志数据:', logs);
      return [];
    }

    console.log('📊 [调试] 格式化时间轴数据，共', logs.length, '条记录');
    return logs.map((log, index) => {
      try {
        const fieldChanges = parseFieldChanges(log);
        console.log('📝 [调试] 日志记录:', {
          id: log.operateLogId,
          content: log.content,
          fieldChangesCount: Object.keys(fieldChanges || {}).length
        });
        return {
          ...log,
          // 确保每个项目有唯一ID
          uniqueId: log.operateLogId || log.id || `log-${index}-${Date.now()}`,
          fieldChanges
        };
      } catch (error) {
        console.error('格式化单条日志失败:', error, log);
        return {
          ...log,
          uniqueId: `error-log-${index}-${Date.now()}`,
          fieldChanges: {}
        };
      }
    });
  }

  // 获取数据
  async function fetchTimelineData() {
    // 组件已销毁，不执行请求
    if (isComponentDestroyed.value) {
      return;
    }

    try {
      loading.value = true;

      // 初始化字段名称映射
      await initFieldDisplayNames();

      console.log('🔍 [调试] 获取操作历史 - 使用新API');

      // 优先使用新的高性能API
      const query = {
        pageNum: 1,
        pageSize: 1000,
        operationType: queryForm.operationType,
        userName: queryForm.userName
      };

      const response = await policeOperationHistoryApi.getOperationHistory(props.reportId, query);

      if (response.success && response.data?.list) {
        console.log('✅ [调试] 新API获取成功:', response.data.list.length, '条记录');

        // 新API已经过滤了相关数据，直接使用
        timelineData.value = formatTimelineData(response.data.list);
        total.value = response.data.total;

        console.log('✅ [调试] 数据加载完成:', {
          timelineDataLength: timelineData.value.length,
          total: total.value,
          paginatedLength: paginatedTimelineData.value.length
        });

        return;
      }

      console.log('⚠️ [调试] 新API失败，降级到旧API');

      // 降级到原有API
      const params = {
        pageNum: 1,
        pageSize: 1000,
      };

      const fallbackResponse = await operateLogApi.queryList(params);
      console.log('📊 [调试] 降级API返回数据:', fallbackResponse.data?.list?.length || 0, '条记录');

      if (fallbackResponse.data?.list) {
        // 筛选当前警情相关的操作
        const filteredLogs = fallbackResponse.data.list.filter(log => {
          if (!log) return false;

          // 检查URL中是否包含当前reportId
          if (log.url) {
            if (log.url.includes(`/${props.reportId}`) ||
                log.url.includes(`reportId=${props.reportId}`) ||
                log.url.includes(`report/${props.reportId}`) ||
                log.url.includes(`sync-field/${props.reportId}`)) {
              return true;
            }
          }

          // 检查参数中是否包含当前reportId
          if (log.requestParam) {
            if (log.requestParam.includes(`"reportId":"${props.reportId}"`) ||
                log.requestParam.includes(`"reportId":${props.reportId}`) ||
                log.requestParam.includes(`reportId=${props.reportId}`)) {
              return true;
            }
          }

          if (log.param) {
            if (log.param.includes(`"reportId":"${props.reportId}"`) ||
                log.param.includes(`"reportId":${props.reportId}`) ||
                log.param.includes(`reportId=${props.reportId}`)) {
              return true;
            }
          }

          // 检查内容中是否包含警情相关操作
          if (log.content && props.reportNumber) {
            if (log.content.includes(props.reportNumber) ||
                log.content.includes(`警情${props.reportId}`) ||
                log.content.includes('同步警情字段更新')) {
              return true;
            }
          }

          return false;
        });

        console.log('🔍 [调试] 筛选详情:', {
          totalLogs: fallbackResponse.data.list.length,
          filteredCount: filteredLogs.length,
          reportId: props.reportId,
          reportNumber: props.reportNumber,
          sampleUrls: fallbackResponse.data.list.slice(0, 3).map(log => log.url)
        });

        // 应用额外的筛选条件
        let finalLogs = filteredLogs;

        if (queryForm.operationType) {
          finalLogs = finalLogs.filter(log => {
            if (queryForm.operationType === 'FIELD_UPDATE') {
              return log.content && log.content.includes('字段更新');
            } else if (queryForm.operationType === 'CREATE') {
              return log.content && log.content.includes('创建');
            } else if (queryForm.operationType === 'DELETE') {
              return log.content && log.content.includes('删除');
            }
            return true;
          });
        }

        if (queryForm.userName) {
          finalLogs = finalLogs.filter(log =>
            log.operateUserName && log.operateUserName.includes(queryForm.userName)
          );
        }

        console.log('📊 [调试] 最终筛选结果:', {
          originalCount: fallbackResponse.data.list.length,
          reportFilteredCount: filteredLogs.length,
          finalCount: finalLogs.length,
          operationType: queryForm.operationType,
          userName: queryForm.userName
        });

        // 检查组件是否已销毁
        if (isComponentDestroyed.value) {
          return;
        }

        // 所有视图都加载完整数据，分页在computed中处理
        timelineData.value = formatTimelineData(finalLogs);
        total.value = finalLogs.length;

        console.log('✅ [调试] 数据加载完成:', {
          timelineDataLength: timelineData.value.length,
          total: total.value,
          paginatedLength: paginatedTimelineData.value.length
        });
      }
    } catch (error) {
      console.error('获取操作历史失败:', error);
    } finally {
      // 只有在组件未销毁时才更新loading状态
      if (!isComponentDestroyed.value) {
        loading.value = false;
      }
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
  function handleSearch() {
    queryForm.pageNum = 1;
    fetchTimelineData();
  }

  function handlePageChange(page: number, pageSize?: number) {
    queryForm.pageNum = page;
    if (pageSize) queryForm.pageSize = pageSize;
    fetchTimelineData();
  }

  function handlePageSizeChange(current: number, size: number) {
    queryForm.pageNum = 1;
    queryForm.pageSize = size;
    fetchTimelineData();
  }

  function handleTableChange(pagination: any) {
    queryForm.pageNum = pagination.current;
    queryForm.pageSize = pagination.pageSize;
    fetchTimelineData();
  }

  function refreshData() {
    fetchTimelineData();
  }

  // 监听查询参数变化
  watch(() => [queryForm.operationType, queryForm.userName], () => {
    handleSearch();
  }, { deep: true });

  // 监听视图模式变化，重置分页
  watch(() => viewMode.value, () => {
    queryForm.pageNum = 1;
  });

  // 组件状态
  const isComponentMounted = ref(false);
  const isComponentDestroyed = ref(false);

  // 环境检测
  const isDevelopment = computed(() => {
    try {
      return import.meta.env?.MODE === 'development' ||
             (typeof process !== 'undefined' && process.env?.NODE_ENV === 'development');
    } catch {
      return false;
    }
  });

  // 组件挂载
  onMounted(() => {
    isComponentMounted.value = true;

    // 异步预热缓存（不影响页面加载速度）
    policeOperationHistoryApi.preloadCache(props.reportId);

    fetchTimelineData();
  });

  // 组件卸载
  onUnmounted(() => {
    isComponentDestroyed.value = true;
    isComponentMounted.value = false;

    // 异步清理缓存（不阻塞页面卸载）
    policeOperationHistoryApi.clearCache(props.reportId);

    // 清理数据
    timelineData.value = [];
    total.value = 0;
  });
</script>

<style scoped>
.compact-operation-history {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 工具栏样式 */
.history-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.toolbar-left .ant-radio-group {
  margin-right: 16px;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

/* 紧凑时间轴视图 */
.compact-timeline-view {
  flex: 1;
  overflow-y: auto;
}

.timeline-list {
  position: relative;
  padding-left: 80px;
}

.timeline-list::before {
  content: '';
  position: absolute;
  left: 68px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: #e8e8e8;
}

.timeline-item-compact {
  position: relative;
  margin-bottom: 20px;
  display: flex;
  align-items: flex-start;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px;
  transition: all 0.2s ease;
}

.timeline-item-compact:hover {
  background: #f5f5f5;
  border-color: #d9d9d9;
}

.item-left {
  position: absolute;
  left: -80px;
  top: 12px;
  width: 70px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.item-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 3px solid #fff;
  box-shadow: 0 0 0 2px #e8e8e8;
  z-index: 1;
}

.dot-update { background-color: #1890ff; box-shadow: 0 0 0 2px #1890ff; }
.dot-create { background-color: #52c41a; box-shadow: 0 0 0 2px #52c41a; }
.dot-delete { background-color: #ff4d4f; box-shadow: 0 0 0 2px #ff4d4f; }
.dot-other { background-color: #d9d9d9; box-shadow: 0 0 0 2px #d9d9d9; }

.item-time-compact {
  font-size: 11px;
  color: #666;
  font-weight: 500;
  text-align: center;
  white-space: nowrap;
}

.item-content {
  flex: 1;
  min-width: 0;
}

/* 核心信息行 */
.item-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8e8e8;
}

.item-user {
  font-weight: 600;
  color: #1890ff;
  background: #e6f7ff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 13px;
}

.item-action {
  font-weight: 500;
  color: #262626;
  background: #f6f6f6;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 13px;
}

.item-full-time {
  font-size: 12px;
  color: #8c8c8c;
  margin-left: auto;
  font-family: 'Courier New', monospace;
}

/* 变更详情 */
.item-changes {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.change-detail {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 6px 0;
}

.field-label {
  min-width: 80px;
  font-weight: 500;
  color: #595959;
  font-size: 13px;
  flex-shrink: 0;
}

.value-change {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.old-value {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  color: #cf1322;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  text-decoration: line-through;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.change-arrow {
  color: #8c8c8c;
  font-weight: bold;
  font-size: 14px;
  flex-shrink: 0;
}

.new-value {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  color: #389e0d;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-description {
  color: #666;
  font-size: 13px;
  font-style: italic;
}

/* 表格视图 */
.table-view {
  flex: 1;
}

.table-changes {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.change-tag-table {
  margin: 0 !important;
  font-size: 11px;
}

/* 卡片视图 */
.card-view {
  flex: 1;
  overflow-y: auto;
}

.card-list {
  display: grid;
  gap: 12px;
}

.operation-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  transition: all 0.2s;
}

.operation-card:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.user-avatar {
  background-color: #1890ff;
}

.card-info {
  flex: 1;
}

.card-user {
  font-weight: 500;
  font-size: 14px;
  color: #262626;
}

.card-time {
  font-size: 12px;
  color: #8c8c8c;
}

.card-content {
  margin-top: 8px;
}

.changes-grid {
  display: grid;
  gap: 8px;
}

.change-item-card {
  background: #fafafa;
  border-radius: 4px;
  padding: 6px 8px;
  font-size: 12px;
}

.field-name {
  font-weight: 500;
  color: #595959;
  margin-bottom: 2px;
}

.field-change {
  display: flex;
  align-items: center;
  gap: 4px;
}

.old-val {
  color: #ff7875;
  text-decoration: line-through;
}

.arrow {
  color: #8c8c8c;
}

.new-val {
  color: #73d13d;
  font-weight: 500;
}

/* 分页 */
.pagination-container {
  padding: 16px 0;
  text-align: center;
  border-top: 1px solid #f0f0f0;
  margin-top: auto;
}

.timeline-pagination {
  justify-content: center;
}

.timeline-pagination .ant-pagination-total-text {
  margin-right: 16px;
}

/* 响应式 */
@media (max-width: 768px) {
  .history-toolbar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .toolbar-left,
  .toolbar-right {
    justify-content: center;
  }
}
</style>