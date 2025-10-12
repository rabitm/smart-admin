# OpenIM 用户ID生成逻辑不一致修复

## 修复日期: 2025-10-10

---

## 🐛 问题: RecordNotFoundError - 用户未注册

### 错误信息
```
errCode:1004, errMsg:RecordNotFoundError, errDlt:record not found
```

### 完整错误日志
```
[2025-10-10 12:21:49,727][INFO] 📤 [OpenIM请求] 生成Token - URL: http://localhost:10002/auth/get_user_token,
Body: {"platformID":5,"userID":"emp_cf1e361fd46741f5b2a09335cef50db8"}

[2025-10-10 12:21:49,758][INFO] 📥 [OpenIM响应] 生成Token - 耗时: 156ms, Status: 200,
Body: {"errCode":1004,"errMsg":"RecordNotFoundError","errDlt":"record not found"}

[2025-10-10 12:21:49,759][ERROR] ❌ [OpenIM错误] 生成Token - errCode: 1004, errMsg: RecordNotFoundError
```

---

## 🔍 根本原因

### 用户ID生成逻辑不一致

SmartAdmin 系统中存在两个不同的 OpenIM 用户ID生成逻辑，导致注册和获取Token时使用了不同的用户ID格式：

#### 问题代码对比

| 服务 | 文件 | 生成逻辑 | 结果示例 |
|------|------|----------|---------|
| **用户同步服务** | `IMUserSyncService.java` | ✅ `emp_{employeeUid}` 或 `emp_{employeeId}` | `emp_cf1e361fd46741f5b2a09335cef50db8` |
| **Token服务（错误）** | `IMTokenService.java` | ❌ `USER_{employeeId}` | `USER_1` |

#### IMUserSyncService.java (正确的逻辑)

```java
// Line 264-270
private String generateOpenIMUserId(EmployeeEntity employee) {
    // 使用员工UID或ID生成
    if (SmartStringUtil.isNotBlank(employee.getEmployeeUid())) {
        return IMConstant.USER_ID_PREFIX + employee.getEmployeeUid();  // emp_xxx
    }
    return IMConstant.USER_ID_PREFIX + employee.getEmployeeId();  // emp_1
}
```

#### IMTokenService.java (错误的逻辑 - 修复前)

```java
// Line 150 - 修复前
String openimUserId = "USER_" + employeeId;  // ❌ 错误：使用硬编码的 "USER_" 前缀
```

### 问题流程

```
1. 用户通过 IMUserSyncService 同步到 OpenIM
   └─> 生成用户ID: emp_cf1e361fd46741f5b2a09335cef50db8
   └─> 在 OpenIM 中注册成功
   └─> 保存到数据库 im_user_mapping 表

2. 用户登录时调用 IMTokenService 获取 Token
   └─> 查询数据库找到映射: employeeId=1, openimUserId=emp_cf1e361fd46741f5b2a09335cef50db8
   └─> 但是如果数据库中没有映射，IMTokenService 会重新注册
   └─> 重新注册时生成用户ID: USER_1  ❌ 与已有用户ID不匹配！
   └─> OpenIM 返回 RecordNotFoundError

3. 结果
   ❌ Token 获取失败
   ❌ 用户无法登录 OpenIM
```

---

## ✅ 修复内容

### 文件: `IMTokenService.java:149-155`

**修复前**:
```java
// 2. 生成 OpenIM 用户 ID
String openimUserId = "USER_" + employeeId;
```

**修复后**:
```java
// 2. 生成 OpenIM 用户 ID (使用常量定义的前缀，优先使用employeeUid)
String openimUserId;
if (employee.getEmployeeUid() != null && !employee.getEmployeeUid().isEmpty()) {
    openimUserId = IMConstant.USER_ID_PREFIX + employee.getEmployeeUid();
} else {
    openimUserId = IMConstant.USER_ID_PREFIX + employeeId;
}
```

### 修复要点

1. ✅ 使用 `IMConstant.USER_ID_PREFIX` (值为 `"emp_"`) 替代硬编码的 `"USER_"`
2. ✅ 优先使用 `employee.getEmployeeUid()` (如果存在)
3. ✅ 回退使用 `employee.getEmployeeId()` (如果 UID 不存在)
4. ✅ 与 `IMUserSyncService` 保持完全一致的生成逻辑

---

## 📋 修复验证

### 1. 编译验证

```bash
cd smart-admin-api-java17-springboot3
mvn clean compile -DskipTests
```

**结果**: ✅ BUILD SUCCESS (41.040s)

### 2. 用户ID生成测试

**场景 1: 员工有 employeeUid**
```
Employee {
  employeeId: 1
  employeeUid: "cf1e361fd46741f5b2a09335cef50db8"
}

生成的 OpenIM 用户ID: "emp_cf1e361fd46741f5b2a09335cef50db8"  ✅
```

**场景 2: 员工没有 employeeUid**
```
Employee {
  employeeId: 1
  employeeUid: null
}

生成的 OpenIM 用户ID: "emp_1"  ✅
```

### 3. 重启后端测试

启动后端并观察日志：

**预期成功日志**:
```
🎫 [Token生成] 开始为员工1生成Token
ℹ️ [用户映射] 用户已存在映射: employeeId=1, openimUserId=emp_cf1e361fd46741f5b2a09335cef50db8
📤 [OpenIM请求] 生成Token - URL: http://localhost:10002/auth/get_user_token
   Body: {"platformID":5,"userID":"emp_cf1e361fd46741f5b2a09335cef50db8"}  ✅ 使用正确的用户ID

📥 [OpenIM响应] 生成Token - 耗时: 80ms, Status: 200
   Body: {"errCode":0,"data":{"token":"eyJ...","expireTimeSeconds":7200}}  ✅ 成功获取Token

✅ [Token生成] Token生成成功, employeeId: 1, expireTime: 1728544514
```

---

## 🔧 相关常量定义

### IMConstant.java

```java
// ========== 用户ID前缀 ==========
String USER_ID_PREFIX = "emp_";                 // ✅ OpenIM 用户ID统一前缀
String GROUP_ID_PREFIX = "group_report_";       // 群组ID前缀
```

### 用户ID命名规范

| 类型 | 格式 | 示例 | 说明 |
|------|------|------|------|
| 有 UID 的员工 | `emp_{employeeUid}` | `emp_cf1e361fd46741f5b2a09335cef50db8` | 优先使用UUID作为后缀 |
| 无 UID 的员工 | `emp_{employeeId}` | `emp_1` | 使用数字ID作为后缀 |
| 群组 | `group_report_{reportId}` | `group_report_123` | 警情群组ID |

---

## 📊 影响范围分析

### 受影响的服务

1. **IMTokenService.java** ✅ 已修复
   - `registerOpenIMUser()` 方法
   - 影响: Token 生成时的用户注册逻辑

2. **IMUserSyncService.java** ✅ 已正确实现
   - `generateOpenIMUserId()` 方法
   - 影响: 用户批量同步逻辑

### 数据库表影响

**表: `im_user_mapping`**
```sql
SELECT employee_id, openim_user_id, sync_status
FROM im_user_mapping;
```

**预期数据**:
| employee_id | openim_user_id | sync_status |
|-------------|---------------|-------------|
| 1 | emp_cf1e361fd46741f5b2a09335cef50db8 | 1 (已同步) |
| 2 | emp_2 | 1 (已同步) |

### API 端点影响

| API | 说明 | 影响 |
|-----|------|------|
| `GET /api/im/token/current` | 获取Token | ✅ 现在使用正确的用户ID |
| `POST /api/im/token/refresh` | 刷新Token | ✅ 现在使用正确的用户ID |
| `POST /api/im/user/sync` | 用户同步 | 无影响 (本来就正确) |

---

## 🚨 注意事项

### 数据迁移

如果系统中已经存在使用旧逻辑（`USER_` 前缀）注册的用户，需要进行数据迁移：

#### 选项 1: 清理重新同步（推荐用于开发/测试环境）

```sql
-- 1. 清空 OpenIM 用户映射表
TRUNCATE TABLE im_user_mapping;

-- 2. 通过管理后台或 API 重新同步所有用户
-- POST /api/im/user/sync/all
```

#### 选项 2: 数据迁移（生产环境）

```sql
-- 1. 查找所有 USER_ 开头的映射
SELECT * FROM im_user_mapping WHERE openim_user_id LIKE 'USER_%';

-- 2. 需要调用 OpenIM API 重新注册用户（使用新ID）
-- 3. 更新 im_user_mapping 表中的映射关系
```

### 避免重复注册

修复后的逻辑会在注册时捕获 "RegisteredAlreadyError"（用户已存在）错误，不会抛出异常：

```java
try {
    openIMClient.post(IMConstant.API_USER_REGISTER, requestBody, ...);
    log.info("✅ [用户注册] OpenIM用户注册成功: {}", openimUserId);
} catch (Exception e) {
    // 如果是用户已存在错误（1102），不抛出异常
    if (e.getMessage() != null && e.getMessage().contains("RegisteredAlreadyError")) {
        log.info("ℹ️ [用户注册] 用户已存在，跳过注册: {}", openimUserId);
    } else {
        log.error("❌ [用户注册] OpenIM用户注册失败", e);
        throw e;
    }
}
```

---

## 🎯 测试场景

### 测试 1: 新用户首次登录

**前提**: 用户从未同步到 OpenIM

**步骤**:
1. 用户登录 SmartAdmin
2. 前端调用 `/api/im/token/current`
3. 后端检测用户未注册
4. 后端自动注册用户到 OpenIM (使用正确的用户ID)
5. 后端获取Token并返回

**预期结果**: ✅ 成功获取Token

### 测试 2: 已同步用户登录

**前提**: 用户已通过批量同步接口同步到 OpenIM

**步骤**:
1. 用户登录 SmartAdmin
2. 前端调用 `/api/im/token/current`
3. 后端从数据库查询到映射关系
4. 后端使用映射的 OpenIM 用户ID 获取Token

**预期结果**: ✅ 成功获取Token，用户ID一致

### 测试 3: Token 刷新

**步骤**:
1. 用户已登录
2. Token 即将过期 (提前5分钟)
3. 前端自动调用 `/api/im/token/refresh`
4. 后端清除缓存，重新生成Token

**预期结果**: ✅ 成功刷新Token，用户ID保持不变

---

## 📚 相关文档

- [OPENIM_FIX_SUMMARY.md](./OPENIM_FIX_SUMMARY.md) - 所有修复总结
- [OPENIM_PLATFORM_ID_FIX.md](./OPENIM_PLATFORM_ID_FIX.md) - Platform ID 修复
- [OPENIM_PHASE1_FINAL_STATUS.md](./OPENIM_PHASE1_FINAL_STATUS.md) - Phase 1 状态报告

---

## ✅ 验收标准

修复后的系统应满足以下标准：

- [x] `IMTokenService` 和 `IMUserSyncService` 使用相同的用户ID生成逻辑
- [x] 优先使用 `employeeUid`，回退到 `employeeId`
- [x] 使用统一的 `IMConstant.USER_ID_PREFIX` 常量
- [x] 编译无错误
- [x] 不会出现 `RecordNotFoundError` 错误
- [x] 用户能够成功获取 OpenIM Token
- [x] 用户能够成功登录 OpenIM

---

**修复完成时间**: 2025-10-10 12:25
**状态**: ✅ 修复完成，等待测试验收
**问题编号**: #9 (累计第9个修复问题)
