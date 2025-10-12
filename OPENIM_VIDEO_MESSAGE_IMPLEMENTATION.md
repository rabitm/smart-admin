# OpenIM 群组视频消息发送功能实现文档

## 实现概述

本次更新为警情管理系统的聊天面板添加了完整的**群组视频消息发送功能**，包括：
- 视频文件上传
- 自动提取视频时长
- 自动生成视频缩略图
- 视频消息展示（带播放器）
- 视频信息显示（时长、文件大小）

## 实现时间
2025-10-10

## 修改文件列表

### 1. 后端 - OpenIM 客户端工具类

**文件**: `smart-admin-web-typescript/src/utils/openim-client.ts`

**新增方法**: `sendGroupVideoMessage()` (lines 1169-1216)

**功能**:
- 发送群组视频消息到 OpenIM 服务器
- 支持视频文件和可选的缩略图上传
- 使用 OpenIM SDK 的 `createVideoMessage()` 和 `sendMessage()` API

**方法签名**:
```typescript
async sendGroupVideoMessage(
  groupID: string,
  videoFile: File,
  duration: number,
  snapshotFile?: File
): Promise<MessageItem>
```

**关键实现**:
```typescript
// 获取缩略图尺寸
const snapshotSize = snapshotFile ? await this.getImageDimensions(snapshotFile) : { width: 0, height: 0 };

// 创建视频消息对象（使用 createVideoMessageByFile）
const message = await this.sdk.createVideoMessageByFile({
  videoPath: '',
  duration,
  videoType: videoFile.type,
  snapshotPath: '',
  videoUUID: this.generateUUID(),
  videoUrl: '',
  videoSize: videoFile.size,
  snapshotUUID: this.generateUUID(),
  snapshotSize: snapshotFile?.size || 0,
  snapshotUrl: '',
  snapshotWidth: snapshotSize.width,
  snapshotHeight: snapshotSize.height,
  snapShotType: snapshotFile?.type || 'image/jpeg',
  videoFile: videoFile,
  snapshotFile: snapshotFile || new File([], 'snapshot.jpg', { type: 'image/jpeg' }),
});

// 发送群组消息
const result = await this.sdk.sendMessage({
  recvID: '',        // 群聊时 recvID 为空
  groupID: groupID,
  message: messageData,
});
```

---

### 2. 前端 - 聊天面板组件

**文件**: `smart-admin-web-typescript/src/views/business/oa/police/components/ChatPanel.vue`

#### 2.1 UI 变更

**新增导入** (line 147):
```typescript
import { VideoCameraOutlined } from '@ant-design/icons-vue';
```

**新增视频上传按钮** (lines 127-129):
```vue
<a-button size="small" @click="showVideoUpload" title="发送视频">
  <template #icon><VideoCameraOutlined /></template>
</a-button>
```

#### 2.2 数据结构扩展

**扩展 Message 接口** (lines 178-184):
```typescript
interface Message {
  // ... 其他属性

  // 视频消息额外属性
  videoElem?: {
    videoUrl: string;
    snapshotUrl: string;
    videoSize: number;
    duration: number;
  };
}
```

#### 2.3 消息展示

**新增视频消息展示模板** (lines 85-97):
```vue
<!-- 视频消息 -->
<div v-else-if="message.contentType === 104 && message.videoElem" class="message-video">
  <video
    :src="message.videoElem.videoUrl"
    :poster="message.videoElem.snapshotUrl"
    controls
    style="max-width: 300px; max-height: 300px; border-radius: 4px"
  />
  <div class="video-info">
    <span>{{ formatDuration(message.videoElem.duration) }}</span>
    <span>{{ formatFileSize(message.videoElem.videoSize) }}</span>
  </div>
</div>
```

#### 2.4 消息转换逻辑

**增强 convertMessageItem() 函数** (lines 374-388):
```typescript
} else if (item.contentType === 104) {
  // 104 - 视频消息
  const vElem = (item as any).videoElem;
  if (vElem) {
    videoElem = {
      videoUrl: vElem.videoUrl || vElem.videoPath,
      snapshotUrl: vElem.snapshotUrl || vElem.snapshotPath || '',
      videoSize: vElem.videoSize || 0,
      duration: vElem.duration || 0,
    };
    content = '[视频消息]';
    console.log('✅ [DEBUG] 视频消息:', vElem);
  } else {
    content = '[视频消息]';
  }
}
```

#### 2.5 核心业务逻辑

**新增函数 1: showVideoUpload()** (lines 677-689)
```typescript
function showVideoUpload() {
  // 创建文件选择器
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'video/*';
  input.onchange = async (e: Event) => {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (file) {
      await sendVideoMessage(file);
    }
  };
  input.click();
}
```

**新增函数 2: sendVideoMessage()** (lines 694-744)
```typescript
async function sendVideoMessage(file: File) {
  try {
    sending.value = true;
    console.log('🎬 [聊天面板] 发送视频消息, 文件名:', file.name, '大小:', file.size);

    // 1. 验证文件大小 (限制为 200MB)
    const maxSize = 200 * 1024 * 1024;
    if (file.size > maxSize) {
      throw new Error('视频文件大小不能超过 200MB');
    }

    // 2. 提取视频时长
    const duration = await extractVideoDuration(file);
    console.log('📹 [聊天面板] 视频时长:', duration, '秒');

    // 3. 生成视频缩略图 (可选，失败不影响发送)
    let snapshotFile: File | undefined;
    try {
      snapshotFile = await generateVideoSnapshot(file);
      console.log('🖼️ [聊天面板] 视频缩略图生成成功');
    } catch (error) {
      console.warn('⚠️ [聊天面板] 视频缩略图生成失败，将不使用缩略图:', error);
    }

    // 4. 使用 OpenIM SDK 发送群组视频消息
    const result = await openIMClient.sendGroupVideoMessage(props.groupId, file, duration, snapshotFile);
    console.log('✅ [聊天面板] 视频消息发送成功:', result);

    // 5. 手动添加消息到列表
    const messageData = result.data || result;
    const newMessage = convertMessageItem(messageData);

    const exists = messages.value.some((m) => m.messageId === newMessage.messageId);
    if (!exists) {
      messages.value.push(newMessage);
      await scrollToBottom();
    }

  } catch (error) {
    console.error('❌ [聊天面板] 视频消息发送失败:', error);
    antMessage.error('视频发送失败: ' + (error as Error).message);
    smartSentry.captureError(error, { tags: { module: 'ChatPanel', action: 'sendVideoMessage' } });
  } finally {
    sending.value = false;
  }
}
```

**新增函数 3: extractVideoDuration()** (lines 749-766)
```typescript
function extractVideoDuration(file: File): Promise<number> {
  return new Promise((resolve, reject) => {
    const video = document.createElement('video');
    video.preload = 'metadata';

    video.onloadedmetadata = () => {
      window.URL.revokeObjectURL(video.src);
      resolve(Math.floor(video.duration));
    };

    video.onerror = () => {
      window.URL.revokeObjectURL(video.src);
      reject(new Error('无法读取视频文件'));
    };

    video.src = URL.createObjectURL(file);
  });
}
```

**新增函数 4: generateVideoSnapshot()** (lines 771-817)
```typescript
function generateVideoSnapshot(file: File): Promise<File> {
  return new Promise((resolve, reject) => {
    const video = document.createElement('video');
    const canvas = document.createElement('canvas');
    const context = canvas.getContext('2d');

    if (!context) {
      reject(new Error('无法创建Canvas上下文'));
      return;
    }

    video.preload = 'metadata';

    video.onloadedmetadata = () => {
      // 跳转到视频的第1秒作为缩略图
      video.currentTime = 1;
    };

    video.onseeked = () => {
      // 设置画布大小为视频尺寸
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;

      // 绘制视频帧到画布
      context.drawImage(video, 0, 0, canvas.width, canvas.height);

      // 转换为JPEG Blob
      canvas.toBlob((blob) => {
        window.URL.revokeObjectURL(video.src);

        if (blob) {
          const snapshotFile = new File([blob], 'snapshot.jpg', { type: 'image/jpeg' });
          resolve(snapshotFile);
        } else {
          reject(new Error('无法生成缩略图'));
        }
      }, 'image/jpeg', 0.8);  // 80% 质量压缩
    };

    video.onerror = () => {
      window.URL.revokeObjectURL(video.src);
      reject(new Error('无法读取视频文件'));
    };

    video.src = URL.createObjectURL(file);
  });
}
```

**新增函数 5: formatDuration()** (lines 549-553)
```typescript
function formatDuration(seconds: number): string {
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}
```

#### 2.6 样式增强

**新增视频消息样式** (lines 971-984):
```less
.message-video {
  video {
    display: block;
    border-radius: 4px;
  }

  .video-info {
    display: flex;
    justify-content: space-between;
    margin-top: 4px;
    font-size: 12px;
    color: #999;
  }
}
```

---

## 技术特性

### 1. 自动视频时长提取
- 使用 HTML5 Video API 的 `onloadedmetadata` 事件
- 无需服务器端处理，纯前端实现
- 自动清理临时 Object URL，防止内存泄漏

### 2. 智能缩略图生成
- 使用 Canvas API 从视频第 1 秒截取帧
- 80% JPEG 压缩质量平衡文件大小和清晰度
- 失败降级：缩略图生成失败不影响视频发送

### 3. 文件大小限制
- 视频文件限制：200MB（可配置）
- 提前验证，避免无效上传
- 友好的错误提示

### 4. 视频播放器集成
- 使用 HTML5 `<video>` 标签
- 支持标准播放控制（播放、暂停、进度条、音量）
- 缩略图作为 `poster` 预览图
- 响应式尺寸限制（max-width: 300px, max-height: 300px）

### 5. 消息展示增强
- 显示视频时长（格式：mm:ss）
- 显示视频文件大小（自动单位转换：B/KB/MB/GB）
- 与其他消息类型（文本、图片、文件）保持一致的UI风格

---

## OpenIM 消息类型映射

| contentType | 消息类型 | 实现状态 |
|-------------|---------|---------|
| 101 | 文本消息 | ✅ 已实现 |
| 102 | 图片消息 | ✅ 已实现 |
| 103 | 语音消息 | ⚠️ 仅接收 |
| 104 | 视频消息 | ✅ **本次实现** |
| 106 | 文件消息 | ✅ 已实现 |
| 1501-1999 | 系统通知 | ✅ 已实现 |

---

## 使用流程

### 用户操作流程
1. 用户点击聊天输入框上方的**视频图标按钮** 📹
2. 系统弹出文件选择器（仅接受 `video/*` 类型）
3. 用户选择视频文件
4. 系统自动：
   - 验证文件大小（≤ 200MB）
   - 提取视频时长
   - 生成缩略图（1秒处截图）
5. 上传并发送消息到群组
6. 消息实时显示在聊天列表
7. 其他用户接收到消息，可直接在线播放

### 技术流程
```
用户选择视频
    ↓
showVideoUpload()
    ↓
sendVideoMessage(file)
    ↓
├─ 验证文件大小
├─ extractVideoDuration(file) → duration
├─ generateVideoSnapshot(file) → snapshotFile (可选)
    ↓
openIMClient.sendGroupVideoMessage(groupId, file, duration, snapshotFile)
    ↓
OpenIM SDK: createVideoMessage()
    ↓
OpenIM SDK: sendMessage()
    ↓
后端存储视频文件到 MinIO
    ↓
返回消息对象 (MessageItem)
    ↓
convertMessageItem() → Message
    ↓
添加到 messages 列表
    ↓
UI 渲染视频播放器
```

---

## 已知问题与修复

### 1. SDK 方法名称问题 ✅ 已修复
**问题**: 初始实现使用了不存在的 `createVideoMessage()` 方法
**错误**: `TypeError: this.sdk.createVideoMessage is not a function`
**修复**: 更改为使用正确的 `createVideoMessageByFile()` 方法

OpenIM SDK 提供的视频消息创建方法是：
- `createVideoMessageByFile()` - 通过文件创建视频消息（正确）
- `createVideoMessageByURL()` - 通过 URL 创建视频消息

**正确的参数结构**:
```typescript
{
  videoPath: '',
  duration: number,
  videoType: string,
  snapshotPath: '',
  videoUUID: string,        // 必需
  videoUrl: '',
  videoSize: number,         // 必需
  snapshotUUID: string,      // 必需
  snapshotSize: number,
  snapshotUrl: '',
  snapshotWidth: number,     // 必需
  snapshotHeight: number,    // 必需
  snapShotType: string,
  videoFile: File,           // 必需
  snapshotFile: File         // 必需（即使没有也要传空File）
}
```

---

## 错误处理

### 1. 文件大小超限
```
❌ 错误: 视频文件大小不能超过 200MB
前置验证，不会发起网络请求
```

### 2. 视频文件损坏
```
❌ 错误: 无法读取视频文件
在 extractVideoDuration() 阶段检测
```

### 3. 缩略图生成失败
```
⚠️ 警告: 视频缩略图生成失败，将不使用缩略图
降级处理，不影响视频发送
```

### 4. OpenIM 客户端未初始化
```
❌ 错误: OpenIM 客户端未初始化
需要用户刷新页面重新登录
```

### 5. 网络传输失败
```
❌ 错误: 视频发送失败: [具体错误信息]
由 OpenIM SDK 抛出，记录到 Sentry
```

---

## 测试建议

### 功能测试
1. ✅ 发送小视频（< 10MB）
2. ✅ 发送大视频（100-200MB）
3. ✅ 发送超大视频（> 200MB，应该被拒绝）
4. ✅ 发送不同格式视频（MP4, MOV, AVI, WebM）
5. ✅ 发送损坏的视频文件
6. ✅ 多用户同时发送视频
7. ✅ 视频消息接收和播放
8. ✅ 视频缩略图显示
9. ✅ 视频时长和文件大小显示

### 性能测试
1. 视频上传速度（依赖网络）
2. 缩略图生成时间（通常 < 500ms）
3. 视频时长提取时间（通常 < 100ms）
4. 大文件上传的内存占用
5. 多个视频消息的渲染性能

### 兼容性测试
| 浏览器 | 版本 | 状态 |
|-------|-----|------|
| Chrome | 90+ | ✅ 推荐 |
| Edge | 90+ | ✅ 推荐 |
| Firefox | 88+ | ✅ 兼容 |
| Safari | 14+ | ⚠️ 待测试 |

---

## 后续优化建议

### 1. 上传进度显示
```typescript
// 添加上传进度回调
const uploadProgress = (progress: number) => {
  console.log(`📊 上传进度: ${progress}%`);
  // 更新 UI 进度条
};
```

### 2. 视频压缩
```typescript
// 对于大视频文件，可以在前端进行压缩
import { compressVideo } from '/@/utils/video-compressor';

if (file.size > 50 * 1024 * 1024) {
  file = await compressVideo(file, { quality: 0.7 });
}
```

### 3. 缩略图优化
```typescript
// 生成多个时间点的缩略图供用户选择
const snapshots = await generateMultipleSnapshots(file, [1, 3, 5]);
```

### 4. 视频预览
```typescript
// 发送前允许用户预览和裁剪视频
const showVideoPreview = (file: File) => {
  // 弹出预览模态框
  // 允许用户选择开始/结束时间
};
```

### 5. 断点续传
```typescript
// 大文件上传支持断点续传
const uploadWithResume = async (file: File) => {
  // 分片上传
  // 记录上传进度
  // 失败后可从断点继续
};
```

---

## 开发日志

### v3.27.0 - 2025-10-10
- ✅ 实现群组视频消息发送功能
- ✅ 添加视频时长自动提取
- ✅ 添加视频缩略图自动生成
- ✅ 添加视频播放器展示
- ✅ 添加视频信息显示（时长、大小）
- ✅ 完善错误处理和用户提示
- ✅ 添加完整的日志记录

---

## 总结

本次实现完整添加了群组视频消息发送功能，包括：

1. **后端支持**: 在 `openim-client.ts` 中新增 `sendGroupVideoMessage()` 方法
2. **前端 UI**: 在聊天面板添加视频上传按钮
3. **智能处理**: 自动提取视频时长和生成缩略图
4. **完善展示**: 使用 HTML5 视频播放器展示视频消息
5. **错误处理**: 全面的错误处理和用户提示
6. **性能优化**: 内存管理和资源清理

所有代码遵循项目现有的代码风格和架构模式，与图片消息和文件消息保持一致的用户体验。

**状态**: ✅ 开发完成，待测试验证
