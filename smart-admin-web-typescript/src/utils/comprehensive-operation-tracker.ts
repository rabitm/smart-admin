/**
 * 全方位操作追踪器 - 重构版时间轴核心
 *
 * 核心功能:
 * 1. 全方位操作捕获(键盘输入、鼠标点击、表单操作等)
 * 2. 智能防抖和批量处理
 * 3. 实时协作集成
 * 4. 安全审计支持
 * 5. 高性能存储策略
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-28
 * @Copyright 1024创新实验室
 */

import { useUserStore } from '/@/store/modules/system/user';
import { getWebSocketClient } from './websocket-manager';

// =============== 枚举定义 ===============

export enum OperationLevel {
  KEYSTROKE = 'keystroke',        // 键盘输入级别
  FIELD = 'field',               // 字段级别
  FORM = 'form',                 // 表单级别
  RECORD = 'record',             // 记录级别
  SYSTEM = 'system'              // 系统级别
}

export enum OperationType {
  // 输入操作
  INPUT_START = 'input_start',
  INPUT_CHANGE = 'input_change',
  INPUT_END = 'input_end',

  // 选择操作
  SELECT_OPTION = 'select_option',
  CHECKBOX_TOGGLE = 'checkbox_toggle',
  RADIO_SELECT = 'radio_select',

  // 界面操作
  FOCUS_FIELD = 'focus_field',
  BLUR_FIELD = 'blur_field',
  SCROLL_PAGE = 'scroll_page',
  RESIZE_WINDOW = 'resize_window',

  // 业务操作
  SAVE_RECORD = 'save_record',
  DELETE_RECORD = 'delete_record',
  STATUS_CHANGE = 'status_change',
  FIELD_UPDATE = 'field_update',

  // 协作操作
  FIELD_LOCK = 'field_lock',
  FIELD_UNLOCK = 'field_unlock',
  CONFLICT_RESOLVE = 'conflict_resolve',
  USER_JOIN = 'user_join',
  USER_LEAVE = 'user_leave',

  // 系统操作
  PAGE_LOAD = 'page_load',
  PAGE_UNLOAD = 'page_unload',
  ERROR_OCCURRED = 'error_occurred'
}

export enum SecurityLevel {
  LOW = 'low',
  MEDIUM = 'medium',
  HIGH = 'high',
  CRITICAL = 'critical'
}

// =============== 数据结构定义 ===============

export interface CaptureStrategy {
  level: OperationLevel;
  realtime: boolean;           // 是否实时同步
  persistent: boolean;         // 是否持久化存储
  sensitive: boolean;          // 是否敏感操作
  batchable: boolean;          // 是否可批量处理
  retention: number;           // 保留时间(天)
  debounceMs: number;         // 防抖时间(毫秒)
}

export interface ComprehensiveOperationRecord {
  // 基础信息
  id: string;
  timestamp: number;
  clientTimestamp: number;

  // 操作信息
  type: OperationType;
  level: OperationLevel;
  category: string;

  // 用户信息
  userId: number;
  userName: string;
  userRole: string;
  department: string;

  // 实体信息
  entityType: string;
  entityId: number;
  fieldPath: string;          // 支持嵌套字段路径，如 'basic.location.address'
  fieldLabel: string;

  // 数据变更
  beforeValue: any;
  afterValue: any;
  deltaData?: any;            // 增量数据

  // 上下文信息
  sessionId: string;
  pageUrl: string;
  referrer: string;
  viewport: { width: number; height: number };

  // 技术信息
  userAgent: string;
  ipAddress: string;
  networkLatency?: number;

  // 业务信息
  businessContext: {
    reportType?: string;
    priority?: string;
    status?: string;
    assignee?: string;
  };

  // 协作信息
  collaborationContext?: {
    conflictUsers: string[];
    lockDuration: number;
    resolutionStrategy: string;
  };

  // 安全信息
  securityLevel: SecurityLevel;
  complianceFlags: string[];

  // 元数据
  metadata: Record<string, any>;
  tags: string[];
}

export interface AnomalyReport {
  type: string;
  severity: 'low' | 'medium' | 'high' | 'critical';
  description: string;
  affectedOperations: string[];
  recommendedActions: string[];
  timestamp: number;
}

export interface OperationInsights {
  productivity: {
    score: number;
    trend: 'up' | 'down' | 'stable';
    keyMetrics: Record<string, number>;
  };
  collaboration: {
    efficiency: number;
    conflictRate: number;
    averageResponseTime: number;
  };
  efficiency: {
    operationsPerHour: number;
    errorRate: number;
    completionRate: number;
  };
  recommendations: string[];
}

// =============== 策略配置 ===============

const CAPTURE_STRATEGIES: Record<OperationType, CaptureStrategy> = {
  [OperationType.INPUT_CHANGE]: {
    level: OperationLevel.KEYSTROKE,
    realtime: false,           // 防抖处理
    persistent: true,
    sensitive: false,
    batchable: true,
    retention: 30,
    debounceMs: 500
  },
  [OperationType.FIELD_UPDATE]: {
    level: OperationLevel.FIELD,
    realtime: true,
    persistent: true,
    sensitive: true,
    batchable: false,
    retention: 90,
    debounceMs: 0
  },
  [OperationType.SAVE_RECORD]: {
    level: OperationLevel.RECORD,
    realtime: true,
    persistent: true,
    sensitive: true,
    batchable: false,
    retention: 365,
    debounceMs: 0
  },
  [OperationType.DELETE_RECORD]: {
    level: OperationLevel.RECORD,
    realtime: true,
    persistent: true,
    sensitive: true,
    batchable: false,
    retention: 2555, // 7年
    debounceMs: 0
  },
  [OperationType.FOCUS_FIELD]: {
    level: OperationLevel.FIELD,
    realtime: false,
    persistent: false,
    sensitive: false,
    batchable: true,
    retention: 7,
    debounceMs: 100
  },
  [OperationType.SELECT_OPTION]: {
    level: OperationLevel.FIELD,
    realtime: true,
    persistent: true,
    sensitive: false,
    batchable: false,
    retention: 30,
    debounceMs: 0
  },
  [OperationType.CONFLICT_RESOLVE]: {
    level: OperationLevel.SYSTEM,
    realtime: true,
    persistent: true,
    sensitive: true,
    batchable: false,
    retention: 365,
    debounceMs: 0
  },
  [OperationType.ERROR_OCCURRED]: {
    level: OperationLevel.SYSTEM,
    realtime: true,
    persistent: true,
    sensitive: true,
    batchable: false,
    retention: 90,
    debounceMs: 0
  },
  // 其他操作类型的默认策略
  [OperationType.INPUT_START]: {
    level: OperationLevel.KEYSTROKE,
    realtime: false,
    persistent: false,
    sensitive: false,
    batchable: true,
    retention: 7,
    debounceMs: 0
  },
  [OperationType.INPUT_END]: {
    level: OperationLevel.KEYSTROKE,
    realtime: false,
    persistent: true,
    sensitive: false,
    batchable: true,
    retention: 30,
    debounceMs: 200
  },
  [OperationType.CHECKBOX_TOGGLE]: {
    level: OperationLevel.FIELD,
    realtime: true,
    persistent: true,
    sensitive: false,
    batchable: false,
    retention: 30,
    debounceMs: 0
  },
  [OperationType.RADIO_SELECT]: {
    level: OperationLevel.FIELD,
    realtime: true,
    persistent: true,
    sensitive: false,
    batchable: false,
    retention: 30,
    debounceMs: 0
  },
  [OperationType.BLUR_FIELD]: {
    level: OperationLevel.FIELD,
    realtime: false,
    persistent: false,
    sensitive: false,
    batchable: true,
    retention: 7,
    debounceMs: 100
  },
  [OperationType.SCROLL_PAGE]: {
    level: OperationLevel.SYSTEM,
    realtime: false,
    persistent: false,
    sensitive: false,
    batchable: true,
    retention: 1,
    debounceMs: 1000
  },
  [OperationType.RESIZE_WINDOW]: {
    level: OperationLevel.SYSTEM,
    realtime: false,
    persistent: false,
    sensitive: false,
    batchable: true,
    retention: 1,
    debounceMs: 500
  },
  [OperationType.STATUS_CHANGE]: {
    level: OperationLevel.RECORD,
    realtime: true,
    persistent: true,
    sensitive: true,
    batchable: false,
    retention: 365,
    debounceMs: 0
  },
  [OperationType.FIELD_LOCK]: {
    level: OperationLevel.SYSTEM,
    realtime: true,
    persistent: true,
    sensitive: false,
    batchable: false,
    retention: 30,
    debounceMs: 0
  },
  [OperationType.FIELD_UNLOCK]: {
    level: OperationLevel.SYSTEM,
    realtime: true,
    persistent: true,
    sensitive: false,
    batchable: false,
    retention: 30,
    debounceMs: 0
  },
  [OperationType.USER_JOIN]: {
    level: OperationLevel.SYSTEM,
    realtime: true,
    persistent: true,
    sensitive: false,
    batchable: false,
    retention: 90,
    debounceMs: 0
  },
  [OperationType.USER_LEAVE]: {
    level: OperationLevel.SYSTEM,
    realtime: true,
    persistent: true,
    sensitive: false,
    batchable: false,
    retention: 90,
    debounceMs: 0
  },
  [OperationType.PAGE_LOAD]: {
    level: OperationLevel.SYSTEM,
    realtime: false,
    persistent: true,
    sensitive: false,
    batchable: true,
    retention: 30,
    debounceMs: 0
  },
  [OperationType.PAGE_UNLOAD]: {
    level: OperationLevel.SYSTEM,
    realtime: false,
    persistent: true,
    sensitive: false,
    batchable: true,
    retention: 30,
    debounceMs: 0
  }
};

// =============== 核心追踪器 ===============

export class ComprehensiveOperationTracker {
  private userStore = useUserStore();
  private webSocketClient = getWebSocketClient();

  // 缓冲区和定时器
  private batchBuffer: Map<string, ComprehensiveOperationRecord[]> = new Map();
  private debounceTimers: Map<string, NodeJS.Timeout> = new Map();
  private sessionId: string;

  // 监听器和状态
  private isCapturing = false;
  private listeners: Array<(operations: ComprehensiveOperationRecord[]) => void> = [];
  private eventListeners: Array<{ element: EventTarget; event: string; handler: EventListener }> = [];

  // 统计信息
  private stats = {
    totalOperations: 0,
    batchedOperations: 0,
    realtimeOperations: 0,
    droppedOperations: 0
  };

  constructor() {
    this.sessionId = this.generateSessionId();
    console.log('🎯 [ComprehensiveOperationTracker] 全方位操作追踪器已初始化');
  }

  /**
   * 启动操作捕获
   */
  start(): void {
    if (this.isCapturing) {
      console.warn('⚠️ [ComprehensiveOperationTracker] 追踪器已在运行中');
      return;
    }

    this.isCapturing = true;
    this.setupGlobalCapture();
    this.recordOperation(OperationType.PAGE_LOAD, {
      entityType: 'system',
      entityId: 0,
      fieldPath: 'page',
      description: '页面加载'
    });

    console.log('🚀 [ComprehensiveOperationTracker] 开始捕获操作');
  }

  /**
   * 停止操作捕获
   */
  stop(): void {
    if (!this.isCapturing) return;

    this.isCapturing = false;
    this.recordOperation(OperationType.PAGE_UNLOAD, {
      entityType: 'system',
      entityId: 0,
      fieldPath: 'page',
      description: '页面卸载'
    });

    // 清理事件监听器
    this.eventListeners.forEach(({ element, event, handler }) => {
      element.removeEventListener(event, handler);
    });
    this.eventListeners = [];

    // 清理定时器
    this.debounceTimers.forEach(timer => clearTimeout(timer));
    this.debounceTimers.clear();

    // 发送剩余批量数据
    this.flushAllBatches();

    console.log('⏹️ [ComprehensiveOperationTracker] 停止捕获操作', this.stats);
  }

  /**
   * 记录操作
   */
  recordOperation(
    type: OperationType,
    context: {
      entityType: string;
      entityId: number;
      fieldPath: string;
      fieldLabel?: string;
      beforeValue?: any;
      afterValue?: any;
      description?: string;
      businessContext?: any;
      collaborationContext?: any;
      metadata?: Record<string, any>;
    }
  ): void {
    if (!this.isCapturing) return;

    try {
      const operation = this.buildOperationRecord(type, context);
      const strategy = CAPTURE_STRATEGIES[type];

      // 应用捕获策略
      if (strategy.debounceMs > 0) {
        this.debounceOperation(operation, strategy);
      } else if (strategy.realtime) {
        this.sendRealtime(operation);
      } else if (strategy.batchable) {
        this.addToBatch(operation);
      } else {
        this.sendImmediate(operation);
      }

      this.stats.totalOperations++;
      this.notifyListeners([operation]);

    } catch (error) {
      console.error('❌ [ComprehensiveOperationTracker] 记录操作失败:', error);
      this.stats.droppedOperations++;
    }
  }

  /**
   * 添加监听器
   */
  addListener(callback: (operations: ComprehensiveOperationRecord[]) => void): () => void {
    this.listeners.push(callback);

    return () => {
      const index = this.listeners.indexOf(callback);
      if (index > -1) {
        this.listeners.splice(index, 1);
      }
    };
  }

  /**
   * 获取统计信息
   */
  getStats() {
    return { ...this.stats };
  }

  // =============== 私有方法 ===============

  /**
   * 设置全局事件捕获
   */
  private setupGlobalCapture(): void {
    // 输入事件
    this.addEventListeners(document, [
      { event: 'input', handler: this.handleInputEvent.bind(this) },
      { event: 'change', handler: this.handleChangeEvent.bind(this) },
      { event: 'keydown', handler: this.handleKeyEvent.bind(this) },
      { event: 'focus', handler: this.handleFocusEvent.bind(this), options: true },
      { event: 'blur', handler: this.handleBlurEvent.bind(this), options: true }
    ]);

    // 鼠标事件
    this.addEventListeners(document, [
      { event: 'click', handler: this.handleClickEvent.bind(this) }
    ]);

    // 窗口事件
    this.addEventListeners(window, [
      { event: 'scroll', handler: this.handleScrollEvent.bind(this) },
      { event: 'resize', handler: this.handleResizeEvent.bind(this) },
      { event: 'beforeunload', handler: this.handleBeforeUnloadEvent.bind(this) }
    ]);

    // 错误事件
    this.addEventListeners(window, [
      { event: 'error', handler: this.handleErrorEvent.bind(this) },
      { event: 'unhandledrejection', handler: this.handleUnhandledRejectionEvent.bind(this) }
    ]);
  }

  /**
   * 添加事件监听器
   */
  private addEventListeners(
    element: EventTarget,
    events: Array<{ event: string; handler: EventListener; options?: boolean }>
  ): void {
    events.forEach(({ event, handler, options }) => {
      element.addEventListener(event, handler, options);
      this.eventListeners.push({ element, event, handler });
    });
  }

  /**
   * 构建操作记录
   */
  private buildOperationRecord(
    type: OperationType,
    context: any
  ): ComprehensiveOperationRecord {
    const timestamp = Date.now();
    const strategy = CAPTURE_STRATEGIES[type];

    return {
      id: this.generateOperationId(),
      timestamp,
      clientTimestamp: timestamp,

      type,
      level: strategy.level,
      category: this.getOperationCategory(type),

      userId: this.userStore.employeeId || 0,
      userName: this.userStore.actualName || '未知用户',
      userRole: this.userStore.roles?.[0] || 'user',
      department: this.userStore.departmentName || '',

      entityType: context.entityType,
      entityId: context.entityId,
      fieldPath: context.fieldPath,
      fieldLabel: context.fieldLabel || context.fieldPath,

      beforeValue: context.beforeValue,
      afterValue: context.afterValue,
      deltaData: this.calculateDelta(context.beforeValue, context.afterValue),

      sessionId: this.sessionId,
      pageUrl: window.location.href,
      referrer: document.referrer,
      viewport: {
        width: window.innerWidth,
        height: window.innerHeight
      },

      userAgent: navigator.userAgent,
      ipAddress: 'unknown', // 需要从服务器获取

      businessContext: context.businessContext || {},
      collaborationContext: context.collaborationContext,

      securityLevel: this.calculateSecurityLevel(type, context),
      complianceFlags: this.getComplianceFlags(type, context),

      metadata: {
        ...context.metadata,
        strategy: strategy,
        captureTime: new Date().toISOString()
      },
      tags: this.generateTags(type, context)
    };
  }

  // =============== 事件处理器 ===============

  private handleInputEvent(event: Event): void {
    const target = event.target as HTMLInputElement;
    if (!target || !this.isTrackableElement(target)) return;

    const context = this.extractElementContext(target);
    this.recordOperation(OperationType.INPUT_CHANGE, {
      ...context,
      afterValue: target.value,
      description: `输入内容: ${target.value.substring(0, 50)}...`
    });
  }

  private handleChangeEvent(event: Event): void {
    const target = event.target as HTMLElement;
    if (!target || !this.isTrackableElement(target)) return;

    const context = this.extractElementContext(target);
    let operationType = OperationType.FIELD_UPDATE;

    if (target.tagName === 'SELECT') {
      operationType = OperationType.SELECT_OPTION;
    } else if (target.getAttribute('type') === 'checkbox') {
      operationType = OperationType.CHECKBOX_TOGGLE;
    } else if (target.getAttribute('type') === 'radio') {
      operationType = OperationType.RADIO_SELECT;
    }

    this.recordOperation(operationType, {
      ...context,
      afterValue: (target as any).value,
      description: `字段变更: ${context.fieldLabel}`
    });
  }

  private handleFocusEvent(event: Event): void {
    const target = event.target as HTMLElement;
    if (!target || !this.isTrackableElement(target)) return;

    const context = this.extractElementContext(target);
    this.recordOperation(OperationType.FOCUS_FIELD, {
      ...context,
      description: `聚焦字段: ${context.fieldLabel}`
    });
  }

  private handleBlurEvent(event: Event): void {
    const target = event.target as HTMLElement;
    if (!target || !this.isTrackableElement(target)) return;

    const context = this.extractElementContext(target);
    this.recordOperation(OperationType.BLUR_FIELD, {
      ...context,
      description: `离开字段: ${context.fieldLabel}`
    });
  }

  private handleClickEvent(event: Event): void {
    const target = event.target as HTMLElement;
    if (!target) return;

    // 检查是否是按钮点击
    const button = target.closest('button, .ant-btn');
    if (button) {
      const buttonText = button.textContent?.trim() || '按钮';
      this.recordOperation(OperationType.FIELD_UPDATE, {
        entityType: 'ui',
        entityId: 0,
        fieldPath: 'button.click',
        fieldLabel: buttonText,
        description: `点击按钮: ${buttonText}`,
        metadata: {
          buttonType: button.className,
          coordinates: { x: (event as MouseEvent).clientX, y: (event as MouseEvent).clientY }
        }
      });
    }
  }

  private handleScrollEvent(): void {
    this.recordOperation(OperationType.SCROLL_PAGE, {
      entityType: 'ui',
      entityId: 0,
      fieldPath: 'window.scroll',
      description: '页面滚动',
      metadata: {
        scrollTop: window.pageYOffset,
        scrollLeft: window.pageXOffset
      }
    });
  }

  private handleResizeEvent(): void {
    this.recordOperation(OperationType.RESIZE_WINDOW, {
      entityType: 'ui',
      entityId: 0,
      fieldPath: 'window.resize',
      description: '窗口调整大小',
      metadata: {
        newSize: { width: window.innerWidth, height: window.innerHeight }
      }
    });
  }

  private handleKeyEvent(event: KeyboardEvent): void {
    // 只记录特殊按键
    if (['Enter', 'Tab', 'Escape'].includes(event.key)) {
      this.recordOperation(OperationType.INPUT_CHANGE, {
        entityType: 'ui',
        entityId: 0,
        fieldPath: 'keyboard.special',
        description: `按键: ${event.key}`,
        metadata: {
          key: event.key,
          ctrlKey: event.ctrlKey,
          altKey: event.altKey,
          shiftKey: event.shiftKey
        }
      });
    }
  }

  private handleErrorEvent(event: ErrorEvent): void {
    this.recordOperation(OperationType.ERROR_OCCURRED, {
      entityType: 'system',
      entityId: 0,
      fieldPath: 'error.javascript',
      description: `JavaScript错误: ${event.message}`,
      metadata: {
        error: {
          message: event.message,
          filename: event.filename,
          lineno: event.lineno,
          colno: event.colno,
          stack: event.error?.stack
        }
      }
    });
  }

  private handleUnhandledRejectionEvent(event: PromiseRejectionEvent): void {
    this.recordOperation(OperationType.ERROR_OCCURRED, {
      entityType: 'system',
      entityId: 0,
      fieldPath: 'error.promise',
      description: `Promise拒绝: ${event.reason}`,
      metadata: {
        reason: String(event.reason)
      }
    });
  }

  private handleBeforeUnloadEvent(): void {
    this.flushAllBatches();
  }

  // =============== 工具方法 ===============

  private isTrackableElement(element: HTMLElement): boolean {
    // 检查是否是表单元素
    const trackableTags = ['INPUT', 'SELECT', 'TEXTAREA'];
    if (!trackableTags.includes(element.tagName)) return false;

    // 排除密码字段
    if (element.getAttribute('type') === 'password') return false;

    // 检查是否有跟踪标识
    return element.hasAttribute('data-track') ||
           element.closest('[data-track]') !== null ||
           element.closest('form') !== null;
  }

  private extractElementContext(element: HTMLElement) {
    const form = element.closest('form');
    const trackContainer = element.closest('[data-track]');

    return {
      entityType: trackContainer?.getAttribute('data-entity-type') || 'form',
      entityId: parseInt(trackContainer?.getAttribute('data-entity-id') || '0'),
      fieldPath: element.name || element.id || this.generateFieldPath(element),
      fieldLabel: this.getFieldLabel(element),
      beforeValue: (element as any).defaultValue || (element as any).value
    };
  }

  private generateFieldPath(element: HTMLElement): string {
    const path: string[] = [];
    let current = element;

    while (current && current !== document.body) {
      if (current.id) {
        path.unshift(current.id);
        break;
      }
      if (current.className) {
        path.unshift(current.className.split(' ')[0]);
      }
      current = current.parentElement!;
    }

    return path.join('.');
  }

  private getFieldLabel(element: HTMLElement): string {
    // 查找label
    const label = document.querySelector(`label[for="${element.id}"]`) ||
                  element.closest('label') ||
                  element.previousElementSibling;

    if (label?.textContent) {
      return label.textContent.trim();
    }

    return element.getAttribute('placeholder') ||
           element.getAttribute('aria-label') ||
           element.name ||
           element.id ||
           '未知字段';
  }

  private debounceOperation(operation: ComprehensiveOperationRecord, strategy: CaptureStrategy): void {
    const key = `${operation.fieldPath}_${operation.type}`;

    // 清除现有定时器
    if (this.debounceTimers.has(key)) {
      clearTimeout(this.debounceTimers.get(key)!);
    }

    // 设置新定时器
    const timer = setTimeout(() => {
      if (strategy.realtime) {
        this.sendRealtime(operation);
      } else if (strategy.batchable) {
        this.addToBatch(operation);
      } else {
        this.sendImmediate(operation);
      }
      this.debounceTimers.delete(key);
    }, strategy.debounceMs);

    this.debounceTimers.set(key, timer);
  }

  private sendRealtime(operation: ComprehensiveOperationRecord): void {
    // 通过WebSocket实时发送
    const message = {
      messageId: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      type: 'OPERATION_RECORD',
      module: 'police',
      content: '全方位操作记录',
      data: {
        operation,
        realtime: true
      },
      timestamp: new Date().toISOString(),
      priority: 2,
      needAck: false
    };

    this.webSocketClient.send(message);
    this.stats.realtimeOperations++;
  }

  private addToBatch(operation: ComprehensiveOperationRecord): void {
    const batchKey = operation.level;

    if (!this.batchBuffer.has(batchKey)) {
      this.batchBuffer.set(batchKey, []);
    }

    this.batchBuffer.get(batchKey)!.push(operation);
    this.stats.batchedOperations++;

    // 检查批量大小
    if (this.batchBuffer.get(batchKey)!.length >= 10) {
      this.flushBatch(batchKey);
    }
  }

  private sendImmediate(operation: ComprehensiveOperationRecord): void {
    // 立即发送到后端API
    this.sendToAPI([operation]);
  }

  private flushBatch(batchKey: string): void {
    const batch = this.batchBuffer.get(batchKey);
    if (!batch || batch.length === 0) return;

    this.sendToAPI(batch);
    this.batchBuffer.set(batchKey, []);
  }

  private flushAllBatches(): void {
    this.batchBuffer.forEach((_, key) => {
      this.flushBatch(key);
    });
  }

  private async sendToAPI(operations: ComprehensiveOperationRecord[]): Promise<void> {
    try {
      const response = await fetch('/api/collaboration/history/batch-record', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.userStore.token}`
        },
        body: JSON.stringify({ operations })
      });

      if (!response.ok) {
        throw new Error(`API响应错误: ${response.status}`);
      }

      console.log(`📤 [ComprehensiveOperationTracker] 成功发送${operations.length}条操作记录`);
    } catch (error) {
      console.error('❌ [ComprehensiveOperationTracker] 发送操作记录失败:', error);
      // 可以考虑重试或存储到本地
    }
  }

  private notifyListeners(operations: ComprehensiveOperationRecord[]): void {
    this.listeners.forEach(callback => {
      try {
        callback(operations);
      } catch (error) {
        console.error('❌ [ComprehensiveOperationTracker] 监听器回调失败:', error);
      }
    });
  }

  private generateOperationId(): string {
    return `op_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  private generateSessionId(): string {
    return `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  private getOperationCategory(type: OperationType): string {
    if ([OperationType.INPUT_START, OperationType.INPUT_CHANGE, OperationType.INPUT_END].includes(type)) {
      return 'input';
    }
    if ([OperationType.SELECT_OPTION, OperationType.CHECKBOX_TOGGLE, OperationType.RADIO_SELECT].includes(type)) {
      return 'selection';
    }
    if ([OperationType.SAVE_RECORD, OperationType.DELETE_RECORD, OperationType.STATUS_CHANGE].includes(type)) {
      return 'business';
    }
    if ([OperationType.FIELD_LOCK, OperationType.FIELD_UNLOCK, OperationType.CONFLICT_RESOLVE].includes(type)) {
      return 'collaboration';
    }
    return 'system';
  }

  private calculateDelta(beforeValue: any, afterValue: any): any {
    if (beforeValue === afterValue) return null;

    if (typeof beforeValue === 'object' && typeof afterValue === 'object') {
      // 简单的对象差异计算
      const delta: any = {};
      const allKeys = new Set([...Object.keys(beforeValue || {}), ...Object.keys(afterValue || {})]);

      allKeys.forEach(key => {
        if (beforeValue?.[key] !== afterValue?.[key]) {
          delta[key] = { from: beforeValue?.[key], to: afterValue?.[key] };
        }
      });

      return Object.keys(delta).length > 0 ? delta : null;
    }

    return { from: beforeValue, to: afterValue };
  }

  private calculateSecurityLevel(type: OperationType, context: any): SecurityLevel {
    // 删除操作为关键级别
    if (type === OperationType.DELETE_RECORD) {
      return SecurityLevel.CRITICAL;
    }

    // 状态变更和重要字段为高级别
    if (type === OperationType.STATUS_CHANGE ||
        ['status', 'assignee', 'priority'].includes(context.fieldPath)) {
      return SecurityLevel.HIGH;
    }

    // 数据变更为中级别
    if ([OperationType.FIELD_UPDATE, OperationType.SAVE_RECORD].includes(type)) {
      return SecurityLevel.MEDIUM;
    }

    return SecurityLevel.LOW;
  }

  private getComplianceFlags(type: OperationType, context: any): string[] {
    const flags: string[] = [];

    if (type === OperationType.DELETE_RECORD) {
      flags.push('data-deletion');
    }

    if (context.fieldPath?.includes('personal')) {
      flags.push('personal-data');
    }

    if (context.securityLevel === SecurityLevel.CRITICAL) {
      flags.push('critical-operation');
    }

    return flags;
  }

  private generateTags(type: OperationType, context: any): string[] {
    const tags: string[] = [type, context.entityType];

    if (context.fieldPath) {
      tags.push(`field:${context.fieldPath}`);
    }

    if (context.businessContext?.reportType) {
      tags.push(`report-type:${context.businessContext.reportType}`);
    }

    return tags;
  }
}

// =============== 单例和导出 ===============

let trackerInstance: ComprehensiveOperationTracker | null = null;

export function getOperationTracker(): ComprehensiveOperationTracker {
  if (!trackerInstance) {
    trackerInstance = new ComprehensiveOperationTracker();
  }
  return trackerInstance;
}

export function destroyOperationTracker(): void {
  if (trackerInstance) {
    trackerInstance.stop();
    trackerInstance = null;
  }
}

// 便捷方法
export const recordFieldChange = (
  entityType: string,
  entityId: number,
  fieldPath: string,
  fieldLabel: string,
  beforeValue: any,
  afterValue: any
) => {
  getOperationTracker().recordOperation(OperationType.FIELD_UPDATE, {
    entityType,
    entityId,
    fieldPath,
    fieldLabel,
    beforeValue,
    afterValue,
    description: `字段变更: ${fieldLabel}`
  });
};

export const recordBusinessOperation = (
  type: OperationType,
  entityType: string,
  entityId: number,
  description: string,
  businessContext?: any
) => {
  getOperationTracker().recordOperation(type, {
    entityType,
    entityId,
    fieldPath: 'business.operation',
    description,
    businessContext
  });
};