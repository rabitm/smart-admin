package net.lab1024.sa.admin.module.business.collaboration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.lab1024.sa.admin.module.system.login.domain.RequestEmployee;
import net.lab1024.sa.admin.util.AdminRequestUtil;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协作控制器 - REST接口版本
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */
@RestController
@RequestMapping("/api/collaboration")
@Tag(name = "协作管理")
public class CollaborationController {

    // 内存存储活跃用户（生产环境应使用Redis）
    private final Map<String, Map<String, Object>> activeUsers = new ConcurrentHashMap<>();

    @Operation(summary = "获取房间活跃用户")
    @GetMapping("/users/{roomId}")
    public ResponseDTO<List<Map<String, Object>>> getActiveUsers(@PathVariable String roomId) {
        Map<String, Object> roomUsers = activeUsers.get(roomId);
        List<Map<String, Object>> users = new ArrayList<>();

        if (roomUsers != null) {
            for (Object user : roomUsers.values()) {
                if (user instanceof Map) {
                    users.add((Map<String, Object>) user);
                }
            }
        }

        return ResponseDTO.ok(users);
    }

    @Operation(summary = "用户加入房间")
    @PostMapping("/join/{roomId}")
    public ResponseDTO<String> joinRoom(@PathVariable String roomId) {

        // 获取当前登录用户的真实信息
        RequestEmployee currentUser = AdminRequestUtil.getRequestUser();
        if (currentUser == null) {
            return ResponseDTO.userErrorParam("用户未登录");
        }

        String userId = currentUser.getEmployeeId().toString();
        String userName = currentUser.getActualName();

        Map<String, Object> user = new HashMap<>();
        user.put("id", userId);
        user.put("name", userName);
        user.put("avatar", currentUser.getAvatar());
        user.put("departmentName", currentUser.getDepartmentName());
        user.put("email", currentUser.getEmail());
        user.put("phone", currentUser.getPhone());
        user.put("color", generateUserColor(userId));
        user.put("sessionId", "session_" + System.currentTimeMillis());
        user.put("lastActivity", System.currentTimeMillis());
        user.put("isOnline", true);
        user.put("joinTime", LocalDateTime.now());

        activeUsers.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(userId, user);

        return ResponseDTO.ok("用户 " + userName + " 加入房间成功");
    }

    @Operation(summary = "用户离开房间")
    @PostMapping("/leave/{roomId}")
    public ResponseDTO<String> leaveRoom(@PathVariable String roomId) {
        // 获取当前登录用户信息
        RequestEmployee currentUser = AdminRequestUtil.getRequestUser();
        if (currentUser == null) {
            return ResponseDTO.userErrorParam("用户未登录");
        }

        String userId = currentUser.getEmployeeId().toString();
        Map<String, Object> roomUsers = activeUsers.get(roomId);
        if (roomUsers != null) {
            roomUsers.remove(userId);
            if (roomUsers.isEmpty()) {
                activeUsers.remove(roomId);
            }
        }
        return ResponseDTO.ok("用户 " + currentUser.getActualName() + " 离开房间成功");
    }

    @Operation(summary = "更新用户活跃状态")
    @PostMapping("/heartbeat/{roomId}")
    public ResponseDTO<String> heartbeat(@PathVariable String roomId) {
        // 获取当前登录用户信息
        RequestEmployee currentUser = AdminRequestUtil.getRequestUser();
        if (currentUser == null) {
            return ResponseDTO.userErrorParam("用户未登录");
        }

        String userId = currentUser.getEmployeeId().toString();
        Map<String, Object> roomUsers = activeUsers.get(roomId);
        if (roomUsers != null) {
            Map<String, Object> user = (Map<String, Object>) roomUsers.get(userId);
            if (user != null) {
                user.put("lastActivity", System.currentTimeMillis());
            }
        }
        return ResponseDTO.ok("心跳更新成功");
    }

    @Operation(summary = "协作状态测试")
    @GetMapping("/status")
    public ResponseDTO<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalRooms", activeUsers.size());
        status.put("totalUsers", activeUsers.values().stream()
                .mapToInt(room -> room.size())
                .sum());
        status.put("timestamp", LocalDateTime.now());
        status.put("websocketSupported", false); // 暂时使用REST接口
        status.put("allRooms", activeUsers);

        return ResponseDTO.ok(status);
    }

    @Operation(summary = "创建测试用户（用于演示协作功能）")
    @PostMapping("/test/addUser/{roomId}")
    public ResponseDTO<String> addTestUser(@PathVariable String roomId) {
        // 创建测试用户
        String testUserId = "test_user_" + System.currentTimeMillis();
        String testUserName = "测试用户" + (int)(Math.random() * 100);

        Map<String, Object> user = new HashMap<>();
        user.put("id", testUserId);
        user.put("name", testUserName);
        user.put("color", generateUserColor(testUserId));
        user.put("sessionId", "session_" + System.currentTimeMillis());
        user.put("lastActivity", System.currentTimeMillis());
        user.put("isOnline", true);
        user.put("joinTime", LocalDateTime.now());

        activeUsers.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(testUserId, user);

        return ResponseDTO.ok("测试用户已添加: " + testUserName);
    }

    /**
     * 生成用户颜色
     */
    private String generateUserColor(String userId) {
        String[] colors = {"#1890ff", "#52c41a", "#fa8c16", "#eb2f96", "#722ed1", "#13c2c2"};
        int hash = Math.abs(userId.hashCode());
        return colors[hash % colors.length];
    }
}