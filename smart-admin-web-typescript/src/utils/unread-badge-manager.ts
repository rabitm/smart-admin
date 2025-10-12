/**
 * 未读消息徽标管理器
 *
 * 功能:
 * - 浏览器标签页标题未读数显示
 * - Favicon 徽标显示
 * - 全局未读数统计
 *
 * @author Claude
 * @date 2025-10-11
 */

export class UnreadBadgeManager {
  private static instance: UnreadBadgeManager;

  // 原始页面标题
  private originalTitle: string = '';

  // 未读消息数
  private unreadCount: number = 0;

  // 原始 favicon
  private originalFavicon: string = '';

  // 标题闪烁定时器
  private titleBlinkTimer: number | null = null;

  // 标题闪烁状态
  private titleBlinkState: boolean = false;

  private constructor() {
    this.init();
  }

  /**
   * 获取单例实例
   */
  public static getInstance(): UnreadBadgeManager {
    if (!UnreadBadgeManager.instance) {
      UnreadBadgeManager.instance = new UnreadBadgeManager();
    }
    return UnreadBadgeManager.instance;
  }

  /**
   * 初始化徽标管理器
   */
  private init(): void {
    // 保存原始标题
    this.originalTitle = document.title;

    // 保存原始 favicon
    const faviconLink = document.querySelector("link[rel*='icon']") as HTMLLinkElement;
    if (faviconLink) {
      this.originalFavicon = faviconLink.href;
    }

    // 监听页面可见性变化
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') {
        // 页面变为可见时,停止标题闪烁
        this.stopTitleBlink();
      } else {
        // 页面变为隐藏时,如果有未读消息则开始闪烁
        if (this.unreadCount > 0) {
          this.startTitleBlink();
        }
      }
    });

    console.log('🏷️ [徽标管理器] 初始化完成');
  }

  /**
   * 设置未读消息数
   */
  public setUnreadCount(count: number): void {
    const oldCount = this.unreadCount;
    this.unreadCount = Math.max(0, count);

    console.log('🏷️ [徽标管理器] 未读数更新:', {
      oldCount,
      newCount: this.unreadCount,
    });

    // 更新标题
    this.updateTitle();

    // 更新 favicon
    this.updateFavicon();

    // 如果页面不可见且有未读消息,开始闪烁
    if (document.visibilityState !== 'visible' && this.unreadCount > 0) {
      this.startTitleBlink();
    } else {
      this.stopTitleBlink();
    }
  }

  /**
   * 增加未读消息数
   */
  public incrementUnreadCount(delta: number = 1): void {
    this.setUnreadCount(this.unreadCount + delta);
  }

  /**
   * 减少未读消息数
   */
  public decrementUnreadCount(delta: number = 1): void {
    this.setUnreadCount(this.unreadCount - delta);
  }

  /**
   * 清零未读消息数
   */
  public clearUnreadCount(): void {
    this.setUnreadCount(0);
  }

  /**
   * 获取当前未读消息数
   */
  public getUnreadCount(): number {
    return this.unreadCount;
  }

  /**
   * 更新页面标题
   */
  private updateTitle(): void {
    if (this.unreadCount > 0) {
      const displayCount = this.unreadCount > 99 ? '99+' : String(this.unreadCount);
      document.title = `(${displayCount}) ${this.originalTitle}`;
    } else {
      document.title = this.originalTitle;
    }
  }

  /**
   * 更新 favicon
   */
  private updateFavicon(): void {
    if (this.unreadCount > 0) {
      this.drawFaviconWithBadge(this.unreadCount);
    } else {
      this.restoreFavicon();
    }
  }

  /**
   * 绘制带徽标的 favicon
   */
  private drawFaviconWithBadge(count: number): void {
    try {
      // 创建 canvas
      const canvas = document.createElement('canvas');
      canvas.width = 32;
      canvas.height = 32;
      const ctx = canvas.getContext('2d');

      if (!ctx) {
        console.warn('⚠️ [徽标管理器] 无法获取 canvas 上下文');
        return;
      }

      // 加载原始 favicon
      const img = new Image();
      img.crossOrigin = 'anonymous';

      img.onload = () => {
        // 绘制原始图标
        ctx.drawImage(img, 0, 0, 32, 32);

        // 绘制红色徽标背景
        ctx.fillStyle = '#ff4d4f';
        ctx.beginPath();
        ctx.arc(24, 8, 8, 0, 2 * Math.PI);
        ctx.fill();

        // 绘制未读数文字
        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold 12px Arial';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';

        const displayText = count > 9 ? '9+' : String(count);
        ctx.fillText(displayText, 24, 8);

        // 更新 favicon
        this.setFavicon(canvas.toDataURL('image/png'));
      };

      img.onerror = () => {
        console.warn('⚠️ [徽标管理器] 加载原始 favicon 失败,使用纯色徽标');
        this.drawSimpleBadge(ctx, count);
      };

      img.src = this.originalFavicon || '/logo.png';
    } catch (error) {
      console.error('❌ [徽标管理器] 绘制 favicon 徽标失败:', error);
    }
  }

  /**
   * 绘制简单的徽标 (当原始 favicon 加载失败时)
   */
  private drawSimpleBadge(ctx: CanvasRenderingContext2D, count: number): void {
    // 清空 canvas
    ctx.clearRect(0, 0, 32, 32);

    // 绘制红色圆形背景
    ctx.fillStyle = '#ff4d4f';
    ctx.beginPath();
    ctx.arc(16, 16, 16, 0, 2 * Math.PI);
    ctx.fill();

    // 绘制未读数文字
    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 16px Arial';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';

    const displayText = count > 9 ? '9+' : String(count);
    ctx.fillText(displayText, 16, 16);

    // 更新 favicon
    const canvas = ctx.canvas;
    this.setFavicon(canvas.toDataURL('image/png'));
  }

  /**
   * 设置 favicon
   */
  private setFavicon(href: string): void {
    let link = document.querySelector("link[rel*='icon']") as HTMLLinkElement;

    if (!link) {
      link = document.createElement('link');
      link.rel = 'icon';
      document.head.appendChild(link);
    }

    link.href = href;
  }

  /**
   * 恢复原始 favicon
   */
  private restoreFavicon(): void {
    if (this.originalFavicon) {
      this.setFavicon(this.originalFavicon);
    }
  }

  /**
   * 开始标题闪烁
   */
  private startTitleBlink(): void {
    // 如果已经在闪烁,不重复启动
    if (this.titleBlinkTimer !== null) {
      return;
    }

    console.log('✨ [徽标管理器] 开始标题闪烁');

    const displayCount = this.unreadCount > 99 ? '99+' : String(this.unreadCount);

    this.titleBlinkTimer = window.setInterval(() => {
      if (this.titleBlinkState) {
        document.title = this.originalTitle;
      } else {
        document.title = `【新消息】(${displayCount}) ${this.originalTitle}`;
      }
      this.titleBlinkState = !this.titleBlinkState;
    }, 1000);
  }

  /**
   * 停止标题闪烁
   */
  private stopTitleBlink(): void {
    if (this.titleBlinkTimer !== null) {
      clearInterval(this.titleBlinkTimer);
      this.titleBlinkTimer = null;
      this.titleBlinkState = false;

      // 恢复正常标题
      this.updateTitle();

      console.log('⏹️ [徽标管理器] 停止标题闪烁');
    }
  }

  /**
   * 重置徽标管理器
   */
  public reset(): void {
    this.stopTitleBlink();
    this.clearUnreadCount();
    this.restoreFavicon();
    document.title = this.originalTitle;

    console.log('🔄 [徽标管理器] 已重置');
  }
}

// 导出单例实例
export const unreadBadgeManager = UnreadBadgeManager.getInstance();
