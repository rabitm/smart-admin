/**
 * IM模块日志工具
 * 用于控制IM模块的日志输出,生产环境默认关闭
 */

// 日志级别
export enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3,
  NONE = 4,
}

class IMLogger {
  private level: LogLevel = LogLevel.NONE; // 默认关闭所有日志
  private isDev: boolean = import.meta.env.DEV;

  constructor() {
    // 开发环境下可以通过 localStorage 启用调试
    if (this.isDev && localStorage.getItem('IM_DEBUG') === 'true') {
      this.level = LogLevel.DEBUG;
    }
  }

  /**
   * 设置日志级别
   */
  setLevel(level: LogLevel) {
    this.level = level;
  }

  /**
   * 启用调试模式
   */
  enableDebug() {
    this.level = LogLevel.DEBUG;
    localStorage.setItem('IM_DEBUG', 'true');
  }

  /**
   * 禁用调试模式
   */
  disableDebug() {
    this.level = LogLevel.NONE;
    localStorage.removeItem('IM_DEBUG');
  }

  /**
   * DEBUG级别日志 - 详细的调试信息
   */
  debug(message: string, ...args: any[]) {
    if (this.level <= LogLevel.DEBUG) {
      console.log(`[IM-DEBUG] ${message}`, ...args);
    }
  }

  /**
   * INFO级别日志 - 一般信息
   */
  info(message: string, ...args: any[]) {
    if (this.level <= LogLevel.INFO) {
      console.log(`[IM-INFO] ${message}`, ...args);
    }
  }

  /**
   * WARN级别日志 - 警告信息
   */
  warn(message: string, ...args: any[]) {
    if (this.level <= LogLevel.WARN) {
      console.warn(`[IM-WARN] ${message}`, ...args);
    }
  }

  /**
   * ERROR级别日志 - 错误信息(总是显示)
   */
  error(message: string, ...args: any[]) {
    if (this.level <= LogLevel.ERROR) {
      console.error(`[IM-ERROR] ${message}`, ...args);
    }
  }
}

// 导出单例
export const imLogger = new IMLogger();

// 在浏览器控制台提供全局访问
if (typeof window !== 'undefined') {
  (window as any).IMLogger = {
    enable: () => imLogger.enableDebug(),
    disable: () => imLogger.disableDebug(),
    setLevel: (level: LogLevel) => imLogger.setLevel(level),
  };
}
