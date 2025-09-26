/*
 * 全局WebSocket管理器
 *
 * @Author:    Claude Code Assistant
 * @Date:      2025-09-25
 * @Copyright  1024创新实验室
 */

import { UnifiedWebSocketClient } from './unified-websocket-client';
import { WebSocketConfig, IWebSocketClient } from '/@/types/websocket';
import { useUserStore } from '/@/store/modules/system/user';

class WebSocketManager {
  private client: UnifiedWebSocketClient | null = null;
  private initialized = false;

  /**
   * 初始化WebSocket连接
   */
  async initialize(): Promise<IWebSocketClient> {
    if (this.initialized && this.client) {
      return this.client;
    }

    const userStore = useUserStore();
    const employeeId = userStore.employeeId;

    if (!employeeId) {
      throw new Error('用户未登录，无法初始化WebSocket连接');
    }

    // 获取后端API地址并转换为WebSocket地址
    const apiUrl = import.meta.env.VITE_APP_API_URL || 'http://127.0.0.1:1024';
    const url = new URL(apiUrl);
    const protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${url.host}/api/websocket/unified`;

    const config: WebSocketConfig = {
      url: wsUrl,
      autoReconnect: true,
      reconnectInterval: 5000,
      maxReconnectAttempts: 5,
      heartbeatInterval: 30000,
      connectTimeout: 10000,
      debug: import.meta.env.DEV,
      params: {
        employeeId: employeeId
      }
    };

    // 销毁现有连接
    if (this.client) {
      this.client.destroy();
    }

    // 创建新连接
    this.client = new UnifiedWebSocketClient(config);

    // 设置全局事件处理
    this.setupGlobalEventHandlers();

    // 连接WebSocket
    await this.client.connect();

    this.initialized = true;
    console.log('🚀 WebSocket管理器初始化完成');

    return this.client;
  }

  /**
   * 获取WebSocket客户端
   */
  getClient(): IWebSocketClient | null {
    return this.client;
  }

  /**
   * 重新连接（用于用户登录后）
   */
  async reconnectAfterLogin(): Promise<IWebSocketClient> {
    console.log('🔄 用户登录后重新连接WebSocket');

    // 强制重新初始化
    this.initialized = false;

    return this.initialize();
  }

  /**
   * 断开连接（用于用户退出登录）
   */
  disconnect(): void {
    console.log('🔌 断开WebSocket连接');

    if (this.client) {
      this.client.disconnect();
      this.client.destroy();
      this.client = null;
    }

    this.initialized = false;
  }

  /**
   * 设置全局事件处理器
   */
  private setupGlobalEventHandlers(): void {
    if (!this.client) return;

    // 连接建立
    this.client.on('connected', () => {
      console.log('✅ WebSocket连接已建立');
    });

    // 连接断开
    this.client.on('disconnected', (code, reason) => {
      console.log(`❌ WebSocket连接已断开: ${code} ${reason}`);
    });

    // 连接错误
    this.client.on('error', (error) => {
      console.error('❌ WebSocket连接错误:', error);
    });

    // 重连开始
    this.client.on('reconnecting', (attempt) => {
      console.log(`🔄 WebSocket重连中... 第${attempt}次尝试`);
    });

    // 重连成功
    this.client.on('reconnected', () => {
      console.log('✅ WebSocket重连成功');
    });

    // 重连失败
    this.client.on('reconnectFailed', () => {
      console.error('❌ WebSocket重连失败，请刷新页面或检查网络连接');
    });

    // 状态变更
    this.client.on('stateChange', (oldState, newState) => {
      console.log(`🔄 WebSocket状态变更: ${oldState} -> ${newState}`);
    });
  }

  /**
   * 检查是否已连接
   */
  isConnected(): boolean {
    return this.client?.isConnected ?? false;
  }

  /**
   * 检查是否已初始化
   */
  isInitialized(): boolean {
    return this.initialized;
  }
}

// 导出单例实例
export const webSocketManager = new WebSocketManager();

/**
 * 获取WebSocket客户端的便捷方法
 */
export function getWebSocketClient(): IWebSocketClient | null {
  return webSocketManager.getClient();
}

/**
 * 初始化WebSocket连接的便捷方法
 */
export async function initWebSocket(): Promise<IWebSocketClient> {
  return webSocketManager.initialize();
}

/**
 * 用户登录后重新连接WebSocket的便捷方法
 */
export async function reconnectWebSocketAfterLogin(): Promise<IWebSocketClient> {
  return webSocketManager.reconnectAfterLogin();
}

/**
 * 断开WebSocket连接的便捷方法
 */
export function disconnectWebSocket(): void {
  webSocketManager.disconnect();
}