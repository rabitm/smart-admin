<template>
  <div class="im-input-area">
    <!-- 工具栏 -->
    <div class="toolbar">
      <!-- 表情 -->
      <a-tooltip title="表情">
        <a-button type="text" size="small" @click="handleEmojiClick">
          <smile-outlined />
        </a-button>
      </a-tooltip>

      <!-- 图片 -->
      <a-tooltip title="发送图片">
        <a-upload
          :show-upload-list="false"
          :before-upload="handleImageUpload"
          accept="image/*"
          :disabled="disabled"
        >
          <a-button type="text" size="small" :disabled="disabled">
            <picture-outlined />
          </a-button>
        </a-upload>
      </a-tooltip>

      <!-- 文件 -->
      <a-tooltip title="发送文件">
        <a-upload :show-upload-list="false" :before-upload="handleFileUpload" :disabled="disabled">
          <a-button type="text" size="small" :disabled="disabled">
            <paper-clip-outlined />
          </a-button>
        </a-upload>
      </a-tooltip>

      <!-- 更多功能预留 -->
      <div class="toolbar-spacer"></div>

      <!-- 发送快捷键提示 -->
      <span class="send-hint">Enter 发送 / Ctrl+Enter 换行</span>
    </div>

    <!-- 输入框 -->
    <div class="input-wrapper">
      <a-textarea
        ref="inputRef"
        v-model:value="inputText"
        :placeholder="placeholder"
        :auto-size="{ minRows: 3, maxRows: 6 }"
        :disabled="disabled"
        @keydown="handleKeyDown"
        @paste="handlePaste"
      />
    </div>

    <!-- 底部操作栏 -->
    <div class="footer">
      <div class="footer-left">
        <!-- 字数统计 -->
        <span class="char-count">{{ inputText.length }} / {{ maxLength }}</span>
      </div>

      <div class="footer-right">
        <!-- 发送按钮 -->
        <a-button type="primary" :disabled="!canSend" :loading="isSending" @click="handleSend">
          发送
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { message as antMessage } from 'ant-design-vue';
import { SmileOutlined, PictureOutlined, PaperClipOutlined } from '@ant-design/icons-vue';

interface Props {
  placeholder?: string;
  disabled?: boolean;
  maxLength?: number;
}

interface Emits {
  (e: 'send-text', text: string): void;
  (e: 'send-image', file: File): void;
  (e: 'send-file', file: File): void;
  (e: 'emoji-click'): void;
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请输入消息...',
  disabled: false,
  maxLength: 5000,
});

const emit = defineEmits<Emits>();

const inputRef = ref<any>(null);
const inputText = ref('');
const isSending = ref(false);

/**
 * 是否可以发送
 */
const canSend = computed(() => {
  return inputText.value.trim().length > 0 && inputText.value.length <= props.maxLength && !props.disabled;
});

/**
 * 发送文本消息
 */
const handleSend = async () => {
  if (!canSend.value) {
    return;
  }

  const text = inputText.value.trim();

  try {
    isSending.value = true;
    emit('send-text', text);

    // 清空输入框
    inputText.value = '';

    // 重新聚焦到输入框
    inputRef.value?.focus();
  } catch (error) {
    console.error('发送消息失败:', error);
  } finally {
    isSending.value = false;
  }
};

/**
 * 处理键盘事件
 */
const handleKeyDown = (e: KeyboardEvent) => {
  // Enter 发送消息 (不按 Ctrl/Shift)
  if (e.key === 'Enter' && !e.ctrlKey && !e.shiftKey && !e.metaKey) {
    e.preventDefault();
    handleSend();
  }

  // Ctrl+Enter / Cmd+Enter 换行
  if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
    e.preventDefault();
    const cursorPos = (e.target as HTMLTextAreaElement).selectionStart;
    const textBefore = inputText.value.substring(0, cursorPos);
    const textAfter = inputText.value.substring(cursorPos);
    inputText.value = textBefore + '\n' + textAfter;

    // 设置光标位置
    setTimeout(() => {
      (e.target as HTMLTextAreaElement).setSelectionRange(cursorPos + 1, cursorPos + 1);
    }, 0);
  }
};

/**
 * 处理粘贴事件 (支持粘贴图片)
 */
const handlePaste = async (e: ClipboardEvent) => {
  const items = e.clipboardData?.items;
  if (!items) {
    return;
  }

  // 检查是否粘贴了图片
  for (let i = 0; i < items.length; i++) {
    const item = items[i];

    if (item.type.indexOf('image') !== -1) {
      e.preventDefault();

      const file = item.getAsFile();
      if (file) {
        await handleImageUpload(file);
      }
      break;
    }
  }
};

/**
 * 处理图片上传
 */
const handleImageUpload = async (file: File): Promise<boolean> => {
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    antMessage.error('只能上传图片文件');
    return false;
  }

  // 验证文件大小 (限制10MB)
  const maxSize = 10 * 1024 * 1024;
  if (file.size > maxSize) {
    antMessage.error('图片大小不能超过10MB');
    return false;
  }

  try {
    emit('send-image', file);
    return true;
  } catch (error) {
    console.error('上传图片失败:', error);
    antMessage.error('上传图片失败');
    return false;
  }
};

/**
 * 处理文件上传
 */
const handleFileUpload = async (file: File): Promise<boolean> => {
  // 验证文件大小 (限制100MB)
  const maxSize = 100 * 1024 * 1024;
  if (file.size > maxSize) {
    antMessage.error('文件大小不能超过100MB');
    return false;
  }

  try {
    emit('send-file', file);
    return true;
  } catch (error) {
    console.error('上传文件失败:', error);
    antMessage.error('上传文件失败');
    return false;
  }
};

/**
 * 处理表情按钮点击
 */
const handleEmojiClick = () => {
  emit('emoji-click');
};

/**
 * 聚焦到输入框
 */
const focus = () => {
  inputRef.value?.focus();
};

/**
 * 插入文本 (用于表情插入)
 */
const insertText = (text: string) => {
  const textarea = inputRef.value?.$el?.querySelector('textarea');
  if (!textarea) {
    inputText.value += text;
    return;
  }

  const cursorPos = textarea.selectionStart;
  const textBefore = inputText.value.substring(0, cursorPos);
  const textAfter = inputText.value.substring(cursorPos);
  inputText.value = textBefore + text + textAfter;

  // 设置光标位置
  setTimeout(() => {
    textarea.setSelectionRange(cursorPos + text.length, cursorPos + text.length);
    textarea.focus();
  }, 0);
};

// 暴露给父组件的方法
defineExpose({
  focus,
  insertText,
});
</script>

<style scoped lang="less">
.im-input-area {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;

  .toolbar-spacer {
    flex: 1;
  }

  .send-hint {
    font-size: 12px;
    color: #00000073;
  }
}

.input-wrapper {
  padding: 12px;

  :deep(.ant-input) {
    border: none;
    box-shadow: none;
    resize: none;

    &:focus {
      border: none;
      box-shadow: none;
    }
  }
}

.footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-top: 1px solid #f0f0f0;

  .footer-left {
    .char-count {
      font-size: 12px;
      color: #00000073;
    }
  }

  .footer-right {
    display: flex;
    gap: 8px;
  }
}
</style>
