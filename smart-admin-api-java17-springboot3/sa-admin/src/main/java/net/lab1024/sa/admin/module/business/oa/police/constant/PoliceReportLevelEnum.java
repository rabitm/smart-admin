package net.lab1024.sa.admin.module.business.oa.police.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.base.common.enumeration.BaseEnum;

/**
 * 警情等级枚举
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-18
 */
@Getter
@AllArgsConstructor
public enum PoliceReportLevelEnum implements BaseEnum {

    URGENT(1, "紧急"),
    HIGH(2, "高"),
    MEDIUM(3, "中"),
    LOW(4, "低");

    private final Integer value;
    private final String desc;
}