package net.lab1024.sa.admin.module.business.oa.police.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.base.common.enumeration.BaseEnum;

/**
 * 警情处理状态枚举
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-18
 */
@Getter
@AllArgsConstructor
public enum PoliceReportStatusEnum implements BaseEnum {

    PENDING(1, "待处理"),
    PROCESSING(2, "处理中"),
    COMPLETED(3, "已完成"),
    CLOSED(4, "已关闭");

    private final Integer value;
    private final String desc;
}