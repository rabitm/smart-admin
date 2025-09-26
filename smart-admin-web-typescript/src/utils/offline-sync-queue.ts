/*
 * 离线同步队列系统 - 支持离线编辑和同步队列
 *
 * 功能特性：
 * 1. 离线数据存储和管理
 * 2. 网络状态监控和自动同步
 * 3. 同步冲突检测和处理
 * 4. 数据完整性保证
 * 5. 错误重试机制
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-24
 * @Copyright 1024创新实验室
 */

import { ConflictResolver, FieldChange, ConflictInfo } from './conflict-resolver';

// 离线操作类型
export enum OfflineOperationType {
  FIELD_UPDATE = 'FIELD_UPDATE',
  FIELD_CREATE = 'FIELD_CREATE',
  FIELD_DELETE = 'FIELD_DELETE',
  RECORD_CREATE = 'RECORD_CREATE',
  RECORD_UPDATE = 'RECORD_UPDATE',
  RECORD_DELETE = 'RECORD_DELETE'
}

// 同步状态
export enum SyncStatus {
  PENDING = 'PENDING',         // 等待同步
  SYNCING = 'SYNCING',         // 同步中
  SYNCED = 'SYNCED',           // 已同步
  FAILED = 'FAILED',           // 同步失败
  CONFLICTED = 'CONFLICTED'    // 发生冲突
}

// 离线操作记录
export interface OfflineOperation {
  id: string;
  type: OfflineOperationType;
  reportId: number;
  fieldName: string;
  oldValue: any;
  newValue: any;
  timestamp: number;
  userId: number;
  userName: string;
  status: SyncStatus;
  retryCount: number;
  maxRetries: number;
  error?: string;
  conflictId?: string;
}

// 同步结果
export interface SyncResult {
  success: boolean;
  operationId: string;
  status: SyncStatus;
  error?: string;
  conflictInfo?: ConflictInfo;
}

// 网络状态信息
export interface NetworkStatus {
  online: boolean;
  lastOnlineTime: number;
  lastOfflineTime: number;
  connectionType?: string;
}

/**
 * 离线同步队列管理器
 */
export class OfflineSyncQueue {
  private queue: Map<string, OfflineOperation> = new Map();
  private networkStatus: NetworkStatus;
  private syncInterval: NodeJS.Timeout | null = null;
  private conflictResolver: ConflictResolver;
  private storageKey = 'police_offline_sync_queue';
  private maxRetries = 3;
  private syncIntervalMs = 30000; // 30秒

  // 事件监听器
  private listeners: {
    [key: string]: ((data: any) => void)[];
  } = {};

  constructor(conflictResolver: ConflictResolver) {
    this.conflictResolver = conflictResolver;
    this.networkStatus = {
      online: navigator.onLine,
      lastOnlineTime: navigator.onLine ? Date.now() : 0,
      lastOfflineTime: navigator.onLine ? 0 : Date.now()
    };

    this.initializeQueue();
    this.setupNetworkMonitoring();
    this.startAutoSync();
  }

  /**
   * 添加离线操作到队列
   */
  addOperation(
    type: OfflineOperationType,
    reportId: number,
    fieldName: string,
    oldValue: any,
    newValue: any,
    userId: number,
    userName: string
  ): string {
    const operation: OfflineOperation = {
      id: this.generateOperationId(),
      type,
      reportId,
      fieldName,
      oldValue,
      newValue,
      timestamp: Date.now(),
      userId,
      userName,
      status: SyncStatus.PENDING,
      retryCount: 0,
      maxRetries: this.maxRetries
    };

    this.queue.set(operation.id, operation);
    this.saveQueueToStorage();

    console.log(`📝 [OfflineSyncQueue] 添加离线操作: ${type} - ${fieldName}`);

    // 触发事件
    this.emit('operationAdded', operation);

    // 如果在线，立即尝试同步
    if (this.networkStatus.online) {
      this.syncSingleOperation(operation.id);
    }

    return operation.id;
  }

  /**
   * 获取待同步操作数量
   */
  getPendingCount(): number {
    return Array.from(this.queue.values())
      .filter(op => op.status === SyncStatus.PENDING || op.status === SyncStatus.FAILED)
      .length;
  }

  /**
   * 获取冲突操作数量
   */
  getConflictCount(): number {
    return Array.from(this.queue.values())
      .filter(op => op.status === SyncStatus.CONFLICTED)
      .length;
  }

  /**
   * 获取所有操作
   */
  getAllOperations(): OfflineOperation[] {
    return Array.from(this.queue.values())
      .sort((a, b) => b.timestamp - a.timestamp);
  }

  /**
   * 获取待同步的操作
   */
  getPendingOperations(): OfflineOperation[] {
    return Array.from(this.queue.values())
      .filter(op => op.status === SyncStatus.PENDING || op.status === SyncStatus.FAILED)
      .sort((a, b) => a.timestamp - b.timestamp);
  }

  /**
   * 获取冲突的操作
   */
  getConflictedOperations(): OfflineOperation[] {
    return Array.from(this.queue.values())
      .filter(op => op.status === SyncStatus.CONFLICTED)
      .sort((a, b) => b.timestamp - a.timestamp);
  }

  /**
   * 手动触发同步
   */
  async syncAll(): Promise<Map<string, SyncResult>> {
    if (!this.networkStatus.online) {
      console.log('🔒 [OfflineSyncQueue] 离线状态，无法同步');
      return new Map();
    }

    const pendingOps = this.getPendingOperations();
    const results = new Map<string, SyncResult>();

    console.log(`🔄 [OfflineSyncQueue] 开始同步 ${pendingOps.length} 个操作`);

    for (const operation of pendingOps) {
      const result = await this.syncSingleOperation(operation.id);
      results.set(operation.id, result);
    }

    this.emit('syncCompleted', results);
    return results;
  }

  /**
   * 同步单个操作
   */
  async syncSingleOperation(operationId: string): Promise<SyncResult> {
    const operation = this.queue.get(operationId);
    if (!operation) {
      return {
        success: false,
        operationId,
        status: SyncStatus.FAILED,
        error: '操作不存在'
      };
    }

    if (operation.status === SyncStatus.SYNCING) {
      return {
        success: false,
        operationId,
        status: SyncStatus.SYNCING,
        error: '操作正在同步中'
      };
    }

    // 更新状态为同步中
    operation.status = SyncStatus.SYNCING;
    this.queue.set(operationId, operation);
    this.saveQueueToStorage();

    try {
      // 模拟API调用
      const syncResult = await this.performSync(operation);

      if (syncResult.success) {
        // 同步成功
        operation.status = SyncStatus.SYNCED;
        operation.error = undefined;
        // 成功日志太多，减少噪音，只在开发模式显示
        if (process.env.NODE_ENV === 'development') {
          console.log(`✅ [OfflineSyncQueue] 操作同步成功: ${operationId}`);
        }

        // 从队列中移除已同步的操作
        this.queue.delete(operationId);

      } else if (syncResult.conflictInfo) {
        // 发生冲突
        operation.status = SyncStatus.CONFLICTED;
        operation.conflictId = syncResult.conflictInfo.id;
        operation.error = '数据冲突，需要手动解决';
        console.log(`⚠️ [OfflineSyncQueue] 操作发生冲突: ${operationId}`);

      } else {
        // 同步失败，准备重试
        operation.retryCount++;
        if (operation.retryCount >= operation.maxRetries) {
          operation.status = SyncStatus.FAILED;
          operation.error = syncResult.error || '同步失败，已达最大重试次数';
          console.log(`❌ [OfflineSyncQueue] 操作同步失败: ${operationId}`);
        } else {
          operation.status = SyncStatus.PENDING;
          operation.error = syncResult.error || '同步失败，将重试';
          // 重试日志减少噪音，只显示第1次和最后一次重试
          if (operation.retryCount === 1 || operation.retryCount === operation.maxRetries - 1) {
            console.log(`🔄 [OfflineSyncQueue] 操作同步失败，将重试: ${operationId} (${operation.retryCount}/${operation.maxRetries})`);
          }
        }
      }

      this.queue.set(operationId, operation);
      this.saveQueueToStorage();

      return {
        success: syncResult.success,
        operationId,
        status: operation.status,
        error: operation.error,
        conflictInfo: syncResult.conflictInfo
      };

    } catch (error) {
      // 异常处理
      operation.retryCount++;
      if (operation.retryCount >= operation.maxRetries) {
        operation.status = SyncStatus.FAILED;
        operation.error = `同步异常: ${error.message}`;
      } else {
        operation.status = SyncStatus.PENDING;
        operation.error = `同步异常，将重试: ${error.message}`;
      }

      this.queue.set(operationId, operation);
      this.saveQueueToStorage();

      console.error(`💥 [OfflineSyncQueue] 操作同步异常: ${operationId}`, error);

      return {
        success: false,
        operationId,
        status: operation.status,
        error: operation.error
      };
    }
  }

  /**
   * 清除已同步的操作
   */
  clearSyncedOperations(): void {
    const syncedOps = Array.from(this.queue.values())
      .filter(op => op.status === SyncStatus.SYNCED);

    for (const op of syncedOps) {
      this.queue.delete(op.id);
    }

    this.saveQueueToStorage();
    console.log(`🗑️ [OfflineSyncQueue] 清除了 ${syncedOps.length} 个已同步操作`);
  }

  /**
   * 清除失败的操作
   */
  clearFailedOperations(): void {
    const failedOps = Array.from(this.queue.values())
      .filter(op => op.status === SyncStatus.FAILED);

    for (const op of failedOps) {
      this.queue.delete(op.id);
    }

    this.saveQueueToStorage();
    console.log(`🗑️ [OfflineSyncQueue] 清除了 ${failedOps.length} 个失败操作`);
  }

  /**
   * 重试失败的操作
   */
  async retryFailedOperations(): Promise<Map<string, SyncResult>> {
    const failedOps = this.getConflictedOperations().concat(
      Array.from(this.queue.values()).filter(op => op.status === SyncStatus.FAILED)
    );

    const results = new Map<string, SyncResult>();

    for (const operation of failedOps) {
      // 重置重试计数
      operation.retryCount = 0;
      operation.status = SyncStatus.PENDING;
      operation.error = undefined;

      const result = await this.syncSingleOperation(operation.id);
      results.set(operation.id, result);
    }

    return results;
  }

  /**
   * 获取网络状态
   */
  getNetworkStatus(): NetworkStatus {
    return { ...this.networkStatus };
  }

  /**
   * 监听事件
   */
  on(event: string, callback: (data: any) => void): void {
    if (!this.listeners[event]) {
      this.listeners[event] = [];
    }
    this.listeners[event].push(callback);
  }

  /**
   * 移除事件监听
   */
  off(event: string, callback?: (data: any) => void): void {
    if (!this.listeners[event]) return;

    if (callback) {
      const index = this.listeners[event].indexOf(callback);
      if (index > -1) {
        this.listeners[event].splice(index, 1);
      }
    } else {
      this.listeners[event] = [];
    }
  }

  /**
   * 销毁队列
   */
  destroy(): void {
    if (this.syncInterval) {
      clearInterval(this.syncInterval);
      this.syncInterval = null;
    }

    window.removeEventListener('online', this.handleOnline);
    window.removeEventListener('offline', this.handleOffline);

    this.listeners = {};
    console.log('🛑 [OfflineSyncQueue] 队列已销毁');
  }

  // =============== 私有方法 ===============

  /**
   * 初始化队列
   */
  private initializeQueue(): void {
    try {
      const stored = localStorage.getItem(this.storageKey);
      if (stored) {
        const operations: OfflineOperation[] = JSON.parse(stored);
        for (const op of operations) {
          // 重置同步中的状态
          if (op.status === SyncStatus.SYNCING) {
            op.status = SyncStatus.PENDING;
          }
          this.queue.set(op.id, op);
        }
        console.log(`📂 [OfflineSyncQueue] 加载了 ${operations.length} 个离线操作`);
      }
    } catch (error) {
      console.error('💥 [OfflineSyncQueue] 加载离线队列失败:', error);
      localStorage.removeItem(this.storageKey);
    }
  }

  /**
   * 保存队列到存储
   */
  private saveQueueToStorage(): void {
    try {
      const operations = Array.from(this.queue.values());
      localStorage.setItem(this.storageKey, JSON.stringify(operations));
    } catch (error) {
      console.error('💥 [OfflineSyncQueue] 保存离线队列失败:', error);
    }
  }

  /**
   * 设置网络监控
   */
  private setupNetworkMonitoring(): void {
    window.addEventListener('online', this.handleOnline.bind(this));
    window.addEventListener('offline', this.handleOffline.bind(this));
  }

  /**
   * 处理网络上线
   */
  private handleOnline = (): void => {
    console.log('🌐 [OfflineSyncQueue] 网络已连接');
    this.networkStatus.online = true;
    this.networkStatus.lastOnlineTime = Date.now();

    this.emit('networkOnline', this.networkStatus);

    // 自动同步待处理的操作
    setTimeout(() => {
      this.syncAll();
    }, 1000);
  };

  /**
   * 处理网络离线
   */
  private handleOffline = (): void => {
    console.log('📵 [OfflineSyncQueue] 网络已断开');
    this.networkStatus.online = false;
    this.networkStatus.lastOfflineTime = Date.now();

    this.emit('networkOffline', this.networkStatus);
  };

  /**
   * 启动自动同步
   */
  private startAutoSync(): void {
    this.syncInterval = setInterval(() => {
      if (this.networkStatus.online && this.getPendingCount() > 0) {
        console.log('🔄 [OfflineSyncQueue] 定时同步检查');
        this.syncAll();
      }
    }, this.syncIntervalMs);
  }

  /**
   * 执行同步操作
   */
  private async performSync(operation: OfflineOperation): Promise<{
    success: boolean;
    error?: string;
    conflictInfo?: ConflictInfo;
  }> {
    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 1000 + Math.random() * 2000));

    // 模拟不同的同步结果
    const rand = Math.random();

    if (rand < 0.1) {
      // 10% 概率发生冲突
      const conflictInfo = this.simulateConflict(operation);
      return {
        success: false,
        conflictInfo
      };
    } else if (rand < 0.2) {
      // 10% 概率同步失败
      return {
        success: false,
        error: '服务器错误或网络异常'
      };
    } else {
      // 80% 概率同步成功
      return {
        success: true
      };
    }
  }

  /**
   * 模拟冲突情况
   */
  private simulateConflict(operation: OfflineOperation): ConflictInfo {
    const conflictChange: FieldChange = {
      fieldName: operation.fieldName,
      fieldLabel: operation.fieldName,
      fieldType: 'text',
      oldValue: operation.oldValue,
      newValue: `服务器值_${Date.now()}`, // 模拟服务器上的不同值
      user: {
        id: operation.userId + 1000, // 模拟其他用户
        name: '其他用户',
        role: 'officer',
        priority: 50,
        timestamp: Date.now() - 5000
      },
      timestamp: Date.now() - 5000
    };

    const localChange: FieldChange = {
      fieldName: operation.fieldName,
      fieldLabel: operation.fieldName,
      fieldType: 'text',
      oldValue: operation.oldValue,
      newValue: operation.newValue,
      user: {
        id: operation.userId,
        name: operation.userName,
        role: 'detective',
        priority: 60,
        timestamp: operation.timestamp
      },
      timestamp: operation.timestamp
    };

    return this.conflictResolver.detectConflict(operation.fieldName, [conflictChange, localChange])!;
  }

  /**
   * 触发事件
   */
  private emit(event: string, data: any): void {
    const callbacks = this.listeners[event];
    if (callbacks) {
      callbacks.forEach(callback => {
        try {
          callback(data);
        } catch (error) {
          console.error(`💥 [OfflineSyncQueue] 事件回调执行失败: ${event}`, error);
        }
      });
    }
  }

  /**
   * 生成操作ID
   */
  private generateOperationId(): string {
    return `offline_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`;
  }
}

/**
 * 创建离线同步队列实例
 */
export function createOfflineSyncQueue(conflictResolver: ConflictResolver): OfflineSyncQueue {
  return new OfflineSyncQueue(conflictResolver);
}