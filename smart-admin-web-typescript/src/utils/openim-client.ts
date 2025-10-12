/**
 * OpenIM Web SDK 客户端封装
 * 基于官方 openim-electron-demo 最佳实践
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 * @Copyright 1024创新实验室
 */

import { getSDK, type CbEvents } from '@openim/wasm-client-sdk';
import type { MessageItem, ConversationItem, GroupItem, GroupMemberItem } from '@openim/wasm-client-sdk';
import { smartSentry } from '/@/lib/smart-sentry';
import { imTokenApi } from '/@/api/business/oa/im-token-api';

// OpenIM配置
const OPENIM_CONFIG = {
  // WebSocket地址 - 从环境变量或配置中获取
  wsAddr: import.meta.env.VITE_OPENIM_WS_URL || 'ws://localhost:10001',
  // API地址
  apiAddr: import.meta.env.VITE_OPENIM_API_URL || 'http://localhost:10002',
  // 平台ID (5-Web)
  platformID: 5,
  // 数据存储目录
  dataDir: './openim-data',
  // 重连配置
  reconnectConfig: {
    enabled: true,
    maxRetries: 5,
    retryInterval: 3000, // 3秒
  },
};

/**
 * OpenIM客户端类
 * 参考官方示例实现，增强了群组管理、多消息类型、错误处理和重连机制
 */
class OpenIMClient {
  private sdk: any;
  private isInitialized: boolean = false;
  private isLoggedIn: boolean = false;
  private currentUserId: string = '';
  private messageListeners: Map<string, Function[]> = new Map();
  private conversationListeners: Function[] = [];
  private groupListeners: Function[] = [];

  // 重连相关
  private reconnectAttempts: number = 0;
  private reconnectTimer: number | null = null;
  private connectionState: 'disconnected' | 'connecting' | 'connected' = 'disconnected';

  // Token 自动刷新相关
  private tokenRefreshTimer: number | null = null;

  constructor() {
    console.log('📱 [OpenIM] 初始化OpenIM客户端');
  }

  /**
   * 初始化SDK
   */
  async initialize(): Promise<void> {
    if (this.isInitialized) {
      console.log('⚠️ [OpenIM] SDK已初始化');
      return;
    }

    try {
      console.log('🔌 [OpenIM] SDK初始化中...');

      // 初始化SDK - 包含WASM加载
      this.sdk = await getSDK();

      // 设置事件监听器
      this.setupEventListeners();

      this.isInitialized = true;
      console.log('✅ [OpenIM] SDK初始化成功');
    } catch (error) {
      console.error('❌ [OpenIM] SDK初始化失败:', error);
      smartSentry.captureError(error, { tags: { module: 'OpenIM', action: 'initialize' } });
      throw error;
    }
  }

  /**
   * 登录OpenIM（新架构）
   *
   * 流程:
   * 1. 从后端获取 OpenIM Token
   * 2. 使用 Token 登录 OpenIM
   * 3. 建立 WebSocket 连接
   *
   * @returns 登录成功后的用户信息
   */
  async loginWithSmartAdmin(): Promise<void> {
    if (!this.isInitialized) {
      await this.initialize();
    }

    try {
      console.log('🔐 [OpenIM] 开始从后端获取 Token...');

      // 1. 从后端获取 OpenIM Token
      const tokenResponse = await imTokenApi.getToken();

      console.log('🔍 [DEBUG] 后端返回的完整响应:', tokenResponse);
      console.log('🔍 [DEBUG] tokenResponse.openimUserId:', tokenResponse.openimUserId);
      console.log('🔍 [DEBUG] tokenResponse.token:', tokenResponse.token);
      console.log('🔍 [DEBUG] tokenResponse.expireTime:', tokenResponse.expireTime);

      console.log('✅ [OpenIM] 成功获取 Token, UserID:', tokenResponse.openimUserId);

      // 2. 检查是否已登录同一用户
      if (this.isLoggedIn && this.currentUserId === tokenResponse.openimUserId) {
        console.log('⚠️ [OpenIM] 用户已登录:', tokenResponse.openimUserId);
        return;
      }

      // 3. 使用 Token 登录 OpenIM
      console.log('📤 [OpenIM] 使用 Token 登录 OpenIM...');

      await this.sdk.login({
        userID: tokenResponse.openimUserId,
        token: tokenResponse.token,
        platformID: OPENIM_CONFIG.platformID,
        apiAddr: OPENIM_CONFIG.apiAddr,
        wsAddr: OPENIM_CONFIG.wsAddr,
        dataDir: OPENIM_CONFIG.dataDir,
      });

      this.isLoggedIn = true;
      this.currentUserId = tokenResponse.openimUserId;

      console.log('✅ [OpenIM] 登录成功, UserID:', tokenResponse.openimUserId);

      // 4. 设置 Token 自动刷新（提前 5 分钟刷新）
      // ⚠️ 临时禁用自动刷新,Token有效期为90天,无需频繁刷新
      // this.scheduleTokenRefresh(tokenResponse.expireTime);

    } catch (error) {
      console.error('❌ [OpenIM] 登录失败:', error);
      smartSentry.captureError(error, { tags: { module: 'OpenIM', action: 'loginWithSmartAdmin' } });
      throw error;
    }
  }

  /**
   * 登录OpenIM（直接使用 Token，保留用于兼容性）
   * @param userID OpenIM用户ID
   * @param token 用户Token
   * @deprecated 使用 loginWithSmartAdmin() 代替
   */
  async login(userID: string, token: string): Promise<void> {
    if (!this.isInitialized) {
      await this.initialize();
    }

    if (this.isLoggedIn && this.currentUserId === userID) {
      console.log('⚠️ [OpenIM] 用户已登录:', userID);
      return;
    }

    try {
      console.log('📤 [OpenIM] 开始登录, UserID:', userID);

      await this.sdk.login({
        userID,
        token,
        platformID: OPENIM_CONFIG.platformID,
        apiAddr: OPENIM_CONFIG.apiAddr,
        wsAddr: OPENIM_CONFIG.wsAddr,
        dataDir: OPENIM_CONFIG.dataDir,
      });

      this.isLoggedIn = true;
      this.currentUserId = userID;

      console.log('✅ [OpenIM] 登录成功, UserID:', userID);
    } catch (error) {
      console.error('❌ [OpenIM] 登录失败:', error);
      smartSentry.captureError(error, { tags: { module: 'OpenIM', action: 'login' }, extra: { userID } });
      throw error;
    }
  }

  /**
   * 登出
   */
  async logout(): Promise<void> {
    if (!this.isLoggedIn) {
      return;
    }

    try {
      // 清除 Token 刷新定时器
      this.clearTokenRefreshTimer();

      await this.sdk.logout();
      this.isLoggedIn = false;
      this.currentUserId = '';
      this.messageListeners.clear();
      this.conversationListeners = [];

      console.log('✅ [OpenIM] 登出成功');
    } catch (error) {
      console.error('❌ [OpenIM] 登出失败:', error);
      throw error;
    }
  }

  /**
   * 设置 Token 自动刷新定时器
   * 提前 5 分钟刷新 Token
   *
   * @param expireTime Token 过期时间（Unix时间戳，秒）
   */
  private scheduleTokenRefresh(expireTime: number): void {
    // 清除旧的定时器
    this.clearTokenRefreshTimer();

    // 🔍 DEBUG: 添加详细调试信息
    const now = Math.floor(Date.now() / 1000);
    const refreshTime = expireTime - 300;
    const delay = (refreshTime - now) * 1000;

    console.log(`🔍 [DEBUG] expireTime: ${expireTime}, now: ${now}, refreshTime: ${refreshTime}, delay: ${delay}ms`);

    if (delay <= 0) {
      console.warn(`⚠️ [OpenIM] Token 即将过期或已过期，立即刷新 (expireTime: ${expireTime}, now: ${now})`);
      this.refreshTokenNow();
      return;
    }

    // ✅ 防止负数或过小的delay
    if (delay < 60000) { // 小于1分钟
      console.warn(`⚠️ [OpenIM] delay太小 (${delay}ms)，调整为1分钟`);
      this.tokenRefreshTimer = window.setTimeout(() => {
        this.refreshTokenNow();
      }, 60000);
      return;
    }

    console.log(`⏰ [OpenIM] Token 将在 ${Math.floor(delay / 1000 / 60)} 分钟后刷新`);

    this.tokenRefreshTimer = window.setTimeout(() => {
      this.refreshTokenNow();
    }, delay);
  }

  /**
   * 清除 Token 刷新定时器
   */
  private clearTokenRefreshTimer(): void {
    if (this.tokenRefreshTimer) {
      clearTimeout(this.tokenRefreshTimer);
      this.tokenRefreshTimer = null;
    }
  }

  /**
   * 立即刷新 Token
   */
  private async refreshTokenNow(): Promise<void> {
    try {
      console.log('🔄 [OpenIM] 开始刷新 Token...');

      const tokenResponse = await imTokenApi.refreshToken();

      console.log('✅ [OpenIM] Token 刷新成功');

      // 设置下一次刷新
      this.scheduleTokenRefresh(tokenResponse.expireTime);

    } catch (error) {
      console.error('❌ [OpenIM] Token 刷新失败:', error);
      smartSentry.captureError(error, { tags: { module: 'OpenIM', action: 'refreshToken' } });

      // 刷新失败，3 分钟后重试
      this.tokenRefreshTimer = window.setTimeout(() => {
        this.refreshTokenNow();
      }, 180000);
    }
  }

  /**
   * 设置事件监听器
   * 参考官方示例，增强了连接状态管理和重连机制
   */
  private setupEventListeners(): void {
    // 监听连接状态
    this.sdk.on('onConnecting', () => {
      console.log('🔄 [OpenIM] 正在连接...');
      this.connectionState = 'connecting';
    });

    this.sdk.on('onConnectSuccess', async () => {
      console.log('✅ [OpenIM] 连接成功');
      this.connectionState = 'connected';
      this.reconnectAttempts = 0; // 重置重连计数
      this.clearReconnectTimer();

      // 🔧 关键修复: 连接成功后立即同步所有会话列表
      // 这会触发SDK创建会话记录到本地数据库
      try {
        console.log('🔄 [OpenIM] 连接成功后同步会话列表...');
        await this.sdk.getAllConversationList();
        console.log('✅ [OpenIM] 会话列表同步完成');
      } catch (error) {
        console.warn('⚠️ [OpenIM] 连接后同步会话列表失败:', error);
      }
    });

    this.sdk.on('onConnectFailed', ({ errCode, errMsg }: any) => {
      console.error('❌ [OpenIM] 连接失败:', errCode, errMsg);
      this.connectionState = 'disconnected';
      this.handleConnectionFailure(errCode, errMsg);
    });

    // 监听踢出登录
    this.sdk.on('onKickedOffline', () => {
      console.warn('⚠️ [OpenIM] 账号在其他设备登录，当前连接已断开');
      this.isLoggedIn = false;
      this.connectionState = 'disconnected';
    });

    // 监听用户Token过期
    this.sdk.on('onUserTokenExpired', () => {
      console.warn('⚠️ [OpenIM] Token已过期，需要重新登录');
      this.isLoggedIn = false;
      this.connectionState = 'disconnected';
    });

    // 监听新消息 (SDK事件名: OnRecvNewMessages - 注意是复数且首字母大写)
    this.sdk.on('OnRecvNewMessages', (data: { data: MessageItem[] }) => {
      console.log('📨 [OpenIM] 收到新消息事件, 消息数:', data.data?.length || 0);

      // SDK返回的是消息数组，需要遍历通知
      const messages = data.data || [];
      messages.forEach((message: MessageItem) => {
        console.log('📨 [OpenIM] 处理消息:', message.clientMsgID);
        this.notifyMessageListeners(message);
      });
    });

    // 监听离线消息
    this.sdk.on('onRecvOfflineNewMessage', (data: MessageItem) => {
      console.log('📨 [OpenIM] 收到离线消息:', data);
      this.notifyMessageListeners(data);
    });

    // 监听会话变化
    this.sdk.on('onConversationChanged', (data: ConversationItem[]) => {
      console.log('🔄 [OpenIM] 会话变化:', data);
      this.conversationListeners.forEach(listener => listener(data));
    });

    this.sdk.on('onNewConversation', (data: ConversationItem[]) => {
      console.log('➕ [OpenIM] 新会话创建:', data);
      console.log('💡 [OpenIM] 提示: 新会话创建后,ChatPanel应该重新加载历史消息');
      this.conversationListeners.forEach(listener => listener(data));
    });

    // 监听群组变化
    this.sdk.on('onJoinedGroupAdded', (data: GroupItem) => {
      console.log('👥 [OpenIM] 加入新群组:', data);
      this.groupListeners.forEach(listener => listener({ type: 'added', data }));
    });

    this.sdk.on('onJoinedGroupDeleted', (data: GroupItem) => {
      console.log('👥 [OpenIM] 退出群组:', data);
      this.groupListeners.forEach(listener => listener({ type: 'deleted', data }));
    });

    this.sdk.on('onGroupMemberAdded', (data: GroupMemberItem) => {
      console.log('👤 [OpenIM] 群成员加入:', data);
      this.groupListeners.forEach(listener => listener({ type: 'member_added', data }));
    });

    this.sdk.on('onGroupMemberDeleted', (data: GroupMemberItem) => {
      console.log('👤 [OpenIM] 群成员退出:', data);
      this.groupListeners.forEach(listener => listener({ type: 'member_deleted', data }));
    });

    this.sdk.on('onGroupInfoChanged', (data: GroupItem) => {
      console.log('ℹ️ [OpenIM] 群信息变更:', data);
      this.groupListeners.forEach(listener => listener({ type: 'info_changed', data }));
    });

    // 监听同步完成事件 - 关键: 登录后SDK会同步数据
    this.sdk.on('onSyncServerStart', () => {
      console.log('🔄 [OpenIM] 开始同步服务器数据...');
    });

    this.sdk.on('onSyncServerFinish', () => {
      console.log('✅ [OpenIM] 服务器数据同步完成');
    });

    this.sdk.on('onSyncServerFailed', () => {
      console.warn('⚠️ [OpenIM] 服务器数据同步失败');
    });
  }

  /**
   * 处理连接失败
   */
  private handleConnectionFailure(errCode: number, errMsg: string): void {
    if (!OPENIM_CONFIG.reconnectConfig.enabled) {
      return;
    }

    if (this.reconnectAttempts >= OPENIM_CONFIG.reconnectConfig.maxRetries) {
      console.error('❌ [OpenIM] 已达到最大重连次数，停止重连');
      smartSentry.captureError(new Error('OpenIM连接失败'), {
        tags: { module: 'OpenIM', action: 'reconnect' },
        extra: { errCode, errMsg, attempts: this.reconnectAttempts },
      });
      return;
    }

    this.reconnectAttempts++;
    console.log(`🔄 [OpenIM] 准备第 ${this.reconnectAttempts} 次重连...`);

    this.clearReconnectTimer();
    this.reconnectTimer = window.setTimeout(() => {
      this.attemptReconnect();
    }, OPENIM_CONFIG.reconnectConfig.retryInterval);
  }

  /**
   * 尝试重连
   */
  private async attemptReconnect(): Promise<void> {
    if (!this.currentUserId || this.connectionState === 'connected') {
      return;
    }

    try {
      console.log('🔄 [OpenIM] 尝试重新连接...');
      // SDK会自动尝试重连，这里只需要监听状态变化
    } catch (error) {
      console.error('❌ [OpenIM] 重连失败:', error);
    }
  }

  /**
   * 清除重连定时器
   */
  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  /**
   * 发送文本消息
   * @param conversationID 会话ID
   * @param text 消息文本
   */
  async sendTextMessage(conversationID: string, text: string): Promise<MessageItem> {
    if (!this.isLoggedIn) {
      throw new Error('未登录OpenIM');
    }

    try {
      const message = await this.sdk.createTextMessage(text);

      const result = await this.sdk.sendMessage({
        message,
        conversationID,
      });

      console.log('✅ [OpenIM] 消息发送成功');
      return result;
    } catch (error) {
      console.error('❌ [OpenIM] 消息发送失败:', error);
      smartSentry.captureError(error, { tags: { module: 'OpenIM', action: 'sendTextMessage' } });
      throw error;
    }
  }

  /**
   * 发送群组文本消息
   * @param groupID 群组ID
   * @param text 消息文本
   */
  async sendGroupTextMessage(groupID: string, text: string): Promise<MessageItem> {
    if (!this.isLoggedIn) {
      throw new Error('未登录OpenIM');
    }

    try {
      console.log('📤 [OpenIM] 发送群组文本消息, groupID:', groupID);

      // 1. 创建文本消息
      const messageResponse = await this.sdk.createTextMessage(text);

      console.log('✅ [OpenIM] 消息创建成功:', messageResponse);

      // 提取实际的消息对象
      const message = messageResponse.data || messageResponse;

      console.log('📝 [OpenIM] 提取的消息对象:', message);

      // 2. 发送消息（直接使用消息对象和群组ID）
      const result = await this.sdk.sendMessage({
        recvID: '', // 群聊时 recvID 为空
        groupID: groupID, // 指定群组ID
        message: message, // 纯粹的消息对象
      });

      console.log('✅ [OpenIM] 群组消息发送成功:', result);
      return result;
    } catch (error) {
      console.error('❌ [OpenIM] 发送群消息失败:', error);
      throw error;
    }
  }

  /**
   * 发送带 @ 提及的群组文本消息
   *
   * ⚠️ WORKAROUND: OpenIM SDK v3.8.3-patch.10 的 createTextAtMessage() 存在严重Bug:
   * - 生成的消息 content 字段为字符串 "null"
   * - 导致接收端解码后内容为空
   *
   * 解决方案: 使用 createTextMessage() + 手动构造 @ 信息
   *
   * @param groupID 群组ID
   * @param text 消息文本
   * @param atUserIDList 被 @ 的用户ID列表 (传入 ['all'] 表示 @所有人)
   */
  async sendGroupTextMessageWithMention(
    groupID: string,
    text: string,
    atUserIDList: string[]
  ): Promise<MessageItem> {
    if (!this.isLoggedIn) {
      throw new Error('未登录OpenIM');
    }

    try {
      console.log('📤 [@提及] 发送带 @ 提及的群组消息 (使用 createTextMessage 方案)', {
        groupID,
        text: text.substring(0, 50) + (text.length > 50 ? '...' : ''),
        atUserIDList,
      });

      // ==================== 新方案: 使用 createTextMessage() ====================
      // 用户反馈: "我用文本复制粘贴的方式可以" - 说明普通文本消息可以正常工作
      // 因此使用普通文本消息API,然后手动添加 @ 信息

      // 1. 创建普通文本消息 (已验证可用)
      const messageResponse = await this.sdk.createTextMessage(text);

      console.log('✅ [@提及] 普通文本消息创建成功:', messageResponse);

      // 提取实际的消息对象
      const message = messageResponse.data || messageResponse;

      console.log('📝 [@提及] 提取的消息对象:', message);
      console.log('🔍 [DEBUG] message.contentType:', message.contentType);
      console.log('🔍 [DEBUG] message.textElem:', message.textElem);
      console.log('🔍 [DEBUG] message.content (原始):', message.content);

      // 2. 手动添加 @ 提及信息到 textElem
      // 根据 OpenIM 协议，@ 信息存储在 textElem.atUserIDList
      if (message.textElem) {
        message.textElem.atUserIDList = atUserIDList;
        console.log('✅ [@提及] 已添加 atUserIDList 到 textElem:', atUserIDList);
      }

      // 3. 重新构造 content 字段（确保包含 @ 信息）
      // content 是 Base64 编码的 JSON，包含完整的消息内容
      try {
        // 解码现有 content 以获取基础结构
        let contentObj: any = {};

        if (message.content && message.content !== 'null' && message.content !== 'undefined') {
          try {
            const decoded = decodeURIComponent(escape(atob(message.content)));
            contentObj = JSON.parse(decoded);
            console.log('🔍 [DEBUG] 解码后的原始 content:', contentObj);
          } catch (e) {
            console.warn('⚠️ [@提及] 无法解码原始 content，使用默认结构');
            contentObj = { content: text };
          }
        } else {
          contentObj = { content: text };
        }

        // 添加 @ 信息到 content 对象
        contentObj.content = text; // 确保内容正确
        contentObj.atUserIDList = atUserIDList;
        contentObj.atUsersInfo = []; // SDK 会自动填充
        contentObj.isNotNotification = false;

        // 重新编码为 Base64
        const jsonStr = JSON.stringify(contentObj);
        message.content = btoa(unescape(encodeURIComponent(jsonStr)));

        console.log('✅ [@提及] content 字段重建成功');
        console.log('🔍 [DEBUG] 重建后的 content (decoded):', contentObj);
      } catch (encodeError) {
        console.error('❌ [@提及] content 字段编码失败:', encodeError);
        // Fallback: 使用未编码的 JSON
        const contentObj = {
          content: text,
          atUserIDList: atUserIDList,
          atUsersInfo: [],
          isNotNotification: false,
        };
        message.content = JSON.stringify(contentObj);
        console.log('⚠️ [@提及] 使用未编码的 content 作为 fallback');
      }

      // 4. 发送消息
      const result = await this.sdk.sendMessage({
        recvID: '', // 群聊时 recvID 为空
        groupID: groupID,
        message: message,
      });

      console.log('✅ [@提及] 群组 @ 消息发送成功 (createTextMessage 方案)');
      console.log('🔍 [DEBUG] result完整结构:', JSON.stringify(result, null, 2));

      return result;
    } catch (error) {
      console.error('❌ [@提及] 发送群组 @ 消息失败:', error);
      throw error;
    }
  }

  /**
   * 获取会话
   * @param sourceID 会话源ID(用户ID或群组ID)
   * @param sessionType 会话类型 1-单聊 3-超级群组
   */
  async getConversation(sourceID: string, sessionType: number): Promise<ConversationItem> {
    try {
      const response = await this.sdk.getOneConversation({
        sourceID,
        sessionType,
      });

      // 提取实际的会话对象 (SDK可能返回包装对象)
      const conversation = response.data || response;

      console.log('✅ [OpenIM] 获取会话成功, conversationID:', conversation.conversationID);

      return conversation;
    } catch (error) {
      console.error('❌ [OpenIM] 获取会话失败:', error);
      throw error;
    }
  }

  /**
   * 获取历史消息
   * @param conversationID 会话ID
   * @param count 消息数量
   * @param startClientMsgID 起始消息ID(用于分页)
   */
  async getHistoryMessages(
    conversationID: string,
    count: number = 20,
    startClientMsgID: string = ''
  ): Promise<MessageItem[]> {
    try {
      console.log(`📥 [OpenIM] 获取历史消息, conversationID: ${conversationID}, count: ${count}`);

      // 🔧 新增: 确保会话存在于本地数据库
      if (!startClientMsgID) {
        await this.ensureConversationExists(conversationID);
      }

      // OpenIM SDK 使用 getAdvancedHistoryMessageList 方法
      const response = await this.sdk.getAdvancedHistoryMessageList({
        conversationID,
        count,
        startClientMsgID,
      });

      // 提取实际的结果对象 (SDK可能返回包装对象)
      const result = response.data || response;

      // result 是 AdvancedGetMessageResult 类型，包含 messageList 数组
      const messages = result.messageList || [];

      console.log(`✅ [OpenIM] 获取历史消息成功, 数量: ${messages.length}`);

      // 如果本地没有消息,尝试从服务器拉取
      if (messages.length === 0 && !startClientMsgID) {
        console.log('⚠️ [OpenIM] 本地没有历史消息,尝试从服务器同步...');
        try {
          await this.syncMessagesFromServer(conversationID, count);

          // 重新获取本地消息
          const retryResponse = await this.sdk.getAdvancedHistoryMessageList({
            conversationID,
            count,
            startClientMsgID: '',
          });

          const retryResult = retryResponse.data || retryResponse;
          const retryMessages = retryResult.messageList || [];

          console.log(`✅ [OpenIM] 服务器同步后获取到消息, 数量: ${retryMessages.length}`);

          // 如果SDK同步后仍然没有消息,返回空数组
          // SuperGroup的会话需要通过发送第一条消息来触发创建
          if (retryMessages.length === 0) {
            console.log('⚠️ [OpenIM] SDK同步后仍无消息 - 这是SuperGroup的正常行为');
            console.log('💡 [OpenIM] 会话会在第一条消息发送后自动创建');
          }

          return retryMessages;
        } catch (syncError) {
          console.warn('⚠️ [OpenIM] 服务器消息同步失败:', syncError);
          // 返回空数组,允许用户发送第一条消息
          return [];
        }
      }

      return messages;
    } catch (error) {
      console.error('❌ [OpenIM] 获取历史消息失败:', error);
      throw error;
    }
  }

  /**
   * 确保会话存在于本地数据库
   * 如果会话不存在，尝试从服务器同步或创建会话记录
   * @param conversationID 会话ID (格式: sg_group_report_5)
   */
  private async ensureConversationExists(conversationID: string): Promise<void> {
    try {
      console.log('🔍 [OpenIM] 检查会话是否存在:', conversationID);

      // 提取 groupID 和 sessionType
      const isSuperGroup = conversationID.startsWith('sg_');
      const groupID = conversationID.replace(/^sg_/, ''); // 只去掉 sg_ 前缀，保留 group_ 前缀
      const sessionType = isSuperGroup ? 3 : 2;

      console.log('🔍 [DEBUG] 会话信息:', { conversationID, groupID, sessionType, isSuperGroup });

      // 尝试获取会话
      try {
        const conversation = await this.sdk.getOneConversation({
          sourceID: groupID,
          sessionType: sessionType,
        });

        console.log('✅ [OpenIM] 会话已存在:', conversation.data?.conversationID || conversation.conversationID);
        return;
      } catch (error: any) {
        console.log('⚠️ [OpenIM] 会话不存在于本地数据库, errCode:', error.errCode, 'message:', error.message);

        // 如果会话不存在 (errCode: 10002)
        if (error.errCode === 10002 || error.message?.includes('RecordNotFoundError')) {
          console.log('🔄 [OpenIM] 尝试创建会话...');

          // 方案1: 强制创建文本消息来触发会话创建 (但不发送)
          try {
            console.log('🔄 [OpenIM] 方案1: 尝试通过创建消息对象触发会话初始化...');

            // 创建一个文本消息对象 (不发送)
            const dummyMessage = await this.sdk.createTextMessage('');
            console.log('✅ [OpenIM] 消息对象创建成功');

            // 等待SDK处理
            await new Promise(resolve => setTimeout(resolve, 300));

            // 再次检查会话
            try {
              const retryConversation = await this.sdk.getOneConversation({
                sourceID: groupID,
                sessionType: sessionType,
              });
              console.log('✅ [OpenIM] 方案1成功: 会话已创建');
              return;
            } catch (retryError) {
              console.warn('⚠️ [OpenIM] 方案1失败: 会话仍不存在');
            }
          } catch (createError) {
            console.warn('⚠️ [OpenIM] 方案1失败: 无法创建消息对象:', createError);
          }

          // 方案2: 同步所有会话列表
          try {
            console.log('🔄 [OpenIM] 方案2: 尝试同步所有会话列表...');
            const conversations = await this.sdk.getAllConversationList();
            console.log('✅ [OpenIM] 会话列表同步完成, 数量:', conversations?.data?.length || conversations?.length || 0);

            // 再次尝试获取会话
            try {
              const retryConversation = await this.sdk.getOneConversation({
                sourceID: groupID,
                sessionType: sessionType,
              });
              console.log('✅ [OpenIM] 方案2成功: 会话同步完成');
              return;
            } catch (retryError) {
              console.warn('⚠️ [OpenIM] 方案2失败: 同步后仍无法获取会话');
            }
          } catch (syncError) {
            console.warn('⚠️ [OpenIM] 方案2失败: 同步会话列表异常:', syncError);
          }

          // 方案3: 获取群组信息来触发会话创建
          try {
            console.log('🔄 [OpenIM] 方案3: 尝试获取群组信息...');
            const groupInfo = await this.sdk.getSpecifiedGroupsInfo([groupID]);
            console.log('✅ [OpenIM] 群组信息获取成功:', groupInfo);

            // 等待SDK处理
            await new Promise(resolve => setTimeout(resolve, 500));

            // 最后一次检查
            try {
              const finalConversation = await this.sdk.getOneConversation({
                sourceID: groupID,
                sessionType: sessionType,
              });
              console.log('✅ [OpenIM] 方案3成功: 会话已创建');
              return;
            } catch (finalError) {
              console.warn('⚠️ [OpenIM] 方案3失败: 获取群组信息后会话仍不存在');
            }
          } catch (groupError) {
            console.warn('⚠️ [OpenIM] 方案3失败: 获取群组信息异常:', groupError);
          }

          console.error('❌ [OpenIM] 所有方案均失败,无法创建会话');
          console.warn('💡 [OpenIM] 提示: 这可能是SuperGroup的已知问题,会话需要通过发送消息来创建');
          console.warn('💡 [OpenIM] 解决方案: 会话会在用户第一次发送消息时自动创建');
        }
      }
    } catch (error) {
      console.error('❌ [OpenIM] ensureConversationExists 异常:', error);
      // 不抛出异常，允许后续流程继续
    }
  }

  /**
   * 从服务器同步消息到本地
   * @param conversationID 会话ID
   * @param count 消息数量
   */
  private async syncMessagesFromServer(conversationID: string, count: number): Promise<void> {
    try {
      console.log('🔄 [OpenIM] 从服务器拉取消息...');
      console.log('🔍 [DEBUG] syncMessagesFromServer参数:', { conversationID, count });

      // 方案1: 尝试使用 SDK 的服务器消息拉取方法
      try {
        const response = await this.sdk.getAdvancedHistoryMessageListReverse({
          conversationID,
          count,
          startClientMsgID: '',
          lastMinSeq: 0,
        });

        console.log('🔍 [DEBUG] getAdvancedHistoryMessageListReverse响应:', response);

        // 检查是否成功拉取到消息
        const result = response.data || response;
        const messages = result.messageList || [];

        if (messages.length > 0) {
          console.log(`✅ [OpenIM] 服务器消息拉取成功, 数量: ${messages.length}`);
          return;
        }

        console.warn('⚠️ [OpenIM] getAdvancedHistoryMessageListReverse返回空消息列表');
      } catch (reverseError) {
        console.warn('⚠️ [OpenIM] getAdvancedHistoryMessageListReverse失败:', reverseError);
      }

      // 方案2: 如果Reverse方法失败或返回空,尝试等待SDK同步
      console.log('🔄 [OpenIM] 方案1失败,等待2秒后重试...');
      await new Promise(resolve => setTimeout(resolve, 2000));

      // 再次尝试从本地获取
      try {
        const retryResponse = await this.sdk.getAdvancedHistoryMessageList({
          conversationID,
          count,
          startClientMsgID: '',
        });

        const retryResult = retryResponse.data || retryResponse;
        const retryMessages = retryResult.messageList || [];

        if (retryMessages.length > 0) {
          console.log(`✅ [OpenIM] 等待后成功获取消息, 数量: ${retryMessages.length}`);
          return;
        }

        console.warn('⚠️ [OpenIM] 等待后仍无法获取消息');
      } catch (retryError) {
        console.warn('⚠️ [OpenIM] 重试获取消息失败:', retryError);
      }

      console.log('✅ [OpenIM] 服务器消息拉取流程完成 (可能无消息)');
    } catch (error) {
      console.error('❌ [OpenIM] 服务器消息拉取失败:', error);
      throw error;
    }
  }

  /**
   * 标记消息已读
   * @param conversationID 会话ID
   */
  async markMessageAsRead(conversationID: string): Promise<void> {
    try {
      await this.sdk.markConversationMessageAsRead(conversationID);
      console.log('✅ [OpenIM] 消息已标记为已读');
    } catch (error) {
      console.error('❌ [OpenIM] 标记已读失败:', error);
    }
  }

  /**
   * 添加消息监听器
   * @param conversationID 会话ID
   * @param listener 监听函数
   */
  onMessage(conversationID: string, listener: (message: MessageItem) => void): void {
    if (!this.messageListeners.has(conversationID)) {
      this.messageListeners.set(conversationID, []);
    }
    this.messageListeners.get(conversationID)!.push(listener);
  }

  /**
   * 移除消息监听器
   * @param conversationID 会话ID
   * @param listener 监听函数
   */
  offMessage(conversationID: string, listener: Function): void {
    const listeners = this.messageListeners.get(conversationID);
    if (listeners) {
      const index = listeners.indexOf(listener);
      if (index > -1) {
        listeners.splice(index, 1);
      }
    }
  }

  /**
   * 通知消息监听器
   */
  private notifyMessageListeners(message: MessageItem): void {
    // 🔧 关键修复: 消息对象可能没有conversationID,需要根据groupID和sessionType构造
    // sessionType: 1=单聊, 3=超级群组
    let conversationID = message.conversationID;

    if (!conversationID && message.groupID) {
      // 根据sessionType构造conversationID
      // sessionType=3 (超级群组) -> sg_group_{groupID}
      // sessionType=2 (普通群组) -> group_{groupID}
      if (message.sessionType === 3) {
        conversationID = `sg_${message.groupID}`;
      } else if (message.sessionType === 2) {
        conversationID = `group_${message.groupID}`;
      } else {
        conversationID = message.groupID; // 兜底方案
      }
      console.log('🔧 [OpenIM] 构造conversationID:', conversationID, '来自groupID:', message.groupID);
    }

    console.log('🔔 [OpenIM] 通知消息监听器:', {
      conversationID,
      groupID: message.groupID,
      sessionType: message.sessionType,
      clientMsgID: message.clientMsgID,
      sendID: message.sendID,
      registeredConversations: Array.from(this.messageListeners.keys()),
      listenerCount: this.messageListeners.get(conversationID)?.length || 0,
    });

    const listeners = this.messageListeners.get(conversationID);
    if (listeners && listeners.length > 0) {
      console.log(`✅ [OpenIM] 找到 ${listeners.length} 个监听器，开始通知`);
      listeners.forEach(listener => {
        try {
          listener(message);
        } catch (error) {
          console.error('❌ [OpenIM] 消息监听器执行失败:', error);
        }
      });
    } else {
      console.warn('⚠️ [OpenIM] 未找到会话的消息监听器:', conversationID);
      console.warn('⚠️ [OpenIM] 已注册的会话列表:', Array.from(this.messageListeners.keys()));
      console.warn('⚠️ [OpenIM] 消息详情:', {
        groupID: message.groupID,
        sessionType: message.sessionType,
        clientMsgID: message.clientMsgID,
      });
    }
  }

  /**
   * 添加会话监听器
   */
  onConversationChanged(listener: (conversations: ConversationItem[]) => void): void {
    this.conversationListeners.push(listener);
  }

  /**
   * 移除会话监听器
   */
  offConversationChanged(listener: (conversations: ConversationItem[]) => void): void {
    const index = this.conversationListeners.indexOf(listener);
    if (index > -1) {
      this.conversationListeners.splice(index, 1);
      console.log('📤 [OpenIM] 移除会话变化监听器');
    }
  }

  /**
   * 获取所有会话列表
   */
  async getAllConversations(): Promise<ConversationItem[]> {
    try {
      return await this.sdk.getAllConversationList();
    } catch (error) {
      console.error('❌ [OpenIM] 获取会话列表失败:', error);
      throw error;
    }
  }

  /**
   * 获取未读消息总数
   */
  async getTotalUnreadCount(): Promise<number> {
    try {
      return await this.sdk.getTotalUnreadMsgCount();
    } catch (error) {
      console.error('❌ [OpenIM] 获取未读数失败:', error);
      return 0;
    }
  }

  /**
   * 检查是否已登录
   */
  get loggedIn(): boolean {
    return this.isLoggedIn;
  }

  /**
   * 获取当前用户ID
   */
  get userId(): string {
    return this.currentUserId;
  }

  /**
   * 获取连接状态
   */
  get status(): 'disconnected' | 'connecting' | 'connected' {
    return this.connectionState;
  }

  // ==================== 群组管理功能（参考官方示例） ====================

  /**
   * 创建群组
   * @param groupInfo 群组信息
   */
  async createGroup(groupInfo: {
    groupName: string;
    notification?: string;
    introduction?: string;
    faceURL?: string;
    memberUserIDs: string[];
  }): Promise<GroupItem> {
    if (!this.isLoggedIn) {
      throw new Error('未登录OpenIM');
    }

    try {
      console.log('👥 [OpenIM] 创建群组:', groupInfo);

      const result = await this.sdk.createGroup({
        groupBaseInfo: {
          groupName: groupInfo.groupName,
          notification: groupInfo.notification || '',
          introduction: groupInfo.introduction || '',
          faceURL: groupInfo.faceURL || '',
          groupType: 2, // 2表示普通群
        },
        memberUserIDs: groupInfo.memberUserIDs,
      });

      console.log('✅ [OpenIM] 群组创建成功:', result);
      return result;
    } catch (error) {
      console.error('❌ [OpenIM] 创建群组失败:', error);
      smartSentry.captureError(error, { tags: { module: 'OpenIM', action: 'createGroup' } });
      throw error;
    }
  }

  /**
   * 邀请用户加入群组
   * @param groupID 群组ID
   * @param userIDs 用户ID列表
   * @param reason 邀请理由
   */
  async inviteUsersToGroup(groupID: string, userIDs: string[], reason?: string): Promise<void> {
    if (!this.isLoggedIn) {
      throw new Error('未登录OpenIM');
    }

    try {
      console.log('👥 [OpenIM] 邀请用户加入群组:', { groupID, userIDs });

      await this.sdk.inviteUserToGroup({
        groupID,
        userIDList: userIDs,
        reason: reason || '',
      });

      console.log('✅ [OpenIM] 邀请成功');
    } catch (error) {
      console.error('❌ [OpenIM] 邀请用户失败:', error);
      throw error;
    }
  }

  /**
   * 移除群成员
   * @param groupID 群组ID
   * @param userIDs 用户ID列表
   * @param reason 移除理由
   */
  async removeGroupMembers(groupID: string, userIDs: string[], reason?: string): Promise<void> {
    if (!this.isLoggedIn) {
      throw new Error('未登录OpenIM');
    }

    try {
      console.log('👥 [OpenIM] 移除群成员:', { groupID, userIDs });

      await this.sdk.kickGroupMember({
        groupID,
        userIDList: userIDs,
        reason: reason || '',
      });

      console.log('✅ [OpenIM] 移除成功');
    } catch (error) {
      console.error('❌ [OpenIM] 移除群成员失败:', error);
      throw error;
    }
  }

  /**
   * 退出群组
   * @param groupID 群组ID
   */
  async quitGroup(groupID: string): Promise<void> {
    if (!this.isLoggedIn) {
      throw new Error('未登录OpenIM');
    }

    try {
      console.log('👥 [OpenIM] 退出群组:', groupID);
      await this.sdk.quitGroup(groupID);
      console.log('✅ [OpenIM] 退出成功');
    } catch (error) {
      console.error('❌ [OpenIM] 退出群组失败:', error);
      throw error;
    }
  }

  /**
   * 解散群组
   * @param groupID 群组ID
   */
  async dismissGroup(groupID: string): Promise<void> {
    if (!this.isLoggedIn) {
      throw new Error('未登录OpenIM');
    }

    try {
      console.log('👥 [OpenIM] 解散群组:', groupID);
      await this.sdk.dismissGroup(groupID);
      console.log('✅ [OpenIM] 解散成功');
    } catch (error) {
      console.error('❌ [OpenIM] 解散群组失败:', error);
      throw error;
    }
  }

  /**
   * 获取群组信息
   * @param groupID 群组ID
   */
  async getGroupInfo(groupID: string): Promise<GroupItem> {
    try {
      const result = await this.sdk.getSpecifiedGroupsInfo([groupID]);
      return result[0];
    } catch (error) {
      console.error('❌ [OpenIM] 获取群组信息失败:', error);
      throw error;
    }
  }

  /**
   * 获取群成员列表
   * @param groupID 群组ID
   */
  async getGroupMembers(groupID: string): Promise<GroupMemberItem[]> {
    try {
      const response = await this.sdk.getGroupMemberList({
        groupID,
        filter: 0, // 0表示所有成员
        offset: 0,
        count: 1000,
      });

      // 提取实际的结果对象 (SDK可能返回包装对象)
      const result = response.data || response;
      console.log('✅ [OpenIM] 获取群成员列表成功, 数量:', result.length);

      return Array.isArray(result) ? result : [];
    } catch (error) {
      console.error('❌ [OpenIM] 获取群成员列表失败:', error);
      throw error;
    }
  }

  /**
   * 获取已加入的群组列表
   */
  async getJoinedGroups(): Promise<GroupItem[]> {
    try {
      return await this.sdk.getJoinedGroupList();
    } catch (error) {
      console.error('❌ [OpenIM] 获取群组列表失败:', error);
      throw error;
    }
  }

  /**
   * 添加群组监听器
   */
  onGroupChanged(listener: (event: { type: string; data: any }) => void): void {
    this.groupListeners.push(listener);
  }

  // ==================== 扩展消息类型支持（参考官方示例） ====================

  /**
   * 生成UUID（兼容性方案）
   */
  private generateUUID(): string {
    // 优先使用原生 crypto.randomUUID
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
      return crypto.randomUUID();
    }

    // 降级方案：使用时间戳 + 随机数
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }

  /**
   * 发送图片消息
   * @param conversationID 会话ID
   * @param file 图片文件
   */
  async sendImageMessage(conversationID: string, file: File): Promise<MessageItem> {
    if (!this.isLoggedIn) {
      throw new Error('未登录OpenIM');
    }

    try {
      console.log('🖼️ [OpenIM] 发送图片消息, 文件:', file.name, file.size);

      // 读取图片尺寸
      const imageSize = await this.getImageDimensions(file);
      console.log('📐 [OpenIM] 图片尺寸:', imageSize);

      const message = await this.sdk.createImageMessageByFile({
        sourcePath: '',
        sourcePicture: {
          uuid: this.generateUUID(),
          type: file.type,
          size: file.size,
          width: imageSize.width,
          height: imageSize.height,
          url: '',
        },
        bigPicture: {
          uuid: this.generateUUID(),
          type: file.type,
          size: file.size,
          width: imageSize.width,
          height: imageSize.height,
          url: '',
        },
        snapshotPicture: {
          uuid: this.generateUUID(),
          type: file.type,
          size: file.size,
          width: imageSize.width,
          height: imageSize.height,
          url: '',
        },
        file,
      });

      console.log('✅ [OpenIM] 图片消息对象创建成功');

      // 提取消息对象
      const messageData = message.data || message;

      const result = await this.sdk.sendMessage({
        recvID: '',
        groupID: '',
        message: messageData,
      });

      console.log('✅ [OpenIM] 图片消息发送成功');
      return result;
    } catch (error) {
      console.error('❌ [OpenIM] 图片消息发送失败:', error);
      smartSentry.captureError(error, { tags: { module: 'OpenIM', action: 'sendImageMessage' } });
      throw error;
    }
  }

  /**
   * 获取图片尺寸
   */
  private getImageDimensions(file: File): Promise<{ width: number; height: number }> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      const url = URL.createObjectURL(file);

      img.onload = () => {
        URL.revokeObjectURL(url);
        resolve({
          width: img.width,
          height: img.height,
        });
      };

      img.onerror = () => {
        URL.revokeObjectURL(url);
        reject(new Error('图片加载失败'));
      };

      img.src = url;
    });
  }

  /**
   * 发送群组图片消息
   * @param groupID 群组ID
   * @param file 图片文件
   */
  async sendGroupImageMessage(groupID: string, file: File): Promise<MessageItem> {
    if (!this.isLoggedIn) {
      throw new Error('未登录OpenIM');
    }

    try {
      console.log('🖼️ [OpenIM] 发送群组图片消息, groupID:', groupID);

      // 读取图片尺寸
      const imageSize = await this.getImageDimensions(file);
      console.log('📐 [OpenIM] 图片尺寸:', imageSize);

      const message = await this.sdk.createImageMessageByFile({
        sourcePath: '',
        sourcePicture: {
          uuid: this.generateUUID(),
          type: file.type,
          size: file.size,
          width: imageSize.width,
          height: imageSize.height,
          url: '',
        },
        bigPicture: {
          uuid: this.generateUUID(),
          type: file.type,
          size: file.size,
          width: imageSize.width,
          height: imageSize.height,
          url: '',
        },
        snapshotPicture: {
          uuid: this.generateUUID(),
          type: file.type,
          size: file.size,
          width: imageSize.width,
          height: imageSize.height,
          url: '',
        },
        file,
      });

      console.log('✅ [OpenIM] 图片消息对象创建成功');

      // 提取消息对象
      const messageData = message.data || message;

      const result = await this.sdk.sendMessage({
        recvID: '', // 群聊时 recvID 为空
        groupID: groupID,
        message: messageData,
      });

      console.log('✅ [OpenIM] 群组图片消息发送成功');
      return result;
    } catch (error) {
      console.error('❌ [OpenIM] 群组图片消息发送失败:', error);
      smartSentry.captureError(error, { tags: { module: 'OpenIM', action: 'sendGroupImageMessage' } });
      throw error;
    }
  }

  /**
   * 发送文件消息
   * @param conversationID 会话ID
   * @param file 文件
   */
  async sendFileMessage(conversationID: string, file: File): Promise<MessageItem> {
    if (!this.isLoggedIn) {
      throw new Error('未登录OpenIM');
    }

    try {
      console.log('📎 [OpenIM] 发送文件消息');

      const message = await this.sdk.createFileMessageByFile({
        filePath: '',
        fileName: file.name,
        file,
      });

      // 提取消息对象
      const messageData = message.data || message;

      const result = await this.sdk.sendMessage({
        recvID: '',
        groupID: '',
        message: messageData,
      });

      console.log('✅ [OpenIM] 文件消息发送成功');
      return result;
    } catch (error) {
      console.error('❌ [OpenIM] 文件消息发送失败:', error);
      smartSentry.captureError(error, { tags: { module: 'OpenIM', action: 'sendFileMessage' } });
      throw error;
    }
  }

  /**
   * 发送群组文件消息
   * @param groupID 群组ID
   * @param file 文件
   */
  async sendGroupFileMessage(groupID: string, file: File): Promise<MessageItem> {
    if (!this.isLoggedIn) {
      throw new Error('未登录OpenIM');
    }

    try {
      console.log('📎 [OpenIM] 发送群组文件消息, groupID:', groupID);

      const message = await this.sdk.createFileMessageByFile({
        filePath: '',
        fileName: file.name,
        file,
      });

      console.log('✅ [OpenIM] 文件消息对象创建成功');

      // 提取消息对象
      const messageData = message.data || message;

      const result = await this.sdk.sendMessage({
        recvID: '', // 群聊时 recvID 为空
        groupID: groupID,
        message: messageData,
      });

      console.log('✅ [OpenIM] 群组文件消息发送成功');
      return result;
    } catch (error) {
      console.error('❌ [OpenIM] 群组文件消息发送失败:', error);
      smartSentry.captureError(error, { tags: { module: 'OpenIM', action: 'sendGroupFileMessage' } });
      throw error;
    }
  }

  /**
   * 发送语音消息
   * @param conversationID 会话ID
   * @param file 语音文件
   * @param duration 时长（秒）
   */
  async sendVoiceMessage(conversationID: string, file: File, duration: number): Promise<MessageItem> {
    if (!this.isLoggedIn) {
      throw new Error('未登录OpenIM');
    }

    try {
      console.log('🎤 [OpenIM] 发送语音消息');

      const message = await this.sdk.createSoundMessage({
        soundPath: file.path || '',
        duration,
        file,
      });

      const result = await this.sdk.sendMessage({
        message,
        conversationID,
      });

      console.log('✅ [OpenIM] 语音消息发送成功');
      return result;
    } catch (error) {
      console.error('❌ [OpenIM] 语音消息发送失败:', error);
      smartSentry.captureError(error, { tags: { module: 'OpenIM', action: 'sendVoiceMessage' } });
      throw error;
    }
  }

  /**
   * 发送视频消息
   * @param conversationID 会话ID
   * @param videoFile 视频文件
   * @param duration 时长（秒）
   * @param snapshotFile 视频封面
   */
  async sendVideoMessage(
    conversationID: string,
    videoFile: File,
    duration: number,
    snapshotFile?: File
  ): Promise<MessageItem> {
    if (!this.isLoggedIn) {
      throw new Error('未登录OpenIM');
    }

    try {
      console.log('🎬 [OpenIM] 发送视频消息');

      const message = await this.sdk.createVideoMessage({
        videoPath: videoFile.path || '',
        duration,
        videoType: videoFile.type,
        snapshotPath: snapshotFile?.path || '',
        videoFile,
        snapshotFile,
      });

      const result = await this.sdk.sendMessage({
        message,
        conversationID,
      });

      console.log('✅ [OpenIM] 视频消息发送成功');
      return result;
    } catch (error) {
      console.error('❌ [OpenIM] 视频消息发送失败:', error);
      smartSentry.captureError(error, { tags: { module: 'OpenIM', action: 'sendVideoMessage' } });
      throw error;
    }
  }

  /**
   * 发送群组视频消息
   * @param groupID 群组ID
   * @param videoFile 视频文件
   * @param duration 时长（秒）
   * @param snapshotFile 视频封面
   */
  async sendGroupVideoMessage(
    groupID: string,
    videoFile: File,
    duration: number,
    snapshotFile?: File
  ): Promise<MessageItem> {
    if (!this.isLoggedIn) {
      throw new Error('未登录OpenIM');
    }

    try {
      console.log('🎬 [OpenIM] 发送群组视频消息, groupID:', groupID);

      // 获取视频尺寸（用于生成缩略图尺寸）
      const snapshotSize = snapshotFile ? await this.getImageDimensions(snapshotFile) : { width: 0, height: 0 };

      // 创建视频消息（使用 createVideoMessageByFile）
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

      console.log('✅ [OpenIM] 视频消息对象创建成功');

      // 提取消息对象
      const messageData = message.data || message;

      const result = await this.sdk.sendMessage({
        recvID: '', // 群聊时 recvID 为空
        groupID: groupID,
        message: messageData,
      });

      console.log('✅ [OpenIM] 群组视频消息发送成功');
      return result;
    } catch (error) {
      console.error('❌ [OpenIM] 群组视频消息发送失败:', error);
      smartSentry.captureError(error, { tags: { module: 'OpenIM', action: 'sendGroupVideoMessage' } });
      throw error;
    }
  }

  /**
   * 发送位置消息
   * @param conversationID 会话ID
   * @param location 位置信息
   */
  async sendLocationMessage(
    conversationID: string,
    location: {
      description: string;
      longitude: number;
      latitude: number;
    }
  ): Promise<MessageItem> {
    if (!this.isLoggedIn) {
      throw new Error('未登录OpenIM');
    }

    try {
      console.log('📍 [OpenIM] 发送位置消息');

      const message = await this.sdk.createLocationMessage(location);

      const result = await this.sdk.sendMessage({
        message,
        conversationID,
      });

      console.log('✅ [OpenIM] 位置消息发送成功');
      return result;
    } catch (error) {
      console.error('❌ [OpenIM] 位置消息发送失败:', error);
      smartSentry.captureError(error, { tags: { module: 'OpenIM', action: 'sendLocationMessage' } });
      throw error;
    }
  }

  /**
   * 发送自定义消息
   * @param conversationID 会话ID
   * @param data 自定义数据
   * @param extension 扩展信息
   * @param description 描述
   */
  async sendCustomMessage(
    conversationID: string,
    data: any,
    extension?: string,
    description?: string
  ): Promise<MessageItem> {
    if (!this.isLoggedIn) {
      throw new Error('未登录OpenIM');
    }

    try {
      console.log('💬 [OpenIM] 发送自定义消息');

      const message = await this.sdk.createCustomMessage({
        data: JSON.stringify(data),
        extension: extension || '',
        description: description || '',
      });

      const result = await this.sdk.sendMessage({
        message,
        conversationID,
      });

      console.log('✅ [OpenIM] 自定义消息发送成功');
      return result;
    } catch (error) {
      console.error('❌ [OpenIM] 自定义消息发送失败:', error);
      smartSentry.captureError(error, { tags: { module: 'OpenIM', action: 'sendCustomMessage' } });
      throw error;
    }
  }

  // ==================== 用户信息和在线状态 ====================

  /**
   * 获取用户在线状态
   * @param userIDs 用户ID列表
   */
  async getUsersOnlineStatus(userIDs: string[]): Promise<Map<string, boolean>> {
    try {
      const response = await this.sdk.subscribeUsersStatus(userIDs);

      // 提取实际的结果对象 (SDK可能返回包装对象)
      const result = response.data || response;
      const statusMap = new Map<string, boolean>();

      // 确保result是数组
      const statusList = Array.isArray(result) ? result : [];

      statusList.forEach((status: any) => {
        // status.platformIDs 数组不为空表示在线
        statusMap.set(status.userID, status.platformIDs && status.platformIDs.length > 0);
      });

      console.log('✅ [OpenIM] 获取用户在线状态成功, 用户数:', statusList.length);
      return statusMap;
    } catch (error) {
      console.error('❌ [OpenIM] 获取用户在线状态失败:', error);
      throw error;
    }
  }

  /**
   * 监听用户在线状态变化
   * @param callback 状态变化回调
   */
  onUserStatusChanged(callback: (data: { userID: string; online: boolean }[]) => void): void {
    this.sdk.on('onUserStatusChanged', (data: any) => {
      console.log('👤 [OpenIM] 用户状态变化:', data);

      const statusList = data.map((status: any) => ({
        userID: status.userID,
        online: status.platformIDs && status.platformIDs.length > 0,
      }));

      callback(statusList);
    });
  }

  /**
   * 获取用户信息
   * @param userIDs 用户ID列表
   */
  async getUsersInfo(userIDs: string[]): Promise<any[]> {
    try {
      const result = await this.sdk.getUsersInfo(userIDs);
      console.log('✅ [OpenIM] 获取用户信息成功');
      return result;
    } catch (error) {
      console.error('❌ [OpenIM] 获取用户信息失败:', error);
      throw error;
    }
  }

  /**
   * 设置自己的用户信息
   * @param userInfo 用户信息
   */
  async setSelfInfo(userInfo: {
    nickname?: string;
    faceURL?: string;
    ex?: string;
  }): Promise<void> {
    try {
      await this.sdk.setSelfInfo(userInfo);
      console.log('✅ [OpenIM] 更新用户信息成功');
    } catch (error) {
      console.error('❌ [OpenIM] 更新用户信息失败:', error);
      throw error;
    }
  }

  // ==================== 高级消息功能 ====================

  /**
   * 删除消息
   * @param conversationID 会话ID
   * @param clientMsgID 客户端消息ID
   */
  async deleteMessage(conversationID: string, clientMsgID: string): Promise<void> {
    try {
      await this.sdk.deleteMessageFromLocalStorage({
        conversationID,
        clientMsgID,
      });
      console.log('✅ [OpenIM] 删除消息成功');
    } catch (error) {
      console.error('❌ [OpenIM] 删除消息失败:', error);
      throw error;
    }
  }

  /**
   * 撤回消息
   * @param conversationID 会话ID
   * @param clientMsgID 客户端消息ID
   */
  async revokeMessage(conversationID: string, clientMsgID: string): Promise<void> {
    try {
      await this.sdk.revokeMessage({
        conversationID,
        clientMsgID,
      });
      console.log('✅ [OpenIM] 撤回消息成功');
    } catch (error) {
      console.error('❌ [OpenIM] 撤回消息失败:', error);
      throw error;
    }
  }

  /**
   * 转发消息
   * @param message 要转发的消息
   * @param conversationID 目标会话ID
   */
  async forwardMessage(message: MessageItem, conversationID: string): Promise<MessageItem> {
    try {
      const forwardMessage = await this.sdk.createForwardMessage(message);

      const result = await this.sdk.sendMessage({
        message: forwardMessage,
        conversationID,
      });

      console.log('✅ [OpenIM] 转发消息成功');
      return result;
    } catch (error) {
      console.error('❌ [OpenIM] 转发消息失败:', error);
      throw error;
    }
  }

  /**
   * 搜索本地消息
   * @param conversationID 会话ID
   * @param keyword 关键词
   */
  async searchLocalMessages(conversationID: string, keyword: string): Promise<MessageItem[]> {
    try {
      const result = await this.sdk.searchLocalMessages({
        conversationID,
        keywordList: [keyword],
        messageTypeList: [101], // 101=文本消息
      });

      console.log('✅ [OpenIM] 搜索消息成功');
      return result.searchResultItems || [];
    } catch (error) {
      console.error('❌ [OpenIM] 搜索消息失败:', error);
      throw error;
    }
  }

  // ==================== 消息已读回执功能 (Phase 2) ====================

  /**
   * 获取群组消息已读状态
   * @param conversationID 会话ID
   * @param messageIDList 消息ID列表
   * @returns 已读状态Map (消息ID -> 已读信息)
   */
  async getGroupMessageReadReceipt(
    conversationID: string,
    messageIDList: string[]
  ): Promise<Map<string, { hasReadCount: number; unreadCount: number; readMembers: GroupMemberItem[] }>> {
    try {
      console.log('📖 [OpenIM] 获取群组消息已读状态, 消息数:', messageIDList.length);

      const readStatusMap = new Map();

      // 遍历每条消息，分别获取已读列表
      for (const clientMsgID of messageIDList) {
        try {
          // 获取该消息的已读成员列表
          const response = await this.sdk.getGroupMessageReaderList({
            conversationID,
            clientMsgID,
            filter: 0, // 0表示所有成员
            offset: 0,
            count: 1000, // 最多获取1000个成员
          });

          // 提取实际的结果对象
          const readMembers: GroupMemberItem[] = response.data || response;

          // 计算已读和未读人数
          const hasReadCount = Array.isArray(readMembers) ? readMembers.length : 0;

          // 存储已读状态
          readStatusMap.set(clientMsgID, {
            hasReadCount,
            unreadCount: 0, // 未读人数需要通过群成员总数计算，在调用方计算
            readMembers: Array.isArray(readMembers) ? readMembers : [],
          });

          console.log(`✅ [OpenIM] 消息 ${clientMsgID.substring(0, 8)}... 已读成员数: ${hasReadCount}`);
        } catch (error) {
          console.warn(`⚠️ [OpenIM] 获取消息 ${clientMsgID.substring(0, 8)}... 已读状态失败:`, error);
          // 失败的消息不添加到Map，调用方会初始化为0
        }
      }

      console.log(`✅ [OpenIM] 获取群组消息已读状态成功, 成功: ${readStatusMap.size}/${messageIDList.length}`);
      return readStatusMap;
    } catch (error) {
      console.error('❌ [OpenIM] 获取群组消息已读状态失败:', error);
      throw error;
    }
  }

  /**
   * 监听单聊已读回执
   * @param callback 回调函数
   */
  onC2CReadReceiptReceived(callback: (data: any[]) => void): void {
    this.sdk.on('onRecvC2CReadReceipt', (data: any) => {
      console.log('✓✓ [OpenIM] 收到单聊已读回执:', data);
      callback(Array.isArray(data) ? data : [data]);
    });
  }

  /**
   * 监听群聊已读回执
   * @param callback 回调函数
   */
  onGroupReadReceiptReceived(callback: (data: any) => void): void {
    this.sdk.on('onRecvGroupReadReceipt', (data: any) => {
      console.log('✓✓ [OpenIM] 收到群聊已读回执:', data);
      callback(data);
    });
  }
}

// 导出单例
export const openIMClient = new OpenIMClient();

// 导出类型
export type { MessageItem, ConversationItem, GroupItem, GroupMemberItem };
