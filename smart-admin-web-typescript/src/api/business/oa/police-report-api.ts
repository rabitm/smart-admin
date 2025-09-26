/*
 * 警情录入API
 *
 * @Author:    Claude Code Assistant
 * @Date:      2025-09-18
 * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
 */
import { postRequest, getRequest } from '/@/lib/axios';

export const policeReportApi = {
  // 分页查询警情信息 @author Claude Code Assistant
  pageQuery: (param) => {
    return postRequest('/oa/police/report/page/query', param);
  },

  // 查询警情信息详情 @author Claude Code Assistant
  getDetail: (reportId) => {
    return getRequest(`/oa/police/report/get/${reportId}`);
  },

  // 新增警情信息 @author Claude Code Assistant
  addPoliceReport: (param) => {
    return postRequest('/oa/police/report/add', param);
  },

  // 更新警情信息 @author Claude Code Assistant
  updatePoliceReport: (param) => {
    return postRequest('/oa/police/report/update', param);
  },

  // 删除警情信息 @author Claude Code Assistant
  deletePoliceReport: (reportId) => {
    return getRequest(`/oa/police/report/delete/${reportId}`);
  },

  // 根据报警人电话查询警情列表 @author Claude Code Assistant
  queryByReporterPhone: (reporterPhone) => {
    return getRequest(`/oa/police/report/query/by-phone/${reporterPhone}`);
  },

  // 根据处理人员查询警情列表 @author Claude Code Assistant
  queryByHandler: (handlerId) => {
    return getRequest(`/oa/police/report/query/by-handler/${handlerId}`);
  },

  // 获取各状态警情统计 @author Claude Code Assistant
  getStatusStatistics: () => {
    return getRequest('/oa/police/report/statistics/status');
  },

  // 搜索地址建议 @author Claude Code Assistant
  searchLocationSuggestions: (keyword) => {
    return getRequest(`/oa/police/report/location/search?keyword=${encodeURIComponent(keyword)}`);
  },

  // 测试地址建议（无权限） @author Claude Code Assistant
  testLocationSuggestions: (keyword) => {
    return getRequest(`/oa/police/report/location/test?keyword=${encodeURIComponent(keyword)}`);
  },

  // 获取警情专业字段数据 @author Claude Code Assistant
  getPoliceReportFieldData: (reportId) => {
    return getRequest(`/oa/police/report/field-data/${reportId}`);
  },

  // 锁定警情（获取编辑权限）@author Claude Code Assistant
  lock: (reportId, seatId) => {
    const params = seatId ? `?seatId=${seatId}` : '';
    return postRequest(`/oa/police/report/lock/${reportId}${params}`, {});
  },

  // 解锁警情（释放编辑权限）@author Claude Code Assistant
  unlock: (reportId) => {
    return postRequest(`/oa/police/report/unlock/${reportId}`, {});
  },

  // 检查警情是否被锁定 @author Claude Code Assistant
  checkLock: (reportId) => {
    return getRequest(`/oa/police/report/check-lock/${reportId}`);
  },

  // 同步字段更新 @author Claude Code Assistant
  syncFieldUpdate: (reportId, fieldName, fieldValue) => {
    const params = new URLSearchParams();
    params.append('fieldName', fieldName);
    if (fieldValue != null) {
      params.append('fieldValue', String(fieldValue));
    }
    return postRequest(`/oa/police/report/sync-field/${reportId}?${params}`);
  },
};