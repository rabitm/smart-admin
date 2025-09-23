/**
 * 字典key 编码 常量
 *
 * 该常量来自于 字典管理中的数据，写在该文件目的是为了统一引用，将来好修改
 *
 * @Author:    1024创新实验室-主任：卓大
 * @Date:      2024-09-03 22:09:10
 * @Wechat:    zhuda1024
 * @Email:     lab1024@163.com
 * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
 */

export const DICT_SPLIT = ',';

export const DICT_CODE_ENUM = {
  GOODS_PLACE: 'GOODS_PLACE',

  // 警务系统数据字典
  // 火灾相关
  FIRE_SCALE: 'FIRE_SCALE', // 火势程度
  FIRE_SOURCE: 'FIRE_SOURCE', // 起火原因
  FIRE_FLOOR: 'FIRE_FLOOR', // 火灾楼层
  BURNING_MATERIAL: 'BURNING_MATERIAL', // 燃烧物质
  SMOKE_CONDITION: 'SMOKE_CONDITION', // 烟雾情况
  ECONOMIC_LOSS_LEVEL: 'ECONOMIC_LOSS_LEVEL', // 经济损失等级

  // 人员相关
  TRAPPED_COUNT: 'TRAPPED_COUNT', // 被困人数
  CASUALTIES_LEVEL: 'CASUALTIES_LEVEL', // 伤亡情况
  INJURY_LEVEL: 'INJURY_LEVEL', // 伤情等级
  CONSCIOUSNESS_STATE: 'CONSCIOUSNESS_STATE', // 意识状态

  // 交通事故相关
  ACCIDENT_TYPE: 'ACCIDENT_TYPE', // 事故类型
  VEHICLE_COUNT: 'VEHICLE_COUNT', // 车辆数量
  ROAD_BLOCK_LEVEL: 'ROAD_BLOCK_LEVEL', // 道路阻塞程度
  ROAD_CONDITION: 'ROAD_CONDITION', // 路面状况
  WEATHER_CONDITION: 'WEATHER_CONDITION', // 天气情况

  // 抢险救援相关
  RESCUE_TYPE: 'RESCUE_TYPE', // 救援类型
  DANGER_LEVEL: 'DANGER_LEVEL', // 危险等级
  RESCUE_EQUIPMENT: 'RESCUE_EQUIPMENT', // 救援设备

  // 医疗急救相关
  EMERGENCY_TYPE: 'EMERGENCY_TYPE', // 急救类型

  // 治安事件相关
  SECURITY_EVENT_NATURE: 'SECURITY_EVENT_NATURE', // 事件性质
  WEAPON_TYPE: 'WEAPON_TYPE', // 涉械类型

  // 刑事案件相关
  CRIMINAL_CASE_NATURE: 'CRIMINAL_CASE_NATURE', // 案件性质
  URGENCY_LEVEL: 'URGENCY_LEVEL', // 紧急程度
  SUSPECT_STATUS: 'SUSPECT_STATUS', // 嫌疑人状态
  SCENE_PROTECTION: 'SCENE_PROTECTION', // 现场保护

  // 自然灾害相关
  DISASTER_TYPE: 'DISASTER_TYPE', // 灾害类型
  AFFECTED_AREA: 'AFFECTED_AREA', // 影响范围
  DISASTER_WARNING_LEVEL: 'DISASTER_WARNING_LEVEL', // 预警等级
  RESCUE_NEEDS: 'RESCUE_NEEDS', // 救援需求

  // 通用
  YES_NO: 'YES_NO', // 是否
  INVOLVEMENT_COUNT: 'INVOLVEMENT_COUNT', // 涉及人数
};

export default {
  DICT_CODE_ENUM,
};
