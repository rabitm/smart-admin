# OpenIM环境配置确认

## 📋 环境信息

### OpenIM服务地址
- **部署方式**: 本地部署 (localhost)
- **环境**: 开发环境 (dev)

### 默认端口配置
根据OpenIM标准部署,默认端口配置如下:

| 服务 | 端口 | 用途 | 访问地址 |
|------|------|------|----------|
| API服务 | 10002 | REST API接口 | http://localhost:10002 |
| WebSocket | 10001 | 实时消息推送 | ws://localhost:10001 |
| 管理端API | 10009 | 管理后台接口 | http://localhost:10009 |

### 验证命令

```bash
# 检查API服务是否可用
curl http://localhost:10002/healthz

# 检查管理端API是否可用
curl http://localhost:10009/healthz
```

## ⚙️ SmartAdmin配置

### 后端配置 (sa-base.yaml)

```yaml
# OpenIM配置
openim:
  # 服务地址 (开发环境)
  api-url: http://localhost:10002
  ws-url: ws://localhost:10001
  admin-api-url: http://localhost:10009

  # 管理员配置 (请确认实际值)
  admin:
    user-id: imAdmin              # OpenIM管理员用户ID
    secret: openIM123             # OpenIM管理员密钥 (请替换为实际值)

  # 平台配置
  platform-id: 5                  # 5=Web平台

  # 自动拉人配置
  auto-invite:
    enabled: true
    rules:
      - type: CREATOR             # 创建人自动加入
        enabled: true
        priority: 1
      - type: OWNER               # 负责人自动加入
        enabled: true
        priority: 2
      - type: DEPARTMENT          # 部门成员自动加入
        enabled: false
        priority: 3
        config:
          include-manager: true   # 包含部门主管
      - type: ROLE                # 角色成员自动加入
        enabled: false
        priority: 4
        config:
          roles:                  # 指定角色列表
            - 消防队员
            - 应急指挥

  # 群组配置
  group:
    max-members: 500              # 最大成员数
    default-type: 2               # 默认群组类型 (2=工作群)
    auto-create: true             # 警情创建时自动创建群组

  # 功能开关
  features:
    text-message: true            # 文本消息
    image-message: true           # 图片消息
    file-message: true            # 文件消息
    voice-message: false          # 语音消息 (第二阶段)
    video-message: false          # 视频消息 (第二阶段)
    at-mention: true              # @提醒
    quote-reply: true             # 引用回复
    message-recall: true          # 消息撤回
    message-search: false         # 消息搜索 (第二阶段)

  # 安全配置
  security:
    token-expire-hours: 24        # Token有效期 (小时)
    max-file-size-mb: 100         # 最大文件大小 (MB)
    allowed-file-types:           # 允许的文件类型
      - jpg
      - jpeg
      - png
      - gif
      - pdf
      - doc
      - docx
      - xls
      - xlsx
      - zip
    rate-limit:
      enabled: true
      max-messages-per-minute: 60  # 每分钟最大消息数
```

### 前端配置 (.env.development)

```bash
# OpenIM配置
VITE_OPENIM_API_URL=http://localhost:10002
VITE_OPENIM_WS_URL=ws://localhost:10001
VITE_OPENIM_PLATFORM_ID=5

# IM功能开关
VITE_IM_ENABLED=true
VITE_IM_TEXT_MESSAGE=true
VITE_IM_IMAGE_MESSAGE=true
VITE_IM_FILE_MESSAGE=true
VITE_IM_VOICE_MESSAGE=false
VITE_IM_VIDEO_MESSAGE=false
VITE_IM_AT_MENTION=true
VITE_IM_QUOTE_REPLY=true
VITE_IM_MESSAGE_RECALL=true

# IM UI配置
VITE_IM_LAYOUT=embedded          # embedded=嵌入式, float=悬浮窗
VITE_IM_MESSAGE_PAGE_SIZE=20     # 消息分页大小
VITE_IM_AUTO_SCROLL=true         # 自动滚动到最新消息
```

## 🎯 功能优先级规划

### 第一阶段 (核心功能) - 2周

**目标**: 实现基础IM功能,嵌入警情详情页面

#### 后端开发 (1周)
- [x] OpenIM环境配置
- [ ] OpenIM API封装 (OpenIMApiService)
- [ ] 用户同步服务 (ImUserSyncService)
- [ ] 群组管理服务 (ImGroupManageService)
- [ ] 警情群组服务 (ImPoliceGroupService)
- [ ] 数据库表创建 (3张表)
- [ ] REST API接口 (认证、群组、消息)
- [ ] 警情创建监听器 (自动创建群组)

#### 前端开发 (1周)
- [ ] OpenIM SDK集成
- [ ] 嵌入式IM聊天组件 (embedded-im-chat.vue)
- [ ] 消息列表组件 (im-message-list.vue)
- [ ] 输入区域组件 (im-input-area.vue)
- [ ] 警情详情页集成
- [ ] 响应式布局适配

#### 功能范围
✅ **已确认**:
- 文本消息 (发送、接收、显示)
- 图片消息 (上传、预览)
- 文件消息 (上传、下载)
- @提醒功能
- 引用回复
- 消息撤回 (2分钟内)
- 群组成员列表
- 在线状态显示

❌ **暂不实现**:
- 语音消息
- 视频消息
- 消息搜索

### 第二阶段 (高级功能) - 1周

**目标**: 增强功能和用户体验

#### 功能清单
- [ ] 消息搜索 (按关键词、时间)
- [ ] 群公告功能
- [ ] 快捷回复 (预设常用语)
- [ ] 消息已读状态
- [ ] 离线消息推送
- [ ] 桌面通知
- [ ] 语音消息 (可选)
- [ ] 视频消息 (可选)

### 第三阶段 (优化和测试) - 1周

**目标**: 性能优化和全面测试

#### 任务清单
- [ ] 性能优化 (消息懒加载、连接复用)
- [ ] 安全加固 (权限验证、防刷机制)
- [ ] 多用户协同测试
- [ ] 移动端适配测试
- [ ] 用户体验优化
- [ ] 文档完善

## 🔧 自动拉人规则配置

### 默认启用规则

#### 规则1: 创建人自动加入 ✅
- **触发时机**: 警情创建时
- **规则说明**: 警情创建人自动成为群主并加入群组
- **优先级**: 1 (最高)
- **配置**: 默认启用,不可关闭

#### 规则2: 负责人自动加入 ✅
- **触发时机**: 分配负责人时
- **规则说明**: 警情负责人自动加入群组并设为管理员
- **优先级**: 2
- **配置**: 默认启用

### 可选规则 (暂不启用)

#### 规则3: 部门成员自动加入 ⏸️
- **触发时机**: 警情创建时
- **规则说明**: 警情所属部门的成员自动加入
- **优先级**: 3
- **配置**: 默认禁用
- **原因**: 可能导致群组成员过多,建议手动添加

#### 规则4: 角色成员自动加入 ⏸️
- **触发时机**: 警情创建时
- **规则说明**: 特定角色成员自动加入 (如消防队员)
- **优先级**: 4
- **配置**: 默认禁用
- **原因**: 需要根据警情类型动态配置,第二阶段实现

### 手动拉人机制 ✅
- 群主和管理员可以手动邀请成员
- 支持从协作人员列表快速添加
- 支持从部门/角色列表选择添加

## 📊 数据库初始化

### SQL脚本位置
```
sql/mysql/
├── im_user_mapping.sql          # 用户映射表
├── im_group_mapping.sql         # 群组映射表
└── im_auto_invite_rule.sql      # 自动拉人规则表
```

### 初始数据
```sql
-- 插入默认自动拉人规则
INSERT INTO t_im_auto_invite_rule (rule_name, rule_type, business_type, enabled, priority, create_time)
VALUES
  ('创建人自动加入', 'CREATOR', 'POLICE', 1, 1, NOW()),
  ('负责人自动加入', 'OWNER', 'POLICE', 1, 2, NOW()),
  ('部门成员自动加入', 'DEPARTMENT', 'POLICE', 0, 3, NOW()),
  ('角色成员自动加入', 'ROLE', 'POLICE', 0, 4, NOW());
```

## ✅ 待办事项清单

### 立即执行
- [x] 确认OpenIM环境配置
- [ ] 创建数据库表和初始数据
- [ ] 配置后端application.yaml
- [ ] 配置前端.env.development
- [ ] 获取OpenIM管理员凭据

### 开发准备
- [ ] 安装OpenIM Web SDK依赖
- [ ] 创建后端模块目录结构
- [ ] 创建前端组件目录结构
- [ ] 准备测试数据

## 🔍 需要确认的信息

### OpenIM管理员凭据
请提供以下信息:
- [ ] 管理员用户ID: `imAdmin` (默认值,请确认)
- [ ] 管理员密钥: `openIM123` (默认值,请替换为实际值)

### 测试环境
- [ ] 是否需要在test/pre环境也部署OpenIM?
- [ ] 生产环境OpenIM地址是什么?

---

**文档版本**: v1.0
**创建时间**: 2025-10-08
**作者**: Claude Code Assistant
**状态**: 等待确认
