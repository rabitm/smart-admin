/**
 * WebSocket消息优化器
 * 提供消息压缩、批量处理、优先级管理等功能
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-26
 * @Copyright 1024创新实验室
 */

import pako from 'pako';
import { throttle, debounce } from 'lodash-es';

/**
 * 消息优先级
 */
export enum MessagePriority {
  CRITICAL = 0,  // 紧急消息，立即发送
  HIGH = 1,      // 高优先级
  NORMAL = 2,    // 普通优先级
  LOW = 3,       // 低优先级
  BATCH = 4      // 批量消息，可延迟
}

/**
 * 消息类型
 */
export interface OptimizedMessage {
  id: string;
  type: string;
  data: any;
  priority: MessagePriority;
  timestamp: number;
  size?: number;
  compressed?: boolean;
}

/**
 * 批量消息
 */
interface BatchMessage {
  type: 'BATCH';
  messages: OptimizedMessage[];
  count: number;
  timestamp: number;
  compressed?: boolean;
}

/**
 * 压缩配置
 */
interface CompressionConfig {
  enabled: boolean;
  threshold: number; // 压缩阈值（字节）
  level: number; // 压缩级别 (1-9)
}

/**
 * 优化配置
 */
interface OptimizerConfig {
  compression: CompressionConfig;
  batching: {
    enabled: boolean;
    maxSize: number;
    maxDelay: number;
  };
  throttling: {
    enabled: boolean;
    interval: number;
  };
  priorityQueue: {
    enabled: boolean;
    maxSize: number;
  };
}

/**
 * WebSocket消息优化器
 */
export class WebSocketMessageOptimizer {
  private config: OptimizerConfig;
  private messageQueue: Map<MessagePriority, OptimizedMessage[]>;
  private batchBuffer: OptimizedMessage[];
  private batchTimer: NodeJS.Timeout | null;
  private sendCallback: ((message: any) => void) | null;
  private throttledSend: Function;
  private stats: {
    totalMessages: number;
    compressedMessages: number;
    batchedMessages: number;
    totalBytes: number;
    compressedBytes: number;
    avgCompressionRatio: number;
  };

  constructor(config?: Partial<OptimizerConfig>) {
    this.config = {
      compression: {
        enabled: true,
        threshold: 1024, // 1KB
        level: 6
      },
      batching: {
        enabled: true,
        maxSize: 20,
        maxDelay: 100
      },
      throttling: {
        enabled: true,
        interval: 16 // 60fps
      },
      priorityQueue: {
        enabled: true,
        maxSize: 1000
      },
      ...config
    };

    // 初始化队列
    this.messageQueue = new Map();
    for (let i = 0; i <= MessagePriority.BATCH; i++) {
      this.messageQueue.set(i, []);
    }

    this.batchBuffer = [];
    this.batchTimer = null;
    this.sendCallback = null;

    // 初始化统计
    this.stats = {
      totalMessages: 0,
      compressedMessages: 0,
      batchedMessages: 0,
      totalBytes: 0,
      compressedBytes: 0,
      avgCompressionRatio: 1
    };

    // 创建限流发送函数
    this.throttledSend = throttle(
      this.processSendQueue.bind(this),
      this.config.throttling.interval
    );
  }

  /**
   * 设置发送回调
   */
  setSendCallback(callback: (message: any) => void) {
    this.sendCallback = callback;
  }

  /**
   * 发送消息
   */
  send(message: any, priority: MessagePriority = MessagePriority.NORMAL): void {
    const optimizedMessage: OptimizedMessage = {
      id: this.generateId(),
      type: message.type || 'UNKNOWN',
      data: message,
      priority,
      timestamp: Date.now(),
      size: this.calculateSize(message)
    };

    this.stats.totalMessages++;

    // 紧急消息立即发送
    if (priority === MessagePriority.CRITICAL) {
      this.sendImmediate(optimizedMessage);
      return;
    }

    // 加入优先级队列
    if (this.config.priorityQueue.enabled) {
      this.enqueueMessage(optimizedMessage);
    } else {
      this.sendImmediate(optimizedMessage);
    }
  }

  /**
   * 加入消息队列
   */
  private enqueueMessage(message: OptimizedMessage) {
    const queue = this.messageQueue.get(message.priority);
    if (!queue) return;

    // 检查队列大小
    if (queue.length >= this.config.priorityQueue.maxSize) {
      console.warn('[MessageOptimizer] 队列已满，丢弃最旧消息');
      queue.shift();
    }

    queue.push(message);

    // 如果启用批量处理，加入批量缓冲区
    if (this.config.batching.enabled && message.priority >= MessagePriority.NORMAL) {
      this.addToBatch(message);
    } else {
      // 触发发送
      this.throttledSend();
    }
  }

  /**
   * 添加到批量缓冲区
   */
  private addToBatch(message: OptimizedMessage) {
    this.batchBuffer.push(message);

    // 如果达到批量大小，立即发送
    if (this.batchBuffer.length >= this.config.batching.maxSize) {
      this.sendBatch();
    } else {
      // 设置延迟发送
      this.scheduleBatchSend();
    }
  }

  /**
   * 调度批量发送
   */
  private scheduleBatchSend() {
    if (this.batchTimer) return;

    this.batchTimer = setTimeout(() => {
      this.sendBatch();
    }, this.config.batching.maxDelay);
  }

  /**
   * 发送批量消息
   */
  private sendBatch() {
    if (this.batchTimer) {
      clearTimeout(this.batchTimer);
      this.batchTimer = null;
    }

    if (this.batchBuffer.length === 0) return;

    const batchMessage: BatchMessage = {
      type: 'BATCH',
      messages: [...this.batchBuffer],
      count: this.batchBuffer.length,
      timestamp: Date.now()
    };

    this.stats.batchedMessages += this.batchBuffer.length;
    this.batchBuffer = [];

    // 压缩并发送
    const compressed = this.compressIfNeeded(batchMessage);
    this.doSend(compressed);
  }

  /**
   * 处理发送队列
   */
  private processSendQueue() {
    // 按优先级发送消息
    for (let priority = 0; priority <= MessagePriority.BATCH; priority++) {
      const queue = this.messageQueue.get(priority);
      if (!queue || queue.length === 0) continue;

      // 发送该优先级的消息
      while (queue.length > 0) {
        const message = queue.shift();
        if (message) {
          this.sendImmediate(message);
        }
      }
    }
  }

  /**
   * 立即发送消息
   */
  private sendImmediate(message: OptimizedMessage) {
    const compressed = this.compressIfNeeded(message.data);
    this.doSend(compressed);
  }

  /**
   * 压缩消息（如果需要）
   */
  private compressIfNeeded(message: any): any {
    if (!this.config.compression.enabled) {
      return message;
    }

    const size = this.calculateSize(message);

    // 小于阈值不压缩
    if (size < this.config.compression.threshold) {
      return message;
    }

    try {
      const jsonStr = JSON.stringify(message);
      const compressed = pako.deflate(jsonStr, {
        level: this.config.compression.level
      });

      const compressedSize = compressed.byteLength;
      const ratio = compressedSize / size;

      // 如果压缩效果不好（大于80%原始大小），不使用压缩
      if (ratio > 0.8) {
        return message;
      }

      // 更新统计
      this.stats.compressedMessages++;
      this.stats.totalBytes += size;
      this.stats.compressedBytes += compressedSize;
      this.stats.avgCompressionRatio =
        this.stats.compressedBytes / this.stats.totalBytes;

      // 返回压缩后的消息
      return {
        compressed: true,
        data: this.arrayBufferToBase64(compressed),
        originalSize: size,
        compressedSize: compressedSize
      };

    } catch (error) {
      console.error('[MessageOptimizer] 压缩失败:', error);
      return message;
    }
  }

  /**
   * 解压消息
   */
  static decompress(message: any): any {
    if (!message.compressed) {
      return message;
    }

    try {
      const compressed = this.base64ToArrayBuffer(message.data);
      const decompressed = pako.inflate(compressed, { to: 'string' });
      return JSON.parse(decompressed);
    } catch (error) {
      console.error('[MessageOptimizer] 解压失败:', error);
      return message;
    }
  }

  /**
   * 执行发送
   */
  private doSend(message: any) {
    if (this.sendCallback) {
      this.sendCallback(message);
    } else {
      console.warn('[MessageOptimizer] 未设置发送回调');
    }
  }

  /**
   * 计算消息大小
   */
  private calculateSize(message: any): number {
    try {
      const str = JSON.stringify(message);
      return new Blob([str]).size;
    } catch {
      return 0;
    }
  }

  /**
   * 生成消息ID
   */
  private generateId(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * ArrayBuffer转Base64
   */
  private arrayBufferToBase64(buffer: ArrayBuffer): string {
    let binary = '';
    const bytes = new Uint8Array(buffer);
    const len = bytes.byteLength;

    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i]);
    }

    return btoa(binary);
  }

  /**
   * Base64转ArrayBuffer
   */
  private static base64ToArrayBuffer(base64: string): ArrayBuffer {
    const binary = atob(base64);
    const len = binary.length;
    const bytes = new Uint8Array(len);

    for (let i = 0; i < len; i++) {
      bytes[i] = binary.charCodeAt(i);
    }

    return bytes.buffer;
  }

  /**
   * 获取统计信息
   */
  getStats() {
    return {
      ...this.stats,
      queueSizes: Array.from(this.messageQueue.entries()).map(([priority, queue]) => ({
        priority,
        size: queue.length
      })),
      batchBufferSize: this.batchBuffer.length,
      compressionRatio: this.stats.avgCompressionRatio
        ? `${((1 - this.stats.avgCompressionRatio) * 100).toFixed(1)}%`
        : '0%'
    };
  }

  /**
   * 清空队列
   */
  clearQueue() {
    this.messageQueue.forEach(queue => queue.length = 0);
    this.batchBuffer = [];

    if (this.batchTimer) {
      clearTimeout(this.batchTimer);
      this.batchTimer = null;
    }
  }

  /**
   * 销毁
   */
  destroy() {
    this.clearQueue();
    this.sendCallback = null;
  }
}

// 导出单例
export const messageOptimizer = new WebSocketMessageOptimizer();
export default messageOptimizer;