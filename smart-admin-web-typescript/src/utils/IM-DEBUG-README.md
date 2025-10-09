# IM模块调试指南

## 问题解决

✅ **已修复的问题:**
1. `ImMessageList` 组件缺少 `currentUserID` 属性警告
2. 控制台输出过多日志(每次操作1000+行)

## 当前状态

默认情况下,IM模块的调试日志已全部关闭,只显示错误信息。控制台输出将大幅减少。

## 如何启用调试模式

如果需要调试IM功能,在浏览器控制台执行:

```javascript
// 启用IM调试日志
IMLogger.enable()

// 禁用IM调试日志
IMLogger.disable()
```

## 日志级别

- **DEBUG** - 详细的调试信息(默认关闭)
- **INFO** - 一般信息(默认关闭)
- **WARN** - 警告信息(默认关闭)
- **ERROR** - 错误信息(总是显示)

## 开发说明

在开发过程中,如果需要添加新的日志,请使用 `imLogger` 而不是 `console.log`:

```typescript
import { imLogger } from '/@/utils/im-logger';

// 调试信息
imLogger.debug('这是调试信息', data);

// 一般信息
imLogger.info('这是一般信息', data);

// 警告
imLogger.warn('这是警告', data);

// 错误(总是显示)
imLogger.error('这是错误', error);
```

这样可以确保生产环境不会产生过多的控制台输出。
