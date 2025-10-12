# OpenIM 消息历史 REST API 调查报告

## 调查时间
**2025-10-11**

## 问题描述

尝试实现 REST API fallback 方案来解决无痕模式首次登录无法加载历史消息的问题。

后端实现了 `/api/im/business/messages/history` 端点,调用 OpenIM Server 的 `/msg/get_history_message_list` API。

### 错误信息

```
POST http://localhost:10002/msg/get_history_message_list
404 page not found
```

## 调查过程

### 1. 验证 OpenIM Server 运行状态

```bash
$ wsl docker ps --filter "name=openim"
NAMES                STATUS                 PORTS
openim-server        Up 8 hours (healthy)   0.0.0.0:10001-10002->10001-10002/tcp
openim-chat          Up 8 hours (healthy)   0.0.0.0:10008-10009->10008-10009/tcp
```

✅ **结论**: OpenIM Server 正常运行

### 2. 测试 OpenIM Server 连接性

```bash
$ curl -X POST http://localhost:10002/auth/get_admin_token \
  -H "Content-Type: application/json" \
  -d '{"secret":"openIM123","userID":"imAdmin"}'

{"errCode":1001,"errMsg":"ArgsError","errDlt":"header must have operationID"}
```

✅ **结论**: OpenIM Server 正常响应,只是缺少 `operationID` 头

### 3. 测试其他 OpenIM 端点

```bash
$ curl -X POST http://localhost:10002/user/get_users_info \
  -H "Content-Type: application/json" \
  -H "operationID: TEST_001" \
  -H "token: test" \
  -d '{"userIDs":["emp_1"]}'

{"errCode":1503,"errMsg":"TokenMalformedError","errDlt":"jwt parse error"}
```

✅ **结论**: 端点存在,只是 Token 无效(预期行为)

### 4. 检查现有代码中的 API 端点

**IMConstant.java** 中定义的端点:

```java
String API_USER_TOKEN = "/auth/get_user_token";           // ✅ 存在
String API_USER_REGISTER = "/user/user_register";         // ✅ 存在
String API_USER_UPDATE = "/user/update_user_info";        // ✅ 存在
String API_GROUP_CREATE = "/group/create_group";          // ✅ 存在
String API_MESSAGE_SEND = "/msg/send_msg";                // ✅ 存在
String API_MESSAGE_HISTORY = "/msg/get_history_message_list";  // ❌ 404
```

### 5. 搜索 OpenIM 官方文档

**文档来源**:
- https://docs.openim.io/
- https://doc.rentsoft.cn/
- https://github.com/openimsdk/open-im-server

**搜索结果**:

1. **REST API 文档** (https://doc.rentsoft.cn/restapi/apis/messagemanagement/sendmessage):
   - ✅ Send Message: `/msg/send_msg`
   - ✅ Batch Send Messages
   - ✅ Delete All User Messages
   - ✅ Revoke Message
   - ❌ **没有** 查询/获取历史消息的 REST API

2. **SDK API 文档** (https://doc.rentsoft.cn/sdks/api/message):
   - ✅ `getAdvancedHistoryMessageList()` - SDK 方法
   - ✅ `getAdvancedHistoryMessageListReverse()` - SDK 方法
   - ℹ️ 这些是 **SDK 级别** 的方法,不是 REST API

3. **GitHub 仓库搜索**:
   - 搜索 `get_history_message` 相关代码
   - 未找到对应的 REST API 路由定义

## 根本原因

### OpenIM 架构设计

OpenIM 采用 **分层架构**,不同操作使用不同的通信方式:

#### 1. **REST API** (管理员级别操作)
**用途**: 后端业务系统集成,具有超级用户权限

**支持的操作**:
- ✅ 用户管理: 注册、更新用户信息
- ✅ 群组管理: 创建、解散、更新群组
- ✅ 消息发送: 模拟发送消息(用于导入历史记录)
- ✅ 权限管理: 邀请、踢出成员
- ❌ **不支持**: 查询消息历史

#### 2. **SDK + WebSocket** (客户端级别操作)
**用途**: 前端应用,用户级别操作

**支持的操作**:
- ✅ 实时消息接收
- ✅ 查询历史消息 (通过 SDK 方法)
- ✅ 用户状态同步
- ✅ 会话管理

**实现方式**:
- 消息存储在 **IndexedDB** (浏览器本地数据库)
- 历史消息通过 **WebSocket** 或 **SDK 内部 RPC** 获取
- **不经过** REST API

### 为什么没有消息历史 REST API?

1. **性能考虑**:
   - 历史消息查询频繁,使用 WebSocket 更高效
   - REST API 每次请求都需要建立 HTTP 连接

2. **安全考虑**:
   - REST API 具有超级用户权限,不应用于频繁的客户端操作
   - SDK 通过用户 Token 控制权限

3. **架构设计**:
   - OpenIM SDK 设计为 **自包含** 的客户端解决方案
   - 所有客户端操作都通过 SDK 完成
   - REST API 只是辅助后端业务集成

## 现状总结

| 需求 | 可行性 | 说明 |
|------|--------|------|
| 通过 REST API 获取消息历史 | ❌ 不可行 | OpenIM Server 不提供此 REST API |
| 通过 SDK 获取消息历史 | ✅ 可行 | 但无痕模式首次登录时 IndexedDB 为空 |
| 后端主动查询消息 | ❌ 不可行 | 后端无法使用 SDK (WASM SDK 只能在浏览器运行) |

## 解决方案

### ❌ 方案 1: REST API Fallback (当前方案)
**状态**: **不可行** - OpenIM Server 不提供 REST API

### ✅ 方案 2: 后端消息镜像存储

**思路**: 在后端数据库存储所有消息副本

**实现**:
1. 当消息通过 WebSocket 到达时,同时保存到后端数据库
2. 无痕模式首次登录时,从后端数据库加载历史消息
3. 后续消息通过 SDK + WebSocket 正常处理

**优点**:
- ✅ 完全可控
- ✅ 可以实现复杂查询(搜索、过滤)
- ✅ 数据备份
- ✅ 支持审计和合规

**缺点**:
- 需要额外的数据库表
- 需要同步逻辑
- 增加存储成本

**数据表设计**:
```sql
CREATE TABLE im_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_id BIGINT NOT NULL COMMENT '警情ID',
    group_id VARCHAR(100) NOT NULL COMMENT 'OpenIM群组ID',
    message_id VARCHAR(100) NOT NULL COMMENT 'OpenIM消息ID',
    sender_id VARCHAR(100) NOT NULL COMMENT '发送者OpenIM用户ID',
    sender_name VARCHAR(100) COMMENT '发送者姓名',
    content TEXT COMMENT '消息内容',
    content_type INT COMMENT '消息类型(101-文本,102-图片等)',
    send_time BIGINT COMMENT '发送时间戳',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_report_id (report_id),
    INDEX idx_group_id (group_id),
    INDEX idx_send_time (send_time),
    UNIQUE KEY uk_message_id (message_id)
) COMMENT='IM消息镜像表';
```

### ✅ 方案 3: 使用 OpenIM Go SDK (后端)

**思路**: 在后端集成 OpenIM Go SDK

**实现**:
1. 引入 `github.com/openimsdk/openim-sdk-core` (Go SDK)
2. 后端使用 SDK 查询消息
3. 提供 REST API 给前端

**优点**:
- ✅ 官方支持
- ✅ 与 OpenIM 架构一致
- ✅ 可以使用完整的 SDK 功能

**缺点**:
- 需要引入 Go 依赖
- 后端需要维护 SDK 连接状态
- 可能存在性能开销

**参考**:
- GitHub: https://github.com/openimsdk/openim-sdk-core
- 示例: https://github.com/openimsdk/open-im-server/tree/main/examples

### ✅ 方案 4: 用户体验优化 (最简单)

**思路**: 接受无痕模式的限制,通过 UI 提示引导用户

**实现**:
1. 检测到无痕模式 + 无历史消息时,显示提示:
   ```
   ℹ️ 提示: 无痕模式首次登录无法加载历史消息

   解决方法:
   - 使用普通模式浏览器
   - 或等待其他用户发送新消息后刷新
   ```

2. 提供"刷新"按钮,重新尝试加载

**优点**:
- ✅ 实现简单
- ✅ 不需要额外开发
- ✅ 符合无痕模式的预期行为(不保留数据)

**缺点**:
- 用户体验略差
- 无法查看历史消息

## 推荐方案

### 🎯 推荐: **方案 2 - 后端消息镜像存储**

**理由**:
1. **完全可控**: 不依赖 OpenIM 的 API 限制
2. **业务价值**: 可以实现消息搜索、统计、审计等功能
3. **合规要求**: 警情系统需要完整的消息记录用于追溯
4. **性能优化**: 可以根据业务需求优化查询性能
5. **数据备份**: 避免依赖 OpenIM 的数据持久化

### 实施步骤

#### Phase 1: 数据库设计
1. 创建 `im_message` 表
2. 创建相关索引
3. 添加迁移脚本

#### Phase 2: 后端实现
1. 创建 `IMMessageEntity` 和 `IMMessageDao`
2. 在 WebSocket 消息处理中添加保存逻辑:
   ```java
   // WebSocketHandler 中
   public void handleMessage(WebSocketMessage message) {
       // 1. 转发给 OpenIM
       forwardToOpenIM(message);

       // 2. 保存到数据库
       imMessageService.saveMessage(message);
   }
   ```
3. 创建查询历史消息的 REST API:
   ```java
   @GetMapping("/api/im/business/messages/history/{reportId}")
   public ResponseDTO<List<MessageVO>> getMessageHistory(
       @PathVariable Long reportId,
       @RequestParam(defaultValue = "50") Integer limit,
       @RequestParam(required = false) Long beforeTime
   ) {
       return ResponseDTO.ok(imMessageService.getMessageHistory(reportId, limit, beforeTime));
   }
   ```

#### Phase 3: 前端集成
1. 修改 ChatPanel.vue 的 `loadHistoryMessages()`:
   ```typescript
   async function loadHistoryMessages() {
       try {
           // 1. 尝试从 SDK 加载
           const sdkMessages = await openIMClient.getHistoryMessages(...);

           if (sdkMessages && sdkMessages.length > 0) {
               // SDK 有数据,使用 SDK 数据
               return sdkMessages;
           }

           // 2. SDK 无数据,从后端加载
           console.log('📥 [历史消息] SDK无数据,从后端数据库加载');
           const response = await imBusinessApi.getMessageHistory(reportId, 50);
           return response.data;

       } catch (error) {
           console.error('❌ [历史消息] 加载失败:', error);
           return [];
       }
   }
   ```

#### Phase 4: 数据同步
1. 实现历史消息批量导入(如果已有消息)
2. 定期同步 OpenIM 和后端数据库

## 待办事项

- [ ] 创建数据库表 `im_message`
- [ ] 实现 `IMMessageService` 保存/查询逻辑
- [ ] 在 WebSocket Handler 中添加消息保存
- [ ] 实现 REST API `/api/im/business/messages/history/{reportId}`
- [ ] 修改前端 ChatPanel.vue 的加载逻辑
- [ ] 测试无痕模式场景
- [ ] 更新文档

## 参考资料

1. **OpenIM 官方文档**:
   - REST API: https://doc.rentsoft.cn/restapi/apis/introduction
   - SDK API: https://doc.rentsoft.cn/sdks/api/message

2. **OpenIM GitHub**:
   - Server: https://github.com/openimsdk/open-im-server
   - SDK Core: https://github.com/openimsdk/openim-sdk-core

3. **相关 Issue**:
   - 本次调查产生的文档

## 结论

OpenIM Server **不提供** 消息历史查询的 REST API。

**原因**: OpenIM 架构设计将消息查询功能限定在 SDK + WebSocket 层面,REST API 仅用于管理员级别的操作。

**最佳解决方案**: 实现后端消息镜像存储,既解决了无痕模式问题,又为业务提供了更多可能性(搜索、审计、统计等)。

---

**调查完成时间**: 2025-10-11
**调查人员**: Claude Code Assistant
**问题类型**: OpenIM 架构理解错误
**影响范围**: REST API fallback 方案需要重新设计
**后续行动**: 采用方案 2 - 后端消息镜像存储

---

## 附录: OpenIM REST API 端点清单

### ✅ 支持的端点

| 类别 | 端点 | 说明 |
|------|------|------|
| 认证 | `/auth/get_user_token` | 获取用户Token |
| 认证 | `/auth/get_admin_token` | 获取管理员Token |
| 用户 | `/user/user_register` | 注册用户 |
| 用户 | `/user/update_user_info` | 更新用户信息 |
| 用户 | `/user/get_users_info` | 获取用户信息 |
| 群组 | `/group/create_group` | 创建群组 |
| 群组 | `/group/set_group_info` | 更新群组信息 |
| 群组 | `/group/dismiss_group` | 解散群组 |
| 群组 | `/group/invite_user_to_group` | 邀请成员 |
| 群组 | `/group/kick_group_member` | 移除成员 |
| 群组 | `/group/get_groups_info` | 获取群组信息 |
| 群组 | `/group/get_group_member_list` | 获取成员列表 |
| 消息 | `/msg/send_msg` | 发送消息 |

### ❌ 不支持的端点

| 期望的端点 | 状态 | 替代方案 |
|-----------|------|---------|
| `/msg/get_history_message_list` | ❌ 404 | 使用 SDK 或后端存储 |
| `/msg/query_messages` | ❌ 不存在 | 使用 SDK 或后端存储 |
| `/msg/pull_messages` | ❌ 不存在 | 使用 SDK 或后端存储 |

---

**文档版本**: v1.0
**最后更新**: 2025-10-11
