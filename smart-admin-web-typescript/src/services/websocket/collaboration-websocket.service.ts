/*
 * 协作系统WebSocket服务
 * 统一管理实时协作相关的WebSocket通信
 *
 * @Author:    Claude Code Assistant
 * @Date:      2025-09-25
 * @Copyright  1024创新实验室
 */

import { getWebSocketClient } from '/@/utils/websocket-manager';
import { IWebSocketClient, BusinessModule, WebSocketMessage, createBusinessMessage } from '/@/types/websocket';

export interface CollaborationUser {
  id: number;
  name: string;
  avatar?: string;
  color: string;
  isOnline: boolean;
  lastActiveTime: number;
}

export interface CollaborationEvent {
  type: 'join' | 'leave' | 'edit' | 'focus' | 'blur' | 'cursor' | 'selection';
  userId: number;
  userName: string;
  targetId: string;
  targetType: 'field' | 'section' | 'document';
  data?: any;
  timestamp: number;
}

export interface CursorPosition {
  userId: number;
  userName: string;
  targetId: string;
  x: number;
  y: number;
  timestamp: number;
}

export class CollaborationWebSocketService {
  private client: IWebSocketClient | null = null;
  private currentDocumentId: string | null = null;
  private collaborationUsers = new Map<number, CollaborationUser>();
  private eventCallbacks = new Map<string, Function[]>();

  /**
   * 初始化服务
   */
  async initialize(): Promise<void> {
    this.client = getWebSocketClient();

    if (!this.client) {
      throw new Error('WebSocket客户端未初始化，请先初始化全局WebSocket连接');
    }

    // 订阅协作模块消息
    await this.client.subscribeModule('collaboration');

    // 设置消息处理器
    this.setupMessageHandlers();

    console.log('🤝 协作WebSocket服务已初始化');
  }

  /**
   * 设置消息处理器
   */
  private setupMessageHandlers(): void {
    if (!this.client) return;

    this.client.onModuleMessage(BusinessModule.COLLABORATION, 'USER_JOIN', (message: WebSocketMessage) => {
      this.handleUserJoin(message.data);
    });

    this.client.onModuleMessage(BusinessModule.COLLABORATION, 'USER_LEAVE', (message: WebSocketMessage) => {
      this.handleUserLeave(message.data);
    });

    this.client.onModuleMessage(BusinessModule.COLLABORATION, 'USER_LIST', (message: WebSocketMessage) => {
      this.handleUserList(message.data);
    });

    this.client.onModuleMessage(BusinessModule.COLLABORATION, 'FIELD_EDIT', (message: WebSocketMessage) => {
      this.handleFieldEdit(message.data);
    });

    this.client.onModuleMessage(BusinessModule.COLLABORATION, 'FIELD_FOCUS', (message: WebSocketMessage) => {
      this.handleFieldFocus(message.data);
    });

    this.client.onModuleMessage(BusinessModule.COLLABORATION, 'FIELD_BLUR', (message: WebSocketMessage) => {
      this.handleFieldBlur(message.data);
    });

    this.client.onModuleMessage(BusinessModule.COLLABORATION, 'CURSOR_MOVE', (message: WebSocketMessage) => {
      this.handleCursorMove(message.data);
    });

    this.client.onModuleMessage(BusinessModule.COLLABORATION, 'TEXT_SELECTION', (message: WebSocketMessage) => {
      this.handleTextSelection(message.data);
    });

    this.client.onModuleMessage(BusinessModule.COLLABORATION, 'DOCUMENT_SAVE', (message: WebSocketMessage) => {
      this.handleDocumentSave(message.data);
    });

    this.client.onModuleMessage(BusinessModule.COLLABORATION, 'CONFLICT_DETECTED', (message: WebSocketMessage) => {
      this.handleConflictDetected(message.data);
    });
  }

  /**
   * 加入文档协作
   */
  async joinDocument(documentId: string, documentType: string = 'report'): Promise<void> {
    if (!this.client?.isConnected) {
      throw new Error('WebSocket未连接');
    }

    this.currentDocumentId = documentId;

    const message = createBusinessMessage('collaboration', 'JOIN_DOCUMENT', {
      documentId,
      documentType,
      timestamp: Date.now()
    });

    await this.client.send(message);
    console.log(`🤝 加入文档${documentId}协作`);
  }

  /**
   * 离开文档协作
   */
  async leaveDocument(): Promise<void> {
    if (!this.client?.isConnected || !this.currentDocumentId) {
      return;
    }

    const message = createBusinessMessage('collaboration', 'LEAVE_DOCUMENT', {
      documentId: this.currentDocumentId,
      timestamp: Date.now()
    });

    await this.client.send(message);
    console.log(`🤝 离开文档${this.currentDocumentId}协作`);

    this.currentDocumentId = null;
    this.collaborationUsers.clear();
  }

  /**
   * 发送字段编辑事件
   */
  async sendFieldEdit(fieldName: string, value: any, selectionStart?: number, selectionEnd?: number): Promise<void> {
    if (!this.client?.isConnected || !this.currentDocumentId) {
      return;
    }

    const message = createBusinessMessage('collaboration', 'FIELD_EDIT', {
      documentId: this.currentDocumentId,
      fieldName,
      value,
      selectionStart,
      selectionEnd,
      timestamp: Date.now()
    });

    await this.client.send(message);
  }

  /**
   * 发送字段焦点事件
   */
  async sendFieldFocus(fieldName: string, cursorPosition?: { x: number; y: number }): Promise<void> {
    if (!this.client?.isConnected || !this.currentDocumentId) {
      return;
    }

    const message = createBusinessMessage('collaboration', 'FIELD_FOCUS', {
      documentId: this.currentDocumentId,
      fieldName,
      cursorPosition,
      timestamp: Date.now()
    });

    await this.client.send(message);
  }

  /**
   * 发送字段失去焦点事件
   */
  async sendFieldBlur(fieldName: string): Promise<void> {
    if (!this.client?.isConnected || !this.currentDocumentId) {
      return;
    }

    const message = createBusinessMessage('collaboration', 'FIELD_BLUR', {
      documentId: this.currentDocumentId,
      fieldName,
      timestamp: Date.now()
    });

    await this.client.send(message);
  }

  /**
   * 发送光标移动事件
   */
  async sendCursorMove(fieldName: string, position: { x: number; y: number }): Promise<void> {
    if (!this.client?.isConnected || !this.currentDocumentId) {
      return;
    }

    const message = createBusinessMessage('collaboration', 'CURSOR_MOVE', {
      documentId: this.currentDocumentId,
      fieldName,
      position,
      timestamp: Date.now()
    });

    await this.client.send(message);
  }

  /**
   * 发送文本选择事件
   */
  async sendTextSelection(fieldName: string, selectionStart: number, selectionEnd: number): Promise<void> {
    if (!this.client?.isConnected || !this.currentDocumentId) {
      return;
    }

    const message = createBusinessMessage('collaboration', 'TEXT_SELECTION', {
      documentId: this.currentDocumentId,
      fieldName,
      selectionStart,
      selectionEnd,
      timestamp: Date.now()
    });

    await this.client.send(message);
  }

  /**
   * 保存文档
   */
  async saveDocument(data: any): Promise<void> {
    if (!this.client?.isConnected || !this.currentDocumentId) {
      return;
    }

    const message = createBusinessMessage('collaboration', 'SAVE_DOCUMENT', {
      documentId: this.currentDocumentId,
      data,
      timestamp: Date.now()
    });

    await this.client.send(message);
  }

  /**
   * 处理用户加入事件
   */
  private handleUserJoin(data: any): void {
    const user: CollaborationUser = {
      id: data.userId,
      name: data.userName,
      avatar: data.avatar,
      color: data.color || this.generateUserColor(data.userId),
      isOnline: true,
      lastActiveTime: data.timestamp
    };

    this.collaborationUsers.set(user.id, user);
    this.triggerCallback('user_join', user);

    console.log(`👤 ${user.name}加入协作`);
  }

  /**
   * 处理用户离开事件
   */
  private handleUserLeave(data: any): void {
    const user = this.collaborationUsers.get(data.userId);
    if (user) {
      user.isOnline = false;
      this.collaborationUsers.delete(data.userId);
      this.triggerCallback('user_leave', user);
      console.log(`👤 ${user.name}离开协作`);
    }
  }

  /**
   * 处理用户列表事件
   */
  private handleUserList(data: any): void {
    this.collaborationUsers.clear();

    if (data.users && Array.isArray(data.users)) {
      data.users.forEach((userData: any) => {
        const user: CollaborationUser = {
          id: userData.userId,
          name: userData.userName,
          avatar: userData.avatar,
          color: userData.color || this.generateUserColor(userData.userId),
          isOnline: true,
          lastActiveTime: userData.timestamp || Date.now()
        };
        this.collaborationUsers.set(user.id, user);
      });
    }

    this.triggerCallback('user_list', Array.from(this.collaborationUsers.values()));
  }

  /**
   * 处理字段编辑事件
   */
  private handleFieldEdit(data: any): void {
    const event: CollaborationEvent = {
      type: 'edit',
      userId: data.userId,
      userName: data.userName,
      targetId: data.fieldName,
      targetType: 'field',
      data: {
        value: data.value,
        selectionStart: data.selectionStart,
        selectionEnd: data.selectionEnd
      },
      timestamp: data.timestamp
    };

    this.triggerCallback('field_edit', event);
  }

  /**
   * 处理字段焦点事件
   */
  private handleFieldFocus(data: any): void {
    const event: CollaborationEvent = {
      type: 'focus',
      userId: data.userId,
      userName: data.userName,
      targetId: data.fieldName,
      targetType: 'field',
      data: data.cursorPosition,
      timestamp: data.timestamp
    };

    this.triggerCallback('field_focus', event);
  }

  /**
   * 处理字段失去焦点事件
   */
  private handleFieldBlur(data: any): void {
    const event: CollaborationEvent = {
      type: 'blur',
      userId: data.userId,
      userName: data.userName,
      targetId: data.fieldName,
      targetType: 'field',
      timestamp: data.timestamp
    };

    this.triggerCallback('field_blur', event);
  }

  /**
   * 处理光标移动事件
   */
  private handleCursorMove(data: any): void {
    const cursor: CursorPosition = {
      userId: data.userId,
      userName: data.userName,
      targetId: data.fieldName,
      x: data.position.x,
      y: data.position.y,
      timestamp: data.timestamp
    };

    this.triggerCallback('cursor_move', cursor);
  }

  /**
   * 处理文本选择事件
   */
  private handleTextSelection(data: any): void {
    this.triggerCallback('text_selection', data);
  }

  /**
   * 处理文档保存事件
   */
  private handleDocumentSave(data: any): void {
    this.triggerCallback('document_save', data);
  }

  /**
   * 处理冲突检测事件
   */
  private handleConflictDetected(data: any): void {
    this.triggerCallback('conflict_detected', data);
    console.warn('⚠️ 检测到协作冲突:', data);
  }

  /**
   * 生成用户颜色
   */
  private generateUserColor(userId: number): string {
    const colors = [
      '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4',
      '#FFEAA7', '#DDA0DD', '#98D8C8', '#F7DC6F',
      '#BB8FCE', '#85C1E9', '#F8C471', '#82E0AA'
    ];
    return colors[userId % colors.length];
  }

  /**
   * 注册事件回调
   */
  on(event: string, callback: Function): void {
    if (!this.eventCallbacks.has(event)) {
      this.eventCallbacks.set(event, []);
    }
    this.eventCallbacks.get(event)!.push(callback);
  }

  /**
   * 移除事件回调
   */
  off(event: string, callback?: Function): void {
    if (!callback) {
      this.eventCallbacks.delete(event);
      return;
    }

    const callbacks = this.eventCallbacks.get(event);
    if (callbacks) {
      const index = callbacks.indexOf(callback);
      if (index !== -1) {
        callbacks.splice(index, 1);
      }
    }
  }

  /**
   * 触发事件回调
   */
  private triggerCallback(event: string, data: any): void {
    const callbacks = this.eventCallbacks.get(event);
    if (callbacks) {
      callbacks.forEach(callback => {
        try {
          callback(data);
        } catch (error) {
          console.error(`协作WebSocket回调执行失败 [${event}]:`, error);
        }
      });
    }
  }

  /**
   * 获取协作用户列表
   */
  getCollaborationUsers(): CollaborationUser[] {
    return Array.from(this.collaborationUsers.values());
  }

  /**
   * 获取当前文档ID
   */
  getCurrentDocumentId(): string | null {
    return this.currentDocumentId;
  }

  /**
   * 检查是否已连接
   */
  isConnected(): boolean {
    return this.client?.isConnected ?? false;
  }

  /**
   * 销毁服务
   */
  destroy(): void {
    if (this.currentDocumentId) {
      this.leaveDocument();
    }

    this.collaborationUsers.clear();
    this.eventCallbacks.clear();
    this.client = null;

    console.log('🤝 协作WebSocket服务已销毁');
  }
}

// 导出单例实例
export const collaborationWebSocketService = new CollaborationWebSocketService();