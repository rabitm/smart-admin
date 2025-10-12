# 群组同步问题修复测试指南

## 修复状态

✅ **后端代码已修复** - `IMBusinessService.java` 添加自动验证和修复逻辑
✅ **前端代码已修复** - `police-report-detail.vue` 直接调用创建接口
✅ **编译验证通过** - `mvn clean compile` 成功

⚠️ **待操作**：需要重启后端服务使修复生效

---

## 快速修复步骤

### 步骤 1: 重启后端服务

```bash
# 方法 A: 如果使用 IDEA 运行
1. 在 IDEA 中停止当前运行的应用
2. 重新点击 Run 按钮启动

# 方法 B: 如果使用 Maven 命令行
cd smart-admin-api-java17-springboot3
mvn spring-boot:run
```

### 步骤 2: 刷新前端

```bash
# 硬刷新浏览器（清除缓存）
Ctrl + Shift + R  (Windows/Linux)
Cmd + Shift + R   (Mac)
```

### 步骤 3: 测试修复效果

访问：`http://localhost:8081/oa/police/report-detail?reportId=5`

切换到 **"即时聊天"** Tab

---

## 预期行为

### 🔍 前端控制台输出

```
📡 [警情详情-IM群组] 开始创建/获取群组, reportId: 5
🆕 [警情详情-IM群组] 开始创建新群组, reportId: 5
✅ [警情详情-IM群组] 群组创建成功, groupId: group_report_5
```

### 🔍 后端控制台输出（关键）

**场景 1: 首次访问，数据库有旧映射但服务器无群组**

```log
⚠️ [群组创建] 数据库中存在群组映射,但OpenIM服务器上不存在,删除旧映射重新创建
📤 [群组创建] 开始为警情5创建群组: group_report_5
✅ [群组创建] 警情5的群组创建成功: group_report_5, 耗时: 150ms
```

**场景 2: 再次访问，群组已正常存在**

```log
✅ [群组创建] 警情5的群组已存在且已验证: group_report_5
```

### 🔍 OpenIM API 验证

访问：`http://localhost:10002/group/get_groups_info`

**修复前**（错误）:
```json
{
  "errCode": 0,
  "errMsg": "",
  "data": {
    "groupInfos": null
  }
}
```

**修复后**（正确）:
```json
{
  "errCode": 0,
  "errMsg": "",
  "data": {
    "groupInfos": [
      {
        "groupID": "group_report_5",
        "groupName": "警情-JQ202501050001",
        "groupType": 2,
        // ... 其他群组信息
      }
    ]
  }
}
```

### ✅ 聊天功能验证

1. **发送消息** - 输入文本并点击发送
2. **查看历史消息** - 应该能看到刚发送的消息
3. **多用户测试** - 在另一个浏览器登录另一个用户，验证消息同步

---

## 自动修复原理

### 修复前的问题

```
┌─────────────────────┐
│ 数据库             │  ✅ 存在映射: report_id=5 → group_report_5
└─────────────────────┘
          ↓
┌─────────────────────┐
│ OpenIM 服务器      │  ❌ 不存在群组: group_report_5
└─────────────────────┘
          ↓
      结果: 聊天功能失败
```

### 修复后的流程

```
1. 用户访问警情详情页面
       ↓
2. 前端调用 createGroupForReport(reportId=5)
       ↓
3. 后端检查数据库
   ├─→ 无映射 → 创建新群组
   └─→ 有映射
       ↓
4. 后端调用 verifyGroupExistsOnServer(groupId)
   ├─→ OpenIM 有群组 → 返回 groupId ✅
   └─→ OpenIM 无群组 → 继续下一步
       ↓
5. 🔧 自动修复逻辑
   ├─→ 删除数据库旧映射
   ├─→ 删除关联的成员记录
   ├─→ 调用 OpenIM API 创建真实群组
   ├─→ 保存新映射到数据库
   └─→ 返回新 groupId ✅
       ↓
6. 前端接收 groupId → 聊天功能正常 ✅
```

---

## 常见问题排查

### Q1: 后端日志没有 "⚠️ 数据库中存在群组映射,但OpenIM服务器上不存在" 提示

**原因**: 后端服务没有重启，还在运行旧代码

**解决**:
1. 停止后端服务
2. 重新运行 `mvn spring-boot:run` 或在 IDEA 中重启
3. 确认日志中显示应用启动成功

### Q2: 前端报错 "404 Not Found"

**原因**: 后端服务未启动或端口不是 1024

**解决**:
1. 检查后端是否正常运行：访问 `http://localhost:1024/doc.html`
2. 检查端口配置：查看 `sa-base.yaml` 中的 `server.port`

### Q3: 后端日志显示 "群组创建失败: Connection refused"

**原因**: OpenIM 服务器未启动

**解决**:
1. 启动 OpenIM 服务器
2. 验证 OpenIM 运行正常：`curl http://localhost:10002/`

### Q4: 仍然报错 "Group ID not found"

**原因**: 可能有以下几种情况

**排查步骤**:

1. **检查数据库是否清理成功**
```sql
SELECT * FROM t_im_group_mapping WHERE report_id = 5;
-- 应该看到 group_status = 1 的记录
```

2. **检查 OpenIM 服务器群组是否创建成功**
```bash
curl -X POST http://localhost:10002/group/get_groups_info \
  -H "Content-Type: application/json" \
  -H "token: YOUR_TOKEN" \
  -d '{"groupIDs": ["group_report_5"]}'
```

3. **检查后端完整日志**
```bash
# 查找关键日志
grep "群组创建" logs/sa-admin.log
grep "group_report_5" logs/sa-admin.log
```

---

## 手动修复（紧急情况）

如果自动修复仍然失败，可以手动执行以下 SQL 清理数据：

```sql
-- 1. 删除群成员记录
DELETE FROM t_im_group_member
WHERE group_mapping_id IN (
    SELECT id FROM t_im_group_mapping WHERE report_id = 5
);

-- 2. 删除群组映射记录
DELETE FROM t_im_group_mapping
WHERE report_id = 5;

-- 3. 验证删除成功
SELECT COUNT(*) FROM t_im_group_mapping WHERE report_id = 5;
-- 预期结果: 0

-- 4. 刷新浏览器，让系统重新创建
```

---

## 验证修复成功的标志

### ✅ 后端日志

```
✅ [群组创建] 警情5的群组创建成功: group_report_5
```

### ✅ 前端控制台

```
✅ [警情详情-IM群组] 群组创建成功, groupId: group_report_5
✅ [OpenIM] 获取历史消息成功, 数量: X
```

### ✅ 数据库

```sql
SELECT
    openim_group_id,
    group_status,
    create_time
FROM t_im_group_mapping
WHERE report_id = 5;
```

结果应该显示一条 `group_status = 1` 的记录

### ✅ OpenIM 服务器

```bash
curl -X POST http://localhost:10002/group/get_groups_info \
  -H "Content-Type: application/json" \
  -d '{"groupIDs": ["group_report_5"]}'
```

返回应该包含群组详细信息（`groupInfos` 不为 null）

### ✅ 功能测试

1. 可以发送消息 ✅
2. 可以查看历史消息 ✅
3. 其他用户可以接收消息 ✅

---

## 下一步

修复验证通过后，建议：

1. **测试其他警情** - 确保修复对所有警情都生效
2. **测试新建警情** - 验证新创建的警情聊天功能正常
3. **监控后端日志** - 观察是否还有其他同步问题
4. **通知团队** - 让其他开发者了解修复方案

---

**修复负责人**: Claude Code Assistant
**修复时间**: 2025-10-10 17:00
**测试状态**: 等待用户重启后端并验证

如有任何问题，请提供完整的后端日志和前端控制台输出！
