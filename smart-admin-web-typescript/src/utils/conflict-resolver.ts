/*
 * 智能冲突解决器 - 处理协作编辑中的数据冲突
 *
 * 功能特性：
 * 1. 多种冲突解决策略
 * 2. 字段类型智能识别
 * 3. 用户权限和角色考虑
 * 4. 时间戳优先级判断
 * 5. 自动合并和手动确认
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-24
 * @Copyright 1024创新实验室
 */

// 冲突类型枚举
export enum ConflictType {
  CONCURRENT_EDIT = 'CONCURRENT_EDIT',     // 并发编辑
  LATE_UPDATE = 'LATE_UPDATE',             // 延迟更新
  PERMISSION_CONFLICT = 'PERMISSION_CONFLICT', // 权限冲突
  DATA_TYPE_MISMATCH = 'DATA_TYPE_MISMATCH'   // 数据类型不匹配
}

// 解决策略枚举
export enum ResolutionStrategy {
  LAST_WRITE_WINS = 'LAST_WRITE_WINS',         // 最后写入胜出
  FIRST_WRITE_WINS = 'FIRST_WRITE_WINS',       // 最先写入胜出
  ROLE_BASED = 'ROLE_BASED',                   // 基于角色权限
  MANUAL_MERGE = 'MANUAL_MERGE',               // 手动合并
  AUTO_MERGE = 'AUTO_MERGE',                   // 自动合并
  REJECT_ALL = 'REJECT_ALL'                    // 拒绝所有变更
}

// 用户信息接口
export interface ConflictUser {
  id: number;
  name: string;
  role: string;
  priority: number;
  timestamp: number;
}

// 字段变更信息
export interface FieldChange {
  fieldName: string;
  fieldLabel: string;
  fieldType: string;
  oldValue: any;
  newValue: any;
  user: ConflictUser;
  timestamp: number;
  confidence?: number;
}

// 冲突信息
export interface ConflictInfo {
  id: string;
  type: ConflictType;
  fieldName: string;
  fieldLabel: string;
  changes: FieldChange[];
  autoResolvable: boolean;
  suggestedStrategy: ResolutionStrategy;
  reportId: number;
  timestamp: number;
}

// 解决结果
export interface ResolutionResult {
  success: boolean;
  strategy: ResolutionStrategy;
  finalValue: any;
  rejectedChanges: FieldChange[];
  reason: string;
  needsManualConfirm: boolean;
  mergeDetails?: any;
}

/**
 * 智能冲突解决器
 */
export class ConflictResolver {
  private roleHierarchy: Map<string, number> = new Map([
    ['admin', 100],
    ['supervisor', 80],
    ['detective', 60],
    ['officer', 40],
    ['clerk', 20]
  ]);

  private fieldPriorities: Map<string, number> = new Map([
    ['incidentNumber', 100],        // 案件编号
    ['incidentType', 90],           // 案件类型
    ['reportingPerson', 85],        // 报案人
    ['incidentTime', 80],           // 发生时间
    ['incidentLocation', 75],       // 发生地点
    ['description', 50],            // 描述信息
    ['notes', 30],                  // 备注
    ['tags', 20]                    // 标签
  ]);

  /**
   * 检测冲突
   */
  detectConflict(
    fieldName: string,
    changes: FieldChange[]
  ): ConflictInfo | null {
    if (changes.length < 2) return null;

    // 按时间戳排序
    const sortedChanges = changes.sort((a, b) => a.timestamp - b.timestamp);

    // 检查是否有真正的冲突
    const hasConflict = this.hasActualConflict(sortedChanges);
    if (!hasConflict) return null;

    const conflictType = this.determineConflictType(sortedChanges);
    const suggestedStrategy = this.suggestResolutionStrategy(fieldName, sortedChanges);

    return {
      id: this.generateConflictId(),
      type: conflictType,
      fieldName,
      fieldLabel: changes[0].fieldLabel,
      changes: sortedChanges,
      autoResolvable: this.isAutoResolvable(conflictType, sortedChanges),
      suggestedStrategy,
      reportId: 0, // 由调用方设置
      timestamp: Date.now()
    };
  }

  /**
   * 解决冲突
   */
  async resolveConflict(
    conflict: ConflictInfo,
    strategy?: ResolutionStrategy,
    manualValue?: any
  ): Promise<ResolutionResult> {
    const resolveStrategy = strategy || conflict.suggestedStrategy;

    try {
      switch (resolveStrategy) {
        case ResolutionStrategy.LAST_WRITE_WINS:
          return this.resolveByTimestamp(conflict, false);

        case ResolutionStrategy.FIRST_WRITE_WINS:
          return this.resolveByTimestamp(conflict, true);

        case ResolutionStrategy.ROLE_BASED:
          return this.resolveByRole(conflict);

        case ResolutionStrategy.AUTO_MERGE:
          return this.autoMerge(conflict);

        case ResolutionStrategy.MANUAL_MERGE:
          return this.manualMerge(conflict, manualValue);

        case ResolutionStrategy.REJECT_ALL:
          return this.rejectAll(conflict);

        default:
          throw new Error(`未支持的解决策略: ${resolveStrategy}`);
      }
    } catch (error) {
      console.error('冲突解决失败:', error);
      return {
        success: false,
        strategy: resolveStrategy,
        finalValue: conflict.changes[0].oldValue,
        rejectedChanges: conflict.changes,
        reason: `解决失败: ${error.message}`,
        needsManualConfirm: true
      };
    }
  }

  /**
   * 批量解决冲突
   */
  async resolveBatchConflicts(
    conflicts: ConflictInfo[],
    strategy?: ResolutionStrategy
  ): Promise<Map<string, ResolutionResult>> {
    const results = new Map<string, ResolutionResult>();

    for (const conflict of conflicts) {
      try {
        const result = await this.resolveConflict(conflict, strategy);
        results.set(conflict.id, result);
      } catch (error) {
        results.set(conflict.id, {
          success: false,
          strategy: strategy || conflict.suggestedStrategy,
          finalValue: null,
          rejectedChanges: conflict.changes,
          reason: `批量解决失败: ${error.message}`,
          needsManualConfirm: true
        });
      }
    }

    return results;
  }

  // =============== 私有方法 ===============

  /**
   * 检查是否有实际冲突
   */
  private hasActualConflict(changes: FieldChange[]): boolean {
    if (changes.length < 2) return false;

    // 检查值是否真的不同
    const values = changes.map(change => this.normalizeValue(change.newValue));
    return !values.every(value => this.isEqual(value, values[0]));
  }

  /**
   * 确定冲突类型
   */
  private determineConflictType(changes: FieldChange[]): ConflictType {
    const timeGap = changes[changes.length - 1].timestamp - changes[0].timestamp;

    // 5秒内的变更认为是并发编辑
    if (timeGap <= 5000) {
      return ConflictType.CONCURRENT_EDIT;
    }

    // 检查数据类型不匹配
    const types = changes.map(change => typeof change.newValue);
    if (!types.every(type => type === types[0])) {
      return ConflictType.DATA_TYPE_MISMATCH;
    }

    // 检查权限冲突
    const hasPermissionIssue = changes.some(change =>
      this.getRolePriority(change.user.role) === 0
    );
    if (hasPermissionIssue) {
      return ConflictType.PERMISSION_CONFLICT;
    }

    return ConflictType.LATE_UPDATE;
  }

  /**
   * 建议解决策略
   */
  private suggestResolutionStrategy(
    fieldName: string,
    changes: FieldChange[]
  ): ResolutionStrategy {
    const fieldPriority = this.fieldPriorities.get(fieldName) || 50;

    // 高优先级字段使用角色解决
    if (fieldPriority >= 80) {
      return ResolutionStrategy.ROLE_BASED;
    }

    // 文本类型字段尝试自动合并
    const isTextField = changes[0].fieldType === 'text' ||
                       changes[0].fieldType === 'textarea';
    if (isTextField && changes.length === 2) {
      return ResolutionStrategy.AUTO_MERGE;
    }

    // 其他情况使用最后写入胜出
    return ResolutionStrategy.LAST_WRITE_WINS;
  }

  /**
   * 检查是否可自动解决
   */
  private isAutoResolvable(type: ConflictType, changes: FieldChange[]): boolean {
    switch (type) {
      case ConflictType.CONCURRENT_EDIT:
        return changes.length === 2;
      case ConflictType.LATE_UPDATE:
        return true;
      case ConflictType.PERMISSION_CONFLICT:
        return false;
      case ConflictType.DATA_TYPE_MISMATCH:
        return false;
      default:
        return false;
    }
  }

  /**
   * 按时间戳解决
   */
  private resolveByTimestamp(
    conflict: ConflictInfo,
    useFirst: boolean
  ): ResolutionResult {
    const sortedChanges = conflict.changes.sort((a, b) =>
      useFirst ? a.timestamp - b.timestamp : b.timestamp - a.timestamp
    );

    const winnerChange = sortedChanges[0];
    const rejectedChanges = sortedChanges.slice(1);

    return {
      success: true,
      strategy: useFirst ? ResolutionStrategy.FIRST_WRITE_WINS : ResolutionStrategy.LAST_WRITE_WINS,
      finalValue: winnerChange.newValue,
      rejectedChanges,
      reason: `采用${useFirst ? '最先' : '最后'}写入的值: ${winnerChange.user.name}`,
      needsManualConfirm: false
    };
  }

  /**
   * 按角色权限解决
   */
  private resolveByRole(conflict: ConflictInfo): ResolutionResult {
    const changesByRole = conflict.changes.sort((a, b) =>
      this.getRolePriority(b.user.role) - this.getRolePriority(a.user.role)
    );

    const winnerChange = changesByRole[0];
    const rejectedChanges = changesByRole.slice(1);

    return {
      success: true,
      strategy: ResolutionStrategy.ROLE_BASED,
      finalValue: winnerChange.newValue,
      rejectedChanges,
      reason: `采用高权限用户的值: ${winnerChange.user.name} (${winnerChange.user.role})`,
      needsManualConfirm: false
    };
  }

  /**
   * 自动合并
   */
  private autoMerge(conflict: ConflictInfo): ResolutionResult {
    if (conflict.changes.length !== 2) {
      return {
        success: false,
        strategy: ResolutionStrategy.AUTO_MERGE,
        finalValue: conflict.changes[0].oldValue,
        rejectedChanges: conflict.changes,
        reason: '自动合并仅支持两个变更',
        needsManualConfirm: true
      };
    }

    const [change1, change2] = conflict.changes;
    const mergedValue = this.mergeValues(
      change1.newValue,
      change2.newValue,
      change1.oldValue
    );

    if (mergedValue === null) {
      return {
        success: false,
        strategy: ResolutionStrategy.AUTO_MERGE,
        finalValue: conflict.changes[0].oldValue,
        rejectedChanges: conflict.changes,
        reason: '无法自动合并这些值',
        needsManualConfirm: true
      };
    }

    return {
      success: true,
      strategy: ResolutionStrategy.AUTO_MERGE,
      finalValue: mergedValue,
      rejectedChanges: [],
      reason: '已自动合并两个变更',
      needsManualConfirm: false,
      mergeDetails: {
        originalValues: [change1.newValue, change2.newValue],
        mergedValue
      }
    };
  }

  /**
   * 手动合并
   */
  private manualMerge(
    conflict: ConflictInfo,
    manualValue: any
  ): ResolutionResult {
    return {
      success: true,
      strategy: ResolutionStrategy.MANUAL_MERGE,
      finalValue: manualValue,
      rejectedChanges: conflict.changes,
      reason: '用户手动指定的值',
      needsManualConfirm: false
    };
  }

  /**
   * 拒绝所有变更
   */
  private rejectAll(conflict: ConflictInfo): ResolutionResult {
    return {
      success: true,
      strategy: ResolutionStrategy.REJECT_ALL,
      finalValue: conflict.changes[0].oldValue,
      rejectedChanges: conflict.changes,
      reason: '已拒绝所有变更，保持原值',
      needsManualConfirm: false
    };
  }

  /**
   * 合并值
   */
  private mergeValues(value1: any, value2: any, originalValue: any): any {
    // 如果值相同，直接返回
    if (this.isEqual(value1, value2)) {
      return value1;
    }

    // 字符串合并
    if (typeof value1 === 'string' && typeof value2 === 'string') {
      return this.mergeStrings(value1, value2, originalValue);
    }

    // 数组合并
    if (Array.isArray(value1) && Array.isArray(value2)) {
      return this.mergeArrays(value1, value2);
    }

    // 对象合并
    if (typeof value1 === 'object' && typeof value2 === 'object' &&
        value1 !== null && value2 !== null) {
      return this.mergeObjects(value1, value2);
    }

    // 其他类型无法合并
    return null;
  }

  /**
   * 合并字符串
   */
  private mergeStrings(str1: string, str2: string, original: string): string {
    // 简单的文本合并策略
    if (str1.length > str2.length) {
      return str1; // 选择更长的文本
    } else if (str2.length > str1.length) {
      return str2;
    } else {
      // 长度相同，选择与原文本差异更大的
      const diff1 = this.getStringDifference(original, str1);
      const diff2 = this.getStringDifference(original, str2);
      return diff1 > diff2 ? str1 : str2;
    }
  }

  /**
   * 合并数组
   */
  private mergeArrays(arr1: any[], arr2: any[]): any[] {
    // 去重合并
    const merged = [...arr1];
    for (const item of arr2) {
      if (!merged.some(existing => this.isEqual(existing, item))) {
        merged.push(item);
      }
    }
    return merged;
  }

  /**
   * 合并对象
   */
  private mergeObjects(obj1: any, obj2: any): any {
    return { ...obj1, ...obj2 };
  }

  /**
   * 获取角色优先级
   */
  private getRolePriority(role: string): number {
    return this.roleHierarchy.get(role.toLowerCase()) || 0;
  }

  /**
   * 规范化值
   */
  private normalizeValue(value: any): any {
    if (typeof value === 'string') {
      return value.trim().toLowerCase();
    }
    return value;
  }

  /**
   * 比较值是否相等
   */
  private isEqual(value1: any, value2: any): boolean {
    if (value1 === value2) return true;
    if (value1 == null || value2 == null) return value1 === value2;

    if (typeof value1 !== typeof value2) return false;

    if (typeof value1 === 'object') {
      return JSON.stringify(value1) === JSON.stringify(value2);
    }

    return false;
  }

  /**
   * 获取字符串差异度
   */
  private getStringDifference(str1: string, str2: string): number {
    let differences = 0;
    const maxLength = Math.max(str1.length, str2.length);

    for (let i = 0; i < maxLength; i++) {
      if (str1[i] !== str2[i]) {
        differences++;
      }
    }

    return differences / maxLength;
  }

  /**
   * 生成冲突ID
   */
  private generateConflictId(): string {
    return `conflict_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`;
  }
}

/**
 * 创建冲突解决器实例
 */
export function createConflictResolver(): ConflictResolver {
  return new ConflictResolver();
}