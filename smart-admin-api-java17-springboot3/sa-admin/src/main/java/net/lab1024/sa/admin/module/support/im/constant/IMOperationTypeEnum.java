package net.lab1024.sa.admin.module.support.im.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * IM操作类型枚举
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Getter
@AllArgsConstructor
public enum IMOperationTypeEnum {

    // ========== 用户操作 ==========
    USER_SYNC("USER_SYNC", "用户同步"),
    USER_BATCH_SYNC("USER_BATCH_SYNC", "用户批量同步"),
    USER_REGISTER("USER_REGISTER", "用户注册"),
    USER_UPDATE("USER_UPDATE", "用户更新"),

    // ========== 群组操作 ==========
    GROUP_CREATE("GROUP_CREATE", "创建群组"),
    GROUP_UPDATE("GROUP_UPDATE", "更新群组"),
    GROUP_DISBAND("GROUP_DISBAND", "解散群组"),
    GROUP_TRANSFER("GROUP_TRANSFER", "转让群组"),
    GROUP_GET_INFO("GROUP_GET_INFO", "获取群组信息"),

    // ========== 群成员操作 ==========
    GROUP_INVITE("GROUP_INVITE", "邀请成员"),
    GROUP_KICK("GROUP_KICK", "移除成员"),
    GROUP_QUIT("GROUP_QUIT", "退出群组"),
    GROUP_JOIN("GROUP_JOIN", "加入群组"),
    GROUP_SET_ADMIN("GROUP_SET_ADMIN", "设置管理员"),
    GROUP_REMOVE_ADMIN("GROUP_REMOVE_ADMIN", "移除管理员"),

    // ========== 消息操作 ==========
    MSG_SEND("MSG_SEND", "发送消息"),
    MSG_SEND_GROUP("MSG_SEND_GROUP", "发送群消息"),
    MESSAGE_SEND("MESSAGE_SEND", "发送消息"),
    MESSAGE_QUERY("MESSAGE_QUERY", "查询消息"),
    MESSAGE_HISTORY_QUERY("MESSAGE_HISTORY_QUERY", "查询历史消息"),

    // ========== 令牌操作 ==========
    TOKEN_GET("TOKEN_GET", "获取Token"),
    TOKEN_REFRESH("TOKEN_REFRESH", "刷新Token"),
    TOKEN_GENERATE("TOKEN_GENERATE", "生成Token"),

    // ========== 配置操作 ==========
    CONFIG_UPDATE("CONFIG_UPDATE", "更新配置"),
    CONFIG_QUERY("CONFIG_QUERY", "查询配置"),

    // ========== 规则操作 ==========
    RULE_CREATE("RULE_CREATE", "创建规则"),
    RULE_UPDATE("RULE_UPDATE", "更新规则"),
    RULE_DELETE("RULE_DELETE", "删除规则"),
    RULE_EXECUTE("RULE_EXECUTE", "执行规则"),
    ;

    private final String code;
    private final String description;
}
