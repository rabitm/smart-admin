/*
 * 警情表单模板管理API
 *
 * @Author:    Claude Code Assistant
 * @Date:      2025-09-22
 * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
 */

import { postRequest, getRequest, request } from '/@/lib/axios';

export interface PoliceFormTemplateVO {
  id: number;
  reportType: number;
  reportTypeName: string;
  templateName: string;
  description: string;
  isDefault: boolean;
  status: boolean;
  organizationId?: number;
  organizationName?: string;
  fieldCount: number;
  createUserId: number;
  createUserName: string;
  createTime: string;
  updateTime: string;
}

export interface PoliceFormTemplateAddForm {
  reportType: number;
  templateName: string;
  description?: string;
  isDefault?: boolean;
  organizationId?: number;
}

export interface FieldItem {
  id?: number;
  fieldKey: string;
  fieldLabel: string;
  fieldType: string;
  isRequired?: boolean;
  fieldIcon?: string;
  placeholder?: string;
  fieldOptions?: string[];
  quickOptions?: string[];
  dictCode?: string; // 数据字典编码，当字段类型为select/checkbox等时使用
  parentFieldId?: number;
  sortOrder?: number;
  stepNumber?: number;
  validationRules?: string;
  defaultValue?: string;
  children?: FieldItem[];
}

export interface PoliceFormFieldSaveForm {
  templateId: number;
  fields: FieldItem[];
}

export const policeFormTemplateApi = {

  /**
   * 获取模板列表
   */
  getTemplateList: () => {
    return getRequest('/api/business/oa/police/form/template/list');
  },

  /**
   * 创建模板
   */
  createTemplate: (form: PoliceFormTemplateAddForm) => {
    return postRequest('/api/business/oa/police/form/template/create', form);
  },

  /**
   * 获取模板详情
   */
  getTemplateDetail: (templateId: number) => {
    return getRequest(`/api/business/oa/police/form/template/detail/${templateId}`);
  },

  /**
   * 保存字段配置
   */
  saveFields: (form: PoliceFormFieldSaveForm) => {
    return postRequest('/api/business/oa/police/form/template/fields/save', form);
  },

  /**
   * 删除模板
   */
  deleteTemplate: (templateId: number) => {
    return request({
      url: `/api/business/oa/police/form/template/${templateId}`,
      method: 'delete'
    });
  }

};

export default policeFormTemplateApi;