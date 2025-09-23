package net.lab1024.sa.admin.module.business.oa.police.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.base.common.enumeration.BaseEnum;

/**
 * 警情类型枚举
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-18
 */
@Getter
@AllArgsConstructor
public enum PoliceReportTypeEnum implements BaseEnum {

    CRIMINAL(1, "刑事案件"),
    TRAFFIC(2, "交通事故"),
    PUBLIC_SECURITY(3, "治安案件"),
    FIRE(4, "火灾事故"),
    MEDICAL(5, "医疗急救"),
    CIVIL_DISPUTE(6, "民事纠纷"),
    OTHER(99, "其他");

    private final Integer value;
    private final String desc;
}