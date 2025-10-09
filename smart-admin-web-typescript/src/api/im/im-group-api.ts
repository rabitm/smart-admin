/**
 * IM群组API
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */

import { getRequest, postRequest } from '/@/lib/axios';
import type { PoliceGroupMapping, InviteMembersForm } from '/@/types/im';

/**
 * 获取警情群组信息
 */
export function getPoliceGroupApi(reportId: number) {
  return getRequest<PoliceGroupMapping>(`/api/im/group/police/${reportId}`);
}

/**
 * 手动创建警情群组
 */
export function createPoliceGroupApi(reportId: number) {
  return postRequest<PoliceGroupMapping>(`/api/im/group/police/create/${reportId}`);
}

/**
 * 邀请成员加入警情群组
 */
export function inviteMembersApi(data: InviteMembersForm) {
  return postRequest('/api/im/group/police/invite', data);
}
