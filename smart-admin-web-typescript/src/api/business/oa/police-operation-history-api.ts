/**
 * 警情操作历史API - 高性能版本
 *
 * 设计原则:
 * 1. 非阻塞加载
 * 2. 错误降级
 * 3. 缓存优先
 * 4. 最小化网络请求
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-28
 * @Copyright 1024创新实验室
 */

import { postRequest, getRequest } from '/@/lib/axios';

export interface OperationHistoryQuery {
  pageNum?: number;
  pageSize?: number;
  operationType?: string;
  userName?: string;
}

export interface OperationHistoryResult {
  list: any[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export const policeOperationHistoryApi = {

  /**
   * 获取操作历史 - 带超时和错误处理
   */
  async getOperationHistory(reportId: string | number, query: OperationHistoryQuery = {}): Promise<{
    data: OperationHistoryResult;
    success: boolean;
  }> {
    try {
      const { pageNum = 1, pageSize = 10, operationType, userName } = query;

      const params = new URLSearchParams();
      params.append('pageNum', String(pageNum));
      params.append('pageSize', String(Math.min(pageSize, 50))); // 限制最大页面大小

      if (operationType) params.append('operationType', operationType);
      if (userName) params.append('userName', userName);

      console.log('📖 [API] 获取操作历史:', { reportId, query });

      // 设置较短的超时时间，避免阻塞主界面
      const response = await getRequest(
        `/oa/police/operation-history/${reportId}?${params}`,
        undefined,
        { timeout: 3000 } // 3秒超时
      );

      console.log('✅ [API] 操作历史获取成功:', response.data);
      return response;
    } catch (error) {
      console.warn('⚠️ [API] 操作历史获取失败:', error);

      // 返回空结果而不是抛出异常
      return {
        data: {
          list: [],
          total: 0,
          pageNum: query.pageNum || 1,
          pageSize: query.pageSize || 10
        },
        success: false
      };
    }
  },

  /**
   * 火焰式记录操作 - 立即返回，不等待结果
   */
  async recordOperation(reportId: string | number, operation: string, details?: any): Promise<void> {
    try {
      console.log('🔥 [API] 记录操作历史:', { reportId, operation });

      // 使用Promise.resolve()立即返回，不等待网络请求完成
      Promise.resolve().then(async () => {
        try {
          await postRequest(`/oa/police/operation-history/${reportId}/record`, details, {
            params: { operation },
            timeout: 1000 // 1秒超时
          });
          console.log('✅ [API] 操作历史记录成功');
        } catch (error) {
          console.warn('⚠️ [API] 操作历史记录失败（不影响业务）:', error);
        }
      });

    } catch (error) {
      console.warn('⚠️ [API] 操作历史记录提交失败:', error);
      // 不抛出异常，不影响主业务
    }
  },

  /**
   * 预热缓存 - 页面加载时异步调用
   */
  async preloadCache(reportId: string | number): Promise<void> {
    try {
      console.log('🔄 [API] 预热操作历史缓存:', reportId);

      // 异步预热，不等待结果
      Promise.resolve().then(async () => {
        try {
          await postRequest(`/oa/police/operation-history/${reportId}/preload`, {}, {
            timeout: 500 // 500ms超时
          });
          console.log('✅ [API] 缓存预热完成');
        } catch (error) {
          console.warn('⚠️ [API] 缓存预热失败:', error);
        }
      });

    } catch (error) {
      console.warn('⚠️ [API] 缓存预热提交失败:', error);
    }
  },

  /**
   * 清理缓存 - 页面卸载时调用（简化版本，只记录日志）
   */
  async clearCache(reportId: string | number): Promise<void> {
    try {
      console.log('🗑️ [API] 清理操作历史缓存（模拟）:', reportId);

      // 由于缓存有TTL自动过期，这里只做日志记录
      // 实际的清理由Redis的TTL机制自动处理
      console.log('✅ [API] 缓存将由TTL自动清理');

    } catch (error) {
      console.warn('⚠️ [API] 缓存清理记录失败:', error);
    }
  },

  /**
   * 健康检查
   */
  async healthCheck(): Promise<boolean> {
    try {
      const response = await getRequest('/oa/police/operation-history/health', undefined, {
        timeout: 1000 // 1秒超时
      });
      return response.success;
    } catch (error) {
      console.warn('⚠️ [API] 操作历史服务健康检查失败:', error);
      return false;
    }
  },

  /**
   * 批量记录操作 - 用于系统初始化时批量导入历史数据
   */
  async recordOperationsBatch(reportId: string | number, operations: any[]): Promise<void> {
    try {
      if (!operations || operations.length === 0) {
        return;
      }

      console.log('📦 [API] 批量记录操作历史:', { reportId, count: operations.length });

      // 分批处理，每批最多50条
      const batchSize = 50;
      for (let i = 0; i < operations.length; i += batchSize) {
        const batch = operations.slice(i, i + batchSize);

        // 异步处理每一批，不等待结果
        Promise.resolve().then(async () => {
          try {
            await postRequest(`/oa/police/operation-history/${reportId}/batch`, batch, {
              timeout: 2000 // 2秒超时
            });
            console.log(`✅ [API] 批量操作历史记录成功: ${i + 1}-${Math.min(i + batchSize, operations.length)}`);
          } catch (error) {
            console.warn(`⚠️ [API] 批量操作历史记录失败: ${i + 1}-${Math.min(i + batchSize, operations.length)}:`, error);
          }
        });
      }

    } catch (error) {
      console.warn('⚠️ [API] 批量操作历史记录提交失败:', error);
    }
  }
};

export default policeOperationHistoryApi;