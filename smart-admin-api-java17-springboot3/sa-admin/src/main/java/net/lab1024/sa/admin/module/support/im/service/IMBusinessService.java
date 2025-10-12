package net.lab1024.sa.admin.module.support.im.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.oa.police.dao.PoliceReportDao;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceReportEntity;
import net.lab1024.sa.admin.module.support.im.client.OpenIMClient;
import net.lab1024.sa.admin.module.support.im.config.OpenIMConfig;
import net.lab1024.sa.admin.module.support.im.constant.IMConstant;
import net.lab1024.sa.admin.module.support.im.constant.IMErrorCodeEnum;
import net.lab1024.sa.admin.module.support.im.constant.IMGroupRoleEnum;
import net.lab1024.sa.admin.module.support.im.constant.IMOperationTypeEnum;
import net.lab1024.sa.admin.module.support.im.dao.IMGroupMappingDao;
import net.lab1024.sa.admin.module.support.im.dao.IMGroupMemberDao;
import net.lab1024.sa.admin.module.support.im.domain.entity.IMGroupMappingEntity;
import net.lab1024.sa.admin.module.support.im.domain.entity.IMGroupMemberEntity;
import net.lab1024.sa.base.common.exception.BusinessException;
import net.lab1024.sa.base.common.util.SmartStringUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * IM 业务集成服务
 *
 * 职责范围 (New Architecture):
 * ✅ 1. 自动为警情创建 IM 群组 (业务触发)
 * ✅ 2. 群组生命周期管理 (创建、归档、解散)
 * ✅ 3. 业务数据与 IM 数据的同步
 * ✅ 4. 记录操作日志
 *
 * 不再负责 (Frontend Direct):
 * ❌ 1. 邀请/移除成员 - 前端直接调用 OpenIM SDK
 * ❌ 2. 发送/接收消息 - 前端直接调用 OpenIM SDK
 * ❌ 3. 消息历史查询 - 前端直接调用 OpenIM SDK
 * ❌ 4. WebSocket 消息代理 - 前端直接连接 OpenIM WebSocket
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-10
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service
public class IMBusinessService {

    @Resource
    private OpenIMClient openIMClient;

    @Resource
    private OpenIMConfig openIMConfig;

    @Resource
    private PoliceReportDao policeReportDao;

    @Resource
    private IMGroupMappingDao imGroupMappingDao;

    @Resource
    private IMGroupMemberDao imGroupMemberDao;

    @Resource
    private IMUserSyncService imUserSyncService;

    @Resource
    private IMOperationLogService imOperationLogService;

    @Resource
    private IMMessageService imMessageService;

    /**
     * 为警情自动创建 IM 群组
     *
     * 业务场景: 当新警情被创建时,后端自动为其创建对应的 IM 群组
     *
     * 调用时机:
     * - 新警情创建后
     * - 警情状态变更需要协作时
     *
     * @param reportId 警情ID
     * @param creatorEmployeeId 创建人员工ID
     * @return OpenIM群组ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String createGroupForReport(Long reportId, Long creatorEmployeeId) {
        if (reportId == null) {
            throw new BusinessException(IMErrorCodeEnum.REPORT_ID_REQUIRED);
        }
        if (creatorEmployeeId == null) {
            throw new BusinessException(IMErrorCodeEnum.EMPLOYEE_ID_REQUIRED);
        }

        long startTime = System.currentTimeMillis();

        try {
            // 1. 检查群组是否已存在
            IMGroupMappingEntity existingGroup = imGroupMappingDao.selectByReportId(reportId);
            if (existingGroup != null) {
                if (IMConstant.GROUP_STATUS_NORMAL.equals(existingGroup.getGroupStatus())) {
                    // 🔧 修复: 验证群组在 OpenIM 服务器上是否真实存在
                    if (verifyGroupExistsOnServer(existingGroup.getOpenimGroupId())) {
                        log.info("✅ [群组创建] 警情{}的群组已存在且已验证: {}", reportId, existingGroup.getOpenimGroupId());
                        return existingGroup.getOpenimGroupId();
                    } else {
                        log.warn("⚠️ [群组创建] 数据库中存在群组映射,但OpenIM服务器上不存在,删除旧映射重新创建");
                        // 删除旧的数据库映射记录
                        imGroupMappingDao.deleteById(existingGroup.getId());
                        // 删除关联的成员记录
                        List<IMGroupMemberEntity> members = imGroupMemberDao.selectByGroupMappingId(existingGroup.getId());
                        for (IMGroupMemberEntity member : members) {
                            imGroupMemberDao.deleteById(member.getId());
                        }
                    }
                }
            }

            // 2. 查询警情信息
            PoliceReportEntity report = policeReportDao.selectById(reportId);
            if (report == null) {
                throw new BusinessException("警情不存在: " + reportId);
            }

            // 3. 确保创建人已同步到OpenIM (如果未同步则自动注册)
            String ownerOpenimUserId = imUserSyncService.getOpenIMUserId(creatorEmployeeId);

            // 4. 生成群组ID和名称
            String groupId = generateGroupId(reportId);
            String groupName = generateGroupName(report);

            // 5. 构建群组信息
            Map<String, Object> groupInfo = buildGroupInfo(groupId, groupName, report);

            // 6. 构建创建群组请求
            // 注意: ownerUserID 会自动成为群成员,所以 memberUserIDs 应为空列表
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("groupInfo", groupInfo);
            requestBody.put("memberUserIDs", Collections.emptyList());  // 群主自动加入
            requestBody.put("adminUserIDs", Collections.emptyList());
            requestBody.put("ownerUserID", ownerOpenimUserId);

            log.info("📤 [群组创建] 开始为警情{}创建群组: {}", reportId, groupId);

            // 7. 调用OpenIM API创建群组
            JSONObject response = openIMClient.postWithRetry(
                    IMConstant.API_GROUP_CREATE,
                    requestBody,
                    JSONObject.class,
                    IMOperationTypeEnum.GROUP_CREATE
            );

            // 8. 保存群组映射关系
            saveGroupMapping(reportId, groupId, groupName, ownerOpenimUserId, creatorEmployeeId);

            // 9. 保存群主成员记录
            saveGroupMember(reportId, creatorEmployeeId, ownerOpenimUserId,
                    IMGroupRoleEnum.OWNER.getCode(), IMConstant.JOIN_TYPE_AUTO, "系统创建");

            // 10. 更新警情表的群组ID字段
            updateReportGroupId(reportId, groupId);

            int executionTime = (int) (System.currentTimeMillis() - startTime);

            imOperationLogService.logSuccess(
                    IMOperationTypeEnum.GROUP_CREATE,
                    "GROUP",
                    groupId,
                    requestBody,
                    response,
                    executionTime
            );

            log.info("✅ [群组创建] 警情{}的群组创建成功: {}, 耗时: {}ms", reportId, groupId, executionTime);

            return groupId;

        } catch (BusinessException e) {
            handleOperationFailure(reportId, startTime, IMOperationTypeEnum.GROUP_CREATE, e.getMessage());
            throw e;
        } catch (Exception e) {
            handleOperationFailure(reportId, startTime, IMOperationTypeEnum.GROUP_CREATE, e.getMessage());
            throw new BusinessException("群组创建失败: " + e.getMessage());
        }
    }

    /**
     * 归档群组
     *
     * 业务场景: 当警情被归档或关闭时,归档对应的群组
     *
     * @param reportId 警情ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void archiveGroupForReport(Long reportId) {
        if (reportId == null) {
            throw new BusinessException(IMErrorCodeEnum.REPORT_ID_REQUIRED);
        }

        IMGroupMappingEntity groupMapping = imGroupMappingDao.selectByReportId(reportId);
        if (groupMapping == null) {
            log.warn("⚠️ [群组归档] 警情{}没有关联的群组", reportId);
            return;
        }

        if (IMConstant.GROUP_STATUS_ARCHIVED.equals(groupMapping.getGroupStatus())) {
            log.info("✅ [群组归档] 群组{}已归档", groupMapping.getOpenimGroupId());
            return;
        }

        // 更新群组状态为已归档
        imGroupMappingDao.updateGroupStatus(groupMapping.getId(), IMConstant.GROUP_STATUS_ARCHIVED);

        log.info("✅ [群组归档] 警情{}的群组{}已归档", reportId, groupMapping.getOpenimGroupId());
    }

    /**
     * 解散群组
     *
     * 业务场景: 当警情被彻底删除时,解散对应的群组
     *
     * @param reportId 警情ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void disbandGroupForReport(Long reportId) {
        if (reportId == null) {
            throw new BusinessException(IMErrorCodeEnum.REPORT_ID_REQUIRED);
        }

        long startTime = System.currentTimeMillis();

        try {
            IMGroupMappingEntity groupMapping = imGroupMappingDao.selectByReportId(reportId);
            if (groupMapping == null) {
                log.warn("⚠️ [群组解散] 警情{}没有关联的群组", reportId);
                return;
            }

            if (IMConstant.GROUP_STATUS_DISBANDED.equals(groupMapping.getGroupStatus())) {
                log.info("✅ [群组解散] 群组{}已解散", groupMapping.getOpenimGroupId());
                return;
            }

            String groupId = groupMapping.getOpenimGroupId();

            // 调用OpenIM API解散群组
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("groupID", groupId);

            log.info("📤 [群组解散] 开始解散群组: {}", groupId);

            openIMClient.postWithRetry(
                    IMConstant.API_GROUP_DISBAND,
                    requestBody,
                    JSONObject.class,
                    IMOperationTypeEnum.GROUP_DISBAND
            );

            // 更新群组状态
            imGroupMappingDao.updateGroupStatus(groupMapping.getId(), IMConstant.GROUP_STATUS_DISBANDED);

            // 软删除所有成员
            List<IMGroupMemberEntity> members = imGroupMemberDao.selectByGroupMappingId(groupMapping.getId());
            for (IMGroupMemberEntity member : members) {
                if (!member.getDeletedFlag()) {
                    imGroupMemberDao.softDelete(member.getId());
                }
            }

            int executionTime = (int) (System.currentTimeMillis() - startTime);

            log.info("✅ [群组解散] 警情{}的群组解散成功, 耗时: {}ms", reportId, executionTime);

        } catch (Exception e) {
            handleOperationFailure(reportId, startTime, IMOperationTypeEnum.GROUP_DISBAND, e.getMessage());
            throw new BusinessException("群组解散失败: " + e.getMessage());
        }
    }

    /**
     * 获取警情的群组ID
     *
     * @param reportId 警情ID
     * @return 群组ID,如果不存在返回null
     */
    public String getGroupIdByReportId(Long reportId) {
        IMGroupMappingEntity groupMapping = imGroupMappingDao.selectByReportId(reportId);
        return groupMapping != null ? groupMapping.getOpenimGroupId() : null;
    }

    /**
     * 检查群组是否存在
     *
     * @param reportId 警情ID
     * @return 是否存在
     */
    public boolean isGroupExist(Long reportId) {
        IMGroupMappingEntity groupMapping = imGroupMappingDao.selectByReportId(reportId);
        return groupMapping != null && IMConstant.GROUP_STATUS_NORMAL.equals(groupMapping.getGroupStatus());
    }

    /**
     * 保存前端发送/接收的消息到数据库
     *
     * 业务场景:
     * - 前端通过 OpenIM SDK 发送或接收消息后调用
     * - 用于解决无痕模式首次登录无法加载历史消息的问题
     *
     * 执行流程:
     * 1. 提取表单数据,转换为 MessageVO
     * 2. 调用 IMMessageService 保存到数据库(自动去重)
     * 3. 异步执行,不阻塞前端操作
     *
     * 容错处理:
     * - 保存失败只记录日志,不抛出异常
     * - 不影响正常的聊天功能
     *
     * @param form 消息表单数据
     */
    public void saveMessageFromFrontend(net.lab1024.sa.admin.module.support.im.domain.form.SaveMessageForm form) {
        if (form == null) {
            log.warn("⚠️ [消息保存] 表单为空,跳过保存");
            return;
        }

        try {
            // 1. 转换表单数据为 MessageVO
            net.lab1024.sa.admin.module.support.im.domain.vo.MessageVO messageVO =
                    new net.lab1024.sa.admin.module.support.im.domain.vo.MessageVO();

            messageVO.setMessageId(form.getMessageId());
            messageVO.setServerMessageId(form.getServerMessageId());
            messageVO.setConversationId(form.getConversationId());
            messageVO.setSenderId(form.getSenderId());
            messageVO.setSenderName(form.getSenderName());
            messageVO.setSenderAvatar(form.getSenderAvatar());
            messageVO.setContentType(form.getContentType());
            messageVO.setContent(form.getContent());
            messageVO.setContentJson(form.getContentJson());
            messageVO.setSendTime(form.getSendTime());
            messageVO.setSeq(form.getSeq());

            // 2. 调用消息服务保存(自动去重)
            imMessageService.saveMessage(messageVO, form.getReportId(), form.getGroupId());

            log.debug("✅ [消息保存] 消息已提交保存: messageId={}", form.getMessageId());

        } catch (Exception e) {
            // 捕获异常,不向上抛出,避免影响前端正常聊天
            log.error("❌ [消息保存] 保存失败: messageId={}, error={}",
                    form.getMessageId(), e.getMessage(), e);
        }
    }

    /**
     * 获取群组历史消息 (数据库 fallback方案)
     *
     * 🔧 重要修改: 从后端数据库查询历史消息,而不是调用 OpenIM REST API
     *
     * 原因:
     * - OpenIM Server 不提供消息历史查询的 REST API
     * - 消息历史查询被设计为 SDK 功能
     * - 通过在后端数据库存储消息副本来解决无痕模式问题
     *
     * 用于前端SDK无法从IndexedDB获取消息时的fallback
     * 直接从后端数据库查询历史消息
     *
     * 使用场景:
     * 1. 无痕模式首次登录 - IndexedDB为空
     * 2. 浏览器清空缓存后 - IndexedDB被清理
     * 3. SDK同步失败 - 本地数据不完整
     *
     * @param groupId 群组ID (格式: group_report_5)
     * @param count 消息数量 (默认50)
     * @return 历史消息列表 (MessageVO格式)
     */
    public Object getGroupHistoryMessages(String groupId, Integer count) {
        if (SmartStringUtil.isBlank(groupId)) {
            throw new BusinessException(IMErrorCodeEnum.GROUP_ID_REQUIRED);
        }

        if (count == null || count <= 0) {
            count = 50; // 默认50条
        }

        long startTime = System.currentTimeMillis();

        try {
            log.info("📥 [历史消息] 开始从数据库获取历史消息: groupId={}, count={}", groupId, count);

            // 1. 根据群组ID查询警情ID
            IMGroupMappingEntity groupMapping = imGroupMappingDao.selectByOpenimGroupId(groupId);
            if (groupMapping == null) {
                log.warn("⚠️ [历史消息] 群组{}不存在", groupId);
                return Collections.emptyList();
            }

            Long reportId = groupMapping.getReportId();

            // 2. 从数据库查询历史消息
            List<net.lab1024.sa.admin.module.support.im.domain.vo.MessageVO> messageList =
                    imMessageService.getMessageHistory(reportId, count);

            int executionTime = (int) (System.currentTimeMillis() - startTime);

            log.info("✅ [历史消息] 获取成功: groupId={}, reportId={}, 消息数量={}, 耗时={}ms",
                    groupId, reportId, messageList.size(), executionTime);

            // 3. 记录操作日志
            imOperationLogService.logSuccess(
                    IMOperationTypeEnum.MESSAGE_HISTORY_QUERY,
                    "GROUP",
                    groupId,
                    Map.of("groupId", groupId, "count", count),
                    Map.of("messageCount", messageList.size()),
                    executionTime
            );

            return messageList;

        } catch (BusinessException e) {
            handleOperationFailure(null, startTime, IMOperationTypeEnum.MESSAGE_HISTORY_QUERY, e.getMessage());
            throw e;
        } catch (Exception e) {
            handleOperationFailure(null, startTime, IMOperationTypeEnum.MESSAGE_HISTORY_QUERY, e.getMessage());
            throw new BusinessException("获取历史消息失败: " + e.getMessage());
        }
    }

    // ========== 私有方法 ==========

    /**
     * 生成群组ID
     *
     * 🔧 重要修改: 不再使用 GROUP_ID_PREFIX 前缀
     *
     * 原因:
     * - OpenIM Server 不接受带前缀的 groupID (如 group_report_5)
     * - SDK 查询时会找不到群组,导致历史消息加载失败
     *
     * 格式: report_{reportId}
     * 示例: report_5, report_10
     */
    private String generateGroupId(Long reportId) {
        // ✅ 修复: 使用完整的 group_report_ 前缀,与 OpenIM Server 存储格式一致
        return IMConstant.GROUP_ID_PREFIX + reportId;
    }

    /**
     * 生成群组名称
     */
    private String generateGroupName(PoliceReportEntity report) {
        String template = openIMConfig.getGroupNameTemplate();
        if (SmartStringUtil.isBlank(template)) {
            template = IMConstant.DEFAULT_GROUP_NAME_TEMPLATE;
        }

        return template.replace("{reportNumber}", report.getReportNumber())
                .replace("{reportId}", String.valueOf(report.getReportId()));
    }

    /**
     * 构建群组信息
     */
    private Map<String, Object> buildGroupInfo(String groupId, String groupName, PoliceReportEntity report) {
        Map<String, Object> groupInfo = new HashMap<>();
        groupInfo.put("groupID", groupId);
        groupInfo.put("groupName", groupName);
        groupInfo.put("groupType", IMConstant.GROUP_TYPE_WORK);
        groupInfo.put("notification", "警情处理协作群");
        groupInfo.put("introduction", "警情编号: " + report.getReportNumber());
        groupInfo.put("needVerification", 0);  // 不需要验证
        groupInfo.put("lookMemberInfo", 1);
        groupInfo.put("applyMemberFriend", 0);

        // 扩展字段 - 存储业务信息
        Map<String, Object> ex = new HashMap<>();
        ex.put("reportId", report.getReportId());
        ex.put("reportType", report.getReportType());
        ex.put("reportLevel", report.getReportLevel());
        groupInfo.put("ex", JSON.toJSONString(ex));

        return groupInfo;
    }

    /**
     * 保存群组映射
     */
    private void saveGroupMapping(Long reportId, String groupId, String groupName,
                                  String ownerOpenimUserId, Long ownerEmployeeId) {
        IMGroupMappingEntity mapping = new IMGroupMappingEntity();
        mapping.setReportId(reportId);
        mapping.setOpenimGroupId(groupId);
        mapping.setGroupName(groupName);
        mapping.setGroupType(IMConstant.GROUP_TYPE_WORK);
        mapping.setOwnerUserId(ownerOpenimUserId);
        mapping.setOwnerEmployeeId(ownerEmployeeId);
        mapping.setGroupStatus(IMConstant.GROUP_STATUS_NORMAL);
        mapping.setMemberCount(1);  // 群主自动加入
        mapping.setMaxMemberCount(openIMConfig.getGroupMaxMembers());
        mapping.setNeedVerification(false);
        mapping.setDeletedFlag(false);

        imGroupMappingDao.insert(mapping);
    }

    /**
     * 保存群成员记录
     */
    private void saveGroupMember(Long reportId, Long employeeId, String openimUserId,
                                 Integer roleInGroup, Integer joinType, String joinSource) {
        IMGroupMappingEntity groupMapping = imGroupMappingDao.selectByReportId(reportId);
        if (groupMapping == null) {
            return;
        }

        IMGroupMemberEntity member = new IMGroupMemberEntity();
        member.setGroupMappingId(groupMapping.getId());
        member.setEmployeeId(employeeId);
        member.setOpenimUserId(openimUserId);
        member.setRoleInGroup(roleInGroup);
        member.setJoinType(joinType);
        member.setJoinSource(joinSource);
        member.setJoinTime(LocalDateTime.now());
        member.setDeletedFlag(false);

        imGroupMemberDao.insert(member);
    }

    /**
     * 更新警情表的群组ID
     */
    private void updateReportGroupId(Long reportId, String groupId) {
        try {
            PoliceReportEntity report = new PoliceReportEntity();
            report.setReportId(reportId);
            report.setImGroupId(groupId);
            policeReportDao.updateById(report);
        } catch (Exception e) {
            log.warn("⚠️ 更新警情{}的群组ID失败: {}", reportId, e.getMessage());
        }
    }

    /**
     * 处理操作失败
     */
    private void handleOperationFailure(Long reportId, long startTime,
                                       IMOperationTypeEnum operationType, String errorMessage) {
        int executionTime = (int) (System.currentTimeMillis() - startTime);

        imOperationLogService.logFailure(
                operationType,
                "GROUP",
                String.valueOf(reportId),
                null,
                errorMessage,
                executionTime
        );
    }

    /**
     * 验证群组在 OpenIM 服务器上是否真实存在
     *
     * @param groupId OpenIM 群组ID
     * @return true-存在, false-不存在
     */
    private boolean verifyGroupExistsOnServer(String groupId) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("groupIDs", Collections.singletonList(groupId));

            JSONObject response = openIMClient.postWithRetry(
                    IMConstant.API_GROUP_GET_INFO,
                    requestBody,
                    JSONObject.class,
                    IMOperationTypeEnum.GROUP_GET_INFO
            );

            // 检查返回的 groupInfos 是否包含该群组
            if (response != null && response.containsKey("groupInfos")) {
                Object groupInfos = response.get("groupInfos");
                if (groupInfos instanceof List) {
                    List<?> infos = (List<?>) groupInfos;
                    return !infos.isEmpty();
                }
            }

            return false;

        } catch (Exception e) {
            log.warn("⚠️ [群组验证] 验证群组{}是否存在失败: {}", groupId, e.getMessage());
            // 发生异常时返回 false,触发重新创建
            return false;
        }
    }
}
