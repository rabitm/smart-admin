/**
 * 优化版字段协作管理器 - 支持200人并发
 * 处理字段级别的实时协作
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
import globalCollaborationManager from './global-collaboration-manager';
import { UnifiedWebSocketClient } from './unified-websocket-client';
import { getWebSocketClient } from './websocket-manager';
import { createBusinessMessage } from '/@/types/websocket';

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

class FieldCollaborationManager {
  // 使用 shallowReactive 减少深度响应式开销
  private fieldStates = shallowReactive<Record<string, Record<string, FieldState>>>({});

  // 当前用户正在编辑的字段
  private currentEditingFields = new Set<string>();

  // 自动解锁定时器
  private autoUnlockTimers = new Map<string, NodeJS.Timeout>();

  // WebSocket连接
  private wsClient: UnifiedWebSocketClient | null = null;

  // 对象池复用 FieldState 对象
  private fieldStatePool: FieldState[] = [];
  private readonly POOL_MAX_SIZE = 1000;

  // 消息限流器 - 防止消息风暴
  private messageThrottlers = new Map<string, Function>();

  // 定时器管理 - 防止内存泄漏
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

    // 为调试添加状态访问日志 (减少频率以提高性能)
    if (process.env.NODE_ENV === 'development' && Math.random() < 0.01) { // 减少到1%概率
      console.log('🔍 [Field Collaboration] getFieldState:', {
        roomId,
        fieldName,
        state: this.fieldStates[roomId][fieldName],
        poolSize: this.fieldStatePool.length,
        memoryUsage: this.stats.memoryUsage
      });
    }

    return this.fieldStates[roomId][fieldName];
  }

  /**
   * 限流版字段聚焦处理 - 支持高频操作
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

    // 首先释放当前用户锁定的其他字段（解决单选/多选字段切换问题）
    this.releaseUserLockedFields(roomId, user, fieldName);

    // 锁定字段
    fieldState.isLocked = true;
    fieldState.lockedBy = user;
    fieldState.lastModifiedAt = Date.now();

    // 添加到当前编辑字段列表
    this.currentEditingFields.add(fieldName);

    // 广播事件 - 使用统一WebSocket客户端广播
    const wsClient = getWebSocketClient();
    if (wsClient && wsClient.isConnected) {
      const message = createBusinessMessage('police', 'FIELD_FOCUS', {
        roomId,
        fieldName,
        user,
        timestamp: Date.now()
      });
      wsClient.send(message);
      console.log(`📡 [Field Collaboration] WebSocket广播字段聚焦: ${fieldName}`);
    } else {
      console.warn(`⚠️ [Field Collaboration] WebSocket未连接，无法广播字段聚焦: ${fieldName}`);
    }

    // 发送活动事件
    globalCollaborationManager.emit('field_focus', {
      userId: user.id,
      fieldName,
      user,
      timestamp: Date.now()
    });

    this.stats.messagesProcessed++;
    console.log(`🎯 [Field Collaboration] 用户 ${user.name} 聚焦字段 ${fieldName} (已处理: ${this.stats.messagesProcessed})`);
  }

  /**
   * 用户离开字段
   */
  onFieldBlur(roomId: string, fieldName: string, user: CollaborationUser) {
    const fieldState = this.getFieldState(roomId, fieldName);

    console.log(`🔍 [Field Collaboration] onFieldBlur 开始:`, {
      fieldName,
      user: user.name,
      userId: user.id,
      beforeState: {
        isLocked: fieldState.isLocked,
        lockedBy: fieldState.lockedBy?.id,
        lockedByName: fieldState.lockedBy?.name
      }
    });

    // 如果是当前用户锁定的字段，解除锁定
    if (fieldState.lockedBy?.id === user.id) {
      fieldState.isLocked = false;
      fieldState.lockedBy = undefined;

      console.log(`✅ [Field Collaboration] 字段已解锁:`, {
        fieldName,
        user: user.name,
        afterState: {
          isLocked: fieldState.isLocked,
          lockedBy: fieldState.lockedBy
        }
      });
    } else {
      console.log(`❌ [Field Collaboration] 用户无权解锁:`, {
        fieldName,
        requestUser: user.id,
        lockedByUser: fieldState.lockedBy?.id
      });
    }

    // 从当前编辑字段列表移除
    this.currentEditingFields.delete(fieldName);

    // 广播事件 - 使用统一WebSocket客户端广播，确保其他用户能看到解锁状态
    const wsClient = getWebSocketClient();
    if (wsClient && wsClient.isConnected) {
      const message = createBusinessMessage('police', 'FIELD_BLUR', {
        roomId,
        fieldName,
        user,
        timestamp: Date.now(),
        isAutoUnlock: false // 标记为非自动解锁事件
      });
      wsClient.send(message);
      console.log(`📡 [Field Collaboration] WebSocket广播字段失焦: ${fieldName}`);
    } else {
      console.warn(`⚠️ [Field Collaboration] WebSocket未连接，无法广播字段失焦: ${fieldName}`);
    }

    console.log(`👋 [Field Collaboration] 用户 ${user.name} 离开字段 ${fieldName}`);
  }

  /**
   * 用户编辑字段内容
   */
  onFieldEdit(roomId: string, fieldName: string, user: CollaborationUser, content?: string) {
    const fieldState = this.getFieldState(roomId, fieldName);

    fieldState.isEditing = true;
    fieldState.lastModifiedBy = user;
    fieldState.lastModifiedAt = Date.now();

    // 广播编辑事件 - 暂时禁用，使用syncManager替代
    // globalCollaborationManager.sendMessage({
    //   type: 'field_edit',
    //   roomId,
    //   fieldName,
    //   user,
    //   content,
    //   timestamp: Date.now()
    // });

    // 发送活动事件 - 保留本地事件触发
    globalCollaborationManager.emit('field_edit', {
      userId: user.id,
      fieldName,
      user,
      content,
      timestamp: Date.now()
    });

    // 为单选/多选字段设置自动解锁定时器（1.5秒后自动解锁，给其他用户足够时间看到锁定状态）
    // 判断是否是单选/多选字段：以field_开头的专业字段通常都是选择类型
    if (fieldName.startsWith('field_') || content === 'AUTO_UNLOCK') {
      console.log(`🎯 [Field Collaboration] 触发自动解锁机制: ${fieldName}, content: ${content}`);
      this.scheduleAutoUnlock(roomId, fieldName, user, 1500);
    }

    console.log(`✏️ [Field Collaboration] 用户 ${user.name} 编辑字段 ${fieldName}`);
  }

  /**
   * 用户保存字段内容
   */
  onFieldSave(roomId: string, fieldName: string, user: CollaborationUser, content?: string) {
    const fieldState = this.getFieldState(roomId, fieldName);

    fieldState.isEditing = false;
    fieldState.lastModifiedBy = user;
    fieldState.lastModifiedAt = Date.now();

    // 广播保存事件 - 暂时禁用，使用syncManager替代
    // globalCollaborationManager.sendMessage({
    //   type: 'field_save',
    //   roomId,
    //   fieldName,
    //   user,
    //   content,
    //   timestamp: Date.now()
    // });

    // 发送活动事件
    globalCollaborationManager.emit('field_save', {
      userId: user.id,
      fieldName,
      user,
      content,
      timestamp: Date.now()
    });

    console.log(`💾 [Field Collaboration] 用户 ${user.name} 保存字段 ${fieldName}`);
  }

  /**
   * 处理远程字段事件
   */
  handleRemoteFieldEvent(data: any) {
    if (!data || !data.type || !data.roomId || !data.fieldName || !data.user) {
      console.warn('🔧 [Field Collaboration] 无效的远程字段事件数据:', data);
      return;
    }

    const { type, roomId, fieldName, user } = data;

    try {
      const fieldState = this.getFieldState(roomId, fieldName);

      if (!fieldState) {
        console.warn('🔧 [Field Collaboration] 无法获取字段状态:', roomId, fieldName);
        return;
      }

      switch (type) {
        case 'field_focus':
          if (user.id !== this.getCurrentUserId()) {
            fieldState.isLocked = true;
            fieldState.lockedBy = user;
          }
          break;

        case 'field_blur':
          if (fieldState.lockedBy?.id === user.id) {
            fieldState.isLocked = false;
            fieldState.lockedBy = undefined;
          }
          break;

        case 'field_edit':
          if (user.id !== this.getCurrentUserId()) {
            fieldState.isEditing = true;
            fieldState.lastModifiedBy = user;
            fieldState.lastModifiedAt = data.timestamp || Date.now();
          }
          break;

        case 'field_save':
          if (user.id !== this.getCurrentUserId()) {
            fieldState.isEditing = false;
            fieldState.lastModifiedBy = user;
            fieldState.lastModifiedAt = data.timestamp || Date.now();
          }
          break;

        default:
          console.warn('🔧 [Field Collaboration] 未知的字段事件类型:', type);
      }
    } catch (error) {
      console.error('🔧 [Field Collaboration] 处理远程字段事件时出错:', error, data);
    }
  }

  /**
   * 清理房间状态
   */
  clearRoomStates(roomId: string) {
    if (!roomId) return;

    try {
      if (this.fieldStates[roomId]) {
        delete this.fieldStates[roomId];
      }
      this.currentEditingFields.clear();
      console.log(`🧹 [Field Collaboration] 已清理房间状态: ${roomId}`);
    } catch (error) {
      console.error('🧹 [Field Collaboration] 清理房间状态时出错:', error, roomId);
    }
  }

  /**
   * 获取当前用户ID
   */
  private getCurrentUserId(): string {
    return globalCollaborationManager.state.currentUser?.id || '';
  }

  /**
   * 创建字段包装器指令
   */
  createFieldDirective(roomId: string, fieldName: string) {
    return {
      mounted(el: HTMLElement) {
        if (!el || !roomId || !fieldName) {
          console.warn('🔧 [Field Collaboration] 无效的指令参数:', { el, roomId, fieldName });
          return;
        }

        const user = globalCollaborationManager.state.currentUser;
        if (!user) {
          console.warn('🔧 [Field Collaboration] 用户未登录，跳过字段监听');
          return;
        }

        try {
          // 监听聚焦事件
          const handleFocus = () => {
            try {
              fieldCollaborationManager.onFieldFocus(roomId, fieldName, user);
            } catch (error) {
              console.error('🔧 [Field Collaboration] 聚焦事件处理出错:', error);
            }
          };

          // 监听失焦事件
          const handleBlur = () => {
            try {
              fieldCollaborationManager.onFieldBlur(roomId, fieldName, user);
            } catch (error) {
              console.error('🔧 [Field Collaboration] 失焦事件处理出错:', error);
            }
          };

          // 监听输入事件
          let editTimer: NodeJS.Timeout;
          const handleInput = () => {
            try {
              clearTimeout(editTimer);
              editTimer = setTimeout(() => {
                const content = (el as HTMLInputElement).value || (el as HTMLTextAreaElement).value;
                fieldCollaborationManager.onFieldEdit(roomId, fieldName, user, content);
              }, 500); // 防抖处理
            } catch (error) {
              console.error('🔧 [Field Collaboration] 输入事件处理出错:', error);
            }
          };

          el.addEventListener('focus', handleFocus);
          el.addEventListener('blur', handleBlur);
          el.addEventListener('input', handleInput);

          // 存储事件处理器以便清理
          (el as any)._collaborationCleanup = () => {
            try {
              el.removeEventListener('focus', handleFocus);
              el.removeEventListener('blur', handleBlur);
              el.removeEventListener('input', handleInput);
              clearTimeout(editTimer);
            } catch (error) {
              console.error('🔧 [Field Collaboration] 清理事件监听器时出错:', error);
            }
          };
        } catch (error) {
          console.error('🔧 [Field Collaboration] 挂载指令时出错:', error);
        }
      },

      unmounted(el: HTMLElement) {
        try {
          if (el && (el as any)._collaborationCleanup) {
            (el as any)._collaborationCleanup();
            delete (el as any)._collaborationCleanup;
          }
        } catch (error) {
          console.error('🔧 [Field Collaboration] 卸载指令时出错:', error);
        }
      }
    };
  }

  /**
   * 自动解锁字段（由定时器触发）
   */
  private onAutoFieldUnlock(roomId: string, fieldName: string, user: CollaborationUser) {
    const fieldState = this.getFieldState(roomId, fieldName);

    console.log(`🔍 [Field Collaboration] onAutoFieldUnlock 开始:`, {
      fieldName,
      user: user.name,
      userId: user.id,
      beforeState: {
        isLocked: fieldState.isLocked,
        lockedBy: fieldState.lockedBy?.id,
        lockedByName: fieldState.lockedBy?.name
      }
    });

    // 如果是当前用户锁定的字段，解除锁定
    if (fieldState.lockedBy?.id === user.id) {
      fieldState.isLocked = false;
      fieldState.lockedBy = undefined;

      console.log(`✅ [Field Collaboration] 自动解锁完成:`, {
        fieldName,
        user: user.name,
        afterState: {
          isLocked: fieldState.isLocked,
          lockedBy: fieldState.lockedBy
        }
      });
    }

    // 从当前编辑字段列表移除
    this.currentEditingFields.delete(fieldName);

    // 广播自动解锁事件
    const wsClient = getWebSocketClient();
    if (wsClient && wsClient.isConnected) {
      // 从roomId提取reportId (格式: police-report-5)
      const reportId = roomId.replace('police-report-', '');

      const messageData = {
        reportId: parseInt(reportId), // 使用reportId，不是roomId
        fieldName,
        timestamp: Date.now(),
        isAutoUnlock: true // 标记为自动解锁事件
      };
      const message = createBusinessMessage('police', 'FIELD_BLUR', messageData);

      console.log(`🚨 [CRITICAL] 准备广播自动解锁WebSocket消息:`, {
        fieldName,
        user: user.name,
        userId: user.id,
        reportId: parseInt(reportId),
        isAutoUnlock: true,
        messageData,
        message,
        wsConnected: wsClient.isConnected,
        wsState: wsClient.state
      });

      try {
        const sendResult = wsClient.send(message);
        console.log(`🚨 [CRITICAL] 自动解锁WebSocket消息发送结果:`, {
          fieldName,
          sendResult,
          message,
          isPromise: sendResult instanceof Promise
        });

        if (sendResult instanceof Promise) {
          sendResult.then(result => {
            console.log(`🚨 [CRITICAL] 异步发送结果:`, { fieldName, result });
          }).catch(error => {
            console.error(`🚨 [CRITICAL] 异步发送失败:`, { fieldName, error });
          });
        }
      } catch (error) {
        console.error(`🚨 [CRITICAL] WebSocket消息发送异常:`, { fieldName, error });
      }
    } else {
      console.error(`🚨 [CRITICAL] WebSocket状态异常:`, {
        fieldName,
        hasClient: !!wsClient,
        isConnected: wsClient?.isConnected,
        state: wsClient?.state
      });
    }

    console.log(`⏰ [Field Collaboration] 自动解锁字段 ${fieldName} (用户: ${user.name})`);
  }

  /**
   * 设置自动解锁定时器
   */
  private scheduleAutoUnlock(roomId: string, fieldName: string, user: CollaborationUser, delayMs: number) {
    const key = `${roomId}:${fieldName}`;

    // 清除已存在的定时器
    if (this.autoUnlockTimers.has(key)) {
      clearTimeout(this.autoUnlockTimers.get(key)!);
    }

    // 设置新的定时器
    const timer = setTimeout(() => {
      console.log(`⏰ [Field Collaboration] 自动解锁字段: ${fieldName}`);
      // 调用自动解锁方法，标记为自动解锁事件
      this.onAutoFieldUnlock(roomId, fieldName, user);
      this.autoUnlockTimers.delete(key);
    }, delayMs);

    this.autoUnlockTimers.set(key, timer);
    console.log(`⏲️ [Field Collaboration] 设置自动解锁定时器: ${fieldName} (${delayMs}ms)`);
  }

  /**
   * 清除自动解锁定时器
   */
  private clearAutoUnlockTimer(roomId: string, fieldName: string) {
    const key = `${roomId}:${fieldName}`;
    if (this.autoUnlockTimers.has(key)) {
      clearTimeout(this.autoUnlockTimers.get(key)!);
      this.autoUnlockTimers.delete(key);
      console.log(`🗑️ [Field Collaboration] 清除自动解锁定时器: ${fieldName}`);
    }
  }

  /**
   * 释放用户锁定的其他字段（除了当前字段）
   * 解决单选/多选字段切换时的自动释放问题
   */
  private releaseUserLockedFields(roomId: string, user: CollaborationUser, currentFieldName: string) {
    const roomState = this.fieldStates[roomId];
    if (!roomState) return;

    const fieldsToRelease: string[] = [];

    // 遍历所有字段，找到该用户锁定的其他字段
    for (const [fieldName, fieldState] of Object.entries(roomState)) {
      if (fieldName !== currentFieldName &&
          fieldState.isLocked &&
          fieldState.lockedBy?.id === user.id) {
        fieldsToRelease.push(fieldName);
      }
    }

    // 释放找到的字段
    for (const fieldName of fieldsToRelease) {
      const fieldState = roomState[fieldName];
      fieldState.isLocked = false;
      fieldState.lockedBy = undefined;

      // 清除自动解锁定时器
      this.clearAutoUnlockTimer(roomId, fieldName);

      // 从当前编辑字段列表移除
      this.currentEditingFields.delete(fieldName);

      console.log(`🔓 [Field Collaboration] 自动释放用户锁定的字段: ${fieldName} (用户: ${user.name})`);

      // 广播字段释放事件
      const wsClient = getWebSocketClient();
      if (wsClient && wsClient.isConnected) {
        const message = createBusinessMessage('police', 'FIELD_BLUR', {
          roomId,
          fieldName,
          user,
          timestamp: Date.now(),
          isAutoUnlock: true // 标记为自动解锁事件
        });
        wsClient.send(message);
        console.log(`📡 [Field Collaboration] WebSocket广播字段自动释放: ${fieldName}`);
      }
    }
  }

  /**
   * 对象池管理 - 性能优化
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
   * 内存清理机制 - 支持200人并发
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
   * 销毁管理器 - 清理资源
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
}

// 导出单例实例
export const fieldCollaborationManager = new FieldCollaborationManager();
export default fieldCollaborationManager;