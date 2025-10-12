# OpenIM Token 验证失败修复指南

## 问题分析

### 症状
```
ERROR: 1507 TokenNotExistError TokenNotExistError
OnUserTokenInvalid callback
SDK => Resource initialization incomplete not load resource
```

### 错误流程

```
1. ✅ 后端调用 /auth/user_token 生成用户 Token (成功)
2. ✅ 前端 SDK 使用 Token 调用 login() (成功)
3. ❌ SDK 尝试连接 WebSocket (失败 - 1507 TokenNotExistError)
4. ❌ SDK 自动退出登录
5. ❌ 后续调用 getConversation() 失败 (资源未初始化)
```

### 根本原因

**Token 验证失败** - OpenIM 服务器认为生成的 Token 不存在或无效。

可能的原因:
1. **Secret 不一致** - 生成 Token 和验证 Token 使用的 secret 不同
2. **Token 签名算法问题** - JWT 签名验证失败
3. **OpenIM 服务器配置问题** - Token 存储或验证逻辑异常
4. **时间同步问题** - 服务器时间不同步导致 Token 过期判断错误

---

## 解决方案

### 方案 1: 检查并统一 Secret 配置 (推荐)

OpenIM v3.x 要求 **管理员 secret** 和 **Token 签名 secret** 保持一致。

#### 步骤 1: 检查后端配置

**文件**: `sa-base/src/main/resources/dev/sa-base.yaml`

```yaml
openim:
  enabled: true
  api-url: http://localhost:10002
  ws-url: ws://localhost:10001
  admin-user-id: imAdmin         # ← 管理员用户ID
  admin-secret: openIM123        # ← 管理员密钥 (重要!)
  platform-id: 10
  token-expire-seconds: 86400
```

**关键配置**: `admin-secret: openIM123`

#### 步骤 2: 检查 OpenIM 服务器配置

**打开 WSL 并检查配置**:

```bash
wsl

# 方式 A: 检查 .env 文件
cd /home/lihongda/openim-docker
cat .env | grep -i secret

# 方式 B: 检查 config.yaml
cat config/config.yaml | grep -A 5 "secret:"

# 预期输出应该包含:
SECRET: "openIM123"          # ← 必须与后端配置一致!
```

#### 步骤 3: 修改配置 (如果不一致)

**编辑 OpenIM 配置**:

```bash
# 编辑 .env 文件
vim .env

# 找到并修改
SECRET=openIM123              # 改为与后端一致

# 或编辑 config.yaml
vim config/config.yaml

# 找到 secret 部分并修改
secret:
  secret: "openIM123"         # 改为与后端一致
```

#### 步骤 4: 重启 OpenIM 服务

```bash
docker-compose down
docker-compose up -d

# 查看日志确认启动成功
docker logs -f openim-server | grep -i "secret\|token"
```

#### 步骤 5: 清除后端 Token 缓存并验证

```bash
# 清除 Redis 中的 Token 缓存
redis-cli -h 101.36.125.254 -p 6379 -a ruoyi123
> select 5
> keys im:token:*
> del im:token:1  # 删除管理员的 Token 缓存
> quit
```

**或者重启后端服务**:
```bash
cd smart-admin-api-java17-springboot3
# 停止应用 (Ctrl+C)
# 重新启动
mvn spring-boot:run
```

#### 步骤 6: 前端测试

1. 刷新浏览器 (`Ctrl + Shift + R`)
2. 访问警情详情页并切换到 "即时聊天" Tab
3. 检查浏览器控制台

**预期成功日志**:
```
✅ [OpenIM] 登录成功, UserID: emp_cf1e361fd46741f5b2a09335cef50db8
SDK => OnConnecting
SDK => OnConnectSuccess  ← 应该出现这个,不再是 OnUserTokenInvalid
✅ [OpenIM] 获取会话成功
```

---

### 方案 2: 使用管理员 Token 直接登录 (临时方案)

如果方案 1 不可行,可以尝试让用户直接使用管理员 Token 登录。

**警告**: 这种方式不安全,仅用于测试!

#### 修改后端 Token 生成逻辑

**文件**: `IMTokenService.java`

```java
// 修改 generateTokenFromOpenIM() 方法

private Object[] generateTokenFromOpenIM(String openimUserId) {
    // 方式 A: 使用管理员 Token (临时测试)
    String adminToken = tokenManager.getToken();  // 获取管理员Token
    Long expireTime = openIMConfig.getTokenExpireSeconds().longValue();

    log.warn("⚠️ [临时方案] 使用管理员Token作为用户Token (仅用于测试!)");

    return new Object[]{adminToken, expireTime};

    // 原方式 B: 调用 API 生成用户专属 Token (正常方式)
    // Map<String, Object> requestBody = new HashMap<>();
    // requestBody.put("userID", openimUserId);
    // requestBody.put("platformID", IMConstant.DEFAULT_PLATFORM_ID);
    // ...
}
```

**注意**: 这种方式会让所有用户共享管理员 Token,存在安全风险,只能用于快速验证问题!

---

### 方案 3: 检查 OpenIM 服务器日志

#### 查看 OpenIM 服务器日志

```bash
wsl

# 查看 OpenIM 服务器日志
docker logs -f openim-server --tail=100

# 查找 Token 相关错误
docker logs openim-server 2>&1 | grep -i "token\|1507\|auth"
```

**关键日志**:
```
# 正常情况应该看到:
INFO: Token验证成功
INFO: User emp_xxx connected

# 错误情况会看到:
ERROR: Token验证失败: 1507 TokenNotExistError
ERROR: JWT signature verification failed
```

#### 常见错误及解决方案

**错误 1: JWT signature does not match**
```
原因: Secret 不一致
解决: 确保 OpenIM 配置的 SECRET 与后端 admin-secret 一致
```

**错误 2: Token expired**
```
原因: 时间不同步或Token过期时间设置过短
解决:
1. 检查服务器时间: date
2. 同步时间: ntpdate pool.ntp.org
3. 增加 Token 有效期
```

**错误 3: Token not found in storage**
```
原因: OpenIM 没有正确存储 Token
解决: 检查 OpenIM 的 Redis/数据库配置
```

---

### 方案 4: 降级到 HTTP API 模式 (最后手段)

如果 WebSocket Token 验证始终失败,可以暂时禁用 WebSocket,只使用 HTTP API。

#### 修改前端 OpenIM 客户端

**文件**: `openim-client.ts`

```typescript
async login(userId: string, token: string): Promise<void> {
  try {
    console.log('📤 [OpenIM] 使用 Token 登录 OpenIM...');

    // ❌ 禁用 WebSocket 登录
    // await this.sdk.login({
    //   userID: userId,
    //   token: token,
    //   platformID: this.platformID,
    //   apiAddr: this.apiUrl,
    //   wsAddr: this.wsUrl,
    //   dataDir: './openim-data',
    // });

    // ✅ 使用 HTTP-only 模式 (不连接 WebSocket)
    console.warn('⚠️ [OpenIM] 降级到 HTTP-only 模式 (WebSocket 已禁用)');

    this.currentUserId = userId;
    this.currentToken = token;
    this.isLoggedIn = true;

    console.log('✅ [OpenIM] HTTP 登录成功, UserID:', userId);

  } catch (error) {
    console.error('❌ [OpenIM] 登录失败:', error);
    throw error;
  }
}
```

**缺点**:
- 无法接收实时消息推送
- 需要轮询获取新消息
- 功能受限

---

## 快速诊断脚本

创建一个诊断脚本来检查配置:

```bash
#!/bin/bash
# openim-diagnosis.sh

echo "=== OpenIM Token 诊断脚本 ==="
echo ""

# 1. 检查后端配置
echo "📋 检查后端配置..."
BACKEND_SECRET=$(grep -A 5 "openim:" smart-admin-api-java17-springboot3/sa-base/src/main/resources/dev/sa-base.yaml | grep "admin-secret:" | awk '{print $2}')
echo "后端 admin-secret: $BACKEND_SECRET"

# 2. 检查 OpenIM 配置 (WSL)
echo ""
echo "📋 检查 OpenIM 配置..."
wsl -e sh -c "cd /home/lihongda/openim-docker && cat .env | grep SECRET"

# 3. 检查 OpenIM 服务状态
echo ""
echo "📋 检查 OpenIM 服务状态..."
wsl docker ps | grep openim-server

# 4. 检查最新日志
echo ""
echo "📋 检查 OpenIM 最新日志..."
wsl docker logs openim-server --tail=20 2>&1 | grep -i "error\|token"

echo ""
echo "=== 诊断完成 ==="
```

**使用方法**:
```bash
chmod +x openim-diagnosis.sh
./openim-diagnosis.sh
```

---

## 验证修复

### 步骤 1: 检查后端日志

**启动后端后,查看日志**:
```
✅ [Token生成] Token生成成功, employeeId: 1, expireTime: xxx
```

### 步骤 2: 检查前端控制台

**刷新浏览器后,应该看到**:
```
✅ [OpenIM] 登录成功, UserID: emp_cf1e361fd46741f5b2a09335cef50db8
SDK => OnConnecting
SDK => OnConnectSuccess                    # ← 关键!不再是 OnUserTokenInvalid
✅ [聊天面板] 会话ID: sg_group_report_5
✅ [聊天面板] 历史消息加载成功, 数量: X
```

### 步骤 3: 功能测试

- [ ] 可以访问聊天面板
- [ ] 可以查看历史消息
- [ ] 可以发送文本消息
- [ ] 可以发送图片消息
- [ ] 可以实时接收消息

---

## 常见问题

### Q1: 修改配置后仍然失败

**原因**: Token 缓存未清除

**解决**:
```bash
# 清除后端 Token 缓存
redis-cli -h 101.36.125.254 -p 6379 -a ruoyi123
> select 5
> flushdb  # 清除当前数据库所有缓存
> quit

# 重启后端服务
mvn spring-boot:run

# 清除浏览器缓存
Ctrl + Shift + R (硬刷新)
```

### Q2: 如何确认 Secret 已生效?

**方法 1**: 查看 OpenIM 启动日志
```bash
docker logs openim-server | grep -i "secret"
# 应该显示加载的 secret 配置
```

**方法 2**: 调用管理员 Token API 测试
```bash
curl -X POST http://localhost:10002/auth/get_admin_token \
  -H "Content-Type: application/json" \
  -d '{
    "userID": "imAdmin",
    "secret": "openIM123"
  }'

# 如果返回 Token,说明 secret 正确
```

### Q3: OpenIM 配置文件在哪里?

**常见位置**:
```
/home/lihongda/openim-docker/.env
/home/lihongda/openim-docker/config/config.yaml
/etc/openim/config.yaml
```

**查找方法**:
```bash
wsl -e sh -c "find ~ -name 'config.yaml' -o -name '.env' | grep openim"
```

---

## 总结

### 修复核心

**一句话**: 确保 OpenIM 服务器配置的 `SECRET` 与后端配置的 `admin-secret` **完全一致**。

### 修复优先级

1. **首选**: 方案 1 - 统一 Secret 配置 (治本)
2. **备选**: 方案 3 - 检查日志定位具体原因
3. **临时**: 方案 2 - 使用管理员 Token (仅测试)
4. **最后**: 方案 4 - 降级到 HTTP-only 模式

### 关键配置文件

**后端配置**:
```
smart-admin-api-java17-springboot3/sa-base/src/main/resources/dev/sa-base.yaml
openim.admin-secret: openIM123
```

**OpenIM 配置**:
```
/home/lihongda/openim-docker/.env
SECRET=openIM123

或

/home/lihongda/openim-docker/config/config.yaml
secret:
  secret: "openIM123"
```

---

**修复负责人**: Claude Code Assistant
**修复时间**: 2025-10-10
**问题级别**: 🔴 Critical (核心功能无法使用)
**预计解决时间**: < 30分钟 (配置修改 + 重启)

---

## 下一步

修复完成后,请继续测试:
1. ✅ 聊天面板功能
2. ✅ 历史消息加载
3. ✅ 文本消息发送
4. ✅ 图片消息发送 (MinIO 问题需要单独修复)
5. ✅ 实时消息同步

如有任何问题,请提供:
1. 后端完整配置 (脱敏后)
2. OpenIM 服务器配置 (脱敏后)
3. OpenIM 启动日志
4. 前端控制台完整日志
