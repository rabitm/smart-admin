package net.lab1024.sa.admin.module.support.im.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.im.client.OpenIMClient;
import net.lab1024.sa.admin.module.support.im.config.OpenIMConfig;
import net.lab1024.sa.admin.module.support.im.constant.IMConstant;
import net.lab1024.sa.admin.module.support.im.constant.IMErrorCodeEnum;
import net.lab1024.sa.admin.module.support.im.constant.IMOperationTypeEnum;
import net.lab1024.sa.admin.module.support.im.dao.IMUserMappingDao;
import net.lab1024.sa.admin.module.support.im.domain.entity.IMUserMappingEntity;
import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.base.common.exception.BusinessException;
import net.lab1024.sa.base.common.util.SmartStringUtil;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * IM用户同步服务
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Service
public class IMUserSyncService {

    @Resource
    private OpenIMClient openIMClient;

    @Resource
    private OpenIMConfig openIMConfig;

    @Resource
    private EmployeeDao employeeDao;

    @Resource
    private IMUserMappingDao imUserMappingDao;

    @Resource
    private IMOperationLogService imOperationLogService;

    /**
     * 同步单个员工到OpenIM
     *
     * @param employeeId 员工ID
     * @return OpenIM用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String syncUser(Long employeeId) {
        if (employeeId == null) {
            throw new BusinessException(IMErrorCodeEnum.EMPLOYEE_ID_REQUIRED);
        }

        long startTime = System.currentTimeMillis();

        try {
            // 1. 查询员工信息
            EmployeeEntity employee = employeeDao.selectById(employeeId);
            if (employee == null) {
                throw new BusinessException(IMErrorCodeEnum.USER_NOT_EXIST);
            }

            // 2. 检查是否已同步
            IMUserMappingEntity existingMapping = imUserMappingDao.selectByEmployeeId(employeeId);
            if (existingMapping != null && IMConstant.SYNC_STATUS_SUCCESS.equals(existingMapping.getSyncStatus())) {
                log.info("👤 [用户同步] 员工{}已同步,OpenIM用户ID: {}", employeeId, existingMapping.getOpenimUserId());
                return existingMapping.getOpenimUserId();
            }

            // 3. 生成OpenIM用户ID
            String openimUserId = generateOpenIMUserId(employee);

            // 4. 构建用户注册请求
            Map<String, Object> userInfo = buildUserInfo(employee, openimUserId);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("users", Collections.singletonList(userInfo));

            // 5. 调用OpenIM API注册用户
            log.info("📤 [用户同步] 开始同步员工{} -> OpenIM用户{}", employeeId, openimUserId);

            JSONObject response = openIMClient.postWithRetry(
                    IMConstant.API_USER_REGISTER,
                    requestBody,
                    JSONObject.class,
                    IMOperationTypeEnum.USER_SYNC
            );

            // 6. 保存或更新映射关系
            saveUserMapping(employee, openimUserId, IMConstant.SYNC_STATUS_SUCCESS, null);

            int executionTime = (int) (System.currentTimeMillis() - startTime);

            // 7. 记录日志
            imOperationLogService.logSuccess(
                    IMOperationTypeEnum.USER_SYNC,
                    "USER",
                    openimUserId,
                    requestBody,
                    response,
                    executionTime
            );

            log.info("✅ [用户同步] 员工{}同步成功,OpenIM用户ID: {}, 耗时: {}ms",
                    employeeId, openimUserId, executionTime);

            return openimUserId;

        } catch (BusinessException e) {
            handleSyncFailure(employeeId, startTime, e.getMessage());
            throw e;
        } catch (Exception e) {
            handleSyncFailure(employeeId, startTime, e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    /**
     * 批量同步员工
     *
     * @param employeeIds 员工ID列表
     * @return 同步结果
     */
    public Map<String, Object> batchSyncUsers(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            throw new BusinessException(IMErrorCodeEnum.PARAM_INVALID);
        }

        log.info("📦 [批量同步] 开始同步{}个员工", employeeIds.size());
        long startTime = System.currentTimeMillis();

        int successCount = 0;
        int failureCount = 0;
        List<String> successUsers = new ArrayList<>();
        List<Map<String, Object>> failures = new ArrayList<>();

        // 分批处理
        int batchSize = openIMConfig.getUserSyncBatchSize();
        List<List<Long>> batches = partition(employeeIds, batchSize);

        for (int i = 0; i < batches.size(); i++) {
            List<Long> batch = batches.get(i);
            log.info("📋 [批量同步] 处理第{}/{}批,数量: {}", i + 1, batches.size(), batch.size());

            for (Long employeeId : batch) {
                try {
                    String openimUserId = syncUser(employeeId);
                    successUsers.add(openimUserId);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    Map<String, Object> failure = new HashMap<>();
                    failure.put("employeeId", employeeId);
                    failure.put("error", e.getMessage());
                    failures.add(failure);
                    log.error("❌ [批量同步] 员工{}同步失败: {}", employeeId, e.getMessage());
                }
            }
        }

        int totalTime = (int) (System.currentTimeMillis() - startTime);

        Map<String, Object> result = new HashMap<>();
        result.put("total", employeeIds.size());
        result.put("successCount", successCount);
        result.put("failureCount", failureCount);
        result.put("successUsers", successUsers);
        result.put("failures", failures);
        result.put("executionTime", totalTime);

        log.info("✅ [批量同步] 完成 - 总数: {}, 成功: {}, 失败: {}, 耗时: {}ms",
                employeeIds.size(), successCount, failureCount, totalTime);

        return result;
    }

    /**
     * 同步所有员工
     */
    @Async("imAsyncExecutor")
    public void syncAllUsersAsync() {
        log.info("🚀 [全量同步] 开始同步所有员工");

        // 查询所有未删除的员工
        LambdaQueryWrapper<EmployeeEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EmployeeEntity::getDeletedFlag, false);
        List<EmployeeEntity> employees = employeeDao.selectList(queryWrapper);

        if (employees.isEmpty()) {
            log.warn("⚠️ [全量同步] 没有需要同步的员工");
            return;
        }

        List<Long> employeeIds = employees.stream()
                .map(EmployeeEntity::getEmployeeId)
                .collect(Collectors.toList());

        batchSyncUsers(employeeIds);
    }

    /**
     * 增量同步(仅同步未同步或同步失败的用户)
     *
     * 🔧 修复: 正确识别待同步用户,包括:
     *  - 没有映射记录的员工
     *  - sync_status != 1 的员工(NULL, 0等)
     *  - openim_user_id为空的员工
     */
    public Map<String, Object> incrementalSync() {
        log.info("🔄 [增量同步] 开始增量同步");

        // 1. 查询所有员工
        LambdaQueryWrapper<EmployeeEntity> employeeQuery = new LambdaQueryWrapper<>();
        employeeQuery.eq(EmployeeEntity::getDeletedFlag, false);
        List<EmployeeEntity> allEmployees = employeeDao.selectList(employeeQuery);

        if (allEmployees.isEmpty()) {
            log.warn("⚠️ [增量同步] 没有员工需要同步");
            Map<String, Object> result = new HashMap<>();
            result.put("message", "没有员工");
            result.put("total", 0);
            return result;
        }

        // 2. 查询已同步成功的员工
        List<IMUserMappingEntity> allMappings = imUserMappingDao.selectList(null);
        Map<Long, IMUserMappingEntity> mappingMap = allMappings.stream()
                .collect(Collectors.toMap(
                        IMUserMappingEntity::getEmployeeId,
                        m -> m,
                        (m1, m2) -> m1
                ));

        // 3. 找出需要同步的员工
        List<Long> toSyncEmployeeIds = new ArrayList<>();

        for (EmployeeEntity employee : allEmployees) {
            Long employeeId = employee.getEmployeeId();
            IMUserMappingEntity mapping = mappingMap.get(employeeId);

            boolean needSync = false;
            String reason = "";

            if (mapping == null) {
                // 情况1: 没有映射记录
                needSync = true;
                reason = "无映射记录";
            } else if (mapping.getOpenimUserId() == null || mapping.getOpenimUserId().isEmpty()) {
                // 情况2: openim_user_id为空
                needSync = true;
                reason = "OpenIM用户ID为空";
            } else if (!IMConstant.SYNC_STATUS_SUCCESS.equals(mapping.getSyncStatus())) {
                // 情况3: sync_status不是1(成功)
                needSync = true;
                reason = String.format("同步状态异常(status=%s)", mapping.getSyncStatus());
            }

            if (needSync) {
                toSyncEmployeeIds.add(employeeId);
                log.debug("📋 [增量同步] 员工{}需要同步: {}", employeeId, reason);
            }
        }

        if (toSyncEmployeeIds.isEmpty()) {
            log.info("✅ [增量同步] 所有员工已同步 (总员工数: {})", allEmployees.size());
            Map<String, Object> result = new HashMap<>();
            result.put("message", "所有员工已同步");
            result.put("total", 0);
            result.put("totalEmployees", allEmployees.size());
            result.put("successCount", 0);
            result.put("failureCount", 0);
            result.put("executionTime", 0);
            return result;
        }

        log.info("📋 [增量同步] 发现{}个待同步员工(总员工数: {}, 待同步: {})",
                toSyncEmployeeIds.size(), allEmployees.size(), toSyncEmployeeIds.size());

        // 4. 执行批量同步
        return batchSyncUsers(toSyncEmployeeIds);
    }

    /**
     * 根据员工ID生成OpenIM用户ID
     */
    private String generateOpenIMUserId(EmployeeEntity employee) {
        // 使用员工UID或ID生成
        if (SmartStringUtil.isNotBlank(employee.getEmployeeUid())) {
            return IMConstant.USER_ID_PREFIX + employee.getEmployeeUid();
        }
        return IMConstant.USER_ID_PREFIX + employee.getEmployeeId();
    }

    /**
     * 构建用户信息
     */
    private Map<String, Object> buildUserInfo(EmployeeEntity employee, String openimUserId) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userID", openimUserId);
        userInfo.put("nickname", employee.getActualName());
        userInfo.put("faceURL", employee.getAvatar());

        // 扩展字段
        Map<String, Object> ex = new HashMap<>();
        ex.put("employeeId", employee.getEmployeeId());
        ex.put("departmentId", employee.getDepartmentId());
        ex.put("positionId", employee.getPositionId());
        userInfo.put("ex", JSON.toJSONString(ex));

        return userInfo;
    }

    /**
     * 保存用户映射
     */
    private void saveUserMapping(EmployeeEntity employee, String openimUserId,
                                 Integer syncStatus, String errorMessage) {
        IMUserMappingEntity existingMapping = imUserMappingDao.selectByEmployeeId(employee.getEmployeeId());

        if (existingMapping != null) {
            // 更新现有映射
            existingMapping.setOpenimUserId(openimUserId);
            existingMapping.setNickname(employee.getActualName());
            existingMapping.setFaceUrl(employee.getAvatar());
            existingMapping.setSyncStatus(syncStatus);
            existingMapping.setSyncTime(LocalDateTime.now());
            existingMapping.setErrorMessage(errorMessage);
            existingMapping.setUpdateTime(LocalDateTime.now());
            imUserMappingDao.updateById(existingMapping);
        } else {
            // 创建新映射
            IMUserMappingEntity newMapping = new IMUserMappingEntity();
            newMapping.setEmployeeId(employee.getEmployeeId());
            newMapping.setOpenimUserId(openimUserId);
            newMapping.setNickname(employee.getActualName());
            newMapping.setFaceUrl(employee.getAvatar());
            newMapping.setSyncStatus(syncStatus);
            newMapping.setSyncTime(LocalDateTime.now());
            newMapping.setErrorMessage(errorMessage);
            newMapping.setDeletedFlag(false);
            imUserMappingDao.insert(newMapping);
        }
    }

    /**
     * 处理同步失败
     */
    private void handleSyncFailure(Long employeeId, long startTime, String errorMessage) {
        int executionTime = (int) (System.currentTimeMillis() - startTime);

        try {
            // 查询员工信息
            EmployeeEntity employee = employeeDao.selectById(employeeId);
            if (employee != null) {
                String openimUserId = generateOpenIMUserId(employee);
                saveUserMapping(employee, openimUserId, IMConstant.SYNC_STATUS_FAILED, errorMessage);
            }
        } catch (Exception e) {
            log.error("❌ [用户同步] 保存失败记录出错", e);
        }

        imOperationLogService.logFailure(
                IMOperationTypeEnum.USER_SYNC,
                "USER",
                String.valueOf(employeeId),
                null,
                errorMessage,
                executionTime
        );

        log.error("❌ [用户同步] 员工{}同步失败: {}, 耗时: {}ms", employeeId, errorMessage, executionTime);
    }

    /**
     * 分批处理列表
     */
    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    /**
     * 根据员工ID获取OpenIM用户ID
     *
     * 🔧 修复: 添加用户验证逻辑,防止数据库有映射但OpenIM服务器上无用户的情况
     */
    public String getOpenIMUserId(Long employeeId) {
        IMUserMappingEntity mapping = imUserMappingDao.selectByEmployeeId(employeeId);

        // 如果映射存在且同步成功
        if (mapping != null && IMConstant.SYNC_STATUS_SUCCESS.equals(mapping.getSyncStatus())) {
            // 🔧 新增: 验证用户在OpenIM服务器上是否真实存在
            if (verifyUserExistsOnServer(mapping.getOpenimUserId())) {
                log.debug("✅ [用户验证] 员工{}的用户已存在且已验证: {}", employeeId, mapping.getOpenimUserId());
                return mapping.getOpenimUserId();
            } else {
                log.warn("⚠️ [用户验证] 数据库中存在用户映射,但OpenIM服务器上不存在,删除旧映射重新同步");
                // 删除旧映射,触发重新同步
                imUserMappingDao.deleteById(mapping.getId());
            }
        }

        // 如果未同步或验证失败,尝试同步
        return syncUser(employeeId);
    }

    /**
     * 验证用户在OpenIM服务器上是否存在
     *
     * @param openimUserId OpenIM用户ID
     * @return true-存在, false-不存在
     */
    private boolean verifyUserExistsOnServer(String openimUserId) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userIDs", Collections.singletonList(openimUserId));

            JSONObject response = openIMClient.postWithRetry(
                    IMConstant.API_USER_GET_INFO,
                    requestBody,
                    JSONObject.class,
                    IMOperationTypeEnum.USER_SYNC
            );

            // 检查返回的 usersInfo 是否包含该用户
            if (response != null && response.containsKey("usersInfo")) {
                Object usersInfo = response.get("usersInfo");
                if (usersInfo instanceof List) {
                    List<?> infos = (List<?>) usersInfo;
                    return !infos.isEmpty();
                }
            }

            return false;
        } catch (Exception e) {
            log.warn("⚠️ [用户验证] 验证用户{}是否存在失败: {}", openimUserId, e.getMessage());
            // 验证失败时,假定用户不存在,触发重新同步
            return false;
        }
    }

    /**
     * 批量获取OpenIM用户ID
     */
    public Map<Long, String> batchGetOpenIMUserIds(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<IMUserMappingEntity> mappings = imUserMappingDao.selectByEmployeeIds(employeeIds);

        return mappings.stream()
                .filter(m -> IMConstant.SYNC_STATUS_SUCCESS.equals(m.getSyncStatus()))
                .collect(Collectors.toMap(
                        IMUserMappingEntity::getEmployeeId,
                        IMUserMappingEntity::getOpenimUserId,
                        (v1, v2) -> v1
                ));
    }
}
