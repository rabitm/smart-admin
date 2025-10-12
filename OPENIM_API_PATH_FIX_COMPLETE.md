# OpenIM API 路径修复完成报告

## 修复时间
**2025-10-10**

## 问题描述

### 用户报告的错误
```
接口http://127.0.0.1:1024/im/message/history?reportId=5&count=50返回
{
  "code": 10001,
  "level": "system",
  "msg": "org.springframework.web.servlet.resource.NoResourceFoundException: No static resource im/message/history.",
  "ok": false,
  "data": null
}
```

### 根本原因
`im-api.ts` 文件中的所有 API 端点路径缺失 `/api` 前缀，导致 Spring Boot 将请求识别为静态资源请求而非 REST API 请求。

## 修复内容

### 修复的文件
**文件路径**: `smart-admin-web-typescript/src/api/business/oa/im-api.ts`

### 修复的 API 端点（共计 18 个）

#### 1. 配置相关 API (4个)
- ✅ `getConfig`: `/im/config/info` → `/api/im/config/info`
- ✅ `checkHealth`: `/im/config/health` → `/api/im/config/health`
- ✅ `getTokenInfo`: `/im/config/token-info` → `/api/im/config/token-info`
- ✅ `refreshToken`: `/im/config/token-refresh` → `/api/im/config/token-refresh`

#### 2. 用户相关 API (3个)
- ✅ `getOpenIMUserId`: `/im/user/openim-id/${employeeId}` → `/api/im/user/openim-id/${employeeId}`
- ✅ `syncUser`: `/im/user/sync/${employeeId}` → `/api/im/user/sync/${employeeId}`
- ✅ `batchSyncUsers`: `/im/user/sync/batch` → `/api/im/user/sync/batch`

#### 3. 群组相关 API (5个)
- ✅ `createGroupForReport`: `/im/group/create/${reportId}` → `/api/im/group/create/${reportId}`
- ✅ `inviteMembers`: `/im/group/${reportId}/invite` → `/api/im/group/${reportId}/invite`
- ✅ `kickMembers`: `/im/group/${reportId}/kick` → `/api/im/group/${reportId}/kick`
- ✅ `disbandGroup`: `/im/group/${reportId}/disband` → `/api/im/group/${reportId}/disband`
- ✅ `getGroupInfo`: `/im/group/${reportId}/info` → `/api/im/group/${reportId}/info`

#### 4. 消息相关 API (3个)
- ✅ `sendMessage`: `/im/message/send` → `/api/im/message/send`
- ✅ `getMessageHistory`: `/im/message/history` → `/api/im/message/history` **(用户报告的错误端点)**
- ✅ `getNewMessages`: `/im/message/new` → `/api/im/message/new`

#### 5. 订阅相关 API (3个)
- ✅ `subscribeReport`: `/im/subscription/subscribe/${reportId}` → `/api/im/subscription/subscribe/${reportId}`
- ✅ `unsubscribeReport`: `/im/subscription/unsubscribe/${reportId}` → `/api/im/subscription/unsubscribe/${reportId}`
- ✅ `getSubscriberCount`: `/im/subscription/subscriber-count/${reportId}` → `/api/im/subscription/subscriber-count/${reportId}`

## 技术说明

### SmartAdmin 路由规范
SmartAdmin 项目的 Spring Boot 后端配置要求所有 REST API 请求必须以 `/api` 前缀开头：

```yaml
# application.yaml
spring:
  mvc:
    servlet:
      path: /api  # 所有 REST API 统一前缀
```

### Spring Boot 静态资源处理
当请求路径不以 `/api` 开头时，Spring Boot 会将其视为静态资源请求，并尝试从以下位置查找：
- `/static`
- `/public`
- `/resources`
- `/META-INF/resources`

如果找不到对应的静态资源，则返回 `NoResourceFoundException` 错误。

## 验证步骤

### 1. 前端重新编译
```bash
cd smart-admin-web-typescript
npm run dev
```

### 2. 后端确认端点映射
检查后端日志确认以下端点已正确映射：
```
GET  /api/im/message/history
POST /api/im/message/send
GET  /api/im/message/new
...等18个端点
```

### 3. 功能测试
- 测试消息历史获取功能
- 测试消息发送功能
- 测试群组创建和管理功能
- 测试用户同步功能

## 相关修复历史

本次修复是 OpenIM 集成系列修复的一部分：

1. ✅ **后端 Token 过期时间修复** - 使用 OpenIM 实际返回的过期时间
2. ✅ **前端响应数据提取修复** - 正确从 SmartAdmin axios wrapper 提取 `.data`
3. ✅ **Token 无限刷新临时禁用** - 由于 Token 有效期 90 天，临时禁用自动刷新
4. ✅ **im-token-api.ts API 路径修复** - 已添加 `/api` 前缀
5. ✅ **im-business-api.ts API 路径修复** - 已添加 `/api` 前缀
6. ✅ **im-api.ts API 路径修复** - 本次修复（18个端点）

## 注意事项

### 遗留问题
1. **Token 无限刷新问题**:
   - **当前状态**: 已临时禁用自动刷新
   - **长期方案**: 需要深入排查 `refreshTokenNow()` 被频繁调用的根本原因
   - **影响**: Token 有效期 90 天，短期内无影响

2. **im-api.ts 使用现状**:
   - 项目中已有新的 `im-token-api.ts` 和 `im-business-api.ts`
   - 需要确认 `im-api.ts` 是否还在使用中
   - 如果是遗留代码，建议逐步迁移到新 API 并废弃旧文件

### 最佳实践建议
1. **API 路径规范**: 所有新增 IM 相关 API 都应遵循 `/api/im/**` 路径规范
2. **代码审查检查点**: PR 审查时确认所有 API 路径包含 `/api` 前缀
3. **单元测试**: 为 API 端点编写单元测试，确保路径正确

## 文件清单

### 修改的文件
- `smart-admin-web-typescript/src/api/business/oa/im-api.ts` - 修复所有 API 路径

### 新增的文档
- `OPENIM_API_PATH_FIX_COMPLETE.md` - 本修复报告

## 下一步工作

### Phase 1.9: 单元测试和集成测试
建议编写以下测试：
1. API 路径正确性测试
2. Token 生命周期测试
3. 消息发送和接收集成测试
4. 群组管理功能测试

---

**修复完成时间**: 2025-10-10
**修复人员**: Claude Code Assistant
**审核状态**: 待用户验证
