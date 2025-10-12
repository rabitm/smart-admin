<!--
  现代化消息编辑器 - 类似钉钉/飞书风格
  支持富文本、Markdown、图片、视频、文件、@ 提及等
  @Author Claude Code Assistant
  @Date 2025-10-11
  @Copyright 1024创新实验室
-->

<template>
  <div class="modern-message-editor">
    <!-- 工具栏 -->
    <div class="editor-toolbar">
      <a-space :size="4">
        <!-- 表情 -->
        <a-tooltip title="表情">
          <a-button
            type="text"
            size="small"
            :class="{ active: showEmojiPicker }"
            @click="toggleEmojiPicker"
          >
            <SmileOutlined />
          </a-button>
        </a-tooltip>

        <!-- @ 提及 -->
        <a-tooltip title="@ 提及成员 (点击测试)">
          <a-button
            type="text"
            size="small"
            :class="{ active: showMentionSelector }"
            @click="insertMention"
          >
            <span class="at-icon">@</span>
          </a-button>
        </a-tooltip>

        <a-divider type="vertical" style="margin: 0 4px" />

        <!-- 文本格式 -->
        <a-tooltip title="加粗">
          <a-button
            type="text"
            size="small"
            :class="{ active: formats.bold }"
            @click="toggleFormat('bold')"
          >
            <BoldOutlined />
          </a-button>
        </a-tooltip>

        <a-tooltip title="斜体">
          <a-button
            type="text"
            size="small"
            :class="{ active: formats.italic }"
            @click="toggleFormat('italic')"
          >
            <ItalicOutlined />
          </a-button>
        </a-tooltip>

        <a-tooltip title="删除线">
          <a-button
            type="text"
            size="small"
            :class="{ active: formats.strikethrough }"
            @click="toggleFormat('strikethrough')"
          >
            <StrikethroughOutlined />
          </a-button>
        </a-tooltip>

        <a-tooltip title="代码">
          <a-button
            type="text"
            size="small"
            :class="{ active: formats.code }"
            @click="toggleFormat('code')"
          >
            <CodeOutlined />
          </a-button>
        </a-tooltip>

        <a-divider type="vertical" style="margin: 0 4px" />

        <!-- 图片 -->
        <a-tooltip title="图片">
          <a-button
            type="text"
            size="small"
            @click="uploadImage"
          >
            <PictureOutlined />
          </a-button>
        </a-tooltip>

        <!-- 视频 -->
        <a-tooltip title="视频">
          <a-button
            type="text"
            size="small"
            @click="uploadVideo"
          >
            <VideoCameraOutlined />
          </a-button>
        </a-tooltip>

        <!-- 文件 -->
        <a-tooltip title="文件">
          <a-button
            type="text"
            size="small"
            @click="uploadFile"
          >
            <PaperClipOutlined />
          </a-button>
        </a-tooltip>

        <a-divider type="vertical" style="margin: 0 4px" />

        <!-- Markdown 开关 -->
        <a-tooltip :title="markdownMode ? 'Markdown 模式' : '切换到 Markdown'">
          <a-button
            type="text"
            size="small"
            :class="{ active: markdownMode }"
            @click="toggleMarkdownMode"
          >
            <span class="markdown-icon">M↓</span>
          </a-button>
        </a-tooltip>

        <!-- 更多选项 -->
        <a-dropdown :trigger="['click']">
          <a-button type="text" size="small">
            <EllipsisOutlined />
          </a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item key="quote" @click="insertQuote">
                <BlockOutlined />
                <span>引用</span>
              </a-menu-item>
              <a-menu-item key="list" @click="insertList">
                <UnorderedListOutlined />
                <span>列表</span>
              </a-menu-item>
              <a-menu-item key="clear" @click="clearFormat">
                <ClearOutlined />
                <span>清除格式</span>
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-space>

      <!-- 发送按钮和快捷键提示 -->
      <div class="editor-actions">
        <span class="shortcut-hint">Enter 发送 / Shift+Enter 换行</span>
        <a-button
          type="primary"
          size="small"
          :loading="sending"
          :disabled="!canSend"
          @click="handleSend"
        >
          <SendOutlined />
          发送
        </a-button>
      </div>
    </div>

    <!-- 编辑区域 -->
    <div class="editor-content" :class="{ 'markdown-mode': markdownMode }">
      <!-- 富文本模式 -->
      <div v-show="!markdownMode" ref="richEditorRef" class="rich-editor"></div>

      <!-- Markdown 模式 -->
      <div v-show="markdownMode" class="markdown-editor">
        <a-textarea
          ref="markdownTextRef"
          v-model:value="markdownContent"
          placeholder="支持 Markdown 语法..."
          :auto-size="{ minRows: 3, maxRows: 10 }"
          @keydown="handleKeyDown"
        />
        <!-- Markdown 预览 -->
        <div v-if="markdownContent" class="markdown-preview">
          <a-divider orientation="left" plain>
            <EyeOutlined />
            预览
          </a-divider>
          <div class="preview-content" v-html="renderedMarkdown"></div>
        </div>
      </div>

      <!-- @ 提及选择器 -->
      <MentionSelector
        v-if="showMentionSelector"
        :visible="showMentionSelector"
        :members="members"
        :position="mentionSelectorPosition"
        :show-mention-all="true"
        @select="handleMentionSelect"
        @close="showMentionSelector = false"
      />

      <!-- 表情选择器 -->
      <div v-show="showEmojiPicker" class="emoji-picker">
        <div class="emoji-grid">
          <div
            v-for="emoji in commonEmojis"
            :key="emoji"
            class="emoji-item"
            @click="insertEmoji(emoji)"
          >
            {{ emoji }}
          </div>
        </div>
      </div>

      <!-- 文件上传预览 -->
      <div v-if="uploadingFiles.length > 0" class="uploading-files">
        <div v-for="(file, index) in uploadingFiles" :key="index" class="file-item">
          <div class="file-info">
            <FileTextOutlined v-if="file.type === 'file'" />
            <PictureOutlined v-else-if="file.type === 'image'" />
            <VideoCameraOutlined v-else-if="file.type === 'video'" />
            <span class="file-name">{{ file.name }}</span>
          </div>
          <a-progress
            v-if="file.uploading"
            :percent="file.progress"
            size="small"
            :show-info="false"
          />
          <CloseCircleOutlined class="file-remove" @click="removeUploadingFile(index)" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import {
  SmileOutlined,
  BoldOutlined,
  ItalicOutlined,
  StrikethroughOutlined,
  CodeOutlined,
  PictureOutlined,
  VideoCameraOutlined,
  PaperClipOutlined,
  EllipsisOutlined,
  SendOutlined,
  BlockOutlined,
  UnorderedListOutlined,
  ClearOutlined,
  EyeOutlined,
  FileTextOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons-vue';
import { message as antMessage } from 'ant-design-vue';
import MentionSelector from './MentionSelector.vue';

// ==================== Props & Emits ====================

const props = withDefaults(defineProps<{
  members?: Array<{ userId: string; nickname: string }>;
  sending?: boolean;
  placeholder?: string;
}>(), {
  members: () => [],
  sending: false,
  placeholder: '输入消息...',
});

const emit = defineEmits<{
  send: [content: string, type: 'text' | 'markdown' | 'rich', metadata?: any];
  uploadImage: [file: File];
  uploadVideo: [file: File];
  uploadFile: [file: File];
  mention: [userIds: string[]];
}>();

// ==================== State ====================

// 编辑器状态
const richEditorRef = ref<HTMLDivElement>();
const markdownTextRef = ref();
const markdownMode = ref(false);
const markdownContent = ref('');

// 格式状态
const formats = ref({
  bold: false,
  italic: false,
  strikethrough: false,
  code: false,
});

// @ 提及
const showMentionSelector = ref(false);
const mentionSelectorPosition = ref({ top: 0, left: 0 });
const selectedMemberIds = ref<string[]>([]);

// 表情
const showEmojiPicker = ref(false);
const commonEmojis = ['😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '🙃', '😉', '😊', '😇',
  '🥰', '😍', '🤩', '😘', '😗', '😚', '😙', '😋', '😛', '😜', '🤪', '😝', '🤑', '🤗',
  '🤭', '🤫', '🤔', '🤐', '🤨', '😐', '😑', '😶', '😏', '😒', '🙄', '😬', '🤥', '😌',
  '😔', '😪', '🤤', '😴', '😷', '🤒', '🤕', '🤢', '🤮', '🤧', '🥵', '🥶', '😎', '🤓',
  '👍', '👎', '👏', '🙌', '👌', '✌️', '🤞', '🤝', '🙏', '💪', '🎉', '🎊', '❤️', '💔'];

// 文件上传
interface UploadingFile {
  name: string;
  type: 'image' | 'video' | 'file';
  uploading: boolean;
  progress: number;
  file: File;
}
const uploadingFiles = ref<UploadingFile[]>([]);

// ==================== Computed ====================

const canSend = computed(() => {
  if (markdownMode.value) {
    return markdownContent.value.trim().length > 0;
  }
  // 富文本模式下检查内容
  return richEditorRef.value && richEditorRef.value.textContent!.trim().length > 0;
});

// Markdown 渲染（简单实现，实际可以使用 marked 库）
const renderedMarkdown = computed(() => {
  if (!markdownContent.value) return '';

  let html = markdownContent.value
    // 标题
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    // 加粗
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    // 斜体
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    // 代码
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    // 链接
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>')
    // 换行
    .replace(/\n/g, '<br>');

  return html;
});

// ==================== Methods ====================

/**
 * 初始化富文本编辑器
 */
function initRichEditor() {
  if (!richEditorRef.value) return;

  // 设置可编辑
  richEditorRef.value.contentEditable = 'true';
  richEditorRef.value.innerHTML = `<p>${props.placeholder}</p>`;

  // 监听输入事件
  richEditorRef.value.addEventListener('input', handleRichEditorInput);
  richEditorRef.value.addEventListener('keydown', handleRichEditorKeyDown);
  richEditorRef.value.addEventListener('keyup', handleRichEditorKeyUp);  // 🔧 新增: 监听 keyup 用于 @ 检测
  richEditorRef.value.addEventListener('focus', handleRichEditorFocus);
  richEditorRef.value.addEventListener('blur', handleRichEditorBlur);
  richEditorRef.value.addEventListener('paste', handlePaste);
}

/**
 * 富文本编辑器输入事件
 */
function handleRichEditorInput(e: Event) {
  const target = e.target as HTMLDivElement;
  const text = target.textContent || '';

  console.log('🔍 [编辑器] 输入事件触发, 内容:', text);

  // 如果内容为空，显示占位符
  if (text.trim() === '' || text.trim() === props.placeholder) {
    target.innerHTML = `<p>${props.placeholder}</p>`;
  }
}

/**
 * 富文本编辑器 keyup 事件 - 用于检测 @ 输入
 * 🔧 修复: 检测实际输入的字符，而非按键组合
 */
function handleRichEditorKeyUp(e: KeyboardEvent) {
  // 获取编辑器当前内容
  if (!richEditorRef.value) return;

  const text = richEditorRef.value.textContent || '';
  const lastChar = text.charAt(text.length - 1);

  console.log('⌨️ [编辑器] KeyUp 事件, key:', e.key, '最后字符:', lastChar, '字符码:', lastChar.charCodeAt(0));

  // 检测 @ 字符（检查最后输入的字符是否为 @）
  if (lastChar === '@' || lastChar === '＠') {
    console.log('✅ [编辑器] 检测到 @ 字符，显示成员选择器');
    console.log('✅ [编辑器] 群成员数量:', props.members?.length || 0);
    console.log('✅ [编辑器] MentionSelector 组件是否已注册:', !!MentionSelector);

    showMentionSelector.value = true;

    // 延迟更新位置，确保DOM已更新
    setTimeout(() => {
      updateMentionSelectorPosition();
      console.log('✅ [编辑器] @ 选择器位置:', mentionSelectorPosition.value);
      console.log('✅ [编辑器] @ 选择器显示状态:', showMentionSelector.value);
    }, 50);
  }
}

/**
 * 处理粘贴事件
 */
function handlePaste(e: ClipboardEvent) {
  e.preventDefault();
  console.log('📋 [编辑器] 处理粘贴事件');

  const clipboardData = e.clipboardData;
  if (!clipboardData) return;

  // 处理图片粘贴
  const items = clipboardData.items;
  let hasImage = false;

  for (let i = 0; i < items.length; i++) {
    const item = items[i];

    // 检查是否为图片
    if (item.type.indexOf('image') !== -1) {
      hasImage = true;
      const file = item.getAsFile();
      if (file) {
        console.log('🖼️ [编辑器] 检测到粘贴的图片:', file.name);
        handleFileUpload(file, 'image');
        antMessage.success(`正在上传图片: ${file.name}`);
      }
    }
  }

  // 如果没有图片，处理纯文本粘贴
  if (!hasImage) {
    const text = clipboardData.getData('text/plain');
    if (text) {
      console.log('📝 [编辑器] 粘贴纯文本');
      // 插入纯文本（不包含格式）
      document.execCommand('insertText', false, text);
    }
  }
}

/**
 * 富文本编辑器键盘事件
 */
function handleRichEditorKeyDown(e: KeyboardEvent) {
  // Enter 发送
  if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey) {
    e.preventDefault();
    handleSend();
    return;
  }

  // Shift + Enter 换行
  if (e.key === 'Enter' && e.shiftKey) {
    e.preventDefault();
    document.execCommand('insertLineBreak');
    return;
  }
}

/**
 * 富文本编辑器获得焦点
 */
function handleRichEditorFocus() {
  if (richEditorRef.value?.textContent?.trim() === props.placeholder) {
    richEditorRef.value.innerHTML = '<p><br></p>';
  }
}

/**
 * 富文本编辑器失去焦点
 */
function handleRichEditorBlur() {
  if (!richEditorRef.value?.textContent?.trim()) {
    richEditorRef.value.innerHTML = `<p>${props.placeholder}</p>`;
  }
}

/**
 * Markdown 键盘事件
 */
function handleKeyDown(e: KeyboardEvent) {
  // Enter 发送
  if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey) {
    e.preventDefault();
    handleSend();
  }
}

/**
 * 切换格式
 */
function toggleFormat(format: keyof typeof formats.value) {
  if (!richEditorRef.value) return;

  const commands: Record<string, string> = {
    bold: 'bold',
    italic: 'italic',
    strikethrough: 'strikeThrough',
    code: 'insertHTML', // 代码需要特殊处理
  };

  if (format === 'code') {
    const selection = window.getSelection();
    if (selection && selection.toString()) {
      document.execCommand('insertHTML', false, `<code>${selection.toString()}</code>`);
    }
  } else {
    document.execCommand(commands[format]);
  }

  formats.value[format] = !formats.value[format];
}

/**
 * 切换 Markdown 模式
 */
function toggleMarkdownMode() {
  markdownMode.value = !markdownMode.value;

  if (markdownMode.value) {
    // 切换到 Markdown 模式，提取富文本内容
    if (richEditorRef.value) {
      markdownContent.value = richEditorRef.value.textContent || '';
    }
  } else {
    // 切换回富文本模式
    if (richEditorRef.value && markdownContent.value) {
      richEditorRef.value.innerHTML = renderedMarkdown.value;
    }
  }
}

/**
 * 插入 @ 提及
 */
function insertMention() {
  console.log('🔧 [TEST] 手动触发 @ 选择器');
  console.log('🔧 [TEST] 当前成员列表:', props.members);
  console.log('🔧 [TEST] 成员数量:', props.members?.length || 0);

  showMentionSelector.value = true;

  setTimeout(() => {
    updateMentionSelectorPosition();
    console.log('🔧 [TEST] showMentionSelector =', showMentionSelector.value);
    console.log('🔧 [TEST] position =', mentionSelectorPosition.value);
  }, 100);
}

/**
 * 更新 @ 选择器位置
 */
function updateMentionSelectorPosition() {
  if (markdownMode.value && markdownTextRef.value) {
    const rect = markdownTextRef.value.$el.getBoundingClientRect();
    mentionSelectorPosition.value = {
      top: rect.bottom + window.scrollY + 5,
      left: rect.left + window.scrollX,
    };
  } else if (richEditorRef.value) {
    const rect = richEditorRef.value.getBoundingClientRect();
    mentionSelectorPosition.value = {
      top: rect.bottom + window.scrollY + 5,
      left: rect.left + window.scrollX,
    };
  }
}

/**
 * 处理 @ 成员选择
 */
function handleMentionSelect(member: any) {
  console.log('选中成员:', member);

  // 添加到选中列表
  if (member.isAll) {
    selectedMemberIds.value.push('all');
  } else {
    selectedMemberIds.value.push(member.userId);
  }

  // 插入 @ 文本
  const mentionText = `@${member.nickname} `;

  if (markdownMode.value) {
    // Markdown 模式
    markdownContent.value += mentionText;
  } else {
    // 富文本模式
    if (richEditorRef.value) {
      document.execCommand('insertHTML', false, `<span class="mention">@${member.nickname}</span>&nbsp;`);
    }
  }

  showMentionSelector.value = false;
}

/**
 * 切换表情选择器
 */
function toggleEmojiPicker() {
  showEmojiPicker.value = !showEmojiPicker.value;
}

/**
 * 插入表情
 */
function insertEmoji(emoji: string) {
  if (markdownMode.value) {
    markdownContent.value += emoji;
  } else if (richEditorRef.value) {
    document.execCommand('insertHTML', false, emoji);
  }
  showEmojiPicker.value = false;
}

/**
 * 插入引用
 */
function insertQuote() {
  if (markdownMode.value) {
    markdownContent.value += '\n> ';
  } else if (richEditorRef.value) {
    document.execCommand('formatBlock', false, 'blockquote');
  }
}

/**
 * 插入列表
 */
function insertList() {
  if (markdownMode.value) {
    markdownContent.value += '\n- ';
  } else if (richEditorRef.value) {
    document.execCommand('insertUnorderedList');
  }
}

/**
 * 清除格式
 */
function clearFormat() {
  if (richEditorRef.value) {
    document.execCommand('removeFormat');
  }
}

/**
 * 上传图片
 */
function uploadImage() {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*';
  input.multiple = true;
  input.onchange = (e: Event) => {
    const files = (e.target as HTMLInputElement).files;
    if (files) {
      Array.from(files).forEach(file => {
        handleFileUpload(file, 'image');
      });
    }
  };
  input.click();
}

/**
 * 上传视频
 */
function uploadVideo() {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'video/*';
  input.onchange = (e: Event) => {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (file) {
      handleFileUpload(file, 'video');
    }
  };
  input.click();
}

/**
 * 上传文件
 */
function uploadFile() {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = '*/*';
  input.multiple = true;
  input.onchange = (e: Event) => {
    const files = (e.target as HTMLInputElement).files;
    if (files) {
      Array.from(files).forEach(file => {
        handleFileUpload(file, 'file');
      });
    }
  };
  input.click();
}

/**
 * 处理文件上传
 */
function handleFileUpload(file: File, type: 'image' | 'video' | 'file') {
  // 添加到上传列表
  const uploadingFile: UploadingFile = {
    name: file.name,
    type,
    uploading: true,
    progress: 0,
    file,
  };

  uploadingFiles.value.push(uploadingFile);

  // 模拟上传进度
  const interval = setInterval(() => {
    uploadingFile.progress += 10;
    if (uploadingFile.progress >= 100) {
      uploadingFile.uploading = false;
      clearInterval(interval);

      // 触发上传事件
      if (type === 'image') {
        emit('uploadImage', file);
      } else if (type === 'video') {
        emit('uploadVideo', file);
      } else {
        emit('uploadFile', file);
      }

      // 上传完成后移除
      setTimeout(() => {
        const index = uploadingFiles.value.indexOf(uploadingFile);
        if (index > -1) {
          uploadingFiles.value.splice(index, 1);
        }
      }, 500);
    }
  }, 100);
}

/**
 * 移除上传中的文件
 */
function removeUploadingFile(index: number) {
  uploadingFiles.value.splice(index, 1);
}

/**
 * 发送消息
 */
function handleSend() {
  if (!canSend.value) return;

  let content = '';
  let type: 'text' | 'markdown' | 'rich' = 'text';
  const metadata: any = {};

  if (markdownMode.value) {
    // Markdown 模式
    content = markdownContent.value;
    type = 'markdown';
    markdownContent.value = '';
  } else {
    // 富文本模式
    if (richEditorRef.value) {
      content = richEditorRef.value.textContent || '';
      type = 'rich';
      // 保存HTML用于渲染
      metadata.html = richEditorRef.value.innerHTML;
      richEditorRef.value.innerHTML = `<p>${props.placeholder}</p>`;
    }
  }

  // 添加 @ 信息
  if (selectedMemberIds.value.length > 0) {
    metadata.mentionIds = [...selectedMemberIds.value];
    emit('mention', selectedMemberIds.value);
    selectedMemberIds.value = [];
  }

  emit('send', content, type, metadata);
}

/**
 * 重置编辑器
 */
function reset() {
  markdownContent.value = '';
  selectedMemberIds.value = [];
  uploadingFiles.value = [];
  showEmojiPicker.value = false;
  showMentionSelector.value = false;

  if (richEditorRef.value) {
    richEditorRef.value.innerHTML = `<p>${props.placeholder}</p>`;
  }
}

// ==================== Lifecycle ====================

onMounted(() => {
  initRichEditor();
});

onUnmounted(() => {
  if (richEditorRef.value) {
    richEditorRef.value.removeEventListener('input', handleRichEditorInput);
    richEditorRef.value.removeEventListener('keydown', handleRichEditorKeyDown);
    richEditorRef.value.removeEventListener('keyup', handleRichEditorKeyUp);
    richEditorRef.value.removeEventListener('focus', handleRichEditorFocus);
    richEditorRef.value.removeEventListener('blur', handleRichEditorBlur);
    richEditorRef.value.removeEventListener('paste', handlePaste);
  }
});

// 暴露方法供父组件调用
defineExpose({
  reset,
});
</script>

<style scoped lang="less">
.modern-message-editor {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s;

  &:focus-within {
    border-color: #1890ff;
    box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
  }

  .editor-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    border-bottom: 1px solid #f0f0f0;
    background: #fafafa;

    :deep(.ant-btn-text) {
      color: #666;
      font-size: 16px;
      min-width: 32px;
      height: 32px;
      padding: 0 8px;
      border-radius: 4px;

      &:hover {
        background: rgba(0, 0, 0, 0.04);
        color: #1890ff;
      }

      &.active {
        background: #e6f7ff;
        color: #1890ff;
      }
    }

    .at-icon,
    .markdown-icon {
      font-size: 14px;
      font-weight: bold;
    }

    .editor-actions {
      display: flex;
      align-items: center;
      gap: 12px;

      .shortcut-hint {
        font-size: 12px;
        color: #999;
      }
    }
  }

  .editor-content {
    position: relative;
    min-height: 120px;
    max-height: 400px;
    overflow-y: auto;

    .rich-editor {
      padding: 12px 16px;
      min-height: 120px;
      outline: none;
      font-size: 14px;
      line-height: 1.6;
      color: #333;

      &:empty::before {
        content: attr(placeholder);
        color: #bbb;
      }

      :deep(code) {
        background: #f5f5f5;
        padding: 2px 6px;
        border-radius: 3px;
        font-family: 'Courier New', monospace;
        font-size: 13px;
        color: #d63200;
      }

      :deep(blockquote) {
        border-left: 3px solid #1890ff;
        margin: 8px 0;
        padding-left: 12px;
        color: #666;
      }

      :deep(.mention) {
        color: #1890ff;
        background: #e6f7ff;
        padding: 0 4px;
        border-radius: 2px;
        font-weight: 500;
      }
    }

    .markdown-editor {
      padding: 12px 16px;

      :deep(.ant-input) {
        border: none;
        box-shadow: none;
        padding: 0;
        font-family: 'Monaco', 'Courier New', monospace;
        font-size: 13px;

        &:focus {
          box-shadow: none;
        }
      }

      .markdown-preview {
        margin-top: 16px;
        padding-top: 16px;
        border-top: 1px dashed #e8e8e8;

        .preview-content {
          padding: 12px;
          background: #fafafa;
          border-radius: 4px;
          font-size: 14px;
          line-height: 1.6;

          :deep(code) {
            background: #f0f0f0;
            padding: 2px 6px;
            border-radius: 3px;
            font-family: 'Courier New', monospace;
            font-size: 13px;
            color: #d63200;
          }

          :deep(a) {
            color: #1890ff;
            text-decoration: none;

            &:hover {
              text-decoration: underline;
            }
          }
        }
      }
    }

    .emoji-picker {
      position: absolute;
      bottom: 100%;
      left: 12px;
      background: #fff;
      border: 1px solid #e8e8e8;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
      padding: 12px;
      z-index: 1000;

      .emoji-grid {
        display: grid;
        grid-template-columns: repeat(10, 1fr);
        gap: 4px;
        max-width: 400px;
        max-height: 200px;
        overflow-y: auto;

        .emoji-item {
          font-size: 24px;
          cursor: pointer;
          text-align: center;
          padding: 4px;
          border-radius: 4px;
          transition: all 0.2s;

          &:hover {
            background: #f0f0f0;
            transform: scale(1.2);
          }
        }
      }
    }

    .uploading-files {
      padding: 8px 16px;
      border-top: 1px dashed #e8e8e8;

      .file-item {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px;
        background: #fafafa;
        border-radius: 4px;
        margin-bottom: 8px;

        .file-info {
          flex: 1;
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 14px;
          color: #333;

          .file-name {
            flex: 1;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        :deep(.ant-progress) {
          flex: 1;
          max-width: 200px;
        }

        .file-remove {
          cursor: pointer;
          color: #999;
          font-size: 16px;
          transition: all 0.3s;

          &:hover {
            color: #ff4d4f;
          }
        }
      }
    }
  }
}

// 滚动条样式
.editor-content::-webkit-scrollbar,
.emoji-grid::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.editor-content::-webkit-scrollbar-thumb,
.emoji-grid::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;

  &:hover {
    background: rgba(0, 0, 0, 0.3);
  }
}
</style>
