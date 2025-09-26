/*
 * WebSocket统一消息类型定义
 *
 * @Author:    Claude Code Assistant
 * @Date:      2025-09-25
 * @Copyright  1024创新实验室
 */

/**
 * WebSocket消息接口
 */
export interface WebSocketMessage {
  /** 消息ID，用于追踪和去重 */
  messageId: string;
  /** 消息类型 */
  type: string;
  /** 业务模块（seat、police、collaboration等） */
  module: string;
  /** 发送者用户ID */
  fromUserId?: number;
  /** 发送者用户名 */
  fromUserName?: string;
  /** 接收者用户ID（为空表示广播） */
  toUserId?: number;
  /** 接收者房间/频道 */
  room?: string;
  /** 消息数据 */
  data?: Record<string, any>;
  /** 消息内容 */
  content?: string;
  /** 优先级：1-高，2-中，3-低 */
  priority?: number;
  /** 是否需要确认接收 */
  needAck?: boolean;
  /** 消息发送时间 */
  timestamp: string;
  /** 消息过期时间（秒） */
  expireAfter?: number;
  /** 重试次数 */
  retryCount?: number;
  /** 最大重试次数 */
  maxRetries?: number;
  /** 额外元数据 */
  metadata?: Record<string, any>;
}

/**
 * WebSocket连接状态
 */
export enum WebSocketState {
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  DISCONNECTING = 'DISCONNECTING',
  DISCONNECTED = 'DISCONNECTED',
  RECONNECTING = 'RECONNECTING'
}

/**
 * WebSocket连接配置
 */
export interface WebSocketConfig {
  /** WebSocket服务器URL */
  url: string;
  /** 是否自动重连 */
  autoReconnect?: boolean;
  /** 重连间隔（毫秒） */
  reconnectInterval?: number;
  /** 最大重连次数 */
  maxReconnectAttempts?: number;
  /** 心跳间隔（毫秒） */
  heartbeatInterval?: number;
  /** 连接超时（毫秒） */
  connectTimeout?: number;
  /** 调试模式 */
  debug?: boolean;
  /** 额外连接参数 */
  params?: Record<string, string | number>;
}

/**
 * 消息处理器接口
 */
export interface MessageHandler {
  (message: WebSocketMessage): void | Promise<void>;
}

/**
 * WebSocket事件类型
 */
export interface WebSocketEvents {
  /** 连接建立 */
  connected: () => void;
  /** 连接断开 */
  disconnected: (code: number, reason: string) => void;
  /** 连接错误 */
  error: (error: Event) => void;
  /** 重连开始 */
  reconnecting: (attempt: number) => void;
  /** 重连成功 */
  reconnected: () => void;
  /** 重连失败 */
  reconnectFailed: () => void;
  /** 消息接收 */
  message: (message: WebSocketMessage) => void;
  /** 状态变更 */
  stateChange: (oldState: WebSocketState, newState: WebSocketState) => void;
}

/**
 * WebSocket客户端接口
 */
export interface IWebSocketClient {
  /** 当前状态 */
  readonly state: WebSocketState;

  /** 是否已连接 */
  readonly isConnected: boolean;

  /** 连接WebSocket */
  connect(): Promise<void>;

  /** 断开连接 */
  disconnect(): void;

  /** 发送消息 */
  send(message: WebSocketMessage): Promise<boolean>;

  /** 订阅房间 */
  subscribeRoom(room: string): Promise<void>;

  /** 取消订阅房间 */
  unsubscribeRoom(room: string): Promise<void>;

  /** 订阅模块 */
  subscribeModule(module: string): Promise<void>;

  /** 取消订阅模块 */
  unsubscribeModule(module: string): Promise<void>;

  /** 添加消息处理器 */
  on<K extends keyof WebSocketEvents>(event: K, handler: WebSocketEvents[K]): void;

  /** 移除消息处理器 */
  off<K extends keyof WebSocketEvents>(event: K, handler?: WebSocketEvents[K]): void;

  /** 添加模块消息处理器 */
  onModuleMessage(module: string, type: string, handler: MessageHandler): void;

  /** 移除模块消息处理器 */
  offModuleMessage(module: string, type?: string, handler?: MessageHandler): void;
}

/**
 * 系统消息类型
 */
export enum SystemMessageType {
  PING = 'PING',
  PONG = 'PONG',
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  ERROR = 'ERROR',
  SUBSCRIBE_ROOM = 'SUBSCRIBE_ROOM',
  UNSUBSCRIBE_ROOM = 'UNSUBSCRIBE_ROOM',
  SUBSCRIBE_MODULE = 'SUBSCRIBE_MODULE',
  UNSUBSCRIBE_MODULE = 'UNSUBSCRIBE_MODULE',
  SUBSCRIBE_ACK = 'SUBSCRIBE_ACK',
  UNSUBSCRIBE_ACK = 'UNSUBSCRIBE_ACK',
  FORCE_DISCONNECT = 'FORCE_DISCONNECT'
}

/**
 * 业务模块类型
 */
export enum BusinessModule {
  SEAT = 'seat',
  POLICE = 'police',
  COLLABORATION = 'collaboration',
  SYSTEM = 'system'
}

/**
 * 创建系统消息
 */
export function createSystemMessage(type: SystemMessageType, content?: string, data?: Record<string, any>): WebSocketMessage {
  return {
    messageId: generateMessageId(),
    type,
    module: BusinessModule.SYSTEM,
    content,
    data,
    timestamp: new Date().toISOString(),
    priority: 2
  };
}

/**
 * 创建业务消息
 */
export function createBusinessMessage(
  module: BusinessModule,
  type: string,
  data?: Record<string, any>,
  options?: Partial<WebSocketMessage>
): WebSocketMessage {
  return {
    messageId: generateMessageId(),
    type,
    module,
    data,
    timestamp: new Date().toISOString(),
    priority: 2,
    ...options
  };
}

/**
 * 创建房间消息
 */
export function createRoomMessage(
  module: BusinessModule,
  type: string,
  room: string,
  data?: Record<string, any>
): WebSocketMessage {
  return createBusinessMessage(module, type, data, { room });
}

/**
 * 创建私人消息
 */
export function createDirectMessage(
  module: BusinessModule,
  type: string,
  toUserId: number,
  data?: Record<string, any>
): WebSocketMessage {
  return createBusinessMessage(module, type, data, { toUserId });
}

/**
 * 生成消息ID
 */
function generateMessageId(): string {
  return `${Date.now()}-${Math.random().toString(36).substring(2, 8)}`;
}