package net.lab1024.sa.admin.module.support.im.constant;

/**
 * IM常量
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public interface IMConstant {

    // ========== OpenIM API端点 ==========
    // 用户Token获取 (OpenIM v3.x 官方文档: https://docs.openim.io/restapi/apis/authenticationmanagement/getusertoken)
    String API_USER_TOKEN = "/auth/get_user_token";
    // 管理员Token获取
    String API_ADMIN_TOKEN = "/auth/get_admin_token";
    String API_USER_REGISTER = "/user/user_register";
    String API_USER_UPDATE = "/user/update_user_info";
    String API_USER_GET_INFO = "/user/get_users_info";

    String API_GROUP_CREATE = "/group/create_group";
    String API_GROUP_UPDATE = "/group/set_group_info";
    String API_GROUP_DISBAND = "/group/dismiss_group";
    String API_GROUP_TRANSFER = "/group/transfer_group";
    String API_GROUP_GET_INFO = "/group/get_groups_info";

    String API_GROUP_INVITE = "/group/invite_user_to_group";
    String API_GROUP_KICK = "/group/kick_group_member";
    String API_GROUP_QUIT = "/group/quit_group";
    String API_GROUP_GET_MEMBERS = "/group/get_group_member_list";

    String API_MESSAGE_SEND = "/msg/send_msg";
    String API_MESSAGE_HISTORY = "/msg/get_history_message_list";

    // ========== 配置键 ==========
    String CONFIG_KEY_API_URL = "openim.server.api.url";
    String CONFIG_KEY_WS_URL = "openim.server.ws.url";
    String CONFIG_KEY_ADMIN_USER_ID = "openim.admin.user.id";
    String CONFIG_KEY_ADMIN_SECRET = "openim.admin.secret";
    String CONFIG_KEY_PLATFORM_ID = "openim.platform.id";
    String CONFIG_KEY_TOKEN_EXPIRE = "openim.token.expire.seconds";
    String CONFIG_KEY_API_TIMEOUT = "openim.api.timeout.seconds";
    String CONFIG_KEY_RETRY_MAX = "openim.api.retry.max.count";
    String CONFIG_KEY_AUTO_CREATE_GROUP = "openim.group.auto.create";
    String CONFIG_KEY_AUTO_INVITE = "openim.group.auto.invite";
    String CONFIG_KEY_GROUP_MAX_MEMBERS = "openim.group.max.members";
    String CONFIG_KEY_GROUP_NAME_TEMPLATE = "openim.group.name.template";
    String CONFIG_KEY_USER_SYNC_BATCH_SIZE = "openim.user.sync.batch.size";
    String CONFIG_KEY_GROUP_INVITE_BATCH_SIZE = "openim.group.invite.batch.size";
    String CONFIG_KEY_CIRCUIT_BREAKER_THRESHOLD = "openim.circuit.breaker.threshold";
    String CONFIG_KEY_CIRCUIT_BREAKER_TIMEOUT = "openim.circuit.breaker.timeout.seconds";

    // ========== 默认值 ==========
    String DEFAULT_API_URL = "http://localhost:10002";
    String DEFAULT_WS_URL = "ws://localhost:10001";
    String DEFAULT_ADMIN_USER_ID = "imAdmin";
    Integer DEFAULT_PLATFORM_ID = 5; // 5-Web (普通用户使用Web平台ID)
    Integer DEFAULT_TOKEN_EXPIRE = 7200; // 2小时
    Integer DEFAULT_API_TIMEOUT = 10; // 10秒
    Integer DEFAULT_RETRY_MAX = 3;
    Integer DEFAULT_GROUP_MAX_MEMBERS = 500;
    String DEFAULT_GROUP_NAME_TEMPLATE = "警情-{reportNumber}";
    Integer DEFAULT_USER_SYNC_BATCH_SIZE = 50;
    Integer DEFAULT_GROUP_INVITE_BATCH_SIZE = 30;
    Integer DEFAULT_CIRCUIT_BREAKER_THRESHOLD = 5;
    Integer DEFAULT_CIRCUIT_BREAKER_TIMEOUT = 30;

    // ========== OpenIM群组类型 ==========
    Integer GROUP_TYPE_NORMAL = 0; // 普通群
    Integer GROUP_TYPE_SUPER = 1; // 超级群
    Integer GROUP_TYPE_WORK = 2; // 工作群

    // ========== OpenIM会话类型 ==========
    Integer SESSION_TYPE_SINGLE = 1; // 单聊
    Integer SESSION_TYPE_GROUP = 2; // 群聊
    Integer SESSION_TYPE_NOTIFICATION = 4; // 通知

    // ========== OpenIM消息类型 ==========
    Integer CONTENT_TYPE_TEXT = 101; // 文本消息
    Integer CONTENT_TYPE_IMAGE = 102; // 图片消息
    Integer CONTENT_TYPE_VOICE = 103; // 语音消息
    Integer CONTENT_TYPE_VIDEO = 104; // 视频消息
    Integer CONTENT_TYPE_FILE = 105; // 文件消息
    Integer CONTENT_TYPE_AT_TEXT = 106; // @消息
    Integer CONTENT_TYPE_CARD = 107; // 名片消息

    // ========== 缓存键前缀 ==========
    String CACHE_PREFIX_TOKEN = "im:token:";
    String CACHE_PREFIX_USER_MAPPING = "im:user:mapping:";
    String CACHE_PREFIX_GROUP_MAPPING = "im:group:mapping:";

    // ========== 用户ID前缀 ==========
    String USER_ID_PREFIX = "emp_";
    String GROUP_ID_PREFIX = "group_report_";

    // ========== 同步状态 ==========
    Integer SYNC_STATUS_SUCCESS = 1; // 已同步
    Integer SYNC_STATUS_FAILED = 2; // 同步失败
    Integer SYNC_STATUS_PENDING = 3; // 待同步

    // ========== 群组状态 ==========
    Integer GROUP_STATUS_NORMAL = 1; // 正常
    Integer GROUP_STATUS_DISBANDED = 2; // 已解散
    Integer GROUP_STATUS_ARCHIVED = 3; // 已归档

    // ========== 加入类型 ==========
    Integer JOIN_TYPE_AUTO = 1; // 自动拉入
    Integer JOIN_TYPE_MANUAL = 2; // 手动邀请
    Integer JOIN_TYPE_APPLY = 3; // 主动申请

    // ========== 规则类型 ==========
    Integer RULE_TYPE_REPORT_TYPE = 1; // 按警情类型
    Integer RULE_TYPE_DEPARTMENT = 2; // 按部门
    Integer RULE_TYPE_ROLE = 3; // 按角色
    Integer RULE_TYPE_REPORT_LEVEL = 4; // 按警情等级
    Integer RULE_TYPE_FIXED = 5; // 固定人员
}
