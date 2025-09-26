<!--
  * 警务专业信息字段渲染组件 - 确保预览和实际录入界面的一致性
  *
  * @Author:    Claude Code Assistant
  * @Date:      2025-09-23
  * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
-->
<template>
  <div class="professional-fields">
    <div
      v-for="field in fields"
      :key="getFieldKey(field)"
      class="pro-field collaboration-field"
      :class="{ 'compact-group-field': getFieldType(field) === 'compact-group' }"
    >
      <div class="pro-label">
        <span class="field-icon">{{ getFieldIcon(field) || '📝' }}</span>
        {{ getFieldLabel(field) }}
        <span v-if="getFieldRequired(field)" class="required-mark">*</span>
      </div>

      <!-- 协作指示器 -->
      <CollaborationFieldIndicator
        :field-name="getFieldKey(field)"
        :field-state="getFieldState(getFieldKey(field))"
      />

      <!-- 紧凑组合字段 -->
      <div v-if="getFieldType(field) === 'compact-group'" class="compact-group">
        <div
          v-for="subField in getFieldChildren(field)"
          :key="getFieldKey(subField)"
          :class="['compact-subfield', { 'locked-by-other': isFieldLockedByOther(getFieldKey(subField)) }]"
          style="position: relative;"
        >
          <span class="subfield-label">{{ getFieldLabel(subField) }}:</span>
          <!-- 子字段快捷选项 -->
          <div v-if="getFieldQuickOptions(subField)" class="compact-options">
            <button
              v-for="option in getFieldQuickOptions(subField)"
              :key="option"
              :class="['compact-btn', { 'selected': isOptionSelected(getFieldKey(subField), option) }]"
              @click="handleButtonClick(getFieldKey(subField), () => selectOption(getFieldKey(subField), option), '快捷选项', option)"
              :disabled="disabled || isFieldLockedByOther(getFieldKey(subField))"
            >
              {{ option }}
            </button>
          </div>
          <!-- 子字段选择选项 -->
          <div v-else-if="getFieldDictCode(subField) || getFieldOptions(subField)" class="compact-select-wrapper">
            <!-- 使用数据字典 - 平铺按钮式 -->
            <div v-if="getFieldDictCode(subField)" class="compact-options">
              <button
                v-for="item in getDictOptions(getFieldDictCode(subField))"
                :key="item.dataValue"
                :class="['compact-btn', { 'selected': modelValue[getFieldKey(subField)] === item.dataValue }]"
                @click="handleButtonClick(getFieldKey(subField), () => updateField(getFieldKey(subField), item.dataValue), '字典单选', item.dataValue)"
                :disabled="disabled || isFieldLockedByOther(getFieldKey(subField))"
                :title="item.dataLabel"
              >
                {{ item.dataLabel }}
              </button>
            </div>
            <!-- 使用硬编码选项 -->
            <div v-else class="compact-options">
              <button
                v-for="option in getFieldOptions(subField)"
                :key="option"
                :class="['compact-btn', { 'selected': modelValue[getFieldKey(subField)] === option }]"
                @click="handleButtonClick(getFieldKey(subField), () => updateField(getFieldKey(subField), option), '硬编码选项', option)"
                :disabled="disabled || isFieldLockedByOther(getFieldKey(subField))"
              >
                {{ option }}
              </button>
            </div>
          </div>
          <!-- 子字段输入框 -->
          <input
            v-else-if="getFieldType(subField) === 'input'"
            type="text"
            :value="modelValue[getFieldKey(subField)] || ''"
            @input="updateField(getFieldKey(subField), ($event.target as HTMLInputElement).value)"
            @focus="onFieldFocus(getFieldKey(subField))"
            @blur="onFieldBlur(getFieldKey(subField))"
            :placeholder="getFieldPlaceholder(subField)"
            :disabled="disabled || isFieldLockedByOther(getFieldKey(subField))"
            class="compact-input"
          />

          <!-- 子字段锁定遮罩 -->
          <div v-if="isFieldLockedByOther(getFieldKey(subField))" class="field-lock-overlay">
            <span class="lock-icon">🔒</span>
            <span class="lock-text">{{ getFieldState(getFieldKey(subField)).lockedBy?.name }} 正在编辑</span>
          </div>
        </div>
      </div>

      <!-- 紧凑多选 -->
      <div v-else-if="getFieldType(field) === 'checkbox-compact'" :class="['checkbox-compact', { 'locked-by-other': isFieldLockedByOther(getFieldKey(field)) }]">
        <button
          v-for="option in getFieldQuickOptions(field) || getFieldOptions(field)"
          :key="option"
          :class="['compact-checkbox-btn', { 'selected': isCheckboxSelected(getFieldKey(field), option) }]"
          @click="handleButtonClick(getFieldKey(field), () => toggleCheckbox(getFieldKey(field), option), '紧凑多选', option)"
          :disabled="disabled || isFieldLockedByOther(getFieldKey(field))"
        >
          <span class="check-icon">{{ isCheckboxSelected(getFieldKey(field), option) ? '✓' : '' }}</span>
          {{ option }}
        </button>
        <div v-if="isFieldLockedByOther(getFieldKey(field))" class="field-lock-overlay">
          <span class="lock-icon">🔒</span>
          <span class="lock-text">{{ getFieldState(getFieldKey(field)).lockedBy?.name }} 正在编辑</span>
        </div>
      </div>

      <!-- 快捷选择按钮 -->
      <div v-else-if="getFieldQuickOptions(field)" :class="['quick-options', { 'locked-by-other': isFieldLockedByOther(getFieldKey(field)) }]">
        <button
          v-for="(option, idx) in getFieldQuickOptions(field)"
          :key="option"
          :class="['quick-btn', { 'selected': isOptionSelected(getFieldKey(field), option), 'debug-locked': isFieldLockedByOther(getFieldKey(field)) }]"
          @click="handleButtonClick(getFieldKey(field), () => selectOption(getFieldKey(field), option), '快捷选择', option)"
          :disabled="disabled || isFieldLockedByOther(getFieldKey(field))"
          :title="`快捷键: ${idx + 1}`"
        >
          {{ option }}
          <span class="btn-hotkey" v-if="!disabled && !isFieldLockedByOther(getFieldKey(field))">{{ idx + 1 }}</span>
        </button>
        <div v-if="isFieldLockedByOther(getFieldKey(field))" class="field-lock-overlay">
          <span class="lock-icon">🔒</span>
          <span class="lock-text">{{ getFieldState(getFieldKey(field)).lockedBy?.name }} 正在编辑</span>
        </div>
      </div>

      <!-- 选择按钮 -->
      <div v-else-if="getFieldType(field) === 'select'" :class="['select-wrapper', { 'locked-by-other': isFieldLockedByOther(getFieldKey(field)) }]">
        <!-- 使用数据字典 - 平铺按钮式 -->
        <div v-if="getFieldDictCode(field)" class="select-options">
          <button
            v-for="item in getDictOptions(getFieldDictCode(field))"
            :key="item.dataValue"
            :class="['select-btn', { 'selected': modelValue[getFieldKey(field)] === item.dataValue }]"
            @click="handleButtonClick(getFieldKey(field), () => updateField(getFieldKey(field), item.dataValue), '字典单选', item.dataValue)"
            :disabled="disabled || isFieldLockedByOther(getFieldKey(field))"
            :title="item.dataLabel"
          >
            {{ item.dataLabel }}
          </button>
        </div>
        <!-- 使用硬编码选项 -->
        <div v-else-if="getFieldOptions(field)" class="select-options">
          <button
            v-for="option in getFieldOptions(field)"
            :key="option"
            :class="['select-btn', { 'selected': modelValue[getFieldKey(field)] === option }]"
            @click="handleButtonClick(getFieldKey(field), () => updateField(getFieldKey(field), option), '硬编码单选', option)"
            :disabled="disabled || isFieldLockedByOther(getFieldKey(field))"
          >
            {{ option }}
          </button>
        </div>
        <div v-if="isFieldLockedByOther(getFieldKey(field))" class="field-lock-overlay">
          <span class="lock-icon">🔒</span>
          <span class="lock-text">{{ getFieldState(getFieldKey(field)).lockedBy?.name }} 正在编辑</span>
        </div>
      </div>

      <!-- 多选 -->
      <div v-else-if="getFieldType(field) === 'checkbox'" :class="['checkbox-wrapper', { 'locked-by-other': isFieldLockedByOther(getFieldKey(field)) }]">
        <!-- 使用数据字典 - 平铺按钮式 -->
        <div v-if="getFieldDictCode(field)" class="checkbox-options">
          <button
            v-for="item in getDictOptions(getFieldDictCode(field))"
            :key="item.dataValue"
            :class="['checkbox-btn', { 'selected': isDictCheckboxSelected(getFieldKey(field), item.dataValue) }]"
            @click="handleButtonClick(getFieldKey(field), () => toggleDictCheckbox(getFieldKey(field), item.dataValue), '字典多选', item.dataValue)"
            :disabled="disabled || isFieldLockedByOther(getFieldKey(field))"
            :title="item.dataLabel"
          >
            <span class="check-icon">{{ isDictCheckboxSelected(getFieldKey(field), item.dataValue) ? '✓' : '' }}</span>
            {{ item.dataLabel }}
          </button>
        </div>
        <!-- 使用硬编码选项 -->
        <div v-else-if="getFieldOptions(field)" class="checkbox-options">
          <button
            v-for="option in getFieldOptions(field)"
            :key="option"
            :class="['checkbox-btn', { 'selected': isCheckboxSelected(getFieldKey(field), option) }]"
            @click="handleButtonClick(getFieldKey(field), () => toggleCheckbox(getFieldKey(field), option), '硬编码多选', option)"
            :disabled="disabled || isFieldLockedByOther(getFieldKey(field))"
          >
            <span class="check-icon">{{ isCheckboxSelected(getFieldKey(field), option) ? '✓' : '' }}</span>
            {{ option }}
          </button>
        </div>
        <div v-if="isFieldLockedByOther(getFieldKey(field))" class="field-lock-overlay">
          <span class="lock-icon">🔒</span>
          <span class="lock-text">{{ getFieldState(getFieldKey(field)).lockedBy?.name }} 正在编辑</span>
        </div>
      </div>

      <!-- 输入框 -->
      <div v-else-if="getFieldType(field) === 'input'" :class="['input-wrapper', 'field-input-wrapper', { 'locked-by-other': isFieldLockedByOther(getFieldKey(field)) }]">
        <input
          type="text"
          :value="modelValue[getFieldKey(field)] || ''"
          @input="updateField(getFieldKey(field), ($event.target as HTMLInputElement).value)"
          @focus="onFieldFocus(getFieldKey(field))"
          @blur="onFieldBlur(getFieldKey(field))"
          :placeholder="getFieldPlaceholder(field) || `请输入${getFieldLabel(field)}`"
          :disabled="disabled || isFieldLockedByOther(getFieldKey(field))"
          class="preview-input-field"
        />
        <div v-if="isFieldLockedByOther(getFieldKey(field))" class="field-lock-overlay">
          <span class="lock-icon">🔒</span>
          <span class="lock-text">{{ getFieldState(getFieldKey(field)).lockedBy?.name }} 正在编辑</span>
        </div>
      </div>

      <!-- 数字输入 -->
      <div v-else-if="getFieldType(field) === 'number'" :class="['input-wrapper', 'field-input-wrapper', { 'locked-by-other': isFieldLockedByOther(getFieldKey(field)) }]">
        <input
          type="number"
          :value="modelValue[getFieldKey(field)] || ''"
          @input="updateField(getFieldKey(field), parseFloat(($event.target as HTMLInputElement).value) || null)"
          @focus="onFieldFocus(getFieldKey(field))"
          @blur="onFieldBlur(getFieldKey(field))"
          :placeholder="getFieldPlaceholder(field) || `请输入${getFieldLabel(field)}`"
          :disabled="disabled || isFieldLockedByOther(getFieldKey(field))"
          class="preview-input-field"
          min="0"
        />
        <div v-if="isFieldLockedByOther(getFieldKey(field))" class="field-lock-overlay">
          <span class="lock-icon">🔒</span>
          <span class="lock-text">{{ getFieldState(getFieldKey(field)).lockedBy?.name }} 正在编辑</span>
        </div>
      </div>

      <!-- 文本域 -->
      <div v-else-if="getFieldType(field) === 'textarea'" :class="['input-wrapper', 'field-input-wrapper', { 'locked-by-other': isFieldLockedByOther(getFieldKey(field)) }]">
        <textarea
          :value="modelValue[getFieldKey(field)] || ''"
          @input="updateField(getFieldKey(field), ($event.target as HTMLTextAreaElement).value)"
          @focus="onFieldFocus(getFieldKey(field))"
          @blur="onFieldBlur(getFieldKey(field))"
          :placeholder="getFieldPlaceholder(field) || `请输入${getFieldLabel(field)}`"
          :disabled="disabled || isFieldLockedByOther(getFieldKey(field))"
          class="preview-textarea-field"
          rows="2"
          :maxlength="200"
        ></textarea>
        <div v-if="isFieldLockedByOther(getFieldKey(field))" class="field-lock-overlay">
          <span class="lock-icon">🔒</span>
          <span class="lock-text">{{ getFieldState(getFieldKey(field)).lockedBy?.name }} 正在编辑</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import type { FieldItem } from '/@/api/business/oa/police-form-template-api';
  import type { PoliceFormFieldVO } from '/@/api/business/oa/police-form-config-api';
  import { useDictStore } from '/@/store/modules/system/dict';
  import CollaborationFieldIndicator from '/@/components/business/collaboration/CollaborationFieldIndicator.vue';
  import fieldCollaborationManager from '/@/utils/field-collaboration-manager';
  import { simpleFieldLockManager } from '/@/utils/simple-field-lock-manager';
  import { useRoute } from 'vue-router';
  import { useUserStore } from '/@/store/modules/system/user';
  import { inject, onMounted, watch } from 'vue';

  // 通用字段接口 - 支持两种格式
  type UnifiedField = FieldItem | PoliceFormFieldVO;

  interface Props {
    fields: UnifiedField[];
    modelValue: Record<string, any>;
    disabled?: boolean;
  }

  interface Emits {
    (e: 'update:modelValue', value: Record<string, any>): void;
  }

  const props = withDefaults(defineProps<Props>(), {
    disabled: false
  });

  const emit = defineEmits<Emits>();

  // 字典存储
  const dictStore = useDictStore();

  // 用户存储
  const userStore = useUserStore();

  // 路由和协作
  const route = useRoute();

  // 同步管理器
  const syncManager = inject('syncManager') as any;

  // 锁定日志缓存，减少重复日志
  let lockLogCache: string | null = null;

  // 获取字段协作状态
  function getFieldState(fieldName: string) {
    const reportId = route.query.reportId?.toString() || '5';
    const roomId = `police-report-${reportId}`;
    const state = fieldCollaborationManager.getFieldState(roomId, fieldName);

    // 添加反应式状态监听
    if (process.env.NODE_ENV === 'development') {
      // 为每个字段创建一个反应式监听器，跟踪状态变化
      const stateKey = `${fieldName}_watcher`;
      if (!(window as any)[stateKey]) {
        (window as any)[stateKey] = watch(
          () => ({ ...state }),
          (newState, oldState) => {
            console.log(`🔄 [Professional Fields] 字段状态变化:`, {
              fieldName,
              oldState,
              newState,
              stateReference: state,
              timestamp: Date.now()
            });
          },
          { deep: true, immediate: false }
        );
      }
    }

    return state;
  }

  // 判断字段是否被其他用户锁定 (使用新的SimpleFieldLockManager)
  function isFieldLockedByOther(fieldName: string): boolean {
    const reportId = route.query.reportId?.toString() || '5';
    const roomId = `police-report-${reportId}`;

    // 获取当前用户ID - 使用和emergency-intake.vue相同的逻辑
    let currentUserId = '';
    if (userStore.employeeId) {
      currentUserId = userStore.employeeId.toString();
    } else if (userStore.userInfo?.userId) {
      currentUserId = userStore.userInfo.userId.toString();
    } else if (userStore.userInfo?.employeeId) {
      currentUserId = userStore.userInfo.employeeId.toString();
    }

    // 使用SimpleFieldLockManager检查是否被其他用户锁定
    const isLockedByOther = simpleFieldLockManager.isFieldLockedByOther(roomId, fieldName, currentUserId);

    // 只在开发模式且锁定状态变化时输出日志，减少噪音
    if (process.env.NODE_ENV === 'development') {
      const cacheKey = `${fieldName}_${isLockedByOther}`;
      if (!lockLogCache || lockLogCache !== cacheKey) {
        const lockInfo = simpleFieldLockManager.getFieldLock(roomId, fieldName);
        console.log('🔒 [Professional Field Lock Check]', {
          fieldName,
          isLocked: lockInfo.isLocked,
          lockedBy: lockInfo.lockedBy,
          currentUserId,
          isLockedByOther,
          message: isLockedByOther ? 'locked by other user' : 'locked by current user or unlocked',
          lockInfo,
          timestamp: Date.now()
        });
        lockLogCache = cacheKey;
      }
    }

    // 重要：只有在字段被其他用户锁定时才返回true，当前用户锁定的字段对自己不应该显示为locked
    return isLockedByOther;
  }

  // 字段聚焦事件处理
  function onFieldFocus(fieldName: string) {
    const reportId = route.query.reportId?.toString() || '5';
    const roomId = `police-report-${reportId}`;

    // 获取用户ID - 使用和emergency-intake.vue相同的逻辑
    let userId = '';
    let userName = '';

    if (userStore.employeeId) {
      userId = userStore.employeeId.toString();
      userName = userStore.actualName || userStore.userInfo?.actualName || '当前用户';
    } else if (userStore.userInfo?.userId) {
      userId = userStore.userInfo.userId.toString();
      userName = userStore.userInfo.actualName || '当前用户';
    } else if (userStore.userInfo?.employeeId) {
      userId = userStore.userInfo.employeeId.toString();
      userName = userStore.userInfo.actualName || '当前用户';
    } else {
      // 从localStorage获取
      try {
        const storedUser = localStorage.getItem('userInfo') || localStorage.getItem('LOGIN_USER_DATA') || localStorage.getItem('user-token');
        if (storedUser) {
          const user = JSON.parse(storedUser);
          userId = (user.userId || user.employeeId || user.id || '').toString();
          userName = user.actualName || user.name || '当前用户';
        }
      } catch (e) {
        console.warn('🔧 [Professional Fields] 无法从localStorage获取用户信息:', e);
      }
    }

    if (userId) {
      const collaborationUser = {
        id: userId,
        name: userName,
        avatar: userStore.userInfo?.avatar,
        color: generateUserColor(userId)
      };

      // 减少日志噪音，只在开发模式显示
      if (process.env.NODE_ENV === 'development') {
        console.log('🔧 [Professional Fields] onFieldFocus 用户信息:', {
          fieldName,
          userId,
          userName,
          collaborationUser
        });
      }

      fieldCollaborationManager.onFieldFocus(roomId, fieldName, collaborationUser);

      // 发送WebSocket编辑状态
      if (syncManager) {
        try {
          syncManager.syncFieldEditState(parseInt(reportId), fieldName, 'FIELD_FOCUS');
        } catch (error) {
          console.warn('🔧 [Professional Fields] syncManager.syncFieldEditState 调用失败:', error);
        }
      } else {
        console.warn('🔧 [Professional Fields] syncManager 未找到，无法同步字段编辑状态');
      }
    } else {
      console.warn('🔧 [Professional Fields] 用户ID为空，无法进行协作:', { userStore: userStore, userInfo: userStore.userInfo });
    }
  }

  // 字段失焦事件处理
  function onFieldBlur(fieldName: string) {
    const reportId = route.query.reportId?.toString() || '5';
    const roomId = `police-report-${reportId}`;

    // 获取用户ID - 使用和emergency-intake.vue相同的逻辑
    let userId = '';
    let userName = '';

    if (userStore.employeeId) {
      userId = userStore.employeeId.toString();
      userName = userStore.actualName || userStore.userInfo?.actualName || '当前用户';
    } else if (userStore.userInfo?.userId) {
      userId = userStore.userInfo.userId.toString();
      userName = userStore.userInfo.actualName || '当前用户';
    } else if (userStore.userInfo?.employeeId) {
      userId = userStore.userInfo.employeeId.toString();
      userName = userStore.userInfo.actualName || '当前用户';
    } else {
      // 从localStorage获取
      try {
        const storedUser = localStorage.getItem('userInfo') || localStorage.getItem('LOGIN_USER_DATA') || localStorage.getItem('user-token');
        if (storedUser) {
          const user = JSON.parse(storedUser);
          userId = (user.userId || user.employeeId || user.id || '').toString();
          userName = user.actualName || user.name || '当前用户';
        }
      } catch (e) {
        console.warn('🔧 [Professional Fields] 无法从localStorage获取用户信息:', e);
      }
    }

    if (userId) {
      const collaborationUser = {
        id: userId,
        name: userName,
        avatar: userStore.userInfo?.avatar,
        color: generateUserColor(userId)
      };

      fieldCollaborationManager.onFieldBlur(roomId, fieldName, collaborationUser);

      // 发送WebSocket编辑状态
      if (syncManager) {
        try {
          syncManager.syncFieldEditState(parseInt(reportId), fieldName, 'FIELD_BLUR');
        } catch (error) {
          console.warn('🔧 [Professional Fields] syncManager.syncFieldEditState 调用失败:', error);
        }
      }
    }
  }

  // 统一的按钮点击处理函数 - 使用简化锁定机制
  function handleButtonClick(fieldName: string, action: () => void, buttonType: string, value: any) {
    const reportId = route.query.reportId?.toString() || '5';
    const roomId = `police-report-${reportId}`;

    // 获取用户信息
    let userId = '';
    let userName = '';

    if (userStore.employeeId) {
      userId = userStore.employeeId.toString();
      userName = userStore.actualName || userStore.userInfo?.actualName || '当前用户';
    } else if (userStore.userInfo?.userId) {
      userId = userStore.userInfo.userId.toString();
      userName = userStore.userInfo.actualName || '当前用户';
    } else if (userStore.userInfo?.employeeId) {
      userId = userStore.userInfo.employeeId.toString();
      userName = userStore.userInfo.actualName || '当前用户';
    } else {
      // 从localStorage获取
      try {
        const storedUser = localStorage.getItem('userInfo') || localStorage.getItem('LOGIN_USER_DATA') || localStorage.getItem('user-token');
        if (storedUser) {
          const user = JSON.parse(storedUser);
          userId = (user.userId || user.employeeId || user.id || '').toString();
          userName = user.actualName || user.name || '当前用户';
        }
      } catch (e) {
        console.warn('🔧 [Professional Fields] 无法从localStorage获取用户信息:', e);
      }
    }

    if (!userId) {
      console.warn('🔧 [Professional Fields] 用户ID为空，无法进行按钮操作');
      return;
    }

    const collaborationUser = {
      id: userId,
      name: userName,
      avatar: userStore.userInfo?.avatar,
      color: generateUserColor(userId)
    };

    // 检查字段是否被其他用户锁定
    if (simpleFieldLockManager.isFieldLockedByOther(roomId, fieldName, userId)) {
      const lock = simpleFieldLockManager.getFieldLock(roomId, fieldName);
      console.log(`🔒 [Professional Fields] 字段 ${fieldName} 已被 ${lock.lockedBy?.name} 锁定，无法操作`);
      return;
    }

    // 释放该用户的其他锁定字段
    const unlockedFields = simpleFieldLockManager.unlockUserFields(roomId, userId, fieldName);

    // 通知WebSocket其他字段的释放
    unlockedFields.forEach(unlockedField => {
      if (syncManager) {
        try {
          syncManager.syncFieldEditState(parseInt(reportId), unlockedField, 'FIELD_UNLOCK');
        } catch (error) {
          console.warn('🔧 [Professional Fields] 字段释放WebSocket通知失败:', error);
        }
      }
    });

    // 尝试锁定当前字段
    const lockSuccess = simpleFieldLockManager.lockField(roomId, fieldName, collaborationUser);

    if (lockSuccess) {
      console.log(`🔒 [Professional Fields] 成功锁定字段 ${fieldName} (${buttonType}:${value})`);

      // 执行字段更新操作
      try {
        action();

        // 通知WebSocket字段锁定和编辑
        if (syncManager) {
          try {
            syncManager.syncFieldEditState(parseInt(reportId), fieldName, 'FIELD_LOCK');
            syncManager.syncFieldEditState(parseInt(reportId), fieldName, 'FIELD_EDIT');
          } catch (error) {
            console.warn('🔧 [Professional Fields] WebSocket通知失败:', error);
          }
        }

        // 设置自动释放定时器 (3秒后自动释放)
        setTimeout(() => {
          const released = simpleFieldLockManager.unlockField(roomId, fieldName, userId);
          if (released && syncManager) {
            try {
              syncManager.syncFieldEditState(parseInt(reportId), fieldName, 'FIELD_UNLOCK');
            } catch (error) {
              console.warn('🔧 [Professional Fields] 自动释放WebSocket通知失败:', error);
            }
          }
        }, 3000);

      } catch (error) {
        console.error('🔧 [Professional Fields] 字段更新操作失败:', error);
        // 操作失败时释放锁定
        simpleFieldLockManager.unlockField(roomId, fieldName, userId);
      }
    } else {
      console.warn(`🔒 [Professional Fields] 无法锁定字段 ${fieldName}`);
    }
  }

  // 字段编辑事件处理
  function onFieldEdit(fieldName: string, content?: string) {
    console.log('🎯 [Professional Fields] onFieldEdit 被调用:', {
      fieldName,
      content,
      userId: getCurrentUserId(),
      userName: getCurrentUserName()
    });
    const reportId = route.query.reportId?.toString() || '5';
    const roomId = `police-report-${reportId}`;

    // 获取用户ID - 使用和emergency-intake.vue相同的逻辑
    let userId = '';
    let userName = '';

    if (userStore.employeeId) {
      userId = userStore.employeeId.toString();
      userName = userStore.actualName || userStore.userInfo?.actualName || '当前用户';
    } else if (userStore.userInfo?.userId) {
      userId = userStore.userInfo.userId.toString();
      userName = userStore.userInfo.actualName || '当前用户';
    } else if (userStore.userInfo?.employeeId) {
      userId = userStore.userInfo.employeeId.toString();
      userName = userStore.userInfo.actualName || '当前用户';
    } else {
      // 从localStorage获取
      try {
        const storedUser = localStorage.getItem('userInfo') || localStorage.getItem('LOGIN_USER_DATA') || localStorage.getItem('user-token');
        if (storedUser) {
          const user = JSON.parse(storedUser);
          userId = (user.userId || user.employeeId || user.id || '').toString();
          userName = user.actualName || user.name || '当前用户';
        }
      } catch (e) {
        console.warn('🔧 [Professional Fields] 无法从localStorage获取用户信息:', e);
      }
    }

    if (userId) {
      const collaborationUser = {
        id: userId,
        name: userName,
        avatar: userStore.userInfo?.avatar,
        color: generateUserColor(userId)
      };

      fieldCollaborationManager.onFieldEdit(roomId, fieldName, collaborationUser, content);

      // 发送WebSocket编辑状态
      if (syncManager) {
        try {
          syncManager.syncFieldEditState(parseInt(reportId), fieldName, 'FIELD_EDIT');
        } catch (error) {
          console.warn('🔧 [Professional Fields] syncManager.syncFieldEditState 调用失败:', error);
        }
      }
    }
  }

  // 生成用户颜色
  function generateUserColor(userId: string): string {
    const colors = [
      '#1890ff', '#52c41a', '#faad14', '#f5222d',
      '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16'
    ];
    let hash = 0;
    for (let i = 0; i < userId.length; i++) {
      hash = userId.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }

  // 获取当前用户ID的辅助函数
  function getCurrentUserId(): string {
    if (userStore.employeeId) {
      return userStore.employeeId.toString();
    } else if (userStore.userInfo?.userId) {
      return userStore.userInfo.userId.toString();
    } else if (userStore.userInfo?.employeeId) {
      return userStore.userInfo.employeeId.toString();
    }
    return '';
  }

  // 获取当前用户名的辅助函数
  function getCurrentUserName(): string {
    return userStore.actualName || userStore.userInfo?.actualName || '当前用户';
  }

  // 字段属性访问辅助函数 - 兼容两种格式
  function getFieldKey(field: UnifiedField): string {
    return 'fieldKey' in field ? field.fieldKey : field.key;
  }

  function getFieldLabel(field: UnifiedField): string {
    return 'fieldLabel' in field ? field.fieldLabel : field.label;
  }

  function getFieldType(field: UnifiedField): string {
    return 'fieldType' in field ? field.fieldType : field.type;
  }

  function getFieldIcon(field: UnifiedField): string | undefined {
    return 'fieldIcon' in field ? field.fieldIcon : field.icon;
  }

  function getFieldRequired(field: UnifiedField): boolean | undefined {
    return 'isRequired' in field ? field.isRequired : field.required;
  }

  function getFieldOptions(field: UnifiedField): string[] | undefined {
    return 'fieldOptions' in field ? field.fieldOptions : field.options;
  }

  function getFieldQuickOptions(field: UnifiedField): string[] | undefined {
    return 'quickOptions' in field ? field.quickOptions : field.quickOptions;
  }

  function getFieldChildren(field: UnifiedField): UnifiedField[] | undefined {
    return 'children' in field ? field.children : field.fields;
  }

  function getFieldPlaceholder(field: UnifiedField): string | undefined {
    return 'placeholder' in field ? field.placeholder : field.placeholder;
  }

  function getFieldDictCode(field: UnifiedField): string | undefined {
    return 'dictCode' in field ? field.dictCode : field.dictCode;
  }

  // 获取字典选项数据
  function getDictOptions(dictCode: string | undefined) {
    if (!dictCode) return [];
    return dictStore.getDictData(dictCode);
  }

  // 判断字典多选框是否被选中
  function isDictCheckboxSelected(fieldKey: string, dataValue: string): boolean {
    const value = props.modelValue[fieldKey];
    if (!value) return false;
    if (Array.isArray(value)) {
      return value.includes(dataValue);
    }
    return value === dataValue;
  }

  // 切换字典多选框状态
  function toggleDictCheckbox(fieldKey: string, dataValue: string) {
    if (props.disabled) return;

    let currentValue = props.modelValue[fieldKey];
    if (!currentValue) {
      currentValue = [];
    }
    if (!Array.isArray(currentValue)) {
      currentValue = [currentValue];
    }

    const newValue = [...currentValue];
    const index = newValue.indexOf(dataValue);
    if (index > -1) {
      newValue.splice(index, 1);
    } else {
      newValue.push(dataValue);
    }

    updateFieldImmediate(fieldKey, newValue);

    // 触发协作编辑事件 - 对于字典多选点击，传递AUTO_UNLOCK标记自动解锁
    onFieldEdit(fieldKey, 'AUTO_UNLOCK');
  }

  // 更新字段值 - 添加防抖优化性能
  const updateField = debounce((fieldKey: string, value: any) => {
    if (props.disabled) return;
    emit('update:modelValue', { ...props.modelValue, [fieldKey]: value });

    // 触发协作编辑事件 - 对于按钮点击更新，使用AUTO_UNLOCK标记
    onFieldEdit(fieldKey, 'AUTO_UNLOCK');
  }, 50); // 50ms防抖

  // 立即更新字段值（用于按钮点击等需要即时响应的场景）
  function updateFieldImmediate(fieldKey: string, value: any) {
    if (props.disabled) return;
    emit('update:modelValue', { ...props.modelValue, [fieldKey]: value });
  }

  // 简单的防抖函数
  function debounce(func: Function, wait: number) {
    let timeout: NodeJS.Timeout;
    return function executedFunction(...args: any[]) {
      const later = () => {
        clearTimeout(timeout);
        func(...args);
      };
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
    };
  }

  // 调试按钮点击事件（生产环境可移除）
  function debugButtonClick(type: string, fieldKey: string, option?: any) {
    if (process.env.NODE_ENV === 'development') {
      const fieldState = getFieldState(fieldKey);
      console.log(`🎯 [Professional Fields] ${type} 按钮点击:`, {
        type,
        fieldKey,
        option,
        disabled: props.disabled,
        isFieldLockedByOther: isFieldLockedByOther(fieldKey),
        fieldState: fieldState,
        buttonShouldBeDisabled: props.disabled || isFieldLockedByOther(fieldKey),
        timestamp: Date.now()
      });
    }
  }

  // 调试按钮状态
  function debugButtonState(type: string, fieldKey: string, option?: any) {
    const isDisabled = props.disabled || isFieldLockedByOther(fieldKey);
    console.log(`🔍 [Professional Fields] ${type} 按钮状态:`, {
      type,
      fieldKey,
      option,
      'props.disabled': props.disabled,
      'isFieldLockedByOther': isFieldLockedByOther(fieldKey),
      'finalDisabled': isDisabled,
      'fieldState': getFieldState(fieldKey),
      timestamp: Date.now()
    });
    return !isDisabled;
  }

  // 选择选项
  function selectOption(fieldKey: string, option: string) {
    debugButtonClick('selectOption', fieldKey, option);
    if (props.disabled) return;

    console.log('🎯 [Professional Fields] selectOption 被调用:', {
      fieldKey,
      option,
      disabled: props.disabled
    });

    if (option === '无' || option === '0') {
      updateFieldImmediate(fieldKey, 0);
    } else {
      const num = extractNumber(option);
      updateFieldImmediate(fieldKey, num !== null ? num : option);
    }

    // 触发协作编辑事件 - 对于按钮点击，传递AUTO_UNLOCK标记自动解锁
    onFieldEdit(fieldKey, 'AUTO_UNLOCK');
  }

  // 判断选项是否被选中
  function isOptionSelected(fieldKey: string, option: string): boolean {
    const value = props.modelValue[fieldKey];
    return value === option || value === extractNumber(option);
  }

  // 判断多选框是否被选中
  function isCheckboxSelected(fieldKey: string, option: string): boolean {
    const value = props.modelValue[fieldKey];
    if (!value) return false;
    if (Array.isArray(value)) {
      return value.includes(option);
    }
    return value === option;
  }

  // 切换多选框状态
  function toggleCheckbox(fieldKey: string, option: string) {
    if (props.disabled) return;

    console.log('🎯 [Professional Fields] toggleCheckbox 被调用:', {
      fieldKey,
      option,
      disabled: props.disabled
    });

    let currentValue = props.modelValue[fieldKey];
    if (!currentValue) {
      currentValue = [];
    }
    if (!Array.isArray(currentValue)) {
      currentValue = [currentValue];
    }

    const newValue = [...currentValue];
    const index = newValue.indexOf(option);
    if (index > -1) {
      newValue.splice(index, 1);
    } else {
      newValue.push(option);
    }

    updateFieldImmediate(fieldKey, newValue);

    // 触发协作编辑事件 - 对于多选点击，传递AUTO_UNLOCK标记自动解锁
    onFieldEdit(fieldKey, 'AUTO_UNLOCK');
  }

  // 提取数字
  function extractNumber(text: string): number | null {
    const match = text.match(/(\d+)/);
    return match ? parseInt(match[1]) : null;
  }

  // 组件挂载时的调试（生产环境可移除）
  onMounted(() => {
    if (process.env.NODE_ENV === 'development') {
      console.log('🏗️ [Professional Fields] 组件已挂载');
      console.log('🏗️ [Professional Fields] props.disabled:', props.disabled);
      console.log('🏗️ [Professional Fields] props.fields长度:', props.fields.length);
    }

    // 设置 SimpleFieldLockManager 的 WebSocket 同步回调
    const reportId = route.query.reportId?.toString() || '5';
    if (syncManager && syncManager.syncFieldEditState) {
      simpleFieldLockManager.setSyncCallback(syncManager.syncFieldEditState, parseInt(reportId));
      console.log(`🔗 [Professional Fields] 已设置 SimpleFieldLockManager WebSocket 同步回调，reportId: ${reportId}`);
    } else {
      console.warn('🔗 [Professional Fields] syncManager 不可用，无法设置同步回调');
    }
  });
</script>

<style scoped lang="scss">
.professional-fields {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: calc(100vh - 280px);
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(156, 163, 175, 0.5) transparent;
}

.professional-fields::-webkit-scrollbar {
  width: 6px;
}

.professional-fields::-webkit-scrollbar-track {
  background: transparent;
}

.professional-fields::-webkit-scrollbar-thumb {
  background: rgba(156, 163, 175, 0.3);
  border-radius: 3px;
}

.professional-fields::-webkit-scrollbar-thumb:hover {
  background: rgba(156, 163, 175, 0.5);
}

.pro-field {
  background: rgba(249, 250, 251, 0.8);
  backdrop-filter: blur(5px);
  border-radius: 10px;
  padding: 12px;
  border: 1px solid rgba(229, 231, 235, 0.6);
  margin-bottom: 8px;
  transition: all 0.2s ease;
  position: relative; /* 为协作指示器提供定位基准 */
}

.pro-field:hover {
  background: rgba(243, 244, 246, 0.9);
  border-color: rgba(156, 163, 175, 0.4);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.pro-label {
  display: flex;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;

  .field-icon {
    margin-right: 6px;
  }

  .required-mark {
    color: #ef4444;
    margin-left: 4px;
  }
}

/* 紧凑组字段样式 */
.compact-group-field {
  padding: 12px;
  background: #f9fafb;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  margin-bottom: 12px;
}

.compact-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.compact-subfield {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.subfield-label {
  font-size: 13px;
  color: #374151;
  font-weight: 500;
  min-width: 50px;
  flex-shrink: 0;
}

.compact-options {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  flex: 1;
  align-items: center;
}

.compact-btn {
  padding: 6px 10px;
  font-size: 12px;
  background: linear-gradient(135deg, #ffffff 0%, #f9fafb 100%);
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  line-height: 1.2;
  font-weight: 500;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  min-width: 45px;
  max-width: 90px;
  position: relative;
  user-select: none;
  transform: translateZ(0); /* 启用硬件加速 */
  will-change: transform, box-shadow; /* 优化性能 */

  &:hover:not(:disabled) {
    border-color: #3b82f6;
    background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
    color: #1d4ed8;
    transform: translateY(-1px) scale(1.02);
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
  }

  &:active:not(:disabled) {
    transform: translateY(0) scale(0.98);
    transition: all 0.1s ease;
  }

  &.selected {
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    color: white;
    border-color: #1d4ed8;
    font-weight: 600;
    box-shadow:
      0 4px 16px rgba(59, 130, 246, 0.4),
      inset 0 1px 0 rgba(255, 255, 255, 0.2);
    transform: translateY(-1px);
  }

  &.selected:hover {
    background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
    box-shadow:
      0 6px 20px rgba(59, 130, 246, 0.5),
      inset 0 1px 0 rgba(255, 255, 255, 0.3);
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.compact-input {
  width: 100px;
  padding: 4px 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 13px;

  &:disabled {
    background-color: #f9fafb;
    cursor: not-allowed;
  }
}

/* 紧凑多选样式 */
.checkbox-compact {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  margin-bottom: 12px;
}

.compact-checkbox-btn {
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

  &:hover:not(:disabled) {
    border-color: #3b82f6;
    background: #eff6ff;
    color: #1d4ed8;
  }

  &.selected {
    background: #3b82f6;
    color: white;
    border-color: #3b82f6;
    font-weight: 600;
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.check-icon {
  width: 14px;
  height: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

/* 快捷选择按钮 */
.quick-options,
.select-options,
.checkbox-options {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.quick-btn,
.select-btn,
.checkbox-btn {
  padding: 6px 12px;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  max-width: 120px;

  &:hover:not(:disabled) {
    border-color: #3b82f6;
    background: #eff6ff;
    color: #1d4ed8;
    transform: translateY(-1px);
    box-shadow: 0 2px 4px rgba(59, 130, 246, 0.2);
  }

  &.selected {
    background: #3b82f6;
    color: white;
    border-color: #3b82f6;
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.btn-hotkey {
  background: rgba(59, 130, 246, 0.1);
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 10px;
  color: #3b82f6;
}

/* 输入框样式 */
.input-wrapper {
  margin-top: 6px;
}

.preview-input-field,
.preview-textarea-field {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 13px;
  background: #f9fafb;

  &:disabled {
    background-color: #f3f4f6;
    cursor: not-allowed;
  }

  &:focus:not(:disabled) {
    outline: none;
    border-color: #3b82f6;
    background: white;
  }
}

.preview-textarea-field {
  resize: vertical;
  min-height: 60px;
}

/* 选择器包装器样式 */
.select-wrapper,
.checkbox-wrapper,
.compact-select-wrapper {
  margin-top: 6px;
}

.compact-select-wrapper {
  flex: 1;
  min-width: 120px;
}

/* 字段协作锁定样式 */
.field-input-wrapper {
  position: relative;
}

/* 被其他用户锁定的字段 */
.field-input-wrapper.locked-by-other {
  opacity: 0.7;
}

.field-input-wrapper.locked-by-other .preview-input-field,
.field-input-wrapper.locked-by-other .preview-textarea-field,
.field-input-wrapper.locked-by-other input,
.field-input-wrapper.locked-by-other textarea {
  background-color: #fff2e8 !important;
  border-color: #ffad99 !important;
  color: #8c4a1a !important;
  cursor: not-allowed;
}

.field-input-wrapper.locked-by-other .preview-input-field:hover,
.field-input-wrapper.locked-by-other .preview-input-field:focus,
.field-input-wrapper.locked-by-other .preview-textarea-field:hover,
.field-input-wrapper.locked-by-other .preview-textarea-field:focus,
.field-input-wrapper.locked-by-other input:hover,
.field-input-wrapper.locked-by-other input:focus,
.field-input-wrapper.locked-by-other textarea:hover,
.field-input-wrapper.locked-by-other textarea:focus {
  border-color: #ff7a45 !important;
  box-shadow: 0 0 0 2px rgba(255, 122, 69, 0.2) !important;
}

/* 字段锁定遮罩层 */
.field-lock-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 122, 69, 0.1);
  border: 2px solid rgba(255, 122, 69, 0.3);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  z-index: 10;
}

.lock-icon {
  font-size: 12px;
  margin-right: 4px;
}

.lock-text {
  font-size: 11px;
  color: #d46b08;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.8);
}

/* 被锁定容器中的按钮样式 */
.locked-by-other .select-btn:disabled,
.locked-by-other .checkbox-btn:disabled,
.locked-by-other .quick-btn:disabled,
.locked-by-other .compact-checkbox-btn:disabled,
.locked-by-other .compact-btn:disabled {
  background-color: #fff2e8 !important;
  border-color: #ffad99 !important;
  color: #8c4a1a !important;
  cursor: not-allowed;
  opacity: 0.7;
}

/* 被锁定但已选中的按钮样式 - 保持高对比度显示 */
.locked-by-other .select-btn.selected:disabled,
.locked-by-other .checkbox-btn.selected:disabled,
.locked-by-other .quick-btn.selected:disabled,
.locked-by-other .compact-checkbox-btn.selected:disabled,
.locked-by-other .compact-btn.selected:disabled {
  background: linear-gradient(135deg, #ff6b35 0%, #f7931e 100%) !important;
  border-color: #ff6b35 !important;
  color: #ffffff !important;
  opacity: 0.9 !important;
  font-weight: 600;
  box-shadow:
    0 4px 12px rgba(255, 107, 53, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.2) !important;
  transform: none;
}

/* 被锁定选中按钮的特殊标识 */
.locked-by-other .select-btn.selected:disabled::after,
.locked-by-other .checkbox-btn.selected:disabled::after,
.locked-by-other .quick-btn.selected:disabled::after,
.locked-by-other .compact-checkbox-btn.selected:disabled::after,
.locked-by-other .compact-btn.selected:disabled::after {
  content: ' ✓';
  font-weight: bold;
  color: #ffffff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.locked-by-other .select-options,
.locked-by-other .checkbox-options,
.locked-by-other .quick-options,
.locked-by-other .checkbox-compact {
  position: relative;
}

/* 确保遮罩层在按钮组上方 */
.select-wrapper.locked-by-other .field-lock-overlay,
.checkbox-wrapper.locked-by-other .field-lock-overlay,
.quick-options.locked-by-other .field-lock-overlay,
.checkbox-compact.locked-by-other .field-lock-overlay {
  z-index: 15;
}

/* 调试样式：显示锁定状态 */
.debug-locked {
  border: 2px dashed orange !important;
  background-color: rgba(255, 165, 0, 0.1) !important;
}
</style>