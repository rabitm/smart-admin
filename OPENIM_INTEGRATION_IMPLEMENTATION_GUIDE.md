# OpenIM Integration Implementation Guide

## 📋 实施进度

### ✅ 已完成
1. 数据库表设计 (v3.28.0-openim-integration.sql)
2. 常量定义 (IMErrorCodeEnum, IMOperationTypeEnum, IMGroupRoleEnum, IMConstant)
3. 配置类 (OpenIMConfig, IMAsyncConfig)
4. 核心客户端 (OpenIMClient, OpenIMTokenManager)

### 🚧 待实施

#### 1. 实体类 (domain/entity/)
需要创建以下实体类对应数据库表:

- IMUserMappingEntity.java
- IMGroupMappingEntity.java
- IMGroupMemberEntity.java
- IMGroupInviteRuleEntity.java
- IMOperationLogEntity.java
- IMConfigEntity.java

#### 2. DAO层 (dao/)
需要创建对应的Mapper接口:

- IMUserMappingDao.java
- IMGroupMappingDao.java
- IMGroupMemberDao.java
- IMGroupInviteRuleDao.java
- IMOperationLogDao.java
- IMConfigDao.java

#### 3. DTO/VO类 (domain/dto/, domain/vo/)
根据业务需求创建数据传输对象

#### 4. 服务层 (service/)
核心业务逻辑实现:

- IMUserSyncService.java - 用户同步服务
- IMGroupManagementService.java - 群组管理服务
- IMMessageService.java - 消息服务
- IMInviteRuleService.java - 邀请规则服务
- IMEventListenerService.java - 事件监听服务
- IMOperationLogService.java - 操作日志服务
- IMConfigService.java - 配置管理服务

#### 5. Manager层 (manager/)
复杂业务编排:

- IMUserSyncManager.java - 用户同步管理器
- IMGroupAutoInviteManager.java - 自动邀请管理器

#### 6. Controller层 (controller/)
REST API端点:

- IMUserController.java - 用户管理API
- IMGroupController.java - 群组管理API
- IMConfigController.java - 配置管理API

#### 7. 事件集成
与警情系统集成:

- 在PoliceReportService中添加创建警情后的事件发布
- 实现IMEventListenerService监听警情创建事件
- 自动创建IM群组并邀请成员

#### 8. 前端集成
- OpenIM Web SDK集成
- 聊天UI组件开发
- 警情页面集成

## 📦 依赖配置

### application.yaml 配置示例

```yaml
# OpenIM配置
openim:
  # 是否启用IM功能
  enabled: true

  # OpenIM服务地址
  api-url: http://localhost:10002
  ws-url: ws://localhost:10001

  # 管理员配置
  admin-user-id: imAdmin
  admin-secret: ${OPENIM_ADMIN_SECRET:}

  # 平台ID (10-Admin)
  platform-id: 10

  # Token配置
  token-expire-seconds: 7200

  # API配置
  api-timeout-seconds: 10
  retry-max-count: 3

  # 业务配置
  auto-create-group: true
  auto-invite: true
  group-max-members: 500
  group-name-template: "警情-{reportNumber}"

  # 批量配置
  user-sync-batch-size: 50
  group-invite-batch-size: 30

  # 熔断器配置
  circuit-breaker-threshold: 5
  circuit-breaker-timeout-seconds: 30
```

## 🔧 快速开始

### 1. 执行数据库脚本
```bash
mysql -u root -p your_database < sql/sql-update-log/v3.28.0-openim-integration.sql
```

### 2. 配置OpenIM服务地址
在`application.yaml`中配置您本地的OpenIM服务地址

### 3. 启动应用
```bash
cd smart-admin-api-java17-springboot3
mvn spring-boot:run
```

### 4. 测试Token获取
访问: GET /api/im/config/token-info

### 5. 同步用户
访问: POST /api/im/user/sync-all

### 6. 创建警情测试群组
创建一个新警情,系统会自动创建对应的IM群组

## 📝 开发说明

### 服务层实现要点

1. **用户同步服务**
   - 监听员工创建/更新事件
   - 批量同步现有员工
   - 维护员工<->OpenIM用户映射关系
   - 支持增量同步

2. **群组管理服务**
   - 警情创建时自动创建群组
   - 根据规则自动邀请成员
   - 支持手动添加/移除成员
   - 群组状态同步

3. **邀请规则引擎**
   - 支持多种规则类型
   - 规则优先级排序
   - 规则条件匹配
   - 目标人员计算

4. **操作日志服务**
   - 记录所有IM操作
   - 支持审计查询
   - 异步写入日志

### 错误处理规范

1. 使用IMErrorCodeEnum统一错误码
2. 所有异常转换为BusinessException
3. 记录详细的错误日志
4. 提供用户友好的错误信息

### 性能优化建议

1. 批量操作使用分批处理
2. 异步执行耗时操作
3. 使用缓存减少API调用
4. 实施熔断器防止雪崩

## 🔍 测试检查清单

- [ ] Token获取和刷新
- [ ] 用户同步(单个/批量)
- [ ] 群组创建
- [ ] 成员邀请(自动/手动)
- [ ] 成员移除
- [ ] 消息发送
- [ ] 规则匹配
- [ ] 操作日志记录
- [ ] 熔断器功能
- [ ] 异常处理
- [ ] 并发测试

## 📚 参考文档

- OpenIM官方文档: https://docs.openim.io
- OpenIM REST API: https://docs.openim.io/restapi/apis/introduction
- SmartAdmin开发规范: https://smartadmin.vip/views/doc/standard/basic.html

## 🎯 下一步行动

由于代码文件较多,建议分阶段实施:

**阶段1 (当前)**:
- 完成配置和客户端层 ✅
- 创建实体类和DAO

**阶段2**:
- 实现用户同步服务
- 实现群组管理服务

**阶段3**:
- 实现规则引擎
- 集成警情系统

**阶段4**:
- 前端集成
- 测试优化

需要我继续实施哪个阶段?
