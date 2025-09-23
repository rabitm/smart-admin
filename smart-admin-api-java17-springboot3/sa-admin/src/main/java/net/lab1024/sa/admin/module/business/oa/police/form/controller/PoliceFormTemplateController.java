package net.lab1024.sa.admin.module.business.oa.police.form.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.lab1024.sa.admin.module.business.oa.police.form.domain.form.PoliceFormFieldSaveForm;
import net.lab1024.sa.admin.module.business.oa.police.form.domain.form.PoliceFormTemplateAddForm;
import net.lab1024.sa.admin.module.business.oa.police.form.domain.vo.PoliceFormConfigVO;
import net.lab1024.sa.admin.module.business.oa.police.form.domain.vo.PoliceFormTemplateVO;
import net.lab1024.sa.admin.module.business.oa.police.form.service.PoliceFormTemplateService;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 警情表单模板管理控制器
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-22
 * @Copyright 1024创新实验室 （ https://1024lab.net ），Since 2012
 */
@RestController
@RequestMapping("/api/business/oa/police/form/template")
@Tag(name = "警情表单模板管理")
public class PoliceFormTemplateController {

    @Autowired
    private PoliceFormTemplateService policeFormTemplateService;

    @Operation(summary = "获取模板列表")
    @GetMapping("/list")
    public ResponseDTO<List<PoliceFormTemplateVO>> getTemplateList() {
        return policeFormTemplateService.getTemplateList();
    }

    @Operation(summary = "创建模板")
    @PostMapping("/create")
    public ResponseDTO<String> createTemplate(@RequestBody @Valid PoliceFormTemplateAddForm form) {
        return policeFormTemplateService.createTemplate(form);
    }

    @Operation(summary = "获取模板详情")
    @GetMapping("/detail/{templateId}")
    public ResponseDTO<PoliceFormConfigVO> getTemplateDetail(@PathVariable Long templateId) {
        return policeFormTemplateService.getTemplateDetail(templateId);
    }

    @Operation(summary = "保存字段配置")
    @PostMapping("/fields/save")
    public ResponseDTO<String> saveFields(@RequestBody @Valid PoliceFormFieldSaveForm form) {
        return policeFormTemplateService.saveFields(form);
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/{templateId}")
    public ResponseDTO<String> deleteTemplate(@PathVariable Long templateId) {
        return policeFormTemplateService.deleteTemplate(templateId);
    }
}