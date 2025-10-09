package net.lab1024.sa.admin.module.business.im.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.im.constant.ImGroupStatusEnum;
import net.lab1024.sa.admin.module.business.im.dao.ImGroupMappingDao;
import net.lab1024.sa.admin.module.business.im.domain.entity.ImGroupMappingEntity;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * IM群组生命周期管理服务
 *
 * 功能：
 * 1. 群组状态机管理
 * 2. 群组创建流程控制
 * 3. 群组验证和修复
 * 4. 本地缓存管理
 * 5. 幂等性保证
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-09
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImGroupLifecycleService {

    private final ImGroupMappingDao imGroupMappingDao;
    private final OpenIMApiService openIMApiService;
    private final ImUserSyncService imUserSyncService;
    private final OpenIMHealthCheckService healthCheckService;
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis缓存键前缀
    private static final String GROUP_CACHE_PREFIX = "openim:group:";
    private static final String GROUP_VALIDATION_PREFIX = "openim:validation:";

    // 缓存过期时间
    private static final long GROUP_CACHE_EXPIRE_MINUTES = 30;
    private static final long VALIDATION_LOCK_EXPIRE_SECONDS = 10;

    // 重试配置
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000;

    // OpenIM同步等待时间（增加到5秒，确保OpenIM完成同步）
    private static final long OPENIM_SYNC_WAIT_MS = 5000;

    /**
     * 创建或获取群组（幂等操作）
     *
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param groupName 群组名称
     * @param ownerEmployeeId 群主员工ID
     * @param memberEmployeeIds 成员员工ID列表
     * @return 群组映射信息
     */
    @Transactional(rollbackFor = Exception.class)
    public ImGroupMappingEntity createOrGetGroup(String businessType, Long businessId,
                                                  String groupName, Long ownerEmployeeId,
                                                  List<Long> memberEmployeeIds) {
        // 1. 检查OpenIM服务健康状态
        if (!healthCheckService.isHealthy()) {
            log.error("❌ [群组生命周期] OpenIM服务不健康，无法创建群组");
            throw new RuntimeException("OpenIM服务暂时不可用，请稍后重试");
        }

        // 2. 检查本地缓存
        ImGroupMappingEntity cachedGroup = getGroupFromCache(businessType, businessId);
        if (cachedGroup != null && ImGroupStatusEnum.ACTIVE.getValue().equals(cachedGroup.getStatus())) {
            log.info("📦 [群组生命周期] 从缓存获取群组 - BusinessType: {}, BusinessID: {}", businessType, businessId);
            return cachedGroup;
        }

        // 3. 检查数据库中的群组
        ImGroupMappingEntity existingGroup = getGroupByBusiness(businessType, businessId);

        if (existingGroup != null) {
            // 验证群组状态
            ImGroupStatusEnum status = ImGroupStatusEnum.getByValue(existingGroup.getStatus());

            if (status != null && status.isUsable()) {
                // 群组正常，验证其在OpenIM中是否存在
                if (validateGroupInOpenIM(existingGroup.getGroupId())) {
                    updateGroupCache(existingGroup);
                    return existingGroup;
                } else {
                    // 群组在OpenIM中不存在，标记为异常并修复
                    log.warn("⚠️ [群组生命周期] 检测到孤儿记录，开始修复 - BusinessType: {}, BusinessID: {}",
                            businessType, businessId);
                    return repairGroup(existingGroup, groupName, ownerEmployeeId, memberEmployeeIds);
                }
            } else if (status != null && status.isTransitional()) {
                // 群组处于过渡状态，等待并重新检查
                log.info("⏳ [群组生命周期] 群组处于过渡状态: {} - BusinessType: {}, BusinessID: {}",
                        status.getDesc(), businessType, businessId);
                return waitAndCheckGroupStatus(existingGroup);
            } else if (status != null && status.isError()) {
                // 群组异常，尝试修复
                return repairGroup(existingGroup, groupName, ownerEmployeeId, memberEmployeeIds);
            }
        }

        // 4. 创建新群组
        return createNewGroup(businessType, businessId, groupName, ownerEmployeeId, memberEmployeeIds);
    }

    /**
     * 创建新群组
     */
    @Transactional(rollbackFor = Exception.class)
    public ImGroupMappingEntity createNewGroup(String businessType, Long businessId,
                                                String groupName, Long ownerEmployeeId,
                                                List<Long> memberEmployeeIds) {
        log.info("🔨 [群组生命周期] 开始创建新群组 - BusinessType: {}, BusinessID: {}", businessType, businessId);

        try {
            // 1. 同步群主到OpenIM
            String ownerOpenImUserId = imUserSyncService.syncUser(ownerEmployeeId);
            if (ownerOpenImUserId == null) {
                throw new RuntimeException("同步群主失败 - EmployeeID: " + ownerEmployeeId);
            }

            // 2. 同步成员到OpenIM
            List<String> memberOpenImUserIds = new java.util.ArrayList<>();
            for (Long memberId : memberEmployeeIds) {
                if (!memberId.equals(ownerEmployeeId)) {
                    String memberOpenImUserId = imUserSyncService.syncUser(memberId);
                    if (memberOpenImUserId != null) {
                        memberOpenImUserIds.add(memberOpenImUserId);
                    }
                }
            }

            // 3. 调用OpenIM API创建群组（带重试）
            String groupId = createGroupWithRetry(groupName, ownerOpenImUserId, memberOpenImUserIds);

            if (groupId == null) {
                throw new RuntimeException("创建群组失败");
            }

            log.info("✅ [群组生命周期] OpenIM群组创建成功 - GroupID: {}", groupId);

            // 4. 创建映射记录（状态：SYNCING，包含group_id）
            ImGroupMappingEntity groupMapping = new ImGroupMappingEntity();
            groupMapping.setBusinessType(businessType);
            groupMapping.setBusinessId(businessId);
            groupMapping.setGroupName(groupName);
            groupMapping.setGroupId(groupId);  // ✅ 修复：先创建OpenIM群组，获取group_id后再插入数据库
            groupMapping.setOwnerEmployeeId(ownerEmployeeId);
            groupMapping.setOwnerUserId(ownerOpenImUserId);
            groupMapping.setMemberCount(memberOpenImUserIds.size() + 1); // +1 for owner
            groupMapping.setStatus(ImGroupStatusEnum.SYNCING.getValue());
            groupMapping.setCreateTime(LocalDateTime.now());
            groupMapping.setDeletedFlag(0);

            imGroupMappingDao.insert(groupMapping);

            log.info("⏳ [群组生命周期] 群组映射记录已创建，等待OpenIM同步 - GroupID: {}", groupId);

            // 5. 等待OpenIM服务器同步
            Thread.sleep(OPENIM_SYNC_WAIT_MS);

            // 6. 验证群组（状态：VALIDATING -> ACTIVE）
            if (validateAndActivateGroup(groupMapping)) {
                log.info("✅ [群组生命周期] 群组创建并激活成功 - GroupID: {}", groupId);
                updateGroupCache(groupMapping);
                return groupMapping;
            } else {
                updateGroupStatus(groupMapping.getId(), ImGroupStatusEnum.ERROR);
                throw new RuntimeException("群组验证失败");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("群组创建被中断", e);
        } catch (Exception e) {
            log.error("❌ [群组生命周期] 创建群组异常 - BusinessType: {}, BusinessID: {}",
                    businessType, businessId, e);
            throw new RuntimeException("创建群组失败", e);
        }
    }

    /**
     * 修复群组
     */
    @Transactional(rollbackFor = Exception.class)
    public ImGroupMappingEntity repairGroup(ImGroupMappingEntity oldGroup, String groupName,
                                             Long ownerEmployeeId, List<Long> memberEmployeeIds) {
        log.info("🔧 [群组生命周期] 开始修复群组 - BusinessType: {}, BusinessID: {}",
                oldGroup.getBusinessType(), oldGroup.getBusinessId());

        // 1. 更新状态为REPAIRING
        updateGroupStatus(oldGroup.getId(), ImGroupStatusEnum.REPAIRING);

        try {
            // 2. 物理删除旧记录
            imGroupMappingDao.deleteById(oldGroup.getId());
            log.info("🗑️ [群组生命周期] 已删除旧的群组映射 - OldGroupID: {}", oldGroup.getGroupId());

            // 3. 清除缓存
            clearGroupCache(oldGroup.getBusinessType(), oldGroup.getBusinessId());

            // 4. 创建新群组
            return createNewGroup(oldGroup.getBusinessType(), oldGroup.getBusinessId(),
                    groupName, ownerEmployeeId, memberEmployeeIds);

        } catch (Exception e) {
            log.error("❌ [群组生命周期] 修复群组失败 - BusinessType: {}, BusinessID: {}",
                    oldGroup.getBusinessType(), oldGroup.getBusinessId(), e);
            throw new RuntimeException("修复群组失败", e);
        }
    }

    /**
     * 带重试的群组创建
     */
    private String createGroupWithRetry(String groupName, String ownerUserId, List<String> memberUserIds) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                String groupId = openIMApiService.createGroup(groupName, ownerUserId, memberUserIds);
                if (groupId != null) {
                    log.info("✅ [群组生命周期] 创建群组成功 (尝试 {}/{}) - GroupID: {}",
                            attempt, MAX_RETRY_ATTEMPTS, groupId);
                    return groupId;
                }
            } catch (Exception e) {
                log.warn("⚠️ [群组生命周期] 创建群组失败 (尝试 {}/{}) - Error: {}",
                        attempt, MAX_RETRY_ATTEMPTS, e.getMessage());

                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt); // 递增延迟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("❌ [群组生命周期] 创建群组失败，已重试{}次", MAX_RETRY_ATTEMPTS);
        return null;
    }

    /**
     * 验证并激活群组
     */
    private boolean validateAndActivateGroup(ImGroupMappingEntity groupMapping) {
        updateGroupStatus(groupMapping.getId(), ImGroupStatusEnum.VALIDATING);

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                if (validateGroupInOpenIM(groupMapping.getGroupId())) {
                    updateGroupStatus(groupMapping.getId(), ImGroupStatusEnum.ACTIVE);
                    groupMapping.setStatus(ImGroupStatusEnum.ACTIVE.getValue());
                    return true;
                }

                if (attempt < MAX_RETRY_ATTEMPTS) {
                    log.warn("⚠️ [群组生命周期] 验证群组失败，等待重试 (尝试 {}/{}) - GroupID: {}",
                            attempt, MAX_RETRY_ATTEMPTS, groupMapping.getGroupId());
                    Thread.sleep(RETRY_DELAY_MS);
                }

            } catch (Exception e) {
                log.warn("⚠️ [群组生命周期] 验证群组异常 (尝试 {}/{}) - GroupID: {}, Error: {}",
                        attempt, MAX_RETRY_ATTEMPTS, groupMapping.getGroupId(), e.getMessage());

                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 验证群组在OpenIM中是否存在
     */
    private boolean validateGroupInOpenIM(String groupId) {
        // 检查验证锁，避免并发验证
        String lockKey = GROUP_VALIDATION_PREFIX + groupId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1",
                VALIDATION_LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(locked)) {
            log.debug("🔒 [群组生命周期] 群组正在被验证中，跳过 - GroupID: {}", groupId);
            return true; // 假设正在验证的群组是有效的
        }

        try {
            JsonNode groupInfo = openIMApiService.getGroupInfo(groupId);
            boolean exists = groupInfo != null;

            log.info("🔍 [群组生命周期] 验证群组{} - GroupID: {}",
                    exists ? "成功" : "失败", groupId);

            return exists;

        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 等待并检查群组状态
     */
    private ImGroupMappingEntity waitAndCheckGroupStatus(ImGroupMappingEntity groupMapping) {
        try {
            Thread.sleep(2000); // 等待2秒
            ImGroupMappingEntity updated = imGroupMappingDao.selectById(groupMapping.getId());

            if (updated != null && ImGroupStatusEnum.ACTIVE.getValue().equals(updated.getStatus())) {
                updateGroupCache(updated);
                return updated;
            }

            return groupMapping;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return groupMapping;
        }
    }

    /**
     * 更新群组状态
     */
    private void updateGroupStatus(Long mappingId, ImGroupStatusEnum status) {
        ImGroupMappingEntity entity = imGroupMappingDao.selectById(mappingId);
        if (entity != null) {
            entity.setStatus(status.getValue());
            entity.setUpdateTime(LocalDateTime.now());
            imGroupMappingDao.updateById(entity);

            log.info("📝 [群组生命周期] 更新群组状态: {} -> {} - MappingID: {}",
                    ImGroupStatusEnum.getByValue(entity.getStatus()),
                    status.getDesc(),
                    mappingId);
        }
    }

    /**
     * 根据业务获取群组
     */
    public ImGroupMappingEntity getGroupByBusiness(String businessType, Long businessId) {
        LambdaQueryWrapper<ImGroupMappingEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImGroupMappingEntity::getBusinessType, businessType);
        queryWrapper.eq(ImGroupMappingEntity::getBusinessId, businessId);
        queryWrapper.eq(ImGroupMappingEntity::getDeletedFlag, 0);
        return imGroupMappingDao.selectOne(queryWrapper);
    }

    /**
     * 从缓存获取群组
     */
    private ImGroupMappingEntity getGroupFromCache(String businessType, Long businessId) {
        String cacheKey = GROUP_CACHE_PREFIX + businessType + ":" + businessId;
        return (ImGroupMappingEntity) redisTemplate.opsForValue().get(cacheKey);
    }

    /**
     * 更新群组缓存
     */
    private void updateGroupCache(ImGroupMappingEntity groupMapping) {
        String cacheKey = GROUP_CACHE_PREFIX + groupMapping.getBusinessType() + ":" + groupMapping.getBusinessId();
        redisTemplate.opsForValue().set(cacheKey, groupMapping, GROUP_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 清除群组缓存
     */
    private void clearGroupCache(String businessType, Long businessId) {
        String cacheKey = GROUP_CACHE_PREFIX + businessType + ":" + businessId;
        redisTemplate.delete(cacheKey);
    }
}
