package net.lab1024.sa.admin.module.support.websocket.service;

import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketMessage;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketSession;

import java.util.List;
import java.util.function.Predicate;

/**
 * 消息传输接口
 * 支持WebSocket和MQ两种实现方式
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
public interface MessageTransport {

    /**
     * 发送消息给指定用户
     *
     * @param userId  用户ID
     * @param message 消息
     * @return 是否发送成功
     */
    boolean sendToUser(Long userId, WebSocketMessage message);

    /**
     * 发送消息给指定会话
     *
     * @param sessionId 会话ID
     * @param message   消息
     * @return 是否发送成功
     */
    boolean sendToSession(String sessionId, WebSocketMessage message);

    /**
     * 广播消息给所有用户
     *
     * @param message 消息
     * @return 发送成功的用户数量
     */
    int broadcast(WebSocketMessage message);

    /**
     * 发送消息给房间内的用户
     *
     * @param room    房间名称
     * @param message 消息
     * @return 发送成功的用户数量
     */
    int sendToRoom(String room, WebSocketMessage message);

    /**
     * 发送消息给订阅了指定模块的用户
     *
     * @param module  模块名称
     * @param message 消息
     * @return 发送成功的用户数量
     */
    int sendToModule(String module, WebSocketMessage message);

    /**
     * 根据条件过滤发送消息
     *
     * @param message   消息
     * @param predicate 过滤条件
     * @return 发送成功的用户数量
     */
    int sendToMatched(WebSocketMessage message, Predicate<WebSocketSession> predicate);

    /**
     * 发送消息给部门用户
     *
     * @param departmentId 部门ID
     * @param message      消息
     * @return 发送成功的用户数量
     */
    int sendToDepartment(Long departmentId, WebSocketMessage message);

    /**
     * 发送消息给角色用户
     *
     * @param role    角色
     * @param message 消息
     * @return 发送成功的用户数量
     */
    int sendToRole(String role, WebSocketMessage message);

    /**
     * 获取在线用户数量
     *
     * @return 在线用户数量
     */
    int getOnlineUserCount();

    /**
     * 获取在线用户列表
     *
     * @return 在线用户ID列表
     */
    List<Long> getOnlineUsers();

    /**
     * 检查用户是否在线
     *
     * @param userId 用户ID
     * @return 是否在线
     */
    boolean isUserOnline(Long userId);

    /**
     * 强制断开用户连接
     *
     * @param userId 用户ID
     * @param reason 断开原因
     * @return 是否成功断开
     */
    boolean disconnectUser(Long userId, String reason);

    /**
     * 获取传输类型
     *
     * @return 传输类型：websocket、mq等
     */
    String getTransportType();
}