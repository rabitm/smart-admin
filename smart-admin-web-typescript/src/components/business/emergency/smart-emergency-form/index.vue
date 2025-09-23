<!--
  * 大气专业接警界面 - 大屏操作，信息充分展示
  *
  * @Author:    Claude Code Assistant
  * @Date:      2025-09-19
  * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
-->
<template>
  <div class="professional-emergency-form">

    <!-- 顶部：灾害类型选择 -->
    <div class="emergency-type-section">
      <h2 class="section-title">🚨 选择灾害类型</h2>
      <div class="type-grid">
        <div
          v-for="(type, key) in POLICE_REPORT_TYPE_ENUM"
          :key="key"
          :class="[
            'type-card',
            {
              'active': formData.emergencyType === type.value,
              'emergency': type.category === 'emergency',
              'accident': type.category === 'accident',
              'security': type.category === 'security'
            }
          ]"
          @click="selectType(type.value)"
          :title="`快捷键: ${getTypeHotkey(key)}`"
        >
          <div class="type-header">
            <span class="type-icon" :style="{ color: type.color }">{{ type.icon }}</span>
            <span class="hotkey-badge">{{ getTypeHotkey(key) }}</span>
          </div>
          <div class="type-title">{{ type.desc }}</div>
          <div class="type-category">{{ getCategoryName(type.category) }}</div>
        </div>
      </div>
    </div>

    <!-- 中部：信息录入区 -->
    <div class="info-input-section" v-if="formData.emergencyType">

      <!-- 左侧：基础信息 -->
      <div class="basic-info-panel">
        <h3 class="panel-title">📍 基础信息</h3>

        <div class="info-grid">
          <div class="info-field full-width">
            <label class="field-label">事发地点</label>
            <SmartLocationInput
              v-model="formData.incidentLocation"
              placeholder="请输入详细地址，支持智能联想"
              :search-api="policeReportApi.searchLocationSuggestions"
              size="large"
              @select="handleLocationSelect"
            />
          </div>

          <div class="info-field">
            <label class="field-label">报警人姓名 *</label>
            <a-input
              v-model:value="formData.reporterName"
              placeholder="请输入报警人姓名"
              size="large"
            />
          </div>

          <div class="info-field">
            <label class="field-label">联系电话 *</label>
            <a-input
              v-model:value="formData.reporterPhone"
              placeholder="请输入联系电话"
              size="large"
            />
          </div>

          <div class="info-field full-width">
            <label class="field-label">现场描述 *</label>
            <a-textarea
              v-model:value="formData.description"
              placeholder="请详细描述现场情况、事件经过、紧急程度等关键信息（Ctrl+Enter快速提交）"
              :rows="4"
              :maxlength="500"
              show-count
              @keydown.ctrl.enter="submitForm"
            />
          </div>
        </div>
      </div>

      <!-- 右侧：专业信息 -->
      <div class="professional-info-panel">
        <h3 class="panel-title">
          <span class="panel-icon" :style="{ color: getSelectedTypeColor() }">
            {{ getSelectedTypeIcon() }}
          </span>
          {{ getTypeName() }} 专业信息
        </h3>

        <div class="pro-fields" v-if="coreFields.length > 0">
          <div
            v-for="field in coreFields"
            :key="field.key"
            class="pro-field"
            :class="{ 'compact-group-field': field.type === 'compact-group' }"
          >
            <label class="pro-label">
              <span class="field-icon">{{ field.icon }}</span>
              {{ field.label }}
              <span v-if="field.required" class="required">*</span>
            </label>

            <!-- 紧凑组合字段 -->
            <div v-if="field.type === 'compact-group'" class="compact-group">
              <div
                v-for="subField in field.fields"
                :key="subField.key"
                class="compact-subfield"
              >
                <span class="subfield-label">{{ subField.label }}</span>
                <!-- 快捷选项 -->
                <div v-if="subField.quickOptions" class="compact-options">
                  <button
                    v-for="option in subField.quickOptions"
                    :key="option"
                    :class="['compact-btn', { 'selected': isSelected(subField.key, option) }]"
                    @click="selectQuick(subField.key, option)"
                  >
                    {{ option }}
                  </button>
                </div>
                <!-- 选择选项 -->
                <div v-else-if="subField.options" class="compact-options">
                  <button
                    v-for="option in subField.options"
                    :key="option"
                    :class="['compact-btn', { 'selected': formData.dynamicData[subField.key] === option }]"
                    @click="formData.dynamicData[subField.key] = option"
                  >
                    {{ option }}
                  </button>
                </div>
                <!-- 输入框 -->
                <a-input
                  v-else-if="subField.type === 'input'"
                  v-model:value="formData.dynamicData[subField.key]"
                  :placeholder="subField.placeholder"
                  size="small"
                  style="width: 100px;"
                />
              </div>
            </div>

            <!-- 紧凑多选 -->
            <div v-else-if="field.type === 'checkbox-compact'" class="checkbox-compact">
              <button
                v-for="option in field.options"
                :key="option"
                :class="['compact-checkbox-btn', { 'selected': isCheckboxSelected(field.key, option) }]"
                @click="toggleCheckbox(field.key, option)"
              >
                <span class="checkbox-icon">{{ isCheckboxSelected(field.key, option) ? '✓' : '' }}</span>
                {{ option }}
              </button>
            </div>

            <!-- 快捷选项 -->
            <div v-else-if="field.quickOptions" class="option-buttons">
              <button
                v-for="(option, idx) in field.quickOptions"
                :key="option"
                :class="['option-btn', { 'selected': isSelected(field.key, option) }]"
                @click="selectQuick(field.key, option)"
                :title="`快捷键: ${idx + 1}`"
              >
                <span class="btn-text">{{ option }}</span>
                <span class="btn-hotkey">{{ idx + 1 }}</span>
              </button>
            </div>

            <!-- 选择选项 -->
            <div v-else-if="field.type === 'select'" class="option-buttons">
              <button
                v-for="option in field.options"
                :key="option"
                :class="['option-btn', { 'selected': formData.dynamicData[field.key] === option }]"
                @click="formData.dynamicData[field.key] = option"
              >
                {{ option }}
              </button>
            </div>

            <!-- 多选选项 -->
            <div v-else-if="field.type === 'checkbox'" class="option-buttons multi-select">
              <button
                v-for="option in field.options"
                :key="option"
                :class="['option-btn', { 'selected': isCheckboxSelected(field.key, option) }]"
                @click="toggleCheckbox(field.key, option)"
              >
                <span class="checkbox-icon">{{ isCheckboxSelected(field.key, option) ? '✓' : '' }}</span>
                {{ option }}
              </button>
            </div>

            <!-- 文本输入 -->
            <a-input
              v-else-if="field.type === 'input'"
              v-model:value="formData.dynamicData[field.key]"
              :placeholder="field.placeholder || `请输入${field.label}`"
              size="large"
            />

            <!-- 文本域 -->
            <a-textarea
              v-else-if="field.type === 'textarea'"
              v-model:value="formData.dynamicData[field.key]"
              :placeholder="field.placeholder || `请输入${field.label}`"
              :rows="3"
              :maxlength="300"
              show-count
            />

            <!-- 数字输入 -->
            <a-input-number
              v-else-if="field.type === 'number'"
              v-model:value="formData.dynamicData[field.key]"
              :placeholder="field.placeholder || `请输入${field.label}`"
              :min="0"
              :max="9999"
              size="large"
              style="width: 100%"
            />
          </div>
        </div>

        <div v-else class="no-fields">
          <div class="no-fields-icon">📝</div>
          <div class="no-fields-text">已选择 {{ getTypeName() }}，请填写左侧基础信息</div>
        </div>
      </div>
    </div>

    <!-- 底部：操作区 -->
    <div class="action-section" v-if="formData.emergencyType">
      <div class="action-left">
        <div class="progress-info">
          <div class="progress-title">录入进度</div>
          <div class="progress-items">
            <span :class="['progress-item', { completed: formData.emergencyType }]">
              {{ formData.emergencyType ? '✓' : '○' }} 灾害类型
            </span>
            <span :class="['progress-item', { completed: formData.incidentLocation }]">
              {{ formData.incidentLocation ? '✓' : '○' }} 事发地点
            </span>
            <span :class="['progress-item', { completed: formData.reporterName && formData.reporterPhone }]">
              {{ (formData.reporterName && formData.reporterPhone) ? '✓' : '○' }} 报警人信息
            </span>
            <span :class="['progress-item', { completed: formData.description }]">
              {{ formData.description ? '✓' : '○' }} 现场描述
            </span>
          </div>
        </div>
      </div>

      <div class="action-right">
        <a-button
          type="primary"
          size="large"
          :loading="submitting"
          :disabled="!canSubmit()"
          @click="submitForm"
          class="submit-button"
        >
          <span class="submit-icon">🚨</span>
          立即处理警情
          <span class="submit-hotkey">(Ctrl+Enter)</span>
        </a-button>
      </div>
    </div>

    <!-- 智能提示 -->
    <div v-if="currentTip" class="smart-notification">
      <div class="notification-content">
        <span class="notification-icon">💡</span>
        <span class="notification-text">{{ currentTip }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, computed, onMounted, onUnmounted } from 'vue';
  import { message } from 'ant-design-vue';
  import SmartLocationInput from '/@/components/framework/smart-location-input/index.vue';
  import { POLICE_REPORT_TYPE_ENUM, EMERGENCY_FORM_CONFIG } from '/@/constants/business/oa/police-report-const';
  import { policeReportApi } from '/@/api/business/oa/police-report-api';

  // Emits
  interface Emits {
    (e: 'submit', data: any): void;
  }

  const emit = defineEmits<Emits>();

  // 状态数据
  const submitting = ref(false);
  const currentTip = ref('');

  // 表单数据
  const formData = reactive({
    emergencyType: null,
    incidentLocation: '',
    reporterName: '',
    reporterPhone: '',
    description: '',
    dynamicData: {} as Record<string, any>
  });

  // 计算属性
  const coreFields = computed(() => {
    if (!formData.emergencyType) return [];
    const config = EMERGENCY_FORM_CONFIG[formData.emergencyType];
    return [...(config?.step2Fields || []), ...(config?.step3Fields || [])];
  });

  // 方法
  function getTypeHotkey(key: string): string {
    const hotkeys = ['1', '2', '3', '4', '5', '6', '7', '8'];
    const index = Object.keys(POLICE_REPORT_TYPE_ENUM).indexOf(key);
    return hotkeys[index] || '';
  }

  function getCategoryName(category: string): string {
    const names = {
      emergency: '紧急事件',
      accident: '事故处理',
      security: '安全事件',
      other: '其他事件'
    };
    return names[category] || category;
  }

  function getTypeName(): string {
    if (!formData.emergencyType) return '';
    const type = Object.values(POLICE_REPORT_TYPE_ENUM).find(t => t.value === formData.emergencyType);
    return type ? type.desc : '';
  }

  function getSelectedTypeIcon(): string {
    if (!formData.emergencyType) return '';
    const type = Object.values(POLICE_REPORT_TYPE_ENUM).find(t => t.value === formData.emergencyType);
    return type ? type.icon : '';
  }

  function getSelectedTypeColor(): string {
    if (!formData.emergencyType) return '#666';
    const type = Object.values(POLICE_REPORT_TYPE_ENUM).find(t => t.value === formData.emergencyType);
    return type ? type.color : '#666';
  }

  function selectType(value: number) {
    formData.emergencyType = value;
    formData.dynamicData = {}; // 清空动态数据
    updateTip();
  }

  function handleLocationSelect(option: any) {
    currentTip.value = `地址已选择: ${option.value}`;
    setTimeout(() => currentTip.value = '', 2000);
  }

  function isSelected(fieldKey: string, option: string): boolean {
    return formData.dynamicData[fieldKey] === option ||
           formData.dynamicData[fieldKey] === extractNumber(option);
  }

  function isCheckboxSelected(fieldKey: string, option: string): boolean {
    if (!formData.dynamicData[fieldKey]) return false;
    if (Array.isArray(formData.dynamicData[fieldKey])) {
      return formData.dynamicData[fieldKey].includes(option);
    }
    return formData.dynamicData[fieldKey] === option;
  }

  function toggleCheckbox(fieldKey: string, option: string) {
    if (!formData.dynamicData[fieldKey]) {
      formData.dynamicData[fieldKey] = [];
    }
    if (!Array.isArray(formData.dynamicData[fieldKey])) {
      formData.dynamicData[fieldKey] = [formData.dynamicData[fieldKey]];
    }

    const index = formData.dynamicData[fieldKey].indexOf(option);
    if (index > -1) {
      formData.dynamicData[fieldKey].splice(index, 1);
    } else {
      formData.dynamicData[fieldKey].push(option);
    }
  }

  function selectQuick(fieldKey: string, option: string) {
    if (option === '无' || option === '0') {
      formData.dynamicData[fieldKey] = 0;
    } else {
      const num = extractNumber(option);
      formData.dynamicData[fieldKey] = num !== null ? num : option;
    }
  }

  function extractNumber(text: string): number | null {
    const match = text.match(/(\d+)/);
    return match ? parseInt(match[1]) : null;
  }

  function canSubmit(): boolean {
    return !!(
      formData.emergencyType &&
      formData.incidentLocation &&
      formData.reporterName &&
      formData.reporterPhone &&
      formData.description
    );
  }

  async function submitForm() {
    if (!canSubmit()) {
      message.error('请填写完整信息');
      return;
    }

    try {
      submitting.value = true;

      const submitData = {
        emergencyType: formData.emergencyType,
        incidentLocation: formData.incidentLocation,
        reporterName: formData.reporterName,
        reporterPhone: formData.reporterPhone,
        description: formData.description,
        dynamicData: formData.dynamicData,
        urgencyLevel: 1, // 默认紧急
        reportTime: new Date().toISOString()
      };

      emit('submit', submitData);
      currentTip.value = '🚨 接警信息提交成功！已分派处理';

    } catch (error) {
      message.error('提交失败，请重试');
    } finally {
      submitting.value = false;
    }
  }

  function updateTip() {
    if (formData.emergencyType) {
      const type = Object.values(POLICE_REPORT_TYPE_ENUM).find(t => t.value === formData.emergencyType);
      if (type?.category === 'emergency') {
        currentTip.value = '⚠️ 紧急事件类型，请快速完成信息录入';
      } else {
        currentTip.value = `✅ 已选择${type?.desc}，请继续填写相关信息`;
      }
      setTimeout(() => currentTip.value = '', 3000);
    }
  }

  // 键盘快捷键
  function handleKeydown(e: KeyboardEvent) {
    if (e.ctrlKey && e.key === 'Enter') {
      e.preventDefault();
      submitForm();
    }

    // 数字键选择类型
    if (!e.ctrlKey && !e.altKey && /^[1-8]$/.test(e.key)) {
      const types = Object.values(POLICE_REPORT_TYPE_ENUM);
      const index = parseInt(e.key) - 1;
      if (types[index]) {
        selectType(types[index].value);
      }
    }
  }

  onMounted(() => {
    document.addEventListener('keydown', handleKeydown);
  });

  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeydown);
  });

  // 暴露方法
  defineExpose({
    handleSubmit: submitForm
  });
</script>

<style scoped>
.professional-emergency-form {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 24px;
}

/* 顶部：灾害类型选择 */
.emergency-type-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 20px;
  text-align: center;
  text-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  max-width: 1200px;
  margin: 0 auto;
}

.type-card {
  background: white;
  border: 2px solid #e0e6ed;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  text-align: center;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.type-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #ff6b6b, #4ecdc4);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.type-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0,0,0,0.15);
  border-color: #3b82f6;
}

.type-card:hover::before {
  opacity: 1;
}

.type-card.active {
  border-color: #3b82f6;
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.25);
  transform: translateY(-2px);
}

.type-card.active::before {
  opacity: 1;
}

.type-card.emergency.active::before {
  background: linear-gradient(90deg, #ef4444, #dc2626);
}

.type-card.accident.active::before {
  background: linear-gradient(90deg, #f59e0b, #d97706);
}

.type-card.security.active::before {
  background: linear-gradient(90deg, #8b5cf6, #7c3aed);
}

.type-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.type-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.hotkey-badge {
  background: #f3f4f6;
  color: #6b7280;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 6px;
  font-weight: 600;
  border: 1px solid #d1d5db;
}

.type-title {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 4px;
}

.type-category {
  font-size: 12px;
  color: #6b7280;
  background: #f9fafb;
  padding: 4px 8px;
  border-radius: 12px;
  display: inline-block;
}

/* 中部：信息录入区 */
.info-input-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
}

.basic-info-panel,
.professional-info-panel {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  border: 1px solid #e5e7eb;
  height: fit-content;
  max-height: calc(100vh - 280px);
  overflow: visible;
}

.panel-title {
  font-size: 20px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f3f4f6;
  display: flex;
  align-items: center;
}

.panel-icon {
  font-size: 24px;
  margin-right: 8px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.info-field.full-width {
  grid-column: 1 / -1;
}

.info-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 4px;
}

/* 专业信息面板 */
.pro-fields {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  max-height: calc(100vh - 380px);
  overflow: visible;
}

.pro-field {
  padding: 8px;
  background: #f9fafb;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

/* 紧凑组字段样式 */
.compact-group-field {
  padding: 6px;
}

.compact-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.compact-subfield {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.subfield-label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
  min-width: 40px;
  flex-shrink: 0;
}

.compact-options {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  flex: 1;
}

.compact-btn {
  padding: 2px 6px;
  font-size: 11px;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  line-height: 1.2;
}

.compact-btn:hover {
  border-color: #3b82f6;
  background: #eff6ff;
}

.compact-btn.selected {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

/* 紧凑多选样式 */
.checkbox-compact {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.compact-checkbox-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  font-size: 11px;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.compact-checkbox-btn:hover {
  border-color: #3b82f6;
  background: #eff6ff;
}

.compact-checkbox-btn.selected {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.compact-checkbox-btn .checkbox-icon {
  width: 12px;
  height: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 10px;
}

.pro-label {
  display: flex;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
}

.field-icon {
  font-size: 16px;
  margin-right: 8px;
}

.required {
  color: #ef4444;
  margin-left: 4px;
}

.option-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.option-btn {
  position: relative;
  padding: 8px 16px;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

.option-btn:hover {
  border-color: #3b82f6;
  background: #eff6ff;
  transform: translateY(-1px);
}

.option-btn.selected {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.25);
}

.btn-text {
  flex: 1;
}

.btn-hotkey {
  background: rgba(255,255,255,0.2);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
}

.option-btn.selected .btn-hotkey {
  background: rgba(255,255,255,0.3);
}

.checkbox-icon {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.no-fields {
  text-align: center;
  padding: 40px 20px;
  color: #6b7280;
}

.no-fields-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.no-fields-text {
  font-size: 16px;
  font-weight: 500;
}

/* 底部：操作区 */
.action-section {
  max-width: 1200px;
  margin: 0 auto;
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  border: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-info {
  flex: 1;
}

.progress-title {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}

.progress-items {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.progress-item {
  font-size: 14px;
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid #d1d5db;
  background: #f9fafb;
  color: #6b7280;
  transition: all 0.2s ease;
}

.progress-item.completed {
  background: #dcfce7;
  color: #166534;
  border-color: #bbf7d0;
}

.submit-button {
  height: 48px;
  padding: 0 32px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  border: none;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
  transition: all 0.3s ease;
}

.submit-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

.submit-icon {
  font-size: 18px;
}

.submit-hotkey {
  font-size: 12px;
  opacity: 0.8;
}

/* 智能提示 */
.smart-notification {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 1000;
  animation: slideInRight 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.notification-content {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
}

.notification-icon {
  font-size: 16px;
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(100%);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .info-input-section {
    grid-template-columns: 1fr;
  }

  .type-grid {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  }
}

@media (max-width: 768px) {
  .professional-emergency-form {
    padding: 16px;
  }

  .section-title {
    font-size: 24px;
  }

  .type-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .action-section {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }

  .progress-items {
    justify-content: center;
  }
}

/* 强制更新缓存: 2025年09月22日 16:27:45 */
</style>