# OpenIM 视频消息功能快速测试指南

## 修复说明

✅ **问题已修复**: 将 `createVideoMessage()` 更改为正确的 `createVideoMessageByFile()` 方法

**错误修复前**:
```
❌ TypeError: this.sdk.createVideoMessage is not a function
```

**错误修复后**:
```
✅ 使用正确的 createVideoMessageByFile() API
```

---

## 快速测试步骤

### 1. 启动服务

**后端**:
```bash
cd smart-admin-api-java17-springboot3
mvn spring-boot:run
```

**前端**:
```bash
cd smart-admin-web-typescript
npm run dev
```

### 2. 访问警情管理页面

1. 打开浏览器访问: `http://localhost:8081`
2. 登录系统（默认管理员账号）
3. 导航到: **业务功能 > 办公管理 > 警情管理**
4. 点击任意警情记录进入详情页

### 3. 测试视频消息发送

#### 测试场景 1: 发送小视频（< 10MB）
1. 在聊天面板中点击 **📹 视频图标按钮**
2. 选择一个小视频文件（推荐 5-10MB）
3. **预期结果**:
   - ✅ 自动提取视频时长
   - ✅ 自动生成视频缩略图
   - ✅ 显示上传进度
   - ✅ 视频消息成功发送
   - ✅ 消息显示视频播放器
   - ✅ 显示视频时长和文件大小

**浏览器控制台日志检查**:
```javascript
✅ [聊天面板] 发送视频消息, 文件名: xxx.mp4 大小: xxx
✅ [聊天面板] 视频时长: 6 秒
✅ [聊天面板] 视频缩略图生成成功
✅ [OpenIM] 发送群组视频消息, groupID: group_report_x
✅ [OpenIM] 视频消息对象创建成功
✅ [OpenIM] 群组视频消息发送成功
```

#### 测试场景 2: 发送大视频（100-200MB）
1. 选择较大的视频文件
2. **预期结果**:
   - ✅ 文件大小验证通过（< 200MB）
   - ✅ 上传时间较长（取决于网络速度）
   - ✅ 最终成功发送

#### 测试场景 3: 发送超大视频（> 200MB）
1. 选择超过 200MB 的视频
2. **预期结果**:
   - ❌ 显示错误提示: "视频文件大小不能超过 200MB"
   - ❌ 不发起上传请求

#### 测试场景 4: 多用户协作测试
1. 打开两个浏览器窗口，使用不同账号登录
2. 进入同一个警情详情页面
3. 用户A发送视频消息
4. **预期结果**:
   - ✅ 用户B实时收到视频消息
   - ✅ 用户B可以直接播放视频
   - ✅ 视频缩略图正确显示

---

## 调试技巧

### 1. 检查 WebSocket 连接
打开浏览器开发者工具 > Network > WS：
- 应该看到 WebSocket 连接状态为 `Connected`
- 视频消息发送时应该看到消息帧

### 2. 检查 OpenIM Token
打开浏览器控制台，查找：
```
✅ [OpenIM] Token已存储到OpenIM Redis: openim:token:emp_xxx:5
✅ [OpenIM] 登录成功, UserID: emp_xxx
```

### 3. 检查视频上传日志
浏览器控制台应显示：
```
🎬 [聊天面板] 发送视频消息, 文件名: video.mp4 大小: 411631
📹 [聊天面板] 视频时长: 6 秒
🖼️ [聊天面板] 视频缩略图生成成功
```

### 4. 检查消息接收
其他用户的控制台应显示：
```
📨 [OpenIM] 收到新消息: {contentType: 104, ...}
🔍 [DEBUG] 转换消息: {contentType: 104, videoElem: {...}}
✅ [DEBUG] 视频消息: {...}
```

---

## 常见问题排查

### 问题 1: 视频无法播放
**可能原因**: 视频格式不支持
**解决方案**:
- 使用标准格式（MP4, WebM）
- 检查浏览器控制台错误

### 问题 2: 缩略图不显示
**可能原因**: 缩略图生成失败
**解决方案**:
- 这不影响视频发送
- 视频仍然可以播放
- 查看控制台警告日志

### 问题 3: 上传速度慢
**可能原因**: 文件太大或网络慢
**解决方案**:
- 使用小一点的视频测试
- 检查网络连接
- 等待上传完成

### 问题 4: TypeError: createVideoMessage is not a function
**状态**: ✅ 已修复
**修复**: 已更改为 `createVideoMessageByFile()`

---

## 成功标准

✅ **所有测试通过标准**:
1. 视频文件可以成功上传
2. 视频时长自动提取正确
3. 视频缩略图自动生成（或降级不影响发送）
4. 视频消息在聊天列表正确显示
5. 视频播放器功能正常（播放、暂停、进度条）
6. 多用户实时接收视频消息
7. 视频信息（时长、大小）正确显示
8. 文件大小限制正确验证

---

## 技术要点

### SDK 方法使用
```typescript
// ✅ 正确用法
await this.sdk.createVideoMessageByFile({
  videoPath: '',
  duration: number,
  videoType: string,
  snapshotPath: '',
  videoUUID: string,
  videoUrl: '',
  videoSize: number,
  snapshotUUID: string,
  snapshotSize: number,
  snapshotUrl: '',
  snapshotWidth: number,
  snapshotHeight: number,
  snapShotType: string,
  videoFile: File,
  snapshotFile: File
});

// ❌ 错误用法（已修复）
await this.sdk.createVideoMessage({...});  // 此方法不存在
```

### 视频时长提取
```typescript
function extractVideoDuration(file: File): Promise<number> {
  return new Promise((resolve, reject) => {
    const video = document.createElement('video');
    video.preload = 'metadata';
    video.onloadedmetadata = () => {
      resolve(Math.floor(video.duration));
      window.URL.revokeObjectURL(video.src);
    };
    video.src = URL.createObjectURL(file);
  });
}
```

### 视频缩略图生成
```typescript
function generateVideoSnapshot(file: File): Promise<File> {
  return new Promise((resolve, reject) => {
    const video = document.createElement('video');
    const canvas = document.createElement('canvas');
    const context = canvas.getContext('2d');

    video.onloadedmetadata = () => {
      video.currentTime = 1; // 第1秒作为缩略图
    };

    video.onseeked = () => {
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      context.drawImage(video, 0, 0);
      canvas.toBlob((blob) => {
        const snapshotFile = new File([blob], 'snapshot.jpg', { type: 'image/jpeg' });
        resolve(snapshotFile);
      }, 'image/jpeg', 0.8);
    };

    video.src = URL.createObjectURL(file);
  });
}
```

---

## 报告问题

如果测试中发现问题，请记录：
1. 问题现象（截图或视频）
2. 浏览器控制台日志
3. 网络请求详情
4. 复现步骤
5. 视频文件信息（格式、大小、时长）

---

**测试完成后，请反馈测试结果！** ✅
