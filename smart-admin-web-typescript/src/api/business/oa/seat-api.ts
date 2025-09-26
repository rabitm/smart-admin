/*
 * 座位管理API
 *
 * @Author:    Claude Code Assistant
 * @Date:      2025-09-25
 * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
 */

import { getRequest, postRequest } from '/@/lib/axios';

export const seatApi = {

  /**
   * 查询座位列表
   */
  queryList: (param: any) => {
    return postRequest('/seat/query', param);
  },

  /**
   * 获取座位详情
   */
  getDetail: (seatId: number) => {
    return getRequest(`/seat/get/${seatId}`);
  },

  /**
   * 添加座位
   */
  add: (param: any) => {
    return postRequest('/seat/add', param);
  },

  /**
   * 更新座位
   */
  update: (param: any) => {
    return postRequest('/seat/update', param);
  },

  /**
   * 删除座位
   */
  delete: (seatId: number) => {
    return postRequest(`/seat/delete/${seatId}`, {});
  },

  /**
   * 批量删除座位
   */
  batchDelete: (seatIdList: number[]) => {
    return postRequest('/seat/batchDelete', seatIdList);
  },

  /**
   * 预约座位
   */
  reserve: (param: any) => {
    return postRequest('/seat/reserve', param);
  },

  /**
   * 取消预约
   */
  cancelReservation: (seatId: number) => {
    return postRequest(`/seat/cancelReservation/${seatId}`, {});
  },

  /**
   * 占用座位
   */
  occupy: (seatId: number) => {
    return postRequest(`/seat/occupy/${seatId}`, {});
  },

  /**
   * 释放座位
   */
  release: (seatId: number) => {
    return postRequest(`/seat/release/${seatId}`, {});
  },

  /**
   * 设置维护状态
   */
  setMaintenance: (seatId: number, isMaintenance: boolean) => {
    return postRequest('/seat/maintenance', { seatId, isMaintenance });
  }

};