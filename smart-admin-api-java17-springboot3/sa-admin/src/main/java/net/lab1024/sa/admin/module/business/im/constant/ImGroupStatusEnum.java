package net.lab1024.sa.admin.module.business.im.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * IM群组状态枚举
 *
 * 定义群组生命周期的所有状态，用于状态机管理
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-09
 * @Copyright 1024创新实验室
 */
@Getter
@AllArgsConstructor
public enum ImGroupStatusEnum {

    /**
     * 创建中 - 群组正在OpenIM服务器创建
     */
    CREATING(0, "创建中"),

    /**
     * 同步中 - 等待OpenIM服务器数据同步完成
     */
    SYNCING(1, "同步中"),

    /**
     * 验证中 - 正在验证群组是否在OpenIM中存在
     */
    VALIDATING(2, "验证中"),

    /**
     * 正常 - 群组已创建并验证成功，可正常使用
     */
    ACTIVE(3, "正常"),

    /**
     * 异常 - 群组状态异常，需要修复
     */
    ERROR(4, "异常"),

    /**
     * 修复中 - 正在自动修复群组
     */
    REPAIRING(5, "修复中"),

    /**
     * 已删除 - 群组已删除
     */
    DELETED(9, "已删除");

    private final Integer value;
    private final String desc;

    /**
     * 根据值获取枚举
     */
    public static ImGroupStatusEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (ImGroupStatusEnum statusEnum : values()) {
            if (statusEnum.getValue().equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }

    /**
     * 是否为终态（不可再变更）
     */
    public boolean isFinalState() {
        return this == DELETED;
    }

    /**
     * 是否为正常可用状态
     */
    public boolean isUsable() {
        return this == ACTIVE;
    }

    /**
     * 是否为过渡状态（需要继续处理）
     */
    public boolean isTransitional() {
        return this == CREATING || this == SYNCING || this == VALIDATING || this == REPAIRING;
    }

    /**
     * 是否为异常状态
     */
    public boolean isError() {
        return this == ERROR;
    }
}
