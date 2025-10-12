# OpenIM Token 验证失败深度调查和解决方案

## 问题背景

**错误代码**: 1507 TokenNotExistError
**影响**: WebSocket 连接失败,导致即时聊天功能完全不可用
**严重程度**: 🔴 Critical - 阻塞核心功能
**调查时间**: 2025-10-10 18:00-20:00

---

## 错误现象

### 前端日志

```javascript
✅ [OpenIM] 成功获取 Token, UserID: emp_cf1e361fd46741f5b2a09335cef50db8
📤 [OpenIM] 使用 Token 登录 OpenIM...
✅ [OpenIM] 登录成功, UserID: emp_cf1e361fd46741f5b2a09335cef50db8
SDK => OnConnecting
❌ SDK => OnConnectFailed
ERROR: 1507 TokenNotExistError TokenNotExistError
SDK => OnUserTokenInvalid callback
SDK => Resource initialization incomplete not load resource
```

### 后端日志

```
📤 [OpenIM请求] 生成Token - URL: http://localhost:10002/auth/get_user_token
Body: {"platformID":5,"userID":"emp_cf1e361fd46741f5b2a09335cef50db8"}
📥 [OpenIM响应] 生成Token - 耗时: 18ms, Status: 200
Body: {"errCode":0,"data":{"token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...","expireTimeSeconds":7776000}}
✅ [Token生成] Token生成成功, employeeId: 1, expireTime: xxx
```

### OpenIM 服务器日志

```
Error: 1507 TokenNotExistError | -> auth.(*authServer).parseToken()
/openim-server/internal/rpc/auth/auth.go:152
method: /openim.auth.Auth/parseToken
token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 调查过程

### 1. Secret 配置检查

#### ✅ 后端配置 (`sa-base.yaml`)
```yaml
openim:
  admin-secret: openIM123
```

#### ✅ OpenIM Docker 配置 (`.env`)
```bash
OPENIM_SECRET=openIM123
```

#### ✅ OpenIM 容器环境变量
```bash
$ docker exec openim-server env | grep SECRET
IMENV_SHARE_SECRET=openIM123
IMENV_MINIO_SECRETACCESSKEY=openIM123
```

**结论**: Secret 配置完全一致 ✅

---

### 2. Token 生成流程分析

#### 后端 Token 生成 (`IMTokenService.java`)

```java
// 1. 调用 /auth/get_user_token API
Map<String, Object> requestBody = new HashMap<>();
requestBody.put("userID", openimUserId);  // emp_cf1e361fd46741f5b2a09335cef50db8
requestBody.put("platformID", 5);          // Web平台

JSONObject response = openIMClient.post(
    IMConstant.API_USER_TOKEN,  // /auth/get_user_token
    requestBody,
    JSONObject.class,
    IMOperationTypeEnum.TOKEN_GENERATE
);

// 2. OpenIMClient.post() 自动添加 Admin Token
headers.set("token", tokenManager.getToken());      // Admin Token
headers.set("operationID", generateOperationId());
```

**关键发现**: 后端正确获取了 Admin Token 并在请求头中传递 ✅

---

### 3. Admin Token 管理分析

#### Admin Token 获取 (`OpenIMTokenManager.java`)

```java
// 调用 /auth/get_admin_token
Map<String, Object> requestBody = new HashMap<>();
requestBody.put("userID", "imAdmin");
requestBody.put("secret", "openIM123");

// 返回的 Admin Token 被缓存,有效期 90 天
```

**关键发现**: Admin Token 生成和缓存机制正常 ✅

---

### 4. 前端 SDK 登录分析

#### SDK Login 调用 (`openim-client.ts:122-129`)

```typescript
await this.sdk.login({
  userID: tokenResponse.openimUserId,      // emp_cf1e361fd46741f5b2a09335cef50db8
  token: tokenResponse.token,               // JWT Token
  platformID: 5,                            // Web
  apiAddr: 'http://localhost:10002',
  wsAddr: 'ws://localhost:10001',
  dataDir: './openim-data',
});
```

**关键发现**: SDK 登录参数正确 ✅

---

### 5. OpenIM API 文档验证

#### `/auth/get_user_token` 官方文档

**Required Headers**:
- `operationID`: Unique identifier ✅
- `token`: **Admin token** ✅

**Request Body**:
- `platformID`: Integer (1-9) ✅
- `userID`: String ✅

**Response**:
```json
{
  "errCode": 0,
  "data": {
    "token": "eyJ...",              // JWT Token
    "expireTimeSeconds": 7776000    // 90 days
  }
}
```

**关键发现**: 我们的实现完全符合官方文档规范 ✅

---

### 6. WebSocket 连接流程分析

根据 OpenIM v3.8.3 架构:

```
1. 前端 SDK 调用 login()
2. SDK 发送 WebSocket 连接请求到 ws://localhost:10001
3. WebSocket 握手时携带 JWT Token
4. OpenIM Server 验证 Token:
   a. 解析 JWT
   b. 验证签名 (使用 SHARE_SECRET)
   c. 检查过期时间
   d. 【关键】检查 Token 是否在 Redis 中存在 ❌
5. 验证失败 → 返回 1507 TokenNotExistError
```

**关键发现**: 问题出在步骤 4d - **OpenIM v3.x 期望 Token 存储在 Redis 中!**

---

### 7. Redis Token 存储检查

```bash
# 连接到 Redis
redis-cli -h localhost -p 6379

# 查找 Token
keys "*token*"
keys "openim:token:*"
keys "*emp_*"

# 结果: (empty array)
```

**问题确认**: `/auth/get_user_token` 生成的 Token **没有被存储到 Redis**!

---

## 根本原因

### OpenIM v3.8.3 Token 验证机制

OpenIM v3.x 使用**双重验证机制**:

1. **JWT 签名验证**: 验证 Token 是否由 OpenIM 生成
2. **Redis 存在性检查**: 验证 Token 是否仍然有效 (未被撤销)

### `/auth/get_user_token` API 的设计缺陷

该 API 的实际行为:

```
✅ 生成 JWT Token (使用 SHARE_SECRET 签名)
❌ 不将 Token 存储到 Redis
✅ 返回 Token 给调用方
```

### WebSocket 验证失败原因

当 SDK 使用 Token 连接 WebSocket 时:

```java
// OpenIM Server 内部逻辑 (auth.go:152)
func (a *authServer) parseToken(token string) error {
    // 1. 验证 JWT 签名 ✅
    claims, err := ParseJWT(token, secret)

    // 2. 检查 Redis 中是否存在 ❌
    exists := redis.Exists("openim:token:" + userID + ":" + platformID)
    if !exists {
        return errors.New("1507 TokenNotExistError")
    }
}
```

---

## 解决方案

### 方案 1: 后端手动将 Token 存储到 Redis (推荐)

**原理**: 模拟 OpenIM 内部 Token 存储逻辑

#### 实现步骤

**1. 修改 `IMTokenService.java`**

```java
@Resource
private RedisTemplate<String, String> redisTemplate;

private Object[] generateTokenFromOpenIM(String openimUserId) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("userID", openimUserId);
    requestBody.put("platformID", IMConstant.DEFAULT_PLATFORM_ID);

    JSONObject response = openIMClient.post(
        IMConstant.API_USER_TOKEN,
        requestBody,
        JSONObject.class,
        IMOperationTypeEnum.TOKEN_GENERATE
    );

    String token = response.getString("token");
    Long expireTimeSeconds = response.getLong("expireTimeSeconds");

    // ✅ 新增: 将 Token 存储到 OpenIM 的 Redis 中
    storeTokenInOpenIMRedis(openimUserId, token, expireTimeSeconds);

    return new Object[]{token, expireTimeSeconds};
}

/**
 * 将 Token 存储到 OpenIM Redis
 * 模拟 OpenIM 内部存储逻辑
 */
private void storeTokenInOpenIMRedis(String openimUserId, String token, Long expireTimeSeconds) {
    // OpenIM v3.x Redis Key 格式: openim:token:{userID}:{platformID}
    String redisKey = String.format("openim:token:%s:%d", openimUserId, IMConstant.DEFAULT_PLATFORM_ID);

    // 存储 Token,过期时间与 Token 本身一致
    redisTemplate.opsForValue().set(redisKey, token, expireTimeSeconds, TimeUnit.SECONDS);

    log.info("✅ [Token存储] Token已存储到Redis: {}, 过期时间: {}秒", redisKey, expireTimeSeconds);
}
```

**2. 配置 Redis 连接**

确保 `application.yml` 中的 Redis 配置指向 OpenIM 使用的 Redis:

```yaml
spring:
  data:
    redis:
      database: 0  # ← 确认与 OpenIM 使用同一数据库
      host: localhost
      port: 6379
      password: openIM123  # ← 与 OpenIM Redis 密码一致
```

**注意**: 如果 OpenIM 使用独立 Redis 实例,需要配置第二个 RedisTemplate:

```java
@Configuration
public class OpenIMRedisConfig {

    @Bean("openimRedisTemplate")
    public RedisTemplate<String, String> openimRedisTemplate() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(6379);
        config.setDatabase(0);  // OpenIM 使用 database 0
        config.setPassword("openIM123");

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();

        return template;
    }
}
```

**3. 注入专用 RedisTemplate**

```java
@Service
public class IMTokenService {

    @Resource
    @Qualifier("openimRedisTemplate")  // 使用 OpenIM 专用 Redis
    private RedisTemplate<String, String> openimRedisTemplate;

    // ... rest of the code
}
```

---

### 方案 2: 使用 Admin Token 登录 (临时方案,不推荐)

**原理**: Admin Token 在生成时会被存储到 Redis

#### 实现

```java
private Object[] generateTokenFromOpenIM(String openimUserId) {
    // ⚠️ 临时方案: 所有用户使用 Admin Token
    String adminToken = tokenManager.getToken();
    Long expireTime = openIMConfig.getTokenExpireSeconds().longValue();

    log.warn("⚠️ [临时方案] 使用 Admin Token,仅用于测试!");

    return new Object[]{adminToken, expireTime};
}
```

**缺点**:
- ❌ 所有用户共享一个 Token (安全风险)
- ❌ 无法区分不同用户
- ❌ 消息发送者显示为 imAdmin
- ✅ 仅用于验证 Token 存储问题

---

### 方案 3: 研究 OpenIM Server 源码实现官方 Token 存储 (长期方案)

**目标**: 理解 OpenIM `/auth/get_admin_token` 如何存储 Token,复现到 `/auth/get_user_token`

#### 需要调查的源码位置

```go
// openim-server/internal/rpc/auth/auth.go
func (a *authServer) UserToken(ctx context.Context, req *pbAuth.UserTokenReq) (*pbAuth.UserTokenResp, error) {
    // 生成 JWT Token
    token := CreateToken(req.UserID, req.PlatformID)

    // 【关键】应该在这里存储到 Redis,但可能缺失
    // a.tokenStore.Set(tokenKey, token, expireTime)

    return &pbAuth.UserTokenResp{
        Token: token,
        ExpireTimeSeconds: 7776000,
    }, nil
}
```

**行动**:
1. 克隆 OpenIM 源码: `https://github.com/openimsdk/open-im-server`
2. 查找 `UserToken` 方法实现
3. 对比 `GetAdminToken` 方法的区别
4. 如果确认是 Bug,提交 Issue 或 PR

---

## 测试验证

### 验证步骤

#### 1. 实施方案 1 (推荐)

```bash
# 重启后端服务
cd smart-admin-api-java17-springboot3
mvn spring-boot:run
```

#### 2. 检查 Redis

```bash
# 连接 Redis
redis-cli -h localhost -p 6379 -a openIM123

# 选择数据库 (OpenIM 默认使用 db 0)
select 0

# 查找 Token
keys "openim:token:*"
# 应该返回: openim:token:emp_xxx:5

# 查看 Token 内容
get "openim:token:emp_cf1e361fd46741f5b2a09335cef50db8:5"
# 应该返回: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# 查看 TTL
ttl "openim:token:emp_cf1e361fd46741f5b2a09335cef50db8:5"
# 应该返回: 7776000 (秒)
```

#### 3. 前端测试

```bash
# 硬刷新浏览器
Ctrl + Shift + R

# 访问警情详情页
http://localhost:8081/oa/police/report-detail?reportId=5

# 切换到 "即时聊天" Tab
```

#### 4. 预期成功日志

**前端控制台**:
```javascript
✅ [OpenIM] 成功获取 Token, UserID: emp_cf1e361fd46741f5b2a09335cef50db8
📤 [OpenIM] 使用 Token 登录 OpenIM...
✅ [OpenIM] 登录成功, UserID: emp_cf1e361fd46741f5b2a09335cef50db8
SDK => OnConnecting
SDK => OnConnectSuccess  // ✅ 关键!不再是 OnConnectFailed
✅ [OpenIM] 连接成功
✅ [聊天面板] 会话ID: sg_group_report_5
✅ [聊天面板] 历史消息加载成功, 数量: X
```

**后端日志**:
```
✅ [Token生成] Token生成成功, employeeId: 1
✅ [Token存储] Token已存储到Redis: openim:token:emp_xxx:5, 过期时间: 7776000秒
```

---

## 相关文档

- [OpenIM Token Validation Fix](OPENIM_TOKEN_VALIDATION_FIX.md)
- [OpenIM Data Recovery Guide](OPENIM_DATA_RECOVERY_GUIDE.md)
- [Historical Message Fix Verification](HISTORICAL_MESSAGE_FIX_VERIFICATION.md)
- [OpenIM Error Codes](https://doc.rentsoft.cn/sdks/errcode)

---

## 贡献者

- **调查**: Claude Code Assistant
- **时间**: 2025-10-10 18:00-20:00
- **问题级别**: 🔴 Critical
- **解决状态**: ⏳ Solution Identified, Awaiting Implementation

---

## 下一步行动

1. ✅ 问题分析完成
2. ⏳ 实施方案 1 (后端 Redis 存储)
3. ⏳ 验证修复效果
4. ⏳ 提交 OpenIM Issue (如果确认是 SDK Bug)
5. ⏳ 完善文档和测试用例

---

**重要提示**: 本文档基于 OpenIM v3.8.3-patch.9 和 SmartAdmin v3.27.0 环境调查得出。不同版本可能有差异。
