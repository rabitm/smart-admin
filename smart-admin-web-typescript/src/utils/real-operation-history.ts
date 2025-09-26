/**
 * 真实操作历史记录管理器 - 集成后端API
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-24
 * @Copyright 1024创新实验室
 */

import { useUserStore } from '/@/store/modules/system/user';

// 操作类型枚举
export enum RealOperationType {
  FIELD_CHANGE = 'field_change',
  RECORD_CREATE = 'record_create',
  RECORD_UPDATE = 'record_update',
  RECORD_DELETE = 'record_delete',
  STATUS_CHANGE = 'status_change',
  ASSIGNMENT_CHANGE = 'assignment_change',
  ATTACHMENT_ADD = 'attachment_add',
  ATTACHMENT_REMOVE = 'attachment_remove',
  COMMENT_ADD = 'comment_add',
  APPROVAL_ACTION = 'approval_action'
}

// 操作历史记录接口
export interface RealOperationRecord {
  id: string;
  type: RealOperationType;
  entityType: string;        // 实体类型: emergency_report, police_case 等
  entityId: string | number; // 实体ID
  fieldName?: string;        // 字段名称
  fieldLabel?: string;       // 字段显示名称
  oldValue?: any;           // 旧值
  newValue?: any;           // 新值
  userId: string | number;   // 操作用户ID
  userName: string;         // 操作用户姓名
  userDepartment?: string;  // 用户部门
  timestamp: number;        // 操作时间戳
  clientTimestamp: number;  // 客户端时间戳
  ipAddress?: string;       // IP地址
  userAgent?: string;       // 用户代理
  description?: string;     // 操作描述
  metadata?: Record<string, any>; // 扩展元数据
  severity: 'low' | 'medium' | 'high' | 'critical'; // 操作严重级别
}

// API响应接口
interface OperationHistoryResponse {
  success: boolean;
  data: {
    records: RealOperationRecord[];
    total: number;
    pageNum: number;
    pageSize: number;
  };
  message?: string;
}

// 查询参数接口
export interface OperationHistoryQuery {
  entityType: string;
  entityId: string | number;
  userId?: string | number;
  fieldName?: string;
  operationType?: RealOperationType;
  startTime?: number;
  endTime?: number;
  pageNum?: number;
  pageSize?: number;
  severity?: string;
}

// 统计信息接口
export interface OperationStatistics {
  totalOperations: number;
  operationsByType: Record<RealOperationType, number>;
  operationsByUser: Record<string, number>;
  operationsByHour: Record<string, number>;
  recentActivity: number; // 最近1小时的操作数
}

/**
 * 真实操作历史记录管理器
 */
export class RealOperationHistoryManager {
  private baseURL = '/api/collaboration/history';
  private userStore = useUserStore();

  // 缓存
  private cache: Map<string, { data: any; timestamp: number }> = new Map();
  private cacheExpiry = 60000; // 1分钟缓存过期

  // 实时更新回调
  private listeners: Array<(records: RealOperationRecord[]) => void> = [];

  constructor() {
    // 初始化WebSocket连接用于实时更新
    this.initializeRealtimeUpdates();
  }

  /**
   * 记录操作历史
   */
  async recordOperation(
    type: RealOperationType,
    entityType: string,
    entityId: string | number,
    options: {
      fieldName?: string;
      fieldLabel?: string;
      oldValue?: any;
      newValue?: any;
      description?: string;
      metadata?: Record<string, any>;
      severity?: 'low' | 'medium' | 'high' | 'critical';
    } = {}
  ): Promise<boolean> {
    try {
      // 构建后端API期望的数据格式
      const record = {
        type: String(type), // 确保类型是字符串
        entityType: String(entityType), // 确保类型是字符串
        entityId: Number(entityId), // 确保是数字类型
        fieldName: options.fieldName || null,
        fieldLabel: options.fieldLabel || null,
        oldValue: options.oldValue,
        newValue: options.newValue,
        userId: Number(this.userStore.employeeId), // 确保是数字类型
        userName: String(this.userStore.actualName || '未知用户'), // 确保是字符串
        userDepartment: this.userStore.departmentName || null,
        clientTimestamp: Date.now(),
        ipAddress: await this.getClientIP(),
        userAgent: navigator.userAgent,
        description: String(options.description || this.generateDescription(type, options)),
        metadata: {
          ...options.metadata,
          url: window.location.href,
          referrer: document.referrer,
          viewport: {
            width: window.innerWidth,
            height: window.innerHeight
          }
        },
        severity: String(options.severity || this.calculateSeverity(type, options))
      };

      const response = await fetch(`${this.baseURL}/record`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.userStore.token}`
        },
        body: JSON.stringify(record)
      });

      if (response.ok) {
        console.log(`📝 [RealOperationHistory] 记录操作成功: ${type}`);

        // 清理相关缓存
        this.invalidateCache(entityType, entityId);

        return true;
      } else {
        console.error(`❌ [RealOperationHistory] 记录操作失败: ${response.status} ${response.statusText}`);

        // 尝试获取错误详情，但如果失败就忽略
        try {
          const errorText = await response.text();
          if (errorText) {
            console.error(`❌ [RealOperationHistory] 错误详情:`, errorText);
          }
        } catch (parseError) {
          console.warn(`⚠️ [RealOperationHistory] 无法解析错误响应`, parseError);
        }

        return false;
      }

    } catch (error) {
      console.error(`💥 [RealOperationHistory] 记录操作异常:`, error);

      // 离线模式：保存到本地存储，等待网络恢复后同步
      this.saveToOfflineQueue(type, entityType, entityId, options);

      return false;
    }
  }

  /**
   * 查询操作历史
   */
  async getOperationHistory(query: OperationHistoryQuery): Promise<RealOperationRecord[]> {
    try {
      // 检查缓存
      const cacheKey = this.generateCacheKey(query);
      const cached = this.getCachedData(cacheKey);
      if (cached) {
        return cached;
      }

      const params = new URLSearchParams();
      Object.entries(query).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          params.append(key, String(value));
        }
      });

      const response = await fetch(`${this.baseURL}/query?${params}`, {
        headers: {
          'Authorization': `Bearer ${this.userStore.token}`
        }
      });

      if (response.ok) {
        try {
          const result: OperationHistoryResponse = await response.json();

          if (result.success) {
            // 缓存结果
            this.setCachedData(cacheKey, result.data.records);

            console.log(`📋 [RealOperationHistory] 查询操作历史成功: ${result.data.records.length}条记录`);
            return result.data.records;
          } else {
            console.error(`❌ [RealOperationHistory] 查询操作历史失败:`, result.message);
            return [];
          }
        } catch (parseError) {
          console.error(`❌ [RealOperationHistory] 查询响应解析失败:`, parseError);
          console.error(`❌ [RealOperationHistory] 响应状态: ${response.status} ${response.statusText}`);
          console.error(`❌ [RealOperationHistory] 响应可能是HTML格式而非JSON`);

          return [];
        }
      } else {
        console.error(`❌ [RealOperationHistory] 查询操作历史请求失败: ${response.status} ${response.statusText}`);
        return [];
      }

    } catch (error) {
      console.error(`💥 [RealOperationHistory] 查询操作历史异常:`, error);
      return [];
    }
  }

  /**
   * 获取操作统计信息
   */
  async getOperationStatistics(
    entityType: string,
    entityId: string | number,
    timeRange: {
      startTime: number;
      endTime: number;
    }
  ): Promise<OperationStatistics | null> {
    try {
      const response = await fetch(`${this.baseURL}/statistics`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.userStore.token}`
        },
        body: JSON.stringify({
          entityType,
          entityId,
          ...timeRange
        })
      });

      if (response.ok) {
        const result = await response.json();
        if (result.success) {
          return result.data;
        }
      }

      return null;
    } catch (error) {
      console.error(`💥 [RealOperationHistory] 获取统计信息异常:`, error);
      return null;
    }
  }

  /**
   * 撤销操作（如果支持）
   */
  async undoOperation(operationId: string): Promise<boolean> {
    try {
      const response = await fetch(`${this.baseURL}/undo/${operationId}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.userStore.token}`
        }
      });

      if (response.ok) {
        const result = await response.json();
        if (result.success) {
          console.log(`↩️ [RealOperationHistory] 撤销操作成功: ${operationId}`);
          return true;
        }
      }

      return false;
    } catch (error) {
      console.error(`💥 [RealOperationHistory] 撤销操作异常:`, error);
      return false;
    }
  }

  /**
   * 导出操作历史
   */
  async exportHistory(
    query: OperationHistoryQuery,
    format: 'excel' | 'pdf' | 'csv' = 'excel'
  ): Promise<Blob | null> {
    try {
      const params = new URLSearchParams();
      Object.entries(query).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          params.append(key, String(value));
        }
      });
      params.append('format', format);

      const response = await fetch(`${this.baseURL}/export?${params}`, {
        headers: {
          'Authorization': `Bearer ${this.userStore.token}`
        }
      });

      if (response.ok) {
        return await response.blob();
      }

      return null;
    } catch (error) {
      console.error(`💥 [RealOperationHistory] 导出历史异常:`, error);
      return null;
    }
  }

  /**
   * 添加实时更新监听器
   */
  addListener(callback: (records: RealOperationRecord[]) => void): () => void {
    this.listeners.push(callback);

    // 返回取消监听的函数
    return () => {
      const index = this.listeners.indexOf(callback);
      if (index > -1) {
        this.listeners.splice(index, 1);
      }
    };
  }

  // =============== 私有方法 ===============

  /**
   * 初始化实时更新
   */
  private initializeRealtimeUpdates(): void {
    // 这里应该建立WebSocket连接，监听实时更新
    // 暂时使用轮询模式作为fallback

    if (typeof window !== 'undefined') {
      // 每30秒检查一次未读更新
      setInterval(() => {
        // 这里可以调用API检查是否有新的操作记录
        // 然后通知所有监听器
      }, 30000);
    }
  }

  /**
   * 生成操作描述
   */
  private generateDescription(
    type: RealOperationType,
    options: { fieldLabel?: string; oldValue?: any; newValue?: any }
  ): string {
    const { fieldLabel, oldValue, newValue } = options;

    switch (type) {
      case RealOperationType.FIELD_CHANGE:
        return `修改了${fieldLabel || '字段'}：从"${oldValue}"改为"${newValue}"`;
      case RealOperationType.RECORD_CREATE:
        return '创建了新记录';
      case RealOperationType.RECORD_UPDATE:
        return '更新了记录';
      case RealOperationType.RECORD_DELETE:
        return '删除了记录';
      case RealOperationType.STATUS_CHANGE:
        return `状态从"${oldValue}"变更为"${newValue}"`;
      case RealOperationType.ASSIGNMENT_CHANGE:
        return `指派从"${oldValue}"变更为"${newValue}"`;
      case RealOperationType.ATTACHMENT_ADD:
        return `添加了附件"${newValue}"`;
      case RealOperationType.ATTACHMENT_REMOVE:
        return `删除了附件"${oldValue}"`;
      case RealOperationType.COMMENT_ADD:
        return '添加了评论';
      case RealOperationType.APPROVAL_ACTION:
        return `执行了审批操作：${newValue}`;
      default:
        return '执行了操作';
    }
  }

  /**
   * 计算操作严重级别
   */
  private calculateSeverity(
    type: RealOperationType,
    options: { fieldName?: string }
  ): 'low' | 'medium' | 'high' | 'critical' {
    // 根据操作类型和字段重要性计算严重级别
    const criticalFields = ['status', 'assignee', 'priority', 'reportType'];
    const highFields = ['description', 'location', 'time', 'reporter'];

    if (type === RealOperationType.RECORD_DELETE) {
      return 'critical';
    }

    if (type === RealOperationType.STATUS_CHANGE || type === RealOperationType.ASSIGNMENT_CHANGE) {
      return 'high';
    }

    if (type === RealOperationType.FIELD_CHANGE && options.fieldName) {
      if (criticalFields.includes(options.fieldName)) {
        return 'critical';
      }
      if (highFields.includes(options.fieldName)) {
        return 'high';
      }
    }

    if (type === RealOperationType.RECORD_CREATE) {
      return 'medium';
    }

    return 'low';
  }

  /**
   * 获取客户端IP地址
   */
  private async getClientIP(): Promise<string> {
    try {
      // 这里可以调用API获取客户端IP
      // 或者从请求头中获取
      return 'unknown';
    } catch {
      return 'unknown';
    }
  }

  /**
   * 生成缓存键
   */
  private generateCacheKey(query: OperationHistoryQuery): string {
    return `history_${JSON.stringify(query)}`;
  }

  /**
   * 获取缓存数据
   */
  private getCachedData(key: string): any {
    const cached = this.cache.get(key);
    if (cached && Date.now() - cached.timestamp < this.cacheExpiry) {
      return cached.data;
    }
    return null;
  }

  /**
   * 设置缓存数据
   */
  private setCachedData(key: string, data: any): void {
    this.cache.set(key, {
      data,
      timestamp: Date.now()
    });
  }

  /**
   * 清理缓存
   */
  private invalidateCache(entityType: string, entityId: string | number): void {
    const keysToDelete: string[] = [];

    for (const [key] of this.cache) {
      if (key.includes(`"entityType":"${entityType}"`) &&
          key.includes(`"entityId":"${entityId}"`)) {
        keysToDelete.push(key);
      }
    }

    keysToDelete.forEach(key => this.cache.delete(key));
  }

  /**
   * 保存到离线队列
   */
  private saveToOfflineQueue(
    type: RealOperationType,
    entityType: string,
    entityId: string | number,
    options: any
  ): void {
    try {
      const offlineQueue = JSON.parse(localStorage.getItem('offline_operations') || '[]');

      offlineQueue.push({
        type,
        entityType,
        entityId,
        options,
        timestamp: Date.now()
      });

      localStorage.setItem('offline_operations', JSON.stringify(offlineQueue));

      console.log(`💾 [RealOperationHistory] 操作已保存到离线队列`);
    } catch (error) {
      console.error(`💥 [RealOperationHistory] 保存离线队列失败:`, error);
    }
  }

  /**
   * 同步离线队列
   */
  async syncOfflineQueue(): Promise<void> {
    try {
      const offlineQueue = JSON.parse(localStorage.getItem('offline_operations') || '[]');

      if (offlineQueue.length === 0) return;

      console.log(`🔄 [RealOperationHistory] 开始同步离线队列: ${offlineQueue.length}条记录`);

      const syncResults = [];

      for (const item of offlineQueue) {
        const success = await this.recordOperation(
          item.type,
          item.entityType,
          item.entityId,
          item.options
        );

        syncResults.push({ item, success });
      }

      // 移除同步成功的记录
      const failedItems = syncResults
        .filter(result => !result.success)
        .map(result => result.item);

      localStorage.setItem('offline_operations', JSON.stringify(failedItems));

      console.log(`✅ [RealOperationHistory] 离线队列同步完成: 成功${syncResults.filter(r => r.success).length}条，失败${failedItems.length}条`);

    } catch (error) {
      console.error(`💥 [RealOperationHistory] 同步离线队列异常:`, error);
    }
  }

  /**
   * 销毁管理器
   */
  destroy(): void {
    this.listeners = [];
    this.cache.clear();
    console.log(`🛑 [RealOperationHistory] 操作历史管理器已销毁`);
  }
}

/**
 * 创建全局实例
 */
export const realOperationHistoryManager = new RealOperationHistoryManager();

/**
 * 创建新的操作历史管理器实例
 */
export function createRealOperationHistoryManager(): RealOperationHistoryManager {
  return new RealOperationHistoryManager();
}

/**
 * 便捷方法：记录字段变化
 */
export const recordFieldChange = async (
  entityType: string,
  entityId: string | number,
  fieldName: string,
  fieldLabel: string,
  oldValue: any,
  newValue: any
): Promise<boolean> => {
  return await realOperationHistoryManager.recordOperation(
    RealOperationType.FIELD_CHANGE,
    entityType,
    entityId,
    {
      fieldName,
      fieldLabel,
      oldValue,
      newValue,
      severity: 'medium'
    }
  );
};

/**
 * 便捷方法：记录状态变化
 */
export const recordStatusChange = async (
  entityType: string,
  entityId: string | number,
  oldStatus: string,
  newStatus: string
): Promise<boolean> => {
  return await realOperationHistoryManager.recordOperation(
    RealOperationType.STATUS_CHANGE,
    entityType,
    entityId,
    {
      fieldName: 'status',
      fieldLabel: '状态',
      oldValue: oldStatus,
      newValue: newStatus,
      severity: 'high'
    }
  );
};