<!--
  * 警情录入编辑页面 - 业界流行现代化设计
  *
  * @Author:    Claude Code Assistant
  * @Date:      2025-09-18
  * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
-->
<template>
  <div class="modern-police-edit">
    <!-- 编辑锁状态 -->
    <PoliceEditLock
      v-if="isEdit && reportId"
      :reportId="reportId"
      :seatId="currentSeatId"
      :autoLock="true"
      @lockStatusChange="handleLockStatusChange"
      @editAccessGranted="handleEditAccessGranted"
      @editAccessDenied="handleEditAccessDenied"
      ref="editLockRef"
    />

    <!-- 顶部状态栏 -->
    <div class="top-status-bar">
      <div class="status-left">
        <a-button type="text" @click="goBack" class="back-btn">
          <ArrowLeftOutlined /> 返回列表
        </a-button>
        <div class="status-info">
          <h1>{{ isEdit ? '编辑警情' : '新建警情' }}</h1>
          <div class="status-badges" v-if="isEdit">
            <a-tag :color="getLevelColor(form.reportLevel)" class="priority-tag">
              {{ getLevelText(form.reportLevel) }}
            </a-tag>
            <a-tag :color="getStatusColor(form.status)">
              {{ getStatusText(form.status) }}
            </a-tag>
          </div>
        </div>
      </div>
      <div class="status-right">
        <a-space size="large">
          <div class="auto-save-info" v-if="lastSaveTime">
            <ClockCircleOutlined />
            <span>{{ lastSaveTime }} 自动保存</span>
          </div>
          <a-button size="large" @click="submitForm" type="primary" :loading="saving" :disabled="isLocked" class="submit-btn">
            <CheckOutlined />
            {{ isEdit ? '保存更改' : '创建警情' }}
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- 超紧凑单屏布局 -->
    <div class="ultra-compact-container">
      <a-row :gutter="12">
        <!-- 左侧主要信息 - 扩大比例 -->
        <a-col :span="18">
          <div class="main-form-panel">
            <a-form ref="formRef" :model="form" layout="inline" class="compact-form" :disabled="isLocked">
              <!-- 第一行：核心信息紧凑排列 -->
              <div class="inline-row primary-row">
                <a-form-item name="reportType" label="类型" :rules="[{required: true, message: '请选择警情类型'}]" class="type-field">
                  <div class="compact-type-selector" data-field="reportType">
                    <a-select v-model:value="form.reportType" size="small" style="width: 100px">
                      <a-select-option v-for="type in POLICE_REPORT_TYPE_ENUM" :key="type.value" :value="type.value">
                        {{ getTypeIcon(type.value) }} {{ type.desc }}
                      </a-select-option>
                    </a-select>
                  </div>
                </a-form-item>

                <a-form-item name="reportLevel" label="等级" :rules="[{required: true, message: '请选择紧急程度'}]" class="level-field">
                  <div class="compact-level-buttons" data-field="reportLevel">
                    <div
                      v-for="level in POLICE_REPORT_LEVEL_ENUM"
                      :key="level.value"
                      :class="['level-tag', `level-${level.value}`, { active: form.reportLevel === level.value }]"
                      @click="form.reportLevel = level.value"
                    >
                      {{ level.desc }}
                    </div>
                  </div>
                </a-form-item>

                <a-form-item name="reporterName" label="报警人" :rules="[{required: true, message: '请输入报警人姓名'}]" class="name-field">
                  <a-input v-model:value="form.reporterName" placeholder="姓名" size="small" style="width: 85px" data-field="reporterName" />
                </a-form-item>

                <a-form-item name="reporterPhone" label="电话" :rules="[{required: true, message: '请输入联系电话'}]" class="phone-field">
                  <a-input v-model:value="form.reporterPhone" placeholder="手机号" size="small" style="width: 110px" data-field="reporterPhone" />
                </a-form-item>

                <a-form-item name="reportTime" label="时间" class="time-field">
                  <a-date-picker
                    v-model:value="form.reportTime"
                    show-time
                    format="MM-DD HH:mm"
                    placeholder="报警时间"
                    size="small"
                    style="width: 120px"
                  />
                </a-form-item>
              </div>

              <!-- 第二行：地点和描述 -->
              <div class="inline-row secondary-row">
                <a-form-item name="incidentLocation" label="地点" :rules="[{required: true, message: '请输入事发地点'}]" class="location-field">
                  <div class="location-input-group" data-field="incidentLocation">
                    <SmartLocationInput
                      v-model="form.incidentLocation"
                      placeholder="事发地点"
                      :search-api="policeReportApi.searchLocationSuggestions"
                      @select="handleLocationSelect"
                      input-size="small"
                      style="width: 200px"
                    />
                    <a-button type="link" size="small" @click="getCurrentLocation" class="location-btn">
                      <AimOutlined />
                    </a-button>
                  </div>
                </a-form-item>

                <a-form-item name="description" label="描述" :rules="[{required: true, message: '请输入事件描述'}]" class="desc-field">
                  <a-textarea
                    v-model:value="form.description"
                    :rows="1"
                    placeholder="事件经过、现场情况等"
                    :maxlength="200"
                    show-count
                    size="small"
                    style="width: 300px"
                    data-field="description"
                  />
                </a-form-item>
              </div>

              <!-- 第三行：处理信息（仅编辑时显示） -->
              <div class="inline-row status-row" v-if="isEdit">
                <a-form-item name="status" label="状态" class="status-field">
                  <div class="compact-status-buttons" data-field="status">
                    <div
                      v-for="status in POLICE_REPORT_STATUS_ENUM"
                      :key="status.value"
                      :class="['status-tag', `status-${status.value}`, { active: form.status === status.value }]"
                      @click="changeStatus(status.value)"
                    >
                      {{ getStatusIcon(status.value) }} {{ status.desc }}
                    </div>
                  </div>
                </a-form-item>

                <a-form-item name="handlerName" label="处理人" v-if="form.status > 1" class="handler-field">
                  <a-input v-model:value="form.handlerName" placeholder="处理人" size="small" style="width: 90px" data-field="handlerName" />
                </a-form-item>

                <a-form-item name="handleResult" label="结果" v-if="form.status >= 3" class="result-field">
                  <a-textarea
                    v-model:value="form.handleResult"
                    :rows="1"
                    placeholder="处理结果"
                    :maxlength="100"
                    size="small"
                    style="width: 200px"
                    data-field="handleResult"
                  />
                </a-form-item>
              </div>
            </a-form>
          </div>
        </a-col>

        <!-- 右侧精简操作栏 -->
        <a-col :span="6">
          <div class="mini-side-panel">
            <!-- 快捷操作 -->
            <div class="mini-actions">
              <a-button type="primary" danger size="small" @click="markUrgent" v-if="form.reportLevel !== 1" block>
                <ExclamationCircleOutlined /> 紧急
              </a-button>
              <a-button size="small" @click="quickAssign" v-if="isEdit && form.status === 1" block>
                <UserAddOutlined /> 分派
              </a-button>
              <a-button type="primary" size="small" @click="quickComplete" v-if="isEdit && form.status === 2" block>
                <CheckCircleOutlined /> 完成
              </a-button>
            </div>

            <!-- 备注 -->
            <div class="mini-remark">
              <div class="mini-label">备注</div>
              <a-textarea
                v-model:value="form.remark"
                :rows="2"
                placeholder="备注..."
                :maxlength="100"
                size="small"
                data-field="remark"
              />
            </div>

            <!-- 操作记录 -->
            <div class="mini-timeline" v-if="isEdit">
              <div class="mini-label">记录</div>
              <div class="timeline-items">
                <div class="timeline-item">
                  <span class="time">{{ form.createTime?.substring(5, 16) }}</span>
                  <span class="action">创建</span>
                </div>
                <div class="timeline-item" v-if="form.handleTime">
                  <span class="time">{{ form.handleTime?.toString().substring(5, 16) }}</span>
                  <span class="action">处理</span>
                </div>
              </div>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup lang="ts">
  // 立即执行的调试日志，用于确认文件是否被正确加载
  console.log('🌟 [IMMEDIATE DEBUG] police-report-edit.vue 文件已被加载！当前时间:', new Date().toLocaleTimeString());

  import { reactive, ref, onMounted, computed, watch, onUnmounted } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { message, Modal } from 'ant-design-vue';
  import { SmartLoading } from '/@/components/framework/smart-loading';
  import SmartLocationInput from '/@/components/framework/smart-location-input/index.vue';
  import PoliceEditLock from './components/police-edit-lock.vue';
  import { policeReportApi } from '/@/api/business/oa/police-report-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import {
    POLICE_REPORT_TYPE_ENUM,
    POLICE_REPORT_LEVEL_ENUM,
    POLICE_REPORT_STATUS_ENUM
  } from '/@/constants/business/oa/police-report-const';
  import { getWebSocketClient } from '/@/utils/websocket-manager';
  import dayjs from 'dayjs';
  import {
    ArrowLeftOutlined,
    CheckOutlined,
    ClockCircleOutlined,
    AlertOutlined,
    EnvironmentOutlined,
    SettingOutlined,
    UserOutlined,
    AimOutlined,
    ExclamationCircleOutlined,
    UserAddOutlined,
    CheckCircleOutlined,
    PlusCircleOutlined,
    HistoryOutlined,
    GlobalOutlined
  } from '@ant-design/icons-vue';

  const route = useRoute();
  const router = useRouter();

  // 基础状态
  const saving = ref(false);
  const draftSaving = ref(false);
  const lastSaveTime = ref('');
  const reporterSuggestions = ref([]);
  const formRef = ref();
  const editLockRef = ref();
  const isLocked = ref(false);
  const currentSeatId = ref(null); // 当前席位ID，从用户信息或API获取

  // 地址选择相关状态

  // 模拟报警人历史数据
  const mockReporterHistory = [
    { name: '张三', phone: '13800138001' },
    { name: '李四', phone: '13900139002' },
    { name: '王五', phone: '13700137003' }
  ];

  // 表单数据
  const formDefault = {
    reportId: null,
    reportNumber: '',
    reportType: 1, // 默认为"刑事案件"
    reportLevel: 2, // 默认为"中"级别
    reporterName: '',
    reporterPhone: '',
    reporterIdCard: '',
    reportTime: dayjs(),
    incidentLocation: '',
    description: '',
    status: 1,
    handlerName: '',
    handleResult: '',
    handleTime: null,
    attachments: '',
    remark: '',
    createTime: '',
    updateTime: '',
    createUserName: '',
  };

  const form = reactive({ ...formDefault });
  console.log('📋 [Form Debug] 表单对象初始化:', form);

  // 计算属性
  const isEdit = computed(() => !!route.query.reportId);
  const reportId = computed(() => route.query.reportId as string);

  // 调试状态变量
  console.log('🐛 [Vue Debug] isEdit 初始值:', isEdit.value);
  console.log('🐛 [Vue Debug] reportId 初始值:', reportId.value);
  console.log('🐛 [Vue Debug] route.query:', route.query);


  // --------------------------- WebSocket实时同步 ---------------------------

  let wsClient: any = null;

  // 处理其他用户的警情更新
  function handlePoliceCaseUpdate(message: SeatSyncMessage) {
    if (message.type === 'POLICE_CASE_UPDATE' &&
        message.policeCaseId &&
        reportId.value &&
        message.policeCaseId.toString() === reportId.value) {

      console.log('收到当前警情的更新消息:', message);

      // 显示更新提示
      Modal.confirm({
        title: '警情已被其他用户更新',
        content: `${message.message || '其他用户更新了此警情'}，是否重新加载最新数据？`,
        okText: '重新加载',
        cancelText: '继续编辑',
        onOk: () => {
          // 重新加载数据
          getDetail();
          message.success('已加载最新数据');
        },
        onCancel: () => {
          message.warning('请注意数据可能不是最新版本');
        }
      });
    }
  }

  // 处理字段实时同步消息
  function handleFieldSync(message: SeatSyncMessage) {
    console.log('📥 [Field Sync Debug] 收到字段同步消息:', message);

    if (message.type === 'POLICE_CASE_FIELD_SYNC') {
      console.log('📥 [Field Sync Debug] 消息类型匹配:', {
        policeCaseId: message.policeCaseId,
        currentReportId: reportId.value,
        messageData: message.data
      });

      if (message.policeCaseId &&
          reportId.value &&
          message.policeCaseId.toString() === reportId.value &&
          message.data) {

        console.log('✅ [Field Sync Debug] 条件检查通过，开始处理字段同步');

        const { fieldName, fieldValue } = message.data;
        console.log('📝 [Field Sync Debug] 处理字段更新:', { fieldName, fieldValue, currentValue: form[fieldName] });

        // 更新表单字段值
        if (form[fieldName] !== undefined) {
          const oldValue = form[fieldName];
          form[fieldName] = fieldValue;
          console.log('🔄 [Field Sync Debug] 字段值已更新:', { fieldName, oldValue, newValue: fieldValue });

          // 显示实时编辑提示
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
          const userName = message.data.userName || '其他用户';
          message.info(`${userName}正在编辑「${displayName}」`, 2);

          // 添加视觉指示器
          const fieldElement = document.querySelector(`[data-field="${fieldName}"]`);
          if (fieldElement) {
            console.log('🎨 [Field Sync Debug] 添加视觉指示器');
            fieldElement.classList.add('field-editing-indicator');
            setTimeout(() => {
              fieldElement.classList.remove('field-editing-indicator');
            }, 3000);
          } else {
            console.warn('❌ [Field Sync Debug] 未找到字段元素:', fieldName);
          }
        } else {
          console.warn('❌ [Field Sync Debug] 表单中不存在字段:', fieldName);
        }
      } else {
        console.log('❌ [Field Sync Debug] 条件检查失败，跳过处理');
      }
    } else {
      console.log('❌ [Field Sync Debug] 消息类型不匹配:', message.type);
    }
  }

  // 字段同步防抖函数
  const fieldSyncDebounce = new Map();

  function syncFieldUpdate(fieldName: string, fieldValue: any, oldValue?: any) {
    console.log('🔄 [Field Sync Debug] 开始字段同步:', {
      fieldName,
      fieldValue,
      oldValue,
      reportId: reportId.value
    });

    if (!reportId.value) {
      console.warn('❌ [Field Sync Debug] reportId为空，跳过同步');
      return;
    }

    // 清除之前的定时器
    if (fieldSyncDebounce.has(fieldName)) {
      console.log('⏰ [Field Sync Debug] 清除之前的防抖定时器:', fieldName);
      clearTimeout(fieldSyncDebounce.get(fieldName));
    }

    // 设置新的防抖定时器
    const timer = setTimeout(async () => {
      try {
        console.log('📡 [Field Sync Debug] 发送字段同步请求:', {
          reportId: reportId.value,
          fieldName,
          fieldValue,
          oldValue
        });

        await policeReportApi.syncFieldUpdate(reportId.value, fieldName, fieldValue, oldValue);
        console.log(`✅ [Field Sync Debug] 字段 ${fieldName} 同步成功`);
      } catch (error) {
        console.error(`❌ [Field Sync Debug] 字段 ${fieldName} 同步失败:`, error);
      }
    }, 500); // 500ms 防抖延迟

    fieldSyncDebounce.set(fieldName, timer);
  }

  // 生命周期
  onMounted(() => {
    console.log('🚀 [Page Debug] 警情编辑页面已加载');
    console.log('🚀 [Page Debug] isEdit状态:', isEdit.value);
    console.log('🚀 [Page Debug] reportId:', reportId.value);
    console.log('🚀 [Page Debug] route.query:', route.query);

    if (isEdit.value) {
      console.log('✅ [Page Debug] 是编辑模式，开始加载详情');
      getDetail();
    } else {
      console.log('❌ [Page Debug] 不是编辑模式，跳过加载详情');
    }

    // 初始化WebSocket监听
    try {
      wsClient = getWebSocketClient();
      wsClient.on('POLICE_CASE_UPDATE', handlePoliceCaseUpdate);
      wsClient.on('POLICE_CASE_FIELD_SYNC', handleFieldSync);
      console.log('🔌 [Page Debug] 警情编辑页面WebSocket监听已启动');
      console.log('🔌 [Page Debug] WebSocket连接状态:', wsClient.readyState);
    } catch (error) {
      console.error('❌ [Page Debug] 启动WebSocket监听失败:', error);
    }
  });

  onUnmounted(() => {
    // 清理WebSocket监听
    if (wsClient) {
      wsClient.off('POLICE_CASE_UPDATE', handlePoliceCaseUpdate);
      wsClient.off('POLICE_CASE_FIELD_SYNC', handleFieldSync);
      console.log('警情编辑页面WebSocket监听已清理');
    }

    // 清理定时器
    clearTimeout(autoSaveTimer);

    // 清理字段同步防抖定时器
    fieldSyncDebounce.forEach((timer) => {
      clearTimeout(timer);
    });
    fieldSyncDebounce.clear();
  });

  // 自动保存草稿
  let autoSaveTimer: NodeJS.Timeout;
  watch(
    () => form,
    () => {
      clearTimeout(autoSaveTimer);
      autoSaveTimer = setTimeout(() => {
        if (!isEdit.value) return; // 只对编辑模式自动保存
        saveDraft();
      }, 10000); // 10秒后自动保存
    },
    { deep: true }
  );

  // 添加一个测试用的简单监听器
  watch(() => form, (newForm) => {
    console.log('🔍 [Test Debug] 表单对象发生了变化 (deep watch)');
  }, { deep: true });

  // 简单测试watch - 应该总是触发
  watch(() => form.reporterName, (newValue, oldValue) => {
    console.log('🚨 [SIMPLE TEST] reporterName 变化了!', { newValue, oldValue });
    console.log('🚨 [SIMPLE TEST] isEdit 值:', isEdit.value);
  });

  // 监听isEdit变化
  watch(() => isEdit.value, (newValue, oldValue) => {
    console.log('🎯 [isEdit Debug] isEdit 变化:', { newValue, oldValue });
  });

  // 监听route变化
  watch(() => route.query, (newQuery, oldQuery) => {
    console.log('🛣️ [Route Debug] route.query 变化:', { newQuery, oldQuery });
  });

  // 实时字段同步监听器
  watch(() => form.reportType, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] reportType变化:', { newValue, oldValue, isEdit: isEdit.value });
    if (isEdit.value && newValue !== null && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发reportType同步');
      syncFieldUpdate('reportType', newValue);
    }
  });

  watch(() => form.reportLevel, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] reportLevel变化:', { newValue, oldValue, isEdit: isEdit.value });
    if (isEdit.value && newValue !== null && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发reportLevel同步');
      syncFieldUpdate('reportLevel', newValue);
    }
  });

  watch(() => form.reporterName, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] reporterName变化:', { newValue, oldValue, isEdit: isEdit.value });
    if (isEdit.value && newValue && newValue.trim() && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发reporterName同步');
      syncFieldUpdate('reporterName', newValue.trim());
    }
  });

  watch(() => form.reporterPhone, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] reporterPhone变化:', { newValue, oldValue, isEdit: isEdit.value });
    if (isEdit.value && newValue && newValue.trim() && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发reporterPhone同步');
      syncFieldUpdate('reporterPhone', newValue.trim());
    }
  });

  watch(() => form.incidentLocation, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] incidentLocation变化:', { newValue, oldValue, isEdit: isEdit.value });
    if (isEdit.value && newValue && newValue.trim() && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发incidentLocation同步');
      syncFieldUpdate('incidentLocation', newValue.trim());
    }
  });

  watch(() => form.description, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] description变化:', { newValue, oldValue, isEdit: isEdit.value });
    if (isEdit.value && newValue && newValue.trim() && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发description同步');
      syncFieldUpdate('description', newValue.trim());
    }
  });

  watch(() => form.status, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] status变化:', { newValue, oldValue, isEdit: isEdit.value });
    if (isEdit.value && newValue !== null && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发status同步');
      syncFieldUpdate('status', newValue);
    }
  });

  watch(() => form.handlerName, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] handlerName变化:', { newValue, oldValue, isEdit: isEdit.value });
    if (isEdit.value && newValue && newValue.trim() && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发handlerName同步');
      syncFieldUpdate('handlerName', newValue.trim());
    }
  });

  watch(() => form.handleResult, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] handleResult变化:', { newValue, oldValue, isEdit: isEdit.value });
    if (isEdit.value && newValue && newValue.trim() && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发handleResult同步');
      syncFieldUpdate('handleResult', newValue.trim());
    }
  });

  watch(() => form.remark, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] remark变化:', { newValue, oldValue, isEdit: isEdit.value });
    if (isEdit.value && newValue && newValue.trim() && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发remark同步');
      syncFieldUpdate('remark', newValue.trim());
    }
  });

  // 方法定义
  async function getDetail() {
    try {
      SmartLoading.show();
      const responseModel = await policeReportApi.getDetail(reportId.value);
      const data = responseModel.data;
      Object.assign(form, data);
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

  async function submitForm() {
    try {
      await formRef.value.validateFields();
      saving.value = true;

      const formData = { ...form };
      if (formData.reportTime) {
        formData.reportTime = dayjs(formData.reportTime).format('YYYY-MM-DD HH:mm:ss');
      }
      if (formData.handleTime) {
        formData.handleTime = dayjs(formData.handleTime).format('YYYY-MM-DD HH:mm:ss');
      }

      if (isEdit.value) {
        await policeReportApi.updatePoliceReport(formData);
        message.success('更新成功');
      } else {
        await policeReportApi.addPoliceReport(formData);
        message.success('提交成功');
      }

      router.push('/oa/police/report-list');
    } catch (e) {
      smartSentry.captureError(e);
    } finally {
      saving.value = false;
    }
  }

  async function saveDraft() {
    if (!isEdit.value) return;

    try {
      draftSaving.value = true;
      // 这里可以实现草稿保存逻辑
      message.success('草稿已保存', 1);
    } catch (e) {
      smartSentry.captureError(e);
    } finally {
      draftSaving.value = false;
    }
  }

  function goBack() {
    router.push('/oa/police/report-list');
  }

  // 编辑锁事件处理
  function handleLockStatusChange(locked: boolean) {
    isLocked.value = locked;
  }

  function handleEditAccessGranted() {
    isLocked.value = false;
    message.success('获得编辑权限');
  }

  function handleEditAccessDenied(reason: string) {
    isLocked.value = true;
    message.warning(reason);
  }

  // 新增现代化交互方法
  function getTypeIcon(type: number) {
    const iconMap = {
      1: '🔴', // 刑事案件
      2: '🚗', // 交通事故
      3: '⚖️', // 治安案件
      4: '🔥', // 火灾事故
      5: '🏥', // 医疗急救
      6: '👥', // 民事纠纷
      99: '❓' // 其他
    };
    return iconMap[type] || '❓';
  }

  function getStatusIcon(status: number) {
    const iconMap = {
      1: '⏳', // 待处理
      2: '🔄', // 处理中
      3: '✅', // 已完成
      4: '❌'  // 已关闭
    };
    return iconMap[status] || '⏳';
  }

  function onReporterNameChange() {
    const input = form.reporterName.trim();
    if (input.length >= 2) {
      // 模拟智能搜索
      reporterSuggestions.value = mockReporterHistory.filter(item =>
        item.name.includes(input)
      );
    } else {
      reporterSuggestions.value = [];
    }
  }

  function selectReporter(reporter) {
    form.reporterName = reporter.name;
    form.reporterPhone = reporter.phone;
    reporterSuggestions.value = [];
  }

  // 地址选择处理
  function handleLocationSelect(option) {
    console.log(`✅ 地址已选择: ${option.value} (${option.type})`);
  }

  function getCurrentLocation() {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const { latitude, longitude } = position.coords;
          // TODO: 通过坐标获取地址信息，可接入第三方地图API
          message.info(`获取到坐标：${latitude.toFixed(6)}, ${longitude.toFixed(6)}`);
        },
        (error) => {
          message.error('获取位置信息失败，请手动输入地址');
          console.error('Geolocation error:', error);
        }
      );
    } else {
      message.error('浏览器不支持地理位置功能');
    }
  }

  function changeStatus(status: number) {
    form.status = status;
    if (status === 2 && !form.handlerName) {
      // 自动提示分派人员
      message.info('请指定处理人员');
    }
    if (status === 3 && !form.handleTime) {
      form.handleTime = dayjs();
    }
  }

  function markUrgent() {
    form.reportLevel = 1;
    message.success('已标记为紧急警情');
  }

  function quickAssign() {
    form.status = 2;
    message.info('请在处理状态中指定处理人员');
  }

  function quickComplete() {
    form.status = 3;
    form.handleTime = dayjs();
    message.success('已标记为完成，请填写处理结果');
  }

  function addFollowUp() {
    message.info('跟进功能开发中...');
  }

  function nextStep() {
    if (currentStep.value < 2) {
      currentStep.value++;
    }
  }

  function prevStep() {
    if (currentStep.value > 0) {
      currentStep.value--;
    }
  }

  function onFormChange() {
    generateSmartTips();
  }

  function onReportTypeChange() {
    generateSmartTips();
  }

  function onStatusChange() {
    if (form.status >= 3 && !form.handleTime) {
      form.handleTime = dayjs();
    }
  }

  async function checkReporterHistory() {
    if (!form.reporterPhone || form.reporterPhone.length < 11) return;

    try {
      const response = await policeReportApi.queryByReporterPhone(form.reporterPhone);
      reporterHistory.value = response.data.slice(0, 3); // 只显示最近3条
    } catch (e) {
      // 忽略错误，不影响主流程
    }
  }

  function generateSmartTips() {
    smartTips.value = [];

    if (form.reportType === 1) { // 刑事案件
      smartTips.value.push({
        type: 'warning',
        message: '刑事案件建议立即分派给专业刑侦人员处理'
      });
    }

    if (form.reportLevel === 1 && form.status === 1) { // 紧急且待处理
      smartTips.value.push({
        type: 'error',
        message: '紧急警情请尽快处理，建议15分钟内响应'
      });
    }

    if (form.reporterPhone && reporterHistory.value.length > 0) {
      smartTips.value.push({
        type: 'info',
        message: `该报警人有${reporterHistory.value.length}次报警记录，请关注是否为重复报警`
      });
    }
  }

  // 快捷操作

  // 颜色工具函数
  function getStatusColor(status) {
    const colorMap = {
      1: 'orange',
      2: 'blue',
      3: 'green',
      4: 'default',
    };
    return colorMap[status] || 'default';
  }

  function getLevelColor(level) {
    const colorMap = {
      1: 'red',
      2: 'orange',
      3: 'blue',
      4: 'default',
    };
    return colorMap[level] || 'default';
  }

  // 文本工具函数
  function getLevelText(level) {
    const textMap = {
      1: '紧急',
      2: '高',
      3: '中',
      4: '低',
    };
    return textMap[level] || '';
  }

  function getStatusText(status) {
    const textMap = {
      1: '待处理',
      2: '处理中',
      3: '已完成',
      4: '已关闭',
    };
    return textMap[status] || '';
  }
</script>

<style scoped>
/* 实用简洁的警情编辑界面样式 */
.modern-police-edit {
  min-height: 100vh;
  background: #f8f9fa;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 顶部状态栏 */
.top-status-bar {
  background: white;
  border-bottom: 1px solid #e8e8e8;
  padding: 12px 20px;
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
}

.status-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  color: #666 !important;
  border: none !important;
  padding: 8px 16px !important;
  border-radius: 8px !important;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background: rgba(24, 144, 255, 0.1) !important;
  color: #1890ff !important;
}

.status-info h1 {
  margin: 0 0 6px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.status-badges {
  display: flex;
  gap: 8px;
}

.priority-tag {
  font-weight: 600;
  border-radius: 6px;
}

.status-right {
  display: flex;
  align-items: center;
}

.auto-save-info {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #52c41a;
  font-size: 14px;
}

.submit-btn {
  height: 36px !important;
  padding: 0 20px !important;
  border-radius: 6px !important;
  font-weight: 500 !important;
}

/* 超紧凑表单容器 */
.ultra-compact-container {
  padding: 8px 12px;
  height: calc(100vh - 80px);
  overflow: hidden;
  background: #f8f9fa;
}

/* 紧凑表单样式 */
.compact-form {
  height: 100%;
}

.compact-form .ant-form-item {
  margin-bottom: 8px;
  margin-right: 12px;
}

.compact-form .ant-form-item-label {
  padding-bottom: 2px;
}

.compact-form .ant-form-item-label > label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
  line-height: 1.2;
}

.main-form-panel {
  background: white;
  border-radius: 8px;
  padding: 12px;
  height: 100%;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

/* 行内表单行样式 */
.inline-row {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  margin-bottom: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.inline-row:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.primary-row {
  background: #f8f9fa;
  border-radius: 6px;
  padding: 8px 12px;
  margin: 0 0 8px 0;
}

.secondary-row {
  padding: 6px 12px;
}

.status-row {
  background: #f0f8ff;
  border-radius: 6px;
  padding: 6px 12px;
}

/* 紧凑等级标签 */
.compact-level-buttons {
  display: flex;
  gap: 4px;
}

.level-tag {
  padding: 2px 8px;
  font-size: 11px;
  border: 1px solid #d9d9d9;
  border-radius: 12px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
  line-height: 1.4;
}

.level-tag:hover {
  border-color: #1890ff;
}

.level-tag.active {
  color: white;
  border-color: transparent;
}

.level-tag.level-1.active {
  background: #ff4d4f;
}

.level-tag.level-2.active {
  background: #fa8c16;
}

.level-tag.level-3.active {
  background: #1890ff;
}

.level-tag.level-4.active {
  background: #52c41a;
}

/* 紧凑状态标签 */
.compact-status-buttons {
  display: flex;
  gap: 4px;
}

.status-tag {
  padding: 2px 8px;
  font-size: 11px;
  border: 1px solid #d9d9d9;
  border-radius: 12px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
  line-height: 1.4;
}

.status-tag:hover {
  border-color: #1890ff;
}

.status-tag.active {
  color: white;
  border-color: transparent;
}

.status-tag.status-1.active {
  background: #fa8c16;
}

.status-tag.status-2.active {
  background: #1890ff;
}

.status-tag.status-3.active {
  background: #52c41a;
}

.status-tag.status-4.active {
  background: #8c8c8c;
}

/* 地点输入组 */
.location-input-group {
  display: flex;
  align-items: center;
  gap: 4px;
}

.location-btn {
  flex-shrink: 0;
  height: 24px;
  width: 24px;
  min-width: 24px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 字段宽度控制 */
.type-field {
  min-width: 120px;
}

.level-field {
  min-width: 200px;
}

.name-field {
  min-width: 100px;
}

.phone-field {
  min-width: 130px;
}

.time-field {
  min-width: 140px;
}

.location-field {
  min-width: 240px;
}

.desc-field {
  min-width: 320px;
}

.status-field {
  min-width: 320px;
}

.handler-field {
  min-width: 110px;
}

.result-field {
  min-width: 220px;
}

/* 迷你侧边栏样式 */
.mini-side-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mini-actions {
  background: white;
  border-radius: 6px;
  padding: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.mini-actions .ant-btn {
  height: 28px;
  font-size: 11px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.mini-remark {
  background: white;
  border-radius: 6px;
  padding: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
}

.mini-timeline {
  background: white;
  border-radius: 6px;
  padding: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
  flex: 1;
  overflow: auto;
}

.mini-label {
  font-size: 12px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
  line-height: 1.2;
}

.timeline-items {
  max-height: 120px;
  overflow-y: auto;
}

.timeline-item {
  padding: 4px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 11px;
}

.timeline-item:last-child {
  border-bottom: none;
}

.timeline-item .time {
  color: #999;
  display: block;
  margin-bottom: 2px;
  font-size: 10px;
}

.timeline-item .action {
  color: #333;
  font-size: 11px;
  font-weight: 500;
}

.side-panel {
  height: 100%;
  overflow: auto;
}

.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  align-items: flex-start;
}

.form-row.dense-row {
  margin-bottom: 12px;
}

.form-col {
  flex: 1;
  min-width: 0;
}

.form-col.wide {
  flex: 2;
}

.form-col.full {
  flex: 1;
  min-width: 100%;
}

.compact-label {
  display: block;
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
  font-weight: 500;
  line-height: 1.3;
}

.compact-label.required::after {
  content: ' *';
  color: #ff4d4f;
}

.type-buttons,
.priority-buttons,
.status-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.type-btn,
.priority-btn,
.status-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
  min-height: 32px;
}

.type-btn:hover,
.priority-btn:hover,
.status-btn:hover {
  border-color: #1890ff;
  background: #f0f8ff;
}

.type-btn.active,
.priority-btn.active,
.status-btn.active {
  border-color: #1890ff;
  background: #1890ff;
  color: white;
}

.priority-btn.priority-1.active {
  background: #ff4d4f;
  border-color: #ff4d4f;
}

.priority-btn.priority-2.active {
  background: #fa8c16;
  border-color: #fa8c16;
}

.priority-btn.priority-3.active {
  background: #1890ff;
  border-color: #1890ff;
}

.priority-btn.priority-4.active {
  background: #52c41a;
  border-color: #52c41a;
}

.status-btn.status-1.active {
  background: #fa8c16;
  border-color: #fa8c16;
}

.status-btn.status-2.active {
  background: #1890ff;
  border-color: #1890ff;
}

.status-btn.status-3.active {
  background: #52c41a;
  border-color: #52c41a;
}

.status-btn.status-4.active {
  background: #8c8c8c;
  border-color: #8c8c8c;
}

.type-icon,
.priority-icon {
  font-size: 14px;
}

.type-text,
.priority-text {
  font-size: 12px;
  white-space: nowrap;
}

.compact-input {
  height: 32px;
  font-size: 13px;
  line-height: 1.3;
}

.compact-textarea {
  min-height: 64px;
  font-size: 13px;
  resize: vertical;
  line-height: 1.4;
}

.location-btn {
  flex-shrink: 0;
  height: 32px;
  width: 32px;
  min-width: 32px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-section,
.log-section,
.remark-section {
  background: white;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.action-section h4,
.log-section h4,
.remark-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-actions .ant-btn {
  height: 32px;
  font-size: 12px;
  justify-content: flex-start;
  padding: 0 12px;
}

.timeline-compact {
  max-height: 150px;
  overflow-y: auto;
}

.log-item {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 12px;
  color: #666;
}

.log-item:last-child {
  border-bottom: none;
}

.log-time {
  color: #999;
  display: block;
  margin-bottom: 4px;
  font-size: 11px;
}

.log-text {
  color: #333;
  font-size: 12px;
}

/* 智能输入建议 */
.smart-input-wrapper {
  position: relative;
}

.input-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  max-height: 150px;
  overflow-y: auto;
}

.suggestion-item {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  font-size: 13px;
}

.suggestion-item:hover {
  background: #f5f5f5;
}

.suggestion-item .anticon {
  margin-right: 8px;
  color: #1890ff;
}

/* 智能地址输入样式 */
.smart-location-input {
  position: relative;
  flex: 1;
}

.location-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  z-index: 1000;
  max-height: 300px;
  overflow-y: auto;
}

.suggestion-group {
  padding: 8px 0;
}

.suggestion-group:not(:last-child) {
  border-bottom: 1px solid #f0f0f0;
}

.suggestion-group-title {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-size: 12px;
  color: #666;
  font-weight: 500;
  background: #fafafa;
}

.suggestion-group-title .anticon {
  font-size: 14px;
}

.location-suggestion-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  font-size: 13px;
  color: #333;
}

.location-suggestion-item:hover {
  background: #f5f5f5;
}

.location-suggestion-item .anticon {
  color: #52c41a;
  font-size: 14px;
}

.location-wrapper {
  display: flex;
  gap: 4px;
  align-items: center;
}

/* 优化Select组件 */
.compact-select {
  font-size: 12px;
}

.compact-select .ant-select-selector {
  height: 28px !important;
  font-size: 12px;
}

.compact-select .ant-select-selection-item {
  line-height: 26px !important;
  font-size: 12px;
}

/* 优化DatePicker组件 */
.compact-date-picker {
  height: 28px;
  font-size: 12px;
}

.compact-date-picker .ant-picker-input > input {
  font-size: 12px;
}

/* 紧凑表单项样式 */
.main-form-panel .ant-form-item {
  margin-bottom: 0;
}

.main-form-panel .ant-form-item-label {
  padding-bottom: 4px;
}

.main-form-panel .ant-form-item-label > label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
  line-height: 1.3;
}

.main-form-panel .ant-form-item-required::before {
  color: #ff4d4f;
}

/* 响应式处理 */
@media (max-width: 1366px) {
  .compact-form-container {
    padding: 10px 14px;
  }

  .form-row {
    gap: 16px;
    margin-bottom: 14px;
  }

  .form-row.dense-row {
    margin-bottom: 10px;
  }

  .type-btn,
  .priority-btn,
  .status-btn {
    padding: 5px 10px;
    font-size: 11px;
    min-height: 30px;
  }

  .compact-input {
    height: 30px;
    font-size: 12px;
  }

  .compact-textarea {
    min-height: 60px;
    font-size: 12px;
  }
}

/* 信息卡片 */
.info-card {
  background: white;
  border-radius: 16px;
  margin-bottom: 24px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.04);
  animation: fadeInUp 0.5s ease-out;
}

.primary-card {
  border-left: 4px solid #1890ff;
}

.card-header {
  display: flex;
  align-items: center;
  padding: 24px 32px 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

.header-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  margin-right: 16px;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

.location-icon {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.3);
}

.process-icon {
  background: linear-gradient(135deg, #722ed1 0%, #9254de 100%);
  box-shadow: 0 4px 12px rgba(114, 46, 209, 0.3);
}

.header-content h2 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.header-content p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.card-body {
  padding: 24px 32px 32px;
}

/* 字段组 */
.field-group {
  margin-bottom: 24px;
}

.field-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
}

.field-label.required::after {
  content: ' *';
  color: #ff4d4f;
}

.priority-field {
  margin-bottom: 32px;
}

/* 按钮组选择器 */
.button-group-selector {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
}

.type-button {
  padding: 16px 12px;
  border: 2px solid #e8e8e8;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  background: white;
}

.type-button:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
  transform: translateY(-1px);
}

.type-button.active {
  border-color: #1890ff;
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.2);
}

.button-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.button-text {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

/* 优先级选择器 */
.priority-selector {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.priority-item {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  border: 2px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: white;
  min-width: 120px;
  position: relative;
}

.priority-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.priority-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 8px;
}

.priority-1 {
  border-color: #ff4d4f;
}

.priority-1.active {
  background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%);
  border-color: #ff4d4f;
}

.priority-1 .priority-dot {
  background: #ff4d4f;
}

.priority-2 {
  border-color: #fa8c16;
}

.priority-2.active {
  background: linear-gradient(135deg, #fff7e6 0%, #ffd591 100%);
  border-color: #fa8c16;
}

.priority-2 .priority-dot {
  background: #fa8c16;
}

.priority-3 {
  border-color: #1890ff;
}

.priority-3.active {
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
  border-color: #1890ff;
}

.priority-3 .priority-dot {
  background: #1890ff;
}

.priority-4 {
  border-color: #52c41a;
}

.priority-4.active {
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
  border-color: #52c41a;
}

.priority-4 .priority-dot {
  background: #52c41a;
}

/* 智能输入 */
.smart-input-wrapper {
  position: relative;
}

.input-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  max-height: 200px;
  overflow-y: auto;
}

.suggestion-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.suggestion-item:hover {
  background: #f5f5f5;
}

.suggestion-content {
  margin-left: 12px;
}

.suggestion-name {
  font-weight: 500;
  color: #1a1a1a;
}

.suggestion-phone {
  font-size: 12px;
  color: #666;
}

/* 状态选择器 */
.status-selector {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.status-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: white;
  min-width: 100px;
}

.status-item:hover {
  border-color: #1890ff;
  transform: translateY(-1px);
}

.status-item.active {
  border-color: #1890ff;
  background: #e6f7ff;
}

.status-icon {
  margin-right: 8px;
  font-size: 16px;
}

/* 快捷操作面板 */
.quick-actions-panel {
  background: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.actions-header h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 12px;
  border: 2px solid #e8e8e8;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: white;
  text-align: center;
}

.action-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.action-item.urgent:hover {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.action-item.assign:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.action-item.complete:hover {
  border-color: #52c41a;
  color: #52c41a;
}

.action-item.follow:hover {
  border-color: #722ed1;
  color: #722ed1;
}

.action-item .anticon {
  font-size: 20px;
  margin-bottom: 8px;
}

/* 时间轴样式 */
.timeline-user {
  color: #666;
  font-size: 12px;
}

.timeline-desc {
  color: #1a1a1a;
  font-size: 14px;
  margin-top: 4px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .form-container {
    padding: 16px;
  }

  .top-status-bar {
    padding: 12px 16px;
    flex-direction: column;
    gap: 16px;
  }

  .status-left {
    width: 100%;
    justify-content: space-between;
  }

  .status-right {
    width: 100%;
    justify-content: center;
  }

  .button-group-selector {
    grid-template-columns: repeat(2, 1fr);
  }

  .priority-selector {
    flex-direction: column;
  }

  .actions-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* 动画定义 */
@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 输入框增强 */
.smart-input,
.phone-input,
.location-input,
.description-textarea {
  border-radius: 8px !important;
  border: 2px solid #e8e8e8 !important;
  transition: all 0.2s ease !important;
}

.smart-input:focus,
.phone-input:focus,
.location-input:focus,
.description-textarea:focus {
  border-color: #1890ff !important;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1) !important;
}

.location-input-wrapper {
  display: flex;
  gap: 8px;
  align-items: center;
}

.location-btn {
  flex-shrink: 0;
  border-radius: 6px !important;
}

.edit-header {
  background: white;
  padding: 16px 24px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.header-left {
  display: flex;
  align-items: center;
}

.back-btn {
  color: #666;
  font-size: 14px;
}

.title-section h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.report-number {
  color: #1890ff;
  font-size: 12px;
  margin-left: 8px;
}

.edit-content {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.form-section {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.step-indicator {
  margin-bottom: 32px;
}

.step-content {
  min-height: 400px;
}

.section-title {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.section-title h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.section-title p {
  margin: 4px 0 0 0;
  color: #8c8c8c;
  font-size: 14px;
}

.edit-form {
  margin-top: 24px;
}

.step-navigation {
  text-align: center;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.info-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.quick-actions,
.smart-tips,
.reporter-history,
.operation-log {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.tip-item {
  margin-bottom: 8px;
}

.tip-item:last-child {
  margin-bottom: 0;
}

.history-list {
  max-height: 200px;
  overflow-y: auto;
}

.history-item {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.history-item:last-child {
  border-bottom: none;
}

.history-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.history-number {
  font-weight: 600;
  color: #1890ff;
  font-size: 12px;
}

.history-time {
  color: #8c8c8c;
  font-size: 11px;
}

.history-desc {
  color: #666;
  font-size: 12px;
  line-height: 1.4;
}

.log-time {
  color: #8c8c8c;
  font-size: 12px;
}

.log-action {
  color: #262626;
  font-weight: 500;
}

.log-user {
  color: #666;
  font-size: 12px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .edit-content {
    padding: 16px;
  }

  .edit-content .ant-col:last-child {
    margin-top: 24px;
  }
}

/* 表单优化 */
.ant-form-item {
  margin-bottom: 20px;
}

.ant-input,
.ant-select-selector,
.ant-picker {
  border-radius: 6px;
}

.ant-input-lg,
.ant-select-lg .ant-select-selector,
.ant-picker-large {
  height: 40px;
}

/* 步骤条样式优化 */
.ant-steps {
  margin-bottom: 40px;
}

.ant-steps-item-title {
  font-weight: 600 !important;
}

/* 卡片样式优化 */
.ant-card-small > .ant-card-head {
  padding: 8px 16px;
  min-height: 40px;
}

.ant-card-small > .ant-card-body {
  padding: 12px 16px;
}

/* 智能提示区域样式增强 */
.smart-tips {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  color: white;
  position: relative;
  overflow: hidden;
  animation: slideInRight 0.6s ease-out;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: linear-gradient(45deg, transparent, rgba(255,255,255,0.1), transparent);
    animation: shimmer 4s infinite;
  }

  .tips-header {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
    font-weight: 600;
    position: relative;
    z-index: 1;

    .anticon {
      margin-right: 8px;
      font-size: 16px;
    }
  }

  .tips-content {
    position: relative;
    z-index: 1;

    .tip-item {
      display: flex;
      align-items: flex-start;
      margin-bottom: 12px;
      animation: fadeInUp 0.6s ease-out;
      animation-fill-mode: both;

      .anticon {
        margin-right: 8px;
        margin-top: 2px;
        color: #ffd700;
      }

      &:nth-child(1) { animation-delay: 0.1s; }
      &:nth-child(2) { animation-delay: 0.2s; }
      &:nth-child(3) { animation-delay: 0.3s; }

      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}

/* 快捷操作按钮增强 */
.quick-actions .ant-btn {
  transition: all 0.3s ease;
  border-radius: 8px;
}

.quick-actions .ant-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

/* 表单输入框聚焦效果 */
.ant-input:focus,
.ant-select-selector:focus,
.ant-picker:focus {
  border-color: #40a9ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
  transform: scale(1.02);
  transition: all 0.3s ease;
}

/* 步骤内容切换动画 */
.step-content {
  animation: fadeInLeft 0.5s ease-out;
}

/* 侧边栏卡片动画 */
.info-panel .ant-card {
  animation: slideInRight 0.6s ease-out;
  animation-fill-mode: both;
}

.info-panel .ant-card:nth-child(1) { animation-delay: 0.1s; }
.info-panel .ant-card:nth-child(2) { animation-delay: 0.2s; }
.info-panel .ant-card:nth-child(3) { animation-delay: 0.3s; }
.info-panel .ant-card:nth-child(4) { animation-delay: 0.4s; }

/* 自动保存提示动画 */
.auto-save-indicator {
  position: fixed;
  top: 80px;
  right: 24px;
  background: rgba(82, 196, 26, 0.9);
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 12px;
  z-index: 1000;
  animation: slideInRight 0.3s ease-out;
  backdrop-filter: blur(10px);
}

/* 关键帧动画定义 */
@keyframes shimmer {
  0% { transform: translateX(-100%) translateY(-100%) rotate(45deg); }
  100% { transform: translateX(100%) translateY(100%) rotate(45deg); }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes fadeInLeft {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 加载状态样式 */
.form-loading {
  position: relative;
  opacity: 0.7;
}

.form-loading::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(2px);
  z-index: 10;
}

/* 成功状态样式 */
.success-feedback {
  border-left: 4px solid #52c41a;
  background: rgba(82, 196, 26, 0.05);
  padding: 12px 16px;
  border-radius: 0 6px 6px 0;
  margin: 16px 0;
  animation: slideInLeft 0.4s ease-out;
}

/* 警告状态样式 */
.warning-feedback {
  border-left: 4px solid #faad14;
  background: rgba(250, 173, 20, 0.05);
  padding: 12px 16px;
  border-radius: 0 6px 6px 0;
  margin: 16px 0;
  animation: slideInLeft 0.4s ease-out;
}

/* 编辑页面样式优化完成 - 智能地址输入功能已迁移到组件 */

/* 字段编辑指示器 */
.field-editing-indicator {
  position: relative;
  animation: fieldEditPulse 2s ease-in-out infinite;
}

.field-editing-indicator::before {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  background: linear-gradient(45deg, #40a9ff, #ff7875, #40a9ff);
  border-radius: 6px;
  z-index: -1;
  animation: fieldEditBorder 3s linear infinite;
}

.field-editing-indicator::after {
  content: '正在编辑...';
  position: absolute;
  top: -20px;
  right: 0;
  background: #1890ff;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  line-height: 1.2;
  z-index: 10;
  animation: fadeInOut 3s ease-in-out;
}

@keyframes fieldEditPulse {
  0%, 100% {
    box-shadow: 0 0 5px rgba(24, 144, 255, 0.5);
  }
  50% {
    box-shadow: 0 0 15px rgba(24, 144, 255, 0.8), 0 0 25px rgba(24, 144, 255, 0.3);
  }
}

@keyframes fieldEditBorder {
  0% {
    background-position: 0% 50%;
  }
  100% {
    background-position: 100% 50%;
  }
}

@keyframes fadeInOut {
  0% {
    opacity: 0;
    transform: translateY(-5px);
  }
  20%, 80% {
    opacity: 1;
    transform: translateY(0);
  }
  100% {
    opacity: 0;
    transform: translateY(-5px);
  }
}

/* 针对不同组件的编辑指示器样式调整 */
.compact-level-buttons.field-editing-indicator,
.compact-status-buttons.field-editing-indicator,
.compact-type-selector.field-editing-indicator {
  border-radius: 8px;
}

.location-input-group.field-editing-indicator {
  border-radius: 6px;
}

/* 确保指示器在所有输入组件上都能正确显示 */
.field-editing-indicator input,
.field-editing-indicator textarea,
.field-editing-indicator .ant-select-selector {
  position: relative;
  z-index: 1;
}

</style>修改时间戳: 2025年09月22日 16:08:28
