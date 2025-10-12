<!--
  群成员面板组件
  参考飞书/钉钉风格设计

  功能:
  - 显示群成员列表
  - 显示在线状态
  - 区分群主/管理员/普通成员
  - 成员操作菜单
  - 邀请成员功能

  @Author Claude Code Assistant
  @Date 2025-10-10
-->
<template>
  <div class="group-member-panel">
    <!-- 头部 -->
    <div class="panel-header">
      <div class="header-title">
        <TeamOutlined />
        <span>群成员 ({{ memberCount }})</span>
      </div>
      <a-button type="primary" size="small" @click="handleInviteClick">
        <template #icon><UserAddOutlined /></template>
        邀请
      </a-button>
    </div>

    <!-- 搜索框 -->
    <div class="panel-search">
      <a-input-search
        v-model:value="searchKeyword"
        placeholder="搜索成员"
        size="small"
        @search="handleSearch"
      />
    </div>

    <!-- 成员列表 -->
    <div class="member-list">
      <!-- 空状态 -->
      <a-empty v-if="filteredMembers.length === 0" description="暂无成员" />

      <!-- 成员项 -->
      <div
        v-for="member in filteredMembers"
        :key="member.userID"
        class="member-item"
        @click="handleMemberClick(member)"
      >
        <!-- 头像 -->
        <div class="member-avatar-wrapper">
          <a-avatar :src="member.faceURL" :size="40">
            {{ member.nickname?.[0] || '?' }}
          </a-avatar>
          <!-- 在线状态指示器 -->
          <span
            v-if="member.isOnline"
            class="online-indicator"
            title="在线"
          ></span>
        </div>

        <!-- 成员信息 -->
        <div class="member-info">
          <div class="member-name">
            <span>{{ member.nickname }}</span>
            <!-- 角色标签 -->
            <a-tag v-if="member.roleLevel === 1" color="red" size="small">群主</a-tag>
            <a-tag v-if="member.roleLevel === 2" color="blue" size="small">管理员</a-tag>
          </div>
          <div class="member-status">
            <span class="status-text" :class="{ online: member.isOnline }">
              {{ member.isOnline ? '在线' : '离线' }}
            </span>
            <span v-if="member.joinTime" class="join-time">
              {{ formatJoinTime(member.joinTime) }}加入
            </span>
          </div>
        </div>

        <!-- 操作菜单 (仅群主/管理员可见，且不能操作自己) -->
        <a-dropdown
          v-if="canManageMember(member)"
          :trigger="['click']"
          placement="bottomRight"
          @click.stop
        >
          <a-button type="text" size="small" class="member-action-btn">
            <MoreOutlined />
          </a-button>
          <template #overlay>
            <a-menu @click="handleMenuClick($event, member)">
              <!-- 设为管理员/取消管理员 -->
              <a-menu-item
                v-if="isGroupOwner && member.roleLevel === 3"
                key="setAdmin"
              >
                <SafetyOutlined />
                设为管理员
              </a-menu-item>
              <a-menu-item
                v-if="isGroupOwner && member.roleLevel === 2"
                key="removeAdmin"
              >
                <StopOutlined />
                取消管理员
              </a-menu-item>
              <!-- 移出群聊 -->
              <a-menu-item key="remove" danger>
                <UserDeleteOutlined />
                移出群聊
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </div>

    <!-- 邀请成员Modal -->
    <InviteMemberModal
      ref="inviteMemberModalRef"
      :group-id="groupId"
      :report-id="reportId"
      :existing-member-ids="members.map((m) => m.userID)"
      @success="handleInviteSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { message as antMessage } from 'ant-design-vue';
import {
  TeamOutlined,
  UserAddOutlined,
  MoreOutlined,
  SafetyOutlined,
  StopOutlined,
  UserDeleteOutlined,
} from '@ant-design/icons-vue';
import { openIMClient } from '/@/utils/openim-client';
import type { GroupMemberItem } from '@openim/wasm-client-sdk';
import { smartSentry } from '/@/lib/smart-sentry';
import InviteMemberModal from './InviteMemberModal.vue';

// 扩展的群成员显示类型
interface GroupMemberDisplay extends GroupMemberItem {
  isOnline?: boolean;
  employeeId?: number;
  roleLabel?: string;
}

// Props
const props = defineProps<{
  groupId: string;
  reportId: number;
}>();

// Emits
const emit = defineEmits<{
  invite: [];
  memberRemoved: [member: GroupMemberDisplay];
  memberUpdated: [member: GroupMemberDisplay];
}>();

// 状态
const loading = ref(false);
const members = ref<GroupMemberDisplay[]>([]);
const searchKeyword = ref('');
const currentUserId = ref('');
const currentUserRole = ref(3); // 1-群主, 2-管理员, 3-普通成员

// 计算属性
const memberCount = computed(() => members.value.length);

const filteredMembers = computed(() => {
  if (!searchKeyword.value) {
    return members.value;
  }

  const keyword = searchKeyword.value.toLowerCase();
  return members.value.filter(
    (member) =>
      member.nickname?.toLowerCase().includes(keyword) ||
      member.userID?.toLowerCase().includes(keyword)
  );
});

const isGroupOwner = computed(() => currentUserRole.value === 1);
const isGroupAdmin = computed(() => currentUserRole.value === 2);

// 是否可以管理成员
function canManageMember(member: GroupMemberDisplay): boolean {
  // 不能操作自己
  if (member.userID === currentUserId.value) {
    return false;
  }

  // 群主可以管理所有人
  if (isGroupOwner.value) {
    return true;
  }

  // 管理员只能管理普通成员
  if (isGroupAdmin.value && member.roleLevel === 3) {
    return true;
  }

  return false;
}

// 加载群成员列表
async function loadGroupMembers() {
  loading.value = true;

  try {
    console.log('📋 [群成员面板] 开始加载群成员列表, groupId:', props.groupId);

    // 1. 获取群成员列表
    const memberList = await openIMClient.getGroupMembers(props.groupId);

    console.log('✅ [群成员面板] 获取到群成员:', memberList.length);

    // 2. 获取在线状态
    const userIds = memberList.map((m) => m.userID);
    const onlineStatusMap = await openIMClient.getUsersOnlineStatus(userIds);

    console.log('✅ [群成员面板] 获取到在线状态');

    // 3. 合并数据
    members.value = memberList.map((member) => ({
      ...member,
      isOnline: onlineStatusMap.get(member.userID) || false,
    }));

    // 4. 获取当前用户信息
    currentUserId.value = openIMClient.userId;
    const currentMember = members.value.find((m) => m.userID === currentUserId.value);
    if (currentMember) {
      currentUserRole.value = currentMember.roleLevel || 3;
      console.log('👤 [群成员面板] 当前用户角色:', currentUserRole.value);
    }

    console.log('✅ [群成员面板] 群成员列表加载完成, 总数:', members.value.length);
  } catch (error) {
    console.error('❌ [群成员面板] 加载群成员失败:', error);
    smartSentry.captureError(error, {
      tags: { module: 'GroupMemberPanel', action: 'loadGroupMembers' },
      extra: { groupId: props.groupId },
    });
    antMessage.error('加载群成员失败: ' + (error as Error).message);
  } finally {
    loading.value = false;
  }
}

// 搜索成员
function handleSearch() {
  console.log('🔍 [群成员面板] 搜索成员:', searchKeyword.value);
}

// 点击成员
function handleMemberClick(member: GroupMemberDisplay) {
  console.log('👤 [群成员面板] 点击成员:', member.nickname);
  // TODO: 显示成员详情或发起单聊
}

// 邀请成员Modal引用
const inviteMemberModalRef = ref();

// 邀请成员
function handleInviteClick() {
  console.log('➕ [群成员面板] 点击邀请按钮');

  // 打开邀请Modal
  if (inviteMemberModalRef.value) {
    inviteMemberModalRef.value.open();
  }
}

// 邀请成功回调
function handleInviteSuccess(userIDs: string[]) {
  console.log('✅ [群成员面板] 邀请成功, 用户数:', userIDs.length);

  // 重新加载群成员列表
  loadGroupMembers();

  // 触发事件通知父组件
  emit('invite');
}

// 处理菜单点击
async function handleMenuClick(event: { key: string }, member: GroupMemberDisplay) {
  console.log('📋 [群成员面板] 菜单操作:', event.key, member.nickname);

  switch (event.key) {
    case 'setAdmin':
      await setAsAdmin(member);
      break;
    case 'removeAdmin':
      await removeAdmin(member);
      break;
    case 'remove':
      await removeMember(member);
      break;
  }
}

// 设为管理员
async function setAsAdmin(member: GroupMemberDisplay) {
  // TODO: 调用 OpenIM SDK 设置管理员
  antMessage.info('设置管理员功能开发中');
  console.log('🔧 [群成员面板] 设置管理员:', member.nickname);
}

// 取消管理员
async function removeAdmin(member: GroupMemberDisplay) {
  // TODO: 调用 OpenIM SDK 取消管理员
  antMessage.info('取消管理员功能开发中');
  console.log('🔧 [群成员面板] 取消管理员:', member.nickname);
}

// 移出群聊
async function removeMember(member: GroupMemberDisplay) {
  try {
    // 确认对话框
    const confirmed = await new Promise<boolean>((resolve) => {
      antMessage.confirm({
        title: '移出群聊',
        content: `确定要将 ${member.nickname} 移出群聊吗？`,
        onOk: () => resolve(true),
        onCancel: () => resolve(false),
      });
    });

    if (!confirmed) {
      return;
    }

    console.log('🚫 [群成员面板] 移出成员:', member.nickname);

    // 调用 OpenIM SDK 移除成员
    await openIMClient.removeGroupMembers(props.groupId, [member.userID], '违反群规');

    antMessage.success(`已将 ${member.nickname} 移出群聊`);

    // 从列表中移除
    members.value = members.value.filter((m) => m.userID !== member.userID);

    // 触发事件
    emit('memberRemoved', member);
  } catch (error) {
    console.error('❌ [群成员面板] 移除成员失败:', error);
    antMessage.error('移除成员失败: ' + (error as Error).message);
  }
}

// 格式化加入时间
function formatJoinTime(timestamp: number): string {
  if (!timestamp || timestamp <= 0) {
    return '';
  }

  const now = Date.now();

  // 判断时间戳是秒还是毫秒(13位数是毫秒,10位数是秒)
  const joinTime = timestamp.toString().length === 10 ? timestamp * 1000 : timestamp;

  const diff = now - joinTime;

  // 如果时间差为负数,说明时间戳有问题
  if (diff < 0) {
    console.warn('⚠️ [群成员面板] 无效的加入时间:', timestamp, '当前时间:', now);
    return '';
  }

  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;
  const month = 30 * day;

  if (diff < minute) {
    return '刚刚';
  } else if (diff < hour) {
    return Math.floor(diff / minute) + '分钟前';
  } else if (diff < day) {
    return Math.floor(diff / hour) + '小时前';
  } else if (diff < month) {
    return Math.floor(diff / day) + '天前';
  } else {
    const date = new Date(joinTime);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
}

// 监听群组变化
function setupGroupListeners() {
  openIMClient.onGroupChanged((event) => {
    console.log('👥 [群成员面板] 群组事件:', event.type);

    if (event.type === 'member_added') {
      // 成员加入
      console.log('➕ [群成员面板] 成员加入');
      loadGroupMembers(); // 重新加载成员列表
    } else if (event.type === 'member_deleted') {
      // 成员退出
      console.log('➖ [群成员面板] 成员退出');
      loadGroupMembers();
    } else if (event.type === 'info_changed') {
      // 群信息变更
      console.log('ℹ️ [群成员面板] 群信息变更');
      loadGroupMembers();
    }
  });
}

// 组件挂载
onMounted(async () => {
  await loadGroupMembers();
  setupGroupListeners();
});

// 监听 groupId 变化
watch(
  () => props.groupId,
  async (newGroupId) => {
    if (newGroupId) {
      await loadGroupMembers();
    }
  }
);

// 暴露方法供父组件调用
defineExpose({
  loadGroupMembers,
});
</script>

<style scoped lang="less">
.group-member-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fafafa;
  border-left: 1px solid #e8e8e8;

  // 头部
  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px;
    background: #fff;
    border-bottom: 1px solid #e8e8e8;

    .header-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 500;
      color: #262626;

      .anticon {
        font-size: 18px;
      }
    }
  }

  // 搜索框
  .panel-search {
    padding: 12px 16px;
    background: #fff;
    border-bottom: 1px solid #e8e8e8;
  }

  // 成员列表
  .member-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px 0;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      background: #d9d9d9;
      border-radius: 3px;
    }

    &::-webkit-scrollbar-thumb:hover {
      background: #bfbfbf;
    }

    // 成员项
    .member-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
      cursor: pointer;
      transition: all 0.2s;
      background: #fff;
      margin-bottom: 1px;

      &:hover {
        background: #f5f5f5;

        .member-action-btn {
          opacity: 1;
        }
      }

      // 头像容器
      .member-avatar-wrapper {
        position: relative;
        flex-shrink: 0;

        // 在线状态指示器
        .online-indicator {
          position: absolute;
          bottom: 2px;
          right: 2px;
          width: 10px;
          height: 10px;
          background: #52c41a;
          border: 2px solid #fff;
          border-radius: 50%;
        }
      }

      // 成员信息
      .member-info {
        flex: 1;
        min-width: 0;

        .member-name {
          display: flex;
          align-items: center;
          gap: 6px;
          margin-bottom: 4px;

          span {
            font-size: 14px;
            font-weight: 500;
            color: #262626;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        .member-status {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 12px;
          color: #8c8c8c;

          .status-text {
            &.online {
              color: #52c41a;
            }
          }

          .join-time {
            color: #bfbfbf;
          }
        }
      }

      // 操作按钮
      .member-action-btn {
        opacity: 0;
        transition: opacity 0.2s;
        color: #8c8c8c;

        &:hover {
          color: #262626;
        }
      }
    }
  }
}
</style>
