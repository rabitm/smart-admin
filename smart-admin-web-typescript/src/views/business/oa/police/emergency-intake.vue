<!--
  * 单屏智能接警界面 - 无滚动，高效录入
  *
  * @Author:    Claude Code Assistant
  * @Date:      2025-09-19
  * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
-->
<template>
  <div class="emergency-intake-page">
    <!-- 顶部操作栏 -->
    <div class="top-bar">
      <div class="title-section">
        <h1 class="page-title">🚨 {{ isEditMode ? '编辑警情' : '智能接警系统' }}</h1>
        <div class="status-info">
          <span class="current-time">{{ currentTime }}</span>
          <span class="operator-info">接警员：{{ operatorName }}</span>
        </div>
      </div>
      <div class="action-section">
        <a-button @click="goBack" size="large">
          <template #icon><ArrowLeftOutlined /></template>
          返回列表
        </a-button>
        <a-button
          type="primary"
          size="large"
          :loading="submitting"
          :disabled="!canSubmit"
          @click="submitReport"
          class="submit-btn"
        >
          🚨 {{ isEditMode ? '保存修改' : '立即处理' }} (Ctrl+Enter)
        </a-button>
      </div>
    </div>

    <!-- 主体内容 - 固定高度，无滚动 -->
    <div class="main-content">

      <!-- 左侧：灾害类型和基础信息 -->
      <div class="left-panel">
        <!-- 灾害类型选择 -->
        <div class="emergency-types">
          <h3 class="section-title">选择灾害类型</h3>
          <div class="type-grid">
            <div
              v-for="(type, key) in POLICE_REPORT_TYPE_ENUM"
              :key="key"
              :class="['type-item', {
                'active': formData.reportType === type.value,
                'emergency': type.category === 'emergency'
              }]"
              @click="selectType(type.value)"
              :title="`按 ${getHotkey(key)} 选择`"
            >
              <div class="type-icon" :style="{ color: type.color }">{{ type.icon }}</div>
              <div class="type-name">{{ type.desc }}</div>
              <div class="type-hotkey">{{ getHotkey(key) }}</div>
            </div>
          </div>
        </div>

        <!-- 基础信息 -->
        <div class="basic-info" v-if="formData.reportType">
          <h3 class="section-title">基础信息</h3>
          <div class="info-fields">
            <div class="field-item">
              <label>事发地点 *</label>
              <SmartLocationInput
                v-model="formData.incidentLocation"
                placeholder="输入地点关键字"
                :search-api="policeReportApi.searchLocationSuggestions"
                @select="handleLocationSelect"
              />
            </div>
            <div class="field-row">
              <div class="field-item">
                <label>报警人 *</label>
                <a-input
                  v-model:value="formData.reporterName"
                  placeholder="姓名"
                />
              </div>
              <div class="field-item">
                <label>电话 *</label>
                <a-input
                  v-model:value="formData.reporterPhone"
                  placeholder="联系电话"
                />
              </div>
            </div>
            <div class="field-item">
              <label>现场描述 *</label>
              <a-textarea
                v-model:value="formData.description"
                placeholder="详细描述现场情况（Ctrl+Enter提交）"
                :rows="3"
                :maxlength="500"
                show-count
                @keydown.ctrl.enter="submitReport"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：专业信息录入 -->
      <div class="right-panel" v-if="currentReportType">
        <h3 class="section-title">
          <span class="section-icon" :style="{ color: typeColor }">{{ typeIcon }}</span>
          {{ typeName }} 专业信息
        </h3>

        <!-- 配置加载状态 -->
        <div v-if="configLoading" class="config-loading">
          <div class="loading-icon">⏳</div>
          <div class="loading-text">正在加载表单配置...</div>
        </div>

        <PoliceProfessionalFields
          v-if="professionalFields.length > 0"
          :fields="professionalFields"
          :modelValue="professionalFieldData"
          @update:modelValue="handleProfessionalFieldUpdate"
        />

        <div v-else class="no-professional-fields">
          <div class="placeholder-icon">📝</div>
          <div class="placeholder-text">{{ typeName }} 无需额外专业信息</div>
          <div class="placeholder-desc">请完善左侧基础信息后提交</div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!currentReportType" class="empty-state">
        <div class="empty-icon">🚨</div>
        <div class="empty-title">请选择灾害类型开始接警</div>
        <div class="empty-desc">点击左侧灾害类型卡片或按数字键1-8快速选择</div>
      </div>
    </div>

    <!-- 底部进度条 -->
    <div class="progress-bar" v-if="formData.reportType">
      <div class="progress-items">
        <div :class="['progress-step', { completed: formData.reportType }]">
          <span class="step-icon">{{ formData.reportType ? '✓' : '1' }}</span>
          <span class="step-text">灾害类型</span>
        </div>
        <div :class="['progress-step', { completed: formData.incidentLocation }]">
          <span class="step-icon">{{ formData.incidentLocation ? '✓' : '2' }}</span>
          <span class="step-text">事发地点</span>
        </div>
        <div :class="['progress-step', { completed: formData.reporterName && formData.reporterPhone }]">
          <span class="step-icon">{{ (formData.reporterName && formData.reporterPhone) ? '✓' : '3' }}</span>
          <span class="step-text">报警人信息</span>
        </div>
        <div :class="['progress-step', { completed: formData.description }]">
          <span class="step-icon">{{ formData.description ? '✓' : '4' }}</span>
          <span class="step-text">现场描述</span>
        </div>
      </div>
      <div class="progress-indicator">
        <div class="progress-fill" :style="{ width: `${progressPercentage}%` }"></div>
      </div>
    </div>

    <!-- 智能提示 -->
    <div v-if="currentTip" class="smart-tip">
      <span class="tip-icon">💡</span>
      <span class="tip-text">{{ currentTip }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue';
  import { useRouter, useRoute } from 'vue-router';
  import { message } from 'ant-design-vue';
  import { ArrowLeftOutlined } from '@ant-design/icons-vue';
  import SmartLocationInput from '/@/components/framework/smart-location-input/index.vue';
  import PoliceProfessionalFields from '/@/components/business/emergency/police-professional-fields.vue';
  import { POLICE_REPORT_TYPE_ENUM, EMERGENCY_FORM_CONFIG } from '/@/constants/business/oa/police-report-const';
  import { policeReportApi } from '/@/api/business/oa/police-report-api';
  import { policeFormConfigApi, type PoliceFormConfigVO } from '/@/api/business/oa/police-form-config-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import { DICT_CODE_ENUM } from '/@/constants/support/dict-const';
  import dayjs from 'dayjs';

  const router = useRouter();
  const route = useRoute();

  // 编辑模式状态
  const isEditMode = ref(false);
  const editReportId = ref<number | null>(null);

  // 字段键名到数据字典代码的映射
  const FIELD_DICT_MAPPING: Record<string, string> = {
    'trappedCount': DICT_CODE_ENUM.TRAPPED_COUNT,
    'casualties': DICT_CODE_ENUM.CASUALTIES_LEVEL,
    'fireFloor': DICT_CODE_ENUM.FIRE_FLOOR,
    'fireScale': DICT_CODE_ENUM.FIRE_SCALE,
    'burningMaterial': DICT_CODE_ENUM.BURNING_MATERIAL,
    'smokeCondition': DICT_CODE_ENUM.SMOKE_CONDITION,
    'fireSource': DICT_CODE_ENUM.FIRE_SOURCE,
    'rescueEquipment': DICT_CODE_ENUM.RESCUE_EQUIPMENT,
    'rescueType': DICT_CODE_ENUM.RESCUE_TYPE,
    'dangerLevel': DICT_CODE_ENUM.DANGER_LEVEL,
    'injuryLevel': DICT_CODE_ENUM.INJURY_LEVEL,
    'emergencyType': DICT_CODE_ENUM.EMERGENCY_TYPE,
    'consciousness': DICT_CODE_ENUM.CONSCIOUSNESS_STATE,
    'accidentType': DICT_CODE_ENUM.ACCIDENT_TYPE,
    'vehicleCount': DICT_CODE_ENUM.VEHICLE_COUNT,
    'roadBlock': DICT_CODE_ENUM.ROAD_BLOCK_LEVEL,
    'roadCondition': DICT_CODE_ENUM.ROAD_CONDITION,
    'weatherCondition': DICT_CODE_ENUM.WEATHER_CONDITION,
    'eventNature': DICT_CODE_ENUM.SECURITY_EVENT_NATURE,
    'involvedCount': DICT_CODE_ENUM.INVOLVEMENT_COUNT,
    'weaponInvolved': DICT_CODE_ENUM.WEAPON_TYPE,
    'caseNature': DICT_CODE_ENUM.CRIMINAL_CASE_NATURE,
    'victimCount': DICT_CODE_ENUM.INVOLVEMENT_COUNT,
    'urgencyLevel': DICT_CODE_ENUM.URGENCY_LEVEL,
    'suspectStatus': DICT_CODE_ENUM.SUSPECT_STATUS,
    'sceneProtection': DICT_CODE_ENUM.SCENE_PROTECTION,
    'disasterType': DICT_CODE_ENUM.DISASTER_TYPE,
    'affectedArea': DICT_CODE_ENUM.AFFECTED_AREA,
    'rescueNeeds': DICT_CODE_ENUM.RESCUE_NEEDS
  };

  // 状态数据
  const submitting = ref(false);
  const currentTip = ref('');
  const currentTime = ref('');
  const operatorName = ref('张警官'); // 可从用户信息获取
  const dynamicFormConfig = ref<PoliceFormConfigVO | null>(null);
  const configLoading = ref(false);

  // 基本表单数据
  const formData = reactive({
    reportType: null,
    reportLevel: 1, // 默认紧急
    reporterName: '',
    reporterPhone: '',
    reporterIdCard: '',
    reportTime: null,
    incidentLocation: '',
    description: '',
    status: 1,
    handlerName: '',
    handleResult: '',
    handleTime: null,
    attachments: '',
    remark: '',
  });

  // 专业字段数据（独立存储，避免污染基本表单响应式）
  const professionalFieldData = reactive({});

  // 专门的响应式变量用于报告类型，避免整个formData响应式污染
  const currentReportType = ref(null);

  // 计算属性 - 只依赖reportType和config，不依赖整个formData
  const professionalFields = computed(() => {
    if (!currentReportType.value || !dynamicFormConfig.value) return [];
    return [...(dynamicFormConfig.value.step2Fields || []), ...(dynamicFormConfig.value.step3Fields || [])];
  });


  const progressPercentage = computed(() => {
    let completed = 0;
    if (formData.reportType) completed += 25;
    if (formData.incidentLocation) completed += 25;
    if (formData.reporterName && formData.reporterPhone) completed += 25;
    if (formData.description) completed += 25;
    return completed;
  });

  const canSubmit = computed(() => {
    return !!(
      formData.reportType &&
      formData.incidentLocation?.trim() &&
      formData.reporterName?.trim() &&
      formData.reporterPhone?.trim() &&
      formData.description?.trim()
    );
  });

  const typeName = computed(() => {
    if (!currentReportType.value) return '';
    const type = Object.values(POLICE_REPORT_TYPE_ENUM).find(t => t.value === currentReportType.value);
    return type ? type.desc : '';
  });

  const typeIcon = computed(() => {
    if (!currentReportType.value) return '';
    const type = Object.values(POLICE_REPORT_TYPE_ENUM).find(t => t.value === currentReportType.value);
    return type ? type.icon : '';
  });

  const typeColor = computed(() => {
    if (!currentReportType.value) return '#666';
    const type = Object.values(POLICE_REPORT_TYPE_ENUM).find(t => t.value === currentReportType.value);
    return type ? type.color : '#666';
  });

  // 方法
  function updateTime() {
    currentTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
  }

  function getHotkey(key: string): string {
    const hotkeys = ['1', '2', '3', '4', '5', '6', '7', '8'];
    const index = Object.keys(POLICE_REPORT_TYPE_ENUM).indexOf(key);
    return hotkeys[index] || '';
  }

  async function selectType(value: number) {
    formData.reportType = value;
    currentReportType.value = value;
    // 清除动态字段
    clearDynamicFields();
    updateTip(`已选择${typeName.value}，请继续填写信息`);

    // 加载动态表单配置
    await loadFormConfig(value);
  }

  function clearDynamicFields() {
    // 清空专业字段数据
    Object.keys(professionalFieldData).forEach(key => {
      delete professionalFieldData[key];
    });
  }

  function handleLocationSelect(option: any) {
    updateTip(`地址已选择: ${option.value}`);
  }

  // 处理专业字段数据更新
  function handleProfessionalFieldUpdate(newValue: any) {
    // 清空原有数据
    Object.keys(professionalFieldData).forEach(key => {
      delete professionalFieldData[key];
    });
    // 设置新数据
    Object.assign(professionalFieldData, newValue);
  }

  // 加载表单配置
  async function loadFormConfig(reportType: number) {
    if (!reportType) return;

    try {
      configLoading.value = true;
      const response = await policeFormConfigApi.getFormConfig(reportType);
      if (response.data) {
        // API返回的配置数据，需要转换为数据字典格式
        const convertedConfig = convertConfigToDictFormat(response.data);
        dynamicFormConfig.value = convertedConfig;
      } else {
        // 没有配置数据时使用默认配置
        dynamicFormConfig.value = createDefaultFormConfig(reportType);
      }
    } catch (error) {
      console.error('加载表单配置失败:', error);
      message.error('加载表单配置失败，将使用默认配置');
      // 使用默认配置
      dynamicFormConfig.value = createDefaultFormConfig(reportType);
    } finally {
      configLoading.value = false;
    }
  }

  // 转换API配置为数据字典格式
  function convertConfigToDictFormat(apiConfig: PoliceFormConfigVO): PoliceFormConfigVO {
    const convertFields = (fields: any[]) => {
      return fields.map(field => {
        const newField = { ...field };

        // 获取字段键名 (兼容两种API格式)
        const fieldKey = newField.fieldKey || newField.key;

        // 如果该字段有对应的数据字典映射，且没有设置dictCode，则添加dictCode并移除options
        if (fieldKey && FIELD_DICT_MAPPING[fieldKey] && !newField.dictCode) {
          newField.dictCode = FIELD_DICT_MAPPING[fieldKey];
          // 移除硬编码的选项
          delete newField.fieldOptions;
          delete newField.options;
          delete newField.quickOptions;
        }

        // 递归处理子字段
        if (newField.children && newField.children.length > 0) {
          newField.children = convertFields(newField.children);
        }
        if (newField.fields && newField.fields.length > 0) {
          newField.fields = convertFields(newField.fields);
        }

        return newField;
      });
    };

    return {
      ...apiConfig,
      step2Fields: convertFields(apiConfig.step2Fields || []),
      step3Fields: convertFields(apiConfig.step3Fields || [])
    };
  }

  // 创建默认表单配置
  function createDefaultFormConfig(reportType: number) {
    // 从常量中获取配置
    const config = EMERGENCY_FORM_CONFIG[reportType];

    if (config) {
      // 获取类型名称
      const typeInfo = Object.values(POLICE_REPORT_TYPE_ENUM).find(t => t.value === reportType);
      const typeName = typeInfo ? typeInfo.desc : '未知类型';

      return {
        reportType,
        templateId: 0,
        templateName: `${typeName}默认模板`,
        step2Fields: config.step2Fields || [],
        step3Fields: config.step3Fields || []
      };
    }

    // 无配置的类型
    const typeInfo = Object.values(POLICE_REPORT_TYPE_ENUM).find(t => t.value === reportType);
    const typeName = typeInfo ? typeInfo.desc : '未知类型';

    return {
      reportType,
      templateId: 0,
      templateName: `${typeName}默认模板`,
      step2Fields: [],
      step3Fields: []
    };
  }

  async function submitReport() {
    if (!canSubmit.value) {
      message.error('请填写完整的必填信息');
      return;
    }

    try {
      submitting.value = true;

      // 准备提交数据 - 合并基本数据和专业字段数据
      const submitData = {
        ...formData,
        ...professionalFieldData,
        professionalFields: professionalFieldData // 专门传递专业字段给后端
      };
      submitData.reportTime = dayjs().format('YYYY-MM-DD HH:mm:ss');

      if (isEditMode.value && editReportId.value) {
        // 编辑模式，调用更新API
        submitData.reportId = editReportId.value;
        await policeReportApi.updatePoliceReport(submitData);
        message.success('🚨 警情信息更新成功！');

        // 编辑模式完成后返回列表页
        setTimeout(() => {
          router.push('/oa/police/report-list');
        }, 1000);
      } else {
        // 新增模式，调用新增API
        await policeReportApi.addPoliceReport(submitData);
        message.success('🚨 接警信息录入成功！已自动分派处理');

        // 清空表单，准备下一次录入
        Object.assign(formData, {
          reportType: null,
          reportLevel: 1,
          reporterName: '',
          reporterPhone: '',
          reporterIdCard: '',
          reportTime: null,
          incidentLocation: '',
          description: '',
          status: 1,
          handlerName: '',
          handleResult: '',
          handleTime: null,
          attachments: '',
          remark: '',
        });
        currentReportType.value = null;
        clearDynamicFields();

        updateTip('录入完成，可继续接警或返回列表');
      }

    } catch (error) {
      smartSentry.captureError(error);
      message.error('提交失败，请重试');
    } finally {
      submitting.value = false;
    }
  }

  function goBack() {
    router.back();
  }

  function updateTip(text: string) {
    currentTip.value = text;
    setTimeout(() => currentTip.value = '', 3000);
  }

  // 键盘快捷键
  function handleKeydown(e: KeyboardEvent) {
    if (e.ctrlKey && e.key === 'Enter') {
      e.preventDefault();
      submitReport();
    }

    // 数字键选择类型 - 只有在没有焦点在输入框时才响应
    if (!e.ctrlKey && !e.altKey && /^[1-8]$/.test(e.key)) {
      const activeElement = document.activeElement;
      const isInputFocused = activeElement && (
        activeElement.tagName === 'INPUT' ||
        activeElement.tagName === 'TEXTAREA' ||
        activeElement.contentEditable === 'true' ||
        activeElement.getAttribute('contenteditable') === 'true'
      );

      // 如果有输入框获得焦点，不响应数字键快捷键
      if (isInputFocused) {
        return;
      }

      const types = Object.values(POLICE_REPORT_TYPE_ENUM);
      const index = parseInt(e.key) - 1;
      if (types[index]) {
        selectType(types[index].value);
      }
    }
  }

  // 初始化页面，检查是否编辑模式
  async function initPage() {
    // 检查路由参数
    const reportId = route.query.reportId;
    const mode = route.query.mode;

    if (reportId && mode === 'edit') {
      isEditMode.value = true;
      editReportId.value = Number(reportId);
      await loadEditData(editReportId.value);
    }
  }

  // 加载编辑数据
  async function loadEditData(reportId: number) {
    try {
      const response = await policeReportApi.getDetail(reportId);
      if (response.data) {
        const data = response.data;

        // 加载基本数据
        Object.assign(formData, {
          reportType: data.reportType,
          reportLevel: data.reportLevel,
          reporterName: data.reporterName,
          reporterPhone: data.reporterPhone,
          reporterIdCard: data.reporterIdCard,
          reportTime: data.reportTime,
          incidentLocation: data.incidentLocation,
          description: data.description,
          handlerName: data.handlerName,
          attachments: data.attachments,
          remark: data.remark,
        });

        // 设置当前报告类型
        currentReportType.value = data.reportType;

        // 加载表单配置
        await loadFormConfig(data.reportType);

        // TODO: 加载专业字段数据
        await loadProfessionalFieldData(reportId);

        message.success('警情数据加载成功');
      }
    } catch (error) {
      console.error('加载编辑数据失败:', error);
      message.error('加载警情数据失败');
      // 如果加载失败，回到列表页
      router.push('/oa/police/report-list');
    }
  }

  // 加载专业字段数据
  async function loadProfessionalFieldData(reportId: number) {
    try {
      const response = await policeReportApi.getPoliceReportFieldData(reportId);
      if (response.data) {
        console.log('🔍 专业字段数据:', response.data);

        // 直接将专业字段数据加载到 professionalFieldData
        // 后端已经正确处理了数组的序列化和反序列化
        Object.assign(professionalFieldData, response.data);
        console.log('✅ 已加载专业字段数据:', response.data);
      }
    } catch (error) {
      console.error('加载专业字段数据失败:', error);
      // 如果加载失败也不阻断编辑流程
    }
  }

  onMounted(() => {
    updateTime();
    setInterval(updateTime, 1000);
    document.addEventListener('keydown', handleKeydown);

    // 初始化页面
    initPage();
  });

  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeydown);
  });
</script>

<style scoped>
.emergency-intake-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow: hidden;
  position: relative;
}

.emergency-intake-page::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="20" cy="20" r="1" fill="rgba(255,255,255,0.03)"/><circle cx="80" cy="80" r="1" fill="rgba(255,255,255,0.03)"/><circle cx="40" cy="60" r="1" fill="rgba(255,255,255,0.03)"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
  pointer-events: none;
}

/* 顶部操作栏 */
.top-bar {
  height: 60px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 10;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 24px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
}

.status-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.current-time {
  font-size: 16px;
  font-weight: 600;
  color: #3b82f6;
}

.operator-info {
  font-size: 14px;
  color: #666;
}

.action-section {
  display: flex;
  gap: 16px;
}

.submit-btn {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  border-color: #ef4444;
  font-weight: 600;
}

/* 主体内容 */
.main-content {
  flex: 1;
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 20px;
  padding: 20px;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.left-panel,
.right-panel {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(15px);
  border-radius: 16px;
  padding: 20px;
  overflow: hidden;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.12),
    0 2px 8px rgba(0, 0, 0, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
  height: calc(100vh - 140px);
  max-height: calc(100vh - 140px);
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.left-panel:hover,
.right-panel:hover {
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.15),
    0 4px 12px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  transform: translateY(-2px);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #f3f4f6;
  display: flex;
  align-items: center;
}

.section-icon {
  font-size: 20px;
  margin-right: 8px;
}

/* 灾害类型网格 */
.type-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 20px;
}

.type-item {
  position: relative;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  border: 2px solid rgba(226, 232, 240, 0.8);
  border-radius: 12px;
  padding: 14px 10px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  text-align: center;
  min-height: 75px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  overflow: hidden;
  backdrop-filter: blur(5px);
}

.type-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.6), transparent);
  transition: left 0.6s;
}

.type-item:hover::before {
  left: 100%;
}

.type-item:hover {
  border-color: #3b82f6;
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.3);
  transform: translateY(-3px);
  background: #eff6ff;
  transform: translateY(-1px);
}

.type-item.active {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-color: #1d4ed8;
  color: white;
  box-shadow:
    0 8px 32px rgba(59, 130, 246, 0.4),
    0 4px 16px rgba(59, 130, 246, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.type-item.active .type-name {
  color: white;
}

.type-item.emergency.active {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  border-color: #b91c1c;
  color: white;
  box-shadow:
    0 8px 32px rgba(239, 68, 68, 0.4),
    0 4px 16px rgba(239, 68, 68, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.type-item.emergency.active .type-name {
  color: white;
}

.type-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.type-name {
  font-size: 12px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 4px;
}

.type-hotkey {
  position: absolute;
  top: 4px;
  right: 4px;
  background: #6b7280;
  color: white;
  font-size: 10px;
  padding: 2px 4px;
  border-radius: 3px;
}

/* 基础信息字段 */
.info-fields {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: relative;
}

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.field-item label {
  font-size: 13px;
  font-weight: 600;
  color: #4b5563;
  margin-bottom: 2px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.field-item label::before {
  content: '';
  width: 3px;
  height: 14px;
  background: linear-gradient(to bottom, #3b82f6, #1d4ed8);
  border-radius: 2px;
}

/* 专业信息字段 */
.professional-fields {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: calc(100vh - 280px);
  overflow: hidden;
}

.pro-field {
  background: #f9fafb;
  border-radius: 6px;
  padding: 8px;
  border: 1px solid #e5e7eb;
}

/* 紧凑组字段样式 - 与其他字段保持一致 */
.compact-group-field {
  padding: 12px;
  background: #f9fafb;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  margin-bottom: 12px;
}

.compact-group-field .pro-label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.compact-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.compact-subfield {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.subfield-label {
  font-size: 13px;
  color: #374151;
  font-weight: 500;
  min-width: 50px;
  flex-shrink: 0;
}

.compact-options {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  flex: 1;
}

.compact-btn {
  padding: 6px 12px;
  font-size: 13px;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  line-height: 1.3;
  font-weight: 500;
}

.compact-btn:hover {
  border-color: #3b82f6;
  background: #eff6ff;
  color: #1d4ed8;
}

.compact-btn.selected {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
  font-weight: 600;
}

/* 紧凑多选样式 - 与其他字段保持一致 */
.checkbox-compact {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  margin-bottom: 12px;
}

.compact-checkbox-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-size: 13px;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 500;
}

.compact-checkbox-btn:hover {
  border-color: #3b82f6;
  background: #eff6ff;
  color: #1d4ed8;
}

.compact-checkbox-btn.selected {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
  font-weight: 600;
}

.pro-label {
  display: flex;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
}

.required {
  color: #ef4444;
  margin-left: 4px;
}

.quick-options,
.select-options,
.checkbox-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-btn,
.select-btn,
.checkbox-btn {
  position: relative;
  padding: 8px 12px;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.quick-btn:hover,
.select-btn:hover,
.checkbox-btn:hover {
  border-color: #3b82f6;
  background: #eff6ff;
}

.quick-btn.selected,
.select-btn.selected,
.checkbox-btn.selected {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.btn-hotkey {
  background: rgba(255,255,255,0.3);
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 10px;
}

.check-icon {
  width: 14px;
  height: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #6b7280;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
}

.no-professional-fields {
  text-align: center;
  padding: 40px 20px;
  color: #6b7280;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.placeholder-text {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.placeholder-desc {
  font-size: 14px;
}

/* 底部进度条 */
.progress-bar {
  height: 60px;
  background: white;
  border-top: 1px solid #e8e8e8;
  padding: 12px 24px;
  flex-shrink: 0;
}

.progress-items {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-bottom: 8px;
}

.progress-step {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #6b7280;
}

.progress-step.completed {
  color: #059669;
}

.step-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.progress-step.completed .step-icon {
  background: #10b981;
  color: white;
}

.progress-indicator {
  height: 4px;
  background: #f3f4f6;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981 0%, #059669 100%);
  transition: width 0.3s ease;
}

/* 智能提示 */
.smart-tip {
  position: fixed;
  top: 80px;
  right: 24px;
  background: #1890ff;
  color: white;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 8px;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(100%);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 配置加载状态样式 */
.config-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #6b7280;
}

.loading-icon {
  font-size: 48px;
  margin-bottom: 12px;
  animation: pulse 1.5s ease-in-out infinite;
}

.loading-text {
  font-size: 16px;
  font-weight: 500;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* 强制更新缓存: 2025年09月22日 16:27:45 */
</style>