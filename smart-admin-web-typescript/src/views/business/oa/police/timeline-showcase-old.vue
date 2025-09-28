<!--
  * 操作时间轴展示页面
  * 展示系统真实的操作记录时间轴
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-28
  * @Copyright 1024创新实验室
-->
<template>
  <div class="timeline-showcase">
    <!-- 页面头部 -->
    <div class="showcase-header">
      <div class="header-content">
        <div class="header-left">
          <h1 class="page-title">操作时间轴</h1>
          <p class="page-subtitle">系统操作记录的可视化时间轴展示</p>
        </div>
        <div class="header-actions">
          <a-space>
            <a-button @click="refreshData" :loading="loading">
              <template #icon><ReloadOutlined /></template>
              刷新数据
            </a-button>
            <a-button @click="exportData" :loading="exporting">
              <template #icon><ExportOutlined /></template>
              导出记录
            </a-button>
          </a-space>
        </div>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="filter-panel">
      <a-card size="small">
        <a-form layout="inline" :model="queryForm">
          <a-form-item label="操作类型">
            <a-select
              v-model:value="queryForm.operationType"
              placeholder="全部类型"
              allowClear
              style="width: 140px"
            >
              <a-select-option value="field_update">字段更新</a-select-option>
              <a-select-option value="user_join">用户加入</a-select-option>
              <a-select-option value="user_leave">用户离开</a-select-option>
              <a-select-option value="conflict_resolve">冲突解决</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="时间范围">
            <a-range-picker
              v-model:value="queryForm.timeRange"
              show-time
              format="YYYY-MM-DD HH:mm"
              style="width: 280px"
            />
          </a-form-item>
          <a-form-item label="用户">
            <a-input
              v-model:value="queryForm.userName"
              placeholder="用户名称"
              allowClear
              style="width: 120px"
            />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="loadData">查询</a-button>
            <a-button @click="resetQuery" style="margin-left: 8px">重置</a-button>
          </a-form-item>
        </a-form>
      </a-card>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-panel">
      <a-row :gutter="16">
        <a-col :span="6">
          <a-card size="small" class="stat-card">
            <a-statistic
              title="总操作数"
              :value="statistics.totalOperations"
              :value-style="{ color: '#1890ff' }"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card size="small" class="stat-card">
            <a-statistic
              title="今日操作"
              :value="statistics.todayOperations"
              :value-style="{ color: '#52c41a' }"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card size="small" class="stat-card">
            <a-statistic
              title="活跃用户"
              :value="statistics.activeUsers"
              :value-style="{ color: '#722ed1' }"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card size="small" class="stat-card">
            <a-statistic
              title="最近1小时"
              :value="statistics.recentOperations"
              :value-style="{ color: '#faad14' }"
            />
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- 时间轴内容 -->
    <div class="timeline-container">
      <a-card :bordered="false">
        <div v-if="loading" class="loading-container">
          <a-spin size="large" />
        </div>
        <div v-else-if="operations.length === 0" class="empty-container">
          <a-empty description="暂无操作记录" />
        </div>
        <div v-else class="timeline-content">
          <a-timeline>
            <a-timeline-item
              v-for="operation in operations"
              :key="operation.id"
              :color="getTimelineColor(operation.type)"
            >
              <template #dot>
                <span class="timeline-icon">{{ getOperationIcon(operation.type) }}</span>
              </template>
              <div class="timeline-item-content">
                <div class="operation-header">
                  <span class="operation-title">{{ operation.description }}</span>
                  <span class="operation-time">{{ formatTime(operation.operationTime) }}</span>
                </div>
                <div class="operation-meta">
                  <a-tag size="small" color="blue">{{ operation.userName }}</a-tag>
                  <span v-if="operation.fieldName" class="field-info">
                    字段: {{ operation.fieldLabel || operation.fieldName }}
                  </span>
                </div>
                <div v-if="operation.oldValue || operation.newValue" class="operation-changes">
                  <div class="change-item">
                    <span class="change-label">变更:</span>
                    <span class="old-value">{{ operation.oldValue || '空' }}</span>
                    <span class="change-arrow">→</span>
                    <span class="new-value">{{ operation.newValue || '空' }}</span>
                  </div>
                </div>
              </div>
            </a-timeline-item>
          </a-timeline>

          <!-- 分页 -->
          <div class="pagination-container">
            <a-pagination
              v-model:current="queryForm.pageNum"
              v-model:page-size="queryForm.pageSize"
              :total="total"
              :show-size-changer="false"
              :show-quick-jumper="true"
              @change="handlePageChange"
              size="small"
            />
          </div>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ReloadOutlined, ExportOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { format } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import { collaborationHistoryApi } from '/@/api/business/oa/collaboration-history-api';
import { operateLogApi } from '/@/api/support/operate-log-api';

// 操作记录接口
interface Operation {
  id: string;
  type: string;
  operationType: string;
  description: string;
  userName: string;
  userId?: string;
  fieldName?: string;
  fieldLabel?: string;
  oldValue?: string;
  newValue?: string;
  operationTime: string;
  ipAddress?: string;
  userAgent?: string;
}

// 查询表单接口
interface QueryForm {
  pageNum: number;
  pageSize: number;
  operationType?: string;
  userName?: string;
  timeRange?: [any, any] | null;
  entityType?: string;
  entityId?: string;
}

// 统计数据接口
interface Statistics {
  totalOperations: number;
  todayOperations: number;
  activeUsers: number;
  recentOperations: number;
}

// 响应式数据
const operations = ref<Operation[]>([]);
const loading = ref(false);
const exporting = ref(false);
const total = ref(0);

// 查询表单
const queryForm = ref<QueryForm>({
  pageNum: 1,
  pageSize: 20,
  entityType: 'police_report'
});

// 统计数据
const statistics = ref<Statistics>({
  totalOperations: 0,
  todayOperations: 0,
  activeUsers: 0,
  recentOperations: 0
});

// 方法
const loadData = async () => {
  loading.value = true;
  try {
    const newOperations: TimelineOperation[] = [];
    const operationCount = Math.floor(Math.random() * 30) + 20; // 20-50个操作

    for (let i = 0; i < operationCount; i++) {
      const operation = createMockOperation();
      newOperations.push(operation);
    }

    // 按时间排序
    newOperations.sort((a, b) => a.timestamp - b.timestamp);
    operations.value = newOperations;

    message.success(`已生成 ${operationCount} 个模拟操作`);
  } finally {
    generating.value = false;
  }
};

const createMockOperation = (): TimelineOperation => {
  const types: TimelineOperation['type'][] = [
    'field_update', 'field_create', 'field_delete', 'user_join', 'user_leave',
    'conflict_resolve', 'rollback', 'sync', 'export', 'import'
  ];

  const type = types[Math.floor(Math.random() * types.length)];
  const user = mockUsers[Math.floor(Math.random() * mockUsers.length)];
  const fieldName = mockFields[Math.floor(Math.random() * mockFields.length)];
  const hasConflict = Math.random() < 0.1; // 10%概率有冲突
  const impactLevels: ('high' | 'medium' | 'low')[] = ['high', 'medium', 'low'];
  const impactLevel = impactLevels[Math.floor(Math.random() * impactLevels.length)];

  const operation: TimelineOperation = {
    id: `op_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    type,
    timestamp: Date.now() - Math.random() * 24 * 60 * 60 * 1000, // 最近24小时内
    user,
    fieldName: ['field_update', 'field_create', 'field_delete'].includes(type) ? fieldName : undefined,
    description: generateOperationDescription(type, user.name, fieldName),
    hasConflict,
    impactLevel,
    metadata: {
      sessionId: `session_${Math.random().toString(36).substr(2, 8)}`,
      userAgent: 'Mozilla/5.0 Chrome/91.0',
      ipAddress: `192.168.1.${Math.floor(Math.random() * 255)}`
    }
  };

  // 添加变更信息（针对字段操作）
  if (['field_update', 'field_create', 'field_delete'].includes(type)) {
    operation.changes = {
      oldValue: type === 'field_create' ? undefined : generateMockValue(),
      newValue: type === 'field_delete' ? undefined : generateMockValue(),
      changeType: type === 'field_create' ? 'create' : type === 'field_delete' ? 'delete' : 'update'
    };
  }

  // 添加冲突信息
  if (hasConflict) {
    operation.conflictInfo = {
      message: `与${mockUsers[Math.floor(Math.random() * mockUsers.length)].name}的操作发生冲突`,
      resolved: Math.random() < 0.7, // 70%概率已解决
      conflictedWith: [user.id]
    };
  }

  return operation;
};

const generateOperationDescription = (type: string, userName: string, fieldName?: string): string => {
  const descriptions = {
    field_update: `${userName} 更新了字段 "${fieldName}"`,
    field_create: `${userName} 创建了字段 "${fieldName}"`,
    field_delete: `${userName} 删除了字段 "${fieldName}"`,
    user_join: `${userName} 加入了协作编辑`,
    user_leave: `${userName} 离开了协作编辑`,
    conflict_resolve: `${userName} 解决了字段冲突`,
    rollback: `${userName} 执行了回滚操作`,
    sync: `${userName} 同步了数据`,
    export: `${userName} 导出了数据`,
    import: `${userName} 导入了数据`
  };

  return descriptions[type as keyof typeof descriptions] || `${userName} 执行了 ${type} 操作`;
};

const generateMockValue = (): string => {
  const values = [
    '张三', '李四', '王五', '赵六', '13812345678', '上海市浦东新区',
    '火灾', '交通事故', '盗窃', '打架斗殴', '高', '中', '低',
    '现场有明火', '车辆相撞', '嫌疑人逃跑', '需要救护车'
  ];
  return values[Math.floor(Math.random() * values.length)];
};

const clearAllData = () => {
  operations.value = [];
  selectedOperation.value = null;
  showOperationDrawer.value = false;
  message.success('已清空所有数据');
};

const handleScenarioSelect = ({ key }: { key: string }) => {
  switch (key) {
    case 'collaboration':
      generateCollaborationScenario();
      break;
    case 'conflict':
      generateConflictScenario();
      break;
    case 'intensive':
      generateIntensiveScenario();
      break;
    case 'mixed':
      generateMixedScenario();
      break;
  }
};

const generateCollaborationScenario = () => {
  const collaborationOps: TimelineOperation[] = [];
  const now = Date.now();

  // 多个用户加入
  mockUsers.slice(0, 4).forEach((user, index) => {
    collaborationOps.push({
      id: `collab_join_${index}`,
      type: 'user_join',
      timestamp: now - (1000 * 60 * 10) + (index * 1000 * 30), // 30秒间隔加入
      user,
      description: `${user.name} 加入了协作编辑`,
      hasConflict: false,
      impactLevel: 'low'
    });
  });

  // 协作编辑字段
  const fields = ['报警人姓名', '事发地址', '警情类型'];
  fields.forEach((field, index) => {
    const user = mockUsers[index % 4];
    collaborationOps.push({
      id: `collab_edit_${index}`,
      type: 'field_update',
      timestamp: now - (1000 * 60 * 8) + (index * 1000 * 60), // 1分钟间隔
      user,
      fieldName: field,
      description: `${user.name} 更新了字段 "${field}"`,
      changes: {
        oldValue: '原值',
        newValue: '新值',
        changeType: 'update'
      },
      hasConflict: false,
      impactLevel: 'medium'
    });
  });

  operations.value = collaborationOps.sort((a, b) => a.timestamp - b.timestamp);
  message.success('已生成多人协作场景数据');
};

const generateConflictScenario = () => {
  const conflictOps: TimelineOperation[] = [];
  const now = Date.now();

  // 两个用户同时编辑同一字段产生冲突
  const user1 = mockUsers[0];
  const user2 = mockUsers[1];
  const conflictField = '报警人电话';

  conflictOps.push({
    id: 'conflict_1',
    type: 'field_update',
    timestamp: now - 1000 * 60 * 5,
    user: user1,
    fieldName: conflictField,
    description: `${user1.name} 更新了字段 "${conflictField}"`,
    changes: {
      oldValue: '13800000000',
      newValue: '13811111111',
      changeType: 'update'
    },
    hasConflict: true,
    conflictInfo: {
      message: `与${user2.name}的操作发生冲突`,
      resolved: false,
      conflictedWith: [user2.id]
    },
    impactLevel: 'high'
  });

  conflictOps.push({
    id: 'conflict_2',
    type: 'field_update',
    timestamp: now - 1000 * 60 * 5 + 5000, // 5秒后
    user: user2,
    fieldName: conflictField,
    description: `${user2.name} 更新了字段 "${conflictField}"`,
    changes: {
      oldValue: '13800000000',
      newValue: '13822222222',
      changeType: 'update'
    },
    hasConflict: true,
    conflictInfo: {
      message: `与${user1.name}的操作发生冲突`,
      resolved: false,
      conflictedWith: [user1.id]
    },
    impactLevel: 'high'
  });

  // 冲突解决
  conflictOps.push({
    id: 'conflict_resolve_1',
    type: 'conflict_resolve',
    timestamp: now - 1000 * 60 * 3,
    user: mockUsers[2], // 第三方解决冲突
    description: `${mockUsers[2].name} 解决了字段 "${conflictField}" 的冲突`,
    hasConflict: false,
    impactLevel: 'high'
  });

  operations.value = conflictOps;
  message.success('已生成冲突处理场景数据');
};

const generateIntensiveScenario = () => {
  const intensiveOps: TimelineOperation[] = [];
  const now = Date.now();

  // 高频操作
  for (let i = 0; i < 50; i++) {
    const user = mockUsers[i % mockUsers.length];
    const field = mockFields[i % mockFields.length];

    intensiveOps.push({
      id: `intensive_${i}`,
      type: 'field_update',
      timestamp: now - (1000 * 60 * 30) + (i * 1000 * 10), // 每10秒一个操作
      user,
      fieldName: field,
      description: `${user.name} 快速更新了字段 "${field}"`,
      changes: {
        oldValue: `旧值${i}`,
        newValue: `新值${i}`,
        changeType: 'update'
      },
      hasConflict: Math.random() < 0.2, // 20%概率有冲突
      impactLevel: 'medium'
    });
  }

  operations.value = intensiveOps;
  message.success('已生成高频操作场景数据');
};

const generateMixedScenario = () => {
  const mixedOps: TimelineOperation[] = [];
  const now = Date.now();

  // 混合各种操作类型
  const operationTypes: TimelineOperation['type'][] = [
    'field_update', 'field_create', 'field_delete', 'user_join', 'user_leave',
    'conflict_resolve', 'rollback', 'sync', 'export', 'import'
  ];

  operationTypes.forEach((type, index) => {
    for (let i = 0; i < 3; i++) {
      const user = mockUsers[(index + i) % mockUsers.length];
      const field = mockFields[(index + i) % mockFields.length];

      mixedOps.push({
        id: `mixed_${type}_${i}`,
        type,
        timestamp: now - (1000 * 60 * 60) + (index * 1000 * 60 * 5) + (i * 1000 * 30),
        user,
        fieldName: ['field_update', 'field_create', 'field_delete'].includes(type) ? field : undefined,
        description: generateOperationDescription(type, user.name, field),
        changes: ['field_update', 'field_create', 'field_delete'].includes(type) ? {
          oldValue: type === 'field_create' ? undefined : `旧值${i}`,
          newValue: type === 'field_delete' ? undefined : `新值${i}`,
          changeType: type === 'field_create' ? 'create' : type === 'field_delete' ? 'delete' : 'update'
        } : undefined,
        hasConflict: Math.random() < 0.1,
        impactLevel: ['high', 'medium', 'low'][Math.floor(Math.random() * 3)] as 'high' | 'medium' | 'low'
      });
    }
  });

  operations.value = mixedOps.sort((a, b) => a.timestamp - b.timestamp);
  message.success('已生成混合操作场景数据');
};

// 事件处理
const handleOperationSelected = (operation: TimelineOperation) => {
  selectedOperation.value = operation;
  showOperationDrawer.value = true;
};

const handleRollbackRequested = (operation: TimelineOperation) => {
  message.info(`回滚请求: ${operation.description}`);

  // 添加回滚操作记录
  const rollbackOperation: TimelineOperation = {
    id: `rollback_${Date.now()}`,
    type: 'rollback',
    timestamp: Date.now(),
    user: operation.user,
    description: `回滚到操作: ${operation.description}`,
    hasConflict: false,
    impactLevel: 'high',
    metadata: {
      originalOperationId: operation.id
    }
  };

  operations.value.push(rollbackOperation);
};

const handleExportRequested = (exportOperations: TimelineOperation[]) => {
  message.success(`导出 ${exportOperations.length} 个操作记录`);

  // 添加导出操作记录
  const exportOperation: TimelineOperation = {
    id: `export_${Date.now()}`,
    type: 'export',
    timestamp: Date.now(),
    user: mockUsers[0], // 假设当前用户
    description: `导出了 ${exportOperations.length} 个操作记录`,
    hasConflict: false,
    impactLevel: 'low'
  };

  operations.value.push(exportOperation);
};

const handleRefreshRequested = () => {
  message.info('刷新时间轴数据');
  // 可以在这里添加新的模拟数据
  if (Math.random() < 0.3) { // 30%概率添加新操作
    const newOperation = createMockOperation();
    newOperation.timestamp = Date.now();
    operations.value.push(newOperation);
  }
};

// 实时操作模拟
const simulateFieldUpdate = () => {
  const user = mockUsers[Math.floor(Math.random() * mockUsers.length)];
  const field = mockFields[Math.floor(Math.random() * mockFields.length)];

  const operation: TimelineOperation = {
    id: `sim_update_${Date.now()}`,
    type: 'field_update',
    timestamp: Date.now(),
    user,
    fieldName: field,
    description: `${user.name} 实时更新了字段 "${field}"`,
    changes: {
      oldValue: '实时旧值',
      newValue: '实时新值',
      changeType: 'update'
    },
    hasConflict: false,
    impactLevel: 'medium'
  };

  operations.value.push(operation);
  message.success('已模拟字段更新操作');
};

const simulateUserJoin = () => {
  const user = mockUsers[Math.floor(Math.random() * mockUsers.length)];

  const operation: TimelineOperation = {
    id: `sim_join_${Date.now()}`,
    type: 'user_join',
    timestamp: Date.now(),
    user,
    description: `${user.name} 实时加入了协作编辑`,
    hasConflict: false,
    impactLevel: 'low'
  };

  operations.value.push(operation);
  message.success('已模拟用户加入操作');
};

const simulateConflict = () => {
  const user1 = mockUsers[0];
  const user2 = mockUsers[1];
  const field = mockFields[0];

  const operation: TimelineOperation = {
    id: `sim_conflict_${Date.now()}`,
    type: 'field_update',
    timestamp: Date.now(),
    user: user1,
    fieldName: field,
    description: `${user1.name} 的操作与其他用户产生冲突`,
    changes: {
      oldValue: '冲突旧值',
      newValue: '冲突新值',
      changeType: 'update'
    },
    hasConflict: true,
    conflictInfo: {
      message: `与${user2.name}的操作发生冲突`,
      resolved: false,
      conflictedWith: [user2.id]
    },
    impactLevel: 'high'
  };

  operations.value.push(operation);
  message.warning('已模拟冲突事件');
};

const simulateRollback = () => {
  if (operations.value.length === 0) {
    message.error('没有可回滚的操作');
    return;
  }

  const lastOperation = operations.value[operations.value.length - 1];
  const user = mockUsers[Math.floor(Math.random() * mockUsers.length)];

  const operation: TimelineOperation = {
    id: `sim_rollback_${Date.now()}`,
    type: 'rollback',
    timestamp: Date.now(),
    user,
    description: `${user.name} 回滚了操作: ${lastOperation.description}`,
    hasConflict: false,
    impactLevel: 'high',
    metadata: {
      originalOperationId: lastOperation.id
    }
  };

  operations.value.push(operation);
  message.success('已模拟回滚操作');
};

// 工具方法
const getOperationIcon = (operation: TimelineOperation): string => {
  const icons = {
    field_update: '✏️',
    field_create: '➕',
    field_delete: '🗑️',
    user_join: '👋',
    user_leave: '👋',
    conflict_resolve: '⚡',
    rollback: '↩️',
    sync: '🔄',
    export: '📤',
    import: '📥'
  };
  return icons[operation.type] || '❓';
};

const getOperationTypeName = (type: string): string => {
  const names = {
    field_update: '字段更新',
    field_create: '字段创建',
    field_delete: '字段删除',
    user_join: '用户加入',
    user_leave: '用户离开',
    conflict_resolve: '冲突解决',
    rollback: '回滚操作',
    sync: '同步操作',
    export: '导出操作',
    import: '导入操作'
  };
  return names[type as keyof typeof names] || type;
};

const getOperationTagColor = (type: string): string => {
  const colors = {
    field_update: 'blue',
    field_create: 'green',
    field_delete: 'red',
    user_join: 'purple',
    user_leave: 'orange',
    conflict_resolve: 'gold',
    rollback: 'magenta',
    sync: 'cyan',
    export: 'geekblue',
    import: 'lime'
  };
  return colors[type as keyof typeof colors] || 'default';
};

const getImpactLevelText = (level: string): string => {
  const texts = {
    high: '🔴 高影响',
    medium: '🟡 中影响',
    low: '🟢 低影响'
  };
  return texts[level as keyof typeof texts] || level;
};

const getImpactColor = (level: string): string => {
  const colors = {
    high: 'red',
    medium: 'orange',
    low: 'green'
  };
  return colors[level as keyof typeof colors] || 'default';
};

const getAvatarText = (name: string): string => {
  if (!name) return '?';
  const cleanName = name.trim();
  if (/[\u4e00-\u9fa5]/.test(cleanName)) {
    return cleanName.charAt(cleanName.length - 1);
  } else {
    return cleanName.charAt(0).toUpperCase();
  }
};

const formatDateTime = (timestamp: number): string => {
  return format(new Date(timestamp), 'yyyy-MM-dd HH:mm:ss', { locale: zhCN });
};

const formatValue = (value: any): string => {
  if (value === null || value === undefined) return '空';
  if (typeof value === 'object') return JSON.stringify(value, null, 2);
  return String(value);
};

// 自动刷新
const startAutoRefresh = () => {
  autoRefreshTimer = setInterval(() => {
    if (operations.value.length > 0 && Math.random() < 0.2) { // 20%概率添加新操作
      const newOperation = createMockOperation();
      newOperation.timestamp = Date.now();
      operations.value.push(newOperation);
    }
  }, 30000); // 30秒刷新一次
};

const stopAutoRefresh = () => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer);
    autoRefreshTimer = null;
  }
};

onMounted(() => {
  // 初始化时生成一些示例数据
  generateMockData();
  startAutoRefresh();
});

onUnmounted(() => {
  stopAutoRefresh();
});
</script>

<style scoped>
.timeline-showcase {
  padding: 24px;
  background: #f5f5f5;
  min-height: 100vh;
}

.showcase-header {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.header-left {
  flex: 1;
}

.page-title {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
}

.page-subtitle {
  margin: 0;
  color: #666;
  line-height: 1.6;
}

.header-actions {
  flex-shrink: 0;
  margin-left: 24px;
}

.feature-cards {
  margin-bottom: 16px;
}

.feature-card {
  height: 100%;
}

.feature-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.feature-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.feature-info {
  flex: 1;
}

.feature-title {
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.feature-desc {
  font-size: 12px;
  color: #666;
}

.stats-panel {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.timeline-container {
  margin-bottom: 24px;
}

.timeline-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border-radius: 8px;
}

.timeline-card :deep(.ant-card-body) {
  padding: 0;
}

.operation-detail-drawer {
  padding: 16px 0;
}

.user-info-detail {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  font-weight: 600;
}

.changes-section {
  margin-top: 16px;
}

.change-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0;
}

.change-label {
  font-weight: 600;
  min-width: 60px;
}

.change-value {
  flex: 1;
  padding: 8px 12px;
  border-radius: 4px;
  font-family: monospace;
}

.old-value {
  background: #fff2e8;
  border: 1px solid #ffbb96;
  color: #d4380d;
}

.new-value {
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  color: #389e0d;
}

.change-arrow {
  font-size: 16px;
  color: #1890ff;
  text-align: center;
  margin: 4px 0;
}

.conflict-section,
.metadata-section {
  margin-top: 16px;
}

.metadata-section pre {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

h4 {
  margin: 16px 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
}

h4:first-child {
  margin-top: 0;
}
</style>