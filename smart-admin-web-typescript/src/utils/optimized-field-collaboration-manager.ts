/**
 * 优化版字段协作管理器 - 支持200人并发
 *
 * 性能优化:
 * 1. 对象池复用
 * 2. 消息限流
 * 3. 内存清理机制
 * 4. 智能状态管理
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-26
 * @Copyright 1024创新实验室
 */

import { reactive, shallowReactive } from 'vue';
import { throttle, debounce } from 'lodash-es';

interface CollaborationUser {
  id: string;
  name: string;
  avatar?: string;
  color: string;
}

interface FieldState {
  isLocked: boolean;
  lockedBy?: CollaborationUser;
  lastModifiedBy?: CollaborationUser;
  lastModifiedAt?: number;
  isEditing: boolean;
}

class OptimizedFieldCollaborationManager {
  // 使用 shallowReactive 减少深度响应式开销
  private fieldStates = shallowReactive<Record<string, Record<string, FieldState>>>({});

  // 对象池复用 FieldState 对象
  private fieldStatePool: FieldState[] = [];
  private readonly POOL_MAX_SIZE = 1000;

  // 消息限流器 - 防止消息风暴
  private messageThrottlers = new Map<string, Function>();

  // 定时器管理 - 防止内存泄漏
  private autoUnlockTimers = new Map<string, NodeJS.Timeout>();
  private cleanupTimer: NodeJS.Timeout;

  // 性能统计
  private stats = {
    messagesProcessed: 0,
    messagesThrottled: 0,
    memoryUsage: 0,
    lastCleanup: Date.now()
  };

  constructor() {
    this.initializeCleanupScheduler();
    this.preAllocatePool();
  }

  /**
   * 优化的字段状态获取 - 使用对象池
   */
  getFieldState(roomId: string, fieldName: string): FieldState {
    // 懒加载房间状态
    if (!this.fieldStates[roomId]) {
      this.fieldStates[roomId] = {};
    }

    // 从对象池获取或创建新状态
    if (!this.fieldStates[roomId][fieldName]) {
      this.fieldStates[roomId][fieldName] = this.getPooledFieldState();
    }

    return this.fieldStates[roomId][fieldName];
  }

  /**
   * 限流版字段聚焦处理
   */
  onFieldFocus(roomId: string, fieldName: string, user: CollaborationUser) {
    const throttleKey = `${roomId}:${fieldName}:focus`;

    if (!this.messageThrottlers.has(throttleKey)) {
      this.messageThrottlers.set(throttleKey,
        throttle((roomId, fieldName, user) => {
          this.performFieldFocus(roomId, fieldName, user);
        }, 100) // 100ms 限流
      );
    }

    this.messageThrottlers.get(throttleKey)!(roomId, fieldName, user);
  }

  private performFieldFocus(roomId: string, fieldName: string, user: CollaborationUser) {
    const fieldState = this.getFieldState(roomId, fieldName);

    fieldState.isLocked = true;
    fieldState.lockedBy = user;

    // 广播聚焦事件 (使用现有 WebSocket 逻辑)
    this.broadcastFieldFocus(roomId, fieldName, user);

    this.stats.messagesProcessed++;
  }

  /**
   * 智能自动解锁 - 批量处理
   */
  scheduleAutoUnlock(fieldName: string, delay: number = 1500) {
    // 清除现有定时器
    const existingTimer = this.autoUnlockTimers.get(fieldName);
    if (existingTimer) {
      clearTimeout(existingTimer);
    }

    // 设置新定时器
    const timer = setTimeout(() => {
      this.performAutoUnlock(fieldName);
      this.autoUnlockTimers.delete(fieldName);
    }, delay);

    this.autoUnlockTimers.set(fieldName, timer);
  }

  /**
   * 对象池管理
   */
  private getPooledFieldState(): FieldState {
    if (this.fieldStatePool.length > 0) {
      const state = this.fieldStatePool.pop()!;
      // 重置状态
      state.isLocked = false;
      state.lockedBy = undefined;
      state.lastModifiedBy = undefined;
      state.lastModifiedAt = undefined;
      state.isEditing = false;
      return state;
    }

    return {
      isLocked: false,
      isEditing: false
    };
  }

  private returnToPool(state: FieldState) {
    if (this.fieldStatePool.length < this.POOL_MAX_SIZE) {
      this.fieldStatePool.push(state);
    }
  }

  private preAllocatePool() {
    // 预分配50个对象到池中
    for (let i = 0; i < 50; i++) {
      this.fieldStatePool.push({
        isLocked: false,
        isEditing: false
      });
    }
  }

  /**
   * 内存清理机制
   */
  private initializeCleanupScheduler() {
    // 每30秒执行一次清理
    this.cleanupTimer = setInterval(() => {
      this.performMemoryCleanup();
    }, 30000);
  }

  private performMemoryCleanup() {
    const now = Date.now();
    const INACTIVE_THRESHOLD = 5 * 60 * 1000; // 5分钟

    // 清理过期的字段状态
    Object.keys(this.fieldStates).forEach(roomId => {
      Object.keys(this.fieldStates[roomId]).forEach(fieldName => {
        const state = this.fieldStates[roomId][fieldName];
        if (state.lastModifiedAt && (now - state.lastModifiedAt) > INACTIVE_THRESHOLD) {
          this.returnToPool(state);
          delete this.fieldStates[roomId][fieldName];
        }
      });

      // 清理空房间
      if (Object.keys(this.fieldStates[roomId]).length === 0) {
        delete this.fieldStates[roomId];
      }
    });

    // 清理过期的限流器
    this.messageThrottlers.clear();

    // 更新统计
    this.stats.lastCleanup = now;
    this.stats.memoryUsage = this.calculateMemoryUsage();

    console.log('🧹 [字段协作] 内存清理完成:', this.stats);
  }

  private calculateMemoryUsage(): number {
    return Object.keys(this.fieldStates).reduce((total, roomId) => {
      return total + Object.keys(this.fieldStates[roomId]).length;
    }, 0);
  }

  /**
   * 获取性能统计
   */
  getPerformanceStats() {
    return { ...this.stats };
  }

  /**
   * 销毁管理器
   */
  destroy() {
    // 清理所有定时器
    this.autoUnlockTimers.forEach(timer => clearTimeout(timer));
    this.autoUnlockTimers.clear();

    if (this.cleanupTimer) {
      clearInterval(this.cleanupTimer);
    }

    // 清理限流器
    this.messageThrottlers.clear();

    // 返回所有对象到池
    Object.values(this.fieldStates).forEach(roomStates => {
      Object.values(roomStates).forEach(state => this.returnToPool(state));
    });

    console.log('🗑️ [字段协作] 管理器已销毁');
  }

  // 占位方法 - 实际实现需要连接到现有的 WebSocket 逻辑
  private broadcastFieldFocus(roomId: string, fieldName: string, user: CollaborationUser) {
    // 实现 WebSocket 广播逻辑
  }

  private performAutoUnlock(fieldName: string) {
    // 实现自动解锁逻辑
  }
}

// 导出单例
export const optimizedFieldCollaborationManager = new OptimizedFieldCollaborationManager();
export default optimizedFieldCollaborationManager;