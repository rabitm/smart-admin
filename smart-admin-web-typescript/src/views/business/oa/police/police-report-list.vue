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

    <!-- 高性能虚拟滚动表格 -->
    <VirtualScrollTable
      :data="enhancedTableData"
      :columns="virtualTableColumns"
      :container-height="600"
      :row-height="50"
      :show-performance-panel="showPerformancePanel"
      :highlight-updates="true"
      @row-click="handleRowClick"
      @row-double-click="handleRowDoubleClick"
      @sort="handleSort"
      ref="virtualTableRef"
    >
      <!-- 警情编号列 -->
      <template #reportNumber="{ record }">
        <a-button
          type="link"
          size="small"
          @click="detail(record.reportId)"
          :disabled="!$privilege('oa:police:query')"
        >
          {{ record.reportNumber }}
        </a-button>
      </template>

      <!-- 处理席位列 -->
      <template #handleSeatCode="{ record }">
        <a-tag v-if="record.handleSeatCode" color="blue">
          {{ record.handleSeatCode }}
        </a-tag>
        <span v-else class="text-gray">-</span>
      </template>

      <!-- 警情类型列 -->
      <template #reportType="{ record }">
        <span>{{ $smartEnumPlugin.getDescByValue('POLICE_REPORT_TYPE_ENUM', record.reportType) }}</span>
      </template>

      <!-- 警情等级列 -->
      <template #reportLevel="{ record }">
        <a-tag :color="getLevelColor(record.reportLevel)">
          {{ $smartEnumPlugin.getDescByValue('POLICE_REPORT_LEVEL_ENUM', record.reportLevel) }}
        </a-tag>
      </template>

      <!-- 处理状态列 -->
      <template #status="{ record }">
        <a-tag :color="getStatusColor(record.status)">
          {{ $smartEnumPlugin.getDescByValue('POLICE_REPORT_STATUS_ENUM', record.status) }}
        </a-tag>
      </template>

      <!-- 警情描述列 -->
      <template #description="{ record }">
        <span class="text-ellipsis" :title="record.description">{{ record.description }}</span>
      </template>

      <!-- 操作列 -->
      <template #action="{ record }">
        <div class="smart-table-operate">
          <a-button @click="detail(record.reportId)" size="small" v-privilege="'oa:police:query'" type="link">详情</a-button>
          <a-button @click="update(record.reportId)" size="small" v-privilege="'oa:police:update'" type="link">编辑</a-button>
          <a-button @click="confirmDelete(record.reportId)" size="small" danger v-privilege="'oa:police:delete'" type="link">删除</a-button>
        </div>
      </template>
    </VirtualScrollTable>

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
  import { reactive, ref, computed, onMounted, onUnmounted } from 'vue';
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
  import VirtualScrollTable from '/@/components/business/police/virtual-scroll-table.vue';
  import { policeListUpdateManager } from '/@/utils/police-list-update-manager';
  import type { PoliceReportData } from '/@/utils/police-list-update-manager';

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

  // 高性能虚拟表格相关
  const virtualTableRef = ref();
  const showPerformancePanel = ref(true);

  // 虚拟表格列配置
  const virtualTableColumns = computed(() => [
    {
      key: 'reportNumber',
      title: '警情编号',
      width: 150,
      sortable: true,
      slot: 'reportNumber'
    },
    {
      key: 'reportType',
      title: '警情类型',
      width: 100,
      sortable: true,
      slot: 'reportType'
    },
    {
      key: 'reportLevel',
      title: '警情等级',
      width: 80,
      sortable: true,
      slot: 'reportLevel'
    },
    {
      key: 'reporterName',
      title: '报警人姓名',
      width: 100,
      sortable: true
    },
    {
      key: 'reporterPhone',
      title: '报警人电话',
      width: 120
    },
    {
      key: 'reportTime',
      title: '报警时间',
      width: 150,
      sortable: true
    },
    {
      key: 'handleSeatCode',
      title: '处理席位',
      width: 100,
      slot: 'handleSeatCode'
    },
    {
      key: 'handlerName',
      title: '处理人员',
      width: 100
    },
    {
      key: 'incidentLocation',
      title: '事发地点',
      width: 200
    },
    {
      key: 'description',
      title: '警情描述',
      width: 200,
      slot: 'description'
    },
    {
      key: 'status',
      title: '处理状态',
      width: 90,
      sortable: true,
      slot: 'status'
    },
    {
      key: 'createUserName',
      title: '创建人',
      width: 100
    },
    {
      key: 'createTime',
      title: '创建时间',
      width: 150,
      sortable: true
    },
    {
      key: 'action',
      title: '操作',
      width: 120,
      slot: 'action'
    }
  ]);

  // 增强的表格数据（转换为高性能格式）
  const enhancedTableData = computed(() => {
    return tableData.value.map((item: any) => ({
      id: item.reportId || item.id, // 兼容两种ID字段
      reportId: item.reportId || item.id,
      reportNumber: item.reportNumber || '',
      reportType: item.reportType,
      reportLevel: item.reportLevel,
      status: item.status,
      reporterName: item.reporterName || '',
      reporterPhone: item.reporterPhone || '',
      reporterIdCard: item.reporterIdCard || '',
      incidentLocation: item.incidentLocation || '',
      description: item.description || item.incidentDescription || '',
      incidentDescription: item.description || item.incidentDescription || '',
      reportTime: item.reportTime,
      handlerName: item.handlerName || '',
      handlerId: item.handlerId,
      handleSeatCode: item.handleSeatCode || '',
      handleResult: item.handleResult || '',
      handleTime: item.handleTime,
      attachments: item.attachments || '',
      remark: item.remark || '',
      createUserId: item.createUserId,
      createUserName: item.createUserName || '',
      createTime: item.createTime,
      updateTime: item.updateTime,
      updatedAt: item.updatedAt || Date.now(),
      version: item.version || 1,
      updateFields: new Set<string>()
    }));
  });

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

  // --------------------------- 高性能虚拟表格事件处理 ---------------------------

  // 虚拟表格事件处理
  const handleRowClick = (row: PoliceReportData) => {
    console.log('🖱️ [VirtualTable] 行点击:', row.reportNumber);
  };

  const handleRowDoubleClick = (row: PoliceReportData) => {
    console.log('🖱️ [VirtualTable] 行双击，进入编辑:', row.reportNumber);
    update(row.reportId);
  };

  const handleSort = (field: string, order: string) => {
    console.log('🔄 [VirtualTable] 排序:', field, order);
    // 在这里实现排序逻辑
    tableData.value.sort((a: any, b: any) => {
      const aVal = a[field];
      const bVal = b[field];

      if (order === 'ascend') {
        return aVal > bVal ? 1 : -1;
      } else {
        return aVal < bVal ? 1 : -1;
      }
    });
  };

  // --------------------------- WebSocket高性能实时同步 ---------------------------

  let wsClient: any = null;

  // 初始化高性能列表更新管理器
  const initializeListUpdateManager = () => {
    // 监听数据刷新请求（降级模式）
    policeListUpdateManager.on('data_refresh_required', () => {
      console.log('🔄 [高性能列表] 收到数据刷新请求');
      ajaxQuery();
    });

    // 监听实时更新事件
    policeListUpdateManager.on('field_update', (data: any) => {
      console.log('📝 [高性能列表] 收到字段更新:', data);
      handleHighPerformanceFieldUpdate(data);
    });

    policeListUpdateManager.on('record_insert', (data: any) => {
      console.log('➕ [高性能列表] 收到记录新增:', data);
      handleHighPerformanceRecordInsert(data);
    });

    policeListUpdateManager.on('record_delete', (data: any) => {
      console.log('🗑️ [高性能列表] 收到记录删除:', data);
      handleHighPerformanceRecordDelete(data);
    });

    policeListUpdateManager.on('batch_update', (data: any) => {
      console.log('📦 [高性能列表] 收到批量更新:', data);
      handleHighPerformanceBatchUpdate(data);
    });

    // 初始化数据到管理器
    if (enhancedTableData.value.length > 0) {
      policeListUpdateManager.initializeData(enhancedTableData.value);
    }

    console.log('🚀 [高性能列表] 更新管理器初始化完成');
  };

  // 处理高性能字段更新
  const handleHighPerformanceFieldUpdate = (data: any) => {
    const { reportId, fieldName, fieldValue, userName } = data;

    // 查找目标记录
    const targetIndex = tableData.value.findIndex((item: any) => item.reportId === reportId);
    if (targetIndex >= 0) {
      const targetRecord = tableData.value[targetIndex];

      // 更新字段值
      targetRecord[fieldName] = fieldValue;
      targetRecord.updatedAt = Date.now();
      targetRecord.version = (targetRecord.version || 1) + 1;

      // 标记更新的字段
      if (!targetRecord.updateFields) {
        targetRecord.updateFields = new Set();
      }
      targetRecord.updateFields.add(fieldName);

      // 显示更新通知（防抖）
      if (userName && userName !== '当前用户') {
        const reportNumber = targetRecord.reportNumber || `#${reportId}`;
        message.info(`${userName} 更新了警情「${reportNumber}」的${getFieldDisplayName(fieldName)}`, 2);
      }

      console.log(`⚡ [高性能更新] 字段 ${fieldName} 已更新，记录ID: ${reportId}`);
    }
  };

  // 处理高性能记录插入
  const handleHighPerformanceRecordInsert = (data: any) => {
    const newRecord = {
      ...data.record,
      updatedAt: Date.now(),
      version: 1,
      updateFields: new Set(['*']) // 标记为全新记录
    };

    tableData.value.unshift(newRecord);
    total.value++;

    message.info(`新增警情「${newRecord.reportNumber}」`, 3);
    console.log('✨ [高性能更新] 新记录已添加:', newRecord.reportNumber);
  };

  // 处理高性能记录删除
  const handleHighPerformanceRecordDelete = (data: any) => {
    const { reportId } = data;
    const targetIndex = tableData.value.findIndex((item: any) => item.reportId === reportId);

    if (targetIndex >= 0) {
      const deletedRecord = tableData.value[targetIndex];
      tableData.value.splice(targetIndex, 1);
      total.value--;

      message.warning(`警情「${deletedRecord.reportNumber}」已被删除`);
      console.log('🗑️ [高性能更新] 记录已删除:', deletedRecord.reportNumber);
    }
  };

  // 处理高性能批量更新
  const handleHighPerformanceBatchUpdate = (data: any) => {
    const { updates } = data;
    let updateCount = 0;

    updates.forEach((update: any) => {
      const { reportId, fieldName, fieldValue } = update;
      const targetIndex = tableData.value.findIndex((item: any) => item.reportId === reportId);

      if (targetIndex >= 0) {
        const targetRecord = tableData.value[targetIndex];
        targetRecord[fieldName] = fieldValue;
        targetRecord.updatedAt = Date.now();
        targetRecord.version++;

        if (!targetRecord.updateFields) {
          targetRecord.updateFields = new Set();
        }
        targetRecord.updateFields.add(fieldName);
        updateCount++;
      }
    });

    if (updateCount > 0) {
      message.info(`批量更新了 ${updateCount} 条记录`, 2);
      console.log(`📦 [高性能批量更新] 已处理 ${updateCount} 条更新`);
    }
  };

  // 字段显示名称映射
  const getFieldDisplayName = (fieldName: string): string => {
    const fieldDisplayNames: Record<string, string> = {
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
    return fieldDisplayNames[fieldName] || fieldName;
  };

  onMounted(() => {
    ajaxQuery();

    // 直接初始化WebSocket监听（简化版本）
    try {
      fallbackToLegacyWebSocket();
      console.log('🚀 [警情列表] WebSocket实时同步已启动');
    } catch (error) {
      console.error('❌ [警情列表] 启动WebSocket实时同步失败:', error);
    }

    // 初始化高性能列表更新管理器（作为补充）
    try {
      initializeListUpdateManager();
      console.log('🚀 [警情列表] 高性能更新系统已启动');
    } catch (error) {
      console.error('❌ [警情列表] 启动高性能更新系统失败:', error);
    }
  });

  onUnmounted(() => {
    // 清理高性能列表更新管理器
    try {
      policeListUpdateManager.destroy();
      console.log('🔥 [警情列表] 高性能更新系统已清理');
    } catch (error) {
      console.error('清理高性能更新系统失败:', error);
    }

    // 清理WebSocket监听（如果有）
    if (wsClient) {
      // 清理模块消息监听器
      wsClient.offModuleMessage('police', 'LIST_UPDATE');
      wsClient.offModuleMessage('police', 'BATCH_UPDATE');
      // 清理传统监听器
      wsClient.off('POLICE_CASE_UPDATE');
      wsClient.off('POLICE_CASE_FIELD_SYNC');
      wsClient.off('POLICE_LIST_REFRESH');
      console.log('🔥 [警情列表] WebSocket监听已清理');
    }
  });

  // 降级到传统WebSocket监听（兼容性保障）
  const fallbackToLegacyWebSocket = () => {
    try {
      wsClient = getWebSocketClient();

      // 监听高性能列表更新消息（修复：使用正确的模块消息监听方式）
      wsClient.onModuleMessage('police', 'LIST_UPDATE', (message: any) => {
        console.log('📡 [警情列表] 收到列表更新消息:', message);
        // 修复：使用message.data，因为业务数据在data字段中
        if (message && message.data) {
          handleWebSocketListUpdate(message.data);
        }
      });

      // 监听批量更新消息（修复：使用正确的模块消息监听方式）
      wsClient.onModuleMessage('police', 'BATCH_UPDATE', (message: any) => {
        console.log('📦 [警情列表] 收到批量更新消息:', message);
        // 修复：使用message.data，因为业务数据在data字段中
        if (message && message.data && message.data.updates) {
          handleWebSocketBatchUpdate(message.data);
        }
      });

      // 监听传统的警情更新消息（兼容性）
      wsClient.on('POLICE_CASE_UPDATE', (data: any) => {
        console.log('📡 [Legacy WebSocket] 收到更新:', data);
        // 转换为高性能格式处理
        if (data.type === 'POLICE_CASE_UPDATE') {
          handleHighPerformanceFieldUpdate({
            reportId: data.policeCaseId,
            fieldName: 'status', // 假设是状态更新
            fieldValue: data.data,
            userName: data.userName || '其他用户'
          });
        }
      });

      // 发送订阅列表更新请求
      const subscribeMessage = {
        module: 'police',
        type: 'SUBSCRIBE_LIST_UPDATES',
        data: {
          timestamp: Date.now()
        }
      };
      wsClient.send(subscribeMessage);

      console.log('⚠️ [警情列表] 已降级到传统WebSocket模式并发送订阅请求');
    } catch (error) {
      console.error('❌ [警情列表] 传统WebSocket初始化也失败:', error);
    }
  };

  // 处理WebSocket列表更新消息
  const handleWebSocketListUpdate = (data: any) => {
    console.log('🔍 [WebSocket列表更新] 处理消息:', {
      type: data.type,
      reportId: data.reportId,
      data: data.data,
      hasData: !!data.data,
      dataKeys: data.data ? Object.keys(data.data) : [],
      fields: data.fields,
      version: data.version,
      timestamp: data.timestamp,
      message: data
    });

    switch (data.type) {
      case 'UPDATE':
        if (data.reportId && data.data) {
          const fieldName = Object.keys(data.data)[0] || 'unknown';
          const fieldValue = Object.values(data.data)[0];

          console.log('⚡ [列表更新] 处理字段更新:', {
            reportId: data.reportId,
            fieldName,
            fieldValue,
            userName: data.userName || '其他用户'
          });

          handleHighPerformanceFieldUpdate({
            reportId: data.reportId,
            fieldName,
            fieldValue,
            userName: data.userName || '其他用户'
          });
        } else {
          console.warn('⚠️ [列表更新] UPDATE消息缺少必要数据:', data);
        }
        break;
      case 'INSERT':
        if (data.reportId && data.data) {
          console.log('✨ [列表更新] 处理记录插入:', data);
          handleHighPerformanceRecordInsert({
            record: data.data
          });
        } else {
          console.warn('⚠️ [列表更新] INSERT消息缺少必要数据:', data);
        }
        break;
      case 'DELETE':
        if (data.reportId) {
          console.log('🗑️ [列表更新] 处理记录删除:', data.reportId);
          handleHighPerformanceRecordDelete({
            reportId: data.reportId
          });
        } else {
          console.warn('⚠️ [列表更新] DELETE消息缺少reportId:', data);
        }
        break;
      case 'BATCH':
        console.log('📦 [列表更新] 处理批量更新:', data);
        handleWebSocketBatchUpdate(data);
        break;
      default:
        console.warn('⚠️ [列表更新] 未知消息类型:', data.type, data);
    }
  };

  // 处理WebSocket批量更新消息
  const handleWebSocketBatchUpdate = (data: any) => {
    if (data.updates && Array.isArray(data.updates)) {
      // 将每个更新中的多个字段展开为单独的更新项
      const expandedUpdates: any[] = [];

      data.updates.forEach((update: any) => {
        if (update.data && typeof update.data === 'object') {
          // 为每个字段创建一个单独的更新
          Object.entries(update.data).forEach(([fieldName, fieldValue]) => {
            expandedUpdates.push({
              reportId: update.reportId,
              fieldName: fieldName,
              fieldValue: fieldValue
            });
          });
        }
      });

      console.log('📦 [批量更新] 展开后的更新项:', expandedUpdates);

      if (expandedUpdates.length > 0) {
        handleHighPerformanceBatchUpdate({
          updates: expandedUpdates
        });
      }
    }
  };
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