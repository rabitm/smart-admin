/**
 * OpenIM SDK 封装类
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */

import { getSDK, type WsResponse } from '@openim/wasm-client-sdk';
import type {
  ImConfig,
  ImMessage,
  ImConversation,
  ImGroupInfo,
  ImConnectStatus
} from '/@/types/im';
import { message as antMessage } from 'ant-design-vue';

/**
 * SDK事件类型
 */
export enum SdkEvent {
  // 连接状态
  CONNECTING = 'connecting',
  CONNECTED = 'connected',
  DISCONNECTED = 'disconnected',
  RECONNECTING = 'reconnecting',

  // 消息事件
  NEW_MESSAGE = 'newMessage',
  MESSAGE_SENT = 'messageSent',
  MESSAGE_REVOKED = 'messageRevoked',

  // 会话事件
  CONVERSATION_CHANGED = 'conversationChanged',
  NEW_CONVERSATION = 'newConversation',
  TOTAL_UNREAD_CHANGED = 'totalUnreadChanged',

  // 群组事件
  GROUP_INFO_CHANGED = 'groupInfoChanged',
  MEMBER_ADDED = 'memberAdded',
  MEMBER_DELETED = 'memberDeleted',

  // 错误事件
  ERROR = 'error',
}

/**
 * 事件回调类型
 */
type EventCallback = (data?: any) => void;

/**
 * OpenIM SDK 封装类
 * 单例模式,全局唯一实例
 */
export class OpenIMSDKWrapper {
  private static instance: OpenIMSDKWrapper | null = null;
  private sdk: any = null;
  private eventListeners: Map<SdkEvent, EventCallback[]> = new Map();
  private isInitialized = false;
  private isConnected = false;
  private currentConfig: ImConfig | null = null;
  private reconnectTimer: number | null = null;
  private reconnectAttempts = 0;
  private readonly maxReconnectAttempts = 5;

  /**
   * 私有构造函数,防止外部实例化
   */
  private constructor() {
    console.log('📱 [OpenIM SDK] 创建SDK封装实例');
  }

  /**
   * 获取单例实例
   */
  public static getInstance(): OpenIMSDKWrapper {
    if (!OpenIMSDKWrapper.instance) {
      OpenIMSDKWrapper.instance = new OpenIMSDKWrapper();
    }
    return OpenIMSDKWrapper.instance;
  }

  /**
   * 初始化SDK
   * 注意: OpenIM WASM SDK v3.8+ 不需要单独调用init()
   * 直接在login()时传入配置即可自动初始化
   */
  public async init(config: ImConfig): Promise<boolean> {
    if (this.isInitialized) {
      console.log('📱 [OpenIM SDK] SDK已初始化,跳过');
      return true;
    }

    try {
      console.log('📱 [OpenIM SDK] 开始初始化SDK...', config);

      // 保存配置
      this.currentConfig = config;

      // 获取SDK实例
      this.sdk = getSDK();

      // 注册SDK事件监听器
      this.registerSdkEventListeners();

      this.isInitialized = true;
      console.log('✅ [OpenIM SDK] SDK初始化成功 (WASM运行时就绪)');

      return true;
    } catch (error) {
      console.error('❌ [OpenIM SDK] SDK初始化失败:', error);
      this.emit(SdkEvent.ERROR, { type: 'init', error });
      return false;
    }
  }

  /**
   * 登录
   * OpenIM WASM SDK v3.8+ 会在login时自动完成初始化
   */
  public async login(userID: string, token: string): Promise<boolean> {
    if (!this.isInitialized || !this.sdk) {
      console.error('❌ [OpenIM SDK] SDK未初始化');
      return false;
    }

    if (!this.currentConfig) {
      console.error('❌ [OpenIM SDK] 配置未设置');
      return false;
    }

    try {
      console.log('📱 [OpenIM SDK] 开始登录...', { userID });
      this.emit(SdkEvent.CONNECTING);

      // OpenIM SDK v3.8+ login参数包含所有配置
      const loginParams = {
        userID,
        token,
        platformID: this.currentConfig.platformID,
        apiAddr: this.currentConfig.apiUrl,
        wsAddr: this.currentConfig.wsUrl,
      };

      console.log('📱 [OpenIM SDK] 登录参数:', loginParams);
      await this.sdk.login(loginParams);

      this.isConnected = true;
      this.reconnectAttempts = 0;

      console.log('✅ [OpenIM SDK] 登录成功');
      this.emit(SdkEvent.CONNECTED);

      return true;
    } catch (error) {
      console.error('❌ [OpenIM SDK] 登录失败:', error);
      this.emit(SdkEvent.ERROR, { type: 'login', error });

      // 尝试重连
      this.scheduleReconnect();

      return false;
    }
  }

  /**
   * 登出
   */
  public async logout(): Promise<boolean> {
    if (!this.sdk) {
      return true;
    }

    try {
      console.log('📱 [OpenIM SDK] 开始登出...');

      // 清除重连定时器
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer);
        this.reconnectTimer = null;
      }

      await this.sdk.logout();

      this.isConnected = false;
      console.log('✅ [OpenIM SDK] 登出成功');
      this.emit(SdkEvent.DISCONNECTED);

      return true;
    } catch (error) {
      console.error('❌ [OpenIM SDK] 登出失败:', error);
      return false;
    }
  }

  /**
   * 发送文本消息
   */
  public async sendTextMessage(conversationID: string, text: string): Promise<ImMessage | null> {
    if (!this.isConnected || !this.sdk) {
      console.error('❌ [OpenIM SDK] 未连接,无法发送消息');
      antMessage.error('IM未连接,无法发送消息');
      return null;
    }

    try {
      console.log('📱 [OpenIM SDK] 发送文本消息...', { conversationID, text });

      // 创建文本消息 - createTextMessage返回的是包装后的响应
      const createResult: any = await this.sdk.createTextMessage(text);
      console.log('📱 [OpenIM SDK] createTextMessage响应:', createResult);

      // OpenIM SDK的响应格式: { operationID, event, data: {...} } 或 {resp}
      // data可能是对象或字符串，统一转为对象
      let messageData: any;
      if (createResult.data) {
        if (typeof createResult.data === 'string') {
          messageData = JSON.parse(createResult.data);
        } else {
          messageData = createResult.data;
        }
      } else if (createResult.resp) {
        // OpenIM SDK v3.8新格式: resp字段
        messageData = createResult.resp;
      } else {
        console.error('❌ [OpenIM SDK] 无法解析createTextMessage响应:', createResult);
        throw new Error('无法创建消息');
      }
      console.log('📱 [OpenIM SDK] 消息数据:', messageData);

      // 解析conversationID获取recvID和groupID
      const { recvID, groupID } = this.parseConversationID(conversationID);

      // 发送消息 - message参数直接传对象
      const result: any = await this.sdk.sendMessage({
        recvID,
        groupID,
        message: messageData,
      });
      console.log('📱 [OpenIM SDK] sendMessage响应:', result);

      // OpenIM SDK v3.8+ sendMessage的响应格式: {operationID, event, data} 或 {resp}
      let sentMessage: ImMessage;
      if (result.data) {
        if (typeof result.data === 'string') {
          sentMessage = JSON.parse(result.data);
        } else {
          sentMessage = result.data;
        }
      } else if (result.resp) {
        // OpenIM SDK v3.8新格式: resp字段
        sentMessage = result.resp;
      } else {
        console.error('❌ [OpenIM SDK] 无法解析sendMessage响应:', result);
        throw new Error('无法解析消息响应');
      }

      console.log('✅ [OpenIM SDK] 消息发送成功', sentMessage);
      this.emit(SdkEvent.MESSAGE_SENT, sentMessage);
      return sentMessage;
    } catch (error) {
      console.error('❌ [OpenIM SDK] 发送消息失败:', error);
      antMessage.error('消息发送失败');
      this.emit(SdkEvent.ERROR, { type: 'sendMessage', error });
      return null;
    }
  }

  /**
   * 发送图片消息
   */
  public async sendImageMessage(conversationID: string, imageFile: File): Promise<ImMessage | null> {
    if (!this.isConnected || !this.sdk) {
      console.error('❌ [OpenIM SDK] 未连接,无法发送消息');
      antMessage.error('IM未连接,无法发送消息');
      return null;
    }

    try {
      console.log('📱 [OpenIM SDK] 发送图片消息...', { conversationID, imageFile });

      // 读取图片文件信息
      const imageInfo = await this.getImageInfo(imageFile);
      console.log('📷 [OpenIM SDK] 图片信息:', imageInfo);

      // 创建图片消息 - 使用createImageMessageByURL方式
      // 注意: 实际项目中应该先上传图片到服务器获取URL
      // 这里我们使用Base64 Data URL作为临时方案
      const base64Url = await this.fileToBase64(imageFile);

      // 修复：生成简单的UUID
      const uuid = Date.now().toString();

      // 根据SDK类型定义 ImageMsgParamsByURL 的正确结构
      const imageParams = {
        sourcePath: imageFile.name,  // 必须有 sourcePath 字段
        sourcePicture: {
          uuid: uuid + '_source',
          type: imageFile.type || 'image/jpeg',
          size: imageFile.size,
          width: imageInfo.width || 0,
          height: imageInfo.height || 0,
          url: base64Url,
        },
        bigPicture: {
          uuid: uuid + '_big',
          type: imageFile.type || 'image/jpeg',
          size: imageFile.size,
          width: imageInfo.width || 0,
          height: imageInfo.height || 0,
          url: base64Url,
        },
        snapshotPicture: {
          uuid: uuid + '_snapshot',
          type: imageFile.type || 'image/jpeg',
          size: Math.min(imageFile.size, 10240), // 缩略图大小限制
          width: Math.min(imageInfo.width || 200, 200),
          height: Math.min(imageInfo.height || 200, 200),
          url: base64Url,
        },
      };

      console.log('📱 [OpenIM SDK] 图片参数 (符合ImageMsgParamsByURL类型):', imageParams);

      const createResult: any = await this.sdk.createImageMessageByURL(imageParams);

      console.log('📱 [OpenIM SDK] createImageMessageByURL响应:', createResult);

      // 处理响应数据 - 与sendTextMessage保持一致的处理逻辑
      let messageData: any;
      if (createResult.data) {
        if (typeof createResult.data === 'string') {
          messageData = JSON.parse(createResult.data);
        } else {
          messageData = createResult.data;
        }
      } else if (createResult.resp) {
        // OpenIM SDK v3.8新格式: resp字段
        messageData = createResult.resp;
      } else {
        console.error('❌ [OpenIM SDK] 无法解析createImageMessageByURL响应:', createResult);
        throw new Error('无法创建图片消息');
      }

      console.log('📱 [OpenIM SDK] 消息数据:', messageData);

      // 解析conversationID获取recvID和groupID
      const { recvID, groupID } = this.parseConversationID(conversationID);

      // 修复：确保message参数格式正确，不需要offlinePushInfo
      // sendMessage期望的参数格式可能更简单
      const sendParams = {
        recvID,
        groupID,
        message: messageData,
      };

      console.log('📱 [OpenIM SDK] 发送参数:', sendParams);

      const result: any = await this.sdk.sendMessage(sendParams);

      console.log('📱 [OpenIM SDK] sendMessage响应:', result);

      // OpenIM SDK v3.8+ sendMessage的响应格式: {operationID, event, data} 或 {resp}
      let sentMessage: ImMessage;
      if (result.data) {
        if (typeof result.data === 'string') {
          sentMessage = JSON.parse(result.data);
        } else {
          sentMessage = result.data;
        }
      } else if (result.resp) {
        // OpenIM SDK v3.8新格式: resp字段
        sentMessage = result.resp;
      } else {
        console.error('❌ [OpenIM SDK] 无法解析sendMessage响应:', result);
        throw new Error('无法解析消息响应');
      }

      console.log('✅ [OpenIM SDK] 图片发送成功', sentMessage);
      this.emit(SdkEvent.MESSAGE_SENT, sentMessage);
      return sentMessage;
    } catch (error) {
      console.error('❌ [OpenIM SDK] 发送图片失败:', error);
      antMessage.error('图片发送失败');
      return null;
    }
  }

  /**
   * 获取图片信息(宽高)
   */
  private async getImageInfo(file: File): Promise<{ width: number; height: number }> {
    return new Promise((resolve) => {
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
        // 默认尺寸
        resolve({
          width: 0,
          height: 0,
        });
      };

      img.src = url;
    });
  }

  /**
   * 将文件转换为Base64
   */
  private async fileToBase64(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        resolve(reader.result as string);
      };
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  }

  /**
   * 生成UUID
   */
  private generateUUID(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
      const r = (Math.random() * 16) | 0;
      const v = c === 'x' ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  }

  /**
   * 发送文件消息
   */
  public async sendFileMessage(conversationID: string, file: File): Promise<ImMessage | null> {
    if (!this.isConnected || !this.sdk) {
      console.error('❌ [OpenIM SDK] 未连接,无法发送消息');
      antMessage.error('IM未连接,无法发送消息');
      return null;
    }

    try {
      console.log('📱 [OpenIM SDK] 发送文件消息...', { conversationID, file });

      // 将文件转换为Base64 (实际项目中应该上传到服务器)
      const base64Url = await this.fileToBase64(file);

      // 修复：生成简单的UUID
      const uuid = Date.now().toString();

      // 根据SDK类型定义 FileMsgParamsByURL 的正确结构
      const fileParams = {
        filePath: file.name,     // 文件路径
        fileName: file.name,     // 文件名
        uuid: uuid,             // 唯一标识符
        sourceUrl: base64Url,   // 文件URL
        fileSize: file.size,    // 文件大小
        fileType: file.type || 'application/octet-stream', // 文件类型（可选）
      };

      console.log('📱 [OpenIM SDK] 文件参数:', fileParams);

      const createResult: any = await this.sdk.createFileMessageByURL(fileParams);

      console.log('📱 [OpenIM SDK] createFileMessageByURL响应:', createResult);

      // 处理响应数据 - 与sendTextMessage保持一致的处理逻辑
      let messageData: any;
      if (createResult.data) {
        if (typeof createResult.data === 'string') {
          messageData = JSON.parse(createResult.data);
        } else {
          messageData = createResult.data;
        }
      } else if (createResult.resp) {
        // OpenIM SDK v3.8新格式: resp字段
        messageData = createResult.resp;
      } else {
        console.error('❌ [OpenIM SDK] 无法解析createFileMessageByURL响应:', createResult);
        throw new Error('无法创建文件消息');
      }

      console.log('📱 [OpenIM SDK] 文件消息数据:', messageData);

      // 解析conversationID获取recvID和groupID
      const { recvID, groupID } = this.parseConversationID(conversationID);

      // 修复：确保message参数格式正确，不需要offlinePushInfo
      // sendMessage期望的参数格式可能更简单
      const sendParams = {
        recvID,
        groupID,
        message: messageData,
      };

      console.log('📱 [OpenIM SDK] 发送参数:', sendParams);

      const result: any = await this.sdk.sendMessage(sendParams);

      console.log('📱 [OpenIM SDK] sendMessage响应:', result);

      // OpenIM SDK v3.8+ sendMessage的响应格式: {operationID, event, data} 或 {resp}
      let sentMessage: ImMessage;
      if (result.data) {
        if (typeof result.data === 'string') {
          sentMessage = JSON.parse(result.data);
        } else {
          sentMessage = result.data;
        }
      } else if (result.resp) {
        // OpenIM SDK v3.8新格式: resp字段
        sentMessage = result.resp;
      } else {
        console.error('❌ [OpenIM SDK] 无法解析sendMessage响应:', result);
        throw new Error('无法解析消息响应');
      }

      console.log('✅ [OpenIM SDK] 文件发送成功', sentMessage);
      this.emit(SdkEvent.MESSAGE_SENT, sentMessage);
      return sentMessage;
    } catch (error) {
      console.error('❌ [OpenIM SDK] 发送文件失败:', error);
      antMessage.error('文件发送失败');
      return null;
    }
  }

  /**
   * 获取会话列表
   */
  public async getConversationList(): Promise<ImConversation[]> {
    if (!this.isConnected || !this.sdk) {
      console.error('❌ [OpenIM SDK] 未连接');
      return [];
    }

    try {
      const result: WsResponse = await this.sdk.getAllConversationList();

      if (result.errCode === 0) {
        const conversations = JSON.parse(result.data) as ImConversation[];
        console.log('✅ [OpenIM SDK] 获取会话列表成功', conversations);
        return conversations;
      } else {
        throw new Error(result.errMsg || '获取失败');
      }
    } catch (error) {
      console.error('❌ [OpenIM SDK] 获取会话列表失败:', error);
      return [];
    }
  }

  /**
   * 获取历史消息
   */
  public async getHistoryMessages(conversationID: string, count = 20, startClientMsgID = ''): Promise<ImMessage[]> {
    if (!this.isConnected || !this.sdk) {
      console.error('❌ [OpenIM SDK] 未连接');
      return [];
    }

    try {
      console.log('📱 [OpenIM SDK] 获取历史消息...', { conversationID, count, startClientMsgID });

      // 如果conversationID不包含前缀，尝试作为groupID构建conversationID
      let actualConversationID = conversationID;
      if (!conversationID.includes('_')) {
        // 可能是裸groupID，需要先获取会话信息
        console.log('📱 [OpenIM SDK] conversationID格式不标准，尝试获取会话...');
        try {
          const convResult: WsResponse = await this.sdk.getOneConversation(JSON.stringify({
            sourceID: conversationID,
            sessionType: 3, // 群聊
          }));

          if (convResult.errCode === 0 && convResult.data) {
            const conversation = JSON.parse(convResult.data);
            actualConversationID = conversation.conversationID;
            console.log('✅ [OpenIM SDK] 获取到会话ID:', actualConversationID);
          } else if ((convResult as any).resp) {
            // OpenIM SDK v3.8新格式
            const conversation = (convResult as any).resp;
            actualConversationID = conversation.conversationID;
            console.log('✅ [OpenIM SDK] 获取到会话ID (resp字段):', actualConversationID);
          }
        } catch (convError) {
          console.warn('⚠️ [OpenIM SDK] 获取会话失败，使用原始ID:', convError);
        }
      }

      // OpenIM SDK期望参数为JSON字符串
      const params = {
        conversationID: actualConversationID,
        startClientMsgID,
        count,
        lastMinSeq: 0,
      };

      const result: any = await this.sdk.getAdvancedHistoryMessageList(params);
      console.log('🔍 [OpenIM SDK] getHistoryMessages原始响应:', JSON.stringify(result));

      // ✅ 修复：OpenIM SDK v3.8的响应格式 { operationID, event, data }
      // data可能是对象或字符串，不再有errCode字段
      if (result.data) {
        let data: any;

        // 判断data是对象还是字符串
        if (typeof result.data === 'object') {
          // v3.8新格式: data直接是对象
          data = result.data;
          console.log('✅ [OpenIM SDK] 获取历史消息成功 (data对象):', (data.messageList || []).length, '条');
        } else if (typeof result.data === 'string') {
          // 旧格式: data是JSON字符串
          data = JSON.parse(result.data);
          console.log('✅ [OpenIM SDK] 获取历史消息成功 (data字符串):', (data.messageList || []).length, '条');
        } else {
          console.warn('⚠️ [OpenIM SDK] 未知的data格式:', typeof result.data);
          return [];
        }

        const messages = (data.messageList || []) as ImMessage[];
        return messages;
      } else if (result.resp) {
        // 备用格式: resp字段
        const data = result.resp;
        const messages = (data.messageList || []) as ImMessage[];
        console.log('✅ [OpenIM SDK] 获取历史消息成功 (resp字段):', messages.length, '条');
        return messages;
      } else {
        console.error('❌ [OpenIM SDK] 获取历史消息失败:', result);
        return [];
      }
    } catch (error) {
      console.error('❌ [OpenIM SDK] 获取历史消息异常:', error);
      return [];
    }
  }

  /**
   * 标记消息已读
   */
  public async markMessageAsRead(conversationID: string, clientMsgIDList: string[]): Promise<boolean> {
    if (!this.isConnected || !this.sdk) {
      return false;
    }

    try {
      await this.sdk.markConversationMessageAsRead({
        conversationID,
        clientMsgIDList,
      });

      console.log('✅ [OpenIM SDK] 标记消息已读成功');
      return true;
    } catch (error) {
      console.error('❌ [OpenIM SDK] 标记消息已读失败:', error);
      return false;
    }
  }

  /**
   * 获取群组信息
   */
  public async getGroupInfo(groupID: string): Promise<ImGroupInfo | null> {
    if (!this.isConnected || !this.sdk) {
      return null;
    }

    try {
      // OpenIM SDK期望参数为数组，不需要stringify
      const result: any = await this.sdk.getSpecifiedGroupsInfo([groupID]);
      console.log('🔍 [OpenIM SDK] getGroupInfo原始响应:', JSON.stringify(result));

      // ✅ 修复：OpenIM SDK v3.8的响应格式 { operationID, event, data }
      // data可能是数组或对象，不再是JSON字符串，也没有errCode字段
      if (result.data) {
        let groups: ImGroupInfo[];

        // 判断data是数组还是字符串
        if (Array.isArray(result.data)) {
          // v3.8新格式: data直接是数组
          groups = result.data as ImGroupInfo[];
          console.log('✅ [OpenIM SDK] 获取群组信息成功 (data数组):', groups);
        } else if (typeof result.data === 'string') {
          // 旧格式: data是JSON字符串
          groups = JSON.parse(result.data) as ImGroupInfo[];
          console.log('✅ [OpenIM SDK] 获取群组信息成功 (data字符串):', groups);
        } else {
          console.warn('⚠️ [OpenIM SDK] 未知的data格式:', typeof result.data);
          return null;
        }

        return groups[0] || null;
      } else if (result.resp) {
        // 备用格式: resp字段
        const groups = result.resp as ImGroupInfo[];
        console.log('✅ [OpenIM SDK] 获取群组信息成功 (resp字段):', groups);
        return groups[0] || null;
      } else {
        console.warn('⚠️ [OpenIM SDK] 未找到群组数据:', result);
        return null;
      }
    } catch (error) {
      console.error('❌ [OpenIM SDK] 获取群组信息失败:', error);
      return null;
    }
  }

  /**
   * 注册事件监听器
   */
  public on(event: SdkEvent, callback: EventCallback): void {
    if (!this.eventListeners.has(event)) {
      this.eventListeners.set(event, []);
    }
    this.eventListeners.get(event)!.push(callback);
  }

  /**
   * 移除事件监听器
   */
  public off(event: SdkEvent, callback: EventCallback): void {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
      const index = listeners.indexOf(callback);
      if (index > -1) {
        listeners.splice(index, 1);
      }
    }
  }

  /**
   * 触发事件
   */
  private emit(event: SdkEvent, data?: any): void {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
      listeners.forEach(callback => {
        try {
          callback(data);
        } catch (error) {
          console.error('❌ [OpenIM SDK] 事件回调执行失败:', error);
        }
      });
    }
  }

  /**
   * 注册SDK内部事件监听器
   */
  private registerSdkEventListeners(): void {
    if (!this.sdk) {
      return;
    }

    console.log('📱 [OpenIM SDK] 注册SDK事件监听器...');

    // 连接状态监听
    this.sdk.on('onConnecting', () => {
      console.log('🔄 [OpenIM SDK] 连接中...');
      this.emit(SdkEvent.CONNECTING);
    });

    this.sdk.on('onConnectSuccess', () => {
      console.log('✅ [OpenIM SDK] 连接成功');
      this.isConnected = true;
      this.reconnectAttempts = 0;
      this.emit(SdkEvent.CONNECTED);
    });

    this.sdk.on('onConnectFailed', ({ errCode, errMsg }: any) => {
      console.error('❌ [OpenIM SDK] 连接失败:', errCode, errMsg);
      this.isConnected = false;
      this.emit(SdkEvent.DISCONNECTED);
      this.scheduleReconnect();
    });

    this.sdk.on('onKickedOffline', () => {
      console.warn('⚠️ [OpenIM SDK] 被踢下线');
      this.isConnected = false;
      this.emit(SdkEvent.DISCONNECTED);
      antMessage.warning('您的账号在其他设备登录');
    });

    this.sdk.on('onUserTokenExpired', () => {
      console.warn('⚠️ [OpenIM SDK] Token过期');
      this.isConnected = false;
      this.emit(SdkEvent.DISCONNECTED);
      antMessage.warning('登录已过期,请重新登录');
    });

    // 消息事件监听
    this.sdk.on('onRecvNewMessage', (data: any) => {
      console.log('📩 [OpenIM SDK] 收到新消息:', data);
      const message = JSON.parse(data.data) as ImMessage;
      this.emit(SdkEvent.NEW_MESSAGE, message);
    });

    this.sdk.on('onRecvMessageRevoked', (data: any) => {
      console.log('🔙 [OpenIM SDK] 消息被撤回:', data);
      this.emit(SdkEvent.MESSAGE_REVOKED, data);
    });

    // 会话事件监听
    this.sdk.on('onConversationChanged', (data: any) => {
      console.log('🔄 [OpenIM SDK] 会话变更:', data);
      const conversations = JSON.parse(data.data) as ImConversation[];
      this.emit(SdkEvent.CONVERSATION_CHANGED, conversations);
    });

    this.sdk.on('onNewConversation', (data: any) => {
      console.log('✨ [OpenIM SDK] 新会话:', data);
      const conversations = JSON.parse(data.data) as ImConversation[];
      this.emit(SdkEvent.NEW_CONVERSATION, conversations);
    });

    this.sdk.on('onTotalUnreadMessageCountChanged', (data: any) => {
      console.log('📬 [OpenIM SDK] 未读数变更:', data);
      this.emit(SdkEvent.TOTAL_UNREAD_CHANGED, data);
    });

    // 群组事件监听
    this.sdk.on('onGroupInfoChanged', (data: any) => {
      console.log('👥 [OpenIM SDK] 群组信息变更:', data);
      this.emit(SdkEvent.GROUP_INFO_CHANGED, data);
    });

    this.sdk.on('onJoinedGroupAdded', (data: any) => {
      console.log('➕ [OpenIM SDK] 加入新群组:', data);
      this.emit(SdkEvent.MEMBER_ADDED, data);
    });

    this.sdk.on('onJoinedGroupDeleted', (data: any) => {
      console.log('➖ [OpenIM SDK] 退出群组:', data);
      this.emit(SdkEvent.MEMBER_DELETED, data);
    });

    console.log('✅ [OpenIM SDK] SDK事件监听器注册完成');
  }

  /**
   * 调度重连
   */
  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('❌ [OpenIM SDK] 重连次数超过上限,停止重连');
      antMessage.error('连接失败,请检查网络后重试');
      return;
    }

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
    }

    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000); // 指数退避,最大30秒
    console.log(`🔄 [OpenIM SDK] 将在 ${delay}ms 后重连 (第${this.reconnectAttempts + 1}次)`);

    this.emit(SdkEvent.RECONNECTING);

    this.reconnectTimer = window.setTimeout(async () => {
      this.reconnectAttempts++;

      if (this.currentConfig) {
        const success = await this.login(this.currentConfig.userID, this.currentConfig.token);
        if (!success) {
          this.scheduleReconnect();
        }
      }
    }, delay);
  }

  /**
   * 获取连接状态
   */
  public getConnectionStatus(): ImConnectStatus {
    if (!this.isInitialized) {
      return 3; // DISCONNECTED
    }
    if (this.isConnected) {
      return 2; // CONNECTED
    }
    if (this.reconnectTimer) {
      return 4; // RECONNECTING
    }
    return 1; // CONNECTING
  }

  /**
   * 解析conversationID
   * conversationID格式:
   * - 单聊: "single_<userID>" 或 "si_<userID>"
   * - 群聊: "group_<groupID>" 或 "sg_<groupID>"
   */
  private parseConversationID(conversationID: string): { recvID: string; groupID: string } {
    if (conversationID.startsWith('group_') || conversationID.startsWith('sg_')) {
      // 群聊 - 支持 group_ 和 sg_ 两种前缀
      const groupID = conversationID.replace(/^(group_|sg_)/, '');
      return {
        recvID: '',
        groupID,
      };
    } else if (conversationID.startsWith('single_') || conversationID.startsWith('si_')) {
      // 单聊 - 支持 single_ 和 si_ 两种前缀
      const recvID = conversationID.replace(/^(single_|si_)/, '');
      return {
        recvID,
        groupID: '',
      };
    } else {
      // 兼容直接传groupID的情况
      console.warn('⚠️ [OpenIM SDK] conversationID格式不规范,假定为groupID:', conversationID);
      return {
        recvID: '',
        groupID: conversationID,
      };
    }
  }

  /**
   * 销毁实例
   */
  public async destroy(): Promise<void> {
    console.log('📱 [OpenIM SDK] 销毁SDK实例...');

    await this.logout();

    this.eventListeners.clear();
    this.isInitialized = false;
    this.isConnected = false;
    this.currentConfig = null;
    this.sdk = null;

    OpenIMSDKWrapper.instance = null;

    console.log('✅ [OpenIM SDK] SDK实例已销毁');
  }
}

/**
 * 导出单例实例的便捷访问方法
 */
export const getOpenIMSDK = () => OpenIMSDKWrapper.getInstance();
