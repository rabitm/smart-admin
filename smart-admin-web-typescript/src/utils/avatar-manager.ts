/**
 * 用户头像管理器 - 高性能头像缓存和管理
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-24
 * @Copyright 1024创新实验室
 */

interface AvatarCache {
  url?: string;
  canvas?: HTMLCanvasElement;
  timestamp: number;
}

class AvatarManager {
  private cache = new Map<string, AvatarCache>();
  private readonly CACHE_DURATION = 5 * 60 * 1000; // 5分钟缓存
  private readonly MAX_CACHE_SIZE = 100;

  /**
   * 生成用户头像文本
   */
  getAvatarText(name: string): string {
    if (!name) return '?';

    // 支持中文和英文名称
    const cleanName = name.trim();
    if (/[\u4e00-\u9fa5]/.test(cleanName)) {
      // 中文：取最后一个字符
      return cleanName.charAt(cleanName.length - 1);
    } else {
      // 英文：取首字母
      return cleanName.charAt(0).toUpperCase();
    }
  }

  /**
   * 生成头像颜色
   */
  generateAvatarColor(userId: string | number): string {
    const colors = [
      '#1890ff', '#722ed1', '#eb2f96', '#f5222d', '#fa541c',
      '#faad14', '#a0d911', '#52c41a', '#13c2c2', '#2f54eb'
    ];

    const hash = String(userId).split('').reduce((a, b) => {
      a = ((a << 5) - a) + b.charCodeAt(0);
      return a & a;
    }, 0);

    return colors[Math.abs(hash) % colors.length];
  }

  /**
   * 创建Canvas头像
   */
  private createCanvasAvatar(text: string, color: string, size: number = 32): HTMLCanvasElement {
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d')!;

    canvas.width = size;
    canvas.height = size;

    // 绘制背景
    ctx.fillStyle = color;
    ctx.fillRect(0, 0, size, size);

    // 绘制文本
    ctx.fillStyle = '#ffffff';
    ctx.font = `${Math.floor(size * 0.5)}px Arial, sans-serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(text, size / 2, size / 2);

    return canvas;
  }

  /**
   * 获取缓存的头像
   */
  getCachedAvatar(userId: string, name: string, size: number = 32): HTMLCanvasElement | null {
    const key = `${userId}_${size}`;
    const cached = this.cache.get(key);

    if (cached && Date.now() - cached.timestamp < this.CACHE_DURATION) {
      return cached.canvas || null;
    }

    return null;
  }

  /**
   * 设置头像缓存
   */
  setCachedAvatar(userId: string, name: string, canvas: HTMLCanvasElement, size: number = 32): void {
    const key = `${userId}_${size}`;

    // 缓存大小控制
    if (this.cache.size >= this.MAX_CACHE_SIZE) {
      const oldestKey = this.cache.keys().next().value;
      this.cache.delete(oldestKey);
    }

    this.cache.set(key, {
      canvas,
      timestamp: Date.now()
    });
  }

  /**
   * 获取优化的头像（优先使用缓存）
   */
  getOptimizedAvatar(userId: string, name: string, color: string, size: number = 32): HTMLCanvasElement {
    // 尝试从缓存获取
    const cached = this.getCachedAvatar(userId, name, size);
    if (cached) {
      return cached;
    }

    // 创建新头像并缓存
    const text = this.getAvatarText(name);
    const canvas = this.createCanvasAvatar(text, color, size);
    this.setCachedAvatar(userId, name, canvas, size);

    return canvas;
  }

  /**
   * 预加载头像（批量）
   */
  async preloadAvatars(users: Array<{id: string, name: string, color: string}>, size: number = 32): Promise<void> {
    const promises = users.map(user => {
      return new Promise<void>((resolve) => {
        // 使用requestIdleCallback来避免阻塞主线程
        if (window.requestIdleCallback) {
          window.requestIdleCallback(() => {
            this.getOptimizedAvatar(user.id, user.name, user.color, size);
            resolve();
          });
        } else {
          setTimeout(() => {
            this.getOptimizedAvatar(user.id, user.name, user.color, size);
            resolve();
          }, 0);
        }
      });
    });

    await Promise.all(promises);
  }

  /**
   * 清理过期缓存
   */
  cleanExpiredCache(): void {
    const now = Date.now();
    for (const [key, cache] of this.cache.entries()) {
      if (now - cache.timestamp > this.CACHE_DURATION) {
        this.cache.delete(key);
      }
    }
  }

  /**
   * 清空所有缓存
   */
  clearCache(): void {
    this.cache.clear();
  }

  /**
   * 获取缓存统计
   */
  getCacheStats(): {size: number, maxSize: number} {
    return {
      size: this.cache.size,
      maxSize: this.MAX_CACHE_SIZE
    };
  }
}

// 导出单例实例
export const avatarManager = new AvatarManager();

// 定期清理过期缓存
setInterval(() => {
  avatarManager.cleanExpiredCache();
}, 60000); // 每分钟清理一次

export default AvatarManager;