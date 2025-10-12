/**
 * IM Token API
 * 与后端交互获取 OpenIM Token
 *
 * 基于 OPENIM_ARCHITECTURE_REFACTORING.md 规范
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-10
 * @Copyright 1024创新实验室
 */

import { getRequest, postRequest } from '/@/lib/axios';

export interface IMTokenVO {
  /**
   * OpenIM 用户 ID
   */
  openimUserId: string;

  /**
   * OpenIM Token
   */
  token: string;

  /**
   * Token 过期时间（Unix时间戳，秒）
   */
  expireTime: number;
}

export interface IMUserMappingVO {
  /**
   * 员工ID
   */
  employeeId: number;

  /**
   * OpenIM 用户 ID
   */
  openimUserId: string;
}

/**
 * IM Token API
 * 提供 Token 获取、刷新和用户映射查询功能
 */
export const imTokenApi = {

  /**
   * 获取当前登录用户的 OpenIM Token
   *
   * 流程:
   * 1. 后端检查用户映射，如不存在则自动注册 OpenIM 用户
   * 2. 后端调用 OpenIM API 生成 Token
   * 3. Token 缓存在 Redis，提前 5 分钟过期
   *
   * @returns OpenIM Token 信息
   */
  getToken: async (): Promise<IMTokenVO> => {
    const response = await getRequest<any>('/api/im/token/current');
    return response.data;
  },

  /**
   * 刷新当前用户的 Token
   *
   * 用于 Token 即将过期时自动刷新
   *
   * @returns 新的 Token 信息
   */
  refreshToken: async (): Promise<IMTokenVO> => {
    const response = await postRequest<any>('/api/im/token/refresh');
    return response.data;
  },

  /**
   * 批量获取员工的 OpenIM 用户 ID
   *
   * 用于邀请成员时获取 OpenIM 用户 ID
   *
   * @param employeeIds 员工ID列表
   * @returns 员工ID到OpenIM用户ID的映射 { employeeId: openimUserId }
   */
  batchGetUserMapping: async (employeeIds: number[]): Promise<Record<number, string>> => {
    const response = await postRequest<any>('/api/im/user-mapping/batch', { employeeIds });
    return response.data;
  },

  /**
   * 获取单个员工的 OpenIM 用户 ID
   *
   * @param employeeId 员工ID
   * @returns OpenIM 用户 ID
   */
  getUserMapping: async (employeeId: number): Promise<string> => {
    const response = await getRequest<any>(`/api/im/user-mapping/${employeeId}`);
    return response.data;
  },

  /**
   * 根据 OpenIM 用户 ID 反查员工 ID
   *
   * 用于 Webhook 回调或消息发送者识别
   *
   * @param openimUserId OpenIM 用户 ID
   * @returns 员工ID
   */
  getEmployeeIdByOpenIMUserId: async (openimUserId: string): Promise<number> => {
    const response = await getRequest<any>(`/api/im/user-mapping/reverse/${openimUserId}`);
    return response.data;
  },
};
