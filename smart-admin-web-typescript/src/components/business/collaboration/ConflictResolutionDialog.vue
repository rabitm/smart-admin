<!--
  * 冲突解决对话框组件 - 处理协作编辑中的数据冲突
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-24
  * @Copyright 1024创新实验室
-->
<template>
  <a-modal
    v-model:open="visible"
    title="🔧 冲突解决"
    width="800px"
    :closable="false"
    :mask-closable="false"
    @ok="handleResolve"
    @cancel="handleCancel"
  >
    <template #footer>
      <div class="conflict-footer">
        <div class="strategy-selector">
          <span class="strategy-label">解决策略:</span>
          <a-select
            v-model:value="selectedStrategy"
            style="width: 160px"
            @change="handleStrategyChange"
          >
            <a-select-option
              v-for="strategy in availableStrategies"
              :key="strategy.value"
              :value="strategy.value"
            >
              {{ strategy.label }}
            </a-select-option>
          </a-select>
        </div>

        <div class="footer-actions">
          <a-button @click="handleCancel">取消</a-button>
          <a-button
            type="primary"
            :loading="resolving"
            @click="handleResolve"
          >
            应用解决方案
          </a-button>
        </div>
      </div>
    </template>

    <div v-if="conflict" class="conflict-dialog">
      <!-- 冲突信息头部 -->
      <div class="conflict-header">
        <div class="conflict-info">
          <span class="conflict-type-badge" :class="getConflictTypeClass(conflict.type)">
            {{ getConflictTypeText(conflict.type) }}
          </span>
          <span class="field-name">{{ conflict.fieldLabel }}</span>
        </div>
        <div class="conflict-time">
          {{ formatTime(conflict.timestamp) }}
        </div>
      </div>

      <!-- 冲突变更列表 -->
      <div class="conflict-changes">
        <div class="changes-title">
          <span>变更详情 ({{ conflict.changes.length }} 个变更)</span>
        </div>

        <div class="changes-list">
          <div
            v-for="(change, index) in conflict.changes"
            :key="`change-${index}`"
            class="change-item"
            :class="{ 'change-selected': selectedChangeIndex === index }"
            @click="selectChange(index)"
          >
            <!-- 用户信息 -->
            <div class="change-user">
              <div class="user-avatar" :style="{ backgroundColor: change.user.role === 'admin' ? '#f5222d' : '#1890ff' }">
                <span class="avatar-text">{{ getAvatarText(change.user.name) }}</span>
              </div>
              <div class="user-details">
                <div class="user-name">{{ change.user.name }}</div>
                <div class="user-role">{{ getRoleText(change.user.role) }}</div>
              </div>
              <div class="change-time">
                {{ formatRelativeTime(change.timestamp) }}
              </div>
            </div>

            <!-- 值变更 -->
            <div class="value-change">
              <div class="value-item old-value">
                <div class="value-label">原值:</div>
                <div class="value-content">{{ formatValue(change.oldValue) }}</div>
              </div>
              <div class="value-arrow">→</div>
              <div class="value-item new-value">
                <div class="value-label">新值:</div>
                <div class="value-content">{{ formatValue(change.newValue) }}</div>
              </div>
            </div>

            <!-- 选择按钮 -->
            <div class="change-actions">
              <a-radio
                :checked="selectedChangeIndex === index"
                @click.stop="selectChange(index)"
              >
                选择此值
              </a-radio>
            </div>
          </div>
        </div>
      </div>

      <!-- 策略说明 -->
      <div class="strategy-info">
        <div class="strategy-title">
          <span>{{ getStrategyInfo().title }}</span>
        </div>
        <div class="strategy-description">
          {{ getStrategyInfo().description }}
        </div>
      </div>

      <!-- 预览结果 -->
      <div class="resolution-preview">
        <div class="preview-title">
          <span>解决结果预览</span>
        </div>
        <div class="preview-content">
          <div class="final-value">
            <span class="preview-label">最终值:</span>
            <span class="preview-value">{{ formatValue(previewResult.finalValue) }}</span>
          </div>
          <div v-if="previewResult.reason" class="resolution-reason">
            <span class="preview-label">原因:</span>
            <span class="reason-text">{{ previewResult.reason }}</span>
          </div>
        </div>
      </div>

      <!-- 手动输入框 -->
      <div v-if="selectedStrategy === 'MANUAL_MERGE'" class="manual-input">
        <div class="manual-title">
          <span>手动输入值</span>
        </div>
        <a-textarea
          v-if="isTextInput"
          v-model:value="manualValue"
          :rows="3"
          placeholder="请输入合并后的值..."
        />
        <a-input
          v-else
          v-model:value="manualValue"
          placeholder="请输入合并后的值..."
        />
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { formatDistanceToNow, format } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import {
  ConflictInfo,
  ConflictType,
  ResolutionStrategy,
  ResolutionResult,
  ConflictResolver,
  createConflictResolver
} from '/@/utils/conflict-resolver';

interface Props {
  modelValue: boolean;
  conflict: ConflictInfo | null;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  resolve: [result: ResolutionResult];
  cancel: [];
}>();

// 响应式数据
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});

const selectedStrategy = ref<ResolutionStrategy>(ResolutionStrategy.LAST_WRITE_WINS);
const selectedChangeIndex = ref<number>(0);
const manualValue = ref<string>('');
const resolving = ref(false);

// 冲突解决器实例
const conflictResolver = createConflictResolver();

// 可用策略
const availableStrategies = computed(() => {
  const strategies = [
    { value: ResolutionStrategy.LAST_WRITE_WINS, label: '最后写入胜出' },
    { value: ResolutionStrategy.FIRST_WRITE_WINS, label: '最先写入胜出' },
    { value: ResolutionStrategy.ROLE_BASED, label: '基于角色权限' },
    { value: ResolutionStrategy.MANUAL_MERGE, label: '手动合并' },
    { value: ResolutionStrategy.AUTO_MERGE, label: '自动合并' },
    { value: ResolutionStrategy.REJECT_ALL, label: '拒绝所有变更' }
  ];

  // 根据冲突类型过滤策略
  if (!props.conflict) return strategies;

  if (props.conflict.changes.length !== 2) {
    // 超过2个变更时不支持自动合并
    return strategies.filter(s => s.value !== ResolutionStrategy.AUTO_MERGE);
  }

  return strategies;
});

// 是否为文本输入
const isTextInput = computed(() => {
  if (!props.conflict) return false;
  return props.conflict.changes[0]?.fieldType === 'text' ||
         props.conflict.changes[0]?.fieldType === 'textarea';
});

// 预览结果
const previewResult = computed(() => {
  if (!props.conflict) {
    return { finalValue: '', reason: '' };
  }

  try {
    // 模拟解决结果
    const tempResult = getPreviewResult();
    return {
      finalValue: tempResult.finalValue,
      reason: tempResult.reason
    };
  } catch (error) {
    return { finalValue: '', reason: '预览失败' };
  }
});

// 获取预览结果
const getPreviewResult = () => {
  if (!props.conflict) throw new Error('No conflict');

  const changes = props.conflict.changes;

  switch (selectedStrategy.value) {
    case ResolutionStrategy.LAST_WRITE_WINS:
      const lastChange = [...changes].sort((a, b) => b.timestamp - a.timestamp)[0];
      return {
        finalValue: lastChange.newValue,
        reason: `采用最后写入的值: ${lastChange.user.name}`
      };

    case ResolutionStrategy.FIRST_WRITE_WINS:
      const firstChange = [...changes].sort((a, b) => a.timestamp - b.timestamp)[0];
      return {
        finalValue: firstChange.newValue,
        reason: `采用最先写入的值: ${firstChange.user.name}`
      };

    case ResolutionStrategy.ROLE_BASED:
      const roleChange = [...changes].sort((a, b) =>
        getRolePriority(b.user.role) - getRolePriority(a.user.role)
      )[0];
      return {
        finalValue: roleChange.newValue,
        reason: `采用高权限用户的值: ${roleChange.user.name} (${getRoleText(roleChange.user.role)})`
      };

    case ResolutionStrategy.MANUAL_MERGE:
      return {
        finalValue: manualValue.value,
        reason: '用户手动指定的值'
      };

    case ResolutionStrategy.AUTO_MERGE:
      if (changes.length === 2) {
        const merged = mergeSimpleValues(changes[0].newValue, changes[1].newValue);
        return {
          finalValue: merged,
          reason: '自动合并两个变更'
        };
      }
      return {
        finalValue: changes[0].newValue,
        reason: '无法自动合并'
      };

    case ResolutionStrategy.REJECT_ALL:
      return {
        finalValue: changes[0].oldValue,
        reason: '保持原值，拒绝所有变更'
      };

    default:
      return {
        finalValue: changes[0].newValue,
        reason: '默认策略'
      };
  }
};

// 获取角色优先级
const getRolePriority = (role: string): number => {
  const priorities: { [key: string]: number } = {
    'admin': 100,
    'supervisor': 80,
    'detective': 60,
    'officer': 40,
    'clerk': 20
  };
  return priorities[role.toLowerCase()] || 0;
};

// 简单值合并
const mergeSimpleValues = (value1: any, value2: any): any => {
  if (typeof value1 === 'string' && typeof value2 === 'string') {
    return value1.length > value2.length ? value1 : value2;
  }
  return value1;
};

// 获取用户头像文字
const getAvatarText = (name: string): string => {
  if (!name) return '?';
  if (/[\u4e00-\u9fa5]/.test(name)) {
    return name.slice(-1);
  }
  const words = name.split(' ');
  return words.map(word => word.charAt(0)).join('').substring(0, 2).toUpperCase();
};

// 获取冲突类型样式类
const getConflictTypeClass = (type: ConflictType): string => {
  const classes: { [key in ConflictType]: string } = {
    [ConflictType.CONCURRENT_EDIT]: 'type-concurrent',
    [ConflictType.LATE_UPDATE]: 'type-late',
    [ConflictType.PERMISSION_CONFLICT]: 'type-permission',
    [ConflictType.DATA_TYPE_MISMATCH]: 'type-mismatch'
  };
  return classes[type] || 'type-default';
};

// 获取冲突类型文本
const getConflictTypeText = (type: ConflictType): string => {
  const texts: { [key in ConflictType]: string } = {
    [ConflictType.CONCURRENT_EDIT]: '并发编辑',
    [ConflictType.LATE_UPDATE]: '延迟更新',
    [ConflictType.PERMISSION_CONFLICT]: '权限冲突',
    [ConflictType.DATA_TYPE_MISMATCH]: '类型不匹配'
  };
  return texts[type] || '未知冲突';
};

// 获取角色文本
const getRoleText = (role: string): string => {
  const roles: { [key: string]: string } = {
    'admin': '管理员',
    'supervisor': '主管',
    'detective': '探员',
    'officer': '警员',
    'clerk': '文员'
  };
  return roles[role.toLowerCase()] || role;
};

// 获取策略信息
const getStrategyInfo = () => {
  const infos: { [key in ResolutionStrategy]: { title: string; description: string } } = {
    [ResolutionStrategy.LAST_WRITE_WINS]: {
      title: '最后写入胜出',
      description: '采用时间戳最晚的变更作为最终值。这是最常用的冲突解决策略。'
    },
    [ResolutionStrategy.FIRST_WRITE_WINS]: {
      title: '最先写入胜出',
      description: '采用时间戳最早的变更作为最终值。适用于需要保护初始输入的场景。'
    },
    [ResolutionStrategy.ROLE_BASED]: {
      title: '基于角色权限',
      description: '根据用户角色权限决定，优先级: 管理员 > 主管 > 探员 > 警员 > 文员。'
    },
    [ResolutionStrategy.MANUAL_MERGE]: {
      title: '手动合并',
      description: '由用户手动输入最终值。适用于复杂情况或需要人工判断的场景。'
    },
    [ResolutionStrategy.AUTO_MERGE]: {
      title: '自动合并',
      description: '系统尝试智能合并两个变更。仅适用于文本类型且变更数量为2的情况。'
    },
    [ResolutionStrategy.REJECT_ALL]: {
      title: '拒绝所有变更',
      description: '保持原值不变，拒绝所有变更。适用于数据保护要求高的场景。'
    }
  };
  return infos[selectedStrategy.value] || { title: '', description: '' };
};

// 格式化时间
const formatTime = (timestamp: number): string => {
  return format(timestamp, 'yyyy-MM-dd HH:mm:ss', { locale: zhCN });
};

// 格式化相对时间
const formatRelativeTime = (timestamp: number): string => {
  return formatDistanceToNow(timestamp, { addSuffix: true, locale: zhCN });
};

// 格式化值
const formatValue = (value: any): string => {
  if (value === null || value === undefined) return '(空)';
  if (typeof value === 'boolean') return value ? '是' : '否';
  if (typeof value === 'object') return JSON.stringify(value, null, 2);
  return String(value);
};

// 选择变更
const selectChange = (index: number) => {
  selectedChangeIndex.value = index;
};

// 策略变更处理
const handleStrategyChange = () => {
  if (selectedStrategy.value === ResolutionStrategy.MANUAL_MERGE) {
    // 初始化手动输入值
    const selectedChange = props.conflict?.changes[selectedChangeIndex.value];
    manualValue.value = selectedChange ? formatValue(selectedChange.newValue) : '';
  }
};

// 解决冲突
const handleResolve = async () => {
  if (!props.conflict) return;

  resolving.value = true;
  try {
    let result: ResolutionResult;

    if (selectedStrategy.value === ResolutionStrategy.MANUAL_MERGE) {
      result = await conflictResolver.resolveConflict(
        props.conflict,
        selectedStrategy.value,
        manualValue.value
      );
    } else {
      result = await conflictResolver.resolveConflict(
        props.conflict,
        selectedStrategy.value
      );
    }

    emit('resolve', result);
    visible.value = false;

  } catch (error) {
    console.error('冲突解决失败:', error);
    // TODO: 显示错误提示
  } finally {
    resolving.value = false;
  }
};

// 取消处理
const handleCancel = () => {
  emit('cancel');
  visible.value = false;
};

// 监听冲突变化，重置状态
watch(() => props.conflict, (newConflict) => {
  if (newConflict) {
    selectedStrategy.value = newConflict.suggestedStrategy;
    selectedChangeIndex.value = 0;
    manualValue.value = '';
  }
}, { immediate: true });
</script>

<style scoped>
.conflict-dialog {
  max-height: 600px;
  overflow-y: auto;
}

.conflict-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
  margin-bottom: 20px;
}

.conflict-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.conflict-type-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  color: white;
}

.type-concurrent { background-color: #1890ff; }
.type-late { background-color: #faad14; }
.type-permission { background-color: #f5222d; }
.type-mismatch { background-color: #722ed1; }
.type-default { background-color: #8c8c8c; }

.field-name {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.conflict-time {
  font-size: 12px;
  color: #8c8c8c;
}

.conflict-changes {
  margin-bottom: 20px;
}

.changes-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 12px;
}

.changes-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.change-item {
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.change-item:hover {
  border-color: rgba(24, 144, 255, 0.3);
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

.change-selected {
  border-color: #1890ff;
  background: #e6f7ff;
}

.change-user {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #1890ff;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.avatar-text {
  color: white;
  font-size: 12px;
  font-weight: bold;
}

.user-details {
  flex: 1;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 2px;
}

.user-role {
  font-size: 12px;
  color: #8c8c8c;
}

.change-time {
  font-size: 12px;
  color: #8c8c8c;
}

.value-change {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.value-item {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 4px;
  background: white;
}

.value-label {
  font-size: 11px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.value-content {
  font-size: 13px;
  color: #262626;
  word-break: break-all;
}

.old-value {
  background: #fff2f0;
  border-color: #ffccc7;
}

.new-value {
  background: #f6ffed;
  border-color: #b7eb8f;
}

.value-arrow {
  color: #1890ff;
  font-weight: bold;
  font-size: 16px;
}

.change-actions {
  text-align: right;
}

.strategy-info {
  background: #f0f7ff;
  border: 1px solid #91d5ff;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 20px;
}

.strategy-title {
  font-size: 14px;
  font-weight: 600;
  color: #1890ff;
  margin-bottom: 8px;
}

.strategy-description {
  font-size: 13px;
  color: #262626;
  line-height: 1.5;
}

.resolution-preview {
  background: #f9f9f9;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 20px;
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 8px;
}

.preview-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.final-value,
.resolution-reason {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.preview-label {
  font-size: 12px;
  color: #8c8c8c;
  min-width: 60px;
}

.preview-value {
  font-size: 13px;
  color: #262626;
  font-weight: 500;
  background: white;
  padding: 4px 8px;
  border-radius: 3px;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.reason-text {
  font-size: 13px;
  color: #595959;
}

.manual-input {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
  padding: 12px;
}

.manual-title {
  font-size: 14px;
  font-weight: 600;
  color: #d46b08;
  margin-bottom: 8px;
}

.conflict-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.strategy-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.strategy-label {
  font-size: 14px;
  color: #262626;
}

.footer-actions {
  display: flex;
  gap: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .conflict-header {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }

  .value-change {
    flex-direction: column;
    gap: 8px;
  }

  .value-arrow {
    transform: rotate(90deg);
  }

  .conflict-footer {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
}
</style>