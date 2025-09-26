/*
 * 统一WebSocket客户端
 *
 * @Author:    Claude Code Assistant
 * @Date:      2025-09-25
 * @Copyright  1024创新实验室
 */

import { message } from 'ant-design-vue';
import {
  WebSocketMessage,
  WebSocketState,
  WebSocketConfig,
  WebSocketEvents,
  MessageHandler,
  IWebSocketClient,
  SystemMessageType,
  createSystemMessage
} from '/@/types/websocket';

export class UnifiedWebSocketClient implements IWebSocketClient {
  private ws: WebSocket | null = null;
  private config: Required<WebSocketConfig>;
  private currentState: WebSocketState = WebSocketState.DISCONNECTED;
  private reconnectAttempts = 0;
  private reconnectTimer: NodeJS.Timeout | null = null;
  private heartbeatTimer: NodeJS.Timeout | null = null;
  private connectTimeout: NodeJS.Timeout | null = null;
  private isManualClose = false;

  // 事件处理器
  private eventHandlers = new Map<keyof WebSocketEvents, Set<Function>>();

  // 模块消息处理器：module -> type -> handlers[]
  private moduleHandlers = new Map<string, Map<string, Set<MessageHandler>>>();

  // 待发送消息队列（连接断开时缓存）
  private messageQueue: WebSocketMessage[] = [];

  // 实例ID，用于调试
  private instanceId = Math.random().toString(36).substring(2, 8);

  constructor(config: WebSocketConfig) {
    this.config = {
      autoReconnect: true,
      reconnectInterval: 5000,
      maxReconnectAttempts: 5,
      heartbeatInterval: 30000,
      connectTimeout: 10000,
      debug: false,
      params: {},
      ...config
    };

    this.log('创建WebSocket客户端实例', this.config);
  }

  get state(): WebSocketState {
    return this.currentState;
  }

  get isConnected(): boolean {
    return this.currentState === WebSocketState.CONNECTED;
  }

  /**
   * 连接WebSocket
   */
  async connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        if (this.isConnected || this.currentState === WebSocketState.CONNECTING) {
          resolve();
          return;
        }

        this.log('开始连接WebSocket');
        this.setState(WebSocketState.CONNECTING);
        this.isManualClose = false;

        // 构建连接URL
        const url = this.buildWebSocketUrl();
        this.log('连接URL:', url);

        this.ws = new WebSocket(url);

        // 设置连接超时
        this.connectTimeout = setTimeout(() => {
          if (this.currentState === WebSocketState.CONNECTING) {
            this.log('连接超时');
            this.ws?.close();
            reject(new Error('WebSocket连接超时'));
          }
        }, this.config.connectTimeout);

        this.ws.onopen = () => {
          this.clearConnectTimeout();
          this.log('WebSocket连接已建立');
          this.setState(WebSocketState.CONNECTED);
          this.reconnectAttempts = 0;
          this.startHeartbeat();
          this.flushMessageQueue();
          this.emit('connected');
          resolve();
        };

        this.ws.onmessage = (event) => {
          try {
            const message: WebSocketMessage = JSON.parse(event.data);
            this.log('收到消息:', message);
            this.handleMessage(message);
          } catch (error) {
            this.log('解析消息失败:', error, event.data);
          }
        };

        this.ws.onclose = (event) => {
          this.clearConnectTimeout();
          this.clearHeartbeat();
          this.log('WebSocket连接已关闭:', event.code, event.reason);

          if (this.currentState === WebSocketState.CONNECTING) {
            reject(new Error(`WebSocket连接失败: ${event.code} ${event.reason}`));
          }

          this.setState(WebSocketState.DISCONNECTED);
          this.emit('disconnected', event.code, event.reason);

          // 自动重连
          if (!this.isManualClose && this.config.autoReconnect) {
            this.scheduleReconnect();
          }
        };

        this.ws.onerror = (error) => {
          this.log('WebSocket错误:', error);
          this.emit('error', error);

          if (this.currentState === WebSocketState.CONNECTING) {
            reject(error);
          }
        };

      } catch (error) {
        this.log('创建WebSocket连接失败:', error);
        reject(error);
      }
    });
  }

  /**
   * 断开连接
   */
  disconnect(): void {
    this.log('手动断开WebSocket连接');
    this.isManualClose = true;
    this.clearReconnectTimer();
    this.clearHeartbeat();
    this.clearConnectTimeout();

    if (this.ws) {
      this.setState(WebSocketState.DISCONNECTING);
      this.ws.close(1000, 'Manual disconnect');
    }

    this.setState(WebSocketState.DISCONNECTED);
    this.ws = null;
  }

  /**
   * 发送消息
   */
  async send(message: WebSocketMessage): Promise<boolean> {
    if (!this.isConnected) {
      this.log('连接未建立，消息加入队列:', message);
      this.messageQueue.push(message);

      // 如果不是连接中状态，尝试重新连接
      if (this.currentState !== WebSocketState.CONNECTING) {
        this.connect().catch(() => {
          // 连接失败，消息保留在队列中
        });
      }
      return false;
    }

    try {
      const jsonMessage = JSON.stringify(message);
      this.ws!.send(jsonMessage);
      this.log('发送消息:', message);
      return true;
    } catch (error) {
      this.log('发送消息失败:', error);
      return false;
    }
  }

  /**
   * 订阅房间
   */
  async subscribeRoom(room: string): Promise<void> {
    const message = createSystemMessage(SystemMessageType.SUBSCRIBE_ROOM, `订阅房间: ${room}`, { room });
    await this.send(message);
  }

  /**
   * 取消订阅房间
   */
  async unsubscribeRoom(room: string): Promise<void> {
    const message = createSystemMessage(SystemMessageType.UNSUBSCRIBE_ROOM, `取消订阅房间: ${room}`, { room });
    await this.send(message);
  }

  /**
   * 订阅模块
   */
  async subscribeModule(module: string): Promise<void> {
    const message = createSystemMessage(SystemMessageType.SUBSCRIBE_MODULE, `订阅模块: ${module}`, { module });
    await this.send(message);
  }

  /**
   * 取消订阅模块
   */
  async unsubscribeModule(module: string): Promise<void> {
    const message = createSystemMessage(SystemMessageType.UNSUBSCRIBE_MODULE, `取消订阅模块: ${module}`, { module });
    await this.send(message);
  }

  /**
   * 添加事件处理器
   */
  on<K extends keyof WebSocketEvents>(event: K, handler: WebSocketEvents[K]): void {
    if (!this.eventHandlers.has(event)) {
      this.eventHandlers.set(event, new Set());
    }
    this.eventHandlers.get(event)!.add(handler);
  }

  /**
   * 移除事件处理器
   */
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

  /**
   * 添加模块消息处理器
   */
  onModuleMessage(module: string, type: string, handler: MessageHandler): void {
    if (!this.moduleHandlers.has(module)) {
      this.moduleHandlers.set(module, new Map());
    }

    const moduleMap = this.moduleHandlers.get(module)!;
    if (!moduleMap.has(type)) {
      moduleMap.set(type, new Set());
    }

    moduleMap.get(type)!.add(handler);
    this.log(`注册模块消息处理器: ${module}.${type}`);
  }

  /**
   * 移除模块消息处理器
   */
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

    this.log(`移除模块消息处理器: ${module}${type ? '.' + type : ''}`);
  }

  /**
   * 处理接收到的消息
   */
  private handleMessage(message: WebSocketMessage): void {
    this.emit('message', message);

    // 处理系统消息
    if (message.module === 'system') {
      this.handleSystemMessage(message);
      return;
    }

    // 处理业务消息
    const moduleMap = this.moduleHandlers.get(message.module);
    if (moduleMap) {
      const typeHandlers = moduleMap.get(message.type);
      if (typeHandlers) {
        typeHandlers.forEach(handler => {
          try {
            handler(message);
          } catch (error) {
            this.log('模块消息处理器执行失败:', error);
          }
        });
      }
    }
  }

  /**
   * 处理系统消息
   */
  private handleSystemMessage(message: WebSocketMessage): void {
    switch (message.type) {
      case SystemMessageType.PONG:
        this.log('收到心跳响应');
        break;

      case SystemMessageType.CONNECTED:
        this.log('服务器确认连接:', message.content);
        break;

      case SystemMessageType.ERROR:
        this.log('服务器错误:', message.content);
        this.emit('error', new Error(message.content || '服务器错误'));
        break;

      case SystemMessageType.SUBSCRIBE_ACK:
      case SystemMessageType.UNSUBSCRIBE_ACK:
        this.log('订阅操作确认:', message.content);
        break;

      case SystemMessageType.FORCE_DISCONNECT:
        this.log('服务器强制断开:', message.content);
        message.warning(message.content || '连接被服务器强制断开');
        this.isManualClose = true; // 避免自动重连
        break;

      default:
        this.log('未处理的系统消息:', message.type);
    }
  }

  /**
   * 构建WebSocket连接URL
   */
  private buildWebSocketUrl(): string {
    const url = new URL(this.config.url);

    // 添加配置参数
    Object.entries(this.config.params).forEach(([key, value]) => {
      url.searchParams.set(key, String(value));
    });

    return url.toString();
  }

  /**
   * 设置状态并触发事件
   */
  private setState(newState: WebSocketState): void {
    const oldState = this.currentState;
    if (oldState !== newState) {
      this.currentState = newState;
      this.emit('stateChange', oldState, newState);
      this.log(`状态变更: ${oldState} -> ${newState}`);
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
          this.log(`事件处理器执行失败 [${event}]:`, error);
        }
      });
    }
  }

  /**
   * 开始心跳
   */
  private startHeartbeat(): void {
    this.clearHeartbeat();
    this.heartbeatTimer = setInterval(() => {
      if (this.isConnected) {
        const pingMessage = createSystemMessage(SystemMessageType.PING, '心跳检测', {
          timestamp: Date.now()
        });
        this.send(pingMessage);
      }
    }, this.config.heartbeatInterval);
  }

  /**
   * 停止心跳
   */
  private clearHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  /**
   * 清除连接超时
   */
  private clearConnectTimeout(): void {
    if (this.connectTimeout) {
      clearTimeout(this.connectTimeout);
      this.connectTimeout = null;
    }
  }

  /**
   * 安排重连
   */
  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.config.maxReconnectAttempts) {
      this.log('达到最大重连次数，停止重连');
      this.emit('reconnectFailed');
      return;
    }

    this.setState(WebSocketState.RECONNECTING);
    this.reconnectAttempts++;

    this.log(`安排第${this.reconnectAttempts}次重连，${this.config.reconnectInterval}ms后执行`);
    this.emit('reconnecting', this.reconnectAttempts);

    this.reconnectTimer = setTimeout(() => {
      this.log(`开始第${this.reconnectAttempts}次重连`);
      this.connect()
        .then(() => {
          this.emit('reconnected');
        })
        .catch((error) => {
          this.log(`第${this.reconnectAttempts}次重连失败:`, error);
          this.scheduleReconnect();
        });
    }, this.config.reconnectInterval);
  }

  /**
   * 清除重连定时器
   */
  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  /**
   * 刷新消息队列
   */
  private flushMessageQueue(): void {
    if (this.messageQueue.length > 0) {
      this.log(`发送缓存的${this.messageQueue.length}条消息`);
      const messages = [...this.messageQueue];
      this.messageQueue = [];

      messages.forEach(message => {
        this.send(message);
      });
    }
  }

  /**
   * 调试日志
   */
  private log(message: string, ...args: any[]): void {
    if (this.config.debug) {
      console.log(`[WebSocket-${this.instanceId}]`, message, ...args);
    }
  }

  /**
   * 销毁客户端
   */
  destroy(): void {
    this.log('销毁WebSocket客户端');
    this.disconnect();
    this.eventHandlers.clear();
    this.moduleHandlers.clear();
    this.messageQueue = [];
  }
}