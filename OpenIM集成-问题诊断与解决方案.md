# OpenIM集成 - 问题诊断与解决方案

## 问题现象

前端聊天面板无法连接IM服务,控制台报错:
```
📱 [聊天面板] 开始初始化... {groupID: '800479929', reportId: 5}
📱 [IM服务] 开始连接IM...
❌ [IM服务] IM连接失败
```

## 根本原因分析

### 1. OpenIM服务内部RPC问题

OpenIM服务日志显示多个RPC服务连接失败:
```
WARN openim-push - rpc client response failed
Error: 14 last connection error: connection error: desc = "transport: Error while dialing: dial tcp 172.20.0.3:34739: connect: connection refused"
initUsersOnlineStatus: getAllOnlineUsers failed
```

**根本原因**:
- `openim-rpc-user` 服务未正常启动或监听
- 多个Push服务无法连接到User RPC服务
- MessageGateway服务也受到影响

### 2. 用户Token获取失败

后端日志显示:
```
WARN [PID:763] openim-rpc-auth - rpc server response failed
Error: 1004 RecordNotFoundError record not found
method: "/openim.auth.Auth/getUserToken"
req: platformID:5 userID:"1"
```

**根本原因**:
- 尝试为员工ID=1获取IM Token
- 该用户在OpenIM中不存在(未注册)
- 虽然后端有自动注册逻辑,但在Token获取时失败

### 3. 后端IM配置条件Bean加载

`ImAuthController`使用条件注解:
```java
@ConditionalOnProperty(prefix = "openim", name = "api-url")
public class ImAuthController {
```

**配置检查**:
- ✅ `sa-base.yaml` 中配置正确: `openim.api-url: http://localhost:10002`
- ✅ Maven Profile激活正确: `active: 'dev'`
- ✅ 编译后的配置文件正确包含OpenIM配置

**潜在问题**:
- 条件注解的property路径需要完全匹配
- SpringBoot配置加载时机可能影响Bean创建

## 解决方案

### 方案一: 重启OpenIM完整服务栈 (推荐)

由于RPC服务连接问题,建议重启整个OpenIM服务栈:

```bash
# 1. 停止所有OpenIM服务
docker stop openim-server openim-chat

# 2. 启动OpenIM服务器 (包含所有RPC服务)
docker start openim-server

# 3. 等待30秒让所有RPC服务完全启动
sleep 30

# 4. 启动OpenIM Chat服务
docker start openim-chat

# 5. 检查服务状态
docker ps | grep openim
docker logs openim-server --tail 20
```

### 方案二: 修复后端Bean加载条件

修改`ImAuthController`的条件注解,使其更宽松:

```java
@ConditionalOnProperty(prefix = "openim", name = "api-url", matchIfMissing = false)
```

或者完全移除条件注解,改为在Service层判断:

```java
// ImUserSyncService.java
public ImTokenVO getImToken(Long employeeId) {
    if (StringUtils.isBlank(openIMProperties.getApiUrl())) {
        throw new RuntimeException("OpenIM未配置,请检查openim.api-url配置项");
    }
    // ... 原有逻辑
}
```

### 方案三: 手动注册测试用户

通过OpenIM Admin API手动注册测试用户:

```bash
# 1. 获取管理员Token
ADMIN_TOKEN=$(curl -s -X POST http://localhost:10002/auth/get_admin_token \
  -H "Content-Type: application/json" \
  -d '{"secret":"openIM123"}' | jq -r '.data.token')

# 2. 注册用户ID=1
curl -X POST http://localhost:10002/user/user_register \
  -H "Content-Type: application/json" \
  -H "token: $ADMIN_TOKEN" \
  -d '{
    "secret": "openIM123",
    "users": [{
      "userID": "1",
      "nickname": "测试用户",
      "faceURL": ""
    }]
  }'
```

### 方案四: 前端降级处理 (临时方案)

在前端IM连接失败时,显示友好提示而不是报错:

```typescript
// im.service.ts - connect方法
const connect = async (): Promise<boolean> => {
  try {
    // ... 连接逻辑
  } catch (error) {
    console.error('❌ [IM服务] IM连接失败:', error);
    connectionStatus.value = 3;

    // 友好提示,不再使用antMessage.error
    console.warn('💡 [IM服务] IM功能暂时不可用,将使用基础消息功能');
    return false;
  }
}
```

## 快速验证步骤

1. **检查OpenIM服务状态**:
```bash
docker ps | grep openim
docker logs openim-server --tail 50 | grep -E "listening|error|Error"
```

2. **测试OpenIM Admin API**:
```bash
curl -X POST http://localhost:10002/auth/get_admin_token \
  -H "Content-Type: application/json" \
  -d '{"secret":"openIM123"}'
```

3. **测试后端IM Token API (需要登录)**:
```bash
curl -X GET http://localhost:1024/api/im/auth/token \
  -H "x-access-token: <your_smartadmin_token>"
```

4. **检查后端Bean加载**:
```bash
# 查看后端启动日志,搜索IM相关Bean
grep -i "ImAuthController\|ImUserSyncService" logs/smart_admin_v3/sa-admin/dev/*.log
```

## 推荐执行顺序

1. **立即执行**: 方案一 (重启OpenIM服务)
2. **如果问题依旧**: 方案三 (手动注册用户)
3. **长期优化**: 方案二 (修改Bean加载条件)
4. **用户体验**: 方案四 (前端降级提示)

## 配置检查清单

- [x] OpenIM配置已添加到 `sa-base.yaml`
- [x] Maven Profile激活为 `dev`
- [x] OpenIM服务容器正在运行
- [ ] OpenIM所有RPC服务正常启动
- [ ] 测试用户已在OpenIM注册
- [ ] 后端IM相关Bean成功加载
- [ ] 前端可以获取IM Token

## 后续优化建议

1. **健康检查**: 添加OpenIM服务健康检查端点
2. **自动重启**: 配置RPC服务连接失败时的自动重启机制
3. **用户同步**: 在用户登录时自动触发OpenIM用户同步
4. **错误处理**: 完善前端IM连接失败的降级逻辑
5. **监控告警**: 添加OpenIM服务状态监控和告警

---

**最后更新时间**: 2025-10-09
**问题严重程度**: 🔴 高 (阻塞IM功能)
**预计修复时间**: 10-30分钟
