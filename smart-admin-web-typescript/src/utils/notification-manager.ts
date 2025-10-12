/**
 * 消息通知管理器
 *
 * 功能:
 * - 浏览器桌面通知
 * - 声音提示
 * - 通知权限管理
 * - 通知点击处理
 *
 * @author Claude
 * @date 2025-10-11
 */

export interface NotificationOptions {
  senderName: string;
  messageContent: string;
  conversationId: string;
  avatar?: string;
  onClick?: () => void;
}

export class NotificationManager {
  private static instance: NotificationManager;

  // 通知权限状态
  private permissionStatus: NotificationPermission = 'default';

  // 通知音效
  private notificationSound: HTMLAudioElement | null = null;

  // 是否静音
  private isMuted: boolean = false;

  // 防止重复通知的缓存 (conversationId -> 最后通知时间)
  private lastNotificationTime: Map<string, number> = new Map();

  // 通知节流时间 (毫秒)
  private throttleMs: number = 3000;

  private constructor() {
    this.init();
  }

  /**
   * 获取单例实例
   */
  public static getInstance(): NotificationManager {
    if (!NotificationManager.instance) {
      NotificationManager.instance = new NotificationManager();
    }
    return NotificationManager.instance;
  }

  /**
   * 初始化通知管理器
   */
  private async init(): Promise<void> {
    // 检查浏览器是否支持通知
    if (!('Notification' in window)) {
      console.warn('⚠️ [通知管理器] 当前浏览器不支持桌面通知');
      return;
    }

    // 获取当前权限状态
    this.permissionStatus = Notification.permission;
    console.log('📢 [通知管理器] 初始化完成, 权限状态:', this.permissionStatus);

    // 预加载通知音效
    this.preloadNotificationSound();

    // 从 localStorage 读取静音设置
    const savedMuteState = localStorage.getItem('notification_muted');
    if (savedMuteState !== null) {
      this.isMuted = savedMuteState === 'true';
    }
  }

  /**
   * 预加载通知音效
   */
  private preloadNotificationSound(): void {
    try {
      this.notificationSound = new Audio('/sounds/notification.mp3');
      this.notificationSound.volume = 0.5;

      // 监听加载成功事件
      this.notificationSound.addEventListener('canplaythrough', () => {
        console.log('🔊 [通知管理器] 通知音效预加载完成');
      }, { once: true });

      // 监听加载失败事件
      this.notificationSound.addEventListener('error', (e) => {
        console.warn('⚠️ [通知管理器] 音效文件不存在或加载失败,通知功能将继续工作但无声音提示');
        console.warn('💡 [通知管理器] 请参考 public/sounds/README.md 添加音效文件');
        this.notificationSound = null; // 清空引用,避免后续播放失败
      }, { once: true });

      // 预加载音频
      this.notificationSound.load();
    } catch (error) {
      console.warn('⚠️ [通知管理器] 音效系统初始化失败:', error);
      this.notificationSound = null;
    }
  }

  /**
   * 请求通知权限
   */
  public async requestPermission(): Promise<NotificationPermission> {
    if (!('Notification' in window)) {
      console.warn('⚠️ [通知管理器] 浏览器不支持通知');
      return 'denied';
    }

    // 如果已经有权限,直接返回
    if (this.permissionStatus === 'granted') {
      return 'granted';
    }

    try {
      const permission = await Notification.requestPermission();
      this.permissionStatus = permission;

      console.log('📢 [通知管理器] 通知权限请求结果:', permission);

      return permission;
    } catch (error) {
      console.error('❌ [通知管理器] 请求通知权限失败:', error);
      return 'denied';
    }
  }

  /**
   * 检查是否有通知权限
   */
  public hasPermission(): boolean {
    return this.permissionStatus === 'granted';
  }

  /**
   * 获取权限状态
   */
  public getPermissionStatus(): NotificationPermission {
    return this.permissionStatus;
  }

  /**
   * 发送消息通知
   */
  public async sendMessageNotification(options: NotificationOptions): Promise<void> {
    const { senderName, messageContent, conversationId, avatar, onClick } = options;

    console.log('🔔 [通知管理器] 收到通知请求:', {
      senderName,
      messageContent: this.truncateMessage(messageContent, 30),
      conversationId,
      pageVisible: document.visibilityState,
      hasFocus: document.hasFocus(),
      hasPermission: this.hasPermission(),
      isMuted: this.isMuted,
    });

    // 检查通知节流
    if (this.isThrottled(conversationId)) {
      console.log('⏱️ [通知管理器] 通知被节流限制, conversationId:', conversationId);
      return;
    }

    // 🔧 优化: 先播放声音,声音不需要通知权限
    console.log('🔊 [通知管理器] 播放通知声音...');
    this.playNotificationSound();
    this.updateThrottleTime(conversationId);

    // 检查页面是否在前台
    if (document.visibilityState === 'visible' && document.hasFocus()) {
      console.log('👁️ [通知管理器] 页面在前台,只播放声音提示,不弹出桌面通知');
      return;
    }

    // 🔧 优化: 没有权限时,先尝试请求权限
    if (!this.hasPermission()) {
      console.log('📢 [通知管理器] 没有通知权限,尝试请求权限...');
      const permission = await this.requestPermission();

      if (permission !== 'granted') {
        console.warn('⚠️ [通知管理器] 用户拒绝通知权限,仅播放声音');
        // 声音已经播放过了,直接返回
        return;
      }
    }

    try {
      // 创建通知
      const notification = new Notification(`${senderName} 发来新消息`, {
        body: this.truncateMessage(messageContent),
        icon: avatar || '/logo.png',
        badge: '/logo.png',
        tag: conversationId, // 使用 conversationId 作为标签,同一会话的通知会替换
        requireInteraction: false, // 自动关闭
        silent: true, // 🔧 修复: 始终静音,因为我们自己控制声音播放
      });

      // 设置点击事件
      notification.onclick = () => {
        console.log('🖱️ [通知管理器] 用户点击通知');

        // 聚焦窗口
        window.focus();

        // 执行自定义回调
        if (onClick) {
          onClick();
        }

        // 关闭通知
        notification.close();
      };

      // 自动关闭通知 (5秒后)
      setTimeout(() => {
        notification.close();
      }, 5000);

      console.log('✅ [通知管理器] 桌面通知发送成功');
    } catch (error) {
      console.error('❌ [通知管理器] 发送通知失败:', error);
      // 声音已经播放过了,不需要重复播放
    }
  }

  /**
   * 播放通知音效
   */
  public playNotificationSound(): void {
    console.log('🔊 [通知管理器] playNotificationSound 被调用');
    console.log('🔍 [通知管理器] 状态检查:', {
      isMuted: this.isMuted,
      soundLoaded: !!this.notificationSound,
      soundSrc: this.notificationSound?.src,
      soundReadyState: this.notificationSound?.readyState,
    });

    if (this.isMuted) {
      console.log('🔇 [通知管理器] 静音模式,跳过音效播放');
      return;
    }

    if (!this.notificationSound) {
      console.warn('⚠️ [通知管理器] 通知音效未加载,音频对象为null');
      console.warn('💡 [通知管理器] 请检查 public/sounds/notification.mp3 文件是否存在');
      return;
    }

    try {
      console.log('🔊 [通知管理器] 准备播放音效...');

      // 重置音频位置并播放
      this.notificationSound.currentTime = 0;

      const playPromise = this.notificationSound.play();

      if (playPromise !== undefined) {
        playPromise
          .then(() => {
            console.log('✅ [通知管理器] 音效播放成功');
          })
          .catch(error => {
            console.warn('⚠️ [通知管理器] 音效播放失败:', error);
            console.warn('💡 [通知管理器] 可能原因:');
            console.warn('   1. 浏览器自动播放策略 - 需要用户先与页面交互');
            console.warn('   2. 音频文件损坏或格式不支持');
            console.warn('   3. 音频文件路径错误');
          });
      }
    } catch (error) {
      console.error('❌ [通知管理器] 播放音效异常:', error);
    }
  }

  /**
   * 设置静音状态
   */
  public setMuted(muted: boolean): void {
    this.isMuted = muted;
    localStorage.setItem('notification_muted', String(muted));
    console.log('🔇 [通知管理器] 静音状态已更新:', muted);
  }

  /**
   * 获取静音状态
   */
  public isMutedState(): boolean {
    return this.isMuted;
  }

  /**
   * 切换静音状态
   */
  public toggleMute(): boolean {
    this.setMuted(!this.isMuted);
    return this.isMuted;
  }

  /**
   * 检查是否在节流时间内
   */
  private isThrottled(conversationId: string): boolean {
    const lastTime = this.lastNotificationTime.get(conversationId);
    if (!lastTime) {
      return false;
    }

    const now = Date.now();
    return now - lastTime < this.throttleMs;
  }

  /**
   * 更新节流时间
   */
  private updateThrottleTime(conversationId: string): void {
    this.lastNotificationTime.set(conversationId, Date.now());
  }

  /**
   * 截断消息内容 (最多显示50个字符)
   */
  private truncateMessage(content: string, maxLength: number = 50): string {
    if (content.length <= maxLength) {
      return content;
    }
    return content.substring(0, maxLength) + '...';
  }

  /**
   * 清除会话的节流缓存
   */
  public clearThrottle(conversationId: string): void {
    this.lastNotificationTime.delete(conversationId);
  }

  /**
   * 清除所有节流缓存
   */
  public clearAllThrottles(): void {
    this.lastNotificationTime.clear();
  }
}

// 导出单例实例
export const notificationManager = NotificationManager.getInstance();
