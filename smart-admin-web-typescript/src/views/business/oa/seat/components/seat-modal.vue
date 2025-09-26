<!--
  座位管理弹窗组件

  @Author: Claude Code Assistant
  @Date: 2025-09-25
  @Copyright: 1024创新实验室
-->
<template>
  <a-modal
    :title="form.seatId ? '编辑座位' : '新增座位'"
    :width="800"
    :open="visible"
    :maskClosable="false"
    :keyboard="false"
    @ok="onSubmit"
    @cancel="onCancel">

    <a-form
      ref="formRef"
      :model="form"
      :rules="rules"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 14 }">

      <a-form-item label="座位编号" name="seatNumber">
        <a-input v-model:value="form.seatNumber" placeholder="请输入座位编号" />
      </a-form-item>

      <a-form-item label="座位名称" name="seatName">
        <a-input v-model:value="form.seatName" placeholder="请输入座位名称" />
      </a-form-item>

      <a-form-item label="座位类型" name="seatType">
        <a-select v-model:value="form.seatType" placeholder="请选择座位类型">
          <a-select-option :value="1">普通座位</a-select-option>
          <a-select-option :value="2">VIP座位</a-select-option>
          <a-select-option :value="3">无障碍座位</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="座位状态" name="seatStatus" v-if="form.seatId">
        <a-select v-model:value="form.seatStatus" placeholder="请选择座位状态">
          <a-select-option :value="1">空闲</a-select-option>
          <a-select-option :value="2">使用中</a-select-option>
          <a-select-option :value="3">预约中</a-select-option>
          <a-select-option :value="4">维护中</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="区域ID" name="areaId">
        <a-input-number v-model:value="form.areaId" placeholder="请输入区域ID" style="width: 100%" />
      </a-form-item>

      <a-form-item label="区域名称" name="areaName">
        <a-input v-model:value="form.areaName" placeholder="请输入区域名称" />
      </a-form-item>

      <a-form-item label="楼层" name="floor">
        <a-input-number v-model:value="form.floor" placeholder="请输入楼层" style="width: 100%" />
      </a-form-item>

      <a-form-item label="位置描述" name="locationDesc">
        <a-textarea v-model:value="form.locationDesc" placeholder="请输入位置描述" :rows="3" />
      </a-form-item>

      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="form.remark" placeholder="请输入备注" :rows="3" />
      </a-form-item>

    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { seatApi } from '/@/api/business/oa/seat-api';
import type { FormInstance } from 'ant-design-vue';

// ---------------------------- 事件定义 ----------------------------
const emit = defineEmits(['reloadList']);

// ---------------------------- 表单相关 ----------------------------
const formRef = ref<FormInstance>();
const visible = ref(false);

const formDefault = {
  seatId: undefined,
  seatNumber: '',
  seatName: '',
  seatType: 1,
  seatStatus: 1,
  areaId: null,
  areaName: '',
  floor: null,
  locationDesc: '',
  remark: '',
};

const form = reactive({ ...formDefault });

const rules = {
  seatNumber: [{ required: true, message: '请输入座位编号' }],
  seatName: [{ required: true, message: '请输入座位名称' }],
  seatType: [{ required: true, message: '请选择座位类型' }],
};

// ---------------------------- 对外方法 ----------------------------
function showModal(record?: any) {
  Object.assign(form, formDefault);
  if (record) {
    Object.assign(form, record);
  }
  visible.value = true;
}

// ---------------------------- 内部方法 ----------------------------
async function onSubmit() {
  try {
    await formRef.value?.validateFields();

    if (form.seatId) {
      await seatApi.update(form);
      message.success('更新成功');
    } else {
      await seatApi.add(form);
      message.success('添加成功');
    }

    onCancel();
    emit('reloadList');
  } catch (error) {
    message.error(form.seatId ? '更新失败' : '添加失败');
  }
}

function onCancel() {
  visible.value = false;
  formRef.value?.resetFields();
}

// 暴露方法给父组件
defineExpose({
  showModal,
});
</script>