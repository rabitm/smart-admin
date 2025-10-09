# IM后端API测试指南

## 问题状态

前端日志显示:
- ✅ 群组加载成功: groupId='800479929'
- ❌ IM连接失败: HTTP 200但包含业务错误

## 测试步骤

### 1. 获取SmartAdmin登录Token

在浏览器开发者工具中运行:
```javascript
localStorage.getItem('x-access-token')
```

### 2. 测试IM Token API

使用获取的token测试后端API:

**Windows PowerShell:**
```powershell
$token = "YOUR_ACCESS_TOKEN_HERE"
$headers = @{
    "x-access-token" = $token
}

Invoke-RestMethod -Uri "http://localhost:1024/api/im/auth/token" -Method GET -Headers $headers
```

**cURL (Git Bash):**
```bash
curl -X GET http://localhost:1024/api/im/auth/token \
  -H "x-access-token: YOUR_ACCESS_TOKEN_HERE"
```

### 3. 预期响应格式

**成功响应:**
```json
{
  "code": 0,
  "msg": "操作成功",
  "ok": true,
  "data": {
    "userToken": "eyJhbGc...",
    "userID": "1",
    "apiUrl": "http://localhost:10002",
    "wsUrl": "ws://localhost:10001",
    "platformID": 5,
    "expireTime": 1767748205
  }
}
```

**失败响应(用户未注册):**
```json
{
  "code": 10001,
  "msg": "用户同步失败",
  "ok": false,
  "data": null
}
```

### 4. 检查后端Bean加载

查看后端启动日志,确认IM相关Bean是否加载:

```bash
# Windows (在项目根目录执行)
cd smart-admin-api-java17-springboot3
dir /s /b *.log | findstr "smart" | head -1 | xargs cat | findstr /i "ImAuth\|ImUser\|OpenIM"
```

预期应该看到类似:
```
Bean 'imAuthController' created
Bean 'imUserSyncService' created
OpenIMProperties loaded: apiUrl=http://localhost:10002
```

### 5. 手动注册OpenIM用户

如果用户未注册,执行以下步骤:

**步骤5.1: 获取OpenIM管理员Token**

```bash
curl -X POST http://localhost:10002/auth/get_admin_token \
  -H "Content-Type: application/json" \
  -d '{"secret":"openIM123"}'
```

响应示例:
```json
{
  "errCode": 0,
  "errMsg": "",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expireTimeSeconds": 1767834605
  }
}
```

**步骤5.2: 注册用户ID=1**

使用上一步获取的token:

```bash
ADMIN_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X POST http://localhost:10002/user/user_register \
  -H "Content-Type: application/json" \
  -H "token: $ADMIN_TOKEN" \
  -d '{
    "secret": "openIM123",
    "users": [{
      "userID": "1",
      "nickname": "系统管理员",
      "faceURL": ""
    }]
  }'
```

成功响应:
```json
{
  "errCode": 0,
  "errMsg": "",
  "data": {}
}
```

**步骤5.3: 验证用户是否注册成功**

```bash
curl -X POST http://localhost:10002/user/get_users \
  -H "Content-Type: application/json" \
  -H "token: $ADMIN_TOKEN" \
  -d '{
    "userIDs": ["1"]
  }'
```

## 故障排查清单

- [ ] 后端服务正常运行 (http://localhost:1024)
- [ ] OpenIM服务正常运行 (http://localhost:10002)
- [ ] `openim.api-url`配置存在于sa-base.yaml
- [ ] ImAuthController Bean成功加载
- [ ] OpenIM用户ID=1已注册
- [ ] SmartAdmin登录token有效
- [ ] 前端错误日志包含详细的后端错误信息

## 快速修复方案

### 方案A: 重新编译后端(推荐)

如果Bean未加载,可能是配置未正确编译:

```bash
cd smart-admin-api-java17-springboot3
mvn clean compile
mvn spring-boot:run
```

### 方案B: 临时移除条件注解

修改`ImAuthController.java`:

```java
// 注释掉条件注解
// @ConditionalOnProperty(prefix = "openim", name = "api-url")
@RestController
@RequestMapping("/api/im/auth")
public class ImAuthController {
    // ...
}
```

### 方案C: 添加配置验证日志

在`ImUserSyncService`添加启动日志:

```java
@PostConstruct
public void init() {
    log.info("📱 [IM服务] ImUserSyncService已加载");
    log.info("📱 [IM配置] API URL: {}", openIMProperties.getApiUrl());
    log.info("📱 [IM配置] WS URL: {}", openIMProperties.getWsUrl());
}
```

---

**最后更新**: 2025-10-09
**测试用户**: employeeId=1 (系统管理员)
**OpenIM版本**: v3.8.3-patch.9
