package net.lab1024.sa.admin.module.support.websocket.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket会话信息
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@Data
@Accessors(chain = true)
public class WebSocketSession {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 角色列表
     */
    private Set<String> roles = ConcurrentHashMap.newKeySet();

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 连接时间
     */
    private LocalDateTime connectTime;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;

    /**
     * 订阅的房间/频道
     */
    private Set<String> subscribedRooms = ConcurrentHashMap.newKeySet();

    /**
     * 订阅的业务模块
     */
    private Set<String> subscribedModules = ConcurrentHashMap.newKeySet();

    /**
     * 会话状态：CONNECTING, CONNECTED, DISCONNECTED
     */
    private String status = "CONNECTING";

    /**
     * 额外属性
     */
    private ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * 更新最后活跃时间
     */
    public void updateLastActiveTime() {
        this.lastActiveTime = LocalDateTime.now();
    }

    /**
     * 添加订阅房间
     */
    public void subscribeRoom(String room) {
        this.subscribedRooms.add(room);
    }

    /**
     * 取消订阅房间
     */
    public void unsubscribeRoom(String room) {
        this.subscribedRooms.remove(room);
    }

    /**
     * 添加订阅模块
     */
    public void subscribeModule(String module) {
        this.subscribedModules.add(module);
    }

    /**
     * 取消订阅模块
     */
    public void unsubscribeModule(String module) {
        this.subscribedModules.remove(module);
    }

    /**
     * 检查是否订阅了房间
     */
    public boolean isSubscribedToRoom(String room) {
        return subscribedRooms.contains(room) || subscribedRooms.contains("all");
    }

    /**
     * 检查是否订阅了模块
     */
    public boolean isSubscribedToModule(String module) {
        return subscribedModules.contains(module) || subscribedModules.contains("all");
    }

    /**
     * 设置会话属性
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 获取会话属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * 检查会话是否已连接
     */
    public boolean isConnected() {
        return "CONNECTED".equals(status);
    }

    /**
     * 检查会话是否过期（超过5分钟无活动）
     */
    public boolean isExpired() {
        return lastActiveTime != null &&
               LocalDateTime.now().isAfter(lastActiveTime.plusMinutes(5));
    }

    /**
     * 获取主要角色（返回第一个角色）
     */
    public String getRole() {
        return roles.isEmpty() ? null : roles.iterator().next();
    }
}