package net.lab1024.sa.admin.module.support.websocket.service;

import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketSession;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * WebSocket会话管理器
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service
public class WebSocketSessionManager {

    /**
     * 会话映射：sessionId -> WebSocketSession
     */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 用户会话映射：userId -> Set<sessionId>
     */
    private final Map<Long, String> userSessions = new ConcurrentHashMap<>();

    /**
     * 房间会话映射：room -> Set<sessionId>
     */
    private final Map<String, Map<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    /**
     * 模块会话映射：module -> Set<sessionId>
     */
    private final Map<String, Map<String, WebSocketSession>> moduleSessions = new ConcurrentHashMap<>();

    /**
     * 添加会话
     */
    public void addSession(WebSocketSession session) {
        log.info("添加WebSocket会话: sessionId={}, userId={}", session.getSessionId(), session.getUserId());

        sessions.put(session.getSessionId(), session);

        if (session.getUserId() != null) {
            userSessions.put(session.getUserId(), session.getSessionId());
        }

        session.setConnectTime(LocalDateTime.now());
        session.setLastActiveTime(LocalDateTime.now());
        session.setStatus("CONNECTED");
    }

    /**
     * 移除会话
     */
    public void removeSession(String sessionId) {
        WebSocketSession session = sessions.remove(sessionId);
        if (session != null) {
            log.info("移除WebSocket会话: sessionId={}, userId={}", sessionId, session.getUserId());

            // 从用户会话映射中移除
            if (session.getUserId() != null) {
                userSessions.remove(session.getUserId());
            }

            // 从房间会话映射中移除
            session.getSubscribedRooms().forEach(room -> {
                Map<String, WebSocketSession> roomSessionMap = roomSessions.get(room);
                if (roomSessionMap != null) {
                    roomSessionMap.remove(sessionId);
                    if (roomSessionMap.isEmpty()) {
                        roomSessions.remove(room);
                    }
                }
            });

            // 从模块会话映射中移除
            session.getSubscribedModules().forEach(module -> {
                Map<String, WebSocketSession> moduleSessionMap = moduleSessions.get(module);
                if (moduleSessionMap != null) {
                    moduleSessionMap.remove(sessionId);
                    if (moduleSessionMap.isEmpty()) {
                        moduleSessions.remove(module);
                    }
                }
            });
        }
    }

    /**
     * 获取会话
     */
    public WebSocketSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 根据用户ID获取会话
     */
    public WebSocketSession getSessionByUserId(Long userId) {
        String sessionId = userSessions.get(userId);
        return sessionId != null ? sessions.get(sessionId) : null;
    }

    /**
     * 更新会话活跃时间
     */
    public void updateSessionActivity(String sessionId) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null) {
            session.updateLastActiveTime();
        }
    }

    /**
     * 订阅房间
     */
    public void subscribeRoom(String sessionId, String room) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null) {
            session.subscribeRoom(room);
            roomSessions.computeIfAbsent(room, k -> new ConcurrentHashMap<>()).put(sessionId, session);
            log.debug("会话{}订阅房间: {}", sessionId, room);
        }
    }

    /**
     * 取消订阅房间
     */
    public void unsubscribeRoom(String sessionId, String room) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null) {
            session.unsubscribeRoom(room);
            Map<String, WebSocketSession> roomSessionMap = roomSessions.get(room);
            if (roomSessionMap != null) {
                roomSessionMap.remove(sessionId);
                if (roomSessionMap.isEmpty()) {
                    roomSessions.remove(room);
                }
            }
            log.debug("会话{}取消订阅房间: {}", sessionId, room);
        }
    }

    /**
     * 订阅模块
     */
    public void subscribeModule(String sessionId, String module) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null) {
            session.subscribeModule(module);
            moduleSessions.computeIfAbsent(module, k -> new ConcurrentHashMap<>()).put(sessionId, session);
            log.debug("会话{}订阅模块: {}", sessionId, module);
        }
    }

    /**
     * 取消订阅模块
     */
    public void unsubscribeModule(String sessionId, String module) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null) {
            session.unsubscribeModule(module);
            Map<String, WebSocketSession> moduleSessionMap = moduleSessions.get(module);
            if (moduleSessionMap != null) {
                moduleSessionMap.remove(sessionId);
                if (moduleSessionMap.isEmpty()) {
                    moduleSessions.remove(module);
                }
            }
            log.debug("会话{}取消订阅模块: {}", sessionId, module);
        }
    }

    /**
     * 获取房间内的会话
     */
    public List<WebSocketSession> getRoomSessions(String room) {
        Map<String, WebSocketSession> roomSessionMap = roomSessions.get(room);
        return roomSessionMap != null ?
            roomSessionMap.values().stream().filter(WebSocketSession::isConnected).collect(Collectors.toList()) :
            List.of();
    }

    /**
     * 获取模块的会话
     */
    public List<WebSocketSession> getModuleSessions(String module) {
        Map<String, WebSocketSession> moduleSessionMap = moduleSessions.get(module);
        return moduleSessionMap != null ?
            moduleSessionMap.values().stream().filter(WebSocketSession::isConnected).collect(Collectors.toList()) :
            List.of();
    }

    /**
     * 获取所有活跃会话
     */
    public List<WebSocketSession> getAllActiveSessions() {
        return sessions.values().stream()
            .filter(WebSocketSession::isConnected)
            .collect(Collectors.toList());
    }

    /**
     * 根据条件过滤会话
     */
    public List<WebSocketSession> getSessionsByPredicate(Predicate<WebSocketSession> predicate) {
        return sessions.values().stream()
            .filter(WebSocketSession::isConnected)
            .filter(predicate)
            .collect(Collectors.toList());
    }

    /**
     * 获取部门会话
     */
    public List<WebSocketSession> getDepartmentSessions(Long departmentId) {
        return getSessionsByPredicate(session -> departmentId.equals(session.getDepartmentId()));
    }

    /**
     * 获取角色会话
     */
    public List<WebSocketSession> getRoleSessions(String role) {
        return getSessionsByPredicate(session -> session.getRoles().contains(role));
    }

    /**
     * 获取在线用户数量
     */
    public int getOnlineUserCount() {
        return (int) sessions.values().stream().filter(WebSocketSession::isConnected).count();
    }

    /**
     * 获取在线用户ID列表
     */
    public List<Long> getOnlineUserIds() {
        return sessions.values().stream()
            .filter(WebSocketSession::isConnected)
            .map(WebSocketSession::getUserId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        return getSessionByUserId(userId) != null;
    }

    /**
     * 清理过期会话
     */
    public void cleanExpiredSessions() {
        List<String> expiredSessionIds = sessions.values().stream()
            .filter(WebSocketSession::isExpired)
            .map(WebSocketSession::getSessionId)
            .collect(Collectors.toList());

        expiredSessionIds.forEach(this::removeSession);

        if (!expiredSessionIds.isEmpty()) {
            log.info("清理过期会话: {} 个", expiredSessionIds.size());
        }
    }

    /**
     * 获取会话统计信息
     */
    public Map<String, Object> getSessionStats() {
        return Map.of(
            "totalSessions", sessions.size(),
            "connectedSessions", getAllActiveSessions().size(),
            "totalRooms", roomSessions.size(),
            "totalModules", moduleSessions.size(),
            "onlineUsers", getOnlineUserCount()
        );
    }
}