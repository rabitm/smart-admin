package net.lab1024.sa.admin.module.support.im.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.im.domain.form.GetHistoryMessagesForm;
import net.lab1024.sa.admin.module.support.im.domain.form.SaveMessageForm;
import net.lab1024.sa.admin.module.support.im.service.IMBusinessService;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.common.util.SmartRequestUtil;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

/**
 * IM 业务集成控制器
 *
 * 提供业务相关的 IM 操作接口
 *
 * 用途:
 * - 后端业务逻辑触发的 IM 操作
 * - 群组生命周期管理
 * - 业务数据查询
 *
 * 注意:
 * - 前端不直接调用这些接口进行 IM 操作
 * - 前端应该直接使用 OpenIM SDK 进行用户级操作
 * - 这些接口主要供后端业务服务调用
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-10
 * @Copyright 1024创新实验室
 */
@Slf4j
@Tag(name = "IM业务集成管理")
@RestController
@RequestMapping("/api/im/business")
public class IMBusinessController {

    @Resource
    private IMBusinessService imBusinessService;

    /**
     * 为警情创建群组
     *
     * 业务场景: 当新警情创建时,后端自动调用此接口创建群组
     */
    @Operation(summary = "为警情创建群组")
    @PostMapping("/group/create/{reportId}")
    public ResponseDTO<String> createGroupForReport(@PathVariable Long reportId) {
        Long creatorId = SmartRequestUtil.getRequestUserId();
        String groupId = imBusinessService.createGroupForReport(reportId, creatorId);
        return ResponseDTO.ok(groupId);
    }

    /**
     * 归档警情群组
     *
     * 业务场景: 当警情被归档时调用
     */
    @Operation(summary = "归档警情群组")
    @PostMapping("/group/archive/{reportId}")
    public ResponseDTO<Void> archiveGroupForReport(@PathVariable Long reportId) {
        imBusinessService.archiveGroupForReport(reportId);
        return ResponseDTO.ok();
    }

    /**
     * 解散警情群组
     *
     * 业务场景: 当警情被删除时调用
     */
    @Operation(summary = "解散警情群组")
    @PostMapping("/group/disband/{reportId}")
    public ResponseDTO<Void> disbandGroupForReport(@PathVariable Long reportId) {
        imBusinessService.disbandGroupForReport(reportId);
        return ResponseDTO.ok();
    }

    /**
     * 获取警情的群组ID
     *
     * 前端用途: 获取群组ID后,前端可以直接使用OpenIM SDK加入群组
     */
    @Operation(summary = "获取警情的群组ID")
    @GetMapping("/group/{reportId}")
    public ResponseDTO<String> getGroupIdByReportId(@PathVariable Long reportId) {
        String groupId = imBusinessService.getGroupIdByReportId(reportId);
        return ResponseDTO.ok(groupId);
    }

    /**
     * 检查警情群组是否存在
     */
    @Operation(summary = "检查警情群组是否存在")
    @GetMapping("/group/exist/{reportId}")
    public ResponseDTO<Boolean> isGroupExist(@PathVariable Long reportId) {
        boolean exist = imBusinessService.isGroupExist(reportId);
        return ResponseDTO.ok(exist);
    }

    /**
     * 获取群组历史消息 (REST API fallback方案)
     *
     * 业务场景:
     * - 无痕模式首次登录时,SDK的IndexedDB为空,无法加载历史消息
     * - 通过此接口直接从数据库查询历史消息作为fallback
     *
     * 用途:
     * - 前端SDK的getHistoryMessages()返回空时调用
     * - 确保用户能看到完整的历史消息
     *
     * @param form 请求参数(groupId, count)
     * @return 历史消息列表
     */
    @Operation(summary = "获取群组历史消息 (REST API fallback)")
    @PostMapping("/messages/history")
    public ResponseDTO<Object> getGroupHistoryMessages(@RequestBody @Valid GetHistoryMessagesForm form) {
        Object messageList = imBusinessService.getGroupHistoryMessages(form.getGroupId(), form.getCount());
        return ResponseDTO.ok(messageList);
    }

    /**
     * 保存IM消息到数据库
     *
     * 业务场景:
     * - 前端通过OpenIM SDK发送或接收消息后,调用此接口保存到数据库
     * - 用于解决无痕模式首次登录无法加载历史消息的问题
     *
     * 用途:
     * - 在发送消息成功后调用(保存自己发送的消息)
     * - 在接收到新消息时调用(保存别人发送的消息)
     *
     * 特性:
     * - 自动去重: 相同messageId的消息只保存一次
     * - 异步保存: 不阻塞前端用户操作
     * - 容错处理: 保存失败不影响正常聊天功能
     *
     * @param form 消息数据
     * @return 操作结果
     */
    @Operation(summary = "保存IM消息到数据库")
    @PostMapping("/messages/save")
    public ResponseDTO<Void> saveMessage(@RequestBody @Valid SaveMessageForm form) {
        imBusinessService.saveMessageFromFrontend(form);
        return ResponseDTO.ok();
    }
}
