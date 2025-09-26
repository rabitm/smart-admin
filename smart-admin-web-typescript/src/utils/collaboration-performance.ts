/**
 * 协作系统性能监控工具
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-24
 * @Copyright 1024创新实验室
 */

interface PerformanceMetrics {
  fps: number;
  memory: number;
  cursors: number;
  collaborators: number;
  renderTime: number;
  updateFrequency: number;
  networkLatency: number;
}

interface PerformanceConfig {
  maxCursors: number;
  fpsThreshold: number;
  memoryThreshold: number;
  renderTimeThreshold: number;
  enableDebugMode: boolean;
}

class CollaborationPerformanceMonitor {
  private metrics: PerformanceMetrics = {
    fps: 60,
    memory: 0,
    cursors: 0,
    collaborators: 0,
    renderTime: 0,
    updateFrequency: 0,
    networkLatency: 0
  };

  private config: PerformanceConfig = {
    maxCursors: 50,
    fpsThreshold: 30,
    memoryThreshold: 100 * 1024 * 1024, // 100MB
    renderTimeThreshold: 16, // 16ms for 60fps
    enableDebugMode: process.env.NODE_ENV === 'development'
  };

  private observers: Array<(metrics: PerformanceMetrics) => void> = [];
  private frameCount = 0;
  private lastFpsUpdate = Date.now();
  private renderTimeHistory: number[] = [];
  private updateTimeHistory: number[] = [];
  private animationFrameId: number | null = null;

  constructor(config?: Partial<PerformanceConfig>) {
    if (config) {
      this.config = { ...this.config, ...config };
    }
    this.startMonitoring();
  }

  /**
   * 开始性能监控
   */
  startMonitoring(): void {
    if (this.animationFrameId !== null) return;

    const monitor = () => {
      this.updateFPS();
      this.updateMemoryUsage();
      this.notifyObservers();
      this.animationFrameId = requestAnimationFrame(monitor);
    };

    this.animationFrameId = requestAnimationFrame(monitor);
  }

  /**
   * 停止性能监控
   */
  stopMonitoring(): void {
    if (this.animationFrameId !== null) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = null;
    }
  }

  /**
   * 更新FPS
   */
  private updateFPS(): void {
    const now = Date.now();
    this.frameCount++;

    if (now - this.lastFpsUpdate >= 1000) {
      this.metrics.fps = this.frameCount;
      this.frameCount = 0;
      this.lastFpsUpdate = now;
    }
  }

  /**
   * 更新内存使用情况
   */
  private updateMemoryUsage(): void {
    if ('memory' in performance) {
      const memInfo = (performance as any).memory;
      this.metrics.memory = memInfo.usedJSHeapSize;
    }
  }

  /**
   * 记录渲染时间
   */
  recordRenderTime(renderTime: number): void {
    this.metrics.renderTime = renderTime;
    this.renderTimeHistory.push(renderTime);

    // 保持历史记录在合理范围内
    if (this.renderTimeHistory.length > 60) {
      this.renderTimeHistory.shift();
    }
  }

  /**
   * 记录更新频率
   */
  recordUpdate(): void {
    const now = Date.now();
    this.updateTimeHistory.push(now);

    // 计算最近1秒内的更新次数
    const recentUpdates = this.updateTimeHistory.filter(time => now - time <= 1000);
    this.metrics.updateFrequency = recentUpdates.length;

    // 清理旧记录
    this.updateTimeHistory = recentUpdates;
  }

  /**
   * 更新光标数量
   */
  updateCursorCount(count: number): void {
    this.metrics.cursors = count;
  }

  /**
   * 更新协作者数量
   */
  updateCollaboratorCount(count: number): void {
    this.metrics.collaborators = count;
  }

  /**
   * 记录网络延迟
   */
  recordNetworkLatency(latency: number): void {
    this.metrics.networkLatency = latency;
  }

  /**
   * 获取当前性能指标
   */
  getMetrics(): PerformanceMetrics {
    return { ...this.metrics };
  }

  /**
   * 获取性能建议
   */
  getPerformanceRecommendations(): string[] {
    const recommendations: string[] = [];

    if (this.metrics.fps < this.config.fpsThreshold) {
      recommendations.push(`FPS过低 (${this.metrics.fps})，建议减少光标数量或优化渲染逻辑`);
    }

    if (this.metrics.memory > this.config.memoryThreshold) {
      recommendations.push(`内存使用过高 (${Math.round(this.metrics.memory / 1024 / 1024)}MB)，建议清理缓存`);
    }

    if (this.metrics.cursors > this.config.maxCursors) {
      recommendations.push(`光标数量过多 (${this.metrics.cursors})，建议启用虚拟化渲染`);
    }

    if (this.metrics.renderTime > this.config.renderTimeThreshold) {
      recommendations.push(`渲染时间过长 (${this.metrics.renderTime}ms)，建议优化DOM操作`);
    }

    if (this.metrics.updateFrequency > 30) {
      recommendations.push(`更新频率过高 (${this.metrics.updateFrequency}/s)，建议添加防抖或节流`);
    }

    if (this.metrics.networkLatency > 200) {
      recommendations.push(`网络延迟较高 (${this.metrics.networkLatency}ms)，可能影响实时协作体验`);
    }

    return recommendations;
  }

  /**
   * 获取平均渲染时间
   */
  getAverageRenderTime(): number {
    if (this.renderTimeHistory.length === 0) return 0;
    return this.renderTimeHistory.reduce((sum, time) => sum + time, 0) / this.renderTimeHistory.length;
  }

  /**
   * 检查是否需要性能优化
   */
  needsOptimization(): boolean {
    return (
      this.metrics.fps < this.config.fpsThreshold ||
      this.metrics.memory > this.config.memoryThreshold ||
      this.metrics.cursors > this.config.maxCursors ||
      this.metrics.renderTime > this.config.renderTimeThreshold
    );
  }

  /**
   * 获取性能等级
   */
  getPerformanceGrade(): 'excellent' | 'good' | 'fair' | 'poor' {
    const score = this.calculatePerformanceScore();

    if (score >= 90) return 'excellent';
    if (score >= 70) return 'good';
    if (score >= 50) return 'fair';
    return 'poor';
  }

  /**
   * 计算性能分数 (0-100)
   */
  private calculatePerformanceScore(): number {
    let score = 100;

    // FPS权重: 30%
    const fpsScore = Math.min(this.metrics.fps / 60, 1) * 30;
    score = fpsScore;

    // 内存使用权重: 20%
    const memoryScore = Math.max(0, 1 - (this.metrics.memory / this.config.memoryThreshold)) * 20;
    score += memoryScore;

    // 渲染时间权重: 25%
    const renderScore = Math.max(0, 1 - (this.metrics.renderTime / this.config.renderTimeThreshold)) * 25;
    score += renderScore;

    // 光标数量权重: 15%
    const cursorScore = Math.max(0, 1 - (this.metrics.cursors / this.config.maxCursors)) * 15;
    score += cursorScore;

    // 网络延迟权重: 10%
    const networkScore = Math.max(0, 1 - (this.metrics.networkLatency / 500)) * 10;
    score += networkScore;

    return Math.round(score);
  }

  /**
   * 订阅性能指标变化
   */
  subscribe(callback: (metrics: PerformanceMetrics) => void): () => void {
    this.observers.push(callback);

    // 返回取消订阅函数
    return () => {
      const index = this.observers.indexOf(callback);
      if (index > -1) {
        this.observers.splice(index, 1);
      }
    };
  }

  /**
   * 通知观察者
   */
  private notifyObservers(): void {
    this.observers.forEach(callback => {
      try {
        callback(this.metrics);
      } catch (error) {
        console.error('Performance observer error:', error);
      }
    });
  }

  /**
   * 导出性能报告
   */
  exportReport(): any {
    return {
      timestamp: new Date().toISOString(),
      metrics: this.getMetrics(),
      recommendations: this.getPerformanceRecommendations(),
      grade: this.getPerformanceGrade(),
      score: this.calculatePerformanceScore(),
      averageRenderTime: this.getAverageRenderTime(),
      config: this.config,
      historyLength: {
        renderTime: this.renderTimeHistory.length,
        updates: this.updateTimeHistory.length
      }
    };
  }

  /**
   * 清理历史数据
   */
  clearHistory(): void {
    this.renderTimeHistory = [];
    this.updateTimeHistory = [];
  }

  /**
   * 更新配置
   */
  updateConfig(config: Partial<PerformanceConfig>): void {
    this.config = { ...this.config, ...config };
  }

  /**
   * 销毁监控器
   */
  destroy(): void {
    this.stopMonitoring();
    this.observers = [];
    this.clearHistory();
  }
}

// 创建默认实例
export const performanceMonitor = new CollaborationPerformanceMonitor();

// 导出工具函数
export const measureRenderTime = <T>(fn: () => T): [T, number] => {
  const start = performance.now();
  const result = fn();
  const renderTime = performance.now() - start;
  performanceMonitor.recordRenderTime(renderTime);
  return [result, renderTime];
};

export const measureAsyncRenderTime = async <T>(fn: () => Promise<T>): Promise<[T, number]> => {
  const start = performance.now();
  const result = await fn();
  const renderTime = performance.now() - start;
  performanceMonitor.recordRenderTime(renderTime);
  return [result, renderTime];
};

export default CollaborationPerformanceMonitor;