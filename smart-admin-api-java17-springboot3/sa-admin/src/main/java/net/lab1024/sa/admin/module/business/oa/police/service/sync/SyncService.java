package net.lab1024.sa.admin.module.business.oa.police.service.sync;

/**
 * 实时同步服务接口
 * 抽象实时同步服务，支持多种消息队列实现（WebSocket、RabbitMQ等）
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-24
 * @Copyright 1024创新实验室
 */
public interface SyncService {

    /**
     * 同步警情字段更新
     *
     * @param reportId 警情ID
     * @param userId 用户ID
     * @param userName 用户名称
     * @param fieldName 字段名称
     * @param fieldValue 字段值
     * @param operationType 操作类型（UPDATE、DELETE等）
     */
    void syncFieldUpdate(Long reportId, Long userId, String userName,
                        String fieldName, String fieldValue, String operationType);

    /**
     * 同步警情字段更新（包含旧值）
     *
     * @param reportId 警情ID
     * @param userId 用户ID
     * @param userName 用户名称
     * @param fieldName 字段名称
     * @param oldValue 旧值
     * @param fieldValue 新值
     * @param operationType 操作类型（UPDATE、DELETE等）
     */
    void syncFieldUpdate(Long reportId, Long userId, String userName,
                        String fieldName, String oldValue, String fieldValue, String operationType);

    /**
     * 同步字段编辑状态
     *
     * @param reportId 警情ID
     * @param userId 用户ID
     * @param userName 用户名称
     * @param fieldName 字段名称
     * @param action 编辑动作（FOCUS、BLUR、LOCK等）
     */
    void syncFieldEditState(Long reportId, Long userId, String userName,
                           String fieldName, String action);

    /**
     * 记录操作历史
     *
     * @param reportId 警情ID
     * @param userId 用户ID
     * @param userName 用户名称
     * @param fieldName 字段名称
     * @param oldValue 旧值
     * @param newValue 新值
     * @param operationType 操作类型
     */
    void recordOperation(Long reportId, Long userId, String userName,
                        String fieldName, String oldValue, String newValue, String operationType);

    /**
     * 获取警情操作历史
     *
     * @param reportId 警情ID
     * @return 操作历史列表
     */
    Object getOperationHistory(Long reportId);

    /**
     * 检查服务是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();
}