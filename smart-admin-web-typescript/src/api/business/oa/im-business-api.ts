/**
 * IM 业务集成 API
 * 处理业务逻辑与 IM 的集成
 *
 * 基于 OPENIM_ARCHITECTURE_REFACTORING.md 规范
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-10
 * @Copyright 1024创新实验室
 */

import { postRequest, getRequest } from '/@/lib/axios';

export interface IMGroupVO {
  /**
   * OpenIM 群组 ID
   */
  groupId: string;

  /**
   * 群组名称
   */
  groupName: string;

  /**
   * 警情ID（业务关联）
   */
  reportId?: number;
}

/**
 * IM 业务集成 API
 * 提供业务与 IM 的集成功能
 */
export const imBusinessApi = {

  /**
   * 为警情创建 IM 群组（后端处理）
   *
   * 流程:
   * 1. 前端调用后端接口
   * 2. 后端调用 OpenIM API 创建群组
   * 3. 后端保存群组映射关系
   * 4. 返回群组ID给前端
   *
   * @param reportId 警情ID
   * @returns 群组ID (String)
   */
  createGroupForReport: (reportId: number) => {
    return postRequest(`/api/im/business/group/create/${reportId}`);
  },

  /**
   * 获取警情关联的群组ID
   *
   * @param reportId 警情ID
   * @returns 群组ID (String)
   */
  getGroupByReportId: (reportId: number) => {
    return getRequest(`/api/im/business/group/${reportId}`);
  },

  /**
   * 检查警情是否已创建群组
   *
   * @param reportId 警情ID
   * @returns 是否已创建群组 (Boolean)
   */
  checkGroupExists: (reportId: number) => {
    return getRequest(`/api/im/business/group/exist/${reportId}`);
  },

  /**
   * 获取群组历史消息 (数据库 fallback方案)
   *
   * 用于SDK无法从本地IndexedDB获取消息时的fallback
   * 直接从后端数据库查询历史消息
   *
   * 使用场景:
   * 1. 无痕模式首次登录 - IndexedDB为空
   * 2. 浏览器清空缓存后 - IndexedDB被清理
   * 3. SDK同步失败 - 本地数据不完整
   *
   * @param groupId 群组ID
   * @param count 消息数量 (默认50)
   * @returns 历史消息列表
   */
  getGroupHistoryMessages: (groupId: string, count: number = 50) => {
    return postRequest(`/api/im/business/messages/history`, { groupId, count });
  },

  /**
   * 保存IM消息到数据库
   *
   * 业务场景:
   * - 前端通过OpenIM SDK发送或接收消息后调用
   * - 用于解决无痕模式首次登录无法加载历史消息的问题
   *
   * 调用时机:
   * - 发送消息成功后
   * - 接收到新消息时
   *
   * 特性:
   * - 自动去重: 相同messageId的消息只保存一次
   * - 异步保存: 不阻塞前端用户操作
   * - 容错处理: 保存失败不影响正常聊天功能
   *
   * @param messageData 消息数据
   * @returns 操作结果
   */
  saveMessage: (messageData: {
    reportId: number;
    groupId: string;
    messageId: string;
    serverMessageId?: string;
    conversationId?: string;
    senderId: string;
    senderName?: string;
    senderAvatar?: string;
    contentType: number;
    content?: string;
    contentJson?: string;
    sendTime: number;
    seq?: number;
  }) => {
    return postRequest(`/api/im/business/messages/save`, messageData);
  },
};
