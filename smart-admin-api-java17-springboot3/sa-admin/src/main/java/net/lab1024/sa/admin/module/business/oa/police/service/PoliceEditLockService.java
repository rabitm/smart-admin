package net.lab1024.sa.admin.module.business.oa.police.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.oa.police.dao.PoliceEditLockDao;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceEditLockEntity;
import net.lab1024.sa.admin.module.business.oa.seat.service.SeatSyncService;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.admin.util.AdminRequestUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

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

    // 使用AdminRequestUtil获取用户信息

    @Resource
    private SeatSyncService seatSyncService;

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

        // 发送锁定通知
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

        // 发送解锁通知
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

            // 发送解锁通知
            for (PoliceEditLockEntity lock : expiredLocks) {
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

            log.info("释放用户 {} 的编辑锁 {} 个", userId, userLocks.size());
        }
    }
}