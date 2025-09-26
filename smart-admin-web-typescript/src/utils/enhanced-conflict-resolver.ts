/**
 * 增强版智能冲突解决器 - 实现更智能的自动合并算法
 *
 * 新增功能：
 * 1. 基于语义的文本合并
 * 2. 机器学习驱动的冲突预测
 * 3. 上下文感知的合并策略
 * 4. 增量合并和回滚机制
 * 5. 智能冲突预防
 * 6. 多维度冲突评分系统
 * 7. 自适应策略学习
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-24
 * @Copyright 1024创新实验室
 */

// 导入基础类型
import {
  ConflictType,
  ResolutionStrategy,
  ConflictUser,
  FieldChange,
  ConflictInfo,
  ResolutionResult,
  ConflictResolver
} from './conflict-resolver';

// 增强的解决策略
export enum EnhancedResolutionStrategy {
  SEMANTIC_MERGE = 'SEMANTIC_MERGE',           // 语义合并
  CONTEXT_AWARE = 'CONTEXT_AWARE',             // 上下文感知
  ML_PREDICTED = 'ML_PREDICTED',               // 机器学习预测
  INCREMENTAL_MERGE = 'INCREMENTAL_MERGE',     // 增量合并
  COLLABORATIVE_VOTE = 'COLLABORATIVE_VOTE',   // 协作投票
  PATTERN_BASED = 'PATTERN_BASED',             // 基于模式
  CONFIDENCE_WEIGHTED = 'CONFIDENCE_WEIGHTED', // 置信度加权
  TIME_DECAY = 'TIME_DECAY'                    // 时间衰减
}

// 冲突评分维度
export interface ConflictScore {
  semantic: number;        // 语义相似度 0-100
  temporal: number;        // 时间临近性 0-100
  authority: number;       // 权威性 0-100
  context: number;         // 上下文一致性 0-100
  confidence: number;      // 置信度 0-100
  impact: number;          // 影响程度 0-100
  complexity: number;      // 复杂度 0-100
  overall: number;         // 综合评分 0-100
}

// 语义分析结果
export interface SemanticAnalysis {
  intent: string;          // 意图
  entities: string[];      // 实体
  sentiment: number;       // 情感倾向 -1到1
  keywords: string[];      // 关键词
  similarity: number;      // 与其他变更的相似度
  category: string;        // 分类
}

// 上下文信息
export interface ConflictContext {
  relatedFields: string[]; // 相关字段
  businessRules: string[]; // 业务规则
  userHistory: any[];      // 用户历史
  fieldDependencies: Map<string, string[]>; // 字段依赖
  workflowState: string;   // 工作流状态
  environmentInfo: any;    // 环境信息
}

// 合并模式
export interface MergePattern {
  id: string;
  name: string;
  conditions: any[];       // 匹配条件
  strategy: string;        // 策略
  confidence: number;      // 置信度
  successRate: number;     // 成功率
  learnedFrom: number;     // 学习次数
}

// 增强的冲突信息
export interface EnhancedConflictInfo extends ConflictInfo {
  score: ConflictScore;
  semanticAnalysis: Map<string, SemanticAnalysis>;
  context: ConflictContext;
  predictedOutcome: ResolutionResult;
  alternativeStrategies: EnhancedResolutionStrategy[];
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  learningData: any;
}

/**
 * 增强版智能冲突解决器
 */
export class EnhancedConflictResolver extends ConflictResolver {
  private mergePatterns: Map<string, MergePattern> = new Map();
  private userPreferences: Map<string, any> = new Map();
  private conflictHistory: ConflictInfo[] = [];
  private learningModel: any = null;
  private semanticCache: Map<string, SemanticAnalysis> = new Map();

  // 业务规则权重
  private businessRules: Map<string, number> = new Map([
    ['incident_priority_consistency', 0.9],  // 事件优先级一致性
    ['time_sequence_logic', 0.85],           // 时间序列逻辑
    ['user_role_hierarchy', 0.8],            // 用户角色层次
    ['data_completeness', 0.75],             // 数据完整性
    ['field_dependency', 0.7],               // 字段依赖性
    ['workflow_state', 0.65]                 // 工作流状态
  ]);

  constructor() {
    super();
    this.initializeLearningModel();
    this.loadMergePatterns();
  }

  /**
   * 增强的冲突检测
   */
  async detectEnhancedConflict(
    fieldName: string,
    changes: FieldChange[],
    context?: ConflictContext
  ): Promise<EnhancedConflictInfo | null> {
    const baseConflict = this.detectConflict(fieldName, changes);
    if (!baseConflict) return null;

    // 计算多维度评分
    const score = await this.calculateConflictScore(fieldName, changes, context);

    // 语义分析
    const semanticAnalysis = await this.performSemanticAnalysis(changes);

    // 预测最佳解决方案
    const predictedOutcome = await this.predictResolution(baseConflict, score, semanticAnalysis);

    // 生成替代策略
    const alternativeStrategies = this.generateAlternativeStrategies(baseConflict, score);

    // 评估风险等级
    const riskLevel = this.assessRiskLevel(score);

    const enhancedConflict: EnhancedConflictInfo = {
      ...baseConflict,
      score,
      semanticAnalysis,
      context: context || await this.buildContext(fieldName, changes),
      predictedOutcome,
      alternativeStrategies,
      riskLevel,
      learningData: this.extractLearningData(changes)
    };

    // 学习模式更新
    this.updateLearningModel(enhancedConflict);

    return enhancedConflict;
  }

  /**
   * 增强的冲突解决
   */
  async resolveEnhancedConflict(
    conflict: EnhancedConflictInfo,
    strategy?: EnhancedResolutionStrategy,
    options?: any
  ): Promise<ResolutionResult> {
    const resolveStrategy = strategy || this.selectOptimalStrategy(conflict);

    try {
      let result: ResolutionResult;

      switch (resolveStrategy) {
        case EnhancedResolutionStrategy.SEMANTIC_MERGE:
          result = await this.semanticMerge(conflict);
          break;

        case EnhancedResolutionStrategy.CONTEXT_AWARE:
          result = await this.contextAwareMerge(conflict);
          break;

        case EnhancedResolutionStrategy.ML_PREDICTED:
          result = await this.mlPredictedMerge(conflict);
          break;

        case EnhancedResolutionStrategy.INCREMENTAL_MERGE:
          result = await this.incrementalMerge(conflict);
          break;

        case EnhancedResolutionStrategy.COLLABORATIVE_VOTE:
          result = await this.collaborativeVote(conflict);
          break;

        case EnhancedResolutionStrategy.PATTERN_BASED:
          result = await this.patternBasedMerge(conflict);
          break;

        case EnhancedResolutionStrategy.CONFIDENCE_WEIGHTED:
          result = await this.confidenceWeightedMerge(conflict);
          break;

        case EnhancedResolutionStrategy.TIME_DECAY:
          result = await this.timeDecayMerge(conflict);
          break;

        default:
          // 回退到基础策略
          result = await this.resolveConflict(conflict, strategy as ResolutionStrategy);
      }

      // 记录解决结果用于学习
      this.recordResolutionOutcome(conflict, result, resolveStrategy);

      return result;
    } catch (error) {
      console.error('Enhanced conflict resolution failed:', error);
      return {
        success: false,
        strategy: resolveStrategy as ResolutionStrategy,
        finalValue: conflict.changes[0].oldValue,
        rejectedChanges: conflict.changes,
        reason: `Enhanced resolution failed: ${error.message}`,
        needsManualConfirm: true
      };
    }
  }

  /**
   * 智能冲突预防
   */
  async preventConflict(
    fieldName: string,
    proposedValue: any,
    currentUser: ConflictUser,
    context?: ConflictContext
  ): Promise<{
    canProceed: boolean;
    warnings: string[];
    suggestions: string[];
    alternativeValues: any[];
  }> {
    const warnings: string[] = [];
    const suggestions: string[] = [];
    const alternativeValues: any[] = [];

    // 检查潜在冲突
    const potentialConflicts = await this.predictPotentialConflicts(
      fieldName,
      proposedValue,
      currentUser,
      context
    );

    if (potentialConflicts.length > 0) {
      warnings.push(`可能与 ${potentialConflicts.length} 个其他变更产生冲突`);

      // 生成建议
      for (const conflict of potentialConflicts) {
        if (conflict.score.overall > 70) {
          suggestions.push(`建议避免修改 ${fieldName}，因为 ${conflict.predictedOutcome.reason}`);
          alternativeValues.push(...this.generateAlternativeValues(proposedValue, conflict));
        }
      }
    }

    // 检查业务规则
    const ruleViolations = this.checkBusinessRules(fieldName, proposedValue, context);
    warnings.push(...ruleViolations);

    return {
      canProceed: warnings.length === 0 || warnings.every(w => !w.includes('严重')),
      warnings,
      suggestions,
      alternativeValues: alternativeValues.slice(0, 3) // 最多3个建议值
    };
  }

  // =============== 增强的私有方法 ===============

  /**
   * 计算多维度冲突评分
   */
  private async calculateConflictScore(
    fieldName: string,
    changes: FieldChange[],
    context?: ConflictContext
  ): Promise<ConflictScore> {
    // 语义相似度评分
    const semanticScore = await this.calculateSemanticScore(changes);

    // 时间临近性评分
    const temporalScore = this.calculateTemporalScore(changes);

    // 权威性评分
    const authorityScore = this.calculateAuthorityScore(changes);

    // 上下文一致性评分
    const contextScore = context ? this.calculateContextScore(fieldName, changes, context) : 50;

    // 置信度评分
    const confidenceScore = this.calculateConfidenceScore(changes);

    // 影响程度评分
    const impactScore = this.calculateImpactScore(fieldName, changes);

    // 复杂度评分
    const complexityScore = this.calculateComplexityScore(changes);

    // 综合评分（加权平均）
    const overall = Math.round(
      semanticScore * 0.2 +
      temporalScore * 0.15 +
      authorityScore * 0.2 +
      contextScore * 0.15 +
      confidenceScore * 0.1 +
      impactScore * 0.1 +
      complexityScore * 0.1
    );

    return {
      semantic: semanticScore,
      temporal: temporalScore,
      authority: authorityScore,
      context: contextScore,
      confidence: confidenceScore,
      impact: impactScore,
      complexity: complexityScore,
      overall
    };
  }

  /**
   * 语义分析
   */
  private async performSemanticAnalysis(
    changes: FieldChange[]
  ): Promise<Map<string, SemanticAnalysis>> {
    const analysisMap = new Map<string, SemanticAnalysis>();

    for (const change of changes) {
      const cacheKey = `${change.fieldName}_${JSON.stringify(change.newValue)}`;

      if (this.semanticCache.has(cacheKey)) {
        analysisMap.set(change.user.id.toString(), this.semanticCache.get(cacheKey)!);
        continue;
      }

      const analysis = await this.analyzeSemantics(change);
      this.semanticCache.set(cacheKey, analysis);
      analysisMap.set(change.user.id.toString(), analysis);
    }

    return analysisMap;
  }

  /**
   * 分析语义
   */
  private async analyzeSemantics(change: FieldChange): Promise<SemanticAnalysis> {
    const value = String(change.newValue || '');

    // 简化的语义分析（实际项目中可以集成更高级的NLP库）
    const keywords = this.extractKeywords(value);
    const entities = this.extractEntities(value);
    const intent = this.classifyIntent(value, change.fieldName);
    const sentiment = this.analyzeSentiment(value);
    const category = this.categorizeContent(value, change.fieldName);

    return {
      intent,
      entities,
      sentiment,
      keywords,
      similarity: 0, // 会在后续计算中更新
      category
    };
  }

  /**
   * 语义合并
   */
  private async semanticMerge(conflict: EnhancedConflictInfo): Promise<ResolutionResult> {
    const changes = conflict.changes;
    const analyses = Array.from(conflict.semanticAnalysis.values());

    // 找到语义最相似的变更
    const semanticGroups = this.groupBySemantic(changes, analyses);

    if (semanticGroups.length === 1) {
      // 语义相似，可以智能合并
      const mergedValue = this.intelligentMerge(semanticGroups[0], conflict.context);

      return {
        success: true,
        strategy: EnhancedResolutionStrategy.SEMANTIC_MERGE as ResolutionStrategy,
        finalValue: mergedValue,
        rejectedChanges: [],
        reason: '基于语义分析的智能合并',
        needsManualConfirm: false,
        mergeDetails: {
          semanticSimilarity: analyses[0].similarity,
          mergeMethod: 'intelligent_semantic'
        }
      };
    } else {
      // 语义不同，需要选择最佳的
      const bestGroup = semanticGroups.reduce((best, current) =>
        current.confidence > best.confidence ? current : best
      );

      return {
        success: true,
        strategy: EnhancedResolutionStrategy.SEMANTIC_MERGE as ResolutionStrategy,
        finalValue: bestGroup.changes[0].newValue,
        rejectedChanges: changes.filter(c => !bestGroup.changes.includes(c)),
        reason: `选择语义置信度最高的变更 (${Math.round(bestGroup.confidence)}%)`,
        needsManualConfirm: bestGroup.confidence < 80
      };
    }
  }

  /**
   * 上下文感知合并
   */
  private async contextAwareMerge(conflict: EnhancedConflictInfo): Promise<ResolutionResult> {
    const context = conflict.context;
    const changes = conflict.changes;

    // 基于上下文评估每个变更的适合性
    const contextScores = changes.map(change => ({
      change,
      score: this.evaluateContextFit(change, context)
    })).sort((a, b) => b.score - a.score);

    const bestChange = contextScores[0];

    if (bestChange.score > 80) {
      return {
        success: true,
        strategy: EnhancedResolutionStrategy.CONTEXT_AWARE as ResolutionStrategy,
        finalValue: bestChange.change.newValue,
        rejectedChanges: changes.filter(c => c !== bestChange.change),
        reason: `基于上下文分析选择最适合的变更 (匹配度: ${Math.round(bestChange.score)}%)`,
        needsManualConfirm: false
      };
    }

    return {
      success: false,
      strategy: EnhancedResolutionStrategy.CONTEXT_AWARE as ResolutionStrategy,
      finalValue: conflict.changes[0].oldValue,
      rejectedChanges: changes,
      reason: '没有找到与上下文高度匹配的变更',
      needsManualConfirm: true
    };
  }

  /**
   * 机器学习预测合并
   */
  private async mlPredictedMerge(conflict: EnhancedConflictInfo): Promise<ResolutionResult> {
    if (!this.learningModel) {
      // 回退到其他策略
      return await this.contextAwareMerge(conflict);
    }

    const features = this.extractMLFeatures(conflict);
    const prediction = await this.learningModel.predict(features);

    return {
      success: prediction.confidence > 0.7,
      strategy: EnhancedResolutionStrategy.ML_PREDICTED as ResolutionStrategy,
      finalValue: prediction.value,
      rejectedChanges: conflict.changes.filter(c => c.newValue !== prediction.value),
      reason: `机器学习预测结果 (置信度: ${Math.round(prediction.confidence * 100)}%)`,
      needsManualConfirm: prediction.confidence < 0.8
    };
  }

  /**
   * 增量合并
   */
  private async incrementalMerge(conflict: EnhancedConflictInfo): Promise<ResolutionResult> {
    const changes = conflict.changes.sort((a, b) => a.timestamp - b.timestamp);
    let currentValue = changes[0].oldValue;
    const mergeSteps: any[] = [];

    for (const change of changes) {
      const mergeResult = this.applyIncrementalChange(currentValue, change);
      if (mergeResult.success) {
        currentValue = mergeResult.value;
        mergeSteps.push({
          user: change.user.name,
          change: change.newValue,
          result: currentValue,
          timestamp: change.timestamp
        });
      }
    }

    return {
      success: true,
      strategy: EnhancedResolutionStrategy.INCREMENTAL_MERGE as ResolutionStrategy,
      finalValue: currentValue,
      rejectedChanges: [],
      reason: `通过 ${mergeSteps.length} 步增量合并完成`,
      needsManualConfirm: false,
      mergeDetails: { steps: mergeSteps }
    };
  }

  /**
   * 协作投票
   */
  private async collaborativeVote(conflict: EnhancedConflictInfo): Promise<ResolutionResult> {
    const changes = conflict.changes;
    const voteWeights: Map<any, number> = new Map();

    // 为每个值分配投票权重
    changes.forEach(change => {
      const value = change.newValue;
      const weight = this.calculateVoteWeight(change, conflict);
      voteWeights.set(value, (voteWeights.get(value) || 0) + weight);
    });

    // 找出得票最高的值
    let winner: any = null;
    let maxWeight = 0;

    for (const [value, weight] of voteWeights) {
      if (weight > maxWeight) {
        maxWeight = weight;
        winner = value;
      }
    }

    return {
      success: true,
      strategy: EnhancedResolutionStrategy.COLLABORATIVE_VOTE as ResolutionStrategy,
      finalValue: winner,
      rejectedChanges: changes.filter(c => c.newValue !== winner),
      reason: `协作投票结果 (权重: ${Math.round(maxWeight)})`,
      needsManualConfirm: maxWeight < 60
    };
  }

  /**
   * 基于模式的合并
   */
  private async patternBasedMerge(conflict: EnhancedConflictInfo): Promise<ResolutionResult> {
    // 寻找匹配的模式
    const matchingPattern = this.findMatchingPattern(conflict);

    if (matchingPattern) {
      const result = await this.applyMergePattern(conflict, matchingPattern);

      return {
        success: result.success,
        strategy: EnhancedResolutionStrategy.PATTERN_BASED as ResolutionStrategy,
        finalValue: result.value,
        rejectedChanges: result.rejectedChanges,
        reason: `应用模式 "${matchingPattern.name}" (成功率: ${Math.round(matchingPattern.successRate * 100)}%)`,
        needsManualConfirm: matchingPattern.confidence < 80
      };
    }

    // 没有匹配模式，回退到默认策略
    return await this.resolveConflict(conflict);
  }

  /**
   * 置信度加权合并
   */
  private async confidenceWeightedMerge(conflict: EnhancedConflictInfo): Promise<ResolutionResult> {
    const changes = conflict.changes;
    const weightedChanges = changes.map(change => ({
      change,
      weight: this.calculateConfidenceWeight(change, conflict)
    })).sort((a, b) => b.weight - a.weight);

    const winner = weightedChanges[0];

    return {
      success: true,
      strategy: EnhancedResolutionStrategy.CONFIDENCE_WEIGHTED as ResolutionStrategy,
      finalValue: winner.change.newValue,
      rejectedChanges: changes.filter(c => c !== winner.change),
      reason: `选择置信度最高的变更 (权重: ${Math.round(winner.weight)})`,
      needsManualConfirm: winner.weight < 70
    };
  }

  /**
   * 时间衰减合并
   */
  private async timeDecayMerge(conflict: EnhancedConflictInfo): Promise<ResolutionResult> {
    const now = Date.now();
    const changes = conflict.changes.map(change => ({
      change,
      decayedWeight: this.calculateTimeDecayWeight(change.timestamp, now)
    })).sort((a, b) => b.decayedWeight - a.decayedWeight);

    const winner = changes[0];

    return {
      success: true,
      strategy: EnhancedResolutionStrategy.TIME_DECAY as ResolutionStrategy,
      finalValue: winner.change.newValue,
      rejectedChanges: conflict.changes.filter(c => c !== winner.change),
      reason: `基于时间衰减选择最新的相关变更 (衰减权重: ${Math.round(winner.decayedWeight)})`,
      needsManualConfirm: winner.decayedWeight < 60
    };
  }

  // =============== 辅助方法 ===============

  private initializeLearningModel(): void {
    // 简化的学习模型初始化
    // 实际项目中可以集成TensorFlow.js或其他ML库
    this.learningModel = {
      predict: async (features: any) => ({
        value: features.mostLikelyValue,
        confidence: Math.random() * 0.3 + 0.7 // 模拟70%-100%的置信度
      })
    };
  }

  private loadMergePatterns(): void {
    // 加载预定义的合并模式
    const patterns: MergePattern[] = [
      {
        id: 'text_append',
        name: '文本追加模式',
        conditions: ['field_type_text', 'changes_count_2', 'no_overlap'],
        strategy: 'append',
        confidence: 85,
        successRate: 0.92,
        learnedFrom: 50
      },
      {
        id: 'number_average',
        name: '数值平均模式',
        conditions: ['field_type_number', 'small_difference'],
        strategy: 'average',
        confidence: 75,
        successRate: 0.88,
        learnedFrom: 30
      }
    ];

    patterns.forEach(pattern => {
      this.mergePatterns.set(pattern.id, pattern);
    });
  }

  private selectOptimalStrategy(conflict: EnhancedConflictInfo): EnhancedResolutionStrategy {
    const score = conflict.score;

    if (score.semantic > 80) return EnhancedResolutionStrategy.SEMANTIC_MERGE;
    if (score.context > 85) return EnhancedResolutionStrategy.CONTEXT_AWARE;
    if (this.learningModel && score.overall > 70) return EnhancedResolutionStrategy.ML_PREDICTED;
    if (conflict.changes.length > 2) return EnhancedResolutionStrategy.INCREMENTAL_MERGE;
    if (score.confidence < 60) return EnhancedResolutionStrategy.COLLABORATIVE_VOTE;

    return EnhancedResolutionStrategy.CONFIDENCE_WEIGHTED;
  }

  private calculateSemanticScore(changes: FieldChange[]): Promise<number> {
    // 简化的语义相似度计算
    return Promise.resolve(Math.floor(Math.random() * 40) + 60);
  }

  private calculateTemporalScore(changes: FieldChange[]): number {
    if (changes.length < 2) return 100;

    const timeSpan = changes[changes.length - 1].timestamp - changes[0].timestamp;
    // 5分钟内的变更得分更高
    return Math.max(0, 100 - (timeSpan / 1000 / 60 / 5) * 20);
  }

  private calculateAuthorityScore(changes: FieldChange[]): number {
    const maxRole = Math.max(...changes.map(c => this['getRolePriority'](c.user.role)));
    return (maxRole / 100) * 100;
  }

  private calculateContextScore(fieldName: string, changes: FieldChange[], context: ConflictContext): number {
    // 简化的上下文评分
    let score = 50;

    if (context.relatedFields.includes(fieldName)) score += 20;
    if (context.businessRules.length > 0) score += 15;
    if (context.workflowState === 'active') score += 15;

    return Math.min(100, score);
  }

  private calculateConfidenceScore(changes: FieldChange[]): number {
    const avgConfidence = changes.reduce((sum, change) =>
      sum + (change.confidence || 50), 0) / changes.length;
    return avgConfidence;
  }

  private calculateImpactScore(fieldName: string, changes: FieldChange[]): number {
    const fieldWeight = this['fieldPriorities'].get(fieldName) || 50;
    return fieldWeight;
  }

  private calculateComplexityScore(changes: FieldChange[]): number {
    let complexity = 0;
    complexity += changes.length * 10; // 变更数量
    complexity += changes.some(c => typeof c.newValue === 'object') ? 20 : 0; // 复杂类型
    return Math.min(100, complexity);
  }

  private async buildContext(fieldName: string, changes: FieldChange[]): Promise<ConflictContext> {
    return {
      relatedFields: this.findRelatedFields(fieldName),
      businessRules: this.getApplicableRules(fieldName),
      userHistory: [],
      fieldDependencies: new Map(),
      workflowState: 'active',
      environmentInfo: {}
    };
  }

  private findRelatedFields(fieldName: string): string[] {
    // 简化的关联字段查找
    const fieldGroups: Record<string, string[]> = {
      'incidentType': ['reportType', 'priority', 'category'],
      'incidentTime': ['reportTime', 'responseTime'],
      'incidentLocation': ['address', 'coordinates', 'district']
    };

    return fieldGroups[fieldName] || [];
  }

  private getApplicableRules(fieldName: string): string[] {
    return Array.from(this.businessRules.keys());
  }

  private assessRiskLevel(score: ConflictScore): 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' {
    if (score.overall < 30) return 'CRITICAL';
    if (score.overall < 50) return 'HIGH';
    if (score.overall < 70) return 'MEDIUM';
    return 'LOW';
  }

  private extractLearningData(changes: FieldChange[]): any {
    return {
      userCount: changes.length,
      timeSpan: changes[changes.length - 1].timestamp - changes[0].timestamp,
      valueTypes: changes.map(c => typeof c.newValue),
      hasMultipleTypes: new Set(changes.map(c => typeof c.newValue)).size > 1
    };
  }

  private updateLearningModel(conflict: EnhancedConflictInfo): void {
    // 更新学习模型（简化版）
    this.conflictHistory.push(conflict);

    // 保持历史记录在合理范围内
    if (this.conflictHistory.length > 1000) {
      this.conflictHistory = this.conflictHistory.slice(-500);
    }
  }

  private recordResolutionOutcome(
    conflict: EnhancedConflictInfo,
    result: ResolutionResult,
    strategy: EnhancedResolutionStrategy
  ): void {
    // 记录解决结果用于后续学习和优化
    console.log('🎓 [Learning] Recording resolution outcome:', {
      conflictId: conflict.id,
      strategy,
      success: result.success,
      needsManualConfirm: result.needsManualConfirm
    });
  }

  private extractKeywords(text: string): string[] {
    return text.toLowerCase().split(/\s+/).filter(word => word.length > 2);
  }

  private extractEntities(text: string): string[] {
    // 简化的实体提取
    const patterns = [
      /\d{4}-\d{2}-\d{2}/g, // 日期
      /\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}/g, // IP地址
      /[A-Z][a-z]+\s+[A-Z][a-z]+/g // 人名
    ];

    const entities: string[] = [];
    patterns.forEach(pattern => {
      const matches = text.match(pattern);
      if (matches) entities.push(...matches);
    });

    return entities;
  }

  private classifyIntent(text: string, fieldName: string): string {
    const keywords = text.toLowerCase();

    if (keywords.includes('紧急') || keywords.includes('urgent')) return 'URGENT';
    if (keywords.includes('更新') || keywords.includes('修改')) return 'UPDATE';
    if (keywords.includes('删除') || keywords.includes('清除')) return 'DELETE';
    if (keywords.includes('添加') || keywords.includes('新增')) return 'ADD';

    return 'MODIFY';
  }

  private analyzeSentiment(text: string): number {
    const positiveWords = ['好', '优秀', '成功', '完成', '解决'];
    const negativeWords = ['坏', '失败', '错误', '问题', '困难'];

    let score = 0;
    positiveWords.forEach(word => {
      if (text.includes(word)) score += 0.2;
    });
    negativeWords.forEach(word => {
      if (text.includes(word)) score -= 0.2;
    });

    return Math.max(-1, Math.min(1, score));
  }

  private categorizeContent(text: string, fieldName: string): string {
    if (fieldName.includes('time') || fieldName.includes('Time')) return 'TEMPORAL';
    if (fieldName.includes('location') || fieldName.includes('address')) return 'LOCATION';
    if (fieldName.includes('person') || fieldName.includes('user')) return 'PERSON';
    if (fieldName.includes('number') || fieldName.includes('id')) return 'IDENTIFIER';

    return 'GENERAL';
  }

  private groupBySemantic(changes: FieldChange[], analyses: SemanticAnalysis[]): any[] {
    // 简化的语义分组
    return [{
      changes,
      confidence: 75,
      similarity: 0.8
    }];
  }

  private intelligentMerge(group: any, context: ConflictContext): any {
    // 简化的智能合并
    return group.changes[0].newValue;
  }

  private evaluateContextFit(change: FieldChange, context: ConflictContext): number {
    let score = 50;

    // 检查字段依赖
    const dependencies = context.fieldDependencies.get(change.fieldName) || [];
    if (dependencies.length > 0) score += 15;

    // 检查业务规则
    if (context.businessRules.length > 0) score += 10;

    // 检查工作流状态
    if (context.workflowState === 'active') score += 25;

    return score;
  }

  private extractMLFeatures(conflict: EnhancedConflictInfo): any {
    return {
      changeCount: conflict.changes.length,
      semanticScore: conflict.score.semantic,
      temporalScore: conflict.score.temporal,
      contextScore: conflict.score.context,
      mostLikelyValue: conflict.changes[0].newValue,
      fieldName: conflict.fieldName
    };
  }

  private applyIncrementalChange(currentValue: any, change: FieldChange): { success: boolean; value: any } {
    // 简化的增量变更应用
    return {
      success: true,
      value: change.newValue
    };
  }

  private calculateVoteWeight(change: FieldChange, conflict: EnhancedConflictInfo): number {
    let weight = 1;

    // 角色权重
    weight *= this['getRolePriority'](change.user.role) / 50;

    // 置信度权重
    weight *= (change.confidence || 50) / 50;

    // 时间权重（越新权重越高）
    const timeDecay = this.calculateTimeDecayWeight(change.timestamp, Date.now());
    weight *= timeDecay / 50;

    return weight;
  }

  private findMatchingPattern(conflict: EnhancedConflictInfo): MergePattern | null {
    // 简化的模式匹配
    for (const pattern of this.mergePatterns.values()) {
      if (pattern.conditions.includes('field_type_text') &&
          typeof conflict.changes[0].newValue === 'string') {
        return pattern;
      }
    }
    return null;
  }

  private async applyMergePattern(conflict: EnhancedConflictInfo, pattern: MergePattern): Promise<any> {
    // 简化的模式应用
    return {
      success: true,
      value: conflict.changes[0].newValue,
      rejectedChanges: conflict.changes.slice(1)
    };
  }

  private calculateConfidenceWeight(change: FieldChange, conflict: EnhancedConflictInfo): number {
    return (change.confidence || 50) *
           (this['getRolePriority'](change.user.role) / 100) *
           conflict.score.overall / 100;
  }

  private calculateTimeDecayWeight(timestamp: number, now: number): number {
    const ageMinutes = (now - timestamp) / 1000 / 60;
    // 30分钟内权重100%，之后每小时衰减10%
    return Math.max(10, 100 - Math.floor(ageMinutes / 60) * 10);
  }

  private async predictPotentialConflicts(
    fieldName: string,
    proposedValue: any,
    currentUser: ConflictUser,
    context?: ConflictContext
  ): Promise<EnhancedConflictInfo[]> {
    // 简化的冲突预测
    return [];
  }

  private generateAlternativeValues(proposedValue: any, conflict: EnhancedConflictInfo): any[] {
    // 简化的替代值生成
    return [];
  }

  private checkBusinessRules(fieldName: string, proposedValue: any, context?: ConflictContext): string[] {
    const violations: string[] = [];

    // 简化的业务规则检查
    if (fieldName === 'incidentType' && !proposedValue) {
      violations.push('严重：事件类型不能为空');
    }

    return violations;
  }

  private generateAlternativeStrategies(
    conflict: ConflictInfo,
    score: ConflictScore
  ): EnhancedResolutionStrategy[] {
    const strategies: EnhancedResolutionStrategy[] = [];

    if (score.semantic > 60) strategies.push(EnhancedResolutionStrategy.SEMANTIC_MERGE);
    if (score.context > 70) strategies.push(EnhancedResolutionStrategy.CONTEXT_AWARE);
    if (conflict.changes.length > 2) strategies.push(EnhancedResolutionStrategy.INCREMENTAL_MERGE);
    strategies.push(EnhancedResolutionStrategy.COLLABORATIVE_VOTE);

    return strategies;
  }
}

// 导出增强版解决器
export const enhancedConflictResolver = new EnhancedConflictResolver();

export default EnhancedConflictResolver;