/**
 * 高性能警情列表更新管理器
 * 支持200-500个并发端的实时列表更新
 *
 * 核心特性:
 * 1. 虚拟滚动支持
 * 2. 增量更新策略
 * 3. 智能批量处理
 * 4. 内存优化
 * 5. 防抖节流控制
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-26
 * @Copyright 1024创新实验室
 */

import { ref, shallowRef, computed, watchEffect, triggerRef } from 'vue';
import { debounce, throttle, chunk } from 'lodash-es';
import { getWebSocketClient } from './websocket-manager';
import { createBusinessMessage } from '/@/types/websocket';

/**
 * 警情数据接口
 */
export interface PoliceReportData {
  id: number; // 前端统一使用 id
  reportId: number; // 兼容后端的 reportId 字段
  reportNumber: string;
  reportType: number | string; // 兼容数值和字符串类型
  reportLevel: number | string;
  status: number | string;
  reporterName?: string;
  reporterPhone?: string;
  reporterIdCard?: string;
  incidentLocation?: string;
  description?: string; // 兼容后端的 description 字段
  incidentDescription?: string;
  reportTime?: Date | string;
  handlerName?: string;
  handlerId?: number;
  handleSeatCode?: string;
  handleResult?: string;
  handleTime?: Date | string;
  attachments?: string;
  remark?: string;
  createUserId?: number;
  createUserName?: string;
  createTime?: Date | string;
  updateTime?: Date | string;
  updatedAt: number; // 前端管理的更新时间戳
  version: number; // 版本号用于冲突检测
  updateFields?: Set<string>; // 更新的字段集合
}

/**
 * 更新消息接口
 */
export interface ListUpdateMessage {
  type: 'INSERT' | 'UPDATE' | 'DELETE' | 'BATCH';
  reportId?: number;
  data?: Partial<PoliceReportData>;
  reportIds?: number[];
  batchData?: Array<{
    reportId: number;
    data: Partial<PoliceReportData>;
  }>;
  timestamp: number;
  userId: string;
  userName: string;
}

/**
 * 性能配置
 */
interface PerformanceConfig {
  virtualScrollEnabled: boolean;
  pageSize: number;
  bufferSize: number;
  updateBatchSize: number;
  updateDebounceMs: number;
  renderThrottleMs: number;
  maxCachedRows: number;
  compressionEnabled: boolean;
}

/**
 * 列表更新管理器
 */
export class PoliceListUpdateManager {
  // 数据存储
  private dataMap = new Map<number, PoliceReportData>();
  private visibleData = shallowRef<PoliceReportData[]>([]);
  private sortedIds = ref<number[]>([]);

  // 虚拟滚动相关
  private scrollTop = ref(0);
  private containerHeight = ref(800);
  private rowHeight = 50;
  private visibleRange = computed(() => {
    const start = Math.floor(this.scrollTop.value / this.rowHeight);
    const end = Math.ceil((this.scrollTop.value + this.containerHeight.value) / this.rowHeight);
    return { start, end };
  });

  // 更新队列和缓冲
  private updateQueue: ListUpdateMessage[] = [];
  private pendingUpdates = new Map<number, Partial<PoliceReportData>>();
  private updateTimer: NodeJS.Timeout | null = null;

  // 性能监控
  private stats = {
    totalUpdates: 0,
    batchedUpdates: 0,
    droppedUpdates: 0,
    renderCount: 0,
    lastUpdateTime: 0,
    avgUpdateTime: 0,
    memoryUsage: 0
  };

  // 高并发优化配置（支持200-500终端）
  private config: PerformanceConfig = {
    virtualScrollEnabled: true,
    pageSize: 100, // 增加页面大小
    bufferSize: 20, // 增加缓冲区
    updateBatchSize: 50, // 增大批量处理
    updateDebounceMs: 50, // 降低延迟
    renderThrottleMs: 32, // 30fps，降低CPU使用
    maxCachedRows: 2000, // 增大缓存
    compressionEnabled: true
  };

  // 高并发性能监控
  private performanceMetrics = {
    totalMessages: 0,
    droppedMessages: 0,
    avgProcessingTime: 0,
    lastCleanupTime: Date.now(),
    memoryPressure: false,
    cpuPressure: false
  };

  // 渲染优化
  private renderThrottled: Function;
  private updateDebounced: Function;

  // WebSocket连接
  private wsClient: ReturnType<typeof getWebSocketClient> | null = null;

  // 轮询定时器
  private pollingTimer: NodeJS.Timeout | null = null;

  // WebSocket重连定时器
  private reconnectTimer: NodeJS.Timeout | null = null;

  // 客户端数据版本号
  private clientDataVersion: string | null = null;

  constructor(config?: Partial<PerformanceConfig>) {
    if (config) {
      this.config = { ...this.config, ...config };
    }

    // 初始化限流/防抖函数
    this.renderThrottled = throttle(this.performRender.bind(this), this.config.renderThrottleMs);
    this.updateDebounced = debounce(this.processUpdateQueue.bind(this), this.config.updateDebounceMs);

    // 初始化WebSocket连接
    this.initializeWebSocket();

    // 设置自动清理
    this.startMemoryCleanup();
  }

  /**
   * 初始化WebSocket连接
   */
  private async initializeWebSocket() {
    try {
      this.wsClient = getWebSocketClient();

      if (this.wsClient && this.wsClient.isConnected) {
        // 监听现有的警务模块消息，而不是创建新的订阅
        this.wsClient.onModuleMessage('police', 'LIST_UPDATE', this.handleWebSocketMessage.bind(this));
        this.wsClient.onModuleMessage('police', 'BATCH_UPDATE', this.handleBatchWebSocketMessage.bind(this));

        console.log('🚀 [ListUpdateManager] 已集成到现有WebSocket系统');
      } else {
        console.warn('⚠️ [ListUpdateManager] WebSocket客户端未连接，使用降级模式');
        this.initializePollingMode();
      }
    } catch (error) {
      console.error('❌ [ListUpdateManager] WebSocket初始化失败:', error);
      this.initializePollingMode();
    }
  }

  /**
   * 初始化轮询模式（降级方案）- 智能轮询，减少服务器压力
   */
  private initializePollingMode() {
    console.log('🔄 [ListUpdateManager] 启动智能轮询降级方案');

    // 在任何环境下都应该避免无脑轮询，改为智能检测
    console.log('🧠 [ListUpdateManager] 使用智能降级策略，避免定时轮询');

    // ⚡ 新策略：不使用定时轮询，而是基于用户行为触发检查
    this.initializeSmartFallback();
  }

  /**
   * 智能降级策略 - 替代无脑轮询
   */
  private initializeSmartFallback() {
    console.log('🎯 [ListUpdateManager] 启动智能降级策略');

    // 1. 监听用户活动，活跃时才检查更新
    let lastActivityTime = Date.now();
    let activityCheckTimer: NodeJS.Timeout | null = null;

    const updateLastActivity = () => {
      lastActivityTime = Date.now();
    };

    // 监听用户活动事件
    ['click', 'keydown', 'scroll', 'mousemove'].forEach(event => {
      document.addEventListener(event, updateLastActivity, { passive: true });
    });

    // 2. 定期检查用户是否活跃，只有活跃用户才进行数据检查
    activityCheckTimer = setInterval(() => {
      const timeSinceLastActivity = Date.now() - lastActivityTime;

      // 如果用户超过2分钟无活动，暂停检查
      if (timeSinceLastActivity > 120000) {
        console.log('😴 [ListUpdateManager] 用户无活动，暂停数据检查');
        return;
      }

      // 如果页面不可见，也暂停检查
      if (document.hidden) {
        console.log('🔇 [ListUpdateManager] 页面不可见，暂停检查');
        return;
      }

      // 3. 使用轻量级心跳检查替代全量查询
      this.performLightweightCheck();
    }, 60000); // 1分钟检查一次用户活跃度

    // 保存定时器用于清理
    this.pollingTimer = activityCheckTimer;

    // 4. WebSocket重连机制
    this.setupWebSocketReconnection();

    console.log('✅ [ListUpdateManager] 智能降级策略已启动');
  }

  /**
   * 轻量级数据检查 - 替代全量查询
   */
  private async performLightweightCheck() {
    try {
      console.log('🔍 [ListUpdateManager] 执行轻量级数据检查');

      // 导入API
      const { policeReportApi } = await import('/@/api/business/oa/police-report-api');

      // 获取当前客户端版本（如果有的话）
      const currentVersion = this.clientDataVersion || null;

      // 调用轻量级版本检查API
      const response = await policeReportApi.checkDataVersion(currentVersion);

      if (response && response.data) {
        const { version, hasUpdates, lastUpdateTime } = response.data;

        console.log('💡 [ListUpdateManager] 版本检查结果:', {
          clientVersion: currentVersion,
          serverVersion: version,
          hasUpdates,
          lastUpdateTime
        });

        // 更新客户端版本
        this.clientDataVersion = version;

        // 如果有更新，触发数据刷新
        if (hasUpdates) {
          console.log('🔄 [ListUpdateManager] 检测到数据更新，触发刷新');
          this.emitEvent('data_refresh_required');
        } else {
          console.log('✅ [ListUpdateManager] 数据无变化，跳过刷新');
        }
      } else {
        console.warn('⚠️ [ListUpdateManager] 版本检查响应异常');
      }

    } catch (error) {
      console.warn('⚠️ [ListUpdateManager] 轻量级检查失败:', error);
      // 检查失败时，为了安全起见，触发一次刷新
      this.emitEvent('data_refresh_required');
    }
  }

  /**
   * WebSocket重连机制
   */
  private setupWebSocketReconnection() {
    // 定期尝试重新连接WebSocket
    const reconnectTimer = setInterval(() => {
      if (!this.wsClient || !this.wsClient.isConnected) {
        console.log('🔄 [ListUpdateManager] 尝试重新连接WebSocket');
        this.initializeWebSocket();
      }
    }, 30000); // 30秒尝试重连一次

    // 保存重连定时器
    this.reconnectTimer = reconnectTimer;
  }

  /**
   * 处理WebSocket消息（高并发优化版本）
   */
  private handleWebSocketMessage(message: any) {
    if (!message || !message.data) return;

    const startTime = performance.now();
    this.performanceMetrics.totalMessages++;

    try {
      // 内存压力检测
      if (this.updateQueue.length > 500) {
        this.performanceMetrics.memoryPressure = true;
        this.performanceMetrics.droppedMessages++;
        console.warn('🚨 [内存压力] 丢弃消息，队列长度:', this.updateQueue.length);
        return;
      }

      // CPU压力检测（基于平均处理时间）
      if (this.performanceMetrics.avgProcessingTime > 10) {
        this.performanceMetrics.cpuPressure = true;
        // 降低处理频率
        if (Math.random() > 0.5) {
          this.performanceMetrics.droppedMessages++;
          console.warn('🔥 [CPU压力] 随机丢弃消息，平均处理时间:', this.performanceMetrics.avgProcessingTime);
          return;
        }
      }

      // 修复：使用message.data，因为业务数据在data字段中
      const updateMessage = message.data as ListUpdateMessage;
      console.log('📨 [ListUpdateManager] 收到WebSocket消息:', updateMessage.type, '数据:', updateMessage);

      // 添加到更新队列
      this.enqueueUpdate(updateMessage);

      // 更新性能指标
      const processTime = performance.now() - startTime;
      this.performanceMetrics.avgProcessingTime =
        (this.performanceMetrics.avgProcessingTime + processTime) / 2;

    } catch (error) {
      console.error('❌ [ListUpdateManager] 处理WebSocket消息失败:', error);
      this.performanceMetrics.droppedMessages++;
    }
  }

  /**
   * 处理批量WebSocket消息
   */
  private handleBatchWebSocketMessage(message: any) {
    if (!message || !message.data || !message.data.updates) return;

    try {
      // 修复：使用message.data，因为业务数据在data字段中
      const batchMessage: ListUpdateMessage = {
        type: 'BATCH',
        batchData: message.data.updates,
        timestamp: message.data.timestamp || Date.now(),
        userId: message.data.userId || 'unknown',
        userName: message.data.userName || '系统'
      };

      console.log('📦 [ListUpdateManager] 收到批量WebSocket消息:', batchMessage.batchData?.length, '个更新');

      // 添加到更新队列
      this.enqueueUpdate(batchMessage);
    } catch (error) {
      console.error('❌ [ListUpdateManager] 处理批量WebSocket消息失败:', error);
    }
  }

  /**
   * 事件发射器
   */
  private eventListeners = new Map<string, Function[]>();

  private emitEvent(eventName: string, ...args: any[]) {
    const listeners = this.eventListeners.get(eventName);
    if (listeners) {
      listeners.forEach(listener => {
        try {
          listener(...args);
        } catch (error) {
          console.error(`❌ [ListUpdateManager] 事件监听器执行失败 [${eventName}]:`, error);
        }
      });
    }
  }

  /**
   * 添加事件监听器
   */
  public on(eventName: string, listener: Function) {
    if (!this.eventListeners.has(eventName)) {
      this.eventListeners.set(eventName, []);
    }
    this.eventListeners.get(eventName)!.push(listener);
  }

  /**
   * 移除事件监听器
   */
  public off(eventName: string, listener?: Function) {
    if (!listener) {
      this.eventListeners.delete(eventName);
      return;
    }

    const listeners = this.eventListeners.get(eventName);
    if (listeners) {
      const index = listeners.indexOf(listener);
      if (index !== -1) {
        listeners.splice(index, 1);
      }
    }
  }

  /**
   * 加入更新队列
   */
  private enqueueUpdate(update: ListUpdateMessage) {
    // 检查队列大小，防止内存溢出
    if (this.updateQueue.length > 1000) {
      console.warn('[ListUpdateManager] 更新队列过大，丢弃旧消息');
      this.updateQueue = this.updateQueue.slice(-500);
      this.stats.droppedUpdates++;
    }

    this.updateQueue.push(update);
    this.stats.totalUpdates++;

    // 触发批量更新
    this.updateDebounced();
  }

  /**
   * 处理更新队列（批量）
   */
  private processUpdateQueue() {
    if (this.updateQueue.length === 0) return;

    const startTime = performance.now();

    // 按类型分组处理
    const updates = this.updateQueue.splice(0, this.config.updateBatchSize);
    const updateGroups = new Map<string, ListUpdateMessage[]>();

    updates.forEach(update => {
      const key = update.type;
      if (!updateGroups.has(key)) {
        updateGroups.set(key, []);
      }
      updateGroups.get(key)!.push(update);
    });

    // 批量处理每种类型的更新
    updateGroups.forEach((groupUpdates, type) => {
      switch (type) {
        case 'UPDATE':
          this.processBatchUpdates(groupUpdates);
          break;
        case 'INSERT':
          this.processBatchInserts(groupUpdates);
          break;
        case 'DELETE':
          this.processBatchDeletes(groupUpdates);
          break;
        case 'BATCH':
          this.processComplexBatch(groupUpdates);
          break;
      }
    });

    // 触发渲染
    this.renderThrottled();

    // 更新统计
    const updateTime = performance.now() - startTime;
    this.stats.avgUpdateTime = (this.stats.avgUpdateTime + updateTime) / 2;
    this.stats.batchedUpdates++;

    // 如果还有待处理的更新，继续处理
    if (this.updateQueue.length > 0) {
      this.updateDebounced();
    }
  }

  /**
   * 批量处理更新
   */
  private processBatchUpdates(updates: ListUpdateMessage[]) {
    updates.forEach(update => {
      if (!update.reportId || !update.data) return;

      const existing = this.dataMap.get(update.reportId);
      if (existing) {
        // 合并更新
        const merged = this.mergeReportData(existing, update.data);
        merged.updatedAt = update.timestamp;
        merged.version++;

        // 标记更新字段
        if (!merged.updateFields) {
          merged.updateFields = new Set();
        }
        Object.keys(update.data).forEach(key => merged.updateFields!.add(key));

        this.dataMap.set(update.reportId, merged);
        this.pendingUpdates.set(update.reportId, update.data);
      }
    });
  }

  /**
   * 批量处理插入
   */
  private processBatchInserts(inserts: ListUpdateMessage[]) {
    inserts.forEach(insert => {
      if (!insert.reportId || !insert.data) return;

      const newReport: PoliceReportData = {
        id: insert.reportId,
        reportNumber: '',
        reportType: '',
        reportLevel: '',
        status: '',
        updatedAt: insert.timestamp,
        version: 1,
        ...insert.data
      };

      this.dataMap.set(insert.reportId, newReport);
      this.sortedIds.value.push(insert.reportId);
    });

    // 重新排序
    this.resortIds();
  }

  /**
   * 批量处理删除
   */
  private processBatchDeletes(deletes: ListUpdateMessage[]) {
    deletes.forEach(del => {
      if (!del.reportId) return;

      this.dataMap.delete(del.reportId);
      const index = this.sortedIds.value.indexOf(del.reportId);
      if (index > -1) {
        this.sortedIds.value.splice(index, 1);
      }
    });
  }

  /**
   * 处理复杂批量更新
   */
  private processComplexBatch(batches: ListUpdateMessage[]) {
    batches.forEach(batch => {
      if (!batch.batchData) return;

      batch.batchData.forEach(item => {
        const existing = this.dataMap.get(item.reportId);
        if (existing) {
          const merged = this.mergeReportData(existing, item.data);
          merged.updatedAt = batch.timestamp;
          merged.version++;
          this.dataMap.set(item.reportId, merged);
        }
      });
    });
  }

  /**
   * 合并报告数据
   */
  private mergeReportData(
    existing: PoliceReportData,
    update: Partial<PoliceReportData>
  ): PoliceReportData {
    return {
      ...existing,
      ...update,
      // 保留原始ID和版本控制字段
      id: existing.id,
      version: existing.version
    };
  }

  /**
   * 执行渲染
   */
  private performRender() {
    const startTime = performance.now();

    if (this.config.virtualScrollEnabled) {
      // 虚拟滚动渲染
      this.renderVisibleRows();
    } else {
      // 全量渲染
      this.renderAllRows();
    }

    // 清除更新标记
    this.clearUpdateMarks();

    this.stats.renderCount++;
    this.stats.lastUpdateTime = performance.now() - startTime;
  }

  /**
   * 渲染可见行（虚拟滚动）
   */
  private renderVisibleRows() {
    const { start, end } = this.visibleRange.value;
    const buffer = this.config.bufferSize;

    const startIndex = Math.max(0, start - buffer);
    const endIndex = Math.min(this.sortedIds.value.length, end + buffer);

    const visibleIds = this.sortedIds.value.slice(startIndex, endIndex);
    const visibleReports: PoliceReportData[] = [];

    visibleIds.forEach(id => {
      const report = this.dataMap.get(id);
      if (report) {
        visibleReports.push(report);
      }
    });

    this.visibleData.value = visibleReports;
    triggerRef(this.visibleData);
  }

  /**
   * 渲染所有行
   */
  private renderAllRows() {
    const allReports: PoliceReportData[] = [];

    this.sortedIds.value.forEach(id => {
      const report = this.dataMap.get(id);
      if (report) {
        allReports.push(report);
      }
    });

    this.visibleData.value = allReports;
    triggerRef(this.visibleData);
  }

  /**
   * 清除更新标记
   */
  private clearUpdateMarks() {
    setTimeout(() => {
      this.pendingUpdates.clear();
      this.dataMap.forEach(report => {
        if (report.updateFields) {
          report.updateFields.clear();
        }
      });
    }, 2000); // 2秒后清除高亮
  }

  /**
   * 重新排序ID列表
   */
  private resortIds() {
    this.sortedIds.value.sort((a, b) => {
      const reportA = this.dataMap.get(a);
      const reportB = this.dataMap.get(b);

      if (!reportA || !reportB) return 0;

      // 按更新时间降序排列
      return reportB.updatedAt - reportA.updatedAt;
    });
  }

  /**
   * 设置滚动位置
   */
  public setScrollPosition(scrollTop: number, containerHeight: number) {
    this.scrollTop.value = scrollTop;
    this.containerHeight.value = containerHeight;

    if (this.config.virtualScrollEnabled) {
      this.renderThrottled();
    }
  }

  /**
   * 获取可见数据
   */
  public getVisibleData() {
    return this.visibleData;
  }

  /**
   * 获取总行数
   */
  public getTotalRows() {
    return this.sortedIds.value.length;
  }

  /**
   * 获取总高度（虚拟滚动）
   */
  public getTotalHeight() {
    return this.sortedIds.value.length * this.rowHeight;
  }

  /**
   * 获取性能统计
   */
  public getStats() {
    return {
      ...this.stats,
      dataSize: this.dataMap.size,
      queueSize: this.updateQueue.length,
      pendingUpdates: this.pendingUpdates.size
    };
  }

  /**
   * 初始化数据
   */
  public initializeData(reports: PoliceReportData[]) {
    this.dataMap.clear();
    this.sortedIds.value = [];

    reports.forEach(report => {
      this.dataMap.set(report.id, report);
      this.sortedIds.value.push(report.id);
    });

    this.resortIds();
    this.renderThrottled();
  }

  /**
   * 手动触发更新
   */
  public manualUpdate(update: ListUpdateMessage) {
    this.enqueueUpdate(update);
  }

  /**
   * 内存清理
   */
  private startMemoryCleanup() {
    setInterval(() => {
      // 清理过期的更新标记
      this.dataMap.forEach(report => {
        if (report.updateFields && report.updateFields.size > 0) {
          const age = Date.now() - report.updatedAt;
          if (age > 60000) { // 1分钟
            report.updateFields.clear();
          }
        }
      });

      // 限制缓存大小
      if (this.dataMap.size > this.config.maxCachedRows) {
        const toDelete = this.dataMap.size - this.config.maxCachedRows;
        const oldestIds = [...this.sortedIds.value].slice(-toDelete);

        oldestIds.forEach(id => {
          this.dataMap.delete(id);
          const index = this.sortedIds.value.indexOf(id);
          if (index > -1) {
            this.sortedIds.value.splice(index, 1);
          }
        });
      }

      // 更新内存使用统计
      this.stats.memoryUsage = this.dataMap.size;
    }, 30000); // 30秒清理一次
  }

  /**
   * 销毁
   */
  public destroy() {
    if (this.wsClient && this.wsClient.offModuleMessage) {
      // 取消模块消息监听
      this.wsClient.offModuleMessage('police', 'LIST_UPDATE');
      this.wsClient.offModuleMessage('police', 'BATCH_UPDATE');
    }

    this.dataMap.clear();
    this.updateQueue = [];
    this.pendingUpdates.clear();

    if (this.updateTimer) {
      clearTimeout(this.updateTimer);
    }

    if (this.performanceTimer) {
      clearInterval(this.performanceTimer);
    }

    if (this.memoryCleanupTimer) {
      clearInterval(this.memoryCleanupTimer);
    }

    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
      this.pollingTimer = null;
      console.log('🚫 [PoliceListUpdateManager] 轮询定时器已清理');
    }

    if (this.reconnectTimer) {
      clearInterval(this.reconnectTimer);
      this.reconnectTimer = null;
      console.log('🚫 [PoliceListUpdateManager] 重连定时器已清理');
    }

    // 清理事件监听器
    ['click', 'keydown', 'scroll', 'mousemove'].forEach(event => {
      document.removeEventListener(event, () => {}, { passive: true } as any);
    });

    console.log('📝 [PoliceListUpdateManager] 高性能更新管理器已销毁');
  }
}

// 导出单例
export const policeListUpdateManager = new PoliceListUpdateManager();
export default policeListUpdateManager;