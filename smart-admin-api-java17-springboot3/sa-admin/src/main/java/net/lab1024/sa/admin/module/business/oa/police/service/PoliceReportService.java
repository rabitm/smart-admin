package net.lab1024.sa.admin.module.business.oa.police.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.oa.police.constant.PoliceReportStatusEnum;
import net.lab1024.sa.admin.module.business.oa.police.dao.PoliceReportDao;
import net.lab1024.sa.admin.module.business.oa.police.dao.PoliceReportFieldDataDao;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceReportEntity;
import net.lab1024.sa.admin.module.business.oa.police.domain.entity.PoliceReportFieldDataEntity;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.PoliceReportAddForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.PoliceReportQueryForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.form.PoliceReportUpdateForm;
import net.lab1024.sa.admin.module.business.oa.police.domain.vo.PoliceReportVO;
import net.lab1024.sa.admin.module.business.oa.police.domain.vo.DataVersionVO;
import net.lab1024.sa.base.common.domain.PageResult;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.common.code.SystemErrorCode;
import net.lab1024.sa.base.common.util.SmartBeanUtil;
import net.lab1024.sa.base.common.util.SmartPageUtil;
import net.lab1024.sa.base.common.util.SmartRequestUtil;
import net.lab1024.sa.base.module.support.datatracer.constant.DataTracerConst;
import net.lab1024.sa.base.module.support.datatracer.constant.DataTracerTypeEnum;
import net.lab1024.sa.admin.module.business.oa.police.service.sync.SyncService;
import net.lab1024.sa.admin.module.business.oa.seat.service.SeatSyncService;
import net.lab1024.sa.base.module.support.datatracer.service.DataTracerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.lab1024.sa.admin.module.business.im.listener.PoliceGroupAutoCreateListener;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 警情录入Service
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-18
 */
@Service
@Slf4j
public class PoliceReportService {

    @Resource
    private PoliceReportDao policeReportDao;

    @Resource
    private PoliceReportFieldDataDao policeReportFieldDataDao;

    @Resource
    private DataTracerService dataTracerService;

    @Resource
    private SyncService syncService;

    @Resource
    private SeatSyncService seatSyncService;

    @Resource
    private PoliceListUpdateService policeListUpdateService;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 分页查询警情信息
     */
    public ResponseDTO<PageResult<PoliceReportVO>> queryByPage(PoliceReportQueryForm queryForm) {
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<PoliceReportVO> policeReportList = policeReportDao.queryPage(page, queryForm);
        PageResult<PoliceReportVO> pageResult = SmartPageUtil.convert2PageResult(page, policeReportList);
        return ResponseDTO.ok(pageResult);
    }

    /**
     * 查询警情信息详情
     */
    public ResponseDTO<PoliceReportVO> getDetail(Long reportId) {
        // 校验警情信息是否存在
        PoliceReportVO policeReportVO = policeReportDao.getDetail(reportId, Boolean.FALSE);
        if (Objects.isNull(policeReportVO)) {
            return ResponseDTO.userErrorParam("警情信息不存在");
        }
        return ResponseDTO.ok(policeReportVO);
    }

    /**
     * 新增警情信息
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> addPoliceReport(PoliceReportAddForm addForm) {
        // 生成警情编号
        String reportNumber = generateReportNumber();

        // 验证警情编号是否重复（理论上不应该重复，但保险起见）
        PoliceReportEntity existingReport = policeReportDao.queryByReportNumber(reportNumber, null, Boolean.FALSE);
        if (Objects.nonNull(existingReport)) {
            return ResponseDTO.userErrorParam("警情编号已存在，请重试");
        }

        // 数据插入
        PoliceReportEntity policeReportEntity = SmartBeanUtil.copy(addForm, PoliceReportEntity.class);
        policeReportEntity.setReportNumber(reportNumber);
        policeReportEntity.setStatus(PoliceReportStatusEnum.PENDING.getValue()); // 默认待处理状态
        policeReportEntity.setDeletedFlag(Boolean.FALSE);

        policeReportDao.insert(policeReportEntity);

        // 保存专业字段数据
        saveProfessionalFields(policeReportEntity.getReportId(), addForm.getProfessionalFields());

        // 数据追踪
        dataTracerService.addTrace(policeReportEntity.getReportId(), DataTracerTypeEnum.OA_ENTERPRISE,
            "新增警情:" + DataTracerConst.HTML_BR + dataTracerService.getChangeContent(policeReportEntity));

        // 发送警情新增实时通知
        try {
            PoliceReportVO newData = SmartBeanUtil.copy(policeReportEntity, PoliceReportVO.class);
            String currentUserName = SmartRequestUtil.getRequestUser() != null ?
                SmartRequestUtil.getRequestUser().getUserName() : "系统";
            Long currentUserId = SmartRequestUtil.getRequestUser() != null ?
                SmartRequestUtil.getRequestUser().getUserId() : null;
            seatSyncService.notifyPoliceCaseUpdate(policeReportEntity.getReportId(), currentUserId, currentUserName, newData);
            // 发送列表刷新通知
            seatSyncService.notifyPoliceListRefresh(currentUserId, currentUserName, "新增了警情");

            // 高性能列表实时更新通知
            policeListUpdateService.handleReportInsert(policeReportEntity,
                String.valueOf(currentUserId), currentUserName);
        } catch (Exception e) {
            log.error("发送警情新增实时通知失败", e);
        }

        // 发布警情创建事件,触发OpenIM群组自动创建
        try {
            eventPublisher.publishEvent(new PoliceGroupAutoCreateListener.PoliceReportCreatedEvent(policeReportEntity));
            log.info("📱 [警情服务] 发布警情创建事件 - 警情ID: {}, 警情编号: {}",
                    policeReportEntity.getReportId(), policeReportEntity.getReportNumber());
        } catch (Exception e) {
            log.error("📱 [警情服务] 发布警情创建事件失败", e);
        }

        return ResponseDTO.ok();
    }

    /**
     * 更新警情信息
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> updatePoliceReport(PoliceReportUpdateForm updateForm) {
        Long reportId = updateForm.getReportId();

        // 校验警情信息是否存在
        PoliceReportEntity oldPoliceReport = policeReportDao.selectById(reportId);
        if (Objects.isNull(oldPoliceReport) || oldPoliceReport.getDeletedFlag()) {
            return ResponseDTO.userErrorParam("警情信息不存在");
        }

        // 记录详细的字段变更信息（在更新前）
        String detailedChanges = buildDetailedChangeLog(oldPoliceReport, updateForm);
        log.info("警情更新详细记录: reportId={}, changes={}", reportId, detailedChanges);

        // 数据更新
        PoliceReportEntity updateEntity = SmartBeanUtil.copy(updateForm, PoliceReportEntity.class);
        policeReportDao.updateById(updateEntity);

        // 更新专业字段数据
        if (updateForm.getProfessionalFields() != null) {
            // 先删除旧的专业字段数据
            policeReportFieldDataDao.deleteByReportId(reportId);
            // 保存新的专业字段数据
            saveProfessionalFields(reportId, updateForm.getProfessionalFields());
        }

        // 数据追踪
        dataTracerService.addTrace(reportId, DataTracerTypeEnum.OA_ENTERPRISE,
            "更新警情:" + DataTracerConst.HTML_BR + dataTracerService.getChangeContent(oldPoliceReport, updateEntity));

        // 发送警情更新实时通知
        try {
            // 构建更新数据，需要重新从数据库查询完整信息
            PoliceReportEntity updatedEntity = policeReportDao.selectById(reportId);
            PoliceReportVO updatedData = SmartBeanUtil.copy(updatedEntity, PoliceReportVO.class);

            // 获取专业字段数据
            ResponseDTO<Map<String, Object>> fieldDataResponse = getPoliceReportFieldData(reportId);
            if (fieldDataResponse.getOk() && fieldDataResponse.getData() != null) {
                updatedData.setProfessionalFields(fieldDataResponse.getData());
            }

            String currentUserName = SmartRequestUtil.getRequestUser() != null ?
                SmartRequestUtil.getRequestUser().getUserName() : "系统";
            Long currentUserId = SmartRequestUtil.getRequestUser() != null ?
                SmartRequestUtil.getRequestUser().getUserId() : null;

            seatSyncService.notifyPoliceCaseUpdate(reportId, currentUserId, currentUserName, updatedData);
            // 发送列表刷新通知
            seatSyncService.notifyPoliceListRefresh(currentUserId, currentUserName, "更新了警情");

            // 高性能列表实时更新通知
            Map<String, Object> changes = buildChangeMap(oldPoliceReport, updateEntity);
            policeListUpdateService.handleReportUpdate(reportId, changes,
                String.valueOf(currentUserId), currentUserName);
        } catch (Exception e) {
            // 不因为同步失败而影响业务操作
            log.error("发送警情更新实时通知失败", e);
        }

        return ResponseDTO.ok();
    }

    /**
     * 同步字段更新 - 重构版本
     */
    // 高并发批量处理队列
    private final Map<Long, Map<String, Object>> batchUpdateQueue = new ConcurrentHashMap<>();
    private final AtomicLong lastBatchProcess = new AtomicLong(0);
    private static final int BATCH_INTERVAL_MS = 100; // 100ms批量处理间隔

    /**
     * 获取指定字段的值（支持主实体字段和专业字段）
     */
    public String getFieldValue(Long reportId, String fieldName) {
        try {
            // 检查是否为主实体字段
            if (isBasicField(fieldName)) {
                return getMainEntityFieldValue(reportId, fieldName);
            } else {
                return getProfessionalFieldValue(reportId, fieldName);
            }
        } catch (Exception e) {
            log.error("获取字段值失败: fieldName={}", fieldName, e);
            return null;
        }
    }

    /**
     * 获取主实体字段值
     */
    private String getMainEntityFieldValue(Long reportId, String fieldName) {
        try {
            PoliceReportEntity entity = policeReportDao.selectById(reportId);
            if (entity == null) {
                return null;
            }

            // 使用反射获取字段值
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(entity);
            return value != null ? String.valueOf(value) : null;
        } catch (NoSuchFieldException e) {
            log.debug("字段不在主实体中: fieldName={}", fieldName);
            return null;
        } catch (Exception e) {
            log.error("获取主实体字段值失败: fieldName={}", fieldName, e);
            return null;
        }
    }

    /**
     * 获取专业字段值
     */
    private String getProfessionalFieldValue(Long reportId, String fieldName) {
        try {
            PoliceReportFieldDataEntity fieldData = policeReportFieldDataDao.selectOne(
                new LambdaQueryWrapper<PoliceReportFieldDataEntity>()
                    .eq(PoliceReportFieldDataEntity::getReportId, reportId)
                    .eq(PoliceReportFieldDataEntity::getFieldKey, fieldName)
            );

            return fieldData != null ? fieldData.getFieldValue() : null;
        } catch (Exception e) {
            log.error("获取专业字段值失败: fieldName={}", fieldName, e);
            return null;
        }
    }

    public ResponseDTO<String> syncFieldUpdate(Long reportId, String fieldName, String fieldValue) {
        try {
            String currentUserName = SmartRequestUtil.getRequestUser() != null ?
                SmartRequestUtil.getRequestUser().getUserName() : "系统";
            Long currentUserId = SmartRequestUtil.getRequestUser() != null ?
                SmartRequestUtil.getRequestUser().getUserId() : null;

            log.debug("字段同步请求: reportId={}, fieldName={}, userId={}", reportId, fieldName, currentUserId);

            // 添加到批量处理队列（高并发优化）
            batchUpdateQueue.computeIfAbsent(reportId, k -> new ConcurrentHashMap<>())
                .put(fieldName, fieldValue);

            // 触发批量处理
            scheduleBatchProcess(currentUserId, currentUserName);

            return ResponseDTO.ok();
        } catch (Exception e) {
            log.error("同步字段更新失败", e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 同步字段更新 - 包含旧值和新值的版本
     */
    public ResponseDTO<String> syncFieldUpdateWithOldValue(Long reportId, String fieldName, String oldValue, String newValue) {
        try {
            String currentUserName = SmartRequestUtil.getRequestUser() != null ?
                SmartRequestUtil.getRequestUser().getUserName() : "系统";
            Long currentUserId = SmartRequestUtil.getRequestUser() != null ?
                SmartRequestUtil.getRequestUser().getUserId() : null;

            log.info("字段更新记录: reportId={}, fieldName={}, oldValue={}, newValue={}", reportId, fieldName, oldValue, newValue);

            // 跳过特殊控制字段
            if ("__FORM_CONFIG_UPDATE__".equals(fieldName)) {
                log.debug("跳过表单配置更新字段: {}", fieldName);
                return ResponseDTO.ok("表单配置更新字段已跳过");
            }

            // 直接更新数据库，避免批量处理的并发问题
            updateSingleField(reportId, fieldName, newValue);

            // 🚀 [重要修复] 调用同步服务进行字段同步（RocketMQ或WebSocket）
            try {
                log.info("🚀 [字段同步] 调用同步服务: reportId={}, fieldName={}, userId={}, userName={}",
                        reportId, fieldName, currentUserId, currentUserName);
                syncService.syncFieldUpdate(reportId, currentUserId, currentUserName,
                    fieldName, oldValue, newValue, "FIELD_UPDATE");
                log.info("✅ [字段同步] 同步服务调用成功");
            } catch (Exception e) {
                log.error("❌ [字段同步] 同步服务调用失败", e);
            }

            // 保留原有WebSocket通知作为兼容性备份
            try {
                seatSyncService.notifyPoliceCaseFieldSync(reportId, currentUserId, currentUserName, fieldName, newValue);
            } catch (Exception e) {
                log.error("发送WebSocket通知失败", e);
            }

            return ResponseDTO.ok();
        } catch (Exception e) {
            log.error("同步字段更新失败", e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 直接更新单个字段到数据库（支持主实体字段和专业字段）
     */
    private void updateSingleField(Long reportId, String fieldName, String newValue) {
        try {
            // 检查是否为主实体字段
            if (isBasicField(fieldName)) {
                updateMainEntityField(reportId, fieldName, newValue);
            } else {
                updateProfessionalField(reportId, fieldName, newValue);
            }

            log.debug("字段更新成功: reportId={}, fieldName={}, newValue={}", reportId, fieldName, newValue);

        } catch (Exception e) {
            log.error("更新字段失败: reportId={}, fieldName={}, newValue={}", reportId, fieldName, newValue, e);
            throw new RuntimeException("字段更新失败", e);
        }
    }

    /**
     * 更新主实体字段
     */
    private void updateMainEntityField(Long reportId, String fieldName, String newValue) {
        try {
            // 获取当前警情实体
            PoliceReportEntity entity = policeReportDao.selectById(reportId);
            if (entity == null) {
                log.warn("警情不存在: reportId={}", reportId);
                return;
            }

            // 使用反射更新字段值
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);

            // 根据字段类型设置值
            if (field.getType() == String.class) {
                field.set(entity, String.valueOf(newValue));
            } else if (field.getType() == Integer.class || field.getType() == int.class) {
                field.set(entity, Integer.valueOf(String.valueOf(newValue)));
            } else if (field.getType() == Long.class || field.getType() == long.class) {
                field.set(entity, Long.valueOf(String.valueOf(newValue)));
            } else {
                field.set(entity, newValue);
            }

            // 更新数据库
            policeReportDao.updateById(entity);

        } catch (Exception e) {
            log.error("更新主实体字段失败: reportId={}, fieldName={}, newValue={}", reportId, fieldName, newValue, e);
            throw new RuntimeException("主实体字段更新失败", e);
        }
    }

    /**
     * 更新专业字段
     */
    private void updateProfessionalField(Long reportId, String fieldName, String newValue) {
        try {
            // 检查字段是否存在
            PoliceReportFieldDataEntity existingField = policeReportFieldDataDao.selectOne(
                new LambdaQueryWrapper<PoliceReportFieldDataEntity>()
                    .eq(PoliceReportFieldDataEntity::getReportId, reportId)
                    .eq(PoliceReportFieldDataEntity::getFieldKey, fieldName)
            );

            if (existingField != null) {
                // 更新现有字段
                existingField.setFieldValue(newValue);
                existingField.setUpdateTime(LocalDateTime.now());
                policeReportFieldDataDao.updateById(existingField);
            } else {
                // 创建新字段
                PoliceReportFieldDataEntity newField = new PoliceReportFieldDataEntity();
                newField.setReportId(reportId);
                newField.setFieldKey(fieldName);
                newField.setFieldValue(newValue);
                newField.setCreateTime(LocalDateTime.now());
                newField.setUpdateTime(LocalDateTime.now());
                policeReportFieldDataDao.insert(newField);
            }

        } catch (Exception e) {
            log.error("更新专业字段失败: reportId={}, fieldName={}, newValue={}", reportId, fieldName, newValue, e);
            throw new RuntimeException("专业字段更新失败", e);
        }
    }


    /**
     * 安排批量处理
     */
    private void scheduleBatchProcess(Long userId, String userName) {
        long now = System.currentTimeMillis();
        long lastProcess = lastBatchProcess.get();

        // 如果距离上次处理超过间隔时间，立即处理
        if (now - lastProcess > BATCH_INTERVAL_MS) {
            if (lastBatchProcess.compareAndSet(lastProcess, now)) {
                CompletableFuture.runAsync(() -> processBatchUpdates(userId, userName));
            }
        }
    }

    /**
     * 处理批量更新
     */
    @Async
    public void processBatchUpdates(Long userId, String userName) {
        if (batchUpdateQueue.isEmpty()) {
            return;
        }

        // 获取当前队列快照并清空
        Map<Long, Map<String, Object>> currentBatch = new HashMap<>(batchUpdateQueue);
        batchUpdateQueue.clear();

        long startTime = System.currentTimeMillis();
        int totalUpdates = currentBatch.values().stream()
            .mapToInt(Map::size)
            .sum();

        log.info("🚀 [批量处理] 开始处理 {} 个报告的 {} 个字段更新", currentBatch.size(), totalUpdates);

        try {
            // 批量处理每个报告的更新
            List<CompletableFuture<Void>> futures = currentBatch.entrySet().stream()
                .map(entry -> CompletableFuture.runAsync(() -> {
                    Long reportId = entry.getKey();
                    Map<String, Object> changes = entry.getValue();

                    try {
                        // 获取当前警情实体用于更新
                        PoliceReportEntity entity = policeReportDao.selectById(reportId);
                        if (entity == null) {
                            log.warn("警情不存在: reportId={}", reportId);
                            return;
                        }

                        // 同步字段更新（现在会获取旧值）
                        changes.forEach((fieldName, fieldValue) -> {
                            try {
                                // 在更新前获取旧值
                                String oldValue = getFieldValue(reportId, fieldName);

                                // 使用反射更新字段值
                                Field field = entity.getClass().getDeclaredField(fieldName);
                                field.setAccessible(true);

                                // 根据字段类型设置值
                                if (field.getType() == String.class) {
                                    field.set(entity, String.valueOf(fieldValue));
                                } else if (field.getType() == Integer.class || field.getType() == int.class) {
                                    field.set(entity, Integer.valueOf(String.valueOf(fieldValue)));
                                } else if (field.getType() == Long.class || field.getType() == long.class) {
                                    field.set(entity, Long.valueOf(String.valueOf(fieldValue)));
                                } else {
                                    field.set(entity, fieldValue);
                                }

                                // 记录操作日志
                                syncService.recordOperation(reportId, userId, userName,
                                    fieldName, oldValue, String.valueOf(fieldValue), "FIELD_UPDATE");

                            } catch (Exception e) {
                                log.error("更新字段失败: fieldName={}, fieldValue={}", fieldName, fieldValue, e);
                            }
                        });

                        // 批量更新数据库
                        policeReportDao.updateById(entity);

                        // 检查是否包含关键字段，触发列表更新
                        Map<String, Object> keyChanges = changes.entrySet().stream()
                            .filter(e -> isKeyFieldForList(e.getKey()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                        if (!keyChanges.isEmpty()) {
                            policeListUpdateService.handleReportUpdate(reportId, keyChanges,
                                String.valueOf(userId), userName);
                        }

                    } catch (Exception e) {
                        log.error("❌ [批量处理] 处理报告更新失败: reportId={}", reportId, e);
                    }
                }))
                .collect(Collectors.toList());

            // 等待所有更新完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(5, TimeUnit.SECONDS)
                .get();

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ [批量处理] 完成 {} 个更新，耗时 {}ms", totalUpdates, duration);

            // 性能监控
            if (duration > 2000) {
                log.warn("🐌 [性能警告] 批量处理耗时过长: {}ms", duration);
            }

        } catch (Exception e) {
            log.error("❌ [批量处理] 批量处理失败", e);
        }
    }

    /**
     * 判断是否是影响列表显示的关键字段
     */
    private boolean isKeyFieldForList(String fieldName) {
        return fieldName != null && (
            fieldName.equals("reportType") ||
            fieldName.equals("reportLevel") ||
            fieldName.equals("status") ||
            fieldName.equals("reporterName") ||
            fieldName.equals("reporterPhone") ||
            fieldName.equals("incidentLocation") ||
            fieldName.equals("description") ||
            fieldName.equals("handlerName")
        );
    }

    /**
     * 获取警情操作历史
     */
    public ResponseDTO<Object> getOperationHistory(Long reportId) {
        try {
            log.info("获取操作历史: reportId={}", reportId);
            Object history = syncService.getOperationHistory(reportId);
            return ResponseDTO.ok(history);
        } catch (Exception e) {
            log.error("获取操作历史失败", e);
            return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 删除警情信息
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> deletePoliceReport(Long reportId) {
        // 校验警情信息是否存在
        PoliceReportEntity policeReportEntity = policeReportDao.selectById(reportId);
        if (Objects.isNull(policeReportEntity) || policeReportEntity.getDeletedFlag()) {
            return ResponseDTO.userErrorParam("警情信息不存在");
        }

        policeReportDao.deletePoliceReport(reportId, Boolean.TRUE);

        // 数据追踪
        dataTracerService.addTrace(reportId, DataTracerTypeEnum.OA_ENTERPRISE,
            "删除警情:" + DataTracerConst.HTML_BR + dataTracerService.getChangeContent(policeReportEntity));

        // 发送列表刷新通知
        try {
            String currentUserName = SmartRequestUtil.getRequestUser() != null ?
                SmartRequestUtil.getRequestUser().getUserName() : "系统";
            Long currentUserId = SmartRequestUtil.getRequestUser() != null ?
                SmartRequestUtil.getRequestUser().getUserId() : null;
            seatSyncService.notifyPoliceListRefresh(currentUserId, currentUserName, "删除了警情");

            // 高性能列表实时更新通知
            policeListUpdateService.handleReportDelete(reportId,
                String.valueOf(currentUserId), currentUserName);
        } catch (Exception e) {
            log.error("发送警情删除通知失败", e);
        }

        return ResponseDTO.ok();
    }

    /**
     * 根据报警人电话查询警情列表
     */
    public ResponseDTO<List<PoliceReportVO>> queryByReporterPhone(String reporterPhone) {
        List<PoliceReportVO> policeReportList = policeReportDao.queryByReporterPhone(reporterPhone, Boolean.FALSE);
        return ResponseDTO.ok(policeReportList);
    }

    /**
     * 根据处理人员查询警情列表
     */
    public ResponseDTO<List<PoliceReportVO>> queryByHandler(Long handlerId) {
        List<PoliceReportVO> policeReportList = policeReportDao.queryByHandler(handlerId, Boolean.FALSE);
        return ResponseDTO.ok(policeReportList);
    }

    /**
     * 获取各状态警情统计
     */
    public ResponseDTO<List<PoliceReportVO>> getStatusStatistics() {
        List<PoliceReportVO> statistics = policeReportDao.getStatusStatistics(Boolean.FALSE);
        return ResponseDTO.ok(statistics);
    }

    /**
     * 生成警情编号
     * 格式：POLICE-YYYYMMDD-XXX（XXX为当天的序号，从001开始）
     */
    private String generateReportNumber() {
        LocalDateTime now = LocalDateTime.now();
        String datePrefix = "POLICE-" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 获取当天最大编号
        String maxReportNumber = policeReportDao.getMaxReportNumberByDate(datePrefix);

        int sequence = 1;
        if (maxReportNumber != null && maxReportNumber.length() > datePrefix.length()) {
            try {
                String sequenceStr = maxReportNumber.substring(datePrefix.length() + 1);
                sequence = Integer.parseInt(sequenceStr) + 1;
            } catch (NumberFormatException e) {
                log.warn("解析警情编号序号失败: {}", maxReportNumber, e);
                // 发生异常时使用当前时间戳后3位作为序号，避免重复
                sequence = (int) (System.currentTimeMillis() % 1000);
            }
        }

        return datePrefix + "-" + String.format("%03d", sequence);
    }

    /**
     * 保存专业字段数据
     */
    private void saveProfessionalFields(Long reportId, Map<String, Object> professionalFields) {
        log.info("开始保存专业字段数据 - reportId: {}, professionalFields: {}", reportId, professionalFields);

        if (professionalFields == null || professionalFields.isEmpty()) {
            log.warn("专业字段数据为空 - reportId: {}", reportId);
            return;
        }

        List<PoliceReportFieldDataEntity> fieldDataList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : professionalFields.entrySet()) {
            String fieldKey = entry.getKey();
            Object fieldValue = entry.getValue();

            log.debug("处理字段 - key: {}, value: {}, isBasicField: {}", fieldKey, fieldValue, isBasicField(fieldKey));

            // 跳过基本字段，只保存专业字段
            if (isBasicField(fieldKey)) {
                log.debug("跳过基本字段: {}", fieldKey);
                continue;
            }

            // 跳过空值
            if (fieldValue == null || (fieldValue instanceof String && ((String) fieldValue).trim().isEmpty())) {
                log.debug("跳过空值字段: {}", fieldKey);
                continue;
            }

            PoliceReportFieldDataEntity fieldData = new PoliceReportFieldDataEntity();
            fieldData.setReportId(reportId);
            fieldData.setFieldKey(fieldKey);

            // 处理数组类型的字段值，将其序列化为JSON字符串
            String fieldValueStr;
            if (fieldValue instanceof List || fieldValue instanceof ArrayList) {
                try {
                    fieldValueStr = objectMapper.writeValueAsString(fieldValue);
                } catch (Exception e) {
                    log.warn("序列化数组字段值失败: {} = {}", fieldKey, fieldValue, e);
                    fieldValueStr = fieldValue.toString();
                }
            } else {
                fieldValueStr = fieldValue.toString();
            }

            fieldData.setFieldValue(fieldValueStr);
            fieldDataList.add(fieldData);

            log.info("准备保存专业字段: {} = {}", fieldKey, fieldValue);
        }

        if (!fieldDataList.isEmpty()) {
            log.info("批量保存 {} 个专业字段数据", fieldDataList.size());
            policeReportFieldDataDao.batchInsertFieldData(fieldDataList);
            log.info("专业字段数据保存成功");
        } else {
            log.warn("没有有效的专业字段数据需要保存");
        }
    }

    /**
     * 判断是否为基本字段
     */
    private boolean isBasicField(String fieldKey) {
        return "reportType".equals(fieldKey) ||
               "reportLevel".equals(fieldKey) ||
               "reporterName".equals(fieldKey) ||
               "reporterPhone".equals(fieldKey) ||
               "reporterIdCard".equals(fieldKey) ||
               "reportTime".equals(fieldKey) ||
               "incidentLocation".equals(fieldKey) ||
               "description".equals(fieldKey) ||
               "handlerId".equals(fieldKey) ||
               "handlerName".equals(fieldKey) ||
               "attachments".equals(fieldKey) ||
               "remark".equals(fieldKey);
    }

    /**
     * 搜索地址建议（历史 + 模拟）
     */
    public ResponseDTO<List<String>> searchLocationSuggestions(String keyword) {
        if (keyword == null || keyword.trim().length() < 2) {
            return ResponseDTO.ok(new java.util.ArrayList<>());
        }

        try {
            List<String> suggestions = new java.util.ArrayList<>();

            // 获取历史地址建议
            try {
                List<String> historySuggestions = policeReportDao.getLocationSuggestions(keyword.trim(), Boolean.FALSE);
                if (historySuggestions != null && !historySuggestions.isEmpty()) {
                    suggestions.addAll(historySuggestions);
                }
            } catch (Exception e) {
                log.warn("获取历史地址建议失败: {}", e.getMessage());
            }

            // 添加模拟地址建议
            String trimmedKeyword = keyword.trim();
            suggestions.add(trimmedKeyword + "街道");
            suggestions.add(trimmedKeyword + "大道");
            suggestions.add(trimmedKeyword + "路");
            suggestions.add(trimmedKeyword + "小区");
            suggestions.add(trimmedKeyword + "广场");
            suggestions.add(trimmedKeyword + "商场");

            // 去重并限制返回数量
            List<String> result = suggestions.stream()
                .distinct()
                .limit(6)
                .collect(java.util.stream.Collectors.toList());

            log.info("地址建议搜索 - 关键字: {}, 结果数量: {}", keyword, result.size());
            return ResponseDTO.ok(result);

        } catch (Exception e) {
            log.error("搜索地址建议出现异常: {}", e.getMessage(), e);
            return ResponseDTO.ok(new java.util.ArrayList<>());
        }
    }

    /**
     * 获取警情专业字段数据
     */
    public ResponseDTO<Map<String, Object>> getPoliceReportFieldData(Long reportId) {
        try {
            List<PoliceReportFieldDataEntity> fieldDataList = policeReportFieldDataDao.getFieldDataByReportId(reportId);
            Map<String, Object> fieldDataMap = new HashMap<>();

            for (PoliceReportFieldDataEntity fieldData : fieldDataList) {
                String fieldValue = fieldData.getFieldValue();
                Object parsedValue;

                // 尝试将JSON数组字符串解析为List对象
                if (fieldValue != null && fieldValue.startsWith("[") && fieldValue.endsWith("]")) {
                    try {
                        parsedValue = objectMapper.readValue(fieldValue, List.class);
                        log.debug("成功解析数组字段: {} = {}", fieldData.getFieldKey(), parsedValue);
                    } catch (Exception e) {
                        log.debug("解析数组字段失败，使用原始值: {} = {}", fieldData.getFieldKey(), fieldValue);
                        parsedValue = fieldValue;
                    }
                } else {
                    parsedValue = fieldValue;
                }

                fieldDataMap.put(fieldData.getFieldKey(), parsedValue);
            }

            return ResponseDTO.ok(fieldDataMap);
        } catch (Exception e) {
            log.error("获取警情专业字段数据失败: reportId={}", reportId, e);
            return ResponseDTO.ok(new HashMap<>());
        }
    }

    /**
     * 高级查询 - 支持专业字段条件查询
     * 适用于复杂的统计分析和业务查询
     */
    public ResponseDTO<PageResult<PoliceReportVO>> advancedQuery(PoliceReportQueryForm queryForm) {
        try {
            Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);

            // 构建动态查询条件
            StringBuilder sql = new StringBuilder();
            Map<String, Object> params = new HashMap<>();

            sql.append("SELECT DISTINCT pr.* FROM t_police_report pr ");

            // 根据查询条件决定是否需要JOIN专业字段表
            boolean needFieldJoin = hasFieldQueryConditions(queryForm);
            if (needFieldJoin) {
                sql.append("LEFT JOIN t_police_report_field_data fd ON pr.report_id = fd.report_id ");
            }

            sql.append("WHERE pr.deleted_flag = 0 ");

            // 基本查询条件
            addBasicQueryConditions(sql, params, queryForm);

            // 专业字段查询条件
            if (needFieldJoin) {
                addFieldQueryConditions(sql, params, queryForm);
            }

            sql.append("ORDER BY pr.report_time DESC, pr.create_time DESC");

            log.info("高级查询SQL: {}", sql.toString());
            log.info("查询参数: {}", params);

            // 执行查询 - TODO: 需要实现DAO中的advancedQuery方法
            // List<PoliceReportVO> policeReportList = policeReportDao.advancedQuery(sql.toString(), params, page);
            // PageResult<PoliceReportVO> pageResult = SmartPageUtil.convert2PageResult(page, policeReportList);
            PageResult<PoliceReportVO> pageResult = new PageResult<>();

            return ResponseDTO.ok(pageResult);

        } catch (Exception e) {
            log.error("高级查询失败", e);
            return ResponseDTO.userErrorParam("查询失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否有专业字段查询条件
     */
    private boolean hasFieldQueryConditions(PoliceReportQueryForm queryForm) {
        return queryForm.getTrappedCountMin() != null ||
               queryForm.getTrappedCountMax() != null ||
               queryForm.getCasualtiesCountMin() != null ||
               queryForm.getCasualtiesCountMax() != null ||
               queryForm.getVehicleCountMin() != null ||
               queryForm.getVehicleCountMax() != null ||
               queryForm.getDangerLevel() != null ||
               queryForm.getRescueType() != null ||
               queryForm.getAccidentType() != null ||
               queryForm.getFireFloor() != null ||
               queryForm.getFireScale() != null;
    }

    /**
     * 添加基本查询条件
     */
    private void addBasicQueryConditions(StringBuilder sql, Map<String, Object> params, PoliceReportQueryForm queryForm) {
        if (queryForm.getReportNumber() != null) {
            sql.append("AND pr.report_number LIKE :reportNumber ");
            params.put("reportNumber", "%" + queryForm.getReportNumber() + "%");
        }

        if (queryForm.getReportType() != null) {
            sql.append("AND pr.report_type = :reportType ");
            params.put("reportType", queryForm.getReportType());
        }

        if (queryForm.getReportLevel() != null) {
            sql.append("AND pr.report_level = :reportLevel ");
            params.put("reportLevel", queryForm.getReportLevel());
        }

        if (queryForm.getStatus() != null) {
            sql.append("AND pr.status = :status ");
            params.put("status", queryForm.getStatus());
        }

        if (queryForm.getReportTimeStart() != null) {
            sql.append("AND pr.report_time >= :reportTimeStart ");
            params.put("reportTimeStart", queryForm.getReportTimeStart());
        }

        if (queryForm.getReportTimeEnd() != null) {
            sql.append("AND pr.report_time <= :reportTimeEnd ");
            params.put("reportTimeEnd", queryForm.getReportTimeEnd());
        }
    }

    /**
     * 添加专业字段查询条件
     */
    private void addFieldQueryConditions(StringBuilder sql, Map<String, Object> params, PoliceReportQueryForm queryForm) {
        List<String> fieldConditions = new ArrayList<>();

        // 被困人数查询
        if (queryForm.getTrappedCountMin() != null || queryForm.getTrappedCountMax() != null) {
            StringBuilder trappedCondition = new StringBuilder();
            trappedCondition.append("(fd.field_key = 'trappedCount' AND ");

            if (queryForm.getTrappedCountMin() != null && queryForm.getTrappedCountMax() != null) {
                trappedCondition.append("CAST(fd.field_value AS UNSIGNED) BETWEEN :trappedCountMin AND :trappedCountMax");
                params.put("trappedCountMin", queryForm.getTrappedCountMin());
                params.put("trappedCountMax", queryForm.getTrappedCountMax());
            } else if (queryForm.getTrappedCountMin() != null) {
                trappedCondition.append("CAST(fd.field_value AS UNSIGNED) >= :trappedCountMin");
                params.put("trappedCountMin", queryForm.getTrappedCountMin());
            } else {
                trappedCondition.append("CAST(fd.field_value AS UNSIGNED) <= :trappedCountMax");
                params.put("trappedCountMax", queryForm.getTrappedCountMax());
            }

            trappedCondition.append(")");
            fieldConditions.add(trappedCondition.toString());
        }

        // 伤亡人数查询
        if (queryForm.getCasualtiesCountMin() != null || queryForm.getCasualtiesCountMax() != null) {
            StringBuilder casualtiesCondition = new StringBuilder();
            casualtiesCondition.append("(fd.field_key = 'casualties' AND ");

            if (queryForm.getCasualtiesCountMin() != null && queryForm.getCasualtiesCountMax() != null) {
                casualtiesCondition.append("CAST(fd.field_value AS UNSIGNED) BETWEEN :casualtiesCountMin AND :casualtiesCountMax");
                params.put("casualtiesCountMin", queryForm.getCasualtiesCountMin());
                params.put("casualtiesCountMax", queryForm.getCasualtiesCountMax());
            } else if (queryForm.getCasualtiesCountMin() != null) {
                casualtiesCondition.append("CAST(fd.field_value AS UNSIGNED) >= :casualtiesCountMin");
                params.put("casualtiesCountMin", queryForm.getCasualtiesCountMin());
            } else {
                casualtiesCondition.append("CAST(fd.field_value AS UNSIGNED) <= :casualtiesCountMax");
                params.put("casualtiesCountMax", queryForm.getCasualtiesCountMax());
            }

            casualtiesCondition.append(")");
            fieldConditions.add(casualtiesCondition.toString());
        }

        // 危险等级查询
        if (queryForm.getDangerLevel() != null) {
            fieldConditions.add("(fd.field_key = 'dangerLevel' AND fd.field_value = :dangerLevel)");
            params.put("dangerLevel", queryForm.getDangerLevel().toString());
        }

        // 救援类型查询
        if (queryForm.getRescueType() != null) {
            fieldConditions.add("(fd.field_key = 'rescueType' AND fd.field_value LIKE :rescueType)");
            params.put("rescueType", "%" + queryForm.getRescueType() + "%");
        }

        // 添加字段条件到SQL
        if (!fieldConditions.isEmpty()) {
            sql.append("AND pr.report_id IN (");
            sql.append("SELECT DISTINCT fd2.report_id FROM t_police_report_field_data fd2 WHERE ");
            sql.append(String.join(" OR ", fieldConditions));
            sql.append(") ");
        }
    }

    /**
     * 专业字段统计查询
     * 例如：按警情类型统计被困人数、伤亡情况等
     */
    public ResponseDTO<List<Map<String, Object>>> getFieldStatistics(String fieldKey, Integer reportType) {
        try {
            // TODO: 需要实现DAO中的getFieldStatistics方法
            // List<Map<String, Object>> statistics = policeReportDao.getFieldStatistics(fieldKey, reportType);
            // return ResponseDTO.ok(statistics);
            return ResponseDTO.ok(new ArrayList<>());
        } catch (Exception e) {
            log.error("获取专业字段统计数据失败: fieldKey={}, reportType={}", fieldKey, reportType, e);
            return ResponseDTO.ok(new ArrayList<>());
        }
    }

    /**
     * 构建变更数据映射
     */
    /**
     * 构建详细的变更日志（包含原值和新值）
     */
    private String buildDetailedChangeLog(PoliceReportEntity oldEntity, PoliceReportUpdateForm updateForm) {
        StringBuilder changeLog = new StringBuilder();
        String currentUser = SmartRequestUtil.getRequestUser() != null ?
            SmartRequestUtil.getRequestUser().getUserName() : "系统";
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        changeLog.append(String.format("[%s] %s 修改了警情信息：\n", currentTime, currentUser));

        // 检查警情类型变更
        if (updateForm.getReportType() != null && !Objects.equals(oldEntity.getReportType(), updateForm.getReportType())) {
            changeLog.append(String.format("  • 警情类型：%s → %s\n",
                getEnumDescription("POLICE_REPORT_TYPE_ENUM", oldEntity.getReportType()),
                getEnumDescription("POLICE_REPORT_TYPE_ENUM", updateForm.getReportType())));
        }

        // 检查警情等级变更
        if (updateForm.getReportLevel() != null && !Objects.equals(oldEntity.getReportLevel(), updateForm.getReportLevel())) {
            changeLog.append(String.format("  • 警情等级：%s → %s\n",
                getEnumDescription("POLICE_REPORT_LEVEL_ENUM", oldEntity.getReportLevel()),
                getEnumDescription("POLICE_REPORT_LEVEL_ENUM", updateForm.getReportLevel())));
        }

        // 检查处理状态变更
        if (updateForm.getStatus() != null && !Objects.equals(oldEntity.getStatus(), updateForm.getStatus())) {
            changeLog.append(String.format("  • 处理状态：%s → %s\n",
                getEnumDescription("POLICE_REPORT_STATUS_ENUM", oldEntity.getStatus()),
                getEnumDescription("POLICE_REPORT_STATUS_ENUM", updateForm.getStatus())));
        }

        // 检查报警人姓名变更
        if (updateForm.getReporterName() != null && !Objects.equals(oldEntity.getReporterName(), updateForm.getReporterName())) {
            changeLog.append(String.format("  • 报警人姓名：%s → %s\n",
                nullToEmpty(oldEntity.getReporterName()), updateForm.getReporterName()));
        }

        // 检查报警人电话变更
        if (updateForm.getReporterPhone() != null && !Objects.equals(oldEntity.getReporterPhone(), updateForm.getReporterPhone())) {
            changeLog.append(String.format("  • 报警人电话：%s → %s\n",
                nullToEmpty(oldEntity.getReporterPhone()), updateForm.getReporterPhone()));
        }

        // 检查报警人身份证变更
        if (updateForm.getReporterIdCard() != null && !Objects.equals(oldEntity.getReporterIdCard(), updateForm.getReporterIdCard())) {
            changeLog.append(String.format("  • 报警人身份证：%s → %s\n",
                nullToEmpty(oldEntity.getReporterIdCard()), updateForm.getReporterIdCard()));
        }

        // 检查事发地点变更
        if (updateForm.getIncidentLocation() != null && !Objects.equals(oldEntity.getIncidentLocation(), updateForm.getIncidentLocation())) {
            changeLog.append(String.format("  • 事发地点：%s → %s\n",
                nullToEmpty(oldEntity.getIncidentLocation()), updateForm.getIncidentLocation()));
        }

        // 检查警情描述变更
        if (updateForm.getDescription() != null && !Objects.equals(oldEntity.getDescription(), updateForm.getDescription())) {
            changeLog.append(String.format("  • 警情描述：%s → %s\n",
                truncateText(nullToEmpty(oldEntity.getDescription()), 50),
                truncateText(updateForm.getDescription(), 50)));
        }

        // 检查处理结果变更
        if (updateForm.getHandleResult() != null && !Objects.equals(oldEntity.getHandleResult(), updateForm.getHandleResult())) {
            changeLog.append(String.format("  • 处理结果：%s → %s\n",
                truncateText(nullToEmpty(oldEntity.getHandleResult()), 50),
                truncateText(updateForm.getHandleResult(), 50)));
        }

        // 检查备注变更
        if (updateForm.getRemark() != null && !Objects.equals(oldEntity.getRemark(), updateForm.getRemark())) {
            changeLog.append(String.format("  • 备注：%s → %s\n",
                truncateText(nullToEmpty(oldEntity.getRemark()), 50),
                truncateText(updateForm.getRemark(), 50)));
        }

        // 检查专业字段变更
        if (updateForm.getProfessionalFields() != null) {
            changeLog.append("  • 专业字段：已更新\n");
        }

        return changeLog.toString();
    }

    /**
     * 获取枚举描述
     */
    private String getEnumDescription(String enumType, Integer value) {
        if (value == null) return "未设置";

        // 这里可以根据实际的枚举映射来返回描述
        switch (enumType) {
            case "POLICE_REPORT_TYPE_ENUM":
                return getReportTypeDescription(value);
            case "POLICE_REPORT_LEVEL_ENUM":
                return getReportLevelDescription(value);
            case "POLICE_REPORT_STATUS_ENUM":
                return getReportStatusDescription(value);
            default:
                return String.valueOf(value);
        }
    }

    private String getReportTypeDescription(Integer type) {
        if (type == null) return "未设置";
        switch (type) {
            case 1: return "火灾";
            case 2: return "救援";
            case 3: return "医疗";
            case 4: return "交通";
            case 5: return "治安";
            default: return "其他(" + type + ")";
        }
    }

    private String getReportLevelDescription(Integer level) {
        if (level == null) return "未设置";
        switch (level) {
            case 1: return "紧急";
            case 2: return "高";
            case 3: return "中";
            case 4: return "低";
            default: return "未知(" + level + ")";
        }
    }

    private String getReportStatusDescription(Integer status) {
        if (status == null) return "未设置";
        switch (status) {
            case 1: return "待处理";
            case 2: return "处理中";
            case 3: return "已完成";
            case 4: return "已关闭";
            default: return "未知(" + status + ")";
        }
    }

    private String nullToEmpty(String str) {
        return str == null ? "未填写" : str;
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private Map<String, Object> buildChangeMap(PoliceReportEntity oldEntity, PoliceReportEntity newEntity) {
        Map<String, Object> changes = new HashMap<>();

        if (newEntity.getReportType() != null && !Objects.equals(oldEntity.getReportType(), newEntity.getReportType())) {
            changes.put("reportType", newEntity.getReportType());
        }
        if (newEntity.getReportLevel() != null && !Objects.equals(oldEntity.getReportLevel(), newEntity.getReportLevel())) {
            changes.put("reportLevel", newEntity.getReportLevel());
        }
        if (newEntity.getStatus() != null && !Objects.equals(oldEntity.getStatus(), newEntity.getStatus())) {
            changes.put("status", newEntity.getStatus());
        }
        if (newEntity.getReporterName() != null && !Objects.equals(oldEntity.getReporterName(), newEntity.getReporterName())) {
            changes.put("reporterName", newEntity.getReporterName());
        }
        if (newEntity.getReporterPhone() != null && !Objects.equals(oldEntity.getReporterPhone(), newEntity.getReporterPhone())) {
            changes.put("reporterPhone", newEntity.getReporterPhone());
        }
        if (newEntity.getIncidentLocation() != null && !Objects.equals(oldEntity.getIncidentLocation(), newEntity.getIncidentLocation())) {
            changes.put("incidentLocation", newEntity.getIncidentLocation());
        }
        if (newEntity.getDescription() != null && !Objects.equals(oldEntity.getDescription(), newEntity.getDescription())) {
            changes.put("description", newEntity.getDescription());
        }
        if (newEntity.getHandlerName() != null && !Objects.equals(oldEntity.getHandlerName(), newEntity.getHandlerName())) {
            changes.put("handlerName", newEntity.getHandlerName());
        }
        if (newEntity.getHandlerId() != null && !Objects.equals(oldEntity.getHandlerId(), newEntity.getHandlerId())) {
            changes.put("handlerId", newEntity.getHandlerId());
        }

        return changes;
    }

    // ========== 轻量级数据版本检查 ==========

    /**
     * 检查数据版本 - 轻量级同步检查
     */
    public ResponseDTO<DataVersionVO> checkDataVersion(String clientVersion) {
        try {
            // 1. 获取最后更新时间（只查询时间字段，不查询完整数据）
            Long lastUpdateTime = policeReportDao.getMaxUpdateTime();

            // 2. 生成服务器端版本号（基于最后更新时间）
            String serverVersion = generateVersionHash(lastUpdateTime);

            // 3. 比较版本
            boolean hasUpdates = !serverVersion.equals(clientVersion);

            // 4. 构建轻量级响应
            DataVersionVO versionVO = new DataVersionVO(serverVersion, lastUpdateTime);
            versionVO.setHasUpdates(hasUpdates);

            log.debug("🔍 [数据版本检查] 客户端版本: {}, 服务器版本: {}, 有更新: {}",
                      clientVersion, serverVersion, hasUpdates);

            return ResponseDTO.ok(versionVO);

        } catch (Exception e) {
            log.error("❌ [数据版本检查] 检查失败", e);
            // 降级：返回有更新，让客户端刷新
            DataVersionVO fallbackVO = new DataVersionVO("error", System.currentTimeMillis());
            fallbackVO.setHasUpdates(true);
            return ResponseDTO.ok(fallbackVO);
        }
    }

    /**
     * 检查数据版本 - 带分页信息
     */
    public ResponseDTO<DataVersionVO> checkDataVersionWithPage(String clientVersion, Integer pageNum, Integer pageSize) {
        try {
            // 1. 获取最后更新时间和总数（轻量级查询）
            Long lastUpdateTime = policeReportDao.getMaxUpdateTime();
            Long totalCount = policeReportDao.getTotalCount();

            // 2. 生成版本号（包含分页信息）
            String serverVersion = generateVersionHashWithPage(lastUpdateTime, totalCount, pageNum, pageSize);

            // 3. 比较版本
            boolean hasUpdates = !serverVersion.equals(clientVersion);

            // 4. 构建响应
            DataVersionVO versionVO = new DataVersionVO(serverVersion, lastUpdateTime, totalCount);
            versionVO.setHasUpdates(hasUpdates);

            log.debug("🔍 [数据版本检查-分页] 客户端版本: {}, 服务器版本: {}, 总数: {}, 有更新: {}",
                      clientVersion, serverVersion, totalCount, hasUpdates);

            return ResponseDTO.ok(versionVO);

        } catch (Exception e) {
            log.error("❌ [数据版本检查-分页] 检查失败", e);
            // 降级：返回有更新
            DataVersionVO fallbackVO = new DataVersionVO("error", System.currentTimeMillis());
            fallbackVO.setHasUpdates(true);
            return ResponseDTO.ok(fallbackVO);
        }
    }

    /**
     * 生成版本hash - 基于最后更新时间
     */
    private String generateVersionHash(Long lastUpdateTime) {
        if (lastUpdateTime == null) {
            return "empty";
        }
        // 简单的版本号生成策略：时间戳的hash
        return String.valueOf(lastUpdateTime.hashCode());
    }

    /**
     * 生成版本hash - 包含分页信息
     */
    private String generateVersionHashWithPage(Long lastUpdateTime, Long totalCount, Integer pageNum, Integer pageSize) {
        if (lastUpdateTime == null) {
            return "empty";
        }
        // 包含分页信息的版本号
        String combined = lastUpdateTime + "_" + totalCount + "_" + pageNum + "_" + pageSize;
        return String.valueOf(combined.hashCode());
    }
}