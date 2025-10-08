/*
 * RocketMQ传输层实现
 * 基于HTTP + SSE的RocketMQ前端客户端
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-29
 * @Copyright 1024创新实验室
 */

import axios, { AxiosInstance } from 'axios';
import {
  RocketMQConfig,
  RocketMQMessage,
  RocketMQSendRequest,
  RocketMQSendResponse,
  RocketMQState,
  RocketMQEvents,
  IRocketMQTransport,
  RocketMQMessageHandler
} from '/@/types/rocketmq';

export class RocketMQTransport implements IRocketMQTransport {
  private httpClient: AxiosInstance;
  private eventSource: EventSource | null = null;
  private currentState: RocketMQState = RocketMQState.DISCONNECTED;
  private reconnectAttempts = 0;
  private reconnectTimer: NodeJS.Timeout | null = null;
  private isManualClose = false;

  // 事件处理器
  private eventHandlers = new Map<keyof RocketMQEvents, Set<Function>>();

  // 订阅的Topic列表
  private subscribedTopics: string[] = [];

  // 消息队列（离线时缓存）
  private messageQueue: RocketMQMessage[] = [];

  // 实例ID
  private instanceId = Math.random().toString(36).substring(2, 8);

  constructor(private config: RocketMQConfig) {
    this.initHttpClient();
    this.log('🚀 [RocketMQ] 创建RocketMQ传输客户端', this.config);
  }

  get state(): RocketMQState {
    return this.currentState;
  }

  get isConnected(): boolean {
    return this.currentState === RocketMQState.CONNECTED;
  }

  /**
   * 初始化HTTP客户端
   */
  private initHttpClient(): void {
    this.httpClient = axios.create({
      baseURL: this.config.proxyUrl,
      timeout: this.config.requestTimeout,
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': this.config.userId?.toString() || '',
        'X-User-Name': this.config.userName || '',
        'X-Session-Id': this.config.sessionId || ''
      }
    });

    // 请求拦截器
    this.httpClient.interceptors.request.use(
      (config) => {
        this.log('🚀 [HTTP] 发送请求:', config.method?.toUpperCase(), config.url);
        return config;
      },
      (error) => {
        this.log('🚀 [HTTP] 请求错误:', error);
        return Promise.reject(error);
      }
    );

    // 响应拦截器
    this.httpClient.interceptors.response.use(
      (response) => {
        this.log('🚀 [HTTP] 收到响应:', response.status, response.data);
        return response;
      },
      (error) => {
        this.log('🚀 [HTTP] 响应错误:', error.response?.status, error.message);
        return Promise.reject(error);
      }
    );
  }

  /**
   * 连接RocketMQ（通过SSE订阅）
   */
  async connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        if (this.isConnected || this.currentState === RocketMQState.CONNECTING) {
          resolve();
          return;
        }

        this.log('🚀 [RocketMQ] 开始连接...');
        this.setState(RocketMQState.CONNECTING);
        this.isManualClose = false;

        // 如果有订阅的Topic，建立SSE连接
        if (this.subscribedTopics.length > 0) {
          this.connectSSE()
            .then(() => {
              this.setState(RocketMQState.CONNECTED);
              this.reconnectAttempts = 0;
              this.flushMessageQueue();
              this.emit('connected');
              resolve();
            })
            .catch((error) => {
              this.setState(RocketMQState.ERROR);
              this.emit('error', error);
              reject(error);
            });
        } else {
          // 没有订阅Topic，直接标记为已连接（仅支持发送消息）
          this.setState(RocketMQState.CONNECTED);
          this.reconnectAttempts = 0;
          this.flushMessageQueue();
          this.emit('connected');
          resolve();
        }

      } catch (error) {
        this.log('🚀 [RocketMQ] 连接失败:', error);
        this.setState(RocketMQState.ERROR);
        reject(error);
      }
    });
  }

  /**
   * 建立SSE连接
   */
  private async connectSSE(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        const topicParams = this.subscribedTopics.map(topic => `topics=${encodeURIComponent(topic)}`).join('&');
        const sseUrl = `${this.config.proxyUrl}/rocketmq/subscribe?${topicParams}`;

        this.log('🚀 [SSE] 建立连接:', sseUrl);

        this.eventSource = new EventSource(sseUrl);

        this.eventSource.onopen = () => {
          this.log('🚀 [SSE] 连接已建立');
          resolve();
        };

        this.eventSource.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data);
            this.handleSSEMessage(data);
          } catch (error) {
            this.log('🚀 [SSE] 解析消息失败:', error, event.data);
          }
        };

        this.eventSource.onerror = (error) => {
          this.log('🚀 [SSE] 连接错误:', error);

          if (this.currentState === RocketMQState.CONNECTING) {
            reject(new Error('SSE连接失败'));
          }

          this.setState(RocketMQState.DISCONNECTED);
          this.emit('disconnected', 'SSE连接错误');

          // 自动重连
          if (!this.isManualClose) {
            this.scheduleReconnect();
          }
        };

        // 监听特定事件
        this.eventSource.addEventListener('connected', (event) => {
          const data = JSON.parse((event as MessageEvent).data);
          this.log('🚀 [SSE] 连接确认:', data);
        });

        this.eventSource.addEventListener('message', (event) => {
          try {
            const message: RocketMQMessage = JSON.parse((event as MessageEvent).data);
            this.emit('message', message);
          } catch (error) {
            this.log('🚀 [SSE] 处理业务消息失败:', error);
          }
        });

      } catch (error) {
        this.log('🚀 [SSE] 创建连接失败:', error);
        reject(error);
      }
    });
  }

  /**
   * 处理SSE消息
   */
  private handleSSEMessage(data: any): void {
    if (data.status === 'connected') {
      this.log('🚀 [SSE] 订阅确认:', data);
    } else if (data.messageId) {
      // 业务消息
      this.emit('message', data as RocketMQMessage);
    }
  }

  /**
   * 断开连接
   */
  disconnect(): void {
    this.log('🚀 [RocketMQ] 手动断开连接');
    this.isManualClose = true;
    this.clearReconnectTimer();

    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }

    this.setState(RocketMQState.DISCONNECTED);
    this.emit('disconnected', '手动断开');
  }

  /**
   * 发送消息
   */
  async send(message: RocketMQMessage): Promise<boolean> {
    if (!this.isConnected) {
      this.log('🚀 [RocketMQ] 连接未建立，消息加入队列:', message);
      this.messageQueue.push(message);

      // 尝试重新连接
      if (this.currentState !== RocketMQState.CONNECTING) {
        this.connect().catch(() => {
          // 连接失败，消息保留在队列中
        });
      }
      return false;
    }

    try {
      const request: RocketMQSendRequest = {
        topic: message.topic,
        tag: message.tag,
        type: message.type,
        module: message.module,
        keys: message.keys || message.messageId,
        data: message.data,
        content: message.content,
        orderly: message.orderly,
        orderId: message.shardingKey,
        delayLevel: message.delayLevel
      };

      const response = await this.httpClient.post<RocketMQSendResponse>('/rocketmq/send', request);

      if (response.data.success) {
        this.log('🚀 [RocketMQ] 消息发送成功:', response.data);
        return true;
      } else {
        this.log('🚀 [RocketMQ] 消息发送失败:', response.data.error);
        return false;
      }

    } catch (error: any) {
      this.log('🚀 [RocketMQ] 发送消息异常:', error.message);
      return false;
    }
  }

  /**
   * 订阅Topic
   */
  async subscribe(topics: string[]): Promise<void> {
    this.subscribedTopics = [...new Set([...this.subscribedTopics, ...topics])];
    this.log('🚀 [RocketMQ] 订阅Topic:', this.subscribedTopics);

    // 如果已连接，重新建立SSE连接
    if (this.isConnected && this.eventSource) {
      this.eventSource.close();
      await this.connectSSE();
    }
  }

  /**
   * 取消订阅Topic
   */
  async unsubscribe(topics: string[]): Promise<void> {
    this.subscribedTopics = this.subscribedTopics.filter(topic => !topics.includes(topic));
    this.log('🚀 [RocketMQ] 取消订阅Topic，剩余:', this.subscribedTopics);

    // 如果已连接，重新建立SSE连接
    if (this.isConnected && this.eventSource) {
      this.eventSource.close();
      if (this.subscribedTopics.length > 0) {
        await this.connectSSE();
      }
    }
  }

  /**
   * 添加事件处理器
   */
  on<K extends keyof RocketMQEvents>(event: K, handler: RocketMQEvents[K]): void {
    if (!this.eventHandlers.has(event)) {
      this.eventHandlers.set(event, new Set());
    }
    this.eventHandlers.get(event)!.add(handler);
  }

  /**
   * 移除事件处理器
   */
  off<K extends keyof RocketMQEvents>(event: K, handler?: RocketMQEvents[K]): void {
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
   * 触发事件
   */
  private emit<K extends keyof RocketMQEvents>(event: K, ...args: Parameters<RocketMQEvents[K]>): void {
    const handlers = this.eventHandlers.get(event);
    if (handlers) {
      handlers.forEach(handler => {
        try {
          // @ts-ignore
          handler(...args);
        } catch (error) {
          this.log(`🚀 [RocketMQ] 事件处理器执行失败 [${event}]:`, error);
        }
      });
    }
  }

  /**
   * 设置状态
   */
  private setState(newState: RocketMQState): void {
    const oldState = this.currentState;
    if (oldState !== newState) {
      this.currentState = newState;
      this.emit('stateChange', oldState, newState);
      this.log(`🚀 [RocketMQ] 状态变更: ${oldState} -> ${newState}`);
    }
  }

  /**
   * 安排重连
   */
  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= 5) {
      this.log('🚀 [RocketMQ] 达到最大重连次数，停止重连');
      return;
    }

    this.setState(RocketMQState.RECONNECTING);
    this.reconnectAttempts++;

    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts - 1), 30000);
    this.log(`🚀 [RocketMQ] 安排第${this.reconnectAttempts}次重连，${delay}ms后执行`);
    this.emit('reconnecting', this.reconnectAttempts);

    this.reconnectTimer = setTimeout(() => {
      this.log(`🚀 [RocketMQ] 开始第${this.reconnectAttempts}次重连`);
      this.connect()
        .then(() => {
          this.emit('reconnected');
        })
        .catch((error) => {
          this.log(`🚀 [RocketMQ] 第${this.reconnectAttempts}次重连失败:`, error);
          this.scheduleReconnect();
        });
    }, delay);
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
      this.log(`🚀 [RocketMQ] 发送缓存的${this.messageQueue.length}条消息`);
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
      console.log(`[RocketMQ-${this.instanceId}]`, message, ...args);
    }
  }

  /**
   * 销毁客户端
   */
  destroy(): void {
    this.log('🚀 [RocketMQ] 销毁客户端');
    this.disconnect();
    this.clearReconnectTimer();
    this.eventHandlers.clear();
    this.messageQueue = [];
    this.subscribedTopics = [];
  }
}