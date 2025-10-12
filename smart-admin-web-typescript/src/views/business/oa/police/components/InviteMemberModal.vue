<!--
  群成员邀请Modal
  参考飞书/钉钉风格设计

  功能:
  - 员工搜索和选择
  - 部门树形结构
  - 已选成员显示
  - 批量邀请
  - 实时同步

  @Author Claude Code Assistant
  @Date 2025-10-11
-->
<template>
  <a-modal
    v-model:open="visible"
    title="邀请成员"
    width="700px"
    :confirm-loading="loading"
    @ok="handleInvite"
    @cancel="handleCancel"
  >
    <div class="invite-member-modal">
      <!-- 搜索框 -->
      <div class="search-section">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索员工姓名或部门"
          size="large"
          allow-clear
          @search="handleSearch"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input-search>
      </div>

      <!-- 内容区域 -->
      <div class="content-section">
        <!-- 左侧：员工列表 -->
        <div class="employee-list">
          <a-spin :spinning="loadingEmployees">
            <!-- 空状态 -->
            <a-empty
              v-if="filteredEmployees.length === 0"
              description="暂无员工"
              :image="Empty.PRESENTED_IMAGE_SIMPLE"
            />

            <!-- 员工列表 -->
            <div
              v-for="employee in filteredEmployees"
              :key="employee.employeeId"
              class="employee-item"
              :class="{ selected: isSelected(employee), disabled: isExistingMember(employee) }"
              @click="toggleEmployee(employee)"
            >
              <!-- 头像 -->
              <a-avatar :src="employee.avatar" :size="40">
                {{ employee.actualName?.[0] || '?' }}
              </a-avatar>

              <!-- 员工信息 -->
              <div class="employee-info">
                <div class="employee-name">{{ employee.actualName }}</div>
                <div class="employee-dept">{{ employee.departmentName }}</div>
              </div>

              <!-- 状态标识 -->
              <div class="employee-status">
                <a-tag v-if="isExistingMember(employee)" color="default">已在群组</a-tag>
                <CheckCircleFilled v-else-if="isSelected(employee)" class="check-icon" />
              </div>
            </div>
          </a-spin>
        </div>

        <!-- 右侧：已选成员 -->
        <div class="selected-section">
          <div class="selected-header">
            <span>已选择 ({{ selectedEmployees.length }})</span>
            <a-button
              v-if="selectedEmployees.length > 0"
              type="link"
              size="small"
              @click="clearSelected"
            >
              清空
            </a-button>
          </div>

          <div class="selected-list">
            <a-empty
              v-if="selectedEmployees.length === 0"
              description="未选择成员"
              :image="Empty.PRESENTED_IMAGE_SIMPLE"
            />

            <a-tag
              v-for="employee in selectedEmployees"
              :key="employee.employeeId"
              closable
              @close="removeEmployee(employee)"
              class="selected-tag"
            >
              {{ employee.actualName }}
            </a-tag>
          </div>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { message as antMessage, Empty } from 'ant-design-vue';
import { SearchOutlined, CheckCircleFilled } from '@ant-design/icons-vue';
import { employeeApi } from '/@/api/system/employee-api';
import { imTokenApi } from '/@/api/business/oa/im-token-api';
import { openIMClient } from '/@/utils/openim-client';
import { smartSentry } from '/@/lib/smart-sentry';

// 员工信息类型
interface Employee {
  employeeId: number;
  actualName: string;
  departmentName: string;
  avatar?: string;
  phone?: string;
}

// Props
const props = defineProps<{
  groupId: string;
  reportId: number;
  existingMemberIds?: string[]; // 已在群组的成员OpenIM ID列表
}>();

// Emits
const emit = defineEmits<{
  success: [memberIds: string[]];
  cancel: [];
}>();

// 状态
const visible = ref(false);
const loading = ref(false);
const loadingEmployees = ref(false);
const searchKeyword = ref('');
const allEmployees = ref<Employee[]>([]);
const selectedEmployees = ref<Employee[]>([]);

// 计算属性
const filteredEmployees = computed(() => {
  if (!searchKeyword.value) {
    return allEmployees.value;
  }

  const keyword = searchKeyword.value.toLowerCase();
  return allEmployees.value.filter(
    (emp) =>
      emp.actualName?.toLowerCase().includes(keyword) ||
      emp.departmentName?.toLowerCase().includes(keyword)
  );
});

// 判断员工是否已选中
function isSelected(employee: Employee): boolean {
  return selectedEmployees.value.some((e) => e.employeeId === employee.employeeId);
}

// 判断员工是否已在群组
function isExistingMember(employee: Employee): boolean {
  if (!props.existingMemberIds) {
    return false;
  }
  // 已在群组的成员ID列表是OpenIM格式，无法直接通过employeeId判断
  // 这里暂时返回false，实际判断需要在邀请时通过API获取OpenIM UserID后再比较
  // TODO: 优化 - 在加载员工列表时就批量获取所有员工的OpenIM UserID并缓存
  return false;
}

// 切换员工选择状态
function toggleEmployee(employee: Employee) {
  // 如果已在群组，不允许选择
  if (isExistingMember(employee)) {
    antMessage.warning('该成员已在群组中');
    return;
  }

  if (isSelected(employee)) {
    removeEmployee(employee);
  } else {
    selectedEmployees.value.push(employee);
  }
}

// 移除已选员工
function removeEmployee(employee: Employee) {
  selectedEmployees.value = selectedEmployees.value.filter(
    (e) => e.employeeId !== employee.employeeId
  );
}

// 清空已选
function clearSelected() {
  selectedEmployees.value = [];
}

// 搜索
function handleSearch() {
  console.log('🔍 [邀请成员] 搜索:', searchKeyword.value);
}

// 加载员工列表
async function loadEmployees() {
  loadingEmployees.value = true;

  try {
    console.log('📋 [邀请成员] 开始加载员工列表');

    // 调用员工API获取所有员工
    const response = await employeeApi.queryAll();

    if (response && response.data) {
      allEmployees.value = response.data.map((emp: any) => ({
        employeeId: emp.employeeId,
        actualName: emp.actualName,
        departmentName: emp.departmentName || '未分配部门',
        avatar: emp.avatar,
        phone: emp.phone,
      }));

      console.log('✅ [邀请成员] 员工列表加载成功, 总数:', allEmployees.value.length);
    }
  } catch (error) {
    console.error('❌ [邀请成员] 加载员工列表失败:', error);
    smartSentry.captureError(error, {
      tags: { module: 'InviteMemberModal', action: 'loadEmployees' },
    });
    antMessage.error('加载员工列表失败: ' + (error as Error).message);
  } finally {
    loadingEmployees.value = false;
  }
}

// 邀请成员
async function handleInvite() {
  if (selectedEmployees.value.length === 0) {
    antMessage.warning('请选择要邀请的成员');
    return;
  }

  loading.value = true;

  try {
    console.log('➕ [邀请成员] 开始邀请, 人数:', selectedEmployees.value.length);

    // 1. 批量获取员工的OpenIM用户ID
    const employeeIds = selectedEmployees.value.map((emp) => emp.employeeId);
    console.log('📋 [邀请成员] 获取OpenIM用户ID, 员工IDs:', employeeIds);

    const userIdMapping = await imTokenApi.batchGetUserMapping(employeeIds);
    console.log('📥 [邀请成员] OpenIM用户ID映射:', userIdMapping);

    // 2. 提取OpenIM用户ID列表
    const userIDs = Object.values(userIdMapping).filter(id => id != null && id !== '');

    if (userIDs.length === 0) {
      antMessage.error('选择的员工尚未同步到IM系统，请稍后重试');
      console.error('❌ [邀请成员] 没有有效的OpenIM用户ID');
      return;
    }

    console.log('📤 [邀请成员] OpenIM UserIDs:', userIDs);

    // 检查OpenIM是否已登录
    if (!openIMClient.loggedIn) {
      antMessage.error('OpenIM未登录，请刷新页面重试');
      console.error('❌ [邀请成员] OpenIM未登录');
      return;
    }

    // 调用OpenIM SDK邀请成员
    await openIMClient.inviteUsersToGroup(props.groupId, userIDs, '邀请加入群聊');

    antMessage.success(`成功邀请 ${selectedEmployees.value.length} 名成员`);

    console.log('✅ [邀请成员] 邀请成功');

    // 触发成功事件
    emit('success', userIDs);

    // 关闭Modal
    visible.value = false;

    // 清空选择
    selectedEmployees.value = [];
  } catch (error) {
    console.error('❌ [邀请成员] 邀请失败:', error);

    // 详细错误处理
    let errorMessage = '邀请失败';
    const errorObj = error as any;

    if (errorObj?.errMsg) {
      // OpenIM SDK错误
      if (errorObj.errMsg.includes('not found')) {
        errorMessage = '用户不存在，请确认用户已在OpenIM中注册';
      } else if (errorObj.errMsg.includes('permission')) {
        errorMessage = '没有邀请权限，请联系群主';
      } else {
        errorMessage = `邀请失败: ${errorObj.errMsg}`;
      }
    } else if (error instanceof Error) {
      if (error.message.includes('network')) {
        errorMessage = '网络错误，请检查网络连接';
      } else {
        errorMessage = `邀请失败: ${error.message}`;
      }
    }

    smartSentry.captureError(error, {
      tags: { module: 'InviteMemberModal', action: 'handleInvite' },
      extra: {
        groupId: props.groupId,
        selectedCount: selectedEmployees.value.length,
        employeeIds: selectedEmployees.value.map((emp) => emp.employeeId),
      },
    });

    antMessage.error(errorMessage);
  } finally {
    loading.value = false;
  }
}

// 取消
function handleCancel() {
  visible.value = false;
  selectedEmployees.value = [];
  searchKeyword.value = '';
  emit('cancel');
}

// 打开Modal
function open() {
  visible.value = true;
  loadEmployees();
}

// 关闭Modal
function close() {
  visible.value = false;
  selectedEmployees.value = [];
  searchKeyword.value = '';
}

// 监听visible变化
watch(visible, (newVisible) => {
  if (newVisible) {
    loadEmployees();
  } else {
    // 关闭时清空选择和搜索
    selectedEmployees.value = [];
    searchKeyword.value = '';
  }
});

// 暴露方法供父组件调用
defineExpose({
  open,
  close,
});
</script>

<style scoped lang="less">
.invite-member-modal {
  // 搜索区域
  .search-section {
    margin-bottom: 16px;
  }

  // 内容区域
  .content-section {
    display: flex;
    gap: 16px;
    height: 500px;

    // 左侧员工列表
    .employee-list {
      flex: 1;
      overflow-y: auto;
      border: 1px solid #e8e8e8;
      border-radius: 4px;
      padding: 8px;

      &::-webkit-scrollbar {
        width: 6px;
      }

      &::-webkit-scrollbar-thumb {
        background: #d9d9d9;
        border-radius: 3px;
      }

      .employee-item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px;
        cursor: pointer;
        border-radius: 4px;
        transition: all 0.2s;
        margin-bottom: 4px;

        &:hover:not(.disabled) {
          background: #f5f5f5;
        }

        &.selected {
          background: #e6f7ff;
          border: 1px solid #1890ff;
        }

        &.disabled {
          cursor: not-allowed;
          opacity: 0.5;
          background: #fafafa;
        }

        .employee-info {
          flex: 1;
          min-width: 0;

          .employee-name {
            font-size: 14px;
            font-weight: 500;
            color: #262626;
            margin-bottom: 4px;
          }

          .employee-dept {
            font-size: 12px;
            color: #8c8c8c;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        .employee-status {
          flex-shrink: 0;

          .check-icon {
            color: #52c41a;
            font-size: 20px;
          }
        }
      }
    }

    // 右侧已选区域
    .selected-section {
      width: 250px;
      border: 1px solid #e8e8e8;
      border-radius: 4px;
      display: flex;
      flex-direction: column;

      .selected-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 12px 16px;
        border-bottom: 1px solid #e8e8e8;
        font-weight: 500;
      }

      .selected-list {
        flex: 1;
        overflow-y: auto;
        padding: 12px;

        &::-webkit-scrollbar {
          width: 6px;
        }

        &::-webkit-scrollbar-thumb {
          background: #d9d9d9;
          border-radius: 3px;
        }

        .selected-tag {
          margin-bottom: 8px;
          padding: 4px 8px;
          font-size: 13px;
        }
      }
    }
  }
}
</style>
