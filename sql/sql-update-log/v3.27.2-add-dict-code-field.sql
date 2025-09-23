-- 为警情表单字段表添加数据字典编码支持
-- v3.27.2 更新

-- 添加 dict_code 字段到 t_police_form_field 表
ALTER TABLE `t_police_form_field`
ADD COLUMN `dict_code` varchar(100) DEFAULT NULL COMMENT '数据字典编码'
AFTER `quick_options`;

-- 输出结果
SELECT '✅ 成功为 t_police_form_field 表添加 dict_code 字段，支持数据字典配置' as result;