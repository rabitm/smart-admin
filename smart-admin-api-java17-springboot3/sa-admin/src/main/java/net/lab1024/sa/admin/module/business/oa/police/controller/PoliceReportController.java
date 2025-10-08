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
import net.lab1024.sa.admin.module.business.oa.police.domain.vo.DataVersionVO;
import net.lab1024.sa.admin.module.business.oa.police.service.PoliceReportService;
import net.lab1024.sa.admin.module.business.oa.police.service.PoliceEditLockService;
import net.lab1024.sa.admin.module.business.oa.police.service.sync.RocketMQSyncServiceImpl;
import net.lab1024.sa.base.common.domain.PageResult;
import net.lab1024.sa.base.common.domain.RequestUser;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.common.util.SmartRequestUtil;
import net.lab1024.sa.base.module.support.operatelog.annotation.OperateLog;
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
@OperateLog
public class PoliceReportController {

    @Resource
    private PoliceReportService policeReportService;

    @Resource
    private PoliceEditLockService policeEditLockService;

    @Resource(name = "rocketMQSyncService")
    private RocketMQSyncServiceImpl rocketMQSyncService;

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

    // ========== 警情编辑锁相关接口 ==========

    @Operation(summary = "锁定警情（获取编辑权限）")
    @PostMapping("/oa/police/report/lock/{reportId}")
    @SaCheckPermission("oa:police:update")
    public ResponseDTO<String> lockPoliceCase(@PathVariable Long reportId, @RequestParam(required = false) Long seatId) {
        return policeEditLockService.lockPoliceCase(reportId, seatId);
    }

    @Operation(summary = "解锁警情（释放编辑权限）")
    @PostMapping("/oa/police/report/unlock/{reportId}")
    @SaCheckPermission("oa:police:update")
    public ResponseDTO<String> unlockPoliceCase(@PathVariable Long reportId) {
        return policeEditLockService.unlockPoliceCase(reportId);
    }

    @Operation(summary = "检查警情是否被锁定")
    @GetMapping("/oa/police/report/check-lock/{reportId}")
    @SaCheckPermission("oa:police:query")
    public ResponseDTO<Boolean> checkPoliceCaseLock(@PathVariable Long reportId) {
        return policeEditLockService.checkPoliceCaseLock(reportId);
    }

    @Operation(summary = "同步警情字段更新")
    @PostMapping("/oa/police/report/sync-field/{reportId}")
    @SaCheckPermission("oa:police:update")
    public ResponseDTO<String> syncFieldUpdate(
            @PathVariable Long reportId,
            @RequestParam String fieldName,
            @RequestParam(required = false) String fieldValue,
            @RequestParam(required = false) String oldValue) {

        // 如果前端没有传递旧值，则从数据库获取（兼容性考虑）
        String actualOldValue = oldValue;
        if (actualOldValue == null) {
            actualOldValue = policeReportService.getFieldValue(reportId, fieldName);
        }

        // 调用修改后的方法，传入旧值和新值
        return policeReportService.syncFieldUpdateWithOldValue(reportId, fieldName, actualOldValue, fieldValue);
    }

    // ========== 轻量级数据同步检查接口 ==========

    @Operation(summary = "检查数据版本 - 轻量级同步检查")
    @GetMapping("/oa/police/report/check-version")
    @SaCheckPermission("oa:police:query")
    public ResponseDTO<DataVersionVO> checkDataVersion(@RequestParam(required = false) String clientVersion) {
        return policeReportService.checkDataVersion(clientVersion);
    }

    @Operation(summary = "检查数据版本 - 带分页信息")
    @GetMapping("/oa/police/report/check-version-with-page")
    @SaCheckPermission("oa:police:query")
    public ResponseDTO<DataVersionVO> checkDataVersionWithPage(
            @RequestParam(required = false) String clientVersion,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return policeReportService.checkDataVersionWithPage(clientVersion, pageNum, pageSize);
    }

    // ========== RocketMQ协同功能端点 ==========

    @Operation(summary = "用户协同状态同步")
    @PostMapping("/oa/police/report/collaboration/user-state")
    @SaCheckPermission("oa:police:update")
    public ResponseDTO<String> syncUserCollaborationState(
            @RequestParam Long reportId,
            @RequestParam String action) {
        try {
            RequestUser currentUser = SmartRequestUtil.getRequestUser();
            rocketMQSyncService.syncUserCollaborationState(reportId, currentUser.getUserId(),
                    currentUser.getUserName(), action);
            return ResponseDTO.ok("用户协同状态同步成功");
        } catch (Exception e) {
            return ResponseDTO.userErrorParam("用户协同状态同步失败: " + e.getMessage());
        }
    }

    @Operation(summary = "多字段锁定管理")
    @PostMapping("/oa/police/report/collaboration/multi-field-lock")
    @SaCheckPermission("oa:police:update")
    public ResponseDTO<String> syncMultiFieldLock(
            @RequestParam Long reportId,
            @RequestParam String[] fieldNames,
            @RequestParam String action) {
        try {
            RequestUser currentUser = SmartRequestUtil.getRequestUser();
            rocketMQSyncService.syncMultiFieldLock(reportId, currentUser.getUserId(),
                    currentUser.getUserName(), fieldNames, action);
            return ResponseDTO.ok("多字段锁定同步成功");
        } catch (Exception e) {
            return ResponseDTO.userErrorParam("多字段锁定同步失败: " + e.getMessage());
        }
    }

    @Operation(summary = "用户活跃度同步")
    @PostMapping("/oa/police/report/collaboration/user-activity")
    @SaCheckPermission("oa:police:update")
    public ResponseDTO<String> syncUserActivity(
            @RequestParam Long reportId,
            @RequestParam String activityType) {
        try {
            RequestUser currentUser = SmartRequestUtil.getRequestUser();
            rocketMQSyncService.syncUserActivity(reportId, currentUser.getUserId(),
                    currentUser.getUserName(), activityType);
            return ResponseDTO.ok("用户活跃度同步成功");
        } catch (Exception e) {
            return ResponseDTO.userErrorParam("用户活跃度同步失败: " + e.getMessage());
        }
    }

    @Operation(summary = "批量字段更新")
    @PostMapping("/oa/police/report/collaboration/batch-field-update")
    @SaCheckPermission("oa:police:update")
    public ResponseDTO<String> syncBatchFieldUpdate(
            @RequestParam Long reportId,
            @RequestBody Map<String, String> fieldUpdates,
            @RequestParam(defaultValue = "BATCH_UPDATE") String operationType) {
        try {
            RequestUser currentUser = SmartRequestUtil.getRequestUser();
            rocketMQSyncService.syncBatchFieldUpdate(reportId, currentUser.getUserId(),
                    currentUser.getUserName(), fieldUpdates, operationType);
            return ResponseDTO.ok("批量字段更新同步成功");
        } catch (Exception e) {
            return ResponseDTO.userErrorParam("批量字段更新同步失败: " + e.getMessage());
        }
    }

    @Operation(summary = "RocketMQ健康状态检查")
    @GetMapping("/oa/police/report/collaboration/health-status")
    @SaCheckPermission("oa:police:query")
    public ResponseDTO<Map<String, Object>> getRocketMQHealthStatus() {
        try {
            Map<String, Object> healthStatus = rocketMQSyncService.getHealthStatus();
            return ResponseDTO.ok(healthStatus);
        } catch (Exception e) {
            return ResponseDTO.userErrorParam("健康状态检查失败: " + e.getMessage());
        }
    }

}