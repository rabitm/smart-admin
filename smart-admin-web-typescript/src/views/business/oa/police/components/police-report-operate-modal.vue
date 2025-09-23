<!--
  * 警情录入操作表单（新增/编辑）
  *
  * @Author:    Claude Code Assistant
  * @Date:      2025-09-18
  * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
-->
<template>
  <a-modal
    :title="form.reportId ? '编辑警情' : '智能接警录入'"
    :width="form.reportId ? 800 : 1000"
    :open="visible"
    @cancel="onClose"
    @ok="onSubmit"
    :confirmLoading="confirmLoading"
    :cancelText="$t('common.cancel')"
    :okText="form.reportId ? $t('common.confirm') : '立即处理'"
    :okButtonProps="{ type: 'primary', size: 'large' }"
    :cancelButtonProps="{ size: 'large' }"
  >
    <!-- 新增警情 - 使用智能三步骤表单 -->
    <div v-if="!form.reportId">
      <SmartEmergencyForm
        ref="emergencyFormRef"
        @submit="handleEmergencySubmit"
      />
    </div>

    <!-- 编辑警情 - 使用传统表单 -->
    <div v-else>
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="警情类型" name="reportType">
              <a-select v-model:value="form.reportType" placeholder="请选择警情类型">
                <a-select-option v-for="(item, key) in POLICE_REPORT_TYPE_ENUM" :key="key" :value="item.value">
                  {{ item.desc }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="警情等级" name="reportLevel">
              <a-select v-model:value="form.reportLevel" placeholder="请选择警情等级">
                <a-select-option v-for="(item, key) in POLICE_REPORT_LEVEL_ENUM" :key="key" :value="item.value">
                  {{ item.desc }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="报警人姓名" name="reporterName">
              <a-input v-model:value="form.reporterName" placeholder="请输入报警人姓名" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="报警人电话" name="reporterPhone">
              <a-input v-model:value="form.reporterPhone" placeholder="请输入报警人电话" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="报警人身份证" name="reporterIdCard">
              <a-input v-model:value="form.reporterIdCard" placeholder="请输入报警人身份证号" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="报警时间" name="reportTime">
              <a-date-picker
                v-model:value="form.reportTime"
                show-time
                format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择报警时间"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="事发地点" name="incidentLocation">
          <SmartLocationInput
            v-model="form.incidentLocation"
            placeholder="请输入事发地点，体验智能联想 ⚡"
            :search-api="policeReportApi.searchLocationSuggestions"
            @select="handleLocationSelect"
          />
        </a-form-item>

        <a-form-item label="警情描述" name="description">
          <a-textarea
            v-model:value="form.description"
            placeholder="请详细描述警情"
            :rows="4"
            :maxlength="500"
            show-count
          />
        </a-form-item>

        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="处理状态" name="status">
              <a-select v-model:value="form.status" placeholder="请选择处理状态">
                <a-select-option v-for="(item, key) in POLICE_REPORT_STATUS_ENUM" :key="key" :value="item.value">
                  {{ item.desc }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="处理人员" name="handlerName">
              <a-input v-model:value="form.handlerName" placeholder="请输入处理人员姓名" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="处理结果" name="handleResult" v-if="form.status >= 3">
          <a-textarea
            v-model:value="form.handleResult"
            placeholder="请输入处理结果"
            :rows="3"
            :maxlength="500"
            show-count
          />
        </a-form-item>

        <a-form-item label="处理完成时间" name="handleTime" v-if="form.status >= 3">
          <a-date-picker
            v-model:value="form.handleTime"
            show-time
            format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择处理完成时间"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="附件信息" name="attachments">
          <a-input v-model:value="form.attachments" placeholder="请输入附件信息（可选）" />
        </a-form-item>

        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="form.remark"
            placeholder="请输入备注信息（可选）"
            :rows="2"
            :maxlength="200"
            show-count
          />
        </a-form-item>
      </a-form>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
  import { reactive, ref, nextTick } from 'vue';
  import { message } from 'ant-design-vue';
  import { SmartLoading } from '/@/components/framework/smart-loading';
  import SmartLocationInput from '/@/components/framework/smart-location-input/index.vue';
  import SmartEmergencyForm from '/@/components/business/emergency/smart-emergency-form/index.vue';
  import { policeReportApi } from '/@/api/business/oa/police-report-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import {
    POLICE_REPORT_TYPE_ENUM,
    POLICE_REPORT_LEVEL_ENUM,
    POLICE_REPORT_STATUS_ENUM
  } from '/@/constants/business/oa/police-report-const';
  import dayjs from 'dayjs';
  import { debounce } from 'lodash';

  // ----------------------- 以下为弹窗相关的变量和方法 -----------------------

  const emits = defineEmits(['refresh']);

  const visible = ref(false);
  const confirmLoading = ref(false);
  const formRef = ref();
  const emergencyFormRef = ref();

  const formDefault = {
    reportId: null,
    reportType: null,
    reportLevel: null,
    reporterName: '',
    reporterPhone: '',
    reporterIdCard: '',
    reportTime: null,
    incidentLocation: '',
    description: '',
    status: 1, // 默认待处理
    handlerName: '',
    handleResult: '',
    handleTime: null,
    attachments: '',
    remark: '',
  };

  let form = reactive({ ...formDefault });

  const rules = {
    reportType: [{ required: true, message: '请选择警情类型', trigger: 'change' }],
    reportLevel: [{ required: true, message: '请选择警情等级', trigger: 'change' }],
    reporterName: [{ required: true, message: '请输入报警人姓名', trigger: 'blur' }],
    reporterPhone: [
      { required: true, message: '请输入报警人电话', trigger: 'blur' },
      { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
    ],
    reportTime: [{ required: true, message: '请选择报警时间', trigger: 'change' }],
    incidentLocation: [{ required: true, message: '请输入事发地点', trigger: 'blur' }],
    description: [{ required: true, message: '请输入警情描述', trigger: 'blur' }],
  };

  function showModal(reportId) {
    Object.assign(form, formDefault);
    visible.value = true;
    if (reportId) {
      getDetail(reportId);
    } else {
      // 新增时设置默认报警时间为当前时间
      form.reportTime = dayjs();
    }
    nextTick(() => {
      formRef.value?.clearValidate();
    });
  }

  async function getDetail(reportId) {
    try {
      SmartLoading.show();
      let responseModel = await policeReportApi.getDetail(reportId);
      let data = responseModel.data;
      Object.assign(form, data);
      // 转换日期格式
      if (form.reportTime) {
        form.reportTime = dayjs(form.reportTime);
      }
      if (form.handleTime) {
        form.handleTime = dayjs(form.handleTime);
      }
    } catch (e) {
      smartSentry.captureError(e);
    } finally {
      SmartLoading.hide();
    }
  }

  async function onSubmit() {
    try {
      confirmLoading.value = true;

      if (form.reportId) {
        // 编辑模式 - 使用传统表单验证
        await formRef.value.validateFields();

        // 准备提交数据
        let formData = { ...form };
        // 转换日期格式
        if (formData.reportTime) {
          formData.reportTime = dayjs(formData.reportTime).format('YYYY-MM-DD HH:mm:ss');
        }
        if (formData.handleTime) {
          formData.handleTime = dayjs(formData.handleTime).format('YYYY-MM-DD HH:mm:ss');
        }

        await policeReportApi.updatePoliceReport(formData);
        message.success('更新成功');
      } else {
        // 新增模式 - 使用智能表单提交
        await emergencyFormRef.value.handleSubmit();
        return; // 由 handleEmergencySubmit 处理后续流程
      }

      emits('refresh');
      onClose();
    } catch (e) {
      smartSentry.captureError(e);
    } finally {
      confirmLoading.value = false;
    }
  }

  function onClose() {
    Object.assign(form, formDefault);
    visible.value = false;
    confirmLoading.value = false;
  }

  // ----------------------- 以下为智能表单相关事件处理 -----------------------

  // 智能表单提交处理
  async function handleEmergencySubmit(emergencyData) {
    try {
      // 将智能表单数据转换为API格式
      const formData = {
        reportType: emergencyData.emergencyType,
        reportLevel: emergencyData.urgencyLevel || 2, // 默认中等级别
        reporterName: emergencyData.reporterName,
        reporterPhone: emergencyData.reporterPhone,
        reporterIdCard: emergencyData.reporterIdCard || '',
        reportTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
        incidentLocation: emergencyData.incidentLocation,
        description: emergencyData.description,
        status: 1, // 待处理
        handlerName: '',
        handleResult: '',
        handleTime: null,
        attachments: '',
        remark: emergencyData.remark || '',
        // 保存动态表单数据
        dynamicData: JSON.stringify(emergencyData.dynamicData || {})
      };

      await policeReportApi.addPoliceReport(formData);
      message.success('🚨 警情录入成功，已自动分派处理！');

      emits('refresh');
      onClose();
    } catch (e) {
      smartSentry.captureError(e);
      message.error('提交失败，请重试');
    } finally {
      confirmLoading.value = false;
    }
  }

  // 地址选择处理
  function handleLocationSelect(option) {
    console.log(`✅ 地址已选择: ${option.value} (${option.type})`);
  }


  defineExpose({
    showModal,
  });
</script>

<style scoped>
/* 弹窗组件样式优化完成 - 智能地址输入功能已迁移到组件 */
</style>