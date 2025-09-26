/*
 * 警务系统WebSocket服务
 * 统一管理警务相关的WebSocket通信
 *
 * @Author:    Claude Code Assistant
 * @Date:      2025-09-25
 * @Copyright  1024创新实验室
 */

import { getWebSocketClient } from '/@/utils/websocket-manager';
import { IWebSocketClient, BusinessModule, WebSocketMessage, createBusinessMessage, createSystemMessage } from '/@/types/websocket';
import { message } from 'ant-design-vue';

export interface PoliceReportCollaboration {
  reportId: number;
  userId: number;
  userName: string;
  operation: string;
  fieldName?: string;
  timestamp: number;
  value?: any;
}

export interface PoliceEditLockInfo {
  reportId: number;
  userId: number;
  userName: string;
  locked: boolean;
  timestamp: number;
}

export interface PoliceFieldUpdate {
  reportId: number;
  fieldName: string;
  value: any;
  userId: number;
  userName: string;
  timestamp: number;
}

export class PoliceWebSocketService {
  private client: IWebSocketClient | null = null;
  private currentReportId: number | null = null;
  private collaborationCallbacks = new Map<string, Function[]>();

  /**
   * 初始化服务
   */
  async initialize(): Promise<void> {
    this.client = getWebSocketClient();

    if (!this.client) {
      throw new Error('WebSocket客户端未初始化，请先初始化全局WebSocket连接');
    }

    // 订阅警务模块消息
    await this.client.subscribeModule('police');

    // 设置消息处理器
    this.setupMessageHandlers();

    console.log('🚨 警务WebSocket服务已初始化');
  }

  /**
   * 设置消息处理器
   */
  private setupMessageHandlers(): void {
    if (!this.client) return;

    // 处理协作消息
    this.client.onModuleMessage(BusinessModule.POLICE, 'FIELD_EDIT', (message: WebSocketMessage) => {
      this.handleFieldEdit(message.data);
    });

    this.client.onModuleMessage(BusinessModule.POLICE, 'FIELD_FOCUS', (message: WebSocketMessage) => {
      this.handleFieldFocus(message.data);
    });

    this.client.onModuleMessage(BusinessModule.POLICE, 'FIELD_BLUR', (message: WebSocketMessage) => {
      this.handleFieldBlur(message.data);
    });

    this.client.onModuleMessage(BusinessModule.POLICE, 'USER_JOIN', (message: WebSocketMessage) => {
      this.handleUserJoin(message.data);
    });

    this.client.onModuleMessage(BusinessModule.POLICE, 'USER_LEAVE', (message: WebSocketMessage) => {
      this.handleUserLeave(message.data);
    });

    this.client.onModuleMessage(BusinessModule.POLICE, 'REPORT_LOCK', (message: WebSocketMessage) => {
      this.handleReportLock(message.data);
    });

    this.client.onModuleMessage(BusinessModule.POLICE, 'REPORT_UNLOCK', (message: WebSocketMessage) => {
      this.handleReportUnlock(message.data);
    });

    this.client.onModuleMessage(BusinessModule.POLICE, 'COLLABORATION_USER_JOIN', (message: WebSocketMessage) => {
      this.handleUserJoin(message.data);
    });

    this.client.onModuleMessage(BusinessModule.POLICE, 'COLLABORATION_USER_LEAVE', (message: WebSocketMessage) => {
      this.handleUserLeave(message.data);
    });

    this.client.onModuleMessage(BusinessModule.POLICE, 'REPORT_UPDATE', (message: WebSocketMessage) => {
      this.handleReportUpdate(message.data);
    });
  }

  /**
   * 加入报告协作
   */
  async joinReport(reportId: number): Promise<void> {
    if (!this.client?.isConnected) {
      throw new Error('WebSocket未连接');
    }

    this.currentReportId = reportId;

    const message = createBusinessMessage('police', 'JOIN_REPORT', {
      reportId,
      timestamp: Date.now()
    });

    await this.client.send(message);
    console.log(`🚨 加入报告${reportId}协作`);
  }

  /**
   * 离开报告协作
   */
  async leaveReport(): Promise<void> {
    if (!this.client?.isConnected || !this.currentReportId) {
      return;
    }

    const message = createBusinessMessage('police', 'LEAVE_REPORT', {
      reportId: this.currentReportId,
      timestamp: Date.now()
    });

    await this.client.send(message);
    console.log(`🚨 离开报告${this.currentReportId}协作`);

    this.currentReportId = null;
  }

  /**
   * 发送字段编辑事件
   */
  async sendFieldEdit(fieldName: string, value: any): Promise<void> {
    if (!this.client?.isConnected || !this.currentReportId) {
      return;
    }

    const message = createBusinessMessage('police', 'FIELD_EDIT', {
      reportId: this.currentReportId,
      fieldName,
      value,
      timestamp: Date.now()
    });

    await this.client.send(message);
  }

  /**
   * 发送字段焦点事件
   */
  async sendFieldFocus(fieldName: string): Promise<void> {
    if (!this.client?.isConnected || !this.currentReportId) {
      return;
    }

    const message = createBusinessMessage('police', 'FIELD_FOCUS', {
      reportId: this.currentReportId,
      fieldName,
      timestamp: Date.now()
    });

    await this.client.send(message);
  }

  /**
   * 发送字段失去焦点事件
   */
  async sendFieldBlur(fieldName: string): Promise<void> {
    if (!this.client?.isConnected || !this.currentReportId) {
      return;
    }

    const message = createBusinessMessage('police', 'FIELD_BLUR', {
      reportId: this.currentReportId,
      fieldName,
      timestamp: Date.now()
    });

    await this.client.send(message);
  }

  /**
   * 锁定报告
   */
  async lockReport(reportId: number): Promise<void> {
    if (!this.client?.isConnected) {
      throw new Error('WebSocket未连接');
    }

    const message = createBusinessMessage('police', 'LOCK_REPORT', {
      reportId,
      timestamp: Date.now()
    });

    await this.client.send(message);
  }

  /**
   * 解锁报告
   */
  async unlockReport(reportId: number): Promise<void> {
    if (!this.client?.isConnected) {
      throw new Error('WebSocket未连接');
    }

    const message = createBusinessMessage('police', 'UNLOCK_REPORT', {
      reportId,
      timestamp: Date.now()
    });

    await this.client.send(message);
  }

  /**
   * 处理字段编辑事件
   */
  private handleFieldEdit(data: any): void {
    this.triggerCallback('field_edit', data);
  }

  /**
   * 处理字段焦点事件
   */
  private handleFieldFocus(data: any): void {
    this.triggerCallback('field_focus', data);
  }

  /**
   * 处理字段失去焦点事件
   */
  private handleFieldBlur(data: any): void {
    this.triggerCallback('field_blur', data);
  }

  /**
   * 处理报告锁定事件
   */
  private handleReportLock(data: any): void {
    this.triggerCallback('report_lock', data);
    message.warning(`报告已被${data.userName}锁定编辑`);
  }

  /**
   * 处理报告解锁事件
   */
  private handleReportUnlock(data: any): void {
    this.triggerCallback('report_unlock', data);
    message.info(`报告锁定已被${data.userName}解除`);
  }

  /**
   * 处理用户加入协作
   */
  private handleUserJoin(data: any): void {
    this.triggerCallback('user_join', data);
    console.log(`👤 ${data.userName}加入协作`);
  }

  /**
   * 处理用户离开协作
   */
  private handleUserLeave(data: any): void {
    this.triggerCallback('user_leave', data);
    console.log(`👤 ${data.userName}离开协作`);
  }

  /**
   * 处理报告更新事件
   */
  private handleReportUpdate(data: any): void {
    this.triggerCallback('report_update', data);
  }

  /**
   * 注册回调函数
   */
  on(event: string, callback: Function): void {
    if (!this.collaborationCallbacks.has(event)) {
      this.collaborationCallbacks.set(event, []);
    }
    this.collaborationCallbacks.get(event)!.push(callback);
  }

  /**
   * 移除回调函数
   */
  off(event: string, callback?: Function): void {
    if (!callback) {
      this.collaborationCallbacks.delete(event);
      return;
    }

    const callbacks = this.collaborationCallbacks.get(event);
    if (callbacks) {
      const index = callbacks.indexOf(callback);
      if (index !== -1) {
        callbacks.splice(index, 1);
      }
    }
  }

  /**
   * 触发回调函数
   */
  private triggerCallback(event: string, data: any): void {
    const callbacks = this.collaborationCallbacks.get(event);
    if (callbacks) {
      callbacks.forEach(callback => {
        try {
          callback(data);
        } catch (error) {
          console.error(`警务WebSocket回调执行失败 [${event}]:`, error);
        }
      });
    }
  }

  /**
   * 获取当前报告ID
   */
  getCurrentReportId(): number | null {
    return this.currentReportId;
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
    if (this.currentReportId) {
      this.leaveReport();
    }

    this.collaborationCallbacks.clear();
    this.client = null;

    console.log('🚨 警务WebSocket服务已销毁');
  }
}

// 导出单例实例
export const policeWebSocketService = new PoliceWebSocketService();