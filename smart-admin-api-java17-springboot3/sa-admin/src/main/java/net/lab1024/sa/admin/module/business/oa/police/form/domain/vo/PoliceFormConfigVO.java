package net.lab1024.sa.admin.module.business.oa.police.form.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 警情表单配置VO
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-22
 * @Copyright 1024创新实验室 （ https://1024lab.net ），Since 2012
 */
@Data
@Schema(description = "警情表单配置")
public class PoliceFormConfigVO {

    @Schema(description = "警情类型")
    private Integer reportType;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "第二步字段")
    private List<PoliceFormFieldVO> step2Fields;

    @Schema(description = "第三步字段")
    private List<PoliceFormFieldVO> step3Fields;

    @Data
    @Schema(description = "表单字段")
    public static class PoliceFormFieldVO {

        @Schema(description = "字段ID")
        private Long id;

        @Schema(description = "字段标识")
        private String key;

        @Schema(description = "字段名称")
        private String label;

        @Schema(description = "字段类型")
        private String type;

        @Schema(description = "是否必填")
        private Boolean required;

        @Schema(description = "字段图标")
        private String icon;

        @Schema(description = "占位符")
        private String placeholder;

        @Schema(description = "字段选项")
        private List<String> options;

        @Schema(description = "快捷选项")
        private List<String> quickOptions;

        @Schema(description = "数据字典编码")
        private String dictCode;

        @Schema(description = "默认值")
        private String defaultValue;

        @Schema(description = "子字段列表（用于组合字段）")
        private List<PoliceFormFieldVO> fields;

        @Schema(description = "排序")
        private Integer sortOrder;

        @Schema(description = "验证规则")
        private String validationRules;
    }
}