<!--
  * 警务专业信息字段渲染组件 - 确保预览和实际录入界面的一致性
  *
  * @Author:    Claude Code Assistant
  * @Date:      2025-09-23
  * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
-->
<template>
  <div class="professional-fields">
    <div
      v-for="field in fields"
      :key="getFieldKey(field)"
      class="pro-field"
      :class="{ 'compact-group-field': getFieldType(field) === 'compact-group' }"
    >
      <div class="pro-label">
        <span class="field-icon">{{ getFieldIcon(field) || '📝' }}</span>
        {{ getFieldLabel(field) }}
        <span v-if="getFieldRequired(field)" class="required-mark">*</span>
      </div>

      <!-- 紧凑组合字段 -->
      <div v-if="getFieldType(field) === 'compact-group'" class="compact-group">
        <div
          v-for="subField in getFieldChildren(field)"
          :key="getFieldKey(subField)"
          class="compact-subfield"
        >
          <span class="subfield-label">{{ getFieldLabel(subField) }}:</span>
          <!-- 子字段快捷选项 -->
          <div v-if="getFieldQuickOptions(subField)" class="compact-options">
            <button
              v-for="option in getFieldQuickOptions(subField)"
              :key="option"
              :class="['compact-btn', { 'selected': isOptionSelected(getFieldKey(subField), option) }]"
              @click="selectOption(getFieldKey(subField), option)"
              :disabled="disabled"
            >
              {{ option }}
            </button>
          </div>
          <!-- 子字段选择选项 -->
          <div v-else-if="getFieldDictCode(subField) || getFieldOptions(subField)" class="compact-select-wrapper">
            <!-- 使用数据字典 - 平铺按钮式 -->
            <div v-if="getFieldDictCode(subField)" class="compact-options">
              <button
                v-for="item in getDictOptions(getFieldDictCode(subField))"
                :key="item.dataValue"
                :class="['compact-btn', { 'selected': modelValue[getFieldKey(subField)] === item.dataValue }]"
                @click="updateField(getFieldKey(subField), item.dataValue)"
                :disabled="disabled"
                :title="item.dataLabel"
              >
                {{ item.dataLabel }}
              </button>
            </div>
            <!-- 使用硬编码选项 -->
            <div v-else class="compact-options">
              <button
                v-for="option in getFieldOptions(subField)"
                :key="option"
                :class="['compact-btn', { 'selected': modelValue[getFieldKey(subField)] === option }]"
                @click="updateField(getFieldKey(subField), option)"
                :disabled="disabled"
              >
                {{ option }}
              </button>
            </div>
          </div>
          <!-- 子字段输入框 -->
          <input
            v-else-if="getFieldType(subField) === 'input'"
            type="text"
            :value="modelValue[getFieldKey(subField)] || ''"
            @input="updateField(getFieldKey(subField), ($event.target as HTMLInputElement).value)"
            :placeholder="getFieldPlaceholder(subField)"
            :disabled="disabled"
            class="compact-input"
          />
        </div>
      </div>

      <!-- 紧凑多选 -->
      <div v-else-if="getFieldType(field) === 'checkbox-compact'" class="checkbox-compact">
        <button
          v-for="option in getFieldQuickOptions(field) || getFieldOptions(field)"
          :key="option"
          :class="['compact-checkbox-btn', { 'selected': isCheckboxSelected(getFieldKey(field), option) }]"
          @click="toggleCheckbox(getFieldKey(field), option)"
          :disabled="disabled"
        >
          <span class="check-icon">{{ isCheckboxSelected(getFieldKey(field), option) ? '✓' : '' }}</span>
          {{ option }}
        </button>
      </div>

      <!-- 快捷选择按钮 -->
      <div v-else-if="getFieldQuickOptions(field)" class="quick-options">
        <button
          v-for="(option, idx) in getFieldQuickOptions(field)"
          :key="option"
          :class="['quick-btn', { 'selected': isOptionSelected(getFieldKey(field), option) }]"
          @click="selectOption(getFieldKey(field), option)"
          :disabled="disabled"
          :title="`快捷键: ${idx + 1}`"
        >
          {{ option }}
          <span class="btn-hotkey" v-if="!disabled">{{ idx + 1 }}</span>
        </button>
      </div>

      <!-- 选择按钮 -->
      <div v-else-if="getFieldType(field) === 'select'" class="select-wrapper">
        <!-- 使用数据字典 - 平铺按钮式 -->
        <div v-if="getFieldDictCode(field)" class="select-options">
          <button
            v-for="item in getDictOptions(getFieldDictCode(field))"
            :key="item.dataValue"
            :class="['select-btn', { 'selected': modelValue[getFieldKey(field)] === item.dataValue }]"
            @click="updateField(getFieldKey(field), item.dataValue)"
            :disabled="disabled"
            :title="item.dataLabel"
          >
            {{ item.dataLabel }}
          </button>
        </div>
        <!-- 使用硬编码选项 -->
        <div v-else-if="getFieldOptions(field)" class="select-options">
          <button
            v-for="option in getFieldOptions(field)"
            :key="option"
            :class="['select-btn', { 'selected': modelValue[getFieldKey(field)] === option }]"
            @click="updateField(getFieldKey(field), option)"
            :disabled="disabled"
          >
            {{ option }}
          </button>
        </div>
      </div>

      <!-- 多选 -->
      <div v-else-if="getFieldType(field) === 'checkbox'" class="checkbox-wrapper">
        <!-- 使用数据字典 - 平铺按钮式 -->
        <div v-if="getFieldDictCode(field)" class="checkbox-options">
          <button
            v-for="item in getDictOptions(getFieldDictCode(field))"
            :key="item.dataValue"
            :class="['checkbox-btn', { 'selected': isDictCheckboxSelected(getFieldKey(field), item.dataValue) }]"
            @click="toggleDictCheckbox(getFieldKey(field), item.dataValue)"
            :disabled="disabled"
            :title="item.dataLabel"
          >
            <span class="check-icon">{{ isDictCheckboxSelected(getFieldKey(field), item.dataValue) ? '✓' : '' }}</span>
            {{ item.dataLabel }}
          </button>
        </div>
        <!-- 使用硬编码选项 -->
        <div v-else-if="getFieldOptions(field)" class="checkbox-options">
          <button
            v-for="option in getFieldOptions(field)"
            :key="option"
            :class="['checkbox-btn', { 'selected': isCheckboxSelected(getFieldKey(field), option) }]"
            @click="toggleCheckbox(getFieldKey(field), option)"
            :disabled="disabled"
          >
            <span class="check-icon">{{ isCheckboxSelected(getFieldKey(field), option) ? '✓' : '' }}</span>
            {{ option }}
          </button>
        </div>
      </div>

      <!-- 输入框 -->
      <div v-else-if="getFieldType(field) === 'input'" class="input-wrapper">
        <input
          type="text"
          :value="modelValue[getFieldKey(field)] || ''"
          @input="updateField(getFieldKey(field), ($event.target as HTMLInputElement).value)"
          :placeholder="getFieldPlaceholder(field) || `请输入${getFieldLabel(field)}`"
          :disabled="disabled"
          class="preview-input-field"
        />
      </div>

      <!-- 数字输入 -->
      <div v-else-if="getFieldType(field) === 'number'" class="input-wrapper">
        <input
          type="number"
          :value="modelValue[getFieldKey(field)] || ''"
          @input="updateField(getFieldKey(field), parseFloat(($event.target as HTMLInputElement).value) || null)"
          :placeholder="getFieldPlaceholder(field) || `请输入${getFieldLabel(field)}`"
          :disabled="disabled"
          class="preview-input-field"
          min="0"
        />
      </div>

      <!-- 文本域 -->
      <div v-else-if="getFieldType(field) === 'textarea'" class="input-wrapper">
        <textarea
          :value="modelValue[getFieldKey(field)] || ''"
          @input="updateField(getFieldKey(field), ($event.target as HTMLTextAreaElement).value)"
          :placeholder="getFieldPlaceholder(field) || `请输入${getFieldLabel(field)}`"
          :disabled="disabled"
          class="preview-textarea-field"
          rows="2"
          :maxlength="200"
        ></textarea>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import type { FieldItem } from '/@/api/business/oa/police-form-template-api';
  import type { PoliceFormFieldVO } from '/@/api/business/oa/police-form-config-api';
  import { useDictStore } from '/@/store/modules/system/dict';

  // 通用字段接口 - 支持两种格式
  type UnifiedField = FieldItem | PoliceFormFieldVO;

  interface Props {
    fields: UnifiedField[];
    modelValue: Record<string, any>;
    disabled?: boolean;
  }

  interface Emits {
    (e: 'update:modelValue', value: Record<string, any>): void;
  }

  const props = withDefaults(defineProps<Props>(), {
    disabled: false
  });

  const emit = defineEmits<Emits>();

  // 字典存储
  const dictStore = useDictStore();

  // 字段属性访问辅助函数 - 兼容两种格式
  function getFieldKey(field: UnifiedField): string {
    return 'fieldKey' in field ? field.fieldKey : field.key;
  }

  function getFieldLabel(field: UnifiedField): string {
    return 'fieldLabel' in field ? field.fieldLabel : field.label;
  }

  function getFieldType(field: UnifiedField): string {
    return 'fieldType' in field ? field.fieldType : field.type;
  }

  function getFieldIcon(field: UnifiedField): string | undefined {
    return 'fieldIcon' in field ? field.fieldIcon : field.icon;
  }

  function getFieldRequired(field: UnifiedField): boolean | undefined {
    return 'isRequired' in field ? field.isRequired : field.required;
  }

  function getFieldOptions(field: UnifiedField): string[] | undefined {
    return 'fieldOptions' in field ? field.fieldOptions : field.options;
  }

  function getFieldQuickOptions(field: UnifiedField): string[] | undefined {
    return 'quickOptions' in field ? field.quickOptions : field.quickOptions;
  }

  function getFieldChildren(field: UnifiedField): UnifiedField[] | undefined {
    return 'children' in field ? field.children : field.fields;
  }

  function getFieldPlaceholder(field: UnifiedField): string | undefined {
    return 'placeholder' in field ? field.placeholder : field.placeholder;
  }

  function getFieldDictCode(field: UnifiedField): string | undefined {
    return 'dictCode' in field ? field.dictCode : field.dictCode;
  }

  // 获取字典选项数据
  function getDictOptions(dictCode: string | undefined) {
    if (!dictCode) return [];
    return dictStore.getDictData(dictCode);
  }

  // 判断字典多选框是否被选中
  function isDictCheckboxSelected(fieldKey: string, dataValue: string): boolean {
    const value = props.modelValue[fieldKey];
    if (!value) return false;
    if (Array.isArray(value)) {
      return value.includes(dataValue);
    }
    return value === dataValue;
  }

  // 切换字典多选框状态
  function toggleDictCheckbox(fieldKey: string, dataValue: string) {
    if (props.disabled) return;

    let currentValue = props.modelValue[fieldKey];
    if (!currentValue) {
      currentValue = [];
    }
    if (!Array.isArray(currentValue)) {
      currentValue = [currentValue];
    }

    const newValue = [...currentValue];
    const index = newValue.indexOf(dataValue);
    if (index > -1) {
      newValue.splice(index, 1);
    } else {
      newValue.push(dataValue);
    }

    updateFieldImmediate(fieldKey, newValue);
  }

  // 更新字段值 - 添加防抖优化性能
  const updateField = debounce((fieldKey: string, value: any) => {
    if (props.disabled) return;
    emit('update:modelValue', { ...props.modelValue, [fieldKey]: value });
  }, 50); // 50ms防抖

  // 立即更新字段值（用于按钮点击等需要即时响应的场景）
  function updateFieldImmediate(fieldKey: string, value: any) {
    if (props.disabled) return;
    emit('update:modelValue', { ...props.modelValue, [fieldKey]: value });
  }

  // 简单的防抖函数
  function debounce(func: Function, wait: number) {
    let timeout: NodeJS.Timeout;
    return function executedFunction(...args: any[]) {
      const later = () => {
        clearTimeout(timeout);
        func(...args);
      };
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
    };
  }

  // 选择选项
  function selectOption(fieldKey: string, option: string) {
    if (props.disabled) return;

    if (option === '无' || option === '0') {
      updateFieldImmediate(fieldKey, 0);
    } else {
      const num = extractNumber(option);
      updateFieldImmediate(fieldKey, num !== null ? num : option);
    }
  }

  // 判断选项是否被选中
  function isOptionSelected(fieldKey: string, option: string): boolean {
    const value = props.modelValue[fieldKey];
    return value === option || value === extractNumber(option);
  }

  // 判断多选框是否被选中
  function isCheckboxSelected(fieldKey: string, option: string): boolean {
    const value = props.modelValue[fieldKey];
    if (!value) return false;
    if (Array.isArray(value)) {
      return value.includes(option);
    }
    return value === option;
  }

  // 切换多选框状态
  function toggleCheckbox(fieldKey: string, option: string) {
    if (props.disabled) return;

    let currentValue = props.modelValue[fieldKey];
    if (!currentValue) {
      currentValue = [];
    }
    if (!Array.isArray(currentValue)) {
      currentValue = [currentValue];
    }

    const newValue = [...currentValue];
    const index = newValue.indexOf(option);
    if (index > -1) {
      newValue.splice(index, 1);
    } else {
      newValue.push(option);
    }

    updateFieldImmediate(fieldKey, newValue);
  }

  // 提取数字
  function extractNumber(text: string): number | null {
    const match = text.match(/(\d+)/);
    return match ? parseInt(match[1]) : null;
  }
</script>

<style scoped lang="scss">
.professional-fields {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: calc(100vh - 280px);
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(156, 163, 175, 0.5) transparent;
}

.professional-fields::-webkit-scrollbar {
  width: 6px;
}

.professional-fields::-webkit-scrollbar-track {
  background: transparent;
}

.professional-fields::-webkit-scrollbar-thumb {
  background: rgba(156, 163, 175, 0.3);
  border-radius: 3px;
}

.professional-fields::-webkit-scrollbar-thumb:hover {
  background: rgba(156, 163, 175, 0.5);
}

.pro-field {
  background: rgba(249, 250, 251, 0.8);
  backdrop-filter: blur(5px);
  border-radius: 10px;
  padding: 12px;
  border: 1px solid rgba(229, 231, 235, 0.6);
  margin-bottom: 8px;
  transition: all 0.2s ease;
}

.pro-field:hover {
  background: rgba(243, 244, 246, 0.9);
  border-color: rgba(156, 163, 175, 0.4);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.pro-label {
  display: flex;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;

  .field-icon {
    margin-right: 6px;
  }

  .required-mark {
    color: #ef4444;
    margin-left: 4px;
  }
}

/* 紧凑组字段样式 */
.compact-group-field {
  padding: 12px;
  background: #f9fafb;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  margin-bottom: 12px;
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
  gap: 4px;
  flex: 1;
  align-items: center;
}

.compact-btn {
  padding: 6px 10px;
  font-size: 12px;
  background: linear-gradient(135deg, #ffffff 0%, #f9fafb 100%);
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  line-height: 1.2;
  font-weight: 500;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  min-width: 45px;
  max-width: 90px;
  position: relative;
  user-select: none;
  transform: translateZ(0); /* 启用硬件加速 */
  will-change: transform, box-shadow; /* 优化性能 */

  &:hover:not(:disabled) {
    border-color: #3b82f6;
    background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
    color: #1d4ed8;
    transform: translateY(-1px) scale(1.02);
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
  }

  &:active:not(:disabled) {
    transform: translateY(0) scale(0.98);
    transition: all 0.1s ease;
  }

  &.selected {
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    color: white;
    border-color: #1d4ed8;
    font-weight: 600;
    box-shadow:
      0 4px 16px rgba(59, 130, 246, 0.4),
      inset 0 1px 0 rgba(255, 255, 255, 0.2);
    transform: translateY(-1px);
  }

  &.selected:hover {
    background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
    box-shadow:
      0 6px 20px rgba(59, 130, 246, 0.5),
      inset 0 1px 0 rgba(255, 255, 255, 0.3);
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.compact-input {
  width: 100px;
  padding: 4px 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 13px;

  &:disabled {
    background-color: #f9fafb;
    cursor: not-allowed;
  }
}

/* 紧凑多选样式 */
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

  &:hover:not(:disabled) {
    border-color: #3b82f6;
    background: #eff6ff;
    color: #1d4ed8;
  }

  &.selected {
    background: #3b82f6;
    color: white;
    border-color: #3b82f6;
    font-weight: 600;
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.check-icon {
  width: 14px;
  height: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

/* 快捷选择按钮 */
.quick-options,
.select-options,
.checkbox-options {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.quick-btn,
.select-btn,
.checkbox-btn {
  padding: 6px 12px;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  max-width: 120px;

  &:hover:not(:disabled) {
    border-color: #3b82f6;
    background: #eff6ff;
    color: #1d4ed8;
    transform: translateY(-1px);
    box-shadow: 0 2px 4px rgba(59, 130, 246, 0.2);
  }

  &.selected {
    background: #3b82f6;
    color: white;
    border-color: #3b82f6;
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.btn-hotkey {
  background: rgba(59, 130, 246, 0.1);
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 10px;
  color: #3b82f6;
}

/* 输入框样式 */
.input-wrapper {
  margin-top: 6px;
}

.preview-input-field,
.preview-textarea-field {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 13px;
  background: #f9fafb;

  &:disabled {
    background-color: #f3f4f6;
    cursor: not-allowed;
  }

  &:focus:not(:disabled) {
    outline: none;
    border-color: #3b82f6;
    background: white;
  }
}

.preview-textarea-field {
  resize: vertical;
  min-height: 60px;
}

/* 选择器包装器样式 */
.select-wrapper,
.checkbox-wrapper,
.compact-select-wrapper {
  margin-top: 6px;
}

.compact-select-wrapper {
  flex: 1;
  min-width: 120px;
}
</style>