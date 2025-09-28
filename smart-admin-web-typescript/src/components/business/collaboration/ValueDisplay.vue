<!--
  * 值显示组件 - 智能显示不同类型的数据值
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-28
  * @Copyright 1024创新实验室
-->
<template>
  <div class="value-display" :class="`value-display-${type}`">
    <!-- 空值显示 -->
    <span v-if="isEmptyValue" class="empty-value">
      {{ type === 'before' ? '(空)' : '(未设置)' }}
    </span>

    <!-- 字符串值 -->
    <span v-else-if="isStringValue" class="string-value">
      <span v-if="isSensitive" class="sensitive-value">
        {{ maskSensitiveValue(stringValue) }}
        <a-button type="text" size="small" @click="toggleSensitiveVisibility">
          <template #icon>
            <EyeOutlined v-if="showSensitive" />
            <EyeInvisibleOutlined v-else />
          </template>
        </a-button>
      </span>
      <span v-else class="normal-string" :title="stringValue">
        {{ truncatedStringValue }}
      </span>
    </span>

    <!-- 数字值 -->
    <span v-else-if="isNumberValue" class="number-value">
      {{ formatNumber(value as number) }}
    </span>

    <!-- 布尔值 -->
    <span v-else-if="isBooleanValue" class="boolean-value">
      <a-tag :color="value ? 'green' : 'red'" size="small">
        {{ value ? '是' : '否' }}
      </a-tag>
    </span>

    <!-- 日期值 -->
    <span v-else-if="isDateValue" class="date-value">
      <a-tooltip :title="formatFullDate(dateValue)">
        {{ formatRelativeDate(dateValue) }}
      </a-tooltip>
    </span>

    <!-- 数组值 -->
    <div v-else-if="isArrayValue" class="array-value">
      <div class="array-header">
        <span class="array-label">数组 ({{ arrayValue.length }} 项)</span>
        <a-button type="text" size="small" @click="toggleArrayExpanded">
          <template #icon>
            <DownOutlined :class="{ 'rotate-180': arrayExpanded }" />
          </template>
        </a-button>
      </div>
      <a-collapse-transition>
        <div v-show="arrayExpanded" class="array-content">
          <div
            v-for="(item, index) in arrayValue"
            :key="index"
            class="array-item"
          >
            <span class="array-index">[{{ index }}]</span>
            <ValueDisplay :value="item" :type="type" />
          </div>
        </div>
      </a-collapse-transition>
    </div>

    <!-- 对象值 -->
    <div v-else-if="isObjectValue" class="object-value">
      <div class="object-header">
        <span class="object-label">对象 ({{ objectKeys.length }} 属性)</span>
        <a-button type="text" size="small" @click="toggleObjectExpanded">
          <template #icon>
            <DownOutlined :class="{ 'rotate-180': objectExpanded }" />
          </template>
        </a-button>
      </div>
      <a-collapse-transition>
        <div v-show="objectExpanded" class="object-content">
          <div
            v-for="key in objectKeys"
            :key="key"
            class="object-property"
          >
            <span class="property-key">{{ key }}:</span>
            <ValueDisplay :value="objectValue[key]" :type="type" />
          </div>
        </div>
      </a-collapse-transition>
    </div>

    <!-- 文件值 -->
    <div v-else-if="isFileValue" class="file-value">
      <div class="file-info">
        <FileOutlined class="file-icon" />
        <span class="file-name">{{ fileValue.name }}</span>
        <span class="file-size">({{ formatFileSize(fileValue.size) }})</span>
      </div>
      <div v-if="fileValue.url" class="file-actions">
        <a-button type="link" size="small" @click="previewFile">
          <template #icon><EyeOutlined /></template>
          预览
        </a-button>
        <a-button type="link" size="small" @click="downloadFile">
          <template #icon><DownloadOutlined /></template>
          下载
        </a-button>
      </div>
    </div>

    <!-- 颜色值 -->
    <div v-else-if="isColorValue" class="color-value">
      <div
        class="color-swatch"
        :style="{ backgroundColor: value as string }"
        :title="value as string"
      ></div>
      <span class="color-text">{{ value }}</span>
    </div>

    <!-- URL值 -->
    <div v-else-if="isUrlValue" class="url-value">
      <a-button type="link" size="small" @click="openUrl" :title="value as string">
        <template #icon><LinkOutlined /></template>
        {{ truncateUrl(value as string) }}
      </a-button>
    </div>

    <!-- 未知类型 -->
    <div v-else class="unknown-value">
      <div class="unknown-header">
        <span class="unknown-label">{{ getValueType(value) }}</span>
        <a-button type="text" size="small" @click="toggleRawExpanded">
          <template #icon>
            <CodeOutlined />
          </template>
          原始
        </a-button>
      </div>
      <a-collapse-transition>
        <div v-show="rawExpanded" class="raw-content">
          <pre>{{ JSON.stringify(value, null, 2) }}</pre>
        </div>
      </a-collapse-transition>
    </div>

    <!-- 差异高亮 -->
    <div v-if="showDiff && previousValue !== undefined" class="diff-indicator">
      <a-tooltip title="与上一次值的差异">
        <DiffOutlined class="diff-icon" />
      </a-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { format, formatDistanceToNow } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import {
  EyeOutlined,
  EyeInvisibleOutlined,
  DownOutlined,
  FileOutlined,
  DownloadOutlined,
  LinkOutlined,
  CodeOutlined,
  DiffOutlined
} from '@ant-design/icons-vue';

// =============== Props ===============

interface Props {
  value: any;
  type: 'before' | 'after' | 'current';
  maxLength?: number;
  sensitive?: boolean;
  showDiff?: boolean;
  previousValue?: any;
}

const props = withDefaults(defineProps<Props>(), {
  maxLength: 100,
  sensitive: false,
  showDiff: false
});

// =============== 响应式数据 ===============

const showSensitive = ref(false);
const arrayExpanded = ref(false);
const objectExpanded = ref(false);
const rawExpanded = ref(false);

// =============== 计算属性 ===============

const isEmptyValue = computed(() => {
  return props.value === null || props.value === undefined || props.value === '';
});

const isStringValue = computed(() => {
  return typeof props.value === 'string' && props.value !== '';
});

const isNumberValue = computed(() => {
  return typeof props.value === 'number' && !isNaN(props.value);
});

const isBooleanValue = computed(() => {
  return typeof props.value === 'boolean';
});

const isDateValue = computed(() => {
  if (!isStringValue.value && !isNumberValue.value) return false;

  const dateRegex = /^\d{4}-\d{2}-\d{2}|\d{4}\/\d{2}\/\d{2}|\d{13}$/;
  return dateRegex.test(String(props.value)) && !isNaN(Date.parse(String(props.value)));
});

const isArrayValue = computed(() => {
  return Array.isArray(props.value);
});

const isObjectValue = computed(() => {
  return props.value !== null &&
         typeof props.value === 'object' &&
         !Array.isArray(props.value) &&
         !(props.value instanceof Date) &&
         !isFileValue.value;
});

const isFileValue = computed(() => {
  return props.value &&
         typeof props.value === 'object' &&
         ('name' in props.value || 'fileName' in props.value) &&
         ('size' in props.value || 'fileSize' in props.value);
});

const isColorValue = computed(() => {
  if (!isStringValue.value) return false;
  const colorRegex = /^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$|^rgb\(|^rgba\(|^hsl\(|^hsla\(/;
  return colorRegex.test(props.value as string);
});

const isUrlValue = computed(() => {
  if (!isStringValue.value) return false;
  try {
    new URL(props.value as string);
    return true;
  } catch {
    return false;
  }
});

const stringValue = computed(() => String(props.value));

const truncatedStringValue = computed(() => {
  if (stringValue.value.length <= props.maxLength) {
    return stringValue.value;
  }
  return stringValue.value.substring(0, props.maxLength) + '...';
});

const isSensitive = computed(() => {
  if (props.sensitive) return true;

  // 自动检测敏感字段
  const sensitivePatterns = [
    /password/i,
    /token/i,
    /secret/i,
    /key/i,
    /phone/i,
    /mobile/i,
    /idcard/i,
    /身份证/i,
    /手机/i,
    /电话/i,
    /密码/i
  ];

  return sensitivePatterns.some(pattern => pattern.test(stringValue.value));
});

const dateValue = computed(() => {
  if (isNumberValue.value && String(props.value).length === 13) {
    return new Date(props.value as number);
  }
  return new Date(props.value as string);
});

const arrayValue = computed(() => props.value as any[]);

const objectValue = computed(() => props.value as Record<string, any>);

const objectKeys = computed(() => {
  if (!isObjectValue.value) return [];
  return Object.keys(objectValue.value).sort();
});

const fileValue = computed(() => {
  return {
    name: props.value.name || props.value.fileName || '未知文件',
    size: props.value.size || props.value.fileSize || 0,
    url: props.value.url || props.value.fileUrl,
    type: props.value.type || props.value.fileType
  };
});

// =============== 方法 ===============

const toggleSensitiveVisibility = () => {
  showSensitive.value = !showSensitive.value;
};

const toggleArrayExpanded = () => {
  arrayExpanded.value = !arrayExpanded.value;
};

const toggleObjectExpanded = () => {
  objectExpanded.value = !objectExpanded.value;
};

const toggleRawExpanded = () => {
  rawExpanded.value = !rawExpanded.value;
};

const maskSensitiveValue = (value: string): string => {
  if (showSensitive.value) return value;

  if (value.length <= 6) {
    return '*'.repeat(value.length);
  }

  const start = value.substring(0, 2);
  const end = value.substring(value.length - 2);
  const middle = '*'.repeat(value.length - 4);

  return start + middle + end;
};

const formatNumber = (num: number): string => {
  if (Number.isInteger(num)) {
    return num.toLocaleString();
  }

  if (num % 1 !== 0) {
    return num.toFixed(2);
  }

  return num.toString();
};

const formatRelativeDate = (date: Date): string => {
  return formatDistanceToNow(date, {
    addSuffix: true,
    locale: zhCN
  });
};

const formatFullDate = (date: Date): string => {
  return format(date, 'yyyy-MM-dd HH:mm:ss', { locale: zhCN });
};

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B';

  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

const truncateUrl = (url: string): string => {
  if (url.length <= 50) return url;

  try {
    const urlObj = new URL(url);
    return `${urlObj.hostname}${urlObj.pathname.substring(0, 20)}...`;
  } catch {
    return url.substring(0, 50) + '...';
  }
};

const getValueType = (value: any): string => {
  if (value === null) return 'null';
  if (value === undefined) return 'undefined';
  if (Array.isArray(value)) return 'array';
  return typeof value;
};

const previewFile = () => {
  if (fileValue.value.url) {
    window.open(fileValue.value.url, '_blank');
  }
};

const downloadFile = () => {
  if (fileValue.value.url) {
    const link = document.createElement('a');
    link.href = fileValue.value.url;
    link.download = fileValue.value.name;
    link.click();
  }
};

const openUrl = () => {
  window.open(props.value as string, '_blank');
};
</script>

<style scoped>
.value-display {
  display: inline-block;
  max-width: 100%;
  word-break: break-word;
}

.value-display-before {
  opacity: 0.8;
}

.value-display-after {
  font-weight: 500;
}

.empty-value {
  color: #bfbfbf;
  font-style: italic;
  font-size: 12px;
}

.string-value {
  color: #262626;
}

.sensitive-value {
  display: flex;
  align-items: center;
  gap: 4px;
}

.normal-string {
  word-break: break-word;
}

.number-value {
  color: #1890ff;
  font-family: monospace;
  font-weight: 500;
}

.boolean-value {
  display: inline-block;
}

.date-value {
  color: #52c41a;
  font-family: monospace;
}

.array-value,
.object-value {
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  background: #fafafa;
  overflow: hidden;
}

.array-header,
.object-header,
.unknown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 8px;
  background: #f5f5f5;
  border-bottom: 1px solid #f0f0f0;
  font-size: 12px;
}

.array-label,
.object-label,
.unknown-label {
  font-weight: 500;
  color: #595959;
}

.rotate-180 {
  transform: rotate(180deg);
  transition: transform 0.3s ease;
}

.array-content,
.object-content {
  padding: 8px;
  max-height: 200px;
  overflow-y: auto;
}

.array-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 4px;
  padding: 2px 0;
}

.array-item:last-child {
  margin-bottom: 0;
}

.array-index {
  color: #8c8c8c;
  font-family: monospace;
  font-size: 11px;
  min-width: 30px;
  text-align: right;
}

.object-property {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 6px;
  padding: 2px 0;
}

.object-property:last-child {
  margin-bottom: 0;
}

.property-key {
  color: #8c8c8c;
  font-family: monospace;
  font-size: 11px;
  min-width: 80px;
  font-weight: 500;
}

.file-value {
  border: 1px solid #e6f7ff;
  border-radius: 4px;
  background: #f6ffed;
  padding: 8px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.file-icon {
  color: #1890ff;
}

.file-name {
  font-weight: 500;
  color: #262626;
}

.file-size {
  color: #8c8c8c;
  font-size: 11px;
}

.file-actions {
  display: flex;
  gap: 4px;
}

.color-value {
  display: flex;
  align-items: center;
  gap: 6px;
}

.color-swatch {
  width: 16px;
  height: 16px;
  border-radius: 2px;
  border: 1px solid #d9d9d9;
  flex-shrink: 0;
}

.color-text {
  font-family: monospace;
  font-size: 12px;
  color: #262626;
}

.url-value {
  display: inline-block;
}

.unknown-value {
  border: 1px solid #ffecb3;
  border-radius: 4px;
  background: #fffbe6;
  overflow: hidden;
}

.raw-content {
  padding: 8px;
  background: white;
  border-top: 1px solid #ffe58f;
  max-height: 150px;
  overflow: auto;
}

.raw-content pre {
  margin: 0;
  font-family: 'Courier New', monospace;
  font-size: 11px;
  color: #595959;
  white-space: pre-wrap;
  word-break: break-all;
}

.diff-indicator {
  position: absolute;
  top: 2px;
  right: 2px;
}

.diff-icon {
  color: #fa8c16;
  font-size: 12px;
}

/* 滚动条美化 */
.array-content::-webkit-scrollbar,
.object-content::-webkit-scrollbar,
.raw-content::-webkit-scrollbar {
  width: 4px;
}

.array-content::-webkit-scrollbar-track,
.object-content::-webkit-scrollbar-track,
.raw-content::-webkit-scrollbar-track {
  background: #f0f0f0;
}

.array-content::-webkit-scrollbar-thumb,
.object-content::-webkit-scrollbar-thumb,
.raw-content::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

/* 暗色主题支持 */
@media (prefers-color-scheme: dark) {
  .array-value,
  .object-value {
    background: #262626;
    border-color: #404040;
  }

  .array-header,
  .object-header,
  .unknown-header {
    background: #1f1f1f;
    border-color: #404040;
  }

  .file-value {
    background: #1f2b1f;
    border-color: #2b5a2b;
  }

  .unknown-value {
    background: #2b2b1f;
    border-color: #5a5a2b;
  }

  .raw-content {
    background: #1f1f1f;
    border-color: #404040;
  }
}
</style>