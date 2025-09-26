<!--
  座位管理列表页面

  @Author: Claude Code Assistant
  @Date: 2025-09-25
  @Copyright: 1024创新实验室
-->
<template>
  <div class="seat-management">
    <div class="smart-query-form">
      <a-form layout="inline" :model="queryForm" class="smart-query-form-row">
        <a-form-item label="座位编号" class="smart-query-form-item">
          <a-input v-model:value="queryForm.seatNumber" placeholder="座位编号" style="width: 150px" />
        </a-form-item>
        <a-form-item label="座位名称" class="smart-query-form-item">
          <a-input v-model:value="queryForm.seatName" placeholder="座位名称" style="width: 150px" />
        </a-form-item>
        <a-form-item label="座位类型" class="smart-query-form-item">
          <a-select v-model:value="queryForm.seatType" placeholder="座位类型" style="width: 120px">
            <a-select-option :value="null">全部</a-select-option>
            <a-select-option :value="1">普通座位</a-select-option>
            <a-select-option :value="2">VIP座位</a-select-option>
            <a-select-option :value="3">无障碍座位</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="座位状态" class="smart-query-form-item">
          <a-select v-model:value="queryForm.seatStatus" placeholder="座位状态" style="width: 120px">
            <a-select-option :value="null">全部</a-select-option>
            <a-select-option :value="1">空闲</a-select-option>
            <a-select-option :value="2">使用中</a-select-option>
            <a-select-option :value="3">预约中</a-select-option>
            <a-select-option :value="4">维护中</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="楼层" class="smart-query-form-item">
          <a-input-number v-model:value="queryForm.floor" placeholder="楼层" style="width: 100px" />
        </a-form-item>
        <a-form-item class="smart-query-form-item">
          <a-button type="primary" @click="queryData">
            <template #icon>
              <SearchOutlined />
            </template>
            查询
          </a-button>
          <a-button @click="resetQuery" class="smart-margin-left10">
            <template #icon>
              <ReloadOutlined />
            </template>
            重置
          </a-button>
        </a-form-item>
      </a-form>
    </div>

    <a-card size="small" :bordered="false" :hoverable="true">
      <a-row class="smart-table-btn-block">
        <div class="smart-table-operate-block">
          <a-button @click="showModal" type="primary" size="small">
            <template #icon>
              <PlusOutlined />
            </template>
            新增座位
          </a-button>
          <a-button @click="confirmBatchDelete" type="danger" size="small" :disabled="selectedRowKeyList.length == 0">
            <template #icon>
              <DeleteOutlined />
            </template>
            批量删除
          </a-button>
        </div>
      </a-row>

      <a-table
        size="small"
        :dataSource="tableData"
        :columns="columns"
        rowKey="seatId"
        :loading="tableLoading"
        :pagination="false"
        :row-selection="{ selectedRowKeys: selectedRowKeyList, onChange: onSelectChange }"
        bordered>

        <template #bodyCell="{ text, record, index, column }">
          <template v-if="column.dataIndex === 'seatType'">
            <a-tag :color="getSeatTypeColor(text)">{{ getSeatTypeName(text) }}</a-tag>
          </template>

          <template v-if="column.dataIndex === 'seatStatus'">
            <a-tag :color="getSeatStatusColor(text)">{{ getSeatStatusName(text) }}</a-tag>
          </template>

          <template v-if="column.dataIndex === 'operate'">
            <div class="smart-table-operate">
              <a-button @click="showModal(record)" type="link" size="small">编辑</a-button>
              <a-button @click="reserveSeat(record)" type="link" size="small" v-if="record.seatStatus === 1">预约</a-button>
              <a-button @click="occupySeat(record)" type="link" size="small" v-if="record.seatStatus === 1 || record.seatStatus === 3">使用</a-button>
              <a-button @click="releaseSeat(record)" type="link" size="small" v-if="record.seatStatus === 2 || record.seatStatus === 3">释放</a-button>
              <a-button @click="deleteSeat(record)" type="link" danger size="small">删除</a-button>
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
          @change="queryData"
          @showSizeChange="queryData"
          :show-total="(total) => `共${total}条`"
        />
      </div>
    </a-card>

    <SeatModal ref="seatModalRef" @reloadList="queryData" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { SearchOutlined, ReloadOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue';
import { seatApi } from '/@/api/business/oa/seat-api';
import { PAGE_SIZE_OPTIONS } from '/@/constants/common-const';
import SeatModal from './components/seat-modal.vue';

// ---------------------------- 表格列定义 ----------------------------
const columns = [
  {
    title: '座位编号',
    dataIndex: 'seatNumber',
    width: 100
  },
  {
    title: '座位名称',
    dataIndex: 'seatName',
    width: 120
  },
  {
    title: '座位类型',
    dataIndex: 'seatType',
    width: 100
  },
  {
    title: '座位状态',
    dataIndex: 'seatStatus',
    width: 100
  },
  {
    title: '区域名称',
    dataIndex: 'areaName',
    width: 120
  },
  {
    title: '楼层',
    dataIndex: 'floor',
    width: 80
  },
  {
    title: '位置描述',
    dataIndex: 'locationDesc',
    width: 150
  },
  {
    title: '当前使用者',
    dataIndex: 'currentUserName',
    width: 120
  },
  {
    title: '预约者',
    dataIndex: 'reservedUserName',
    width: 120
  },
  {
    title: '操作',
    dataIndex: 'operate',
    fixed: 'right',
    width: 200
  }
];

// ---------------------------- 查询相关 ----------------------------
const queryFormState = {
  seatNumber: '',
  seatName: '',
  seatType: null,
  seatStatus: null,
  floor: null,
  pageNum: 1,
  pageSize: 10,
};
const queryForm = reactive({ ...queryFormState });
const tableLoading = ref(false);
const tableData = ref([]);
const total = ref(0);

// 重置查询条件
function resetQuery() {
  let pageSize = queryForm.pageSize;
  Object.assign(queryForm, queryFormState);
  queryForm.pageSize = pageSize;
  queryData();
}

// 查询表格数据
async function queryData() {
  tableLoading.value = true;
  try {
    const res = await seatApi.queryList(queryForm);
    if (res.data) {
      tableData.value = res.data.list;
      total.value = res.data.total;
    }
  } catch (error) {
    message.error('查询失败');
  } finally {
    tableLoading.value = false;
  }
}

// ---------------------------- 表格操作相关 ----------------------------
const selectedRowKeyList = ref([]);

function onSelectChange(selectedRowKeys: any[]) {
  selectedRowKeyList.value = selectedRowKeys;
}

// ---------------------------- 模态框相关 ----------------------------
const seatModalRef = ref();

function showModal(record?: any) {
  seatModalRef.value.showModal(record);
}

// ---------------------------- 座位操作 ----------------------------

// 预约座位
async function reserveSeat(record: any) {
  // 这里可以打开预约模态框，简化处理直接调用API
  try {
    const now = new Date();
    const endTime = new Date(now.getTime() + 2 * 60 * 60 * 1000); // 预约2小时
    await seatApi.reserve({
      seatId: record.seatId,
      userId: 1, // 这里应该从用户store获取
      userName: '当前用户',
      startTime: now,
      endTime: endTime
    });
    message.success('预约成功');
    queryData();
  } catch (error) {
    message.error('预约失败');
  }
}

// 占用座位
async function occupySeat(record: any) {
  try {
    await seatApi.occupy(record.seatId, {
      userId: 1, // 这里应该从用户store获取
      userName: '当前用户'
    });
    message.success('使用座位成功');
    queryData();
  } catch (error) {
    message.error('使用座位失败');
  }
}

// 释放座位
async function releaseSeat(record: any) {
  Modal.confirm({
    title: '提示',
    content: '确定要释放此座位吗？',
    onOk: async () => {
      try {
        await seatApi.release(record.seatId);
        message.success('释放成功');
        queryData();
      } catch (error) {
        message.error('释放失败');
      }
    }
  });
}

// 删除座位
function deleteSeat(record: any) {
  Modal.confirm({
    title: '提示',
    content: '确定要删除此座位吗？',
    onOk: async () => {
      try {
        await seatApi.delete(record.seatId);
        message.success('删除成功');
        queryData();
      } catch (error) {
        message.error('删除失败');
      }
    }
  });
}

// 批量删除
function confirmBatchDelete() {
  if (selectedRowKeyList.value.length === 0) {
    message.warning('请选择要删除的数据');
    return;
  }

  Modal.confirm({
    title: '提示',
    content: `确定要删除选中的${selectedRowKeyList.value.length}条记录吗？`,
    onOk: async () => {
      try {
        await seatApi.batchDelete(selectedRowKeyList.value);
        message.success('批量删除成功');
        selectedRowKeyList.value = [];
        queryData();
      } catch (error) {
        message.error('批量删除失败');
      }
    }
  });
}

// ---------------------------- 辅助方法 ----------------------------
function getSeatTypeName(type: number) {
  const typeMap = { 1: '普通座位', 2: 'VIP座位', 3: '无障碍座位' };
  return typeMap[type] || '未知';
}

function getSeatTypeColor(type: number) {
  const colorMap = { 1: 'blue', 2: 'purple', 3: 'cyan' };
  return colorMap[type] || 'default';
}

function getSeatStatusName(status: number) {
  const statusMap = { 1: '空闲', 2: '使用中', 3: '预约中', 4: '维护中' };
  return statusMap[status] || '未知';
}

function getSeatStatusColor(status: number) {
  const colorMap = { 1: 'green', 2: 'orange', 3: 'blue', 4: 'red' };
  return colorMap[status] || 'default';
}

// ---------------------------- 页面初始化 ----------------------------
onMounted(() => {
  queryData();
});
</script>

<style scoped lang="less">
.seat-management {
  .smart-table-btn-block {
    padding-bottom: 16px;
  }

  .smart-table-operate-block {
    display: flex;
    gap: 8px;
  }
}</style>