package net.lab1024.sa.admin.module.business.oa.police.form.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.lab1024.sa.admin.module.business.oa.police.form.domain.vo.PoliceFormConfigVO;
import net.lab1024.sa.admin.module.business.oa.police.form.service.PoliceFormConfigService;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.common.domain.RequestUser;
import net.lab1024.sa.base.common.util.SmartRequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 警情表单配置控制器
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-22
 * @Copyright 1024创新实验室 （ https://1024lab.net ），Since 2012
 */
@RestController
@RequestMapping("/api/business/oa/police/form")
@Tag(name = "警情表单配置")
public class PoliceFormConfigController {

    @Autowired
    private PoliceFormConfigService policeFormConfigService;

    @Operation(summary = "获取表单配置")
    @GetMapping("/config/{reportType}")
    public ResponseDTO<PoliceFormConfigVO> getFormConfig(@PathVariable Integer reportType) {
        RequestUser requestUser = SmartRequestUtil.getRequestUser();
        Long organizationId = null;

        // 如果是员工用户，获取部门ID作为组织ID
        if (requestUser instanceof net.lab1024.sa.admin.module.system.login.domain.RequestEmployee) {
            net.lab1024.sa.admin.module.system.login.domain.RequestEmployee employee =
                (net.lab1024.sa.admin.module.system.login.domain.RequestEmployee) requestUser;
            organizationId = employee.getDepartmentId();
        }

        return policeFormConfigService.getFormConfig(reportType, organizationId);
    }

    @Operation(summary = "获取表单配置（带组织ID）")
    @GetMapping("/config/{reportType}/{organizationId}")
    public ResponseDTO<PoliceFormConfigVO> getFormConfigWithOrg(@PathVariable Integer reportType,
                                                               @PathVariable Long organizationId) {
        return policeFormConfigService.getFormConfig(reportType, organizationId);
    }
}