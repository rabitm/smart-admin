/**
 * 生产级协作跟踪器 - 真实DOM元素跟踪和光标定位
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-24
 * @Copyright 1024创新实验室
 */

interface FieldPosition {
  x: number;
  y: number;
  width: number;
  height: number;
  element: HTMLElement;
}

interface ActiveUser {
  id: string;
  name: string;
  avatar?: string;
  color: string;
  sessionId: string;
  lastActivity: number;
}

interface UserCursor {
  userId: string;
  fieldName: string;
  position: FieldPosition;
  caretPosition?: number;
  isActive: boolean;
  timestamp: number;
}

interface FieldChangeEvent {
  fieldName: string;
  fieldValue: any;
  oldValue: any;
  userId: string;
  timestamp: number;
  caretPosition?: number;
}

export class ProductionCollaborationTracker {
  private fieldElements: Map<string, HTMLElement> = new Map();
  private activeUsers: Map<string, ActiveUser> = new Map();
  private userCursors: Map<string, UserCursor> = new Map();
  private fieldObservers: Map<string, MutationObserver> = new Map();
  private resizeObserver: ResizeObserver;
  private scrollObserver: IntersectionObserver;

  // 事件回调
  private onCursorUpdate?: (cursors: UserCursor[]) => void;
  private onUserJoin?: (user: ActiveUser) => void;
  private onUserLeave?: (userId: string) => void;
  private onFieldChange?: (event: FieldChangeEvent) => void;

  // WebSocket连接 (生产环境)
  private websocket: WebSocket | null = null;
  private websocketUrl: string | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000;

  constructor(websocketUrl?: string) {
    this.setupResizeObserver();
    this.setupScrollObserver();

    // 生产环境下建立WebSocket连接
    if (websocketUrl && typeof window !== 'undefined') {
      this.connectWebSocket(websocketUrl);
    }
  }

  /**
   * 注册字段元素进行跟踪
   */
  registerField(fieldName: string, element: HTMLElement): void {
    console.log(`📍 [ProductionTracker] 注册字段: ${fieldName}`, element);

    // 清理之前的监听器
    this.unregisterField(fieldName);

    // 保存字段元素
    this.fieldElements.set(fieldName, element);

    // 设置字段属性用于识别
    element.setAttribute('data-collaboration-field', fieldName);

    // 监听字段变化事件
    this.setupFieldListeners(fieldName, element);

    // 监听DOM变化
    this.setupMutationObserver(fieldName, element);

    // 立即更新位置信息
    this.updateFieldPosition(fieldName);
  }

  /**
   * 取消字段跟踪
   */
  unregisterField(fieldName: string): void {
    const element = this.fieldElements.get(fieldName);
    if (element) {
      // 移除事件监听器
      element.removeEventListener('focus', this.handleFieldFocus);
      element.removeEventListener('blur', this.handleFieldBlur);
      element.removeEventListener('input', this.handleFieldInput);
      element.removeEventListener('selectionchange', this.handleSelectionChange);

      // 移除属性
      element.removeAttribute('data-collaboration-field');
    }

    // 清理观察者
    const observer = this.fieldObservers.get(fieldName);
    if (observer) {
      observer.disconnect();
      this.fieldObservers.delete(fieldName);
    }

    this.fieldElements.delete(fieldName);

    // 移除相关的光标
    for (const [cursorKey, cursor] of this.userCursors) {
      if (cursor.fieldName === fieldName) {
        this.userCursors.delete(cursorKey);
      }
    }

    this.notifyCursorUpdate();
  }

  /**
   * 设置字段事件监听器
   */
  private setupFieldListeners(fieldName: string, element: HTMLElement): void {
    element.addEventListener('focus', (e) => this.handleFieldFocus(e, fieldName));
    element.addEventListener('blur', (e) => this.handleFieldBlur(e, fieldName));
    element.addEventListener('input', (e) => this.handleFieldInput(e, fieldName));
    element.addEventListener('keyup', (e) => this.handleSelectionChange(e, fieldName));
    element.addEventListener('click', (e) => this.handleSelectionChange(e, fieldName));
  }

  /**
   * 设置DOM变化监听器
   */
  private setupMutationObserver(fieldName: string, element: HTMLElement): void {
    const observer = new MutationObserver((mutations) => {
      let shouldUpdate = false;

      mutations.forEach((mutation) => {
        if (mutation.type === 'attributes' || mutation.type === 'childList') {
          shouldUpdate = true;
        }
      });

      if (shouldUpdate) {
        this.updateFieldPosition(fieldName);
      }
    });

    observer.observe(element, {
      attributes: true,
      childList: true,
      subtree: true,
      attributeFilter: ['style', 'class']
    });

    this.fieldObservers.set(fieldName, observer);
  }

  /**
   * 设置窗口大小变化监听器
   */
  private setupResizeObserver(): void {
    this.resizeObserver = new ResizeObserver((entries) => {
      // 当窗口大小改变时，更新所有字段位置
      setTimeout(() => {
        this.updateAllFieldPositions();
      }, 100); // 延迟执行，等待布局稳定
    });

    // 监听document.body的大小变化
    if (typeof document !== 'undefined') {
      this.resizeObserver.observe(document.body);
    }
  }

  /**
   * 设置滚动监听器
   */
  private setupScrollObserver(): void {
    this.scrollObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          const element = entry.target as HTMLElement;
          const fieldName = element.getAttribute('data-collaboration-field');

          if (fieldName) {
            // 当字段元素进入或离开视口时更新位置
            this.updateFieldPosition(fieldName);
          }
        });
      },
      {
        root: null,
        rootMargin: '50px',
        threshold: [0, 0.1, 1]
      }
    );
  }

  /**
   * 字段焦点事件处理
   */
  private handleFieldFocus = (event: FocusEvent, fieldName: string): void => {
    console.log(`🎯 [ProductionTracker] 字段获取焦点: ${fieldName}`);

    const currentUser = this.getCurrentUser();
    if (!currentUser) return;

    // 立即更新字段位置
    this.updateFieldPosition(fieldName);

    // 创建或更新光标信息
    const position = this.getFieldPosition(fieldName);
    if (position) {
      const cursorKey = `${currentUser.id}_${fieldName}`;
      const caretPos = this.getCaretPosition(event.target as HTMLElement);

      this.userCursors.set(cursorKey, {
        userId: currentUser.id,
        fieldName,
        position,
        caretPosition: caretPos,
        isActive: true,
        timestamp: Date.now()
      });

      // 发送到WebSocket
      this.sendCursorUpdate(currentUser.id, fieldName, position, caretPos);
      this.notifyCursorUpdate();
    }
  };

  /**
   * 字段失焦事件处理
   */
  private handleFieldBlur = (event: FocusEvent, fieldName: string): void => {
    console.log(`🎯 [ProductionTracker] 字段失去焦点: ${fieldName}`);

    const currentUser = this.getCurrentUser();
    if (!currentUser) return;

    // 标记光标为非活跃状态
    const cursorKey = `${currentUser.id}_${fieldName}`;
    const cursor = this.userCursors.get(cursorKey);
    if (cursor) {
      cursor.isActive = false;
      cursor.timestamp = Date.now();
    }

    // 延迟移除光标（避免快速切换时闪烁）
    setTimeout(() => {
      const cursor = this.userCursors.get(cursorKey);
      if (cursor && !cursor.isActive && Date.now() - cursor.timestamp > 2000) {
        this.userCursors.delete(cursorKey);
        this.notifyCursorUpdate();
      }
    }, 2500);

    this.sendCursorUpdate(currentUser.id, fieldName, null, null);
  };

  /**
   * 字段输入事件处理
   */
  private handleFieldInput = (event: Event, fieldName: string): void => {
    const target = event.target as HTMLInputElement | HTMLTextAreaElement;
    const currentUser = this.getCurrentUser();
    if (!currentUser) return;

    // 更新光标位置
    const position = this.getFieldPosition(fieldName);
    const caretPos = this.getCaretPosition(target);

    if (position) {
      const cursorKey = `${currentUser.id}_${fieldName}`;
      this.userCursors.set(cursorKey, {
        userId: currentUser.id,
        fieldName,
        position,
        caretPosition: caretPos,
        isActive: true,
        timestamp: Date.now()
      });

      this.sendCursorUpdate(currentUser.id, fieldName, position, caretPos);
      this.notifyCursorUpdate();
    }

    // 触发字段变化事件
    if (this.onFieldChange) {
      this.onFieldChange({
        fieldName,
        fieldValue: target.value,
        oldValue: target.defaultValue,
        userId: currentUser.id,
        timestamp: Date.now(),
        caretPosition: caretPos
      });
    }
  };

  /**
   * 光标位置变化处理
   */
  private handleSelectionChange = (event: Event, fieldName: string): void => {
    const currentUser = this.getCurrentUser();
    if (!currentUser) return;

    const target = event.target as HTMLElement;
    const caretPos = this.getCaretPosition(target);
    const position = this.getFieldPosition(fieldName);

    if (position && document.activeElement === target) {
      const cursorKey = `${currentUser.id}_${fieldName}`;
      const existingCursor = this.userCursors.get(cursorKey);

      if (existingCursor) {
        existingCursor.caretPosition = caretPos;
        existingCursor.timestamp = Date.now();

        this.sendCursorUpdate(currentUser.id, fieldName, position, caretPos);
        this.notifyCursorUpdate();
      }
    }
  };

  /**
   * 获取字段的精确位置信息
   */
  private getFieldPosition(fieldName: string): FieldPosition | null {
    const element = this.fieldElements.get(fieldName);
    if (!element) return null;

    const rect = element.getBoundingClientRect();
    const scrollX = window.pageXOffset || document.documentElement.scrollLeft;
    const scrollY = window.pageYOffset || document.documentElement.scrollTop;

    return {
      x: rect.left + scrollX,
      y: rect.top + scrollY,
      width: rect.width,
      height: rect.height,
      element
    };
  }

  /**
   * 获取光标位置（支持input和textarea）
   */
  private getCaretPosition(element: HTMLElement): number {
    if (element instanceof HTMLInputElement || element instanceof HTMLTextAreaElement) {
      return element.selectionStart || 0;
    }

    // 对于contenteditable元素
    if (element.isContentEditable) {
      const selection = window.getSelection();
      if (selection && selection.rangeCount > 0) {
        const range = selection.getRangeAt(0);
        return range.startOffset;
      }
    }

    return 0;
  }

  /**
   * 更新指定字段的位置信息
   */
  private updateFieldPosition(fieldName: string): void {
    const position = this.getFieldPosition(fieldName);
    if (!position) return;

    // 更新所有相关的光标位置
    for (const [cursorKey, cursor] of this.userCursors) {
      if (cursor.fieldName === fieldName) {
        cursor.position = position;
        cursor.timestamp = Date.now();
      }
    }

    this.notifyCursorUpdate();
  }

  /**
   * 更新所有字段的位置信息
   */
  private updateAllFieldPositions(): void {
    for (const fieldName of this.fieldElements.keys()) {
      this.updateFieldPosition(fieldName);
    }
  }

  /**
   * 获取当前用户信息
   */
  private getCurrentUser(): ActiveUser | null {
    // 这里应该从用户store或session中获取当前用户信息
    // 暂时返回模拟数据，实际使用时需要替换
    const userStore = (window as any).userStore || {};
    return {
      id: userStore.employeeId || 'current_user',
      name: userStore.actualName || '当前用户',
      avatar: userStore.avatar || '',
      color: '#1890ff',
      sessionId: this.generateSessionId(),
      lastActivity: Date.now()
    };
  }

  /**
   * WebSocket连接管理
   */
  private connectWebSocket(url: string): void {
    try {
      // 保存WebSocket URL用于重连
      this.websocketUrl = url;

      // 检查后端服务是否可用
      const healthCheckUrl = url.replace('ws://', 'http://').replace('/api/websocket/seat', '/api/health');

      fetch(healthCheckUrl, {
        method: 'HEAD',
        mode: 'no-cors',
        cache: 'no-cache',
        signal: AbortSignal.timeout(5000) // 5秒超时
      })
      .then(() => {
        // 后端可用，建立WebSocket连接
        this.establishWebSocketConnection(url);
      })
      .catch(() => {
        console.warn('⚠️ [ProductionTracker] 后端服务不可用，启用离线模式');
        this.enableOfflineMode();
      });

    } catch (error) {
      console.warn('⚠️ [ProductionTracker] WebSocket初始化失败，启用离线模式:', error);
      this.enableOfflineMode();
    }
  }

  /**
   * 建立WebSocket连接
   */
  private establishWebSocketConnection(url: string): void {
    this.websocket = new WebSocket(url);

    this.websocket.onopen = () => {
      console.log('📡 [ProductionTracker] WebSocket连接已建立');
      this.reconnectAttempts = 0;
    };

    this.websocket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        this.handleWebSocketMessage(data);
      } catch (error) {
        console.error('❌ [ProductionTracker] WebSocket消息解析失败:', error);
      }
    };

    this.websocket.onclose = () => {
      console.log('📡 [ProductionTracker] WebSocket连接已关闭');
      this.scheduleReconnect();
    };

    this.websocket.onerror = (error) => {
      console.error('❌ [ProductionTracker] WebSocket连接错误:', error);
      this.enableOfflineMode();
    };
  }

  /**
   * 启用离线模式
   */
  private enableOfflineMode(): void {
    console.log('📴 [ProductionTracker] 启用离线模式，仅跟踪本地光标');
    // 在离线模式下，仍然跟踪本地光标和字段变化
    // 但不发送到WebSocket服务器
  }

  /**
   * 处理WebSocket消息
   */
  private handleWebSocketMessage(data: any): void {
    switch (data.type) {
      case 'cursor_update':
        this.handleRemoteCursorUpdate(data);
        break;
      case 'user_join':
        this.handleUserJoin(data.user);
        break;
      case 'user_leave':
        this.handleUserLeave(data.userId);
        break;
      case 'field_change':
        this.handleRemoteFieldChange(data);
        break;
    }
  }

  /**
   * 处理远程光标更新
   */
  private handleRemoteCursorUpdate(data: any): void {
    const { userId, fieldName, position, caretPosition } = data;

    if (userId === this.getCurrentUser()?.id) {
      return; // 忽略自己的光标更新
    }

    const cursorKey = `${userId}_${fieldName}`;

    if (position) {
      this.userCursors.set(cursorKey, {
        userId,
        fieldName,
        position,
        caretPosition,
        isActive: true,
        timestamp: Date.now()
      });
    } else {
      // 移除光标
      this.userCursors.delete(cursorKey);
    }

    this.notifyCursorUpdate();
  }

  /**
   * 处理用户加入
   */
  private handleUserJoin(user: ActiveUser): void {
    this.activeUsers.set(user.id, user);
    if (this.onUserJoin) {
      this.onUserJoin(user);
    }
  }

  /**
   * 处理用户离开
   */
  private handleUserLeave(userId: string): void {
    this.activeUsers.delete(userId);

    // 移除该用户的所有光标
    for (const [cursorKey, cursor] of this.userCursors) {
      if (cursor.userId === userId) {
        this.userCursors.delete(cursorKey);
      }
    }

    this.notifyCursorUpdate();

    if (this.onUserLeave) {
      this.onUserLeave(userId);
    }
  }

  /**
   * 处理远程字段变化
   */
  private handleRemoteFieldChange(data: any): void {
    if (this.onFieldChange) {
      this.onFieldChange(data);
    }
  }

  /**
   * 发送光标更新到WebSocket
   */
  private sendCursorUpdate(
    userId: string,
    fieldName: string,
    position: FieldPosition | null,
    caretPosition: number | null
  ): void {
    if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
      const message = {
        type: 'cursor_update',
        userId,
        fieldName,
        position,
        caretPosition,
        timestamp: Date.now()
      };

      this.websocket.send(JSON.stringify(message));
    }
  }

  /**
   * 重连调度
   */
  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('❌ [ProductionTracker] WebSocket重连次数已达上限');
      return;
    }

    this.reconnectAttempts++;
    const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);

    console.log(`🔄 [ProductionTracker] 将在${delay}ms后尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);

    setTimeout(() => {
      // 使用保存的WebSocket URL进行重连
      if (this.websocketUrl) {
        this.connectWebSocket(this.websocketUrl);
      }
    }, delay);
  }

  /**
   * 通知光标更新
   */
  private notifyCursorUpdate(): void {
    if (this.onCursorUpdate) {
      const cursors = Array.from(this.userCursors.values());
      this.onCursorUpdate(cursors);
    }
  }

  /**
   * 生成会话ID
   */
  private generateSessionId(): string {
    return `session_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`;
  }

  // =============== 公共接口方法 ===============

  /**
   * 设置事件回调
   */
  setEventHandlers(handlers: {
    onCursorUpdate?: (cursors: UserCursor[]) => void;
    onUserJoin?: (user: ActiveUser) => void;
    onUserLeave?: (userId: string) => void;
    onFieldChange?: (event: FieldChangeEvent) => void;
  }): void {
    this.onCursorUpdate = handlers.onCursorUpdate;
    this.onUserJoin = handlers.onUserJoin;
    this.onUserLeave = handlers.onUserLeave;
    this.onFieldChange = handlers.onFieldChange;
  }

  /**
   * 获取所有活跃用户
   */
  getActiveUsers(): ActiveUser[] {
    return Array.from(this.activeUsers.values());
  }

  /**
   * 获取所有用户光标
   */
  getUserCursors(): UserCursor[] {
    return Array.from(this.userCursors.values());
  }

  /**
   * 强制刷新所有位置
   */
  refreshAllPositions(): void {
    this.updateAllFieldPositions();
  }

  /**
   * 销毁跟踪器
   */
  destroy(): void {
    // 清理所有字段监听
    for (const fieldName of this.fieldElements.keys()) {
      this.unregisterField(fieldName);
    }

    // 关闭观察者
    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
    }

    if (this.scrollObserver) {
      this.scrollObserver.disconnect();
    }

    // 关闭WebSocket连接
    if (this.websocket) {
      this.websocket.close();
    }

    // 清理数据
    this.fieldElements.clear();
    this.activeUsers.clear();
    this.userCursors.clear();
    this.fieldObservers.clear();

    console.log('🛑 [ProductionTracker] 协作跟踪器已销毁');
  }
}

/**
 * 创建生产级协作跟踪器实例
 */
export function createProductionCollaborationTracker(websocketUrl?: string): ProductionCollaborationTracker {
  return new ProductionCollaborationTracker(websocketUrl);
}