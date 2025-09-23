package net.lab1024.sa.admin.module.business.oa.police.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import net.lab1024.sa.admin.constant.AdminSwaggerTagConst;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.PoliceReportAddForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.PoliceReportQueryForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.PoliceReportUpdateForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.vo.PoliceReportVO;
import net.lab1024.sa.admin.module.business.oa.police.service.PoliceReportService;
import net.lab1024.sa.base.common.domain.PageResult;
import net.lab1024.sa.base.common.domain.RequestUser;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.common.util.SmartRequestUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 警情录入Controller
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-18
 */
@RestController
@Tag(name = "警情录入管理")
public class PoliceReportController {

    @Resource
    private PoliceReportService policeReportService;

    @Operation(summary = "分页查询警情信息")
    @PostMapping("/oa/police/report/page/query")
    @SaCheckPermission("oa:police:query")
    public ResponseDTO<PageResult<PoliceReportVO>> queryByPage(@RequestBody @Valid PoliceReportQueryForm queryForm) {
        return policeReportService.queryByPage(queryForm);
    }

    @Operation(summary = "查询警情信息详情")
    @GetMapping("/oa/police/report/get/{reportId}")
    @SaCheckPermission("oa:police:query")
    public ResponseDTO<PoliceReportVO> getDetail(@PathVariable Long reportId) {
        return policeReportService.getDetail(reportId);
    }

    @Operation(summary = "新增警情信息")
    @PostMapping("/oa/police/report/add")
    @SaCheckPermission("oa:police:add")
    public ResponseDTO<String> addPoliceReport(@RequestBody @Valid PoliceReportAddForm addForm) {
        RequestUser requestUser = SmartRequestUtil.getRequestUser();
        addForm.setCreateUserId(requestUser.getUserId());
        addForm.setCreateUserName(requestUser.getUserName());
        return policeReportService.addPoliceReport(addForm);
    }

    @Operation(summary = "更新警情信息")
    @PostMapping("/oa/police/report/update")
    @SaCheckPermission("oa:police:update")
    public ResponseDTO<String> updatePoliceReport(@RequestBody @Valid PoliceReportUpdateForm updateForm) {
        return policeReportService.updatePoliceReport(updateForm);
    }

    @Operation(summary = "删除警情信息")
    @GetMapping("/oa/police/report/delete/{reportId}")
    @SaCheckPermission("oa:police:delete")
    public ResponseDTO<String> deletePoliceReport(@PathVariable Long reportId) {
        return policeReportService.deletePoliceReport(reportId);
    }

    @Operation(summary = "根据报警人电话查询警情列表")
    @GetMapping("/oa/police/report/query/by-phone/{reporterPhone}")
    @SaCheckPermission("oa:police:query")
    public ResponseDTO<List<PoliceReportVO>> queryByReporterPhone(@PathVariable String reporterPhone) {
        return policeReportService.queryByReporterPhone(reporterPhone);
    }

    @Operation(summary = "根据处理人员查询警情列表")
    @GetMapping("/oa/police/report/query/by-handler/{handlerId}")
    @SaCheckPermission("oa:police:query")
    public ResponseDTO<List<PoliceReportVO>> queryByHandler(@PathVariable Long handlerId) {
        return policeReportService.queryByHandler(handlerId);
    }

    @Operation(summary = "获取各状态警情统计")
    @GetMapping("/oa/police/report/statistics/status")
    @SaCheckPermission("oa:police:query")
    public ResponseDTO<List<PoliceReportVO>> getStatusStatistics() {
        return policeReportService.getStatusStatistics();
    }

    @Operation(summary = "获取地址建议")
    @GetMapping("/oa/police/report/location/search")
    @SaCheckPermission("oa:police:add")
    public ResponseDTO<List<String>> searchLocationSuggestions(@RequestParam String keyword) {
        return policeReportService.searchLocationSuggestions(keyword);
    }

    @Operation(summary = "测试地址建议（无权限）")
    @GetMapping("/oa/police/report/location/test")
    @SaIgnore
    public ResponseDTO<List<String>> testLocationSuggestions(@RequestParam String keyword) {
        List<String> testData = List.of(
            keyword + "街道",
            keyword + "大道",
            keyword + "路",
            keyword + "小区",
            keyword + "广场"
        );
        return ResponseDTO.ok(testData);
    }

    @Operation(summary = "获取警情专业字段数据")
    @GetMapping("/oa/police/report/field-data/{reportId}")
    @SaCheckPermission("oa:police:query")
    public ResponseDTO<Map<String, Object>> getPoliceReportFieldData(@PathVariable Long reportId) {
        return policeReportService.getPoliceReportFieldData(reportId);
    }

}