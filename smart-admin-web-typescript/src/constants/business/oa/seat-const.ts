/*
 * 座位管理常量
 *
 * @Author:    Claude Code Assistant
 * @Date:      2025-09-25
 * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
 */

import { SmartEnum } from '/@/types/smart-enum';

// 席位状态枚举
export const SEAT_STATUS_ENUM: SmartEnum<number> = {
  AVAILABLE: {
    value: 0,
    desc: '空闲',
    icon: '✅',
    color: '#52c41a'
  },
  BUSY: {
    value: 1,
    desc: '忙碌',
    icon: '👤',
    color: '#fa8c16'
  },
  OFFLINE: {
    value: 2,
    desc: '离线',
    icon: '📴',
    color: '#8c8c8c'
  },
  MAINTENANCE: {
    value: 3,
    desc: '维护',
    icon: '🔧',
    color: '#f5222d'
  }
};

// 席位类型枚举
export const SEAT_TYPE_ENUM: SmartEnum<number> = {
  ANSWER: {
    value: 1,
    desc: '接警席',
    icon: '📞',
    color: '#1890ff'
  },
  DISPATCH: {
    value: 2,
    desc: '处警席',
    icon: '🚔',
    color: '#52c41a'
  },
  SUPERVISION: {
    value: 3,
    desc: '督导席',
    icon: '👨‍💼',
    color: '#722ed1'
  }
};

// 在线状态枚举
export const ONLINE_STATUS_ENUM: SmartEnum<number> = {
  OFFLINE: {
    value: 0,
    desc: '离线',
    icon: '⚫',
    color: '#8c8c8c'
  },
  ONLINE: {
    value: 1,
    desc: '在线',
    icon: '🟢',
    color: '#52c41a'
  },
  BUSY: {
    value: 2,
    desc: '忙碌',
    icon: '🔴',
    color: '#fa8c16'
  },
  AWAY: {
    value: 3,
    desc: '离开',
    icon: '🟡',
    color: '#fadb14'
  }
};

// 导出所有常量
export default {
  SEAT_STATUS_ENUM,
  SEAT_TYPE_ENUM,
  ONLINE_STATUS_ENUM,
};