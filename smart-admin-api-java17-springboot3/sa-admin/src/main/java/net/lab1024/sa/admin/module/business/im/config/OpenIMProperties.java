package net.lab1024.sa.admin.module.business.im.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OpenIM 配置属性
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */
@Data
@Component
@ConfigurationProperties(prefix = "openim")
public class OpenIMProperties {

    /**
     * OpenIM API地址
     */
    private String apiUrl;

    /**
     * OpenIM WebSocket地址
     */
    private String wsUrl;

    /**
     * OpenIM管理端API地址
     */
    private String adminApiUrl;

    /**
     * 管理员配置
     */
    private Admin admin = new Admin();

    /**
     * 平台ID (5=Web)
     */
    private Integer platformId = 5;

    /**
     * 自动拉人配置
     */
    private AutoInvite autoInvite = new AutoInvite();

    /**
     * 群组配置
     */
    private Group group = new Group();

    /**
     * 功能开关
     */
    private Features features = new Features();

    /**
     * 安全配置
     */
    private Security security = new Security();

    /**
     * 管理员配置
     */
    @Data
    public static class Admin {
        /**
         * 管理员用户ID
         */
        private String userId;

        /**
         * 管理员密钥
         */
        private String secret;
    }

    /**
     * 自动拉人配置
     */
    @Data
    public static class AutoInvite {
        /**
         * 是否启用自动拉人
         */
        private Boolean enabled = true;

        /**
         * 自动拉人规则列表
         */
        private List<Rule> rules;

        @Data
        public static class Rule {
            /**
             * 规则类型: CREATOR, OWNER, DEPARTMENT, ROLE
             */
            private String type;

            /**
             * 是否启用
             */
            private Boolean enabled;

            /**
             * 优先级
             */
            private Integer priority;

            /**
             * 配置信息
             */
            private Config config;

            @Data
            public static class Config {
                /**
                 * 是否包含部门主管 (DEPARTMENT规则)
                 */
                private Boolean includeManager;

                /**
                 * 角色列表 (ROLE规则)
                 */
                private List<String> roles;
            }
        }
    }

    /**
     * 群组配置
     */
    @Data
    public static class Group {
        /**
         * 最大成员数
         */
        private Integer maxMembers = 500;

        /**
         * 默认群组类型 (2=工作群)
         */
        private Integer defaultType = 2;

        /**
         * 是否自动创建群组
         */
        private Boolean autoCreate = true;
    }

    /**
     * 功能开关
     */
    @Data
    public static class Features {
        /**
         * 文本消息
         */
        private Boolean textMessage = true;

        /**
         * 图片消息
         */
        private Boolean imageMessage = true;

        /**
         * 文件消息
         */
        private Boolean fileMessage = true;

        /**
         * 语音消息
         */
        private Boolean voiceMessage = false;

        /**
         * 视频消息
         */
        private Boolean videoMessage = false;

        /**
         * @提醒
         */
        private Boolean atMention = true;

        /**
         * 引用回复
         */
        private Boolean quoteReply = true;

        /**
         * 消息撤回
         */
        private Boolean messageRecall = true;

        /**
         * 消息搜索
         */
        private Boolean messageSearch = false;
    }

    /**
     * 安全配置
     */
    @Data
    public static class Security {
        /**
         * Token有效期 (小时)
         */
        private Integer tokenExpireHours = 24;

        /**
         * 最大文件大小 (MB)
         */
        private Integer maxFileSizeMb = 100;

        /**
         * 允许的文件类型
         */
        private List<String> allowedFileTypes;

        /**
         * 限流配置
         */
        private RateLimit rateLimit = new RateLimit();

        @Data
        public static class RateLimit {
            /**
             * 是否启用限流
             */
            private Boolean enabled = true;

            /**
             * 每分钟最大消息数
             */
            private Integer maxMessagesPerMinute = 60;
        }
    }
}
