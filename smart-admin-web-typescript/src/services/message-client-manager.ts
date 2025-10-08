/*
 * 消息客户端管理器
 * 提供全局统一的消息客户端实例管理
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-29
 * @Copyright 1024创新实验室
 */

import { UnifiedMessageClient, UnifiedMessageConfig } from '/@/utils/unified-message-client';
import { getMessageClientConfig } from '/@/config/message-client.config';
import { WebSocketMessage, MessageHandler } from '/@/types/websocket';
import { useUserStore } from '/@/store/modules/system/user';

/**
 * 消息客户端管理器
 */
class MessageClientManager {
  private client: UnifiedMessageClient | null = null;
  private config: UnifiedMessageConfig | null = null;
  private isInitialized = false;
  private initPromise: Promise<void> | null = null;

  /**
   * 初始化消息客户端
   */
  async initialize(customConfig?: Partial<UnifiedMessageConfig>): Promise<void> {
    if (this.initPromise) {
      return this.initPromise;
    }

    this.initPromise = this._doInitialize(customConfig);
    return this.initPromise;
  }

  private async _doInitialize(customConfig?: Partial<UnifiedMessageConfig>): Promise<void> {
    try {
      console.log('🔄 [消息管理器] 开始初始化消息客户端...');

      // 获取配置
      this.config = {
        ...getMessageClientConfig(),
        ...customConfig
      };

      // 添加用户信息到配置
      const userStore = useUserStore();
      const user = userStore.getLoginUser();
      if (user) {
        if (this.config.rocketmq) {
          this.config.rocketmq.userId = user.userId;
          this.config.rocketmq.userName = user.userName;
          this.config.rocketmq.sessionId = this.generateSessionId();
        }
        if (this.config.websocket) {
          this.config.websocket.params = {
            ...this.config.websocket.params,
            userId: user.userId,
            userName: user.userName
          };
        }
      }

      // 创建客户端
      this.client = new UnifiedMessageClient(this.config);

      // 设置事件监听
      this.setupEventListeners();

      // 连接
      await this.client.connect();

      // 订阅默认模块
      await this.subscribeDefaultModules();

      this.isInitialized = true;
      console.log('🔄 [消息管理器] 消息客户端初始化成功');

    } catch (error) {
      console.error('🔄 [消息管理器] 消息客户端初始化失败:', error);
      this.isInitialized = false;
      throw error;
    }
  }

  /**
   * 设置事件监听
   */
  private setupEventListeners(): void {
    if (!this.client) return;

    this.client.on('connected', () => {
      console.log('🔄 [消息管理器] 消息客户端已连接');
    });

    this.client.on('disconnected', (reason) => {
      console.log('🔄 [消息管理器] 消息客户端已断开:', reason);
    });

    this.client.on('error', (error) => {
      console.error('🔄 [消息管理器] 消息客户端错误:', error);
    });

    this.client.on('reconnecting', (attempt) => {
      console.log(`🔄 [消息管理器] 消息客户端重连中 (${attempt}/5)...`);
    });

    this.client.on('reconnected', () => {
      console.log('🔄 [消息管理器] 消息客户端重连成功');
    });

    this.client.on('message', (message) => {
      console.log('🔄 [消息管理器] 收到消息:', message);
    });
  }

  /**
   * 订阅默认模块
   */
  private async subscribeDefaultModules(): Promise<void> {
    if (!this.client) return;

    try {
      // 订阅用户相关的模块
      await this.client.subscribeModule('system');
      await this.client.subscribeModule('police');
      await this.client.subscribeModule('collaboration');
      await this.client.subscribeModule('seat');

      console.log('🔄 [消息管理器] 默认模块订阅完成');
    } catch (error) {
      console.error('🔄 [消息管理器] 默认模块订阅失败:', error);
    }
  }

  /**
   * 获取客户端实例
   */
  getClient(): UnifiedMessageClient | null {
    return this.client;
  }

  /**
   * 检查是否已初始化
   */
  isReady(): boolean {
    return this.isInitialized && this.client !== null;
  }

  /**
   * 发送消息
   */
  async sendMessage(message: WebSocketMessage): Promise<boolean> {
    if (!this.isReady() || !this.client) {
      console.warn('🔄 [消息管理器] 客户端未初始化，无法发送消息');
      return false;
    }

    try {
      return await this.client.send(message);
    } catch (error) {
      console.error('🔄 [消息管理器] 发送消息失败:', error);
      return false;
    }
  }

  /**
   * 订阅模块消息
   */
  onModuleMessage(module: string, type: string, handler: MessageHandler): void {
    if (!this.isReady() || !this.client) {
      console.warn('🔄 [消息管理器] 客户端未初始化，无法注册消息处理器');
      return;
    }

    this.client.onModuleMessage(module, type, handler);
  }

  /**
   * 取消订阅模块消息
   */
  offModuleMessage(module: string, type?: string, handler?: MessageHandler): void {
    if (!this.client) return;
    this.client.offModuleMessage(module, type, handler);
  }

  /**
   * 订阅房间
   */
  async subscribeRoom(room: string): Promise<void> {
    if (!this.isReady() || !this.client) {
      console.warn('🔄 [消息管理器] 客户端未初始化，无法订阅房间');
      return;
    }

    try {
      await this.client.subscribeRoom(room);
      console.log(`🔄 [消息管理器] 已订阅房间: ${room}`);
    } catch (error) {
      console.error(`🔄 [消息管理器] 订阅房间失败: ${room}`, error);
    }
  }

  /**
   * 取消订阅房间
   */
  async unsubscribeRoom(room: string): Promise<void> {
    if (!this.client) return;

    try {
      await this.client.unsubscribeRoom(room);
      console.log(`🔄 [消息管理器] 已取消订阅房间: ${room}`);
    } catch (error) {
      console.error(`🔄 [消息管理器] 取消订阅房间失败: ${room}`, error);
    }
  }

  /**
   * 获取连接状态
   */
  getConnectionState(): string {
    if (!this.client) return 'NOT_INITIALIZED';
    return this.client.state;
  }

  /**
   * 获取连接统计信息
   */
  getConnectionStats(): Record<string, any> {
    if (!this.client) {
      return {
        initialized: false,
        transportType: 'unknown',
        isConnected: false,
        state: 'NOT_INITIALIZED'
      };
    }

    return {
      initialized: this.isInitialized,
      ...this.client.getConnectionStats()
    };
  }

  /**
   * 手动切换传输方式（仅混合模式）
   */
  async switchTransport(): Promise<void> {
    if (!this.client) {
      throw new Error('客户端未初始化');
    }

    try {
      await this.client.switchTransport();
      console.log('🔄 [消息管理器] 传输方式切换成功');
    } catch (error) {
      console.error('🔄 [消息管理器] 传输方式切换失败:', error);
      throw error;
    }
  }

  /**
   * 重新初始化客户端
   */
  async reinitialize(customConfig?: Partial<UnifiedMessageConfig>): Promise<void> {
    this.destroy();
    await this.initialize(customConfig);
  }

  /**
   * 销毁客户端
   */
  destroy(): void {
    if (this.client) {
      this.client.destroy();
      this.client = null;
    }
    this.config = null;
    this.isInitialized = false;
    this.initPromise = null;
    console.log('🔄 [消息管理器] 消息客户端已销毁');
  }

  /**
   * 生成会话ID
   */
  private generateSessionId(): string {
    return 'session-' + Date.now() + '-' + Math.random().toString(36).substring(2, 8);
  }
}

// 创建全局单例
export const messageClientManager = new MessageClientManager();

// 导出便捷方法
export const useMessageClient = () => {
  return {
    manager: messageClientManager,
    client: messageClientManager.getClient(),
    isReady: messageClientManager.isReady(),
    sendMessage: messageClientManager.sendMessage.bind(messageClientManager),
    onModuleMessage: messageClientManager.onModuleMessage.bind(messageClientManager),
    offModuleMessage: messageClientManager.offModuleMessage.bind(messageClientManager),
    subscribeRoom: messageClientManager.subscribeRoom.bind(messageClientManager),
    unsubscribeRoom: messageClientManager.unsubscribeRoom.bind(messageClientManager),
    getConnectionState: messageClientManager.getConnectionState.bind(messageClientManager),
    getConnectionStats: messageClientManager.getConnectionStats.bind(messageClientManager),
    switchTransport: messageClientManager.switchTransport.bind(messageClientManager)
  };
};