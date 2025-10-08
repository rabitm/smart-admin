/*
 * 统一消息客户端
 * 支持WebSocket和RocketMQ双传输模式，提供统一的API接口
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-29
 * @Copyright 1024创新实验室
 */

import { UnifiedWebSocketClient } from './unified-websocket-client';
import { RocketMQTransport } from './rocketmq-transport';
import {
  WebSocketMessage,
  WebSocketConfig,
  WebSocketState,
  WebSocketEvents,
  MessageHandler,
  IWebSocketClient
} from '/@/types/websocket';
import {
  RocketMQMessage,
  RocketMQConfig,
  RocketMQState,
  RocketMQEvents,
  getTopicByModule,
  ROCKETMQ_TOPICS
} from '/@/types/rocketmq';

/**
 * 传输类型
 */
export type TransportType = 'websocket' | 'rocketmq' | 'hybrid';

/**
 * 统一消息客户端配置
 */
export interface UnifiedMessageConfig {
  transportType: TransportType;
  primary?: 'websocket' | 'rocketmq';  // 混合模式下的主要传输方式
  fallback?: 'websocket' | 'rocketmq'; // 混合模式下的降级传输方式

  websocket?: WebSocketConfig;
  rocketmq?: RocketMQConfig;

  debug?: boolean;
}

/**
 * 传输接口
 */
interface IMessageTransport {
  readonly isConnected: boolean;
  connect(): Promise<void>;
  disconnect(): void;
  send(message: any): Promise<boolean>;
  on(event: string, handler: Function): void;
  off(event: string, handler?: Function): void;
  destroy(): void;
}

/**
 * 统一消息客户端
 * 兼容WebSocket和RocketMQ，提供相同的API接口
 */
export class UnifiedMessageClient implements IWebSocketClient {
  private primaryTransport: IMessageTransport;
  private fallbackTransport?: IMessageTransport;
  private config: UnifiedMessageConfig;
  private currentTransport: IMessageTransport;

  // 事件处理器
  private eventHandlers = new Map<keyof WebSocketEvents, Set<Function>>();

  // 模块消息处理器
  private moduleHandlers = new Map<string, Map<string, Set<MessageHandler>>>();

  // 实例ID
  private instanceId = Math.random().toString(36).substring(2, 8);

  constructor(config: UnifiedMessageConfig) {
    this.config = {
      debug: false,
      ...config
    };

    this.initializeTransports();
    this.log('🔄 [统一客户端] 创建实例', this.config);
  }

  /**
   * 初始化传输层
   */
  private initializeTransports(): void {
    if (this.config.transportType === 'websocket') {
      // 纯WebSocket模式
      if (!this.config.websocket) {
        throw new Error('WebSocket配置缺失');
      }
      this.primaryTransport = new UnifiedWebSocketClient(this.config.websocket);
      this.currentTransport = this.primaryTransport;

    } else if (this.config.transportType === 'rocketmq') {
      // 纯RocketMQ模式
      if (!this.config.rocketmq) {
        throw new Error('RocketMQ配置缺失');
      }
      this.primaryTransport = new RocketMQTransport(this.config.rocketmq) as any;
      this.currentTransport = this.primaryTransport;

    } else if (this.config.transportType === 'hybrid') {
      // 混合模式
      if (!this.config.websocket || !this.config.rocketmq) {
        throw new Error('混合模式需要WebSocket和RocketMQ配置');
      }

      const primary = this.config.primary || 'websocket';
      const fallback = this.config.fallback || (primary === 'websocket' ? 'rocketmq' : 'websocket');

      if (primary === 'websocket') {
        this.primaryTransport = new UnifiedWebSocketClient(this.config.websocket);
        this.fallbackTransport = new RocketMQTransport(this.config.rocketmq) as any;
      } else {
        this.primaryTransport = new RocketMQTransport(this.config.rocketmq) as any;
        this.fallbackTransport = new UnifiedWebSocketClient(this.config.websocket);
      }

      this.currentTransport = this.primaryTransport;
      this.setupHybridMode();
    }

    this.setupEventForwarding();
  }

  /**
   * 设置混合模式
   */
  private setupHybridMode(): void {
    if (!this.fallbackTransport) return;

    // 监听主传输层错误，自动切换到备用传输层
    this.primaryTransport.on('error', () => {
      this.log('🔄 [混合模式] 主传输层错误，切换到备用传输层');
      this.switchToFallback();
    });

    // 监听主传输层断开，自动切换到备用传输层
    this.primaryTransport.on('disconnected', () => {
      this.log('🔄 [混合模式] 主传输层断开，切换到备用传输层');
      this.switchToFallback();
    });

    // 监听主传输层重连成功，切换回主传输层
    this.primaryTransport.on('reconnected', () => {
      this.log('🔄 [混合模式] 主传输层重连成功，切换回主传输层');
      this.switchToPrimary();
    });
  }

  /**
   * 切换到备用传输层
   */
  private async switchToFallback(): Promise<void> {
    if (!this.fallbackTransport || this.currentTransport === this.fallbackTransport) {
      return;
    }

    try {
      this.log('🔄 [混合模式] 正在切换到备用传输层...');
      this.currentTransport = this.fallbackTransport;
      await this.currentTransport.connect();
      this.log('🔄 [混合模式] 已切换到备用传输层');
    } catch (error) {
      this.log('🔄 [混合模式] 切换到备用传输层失败:', error);
    }
  }

  /**
   * 切换回主传输层
   */
  private async switchToPrimary(): Promise<void> {
    if (this.currentTransport === this.primaryTransport) {
      return;
    }

    try {
      this.log('🔄 [混合模式] 正在切换回主传输层...');
      this.currentTransport = this.primaryTransport;
      this.log('🔄 [混合模式] 已切换回主传输层');
    } catch (error) {
      this.log('🔄 [混合模式] 切换回主传输层失败:', error);
    }
  }

  /**
   * 设置事件转发
   */
  private setupEventForwarding(): void {
    const events = ['connected', 'disconnected', 'error', 'message', 'reconnecting', 'reconnected'];

    events.forEach(event => {
      this.currentTransport.on(event, (...args: any[]) => {
        this.emit(event as keyof WebSocketEvents, ...args);
      });
    });
  }

  // ============ 实现IWebSocketClient接口 ============

  get state(): WebSocketState {
    if (this.config.transportType === 'rocketmq') {
      // 将RocketMQ状态映射为WebSocket状态
      const rocketState = (this.currentTransport as any).state as RocketMQState;
      return this.mapRocketMQStateToWebSocket(rocketState);
    }
    return (this.currentTransport as IWebSocketClient).state;
  }

  get isConnected(): boolean {
    return this.currentTransport.isConnected;
  }

  async connect(): Promise<void> {
    this.log('🔄 [统一客户端] 开始连接...');

    try {
      await this.currentTransport.connect();
      this.log('🔄 [统一客户端] 连接成功');
    } catch (error) {
      this.log('🔄 [统一客户端] 连接失败:', error);

      // 混合模式下尝试切换到备用传输层
      if (this.config.transportType === 'hybrid' && this.fallbackTransport) {
        await this.switchToFallback();
      } else {
        throw error;
      }
    }
  }

  disconnect(): void {
    this.log('🔄 [统一客户端] 断开连接');
    this.currentTransport.disconnect();
    this.fallbackTransport?.disconnect();
  }

  async send(message: WebSocketMessage): Promise<boolean> {
    try {
      if (this.config.transportType === 'rocketmq' ||
          (this.config.transportType === 'hybrid' && this.currentTransport === this.primaryTransport && this.config.primary === 'rocketmq')) {
        // 转换为RocketMQ消息
        const rocketMessage = this.convertToRocketMQMessage(message);
        return await (this.currentTransport as any).send(rocketMessage);
      } else {
        // WebSocket消息
        return await (this.currentTransport as IWebSocketClient).send(message);
      }
    } catch (error) {
      this.log('🔄 [统一客户端] 发送消息失败:', error);
      return false;
    }
  }

  async subscribeRoom(room: string): Promise<void> {
    if (this.config.transportType === 'rocketmq' ||
        (this.config.transportType === 'hybrid' && this.currentTransport === this.primaryTransport && this.config.primary === 'rocketmq')) {
      // RocketMQ订阅Topic
      const topics = [ROCKETMQ_TOPICS.POLICE_COLLABORATION]; // 房间相关的Topic
      await (this.currentTransport as any).subscribe(topics);
    } else {
      // WebSocket订阅房间
      await (this.currentTransport as IWebSocketClient).subscribeRoom(room);
    }
  }

  async unsubscribeRoom(room: string): Promise<void> {
    if (this.config.transportType === 'rocketmq' ||
        (this.config.transportType === 'hybrid' && this.currentTransport === this.primaryTransport && this.config.primary === 'rocketmq')) {
      // RocketMQ取消订阅Topic
      const topics = [ROCKETMQ_TOPICS.POLICE_COLLABORATION];
      await (this.currentTransport as any).unsubscribe(topics);
    } else {
      // WebSocket取消订阅房间
      await (this.currentTransport as IWebSocketClient).unsubscribeRoom(room);
    }
  }

  async subscribeModule(module: string): Promise<void> {
    if (this.config.transportType === 'rocketmq' ||
        (this.config.transportType === 'hybrid' && this.currentTransport === this.primaryTransport && this.config.primary === 'rocketmq')) {
      // RocketMQ订阅模块Topic
      const topic = getTopicByModule(module);
      await (this.currentTransport as any).subscribe([topic]);
    } else {
      // WebSocket订阅模块
      await (this.currentTransport as IWebSocketClient).subscribeModule(module);
    }
  }

  async unsubscribeModule(module: string): Promise<void> {
    if (this.config.transportType === 'rocketmq' ||
        (this.config.transportType === 'hybrid' && this.currentTransport === this.primaryTransport && this.config.primary === 'rocketmq')) {
      // RocketMQ取消订阅模块Topic
      const topic = getTopicByModule(module);
      await (this.currentTransport as any).unsubscribe([topic]);
    } else {
      // WebSocket取消订阅模块
      await (this.currentTransport as IWebSocketClient).unsubscribeModule(module);
    }
  }

  on<K extends keyof WebSocketEvents>(event: K, handler: WebSocketEvents[K]): void {
    if (!this.eventHandlers.has(event)) {
      this.eventHandlers.set(event, new Set());
    }
    this.eventHandlers.get(event)!.add(handler);
  }

  off<K extends keyof WebSocketEvents>(event: K, handler?: WebSocketEvents[K]): void {
    const handlers = this.eventHandlers.get(event);
    if (handlers) {
      if (handler) {
        handlers.delete(handler);
      } else {
        handlers.clear();
      }
    }
  }

  onModuleMessage(module: string, type: string, handler: MessageHandler): void {
    if (!this.moduleHandlers.has(module)) {
      this.moduleHandlers.set(module, new Map());
    }

    const moduleMap = this.moduleHandlers.get(module)!;
    if (!moduleMap.has(type)) {
      moduleMap.set(type, new Set());
    }

    moduleMap.get(type)!.add(handler);
    this.log(`🔄 [统一客户端] 注册模块消息处理器: ${module}.${type}`);
  }

  offModuleMessage(module: string, type?: string, handler?: MessageHandler): void {
    const moduleMap = this.moduleHandlers.get(module);
    if (!moduleMap) return;

    if (type) {
      const typeHandlers = moduleMap.get(type);
      if (typeHandlers) {
        if (handler) {
          typeHandlers.delete(handler);
        } else {
          typeHandlers.clear();
        }
        if (typeHandlers.size === 0) {
          moduleMap.delete(type);
        }
      }
    } else {
      moduleMap.clear();
    }

    if (moduleMap.size === 0) {
      this.moduleHandlers.delete(module);
    }

    this.log(`🔄 [统一客户端] 移除模块消息处理器: ${module}${type ? '.' + type : ''}`);
  }

  destroy(): void {
    this.log('🔄 [统一客户端] 销毁客户端');
    this.currentTransport.destroy();
    this.fallbackTransport?.destroy();
    this.eventHandlers.clear();
    this.moduleHandlers.clear();
  }

  // ============ 辅助方法 ============

  /**
   * 转换WebSocket消息为RocketMQ消息
   */
  private convertToRocketMQMessage(wsMessage: WebSocketMessage): RocketMQMessage {
    const topic = getTopicByModule(wsMessage.module || 'system');

    return {
      ...wsMessage,
      topic,
      tag: wsMessage.room ? `ROOM_${wsMessage.room}` :
           wsMessage.toUserId ? `USER_${wsMessage.toUserId}` : 'BROADCAST',
      keys: wsMessage.messageId
    };
  }

  /**
   * 映射RocketMQ状态到WebSocket状态
   */
  private mapRocketMQStateToWebSocket(rocketState: RocketMQState): WebSocketState {
    switch (rocketState) {
      case RocketMQState.CONNECTED:
        return WebSocketState.CONNECTED;
      case RocketMQState.CONNECTING:
        return WebSocketState.CONNECTING;
      case RocketMQState.DISCONNECTED:
        return WebSocketState.DISCONNECTED;
      case RocketMQState.RECONNECTING:
        return WebSocketState.RECONNECTING;
      case RocketMQState.ERROR:
        return WebSocketState.DISCONNECTED;
      default:
        return WebSocketState.DISCONNECTED;
    }
  }

  /**
   * 触发事件
   */
  private emit<K extends keyof WebSocketEvents>(event: K, ...args: Parameters<WebSocketEvents[K]>): void {
    const handlers = this.eventHandlers.get(event);
    if (handlers) {
      handlers.forEach(handler => {
        try {
          // @ts-ignore
          handler(...args);
        } catch (error) {
          this.log(`🔄 [统一客户端] 事件处理器执行失败 [${event}]:`, error);
        }
      });
    }

    // 处理模块消息
    if (event === 'message') {
      const message = args[0] as WebSocketMessage;
      this.handleModuleMessage(message);
    }
  }

  /**
   * 处理模块消息
   */
  private handleModuleMessage(message: WebSocketMessage): void {
    if (!message.module) return;

    const moduleMap = this.moduleHandlers.get(message.module);
    if (moduleMap) {
      const typeHandlers = moduleMap.get(message.type);
      if (typeHandlers) {
        typeHandlers.forEach(handler => {
          try {
            handler(message);
          } catch (error) {
            this.log('🔄 [统一客户端] 模块消息处理器执行失败:', error);
          }
        });
      }
    }
  }

  /**
   * 调试日志
   */
  private log(message: string, ...args: any[]): void {
    if (this.config.debug) {
      console.log(`[UnifiedMessage-${this.instanceId}]`, message, ...args);
    }
  }

  // ============ 公共方法 ============

  /**
   * 获取当前传输类型
   */
  getCurrentTransportType(): string {
    if (this.config.transportType === 'hybrid') {
      return this.currentTransport === this.primaryTransport ?
        (this.config.primary || 'websocket') :
        (this.config.fallback || 'rocketmq');
    }
    return this.config.transportType;
  }

  /**
   * 手动切换传输方式（仅混合模式）
   */
  async switchTransport(): Promise<void> {
    if (this.config.transportType !== 'hybrid' || !this.fallbackTransport) {
      throw new Error('仅混合模式支持手动切换传输方式');
    }

    if (this.currentTransport === this.primaryTransport) {
      await this.switchToFallback();
    } else {
      await this.switchToPrimary();
    }
  }

  /**
   * 获取连接统计信息
   */
  getConnectionStats(): Record<string, any> {
    return {
      transportType: this.config.transportType,
      currentTransport: this.getCurrentTransportType(),
      isConnected: this.isConnected,
      state: this.state,
      moduleHandlers: this.moduleHandlers.size,
      eventHandlers: this.eventHandlers.size
    };
  }
}