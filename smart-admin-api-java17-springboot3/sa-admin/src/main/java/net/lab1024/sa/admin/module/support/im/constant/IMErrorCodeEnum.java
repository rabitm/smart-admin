package net.lab1024.sa.admin.module.support.im.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.base.common.code.ErrorCode;

/**
 * IM错误码枚举
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Getter
@AllArgsConstructor
public enum IMErrorCodeEnum implements ErrorCode {

    // ========== 通用错误 ==========
    IM_SERVICE_ERROR(40001, "IM服务异常"),
    IM_CONFIG_ERROR(40002, "IM配置错误"),
    IM_API_CALL_FAILED(40003, "IM API调用失败"),
    IM_TOKEN_EXPIRED(40004, "IM Token已过期"),
    IM_CIRCUIT_BREAKER_OPEN(40005, "IM服务熔断中,请稍后重试"),

    // ========== 用户相关错误 ==========
    USER_NOT_EXIST(40101, "用户不存在"),
    USER_ALREADY_SYNCED(40102, "用户已同步"),
    USER_SYNC_FAILED(40103, "用户同步失败"),
    USER_MAPPING_NOT_FOUND(40104, "用户映射关系不存在"),
    USER_REGISTER_FAILED(40105, "用户注册失败"),
    USER_NOT_LOGIN(40106, "用户未登录"),
    USER_NOT_SYNCED(40107, "用户未同步到OpenIM"),

    // ========== 群组相关错误 ==========
    GROUP_NOT_EXIST(40201, "群组不存在"),
    GROUP_ALREADY_EXIST(40202, "群组已存在"),
    GROUP_CREATE_FAILED(40203, "群组创建失败"),
    GROUP_MAPPING_NOT_FOUND(40204, "群组映射关系不存在"),
    GROUP_MEMBER_LIMIT_EXCEEDED(40205, "群组成员数量已达上限"),
    GROUP_DISBANDED(40206, "群组已解散"),

    // ========== 群成员相关错误 ==========
    MEMBER_NOT_IN_GROUP(40301, "成员不在群组中"),
    MEMBER_ALREADY_IN_GROUP(40302, "成员已在群组中"),
    MEMBER_INVITE_FAILED(40303, "成员邀请失败"),
    MEMBER_KICK_FAILED(40304, "成员移除失败"),
    CANNOT_KICK_OWNER(40305, "不能移除群主"),

    // ========== 规则相关错误 ==========
    RULE_NOT_FOUND(40401, "规则不存在"),
    RULE_CONFIG_INVALID(40402, "规则配置无效"),
    RULE_ALREADY_EXIST(40403, "规则已存在"),

    // ========== 消息相关错误 ==========
    MESSAGE_SEND_FAILED(40501, "消息发送失败"),
    MESSAGE_CONTENT_EMPTY(40502, "消息内容为空"),

    // ========== 参数校验错误 ==========
    PARAM_INVALID(40601, "参数无效"),
    REPORT_ID_REQUIRED(40602, "警情ID不能为空"),
    EMPLOYEE_ID_REQUIRED(40603, "员工ID不能为空"),
    GROUP_ID_REQUIRED(40604, "群组ID不能为空"),
    USER_ID_REQUIRED(40605, "用户ID不能为空"),
    ;

    private final int code;
    private final String message;

    @Override
    public String getMsg() {
        return this.message;
    }

    @Override
    public String getLevel() {
        return ErrorCode.LEVEL_USER;
    }
}
