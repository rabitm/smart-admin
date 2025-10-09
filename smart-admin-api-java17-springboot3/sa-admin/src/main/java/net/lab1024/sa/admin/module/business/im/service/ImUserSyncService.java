package net.lab1024.sa.admin.module.business.im.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.im.dao.ImUserMappingDao;
import net.lab1024.sa.admin.module.business.im.domain.entity.ImUserMappingEntity;
import net.lab1024.sa.admin.module.business.im.domain.vo.ImTokenVO;
import net.lab1024.sa.admin.module.business.im.config.OpenIMProperties;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * IM用户同步服务
 * 负责SmartAdmin用户与OpenIM用户的同步
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
public class ImUserSyncService {

    private final OpenIMApiService openIMApiService;
    private final ImUserMappingDao imUserMappingDao;
    private final EmployeeDao employeeDao;
    private final OpenIMProperties openIMProperties;

    /**
     * 同步用户到OpenIM
     * 如果用户不存在则注册,如果已存在则跳过
     *
     * @param employeeId 员工ID
     * @return OpenIM用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String syncUser(Long employeeId) {
        try {
            // 1. 检查数据库中的映射记录
            LambdaQueryWrapper<ImUserMappingEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ImUserMappingEntity::getEmployeeId, employeeId);
            queryWrapper.eq(ImUserMappingEntity::getDeletedFlag, 0);
            ImUserMappingEntity mapping = imUserMappingDao.selectOne(queryWrapper);

            // 2. 获取员工信息
            EmployeeEntity employee = employeeDao.selectById(employeeId);
            if (employee == null) {
                log.warn("📱 [用户同步] 员工不存在 - EmployeeID: {}", employeeId);
                return null;
            }

            // 3. 使用employeeId作为OpenIM UserID
            String openImUserId = String.valueOf(employeeId);

            // 4. 检查OpenIM用户是否已存在 (即使数据库有记录也要验证,防止数据不一致)
            log.info("📱 [用户同步] 检查OpenIM用户是否存在 - EmployeeID: {}, OpenIMUserID: {}", employeeId, openImUserId);
            boolean exists = openIMApiService.checkUserExists(openImUserId);
            log.info("📱 [用户同步] OpenIM用户存在性检查结果 - OpenIMUserID: {}, Exists: {}", openImUserId, exists);

            if (!exists) {
                // 5. 注册到OpenIM
                log.info("📱 [用户同步] 开始注册OpenIM用户 - EmployeeID: {}, OpenIMUserID: {}, Name: {}",
                        employeeId, openImUserId, employee.getActualName());
                boolean success = openIMApiService.registerUser(
                        openImUserId,
                        employee.getActualName(),
                        employee.getAvatar()
                );

                if (!success) {
                    log.error("📱 [用户同步] 注册OpenIM用户失败 - EmployeeID: {}", employeeId);
                    return null;
                }
                log.info("📱 [用户同步] 注册OpenIM用户成功 - EmployeeID: {}, OpenIMUserID: {}", employeeId, openImUserId);
            } else {
                log.info("📱 [用户同步] OpenIM用户已存在，跳过注册 - OpenIMUserID: {}", openImUserId);
            }

            // 6. 保存或更新映射关系
            if (mapping == null) {
                mapping = new ImUserMappingEntity();
                mapping.setEmployeeId(employeeId);
                mapping.setOpenImUserId(openImUserId);
                mapping.setSyncStatus(1);
                mapping.setLastSyncTime(LocalDateTime.now());
                mapping.setCreateTime(LocalDateTime.now());
                mapping.setDeletedFlag(0);
                imUserMappingDao.insert(mapping);

                log.info("📱 [用户同步] 创建用户映射成功 - EmployeeID: {}, OpenIMUserID: {}",
                        employeeId, openImUserId);
            } else {
                mapping.setSyncStatus(1);
                mapping.setLastSyncTime(LocalDateTime.now());
                mapping.setUpdateTime(LocalDateTime.now());
                imUserMappingDao.updateById(mapping);

                log.info("📱 [用户同步] 更新用户映射成功 - EmployeeID: {}, OpenIMUserID: {}",
                        employeeId, openImUserId);
            }

            return openImUserId;

        } catch (Exception e) {
            log.error("📱 [用户同步] 同步用户异常 - EmployeeID: {}", employeeId, e);
            return null;
        }
    }

    /**
     * 获取IM Token
     * 自动同步用户(如果未同步)
     *
     * @param employeeId 员工ID
     * @return IM Token信息
     */
    public ImTokenVO getImToken(Long employeeId) {
        try {
            // 1. 同步用户
            String openImUserId = syncUser(employeeId);
            if (openImUserId == null) {
                throw new RuntimeException("用户同步失败");
            }

            // 2. 获取用户Token
            String userToken = openIMApiService.getUserToken(openImUserId);

            // 3. 构造响应
            ImTokenVO tokenVO = new ImTokenVO();
            tokenVO.setUserToken(userToken);
            tokenVO.setUserID(openImUserId);
            tokenVO.setApiUrl(openIMProperties.getApiUrl());
            tokenVO.setWsUrl(openIMProperties.getWsUrl());
            tokenVO.setPlatformID(openIMProperties.getPlatformId());

            // Token有效期 (当前时间 + 配置的有效期)
            long expireSeconds = openIMProperties.getSecurity().getTokenExpireHours() * 3600L;
            tokenVO.setExpireTime(System.currentTimeMillis() / 1000 + expireSeconds);

            log.info("📱 [用户同步] 获取IM Token成功 - EmployeeID: {}, OpenIMUserID: {}",
                    employeeId, openImUserId);

            return tokenVO;

        } catch (Exception e) {
            log.error("📱 [用户同步] 获取IM Token异常 - EmployeeID: {}", employeeId, e);
            throw new RuntimeException("获取IM Token失败", e);
        }
    }

    /**
     * 通过员工ID获取OpenIM用户ID
     *
     * @param employeeId 员工ID
     * @return OpenIM用户ID
     */
    public String getOpenImUserId(Long employeeId) {
        LambdaQueryWrapper<ImUserMappingEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImUserMappingEntity::getEmployeeId, employeeId);
        queryWrapper.eq(ImUserMappingEntity::getDeletedFlag, 0);
        ImUserMappingEntity mapping = imUserMappingDao.selectOne(queryWrapper);

        if (mapping != null) {
            return mapping.getOpenImUserId();
        }

        // 如果未同步,则自动同步
        return syncUser(employeeId);
    }

    /**
     * 批量同步用户
     *
     * @param employeeIds 员工ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchSyncUsers(java.util.List<Long> employeeIds) {
        log.info("📱 [用户同步] 开始批量同步用户 - 数量: {}", employeeIds.size());

        int successCount = 0;
        int failCount = 0;

        for (Long employeeId : employeeIds) {
            try {
                String openImUserId = syncUser(employeeId);
                if (openImUserId != null) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                log.error("📱 [用户同步] 同步用户失败 - EmployeeID: {}", employeeId, e);
                failCount++;
            }
        }

        log.info("📱 [用户同步] 批量同步完成 - 成功: {}, 失败: {}", successCount, failCount);
    }
}
