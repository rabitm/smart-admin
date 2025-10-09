/**
 * IM认证API
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */

import { getRequest, postRequest } from '/@/lib/axios';
import type { ImTokenInfo } from '/@/types/im';

/**
 * 获取IM Token
 */
export function getImTokenApi() {
  return getRequest<ImTokenInfo>('/api/im/auth/token');
}

/**
 * 刷新IM Token
 */
export function refreshImTokenApi() {
  return postRequest<ImTokenInfo>('/api/im/auth/refresh');
}
