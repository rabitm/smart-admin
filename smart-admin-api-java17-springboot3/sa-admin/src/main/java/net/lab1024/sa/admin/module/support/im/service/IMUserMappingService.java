package net.lab1024.sa.admin.module.support.im.service;

import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.im.dao.IMUserMappingDao;
import net.lab1024.sa.admin.module.support.im.domain.entity.IMUserMappingEntity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * IM 用户映射服务
 * 管理 SmartAdmin 用户与 OpenIM 用户的映射关系
 *
 * 核心功能:
 * - 批量获取 OpenIM 用户 ID (用于前端邀请成员)
 * - 反向查询员工 ID (用于 Webhook 回调)
 * - 缓存优化提升性能
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-10
 * @Copyright 1024创新实验室
 */
@Service
@Slf4j
public class IMUserMappingService {

    @Resource
    private IMUserMappingDao imUserMappingDao;

    /**
     * 批量获取 OpenIM 用户 ID
     * 用于前端邀请成员时获取 OpenIM 用户 ID
     *
     * 性能优化:
     * - Spring @Cacheable 缓存
     * - 分批查询处理大量用户
     * - 批量数据库查询减少 IO
     *
     * @param employeeIds 员工ID列表
     * @return Map<员工ID, OpenIM用户ID>
     */
    @Cacheable(value = "im:user:mapping:batch", key = "#employeeIds.hashCode()", unless = "#result.isEmpty()")
    public Map<Long, String> batchGetOpenIMUserIds(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            log.warn("⚠️ [用户映射] 批量查询参数为空");
            return Collections.emptyMap();
        }

        log.info("📋 [用户映射] 批量查询 OpenIM 用户ID, 数量: {}", employeeIds.size());

        // 如果超过 1000 个用户,分批查询
        if (employeeIds.size() > 1000) {
            log.info("📊 [用户映射] 用户数超过1000,启动分批查询");
            return batchQueryInChunks(employeeIds);
        }

        // 批量查询映射
        List<IMUserMappingEntity> mappings = imUserMappingDao.selectByEmployeeIds(employeeIds);

        Map<Long, String> result = mappings.stream()
                .filter(m -> m.getSyncStatus() != null && m.getSyncStatus() == 1) // 只返回已同步的用户
                .collect(Collectors.toMap(
                        IMUserMappingEntity::getEmployeeId,
                        IMUserMappingEntity::getOpenimUserId
                ));

        log.info("✅ [用户映射] 批量查询完成, 请求: {}, 命中: {}", employeeIds.size(), result.size());

        return result;
    }

    /**
     * 分批查询用户映射
     * 每批最多 1000 个用户
     */
    private Map<Long, String> batchQueryInChunks(List<Long> employeeIds) {
        // 分批,每批 1000 个
        int chunkSize = 1000;
        int totalChunks = (int) Math.ceil((double) employeeIds.size() / chunkSize);

        log.info("📦 [用户映射] 分批查询, 总数: {}, 分批数: {}, 每批大小: {}",
                employeeIds.size(), totalChunks, chunkSize);

        Map<Long, String> result = employeeIds.stream()
                .collect(Collectors.groupingBy(id -> id / chunkSize))  // 按批次分组
                .values()
                .stream()
                .flatMap(batch -> {
                    log.debug("🔍 [用户映射] 查询批次, 大小: {}", batch.size());
                    return imUserMappingDao.selectByEmployeeIds(batch).stream();
                })
                .filter(m -> m.getSyncStatus() != null && m.getSyncStatus() == 1)
                .collect(Collectors.toMap(
                        IMUserMappingEntity::getEmployeeId,
                        IMUserMappingEntity::getOpenimUserId,
                        (v1, v2) -> v1  // 如果有重复,保留第一个
                ));

        log.info("✅ [用户映射] 分批查询完成, 总命中: {}", result.size());

        return result;
    }

    /**
     * 根据 OpenIM 用户 ID 反查员工 ID
     * 用于 Webhook 回调时识别用户
     *
     * @param openimUserId OpenIM用户ID
     * @return 员工ID
     */
    @Cacheable(value = "im:user:mapping:reverse", key = "#openimUserId", unless = "#result == null")
    public Long getEmployeeIdByOpenIMUserId(String openimUserId) {
        if (openimUserId == null || openimUserId.isEmpty()) {
            log.warn("⚠️ [用户映射] OpenIM用户ID为空");
            return null;
        }

        IMUserMappingEntity mapping = imUserMappingDao.selectByOpenimUserId(openimUserId);

        if (mapping == null) {
            log.warn("⚠️ [用户映射] 未找到映射, openimUserId: {}", openimUserId);
            return null;
        }

        return mapping.getEmployeeId();
    }

    /**
     * 获取单个员工的 OpenIM 用户 ID
     *
     * @param employeeId 员工ID
     * @return OpenIM用户ID
     */
    @Cacheable(value = "im:user:mapping:single", key = "#employeeId", unless = "#result == null")
    public String getOpenIMUserId(Long employeeId) {
        if (employeeId == null) {
            log.warn("⚠️ [用户映射] 员工ID为空");
            return null;
        }

        IMUserMappingEntity mapping = imUserMappingDao.selectByEmployeeId(employeeId);

        if (mapping == null) {
            log.warn("⚠️ [用户映射] 未找到映射, employeeId: {}", employeeId);
            return null;
        }

        if (mapping.getSyncStatus() == null || mapping.getSyncStatus() != 1) {
            log.warn("⚠️ [用户映射] 用户未同步, employeeId: {}, syncStatus: {}",
                    employeeId, mapping.getSyncStatus());
            return null;
        }

        return mapping.getOpenimUserId();
    }

    /**
     * 批量获取 OpenIM 用户 ID (带去重)
     * 自动过滤重复的员工ID
     *
     * @param employeeIds 员工ID列表(可能包含重复)
     * @return Map<员工ID, OpenIM用户ID>
     */
    public Map<Long, String> batchGetOpenIMUserIdsDistinct(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 去重
        List<Long> distinctIds = employeeIds.stream()
                .distinct()
                .collect(Collectors.toList());

        log.info("📋 [用户映射] 批量查询(去重), 原始数量: {}, 去重后: {}",
                employeeIds.size(), distinctIds.size());

        return batchGetOpenIMUserIds(distinctIds);
    }
}
