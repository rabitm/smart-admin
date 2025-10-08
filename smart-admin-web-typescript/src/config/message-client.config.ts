/*
 * 消息客户端配置
 * 支持WebSocket和RocketMQ双传输模式配置
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-29
 * @Copyright 1024创新实验室
 */

import { UnifiedMessageConfig } from '/@/utils/unified-message-client';

/**
 * 获取消息客户端配置
 */
export function getMessageClientConfig(): UnifiedMessageConfig {
  // 从环境变量或配置中读取传输模式
  const transportType = import.meta.env.VITE_MESSAGE_TRANSPORT_TYPE || 'hybrid';
  const primaryTransport = import.meta.env.VITE_MESSAGE_PRIMARY_TRANSPORT || 'websocket';
  const fallbackTransport = import.meta.env.VITE_MESSAGE_FALLBACK_TRANSPORT || 'rocketmq';

  // 获取基础URL
  const baseURL = window.location.origin;
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const httpProtocol = window.location.protocol;

  // 配置
  const config: UnifiedMessageConfig = {
    transportType: transportType as any,
    primary: primaryTransport as any,
    fallback: fallbackTransport as any,
    debug: import.meta.env.DEV || import.meta.env.VITE_MESSAGE_DEBUG === 'true',

    // WebSocket配置
    websocket: {
      url: `${wsProtocol}//${window.location.host}/websocket`,
      autoReconnect: true,
      reconnectInterval: 5000,
      maxReconnectAttempts: 5,
      heartbeatInterval: 30000,
      connectTimeout: 10000,
      debug: import.meta.env.DEV,
      params: {}
    },

    // RocketMQ配置
    rocketmq: {
      proxyUrl: `${httpProtocol}//${window.location.host}`,
      reconnectInterval: 3000,
      requestTimeout: 30000,
      batchSize: 50,
      debug: import.meta.env.DEV
    }
  };

  // 开发环境特殊配置
  if (import.meta.env.DEV) {
    config.websocket!.url = 'ws://localhost:1024/websocket';
    config.rocketmq!.proxyUrl = 'http://localhost:1024';
  }

  return config;
}

/**
 * RocketMQ专用配置（用于纯RocketMQ模式）
 */
export function getRocketMQOnlyConfig(): UnifiedMessageConfig {
  const baseConfig = getMessageClientConfig();
  return {
    ...baseConfig,
    transportType: 'rocketmq'
  };
}

/**
 * WebSocket专用配置（用于纯WebSocket模式）
 */
export function getWebSocketOnlyConfig(): UnifiedMessageConfig {
  const baseConfig = getMessageClientConfig();
  return {
    ...baseConfig,
    transportType: 'websocket'
  };
}

/**
 * 混合模式配置（默认）
 */
export function getHybridConfig(primary: 'websocket' | 'rocketmq' = 'websocket'): UnifiedMessageConfig {
  const baseConfig = getMessageClientConfig();
  return {
    ...baseConfig,
    transportType: 'hybrid',
    primary,
    fallback: primary === 'websocket' ? 'rocketmq' : 'websocket'
  };
}