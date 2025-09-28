package net.lab1024.sa.admin.module.business.oa.police.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据版本检查VO - 轻量级数据同步
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-28
 * @Copyright 1024创新实验室
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataVersionVO {

    /**
     * 数据版本号 - 基于最后更新时间的hash
     */
    private String version;

    /**
     * 最后更新时间戳
     */
    private Long lastUpdateTime;

    /**
     * 最后更新时间（可读格式）
     */
    private LocalDateTime lastUpdateDateTime;

    /**
     * 总记录数（可选）
     */
    private Long totalCount;

    /**
     * 是否有更新
     */
    private Boolean hasUpdates;

    /**
     * 服务器时间戳
     */
    private Long serverTime;

    public DataVersionVO(String version, Long lastUpdateTime) {
        this.version = version;
        this.lastUpdateTime = lastUpdateTime;
        this.lastUpdateDateTime = lastUpdateTime != null ?
            new java.sql.Timestamp(lastUpdateTime).toLocalDateTime() : null;
        this.serverTime = System.currentTimeMillis();
    }

    public DataVersionVO(String version, Long lastUpdateTime, Long totalCount) {
        this(version, lastUpdateTime);
        this.totalCount = totalCount;
    }
}