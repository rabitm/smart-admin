package net.lab1024.sa.admin.module.business.im.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.im.dao.ImGroupMappingDao;
import net.lab1024.sa.admin.module.business.im.domain.entity.ImGroupMappingEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * IM群组管理服务
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service
@RequiredArgsConstructor
// 临时移除条件注解以便调试
// @ConditionalOnProperty(prefix = "openim", name = "api-url")
public class ImGroupManageService {

    private final OpenIMApiService openIMApiService;
    private final ImGroupMappingDao imGroupMappingDao;
    private final ImUserSyncService imUserSyncService;

    /**
     * 创建群组
     *
     * @param businessType 业务类型 (POLICE)
     * @param businessId 业务ID (警情ID)
     * @param groupName 群组名称
     * @param ownerEmployeeId 群主员工ID
     * @param memberEmployeeIds 成员员工ID列表
     * @return 群组ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String createGroup(String businessType, Long businessId, String groupName,
                              Long ownerEmployeeId, List<Long> memberEmployeeIds) {
        try {
            // 1. 检查群组是否已存在
            LambdaQueryWrapper<ImGroupMappingEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ImGroupMappingEntity::getBusinessType, businessType);
            queryWrapper.eq(ImGroupMappingEntity::getBusinessId, businessId);
            queryWrapper.eq(ImGroupMappingEntity::getDeletedFlag, 0);
            ImGroupMappingEntity existingGroup = imGroupMappingDao.selectOne(queryWrapper);

            if (existingGroup != null) {
                log.warn("📱 [群组管理] 群组已存在 - BusinessType: {}, BusinessID: {}, GroupID: {}",
                        businessType, businessId, existingGroup.getGroupId());
                return existingGroup.getGroupId();
            }

            // 2. 同步所有成员到OpenIM
            String ownerOpenImUserId = imUserSyncService.syncUser(ownerEmployeeId);
            if (ownerOpenImUserId == null) {
                throw new RuntimeException("同步群主失败 - EmployeeID: " + ownerEmployeeId);
            }

            // OpenIM会自动把ownerUserID加为群成员,所以memberUserIDs不应该包含群主
            List<String> memberOpenImUserIds = new java.util.ArrayList<>();

            for (Long memberId : memberEmployeeIds) {
                if (!memberId.equals(ownerEmployeeId)) {  // 避免重复添加群主
                    String memberOpenImUserId = imUserSyncService.syncUser(memberId);
                    if (memberOpenImUserId != null) {
                        memberOpenImUserIds.add(memberOpenImUserId);
                    }
                }
            }

            // 3. 调用OpenIM API创建群组
            String groupId = openIMApiService.createGroup(
                    groupName,
                    ownerOpenImUserId,
                    memberOpenImUserIds
            );

            if (groupId == null) {
                throw new RuntimeException("创建群组失败");
            }

            // 4. 保存群组映射关系
            ImGroupMappingEntity groupMapping = new ImGroupMappingEntity();
            groupMapping.setGroupId(groupId);
            groupMapping.setBusinessType(businessType);
            groupMapping.setBusinessId(businessId);
            groupMapping.setGroupName(groupName);
            groupMapping.setOwnerUserId(ownerOpenImUserId);
            groupMapping.setOwnerEmployeeId(ownerEmployeeId);
            groupMapping.setMemberCount(memberOpenImUserIds.size());
            groupMapping.setCreateTime(LocalDateTime.now());
            groupMapping.setDeletedFlag(0);

            imGroupMappingDao.insert(groupMapping);

            log.info("📱 [群组管理] 创建群组成功 - BusinessType: {}, BusinessID: {}, GroupID: {}, 成员数: {}",
                    businessType, businessId, groupId, memberOpenImUserIds.size());

            return groupId;

        } catch (Exception e) {
            log.error("📱 [群组管理] 创建群组异常 - BusinessType: {}, BusinessID: {}",
                    businessType, businessId, e);
            throw new RuntimeException("创建群组失败", e);
        }
    }

    /**
     * 邀请成员加入群组
     *
     * @param groupId 群组ID
     * @param employeeIds 员工ID列表
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean inviteMembers(String groupId, List<Long> employeeIds) {
        try {
            // 1. 同步用户并获取OpenIM用户ID
            List<String> openImUserIds = new java.util.ArrayList<>();
            for (Long employeeId : employeeIds) {
                String openImUserId = imUserSyncService.syncUser(employeeId);
                if (openImUserId != null) {
                    openImUserIds.add(openImUserId);
                }
            }

            if (openImUserIds.isEmpty()) {
                log.warn("📱 [群组管理] 没有可邀请的成员 - GroupID: {}", groupId);
                return false;
            }

            // 2. 调用OpenIM API邀请成员
            boolean success = openIMApiService.inviteToGroup(groupId, openImUserIds);

            if (success) {
                // 3. 更新群组成员数量
                updateMemberCount(groupId);

                log.info("📱 [群组管理] 邀请成员成功 - GroupID: {}, 成员数: {}",
                        groupId, openImUserIds.size());
            }

            return success;

        } catch (Exception e) {
            log.error("📱 [群组管理] 邀请成员异常 - GroupID: {}", groupId, e);
            return false;
        }
    }

    /**
     * 删除群组映射（物理删除，用于重建时清理）
     *
     * @param mappingId 映射ID
     */
    public void deleteGroupMapping(Long mappingId) {
        try {
            ImGroupMappingEntity mapping = imGroupMappingDao.selectById(mappingId);
            if (mapping != null) {
                // 物理删除以避免唯一键冲突
                imGroupMappingDao.deleteById(mappingId);
                log.info("📱 [群组管理] 物理删除群组映射成功 - MappingID: {}, GroupID: {}", mappingId, mapping.getGroupId());
            }
        } catch (Exception e) {
            log.error("📱 [群组管理] 删除群组映射失败 - MappingID: {}", mappingId, e);
            throw new RuntimeException("删除群组映射失败", e);
        }
    }

    /**
     * 更新群组成员数量
     */
    private void updateMemberCount(String groupId) {
        try {
            JsonNode groupInfo = openIMApiService.getGroupInfo(groupId);
            if (groupInfo != null) {
                int memberCount = groupInfo.get("memberCount").asInt();

                LambdaQueryWrapper<ImGroupMappingEntity> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(ImGroupMappingEntity::getGroupId, groupId);
                queryWrapper.eq(ImGroupMappingEntity::getDeletedFlag, 0);
                ImGroupMappingEntity groupMapping = imGroupMappingDao.selectOne(queryWrapper);

                if (groupMapping != null) {
                    groupMapping.setMemberCount(memberCount);
                    groupMapping.setUpdateTime(LocalDateTime.now());
                    imGroupMappingDao.updateById(groupMapping);
                }
            }
        } catch (Exception e) {
            log.error("📱 [群组管理] 更新成员数量失败 - GroupID: {}", groupId, e);
        }
    }

    /**
     * 根据业务ID获取群组
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 群组映射信息
     */
    public ImGroupMappingEntity getGroupByBusiness(String businessType, Long businessId) {
        LambdaQueryWrapper<ImGroupMappingEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImGroupMappingEntity::getBusinessType, businessType);
        queryWrapper.eq(ImGroupMappingEntity::getBusinessId, businessId);
        queryWrapper.eq(ImGroupMappingEntity::getDeletedFlag, 0);
        return imGroupMappingDao.selectOne(queryWrapper);
    }

    /**
     * 根据群组ID获取映射信息
     *
     * @param groupId 群组ID
     * @return 群组映射信息
     */
    public ImGroupMappingEntity getGroupById(String groupId) {
        LambdaQueryWrapper<ImGroupMappingEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImGroupMappingEntity::getGroupId, groupId);
        queryWrapper.eq(ImGroupMappingEntity::getDeletedFlag, 0);
        return imGroupMappingDao.selectOne(queryWrapper);
    }
}
