# IM Business API 端点路径不匹配修复

## 修复时间
**2025-10-10**

## 问题描述

用户报告错误：
```
http://127.0.0.1:1024/api/im/business/group/5/exists
返回: {
    "code": 10001,
    "level": "system",
    "msg": "org.springframework.web.servlet.resource.NoResourceFoundException: No static resource api/im/business/group/5/exists.",
    "ok": false,
    "data": null,
    "dataType": 1
}
```

这是一个 **404 错误**，表明前端调用的 API 端点在后端不存在。

## 根本原因

前端 API 路径与后端 Controller 端点不匹配。

### 前端期望的路径 (im-business-api.ts)

```typescript
// ❌ 错误的路径
createGroupForReport: (reportId: number) => {
  return postRequest('/api/im/business/create-group', { reportId });
},

getGroupByReportId: (reportId: number) => {
  return getRequest(`/api/im/business/group/${reportId}`);
},

checkGroupExists: (reportId: number) => {
  return getRequest(`/api/im/business/group/${reportId}/exists`);
},
```

### 后端实际的路径 (IMBusinessController.java)

```java
@RestController
@RequestMapping("/api/im/business")
public class IMBusinessController {

    // ✅ 实际路径
    @PostMapping("/group/create/{reportId}")  // POST /api/im/business/group/create/{reportId}
    public ResponseDTO<String> createGroupForReport(@PathVariable Long reportId) {
        // ...
    }

    @GetMapping("/group/{reportId}")  // GET /api/im/business/group/{reportId} ✅ 匹配
    public ResponseDTO<String> getGroupIdByReportId(@PathVariable Long reportId) {
        // ...
    }

    @GetMapping("/group/exist/{reportId}")  // GET /api/im/business/group/exist/{reportId}
    public ResponseDTO<Boolean> isGroupExist(@PathVariable Long reportId) {
        // ...
    }
}
```

### 路径对比

| 方法 | 前端期望 | 后端实际 | 状态 |
|------|---------|---------|------|
| `createGroupForReport` | `POST /api/im/business/create-group` (body) | `POST /api/im/business/group/create/{reportId}` (path param) | ❌ 不匹配 |
| `getGroupByReportId` | `GET /api/im/business/group/{reportId}` | `GET /api/im/business/group/{reportId}` | ✅ 匹配 |
| `checkGroupExists` | `GET /api/im/business/group/{reportId}/exists` | `GET /api/im/business/group/exist/{reportId}` | ❌ 不匹配 (`exists` vs `exist`) |

## 解决方案

修改前端 API 路径以匹配后端。

### 修复 1: im-business-api.ts

```typescript
// ✅ 修复后：路径与后端匹配
export const imBusinessApi = {

  /**
   * 为警情创建 IM 群组
   * 后端返回: String (groupId)
   */
  createGroupForReport: (reportId: number) => {
    return postRequest(`/api/im/business/group/create/${reportId}`);
  },

  /**
   * 获取警情关联的群组ID
   * 后端返回: String (groupId)
   */
  getGroupByReportId: (reportId: number) => {
    return getRequest(`/api/im/business/group/${reportId}`);
  },

  /**
   * 检查警情是否已创建群组
   * 后端返回: Boolean
   */
  checkGroupExists: (reportId: number) => {
    return getRequest(`/api/im/business/group/exist/${reportId}`);
  },
};
```

**关键变更**:
1. `createGroupForReport`:
   - ❌ `POST /api/im/business/create-group` (body: `{reportId}`)
   - ✅ `POST /api/im/business/group/create/${reportId}` (path param)
2. `checkGroupExists`:
   - ❌ `GET /api/im/business/group/${reportId}/exists`
   - ✅ `GET /api/im/business/group/exist/${reportId}`

### 修复 2: 后端返回值类型变更

后端方法返回的是 **简单类型**，不是对象：

```java
// 返回 String (groupId)
public ResponseDTO<String> createGroupForReport(@PathVariable Long reportId) {
    String groupId = imBusinessService.createGroupForReport(reportId, creatorId);
    return ResponseDTO.ok(groupId);  // ← 直接返回 groupId 字符串
}

// 返回 String (groupId or null)
public ResponseDTO<String> getGroupIdByReportId(@PathVariable Long reportId) {
    String groupId = imBusinessService.getGroupIdByReportId(reportId);
    return ResponseDTO.ok(groupId);
}

// 返回 Boolean
public ResponseDTO<Boolean> isGroupExist(@PathVariable Long reportId) {
    boolean exist = imBusinessService.isGroupExist(reportId);
    return ResponseDTO.ok(exist);
}
```

所以前端需要调整响应处理逻辑。

### 修复 3: police-report-detail.vue

```typescript
async function ensureIMGroupExists(reportId: number) {
  try {
    // 1. 检查群组是否已存在
    const existsResponse = await imBusinessApi.checkGroupExists(reportId);
    const exists = existsResponse.data; // Boolean

    if (exists) {
      // 2. 获取群组ID
      const groupIdResponse = await imBusinessApi.getGroupByReportId(reportId);
      const groupId = groupIdResponse.data; // String (groupId)

      if (groupId) {
        imGroupId.value = groupId;
        detailData.value.imGroupId = groupId;
        console.log('✅ [警情详情-IM群组] 群组ID获取成功:', imGroupId.value);
      } else {
        await createNewIMGroup(reportId);
      }
    } else {
      await createNewIMGroup(reportId);
    }
  } catch (error) {
    console.error('❌ [警情详情-IM群组] 检查/创建群组失败:', error);
  }
}

async function createNewIMGroup(reportId: number) {
  try {
    const groupIdResponse = await imBusinessApi.createGroupForReport(reportId);
    const groupId = groupIdResponse.data; // String (groupId)

    if (groupId) {
      imGroupId.value = groupId;
      detailData.value.imGroupId = groupId;
      console.log('✅ [警情详情-IM群组] 群组创建成功, groupId:', imGroupId.value);
    }
  } catch (error) {
    console.error('❌ [警情详情-IM群组] 创建群组失败:', error);
    throw error;
  }
}
```

**关键变更**:
- ❌ `const groupInfo = groupInfoResponse.data; if (groupInfo && groupInfo.groupId)`
- ✅ `const groupId = groupIdResponse.data; if (groupId)` (直接是字符串)

### 修复 4: emergency-intake.vue

与 `police-report-detail.vue` 完全相同的修复模式。

## 响应数据结构

### SmartAdmin 标准响应

```json
{
  "code": 1,
  "msg": "success",
  "data": "sg_5",  // ← String (groupId)
  "dataType": 1,
  "ok": true
}
```

### Axios 响应包装

```json
{
  "data": {
    "code": 1,
    "msg": "success",
    "data": "sg_5"  // ← 实际业务数据
  },
  "status": 200,
  "statusText": "OK",
  "headers": {...},
  "config": {...}
}
```

### SmartAdmin Axios 拦截器处理

```typescript
// axios.ts:56
return Promise.resolve(res);  // res = response.data
```

所以在组件中：
```typescript
const response = await imBusinessApi.checkGroupExists(reportId);
// response = { code: 1, msg: "success", data: true }

const exists = response.data;  // true
```

## 文件修改清单

| 文件 | 修改内容 | 行号 | 状态 |
|------|---------|------|------|
| `im-business-api.ts` | 修复 3 个 API 路径 | 49-71 | ✅ 完成 |
| `police-report-detail.vue` | 修改响应处理逻辑 (String) | 186-237 | ✅ 完成 |
| `emergency-intake.vue` | 修改响应处理逻辑 (String) | 2427-2479 | ✅ 完成 |

## 修改代码统计

### im-business-api.ts

**修改前**: 错误的路径和参数传递方式
**修改后**: 正确的路径和 path parameter
**主要变更**:
- 3 个方法的路径修改
- `createGroupForReport` 从 body 参数改为 path parameter

### police-report-detail.vue & emergency-intake.vue

**修改内容**:
- 将 `groupInfo.groupId` 改为直接使用 `groupId` (String)
- 移除对象属性访问，改为直接字符串判断

## 测试验证

### 测试步骤

1. **刷新浏览器**
```bash
Ctrl + Shift + R  # 硬刷新，清除缓存
```

2. **访问警情详情页面**
```
http://localhost:8081/oa/police/report-detail?reportId=5
```

3. **观察控制台输出**

### 预期成功输出

```
📡 [警情详情-IM群组] 检查警情群组是否存在, reportId: 5
✅ [警情详情-IM群组] 群组已存在，正在获取群组ID...
✅ [警情详情-IM群组] 群组ID获取成功: sg_5  ← ✅ 不再是 undefined
```

**或者（群组不存在时）**:
```
📡 [警情详情-IM群组] 检查警情群组是否存在, reportId: 5
⚠️ [警情详情-IM群组] 群组不存在，正在创建新群组...
🆕 [警情详情-IM群组] 开始创建新群组, reportId: 5
✅ [警情详情-IM群组] 群组创建成功, groupId: sg_5
```

4. **验证 HTTP 请求**

打开浏览器开发者工具 → Network Tab:

```
✅ GET /api/im/business/group/exist/5  → 200 OK
✅ GET /api/im/business/group/5        → 200 OK
✅ POST /api/im/business/group/create/5 → 200 OK
```

### 预期失败输出（如果后端服务未启动）

```
❌ [警情详情-IM群组] 检查/创建群组失败: Error: Network Error
```

## 问题根源分析

### 为什么会有这个错误？

1. **前端代码是假设的实现**：在编写前端 API 时，我们假设了后端的接口设计，但没有先查看实际的后端代码。

2. **缺少接口文档**：如果有 Swagger/OpenAPI 文档，可以避免这种路径不匹配问题。

3. **命名不一致**：`exists` vs `exist` 这种细微差异很容易被忽略。

### 教训

1. **先读后端代码**：在编写前端 API 调用前，先查看后端 Controller 的实际实现。
2. **使用 Swagger/Knife4j**：SmartAdmin 已集成 Knife4j，应该先访问 `/doc.html` 查看 API 文档。
3. **端到端测试**：在集成完成后立即进行端到端测试，而不是等到用户报错。
4. **注意细节**：`exist` vs `exists`、路径参数 vs body 参数等细节很重要。

## 相关 API 文档

### Knife4j 文档访问

```
http://localhost:1024/doc.html
```

在 `IM业务集成管理` 分组下可以看到：
- `POST /api/im/business/group/create/{reportId}` - 为警情创建群组
- `GET /api/im/business/group/{reportId}` - 获取警情的群组ID
- `GET /api/im/business/group/exist/{reportId}` - 检查警情群组是否存在
- `POST /api/im/business/group/archive/{reportId}` - 归档警情群组
- `POST /api/im/business/group/disband/{reportId}` - 解散警情群组

## 后续优化建议

### 1. 前端 API 类型定义

```typescript
import type { ResponseModel } from '/@/model/response-model';

export const imBusinessApi = {
  /**
   * 检查群组是否存在
   * @returns Promise<ResponseModel<boolean>>
   */
  checkGroupExists: (reportId: number): Promise<ResponseModel<boolean>> => {
    return getRequest(`/api/im/business/group/exist/${reportId}`);
  },

  /**
   * 获取群组ID
   * @returns Promise<ResponseModel<string>>
   */
  getGroupByReportId: (reportId: number): Promise<ResponseModel<string>> => {
    return getRequest(`/api/im/business/group/${reportId}`);
  },
};
```

### 2. API 调用示例注释

```typescript
/**
 * 检查群组是否存在
 *
 * @example
 * ```typescript
 * const response = await imBusinessApi.checkGroupExists(5);
 * const exists = response.data; // boolean
 * console.log(exists); // true or false
 * ```
 */
checkGroupExists: (reportId: number) => {
  return getRequest(`/api/im/business/group/exist/${reportId}`);
},
```

### 3. 统一错误处理

```typescript
async function ensureIMGroupExists(reportId: number) {
  try {
    const existsResponse = await imBusinessApi.checkGroupExists(reportId);
    // ...
  } catch (error: any) {
    // 统一错误处理
    if (error.response?.status === 404) {
      console.error('❌ API 端点不存在:', error.config?.url);
    } else if (error.response?.status === 500) {
      console.error('❌ 服务器错误:', error.response.data);
    } else {
      console.error('❌ 请求失败:', error.message);
    }
  }
}
```

## 下一步行动

### 立即测试

1. **刷新浏览器** (`Ctrl + Shift + R`)
2. **访问警情详情页面** (`http://localhost:8081/oa/police/report-detail?reportId=5`)
3. **切换到"即时聊天"Tab**
4. **验证以下内容**:
   - ✅ `groupId` 不再是 `undefined`
   - ✅ 可以加载历史消息
   - ✅ 可以发送消息
   - ✅ 可以接收其他用户的消息

### 后续工作

5. **访问 Knife4j 文档** (`http://localhost:1024/doc.html`)
   - 验证所有 IM API 端点
   - 测试每个端点的响应格式

6. **端到端测试场景**:
   - 创建新警情 → 自动创建 IM 群组
   - 编辑警情 → 聊天功能可用
   - 多用户协作 → 消息实时同步

---

**修复完成时间**: 2025-10-10
**修复人员**: Claude Code Assistant
**问题类型**: 前后端 API 端点路径不匹配
**影响范围**: `im-business-api.ts`, `police-report-detail.vue`, `emergency-intake.vue`
**测试状态**: 待用户验证

---

## 总结

这是一个经典的 **前后端接口不一致错误**：

- **错误原因**: 前端假设的 API 路径与后端实际实现不符
- **解决方法**: 修改前端路径以匹配后端，同时调整响应数据处理逻辑
- **预防措施**: 使用 Swagger/Knife4j 文档，先读后端代码再写前端调用

**关键经验**:
1. 先查看 API 文档或后端代码
2. 注意路径细节 (`exist` vs `exists`)
3. 注意参数传递方式 (path param vs body)
4. 注意返回值类型 (String vs Object)
5. 立即进行端到端测试

这次修复不仅解决了 404 错误，还统一了前后端的接口约定，提高了系统的可靠性！
