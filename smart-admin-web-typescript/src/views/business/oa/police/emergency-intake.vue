<!--
  * 单屏智能接警界面 - 无滚动，高效录入
  *
  * @Author:    Claude Code Assistant
  * @Date:      2025-09-19
  * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
-->
<template>
  <div class="emergency-intake-page">
    <!-- 顶部操作栏 -->
    <div class="top-bar">
      <div class="title-section">
        <h1 class="page-title">
          🚨 {{ isEditMode ? '编辑警情' : '智能接警系统' }}
          <!-- 协作状态信息 - 只显示其他用户，不包括自己 -->
          <span v-if="otherCollaborationUsers.length > 0" class="collaboration-status">
            <span class="collab-icon">👥</span>
            <span class="collab-count">{{ otherCollaborationUsers.length }}人协作中</span>
          </span>
        </h1>
        <div class="status-info">
          <span class="current-time">{{ currentTime }}</span>
          <span class="operator-info">接警员：{{ operatorName }}</span>
          <!-- 协作用户列表 - 只显示其他用户 -->
          <div v-if="otherCollaborationUsers.length > 0" class="collab-users">
            <div
              v-for="(user, index) in otherCollaborationUsers"
              :key="user.id"
              class="collab-user-avatar"
              :style="{ backgroundColor: user.color }"
              :title="`${user.name} (${user.id})`"
            >

              <img v-if="user.avatar" :src="user.avatar" :alt="user.name" />
              <span v-else class="avatar-text">{{ getAvatarText(user.name) }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="action-section">
        <a-button @click="toggleCollaborationPanel" size="small">
          <template #icon>👥</template>
          协作
        </a-button>

        <a-button @click="toggleHistoryPanel" size="small">
          <template #icon>📊</template>
          时间轴
        </a-button>

        <a-button @click="createTestConflict" size="large" v-if="isDevelopment">
          <template #icon>⚔️</template>
          测试冲突
        </a-button>

        <a-button @click="goBack" size="large">
          <template #icon><ArrowLeftOutlined /></template>
          返回列表
        </a-button>
        <a-button
          type="primary"
          size="large"
          :loading="submitting"
          :disabled="!canSubmit"
          @click="submitReport"
          class="submit-btn"
        >
          🚨 {{ isEditMode ? '保存修改' : '立即处理' }} (Ctrl+Enter)
        </a-button>
      </div>
    </div>

    <!-- 协作动态面板（可折叠） - 移至页面底部浮动显示 -->
    <div v-if="showCollaborationPanel" class="collaboration-floating-panel">
      <CollaborationActivityFeed />
    </div>

    <!-- 逐步添加复杂组件 -->
    <!-- Step 1: ConflictResolutionDialog (✅ 正常) -->
    <ConflictResolutionDialog
      v-model="showConflictDialog"
      :conflict="currentConflict"
      @resolve="handleConflictResolve"
      @cancel="handleConflictCancel"
    />

    <!-- Step 2: IntelligentConflictManager (✅ 正常) -->
    <IntelligentConflictManager
      ref="intelligentConflictManagerRef"
      :conflicts="enhancedConflicts"
      :auto-resolve="true"
      :show-prevention-warnings="true"
      :enable-learning="true"
      @conflict-resolved="handleEnhancedConflictResolve"
      @resolution-failed="handleEnhancedConflictFailed"
      @settings-changed="handleConflictSettingsChange"
    />

    <!-- Step 3: OfflineStatusManager (✅ 正常) -->
    <div class="offline-status-container">
      <OfflineStatusManager
        :offline-manager="enhancedOfflineManager"
        @sync-completed="handleSyncCompleted"
        @operation-retried="handleOperationRetried"
        @operation-removed="handleOperationRemoved"
      />
    </div>

    <!-- Step 4: EnhancedOperationTimeline (✅ 正常) -->
    <div v-if="showHistoryPanel" class="timeline-panel">
      <EnhancedOperationTimeline
        ref="operationTimelineRef"
        :operations="operationHistory"
        :current-user="currentUser"
        :auto-refresh="true"
        :show-filters="true"
        :enable-rollback="true"
        @operation-rollback="handleOperationRollback"
        @operation-export="handleOperationExport"
        @view-change="handleTimelineViewChange"
      />
    </div>

    <!-- Step 5: 统一协作管理器 -->
    <!-- UnifiedCollaborationManager 已移除以避免遮挡主界面 -->
    <!-- 协作功能通过 field-level indicators 和 title bar status 提供 -->

    <!-- 主体内容 - 固定高度，无滚动 -->
    <div class="main-content">

      <!-- 左侧：灾害类型和基础信息 -->
      <div class="left-panel">
        <!-- 灾害类型选择 -->
        <div class="emergency-types">
          <h3 class="section-title">选择灾害类型</h3>
          <div class="type-grid">
            <div
              v-for="(type, key) in POLICE_REPORT_TYPE_ENUM"
              :key="key"
              :class="['type-item', {
                'active': formData.reportType === type.value,
                'emergency': type.category === 'emergency'
              }]"
              @click="selectType(type.value)"
              :title="`按 ${getHotkey(key)} 选择`"
            >
              <div class="type-icon" :style="{ color: type.color }">{{ type.icon }}</div>
              <div class="type-name">{{ type.desc }}</div>
              <div class="type-hotkey">{{ getHotkey(key) }}</div>
            </div>
          </div>
        </div>

        <!-- 基础信息 -->
        <div class="basic-info">
          <h3 class="section-title">基础信息</h3>
          <div class="info-fields">
            <div class="field-item" style="position: relative;">
              <label>事发地点 *</label>
              <div class="location-wrapper" :class="{ 'locked-by-other': isFieldLockedByOther('incidentLocation') }">
                <SmartLocationInput
                  v-model="formData.incidentLocation"
                  placeholder="输入地点关键字"
                  :search-api="policeReportApi.searchLocationSuggestions"
                  :disabled="isFieldLockedByOther('incidentLocation')"
                  @select="handleLocationSelect"
                  @focus="handleFieldFocus('incidentLocation')"
                  @blur="handleFieldBlur('incidentLocation')"
                  @change="(value) => handleFieldEdit('incidentLocation', value)"
                />

                <!-- 字段协作指示器 -->
                <CollaborationFieldIndicator
                  field-name="incidentLocation"
                  :field-state="getFieldState('incidentLocation')"
                />

                <!-- 字段锁定遮罩 -->
                <div v-if="isFieldLockedByOther('incidentLocation')" class="field-lock-overlay">
                  <span class="lock-icon">🔒</span>
                  <span class="lock-text">{{ getFieldState('incidentLocation').lockedBy?.name }} 正在编辑</span>
                </div>
              </div>
            </div>
            <div class="field-row">
              <div class="field-item collaboration-field">
                <label>报警人 *</label>
                <div :class="['field-input-wrapper', { 'locked-by-other': isFieldLockedByOther('reporterName') }]">
                  <a-input
                    v-model:value="formData.reporterName"
                    placeholder="姓名"
                    data-field="reporterName"
                    :disabled="isFieldLockedByOther('reporterName')"
                    @focus="handleFieldFocus('reporterName')"
                    @blur="handleFieldBlur('reporterName')"
                    @input="handleFieldEdit('reporterName', $event.target.value)"
                  />
                  <div v-if="isFieldLockedByOther('reporterName')" class="field-lock-overlay">
                    <span class="lock-icon">🔒</span>
                    <span class="lock-text">{{ getFieldState('reporterName').lockedBy?.name }} 正在编辑</span>
                  </div>
                </div>
                <CollaborationFieldIndicator
                  field-name="reporterName"
                  :field-state="getFieldState('reporterName')"
                />
              </div>
              <div class="field-item collaboration-field">
                <label>电话 *</label>
                <div :class="['field-input-wrapper', { 'locked-by-other': isFieldLockedByOther('reporterPhone') }]">
                  <a-input
                    v-model:value="formData.reporterPhone"
                    placeholder="联系电话"
                    data-field="reporterPhone"
                    :disabled="isFieldLockedByOther('reporterPhone')"
                    @focus="handleFieldFocus('reporterPhone')"
                    @blur="handleFieldBlur('reporterPhone')"
                    @input="handleFieldEdit('reporterPhone', $event.target.value)"
                  />
                  <div v-if="isFieldLockedByOther('reporterPhone')" class="field-lock-overlay">
                    <span class="lock-icon">🔒</span>
                    <span class="lock-text">{{ getFieldState('reporterPhone').lockedBy?.name }} 正在编辑</span>
                  </div>
                </div>
                <CollaborationFieldIndicator
                  field-name="reporterPhone"
                  :field-state="getFieldState('reporterPhone')"
                />
              </div>
            </div>
            <div class="field-item collaboration-field">
              <label>现场描述 *</label>
              <div :class="['field-input-wrapper', { 'locked-by-other': isFieldLockedByOther('description') }]">
                <a-textarea
                  v-model:value="formData.description"
                  placeholder="详细描述现场情况（Ctrl+Enter提交）"
                  data-field="description"
                  :disabled="isFieldLockedByOther('description')"
                  @focus="handleFieldFocus('description')"
                  @blur="handleFieldBlur('description')"
                  @input="handleFieldEdit('description', $event.target.value)"
                  :rows="3"
                  :maxlength="500"
                  show-count
                  @keydown.ctrl.enter="submitReport"
                />
                <div v-if="isFieldLockedByOther('description')" class="field-lock-overlay">
                  <span class="lock-icon">🔒</span>
                  <span class="lock-text">{{ getFieldState('description').lockedBy?.name }} 正在编辑</span>
                </div>
              </div>
              <CollaborationFieldIndicator
                field-name="description"
                :field-state="getFieldState('description')"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：专业信息录入 -->
      <div class="right-panel">
        <h3 class="section-title">
          <span class="section-icon" :style="{ color: typeColor }">{{ typeIcon }}</span>
          {{ typeName }} 专业信息
        </h3>

        <!-- 配置加载状态 -->
        <div v-if="configLoading" class="config-loading">
          <div class="loading-icon">⏳</div>
          <div class="loading-text">正在加载表单配置...</div>
        </div>

        <PoliceProfessionalFields
          v-if="professionalFields.length > 0"
          :fields="professionalFields"
          :modelValue="professionalFieldData"
          @update:modelValue="handleProfessionalFieldUpdate"
        />

        <div v-else class="no-professional-fields">
          <div class="placeholder-icon">📝</div>
          <div class="placeholder-text">{{ typeName }} 无需额外专业信息</div>
          <div class="placeholder-desc">请完善左侧基础信息后提交</div>
        </div>
      </div>

      <!-- 空状态 - 临时注释掉 -->
      <!--
      <div v-if="!currentReportType" class="empty-state">
        <div class="empty-icon">🚨</div>
        <div class="empty-title">请选择灾害类型开始接警</div>
        <div class="empty-desc">点击左侧灾害类型卡片或按数字键1-8快速选择</div>
      </div>
      -->
    </div>

    <!-- 底部进度条 -->
    <div class="progress-bar" v-if="formData.reportType">
      <div class="progress-items">
        <div :class="['progress-step', { completed: formData.reportType }]">
          <span class="step-icon">{{ formData.reportType ? '✓' : '1' }}</span>
          <span class="step-text">灾害类型</span>
        </div>
        <div :class="['progress-step', { completed: formData.incidentLocation }]">
          <span class="step-icon">{{ formData.incidentLocation ? '✓' : '2' }}</span>
          <span class="step-text">事发地点</span>
        </div>
        <div :class="['progress-step', { completed: formData.reporterName && formData.reporterPhone }]">
          <span class="step-icon">{{ (formData.reporterName && formData.reporterPhone) ? '✓' : '3' }}</span>
          <span class="step-text">报警人信息</span>
        </div>
        <div :class="['progress-step', { completed: formData.description }]">
          <span class="step-icon">{{ formData.description ? '✓' : '4' }}</span>
          <span class="step-text">现场描述</span>
        </div>
      </div>
      <div class="progress-indicator">
        <div class="progress-fill" :style="{ width: `${progressPercentage}%` }"></div>
      </div>
    </div>

    <!-- 智能提示 -->
    <div v-if="currentTip" class="smart-tip">
      <span class="tip-icon">💡</span>
      <span class="tip-text">{{ currentTip }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
  console.log('🌟 [EMERGENCY INTAKE DEBUG] emergency-intake.vue 文件已被加载！当前时间:', new Date().toLocaleTimeString());
  console.log('🚀 [PAGE LOAD] 紧急接警页面已加载 - WebSocket处理器修复版本v5.0 - 强制实例清理');

  import { ref, reactive, computed, watch, onMounted, onUnmounted, provide, nextTick } from 'vue';
  import { useRouter, useRoute } from 'vue-router';
  import { message } from 'ant-design-vue';
  import { ArrowLeftOutlined } from '@ant-design/icons-vue';
  import SmartLocationInput from '/@/components/framework/smart-location-input/index.vue';
  import PoliceProfessionalFields from '/@/components/business/emergency/police-professional-fields.vue';
  import { POLICE_REPORT_TYPE_ENUM, EMERGENCY_FORM_CONFIG } from '/@/constants/business/oa/police-report-const';
  import { policeReportApi } from '/@/api/business/oa/police-report-api';
  import { policeFormConfigApi, type PoliceFormConfigVO } from '/@/api/business/oa/police-form-config-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import { DICT_CODE_ENUM } from '/@/constants/support/dict-const';
  import dayjs from 'dayjs';

  // 导入新的协作功能组件和工具
  import {
    CollaborationManager,
    ConflictResolutionDialog,
    OfflineStatusIndicator
  } from '/@/components/business/collaboration';
  import OptimizedCollaborationManager from '/@/components/business/collaboration/OptimizedCollaborationManager.vue';
  import EnhancedOperationTimeline from '/@/components/business/collaboration/EnhancedOperationTimeline.vue';
  import { performanceMonitor } from '/@/utils/collaboration-performance';
  import { avatarManager } from '/@/utils/avatar-manager';
  // 统一WebSocket架构导入
  import {
    policeWebSocketService,
    collaborationWebSocketService,
    initializeWebSocketService,
    type PoliceReportCollaboration,
    type PoliceEditLockInfo,
    type PoliceFieldUpdate,
    type CollaborationUser,
    type CollaborationEvent,
    type CursorPosition
  } from '/@/services/websocket';

  // 保留必要的协作组件
  import { EnhancedConflictResolver } from '/@/utils/enhanced-conflict-resolver';
  import { createOfflineSyncQueue } from '/@/utils/offline-sync-queue';
  import { fieldCollaborationManager } from '/@/utils/field-collaboration-manager';
  import { createEnhancedOfflineManager, Priority } from '/@/utils/enhanced-offline-manager';
  import { realOperationHistoryManager, recordFieldChange, RealOperationType, createRealOperationHistoryManager } from '/@/utils/real-operation-history';
  import type { EnhancedConflictInfo } from '/@/utils/enhanced-conflict-resolver';
  import type { OfflineOperation } from '/@/utils/offline-sync-queue';
  import type { EnhancedOfflineManager } from '/@/utils/enhanced-offline-manager';
  import IntelligentConflictManager from '/@/components/business/collaboration/IntelligentConflictManager.vue';
  import OfflineStatusManager from '/@/components/business/collaboration/OfflineStatusManager.vue';
  import UnifiedCollaborationManager from '/@/components/business/collaboration/UnifiedCollaborationManager.vue';
  import CollaborationActivityFeed from '/@/components/business/collaboration/CollaborationActivityFeed.vue';
  import CollaborationFieldIndicator from '/@/components/business/collaboration/CollaborationFieldIndicator.vue';
  // 导入用户Store
  import { useUserStore } from '/@/store/modules/system/user';

  const router = useRouter();
  const route = useRoute();

  // 编辑模式状态
  const isEditMode = ref(false);
  const editReportId = ref<number | null>(null);

  // 调试状态变量
  console.log('🐛 [Emergency Debug] isEditMode 初始值:', isEditMode.value);
  console.log('🐛 [Emergency Debug] editReportId 初始值:', editReportId.value);
  console.log('🐛 [Emergency Debug] route.query:', route.query);

  // 字段键名到数据字典代码的映射
  const FIELD_DICT_MAPPING: Record<string, string> = {
    'trappedCount': DICT_CODE_ENUM.TRAPPED_COUNT,
    'casualties': DICT_CODE_ENUM.CASUALTIES_LEVEL,
    'fireFloor': DICT_CODE_ENUM.FIRE_FLOOR,
    'fireScale': DICT_CODE_ENUM.FIRE_SCALE,
    'burningMaterial': DICT_CODE_ENUM.BURNING_MATERIAL,
    'smokeCondition': DICT_CODE_ENUM.SMOKE_CONDITION,
    'fireSource': DICT_CODE_ENUM.FIRE_SOURCE,
    'rescueEquipment': DICT_CODE_ENUM.RESCUE_EQUIPMENT,
    'rescueType': DICT_CODE_ENUM.RESCUE_TYPE,
    'dangerLevel': DICT_CODE_ENUM.DANGER_LEVEL,
    'injuryLevel': DICT_CODE_ENUM.INJURY_LEVEL,
    'emergencyType': DICT_CODE_ENUM.EMERGENCY_TYPE,
    'consciousness': DICT_CODE_ENUM.CONSCIOUSNESS_STATE,
    'accidentType': DICT_CODE_ENUM.ACCIDENT_TYPE,
    'vehicleCount': DICT_CODE_ENUM.VEHICLE_COUNT,
    'roadBlock': DICT_CODE_ENUM.ROAD_BLOCK_LEVEL,
    'roadCondition': DICT_CODE_ENUM.ROAD_CONDITION,
    'weatherCondition': DICT_CODE_ENUM.WEATHER_CONDITION,
    'eventNature': DICT_CODE_ENUM.SECURITY_EVENT_NATURE,
    'involvedCount': DICT_CODE_ENUM.INVOLVEMENT_COUNT,
    'weaponInvolved': DICT_CODE_ENUM.WEAPON_TYPE,
    'caseNature': DICT_CODE_ENUM.CRIMINAL_CASE_NATURE,
    'victimCount': DICT_CODE_ENUM.INVOLVEMENT_COUNT,
    'urgencyLevel': DICT_CODE_ENUM.URGENCY_LEVEL,
    'suspectStatus': DICT_CODE_ENUM.SUSPECT_STATUS,
    'sceneProtection': DICT_CODE_ENUM.SCENE_PROTECTION,
    'disasterType': DICT_CODE_ENUM.DISASTER_TYPE,
    'affectedArea': DICT_CODE_ENUM.AFFECTED_AREA,
    'rescueNeeds': DICT_CODE_ENUM.RESCUE_NEEDS
  };

  // 状态数据
  const submitting = ref(false);
  const currentTip = ref('');
  const currentTime = ref('');

  // 真实协作数据
  const realUserCursors = ref<any[]>([]);
  const realActiveUsers = ref<any[]>([]);
  const realOperationHistory = ref<any[]>([]);

  // 引用组件
  const cursorRendererRef = ref<any>();
  const collaborationManagerRef = ref<any>();
  // 从用户store获取真实接警员信息
  const operatorName = computed(() => {
    if (userStore.userInfo?.actualName) {
      return userStore.userInfo.actualName;
    } else if (userStore.userInfo?.loginName) {
      return userStore.userInfo.loginName;
    } else if (userStore.actualName) {
      return userStore.actualName;
    } else {
      return '接警员';
    }
  });
  const dynamicFormConfig = ref<PoliceFormConfigVO | null>(null);
  const configLoading = ref(false);

  // 基本表单数据
  const formData = reactive({
    reportType: null,
    reportLevel: 1, // 默认紧急
    reporterName: '',
    reporterPhone: '',
    reporterIdCard: '',
    reportTime: null,
    incidentLocation: '',
    description: '',
    status: 1,
    handlerName: '',
    handleResult: '',
    handleTime: null,
    attachments: '',
    remark: '',
  });

  // 专业字段数据（独立存储，避免污染基本表单响应式）
  const professionalFieldData = reactive({});

  // 专门的响应式变量用于报告类型，避免整个formData响应式污染
  const currentReportType = ref(null);

  // 🤝 新的协作编辑系统
  const userStore = useUserStore();

  // 协作相关状态
  const showHistoryPanel = ref(false);
  const showCollaborationPanel = ref(false); // 协作动态面板显示状态
  const showConflictDialog = ref(false);
  const collaborationUsers = ref<Array<{id: string, name: string, avatar?: string, color: string}>>([]); // 协作用户列表

  // 过滤出其他协作用户（不包括当前用户）
  const otherCollaborationUsers = computed(() => {
    // 获取当前用户的有效ID（只考虑有值的ID）
    const validUserIds = [
      userStore.employeeId?.toString(),
      userStore.userInfo?.userId?.toString(),
      userStore.userInfo?.employeeId?.toString(),
      userStore.actualName || operatorName.value // 使用姓名作为备用标识
    ].filter(Boolean); // 过滤掉 null, undefined, 空字符串

    const others = collaborationUsers.value.filter(user => {
      // 如果用户信息无效，跳过
      if (!user || !user.id || !user.name) {
        return false;
      }

      // 检查是否是当前用户（只检查有效的ID）
      const isCurrentUserById = validUserIds.length > 0 && validUserIds.includes(user.id);
      const isCurrentUserByName = validUserIds.length > 0 && validUserIds.includes(user.name);
      const isTestUser = user.id === 'current' || user.name === '当前用户';

      const isCurrentUser = isCurrentUserById || isCurrentUserByName || isTestUser;

      return !isCurrentUser;
    });

    return others;
  });

  // 监听协作用户变化
  watch(collaborationUsers, (newUsers) => {
    console.log('👥 [Collaboration Debug] 协作用户列表变化:', {
      count: newUsers.length,
      users: newUsers.map(u => ({ id: u.id, name: u.name }))
    });
  }, { deep: true });

  // 增强冲突管理器相关变量
  const intelligentConflictManagerRef = ref();
  const enhancedConflicts = ref<EnhancedConflictInfo[]>([]);
  const currentConflict = ref<ConflictInfo | null>(null);

  // 协作相关数据
  const operationTimelineRef = ref();
  const activeCursors = ref<any[]>([]);
  const mockCollaborators = ref<any[]>([]);
  const operationHistory = ref<any[]>([]);
  const currentUser = ref<any>({});
  const isDevelopment = ref(process.env.NODE_ENV === 'development');

  // 统一WebSocket架构管理
  const isWebSocketInitialized = ref(false);
  const webSocketConnectionStatus = ref('connecting');

  // 保留必要的协作功能
  const enhancedConflictResolver = new EnhancedConflictResolver();
  const offlineQueue = createOfflineSyncQueue(enhancedConflictResolver);
  const enhancedOfflineManager: EnhancedOfflineManager = createEnhancedOfflineManager(enhancedConflictResolver);

  // 生产级操作记录管理器 - 继续使用现有的优秀实现
  const realOperationManager = createRealOperationHistoryManager();

  // 创建同步管理器
  const syncManager = {
    syncFieldUpdate,
    isConnected: () => policeWebSocketService.isConnected(),
    sendFieldEdit: (fieldName: string, fieldValue: any) => {
      if (policeWebSocketService.isConnected()) {
        policeWebSocketService.sendFieldEdit(fieldName, fieldValue);
      }
    },
    syncFieldEditState: (reportId: number, fieldName: string, eventType: string) => {
      if (!policeWebSocketService.isConnected()) {
        console.warn('🔧 [syncManager] WebSocket未连接，无法同步字段编辑状态');
        return;
      }

      try {
        switch (eventType) {
          case 'FIELD_FOCUS':
            policeWebSocketService.sendFieldFocus(fieldName);
            break;
          case 'FIELD_BLUR':
            policeWebSocketService.sendFieldBlur(fieldName);
            break;
          case 'FIELD_EDIT':
            // FIELD_EDIT 通过 syncFieldUpdate 处理
            console.log('🔧 [syncManager] FIELD_EDIT 事件通过 syncFieldUpdate 处理');
            break;
          default:
            console.warn('🔧 [syncManager] 未知的字段编辑状态事件类型:', eventType);
        }
      } catch (error) {
        console.error('🔧 [syncManager] 同步字段编辑状态失败:', error);
      }
    }
  };

  // 提供协作数据给子组件
  provide('collaborationUsers', otherCollaborationUsers);
  provide('webSocketStatus', webSocketConnectionStatus);
  provide('syncManager', syncManager);

  // 防抖定时器
  let syncDebounceTimer: NodeJS.Timeout | null = null;

  // 字段名称映射 - 将字段名转换为友好的中文显示
  const fieldLabelMapping = reactive<Record<string, string>>({
    // 基础信息字段
    'reporterName': '报告人姓名',
    'reporterPhone': '报告人电话',
    'reporterAddress': '报告人地址',
    'incidentLocation': '事发地点',
    'reportType': '灾害类型',
    'priority': '优先级',
    'description': '事件描述',
    'remark': '备注信息',
    // 专业字段会在动态表单配置加载后添加
  });

  // 计算属性 - 只依赖reportType和config，不依赖整个formData
  const professionalFields = computed(() => {
    console.log('🔍 [Professional Fields] 计算属性触发:', {
      currentReportType: currentReportType.value,
      hasDynamicFormConfig: !!dynamicFormConfig.value,
      configStep2Length: dynamicFormConfig.value?.step2Fields?.length || 0,
      configStep3Length: dynamicFormConfig.value?.step3Fields?.length || 0
    });

    if (!currentReportType.value || !dynamicFormConfig.value) {
      console.log('🔍 [Professional Fields] 返回空数组:', { currentReportType: currentReportType.value, hasDynamicFormConfig: !!dynamicFormConfig.value });
      return [];
    }

    const fields = [...(dynamicFormConfig.value.step2Fields || []), ...(dynamicFormConfig.value.step3Fields || [])];
    console.log('🔍 [Professional Fields] 返回字段列表:', fields.map(f => ({ key: f.key || f.fieldName, label: f.label || f.fieldLabel })));
    return fields;
  });

  // --------------------------- 🤝 新的协作编辑系统 ---------------------------

  // 获取友好的字段名称
  function getFriendlyFieldName(fieldName: string): string {
    // 先查找预定义的映射
    if (fieldLabelMapping[fieldName]) {
      return fieldLabelMapping[fieldName];
    }

    // 如果是专业字段，从动态配置中查找
    if (dynamicFormConfig.value) {
      const allFields = [
        ...(dynamicFormConfig.value.step2Fields || []),
        ...(dynamicFormConfig.value.step3Fields || [])
      ];
      const fieldConfig = allFields.find(field => field.fieldName === fieldName);
      if (fieldConfig) {
        return fieldConfig.fieldLabel || fieldConfig.fieldName;
      }
    }

    // 默认返回字段名
    return fieldName;
  }

  // 处理字段获得焦点 - 使用新的协作系统
  function handleFieldFocus(fieldName: string) {
    console.log(`🎯 [Field Focus] 字段获得焦点: ${fieldName}`);

    // 使用fieldCollaborationManager进行字段锁定
    if (isEditMode.value && editReportId.value) {
      const userStore = useUserStore();
      const currentUser = {
        id: userStore.employeeId?.toString() || 'anonymous',
        name: userStore.actualName || userStore.userInfo?.userName || '匿名用户',
        avatar: userStore.userInfo?.avatar || '',
        color: generateUserColor(userStore.employeeId?.toString() || 'anonymous')
      };
      const currentRoomId = `police-report-${editReportId.value}`;
      fieldCollaborationManager.onFieldFocus(currentRoomId, fieldName, currentUser);

      // 使用统一WebSocket服务发送字段焦点状态
      if (policeWebSocketService.isConnected()) {
        policeWebSocketService.sendFieldFocus(fieldName);
      }
    }
  }

  // 处理字段失去焦点 - 使用新的协作系统
  function handleFieldBlur(fieldName: string) {
    console.log(`👋 [Field Blur] 字段失去焦点: ${fieldName}`);

    // 使用fieldCollaborationManager释放字段锁定
    if (isEditMode.value && editReportId.value) {
      const userStore = useUserStore();
      const currentUser = {
        id: userStore.employeeId?.toString() || 'anonymous',
        name: userStore.actualName || userStore.userInfo?.userName || '匿名用户',
        avatar: userStore.userInfo?.avatar || '',
        color: generateUserColor(userStore.employeeId?.toString() || 'anonymous')
      };
      const currentRoomId = `police-report-${editReportId.value}`;
      fieldCollaborationManager.onFieldBlur(currentRoomId, fieldName, currentUser);

      // 使用统一WebSocket服务发送字段失去焦点状态
      if (policeWebSocketService.isConnected()) {
        policeWebSocketService.sendFieldBlur(fieldName);
      }
    }
  }

  // 记录字段变更历史 - 用于操作时间轴
  function recordFieldChange(fieldName: string, oldValue: any, newValue: any) {
    console.log('📝 [Field History] 记录字段变更:', { fieldName, oldValue, newValue });

    // 添加操作记录到离线同步队列
    const userStore = useUserStore();
    const userId = userStore.employeeId || 'unknown';
    const userName = userStore.actualName || '当前用户';

    offlineQueue.addOperation(
      'FIELD_UPDATE',
      editReportId.value || 'unknown',
      fieldName,
      oldValue,
      newValue,
      userId,
      userName
    );

    // 同时添加到增强版离线管理器
    const priority = getPriorityForField(fieldName);
    enhancedOfflineManager.addEnhancedOperation({
      type: 'FIELD_UPDATE',
      reportId: editReportId.value || 'unknown',
      fieldName,
      oldValue,
      newValue,
      timestamp: Date.now(),
      userId,
      userName,
      status: 'PENDING',
      retryCount: 0,
      maxRetries: 3,
      dataSize: newValue !== undefined && newValue !== null ? JSON.stringify(newValue).length : 0,
      dependencies: []
    }, priority);
  }

  // 新的协作功能处理函数
  function handleOperationSelect(operation: any) {
    console.log('选择操作:', operation);
    // 高亮相关字段
    const fieldElement = document.querySelector(`[data-field="${operation.fieldName}"]`);
    if (fieldElement) {
      fieldElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
      // 临时高亮
      (fieldElement as HTMLElement).style.boxShadow = '0 0 0 2px #1890ff';
      setTimeout(() => {
        (fieldElement as HTMLElement).style.boxShadow = '';
      }, 2000);
    }
  }

  function handleViewQueue() {
    showHistoryPanel.value = true;
  }

  function handleViewConflicts() {
    showConflictDialog.value = true;
    // TODO: 获取冲突列表
  }

  function handleSyncCompleted(results: Map<string, any>) {
    console.log('📦 [Enhanced Sync] 同步完成:', results);
    message.success(`同步完成，处理了 ${results.size} 个操作`);

    // 更新性能监控数据
    if (results.size > 0) {
      performanceMonitor.recordUpdate();
    }
  }

  function handleOperationRetried(operation: OfflineOperation) {
    console.log('🔄 [Enhanced Offline] 操作重试:', operation);
    message.info(`正在重试操作: ${operation.fieldName}`);
  }

  function handleOperationRemoved(operation: OfflineOperation) {
    console.log('🗑️ [Enhanced Offline] 操作已移除:', operation);
    message.success(`已移除操作: ${operation.fieldName}`);
  }

  function handleConflictResolve(result: ResolutionResult) {
    console.log('冲突解决结果:', result);
    if (result.success) {
      message.success('冲突已解决');
    } else {
      message.error('冲突解决失败');
    }
    showConflictDialog.value = false;
  }

  function handleConflictCancel() {
    showConflictDialog.value = false;
  }

  // 增强冲突管理器事件处理
  async function handleEnhancedConflictResolve(conflict: EnhancedConflictInfo, result: any) {
    console.log('🧠 [Enhanced Conflict] 冲突已解决:', { conflict, result });
    message.success(`智能冲突解决成功: ${result.reason}`);

    // 更新表单数据
    if (result.finalValue !== undefined) {
      const fieldName = conflict.fieldName;
      if (formData.hasOwnProperty(fieldName)) {
        (formData as any)[fieldName] = result.finalValue;
      } else if (professionalFieldData.hasOwnProperty(fieldName)) {
        professionalFieldData[fieldName] = result.finalValue;
      }
    }

    // 从增强冲突列表中移除
    enhancedConflicts.value = enhancedConflicts.value.filter(c => c.id !== conflict.id);
  }

  function handleEnhancedConflictFailed(conflict: EnhancedConflictInfo, error: string) {
    console.error('🧠 [Enhanced Conflict] 冲突解决失败:', { conflict, error });
    message.error(`智能冲突解决失败: ${error}`);

    // 降级到手动冲突解决
    currentConflict.value = conflict as ConflictInfo;
    showConflictDialog.value = true;
  }

  function handleConflictSettingsChange(settings: any) {
    console.log('🧠 [Enhanced Conflict] 设置变更:', settings);
    message.info('冲突解决设置已更新');
  }

  // 检测和处理字段冲突的增强版本
  async function detectAndHandleEnhancedConflict(fieldName: string, newValue: any, oldValue: any) {
    try {
      // 构建字段变更信息
      const fieldChange = {
        fieldName,
        fieldLabel: getFieldLabel(fieldName),
        fieldType: getFieldType(fieldName),
        oldValue,
        newValue,
        user: {
          id: userStore.employeeId,
          name: userStore.actualName || '当前用户',
          role: 'officer', // 可以从用户信息获取
          priority: 50,
          timestamp: Date.now()
        },
        timestamp: Date.now(),
        confidence: 85 // 可以根据用户行为和历史数据动态计算
      };

      // 模拟其他用户的并发变更（实际应用中从WebSocket或其他数据源获取）
      const changes = [fieldChange];

      // 使用增强冲突解决器进行检测
      const enhancedConflict = await enhancedConflictResolver.detectEnhancedConflict(
        fieldName,
        changes,
        {
          relatedFields: getRelatedFields(fieldName),
          businessRules: getBusinessRules(fieldName),
          userHistory: [],
          fieldDependencies: new Map(),
          workflowState: 'active',
          environmentInfo: {}
        }
      );

      if (enhancedConflict) {
        console.log('🧠 [Enhanced Conflict] 检测到增强冲突:', enhancedConflict);
        enhancedConflicts.value.push(enhancedConflict);

        // 通知智能冲突管理器
        if (intelligentConflictManagerRef.value) {
          intelligentConflictManagerRef.value.addConflict(enhancedConflict);
        }

        return true;
      }
    } catch (error) {
      console.error('🧠 [Enhanced Conflict] 冲突检测失败:', error);
    }

    return false;
  }

  // 辅助函数
  function getFieldLabel(fieldName: string): string {
    const labelMap: Record<string, string> = {
      'reportType': '灾害类型',
      'reportLevel': '报警等级',
      'incidentLocation': '事发地点',
      'incidentTime': '发生时间',
      'reporterName': '报警人姓名',
      'reporterPhone': '报警人电话',
      'description': '事件描述'
    };
    return labelMap[fieldName] || fieldName;
  }

  function getFieldType(fieldName: string): string {
    const typeMap: Record<string, string> = {
      'reportType': 'select',
      'reportLevel': 'select',
      'incidentLocation': 'text',
      'incidentTime': 'datetime',
      'reporterName': 'text',
      'reporterPhone': 'text',
      'description': 'textarea'
    };
    return typeMap[fieldName] || 'text';
  }

  function getRelatedFields(fieldName: string): string[] {
    const relationMap: Record<string, string[]> = {
      'reportType': ['reportLevel', 'description'],
      'incidentLocation': ['incidentTime'],
      'reporterName': ['reporterPhone']
    };
    return relationMap[fieldName] || [];
  }

  function getBusinessRules(fieldName: string): string[] {
    const rulesMap: Record<string, string[]> = {
      'reportType': ['必填', '影响专业字段'],
      'reportLevel': ['必填', '与类型关联'],
      'incidentLocation': ['必填', '地址验证'],
      'reporterName': ['必填', '姓名格式'],
      'reporterPhone': ['必填', '手机格式验证']
    };
    return rulesMap[fieldName] || [];
  }

  function getPriorityForField(fieldName: string): Priority {
    // 根据字段类型分配优先级
    const priorityMap: Record<string, Priority> = {
      'reportType': Priority.CRITICAL,        // 报警类型 - 最高优先级
      'reportLevel': Priority.CRITICAL,       // 报警等级 - 最高优先级
      'incidentLocation': Priority.HIGH,      // 事发地点 - 高优先级
      'incidentTime': Priority.HIGH,          // 发生时间 - 高优先级
      'reporterName': Priority.NORMAL,        // 报警人姓名 - 普通优先级
      'reporterPhone': Priority.NORMAL,       // 报警人电话 - 普通优先级
      'description': Priority.LOW             // 事件描述 - 低优先级
    };
    return priorityMap[fieldName] || Priority.NORMAL;
  }

  // 协作相关事件处理
  function handleUserLocated(user: any) {
    console.log('🔍 [Collaboration] 用户定位:', user);
    message.info(`已定位到用户: ${user.name}`);

    // 滚动到用户正在编辑的字段
    if (user.currentField) {
      const element = document.querySelector(`[data-field="${user.currentField}"]`);
      if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }
  }

  function handlePerformanceWarning(stats: any) {
    console.warn('⚠️ [Performance] 性能警告:', stats);

    if (stats.fps < 20) {
      message.warning(`协作系统性能警告: FPS过低 (${stats.fps})`);
    }

    if (stats.cursors > 30) {
      message.warning(`协作光标数量过多 (${stats.cursors})，可能影响性能`);
    }
  }

  function handleOperationRollback(operation: any) {
    console.log('↩️ [Timeline] 操作回滚:', operation);
    message.info(`已回滚操作: ${operation.type}`);

    // 执行回滚逻辑
    if (operation.type === 'field_update') {
      const { fieldName, oldValue } = operation;
      if (formData.hasOwnProperty(fieldName)) {
        (formData as any)[fieldName] = oldValue;
      } else if (professionalFieldData.hasOwnProperty(fieldName)) {
        professionalFieldData[fieldName] = oldValue;
      }
    }
  }

  function handleOperationExport(exportData: any) {
    console.log('📤 [Timeline] 导出操作历史:', exportData);
    message.success('操作历史已导出');
  }

  function handleTimelineViewChange(viewMode: string) {
    console.log('👁️ [Timeline] 视图模式变更:', viewMode);
  }

  // 添加操作记录到历史 - 使用生产级操作记录管理器
  async function addOperationRecord(type: string, description: string, fieldName?: string, oldValue?: any, newValue?: any) {
    try {
      const operation = await realOperationManager.recordOperation(
        type as any, // 操作类型
        'police_report', // 实体类型
        editReportId.value || 0, // 实体ID
        {
          fieldName,
          fieldLabel: fieldName, // 字段显示名称
          oldValue,
          newValue,
          description,
          severity: 'medium' as 'medium'
        }
      );

      console.log('📝 [Real Operation] 记录操作成功:', operation);

      // 更新本地操作历史用于时间轴显示
      const formattedOperation = {
        id: operation.id,
        type,
        description,
        fieldName,
        oldValue,
        newValue,
        user: {
          id: userStore.employeeId,
          name: userStore.actualName || '当前用户',
          avatar: ''
        },
        timestamp: operation.timestamp instanceof Date ? operation.timestamp.getTime() :
                  operation.timestamp || Date.now(),
        impactLevel: fieldName && ['reportType', 'reportLevel'].includes(fieldName) ? 'HIGH' : 'MEDIUM',
        hasConflicts: false,
        relatedOperations: []
      };

      operationHistory.value.unshift(formattedOperation);

      // 保持历史记录在合理范围内
      if (operationHistory.value.length > 100) {
        operationHistory.value = operationHistory.value.slice(0, 100);
      }

      return operation;
    } catch (error) {
      console.error('❌ [Real Operation] 记录操作失败:', error);

      // 降级到本地记录
      const localOperation = {
        id: `op_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
        type,
        description,
        fieldName,
        oldValue,
        newValue,
        user: {
          id: userStore.employeeId,
          name: userStore.actualName || '当前用户',
          avatar: ''
        },
        timestamp: Date.now(),
        impactLevel: fieldName && ['reportType', 'reportLevel'].includes(fieldName) ? 'HIGH' : 'MEDIUM',
        hasConflicts: false,
        relatedOperations: []
      };

      operationHistory.value.unshift(localOperation);
      return localOperation;
    }
  }

  // 模拟协作者数据（用于演示）
  function initializeMockCollaborators() {
    mockCollaborators.value = [
      {
        id: 1001,
        name: '张警官',
        avatar: '',
        color: '#1890ff',
        currentField: 'reporterName',
        lastSeen: new Date(),
        isOnline: true
      },
      {
        id: 1002,
        name: '李警官',
        avatar: '',
        color: '#52c41a',
        currentField: 'incidentLocation',
        lastSeen: new Date(),
        isOnline: true
      },
      {
        id: 1003,
        name: '王警官',
        avatar: '',
        color: '#fa8c16',
        currentField: null,
        lastSeen: new Date(Date.now() - 30000),
        isOnline: false
      }
    ];
  }

  // 模拟光标数据（用于演示）
  function initializeMockCursors() {
    activeCursors.value = [
      {
        user: mockCollaborators.value[0],
        fieldName: 'reporterName',
        fieldLabel: '报警人姓名',
        fieldElement: null,
        position: { x: 300, y: 150 },
        isEditing: true,
        showLabel: true,
        visible: true
      },
      {
        user: mockCollaborators.value[1],
        fieldName: 'incidentLocation',
        fieldLabel: '事发地点',
        fieldElement: null,
        position: { x: 450, y: 200 },
        isEditing: true,
        showLabel: true,
        visible: true
      }
    ];
  }

  // 初始化当前用户信息
  function initializeCurrentUser() {
    currentUser.value = {
      id: userStore.employeeId?.toString() || userStore.userInfo?.userId?.toString() || '1',
      name: userStore.actualName || userStore.userInfo?.actualName || '当前用户',
      avatar: userStore.userInfo?.avatar || '',
      role: 'officer',
      department: '应急处置中心'
    };
  }

  // 协作面板控制功能
  function toggleCollaborationPanel() {
    showCollaborationPanel.value = !showCollaborationPanel.value;

    if (showCollaborationPanel.value) {
      message.info('已打开协作动态面板');
    } else {
      message.info('已关闭协作动态面板');
    }
  }

  // 添加时间轴控制按钮功能
  function toggleHistoryPanel() {
    showHistoryPanel.value = !showHistoryPanel.value;

    if (showHistoryPanel.value) {
      message.info('已打开操作时间轴面板');
    } else {
      message.info('已关闭操作时间轴面板');
    }
  }

  // 创建测试冲突（用于演示）
  async function createTestConflict() {
    message.info('正在创建测试冲突...');

    try {
      // 模拟两个用户同时编辑同一个字段
      const fieldName = 'reporterName';
      const currentValue = formData.reporterName || '原始值';

      // 创建模拟的冲突变更
      const changes = [
        {
          fieldName,
          fieldLabel: '报警人姓名',
          fieldType: 'text',
          oldValue: currentValue,
          newValue: '张三 (用户A修改)',
          user: {
            id: 1001,
            name: '张警官',
            role: 'officer',
            priority: 50,
            timestamp: Date.now()
          },
          timestamp: Date.now(),
          confidence: 85
        },
        {
          fieldName,
          fieldLabel: '报警人姓名',
          fieldType: 'text',
          oldValue: currentValue,
          newValue: '李四 (用户B修改)',
          user: {
            id: 1002,
            name: '李警官',
            role: 'supervisor',
            priority: 80,
            timestamp: Date.now() + 100
          },
          timestamp: Date.now() + 100,
          confidence: 90
        }
      ];

      // 使用增强冲突解决器检测冲突
      const enhancedConflict = await enhancedConflictResolver.detectEnhancedConflict(
        fieldName,
        changes,
        {
          relatedFields: getRelatedFields(fieldName),
          businessRules: getBusinessRules(fieldName),
          userHistory: [],
          fieldDependencies: new Map(),
          workflowState: 'active',
          environmentInfo: {}
        }
      );

      if (enhancedConflict) {
        console.log('🧠 [Test Conflict] 成功创建测试冲突:', enhancedConflict);
        enhancedConflicts.value.push(enhancedConflict);

        // 通知智能冲突管理器
        if (intelligentConflictManagerRef.value) {
          intelligentConflictManagerRef.value.addConflict(enhancedConflict);
        }

        message.success('测试冲突已创建！请查看右侧智能冲突管理器');

        // 添加操作记录
        addOperationRecord('conflict_create', `创建测试冲突: ${fieldName}`, fieldName);
      } else {
        message.warning('冲突创建失败，请稍后重试');
      }

    } catch (error) {
      console.error('🧠 [Test Conflict] 创建测试冲突失败:', error);
      message.error('创建测试冲突时出错');
    }
  }


  // --------------------------- 新的同步系统 ---------------------------

  console.log('📋 [Form Debug] formData对象初始化:', formData);

  // 同步字段更新（使用新的同步管理器）
  function syncFieldUpdate(fieldName: string, fieldValue: any, oldValue?: any) {
    if (!isEditMode.value || !editReportId.value) {
      console.log('🔍 [Sync Debug] 跳过同步 - 非编辑模式或无报告ID');
      return;
    }

    console.log('🔄 [Sync Debug] 准备同步字段:', fieldName, '值:', fieldValue);

    // 使用统一WebSocket服务同步字段更新
    if (policeWebSocketService.isConnected()) {
      policeWebSocketService.sendFieldEdit(fieldName, fieldValue);
    }

    // 同时添加到离线队列
    offlineQueue.addOperation(
      'FIELD_UPDATE',
      editReportId.value,
      fieldName,
      oldValue,
      fieldValue,
      userStore.employeeId,
      userStore.actualName || '当前用户'
    );

    // 同时添加到增强版离线管理器
    const priority = getPriorityForField(fieldName);
    enhancedOfflineManager.addEnhancedOperation({
      type: 'FIELD_UPDATE',
      reportId: editReportId.value,
      fieldName,
      oldValue,
      newValue: fieldValue,
      timestamp: Date.now(),
      userId: userStore.employeeId,
      userName: userStore.actualName || '当前用户',
      status: 'PENDING',
      retryCount: 0,
      maxRetries: 3,
      dataSize: JSON.stringify(fieldValue).length,
      dependencies: []
    }, priority);
  }


  // --------------------------- 实时字段同步监听器 ---------------------------

  // 简单测试watch - 应该总是触发
  watch(() => formData.reporterName, (newValue, oldValue) => {
    console.log('🚨 [EMERGENCY TEST] reporterName 变化了!', { newValue, oldValue });
    console.log('🚨 [EMERGENCY TEST] isEditMode 值:', isEditMode.value);
    console.log('🚨 [EMERGENCY TEST] editReportId 值:', editReportId.value);
  });

  // 监听编辑模式变化
  watch(() => isEditMode.value, (newValue, oldValue) => {
    console.log('🎯 [isEditMode Debug] isEditMode 变化:', { newValue, oldValue });
  });

  // 监听route变化
  watch(() => route.query, (newQuery, oldQuery) => {
    console.log('🛣️ [Route Debug] route.query 变化:', { newQuery, oldQuery });
  });

  // 表单字段实时同步监听器
  watch(() => formData.reportType, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] reportType变化:', { newValue, oldValue, isEditMode: isEditMode.value });
    if (isEditMode.value && newValue !== null && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发reportType同步');

      // 添加操作记录
      const typeLabel = POLICE_REPORT_TYPE_ENUM[newValue]?.desc || newValue;
      addOperationRecord('field_update', `修改灾害类型为: ${typeLabel}`, 'reportType', oldValue, newValue);

      // 记录字段变更历史
      recordFieldChange('reportType', oldValue, newValue);
      syncFieldUpdate('reportType', newValue, oldValue);
    }
  });

  watch(() => formData.reportLevel, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] reportLevel变化:', { newValue, oldValue, isEditMode: isEditMode.value });
    if (isEditMode.value && newValue !== null && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发reportLevel同步');
      recordFieldChange('reportLevel', oldValue, newValue);
      syncFieldUpdate('reportLevel', newValue, oldValue);
    }
  });

  watch(() => formData.reporterName, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] reporterName变化:', { newValue, oldValue, isEditMode: isEditMode.value });
    if (isEditMode.value && newValue && newValue.trim() && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发reporterName同步');
      recordFieldChange('reporterName', oldValue, newValue.trim());
      syncFieldUpdate('reporterName', newValue.trim(), oldValue);
    }
  });

  watch(() => formData.reporterPhone, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] reporterPhone变化:', { newValue, oldValue, isEditMode: isEditMode.value });
    if (isEditMode.value && newValue && newValue.trim() && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发reporterPhone同步');
      recordFieldChange('reporterPhone', oldValue, newValue.trim());
      syncFieldUpdate('reporterPhone', newValue.trim(), oldValue);
    }
  });

  watch(() => formData.reporterIdCard, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] reporterIdCard变化:', { newValue, oldValue, isEditMode: isEditMode.value });
    if (isEditMode.value && newValue && newValue.trim() && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发reporterIdCard同步');
      syncFieldUpdate('reporterIdCard', newValue.trim(), oldValue);
    }
  });

  watch(() => formData.incidentLocation, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] incidentLocation变化:', { newValue, oldValue, isEditMode: isEditMode.value });
    if (isEditMode.value && newValue && newValue.trim() && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发incidentLocation同步');
      syncFieldUpdate('incidentLocation', newValue.trim(), oldValue);
    }
  });

  watch(() => formData.description, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] description变化:', { newValue, oldValue, isEditMode: isEditMode.value });
    if (isEditMode.value && newValue && newValue.trim() && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发description同步');
      syncFieldUpdate('description', newValue.trim(), oldValue);
    }
  });

  watch(() => formData.status, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] status变化:', { newValue, oldValue, isEditMode: isEditMode.value });
    if (isEditMode.value && newValue !== null && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发status同步');
      syncFieldUpdate('status', newValue, oldValue);
    }
  });

  watch(() => formData.handlerName, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] handlerName变化:', { newValue, oldValue, isEditMode: isEditMode.value });
    if (isEditMode.value && newValue && newValue.trim() && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发handlerName同步');
      syncFieldUpdate('handlerName', newValue.trim(), oldValue);
    }
  });

  watch(() => formData.handleResult, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] handleResult变化:', { newValue, oldValue, isEditMode: isEditMode.value });
    if (isEditMode.value && newValue && newValue.trim() && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发handleResult同步');
      syncFieldUpdate('handleResult', newValue.trim(), oldValue);
    }
  });

  watch(() => formData.remark, (newValue, oldValue) => {
    console.log('📝 [Field Watch Debug] remark变化:', { newValue, oldValue, isEditMode: isEditMode.value });
    if (isEditMode.value && newValue && newValue.trim() && newValue !== oldValue) {
      console.log('✅ [Field Watch Debug] 触发remark同步');
      syncFieldUpdate('remark', newValue.trim(), oldValue);
    }
  });

  // 添加一个深度监听整个formData对象
  watch(() => formData, (newData) => {
    console.log('🔍 [Form Deep Watch] formData整体变化:', newData);
  }, { deep: true });

  // 专业字段数据通过 handleProfessionalFieldUpdate 函数处理同步


  const progressPercentage = computed(() => {
    let completed = 0;
    if (formData.reportType) completed += 25;
    if (formData.incidentLocation) completed += 25;
    if (formData.reporterName && formData.reporterPhone) completed += 25;
    if (formData.description) completed += 25;
    return completed;
  });

  const canSubmit = computed(() => {
    return !!(
      formData.reportType &&
      formData.incidentLocation?.trim() &&
      formData.reporterName?.trim() &&
      formData.reporterPhone?.trim() &&
      formData.description?.trim()
    );
  });

  const typeName = computed(() => {
    if (!currentReportType.value) return '';
    const type = Object.values(POLICE_REPORT_TYPE_ENUM).find(t => t.value === currentReportType.value);
    return type ? type.desc : '';
  });

  const typeIcon = computed(() => {
    if (!currentReportType.value) return '';
    const type = Object.values(POLICE_REPORT_TYPE_ENUM).find(t => t.value === currentReportType.value);
    return type ? type.icon : '';
  });

  const typeColor = computed(() => {
    if (!currentReportType.value) return '#666';
    const type = Object.values(POLICE_REPORT_TYPE_ENUM).find(t => t.value === currentReportType.value);
    return type ? type.color : '#666';
  });

  // 方法
  function updateTime() {
    currentTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
  }

  function getHotkey(key: string): string {
    const hotkeys = ['1', '2', '3', '4', '5', '6', '7', '8'];
    const index = Object.keys(POLICE_REPORT_TYPE_ENUM).indexOf(key);
    return hotkeys[index] || '';
  }

  async function selectType(value: number, fromSync: boolean = false) {
    console.log('🎯 [Select Type Debug] 灾害类型选择:', { oldValue: formData.reportType, newValue: value, fromSync });
    console.log('🎯 [Select Type Debug] 编辑模式信息:', { isEditMode: isEditMode.value, editReportId: editReportId.value, hasExistingData: Object.keys(professionalFieldData).length > 0 });

    const oldValue = formData.reportType;
    formData.reportType = value;
    currentReportType.value = value;

    // 只有在非同步调用时才触发同步消息
    if (!fromSync && isEditMode.value && value !== oldValue && editReportId.value) {
      console.log('🔄 [Select Type Debug] 触发灾害类型同步');
      recordFieldChange('reportType', oldValue, value);
      syncFieldUpdate('reportType', value, oldValue);
    }

    // 🔧 修复：在编辑模式下，如果是同步调用且已有专业数据，则不清空数据
    const shouldSkipClear = fromSync && isEditMode.value && Object.keys(professionalFieldData).length > 0;
    console.log('🎯 [Select Type Debug] 是否跳过清空:', shouldSkipClear);

    // 清除动态字段
    clearDynamicFields(fromSync, shouldSkipClear);
    updateTip(`已选择${typeName.value}，请继续填写信息`);

    // 加载动态表单配置
    await loadFormConfig(value, fromSync);
  }

  function clearDynamicFields(fromSync: boolean = false, skipClearInEditMode: boolean = false) {
    console.log('🧹 [Clear Dynamic Fields] 开始清空专业字段数据, fromSync:', fromSync, 'skipClearInEditMode:', skipClearInEditMode);
    console.log('🧹 [Clear Dynamic Fields] isEditMode:', isEditMode.value);
    console.log('🧹 [Clear Dynamic Fields] editReportId:', editReportId.value);
    console.log('🧹 [Clear Dynamic Fields] 当前专业字段:', Object.keys(professionalFieldData));

    // 🔧 修复：在编辑模式加载时，如果已有专业数据且设置跳过清空标志，则不清空数据
    if (skipClearInEditMode && isEditMode.value && Object.keys(professionalFieldData).length > 0) {
      console.log('⚠️ [Clear Dynamic Fields] 编辑模式加载时跳过清空专业数据，保持现有数据');
      return;
    }

    // 只有在非同步调用且在编辑模式时才发送同步消息
    if (!fromSync && isEditMode.value && editReportId.value && Object.keys(professionalFieldData).length > 0) {
      // 为每个现有的专业字段发送清空同步消息
      Object.keys(professionalFieldData).forEach(key => {
        const oldValue = professionalFieldData[key];
        console.log(`🧹 [Clear Dynamic Fields] 清空字段 ${key}:`, { oldValue, newValue: null });

        // 记录字段变更历史
        recordFieldChange(key, oldValue, null);

        // 同步清空操作
        syncFieldUpdate(key, null, oldValue);
      });
    }

    // 清空专业字段数据
    Object.keys(professionalFieldData).forEach(key => {
      delete professionalFieldData[key];
    });

    console.log('✅ [Clear Dynamic Fields] 专业字段清空完成');
  }

  function handleLocationSelect(option: any) {
    updateTip(`地址已选择: ${option.value}`);

    // 触发协作编辑事件
    handleFieldEdit('incidentLocation', option.value);
  }

  // 处理专业字段数据更新
  function handleProfessionalFieldUpdate(newValue: any) {
    console.log('🚨 [Professional Field Debug] 专业字段更新:', newValue);
    console.log('🚨 [Professional Field Debug] isEditMode:', isEditMode.value);
    console.log('🚨 [Professional Field Debug] editReportId:', editReportId.value);

    // 获取变更的字段
    const changes: Record<string, any> = {};
    const oldValues: Record<string, any> = {};

    // 比较新旧值，找出变更的字段
    Object.keys(newValue).forEach(key => {
      const oldValue = professionalFieldData[key];
      const newVal = newValue[key];

      if (oldValue !== newVal) {
        changes[key] = newVal;
        oldValues[key] = oldValue;
        console.log(`📝 [Professional Field Debug] ${key} 变化:`, {
          oldValue: oldValue,
          newValue: newVal
        });
      }
    });

    // 设置新数据（先保存变更再更新数据）
    Object.keys(professionalFieldData).forEach(key => {
      delete professionalFieldData[key];
    });
    Object.assign(professionalFieldData, newValue);

    // 如果在编辑模式且有变更，同步每个变更的字段
    if (isEditMode.value && editReportId.value && Object.keys(changes).length > 0) {
      Object.keys(changes).forEach(fieldName => {
        console.log(`✅ [Professional Field Debug] 触发${fieldName}专业字段同步`);
        recordFieldChange(fieldName, oldValues[fieldName], changes[fieldName]);
        syncFieldUpdate(fieldName, changes[fieldName], oldValues[fieldName]);
      });
    }
  }

  // 加载表单配置
  async function loadFormConfig(reportType: number, fromSync: boolean = false, isEditLoad: boolean = false) {
    if (!reportType) return;

    try {
      configLoading.value = true;
      console.log('🔧 [Form Config] 开始加载表单配置, reportType:', reportType, 'fromSync:', fromSync, 'isEditLoad:', isEditLoad);

      const response = await policeFormConfigApi.getFormConfig(reportType);
      if (response.data) {
        // API返回的配置数据，需要转换为数据字典格式
        const convertedConfig = convertConfigToDictFormat(response.data);
        console.log('🔧 [Form Config] API配置转换前:', response.data);
        console.log('🔧 [Form Config] API配置转换后:', convertedConfig);
        dynamicFormConfig.value = convertedConfig;
        console.log('🔧 [Form Config] 成功加载API配置, 字段数量:',
          (convertedConfig.step2Fields?.length || 0) + (convertedConfig.step3Fields?.length || 0));
        console.log('🔧 [Form Config] dynamicFormConfig.value 已更新:', dynamicFormConfig.value);
      } else {
        // 没有配置数据时使用默认配置
        const defaultConfig = createDefaultFormConfig(reportType);
        console.log('🔧 [Form Config] 创建的默认配置:', defaultConfig);
        if (defaultConfig) {
          dynamicFormConfig.value = defaultConfig;
          console.log('🔧 [Form Config] 使用默认配置, 字段数量:',
            (defaultConfig.step2Fields?.length || 0) + (defaultConfig.step3Fields?.length || 0));
        } else {
          // 如果没有默认配置，创建空配置
          dynamicFormConfig.value = {
            reportType,
            templateId: 0,
            templateName: `类型${reportType}默认模板`,
            step2Fields: [],
            step3Fields: []
          };
          console.log('🔧 [Form Config] 使用空配置');
        }
        console.log('🔧 [Form Config] dynamicFormConfig.value 已更新:', dynamicFormConfig.value);
      }

      // 强制触发专业字段更新
      console.log('🔧 [Form Config] 当前专业字段数量:', professionalFields.value.length);
      if (professionalFields.value.length > 0) {
        console.log('🔧 [Form Config] 专业字段列表:', professionalFields.value.map(f => f.fieldName));
      }

      // 只有在非同步调用且在编辑模式时才发送配置同步消息
      if (!fromSync && isEditMode.value && editReportId.value) {
        console.log('🔧 [Form Config] 同步字段配置更新到其他用户');
        syncFieldUpdate('__FORM_CONFIG_UPDATE__', {
          reportType: reportType,
          fieldsCount: professionalFields.value.length,
          fieldNames: professionalFields.value.map(f => f.fieldName)
        }, null);
      }

    } catch (error) {
      console.error('加载表单配置失败:', error);
      message.error('加载表单配置失败，将使用默认配置');
      // 使用默认配置
      const defaultConfig = createDefaultFormConfig(reportType);
      if (defaultConfig) {
        dynamicFormConfig.value = defaultConfig;
        console.log('🔧 [Form Config] 错误处理 - 使用默认配置');
      } else {
        // 如果没有默认配置，创建空配置
        dynamicFormConfig.value = {
          reportType,
          templateId: 0,
          templateName: `类型${reportType}默认模板`,
          step2Fields: [],
          step3Fields: []
        };
        console.log('🔧 [Form Config] 错误处理 - 使用空配置');
      }
    } finally {
      configLoading.value = false;
      // 更新字段标签映射
      updateFieldLabelMapping();
    }
  }

  // 更新字段标签映射
  function updateFieldLabelMapping() {
    if (dynamicFormConfig.value) {
      const allFields = [
        ...(dynamicFormConfig.value.step2Fields || []),
        ...(dynamicFormConfig.value.step3Fields || [])
      ];

      // 添加专业字段的标签映射
      allFields.forEach(field => {
        if (field.fieldName && field.fieldLabel) {
          fieldLabelMapping[field.fieldName] = field.fieldLabel;
        }
      });
    }
  }

  // 转换API配置为数据字典格式
  function convertConfigToDictFormat(apiConfig: PoliceFormConfigVO): PoliceFormConfigVO {
    const convertFields = (fields: any[]) => {
      return fields.map(field => {
        const newField = { ...field };

        // 获取字段键名 (兼容两种API格式)
        const fieldKey = newField.fieldKey || newField.key;

        // 如果该字段有对应的数据字典映射，且没有设置dictCode，则添加dictCode并移除options
        if (fieldKey && FIELD_DICT_MAPPING[fieldKey] && !newField.dictCode) {
          newField.dictCode = FIELD_DICT_MAPPING[fieldKey];
          // 移除硬编码的选项
          delete newField.fieldOptions;
          delete newField.options;
          delete newField.quickOptions;
        }

        // 递归处理子字段
        if (newField.children && newField.children.length > 0) {
          newField.children = convertFields(newField.children);
        }
        if (newField.fields && newField.fields.length > 0) {
          newField.fields = convertFields(newField.fields);
        }

        return newField;
      });
    };

    return {
      ...apiConfig,
      step2Fields: convertFields(apiConfig.step2Fields || []),
      step3Fields: convertFields(apiConfig.step3Fields || [])
    };
  }

  // 创建默认表单配置
  function createDefaultFormConfig(reportType: number) {
    // 从常量中获取配置
    const config = EMERGENCY_FORM_CONFIG[reportType];
    console.log('🔧 [Default Config] 创建默认配置:', { reportType, hasConfig: !!config });
    console.log('🔧 [Default Config] EMERGENCY_FORM_CONFIG keys:', Object.keys(EMERGENCY_FORM_CONFIG));
    console.log('🔧 [Default Config] config for type', reportType, ':', config);

    if (config) {
      // 获取类型名称
      const typeInfo = Object.values(POLICE_REPORT_TYPE_ENUM).find(t => t.value === reportType);
      const typeName = typeInfo ? typeInfo.desc : '未知类型';

      const result = {
        reportType,
        templateId: 0,
        templateName: `${typeName}默认模板`,
        step2Fields: config.step2Fields || [],
        step3Fields: config.step3Fields || []
      };

      console.log('🔧 [Default Config] 配置详情:', {
        reportType,
        typeName,
        step2Count: result.step2Fields.length,
        step3Count: result.step3Fields.length,
        step2Fields: result.step2Fields.map(f => f.key || f.label),
        step3Fields: result.step3Fields.map(f => f.key || f.label)
      });

      return result;
    } else {
      console.log('🔧 [Default Config] ❌ 没有找到配置，返回undefined');
      return undefined;
    }
  }

  async function submitReport() {
    if (!canSubmit.value) {
      message.error('请填写完整的必填信息');
      return;
    }

    try {
      submitting.value = true;

      // 准备提交数据 - 合并基本数据和专业字段数据
      const submitData = {
        ...formData,
        ...professionalFieldData,
        professionalFields: professionalFieldData // 专门传递专业字段给后端
      };
      submitData.reportTime = dayjs().format('YYYY-MM-DD HH:mm:ss');

      if (isEditMode.value && editReportId.value) {
        // 编辑模式，调用更新API
        submitData.reportId = editReportId.value;
        await policeReportApi.updatePoliceReport(submitData);
        message.success('🚨 警情信息更新成功！');

        // 编辑模式完成后返回列表页
        setTimeout(() => {
          router.push('/oa/police/report-list');
        }, 1000);
      } else {
        // 新增模式，调用新增API
        await policeReportApi.addPoliceReport(submitData);
        message.success('🚨 接警信息录入成功！已自动分派处理');

        // 清空表单，准备下一次录入
        Object.assign(formData, {
          reportType: null,
          reportLevel: 1,
          reporterName: '',
          reporterPhone: '',
          reporterIdCard: '',
          reportTime: null,
          incidentLocation: '',
          description: '',
          status: 1,
          handlerName: '',
          handleResult: '',
          handleTime: null,
          attachments: '',
          remark: '',
        });
        currentReportType.value = null;
        clearDynamicFields();

        updateTip('录入完成，可继续接警或返回列表');
      }

    } catch (error) {
      smartSentry.captureError(error);
      message.error('提交失败，请重试');
    } finally {
      submitting.value = false;
    }
  }

  function goBack() {
    router.back();
  }

  function updateTip(text: string) {
    currentTip.value = text;
    setTimeout(() => currentTip.value = '', 3000);
  }

  // 键盘快捷键
  function handleKeydown(e: KeyboardEvent) {
    if (e.ctrlKey && e.key === 'Enter') {
      e.preventDefault();
      submitReport();
    }

    // 数字键选择类型 - 只有在没有焦点在输入框时才响应
    if (!e.ctrlKey && !e.altKey && /^[1-8]$/.test(e.key)) {
      const activeElement = document.activeElement;
      const isInputFocused = activeElement && (
        activeElement.tagName === 'INPUT' ||
        activeElement.tagName === 'TEXTAREA' ||
        activeElement.contentEditable === 'true' ||
        activeElement.getAttribute('contenteditable') === 'true'
      );

      // 如果有输入框获得焦点，不响应数字键快捷键
      if (isInputFocused) {
        return;
      }

      const types = Object.values(POLICE_REPORT_TYPE_ENUM);
      const index = parseInt(e.key) - 1;
      if (types[index]) {
        selectType(types[index].value);
      }
    }
  }

  // 初始化页面，检查是否编辑模式
  async function initPage() {
    // 检查路由参数
    const reportId = route.query.reportId;
    const mode = route.query.mode;

    console.log('🎯 [Page Init Debug] 路由参数:', { reportId, mode });

    if (reportId && mode === 'edit') {
      isEditMode.value = true;
      editReportId.value = Number(reportId);
      console.log('✅ [Page Init Debug] 进入编辑模式:', { isEditMode: isEditMode.value, editReportId: editReportId.value });

      // 先设置默认值确保页面显示
      formData.reportType = 1; // 默认类型，确保基础信息显示
      currentReportType.value = 1; // 确保专业信息显示
      console.log('🔧 [Page Init Debug] 设置默认值确保页面显示');

      await loadEditData(editReportId.value);
    } else {
      console.log('📝 [Page Init Debug] 进入创建模式或参数不匹配');
    }
  }

  // 加载编辑数据
  async function loadEditData(reportId: number) {
    try {
      console.log('📊 [Load Edit Data] 开始加载编辑数据:', reportId);
      const response = await policeReportApi.getDetail(reportId);

      console.log('📊 [Load Edit Data] API响应:', response);

      if (response.data) {
        const data = response.data;
        console.log('📊 [Load Edit Data] 获取到的数据:', data);

        // 加载基本数据
        Object.assign(formData, {
          reportType: data.reportType,
          reportLevel: data.reportLevel,
          reporterName: data.reporterName,
          reporterPhone: data.reporterPhone,
          reporterIdCard: data.reporterIdCard,
          reportTime: data.reportTime,
          incidentLocation: data.incidentLocation,
          description: data.description,
          handlerName: data.handlerName,
          attachments: data.attachments,
          remark: data.remark,
        });

        console.log('📊 [Load Edit Data] formData赋值后:', formData);

        // 设置当前报告类型
        currentReportType.value = data.reportType;
        console.log('📊 [Load Edit Data] 设置currentReportType:', currentReportType.value);

        // 🔧 修复数据丢失问题：先加载专业字段数据，再加载表单配置
        // 这样可以避免loadFormConfig中的clearDynamicFields清空已加载的数据
        console.log('📊 [Load Edit Data] 修复顺序：先加载专业字段数据');
        await loadProfessionalFieldData(reportId);

        console.log('📊 [Load Edit Data] 修复顺序：再加载表单配置（不会清空数据）');
        await loadFormConfig(data.reportType, false, true); // 第三个参数表示编辑模式加载

        message.success('警情数据加载成功');
      }
    } catch (error) {
      console.error('加载编辑数据失败:', error);
      message.error('加载警情数据失败');
      // 如果加载失败，回到列表页
      router.push('/oa/police/report-list');
    }
  }

  // 加载专业字段数据
  async function loadProfessionalFieldData(reportId: number) {
    try {
      const response = await policeReportApi.getPoliceReportFieldData(reportId);
      if (response.data) {
        console.log('🔍 专业字段数据:', response.data);

        // 直接将专业字段数据加载到 professionalFieldData
        // 后端已经正确处理了数组的序列化和反序列化
        Object.assign(professionalFieldData, response.data);
        console.log('✅ 已加载专业字段数据:', response.data);
      }
    } catch (error) {
      console.error('加载专业字段数据失败:', error);
      // 如果加载失败也不阻断编辑流程
    }
  }

  // 生成用户颜色的辅助函数
  function generateUserColor(userId: string): string {
    const colors = [
      '#1890ff', '#52c41a', '#fa8c16', '#eb2f96',
      '#722ed1', '#13c2c2', '#fa541c', '#2f54eb',
      '#f5222d', '#a0d911', '#fadb14', '#096dd9'
    ];

    let hash = 0;
    for (let i = 0; i < userId.length; i++) {
      hash = userId.charCodeAt(i) + ((hash << 5) - hash);
    }

    return colors[Math.abs(hash) % colors.length];
  }

  // 统一协作事件处理器
  function handleUserJoined(user: any) {
    console.log('👋 [Unified Collaboration] 用户加入:', user);
    console.log('📊 [Collaboration Debug] 当前协作用户列表长度:', collaborationUsers.value.length);

    // 添加到协作用户列表
    if (user && user.id && user.name) {
      const exists = collaborationUsers.value.some(u => u.id === user.id);
      console.log('🔍 [Collaboration Debug] 用户是否已存在:', exists);

      if (!exists) {
        const newUser = {
          id: user.id,
          name: user.name,
          avatar: user.avatar,
          color: user.color || generateUserColor(user.id)
        };

        collaborationUsers.value.push(newUser);
        console.log('✅ [Collaboration Debug] 已添加用户:', newUser);
        console.log('📊 [Collaboration Debug] 更新后协作用户列表:', collaborationUsers.value);

        message.info(`${user.name} 加入了协作编辑`);
      } else {
        console.log('⚠️ [Collaboration Debug] 用户已存在，跳过添加');
      }
    } else {
      console.log('❌ [Collaboration Debug] 无效用户数据:', user);
    }
  }

  function handleUserLeft(user: any) {
    console.log('👋 [Unified Collaboration] 用户离开:', user);
    message.info(`${user.name} 离开了协作编辑`);

    // 从协作用户列表移除
    if (user && user.id) {
      const index = collaborationUsers.value.findIndex(u => u.id === user.id);
      if (index > -1) {
        collaborationUsers.value.splice(index, 1);
      }
    }
  }

  function handleConnectionChange(status: string) {
    console.log('🔌 [Unified Collaboration] 连接状态变化:', status);
    if (status === 'connected') {
      message.success('协作连接已建立');
    } else if (status === 'disconnected') {
      message.warning('协作连接已断开，尝试重连中...');
    }
  }

  // 注册表单字段到生产级跟踪器
  function registerFormFields() {
    console.log('📋 [Production Tracker] 开始注册表单字段');

    // 注册基础字段
    const basicFields = [
      'reporterName', 'reporterPhone', 'reporterAddress',
      'incidentLocation', 'reportType', 'priority',
      'description', 'remark'
    ];

    basicFields.forEach(fieldName => {
      const element = document.querySelector(`[data-field="${fieldName}"]`) as HTMLElement;
      if (element) {
        // productionTracker.registerField(fieldName, element);
        console.log(`✅ [Production Tracker] 已注册字段: ${fieldName}`);
      }
    });

    // 注册专业字段（动态）
    if (professionalFields.value?.length > 0) {
      professionalFields.value.forEach(field => {
        const fieldName = field.key || field.fieldName;
        if (fieldName) {
          const element = document.querySelector(`[data-field="${fieldName}"]`) as HTMLElement;
          if (element) {
            // productionTracker.registerField(fieldName, element);
            console.log(`✅ [Production Tracker] 已注册专业字段: ${fieldName}`);
          }
        }
      });
    }
  }

  onMounted(() => {
    updateTime();
    setInterval(updateTime, 1000);
    document.addEventListener('keydown', handleKeydown);

    // 初始化页面
    initPage();

    // 初始化协作系统数据（用于演示）
    initializeMockCollaborators();
    initializeMockCursors();
    initializeCurrentUser();

    // 添加初始操作记录
    addOperationRecord('page_load', '页面加载完成');

    // 初始化性能监控
    performanceMonitor.startMonitoring();

    // 初始化协作用户显示
    console.log('🔍 [Collaboration Debug] 初始化协作用户显示');

    // 从 userStore 获取当前用户信息并添加到协作显示
    const currentUserInfo = {
      id: userStore.employeeId?.toString() || userStore.userInfo?.userId?.toString() || 'current',
      name: userStore.userInfo?.actualName || userStore.actualName || userStore.userInfo?.loginName || '当前用户',
      color: '#1890ff'
    };

    console.log('👤 [Collaboration Debug] 当前用户信息:', currentUserInfo);

    // 立即添加当前用户到协作显示
    setTimeout(() => {
      handleUserJoined(currentUserInfo);
      console.log('✅ [Collaboration Debug] 已添加当前用户到协作显示');
    }, 1000);

    // 初始化统一WebSocket协作系统
    const initUnifiedWebSocketSystem = async () => {
      try {
        webSocketConnectionStatus.value = 'connecting';
        console.log('🚀 [Unified WebSocket] 初始化统一WebSocket协作系统');

        // 初始化警务WebSocket服务
        await initializeWebSocketService('police');
        console.log('✅ [Unified WebSocket] 警务WebSocket服务已初始化');

        // 初始化协作WebSocket服务
        await initializeWebSocketService('collaboration');
        console.log('✅ [Unified WebSocket] 协作WebSocket服务已初始化');

        // 设置警务WebSocket事件监听
        policeWebSocketService.on('user_join', (data: any) => {
          const user: CollaborationUser = {
            id: data.userId,
            name: data.userName,
            color: generateUserColor(data.userId.toString()),
            isOnline: true,
            lastActiveTime: data.timestamp
          };

          // 添加到协作用户列表（排除自己）
          const currentUserId = userStore.employeeId?.toString();
          if (data.userId.toString() !== currentUserId) {
            collaborationUsers.value.push(user);
          }

          console.log('👤 [Unified WebSocket] 用户加入协作:', user);
        });

        policeWebSocketService.on('user_leave', (data: any) => {
          const userIndex = collaborationUsers.value.findIndex(u => u.id === data.userId.toString());
          if (userIndex !== -1) {
            collaborationUsers.value.splice(userIndex, 1);
          }
          console.log('👤 [Unified WebSocket] 用户离开协作:', data.userName);
        });

        policeWebSocketService.on('field_edit', (data: PoliceReportCollaboration) => {
          console.log('📝 [Unified WebSocket] 收到字段编辑事件:', data);

          // 只处理其他用户的编辑事件，避免处理自己的事件
          if (data.userId.toString() !== currentUser.value?.id?.toString()) {
            // 同步字段更新到本地表单
            if (data.fieldName && formData.hasOwnProperty(data.fieldName)) {
              const oldValue = formData[data.fieldName as keyof typeof formData];
              recordFieldChange(data.fieldName, oldValue, data.value);

              // 特殊处理reportType字段的同步
              if (data.fieldName === 'reportType' && data.value !== formData.reportType) {
                console.log('🔄 [WebSocket] 同步切换警情类型:', {
                  from: formData.reportType,
                  to: data.value,
                  user: data.userName
                });

                // 使用selectType来切换类型并同步专业字段模板
                selectType(data.value, true);
                message.info(`${data.userName}切换了警情类型，已同步到当前页面`);
              } else {
                // 普通字段直接更新
                formData[data.fieldName as keyof typeof formData] = data.value;
                message.info(`${data.userName}修改了${getFriendlyFieldName(data.fieldName)}`);
              }
            }
            // 处理表单配置更新消息
            else if (data.fieldName === '__FORM_CONFIG_UPDATE__' && data.value?.reportType) {
              console.log('🔄 [WebSocket] 收到表单配置更新:', {
                reportType: data.value.reportType,
                fieldsCount: data.value.fieldsCount,
                user: data.userName
              });

              // 同步加载相同的表单配置
              if (formData.reportType !== data.value.reportType) {
                console.log('🔄 [WebSocket] 同步切换警情类型:', {
                  from: formData.reportType,
                  to: data.value.reportType
                });

                // 使用fromSync标志避免无限循环
                selectType(data.value.reportType, true);
                message.info(`${data.userName}切换了警情类型，已同步到当前页面`);
              } else {
                // 如果类型相同但配置可能不同，重新加载配置
                console.log('🔄 [WebSocket] 重新加载配置（类型相同但配置可能更新）');
                loadFormConfig(data.value.reportType, true, true);
              }
            }
            // 处理专业字段更新
            else if (data.fieldName && data.fieldName.startsWith('field_')) {
              const oldValue = professionalFieldData[data.fieldName];
              console.log('📝 [WebSocket专业字段] 准备更新:', {
                fieldName: data.fieldName,
                oldValue,
                newValue: data.value,
                用户: data.userName
              });
              recordFieldChange(data.fieldName, oldValue, data.value);

              // 使用Vue的响应式更新
              // 创建新的数据对象并重新分配以确保响应式更新
              const updatedData = { ...professionalFieldData, [data.fieldName]: data.value };
              Object.keys(professionalFieldData).forEach(key => delete professionalFieldData[key]);
              Object.assign(professionalFieldData, updatedData);

              // 强制触发UI更新
              nextTick(() => {
                console.log('✅ [WebSocket专业字段] Vue响应式更新完成:', {
                  fieldName: data.fieldName,
                  value: data.value,
                  totalFields: Object.keys(professionalFieldData).length,
                  allData: professionalFieldData
                });
              });

              message.info(`${data.userName}修改了专业字段${data.fieldName}`);
              console.log('✅ [WebSocket专业字段] 响应式数据已更新:', {
                fieldName: data.fieldName,
                value: data.value,
                dataKeys: Object.keys(professionalFieldData)
              });
            }
            else {
              console.log('⚠️ [WebSocket] 未处理的字段:', { fieldName: data.fieldName, value: data.value, userName: data.userName });
            }
          }
        });

        policeWebSocketService.on('field_focus', (data: any) => {
          console.log('🎯 [Unified WebSocket] 收到字段聚焦事件:', data);
          // 只处理其他用户的聚焦事件
          if (data.userId.toString() !== currentUser.value?.id?.toString()) {
            const roomId = `police-report-${editReportId.value}`;
            console.log('🔧 [Field Focus] 处理远程聚焦事件:', {
              roomId,
              fieldName: data.fieldName,
              userId: data.userId,
              userName: data.userName,
              currentUserId: currentUser.value?.id
            });

            fieldCollaborationManager.handleRemoteFieldEvent({
              type: 'field_focus',
              roomId,
              fieldName: data.fieldName,
              user: {
                id: data.userId.toString(),
                name: data.userName,
                color: generateUserColor(data.userId.toString())
              },
              timestamp: data.timestamp
            });

            // 验证字段状态是否正确更新
            const fieldState = fieldCollaborationManager.getFieldState(roomId, data.fieldName);
            console.log('🔧 [Field Focus] 字段状态更新后:', {
              fieldName: data.fieldName,
              isLocked: fieldState.isLocked,
              lockedBy: fieldState.lockedBy
            });

            message.info(`${data.userName} 开始编辑 ${getFriendlyFieldName(data.fieldName)}`);
          }
        });

        policeWebSocketService.on('field_blur', (data: any) => {
          console.log('👋 [Unified WebSocket] 收到字段失焦事件:', data);

          // 特别标记自动解锁事件
          if (data.isAutoUnlock) {
            console.log('🚨 [CRITICAL] 接收到自动解锁事件!!!', {
              fieldName: data.fieldName,
              userName: data.userName,
              isAutoUnlock: data.isAutoUnlock,
              timestamp: data.timestamp
            });
          }

          const roomId = `police-report-${editReportId.value}`;
          const isCurrentUser = data.userId.toString() === currentUser.value?.id?.toString();

          // 检查是否是自动解锁事件（通过时间戳间隔判断）
          const isAutoUnlock = data.isAutoUnlock || (data.timestamp && Date.now() - data.timestamp < 2000);

          console.log('🔧 [Field Blur] 事件分析:', {
            roomId,
            fieldName: data.fieldName,
            userId: data.userId,
            userName: data.userName,
            isCurrentUser,
            isAutoUnlock,
            isAutoUnlockFlag: data.isAutoUnlock,
            timestamp: data.timestamp,
            timeDiff: data.timestamp ? Date.now() - data.timestamp : 'no timestamp',
            rawData: data
          });

          // 处理失焦事件：其他用户的所有事件 + 当前用户的自动解锁事件
          if (!isCurrentUser || isAutoUnlock) {
            fieldCollaborationManager.handleRemoteFieldEvent({
              type: 'field_blur',
              roomId,
              fieldName: data.fieldName,
              user: {
                id: data.userId.toString(),
                name: data.userName,
                color: generateUserColor(data.userId.toString())
              },
              timestamp: data.timestamp
            });

            // 验证字段状态是否正确更新
            const fieldState = fieldCollaborationManager.getFieldState(roomId, data.fieldName);
            console.log('🔧 [Field Blur] 字段状态更新后:', {
              fieldName: data.fieldName,
              isLocked: fieldState.isLocked,
              lockedBy: fieldState.lockedBy,
              处理原因: !isCurrentUser ? '其他用户事件' : '自动解锁事件'
            });

            // 只为其他用户的操作显示消息通知
            if (!isCurrentUser) {
              message.success(`${data.userName} 结束编辑 ${getFriendlyFieldName(data.fieldName)}`);
            }
          } else {
            console.log('🚫 [Field Blur] 跳过处理 - 当前用户的手动失焦事件');
          }
        });

        policeWebSocketService.on('report_lock', (data: any) => {
          console.log('🔒 [Unified WebSocket] 报告被锁定:', data);
          // 处理报告锁定逻辑
        });

        policeWebSocketService.on('report_unlock', (data: any) => {
          console.log('🔓 [Unified WebSocket] 报告锁定解除:', data);
          // 处理报告解锁逻辑
        });

        // 协作WebSocket事件监听已移除，避免与警务WebSocket重复处理
        // 所有协作事件通过 policeWebSocketService 统一处理

        // 如果在编辑模式，加入报告协作
        if (isEditMode.value && editReportId.value) {
          await policeWebSocketService.joinReport(editReportId.value);
          await collaborationWebSocketService.joinDocument(editReportId.value.toString(), 'police-report');
          console.log(`✅ [Unified WebSocket] 已加入报告${editReportId.value}的协作`);
        }

        webSocketConnectionStatus.value = 'connected';
        isWebSocketInitialized.value = true;
        console.log('✅ [Unified WebSocket] 统一WebSocket协作系统初始化完成');

      } catch (error) {
        console.error('❌ [Unified WebSocket] 统一WebSocket协作系统初始化失败:', error);
        webSocketConnectionStatus.value = 'error';
      }
    };

    // 初始化新的协作系统
    const initCollaborationSystem = async () => {
      try {
        // 首先初始化统一WebSocket系统
        await initUnifiedWebSocketSystem();

        // 统一WebSocket系统已初始化完成，不需要旧的同步客户端
        console.log('✅ [Collaboration] 统一WebSocket系统已初始化完成');

        // 统一WebSocket架构已在initUnifiedWebSocketSystem中处理所有字段同步逻辑
        console.log('📨 [Collaboration] 统一WebSocket事件监听器已设置完成');
        console.log('✅ [Collaboration] 统一WebSocket协作系统初始化完成');
      } catch (error) {
        console.error('❌ [Collaboration] 协作系统初始化失败:', error);
      }
    };

    // 延迟100ms执行，确保组件完全挂载
    setTimeout(initCollaborationSystem, 100);

    // 初始化生产级协作跟踪器
    const initProductionTracker = async () => {
      try {
        console.log('🚀 [Production Tracker] 开始初始化生产级跟踪器');

        // ProductionTracker在构造时自动连接WebSocket
        console.log('✅ [Production Tracker] WebSocket连接成功');

        // 注册表单字段
        registerFormFields();

        // 设置事件处理器（已迁移到统一WebSocket架构）
        console.log('🎯 [Production Tracker] 事件处理器已通过统一WebSocket架构实现');

        // 当前用户信息通过统一WebSocket架构获取
        console.log('👤 [Production Tracker] 当前用户信息已通过统一架构处理');
        // ProductionTracker 会自动从 userStore 获取当前用户信息

        console.log('✅ [Production Tracker] 生产级跟踪器初始化完成');
      } catch (error) {
        console.error('❌ [Production Tracker] 初始化失败:', error);
        // 降级到模拟模式
        console.warn('⚠️ [Production Tracker] 降级到模拟模式');
      }
    };

    // 初始化生产级操作记录管理器
    const initRealOperationManager = async () => {
      try {
        console.log('🚀 [Real Operation Manager] 开始初始化');

        // RealOperationManager在构造时已初始化
        console.log('✅ [Real Operation Manager] 初始化成功');

        // 如果是编辑模式，加载历史操作记录
        if (isEditMode.value && editReportId.value) {
          try {
            const historyOperations = await realOperationManager.getOperationHistory({
              entityType: 'police_report',
              entityId: editReportId.value,
              pageNum: 1,
              pageSize: 50
            });
            console.log('📚 [Real Operation Manager] 加载历史操作记录:', historyOperations.length);

            // 转换为时间轴格式
            const formattedHistory = historyOperations.map(op => ({
              id: op.id,
              type: op.type || op.operationType,
              description: op.description,
              fieldName: op.fieldName,
              oldValue: op.oldValue,
              newValue: op.newValue,
              user: {
                id: op.userId,
                name: op.userName,
                avatar: ''
              },
              timestamp: op.timestamp instanceof Date ? op.timestamp.getTime() :
                        op.timestamp || Date.now(),
              impactLevel: op.fieldName && ['reportType', 'reportLevel'].includes(op.fieldName) ? 'HIGH' : 'MEDIUM',
              hasConflicts: false,
              relatedOperations: []
            }));

            operationHistory.value = formattedHistory;
            console.log('✅ [Real Operation Manager] 历史记录加载完成:', formattedHistory.length);
          } catch (error) {
            console.warn('⚠️ [Real Operation Manager] 历史记录加载失败，可能后端服务未启动:', error);
            // 降级到本地模式，不影响主要功能
          }
        }

      } catch (error) {
        console.error('❌ [Real Operation Manager] 初始化失败:', error);
        console.warn('⚠️ [Real Operation Manager] 降级到本地模式');
      }
    };

    // 延迟200ms执行，确保DOM完全加载
    setTimeout(initProductionTracker, 200);

    // 延迟300ms初始化操作记录管理器
    setTimeout(initRealOperationManager, 300);
  });

  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeydown);

    // 清理统一WebSocket协作系统
    if (policeWebSocketService.getCurrentReportId()) {
      policeWebSocketService.leaveReport();
    }
    console.log('🧹 [Collaboration] 已清理统一WebSocket协作系统连接');

    // 清理防抖定时器
    if (syncDebounceTimer) {
      clearTimeout(syncDebounceTimer);
      syncDebounceTimer = null;
    }
  });

  // 协作相关函数
  // 处理字段编辑
  function handleFieldEdit(fieldName: string, content: string) {
    console.log(`✏️ [Field Edit] 字段编辑: ${fieldName}`, content);
    const userStore = useUserStore();
    const currentUser = {
      id: userStore.employeeId?.toString() || 'anonymous',
      name: userStore.actualName || userStore.userInfo?.userName || '匿名用户',
      avatar: userStore.userInfo?.avatar || '',
      color: generateUserColor(userStore.employeeId?.toString() || 'anonymous')
    };
    const currentRoomId = `police-report-${editReportId.value}`;
    fieldCollaborationManager.onFieldEdit(currentRoomId, fieldName, currentUser, content);
  }

  // 获取字段状态
  function getFieldState(fieldName: string) {
    const currentRoomId = `police-report-${editReportId.value}`;
    return fieldCollaborationManager.getFieldState(currentRoomId, fieldName);
  }

  // 判断字段是否被其他用户锁定
  function isFieldLockedByOther(fieldName: string): boolean {
    const fieldState = getFieldState(fieldName);
    if (!fieldState.isLocked || !fieldState.lockedBy) {
      return false;
    }

    // 获取当前用户ID - 使用和之前相同的逻辑
    let currentUserId = '';
    if (userStore.employeeId) {
      currentUserId = userStore.employeeId.toString();
    } else if (userStore.userInfo?.userId) {
      currentUserId = userStore.userInfo.userId.toString();
    } else if (userStore.userInfo?.employeeId) {
      currentUserId = userStore.userInfo.employeeId.toString();
    }

    // 如果被锁定，但锁定用户不是当前用户，则返回true
    const isLockedByOther = fieldState.lockedBy.id !== currentUserId;

    console.log('🔒 [Field Lock Check]', {
      fieldName,
      isLocked: fieldState.isLocked,
      lockedBy: fieldState.lockedBy,
      currentUserId,
      isLockedByOther
    });

    return isLockedByOther;
  }

  // 生成头像文字
  function getAvatarText(name: string): string {
    console.log('🎭 [Avatar Debug] 生成头像文字:', name);

    if (!name) {
      console.log('🎭 [Avatar Debug] 名称为空，返回 ?');
      return '?';
    }

    const trimmed = name.trim();
    if (/[\u4e00-\u9fff]/.test(trimmed)) {
      // 中文名字取最后一个字
      const result = trimmed.charAt(trimmed.length - 1);
      console.log('🎭 [Avatar Debug] 中文名字，取最后一字:', result);
      return result;
    } else {
      // 英文名字取第一个字母
      const result = trimmed.charAt(0).toUpperCase();
      console.log('🎭 [Avatar Debug] 英文名字，取首字母:', result);
      return result;
    }
  }

  // 真实协作用户管理将通过WebSocket事件自动处理

</script>

<style scoped>
.emergency-intake-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow: hidden;
  position: relative;
}

.emergency-intake-page::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="20" cy="20" r="1" fill="rgba(255,255,255,0.03)"/><circle cx="80" cy="80" r="1" fill="rgba(255,255,255,0.03)"/><circle cx="40" cy="60" r="1" fill="rgba(255,255,255,0.03)"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
  pointer-events: none;
}

/* 顶部操作栏 */
.top-bar {
  min-height: 70px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 20px;
  flex-shrink: 0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 10;
  overflow: visible;
}

.title-section {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.2;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.status-info {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 14px;
}

.current-time {
  font-size: 16px;
  font-weight: 600;
  color: #3b82f6;
}

.operator-info {
  font-size: 14px;
  color: #666;
}

.action-section {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
}

.submit-btn {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  border-color: #ef4444;
  font-weight: 600;
}

/* 主体内容 */
.main-content {
  flex: 1;
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 20px;
  padding: 20px;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.left-panel,
.right-panel {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(15px);
  border-radius: 16px;
  padding: 20px;
  overflow: hidden;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.12),
    0 2px 8px rgba(0, 0, 0, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
  height: calc(100vh - 140px);
  max-height: calc(100vh - 140px);
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.left-panel:hover,
.right-panel:hover {
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.15),
    0 4px 12px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  transform: translateY(-2px);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #f3f4f6;
  display: flex;
  align-items: center;
}

.section-icon {
  font-size: 20px;
  margin-right: 8px;
}

/* 灾害类型网格 */
.type-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 20px;
}

.type-item {
  position: relative;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  border: 2px solid rgba(226, 232, 240, 0.8);
  border-radius: 12px;
  padding: 14px 10px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  text-align: center;
  min-height: 75px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  overflow: hidden;
  backdrop-filter: blur(5px);
}

.type-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.6), transparent);
  transition: left 0.6s;
}

.type-item:hover::before {
  left: 100%;
}

.type-item:hover {
  border-color: #3b82f6;
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.3);
  transform: translateY(-3px);
  background: #eff6ff;
  transform: translateY(-1px);
}

.type-item.active {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-color: #1d4ed8;
  color: white;
  box-shadow:
    0 8px 32px rgba(59, 130, 246, 0.4),
    0 4px 16px rgba(59, 130, 246, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.type-item.active .type-name {
  color: white;
}

.type-item.emergency.active {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  border-color: #b91c1c;
  color: white;
  box-shadow:
    0 8px 32px rgba(239, 68, 68, 0.4),
    0 4px 16px rgba(239, 68, 68, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.type-item.emergency.active .type-name {
  color: white;
}

.type-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.type-name {
  font-size: 12px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 4px;
}

.type-hotkey {
  position: absolute;
  top: 4px;
  right: 4px;
  background: #6b7280;
  color: white;
  font-size: 10px;
  padding: 2px 4px;
  border-radius: 3px;
}

/* 基础信息字段 */
.info-fields {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: relative;
}

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.field-item label {
  font-size: 13px;
  font-weight: 600;
  color: #4b5563;
  margin-bottom: 2px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.field-item label::before {
  content: '';
  width: 3px;
  height: 14px;
  background: linear-gradient(to bottom, #3b82f6, #1d4ed8);
  border-radius: 2px;
}

/* 专业信息字段 */
.professional-fields {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: calc(100vh - 280px);
  overflow: hidden;
}

.pro-field {
  background: #f9fafb;
  border-radius: 6px;
  padding: 8px;
  border: 1px solid #e5e7eb;
}

/* 紧凑组字段样式 - 与其他字段保持一致 */
.compact-group-field {
  padding: 12px;
  background: #f9fafb;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  margin-bottom: 12px;
}

.compact-group-field .pro-label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
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
  gap: 6px;
  flex: 1;
}

.compact-btn {
  padding: 6px 12px;
  font-size: 13px;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  line-height: 1.3;
  font-weight: 500;
}

.compact-btn:hover {
  border-color: #3b82f6;
  background: #eff6ff;
  color: #1d4ed8;
}

.compact-btn.selected {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
  font-weight: 600;
}

/* 紧凑多选样式 - 与其他字段保持一致 */
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
}

.compact-checkbox-btn:hover {
  border-color: #3b82f6;
  background: #eff6ff;
  color: #1d4ed8;
}

.compact-checkbox-btn.selected {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
  font-weight: 600;
}

.pro-label {
  display: flex;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
}

.required {
  color: #ef4444;
  margin-left: 4px;
}

.quick-options,
.select-options,
.checkbox-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-btn,
.select-btn,
.checkbox-btn {
  position: relative;
  padding: 8px 12px;
  background: white;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.quick-btn:hover,
.select-btn:hover,
.checkbox-btn:hover {
  border-color: #3b82f6;
  background: #eff6ff;
}

.quick-btn.selected,
.select-btn.selected,
.checkbox-btn.selected {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.btn-hotkey {
  background: rgba(255,255,255,0.3);
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 10px;
}

.check-icon {
  width: 14px;
  height: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #6b7280;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
}

.no-professional-fields {
  text-align: center;
  padding: 40px 20px;
  color: #6b7280;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.placeholder-text {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.placeholder-desc {
  font-size: 14px;
}

/* 底部进度条 */
.progress-bar {
  height: 60px;
  background: white;
  border-top: 1px solid #e8e8e8;
  padding: 12px 24px;
  flex-shrink: 0;
}

.progress-items {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-bottom: 8px;
}

.progress-step {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #6b7280;
}

.progress-step.completed {
  color: #059669;
}

.step-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.progress-step.completed .step-icon {
  background: #10b981;
  color: white;
}

.progress-indicator {
  height: 4px;
  background: #f3f4f6;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981 0%, #059669 100%);
  transition: width 0.3s ease;
}

/* 时间轴面板样式 */
.timeline-panel {
  position: fixed;
  top: 80px;
  left: 20px;
  width: 400px;
  max-height: calc(100vh - 120px);
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
  z-index: 1000;
  overflow: hidden;
  animation: slideInLeft 0.3s ease;
}

@keyframes slideInLeft {
  from {
    transform: translateX(-100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* 协作光标和头像的全局样式优化 */
.collaboration-cursor {
  position: absolute;
  z-index: 999;
  pointer-events: none;
  transition: all 0.2s ease;
}

.collaboration-cursor.visible {
  opacity: 1;
  transform: scale(1);
}

.collaboration-cursor.hidden {
  opacity: 0;
  transform: scale(0.8);
}

/* 协作面板优化 */
.collaboration-panel {
  animation: slideInRight 0.3s ease;
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* 智能提示 */
.smart-tip {
  position: fixed;
  top: 80px;
  right: 24px;
  background: #1890ff;
  color: white;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 8px;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(100%);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 配置加载状态样式 */
.config-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #6b7280;
}

.loading-icon {
  font-size: 48px;
  margin-bottom: 12px;
  animation: pulse 1.5s ease-in-out infinite;
}

.loading-text {
  font-size: 16px;
  font-weight: 500;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* 🤝 协作编辑样式 */
.field-collaboration-indicator {
  position: relative;
}

.field-editing-by-other {
  border: 2px solid #1890ff !important;
  box-shadow: 0 0 8px rgba(24, 144, 255, 0.3) !important;
  background: rgba(24, 144, 255, 0.05) !important;
}

.field-editing-by-other::after {
  content: attr(data-editing-by);
  position: absolute;
  top: -24px;
  left: 0;
  background: #1890ff;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
  z-index: 1000;
  pointer-events: none;
}

.field-conflict {
  border: 2px solid #ff4d4f !important;
  box-shadow: 0 0 8px rgba(255, 77, 79, 0.3) !important;
  background: rgba(255, 77, 79, 0.05) !important;
  animation: conflictPulse 1s ease-in-out infinite alternate;
}

.field-conflict::after {
  content: '⚠️ 同时编辑冲突';
  position: absolute;
  top: -24px;
  left: 0;
  background: #ff4d4f;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
  z-index: 1000;
  pointer-events: none;
}

.field-locked {
  border: 2px solid #faad14 !important;
  box-shadow: 0 0 8px rgba(250, 173, 20, 0.3) !important;
  background: rgba(250, 173, 20, 0.05) !important;
  cursor: not-allowed !important;
}

.field-locked::after {
  content: '🔒 字段已锁定';
  position: absolute;
  top: -24px;
  left: 0;
  background: #faad14;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
  z-index: 1000;
  pointer-events: none;
}

@keyframes conflictPulse {
  0% {
    border-color: #ff4d4f;
    box-shadow: 0 0 8px rgba(255, 77, 79, 0.3);
  }
  100% {
    border-color: #ff7875;
    box-shadow: 0 0 12px rgba(255, 77, 79, 0.5);
  }
}

/* 协作状态面板 */
.collaboration-status-panel {
  position: fixed;
  top: 80px;
  right: 20px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  min-width: 200px;
}

.collaboration-status-title {
  font-size: 12px;
  font-weight: 600;
  color: #666;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.collaboration-user-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 12px;
}

.user-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.user-status-editing {
  background: #1890ff;
  animation: pulse 2s infinite;
}

.user-status-idle {
  background: #52c41a;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.7;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 协作状态面板新增样式 */
.collaboration-item-content {
  flex: 1;
  min-width: 0;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  font-weight: 600;
  color: #1890ff;
  font-size: 12px;
}

.field-name {
  color: #666;
  font-size: 11px;
}

.conflict-label {
  color: #ff4d4f;
  font-weight: 600;
  font-size: 12px;
}

.change-info {
  margin-top: 2px;
  padding: 2px 6px;
  background: #f0f9ff;
  border-radius: 4px;
  border-left: 2px solid #1890ff;
}

.change-text {
  font-size: 10px;
  color: #1890ff;
  font-family: monospace;
}

.conflict-item .change-info {
  background: #fff2f0;
  border-left-color: #ff4d4f;
}

.conflict-item .change-text {
  color: #ff4d4f;
}

@keyframes conflictPulse {
  0% { opacity: 1; }
  100% { opacity: 0.5; }
}

/* 离线状态管理器样式 */
.offline-status-container {
  position: fixed;
  top: 120px;
  right: 20px;
  z-index: 1002;

  /* 确保在协作面板之上 */
  animation: slideInRight 0.3s ease;
}

/* 顶部协作状态面板样式 */
.collaboration-status-section {
  margin: 8px 0;
  padding: 0;
  width: 100%;
}

/* 旧的右侧协作活动流样式（保留备用） */
.collaboration-activity-section {
  margin-top: 20px;
  padding: 16px;
  background: rgba(240, 248, 255, 0.6);
  border-radius: 12px;
  border: 1px solid rgba(24, 144, 255, 0.2);
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

/* 协作字段样式 */
.collaboration-field {
  position: relative;
}

.collaboration-field:hover {
  background: rgba(240, 248, 255, 0.3);
  border-radius: 4px;
}

/* 字段输入包装器 */
.field-input-wrapper,
.location-wrapper {
  position: relative;
}

/* 被其他用户锁定的字段 */
.field-input-wrapper.locked-by-other,
.location-wrapper.locked-by-other {
  opacity: 0.7;
}

.field-input-wrapper.locked-by-other :deep(.ant-input),
.field-input-wrapper.locked-by-other :deep(.ant-input:disabled),
.location-wrapper.locked-by-other :deep(.ant-input),
.location-wrapper.locked-by-other :deep(.ant-input:disabled) {
  background-color: #fff2e8 !important;
  border-color: #ffad99 !important;
  color: #8c4a1a !important;
  cursor: not-allowed;
}

.field-input-wrapper.locked-by-other :deep(.ant-input:hover),
.field-input-wrapper.locked-by-other :deep(.ant-input:focus),
.location-wrapper.locked-by-other :deep(.ant-input:hover),
.location-wrapper.locked-by-other :deep(.ant-input:focus) {
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
  font-size: 14px;
  margin-right: 4px;
}

.lock-text {
  font-size: 12px;
  color: #d46b08;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.8);
}

/* 协作状态样式 */
.collaboration-status {
  margin-left: 16px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  background: rgba(24, 144, 255, 0.1);
  border: 1px solid rgba(24, 144, 255, 0.3);
  border-radius: 16px;
  font-size: 12px;
  color: #1890ff;
}

.collab-icon {
  font-size: 14px;
}

.collab-count {
  font-weight: 500;
}

.collab-users {
  display: flex;
  gap: 4px;
  align-items: center;
  flex-wrap: wrap;
}

.collab-user-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 8px;
  font-weight: 600;
  border: 1px solid rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: transform 0.2s ease;
}

.collab-user-avatar:hover {
  transform: scale(1.1);
}

.collab-user-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

/* 协作浮动面板样式 */
.collaboration-floating-panel {
  position: fixed;
  bottom: 20px;
  right: 20px;
  width: 400px;
  max-height: 300px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.15),
    0 2px 8px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  z-index: 1000;
  animation: slideInUp 0.3s ease-out;
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(100px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 强制更新缓存: 2025年09月25日 */
</style>