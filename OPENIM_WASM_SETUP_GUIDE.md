# OpenIM WASM SDK 配置指南

## 问题描述

使用 `@openim/wasm-client-sdk` 时可能遇到以下错误：

```
Uncaught (in promise) ReferenceError: Go is not defined
    at initializeWasm (index.es.js:953:5)
```

**原因**: OpenIM WASM SDK 依赖 Go WebAssembly 运行时，需要在页面加载时引入 `wasm_exec.js` 脚本。

---

## 解决方案

### 1. 复制必要的 WASM 文件到 public 目录

OpenIM SDK 需要以下文件：

```bash
# 从 node_modules 复制到 public 目录
cp node_modules/@openim/wasm-client-sdk/assets/wasm_exec.js public/
cp node_modules/@openim/wasm-client-sdk/assets/openIM.wasm public/
cp node_modules/@openim/wasm-client-sdk/assets/sql-wasm.wasm public/
```

**文件说明**:
- `wasm_exec.js` (17KB) - Go WebAssembly 运行时，定义全局 `Go` 类
- `openIM.wasm` (35MB) - OpenIM 核心 WASM 模块
- `sql-wasm.wasm` (1.1MB) - SQLite WASM 模块（用于本地存储）

### 2. 在 index.html 中引入 wasm_exec.js

在 `<head>` 标签中添加脚本引用（**必须在主模块之前加载**）：

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <link rel="icon" href="/favicon.ico">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title> %VITE_APP_TITLE%</title>

  <!-- ✅ OpenIM WASM SDK requires Go WebAssembly runtime -->
  <script src="/wasm_exec.js"></script>
</head>
<body>
  <div id="app"></div>
  <script type="module" src="/src/main.ts"></script>
</body>
</html>
```

### 3. 验证配置

重新启动开发服务器：

```bash
npm run dev
```

在浏览器控制台检查：

```javascript
// 应该能看到 Go 类已定义
console.log(typeof Go); // "function"

// 检查 WASM 文件是否可访问
fetch('/openIM.wasm').then(res => console.log('WASM loaded:', res.ok));
```

---

## 技术细节

### SDK 初始化流程

```typescript
// 1. 前端调用 getSDK()
import { getSDK } from '@openim/wasm-client-sdk';
const sdk = await getSDK();

// 2. SDK 内部执行初始化
async function initializeWasm(url = '/openIM.wasm') {
  // ⚠️ 这里需要 Go 类，由 wasm_exec.js 提供
  go = new Go();  // ← 如果没有 wasm_exec.js 会报错

  // 加载 WASM 模块
  const wasm = await WebAssembly.instantiateStreaming(
    fetch(url),
    go.importObject
  );

  // 启动 Go 运行时
  go.run(wasm.instance);
}

// 3. 初始化完成后可以使用 SDK
await sdk.login({ ... });
```

### 文件大小优化

由于 `openIM.wasm` 较大（35MB），建议：

1. **启用 Gzip 压缩** - 可减少到约 10MB
2. **使用 CDN 缓存** - 用户只需下载一次
3. **懒加载** - 只在需要 IM 功能时加载

```typescript
// 懒加载示例
async function initIM() {
  // 动态导入 SDK（Vite 会自动代码分割）
  const { openIMClient } = await import('/@/utils/openim-client');
  await openIMClient.loginWithSmartAdmin();
}
```

---

## 常见问题

### Q1: 为什么不能通过 npm 导入 wasm_exec.js？

**A**: `wasm_exec.js` 需要定义全局 `Go` 类，必须在主模块之前加载。通过 `<script>` 标签加载可以确保正确的加载顺序。

### Q2: 能否使用 import.meta.url 动态加载 WASM？

**A**: 可以，但需要修改 SDK 配置：

```typescript
import { getSDK } from '@openim/wasm-client-sdk';

const sdk = await getSDK({
  coreWasmPath: new URL('/openIM.wasm', import.meta.url).href,
  sqlWasmPath: new URL('/sql-wasm.wasm', import.meta.url).href,
});
```

### Q3: 开发环境正常，生产环境报错？

**A**: 检查构建配置，确保 WASM 文件被正确复制到 dist 目录：

```javascript
// vite.config.ts
export default {
  publicDir: 'public', // ✅ Vite 会自动复制 public 目录到 dist
}
```

### Q4: WASM 文件 404 错误？

**A**: 检查文件路径和服务器配置：

```bash
# 验证文件存在
ls -lh public/openIM.wasm
ls -lh dist/openIM.wasm  # 构建后

# 检查服务器 MIME 类型配置
# 确保 .wasm 文件使用 application/wasm
```

---

## 自动化脚本

为了简化部署流程，可以创建一个 postinstall 脚本：

```json
// package.json
{
  "scripts": {
    "postinstall": "node scripts/copy-openim-assets.js"
  }
}
```

```javascript
// scripts/copy-openim-assets.js
const fs = require('fs');
const path = require('path');

const assets = [
  'wasm_exec.js',
  'openIM.wasm',
  'sql-wasm.wasm'
];

const srcDir = 'node_modules/@openim/wasm-client-sdk/assets';
const destDir = 'public';

assets.forEach(file => {
  const src = path.join(srcDir, file);
  const dest = path.join(destDir, file);

  if (fs.existsSync(src)) {
    fs.copyFileSync(src, dest);
    console.log(`✅ Copied ${file}`);
  } else {
    console.warn(`⚠️ File not found: ${file}`);
  }
});
```

---

## 性能优化建议

### 1. 使用 Service Worker 缓存 WASM

```javascript
// service-worker.js
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open('openim-wasm-v1').then((cache) => {
      return cache.addAll([
        '/openIM.wasm',
        '/sql-wasm.wasm',
        '/wasm_exec.js'
      ]);
    })
  );
});
```

### 2. 预加载 WASM 文件

```html
<!-- index.html -->
<head>
  <!-- 预加载关键资源 -->
  <link rel="preload" href="/wasm_exec.js" as="script">
  <link rel="preload" href="/openIM.wasm" as="fetch" crossorigin>
</head>
```

### 3. 显示加载进度

```typescript
// 监听 WASM 加载进度
const response = await fetch('/openIM.wasm');
const reader = response.body.getReader();
const contentLength = +response.headers.get('Content-Length');

let receivedLength = 0;
const chunks = [];

while (true) {
  const { done, value } = await reader.read();

  if (done) break;

  chunks.push(value);
  receivedLength += value.length;

  const progress = (receivedLength / contentLength) * 100;
  console.log(`Loading WASM: ${progress.toFixed(2)}%`);
}
```

---

## 总结

✅ **已完成的配置**:
- [x] 复制 `wasm_exec.js` 到 `public/`
- [x] 复制 `openIM.wasm` 到 `public/`
- [x] 复制 `sql-wasm.wasm` 到 `public/`
- [x] 在 `index.html` 中引入 `wasm_exec.js`

🎯 **下一步**:
- 重启开发服务器
- 测试登录流程
- 验证 OpenIM 连接

---

**版本**: v1.0
**日期**: 2025-10-10
**状态**: ✅ 已完成
