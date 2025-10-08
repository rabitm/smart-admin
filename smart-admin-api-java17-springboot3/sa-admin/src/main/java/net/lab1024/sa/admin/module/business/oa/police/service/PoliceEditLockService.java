package net.lab1024.sa.admin.module.business.oa.police.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.oa.police.dao.PoliceEditLockDao;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceEditLockEntity;
import net.lab1024.sa.admin.module.business.oa.seat.service.SeatSyncService;
import net.lab1024.sa.admin.module.business.oa.police.service.sync.SyncService;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.admin.util.AdminRequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 警情编辑锁服务
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-24
 * @Copyright: 1024创新实验室
 */
@Slf4j
@Service
public class PoliceEditLockService {

    @Resource
    private PoliceEditLockDao policeEditLockDao;

    @Resource
    private SeatSyncService seatSyncService;

    // RocketMQ同步服务（优先使用）
    @Autowired(required = false)
    @Qualifier("rocketMQSyncService")
    private SyncService rocketMQSyncService;

    // WebSocket同步服务（备用）
    @Autowired(required = false)
    @Qualifier("webSocketSyncServiceImpl")
    private SyncService webSocketSyncService;

    /**
     * 锁定警情（获取编辑权限）
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> lockPoliceCase(Long policeCaseId, Long seatId) {
        Long currentUserId = AdminRequestUtil.getRequestUserId();
        String currentUserName = AdminRequestUtil.getRequestUser().getUserName();

        // 检查是否已被其他用户锁定
        PoliceEditLockEntity existingLock = policeEditLockDao.selectOne(
                new LambdaQueryWrapper<PoliceEditLockEntity>()
                        .eq(PoliceEditLockEntity::getPoliceCaseId, policeCaseId)
                        .eq(PoliceEditLockEntity::getLockType, 2) // 写锁
                        .eq(PoliceEditLockEntity::getLockStatus, 1) // 锁定中
                        .gt(PoliceEditLockEntity::getExpireTime, LocalDateTime.now())
        );

        if (existingLock != null && !existingLock.getLockUserId().equals(currentUserId)) {
            return ResponseDTO.userErrorParam("该警情正在被 " + existingLock.getLockUserName() + " 编辑中");
        }

        // 如果是当前用户的锁，更新过期时间
        if (existingLock != null && existingLock.getLockUserId().equals(currentUserId)) {
            existingLock.setExpireTime(LocalDateTime.now().plusMinutes(30));
            existingLock.setLockVersion(existingLock.getLockVersion() + 1);
            policeEditLockDao.updateById(existingLock);

            log.info("续期警情编辑锁，用户: {}, 警情ID: {}", currentUserName, policeCaseId);
            return ResponseDTO.ok("续期成功");
        }

        // 创建新的编辑锁
        PoliceEditLockEntity newLock = new PoliceEditLockEntity();
        newLock.setPoliceCaseId(policeCaseId);
        newLock.setLockType(2); // 写锁
        newLock.setLockUserId(currentUserId);
        newLock.setLockUserName(currentUserName);
        newLock.setLockSeatId(seatId);
        newLock.setLockTime(LocalDateTime.now());
        newLock.setExpireTime(LocalDateTime.now().plusMinutes(30)); // 30分钟过期
        newLock.setLockStatus(1); // 锁定中
        newLock.setLockVersion(0);
        newLock.setCreateTime(LocalDateTime.now());
        newLock.setUpdateTime(LocalDateTime.now());

        policeEditLockDao.insert(newLock);

        // 发送锁定通知 - 同时使用RocketMQ和WebSocket
        sendLockNotification(policeCaseId, currentUserId, currentUserName, seatId, "LOCK");
        seatSyncService.notifyPoliceCaseLock(policeCaseId, currentUserId, currentUserName, seatId);

        log.info("警情编辑锁定成功，用户: {}, 警情ID: {}", currentUserName, policeCaseId);
        return ResponseDTO.ok("锁定成功");
    }

    /**
     * 解锁警情（释放编辑权限）
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> unlockPoliceCase(Long policeCaseId) {
        Long currentUserId = AdminRequestUtil.getRequestUserId();
        String currentUserName = AdminRequestUtil.getRequestUser().getUserName();

        // 查找当前用户的锁
        PoliceEditLockEntity existingLock = policeEditLockDao.selectOne(
                new LambdaQueryWrapper<PoliceEditLockEntity>()
                        .eq(PoliceEditLockEntity::getPoliceCaseId, policeCaseId)
                        .eq(PoliceEditLockEntity::getLockUserId, currentUserId)
                        .eq(PoliceEditLockEntity::getLockStatus, 1)
        );

        if (existingLock == null) {
            return ResponseDTO.userErrorParam("未找到对应的编辑锁");
        }

        // 更新锁状态为已释放
        existingLock.setLockStatus(0);
        existingLock.setUpdateTime(LocalDateTime.now());
        policeEditLockDao.updateById(existingLock);

        // 发送解锁通知 - 同时使用RocketMQ和WebSocket
        sendLockNotification(policeCaseId, currentUserId, currentUserName, existingLock.getLockSeatId(), "UNLOCK");
        seatSyncService.notifyPoliceCaseUnlock(policeCaseId, currentUserId, currentUserName, existingLock.getLockSeatId());

        log.info("警情编辑解锁成功，用户: {}, 警情ID: {}", currentUserName, policeCaseId);
        return ResponseDTO.ok("解锁成功");
    }

    /**
     * 检查警情是否被锁定
     */
    public ResponseDTO<Boolean> checkPoliceCaseLock(Long policeCaseId) {
        Long currentUserId = AdminRequestUtil.getRequestUserId();

        PoliceEditLockEntity existingLock = policeEditLockDao.selectOne(
                new LambdaQueryWrapper<PoliceEditLockEntity>()
                        .eq(PoliceEditLockEntity::getPoliceCaseId, policeCaseId)
                        .eq(PoliceEditLockEntity::getLockType, 2) // 写锁
                        .eq(PoliceEditLockEntity::getLockStatus, 1) // 锁定中
                        .gt(PoliceEditLockEntity::getExpireTime, LocalDateTime.now())
        );

        if (existingLock == null) {
            return ResponseDTO.ok(false); // 未锁定
        }

        if (existingLock.getLockUserId().equals(currentUserId)) {
            return ResponseDTO.ok(false); // 自己的锁，可以编辑
        }

        return ResponseDTO.ok(true); // 被他人锁定
    }

    /**
     * 获取警情的编辑锁信息
     */
    public PoliceEditLockEntity getPoliceCaseLock(Long policeCaseId) {
        return policeEditLockDao.selectOne(
                new LambdaQueryWrapper<PoliceEditLockEntity>()
                        .eq(PoliceEditLockEntity::getPoliceCaseId, policeCaseId)
                        .eq(PoliceEditLockEntity::getLockStatus, 1)
                        .gt(PoliceEditLockEntity::getExpireTime, LocalDateTime.now())
        );
    }

    /**
     * 清理过期的编辑锁
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanExpiredLocks() {
        List<PoliceEditLockEntity> expiredLocks = policeEditLockDao.selectList(
                new LambdaQueryWrapper<PoliceEditLockEntity>()
                        .eq(PoliceEditLockEntity::getLockStatus, 1)
                        .lt(PoliceEditLockEntity::getExpireTime, LocalDateTime.now())
        );

        if (!expiredLocks.isEmpty()) {
            // 批量更新过期锁状态
            policeEditLockDao.update(null,
                    new LambdaUpdateWrapper<PoliceEditLockEntity>()
                            .set(PoliceEditLockEntity::getLockStatus, 0)
                            .set(PoliceEditLockEntity::getUpdateTime, LocalDateTime.now())
                            .eq(PoliceEditLockEntity::getLockStatus, 1)
                            .lt(PoliceEditLockEntity::getExpireTime, LocalDateTime.now())
            );

            log.info("清理过期编辑锁 {} 个", expiredLocks.size());

            // 发送解锁通知 - 增强版本，支持RocketMQ
            for (PoliceEditLockEntity lock : expiredLocks) {
                sendLockNotification(lock.getPoliceCaseId(), lock.getLockUserId(),
                                   lock.getLockUserName() + "(超时)", lock.getLockSeatId(), "EXPIRE_UNLOCK");
                seatSyncService.notifyPoliceCaseUnlock(
                        lock.getPoliceCaseId(),
                        lock.getLockUserId(),
                        lock.getLockUserName() + "(超时)",
                        lock.getLockSeatId()
                );
            }
        }
    }

    /**
     * 强制释放用户的所有编辑锁（用户登出时调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseUserLocks(Long userId) {
        List<PoliceEditLockEntity> userLocks = policeEditLockDao.selectList(
                new LambdaQueryWrapper<PoliceEditLockEntity>()
                        .eq(PoliceEditLockEntity::getLockUserId, userId)
                        .eq(PoliceEditLockEntity::getLockStatus, 1)
        );

        if (!userLocks.isEmpty()) {
            // 批量释放用户的锁
            policeEditLockDao.update(null,
                    new LambdaUpdateWrapper<PoliceEditLockEntity>()
                            .set(PoliceEditLockEntity::getLockStatus, 0)
                            .set(PoliceEditLockEntity::getUpdateTime, LocalDateTime.now())
                            .eq(PoliceEditLockEntity::getLockUserId, userId)
                            .eq(PoliceEditLockEntity::getLockStatus, 1)
            );

            // 发送批量解锁通知
            for (PoliceEditLockEntity lock : userLocks) {
                sendLockNotification(lock.getPoliceCaseId(), lock.getLockUserId(),
                                   "系统自动释放", lock.getLockSeatId(), "FORCE_UNLOCK");
            }

            log.info("释放用户 {} 的编辑锁 {} 个", userId, userLocks.size());
        }
    }

    /**
     * 发送锁定/解锁通知 - RocketMQ增强版本
     */
    private void sendLockNotification(Long policeCaseId, Long userId, String userName, Long seatId, String action) {
        try {
            // 优先使用RocketMQ同步服务
            SyncService syncService = getSyncService();
            if (syncService != null) {
                log.debug("🚀 [字段锁定] 发送{}通知: policeCaseId={}, userId={}, action={}",
                         action, policeCaseId, userId, action);

                // 发送字段编辑状态消息
                syncService.syncFieldEditState(policeCaseId, userId, userName, "case_lock", action);

                // 记录锁定操作历史
                String operationDescription = switch (action) {
                    case "LOCK" -> "获取编辑锁定";
                    case "UNLOCK" -> "释放编辑锁定";
                    case "EXPIRE_UNLOCK" -> "锁定超时自动释放";
                    case "FORCE_UNLOCK" -> "强制释放锁定";
                    default -> "锁定状态变更: " + action;
                };

                syncService.recordOperation(policeCaseId, userId, userName, "case_lock",
                                          "", operationDescription, "LOCK_" + action);
            } else {
                log.warn("🚀 [字段锁定] 同步服务不可用，仅使用WebSocket通知");
            }

        } catch (Exception e) {
            log.error("🚀 [字段锁定] 发送{}通知失败: policeCaseId={}, userId={}, error={}",
                     action, policeCaseId, userId, e.getMessage(), e);
        }
    }

    /**
     * 获取可用的同步服务（优先RocketMQ）
     */
    private SyncService getSyncService() {
        log.debug("🔍 [同步服务选择] 检查可用的同步服务...");
        log.debug("🔍 [同步服务选择] rocketMQSyncService: {}", rocketMQSyncService != null ? "已注入" : "未注入");
        log.debug("🔍 [同步服务选择] webSocketSyncService: {}", webSocketSyncService != null ? "已注入" : "未注入");

        if (rocketMQSyncService != null && rocketMQSyncService.isAvailable()) {
            log.info("🚀 [同步服务选择] 使用RocketMQ同步服务");
            return rocketMQSyncService;
        } else if (webSocketSyncService != null && webSocketSyncService.isAvailable()) {
            log.info("🌐 [同步服务选择] 使用WebSocket同步服务");
            return webSocketSyncService;
        }
        log.warn("❌ [同步服务选择] 没有可用的同步服务");
        return null;
    }

    /**
     * 字段级锁定（新功能 - 支持字段级编辑锁定）
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> lockField(Long policeCaseId, String fieldName, Long seatId) {
        Long currentUserId = AdminRequestUtil.getRequestUserId();
        String currentUserName = AdminRequestUtil.getRequestUser().getUserName();

        try {
            log.info("🚀 [字段锁定] 锁定字段: policeCaseId={}, fieldName={}, userId={}, seatId={}",
                    policeCaseId, fieldName, currentUserId, seatId);

            // 发送字段锁定通知
            SyncService syncService = getSyncService();
            if (syncService != null) {
                syncService.syncFieldEditState(policeCaseId, currentUserId, currentUserName, fieldName, "FOCUS");
            }

            return ResponseDTO.ok("字段锁定成功");

        } catch (Exception e) {
            log.error("🚀 [字段锁定] 锁定字段失败: policeCaseId={}, fieldName={}, error={}",
                     policeCaseId, fieldName, e.getMessage(), e);
            return ResponseDTO.userErrorParam("字段锁定失败: " + e.getMessage());
        }
    }

    /**
     * 字段级解锁（新功能 - 支持字段级编辑解锁）
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> unlockField(Long policeCaseId, String fieldName) {
        Long currentUserId = AdminRequestUtil.getRequestUserId();
        String currentUserName = AdminRequestUtil.getRequestUser().getUserName();

        try {
            log.info("🚀 [字段解锁] 解锁字段: policeCaseId={}, fieldName={}, userId={}",
                    policeCaseId, fieldName, currentUserId);

            // 发送字段解锁通知
            SyncService syncService = getSyncService();
            if (syncService != null) {
                syncService.syncFieldEditState(policeCaseId, currentUserId, currentUserName, fieldName, "BLUR");
            }

            return ResponseDTO.ok("字段解锁成功");

        } catch (Exception e) {
            log.error("🚀 [字段解锁] 解锁字段失败: policeCaseId={}, fieldName={}, error={}",
                     policeCaseId, fieldName, e.getMessage(), e);
            return ResponseDTO.userErrorParam("字段解锁失败: " + e.getMessage());
        }
    }

    /**
     * 批量字段更新通知
     */
    public void notifyFieldUpdates(Long policeCaseId, Map<String, String> fieldUpdates, String operationType) {
        Long currentUserId = AdminRequestUtil.getRequestUserId();
        String currentUserName = AdminRequestUtil.getRequestUser().getUserName();

        try {
            log.info("🚀 [批量更新] 通知字段更新: policeCaseId={}, fieldCount={}, operationType={}",
                    policeCaseId, fieldUpdates.size(), operationType);

            SyncService syncService = getSyncService();
            if (syncService != null) {
                // 逐个发送字段更新通知
                for (Map.Entry<String, String> entry : fieldUpdates.entrySet()) {
                    syncService.syncFieldUpdate(policeCaseId, currentUserId, currentUserName,
                                              entry.getKey(), entry.getValue(), operationType);
                }
            }

        } catch (Exception e) {
            log.error("🚀 [批量更新] 通知字段更新失败: policeCaseId={}, error={}", policeCaseId, e.getMessage(), e);
        }
    }

    /**
     * 获取锁定状态监控信息
     */
    public Map<String, Object> getLockStatusMonitor() {
        try {
            Map<String, Object> status = new HashMap<>();

            // 统计当前锁定数量
            Long activeLocks = policeEditLockDao.selectCount(
                    new LambdaQueryWrapper<PoliceEditLockEntity>()
                            .eq(PoliceEditLockEntity::getLockStatus, 1)
                            .gt(PoliceEditLockEntity::getExpireTime, LocalDateTime.now())
            );

            // 统计即将过期的锁（5分钟内）
            Long expiringLocks = policeEditLockDao.selectCount(
                    new LambdaQueryWrapper<PoliceEditLockEntity>()
                            .eq(PoliceEditLockEntity::getLockStatus, 1)
                            .between(PoliceEditLockEntity::getExpireTime,
                                   LocalDateTime.now(),
                                   LocalDateTime.now().plusMinutes(5))
            );

            status.put("activeLocks", activeLocks);
            status.put("expiringLocks", expiringLocks);
            status.put("syncServiceType", getSyncService() != null ?
                      (rocketMQSyncService != null && rocketMQSyncService.isAvailable() ? "RocketMQ" : "WebSocket") : "None");
            status.put("timestamp", LocalDateTime.now());

            return status;

        } catch (Exception e) {
            log.error("🚀 [监控] 获取锁定状态失败: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }
}