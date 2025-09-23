/*
 * 警情录入常量
 *
 * @Author:    Claude Code Assistant
 * @Date:      2025-09-18
 * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
 */

import { SmartEnum } from '/@/types/smart-enum';
import { DICT_CODE_ENUM } from '/@/constants/support/dict-const';

// 应急警情类型枚举
export const POLICE_REPORT_TYPE_ENUM: SmartEnum<number> = {
  FIRE: {
    value: 1,
    desc: '火灾事故',
    icon: '🔥',
    color: '#ff4d4f',
    category: 'emergency'
  },
  RESCUE: {
    value: 2,
    desc: '抢险救援',
    icon: '🚑',
    color: '#fa8c16',
    category: 'emergency'
  },
  MEDICAL: {
    value: 3,
    desc: '医疗急救',
    icon: '🏥',
    color: '#52c41a',
    category: 'emergency'
  },
  TRAFFIC: {
    value: 4,
    desc: '交通事故',
    icon: '🚗',
    color: '#1890ff',
    category: 'accident'
  },
  SECURITY: {
    value: 5,
    desc: '治安事件',
    icon: '⚖️',
    color: '#722ed1',
    category: 'security'
  },
  CRIMINAL: {
    value: 6,
    desc: '刑事案件',
    icon: '🔴',
    color: '#eb2f96',
    category: 'security'
  },
  NATURAL_DISASTER: {
    value: 7,
    desc: '自然灾害',
    icon: '🌪️',
    color: '#13c2c2',
    category: 'emergency'
  },
  OTHER: {
    value: 99,
    desc: '其他事件',
    icon: '❓',
    color: '#8c8c8c',
    category: 'other'
  },
};

// 警情等级枚举
export const POLICE_REPORT_LEVEL_ENUM: SmartEnum<number> = {
  URGENT: {
    value: 1,
    desc: '紧急',
  },
  HIGH: {
    value: 2,
    desc: '高',
  },
  MEDIUM: {
    value: 3,
    desc: '中',
  },
  LOW: {
    value: 4,
    desc: '低',
  },
};

// 警情状态枚举
export const POLICE_REPORT_STATUS_ENUM: SmartEnum<number> = {
  PENDING: {
    value: 1,
    desc: '待处理',
  },
  PROCESSING: {
    value: 2,
    desc: '处理中',
  },
  COMPLETED: {
    value: 3,
    desc: '已完成',
  },
  CLOSED: {
    value: 4,
    desc: '已关闭',
  },
};

// 动态表单配置 - 根据警情类型显示不同录入项
export const EMERGENCY_FORM_CONFIG = {
  // 火灾事故
  1: {
    step2Fields: [
      {
        key: 'peopleInfo',
        label: '人员情况',
        type: 'compact-group',
        required: true,
        icon: '👥',
        fields: [
          { key: 'trappedCount', label: '被困', dictCode: DICT_CODE_ENUM.TRAPPED_COUNT },
          { key: 'casualties', label: '伤亡', dictCode: DICT_CODE_ENUM.CASUALTIES_LEVEL }
        ]
      },
      {
        key: 'fireInfo',
        label: '火情详情',
        type: 'compact-group',
        required: true,
        icon: '🔥',
        fields: [
          { key: 'fireFloor', label: '楼层', dictCode: DICT_CODE_ENUM.FIRE_FLOOR },
          { key: 'fireScale', label: '火势', dictCode: DICT_CODE_ENUM.FIRE_SCALE }
        ]
      },
      {
        key: 'materialSmoke',
        label: '物质与烟雾',
        type: 'compact-group',
        required: true,
        icon: '💨',
        fields: [
          { key: 'burningMaterial', label: '燃烧物', dictCode: DICT_CODE_ENUM.BURNING_MATERIAL },
          { key: 'smokeCondition', label: '烟雾', dictCode: DICT_CODE_ENUM.SMOKE_CONDITION }
        ]
      }
    ],
    step3Fields: [
      {
        key: 'additionalInfo',
        label: '补充信息',
        type: 'compact-group',
        required: false,
        icon: '📋',
        fields: [
          { key: 'burnArea', label: '面积', type: 'input', placeholder: '约50平米' },
          { key: 'fireSource', label: '原因', dictCode: DICT_CODE_ENUM.FIRE_SOURCE }
        ]
      },
      { key: 'rescueEquipment', label: '所需装备', type: 'checkbox-compact', required: false, icon: '🚒', dictCode: DICT_CODE_ENUM.RESCUE_EQUIPMENT }
    ]
  },

  // 抢险救援
  2: {
    step2Fields: [
      { key: 'rescueType', label: '救援类型', type: 'select', required: true, icon: '🚑', dictCode: DICT_CODE_ENUM.RESCUE_TYPE },
      { key: 'trappedCount', label: '被困人数', type: 'number', required: true, icon: '👥', dictCode: DICT_CODE_ENUM.TRAPPED_COUNT },
      { key: 'dangerLevel', label: '危险等级', type: 'select', required: true, icon: '⚠️', dictCode: DICT_CODE_ENUM.DANGER_LEVEL },
      { key: 'casualties', label: '伤亡情况', type: 'select', required: false, icon: '🚨', dictCode: DICT_CODE_ENUM.CASUALTIES_LEVEL }
    ],
    step3Fields: [
      { key: 'rescueEquipment', label: '所需设备', type: 'checkbox', required: true, icon: '🔧', dictCode: DICT_CODE_ENUM.RESCUE_EQUIPMENT },
      { key: 'specialRequirements', label: '特殊要求', type: 'textarea', required: false, icon: '📝', placeholder: '如：需要专业技术人员、特殊工具等' },
      { key: 'accessRoute', label: '进入路线', type: 'input', required: false, icon: '🛣️', placeholder: '救援车辆最佳进入路线' }
    ]
  },

  // 医疗急救
  3: {
    step2Fields: [
      { key: 'casualties', label: '伤亡人数', type: 'select', required: true, icon: '🚨', dictCode: DICT_CODE_ENUM.CASUALTIES_LEVEL },
      { key: 'injuryLevel', label: '伤情等级', type: 'select', required: true, icon: '🏥', dictCode: DICT_CODE_ENUM.INJURY_LEVEL },
      { key: 'emergencyType', label: '急救类型', type: 'select', required: true, icon: '💊', dictCode: DICT_CODE_ENUM.EMERGENCY_TYPE },
      { key: 'consciousness', label: '意识状态', type: 'select', required: false, icon: '🧠', dictCode: DICT_CODE_ENUM.CONSCIOUSNESS_STATE }
    ],
    step3Fields: [
      { key: 'symptoms', label: '主要症状', type: 'textarea', required: true, icon: '📋', placeholder: '详细描述伤情和症状' },
      { key: 'vitalSigns', label: '生命体征', type: 'input', required: false, icon: '💓', placeholder: '如：呼吸、脉搏、血压等' },
      { key: 'medicalHistory', label: '病史', type: 'input', required: false, icon: '📄', placeholder: '已知的疾病史或过敏史' }
    ]
  },

  // 交通事故
  4: {
    step2Fields: [
      { key: 'accidentType', label: '事故类型', type: 'select', required: true, icon: '🚗', dictCode: DICT_CODE_ENUM.ACCIDENT_TYPE },
      { key: 'vehicleCount', label: '车辆数量', type: 'select', required: true, icon: '🚙', dictCode: DICT_CODE_ENUM.VEHICLE_COUNT },
      { key: 'casualties', label: '伤亡情况', type: 'select', required: true, icon: '🚨', dictCode: DICT_CODE_ENUM.CASUALTIES_LEVEL },
      { key: 'roadBlock', label: '道路阻塞', type: 'select', required: true, icon: '🚧', dictCode: DICT_CODE_ENUM.ROAD_BLOCK_LEVEL }
    ],
    step3Fields: [
      { key: 'vehicleInfo', label: '车辆信息', type: 'textarea', required: false, icon: '🚗', placeholder: '车牌号、车型、损毁情况等' },
      { key: 'roadCondition', label: '路面状况', type: 'select', required: false, icon: '🛣️', dictCode: DICT_CODE_ENUM.ROAD_CONDITION },
      { key: 'weatherCondition', label: '天气情况', type: 'select', required: false, icon: '🌤️', dictCode: DICT_CODE_ENUM.WEATHER_CONDITION }
    ]
  },

  // 治安事件
  5: {
    step2Fields: [
      { key: 'eventNature', label: '事件性质', type: 'select', required: true, icon: '⚖️', dictCode: DICT_CODE_ENUM.SECURITY_EVENT_NATURE },
      { key: 'involvedCount', label: '涉及人数', type: 'select', required: true, icon: '👥', dictCode: DICT_CODE_ENUM.INVOLVEMENT_COUNT },
      { key: 'dangerLevel', label: '危险程度', type: 'select', required: true, icon: '⚠️', dictCode: DICT_CODE_ENUM.DANGER_LEVEL },
      { key: 'weaponInvolved', label: '是否涉械', type: 'select', required: false, icon: '🔪', dictCode: DICT_CODE_ENUM.WEAPON_TYPE }
    ],
    step3Fields: [
      { key: 'eventProcess', label: '事件经过', type: 'textarea', required: true, icon: '📝', placeholder: '简要描述事件发生经过' },
      { key: 'suspects', label: '嫌疑人特征', type: 'textarea', required: false, icon: '👤', placeholder: '身高、年龄、衣着等特征' },
      { key: 'evidence', label: '现场证据', type: 'input', required: false, icon: '🔍', placeholder: '遗留物品、监控等' }
    ]
  },

  // 刑事案件
  6: {
    step2Fields: [
      { key: 'caseNature', label: '案件性质', type: 'select', required: true, icon: '🔴', dictCode: DICT_CODE_ENUM.CRIMINAL_CASE_NATURE },
      { key: 'victimCount', label: '受害人数', type: 'select', required: true, icon: '👥', dictCode: DICT_CODE_ENUM.INVOLVEMENT_COUNT },
      { key: 'urgencyLevel', label: '紧急程度', type: 'select', required: true, icon: '🚨', dictCode: DICT_CODE_ENUM.URGENCY_LEVEL },
      { key: 'suspectStatus', label: '嫌疑人状态', type: 'select', required: false, icon: '👤', dictCode: DICT_CODE_ENUM.SUSPECT_STATUS }
    ],
    step3Fields: [
      { key: 'crimeMethod', label: '作案手段', type: 'textarea', required: true, icon: '🔍', placeholder: '作案方式、使用工具等' },
      { key: 'victimCondition', label: '受害情况', type: 'textarea', required: true, icon: '🚨', placeholder: '受害人伤情、财物损失等' },
      { key: 'sceneProtection', label: '现场保护', type: 'select', required: false, icon: '🚧', dictCode: DICT_CODE_ENUM.SCENE_PROTECTION }
    ]
  },

  // 自然灾害
  7: {
    step2Fields: [
      { key: 'disasterType', label: '灾害类型', type: 'select', required: true, icon: '🌪️', dictCode: DICT_CODE_ENUM.DISASTER_TYPE },
      { key: 'affectedArea', label: '影响范围', type: 'select', required: true, icon: '📐', dictCode: DICT_CODE_ENUM.AFFECTED_AREA },
      { key: 'dangerLevel', label: '危险等级', type: 'select', required: true, icon: '⚠️', dictCode: DICT_CODE_ENUM.DISASTER_WARNING_LEVEL },
      { key: 'casualties', label: '伤亡情况', type: 'select', required: false, icon: '🚨', dictCode: DICT_CODE_ENUM.CASUALTIES_LEVEL }
    ],
    step3Fields: [
      { key: 'disasterScale', label: '灾害规模', type: 'textarea', required: true, icon: '📊', placeholder: '受灾面积、损失程度等' },
      { key: 'rescueNeeds', label: '救援需求', type: 'checkbox', required: true, icon: '🚁', dictCode: DICT_CODE_ENUM.RESCUE_NEEDS },
      { key: 'evacuationPlan', label: '疏散方案', type: 'textarea', required: false, icon: '🚶', placeholder: '疏散路线、安置地点等' }
    ]
  },

  // 其他事件
  99: {
    step2Fields: [
      { key: 'eventType', label: '事件类型', type: 'input', required: true, icon: '❓', placeholder: '请描述事件类型' },
      { key: 'urgencyLevel', label: '紧急程度', type: 'select', required: true, icon: '⚠️', dictCode: DICT_CODE_ENUM.URGENCY_LEVEL },
      { key: 'involvedCount', label: '涉及人数', type: 'select', required: false, icon: '👥', dictCode: DICT_CODE_ENUM.INVOLVEMENT_COUNT }
    ],
    step3Fields: [
      { key: 'detailDescription', label: '详细描述', type: 'textarea', required: true, icon: '📝', placeholder: '请详细描述事件情况' },
      { key: 'specialNeeds', label: '特殊需求', type: 'textarea', required: false, icon: '🔧', placeholder: '所需支援、设备等' }
    ]
  }
};

export default {
  POLICE_REPORT_TYPE_ENUM,
  POLICE_REPORT_LEVEL_ENUM,
  POLICE_REPORT_STATUS_ENUM,
  EMERGENCY_FORM_CONFIG,
};// 强制更新: 2025年09月22日 16:26:31
