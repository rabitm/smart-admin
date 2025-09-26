package net.lab1024.sa.admin.module.support.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketMessage;
import net.lab1024.sa.admin.module.support.websocket.domain.WebSocketSession;
import net.lab1024.sa.admin.module.support.websocket.service.MessageTransport;
import net.lab1024.sa.admin.module.support.websocket.service.WebSocketSessionManager;
import net.lab1024.sa.admin.module.support.websocket.service.impl.WebSocketTransport;
import net.lab1024.sa.admin.module.system.employee.service.EmployeeService;
import net.lab1024.sa.base.common.domain.RequestUser;
import net.lab1024.sa.base.common.util.SmartRequestUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一WebSocket处理器
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnifiedWebSocketHandler implements WebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final WebSocketTransport webSocketTransport;
    private final EmployeeService employeeService;
    private final ObjectMapper objectMapper;

    /**
     * 业务消息处理器映射
     */
    private final Map<String, MessageHandler> messageHandlers = new ConcurrentHashMap<>();

    /**
     * 注册消息处理器
     */
    public void registerMessageHandler(String module, MessageHandler handler) {
        messageHandlers.put(module, handler);
        log.info("注册WebSocket消息处理器: {}", module);
    }

    @Override
    public void afterConnectionEstablished(org.springframework.web.socket.WebSocketSession session) throws Exception {
        try {
            // 解析连接参数
            URI uri = session.getUri();
            Map<String, String> params = parseQueryParams(uri.getQuery());

            String employeeIdStr = params.get("employeeId");
            String seatIdStr = params.get("seatId");

            if (employeeIdStr == null) {
                log.warn("WebSocket连接缺少employeeId参数，关闭连接: {}", session.getId());
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            Long employeeId = Long.parseLong(employeeIdStr);

            // 创建会话信息
            WebSocketSession wsSession = new WebSocketSession()
                .setSessionId(session.getId())
                .setUserId(employeeId)
                .setClientIp(getClientIp(session))
                .setUserAgent(getUserAgent(session))
                .setConnectTime(LocalDateTime.now())
                .setLastActiveTime(LocalDateTime.now());

            // 设置座位信息
            if (seatIdStr != null) {
                wsSession.setAttribute("seatId", Long.parseLong(seatIdStr));
            }

            // 获取用户信息并设置权限
            try {
                var employee = employeeService.getById(employeeId);
                if (employee != null) {
                    wsSession.setUsername(employee.getActualName())
                            .setDepartmentId(employee.getDepartmentId());

                    // TODO: 获取用户角色信息
                    wsSession.getRoles().add("user");
                } else {
                    // 员工不存在，设置默认值
                    wsSession.setUsername("User_" + employeeId);
                    wsSession.getRoles().add("user");
                }
            } catch (Exception e) {
                log.warn("获取员工信息失败: employeeId={}, error={}, 使用默认值", employeeId, e.getMessage());
                // 设置默认值，不阻止连接
                wsSession.setUsername("User_" + employeeId);
                wsSession.getRoles().add("user");
            }

            // 添加到会话管理器
            sessionManager.addSession(wsSession);

            // 添加到WebSocket传输层
            webSocketTransport.addSpringWebSocketSession(session.getId(), session);

            // 发送连接成功消息
            try {
                WebSocketMessage welcomeMessage = WebSocketMessage.system("CONNECTED", "WebSocket连接成功")
                    .setFromUserId(wsSession.getUserId())
                    .setFromUserName(wsSession.getUsername());

                webSocketTransport.sendToSession(session.getId(), welcomeMessage);
            } catch (Exception e) {
                log.warn("发送WebSocket欢迎消息失败: sessionId={}, error={}", session.getId(), e.getMessage());
                // 不因为发送消息失败而关闭连接
            }

            log.info("WebSocket连接建立成功: sessionId={}, userId={}, seatId={}",
                session.getId(), employeeId, seatIdStr);

        } catch (Exception e) {
            log.error("建立WebSocket连接失败: sessionId={}, error={}", session.getId(), e.getMessage(), e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void handleMessage(org.springframework.web.socket.WebSocketSession session, org.springframework.web.socket.WebSocketMessage<?> message) throws Exception {
        try {
            String payload = message.getPayload().toString();
            net.lab1024.sa.admin.module.support.websocket.domain.WebSocketMessage wsMessage = objectMapper.readValue(payload, net.lab1024.sa.admin.module.support.websocket.domain.WebSocketMessage.class);

            // 更新会话活跃时间
            sessionManager.updateSessionActivity(session.getId());

            // 设置消息来源信息
            WebSocketSession wsSession = sessionManager.getSession(session.getId());
            if (wsSession != null) {
                wsMessage.setFromUserId(wsSession.getUserId())
                          .setFromUserName(wsSession.getUsername());
            }

            log.debug("收到WebSocket消息: sessionId={}, type={}, module={}",
                session.getId(), wsMessage.getType(), wsMessage.getModule());

            // 处理系统消息
            if (handleSystemMessage(session, wsMessage)) {
                return;
            }

            // 处理业务消息
            handleBusinessMessage(session, wsMessage);

        } catch (Exception e) {
            log.error("处理WebSocket消息失败: sessionId={}, error={}", session.getId(), e.getMessage(), e);

            // 发送错误响应
            WebSocketMessage errorMessage = WebSocketMessage.system("ERROR", "消息处理失败: " + e.getMessage());
            webSocketTransport.sendToSession(session.getId(), errorMessage);
        }
    }

    @Override
    public void handleTransportError(org.springframework.web.socket.WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: sessionId={}, error={}", session.getId(), exception.getMessage(), exception);
    }

    @Override
    public void afterConnectionClosed(org.springframework.web.socket.WebSocketSession session, CloseStatus closeStatus) throws Exception {
        try {
            WebSocketSession wsSession = sessionManager.getSession(session.getId());
            log.info("WebSocket连接关闭: sessionId={}, userId={}, status={}",
                session.getId(),
                wsSession != null ? wsSession.getUserId() : "unknown",
                closeStatus);

            // 从会话管理器移除
            sessionManager.removeSession(session.getId());

            // 从WebSocket传输层移除
            webSocketTransport.removeSpringWebSocketSession(session.getId());

        } catch (Exception e) {
            log.error("处理WebSocket连接关闭失败: sessionId={}, error={}", session.getId(), e.getMessage(), e);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 处理系统消息
     */
    private boolean handleSystemMessage(org.springframework.web.socket.WebSocketSession session, WebSocketMessage wsMessage) {
        String type = wsMessage.getType();

        switch (type) {
            case "PING":
                // 心跳消息
                WebSocketMessage pongMessage = WebSocketMessage.system("PONG", "心跳响应")
                    .setData(wsMessage.getData());
                webSocketTransport.sendToSession(session.getId(), pongMessage);
                return true;

            case "SUBSCRIBE_ROOM":
                // 订阅房间
                String room = (String) wsMessage.getData().get("room");
                if (room != null) {
                    sessionManager.subscribeRoom(session.getId(), room);
                    WebSocketMessage ackMessage = WebSocketMessage.system("SUBSCRIBE_ACK", "订阅房间成功: " + room);
                    webSocketTransport.sendToSession(session.getId(), ackMessage);
                }
                return true;

            case "UNSUBSCRIBE_ROOM":
                // 取消订阅房间
                String unsubRoom = (String) wsMessage.getData().get("room");
                if (unsubRoom != null) {
                    sessionManager.unsubscribeRoom(session.getId(), unsubRoom);
                    WebSocketMessage ackMessage = WebSocketMessage.system("UNSUBSCRIBE_ACK", "取消订阅房间成功: " + unsubRoom);
                    webSocketTransport.sendToSession(session.getId(), ackMessage);
                }
                return true;

            case "SUBSCRIBE_MODULE":
                // 订阅模块
                String module = (String) wsMessage.getData().get("module");
                if (module != null) {
                    sessionManager.subscribeModule(session.getId(), module);
                    WebSocketMessage ackMessage = WebSocketMessage.system("SUBSCRIBE_ACK", "订阅模块成功: " + module);
                    webSocketTransport.sendToSession(session.getId(), ackMessage);
                }
                return true;

            case "UNSUBSCRIBE_MODULE":
                // 取消订阅模块
                String unsubModule = (String) wsMessage.getData().get("module");
                if (unsubModule != null) {
                    sessionManager.unsubscribeModule(session.getId(), unsubModule);
                    WebSocketMessage ackMessage = WebSocketMessage.system("UNSUBSCRIBE_ACK", "取消订阅模块成功: " + unsubModule);
                    webSocketTransport.sendToSession(session.getId(), ackMessage);
                }
                return true;

            default:
                return false;
        }
    }

    /**
     * 处理业务消息
     */
    private void handleBusinessMessage(org.springframework.web.socket.WebSocketSession session, WebSocketMessage wsMessage) {
        String module = wsMessage.getModule();
        if (module == null) {
            log.warn("业务消息缺少module字段: sessionId={}", session.getId());
            return;
        }

        MessageHandler handler = messageHandlers.get(module);
        if (handler == null) {
            log.warn("未找到模块{}的消息处理器", module);
            return;
        }

        try {
            handler.handleMessage(session.getId(), wsMessage);
        } catch (Exception e) {
            log.error("业务消息处理失败: module={}, sessionId={}, error={}", module, session.getId(), e.getMessage(), e);
        }
    }

    /**
     * 解析查询参数
     */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new ConcurrentHashMap<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    params.put(kv[0], kv[1]);
                }
            }
        }
        return params;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(org.springframework.web.socket.WebSocketSession session) {
        return session.getRemoteAddress() != null ? session.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    /**
     * 获取用户代理
     */
    private String getUserAgent(org.springframework.web.socket.WebSocketSession session) {
        return session.getHandshakeHeaders().getFirst("User-Agent");
    }

    /**
     * 消息处理器接口
     */
    public interface MessageHandler {
        void handleMessage(String sessionId, WebSocketMessage message) throws Exception;
    }
}