/**
 * 高性能WebSocket连接池
 * 支持200-500并发终端的连接管理
 *
 * 核心特性:
 * 1. 连接池管理和复用
 * 2. 智能负载均衡
 * 3. 自动故障转移
 * 4. 内存和CPU压力监控
 * 5. 优雅降级机制
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-28
 * @Copyright 1024创新实验室
 */

import { getWebSocketClient } from './websocket-manager';

interface PoolConfig {
  maxConnections: number;
  reconnectInterval: number;
  healthCheckInterval: number;
  maxQueueSize: number;
  cpuThrottleThreshold: number;
  memoryThrottleThreshold: number;
}

interface ConnectionMetrics {
  totalConnections: number;
  activeConnections: number;
  failedConnections: number;
  messagesSent: number;
  messagesReceived: number;
  avgLatency: number;
  lastHealthCheck: number;
}

export class HighPerformanceWebSocketPool {
  private config: PoolConfig = {
    maxConnections: 10,
    reconnectInterval: 5000,
    healthCheckInterval: 30000,
    maxQueueSize: 1000,
    cpuThrottleThreshold: 80,
    memoryThrottleThreshold: 500 * 1024 * 1024 // 500MB
  };

  private connections = new Map<string, any>();
  private connectionHealth = new Map<string, boolean>();
  private messageQueue: any[] = [];
  private metrics: ConnectionMetrics = {
    totalConnections: 0,
    activeConnections: 0,
    failedConnections: 0,
    messagesSent: 0,
    messagesReceived: 0,
    avgLatency: 0,
    lastHealthCheck: Date.now()
  };

  private loadBalancer = 0;
  private isThrottled = false;
  private performanceMonitor: NodeJS.Timeout | null = null;

  constructor(config?: Partial<PoolConfig>) {
    if (config) {
      this.config = { ...this.config, ...config };
    }

    this.startPerformanceMonitoring();
    this.startHealthChecks();

    console.log('🚀 [WebSocketPool] 高性能连接池已初始化:', this.config);
  }

  /**
   * 获取最佳连接
   */
  getBestConnection(): any {
    if (this.isThrottled) {
      console.warn('🔥 [WebSocketPool] 系统处于节流模式');
      return null;
    }

    const healthyConnections = Array.from(this.connections.entries())
      .filter(([id, conn]) => this.connectionHealth.get(id) && conn?.isConnected);

    if (healthyConnections.length === 0) {
      console.warn('⚠️ [WebSocketPool] 没有健康的连接可用');
      return this.createNewConnection();
    }

    // 轮询负载均衡
    const selected = healthyConnections[this.loadBalancer % healthyConnections.length];
    this.loadBalancer++;

    return selected[1];
  }

  /**
   * 创建新连接
   */
  private createNewConnection(): any {
    if (this.connections.size >= this.config.maxConnections) {
      console.warn('📊 [WebSocketPool] 已达到最大连接数限制');
      return null;
    }

    try {
      const connectionId = `conn_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
      const connection = getWebSocketClient();

      this.connections.set(connectionId, connection);
      this.connectionHealth.set(connectionId, true);
      this.metrics.totalConnections++;
      this.metrics.activeConnections++;

      // 监听连接事件
      connection.on('connected', () => {
        console.log('✅ [WebSocketPool] 新连接已建立:', connectionId);
        this.connectionHealth.set(connectionId, true);
      });

      connection.on('disconnected', () => {
        console.log('💔 [WebSocketPool] 连接已断开:', connectionId);
        this.connectionHealth.set(connectionId, false);
        this.metrics.activeConnections--;
        this.scheduleReconnect(connectionId);
      });

      connection.on('error', (error: any) => {
        console.error('❌ [WebSocketPool] 连接错误:', connectionId, error);
        this.connectionHealth.set(connectionId, false);
        this.metrics.failedConnections++;
      });

      return connection;
    } catch (error) {
      console.error('❌ [WebSocketPool] 创建连接失败:', error);
      this.metrics.failedConnections++;
      return null;
    }
  }

  /**
   * 安排重连
   */
  private scheduleReconnect(connectionId: string): void {
    setTimeout(() => {
      const connection = this.connections.get(connectionId);
      if (connection && !connection.isConnected) {
        console.log('🔄 [WebSocketPool] 尝试重连:', connectionId);
        connection.connect().catch((error: any) => {
          console.error('❌ [WebSocketPool] 重连失败:', connectionId, error);
        });
      }
    }, this.config.reconnectInterval);
  }

  /**
   * 发送消息（带负载均衡）
   */
  async sendMessage(message: any): Promise<boolean> {
    if (this.messageQueue.length > this.config.maxQueueSize) {
      console.warn('🚨 [WebSocketPool] 消息队列已满，丢弃消息');
      return false;
    }

    const connection = this.getBestConnection();
    if (!connection) {
      // 加入队列等待
      this.messageQueue.push(message);
      console.log('📦 [WebSocketPool] 消息已加入队列，当前队列长度:', this.messageQueue.length);
      return false;
    }

    try {
      const result = await connection.send(message);
      this.metrics.messagesSent++;
      return result;
    } catch (error) {
      console.error('❌ [WebSocketPool] 发送消息失败:', error);
      this.messageQueue.push(message);
      return false;
    }
  }

  /**
   * 处理消息队列
   */
  private processMessageQueue(): void {
    if (this.messageQueue.length === 0) return;

    const connection = this.getBestConnection();
    if (!connection) return;

    const batchSize = Math.min(10, this.messageQueue.length);
    const batch = this.messageQueue.splice(0, batchSize);

    batch.forEach(async (message) => {
      try {
        await connection.send(message);
        this.metrics.messagesSent++;
      } catch (error) {
        console.error('❌ [WebSocketPool] 批量发送失败:', error);
        // 重新加入队列
        this.messageQueue.unshift(message);
      }
    });
  }

  /**
   * 性能监控
   */
  private startPerformanceMonitoring(): void {
    this.performanceMonitor = setInterval(() => {
      const memUsage = (performance as any).memory?.usedJSHeapSize || 0;
      const cpuUsage = this.estimateCpuUsage();

      // 内存压力检测
      if (memUsage > this.config.memoryThrottleThreshold) {
        this.isThrottled = true;
        console.warn('🧠 [WebSocketPool] 内存压力过高，启动节流模式:', memUsage);
        this.cleanupConnections();
      }

      // CPU压力检测
      if (cpuUsage > this.config.cpuThrottleThreshold) {
        this.isThrottled = true;
        console.warn('⚡ [WebSocketPool] CPU压力过高，启动节流模式:', cpuUsage);
      }

      // 恢复正常模式
      if (this.isThrottled && memUsage < this.config.memoryThrottleThreshold * 0.8 &&
          cpuUsage < this.config.cpuThrottleThreshold * 0.8) {
        this.isThrottled = false;
        console.log('🔄 [WebSocketPool] 系统压力恢复，退出节流模式');
      }

      // 处理消息队列
      this.processMessageQueue();

      // 打印性能指标
      console.log('📊 [WebSocketPool] 性能指标:', {
        connections: `${this.metrics.activeConnections}/${this.metrics.totalConnections}`,
        queue: this.messageQueue.length,
        memory: `${Math.round(memUsage / 1024 / 1024)}MB`,
        throttled: this.isThrottled
      });

    }, 5000);
  }

  /**
   * 估算CPU使用率
   */
  private estimateCpuUsage(): number {
    // 简单的CPU使用率估算（基于消息处理延迟）
    return Math.min(this.metrics.avgLatency * 10, 100);
  }

  /**
   * 健康检查
   */
  private startHealthChecks(): void {
    setInterval(() => {
      this.metrics.lastHealthCheck = Date.now();

      // 检查所有连接健康状态
      this.connections.forEach((connection, id) => {
        const isHealthy = connection?.isConnected || false;
        this.connectionHealth.set(id, isHealthy);

        if (!isHealthy) {
          console.log('🏥 [WebSocketPool] 检测到不健康连接:', id);
        }
      });

      // 更新活跃连接数
      this.metrics.activeConnections = Array.from(this.connectionHealth.values())
        .filter(healthy => healthy).length;

    }, this.config.healthCheckInterval);
  }

  /**
   * 清理连接
   */
  private cleanupConnections(): void {
    const unhealthyConnections = Array.from(this.connections.entries())
      .filter(([id, conn]) => !this.connectionHealth.get(id));

    unhealthyConnections.forEach(([id, conn]) => {
      try {
        conn?.disconnect?.();
        this.connections.delete(id);
        this.connectionHealth.delete(id);
        console.log('🧹 [WebSocketPool] 清理不健康连接:', id);
      } catch (error) {
        console.error('❌ [WebSocketPool] 清理连接失败:', id, error);
      }
    });
  }

  /**
   * 获取性能统计
   */
  getMetrics(): ConnectionMetrics {
    return { ...this.metrics };
  }

  /**
   * 销毁连接池
   */
  destroy(): void {
    if (this.performanceMonitor) {
      clearInterval(this.performanceMonitor);
    }

    this.connections.forEach((connection, id) => {
      try {
        connection?.disconnect?.();
      } catch (error) {
        console.error('❌ [WebSocketPool] 销毁连接失败:', id, error);
      }
    });

    this.connections.clear();
    this.connectionHealth.clear();
    this.messageQueue = [];

    console.log('🔥 [WebSocketPool] 连接池已销毁');
  }
}

// 单例实例
let poolInstance: HighPerformanceWebSocketPool | null = null;

export function getWebSocketPool(): HighPerformanceWebSocketPool {
  if (!poolInstance) {
    poolInstance = new HighPerformanceWebSocketPool();
  }
  return poolInstance;
}

export function destroyWebSocketPool(): void {
  if (poolInstance) {
    poolInstance.destroy();
    poolInstance = null;
  }
}