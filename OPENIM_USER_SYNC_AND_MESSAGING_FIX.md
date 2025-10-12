# OpenIM 用户同步和消息发送修复指南

## 修复状态

✅ **后端用户验证逻辑** - `IMUserSyncService.java` 添加自动验证和修复逻辑
✅ **前端消息发送参数** - `openim-client.ts` 修复 `sendGroupTextMessage` 参数错误
✅ **编译验证通过** - `mvn clean compile` 成功

⚠️ **待操作**：重启后端服务和刷新前端，验证修复效果

---

## 问题分析

### 问题 1: 用户同步状态不一致

**症状**:
- 数据库中用户映射记录显示 `sync_status = 1` (成功)
- OpenIM 服务器上实际不存在该用户
- 获取群成员信息返回 `{"members":null}`

**根本原因**: 类似群组同步问题，数据库状态与 OpenIM 服务器实际状态不一致

**修复方案**: 在 `IMUserSyncService.getOpenIMUserId()` 中添加用户验证逻辑

### 问题 2: OpenIM SDK 消息发送参数错误

**症状**:
```
❌ [OpenIM] 消息发送失败: {errMsg: "10204 Only failed messages can be resent", errCode: 10204}
=> (invoked by go wasm) run getConversation method with args ["si_<undefined>_emp_cf1e361fd46741f5b2a09335cef50db8"]
```

**根本原因**: `sendMessage` 方法缺少 `groupID` 和 `recvID` 参数，导致 SDK 错误解析为单聊消息

**修复方案**: 修复 `sendGroupTextMessage()` 方法，正确传入群聊参数

---

## 修复详情

### 1. 后端用户验证逻辑

**文件**: `IMUserSyncService.java:368-421`

```java
/**
 * 根据员工ID获取OpenIM用户ID
 *
 * 🔧 修复: 添加用户验证逻辑,防止数据库有映射但OpenIM服务器上无用户的情况
 */
public String getOpenIMUserId(Long employeeId) {
    IMUserMappingEntity mapping = imUserMappingDao.selectByEmployeeId(employeeId);

    // 如果映射存在且同步成功
    if (mapping != null && IMConstant.SYNC_STATUS_SUCCESS.equals(mapping.getSyncStatus())) {
        // 🔧 新增: 验证用户在OpenIM服务器上是否真实存在
        if (verifyUserExistsOnServer(mapping.getOpenimUserId())) {
            log.debug("✅ [用户验证] 员工{}的用户已存在且已验证: {}", employeeId, mapping.getOpenimUserId());
            return mapping.getOpenimUserId();
        } else {
            log.warn("⚠️ [用户验证] 数据库中存在用户映射,但OpenIM服务器上不存在,删除旧映射重新同步");
            // 删除旧映射,触发重新同步
            imUserMappingDao.deleteById(mapping.getId());
        }
    }

    // 如果未同步或验证失败,尝试同步
    return syncUser(employeeId);
}

/**
 * 验证用户在OpenIM服务器上是否存在
 */
private boolean verifyUserExistsOnServer(String openimUserId) {
    try {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userIDs", Collections.singletonList(openimUserId));

        JSONObject response = openIMClient.postWithRetry(
                IMConstant.API_USER_GET_INFO,
                requestBody,
                JSONObject.class,
                IMOperationTypeEnum.USER_SYNC
        );

        // 检查返回的 usersInfo 是否包含该用户
        if (response != null && response.containsKey("usersInfo")) {
            Object usersInfo = response.get("usersInfo");
            if (usersInfo instanceof List) {
                List<?> infos = (List<?>) usersInfo;
                return !infos.isEmpty();
            }
        }

        return false;
    } catch (Exception e) {
        log.warn("⚠️ [用户验证] 验证用户{}是否存在失败: {}", openimUserId, e.getMessage());
        // 验证失败时,假定用户不存在,触发重新同步
        return false;
    }
}
```

### 2. 前端消息发送参数修复

**文件**: `openim-client.ts:457-488`

```typescript
/**
 * 发送群组文本消息
 * @param groupID 群组ID
 * @param text 消息文本
 */
async sendGroupTextMessage(groupID: string, text: string): Promise<MessageItem> {
  if (!this.isLoggedIn) {
    throw new Error('未登录OpenIM');
  }

  try {
    console.log('📤 [OpenIM] 发送群组文本消息, groupID:', groupID);

    // 1. 创建文本消息
    const message = await this.sdk.createTextMessage(text);

    console.log('✅ [OpenIM] 消息创建成功:', message);

    // 2. 获取群组会话
    const conversation = await this.getConversation(groupID, 2); // 2表示群聊

    console.log('✅ [OpenIM] 获取会话成功:', conversation);

    // 3. 发送消息（必须使用 recvID 和 groupID）
    const result = await this.sdk.sendMessage({
      recvID: '', // 群聊时 recvID 为空
      groupID: groupID, // 指定群组ID
      message: message,
    });

    console.log('✅ [OpenIM] 群组消息发送成功:', result);
    return result;
  } catch (error) {
    console.error('❌ [OpenIM] 发送群消息失败:', error);
    throw error;
  }
}
```

**关键修复点**:
- ✅ 添加 `groupID` 参数明确指定群组
- ✅ 设置 `recvID: ''` (群聊时必须为空)
- ✅ 传入 `message` 对象而不是 `conversationID`

---

## 测试步骤

### 步骤 1: 重启后端服务

```bash
# 方法 A: 使用 IDEA
1. 停止当前运行的应用
2. 重新点击 Run 按钮启动

# 方法 B: 使用 Maven 命令行
cd smart-admin-api-java17-springboot3
mvn spring-boot:run
```

### 步骤 2: 刷新前端

```bash
# 硬刷新浏览器（清除缓存）
Ctrl + Shift + R  (Windows/Linux)
Cmd + Shift + R   (Mac)
```

### 步骤 3: 测试用户同步修复

访问：`http://localhost:8081/oa/police/report-detail?reportId=5`

切换到 **\"即时聊天\"** Tab

#### 🔍 预期后端日志

**场景 1: 首次访问，数据库有旧用户映射但服务器无用户**

```log
⚠️ [用户验证] 数据库中存在用户映射,但OpenIM服务器上不存在,删除旧映射重新同步
📤 [用户同步] 开始同步员工{employeeId} -> OpenIM用户{openimUserId}
✅ [用户同步] 员工{employeeId}同步成功,OpenIM用户ID: {openimUserId}, 耗时: {time}ms
```

**场景 2: 再次访问，用户已正常存在**

```log
✅ [用户验证] 员工{employeeId}的用户已存在且已验证: {openimUserId}
```

### 步骤 4: 测试消息发送修复

在聊天面板输入消息并点击 **\"发送\"** 按钮

#### 🔍 预期前端控制台输出

```
📤 [聊天面板] 发送消息: 测试消息
📤 [OpenIM] 发送群组文本消息, groupID: group_report_5
✅ [OpenIM] 消息创建成功: {clientMsgID: "xxx", ...}
✅ [OpenIM] 获取会话成功: {conversationID: "sg_group_report_5", ...}
✅ [OpenIM] 群组消息发送成功: {clientMsgID: "xxx", ...}
✅ [聊天面板] 消息发送成功: {clientMsgID: "xxx", ...}
📨 [聊天面板] 收到新消息: {content: "测试消息", ...}
```

#### ❌ 不应该再出现的错误

```
❌ [OpenIM] 消息发送失败: {errMsg: "10204 Only failed messages can be resent", errCode: 10204}
=> (invoked by go wasm) run getConversation method with args ["si_<undefined>_emp_..."]
```

---

## 验证成功的标志

### ✅ 后端日志

```
✅ [用户验证] 员工X的用户已存在且已验证: emp_xxx
✅ [群组创建] 警情5的群组创建成功: group_report_5
```

### ✅ 前端控制台

```
✅ [OpenIM] 群组消息发送成功
✅ [聊天面板] 消息发送成功
📨 [聊天面板] 收到新消息
```

### ✅ 数据库验证

```sql
-- 验证用户映射存在且状态正常
SELECT
    employee_id,
    openim_user_id,
    sync_status,
    sync_time
FROM t_im_user_mapping
WHERE employee_id = {your_employee_id};

-- 预期结果: sync_status = 1
```

### ✅ OpenIM 服务器验证

```bash
# 验证用户存在
curl -X POST http://localhost:10002/user/get_users_info \
  -H "Content-Type: application/json" \
  -H "token: YOUR_ADMIN_TOKEN" \
  -d '{"userIDs": ["emp_cf1e361fd46741f5b2a09335cef50db8"]}'

# 预期返回: usersInfo 不为 null，包含用户详细信息
```

### ✅ 功能测试

1. ✅ 可以发送消息
2. ✅ 可以查看历史消息
3. ✅ 消息实时同步到聊天面板
4. ✅ 其他用户可以接收消息（多窗口测试）

---

## 常见问题排查

### Q1: 后端日志没有 \"⚠️ 数据库中存在用户映射,但OpenIM服务器上不存在\" 提示

**原因**: 后端服务没有重启，还在运行旧代码

**解决**:
1. 停止后端服务
2. 重新运行 `mvn spring-boot:run` 或在 IDEA 中重启
3. 确认日志中显示应用启动成功

### Q2: 前端仍然报错 \"10204 Only failed messages can be resent\"

**原因**: 浏览器缓存了旧的 JavaScript 代码

**解决**:
1. 硬刷新: `Ctrl + Shift + R`
2. 或者清除浏览器缓存后重新访问
3. 或者使用隐私模式/无痕模式测试

### Q3: 后端报错 \"Connection refused\" 连接 OpenIM

**原因**: OpenIM 服务器未启动

**解决**:
1. 启动 OpenIM 服务器
2. 验证 OpenIM 运行正常：`curl http://localhost:10002/`
3. 检查端口配置是否正确

### Q4: 消息发送成功但看不到消息

**可能原因**:

1. **会话ID不匹配**: 检查前端控制台的 `conversationID`
2. **消息监听器未注册**: 检查 `onMessage` 是否正确调用
3. **群组ID错误**: 确认 `groupId` 传递正确

**排查步骤**:

```javascript
// 在浏览器控制台执行
console.log('当前会话ID:', openIMClient.currentUserId);
console.log('群组ID:', props.groupId);
console.log('消息监听器:', openIMClient.messageListeners);
```

---

## 自动修复流程

### 用户同步自动修复

```
1. 前端调用 createGroupForReport(reportId=5)
       ↓
2. 后端获取群组创建人 employeeId
       ↓
3. 调用 getOpenIMUserId(employeeId)
   ├─→ 查询数据库映射
   └─→ 映射存在且 sync_status=1
       ↓
4. 🔧 调用 verifyUserExistsOnServer(openimUserId)
   ├─→ OpenIM 有用户 → 返回 openimUserId ✅
   └─→ OpenIM 无用户 → 继续下一步
       ↓
5. 🔧 自动修复逻辑
   ├─→ 删除数据库旧用户映射
   ├─→ 调用 syncUser(employeeId)
   ├─→ 调用 OpenIM API 注册用户
   ├─→ 保存新映射到数据库
   └─→ 返回新 openimUserId ✅
       ↓
6. 使用正确的 openimUserId 创建群组 ✅
```

### 消息发送参数修复

**修复前（错误）**:
```typescript
await this.sdk.sendMessage({
  message: message,
  conversationID: conversation.conversationID
});
// ❌ 缺少 groupID，SDK 错误解析为单聊
```

**修复后（正确）**:
```typescript
await this.sdk.sendMessage({
  recvID: '',           // ✅ 群聊时 recvID 必须为空
  groupID: groupID,     // ✅ 明确指定群组ID
  message: message,     // ✅ 消息对象
});
```

---

## 下一步

修复验证通过后，建议：

1. **测试其他警情** - 确保修复对所有警情都生效
2. **测试新建警情** - 验证新创建的警情聊天功能正常
3. **多用户测试** - 在多个浏览器窗口测试消息同步
4. **监控后端日志** - 观察是否还有其他同步问题
5. **性能测试** - 测试高并发场景下的用户同步性能

---

**修复负责人**: Claude Code Assistant
**修复时间**: 2025-10-10 17:10
**测试状态**: 等待用户重启后端、刷新前端并验证

如有任何问题，请提供完整的后端日志和前端控制台输出！
