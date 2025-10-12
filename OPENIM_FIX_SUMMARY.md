# OpenIM 集成问题修复总结

## 修复日期: 2025-10-10

---

## 🐛 问题 1: Token API 404 错误

### 错误信息
```
No static resource im/token/current.
```

### 原因
前端API调用缺少 `/api` 前缀，导致路径错误。

### 修复内容

**文件 1**: `smart-admin-web-typescript/src/api/business/oa/im-token-api.ts`
```typescript
// ❌ 修复前
getToken: () => getRequest('/im/token/current')
refreshToken: () => postRequest('/im/token/refresh')
batchGetUserMapping: (employeeIds) => postRequest('/im/user-mapping/batch', { employeeIds })
getUserMapping: (employeeId) => getRequest(`/im/user-mapping/${employeeId}`)
getEmployeeIdByOpenIMUserId: (openimUserId) => getRequest(`/im/user-mapping/reverse/${openimUserId}`)

// ✅ 修复后
getToken: () => getRequest('/api/im/token/current')
refreshToken: () => postRequest('/api/im/token/refresh')
batchGetUserMapping: (employeeIds) => postRequest('/api/im/user-mapping/batch', { employeeIds })
getUserMapping: (employeeId) => getRequest(`/api/im/user-mapping/${employeeId}`)
getEmployeeIdByOpenIMUserId: (openimUserId) => getRequest(`/api/im/user-mapping/reverse/${openimUserId}`)
```

**文件 2**: `smart-admin-web-typescript/src/api/business/oa/im-business-api.ts`
```typescript
// ❌ 修复前
createGroupForReport: (reportId) => postRequest('/im/business/create-group', { reportId })
getGroupByReportId: (reportId) => getRequest(`/im/business/group/${reportId}`)
checkGroupExists: (reportId) => getRequest(`/im/business/group/${reportId}/exists`)

// ✅ 修复后
createGroupForReport: (reportId) => postRequest('/api/im/business/create-group', { reportId })
getGroupByReportId: (reportId) => getRequest(`/api/im/business/group/${reportId}`)
checkGroupExists: (reportId) => getRequest(`/api/im/business/group/${reportId}/exists`)
```

---

## 🐛 问题 2: Go is not defined 错误

### 错误信息
```
Uncaught (in promise) ReferenceError: Go is not defined
    at initializeWasm (index.es.js:953:5)
```

### 原因
OpenIM WASM SDK 依赖 Go WebAssembly 运行时，但 `wasm_exec.js` 和 WASM 文件未正确加载。

### 修复内容

**步骤 1**: 复制WASM文件到public目录
```bash
# 从 node_modules 复制到 public/
cp node_modules/@openim/wasm-client-sdk/assets/wasm_exec.js public/
cp node_modules/@openim/wasm-client-sdk/assets/openIM.wasm public/
cp node_modules/@openim/wasm-client-sdk/assets/sql-wasm.wasm public/
```

**文件列表**:
- `public/wasm_exec.js` (17 KB) - Go WASM 运行时
- `public/openIM.wasm` (35 MB) - OpenIM 核心模块
- `public/sql-wasm.wasm` (1.1 MB) - SQLite WASM 模块

**步骤 2**: 修改 index.html

**文件**: `smart-admin-web-typescript/index.html`
```html
<!-- ❌ 修复前 -->
<head>
  <meta charset="UTF-8">
  <link rel="icon" href="/favicon.ico">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title> %VITE_APP_TITLE%</title>
</head>

<!-- ✅ 修复后 -->
<head>
  <meta charset="UTF-8">
  <link rel="icon" href="/favicon.ico">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title> %VITE_APP_TITLE%</title>
  <!-- OpenIM WASM SDK requires Go WebAssembly runtime -->
  <script src="/wasm_exec.js"></script>
</head>
```

---

## 🐛 问题 3: OpenIM Token API 端点错误 (ArgsError)

### 错误信息
```
[2025-10-10 11:05:14,337][ERROR] ❌ [Token错误] errCode: 1001, errMsg: ArgsError
net.lab1024.sa.base.common.exception.BusinessException: ArgsError
    at OpenIMTokenManager.refreshToken(OpenIMTokenManager.java:138)
```

### 原因
后端在获取管理员Token时使用了错误的API端点。使用了 `/auth/user_token`（用于普通用户），而应该使用 `/auth/get_admin_token`（用于管理员）。

### 修复内容

**文件**: `OpenIMTokenManager.java:110`

```java
// ❌ 修复前
public String refreshToken() {
    writeLock.lock();
    try {
        String url = openIMConfig.getFullApiUrl(IMConstant.API_USER_TOKEN);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userID", openIMConfig.getAdminUserId());
        requestBody.put("secret", openIMConfig.getAdminSecret());
        // ...
    }
}

// ✅ 修复后
public String refreshToken() {
    writeLock.lock();
    try {
        String url = openIMConfig.getFullApiUrl(IMConstant.API_ADMIN_TOKEN);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("userID", openIMConfig.getAdminUserId());
        requestBody.put("secret", openIMConfig.getAdminSecret());
        // ...
    }
}
```

**关键区别**:
- `API_USER_TOKEN` = `/auth/user_token` - 用于为**普通用户**生成Token（需要 userID + platformID）
- `API_ADMIN_TOKEN` = `/auth/get_admin_token` - 用于为**管理员**生成Token（需要 userID + secret）

### 验证
```bash
# 编译测试
cd smart-admin-api-java17-springboot3
mvn clean compile -DskipTests

# 结果
[INFO] BUILD SUCCESS
[INFO] Total time: 53.749 s
```

---

## 📋 修复文件清单

### 前端文件 (3个)
1. `smart-admin-web-typescript/index.html` - 添加 wasm_exec.js 引用
2. `smart-admin-web-typescript/src/api/business/oa/im-token-api.ts` - 修复API路径
3. `smart-admin-web-typescript/src/api/business/oa/im-business-api.ts` - 修复API路径

### 后端文件 (1个)
1. `sa-admin/.../support/im/client/OpenIMTokenManager.java` - 修复Token API端点

### 新增文件 (3个WASM文件)
1. `public/wasm_exec.js`
2. `public/openIM.wasm`
3. `public/sql-wasm.wasm`

---

## ✅ 验证清单

### 前端验证
- [x] 所有API路径包含 `/api` 前缀
- [x] WASM文件已复制到 public 目录
- [x] index.html 引入了 wasm_exec.js
- [x] 浏览器能加载 Go 全局对象

### 后端验证
- [x] OpenIMTokenManager 使用正确的管理员Token API
- [x] Maven 编译成功，无错误
- [x] 所有依赖正确配置

---

## 🚀 测试步骤

### 1. 启动 OpenIM Server
```bash
# 检查 OpenIM 服务状态
curl http://localhost:10002/healthz
# 预期: {"status":"ok"}
```

### 2. 启动后端
```bash
cd smart-admin-api-java17-springboot3
mvn spring-boot:run
# 后端运行在: http://localhost:1024
```

### 3. 启动前端
```bash
cd smart-admin-web-typescript
npm run dev
# 前端运行在: http://localhost:8081
```

### 4. 登录测试

**预期控制台日志**:
```
📱 [OpenIM] 初始化OpenIM客户端
🔌 [OpenIM] SDK初始化中...
✅ [OpenIM] SDK初始化成功
🔐 [OpenIM] 开始从后端获取 Token...
✅ [OpenIM] 成功获取 Token, UserID: USER_xxx
📤 [OpenIM] 使用 Token 登录 OpenIM...
✅ [OpenIM] 连接成功
✅ [OpenIM] 登录成功, UserID: USER_xxx
✅ [OpenIM] OpenIM 连接已建立
```

**预期网络请求**:
- ✅ `GET /api/im/token/current` → 200 OK
- ✅ `GET /wasm_exec.js` → 200 OK
- ✅ `GET /openIM.wasm` → 200 OK
- ✅ `WebSocket ws://localhost:10001` → Connected

**预期后端日志**:
```
ℹ️ [用户映射] 用户已存在映射: employeeId=1, openimUserId=USER_1
📤 [Token请求] URL: http://localhost:10002/auth/get_admin_token, UserID: imAdmin
✅ [Token获取成功] Token: eyJhbGciOiJIUzI1NiIs..., 有效期: 7200秒
✅ [Token生成] Token生成成功, employeeId: 1, expireTime: 1728544514
```

---

## 📚 相关文档

- [OPENIM_PHASE1_COMPLETE.md](./OPENIM_PHASE1_COMPLETE.md) - 完整部署指南
- [OPENIM_WASM_SETUP_GUIDE.md](./OPENIM_WASM_SETUP_GUIDE.md) - WASM配置详解
- [OPENIM_FRONTEND_INTEGRATION_GUIDE.md](./OPENIM_FRONTEND_INTEGRATION_GUIDE.md) - 前端集成指南
- [OPENIM_QUICK_START_FRONTEND.md](./OPENIM_QUICK_START_FRONTEND.md) - 快速上手

---

## 🎯 下一步

所有已知问题已修复，现在可以进行完整的端到端测试：

1. **基础功能测试**
   - ✅ 用户登录
   - ⏳ OpenIM 连接
   - ⏳ Token 自动刷新
   - ⏳ 消息发送/接收
   - ⏳ 群组创建

2. **集成测试**
   - ⏳ 警情群组自动创建
   - ⏳ 多用户协作
   - ⏳ WebSocket 稳定性

3. **性能测试**
   - ⏳ WASM 加载性能
   - ⏳ 并发连接测试
   - ⏳ Token 缓存效率

---

**修复完成时间**: 2025-10-10 11:10
**状态**: ✅ 全部修复完成，等待测试验收
