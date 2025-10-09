# IM模块日志清理说明

## 问题
im-chat-panel.vue 文件中包含大量的 console.log 调用,导致控制台输出过多(每次操作一两千行)。

## 解决方案
已创建 `im-logger.ts` 工具,提供分级日志功能,默认关闭所有调试日志。

## 如何启用调试模式

在浏览器控制台中执行:
```javascript
// 启用IM调试日志
IMLogger.enable()

// 禁用IM调试日志
IMLogger.disable()
```

## 需要替换的日志

所有的 `console.log`、`console.warn`、`console.error` 应替换为:
- `imLogger.debug()` - 调试信息(默认不显示)
- `imLogger.info()` - 一般信息(默认不显示)
- `imLogger.warn()` - 警告信息(默认不显示)
- `imLogger.error()` - 错误信息(默认显示)

## 手动替换命令

由于文件较大,建议使用编辑器的查找替换功能:

1. 打开 `im-chat-panel.vue`
2. 查找: `console.log\('📱 \[聊天面板\] ([^']+)'`
   替换为: `imLogger.debug('$1'`

3. 查找: `console.log\('✅ \[聊天面板\] ([^']+)'`
   替换为: `imLogger.debug('$1'`

4. 查找: `console.log\('⏳ \[聊天面板\] ([^']+)'`
   替换为: `imLogger.debug('$1'`

5. 查找: `console.log\('🔄 \[聊天面板\] ([^']+)'`
   替换为: `imLogger.debug('$1'`

6. 查找: `console.warn\('⚠️ \[聊天面板\] ([^']+)'`
   替换为: `imLogger.warn('$1'`

7. 查找: `console.error\('❌ \[聊天面板\] ([^']+)'`
   替换为: `imLogger.error('$1'`

完成后,控制台将只显示错误信息,极大减少日志输出。
