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
  import { reactive, ref, onMounted } from 'vue';
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

  onMounted(ajaxQuery);
</script>

<style scoped>
.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
  display: inline-block;
}
</style>