package net.lab1024.sa.admin.module.support.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IM操作日志实体
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@TableName("t_im_operation_log")
public class IMOperationLogEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作详情
     */
    private String operationDetail;

    /**
     * 目标类型
     */
    private String targetType;

    /**
     * 目标ID
     */
    private String targetId;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 是否成功
     */
    private Boolean successFlag;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 请求数据
     */
    private String requestData;

    /**
     * 响应数据
     */
    private String responseData;

    /**
     * 执行时间(ms)
     */
    private Integer executionTime;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * User Agent
     */
    private String userAgent;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
