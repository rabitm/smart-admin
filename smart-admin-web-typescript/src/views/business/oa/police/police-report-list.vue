<!--
  * 警情录入列表
  *
  * @Author:    Claude Code Assistant
  * @Date:      2025-09-18
  * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
-->
<template>
  <a-form class="smart-query-form" v-privilege="'oa:police:query'">
    <a-row class="smart-query-form-row">
      <a-form-item label="警情编号" class="smart-query-form-item">
        <a-input style="width: 200px" v-model:value="queryForm.reportNumber" placeholder="请输入警情编号" />
      </a-form-item>

      <a-form-item label="警情类型" class="smart-query-form-item">
        <a-select style="width: 150px" v-model:value="queryForm.reportType" placeholder="请选择警情类型" allow-clear>
          <a-select-option v-for="(item, key) in POLICE_REPORT_TYPE_ENUM" :key="key" :value="item.value">
            {{ item.desc }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="警情等级" class="smart-query-form-item">
        <a-select style="width: 120px" v-model:value="queryForm.reportLevel" placeholder="请选择警情等级" allow-clear>
          <a-select-option v-for="(item, key) in POLICE_REPORT_LEVEL_ENUM" :key="key" :value="item.value">
            {{ item.desc }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="处理状态" class="smart-query-form-item">
        <a-select style="width: 120px" v-model:value="queryForm.status" placeholder="请选择处理状态" allow-clear>
          <a-select-option v-for="(item, key) in POLICE_REPORT_STATUS_ENUM" :key="key" :value="item.value">
            {{ item.desc }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="报警人姓名" class="smart-query-form-item">
        <a-input style="width: 150px" v-model:value="queryForm.reporterName" placeholder="请输入报警人姓名" />
      </a-form-item>

      <a-form-item label="报警人电话" class="smart-query-form-item">
        <a-input style="width: 150px" v-model:value="queryForm.reporterPhone" placeholder="请输入报警人电话" />
      </a-form-item>

      <a-form-item label="处理人员" class="smart-query-form-item">
        <a-input style="width: 120px" v-model:value="queryForm.handlerName" placeholder="请输入处理人员" />
      </a-form-item>

      <a-form-item label="处理席位" class="smart-query-form-item">
        <a-input style="width: 120px" v-model:value="queryForm.handleSeatCode" placeholder="请输入席位编码" />
      </a-form-item>

      <a-form-item label="事发地点" class="smart-query-form-item">
        <a-input style="width: 200px" v-model:value="queryForm.incidentLocation" placeholder="请输入事发地点" />
      </a-form-item>

      <a-form-item label="报警时间" class="smart-query-form-item">
        <a-space direction="vertical" :size="12">
          <a-range-picker v-model:value="reportTimeRange" :presets="defaultTimeRanges" @change="reportTimeChange" />
        </a-space>
      </a-form-item>

      <a-form-item label="创建时间" class="smart-query-form-item">
        <a-space direction="vertical" :size="12">
          <a-range-picker v-model:value="createTimeRange" :presets="defaultTimeRanges" @change="createTimeChange" />
        </a-space>
      </a-form-item>

      <a-form-item class="smart-query-form-item smart-margin-left10">
        <a-button-group>
          <a-button type="primary" @click="onSearch">
            <template #icon>
              <SearchOutlined />
            </template>
            查询
          </a-button>
          <a-button @click="resetQuery">
            <template #icon>
              <ReloadOutlined />
            </template>
            重置
          </a-button>
        </a-button-group>
      </a-form-item>
    </a-row>
  </a-form>

  <a-card size="small" :bordered="false" :hoverable="true">
    <a-row class="smart-table-btn-block">
      <div class="smart-table-operate-block">
        <a-button @click="add()" v-privilege="'oa:police:add'" type="primary">
          <template #icon>
            <PlusOutlined />
          </template>
          新增警情
        </a-button>
      </div>
      <div class="smart-table-setting-block">
        <TableOperator v-model="columns" :tableId="TABLE_ID_CONST.BUSINESS.OA.POLICE_REPORT" :refresh="ajaxQuery" />
      </div>
    </a-row>

    <a-table
      :scroll="{ x: 1800 }"
      size="small"
      :dataSource="tableData"
      :columns="columns"
      rowKey="reportId"
      :pagination="false"
      :loading="tableLoading"
      bordered
    >
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'reportNumber'">
          <a-button type="link" @click="detail(record.reportId)" :disabled="!$privilege('oa:police:query')">
            {{ record.reportNumber }}
          </a-button>
        </template>

        <template v-if="column.dataIndex === 'handleSeatCode'">
          <a-tag v-if="record.handleSeatCode" color="blue">
            {{ record.handleSeatCode }}
          </a-tag>
          <span v-else class="text-gray">-</span>
        </template>
        <template v-if="column.dataIndex === 'reportType'">
          <span>{{ $smartEnumPlugin.getDescByValue('POLICE_REPORT_TYPE_ENUM', text) }}</span>
        </template>
        <template v-if="column.dataIndex === 'reportLevel'">
          <a-tag :color="getLevelColor(text)">
            {{ $smartEnumPlugin.getDescByValue('POLICE_REPORT_LEVEL_ENUM', text) }}
          </a-tag>
        </template>
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="getStatusColor(text)">
            {{ $smartEnumPlugin.getDescByValue('POLICE_REPORT_STATUS_ENUM', text) }}
          </a-tag>
        </template>
        <template v-if="column.dataIndex === 'description'">
          <span class="text-ellipsis" :title="text">{{ text }}</span>
        </template>
        <template v-if="column.dataIndex === 'action'">
          <div class="smart-table-operate">
            <a-button @click="update(record.reportId)" size="small" v-privilege="'oa:police:update'" type="link">编辑</a-button>
            <a-button @click="confirmDelete(record.reportId)" size="small" danger v-privilege="'oa:police:delete'" type="link">删除</a-button>
          </div>
        </template>
      </template>
    </a-table>

    <div class="smart-query-table-page">
      <a-pagination
        showSizeChanger
        showQuickJumper
        show-less-items
        :pageSizeOptions="PAGE_SIZE_OPTIONS"
        :defaultPageSize="queryForm.pageSize"
        v-model:current="queryForm.pageNum"
        v-model:pageSize="queryForm.pageSize"
        :total="total"
        @change="ajaxQuery"
        :show-total="(total) => `共${total}条`"
      />
    </div>

    <PoliceReportOperate ref="operateRef" @refresh="ajaxQuery" />
  </a-card>
</template>

<script setup lang="ts">
  import { reactive, ref, onMounted, onUnmounted } from 'vue';
  import { message, Modal } from 'ant-design-vue';
  import { SmartLoading } from '/@/components/framework/smart-loading';
  import { policeReportApi } from '/@/api/business/oa/police-report-api';
  import { PAGE_SIZE, PAGE_SIZE_OPTIONS } from '/@/constants/common-const';
  import { useRouter } from 'vue-router';
  import PoliceReportOperate from './components/police-report-operate-modal.vue';
  import { smartSentry } from '/@/lib/smart-sentry';
  import { defaultTimeRanges } from '/@/lib/default-time-ranges';
  import TableOperator from '/@/components/support/table-operator/index.vue';
  import { TABLE_ID_CONST } from '/@/constants/support/table-id-const';
  import {
    POLICE_REPORT_TYPE_ENUM,
    POLICE_REPORT_LEVEL_ENUM,
    POLICE_REPORT_STATUS_ENUM
  } from '/@/constants/business/oa/police-report-const';
  import { getWebSocketClient } from '/@/utils/websocket-manager';

  // --------------------------- 警情录入表格 列 ---------------------------

  const columns = ref([
    {
      title: '警情编号',
      dataIndex: 'reportNumber',
      width: 150,
      fixed: 'left',
    },
    {
      title: '警情类型',
      dataIndex: 'reportType',
      width: 100,
    },
    {
      title: '警情等级',
      dataIndex: 'reportLevel',
      width: 80,
    },
    {
      title: '报警人姓名',
      dataIndex: 'reporterName',
      width: 100,
    },
    {
      title: '报警人电话',
      dataIndex: 'reporterPhone',
      width: 120,
    },
    {
      title: '报警时间',
      dataIndex: 'reportTime',
      width: 150,
    },
    {
      title: '处理席位',
      dataIndex: 'handleSeatCode',
      width: 100,
    },
    {
      title: '处理人员',
      dataIndex: 'handlerName',
      width: 100,
    },
    {
      title: '事发地点',
      dataIndex: 'incidentLocation',
      width: 200,
      ellipsis: true,
    },
    {
      title: '警情描述',
      dataIndex: 'description',
      width: 200,
      ellipsis: true,
    },
    {
      title: '处理状态',
      dataIndex: 'status',
      width: 90,
    },
    {
      title: '处理人员',
      dataIndex: 'handlerName',
      width: 100,
    },
    {
      title: '创建人',
      dataIndex: 'createUserName',
      width: 100,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 150,
    },
    {
      title: '操作',
      dataIndex: 'action',
      fixed: 'right',
      width: 120,
    },
  ]);

  // --------------------------- 查询 ---------------------------

  const queryFormState = {
    reportNumber: '',
    reportType: null,
    reportLevel: null,
    status: null,
    reporterName: '',
    reporterPhone: '',
    handlerName: '',
    handleSeatCode: '',
    incidentLocation: '',
    reportTimeStart: null,
    reportTimeEnd: null,
    createTimeStart: null,
    createTimeEnd: null,
    pageNum: 1,
    pageSize: PAGE_SIZE,
    searchCount: true,
  };
  const queryForm = reactive({ ...queryFormState });
  const tableLoading = ref(false);
  const tableData = ref([]);
  const total = ref(0);

  // 日期选择
  let reportTimeRange = ref();
  let createTimeRange = ref();

  function reportTimeChange(dates, dateStrings) {
    queryForm.reportTimeStart = dateStrings[0];
    queryForm.reportTimeEnd = dateStrings[1];
  }

  function createTimeChange(dates, dateStrings) {
    queryForm.createTimeStart = dateStrings[0];
    queryForm.createTimeEnd = dateStrings[1];
  }

  function onSearch() {
    queryForm.pageNum = 1;
    ajaxQuery();
  }

  function resetQuery() {
    reportTimeRange.value = [];
    createTimeRange.value = [];
    Object.assign(queryForm, queryFormState);
    ajaxQuery();
  }

  async function ajaxQuery() {
    try {
      tableLoading.value = true;
      let responseModel = await policeReportApi.pageQuery(queryForm);
      const list = responseModel.data.list;
      total.value = responseModel.data.total;
      tableData.value = list;
    } catch (e) {
      smartSentry.captureError(e);
    } finally {
      tableLoading.value = false;
    }
  }

  // --------------------------- 删除 ---------------------------

  function confirmDelete(reportId) {
    Modal.confirm({
      title: '确定要删除吗？',
      content: '删除后，该警情信息将不可恢复',
      okText: '删除',
      okType: 'danger',
      onOk() {
        del(reportId);
      },
      cancelText: '取消',
      onCancel() {},
    });
  }

  async function del(reportId) {
    try {
      SmartLoading.show();
      await policeReportApi.deletePoliceReport(reportId);
      message.success('删除成功');
      ajaxQuery();
    } catch (e) {
      smartSentry.captureError(e);
    } finally {
      SmartLoading.hide();
    }
  }

  // --------------------------- 增加、修改、详情 ---------------------------

  let router = useRouter();
  const operateRef = ref();

  function add() {
    router.push({ path: '/oa/police/emergency-intake' });
  }

  function update(reportId) {
    router.push({ path: '/oa/police/emergency-intake', query: { reportId: reportId, mode: 'edit' } });
  }

  function detail(reportId) {
    router.push({ path: '/oa/police/report-detail', query: { reportId: reportId } });
  }

  // --------------------------- 状态和等级颜色 ---------------------------

  function getStatusColor(status) {
    const colorMap = {
      1: 'orange',    // 待处理
      2: 'blue',      // 处理中
      3: 'green',     // 已完成
      4: 'default',   // 已关闭
    };
    return colorMap[status] || 'default';
  }

  function getLevelColor(level) {
    const colorMap = {
      1: 'red',       // 紧急
      2: 'orange',    // 高
      3: 'blue',      // 中
      4: 'default',   // 低
    };
    return colorMap[level] || 'default';
  }

  // --------------------------- WebSocket实时同步 ---------------------------

  let wsClient: any = null;

  // 处理警情更新消息
  function handlePoliceCaseUpdate(message: SeatSyncMessage) {
    if (message.type === 'POLICE_CASE_UPDATE' && message.data) {
      console.log('收到警情更新消息:', message);

      // 查找列表中的对应项并更新
      const reportList = tableData.value;
      const index = reportList.findIndex((item: any) => item.reportId === message.policeCaseId);

      if (index >= 0) {
        // 更新列表中的数据
        Object.assign(reportList[index], message.data);
        console.log('已更新列表中的警情数据:', reportList[index]);

        // 显示更新提示
        const reportNumber = reportList[index].reportNumber || `#${message.policeCaseId}`;
        message.success(`警情「${reportNumber}」已被其他用户更新`);

        // 添加更新高亮效果
        highlightTableRow(message.policeCaseId);
      } else {
        // 如果是新增的警情，重新加载列表
        console.log('收到新增警情，刷新列表');
        ajaxQuery();
        message.info('列表已更新，有新的警情记录');
      }
    }
  }

  // 处理字段实时同步消息
  function handleFieldSync(message: SeatSyncMessage) {
    if (message.type === 'POLICE_CASE_FIELD_SYNC' && message.data) {
      console.log('收到字段同步消息:', message);

      const { fieldName, fieldValue } = message.data;
      const reportList = tableData.value;
      const index = reportList.findIndex((item: any) => item.reportId === message.policeCaseId);

      if (index >= 0) {
        // 更新对应字段
        reportList[index][fieldName] = fieldValue;

        // 字段显示名称映射
        const fieldDisplayNames = {
          reportType: '警情类型',
          reportLevel: '紧急程度',
          reporterName: '报警人姓名',
          reporterPhone: '报警人电话',
          incidentLocation: '事发地点',
          description: '事件描述',
          status: '处理状态',
          handlerName: '处理人',
          handleResult: '处理结果',
          remark: '备注'
        };

        const displayName = fieldDisplayNames[fieldName] || fieldName;
        const reportNumber = reportList[index].reportNumber || `#${message.policeCaseId}`;

        // 显示实时编辑提示
        message.info(`「${reportNumber}」的${displayName}正在被其他用户编辑`, 2);

        // 添加列表行闪烁效果
        highlightTableRow(message.policeCaseId);
      }
    }
  }

  // 高亮表格行
  function highlightTableRow(reportId: number) {
    // 添加高亮效果
    const tableRow = document.querySelector(`[data-row-key="${reportId}"]`);
    if (tableRow) {
      tableRow.classList.add('realtime-update-row');
      setTimeout(() => {
        tableRow.classList.remove('realtime-update-row');
      }, 3000);
    }
  }

  // 处理列表刷新消息
  function handleListRefresh(message: SeatSyncMessage) {
    if (message.type === 'POLICE_LIST_REFRESH') {
      console.log('收到列表刷新消息:', message);

      // 重新加载列表数据
      ajaxQuery();

      // 显示操作提示
      if (message.message) {
        message.info(message.message);
      }
    }
  }

  // 处理警情锁定消息
  function handlePoliceCaseLock(message: SeatSyncMessage) {
    if (message.type === 'POLICE_CASE_LOCK') {
      console.log('收到警情锁定消息:', message);
      message.info(`警情正在被 ${message.message} 编辑中`);
    }
  }

  // 处理警情解锁消息
  function handlePoliceCaseUnlock(message: SeatSyncMessage) {
    if (message.type === 'POLICE_CASE_UNLOCK') {
      console.log('收到警情解锁消息:', message);
    }
  }

  onMounted(() => {
    ajaxQuery();

    // 初始化WebSocket监听
    try {
      wsClient = getWebSocketClient();
      wsClient.on('POLICE_CASE_UPDATE', handlePoliceCaseUpdate);
      wsClient.on('POLICE_CASE_FIELD_SYNC', handleFieldSync);
      wsClient.on('POLICE_LIST_REFRESH', handleListRefresh);
      wsClient.on('POLICE_CASE_LOCK', handlePoliceCaseLock);
      wsClient.on('POLICE_CASE_UNLOCK', handlePoliceCaseUnlock);
      console.log('警情列表页面WebSocket监听已启动');
    } catch (error) {
      console.error('启动WebSocket监听失败:', error);
    }
  });

  onUnmounted(() => {
    // 清理WebSocket监听
    if (wsClient) {
      wsClient.off('POLICE_CASE_UPDATE', handlePoliceCaseUpdate);
      wsClient.off('POLICE_CASE_FIELD_SYNC', handleFieldSync);
      wsClient.off('POLICE_LIST_REFRESH', handleListRefresh);
      wsClient.off('POLICE_CASE_LOCK', handlePoliceCaseLock);
      wsClient.off('POLICE_CASE_UNLOCK', handlePoliceCaseUnlock);
      console.log('警情列表页面WebSocket监听已清理');
    }
  });
</script>

<style scoped>
.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
  display: inline-block;
}

/* 实时更新表格行高亮效果 */
:deep(.realtime-update-row) {
  background: linear-gradient(90deg, rgba(24, 144, 255, 0.1), rgba(24, 144, 255, 0.05), rgba(24, 144, 255, 0.1));
  background-size: 200% 100%;
  animation: realtimeRowHighlight 2s ease-in-out;
  position: relative;
}

:deep(.realtime-update-row::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(to bottom, #1890ff, #40a9ff);
  border-radius: 0 2px 2px 0;
}

:deep(.realtime-update-row td) {
  position: relative;
  z-index: 1;
}

@keyframes realtimeRowHighlight {
  0% {
    background-position: -100% 0;
    box-shadow: 0 0 0 rgba(24, 144, 255, 0.3);
  }
  50% {
    background-position: 100% 0;
    box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
  }
  100% {
    background-position: 200% 0;
    box-shadow: 0 0 0 rgba(24, 144, 255, 0.3);
  }
}

/* 新增记录高亮效果 */
:deep(.new-record-row) {
  background: linear-gradient(90deg, rgba(82, 196, 26, 0.15), rgba(82, 196, 26, 0.08), rgba(82, 196, 26, 0.15));
  animation: newRecordPulse 3s ease-in-out;
}

:deep(.new-record-row::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(to bottom, #52c41a, #73d13d);
  border-radius: 0 2px 2px 0;
}

@keyframes newRecordPulse {
  0%, 100% {
    background-color: rgba(82, 196, 26, 0.05);
  }
  50% {
    background-color: rgba(82, 196, 26, 0.15);
  }
}

/* 表格行状态指示器 */
:deep(.ant-table-tbody > tr.editing-indicator) {
  border-left: 3px solid #faad14;
  background: rgba(250, 173, 20, 0.05);
}

:deep(.ant-table-tbody > tr.locked-indicator) {
  border-left: 3px solid #ff4d4f;
  background: rgba(255, 77, 79, 0.05);
}

/* 优化表格整体样式 */
:deep(.ant-table) {
  font-size: 13px;
}

:deep(.ant-table-thead > tr > th) {
  background: #fafafa;
  font-weight: 600;
  color: #262626;
  border-bottom: 2px solid #f0f0f0;
}

:deep(.ant-table-tbody > tr:hover > td) {
  background-color: #e6f7ff;
}

/* 状态标签样式 */
.status-tag {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.level-tag {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

/* 紧急等级特殊样式 */
:deep(.ant-tag.urgent-level) {
  background: linear-gradient(45deg, #ff4d4f, #ff7875);
  color: white;
  border: none;
  animation: urgentPulse 2s infinite;
}

@keyframes urgentPulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(255, 77, 79, 0.4);
  }
  50% {
    box-shadow: 0 0 0 4px rgba(255, 77, 79, 0.1);
  }
}
</style>