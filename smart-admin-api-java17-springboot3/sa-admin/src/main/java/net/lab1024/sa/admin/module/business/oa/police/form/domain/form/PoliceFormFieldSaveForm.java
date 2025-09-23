package net.lab1024.sa.admin.module.business.oa.police.form.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 警情表单字段保存表单
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-22
 * @Copyright 1024创新实验室 （ https://1024lab.net ），Since 2012
 */
@Data
@Schema(description = "警情表单字段保存")
public class PoliceFormFieldSaveForm {

    @Schema(description = "模板ID")
    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    @Schema(description = "字段列表")
    @Valid
    private List<FieldItem> fields;

    @Data
    @Schema(description = "字段项")
    public static class FieldItem {

        @Schema(description = "字段ID，新增时为空")
        private Long id;

        @Schema(description = "字段标识")
        private String fieldKey;

        @Schema(description = "字段名称")
        private String fieldLabel;

        @Schema(description = "字段类型")
        private String fieldType;

        @Schema(description = "是否必填")
        private Boolean isRequired = false;

        @Schema(description = "字段图标")
        private String fieldIcon;

        @Schema(description = "占位符")
        private String placeholder;

        @Schema(description = "字段选项")
        private List<String> fieldOptions;

        @Schema(description = "快捷选项")
        private List<String> quickOptions;

        @Schema(description = "数据字典编码")
        private String dictCode;

        @Schema(description = "父字段ID")
        private Long parentFieldId;

        @Schema(description = "排序")
        private Integer sortOrder;

        @Schema(description = "步骤号")
        private Integer stepNumber;

        @Schema(description = "验证规则")
        private String validationRules;

        @Schema(description = "默认值")
        private String defaultValue;

        @Schema(description = "子字段列表")
        private List<FieldItem> children;
    }
}