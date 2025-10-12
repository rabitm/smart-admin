<!--
  @提及选择器组件

  功能:
  - 成员列表展示
  - 搜索过滤
  - 键盘导航 (上下键 + Enter)
  - @所有人选项

  @Author Claude Code Assistant
  @Date 2025-10-11
  @Copyright 1024创新实验室
-->

<template>
  <div v-if="visible" class="mention-selector" :style="{ top: position.top + 'px', left: position.left + 'px' }">
    <!-- 搜索框 -->
    <div class="mention-search">
      <a-input
        ref="searchInputRef"
        v-model:value="searchKeyword"
        size="small"
        placeholder="搜索成员..."
        :bordered="false"
        @keydown="handleKeydown"
      />
    </div>

    <!-- 成员列表 -->
    <div class="mention-list">
      <!-- @所有人 选项 -->
      <div
        v-if="showMentionAll && filteredMembers.length > 0"
        :class="['mention-item', { active: selectedIndex === -1 }]"
        @click="selectMember({ userId: 'all', nickname: '所有人', isAll: true })"
        @mouseenter="selectedIndex = -1"
      >
        <a-avatar size="small" style="background-color: #ff4d4f">
          <template #icon><TeamOutlined /></template>
        </a-avatar>
        <span class="mention-name">所有人</span>
      </div>

      <!-- 成员列表 -->
      <div
        v-for="(member, index) in filteredMembers"
        :key="member.userId"
        :class="['mention-item', { active: selectedIndex === index }]"
        @click="selectMember(member)"
        @mouseenter="selectedIndex = index"
      >
        <a-avatar size="small" :style="{ backgroundColor: getAvatarColor(member.nickname) }">
          {{ member.nickname?.charAt(0) || '?' }}
        </a-avatar>
        <span class="mention-name">{{ member.nickname }}</span>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredMembers.length === 0" class="mention-empty">
        <a-empty description="未找到成员" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue';
import { TeamOutlined } from '@ant-design/icons-vue';
import { Empty } from 'ant-design-vue';

// 定义成员接口
interface Member {
  userId: string;
  nickname: string;
  isAll?: boolean; // 是否为"所有人"
}

// 定义组件属性
const props = defineProps<{
  visible: boolean;
  members: Member[];
  position: { top: number; left: number };
  showMentionAll?: boolean; // 是否显示 @所有人 选项
}>();

// 定义组件事件
const emit = defineEmits<{
  select: [member: Member];
  close: [];
}>();

// 状态定义
const searchKeyword = ref('');
const selectedIndex = ref(-1); // -1 表示选中"所有人"
const searchInputRef = ref();

// 过滤后的成员列表
const filteredMembers = computed(() => {
  if (!searchKeyword.value.trim()) {
    return props.members;
  }

  const keyword = searchKeyword.value.toLowerCase();
  return props.members.filter((member) =>
    member.nickname.toLowerCase().includes(keyword)
  );
});

// 监听可见性变化,自动聚焦搜索框
watch(
  () => props.visible,
  async (newVisible) => {
    if (newVisible) {
      searchKeyword.value = '';
      selectedIndex.value = props.showMentionAll ? -1 : 0;

      await nextTick();
      if (searchInputRef.value) {
        searchInputRef.value.focus();
      }
    }
  }
);

// 键盘导航
function handleKeydown(event: KeyboardEvent) {
  const totalItems = filteredMembers.value.length + (props.showMentionAll ? 1 : 0);

  if (event.key === 'ArrowDown') {
    // 向下
    event.preventDefault();
    if (props.showMentionAll) {
      selectedIndex.value = (selectedIndex.value + 1) % totalItems - (totalItems > 0 ? 1 : 0);
    } else {
      selectedIndex.value = Math.min(selectedIndex.value + 1, filteredMembers.value.length - 1);
    }
  } else if (event.key === 'ArrowUp') {
    // 向上
    event.preventDefault();
    if (props.showMentionAll) {
      selectedIndex.value = selectedIndex.value <= -1 ? filteredMembers.value.length - 1 : selectedIndex.value - 1;
    } else {
      selectedIndex.value = Math.max(selectedIndex.value - 1, 0);
    }
  } else if (event.key === 'Enter') {
    // 选择
    event.preventDefault();
    if (selectedIndex.value === -1 && props.showMentionAll) {
      selectMember({ userId: 'all', nickname: '所有人', isAll: true });
    } else if (selectedIndex.value >= 0 && selectedIndex.value < filteredMembers.value.length) {
      selectMember(filteredMembers.value[selectedIndex.value]);
    }
  } else if (event.key === 'Escape') {
    // 取消
    event.preventDefault();
    emit('close');
  }
}

// 选择成员
function selectMember(member: Member) {
  console.log('📌 [@提及] 选中成员:', member);
  emit('select', member);
}

// 生成头像颜色
function getAvatarColor(name: string): string {
  const colors = [
    '#1890ff',
    '#52c41a',
    '#fa8c16',
    '#eb2f96',
    '#722ed1',
    '#13c2c2',
    '#faad14',
  ];
  const hash = name.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
  return colors[hash % colors.length];
}
</script>

<style scoped lang="less">
.mention-selector {
  position: absolute;
  width: 280px;
  max-height: 320px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  z-index: 1000;

  .mention-search {
    padding: 8px;
    border-bottom: 1px solid #f0f0f0;

    :deep(.ant-input) {
      border: none;
      box-shadow: none;

      &:focus {
        box-shadow: none;
      }
    }
  }

  .mention-list {
    max-height: 280px;
    overflow-y: auto;
    padding: 4px 0;

    .mention-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      cursor: pointer;
      transition: all 0.2s;

      &:hover,
      &.active {
        background-color: #f5f5f5;
      }

      .mention-name {
        flex: 1;
        font-size: 14px;
        color: #333;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .mention-empty {
      padding: 20px;
      text-align: center;
    }
  }

  // 滚动条样式
  .mention-list::-webkit-scrollbar {
    width: 6px;
  }

  .mention-list::-webkit-scrollbar-thumb {
    background-color: rgba(0, 0, 0, 0.2);
    border-radius: 3px;
  }

  .mention-list::-webkit-scrollbar-thumb:hover {
    background-color: rgba(0, 0, 0, 0.3);
  }
}
</style>
