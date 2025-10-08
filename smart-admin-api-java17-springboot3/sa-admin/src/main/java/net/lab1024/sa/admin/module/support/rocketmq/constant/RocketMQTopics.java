package net.lab1024.sa.admin.module.support.rocketmq.constant;

/**
 * RocketMQ Topic常量
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-29
 * @Copyright 1024创新实验室
 */
public class RocketMQTopics {

    /**
     * 系统级消息
     */
    public static final String SYSTEM_BROADCAST = "SYSTEM_BROADCAST";
    public static final String SYSTEM_NOTIFICATION = "SYSTEM_NOTIFICATION";

    /**
     * 警情模块
     */
    public static final String POLICE_FIELD_SYNC = "POLICE_FIELD_SYNC";
    public static final String POLICE_LIST_UPDATE = "POLICE_LIST_UPDATE";
    public static final String POLICE_COLLABORATION = "POLICE_COLLABORATION";

    /**
     * 协同模块
     */
    public static final String COLLABORATION_FIELD_LOCK = "COLLABORATION_FIELD_LOCK";
    public static final String COLLABORATION_USER_STATUS = "COLLABORATION_USER_STATUS";

    /**
     * 座席模块
     */
    public static final String SEAT_STATUS_UPDATE = "SEAT_STATUS_UPDATE";

    /**
     * 业务模块到Topic映射
     */
    public static String getTopicByModule(String module) {
        return switch (module.toLowerCase()) {
            case "police" -> POLICE_FIELD_SYNC;
            case "collaboration" -> COLLABORATION_FIELD_LOCK;
            case "seat" -> SEAT_STATUS_UPDATE;
            case "system" -> SYSTEM_BROADCAST;
            default -> SYSTEM_BROADCAST;
        };
    }

    /**
     * 获取所有Topic列表
     */
    public static String[] getAllTopics() {
        return new String[] {
            SYSTEM_BROADCAST,
            SYSTEM_NOTIFICATION,
            POLICE_FIELD_SYNC,
            POLICE_LIST_UPDATE,
            POLICE_COLLABORATION,
            COLLABORATION_FIELD_LOCK,
            COLLABORATION_USER_STATUS,
            SEAT_STATUS_UPDATE
        };
    }
}