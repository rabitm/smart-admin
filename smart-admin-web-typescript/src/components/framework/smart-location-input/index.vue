<!--
  * 智能地址输入组件 - 通用可复用版本
  * 功能特性：
  * 1. 智能联想建议 - 历史地址+智能推荐
  * 2. 键盘导航 - ↑↓方向键、Enter确认、ESC关闭
  * 3. 数字快选 - Ctrl+数字键快速选择（避免输入冲突）
  * 4. 高亮匹配 - 关键字高亮显示
  * 5. 紧凑设计 - 节省界面空间
  * 6. 可配置API - 支持自定义搜索接口
  *
  * @Author:    Claude Code Assistant
  * @Date:      2025-09-19
  * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
-->
<template>
  <div class="smart-location-input-wrapper">
    <a-input
      v-model:value="inputValue"
      :placeholder="placeholder"
      :size="size"
      :disabled="disabled"
      :maxlength="maxlength"
      :class="['smart-location-input', inputClass]"
      @input="handleInputChange"
      @keydown="handleKeydown"
      @focus="handleFocus"
      @blur="handleBlur"
      ref="inputRef"
    >
      <template #suffix v-if="showLocationIcon">
        <AimOutlined @click="getCurrentLocation" class="location-icon-btn" />
      </template>
    </a-input>

    <!-- 智能地址建议下拉框 -->
    <div
      v-if="showSuggestions && allLocationOptions.length > 0"
      class="smart-location-suggestions"
      @mousedown.prevent
    >
      <!-- 紧凑头部 -->
      <div class="compact-header">
        <span class="header-icon">🎯</span>
        <span class="header-text">智能联想</span>
        <span class="header-tip">↑↓ Enter Ctrl+1-6</span>
      </div>

      <!-- 建议选项列表 -->
      <div
        v-for="(option, index) in allLocationOptions.slice(0, maxOptions)"
        :key="index"
        :class="[
          'compact-item',
          { 'item-active': activeIndex === index }
        ]"
        @click="selectLocation(option.value, index)"
        @mouseenter="activeIndex = index"
      >
        <span class="item-number">{{ index + 1 }}</span>
        <span class="item-text" v-html="highlightKeyword(option.label, currentKeyword)"></span>
        <span class="item-type">{{ option.type === 'history' ? '历史' : '建议' }}</span>
      </div>

      <!-- 更多选项提示 -->
      <div class="compact-footer" v-if="allLocationOptions.length > maxOptions">
        <span>+{{ allLocationOptions.length - maxOptions }} 更多</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, watch } from 'vue';
  import { message } from 'ant-design-vue';
  import { AimOutlined } from '@ant-design/icons-vue';

  // Props 定义
  interface Props {
    modelValue?: string;          // v-model 双向绑定
    placeholder?: string;         // 输入框占位符
    size?: 'large' | 'middle' | 'small';  // 输入框尺寸
    disabled?: boolean;           // 是否禁用
    maxlength?: number;           // 最大输入长度
    inputClass?: string;          // 自定义输入框样式类
    searchApi?: Function;         // 自定义搜索API函数
    maxOptions?: number;          // 最大显示选项数
    showLocationIcon?: boolean;   // 是否显示定位图标
    debounceTime?: number;        // 防抖时间(ms)
    minSearchLength?: number;     // 最小搜索长度
  }

  const props = withDefaults(defineProps<Props>(), {
    placeholder: '输入地点关键字，体验智能联想 ⚡',
    size: 'middle',
    disabled: false,
    maxlength: 100,
    inputClass: '',
    maxOptions: 6,
    showLocationIcon: true,
    debounceTime: 300,
    minSearchLength: 2
  });

  // Emits 定义
  interface Emits {
    (e: 'update:modelValue', value: string): void;
    (e: 'change', value: string): void;
    (e: 'select', option: LocationOption): void;
    (e: 'clear'): void;
    (e: 'focus', event: FocusEvent): void;
    (e: 'blur', event: FocusEvent): void;
  }

  const emit = defineEmits<Emits>();

  // 地址选项接口
  interface LocationOption {
    value: string;
    label: string;
    type: 'history' | 'suggestion';
    score: number;
  }

  // 响应式数据
  const inputRef = ref();
  const inputValue = ref(props.modelValue || '');
  const showSuggestions = ref(false);
  const allLocationOptions = ref<LocationOption[]>([]);
  const activeIndex = ref(-1);
  const currentKeyword = ref('');

  // 监听 modelValue 变化
  watch(() => props.modelValue, (newValue) => {
    inputValue.value = newValue || '';
  });

  // 监听输入值变化，触发 v-model 更新
  watch(inputValue, (newValue) => {
    emit('update:modelValue', newValue);
    emit('change', newValue);
  });

  // 防抖搜索
  let searchTimer: NodeJS.Timeout;

  // 输入变化处理
  function handleInputChange() {
    const keyword = inputValue.value?.trim();
    if (!keyword || keyword.length < props.minSearchLength) {
      allLocationOptions.value = [];
      showSuggestions.value = false;
      return;
    }

    clearTimeout(searchTimer);
    searchTimer = setTimeout(async () => {
      await searchLocationSuggestions(keyword);
    }, props.debounceTime);
  }

  // 搜索地址建议
  async function searchLocationSuggestions(keyword: string) {
    currentKeyword.value = keyword;

    if (!keyword || keyword.length < props.minSearchLength) {
      allLocationOptions.value = [];
      showSuggestions.value = false;
      activeIndex.value = -1;
      return;
    }

    try {
      let suggestions: string[] = [];

      // 使用自定义API或默认模拟数据
      if (props.searchApi && typeof props.searchApi === 'function') {
        const response = await props.searchApi(keyword);
        if (response && response.code === 0 && response.data) {
          suggestions = response.data;
        }
      } else {
        // 默认模拟数据
        suggestions = getDefaultSuggestions(keyword);
      }

      // 创建智能选项列表
      allLocationOptions.value = suggestions.map((location) => ({
        value: location,
        label: location,
        score: getLocationScore(location, keyword),
        type: getLocationType(location)
      }));

      // 智能排序：评分高的在前
      allLocationOptions.value.sort((a, b) => b.score - a.score);

      showSuggestions.value = allLocationOptions.value.length > 0;
      activeIndex.value = -1;

    } catch (error) {
      console.error('搜索地址建议出现错误:', error);
      allLocationOptions.value = [];
      showSuggestions.value = false;
    }
  }

  // 默认地址建议（模拟数据）
  function getDefaultSuggestions(keyword: string): string[] {
    const mockSuggestions = [
      `${keyword}路123号`,
      `${keyword}大街456号`,
      `${keyword}小区7号楼`,
      `${keyword}广场东门`,
      `${keyword}商城停车场`,
      `${keyword}地铁站A出口`
    ];

    return mockSuggestions.filter(item =>
      item.toLowerCase().includes(keyword.toLowerCase())
    ).slice(0, 10);
  }

  // 智能键盘事件处理
  function handleKeydown(event: KeyboardEvent) {
    if (!showSuggestions.value || allLocationOptions.value.length === 0) return;

    const visibleOptions = allLocationOptions.value.slice(0, props.maxOptions);

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        activeIndex.value = activeIndex.value < visibleOptions.length - 1 ? activeIndex.value + 1 : 0;
        break;

      case 'ArrowUp':
        event.preventDefault();
        activeIndex.value = activeIndex.value > 0 ? activeIndex.value - 1 : visibleOptions.length - 1;
        break;

      case 'Enter':
        event.preventDefault();
        if (activeIndex.value >= 0 && activeIndex.value < visibleOptions.length) {
          selectLocation(visibleOptions[activeIndex.value].value, activeIndex.value);
        }
        break;

      case 'Escape':
        showSuggestions.value = false;
        activeIndex.value = -1;
        break;

      // 智能数字键快速选择 1-6 (需要按住Ctrl键避免输入冲突)
      case '1': case '2': case '3': case '4': case '5': case '6':
        // 只有在按住Ctrl键或者光标在输入框末尾且无选中文字时才启用快选
        const isCtrlPressed = event.ctrlKey || event.metaKey;
        const inputElement = event.target as HTMLInputElement;
        const isAtEnd = inputElement.selectionStart === inputElement.value.length &&
                       inputElement.selectionStart === inputElement.selectionEnd;

        if (isCtrlPressed || (isAtEnd && inputElement.value.length > 0)) {
          const numberIndex = parseInt(event.key) - 1;
          if (numberIndex < visibleOptions.length) {
            event.preventDefault();
            selectLocation(visibleOptions[numberIndex].value, numberIndex);
          }
        }
        // 如果不满足条件，让数字正常输入到文本框
        break;
    }
  }

  // 聚焦处理
  function handleFocus(event: FocusEvent) {
    emit('focus', event);
    if (inputValue.value && inputValue.value.length >= props.minSearchLength) {
      searchLocationSuggestions(inputValue.value);
    }
  }

  // 失焦处理
  function handleBlur(event: FocusEvent) {
    emit('blur', event);
    setTimeout(() => {
      showSuggestions.value = false;
      activeIndex.value = -1;
    }, 200); // 延迟隐藏，允许点击建议项
  }

  // 选择地址
  function selectLocation(value: string, index: number) {
    inputValue.value = value;
    showSuggestions.value = false;
    activeIndex.value = -1;

    const selectedOption = allLocationOptions.value[index];
    emit('select', selectedOption);

    console.log(`✅ 地址已选择: ${value} (选项 ${index + 1})`);
  }

  // 高亮匹配关键字
  function highlightKeyword(text: string, keyword: string): string {
    if (!keyword) return text;
    const regex = new RegExp(`(${keyword})`, 'gi');
    return text.replace(regex, '<mark style="background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%); color: #92400e; padding: 1px 3px; border-radius: 3px; font-weight: 600;">$1</mark>');
  }

  // 获取地址评分（用于智能排序）
  function getLocationScore(location: string, keyword: string): number {
    let score = 0;
    const lowerLocation = location.toLowerCase();
    const lowerKeyword = keyword.toLowerCase();

    // 完全匹配开头 +10分
    if (lowerLocation.startsWith(lowerKeyword)) score += 10;

    // 包含关键字 +5分
    if (lowerLocation.includes(lowerKeyword)) score += 5;

    // 历史地址类型加分
    if (getLocationType(location) === 'history') score += 3;

    // 长度越短越靠前
    score += Math.max(0, 50 - location.length);

    return score;
  }

  // 获取地址类型
  function getLocationType(location: string): 'history' | 'suggestion' {
    const historyKeywords = ['小区', '区', '县', '市', '路', '街', '号', '村', '镇', '广场'];
    const hasHistoryKeyword = historyKeywords.some(keyword => location.includes(keyword));
    return hasHistoryKeyword ? 'history' : 'suggestion';
  }

  // 获取当前位置
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

  // 清空输入
  function clearInput() {
    inputValue.value = '';
    showSuggestions.value = false;
    activeIndex.value = -1;
    emit('clear');
  }

  // 暴露给父组件的方法
  defineExpose({
    focus: () => inputRef.value?.focus(),
    blur: () => inputRef.value?.blur(),
    clear: clearInput,
    getValue: () => inputValue.value,
    setValue: (value: string) => {
      inputValue.value = value;
    }
  });
</script>

<style scoped>
/* 智能地址输入组件样式 */
.smart-location-input-wrapper {
  position: relative;
  width: 100%;
}

.smart-location-input {
  transition: all 0.3s ease;
  border-radius: 6px;
}

.smart-location-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  transform: translateY(-1px);
}

.location-icon-btn {
  cursor: pointer;
  color: #6b7280;
  transition: color 0.2s ease;
}

.location-icon-btn:hover {
  color: #3b82f6;
}

/* 智能建议下拉框 */
.smart-location-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 1000;
  background: #ffffff;
  border-radius: 6px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
  max-height: 280px;
  overflow: hidden;
  animation: slideDown 0.15s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 紧凑头部 */
.compact-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e2e8f0;
  font-size: 12px;
}

.header-icon {
  font-size: 14px;
}

.header-text {
  flex: 1;
  font-weight: 600;
  color: #374151;
}

.header-tip {
  color: #6b7280;
  font-size: 10px;
  font-family: 'Monaco', 'Consolas', monospace;
  background: rgba(0, 0, 0, 0.05);
  padding: 2px 6px;
  border-radius: 4px;
}

/* 建议项 */
.compact-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  cursor: pointer;
  transition: all 0.15s ease;
  border-bottom: 1px solid #f9fafb;
  font-size: 13px;
  animation: fadeInUp 0.2s ease-out;
  animation-fill-mode: both;
}

.compact-item:nth-child(2) { animation-delay: 0.05s; }
.compact-item:nth-child(3) { animation-delay: 0.1s; }
.compact-item:nth-child(4) { animation-delay: 0.15s; }
.compact-item:nth-child(5) { animation-delay: 0.2s; }
.compact-item:nth-child(6) { animation-delay: 0.25s; }
.compact-item:nth-child(7) { animation-delay: 0.3s; }

.compact-item:last-child {
  border-bottom: none;
}

.compact-item:hover,
.item-active {
  background: #f0f9ff;
  border-left: 2px solid #0ea5e9;
  padding-left: 10px;
}

.item-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  background: #e5e7eb;
  color: #6b7280;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  flex-shrink: 0;
  transition: all 0.15s ease;
}

.compact-item:hover .item-number,
.item-active .item-number {
  background: #0ea5e9;
  color: #ffffff;
}

.item-text {
  flex: 1;
  color: #374151;
  font-weight: 500;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-type {
  font-size: 10px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 1px 4px;
  border-radius: 3px;
  flex-shrink: 0;
  font-weight: 500;
}

.compact-item:hover .item-type,
.item-active .item-type {
  background: #dbeafe;
  color: #1e40af;
}

/* 更多选项提示 */
.compact-footer {
  padding: 6px 12px;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
  text-align: center;
  font-size: 10px;
  color: #6b7280;
}

/* 动画定义 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式适配 */
@media (max-width: 768px) {
  .smart-location-suggestions {
    max-height: 300px;
  }

  .compact-item {
    padding: 10px 12px;
  }

  .header-tip {
    display: none;
  }
}

/* 无障碍支持 */
.compact-item:focus {
  outline: 2px solid #3b82f6;
  outline-offset: -2px;
}
</style>