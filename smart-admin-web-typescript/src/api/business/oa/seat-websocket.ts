/*
 * 座位管理WebSocket服务
 *
 * @Author:    Claude Code Assistant
 * @Date:      2025-09-25
 * @Copyright  1024创新实验室
 */

import { message } from 'ant-design-vue';
import { getWebSocketClient } from '/@/utils/websocket-manager';
import {
  BusinessModule,
  WebSocketMessage,
  createBusinessMessage,
  MessageHandler
} from '/@/types/websocket';

/**
 * 座位状态枚举
 */
export enum SeatStatus {
  AVAILABLE = 'available',
  OCCUPIED = 'occupied',
  RESERVED = 'reserved',
  MAINTENANCE = 'maintenance'
}

/**
 * 座位操作类型
 */
export enum SeatOperation {
  RESERVE = 'reserve',
  OCCUPY = 'occupy',
  RELEASE = 'release'
}

/**
 * 座位信息接口
 */
export interface SeatInfo {
  seatId: number;
  seatCode: string;
  status: SeatStatus;
  currentUser?: string;
  currentUserId?: number;
  reservedUser?: string;
  reservedUserId?: number;
  lastUpdateTime?: number;
}

/**
 * 座位WebSocket服务类
 */
export class SeatWebSocketService {
  private client = getWebSocketClient();

  constructor() {
    this.init();
  }

  /**
   * 初始化服务
   */
  private init(): void {
    if (!this.client) {
      console.warn('WebSocket客户端未初始化，座位服务不可用');
      return;
    }

    // 订阅座位模块
    this.client.subscribeModule(BusinessModule.SEAT);

    // 注册消息处理器
    this.registerMessageHandlers();

    console.log('🪑 座位WebSocket服务已初始化');
  }

  /**
   * 注册消息处理器
   */
  private registerMessageHandlers(): void {
    if (!this.client) return;

    // 座位状态响应
    this.client.onModuleMessage(BusinessModule.SEAT, 'SEAT_STATUS_RESPONSE', (message) => {
      console.log('📋 收到座位状态响应:', message.data);
    });

    // 座位操作响应
    this.client.onModuleMessage(BusinessModule.SEAT, 'SEAT_OPERATION_RESPONSE', (message) => {
      const data = message.data;
      if (data?.result === 'success') {
        const operation = data.operation;
        const userName = data.userName;
        const seatId = data.seatId;

        switch (operation) {
          case 'reserve':
            message.success(`${userName} 成功预约座位 ${seatId}`);
            break;
          case 'occupy':
            message.success(`${userName} 成功占用座位 ${seatId}`);
            break;
          case 'release':
            message.success(`${userName} 成功释放座位 ${seatId}`);
            break;
        }
      }
    });

    // 座位状态变更广播
    this.client.onModuleMessage(BusinessModule.SEAT, 'SEAT_STATUS_CHANGED', (message) => {
      const data = message.data;
      console.log('🔄 座位状态变更:', data);

      // 可以在这里触发页面数据刷新
      this.onSeatStatusChanged?.(data);
    });

    // 座位列表响应
    this.client.onModuleMessage(BusinessModule.SEAT, 'SEAT_LIST_RESPONSE', (message) => {
      console.log('📃 收到座位列表响应:', message.data);
      this.onSeatListReceived?.(message.data);
    });

    // 错误处理
    this.client.onModuleMessage(BusinessModule.SEAT, 'ERROR', (message) => {
      const error = message.data?.error || '座位操作失败';
      message.error(error);
      console.error('❌ 座位操作错误:', message.data);
    });
  }

  /**
   * 查询座位状态
   */
  async querySeatStatus(seatId: number): Promise<boolean> {
    if (!this.client) {
      message.error('WebSocket连接未建立');
      return false;
    }

    const queryMessage = createBusinessMessage(
      BusinessModule.SEAT,
      'SEAT_STATUS_QUERY',
      { seatId }
    );

    return await this.client.send(queryMessage);
  }

  /**
   * 预约座位
   */
  async reserveSeat(seatId: number): Promise<boolean> {
    if (!this.client) {
      message.error('WebSocket连接未建立');
      return false;
    }

    const reserveMessage = createBusinessMessage(
      BusinessModule.SEAT,
      'SEAT_RESERVE',
      { seatId, operation: SeatOperation.RESERVE }
    );

    return await this.client.send(reserveMessage);
  }

  /**
   * 占用座位
   */
  async occupySeat(seatId: number): Promise<boolean> {
    if (!this.client) {
      message.error('WebSocket连接未建立');
      return false;
    }

    const occupyMessage = createBusinessMessage(
      BusinessModule.SEAT,
      'SEAT_OCCUPY',
      { seatId, operation: SeatOperation.OCCUPY }
    );

    return await this.client.send(occupyMessage);
  }

  /**
   * 释放座位
   */
  async releaseSeat(seatId: number): Promise<boolean> {
    if (!this.client) {
      message.error('WebSocket连接未建立');
      return false;
    }

    const releaseMessage = createBusinessMessage(
      BusinessModule.SEAT,
      'SEAT_RELEASE',
      { seatId, operation: SeatOperation.RELEASE }
    );

    return await this.client.send(releaseMessage);
  }

  /**
   * 查询座位列表
   */
  async querySeatList(): Promise<boolean> {
    if (!this.client) {
      message.error('WebSocket连接未建立');
      return false;
    }

    const listMessage = createBusinessMessage(
      BusinessModule.SEAT,
      'SEAT_LIST_QUERY',
      {}
    );

    return await this.client.send(listMessage);
  }

  /**
   * 座位状态变更回调
   */
  public onSeatStatusChanged?: (data: any) => void;

  /**
   * 座位列表接收回调
   */
  public onSeatListReceived?: (data: any) => void;

  /**
   * 设置座位状态变更监听器
   */
  setSeatStatusChangeListener(callback: (data: any) => void): void {
    this.onSeatStatusChanged = callback;
  }

  /**
   * 设置座位列表接收监听器
   */
  setSeatListReceiveListener(callback: (data: any) => void): void {
    this.onSeatListReceived = callback;
  }

  /**
   * 移除所有监听器
   */
  removeAllListeners(): void {
    this.onSeatStatusChanged = undefined;
    this.onSeatListReceived = undefined;
  }

  /**
   * 销毁服务
   */
  destroy(): void {
    if (!this.client) return;

    // 取消订阅模块
    this.client.unsubscribeModule(BusinessModule.SEAT);

    // 移除消息处理器
    this.client.offModuleMessage(BusinessModule.SEAT);

    // 移除监听器
    this.removeAllListeners();

    console.log('🪑 座位WebSocket服务已销毁');
  }
}

// 导出单例实例
let seatWebSocketService: SeatWebSocketService | null = null;

/**
 * 获取座位WebSocket服务实例
 */
export function getSeatWebSocketService(): SeatWebSocketService {
  if (!seatWebSocketService) {
    seatWebSocketService = new SeatWebSocketService();
  }
  return seatWebSocketService;
}

/**
 * 销毁座位WebSocket服务
 */
export function destroySeatWebSocketService(): void {
  if (seatWebSocketService) {
    seatWebSocketService.destroy();
    seatWebSocketService = null;
  }
}