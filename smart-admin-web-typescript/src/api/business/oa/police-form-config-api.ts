/*
 * 警情表单配置API
 *
 * @Author:    Claude Code Assistant
 * @Date:      2025-09-22
 * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
 */

import { postRequest, getRequest } from '/@/lib/axios';

export interface PoliceFormFieldVO {
  id?: number;
  key: string;
  label: string;
  type: string;
  required?: boolean;
  icon?: string;
  placeholder?: string;
  options?: string[];
  quickOptions?: string[];
  dictCode?: string; // 数据字典编码，当字段类型为select/checkbox等时使用
  defaultValue?: string;
  fields?: PoliceFormFieldVO[]; // 子字段，用于组合字段
  sortOrder?: number;
  validationRules?: string;
}

export interface PoliceFormConfigVO {
  reportType: number;
  templateId: number;
  templateName: string;
  step2Fields: PoliceFormFieldVO[];
  step3Fields: PoliceFormFieldVO[];
}

export const policeFormConfigApi = {

  /**
   * 获取表单配置
   */
  getFormConfig: (reportType: number) => {
    return getRequest(`/api/business/oa/police/form/config/${reportType}`);
  },

  /**
   * 获取表单配置（指定组织）
   */
  getFormConfigWithOrg: (reportType: number, organizationId: number) => {
    return getRequest(`/api/business/oa/police/form/config/${reportType}/${organizationId}`);
  }

};

export default policeFormConfigApi;