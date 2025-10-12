# OpenIM MinIO 存储访问修复指南

## 问题分析

### 症状
```
PUT http://192.168.1.7:10005/openim/... net::ERR_CONNECTION_TIMED_OUT
error *url.Error not implement CodeError: Put "http://192.168.1.7:10005/..."
```

### 根本原因

**网络隔离问题**: OpenIM 服务器配置的 MinIO 地址是 `192.168.1.7:10005`,这是一个内网 IP 地址,浏览器无法直接访问。

**架构流程**:
```
浏览器 -> OpenIM SDK (WASM) -> 获取 presigned URL
                              ↓
                    返回: http://192.168.1.7:10005/...
                              ↓
浏览器尝试 PUT -> ❌ ERR_CONNECTION_TIMED_OUT
```

**问题所在**:
- MinIO 实际运行在 `localhost:10005` (Docker 端口映射)
- OpenIM 服务器内部配置使用 `192.168.1.7:10005`
- 浏览器无法访问 `192.168.1.7` 这个内网地址

### 容器状态确认

```bash
$ wsl docker ps | grep -E "openim|minio"
minio/minio              Up 8 hours   0.0.0.0:10005->9000/tcp   ✅ MinIO运行正常
openim/openim-server     Up 8 hours   0.0.0.0:10001-10002      ✅ OpenIM运行正常
```

---

## 解决方案

### 方案 1: 修改 OpenIM 配置 (推荐)

**优点**:
- 治本,从源头解决问题
- 不需要额外的代理层
- 性能最优

**步骤**:

#### 1. 找到 OpenIM 配置文件

```bash
# 进入 WSL
wsl

# 找到 docker-compose 文件位置
cd /home/lihongda/openim-docker  # 或者你的实际路径

# 编辑 .env 文件或 config.yaml
vim .env
```

#### 2. 修改 MinIO 配置

查找并修改以下配置项:

**在 `.env` 文件中**:
```bash
# 修改前 (错误)
MINIO_ENDPOINT=192.168.1.7:10005
# 或
OBJECT_STORAGE_ENDPOINT=192.168.1.7:10005

# 修改后 (正确) - 使用 localhost
MINIO_ENDPOINT=localhost:10005
OBJECT_STORAGE_ENDPOINT=localhost:10005

# 或者使用 Docker 网络内部地址
MINIO_ENDPOINT=minio:9000  # 推荐,使用 Docker 内部网络
```

**在 `config/config.yaml` 中**:
```yaml
object:
  enable: "minio"
  minio:
    bucket: "openim"
    # 修改这里 ↓
    endpoint: "http://localhost:10005"  # 改为 localhost
    # 或
    endpoint: "http://minio:9000"      # Docker 内部网络(推荐)
    accessKeyID: "root"
    secretAccessKey: "openIM123"
    sessionToken: ""
    signEndpoint: "http://localhost:10005"  # 这个也要改
```

#### 3. 重启 OpenIM 服务

```bash
# 停止服务
docker-compose down

# 重新启动
docker-compose up -d

# 查看日志确认启动成功
docker logs -f openim-server
```

#### 4. 验证修复

**前端测试**:
1. 刷新浏览器 (`Ctrl + Shift + R`)
2. 访问: `http://localhost:8081/oa/police/report-detail?reportId=5`
3. 切换到 "即时聊天" Tab
4. 尝试发送图片

**预期成功日志**:
```
🖼️ [OpenIM] 发送群组图片消息, groupID: group_report_5
📐 [OpenIM] 图片尺寸: {width: 960, height: 1280}
✅ [OpenIM] 图片消息对象创建成功
SDK => upload progress: 100%
SDK => upload success: http://localhost:10005/openim/...  # ← 注意这里是 localhost
✅ [OpenIM] 群组图片消息发送成功
```

---

### 方案 2: 实现后端代理 (备选)

如果无法修改 OpenIM 配置,可以实现一个后端代理层。

**优点**:
- 不需要修改 OpenIM 配置
- 可以添加额外的安全控制

**缺点**:
- 增加了一层代理,性能稍差
- 需要额外的开发工作

**实现方案** (暂不推荐,除非方案1不可行):

#### 1. 创建代理 Controller

```java
@RestController
@RequestMapping("/api/openim/proxy")
@Tag(name = "OpenIM 文件代理")
public class OpenIMFileProxyController {

    @PostMapping("/upload/image")
    public ResponseDTO<String> uploadImage(@RequestParam("file") MultipartFile file) {
        // 1. 接收前端上传的图片
        // 2. 转发到 MinIO (后端可以访问 192.168.1.7:10005)
        // 3. 返回可访问的 URL
    }
}
```

#### 2. 修改前端上传逻辑

```typescript
// openim-client.ts
async sendGroupImageMessage(groupID: string, file: File): Promise<MessageItem> {
  // 方案A: 先上传到代理接口
  const uploadResult = await axios.post('/api/openim/proxy/upload/image', formData);
  const imageUrl = uploadResult.data.data;

  // 方案B: 创建自定义消息对象
  const message = await this.sdk.createCustomMessage({
    type: 'image',
    url: imageUrl,
    // ...
  });
}
```

---

## 验证步骤

### 步骤 1: 确认 MinIO 可访问性

```bash
# 测试 localhost 访问
curl http://localhost:10005/minio/health/live

# 预期返回
{"status":"ok"}
```

### 步骤 2: 确认 OpenIM 配置生效

```bash
# 查看 OpenIM 日志
docker logs openim-server | grep -i minio

# 预期日志应该显示 localhost 或 minio:9000
INFO: MinIO endpoint: http://localhost:10005
```

### 步骤 3: 前端功能测试

**测试清单**:
- [ ] 可以选择图片文件
- [ ] 图片尺寸读取成功
- [ ] 上传进度正常显示 (1% -> 100%)
- [ ] 上传成功,返回 presigned URL
- [ ] URL 使用 `localhost:10005` 或可访问的域名
- [ ] 图片消息在聊天面板正常显示
- [ ] 图片可以预览和下载

---

## 常见问题

### Q1: 修改配置后仍然失败

**检查清单**:
1. 确认配置文件路径正确
2. 确认已重启 OpenIM 服务
3. 确认浏览器已硬刷新 (`Ctrl + Shift + R`)
4. 检查 Docker 容器日志

```bash
docker logs openim-server | tail -50
```

### Q2: Docker 内部网络 vs localhost

**推荐使用 Docker 内部网络**:
```yaml
endpoint: "http://minio:9000"  # ← 推荐
```

**原因**:
- 更稳定,不受宿主机网络影响
- Docker 内部 DNS 自动解析
- 性能更好

**如果使用 localhost**:
```yaml
endpoint: "http://localhost:10005"
signEndpoint: "http://localhost:10005"  # 这个给浏览器用,必须是浏览器可访问的地址
```

### Q3: signEndpoint 是什么?

**作用**: `signEndpoint` 是生成 presigned URL 时使用的基础 URL,这个 URL 会返回给前端浏览器使用。

**配置要点**:
```yaml
object:
  minio:
    endpoint: "http://minio:9000"        # 后端内部访问
    signEndpoint: "http://localhost:10005"  # 前端浏览器访问
```

**如果配置错误**:
- `signEndpoint` 设置为 `192.168.1.7:10005` → 浏览器无法访问 ❌
- `signEndpoint` 设置为 `localhost:10005` → 浏览器可以访问 ✅

---

## 配置文件参考

### 完整的 MinIO 配置示例

```yaml
# config/config.yaml
object:
  enable: "minio"
  apiURL: ""

  minio:
    bucket: "openim"
    endpoint: "http://minio:9000"           # Docker 内部访问
    signEndpoint: "http://localhost:10005"  # 浏览器访问
    accessKeyID: "root"
    secretAccessKey: "openIM123"
    sessionToken: ""
    publicRead: false
```

### Docker Compose 网络配置

```yaml
# docker-compose.yml
services:
  openim-server:
    image: openim/openim-server:v3.8.3-patch.9
    environment:
      - MINIO_ENDPOINT=http://minio:9000
      - MINIO_SIGN_ENDPOINT=http://localhost:10005
    networks:
      - openim-network

  minio:
    image: minio/minio:RELEASE.2024-01-11T07-46-16Z
    ports:
      - "10005:9000"  # 映射到宿主机
      - "10004:9090"  # MinIO Console
    networks:
      - openim-network

networks:
  openim-network:
    driver: bridge
```

---

## 总结

### 修复核心

**一句话**: 将 OpenIM 配置中的 MinIO `signEndpoint` 从 `192.168.1.7:10005` 改为 `localhost:10005`。

### 修复检查

修复成功的标志:
1. ✅ OpenIM 日志显示 MinIO endpoint 为 `localhost` 或 `minio:9000`
2. ✅ 前端控制台显示上传 URL 为 `http://localhost:10005/...`
3. ✅ 图片上传成功,进度 100%,无超时错误
4. ✅ 图片消息在聊天面板正常显示

### 后续优化

1. **生产环境配置**: 使用公网可访问的域名
   ```yaml
   signEndpoint: "https://storage.yourdomain.com"
   ```

2. **CDN 加速**: 配置 CDN 加速图片访问
3. **安全加固**: 启用 MinIO HTTPS 和访问控制
4. **监控告警**: 添加 MinIO 健康检查和告警

---

**修复负责人**: Claude Code Assistant
**修复时间**: 2025-10-10
**问题级别**: 🔴 Critical (阻塞核心功能)
**预计解决时间**: < 30分钟 (配置修改 + 重启)

---

## 快速修复命令

```bash
# 1. 进入 OpenIM 配置目录
cd /home/lihongda/openim-docker  # 根据实际路径调整

# 2. 备份配置
cp .env .env.backup

# 3. 修改配置 (方式1: 使用 sed)
sed -i 's|MINIO_ENDPOINT=192.168.1.7:10005|MINIO_ENDPOINT=localhost:10005|g' .env
sed -i 's|OBJECT_STORAGE_ENDPOINT=192.168.1.7:10005|OBJECT_STORAGE_ENDPOINT=localhost:10005|g' .env

# 4. 重启服务
docker-compose down && docker-compose up -d

# 5. 验证
docker logs -f openim-server | grep -i minio
```

**完成!** 🎉
