/*
 * 协作历史记录API
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-28
 * @Copyright 1024创新实验室
 */
import { postRequest, getRequest } from '/@/lib/axios';

export const collaborationHistoryApi = {
  // 查询协作历史记录
  queryHistory: (param) => {
    return getRequest('/api/collaboration/history/query', param);
  },

  // 获取操作统计信息
  getStatistics: (param) => {
    return postRequest('/api/collaboration/history/statistics', param);
  },

  // 导出操作历史
  exportHistory: (param, format = 'excel') => {
    return postRequest(`/api/collaboration/history/export?format=${format}`, param);
  },

  // 检测异常操作
  detectAnomalies: (param) => {
    return postRequest('/api/collaboration/history/detect-anomalies', param);
  }
};