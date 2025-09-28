/**
 * 高并发性能监控和降级机制
 *
 * 核心功能:
 * 1. 实时性能监控
 * 2. 智能降级策略
 * 3. 自适应调节
 * 4. 告警和恢复机制
 * 5. 性能数据收集
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-28
 * @Copyright 1024创新实验室
 */

export interface PerformanceMetrics {
  cpu: number;
  memory: number;
  network: number;
  fps: number;
  latency: number;
  messageQueue: number;
  errorRate: number;
  timestamp: number;
}

export interface DegradationConfig {
  cpuThreshold: number;
  memoryThreshold: number;
  latencyThreshold: number;
  errorRateThreshold: number;
  degradationSteps: DegradationStep[];
}

export interface DegradationStep {
  level: number;
  name: string;
  actions: string[];
  conditions: {
    cpu?: number;
    memory?: number;
    latency?: number;
    errorRate?: number;
  };
}

export class PerformanceMonitor {
  private metrics: PerformanceMetrics[] = [];
  private currentLevel = 0;
  private isMonitoring = false;
  private monitorTimer: NodeJS.Timeout | null = null;
  private frameCount = 0;
  private lastFrameTime = 0;

  private config: DegradationConfig = {
    cpuThreshold: 80,
    memoryThreshold: 500 * 1024 * 1024, // 500MB
    latencyThreshold: 1000, // 1s
    errorRateThreshold: 0.1, // 10%
    degradationSteps: [
      {
        level: 0,
        name: 'Normal',
        actions: [],
        conditions: {}
      },
      {
        level: 1,
        name: 'Light Degradation',
        actions: ['reduce_update_frequency', 'increase_batch_size'],
        conditions: { cpu: 70, memory: 400 * 1024 * 1024 }
      },
      {
        level: 2,
        name: 'Medium Degradation',
        actions: ['disable_animations', 'reduce_render_frequency', 'compress_messages'],
        conditions: { cpu: 80, memory: 500 * 1024 * 1024, latency: 500 }
      },
      {
        level: 3,
        name: 'Heavy Degradation',
        actions: ['minimal_updates_only', 'disable_realtime', 'emergency_mode'],
        conditions: { cpu: 90, memory: 700 * 1024 * 1024, latency: 1000, errorRate: 0.1 }
      }
    ]
  };

  private callbacks = new Map<string, Function[]>();

  constructor(config?: Partial<DegradationConfig>) {
    if (config) {
      this.config = { ...this.config, ...config };
    }

    this.startFPSMonitoring();
    console.log('📊 [PerformanceMonitor] 性能监控器已初始化');
  }

  /**
   * 开始监控
   */
  start(): void {
    if (this.isMonitoring) return;

    this.isMonitoring = true;
    this.monitorTimer = setInterval(() => {
      this.collectMetrics();
      this.evaluateDegradation();
    }, 1000);

    console.log('🚀 [PerformanceMonitor] 开始性能监控');
  }

  /**
   * 停止监控
   */
  stop(): void {
    this.isMonitoring = false;
    if (this.monitorTimer) {
      clearInterval(this.monitorTimer);
      this.monitorTimer = null;
    }

    console.log('⏹️ [PerformanceMonitor] 停止性能监控');
  }

  /**
   * 收集性能指标
   */
  private collectMetrics(): void {
    const now = performance.now();
    const memory = (performance as any).memory;

    const metrics: PerformanceMetrics = {
      cpu: this.estimateCPUUsage(),
      memory: memory?.usedJSHeapSize || 0,
      network: this.estimateNetworkLatency(),
      fps: this.calculateFPS(),
      latency: this.calculateAverageLatency(),
      messageQueue: this.getMessageQueueSize(),
      errorRate: this.calculateErrorRate(),
      timestamp: now
    };

    this.metrics.push(metrics);

    // 保持最近100个数据点
    if (this.metrics.length > 100) {
      this.metrics.shift();
    }

    this.emitEvent('metrics_updated', metrics);
  }

  /**
   * 估算CPU使用率
   */
  private estimateCPUUsage(): number {
    // 基于任务执行时间和帧率估算CPU使用率
    const avgLatency = this.calculateAverageLatency();
    const fpsPenalty = Math.max(0, 60 - this.calculateFPS()) * 2;

    return Math.min(100, avgLatency / 10 + fpsPenalty);
  }

  /**
   * 估算网络延迟
   */
  private estimateNetworkLatency(): number {
    // 简单的网络延迟估算（实际项目中可以用WebRTC或其他方法）
    return Math.random() * 100 + 50;
  }

  /**
   * 计算FPS
   */
  private calculateFPS(): number {
    const now = performance.now();
    const deltaTime = now - this.lastFrameTime;

    if (deltaTime >= 1000) {
      const fps = this.frameCount * 1000 / deltaTime;
      this.frameCount = 0;
      this.lastFrameTime = now;
      return fps;
    }

    return 60; // 默认60fps
  }

  /**
   * 开始FPS监控
   */
  private startFPSMonitoring(): void {
    const countFrame = () => {
      this.frameCount++;
      requestAnimationFrame(countFrame);
    };
    requestAnimationFrame(countFrame);
  }

  /**
   * 计算平均延迟
   */
  private calculateAverageLatency(): number {
    if (this.metrics.length === 0) return 0;

    const recentMetrics = this.metrics.slice(-10);
    const sum = recentMetrics.reduce((acc, m) => acc + m.latency, 0);
    return sum / recentMetrics.length;
  }

  /**
   * 获取消息队列大小
   */
  private getMessageQueueSize(): number {
    // 这里需要从WebSocket管理器获取队列大小
    return 0; // 占位符
  }

  /**
   * 计算错误率
   */
  private calculateErrorRate(): number {
    // 基于最近的性能数据计算错误率
    return 0; // 占位符
  }

  /**
   * 评估降级策略
   */
  private evaluateDegradation(): void {
    if (this.metrics.length === 0) return;

    const current = this.metrics[this.metrics.length - 1];
    let targetLevel = 0;

    // 从高到低检查降级条件
    for (let i = this.config.degradationSteps.length - 1; i >= 0; i--) {
      const step = this.config.degradationSteps[i];
      if (this.shouldDegradeToLevel(current, step)) {
        targetLevel = step.level;
        break;
      }
    }

    if (targetLevel !== this.currentLevel) {
      this.changeDegradationLevel(targetLevel);
    }
  }

  /**
   * 检查是否应该降级到指定级别
   */
  private shouldDegradeToLevel(metrics: PerformanceMetrics, step: DegradationStep): boolean {
    const conditions = step.conditions;

    if (conditions.cpu && metrics.cpu > conditions.cpu) return true;
    if (conditions.memory && metrics.memory > conditions.memory) return true;
    if (conditions.latency && metrics.latency > conditions.latency) return true;
    if (conditions.errorRate && metrics.errorRate > conditions.errorRate) return true;

    return false;
  }

  /**
   * 改变降级级别
   */
  private changeDegradationLevel(newLevel: number): void {
    const oldLevel = this.currentLevel;
    this.currentLevel = newLevel;

    const step = this.config.degradationSteps[newLevel];

    console.log(`🔄 [PerformanceMonitor] 降级级别变更: ${oldLevel} -> ${newLevel} (${step.name})`);
    console.log(`📋 [PerformanceMonitor] 降级动作:`, step.actions);

    // 执行降级动作
    step.actions.forEach(action => {
      this.emitEvent('degradation_action', action, newLevel);
    });

    this.emitEvent('level_changed', oldLevel, newLevel, step);
  }

  /**
   * 获取当前性能状态
   */
  getCurrentStatus(): {
    level: number;
    step: DegradationStep;
    metrics: PerformanceMetrics | null;
    health: 'good' | 'warning' | 'critical';
  } {
    const step = this.config.degradationSteps[this.currentLevel];
    const latest = this.metrics[this.metrics.length - 1];

    let health: 'good' | 'warning' | 'critical' = 'good';
    if (this.currentLevel >= 2) health = 'critical';
    else if (this.currentLevel >= 1) health = 'warning';

    return {
      level: this.currentLevel,
      step,
      metrics: latest || null,
      health
    };
  }

  /**
   * 添加事件监听器
   */
  on(event: string, callback: Function): void {
    if (!this.callbacks.has(event)) {
      this.callbacks.set(event, []);
    }
    this.callbacks.get(event)!.push(callback);
  }

  /**
   * 移除事件监听器
   */
  off(event: string, callback?: Function): void {
    const callbacks = this.callbacks.get(event);
    if (!callbacks) return;

    if (callback) {
      const index = callbacks.indexOf(callback);
      if (index > -1) {
        callbacks.splice(index, 1);
      }
    } else {
      callbacks.length = 0;
    }
  }

  /**
   * 触发事件
   */
  private emitEvent(event: string, ...args: any[]): void {
    const callbacks = this.callbacks.get(event);
    if (callbacks) {
      callbacks.forEach(callback => {
        try {
          callback(...args);
        } catch (error) {
          console.error(`❌ [PerformanceMonitor] 事件回调失败 [${event}]:`, error);
        }
      });
    }
  }

  /**
   * 获取性能历史数据
   */
  getMetricsHistory(): PerformanceMetrics[] {
    return [...this.metrics];
  }

  /**
   * 手动触发降级
   */
  forceDegradation(level: number): void {
    if (level >= 0 && level < this.config.degradationSteps.length) {
      console.log(`🚨 [PerformanceMonitor] 手动触发降级: level ${level}`);
      this.changeDegradationLevel(level);
    }
  }

  /**
   * 重置到正常模式
   */
  reset(): void {
    console.log('🔄 [PerformanceMonitor] 重置到正常模式');
    this.changeDegradationLevel(0);
    this.metrics = [];
  }

  /**
   * 销毁监控器
   */
  destroy(): void {
    this.stop();
    this.callbacks.clear();
    this.metrics = [];
    console.log('🔥 [PerformanceMonitor] 性能监控器已销毁');
  }
}

// 单例实例
let monitorInstance: PerformanceMonitor | null = null;

export function getPerformanceMonitor(): PerformanceMonitor {
  if (!monitorInstance) {
    monitorInstance = new PerformanceMonitor();
  }
  return monitorInstance;
}

export function destroyPerformanceMonitor(): void {
  if (monitorInstance) {
    monitorInstance.destroy();
    monitorInstance = null;
  }
}