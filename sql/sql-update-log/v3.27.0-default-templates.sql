-- 插入默认的火灾事故表单模板
-- @Author: Claude Code Assistant
-- @Date: 2025-09-22

-- 插入火灾事故默认模板
INSERT IGNORE INTO t_police_form_template (
    template_name, report_type, organization_id, description, status, create_user_id, create_time, update_time
) VALUES (
    '火灾事故默认模板', 1, NULL, '火灾事故的默认表单模板', 1, 1, NOW(), NOW()
);

-- 获取模板ID
SET @template_id = (SELECT id FROM t_police_form_template WHERE report_type = 1 AND organization_id IS NULL LIMIT 1);

-- 插入默认字段
INSERT IGNORE INTO t_police_form_field (
    template_id, field_key, field_label, field_type, is_required, field_icon, placeholder,
    field_options, quick_options, parent_field_id, sort_order, step_number, validation_rules,
    default_value, status, create_user_id, create_time, update_time
) VALUES
-- 步骤2字段（基本信息）
(@template_id, 'fireLocation', '起火地点', 'input', 1, '📍', '请输入详细地址', NULL, NULL, NULL, 1, 2, NULL, NULL, 1, 1, NOW(), NOW()),
(@template_id, 'fireScale', '火势程度', 'select', 1, '🔥', NULL,
 '["轻微火情", "一般火灾", "较大火灾", "重大火灾", "特别重大火灾"]',
 '["轻微", "一般", "较大", "重大"]', NULL, 2, 2, NULL, NULL, 1, 1, NOW(), NOW()),
(@template_id, 'peopleInfo', '人员情况', 'compact-group', 1, '👥', NULL, NULL, NULL, NULL, 3, 2, NULL, NULL, 1, 1, NOW(), NOW()),

-- 步骤3字段（详细信息）
(@template_id, 'fireSource', '起火原因', 'select', 0, '🔍', NULL,
 '["电气故障", "用火不慎", "吸烟", "玩火", "自燃", "雷击", "其他", "不明"]',
 '["电气", "用火", "吸烟", "其他"]', NULL, 1, 3, NULL, NULL, 1, 1, NOW(), NOW()),
(@template_id, 'burnArea', '过火面积', 'input', 0, '📏', '平方米', NULL,
 '["<10平方米", "10-100平方米", "100-1000平方米", ">1000平方米"]', NULL, 2, 3, NULL, NULL, 1, 1, NOW(), NOW()),
(@template_id, 'economicLoss', '经济损失', 'select', 0, '💰', NULL,
 '["无损失", "轻微损失(<1万)", "一般损失(1-10万)", "较大损失(10-100万)", "重大损失(>100万)"]',
 '["无", "轻微", "一般", "较大", "重大"]', NULL, 3, 3, NULL, NULL, 1, 1, NOW(), NOW()),
(@template_id, 'rescueStatus', '救援状态', 'checkbox-compact', 0, '🚒', NULL, NULL,
 '["已派出", "到达现场", "开始救援", "火势控制", "完全扑灭"]', NULL, 4, 3, NULL, NULL, 1, 1, NOW(), NOW());

-- 获取人员情况字段的ID，插入子字段
SET @people_info_id = (SELECT id FROM t_police_form_field WHERE template_id = @template_id AND field_key = 'peopleInfo' LIMIT 1);

-- 插入人员情况的子字段
INSERT IGNORE INTO t_police_form_field (
    template_id, field_key, field_label, field_type, is_required, field_icon, placeholder,
    field_options, quick_options, parent_field_id, sort_order, step_number, validation_rules,
    default_value, status, create_user_id, create_time, update_time
) VALUES
(@template_id, 'trappedCount', '被困人数', 'select', 0, '🔒', NULL, NULL,
 '["0", "1-3人", "4-10人", "10+人"]', @people_info_id, 1, 2, NULL, '0', 1, 1, NOW(), NOW()),
(@template_id, 'casualties', '伤亡情况', 'select', 0, '🏥', NULL, NULL,
 '["无", "轻伤", "重伤", "死亡"]', @people_info_id, 2, 2, NULL, '无', 1, 1, NOW(), NOW()),
(@template_id, 'evacuated', '疏散人数', 'input', 0, '🏃', '人', NULL,
 '["0", "1-10人", "11-50人", "50+人"]', @people_info_id, 3, 2, NULL, '0', 1, 1, NOW(), NOW());

-- 输出结果
SELECT
    CONCAT('✅ 成功创建火灾事故默认模板，模板ID: ', @template_id, '，共创建 ',
    (SELECT COUNT(*) FROM t_police_form_field WHERE template_id = @template_id), ' 个字段') as result;