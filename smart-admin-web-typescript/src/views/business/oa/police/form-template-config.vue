<!--
  * 警情表单字段配置页面 - 可视化拖拽配置
  *
  * @Author:    Claude Code Assistant
  * @Date:      2025-09-22
  * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
-->
<template>
  <div class="form-template-config">
    <!-- 顶部工具栏 -->
    <div class="top-toolbar">
      <div class="toolbar-left">
        <a-button @click="goBack" size="large">
          <ArrowLeftOutlined />
          返回列表
        </a-button>
        <div class="page-info">
          <h1 class="page-title">{{ pageTitle }}</h1>
          <p class="page-subtitle">{{ templateConfig?.templateName }}</p>
        </div>
      </div>
      <div class="toolbar-right">
        <a-button @click="previewForm" size="large">
          <EyeOutlined />
          预览表单
        </a-button>
        <a-button type="primary" size="large" @click="saveConfig" :loading="saving">
          <SaveOutlined />
          保存配置
        </a-button>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="config-content" v-if="!loading">
      <!-- 左侧：字段组件库 -->
      <div class="field-library">
        <div class="library-header">
          <h3>🧩 字段组件库</h3>
          <p>拖拽字段到右侧配置区域</p>
        </div>

        <div class="field-categories">
          <!-- 基础字段 -->
          <div class="field-category">
            <h4 class="category-title">基础字段</h4>
            <div class="field-items">
              <div
                v-for="field in basicFields"
                :key="field.type"
                class="field-item"
                draggable="true"
                @dragstart="onFieldDragStart($event, field)"
              >
                <span class="field-icon">{{ field.icon }}</span>
                <span class="field-name">{{ field.name }}</span>
              </div>
            </div>
          </div>

          <!-- 高级字段 -->
          <div class="field-category">
            <h4 class="category-title">高级字段</h4>
            <div class="field-items">
              <div
                v-for="field in advancedFields"
                :key="field.type"
                class="field-item"
                draggable="true"
                @dragstart="onFieldDragStart($event, field)"
              >
                <span class="field-icon">{{ field.icon }}</span>
                <span class="field-name">{{ field.name }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间：配置区域 -->
      <div class="config-area">
        <!-- 步骤2配置 -->
        <div class="step-section">
          <div class="step-header">
            <h3 class="step-title">📝 第二步字段</h3>
            <p class="step-desc">核心专业信息录入字段</p>
          </div>
          <div
            class="field-drop-zone"
            @drop="onFieldDrop($event, 2)"
            @dragover.prevent
            @dragenter.prevent
          >
            <div
              v-for="(field, index) in step2Fields"
              :key="`step2-${index}`"
              class="config-field-item"
              @click="editField(field, index, 2)"
            >
              <div class="field-content">
                <span class="field-icon">{{ field.fieldIcon || '📝' }}</span>
                <div class="field-info">
                  <div class="field-label">{{ field.fieldLabel }}</div>
                  <div class="field-type">{{ getFieldTypeName(field.fieldType) }}</div>
                </div>
                <div class="field-required" v-if="field.isRequired">
                  <span class="required-mark">*</span>
                </div>
              </div>
              <div class="field-actions">
                <a-button type="text" size="small" @click.stop="editField(field, index, 2)">
                  <EditOutlined />
                </a-button>
                <a-button type="text" size="small" @click.stop="removeField(index, 2)" class="danger-btn">
                  <DeleteOutlined />
                </a-button>
              </div>
            </div>

            <div v-if="step2Fields.length === 0" class="empty-drop-zone">
              <div class="empty-icon">📝</div>
              <div class="empty-text">拖拽字段到这里</div>
            </div>
          </div>
        </div>

        <!-- 步骤3配置 -->
        <div class="step-section">
          <div class="step-header">
            <h3 class="step-title">📋 第三步字段</h3>
            <p class="step-desc">补充信息和详细配置字段</p>
          </div>
          <div
            class="field-drop-zone"
            @drop="onFieldDrop($event, 3)"
            @dragover.prevent
            @dragenter.prevent
          >
            <div
              v-for="(field, index) in step3Fields"
              :key="`step3-${index}`"
              class="config-field-item"
              @click="editField(field, index, 3)"
            >
              <div class="field-content">
                <span class="field-icon">{{ field.fieldIcon || '📝' }}</span>
                <div class="field-info">
                  <div class="field-label">{{ field.fieldLabel }}</div>
                  <div class="field-type">{{ getFieldTypeName(field.fieldType) }}</div>
                </div>
                <div class="field-required" v-if="field.isRequired">
                  <span class="required-mark">*</span>
                </div>
              </div>
              <div class="field-actions">
                <a-button type="text" size="small" @click.stop="editField(field, index, 3)">
                  <EditOutlined />
                </a-button>
                <a-button type="text" size="small" @click.stop="removeField(index, 3)" class="danger-btn">
                  <DeleteOutlined />
                </a-button>
              </div>
            </div>

            <div v-if="step3Fields.length === 0" class="empty-drop-zone">
              <div class="empty-icon">📋</div>
              <div class="empty-text">拖拽字段到这里</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：字段属性配置 -->
      <div class="field-properties">
        <div class="properties-header">
          <h3>⚙️ 字段属性</h3>
        </div>

        <div v-if="currentField" class="properties-form">
          <a-form :label-col="{ span: 24 }" :wrapper-col="{ span: 24 }">
            <a-form-item label="字段标识">
              <a-input v-model:value="currentField.fieldKey" placeholder="英文标识，如 fireScale" />
            </a-form-item>

            <a-form-item label="字段名称">
              <a-input v-model:value="currentField.fieldLabel" placeholder="显示名称，如 火势程度" />
            </a-form-item>

            <a-form-item label="字段图标">
              <a-input v-model:value="currentField.fieldIcon" placeholder="emoji图标，如 🔥" />
            </a-form-item>

            <a-form-item label="字段类型">
              <a-select v-model:value="currentField.fieldType" @change="onFieldTypeChange">
                <a-select-option value="input">文本输入</a-select-option>
                <a-select-option value="textarea">多行文本</a-select-option>
                <a-select-option value="select">单选下拉</a-select-option>
                <a-select-option value="checkbox">多选框</a-select-option>
                <a-select-option value="number">数字输入</a-select-option>
                <a-select-option value="compact-group">紧凑组合</a-select-option>
                <a-select-option value="checkbox-compact">紧凑多选</a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item>
              <a-checkbox v-model:checked="currentField.isRequired">必填字段</a-checkbox>
            </a-form-item>

            <a-form-item label="占位符" v-if="needsPlaceholder(currentField.fieldType)">
              <a-input v-model:value="currentField.placeholder" placeholder="输入提示文本" />
            </a-form-item>

            <a-form-item label="默认值" v-if="needsDefaultValue(currentField.fieldType)">
              <a-input v-model:value="currentField.defaultValue" placeholder="默认值" />
            </a-form-item>

            <!-- 选项配置 -->
            <a-form-item label="选项来源" v-if="needsOptions(currentField.fieldType)">
              <a-radio-group v-model:value="optionSource" @change="onOptionSourceChange">
                <a-radio value="dict">数据字典</a-radio>
                <a-radio value="custom">自定义选项</a-radio>
              </a-radio-group>
            </a-form-item>

            <!-- 数据字典选择 -->
            <a-form-item label="选择数据字典" v-if="needsOptions(currentField.fieldType) && optionSource === 'dict'">
              <a-select
                v-model:value="currentField.dictCode"
                placeholder="请选择数据字典"
                show-search
                :filter-option="filterDictOption"
                @change="onDictCodeChange"
              >
                <a-select-option
                  v-for="dict in availableDictionaries"
                  :key="dict.code"
                  :value="dict.code"
                >
                  {{ dict.label }}
                </a-select-option>
              </a-select>

              <!-- 显示字典数据预览 -->
              <div v-if="currentField.dictCode" class="dict-preview">
                <div class="preview-title">数据预览:</div>
                <div class="preview-items">
                  <a-tag
                    v-for="item in getDictDataPreview(currentField.dictCode)"
                    :key="item.dataValue"
                    class="preview-tag"
                  >
                    {{ item.dataLabel }} ({{ item.dataValue }})
                  </a-tag>
                </div>
              </div>
            </a-form-item>

            <!-- 自定义选项配置 -->
            <a-form-item label="自定义选项" v-if="needsOptions(currentField.fieldType) && optionSource === 'custom'">
              <div class="options-config">
                <div v-for="(option, index) in currentField.fieldOptions" :key="index" class="option-item">
                  <a-input v-model:value="currentField.fieldOptions[index]" placeholder="选项文本" />
                  <a-button type="text" @click="removeOption(index)" class="remove-option">
                    <DeleteOutlined />
                  </a-button>
                </div>
                <a-button type="dashed" @click="addOption" block>
                  <PlusOutlined />
                  添加选项
                </a-button>
              </div>
            </a-form-item>

            <!-- 快捷选项 -->
            <a-form-item label="快捷选项" v-if="needsQuickOptions(currentField.fieldType)">
              <div class="options-config">
                <div v-for="(option, index) in currentField.quickOptions" :key="index" class="option-item">
                  <a-input v-model:value="currentField.quickOptions[index]" placeholder="快捷选项" />
                  <a-button type="text" @click="removeQuickOption(index)" class="remove-option">
                    <DeleteOutlined />
                  </a-button>
                </div>
                <a-button type="dashed" @click="addQuickOption" block>
                  <PlusOutlined />
                  添加快捷选项
                </a-button>
              </div>
            </a-form-item>

            <!-- 子字段配置 -->
            <a-form-item label="子字段配置" v-if="currentField.fieldType === 'compact-group'">
              <div class="sub-fields-config">
                <div v-for="(subField, index) in currentField.children" :key="index" class="sub-field-item">
                  <div class="sub-field-header">
                    <span class="sub-field-title">子字段 {{ index + 1 }}</span>
                    <a-button type="text" @click="removeSubField(index)" class="remove-sub-field">
                      <DeleteOutlined />
                    </a-button>
                  </div>
                  <div class="sub-field-form">
                    <a-input v-model:value="subField.fieldKey" placeholder="字段标识" style="margin-bottom: 8px;" />
                    <a-input v-model:value="subField.fieldLabel" placeholder="字段名称" style="margin-bottom: 8px;" />
                    <a-select v-model:value="subField.fieldType" placeholder="字段类型" style="width: 100%; margin-bottom: 8px;">
                      <a-select-option value="select">单选下拉</a-select-option>
                      <a-select-option value="input">文本输入</a-select-option>
                      <a-select-option value="number">数字输入</a-select-option>
                    </a-select>

                    <!-- 子字段选项配置 -->
                    <div v-if="subField.fieldType === 'select'" style="margin-bottom: 8px;">
                      <div style="margin-bottom: 8px;">
                        <span style="font-size: 12px; color: #6b7280; margin-bottom: 4px; display: block;">选项来源</span>
                        <a-radio-group v-model:value="subField.optionSource" size="small" style="width: 100%;">
                          <a-radio-button value="dict" style="flex: 1; text-align: center;">数据字典</a-radio-button>
                          <a-radio-button value="custom" style="flex: 1; text-align: center;">自定义选项</a-radio-button>
                        </a-radio-group>
                      </div>

                      <!-- 数据字典选择 -->
                      <div v-if="subField.optionSource === 'dict'" style="margin-bottom: 8px;">
                        <a-select
                          v-model:value="subField.dictCode"
                          placeholder="选择数据字典"
                          style="width: 100%;"
                          size="small"
                          show-search
                          :filter-option="filterDictOption"
                          @change="onSubFieldDictCodeChange(subField, $event)"
                        >
                          <a-select-option
                            v-for="dict in availableDictionaries"
                            :key="dict.code"
                            :value="dict.code"
                          >
                            {{ dict.label }}
                          </a-select-option>
                        </a-select>
                      </div>

                      <!-- 自定义选项 -->
                      <div v-if="subField.optionSource === 'custom'" style="margin-bottom: 8px;">
                        <div style="margin-bottom: 4px;">
                          <span style="font-size: 12px; color: #6b7280;">字段选项</span>
                        </div>
                        <a-textarea
                          v-model:value="subField.fieldOptionsText"
                          placeholder="每行一个选项"
                          :rows="3"
                          size="small"
                          @blur="updateSubFieldOptions(subField)"
                        />
                      </div>

                      <!-- 快捷选项 -->
                      <div style="margin-bottom: 8px;">
                        <div style="margin-bottom: 4px;">
                          <span style="font-size: 12px; color: #6b7280;">快捷选项（可选）</span>
                        </div>
                        <a-textarea
                          v-model:value="subField.quickOptionsText"
                          placeholder="每行一个快捷选项"
                          :rows="2"
                          size="small"
                          @blur="updateSubFieldQuickOptions(subField)"
                        />
                      </div>
                    </div>

                    <a-input v-model:value="subField.placeholder" placeholder="占位符" size="small" style="margin-bottom: 8px;" />
                    <a-input v-model:value="subField.defaultValue" placeholder="默认值" size="small" />
                  </div>
                </div>
                <a-button type="dashed" @click="addSubField" block>
                  <PlusOutlined />
                  添加子字段
                </a-button>
              </div>
            </a-form-item>
          </a-form>
        </div>

        <div v-else class="no-selection">
          <div class="no-selection-content">
            <div class="no-selection-icon">⚙️</div>
            <div class="no-selection-text">选择字段进行配置</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <a-spin size="large" />
      <div class="loading-text">正在加载配置...</div>
    </div>

    <!-- 预览弹窗 -->
    <a-modal
      v-model:open="previewVisible"
      title="🚨 智能接警系统 - 表单预览"
      width="1200px"
      :footer="null"
      @cancel="previewVisible = false"
      style="top: 20px;"
    >
      <div class="real-form-preview">
        <!-- 模拟真实页面的左右分栏布局 -->
        <div class="preview-main-content">
          <!-- 左侧：基本信息（固定内容） -->
          <div class="preview-left-panel">
            <h3 class="section-title">📋 基本信息</h3>
            <div class="basic-info-fields">
              <div class="basic-field-item">
                <label>事发地点 *</label>
                <input type="text" placeholder="输入地点关键字" disabled class="preview-input-field" />
              </div>
              <div class="basic-field-row">
                <div class="basic-field-item">
                  <label>报警人 *</label>
                  <input type="text" placeholder="姓名" disabled class="preview-input-field" />
                </div>
                <div class="basic-field-item">
                  <label>电话 *</label>
                  <input type="text" placeholder="联系电话" disabled class="preview-input-field" />
                </div>
              </div>
              <div class="basic-field-item">
                <label>现场描述 *</label>
                <textarea placeholder="详细描述现场情况" disabled class="preview-textarea-field" rows="3"></textarea>
              </div>
            </div>
          </div>

          <!-- 右侧：专业信息（动态字段） -->
          <div class="preview-right-panel">
            <h3 class="section-title">
              <span class="section-icon" style="color: #ef4444;">🔥</span>
              专业信息
            </h3>
            <PoliceProfessionalFields
              :fields="[...step2Fields, ...step3Fields]"
              :model-value="previewFormData"
              :disabled="true"
            />
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, computed, onMounted } from 'vue';
  import { useRouter, useRoute } from 'vue-router';
  import { message } from 'ant-design-vue';
  import {
    ArrowLeftOutlined,
    EyeOutlined,
    SaveOutlined,
    EditOutlined,
    DeleteOutlined,
    PlusOutlined,
  } from '@ant-design/icons-vue';
  import PoliceProfessionalFields from '/@/components/business/emergency/police-professional-fields.vue';
  import { POLICE_REPORT_TYPE_ENUM } from '/@/constants/business/oa/police-report-const';
  import { policeFormTemplateApi, type FieldItem } from '/@/api/business/oa/police-form-template-api';
  import { policeFormConfigApi, type PoliceFormConfigVO } from '/@/api/business/oa/police-form-config-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import { useDictStore } from '/@/store/modules/system/dict';
  import { DICT_CODE_ENUM } from '/@/constants/support/dict-const';

  const router = useRouter();
  const route = useRoute();

  // 字典存储
  const dictStore = useDictStore();

  // 可用的数据字典列表
  const availableDictionaries = computed(() => {
    const dictList = dictStore.getDictList();
    console.log('Available dictionaries:', dictList); // 调试日志
    return dictList.map(dict => ({
      code: dict.dictCode,
      name: dict.dictName,
      label: `${dict.dictName} (${dict.dictCode})`
    }));
  });

  // 页面状态
  const loading = ref(true);
  const saving = ref(false);
  const templateConfig = ref<PoliceFormConfigVO | null>(null);
  const previewVisible = ref(false);

  // 选项来源状态
  const optionSource = ref<'dict' | 'custom'>('dict');

  // 字段配置
  const step2Fields = ref<FieldItem[]>([]);
  const step3Fields = ref<FieldItem[]>([]);
  const currentField = ref<FieldItem | null>(null);
  const currentFieldIndex = ref(-1);
  const currentFieldStep = ref(2);

  // 预览表单数据
  const previewFormData = ref<Record<string, any>>({});

  // 字段组件库
  const basicFields = [
    { type: 'input', name: '文本输入', icon: '📝' },
    { type: 'textarea', name: '多行文本', icon: '📄' },
    { type: 'number', name: '数字输入', icon: '🔢' },
    { type: 'select', name: '单选下拉', icon: '📋' },
    { type: 'checkbox', name: '多选框', icon: '☑️' },
  ];

  const advancedFields = [
    { type: 'compact-group', name: '紧凑组合', icon: '📦' },
    { type: 'checkbox-compact', name: '紧凑多选', icon: '✅' },
  ];

  // 计算属性
  const pageTitle = computed(() => {
    if (!templateConfig.value) return '字段配置';
    const typeName = getReportTypeName(templateConfig.value.reportType);
    return `${typeName} - 字段配置`;
  });

  // 页面初始化
  onMounted(async () => {
    // 初始化数据字典
    try {
      await dictStore.loadDictAll();
    } catch (error) {
      console.error('加载数据字典失败:', error);
    }

    const templateId = route.query.templateId as string;
    if (templateId) {
      loadTemplateConfig(parseInt(templateId));
    } else {
      message.error('缺少模板ID参数');
      goBack();
    }
  });

  // 加载模板配置
  async function loadTemplateConfig(templateId: number) {
    try {
      loading.value = true;
      console.log('正在加载模板配置，templateId:', templateId);
      const response = await policeFormTemplateApi.getTemplateDetail(templateId);
      console.log('模板配置响应:', response);
      if (response.data) {
        templateConfig.value = response.data;
        step2Fields.value = convertFieldsFormat(response.data.step2Fields || []);
        step3Fields.value = convertFieldsFormat(response.data.step3Fields || []);
        console.log('step2Fields:', step2Fields.value);
        console.log('step3Fields:', step3Fields.value);
      } else {
        console.warn('模板配置数据为空');
        message.warning('模板配置数据为空，请先创建模板');
      }
    } catch (error) {
      console.error('加载模板配置失败:', error);
      smartSentry.captureError(error);
      message.error('加载配置失败');
    } finally {
      loading.value = false;
    }
  }

  // 获取警情类型名称
  function getReportTypeName(reportType: number): string {
    const type = Object.values(POLICE_REPORT_TYPE_ENUM).find(t => t.value === reportType);
    return type ? type.desc : '未知类型';
  }

  // 获取字段类型名称
  function getFieldTypeName(fieldType: string): string {
    const typeNames: Record<string, string> = {
      'input': '文本输入',
      'textarea': '多行文本',
      'number': '数字输入',
      'select': '单选下拉',
      'checkbox': '多选框',
      'compact-group': '紧凑组合',
      'checkbox-compact': '紧凑多选',
    };
    return typeNames[fieldType] || fieldType;
  }

  // 拖拽开始
  function onFieldDragStart(event: DragEvent, field: any) {
    if (event.dataTransfer) {
      event.dataTransfer.setData('text/plain', JSON.stringify(field));
    }
  }

  // 字段拖放
  function onFieldDrop(event: DragEvent, stepNumber: number) {
    event.preventDefault();
    if (event.dataTransfer) {
      const fieldData = JSON.parse(event.dataTransfer.getData('text/plain'));
      const newField: FieldItem = {
        fieldKey: `field_${Date.now()}`,
        fieldLabel: fieldData.name,
        fieldType: fieldData.type,
        fieldIcon: fieldData.icon,
        required: false,
        sortOrder: stepNumber === 2 ? step2Fields.value.length : step3Fields.value.length,
        stepNumber,
        fieldOptions: needsOptions(fieldData.type) ? ['选项1', '选项2'] : undefined,
        quickOptions: needsQuickOptions(fieldData.type) ? ['快捷1', '快捷2'] : undefined,
        children: fieldData.type === 'compact-group' ? [] : undefined,
      };

      if (stepNumber === 2) {
        step2Fields.value.push(newField);
      } else {
        step3Fields.value.push(newField);
      }
    }
  }

  // 编辑字段
  function editField(field: FieldItem, index: number, stepNumber: number) {
    currentField.value = { ...field };
    currentFieldIndex.value = index;
    currentFieldStep.value = stepNumber;

    // 根据字段配置设置选项来源
    if (needsOptions(field.fieldType)) {
      if (field.dictCode) {
        optionSource.value = 'dict';
      } else {
        optionSource.value = 'custom';
        // 确保选项数组存在
        if (!currentField.value.fieldOptions) {
          currentField.value.fieldOptions = [];
        }
      }
    }

    if (!currentField.value.quickOptions && needsQuickOptions(field.fieldType)) {
      currentField.value.quickOptions = [];
    }
    if (!currentField.value.children && field.fieldType === 'compact-group') {
      currentField.value.children = [];
    }
  }

  // 移除字段
  function removeField(index: number, stepNumber: number) {
    if (stepNumber === 2) {
      step2Fields.value.splice(index, 1);
    } else {
      step3Fields.value.splice(index, 1);
    }

    // 如果删除的是当前编辑的字段，清空编辑状态
    if (currentFieldIndex.value === index && currentFieldStep.value === stepNumber) {
      currentField.value = null;
      currentFieldIndex.value = -1;
    }
  }

  // 字段类型变化
  function onFieldTypeChange() {
    if (!currentField.value) return;

    // 根据新类型初始化相应属性
    if (needsOptions(currentField.value.fieldType)) {
      // 重置选项来源为数据字典（默认推荐）
      optionSource.value = 'dict';
      // 清空现有配置
      currentField.value.fieldOptions = undefined;
      currentField.value.dictCode = undefined;
    }
    if (needsQuickOptions(currentField.value.fieldType) && !currentField.value.quickOptions) {
      currentField.value.quickOptions = [];
    }
    if (currentField.value.fieldType === 'compact-group' && !currentField.value.children) {
      currentField.value.children = [];
    }
  }

  // 判断是否需要选项配置
  function needsOptions(fieldType: string): boolean {
    return ['select', 'checkbox', 'checkbox-compact'].includes(fieldType);
  }

  // 判断是否需要快捷选项
  function needsQuickOptions(fieldType: string): boolean {
    return ['select', 'number'].includes(fieldType);
  }

  // 判断是否需要占位符
  function needsPlaceholder(fieldType: string): boolean {
    return ['input', 'textarea', 'number'].includes(fieldType);
  }

  // 判断是否需要默认值
  function needsDefaultValue(fieldType: string): boolean {
    return ['input', 'textarea', 'number'].includes(fieldType);
  }

  // 数据字典搜索过滤
  function filterDictOption(input: string, option: any): boolean {
    return option.children.toLowerCase().includes(input.toLowerCase());
  }

  // 获取数据字典数据预览
  function getDictDataPreview(dictCode: string) {
    return dictStore.getDictData(dictCode).slice(0, 10); // 只显示前10条
  }

  // 选项来源改变处理
  function onOptionSourceChange(e: any) {
    const source = e.target.value;
    if (source === 'dict') {
      // 切换到数据字典，清空自定义选项
      if (currentField.value) {
        currentField.value.fieldOptions = undefined;
        currentField.value.quickOptions = undefined;
      }
    } else {
      // 切换到自定义选项，清空数据字典
      if (currentField.value) {
        currentField.value.dictCode = undefined;
        currentField.value.fieldOptions = ['选项1', '选项2'];
      }
    }
  }

  // 数据字典编码改变处理
  function onDictCodeChange(dictCode: string) {
    if (currentField.value) {
      currentField.value.dictCode = dictCode;
      // 清空自定义选项
      currentField.value.fieldOptions = undefined;
      currentField.value.quickOptions = undefined;
    }
  }

  // 添加选项
  function addOption() {
    if (currentField.value?.fieldOptions) {
      currentField.value.fieldOptions.push('新选项');
    }
  }

  // 移除选项
  function removeOption(index: number) {
    if (currentField.value?.fieldOptions) {
      currentField.value.fieldOptions.splice(index, 1);
    }
  }

  // 添加快捷选项
  function addQuickOption() {
    if (currentField.value?.quickOptions) {
      currentField.value.quickOptions.push('新快捷选项');
    }
  }

  // 移除快捷选项
  function removeQuickOption(index: number) {
    if (currentField.value?.quickOptions) {
      currentField.value.quickOptions.splice(index, 1);
    }
  }

  // 添加子字段
  function addSubField() {
    if (currentField.value?.children) {
      currentField.value.children.push({
        fieldKey: `sub_field_${Date.now()}`,
        fieldLabel: '子字段',
        fieldType: 'select',
        isRequired: false,
        sortOrder: currentField.value.children.length,
        stepNumber: currentFieldStep.value,
        fieldOptions: ['选项1', '选项2'],
        quickOptions: [],
        optionSource: 'custom',
        dictCode: null,
        fieldOptionsText: '选项1\n选项2',
        quickOptionsText: '',
      });
    }
  }

  // 移除子字段
  function removeSubField(index: number) {
    if (currentField.value?.children) {
      currentField.value.children.splice(index, 1);
    }
  }

  // 子字段数据字典选择变化
  function onSubFieldDictCodeChange(subField: any, dictCode: string) {
    subField.dictCode = dictCode;
    // 清空自定义选项
    subField.fieldOptions = undefined;
    subField.quickOptions = undefined;
    subField.fieldOptionsText = '';
    subField.quickOptionsText = '';
  }

  // 更新子字段选项
  function updateSubFieldOptions(subField: any) {
    if (subField.fieldOptionsText) {
      subField.fieldOptions = subField.fieldOptionsText
        .split('\n')
        .map((item: string) => item.trim())
        .filter((item: string) => item.length > 0);
    } else {
      subField.fieldOptions = [];
    }
  }

  // 更新子字段快捷选项
  function updateSubFieldQuickOptions(subField: any) {
    if (subField.quickOptionsText) {
      subField.quickOptions = subField.quickOptionsText
        .split('\n')
        .map((item: string) => item.trim())
        .filter((item: string) => item.length > 0);
    } else {
      subField.quickOptions = [];
    }
  }

  // 保存配置
  async function saveConfig() {
    if (!templateConfig.value) {
      message.error('模板配置不存在');
      return;
    }

    // 应用当前编辑的字段
    applyCurrentFieldChanges();

    try {
      saving.value = true;

      // 构建保存数据（转换为后端格式）
      // 为 step2Fields 设置正确的 stepNumber
      const step2FieldsWithStepNumber = step2Fields.value.map(field => ({
        ...field,
        stepNumber: 2
      }));

      // 为 step3Fields 设置正确的 stepNumber
      const step3FieldsWithStepNumber = step3Fields.value.map(field => ({
        ...field,
        stepNumber: 3
      }));

      const allFields = [...step2FieldsWithStepNumber, ...step3FieldsWithStepNumber];
      console.log('保存前的所有字段:', allFields);

      const convertedFields = convertFieldsToBackend(allFields);
      console.log('转换后的字段数据:', convertedFields);

      const saveData = {
        templateId: templateConfig.value.templateId,
        fields: convertedFields,
      };

      const response = await policeFormTemplateApi.saveFields(saveData);
      if (response.ok) {
        message.success('配置保存成功');
      } else {
        message.error(response.msg || '保存失败');
      }
    } catch (error) {
      console.error('保存配置失败:', error);
      smartSentry.captureError(error);
      message.error('保存配置失败');
    } finally {
      saving.value = false;
    }
  }

  // 应用当前字段的修改
  function applyCurrentFieldChanges() {
    if (!currentField.value || currentFieldIndex.value === -1) return;

    if (currentFieldStep.value === 2) {
      step2Fields.value[currentFieldIndex.value] = { ...currentField.value };
    } else {
      step3Fields.value[currentFieldIndex.value] = { ...currentField.value };
    }
  }

  // 预览表单
  function previewForm() {
    previewVisible.value = true;
  }

  // 转换后端字段格式到前端格式
  function convertFieldsFormat(backendFields: any[]): FieldItem[] {
    return backendFields.map(field => {
      // 兼容不同的字段名格式
      const fieldKey = field.fieldKey || field.key || `field_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

      // 处理选项来源
      const optionSource = field.dictCode ? 'dict' : 'custom';
      const fieldOptionsText = field.fieldOptions ?
        (Array.isArray(field.fieldOptions) ? field.fieldOptions.join('\n') : field.fieldOptions.toString()) : '';
      const quickOptionsText = field.quickOptions ?
        (Array.isArray(field.quickOptions) ? field.quickOptions.join('\n') : field.quickOptions.toString()) : '';

      const convertedField = {
        id: field.id,
        fieldKey: fieldKey,
        fieldLabel: field.fieldLabel || field.label,
        fieldType: field.fieldType || field.type,
        isRequired: field.isRequired !== undefined ? field.isRequired : field.required,
        fieldIcon: field.fieldIcon || field.icon,
        placeholder: field.placeholder,
        fieldOptions: field.fieldOptions || field.options,
        quickOptions: field.quickOptions,
        dictCode: field.dictCode, // 添加数据字典编码
        parentFieldId: field.parentFieldId,
        sortOrder: field.sortOrder,
        stepNumber: field.stepNumber,
        validationRules: field.validationRules,
        defaultValue: field.defaultValue,
        optionSource: optionSource,
        fieldOptionsText: fieldOptionsText,
        quickOptionsText: quickOptionsText,
        children: field.children ? convertFieldsFormat(field.children) :
                 field.fields ? convertFieldsFormat(field.fields) : undefined
      };

      return convertedField;
    });
  }

  // 转换前端字段格式到后端格式
  function convertFieldsToBackend(frontendFields: FieldItem[]): any[] {
    return frontendFields.map(field => {
      // 确保 fieldKey 不为空
      if (!field.fieldKey) {
        console.warn('字段缺少 fieldKey:', field);
        field.fieldKey = `field_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
      }

      // 确保 stepNumber 不为空 - 这很关键！
      if (!field.stepNumber) {
        console.warn('字段缺少 stepNumber:', field);
        field.stepNumber = 2; // 默认设为第2步
      }

      // 处理子字段，确保它们继承父字段的 stepNumber
      const processedChildren = field.children ? field.children.map(child => ({
        ...child,
        stepNumber: field.stepNumber, // 子字段继承父字段的步骤号
        fieldKey: child.fieldKey || `sub_field_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
      })) : undefined;

      return {
        id: field.id,
        fieldKey: field.fieldKey,  // 使用 fieldKey 而不是 key
        fieldLabel: field.fieldLabel,
        fieldType: field.fieldType,
        isRequired: field.isRequired,
        fieldIcon: field.fieldIcon,
        placeholder: field.placeholder,
        fieldOptions: field.dictCode ? undefined : field.fieldOptions, // 如果使用字典则不保存 fieldOptions
        quickOptions: field.quickOptions,
        dictCode: field.dictCode, // 添加数据字典编码
        parentFieldId: field.parentFieldId,
        sortOrder: field.sortOrder || 0, // 确保 sortOrder 不为 null
        stepNumber: field.stepNumber,
        validationRules: field.validationRules,
        defaultValue: field.defaultValue,
        children: processedChildren ? convertFieldsToBackend(processedChildren) : undefined
      };
    });
  }

  // 创建默认配置（临时方案）
  function createDefaultConfig() {
    step2Fields.value = [
      {
        fieldKey: 'fireLocation',
        fieldLabel: '起火地点',
        fieldType: 'input',
        isRequired: true,
        fieldIcon: '📍',
        placeholder: '请输入详细地址',
        sortOrder: 1,
        stepNumber: 2
      },
      {
        fieldKey: 'fireScale',
        fieldLabel: '火势程度',
        fieldType: 'select',
        isRequired: true,
        fieldIcon: '🔥',
        fieldOptions: ['轻微火情', '一般火灾', '较大火灾', '重大火灾', '特别重大火灾'],
        quickOptions: ['轻微', '一般', '较大', '重大'],
        sortOrder: 2,
        stepNumber: 2
      },
      {
        fieldKey: 'peopleInfo',
        fieldLabel: '人员情况',
        fieldType: 'compact-group',
        isRequired: true,
        fieldIcon: '👥',
        sortOrder: 3,
        stepNumber: 2,
        children: [
          {
            fieldKey: 'trappedCount',
            fieldLabel: '被困人数',
            fieldType: 'select',
            isRequired: false,
            quickOptions: ['0', '1-3人', '4-10人', '10+人'],
            sortOrder: 1
          },
          {
            fieldKey: 'casualties',
            fieldLabel: '伤亡情况',
            fieldType: 'select',
            isRequired: false,
            quickOptions: ['无', '轻伤', '重伤', '死亡'],
            sortOrder: 2
          },
          {
            fieldKey: 'evacuated',
            fieldLabel: '疏散人数',
            fieldType: 'input',
            isRequired: false,
            placeholder: '人',
            quickOptions: ['0', '1-10人', '11-50人', '50+人'],
            sortOrder: 3
          }
        ]
      }
    ];

    step3Fields.value = [
      {
        fieldKey: 'fireSource',
        fieldLabel: '起火原因',
        fieldType: 'select',
        isRequired: false,
        fieldIcon: '🔍',
        fieldOptions: ['电气故障', '用火不慎', '吸烟', '玩火', '自燃', '雷击', '其他', '不明'],
        quickOptions: ['电气', '用火', '吸烟', '其他'],
        sortOrder: 1,
        stepNumber: 3
      },
      {
        fieldKey: 'burnArea',
        fieldLabel: '过火面积',
        fieldType: 'input',
        isRequired: false,
        fieldIcon: '📏',
        placeholder: '平方米',
        quickOptions: ['<10平方米', '10-100平方米', '100-1000平方米', '>1000平方米'],
        sortOrder: 2,
        stepNumber: 3
      },
      {
        fieldKey: 'economicLoss',
        fieldLabel: '经济损失',
        fieldType: 'select',
        isRequired: false,
        fieldIcon: '💰',
        fieldOptions: ['无损失', '轻微损失(<1万)', '一般损失(1-10万)', '较大损失(10-100万)', '重大损失(>100万)'],
        quickOptions: ['无', '轻微', '一般', '较大', '重大'],
        sortOrder: 3,
        stepNumber: 3
      },
      {
        fieldKey: 'rescueStatus',
        fieldLabel: '救援状态',
        fieldType: 'checkbox-compact',
        isRequired: false,
        fieldIcon: '🚒',
        quickOptions: ['已派出', '到达现场', '开始救援', '火势控制', '完全扑灭'],
        sortOrder: 4,
        stepNumber: 3
      }
    ];

    templateConfig.value = {
      reportType: 1,
      step2Fields: step2Fields.value,
      step3Fields: step3Fields.value
    };

    console.log('已创建默认配置');
  }

  // 返回列表
  function goBack() {
    router.push('/oa/police/form-template-list');
  }
</script>

<style scoped lang="scss">
.form-template-config {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

/* 顶部工具栏 */
.top-toolbar {
  height: 80px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .page-info {
      .page-title {
        font-size: 20px;
        font-weight: 600;
        color: #1f2937;
        margin: 0;
      }

      .page-subtitle {
        font-size: 14px;
        color: #6b7280;
        margin: 4px 0 0 0;
      }
    }
  }

  .toolbar-right {
    display: flex;
    gap: 12px;
  }
}

/* 主体内容 */
.config-content {
  flex: 1;
  display: flex;
  gap: 24px;
  padding: 24px;
  overflow: hidden;
}

/* 字段组件库 */
.field-library {
  width: 280px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  overflow-y: auto;

  .library-header {
    padding: 20px;
    border-bottom: 1px solid #f0f0f0;

    h3 {
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
      margin: 0 0 4px 0;
    }

    p {
      font-size: 12px;
      color: #6b7280;
      margin: 0;
    }
  }

  .field-categories {
    padding: 16px;
  }

  .field-category {
    margin-bottom: 24px;

    .category-title {
      font-size: 14px;
      font-weight: 600;
      color: #374151;
      margin: 0 0 12px 0;
    }

    .field-items {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .field-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px;
      background: #f9fafb;
      border: 1px solid #e5e7eb;
      border-radius: 6px;
      cursor: grab;
      transition: all 0.2s ease;

      &:hover {
        background: #f3f4f6;
        border-color: #3b82f6;
        transform: translateY(-1px);
      }

      &:active {
        cursor: grabbing;
      }

      .field-icon {
        font-size: 16px;
      }

      .field-name {
        font-size: 14px;
        font-weight: 500;
        color: #374151;
      }
    }
  }
}

/* 配置区域 */
.config-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
  overflow-y: auto;
}

.step-section {
  background: white;
  border-radius: 8px;
  border: 1px solid #e8e8e8;

  .step-header {
    padding: 20px;
    border-bottom: 1px solid #f0f0f0;

    .step-title {
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
      margin: 0 0 4px 0;
    }

    .step-desc {
      font-size: 12px;
      color: #6b7280;
      margin: 0;
    }
  }

  .field-drop-zone {
    min-height: 200px;
    padding: 16px;

    .config-field-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      background: #f9fafb;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      margin-bottom: 12px;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        background: #f3f4f6;
        border-color: #3b82f6;
      }

      .field-content {
        display: flex;
        align-items: center;
        gap: 12px;
        flex: 1;

        .field-icon {
          font-size: 20px;
        }

        .field-info {
          .field-label {
            font-size: 14px;
            font-weight: 600;
            color: #1f2937;
          }

          .field-type {
            font-size: 12px;
            color: #6b7280;
          }
        }

        .field-required {
          .required-mark {
            color: #dc2626;
            font-weight: 600;
          }
        }
      }

      .field-actions {
        display: flex;
        gap: 4px;
      }
    }

    .empty-drop-zone {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 120px;
      border: 2px dashed #d1d5db;
      border-radius: 8px;
      color: #6b7280;

      .empty-icon {
        font-size: 32px;
        margin-bottom: 8px;
      }

      .empty-text {
        font-size: 14px;
      }
    }
  }
}

/* 字段属性配置 */
.field-properties {
  width: 320px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  overflow-y: auto;

  .properties-header {
    padding: 20px;
    border-bottom: 1px solid #f0f0f0;

    h3 {
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
      margin: 0;
    }
  }

  .properties-form {
    padding: 20px;

    .options-config {
      .option-item {
        display: flex;
        gap: 8px;
        margin-bottom: 8px;
        align-items: center;

        .remove-option {
          color: #dc2626;
          flex-shrink: 0;
        }
      }
    }

    // 数据字典预览样式
    .dict-preview {
      margin-top: 12px;
      padding: 12px;
      background: #f9f9f9;
      border-radius: 6px;
      border: 1px solid #e1e1e1;

      .preview-title {
        font-size: 13px;
        font-weight: 600;
        color: #666;
        margin-bottom: 8px;
      }

      .preview-items {
        display: flex;
        flex-wrap: wrap;
        gap: 4px;
      }

      .preview-tag {
        font-size: 12px;
        margin: 0;
      }
    }

    .sub-fields-config {
      .sub-field-item {
        border: 1px solid #e5e7eb;
        border-radius: 6px;
        padding: 12px;
        margin-bottom: 12px;

        .sub-field-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 12px;

          .sub-field-title {
            font-size: 14px;
            font-weight: 600;
            color: #374151;
          }

          .remove-sub-field {
            color: #dc2626;
          }
        }

        .sub-field-form {
          display: flex;
          flex-direction: column;
          gap: 8px;
        }
      }
    }
  }

  .no-selection {
    height: 300px;
    display: flex;
    align-items: center;
    justify-content: center;

    .no-selection-content {
      text-align: center;

      .no-selection-icon {
        font-size: 48px;
        margin-bottom: 12px;
      }

      .no-selection-text {
        font-size: 14px;
        color: #6b7280;
      }
    }
  }
}

/* 加载状态 */
.loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;

  .loading-text {
    font-size: 16px;
    color: #6b7280;
  }
}

/* 危险按钮 */
.danger-btn {
  color: #dc2626;

  &:hover {
    background: #fee2e2;
    color: #dc2626;
  }
}

/* 真实表单预览样式 - 完全复制emergency-intake的样式 */
.real-form-preview {
  .preview-main-content {
    display: flex;
    gap: 24px;
    height: 600px;
    max-height: 600px;
    overflow: hidden;
  }

  .preview-left-panel,
  .preview-right-panel {
    flex: 1;
    background: #f5f7fa;
    border-radius: 12px;
    padding: 16px;
    overflow: hidden;
    height: 100%;
    max-height: 100%;
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


  /* 基本信息字段样式 */
  .basic-info-fields {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .basic-field-item {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .basic-field-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .basic-field-item label {
    font-size: 14px;
    font-weight: 600;
    color: #374151;
  }

  /* 多选按钮样式 */
  .checkbox-options {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .checkbox-btn {
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
}
</style>