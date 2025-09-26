<!--
  * 智能冲突解决管理器组件
  *
  * 功能特性：
  * 1. 实时冲突监测和展示
  * 2. 多种解决策略可视化
  * 3. 智能推荐和自动解决
  * 4. 冲突历史和学习统计
  * 5. 预防性冲突警告
  *
  * @Author: Claude Code Assistant
  * @Date: 2025-09-24
  * @Copyright 1024创新实验室
-->
<template>
  <div class="intelligent-conflict-manager">
    <!-- 冲突概览面板 -->
    <div v-if="hasActiveConflicts || showHistory" class="conflict-overview-panel">
      <div class="panel-header">
        <div class="header-title">
          <span class="title">🧠 智能冲突管理</span>
          <a-badge
            :count="activeConflicts.length"
            :number-style="{ backgroundColor: getConflictLevelColor() }"
          />
        </div>

        <div class="header-actions">
          <a-tooltip title="自动解决所有可解决的冲突">
            <a-button
              type="primary"
              size="small"
              :disabled="!hasAutoResolvableConflicts"
              @click="autoResolveAll"
              :loading="resolving"
            >
              <template #icon>🤖</template>
              智能解决
            </a-button>
          </a-tooltip>

          <a-tooltip title="查看解决历史">
            <a-button
              type="text"
              size="small"
              @click="showHistory = !showHistory"
            >
              <template #icon>📊</template>
            </a-button>
          </a-tooltip>

          <a-tooltip title="设置">
            <a-button
              type="text"
              size="small"
              @click="showSettings = !showSettings"
            >
              <template #icon>⚙️</template>
            </a-button>
          </a-tooltip>
        </div>
      </div>

      <!-- 活跃冲突列表 -->
      <div v-if="activeConflicts.length > 0" class="active-conflicts">
        <div class="section-title">
          <span>活跃冲突 ({{ activeConflicts.length }})</span>
          <a-select
            v-model:value="filterLevel"
            size="small"
            style="width: 120px"
            placeholder="筛选级别"
          >
            <a-select-option value="">全部</a-select-option>
            <a-select-option value="CRITICAL">严重</a-select-option>
            <a-select-option value="HIGH">高</a-select-option>
            <a-select-option value="MEDIUM">中</a-select-option>
            <a-select-option value="LOW">低</a-select-option>
          </a-select>
        </div>

        <div class="conflicts-list">
          <div
            v-for="conflict in filteredConflicts"
            :key="conflict.id"
            class="conflict-item"
            :class="[
              `risk-${conflict.riskLevel.toLowerCase()}`,
              { 'auto-resolvable': conflict.autoResolvable }
            ]"
          >
            <!-- 冲突头部信息 -->
            <div class="conflict-header">
              <div class="conflict-info">
                <div class="field-info">
                  <span class="field-name">{{ conflict.fieldLabel || conflict.fieldName }}</span>
                  <a-tag :color="getConflictTypeColor(conflict.type)" size="small">
                    {{ getConflictTypeLabel(conflict.type) }}
                  </a-tag>
                  <a-tag :color="getRiskLevelColor(conflict.riskLevel)" size="small">
                    {{ conflict.riskLevel }}
                  </a-tag>
                </div>

                <div class="conflict-meta">
                  <span class="timestamp">{{ formatTime(conflict.timestamp) }}</span>
                  <span class="changes-count">{{ conflict.changes.length }} 个变更</span>
                </div>
              </div>

              <div class="conflict-actions">
                <a-tooltip v-if="conflict.autoResolvable" title="可自动解决">
                  <a-button
                    type="primary"
                    size="small"
                    @click="resolveConflict(conflict)"
                    :loading="resolvingIds.has(conflict.id)"
                  >
                    🤖 自动解决
                  </a-button>
                </a-tooltip>

                <a-button
                  size="small"
                  @click="showConflictDetail(conflict)"
                >
                  详情
                </a-button>

                <a-dropdown placement="bottomRight">
                  <a-button size="small" type="text">
                    <template #icon>⋯</template>
                  </a-button>
                  <template #overlay>
                    <a-menu @click="(e) => handleConflictAction(conflict, e.key)">
                      <a-menu-item key="manual">手动解决</a-menu-item>
                      <a-menu-item key="ignore">忽略冲突</a-menu-item>
                      <a-menu-item key="rollback">回滚变更</a-menu-item>
                      <a-menu-item key="preview">预览结果</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </div>
            </div>

            <!-- 冲突评分可视化 -->
            <div class="conflict-scores">
              <div class="score-item" v-for="(score, key) in getDisplayScores(conflict.score)" :key="key">
                <span class="score-label">{{ getScoreLabel(key) }}</span>
                <div class="score-bar">
                  <div
                    class="score-fill"
                    :style="{
                      width: `${score}%`,
                      backgroundColor: getScoreColor(score)
                    }"
                  ></div>
                </div>
                <span class="score-value">{{ score }}</span>
              </div>
            </div>

            <!-- 推荐策略 -->
            <div class="recommended-strategy">
              <span class="strategy-label">推荐策略:</span>
              <a-tag color="blue">{{ getStrategyLabel(conflict.suggestedStrategy) }}</a-tag>
              <span class="confidence">置信度: {{ Math.round(conflict.predictedOutcome?.confidence || 0) }}%</span>

              <div v-if="conflict.alternativeStrategies.length > 0" class="alternatives">
                <span class="alt-label">替代方案:</span>
                <a-tag
                  v-for="strategy in conflict.alternativeStrategies.slice(0, 2)"
                  :key="strategy"
                  size="small"
                  @click="previewStrategy(conflict, strategy)"
                  class="clickable-tag"
                >
                  {{ getStrategyLabel(strategy) }}
                </a-tag>
              </div>
            </div>

            <!-- 变更详情预览 -->
            <div class="changes-preview">
              <div
                v-for="(change, index) in conflict.changes.slice(0, 2)"
                :key="index"
                class="change-item"
              >
                <div class="user-info">
                  <span class="user-name">{{ change.user.name }}</span>
                  <span class="user-role">({{ change.user.role }})</span>
                  <span class="change-time">{{ formatTime(change.timestamp) }}</span>
                </div>
                <div class="value-change">
                  <span class="old-value">"{{ truncateValue(change.oldValue) }}"</span>
                  <span class="arrow">→</span>
                  <span class="new-value">"{{ truncateValue(change.newValue) }}"</span>
                </div>
              </div>

              <div v-if="conflict.changes.length > 2" class="more-changes">
                <a-button type="link" size="small" @click="showAllChanges(conflict)">
                  还有 {{ conflict.changes.length - 2 }} 个变更...
                </a-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 预防性警告 -->
      <div v-if="preventionWarnings.length > 0" class="prevention-warnings">
        <div class="section-title">
          <span>⚠️ 预防性警告</span>
          <a-button type="link" size="small" @click="clearWarnings">清除</a-button>
        </div>

        <div class="warnings-list">
          <a-alert
            v-for="warning in preventionWarnings"
            :key="warning.id"
            :type="warning.type"
            :message="warning.message"
            :description="warning.suggestions.join('; ')"
            closable
            @close="removeWarning(warning.id)"
          />
        </div>
      </div>

      <!-- 解决历史统计 -->
      <div v-if="showHistory && resolutionHistory.length > 0" class="resolution-history">
        <div class="section-title">
          <span>📈 解决历史</span>
          <div class="history-stats">
            <span>成功率: {{ Math.round(successRate) }}%</span>
            <span>自动解决: {{ autoResolveCount }}</span>
            <span>手动解决: {{ manualResolveCount }}</span>
          </div>
        </div>

        <div class="history-chart">
          <canvas ref="historyChartRef" width="400" height="200"></canvas>
        </div>
      </div>
    </div>

    <!-- 冲突详情弹窗 -->
    <a-modal
      v-model:open="showDetailModal"
      title="冲突详细信息"
      width="800px"
      :footer="null"
      @cancel="selectedConflict = null"
    >
      <div v-if="selectedConflict" class="conflict-detail">
        <!-- 基本信息 -->
        <div class="detail-section">
          <h4>基本信息</h4>
          <a-descriptions :column="2" size="small">
            <a-descriptions-item label="字段">{{ selectedConflict.fieldLabel }}</a-descriptions-item>
            <a-descriptions-item label="冲突类型">{{ getConflictTypeLabel(selectedConflict.type) }}</a-descriptions-item>
            <a-descriptions-item label="风险等级">{{ selectedConflict.riskLevel }}</a-descriptions-item>
            <a-descriptions-item label="发生时间">{{ formatTime(selectedConflict.timestamp) }}</a-descriptions-item>
          </a-descriptions>
        </div>

        <!-- 评分详情 -->
        <div class="detail-section">
          <h4>智能评分</h4>
          <div class="score-details">
            <a-progress
              v-for="(score, key) in selectedConflict.score"
              :key="key"
              :percent="score"
              :format="() => `${getScoreLabel(key)}: ${score}`"
              :stroke-color="getScoreColor(score)"
            />
          </div>
        </div>

        <!-- 语义分析 -->
        <div v-if="selectedConflict.semanticAnalysis?.size > 0" class="detail-section">
          <h4>语义分析</h4>
          <div class="semantic-analysis">
            <div
              v-for="[userId, analysis] in selectedConflict.semanticAnalysis"
              :key="userId"
              class="analysis-item"
            >
              <h5>用户 {{ getUserName(userId) }}</h5>
              <a-tag>意图: {{ analysis.intent }}</a-tag>
              <a-tag>情感: {{ analysis.sentiment > 0 ? '积极' : analysis.sentiment < 0 ? '消极' : '中性' }}</a-tag>
              <a-tag>分类: {{ analysis.category }}</a-tag>
              <div class="keywords">
                <span>关键词: </span>
                <a-tag v-for="keyword in analysis.keywords.slice(0, 5)" :key="keyword" size="small">
                  {{ keyword }}
                </a-tag>
              </div>
            </div>
          </div>
        </div>

        <!-- 解决策略选择 -->
        <div class="detail-section">
          <h4>解决策略</h4>
          <a-radio-group v-model:value="selectedStrategy" @change="previewResolution">
            <a-radio :value="selectedConflict.suggestedStrategy">
              {{ getStrategyLabel(selectedConflict.suggestedStrategy) }} (推荐)
            </a-radio>
            <a-radio
              v-for="strategy in selectedConflict.alternativeStrategies"
              :key="strategy"
              :value="strategy"
            >
              {{ getStrategyLabel(strategy) }}
            </a-radio>
          </a-radio-group>

          <div v-if="previewResult" class="preview-result">
            <h5>预览结果</h5>
            <p><strong>最终值:</strong> {{ previewResult.finalValue }}</p>
            <p><strong>被拒绝的变更:</strong> {{ previewResult.rejectedChanges.length }} 个</p>
            <p><strong>原因:</strong> {{ previewResult.reason }}</p>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="detail-actions">
          <a-button type="primary" @click="applyResolution" :loading="resolving">
            应用解决方案
          </a-button>
          <a-button @click="showDetailModal = false">取消</a-button>
        </div>
      </div>
    </a-modal>

    <!-- 设置面板 -->
    <a-drawer
      v-model:open="showSettings"
      title="冲突解决设置"
      placement="right"
      width="400"
    >
      <div class="settings-content">
        <div class="setting-group">
          <h4>自动解决设置</h4>
          <a-switch
            v-model:checked="settings.autoResolveEnabled"
            checked-children="启用"
            un-checked-children="禁用"
          />
          <p class="setting-desc">启用后，系统将自动解决置信度高的冲突</p>

          <div v-if="settings.autoResolveEnabled" class="sub-settings">
            <div class="setting-item">
              <label>最低置信度阈值</label>
              <a-slider
                v-model:value="settings.confidenceThreshold"
                :min="50"
                :max="95"
                :marks="{ 50: '50%', 70: '70%', 90: '90%' }"
              />
            </div>

            <div class="setting-item">
              <label>自动解决延迟</label>
              <a-select v-model:value="settings.autoResolveDelay">
                <a-select-option :value="0">立即</a-select-option>
                <a-select-option :value="5000">5秒</a-select-option>
                <a-select-option :value="10000">10秒</a-select-option>
                <a-select-option :value="30000">30秒</a-select-option>
              </a-select>
            </div>
          </div>
        </div>

        <div class="setting-group">
          <h4>预防设置</h4>
          <a-switch
            v-model:checked="settings.preventionEnabled"
            checked-children="启用"
            un-checked-children="禁用"
          />
          <p class="setting-desc">启用冲突预防和警告提醒</p>
        </div>

        <div class="setting-group">
          <h4>学习设置</h4>
          <a-switch
            v-model:checked="settings.learningEnabled"
            checked-children="启用"
            un-checked-children="禁用"
          />
          <p class="setting-desc">启用机器学习和模式识别</p>
        </div>

        <div class="setting-actions">
          <a-button type="primary" @click="saveSettings">保存设置</a-button>
          <a-button @click="resetSettings">重置</a-button>
        </div>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import { message } from 'ant-design-vue';
import {
  EnhancedConflictResolver,
  EnhancedConflictInfo,
  EnhancedResolutionStrategy,
  ConflictScore
} from '/@/utils/enhanced-conflict-resolver';

interface PreventionWarning {
  id: string;
  type: 'warning' | 'error' | 'info';
  message: string;
  suggestions: string[];
  fieldName: string;
  timestamp: number;
}

interface ResolutionHistory {
  id: string;
  conflictId: string;
  strategy: string;
  success: boolean;
  timestamp: number;
  duration: number;
}

const props = defineProps<{
  conflicts: EnhancedConflictInfo[];
  autoResolve?: boolean;
  showPreventionWarnings?: boolean;
  enableLearning?: boolean;
}>();

const emit = defineEmits<{
  conflictResolved: [conflict: EnhancedConflictInfo, result: any];
  resolutionFailed: [conflict: EnhancedConflictInfo, error: string];
  warningDismissed: [warning: PreventionWarning];
  settingsChanged: [settings: any];
}>();

// 组件状态
const resolver = new EnhancedConflictResolver();
const activeConflicts = ref<EnhancedConflictInfo[]>([]);
const preventionWarnings = ref<PreventionWarning[]>([]);
const resolutionHistory = ref<ResolutionHistory[]>([]);
const resolving = ref(false);
const resolvingIds = ref<Set<string>>(new Set());

// UI状态
const showHistory = ref(false);
const showSettings = ref(false);
const showDetailModal = ref(false);
const selectedConflict = ref<EnhancedConflictInfo | null>(null);
const selectedStrategy = ref<string>('');
const previewResult = ref<any>(null);
const filterLevel = ref<string>('');
const historyChartRef = ref<HTMLCanvasElement>();

// 设置
const settings = ref({
  autoResolveEnabled: true,
  confidenceThreshold: 80,
  autoResolveDelay: 5000,
  preventionEnabled: true,
  learningEnabled: true,
  maxHistorySize: 100
});

// 计算属性
const hasActiveConflicts = computed(() => activeConflicts.value.length > 0);

const hasAutoResolvableConflicts = computed(() =>
  activeConflicts.value.some(c => c.autoResolvable && c.score.overall >= settings.value.confidenceThreshold)
);

const filteredConflicts = computed(() => {
  let conflicts = activeConflicts.value;

  if (filterLevel.value) {
    conflicts = conflicts.filter(c => c.riskLevel === filterLevel.value);
  }

  return conflicts.sort((a, b) => {
    // 高风险和可自动解决的排在前面
    const aScore = (a.riskLevel === 'CRITICAL' ? 1000 :
                   a.riskLevel === 'HIGH' ? 100 : 0) +
                   (a.autoResolvable ? 50 : 0);
    const bScore = (b.riskLevel === 'CRITICAL' ? 1000 :
                   b.riskLevel === 'HIGH' ? 100 : 0) +
                   (b.autoResolvable ? 50 : 0);

    return bScore - aScore;
  });
});

const successRate = computed(() => {
  if (resolutionHistory.value.length === 0) return 0;
  const successful = resolutionHistory.value.filter(h => h.success).length;
  return (successful / resolutionHistory.value.length) * 100;
});

const autoResolveCount = computed(() =>
  resolutionHistory.value.filter(h => h.strategy.includes('AUTO') || h.strategy.includes('ML')).length
);

const manualResolveCount = computed(() =>
  resolutionHistory.value.filter(h => h.strategy === 'MANUAL_MERGE').length
);

// 监听冲突变化
const updateConflicts = () => {
  activeConflicts.value = [...props.conflicts];

  // 自动解决
  if (settings.value.autoResolveEnabled) {
    nextTick(() => {
      const autoResolvableConflicts = activeConflicts.value.filter(
        c => c.autoResolvable &&
             c.score.overall >= settings.value.confidenceThreshold &&
             !resolvingIds.value.has(c.id)
      );

      autoResolvableConflicts.forEach(conflict => {
        setTimeout(() => {
          resolveConflict(conflict);
        }, settings.value.autoResolveDelay);
      });
    });
  }
};

// 冲突解决
const resolveConflict = async (conflict: EnhancedConflictInfo, strategy?: string) => {
  if (resolvingIds.value.has(conflict.id)) return;

  resolvingIds.value.add(conflict.id);

  try {
    const startTime = Date.now();
    const result = await resolver.resolveEnhancedConflict(
      conflict,
      strategy as EnhancedResolutionStrategy
    );

    const duration = Date.now() - startTime;

    // 记录历史
    resolutionHistory.value.unshift({
      id: `resolution_${Date.now()}`,
      conflictId: conflict.id,
      strategy: result.strategy,
      success: result.success,
      timestamp: Date.now(),
      duration
    });

    // 保持历史记录大小
    if (resolutionHistory.value.length > settings.value.maxHistorySize) {
      resolutionHistory.value = resolutionHistory.value.slice(0, settings.value.maxHistorySize);
    }

    if (result.success) {
      // 从活跃冲突中移除
      activeConflicts.value = activeConflicts.value.filter(c => c.id !== conflict.id);

      message.success(`冲突已解决: ${result.reason}`);
      emit('conflictResolved', conflict, result);
    } else {
      message.error(`冲突解决失败: ${result.reason}`);
      emit('resolutionFailed', conflict, result.reason);
    }
  } catch (error) {
    message.error(`解决冲突时发生错误: ${error.message}`);
    emit('resolutionFailed', conflict, error.message);
  } finally {
    resolvingIds.value.delete(conflict.id);
  }
};

const autoResolveAll = async () => {
  resolving.value = true;

  try {
    const resolvableConflicts = activeConflicts.value.filter(
      c => c.autoResolvable && c.score.overall >= settings.value.confidenceThreshold
    );

    const promises = resolvableConflicts.map(conflict => resolveConflict(conflict));
    await Promise.all(promises);

    message.success(`已自动解决 ${resolvableConflicts.length} 个冲突`);
  } catch (error) {
    message.error('批量解决失败');
  } finally {
    resolving.value = false;
  }
};

const showConflictDetail = (conflict: EnhancedConflictInfo) => {
  selectedConflict.value = conflict;
  selectedStrategy.value = conflict.suggestedStrategy;
  showDetailModal.value = true;
  previewResolution();
};

const previewResolution = async () => {
  if (!selectedConflict.value || !selectedStrategy.value) return;

  try {
    previewResult.value = await resolver.resolveEnhancedConflict(
      selectedConflict.value,
      selectedStrategy.value as EnhancedResolutionStrategy
    );
  } catch (error) {
    console.error('Preview failed:', error);
  }
};

const applyResolution = async () => {
  if (!selectedConflict.value || !selectedStrategy.value) return;

  await resolveConflict(selectedConflict.value, selectedStrategy.value);
  showDetailModal.value = false;
  selectedConflict.value = null;
};

const handleConflictAction = async (conflict: EnhancedConflictInfo, action: string) => {
  switch (action) {
    case 'manual':
      showConflictDetail(conflict);
      break;
    case 'ignore':
      activeConflicts.value = activeConflicts.value.filter(c => c.id !== conflict.id);
      message.info('已忽略冲突');
      break;
    case 'rollback':
      // 实现回滚逻辑
      message.info('回滚功能开发中');
      break;
    case 'preview':
      showConflictDetail(conflict);
      break;
  }
};

const previewStrategy = (conflict: EnhancedConflictInfo, strategy: EnhancedResolutionStrategy) => {
  selectedConflict.value = conflict;
  selectedStrategy.value = strategy;
  showDetailModal.value = true;
  previewResolution();
};

const showAllChanges = (conflict: EnhancedConflictInfo) => {
  showConflictDetail(conflict);
};

// 预防警告管理
const addPreventionWarning = (warning: PreventionWarning) => {
  preventionWarnings.value.unshift(warning);
};

const removeWarning = (warningId: string) => {
  preventionWarnings.value = preventionWarnings.value.filter(w => w.id !== warningId);
};

const clearWarnings = () => {
  preventionWarnings.value = [];
};

// 设置管理
const saveSettings = () => {
  emit('settingsChanged', settings.value);
  message.success('设置已保存');
  showSettings.value = false;
};

const resetSettings = () => {
  settings.value = {
    autoResolveEnabled: true,
    confidenceThreshold: 80,
    autoResolveDelay: 5000,
    preventionEnabled: true,
    learningEnabled: true,
    maxHistorySize: 100
  };
  message.info('设置已重置');
};

// 工具方法
const getConflictLevelColor = () => {
  const highRiskCount = activeConflicts.value.filter(c =>
    c.riskLevel === 'CRITICAL' || c.riskLevel === 'HIGH'
  ).length;

  if (highRiskCount > 0) return '#ff4d4f';
  return '#52c41a';
};

const getConflictTypeColor = (type: string) => {
  const colors = {
    'CONCURRENT_EDIT': 'orange',
    'LATE_UPDATE': 'blue',
    'PERMISSION_CONFLICT': 'red',
    'DATA_TYPE_MISMATCH': 'purple'
  };
  return colors[type] || 'default';
};

const getConflictTypeLabel = (type: string) => {
  const labels = {
    'CONCURRENT_EDIT': '并发编辑',
    'LATE_UPDATE': '延迟更新',
    'PERMISSION_CONFLICT': '权限冲突',
    'DATA_TYPE_MISMATCH': '类型不匹配'
  };
  return labels[type] || type;
};

const getRiskLevelColor = (level: string) => {
  const colors = {
    'CRITICAL': 'red',
    'HIGH': 'orange',
    'MEDIUM': 'yellow',
    'LOW': 'green'
  };
  return colors[level] || 'default';
};

const getStrategyLabel = (strategy: string) => {
  const labels = {
    'SEMANTIC_MERGE': '语义合并',
    'CONTEXT_AWARE': '上下文感知',
    'ML_PREDICTED': 'AI预测',
    'INCREMENTAL_MERGE': '增量合并',
    'COLLABORATIVE_VOTE': '协作投票',
    'PATTERN_BASED': '模式匹配',
    'CONFIDENCE_WEIGHTED': '置信度加权',
    'TIME_DECAY': '时间衰减',
    'LAST_WRITE_WINS': '最后写入胜出',
    'FIRST_WRITE_WINS': '最先写入胜出',
    'ROLE_BASED': '基于角色',
    'AUTO_MERGE': '自动合并',
    'MANUAL_MERGE': '手动合并',
    'REJECT_ALL': '拒绝所有'
  };
  return labels[strategy] || strategy;
};

const getDisplayScores = (score: ConflictScore) => {
  return {
    semantic: score.semantic,
    context: score.context,
    authority: score.authority,
    overall: score.overall
  };
};

const getScoreLabel = (key: string) => {
  const labels = {
    'semantic': '语义',
    'temporal': '时间',
    'authority': '权威',
    'context': '上下文',
    'confidence': '置信度',
    'impact': '影响',
    'complexity': '复杂度',
    'overall': '综合'
  };
  return labels[key] || key;
};

const getScoreColor = (score: number) => {
  if (score >= 80) return '#52c41a';
  if (score >= 60) return '#faad14';
  if (score >= 40) return '#fa8c16';
  return '#ff4d4f';
};

const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleString();
};

const truncateValue = (value: any) => {
  const str = String(value || '');
  return str.length > 30 ? str.substring(0, 30) + '...' : str;
};

const getUserName = (userId: string) => {
  const conflict = selectedConflict.value;
  if (!conflict) return userId;

  const change = conflict.changes.find(c => c.user.id.toString() === userId);
  return change?.user.name || userId;
};

// 图表绘制
const drawHistoryChart = () => {
  if (!historyChartRef.value) return;

  const canvas = historyChartRef.value;
  const ctx = canvas.getContext('2d');
  if (!ctx) return;

  // 清空画布
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  // 简化的图表绘制
  const history = resolutionHistory.value.slice(0, 10);
  if (history.length === 0) return;

  const width = canvas.width;
  const height = canvas.height;
  const margin = 20;

  // 绘制成功率趋势
  ctx.strokeStyle = '#52c41a';
  ctx.lineWidth = 2;
  ctx.beginPath();

  history.forEach((item, index) => {
    const x = margin + (index / (history.length - 1)) * (width - 2 * margin);
    const y = height - margin - (item.success ? 0.8 : 0.2) * (height - 2 * margin);

    if (index === 0) {
      ctx.moveTo(x, y);
    } else {
      ctx.lineTo(x, y);
    }
  });

  ctx.stroke();
};

// 生命周期
onMounted(() => {
  updateConflicts();
  if (showHistory.value) {
    nextTick(drawHistoryChart);
  }
});

// 监听props变化
$: {
  updateConflicts();
}

// 暴露方法
defineExpose({
  addConflict: (conflict: EnhancedConflictInfo) => {
    activeConflicts.value.push(conflict);
  },
  removeConflict: (conflictId: string) => {
    activeConflicts.value = activeConflicts.value.filter(c => c.id !== conflictId);
  },
  addWarning: addPreventionWarning,
  getStatistics: () => ({
    activeConflicts: activeConflicts.value.length,
    successRate: successRate.value,
    totalResolved: resolutionHistory.value.length
  })
});
</script>

<style scoped>
.intelligent-conflict-manager {
  position: relative;
}

.conflict-overview-panel {
  position: fixed;
  top: 80px;
  right: 20px;
  width: 450px;
  max-height: calc(100vh - 120px);
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
  z-index: 1100;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-actions .ant-btn {
  border-color: rgba(255, 255, 255, 0.3);
  color: white;
}

.header-actions .ant-btn[type="primary"] {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
}

.active-conflicts {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.section-title {
  padding: 12px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 500;
  background: #fafafa;
}

.conflicts-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conflict-item {
  margin-bottom: 12px;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  background: white;
  transition: all 0.3s ease;
}

.conflict-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.conflict-item.risk-critical {
  border-left: 4px solid #ff4d4f;
}

.conflict-item.risk-high {
  border-left: 4px solid #fa8c16;
}

.conflict-item.risk-medium {
  border-left: 4px solid #faad14;
}

.conflict-item.risk-low {
  border-left: 4px solid #52c41a;
}

.conflict-item.auto-resolvable {
  background: linear-gradient(135deg, #f6ffed 0%, #f0f9ff 100%);
}

.conflict-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}

.conflict-info {
  flex: 1;
}

.field-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.field-name {
  font-weight: 600;
  color: #1a1a1a;
}

.conflict-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #666;
}

.conflict-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.conflict-scores {
  margin: 12px 0;
}

.score-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.score-label {
  min-width: 60px;
  font-size: 12px;
  color: #666;
}

.score-bar {
  flex: 1;
  height: 6px;
  background: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
}

.score-fill {
  height: 100%;
  transition: width 0.3s ease;
}

.score-value {
  min-width: 30px;
  font-size: 12px;
  font-weight: 500;
  text-align: right;
}

.recommended-strategy {
  margin: 12px 0;
  padding: 12px;
  background: #f8f9ff;
  border-radius: 8px;
}

.strategy-label, .alt-label {
  font-size: 12px;
  color: #666;
  margin-right: 8px;
}

.confidence {
  font-size: 12px;
  color: #1890ff;
  margin-left: 12px;
}

.alternatives {
  margin-top: 8px;
}

.clickable-tag {
  cursor: pointer;
}

.clickable-tag:hover {
  opacity: 0.8;
}

.changes-preview {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.change-item {
  margin-bottom: 8px;
  padding: 8px;
  background: #fafafa;
  border-radius: 6px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
  font-size: 12px;
}

.user-name {
  font-weight: 500;
  color: #1a1a1a;
}

.user-role {
  color: #666;
}

.change-time {
  color: #999;
}

.value-change {
  font-size: 12px;
  font-family: monospace;
}

.old-value {
  color: #ff4d4f;
}

.arrow {
  color: #666;
  margin: 0 4px;
}

.new-value {
  color: #52c41a;
}

.more-changes {
  text-align: center;
  margin-top: 8px;
}

.prevention-warnings {
  padding: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  max-height: 200px;
  overflow-y: auto;
}

.warnings-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.resolution-history {
  padding: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  background: #fafafa;
}

.history-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #666;
}

.history-chart {
  margin-top: 12px;
  text-align: center;
}

.conflict-detail {
  max-height: 80vh;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.detail-section:last-child {
  border-bottom: none;
}

.detail-section h4 {
  margin-bottom: 12px;
  color: #1a1a1a;
  font-weight: 600;
}

.detail-section h5 {
  margin: 8px 0;
  color: #666;
  font-weight: 500;
}

.score-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.semantic-analysis {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.analysis-item {
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
}

.keywords {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.preview-result {
  margin-top: 16px;
  padding: 12px;
  background: #f0f9ff;
  border-radius: 8px;
}

.preview-result h5 {
  margin-bottom: 8px;
  color: #1890ff;
}

.detail-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.settings-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.setting-group {
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.setting-group h4 {
  margin-bottom: 12px;
  color: #1a1a1a;
  font-weight: 600;
}

.setting-desc {
  margin: 8px 0;
  font-size: 12px;
  color: #666;
}

.sub-settings {
  margin-top: 16px;
  padding-left: 16px;
}

.setting-item {
  margin-bottom: 16px;
}

.setting-item label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #1a1a1a;
}

.setting-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

/* 滚动条优化 */
.conflicts-list::-webkit-scrollbar,
.prevention-warnings::-webkit-scrollbar,
.conflict-detail::-webkit-scrollbar {
  width: 4px;
}

.conflicts-list::-webkit-scrollbar-track,
.prevention-warnings::-webkit-scrollbar-track,
.conflict-detail::-webkit-scrollbar-track {
  background: transparent;
}

.conflicts-list::-webkit-scrollbar-thumb,
.prevention-warnings::-webkit-scrollbar-thumb,
.conflict-detail::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 2px;
}

.conflicts-list::-webkit-scrollbar-thumb:hover,
.prevention-warnings::-webkit-scrollbar-thumb:hover,
.conflict-detail::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}
</style>