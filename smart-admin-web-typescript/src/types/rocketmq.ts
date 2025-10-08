/*
 * RocketMQ消息类型定义
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-29
 * @Copyright 1024创新实验室
 */

import { WebSocketMessage } from './websocket';

/**
 * RocketMQ消息协议
 */
export interface RocketMQMessage extends WebSocketMessage {
  // RocketMQ特有字段
  topic: string;           // 消息主题
  tag?: string;           // 消息标签
  keys?: string;          // 消息关键字
  orderly?: boolean;      // 是否顺序消息
  delayLevel?: number;    // 延迟级别

  // 消息路由信息
  targetUsers?: number[]; // 目标用户ID列表
  targetDepts?: number[]; // 目标部门ID列表
  targetRoles?: string[]; // 目标角色列表

  // 分区键，用于顺序消息
  shardingKey?: string;
}

/**
 * RocketMQ发送请求
 */
export interface RocketMQSendRequest {
  topic: string;
  tag?: string;
  type: string;
  module: string;
  keys?: string;
  data?: Record<string, any>;
  content?: string;
  orderly?: boolean;
  orderId?: string;
  delayLevel?: number;
}

/**
 * RocketMQ发送响应
 */
export interface RocketMQSendResponse {
  success: boolean;
  messageId?: string;
  topic?: string;
  tag?: string;
  timestamp?: string;
  error?: string;
}

/**
 * RocketMQ配置
 */
export interface RocketMQConfig {
  proxyUrl: string;        // HTTP代理地址
  userId?: number;         // 用户ID
  userName?: string;       // 用户名
  sessionId?: string;      // 会话ID
  reconnectInterval: number; // 重连间隔(ms)
  requestTimeout: number;  // 请求超时(ms)
  batchSize: number;       // 批量处理大小
  debug?: boolean;         // 调试模式
}

/**
 * RocketMQ Topic常量
 */
export const ROCKETMQ_TOPICS = {
  // 系统级消息
  SYSTEM_BROADCAST: 'SYSTEM_BROADCAST',
  SYSTEM_NOTIFICATION: 'SYSTEM_NOTIFICATION',

  // 警情模块
  POLICE_FIELD_SYNC: 'POLICE_FIELD_SYNC',
  POLICE_LIST_UPDATE: 'POLICE_LIST_UPDATE',
  POLICE_COLLABORATION: 'POLICE_COLLABORATION',

  // 协同模块
  COLLABORATION_FIELD_LOCK: 'COLLABORATION_FIELD_LOCK',
  COLLABORATION_USER_STATUS: 'COLLABORATION_USER_STATUS',

  // 座席模块
  SEAT_STATUS_UPDATE: 'SEAT_STATUS_UPDATE'
} as const;

/**
 * 业务模块到Topic映射
 */
export const MODULE_TOPIC_MAPPING: Record<string, string> = {
  police: ROCKETMQ_TOPICS.POLICE_FIELD_SYNC,
  collaboration: ROCKETMQ_TOPICS.COLLABORATION_FIELD_LOCK,
  seat: ROCKETMQ_TOPICS.SEAT_STATUS_UPDATE,
  system: ROCKETMQ_TOPICS.SYSTEM_BROADCAST
};

/**
 * 获取模块对应的Topic
 */
export function getTopicByModule(module: string): string {
  return MODULE_TOPIC_MAPPING[module.toLowerCase()] || ROCKETMQ_TOPICS.SYSTEM_BROADCAST;
}

/**
 * RocketMQ消息处理器
 */
export type RocketMQMessageHandler = (message: RocketMQMessage) => void;

/**
 * RocketMQ连接状态
 */
export enum RocketMQState {
  DISCONNECTED = 'DISCONNECTED',
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  RECONNECTING = 'RECONNECTING',
  ERROR = 'ERROR'
}

/**
 * RocketMQ事件类型
 */
export interface RocketMQEvents {
  connected: () => void;
  disconnected: (reason?: string) => void;
  error: (error: Error) => void;
  message: (message: RocketMQMessage) => void;
  reconnecting: (attempt: number) => void;
  reconnected: () => void;
  stateChange: (oldState: RocketMQState, newState: RocketMQState) => void;
}

/**
 * RocketMQ传输接口
 */
export interface IRocketMQTransport {
  readonly state: RocketMQState;
  readonly isConnected: boolean;

  connect(): Promise<void>;
  disconnect(): void;
  send(message: RocketMQMessage): Promise<boolean>;
  subscribe(topics: string[]): Promise<void>;
  unsubscribe(topics: string[]): Promise<void>;

  on<K extends keyof RocketMQEvents>(event: K, handler: RocketMQEvents[K]): void;
  off<K extends keyof RocketMQEvents>(event: K, handler?: RocketMQEvents[K]): void;

  destroy(): void;
}