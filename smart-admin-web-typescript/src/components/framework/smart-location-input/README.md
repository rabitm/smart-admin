# SmartLocationInput 智能地址输入组件

一个功能强大的智能地址输入组件，集成了智能联想、键盘导航、数字快选等高级功能。

## 功能特性

🎯 **智能联想建议** - 支持历史地址+智能推荐
⌨️ **键盘导航** - ↑↓方向键、Enter确认、ESC关闭
🔢 **数字快选** - Ctrl+数字键快速选择（避免输入冲突）
🎨 **高亮匹配** - 关键字智能高亮显示
📱 **紧凑设计** - 节省界面空间的优雅设计
🔧 **可配置API** - 支持自定义搜索接口
🎮 **智能排序** - 基于相关度的智能排序算法

## 快速开始

### 基础用法

```vue
<template>
  <SmartLocationInput
    v-model="address"
    placeholder="请输入地址"
    @select="handleAddressSelect"
  />
</template>

<script setup>
import SmartLocationInput from '/@/components/framework/smart-location-input/index.vue';
import { ref } from 'vue';

const address = ref('');

function handleAddressSelect(option) {
  console.log('选择的地址:', option.value);
}
</script>
```

### 自定义API

```vue
<template>
  <SmartLocationInput
    v-model="address"
    :search-api="myCustomSearchApi"
    placeholder="输入地点关键字"
    :max-options="8"
    @select="handleSelect"
  />
</template>

<script setup>
import { myApi } from '/@/api/my-api';

async function myCustomSearchApi(keyword) {
  const response = await myApi.searchLocations(keyword);
  return response; // 需要返回 { code: 0, data: string[] } 格式
}
</script>
```

## API 参数

### Props

| 参数 | 类型 | 默认值 | 说明 |
|------|------|-------|------|
| `modelValue` | `string` | `''` | v-model 绑定值 |
| `placeholder` | `string` | `'输入地点关键字，体验智能联想 ⚡'` | 输入框占位符 |
| `size` | `'large' \| 'middle' \| 'small'` | `'middle'` | 输入框尺寸 |
| `disabled` | `boolean` | `false` | 是否禁用 |
| `maxlength` | `number` | `100` | 最大输入长度 |
| `inputClass` | `string` | `''` | 自定义输入框样式类 |
| `searchApi` | `Function` | `undefined` | 自定义搜索API函数 |
| `maxOptions` | `number` | `6` | 最大显示选项数 |
| `showLocationIcon` | `boolean` | `true` | 是否显示定位图标 |
| `debounceTime` | `number` | `300` | 防抖时间(ms) |
| `minSearchLength` | `number` | `2` | 最小搜索长度 |

### Events

| 事件名 | 参数 | 说明 |
|--------|------|------|
| `update:modelValue` | `(value: string)` | v-model 更新事件 |
| `change` | `(value: string)` | 输入值变化事件 |
| `select` | `(option: LocationOption)` | 选择地址选项事件 |
| `clear` | `()` | 清空输入事件 |

### LocationOption 接口

```typescript
interface LocationOption {
  value: string;          // 地址值
  label: string;          // 显示标签
  type: 'history' | 'suggestion';  // 类型：历史或建议
  score: number;          // 相关度评分
}
```

### 方法

通过 `ref` 可以调用以下方法：

```vue
<template>
  <SmartLocationInput ref="locationInputRef" v-model="address" />
</template>

<script setup>
const locationInputRef = ref();

// 调用组件方法
locationInputRef.value.focus();       // 聚焦输入框
locationInputRef.value.blur();        // 失焦输入框
locationInputRef.value.clear();       // 清空输入
locationInputRef.value.getValue();    // 获取当前值
locationInputRef.value.setValue('新地址'); // 设置值
</script>
```

## 键盘操作

| 按键 | 功能 |
|------|------|
| `↑` `↓` | 导航选项 |
| `Enter` | 确认选择 |
| `ESC` | 关闭下拉框 |
| `Ctrl + 1~6` | 快速选择对应编号的选项 |

**注意**: 数字键快选只在按住 Ctrl 键或光标在输入框末尾时生效，避免与正常数字输入冲突。

## 自定义样式

组件提供了多个CSS类名供自定义样式：

```css
/* 输入框容器 */
.smart-location-input-wrapper { }

/* 输入框本身 */
.smart-location-input { }

/* 建议下拉框 */
.smart-location-suggestions { }

/* 建议项 */
.compact-item { }

/* 激活状态的建议项 */
.item-active { }
```

## 自定义搜索API

搜索API函数需要返回以下格式的数据：

```typescript
// API响应格式
interface ApiResponse {
  code: number;           // 0表示成功
  data: string[];         // 地址字符串数组
  msg?: string;          // 可选的消息
}

// 自定义搜索函数示例
async function customSearchApi(keyword: string): Promise<ApiResponse> {
  const response = await fetch(`/api/locations/search?q=${keyword}`);
  return await response.json();
}
```

## 高级配置

### 智能排序算法

组件内置智能排序算法，会根据以下因素对建议进行排序：

- 关键字匹配位置（开头匹配优先）
- 地址类型（历史地址优先）
- 地址长度（较短地址优先）
- 包含关键字的程度

### 地址类型识别

组件会自动识别地址类型：

- **历史地址**: 包含 '小区', '区', '县', '市', '路', '街', '号', '村', '镇', '广场' 等关键字
- **智能建议**: 其他类型的地址建议

## 最佳实践

1. **性能优化**: 使用 `debounceTime` 参数控制防抖时间，避免频繁API调用
2. **用户体验**: 设置合适的 `minSearchLength`，通常2-3个字符开始搜索
3. **响应式**: 组件已内置响应式适配，在移动端会自动调整样式
4. **无障碍**: 组件支持键盘导航，符合无障碍设计标准

## 示例场景

### 警情录入系统
```vue
<SmartLocationInput
  v-model="form.incidentLocation"
  placeholder="输入事发地点"
  :search-api="policeReportApi.searchLocationSuggestions"
  @select="handleLocationSelect"
/>
```

### 物流地址选择
```vue
<SmartLocationInput
  v-model="form.deliveryAddress"
  placeholder="输入收货地址"
  :search-api="logisticsApi.searchAddresses"
  :max-options="8"
  :show-location-icon="false"
/>
```

### 房产地址搜索
```vue
<SmartLocationInput
  v-model="searchForm.propertyLocation"
  placeholder="搜索房产地址"
  :search-api="realEstateApi.searchProperties"
  size="large"
  input-class="custom-search-input"
/>
```

## 更新日志

### v1.0.0 (2025-09-19)

- ✨ 初始版本发布
- 🎯 智能地址联想功能
- ⌨️ 完整的键盘导航支持
- 🔢 智能数字键快选（解决输入冲突）
- 🎨 紧凑优雅的UI设计
- 📱 响应式适配
- 🔧 可配置的搜索API
- 🎮 智能排序算法

---

如有问题或建议，请联系开发团队或提交 Issue。