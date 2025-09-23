-- --------------------------------------------------------
-- 版本更新脚本 v3.27.1
-- 警务系统数据字典
-- --------------------------------------------------------

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 插入警务系统数据字典
-- ----------------------------

-- 1. 火灾相关字典
INSERT INTO `t_dict` (`dict_name`, `dict_code`, `remark`, `disabled_flag`) VALUES
('火势程度', 'FIRE_SCALE', '用于火灾事故中描述火势大小程度', 0),
('起火原因', 'FIRE_SOURCE', '用于火灾事故中记录起火原因类型', 0),
('燃烧物质', 'BURNING_MATERIAL', '用于火灾事故中记录燃烧的物质类型', 0),
('烟雾情况', 'SMOKE_CONDITION', '用于火灾事故中描述烟雾状况', 0),
('经济损失等级', 'ECONOMIC_LOSS_LEVEL', '用于火灾事故中评估经济损失程度', 0);

-- 2. 人员相关字典
INSERT INTO `t_dict` (`dict_name`, `dict_code`, `remark`, `disabled_flag`) VALUES
('被困人数', 'TRAPPED_COUNT', '用于各类事故中记录被困人员数量', 0),
('伤亡情况', 'CASUALTIES_LEVEL', '用于各类事故中记录人员伤亡情况', 0),
('伤情等级', 'INJURY_LEVEL', '用于医疗急救中评估伤情严重程度', 0),
('意识状态', 'CONSCIOUSNESS_STATE', '用于医疗急救中记录伤员意识状态', 0);

-- 3. 交通事故相关字典
INSERT INTO `t_dict` (`dict_name`, `dict_code`, `remark`, `disabled_flag`) VALUES
('事故类型', 'ACCIDENT_TYPE', '用于交通事故中记录事故发生类型', 0),
('车辆数量', 'VEHICLE_COUNT', '用于交通事故中记录涉及车辆数量', 0),
('道路阻塞程度', 'ROAD_BLOCK_LEVEL', '用于交通事故中描述道路阻塞情况', 0),
('路面状况', 'ROAD_CONDITION', '用于交通事故中记录路面条件', 0),
('天气情况', 'WEATHER_CONDITION', '用于各类事故中记录天气状况', 0);

-- 4. 抢险救援相关字典
INSERT INTO `t_dict` (`dict_name`, `dict_code`, `remark`, `disabled_flag`) VALUES
('救援类型', 'RESCUE_TYPE', '用于抢险救援中记录救援行动类型', 0),
('危险等级', 'DANGER_LEVEL', '用于各类事故中评估危险程度', 0),
('救援设备', 'RESCUE_EQUIPMENT', '用于抢险救援中记录所需设备', 0);

-- 5. 医疗急救相关字典
INSERT INTO `t_dict` (`dict_name`, `dict_code`, `remark`, `disabled_flag`) VALUES
('急救类型', 'EMERGENCY_TYPE', '用于医疗急救中记录急救情况类型', 0);

-- 6. 治安事件相关字典
INSERT INTO `t_dict` (`dict_name`, `dict_code`, `remark`, `disabled_flag`) VALUES
('事件性质', 'SECURITY_EVENT_NATURE', '用于治安事件中记录事件性质', 0),
('涉械类型', 'WEAPON_TYPE', '用于治安/刑事案件中记录涉械情况', 0);

-- 7. 刑事案件相关字典
INSERT INTO `t_dict` (`dict_name`, `dict_code`, `remark`, `disabled_flag`) VALUES
('案件性质', 'CRIMINAL_CASE_NATURE', '用于刑事案件中记录案件性质', 0),
('紧急程度', 'URGENCY_LEVEL', '用于各类事件中评估紧急程度', 0),
('嫌疑人状态', 'SUSPECT_STATUS', '用于刑事案件中记录嫌疑人状态', 0),
('现场保护', 'SCENE_PROTECTION', '用于刑事案件中记录现场保护情况', 0);

-- 8. 自然灾害相关字典
INSERT INTO `t_dict` (`dict_name`, `dict_code`, `remark`, `disabled_flag`) VALUES
('灾害类型', 'DISASTER_TYPE', '用于自然灾害中记录灾害类型', 0),
('影响范围', 'AFFECTED_AREA', '用于自然灾害中描述影响范围', 0),
('预警等级', 'DISASTER_WARNING_LEVEL', '用于自然灾害中记录预警等级', 0),
('救援需求', 'RESCUE_NEEDS', '用于自然灾害中记录救援需求', 0);

-- 9. 通用字典
INSERT INTO `t_dict` (`dict_name`, `dict_code`, `remark`, `disabled_flag`) VALUES
('是否', 'YES_NO', '通用的是否选择', 0),
('涉及人数', 'INVOLVEMENT_COUNT', '用于各类事件中记录涉及人员数量', 0);

-- ----------------------------
-- 插入字典数据
-- ----------------------------

-- 获取字典ID（使用变量存储）
SET @fire_scale_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'FIRE_SCALE');
SET @fire_source_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'FIRE_SOURCE');
SET @burning_material_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'BURNING_MATERIAL');
SET @smoke_condition_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'SMOKE_CONDITION');
SET @economic_loss_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'ECONOMIC_LOSS_LEVEL');
SET @trapped_count_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'TRAPPED_COUNT');
SET @casualties_level_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'CASUALTIES_LEVEL');
SET @injury_level_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'INJURY_LEVEL');
SET @consciousness_state_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'CONSCIOUSNESS_STATE');
SET @accident_type_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'ACCIDENT_TYPE');
SET @vehicle_count_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'VEHICLE_COUNT');
SET @road_block_level_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'ROAD_BLOCK_LEVEL');
SET @road_condition_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'ROAD_CONDITION');
SET @weather_condition_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'WEATHER_CONDITION');
SET @rescue_type_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'RESCUE_TYPE');
SET @danger_level_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'DANGER_LEVEL');
SET @rescue_equipment_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'RESCUE_EQUIPMENT');
SET @emergency_type_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'EMERGENCY_TYPE');
SET @security_event_nature_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'SECURITY_EVENT_NATURE');
SET @weapon_type_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'WEAPON_TYPE');
SET @criminal_case_nature_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'CRIMINAL_CASE_NATURE');
SET @urgency_level_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'URGENCY_LEVEL');
SET @suspect_status_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'SUSPECT_STATUS');
SET @scene_protection_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'SCENE_PROTECTION');
SET @disaster_type_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'DISASTER_TYPE');
SET @affected_area_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'AFFECTED_AREA');
SET @disaster_warning_level_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'DISASTER_WARNING_LEVEL');
SET @rescue_needs_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'RESCUE_NEEDS');
SET @yes_no_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'YES_NO');
SET @involvement_count_id = (SELECT dict_id FROM t_dict WHERE dict_code = 'INVOLVEMENT_COUNT');

-- 1. 火势程度
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@fire_scale_id, 'MINOR', '轻微火情', '初起阶段，火势较小', 10, 0),
(@fire_scale_id, 'GENERAL', '一般火灾', '火势发展中等', 20, 0),
(@fire_scale_id, 'SERIOUS', '较大火灾', '火势较为严重', 30, 0),
(@fire_scale_id, 'MAJOR', '重大火灾', '火势重大，损失严重', 40, 0),
(@fire_scale_id, 'EXTREME', '特别重大火灾', '火势极其严重', 50, 0);

-- 2. 起火原因
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@fire_source_id, 'ELECTRICAL', '电气故障', '电线短路、设备故障等', 10, 0),
(@fire_source_id, 'CARELESS_FIRE', '用火不慎', '烹饪、用火不当等', 20, 0),
(@fire_source_id, 'SMOKING', '吸烟', '烟头引起火灾', 30, 0),
(@fire_source_id, 'PLAYING_FIRE', '玩火', '儿童玩火等', 40, 0),
(@fire_source_id, 'SPONTANEOUS', '自燃', '物质自燃', 50, 0),
(@fire_source_id, 'LIGHTNING', '雷击', '雷电引起', 60, 0),
(@fire_source_id, 'ARSON', '纵火', '人为纵火', 70, 0),
(@fire_source_id, 'OTHER', '其他', '其他原因', 80, 0),
(@fire_source_id, 'UNKNOWN', '不明', '原因不明', 90, 0);

-- 3. 燃烧物质
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@burning_material_id, 'WOOD', '木材', '木制品、家具等', 10, 0),
(@burning_material_id, 'PLASTIC', '塑料', '塑料制品', 20, 0),
(@burning_material_id, 'OIL', '油类', '汽油、柴油等', 30, 0),
(@burning_material_id, 'ELECTRICAL', '电器', '电器设备', 40, 0),
(@burning_material_id, 'CHEMICAL', '化学品', '化学物质', 50, 0),
(@burning_material_id, 'FABRIC', '纺织品', '衣物、布料等', 60, 0),
(@burning_material_id, 'PAPER', '纸类', '纸张、书籍等', 70, 0),
(@burning_material_id, 'OTHER', '其他', '其他燃烧物', 80, 0);

-- 4. 烟雾情况
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@smoke_condition_id, 'NO_SMOKE', '无烟', '无明显烟雾', 10, 0),
(@smoke_condition_id, 'LIGHT', '轻微', '轻微烟雾', 20, 0),
(@smoke_condition_id, 'MODERATE', '中等', '中等烟雾', 30, 0),
(@smoke_condition_id, 'HEAVY', '浓烟', '浓烟滚滚', 40, 0),
(@smoke_condition_id, 'TOXIC', '有毒', '有毒烟雾', 50, 0);

-- 5. 被困人数
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@trapped_count_id, '0', '无', '无人被困', 10, 0),
(@trapped_count_id, '1-3', '1-3人', '少量人员被困', 20, 0),
(@trapped_count_id, '4-10', '4-10人', '多人被困', 30, 0),
(@trapped_count_id, '10+', '10人以上', '大量人员被困', 40, 0);

-- 6. 伤亡情况
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@casualties_level_id, 'NONE', '无伤亡', '无人员伤亡', 10, 0),
(@casualties_level_id, 'MINOR_INJURY', '轻伤', '轻微伤', 20, 0),
(@casualties_level_id, 'SERIOUS_INJURY', '重伤', '重伤', 30, 0),
(@casualties_level_id, 'DEATH', '死亡', '有人员死亡', 40, 0);

-- 7. 事故类型
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@accident_type_id, 'REAR_END', '追尾', '车辆追尾碰撞', 10, 0),
(@accident_type_id, 'ROLLOVER', '侧翻', '车辆侧翻', 20, 0),
(@accident_type_id, 'HEAD_ON', '正面碰撞', '正面相撞', 30, 0),
(@accident_type_id, 'SIDE_IMPACT', '侧面碰撞', '侧面碰撞', 40, 0),
(@accident_type_id, 'FIXED_OBJECT', '撞固定物', '撞击固定物体', 50, 0),
(@accident_type_id, 'HIT_PEDESTRIAN', '撞行人', '车撞行人', 60, 0),
(@accident_type_id, 'MULTI_VEHICLE', '多车连环', '多车连环相撞', 70, 0);

-- 8. 车辆数量
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@vehicle_count_id, '1', '1辆', '单车事故', 10, 0),
(@vehicle_count_id, '2', '2辆', '双车事故', 20, 0),
(@vehicle_count_id, '3', '3辆', '三车事故', 30, 0),
(@vehicle_count_id, '3+', '3辆以上', '多车事故', 40, 0);

-- 9. 道路阻塞程度
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@road_block_level_id, 'NO_BLOCK', '无阻塞', '道路畅通', 10, 0),
(@road_block_level_id, 'PARTIAL', '部分阻塞', '部分车道阻塞', 20, 0),
(@road_block_level_id, 'COMPLETE', '完全阻塞', '道路完全堵塞', 30, 0);

-- 10. 路面状况
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@road_condition_id, 'DRY', '干燥', '路面干燥', 10, 0),
(@road_condition_id, 'WET', '湿滑', '路面湿滑', 20, 0),
(@road_condition_id, 'WATER', '积水', '路面积水', 30, 0),
(@road_condition_id, 'ICE', '结冰', '路面结冰', 40, 0),
(@road_condition_id, 'CONSTRUCTION', '施工', '道路施工', 50, 0);

-- 11. 天气情况
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@weather_condition_id, 'SUNNY', '晴朗', '天气晴朗', 10, 0),
(@weather_condition_id, 'CLOUDY', '阴天', '阴天', 20, 0),
(@weather_condition_id, 'LIGHT_RAIN', '小雨', '小雨', 30, 0),
(@weather_condition_id, 'HEAVY_RAIN', '大雨', '大雨', 40, 0),
(@weather_condition_id, 'FOG', '雾天', '有雾', 50, 0),
(@weather_condition_id, 'SNOW', '雪天', '下雪', 60, 0);

-- 12. 救援类型
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@rescue_type_id, 'HIGH_ALTITUDE', '高空救援', '高空作业救援', 10, 0),
(@rescue_type_id, 'WATER', '水域救援', '水上救援', 20, 0),
(@rescue_type_id, 'GEOLOGICAL', '地质灾害', '山体滑坡等', 30, 0),
(@rescue_type_id, 'BUILDING_COLLAPSE', '建筑坍塌', '建筑物坍塌救援', 40, 0),
(@rescue_type_id, 'EQUIPMENT_FAILURE', '设备故障', '设备故障救援', 50, 0),
(@rescue_type_id, 'TRAPPED_PERSON', '人员被困', '人员被困救援', 60, 0);

-- 13. 危险等级
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@danger_level_id, 'LOW', '低危', '危险程度较低', 10, 0),
(@danger_level_id, 'MEDIUM', '中危', '危险程度中等', 20, 0),
(@danger_level_id, 'HIGH', '高危', '危险程度较高', 30, 0),
(@danger_level_id, 'EXTREME', '极危', '危险程度极高', 40, 0);

-- 14. 救援设备
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@rescue_equipment_id, 'FIRE_TRUCK', '消防车', '消防车辆', 10, 0),
(@rescue_equipment_id, 'LADDER_TRUCK', '云梯车', '高空作业车', 20, 0),
(@rescue_equipment_id, 'AMBULANCE', '救护车', '医疗救护车', 30, 0),
(@rescue_equipment_id, 'RESCUE_TRUCK', '抢险车', '抢险救援车', 40, 0),
(@rescue_equipment_id, 'WATER_TRUCK', '供水车', '供水车辆', 50, 0),
(@rescue_equipment_id, 'CRANE', '吊车', '起重设备', 60, 0),
(@rescue_equipment_id, 'EXCAVATOR', '挖掘机', '挖掘设备', 70, 0),
(@rescue_equipment_id, 'CUTTING_TOOL', '切割设备', '切割工具', 80, 0);

-- 15. 伤情等级
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@injury_level_id, 'MINOR', '轻伤', '轻微伤', 10, 0),
(@injury_level_id, 'MODERATE', '中度伤', '中度伤', 20, 0),
(@injury_level_id, 'SERIOUS', '重伤', '重伤', 30, 0),
(@injury_level_id, 'CRITICAL', '危重', '危重伤', 40, 0),
(@injury_level_id, 'DEATH', '死亡', '死亡', 50, 0);

-- 16. 意识状态
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@consciousness_state_id, 'CLEAR', '清醒', '意识清醒', 10, 0),
(@consciousness_state_id, 'CONFUSED', '模糊', '意识模糊', 20, 0),
(@consciousness_state_id, 'COMA', '昏迷', '昏迷状态', 30, 0),
(@consciousness_state_id, 'UNKNOWN', '不明', '意识状态不明', 40, 0);

-- 17. 急救类型
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@emergency_type_id, 'TRAUMA', '外伤', '外伤急救', 10, 0),
(@emergency_type_id, 'INTERNAL', '内科急症', '内科疾病', 20, 0),
(@emergency_type_id, 'POISONING', '中毒', '中毒事件', 30, 0),
(@emergency_type_id, 'DROWNING', '溺水', '溺水急救', 40, 0),
(@emergency_type_id, 'HEART_DISEASE', '心脏病', '心脏疾病', 50, 0),
(@emergency_type_id, 'STROKE', '脑血管', '脑血管疾病', 60, 0),
(@emergency_type_id, 'OTHER', '其他', '其他急救', 70, 0);

-- 18. 事件性质
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@security_event_nature_id, 'FIGHT', '打架斗殴', '打架斗殴事件', 10, 0),
(@security_event_nature_id, 'THEFT', '盗窃', '盗窃案件', 20, 0),
(@security_event_nature_id, 'FRAUD', '诈骗', '诈骗案件', 30, 0),
(@security_event_nature_id, 'DISTURBANCE', '扰民', '扰民事件', 40, 0),
(@security_event_nature_id, 'CROWD_TROUBLE', '聚众闹事', '聚众闹事', 50, 0),
(@security_event_nature_id, 'OTHER', '其他', '其他治安事件', 60, 0);

-- 19. 涉械类型
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@weapon_type_id, 'NONE', '无', '无涉械', 10, 0),
(@weapon_type_id, 'KNIFE', '管制刀具', '刀具类', 20, 0),
(@weapon_type_id, 'STICK', '棍棒', '棍棒类', 30, 0),
(@weapon_type_id, 'GUN', '枪支', '枪支类', 40, 0),
(@weapon_type_id, 'OTHER', '其他', '其他器械', 50, 0);

-- 20. 案件性质
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@criminal_case_nature_id, 'ROBBERY', '抢劫', '抢劫案件', 10, 0),
(@criminal_case_nature_id, 'MURDER', '杀人', '故意杀人', 20, 0),
(@criminal_case_nature_id, 'KIDNAPPING', '绑架', '绑架案件', 30, 0),
(@criminal_case_nature_id, 'EXPLOSION', '爆炸', '爆炸案件', 40, 0),
(@criminal_case_nature_id, 'POISONING', '投毒', '投毒案件', 50, 0),
(@criminal_case_nature_id, 'ARSON', '纵火', '纵火案件', 60, 0),
(@criminal_case_nature_id, 'OTHER', '其他', '其他刑事案件', 70, 0);

-- 21. 紧急程度
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@urgency_level_id, 'LOW', '低', '一般事件', 10, 0),
(@urgency_level_id, 'MEDIUM', '中', '中等紧急', 20, 0),
(@urgency_level_id, 'HIGH', '高', '高度紧急', 30, 0),
(@urgency_level_id, 'URGENT', '紧急', '紧急事件', 40, 0),
(@urgency_level_id, 'CRITICAL', '特急', '特别紧急', 50, 0);

-- 22. 嫌疑人状态
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@suspect_status_id, 'ON_SCENE', '在场', '嫌疑人在现场', 10, 0),
(@suspect_status_id, 'ESCAPED', '逃离', '嫌疑人已逃离', 20, 0),
(@suspect_status_id, 'UNKNOWN', '不明', '嫌疑人状态不明', 30, 0);

-- 23. 现场保护
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@scene_protection_id, 'PROTECTED', '已保护', '现场已保护', 10, 0),
(@scene_protection_id, 'NOT_PROTECTED', '未保护', '现场未保护', 20, 0),
(@scene_protection_id, 'DAMAGED', '被破坏', '现场被破坏', 30, 0);

-- 24. 灾害类型
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@disaster_type_id, 'FLOOD', '洪水', '洪水灾害', 10, 0),
(@disaster_type_id, 'EARTHQUAKE', '地震', '地震灾害', 20, 0),
(@disaster_type_id, 'TYPHOON', '台风', '台风灾害', 30, 0),
(@disaster_type_id, 'MUDSLIDE', '泥石流', '泥石流灾害', 40, 0),
(@disaster_type_id, 'LANDSLIDE', '山体滑坡', '山体滑坡', 50, 0),
(@disaster_type_id, 'HAIL', '冰雹', '冰雹灾害', 60, 0),
(@disaster_type_id, 'TORNADO', '龙卷风', '龙卷风灾害', 70, 0);

-- 25. 影响范围
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@affected_area_id, 'LOCAL', '局部', '局部范围', 10, 0),
(@affected_area_id, 'SMALL', '小范围', '小范围影响', 20, 0),
(@affected_area_id, 'LARGE', '大范围', '大范围影响', 30, 0),
(@affected_area_id, 'REGION', '全区域', '整个区域', 40, 0);

-- 26. 预警等级
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@disaster_warning_level_id, 'BLUE', 'Ⅳ级(蓝色)', '蓝色预警', 10, 0),
(@disaster_warning_level_id, 'YELLOW', 'Ⅲ级(黄色)', '黄色预警', 20, 0),
(@disaster_warning_level_id, 'ORANGE', 'Ⅱ级(橙色)', '橙色预警', 30, 0),
(@disaster_warning_level_id, 'RED', 'Ⅰ级(红色)', '红色预警', 40, 0);

-- 27. 救援需求
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@rescue_needs_id, 'EVACUATION', '人员疏散', '需要人员疏散', 10, 0),
(@rescue_needs_id, 'MEDICAL', '医疗救援', '需要医疗救援', 20, 0),
(@rescue_needs_id, 'SUPPLIES', '物资补给', '需要物资补给', 30, 0),
(@rescue_needs_id, 'TRAFFIC', '交通疏导', '需要交通疏导', 40, 0),
(@rescue_needs_id, 'COMMUNICATION', '通信保障', '需要通信保障', 50, 0);

-- 28. 是否
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@yes_no_id, 'YES', '是', '是', 10, 0),
(@yes_no_id, 'NO', '否', '否', 20, 0);

-- 29. 涉及人数
INSERT INTO `t_dict_data` (`dict_id`, `data_value`, `data_label`, `remark`, `sort_order`, `disabled_flag`) VALUES
(@involvement_count_id, 'NONE', '不涉及', '不涉及人员', 10, 0),
(@involvement_count_id, '1', '1人', '涉及1人', 20, 0),
(@involvement_count_id, '2-5', '2-5人', '涉及2-5人', 30, 0),
(@involvement_count_id, '6-10', '6-10人', '涉及6-10人', 40, 0),
(@involvement_count_id, '10+', '10人以上', '涉及10人以上', 50, 0);

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- 执行完成提示
-- ----------------------------
SELECT '警务系统数据字典导入完成！' AS message;
SELECT CONCAT('共导入 ', COUNT(*), ' 个字典分类') AS dict_count FROM t_dict WHERE dict_code LIKE '%FIRE%' OR dict_code LIKE '%TRAPPED%' OR dict_code LIKE '%ACCIDENT%' OR dict_code LIKE '%RESCUE%' OR dict_code LIKE '%EMERGENCY%' OR dict_code LIKE '%SECURITY%' OR dict_code LIKE '%CRIMINAL%' OR dict_code LIKE '%DISASTER%' OR dict_code LIKE '%YES_NO%';
SELECT CONCAT('共导入 ', COUNT(*), ' 条字典数据') AS dict_data_count FROM t_dict_data WHERE dict_id IN (SELECT dict_id FROM t_dict WHERE dict_code LIKE '%FIRE%' OR dict_code LIKE '%TRAPPED%' OR dict_code LIKE '%ACCIDENT%' OR dict_code LIKE '%RESCUE%' OR dict_code LIKE '%EMERGENCY%' OR dict_code LIKE '%SECURITY%' OR dict_code LIKE '%CRIMINAL%' OR dict_code LIKE '%DISASTER%' OR dict_code LIKE '%YES_NO%');