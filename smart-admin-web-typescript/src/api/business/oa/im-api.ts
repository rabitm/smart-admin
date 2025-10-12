/**
 * IM即时聊天 API
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright 1024创新实验室
 */

import { getRequest, postRequest } from '/@/lib/axios';

export const imApi = {
  /**
   * 获取IM配置信息
   */
  getConfig: () => {
    return getRequest('/api/im/config/info');
  },

  /**
   * 检查IM服务健康状态
   */
  checkHealth: () => {
    return getRequest('/api/im/config/health');
  },

  /**
   * 获取Token信息
   */
  getTokenInfo: () => {
    return getRequest('/api/im/config/token-info');
  },

  /**
   * 刷新Token
   */
  refreshToken: () => {
    return postRequest('/api/im/config/token-refresh');
  },

  /**
   * 根据员工ID获取OpenIM用户ID
   */
  getOpenIMUserId: (employeeId: number) => {
    return getRequest(`/api/im/user/openim-id/${employeeId}`);
  },

  /**
   * 同步单个用户
   */
  syncUser: (employeeId: number) => {
    return postRequest(`/api/im/user/sync/${employeeId}`);
  },

  /**
   * 批量同步用户
   */
  batchSyncUsers: (employeeIds: number[]) => {
    return postRequest('/api/im/user/sync/batch', employeeIds);
  },

  /**
   * 为警情创建IM群组
   */
  createGroupForReport: (reportId: number) => {
    return postRequest(`/api/im/group/create/${reportId}`);
  },

  /**
   * 邀请成员入群
   */
  inviteMembers: (reportId: number, employeeIds: number[]) => {
    return postRequest(`/api/im/group/${reportId}/invite`, employeeIds);
  },

  /**
   * 移除群成员
   */
  kickMembers: (reportId: number, employeeIds: number[]) => {
    return postRequest(`/api/im/group/${reportId}/kick`, employeeIds);
  },

  /**
   * 解散群组
   */
  disbandGroup: (reportId: number) => {
    return postRequest(`/api/im/group/${reportId}/disband`);
  },

  /**
   * 获取警情群组信息
   */
  getGroupInfo: (reportId: number) => {
    return getRequest(`/api/im/group/${reportId}/info`);
  },

  // ========== 消息相关 ==========

  /**
   * 发送群组消息
   */
  sendMessage: (reportId: number, content: string) => {
    return postRequest('/api/im/message/send', {
      reportId,
      content,
      contentType: 101 // 101-文本消息
    });
  },

  /**
   * 获取群组消息历史
   */
  getMessageHistory: (reportId: number, count?: number, startMsgId?: string) => {
    return getRequest('/api/im/message/history', {
      reportId,
      count: count || 20,
      startMsgId
    });
  },

  /**
   * 获取新消息(轮询使用)
   */
  getNewMessages: (reportId: number, sinceTime?: number) => {
    return getRequest('/api/im/message/new', {
      reportId,
      sinceTime
    });
  },

  // ========== 订阅相关 ==========

  /**
   * 订阅警情消息
   */
  subscribeReport: (reportId: number) => {
    return postRequest(`/api/im/subscription/subscribe/${reportId}`);
  },

  /**
   * 取消订阅警情消息
   */
  unsubscribeReport: (reportId: number) => {
    return postRequest(`/api/im/subscription/unsubscribe/${reportId}`);
  },

  /**
   * 获取警情订阅者数量
   */
  getSubscriberCount: (reportId: number) => {
    return getRequest(`/api/im/subscription/subscriber-count/${reportId}`);
  },
};
