/**
 * IM服务层 - 统一管理IM相关业务逻辑
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */

import { ref, computed, onUnmounted } from 'vue';
import { message as antMessage } from 'ant-design-vue';
import { getOpenIMSDK, SdkEvent } from '/@/utils/openim-sdk-wrapper';
import { getImTokenApi, refreshImTokenApi } from '/@/api/im/im-auth-api';
import { getPoliceGroupApi, inviteMembersApi } from '/@/api/im/im-group-api';
import type {
  ImConfig,
  ImMessage,
  ImConversation,
  ImGroupInfo,
  ImConnectStatus,
  PoliceGroupMapping,
  InviteMembersForm
} from '/@/types/im';

// 重新导出SDK实例获取函数，供组件直接使用
export { getOpenIMSDK };

/**
 * IM连接状态管理
 */
export const useIMConnection = () => {
  const sdk = getOpenIMSDK();
  const connectionStatus = ref<ImConnectStatus>(3); // 默认未连接
  const isConnecting = ref(false);
  const currentUserID = ref<string>('0'); // 初始值设为'0',连接后会更新为真实userID

  /**
   * 初始化并登录IM
   */
  const connect = async (): Promise<boolean> => {
    try {
      isConnecting.value = true;
      connectionStatus.value = 1; // 连接中
      console.log('📱 [IM服务] 开始连接IM...');

      // 1. 获取Token
      const tokenResult = await getImTokenApi();
      if (!tokenResult.data) {
        throw new Error('获取IM Token失败');
      }

      const tokenInfo = tokenResult.data;
      currentUserID.value = tokenInfo.userID;

      // 2. 构建配置
      const config: ImConfig = {
        apiUrl: tokenInfo.apiUrl,
        wsUrl: tokenInfo.wsUrl,
        platformID: tokenInfo.platformID,
        userID: tokenInfo.userID,
        token: tokenInfo.userToken,
      };

      // 3. 初始化SDK
      const initSuccess = await sdk.init(config);
      if (!initSuccess) {
        throw new Error('SDK初始化失败');
      }

      // 4. 登录
      const loginSuccess = await sdk.login(config.userID, config.token);
      if (!loginSuccess) {
        throw new Error('登录失败');
      }

      // 5. 手动更新连接状态为已连接
      connectionStatus.value = 2; // 已连接

      console.log('✅ [IM服务] IM连接成功');
      antMessage.success('IM连接成功');
      return true;
    } catch (error: any) {
      console.error('❌ [IM服务] IM连接失败:', error);

      // 详细错误信息
      let errorMsg = 'IM连接失败';
      if (error?.response?.data?.msg) {
        errorMsg = error.response.data.msg;
        console.error('📱 [IM服务] 后端错误:', error.response.data);
      } else if (error?.message) {
        errorMsg = error.message;
      }

      connectionStatus.value = 3; // 未连接
      antMessage.error(errorMsg);
      return false;
    } finally {
      isConnecting.value = false;
    }
  };

  /**
   * 断开连接
   */
  const disconnect = async (): Promise<void> => {
    await sdk.logout();
    currentUserID.value = '';
  };

  /**
   * 刷新Token
   */
  const refreshToken = async (): Promise<boolean> => {
    try {
      const result = await refreshImTokenApi();
      if (!result.data) {
        return false;
      }

      const tokenInfo = result.data;

      // 重新登录
      return await sdk.login(tokenInfo.userID, tokenInfo.userToken);
    } catch (error) {
      console.error('❌ [IM服务] 刷新Token失败:', error);
      return false;
    }
  };

  /**
   * 监听连接状态
   */
  const onConnectionStatusChange = (callback: (status: ImConnectStatus) => void) => {
    const handleConnecting = () => {
      connectionStatus.value = 1;
      callback(1);
    };

    const handleConnected = () => {
      connectionStatus.value = 2;
      callback(2);
    };

    const handleDisconnected = () => {
      connectionStatus.value = 3;
      callback(3);
    };

    const handleReconnecting = () => {
      connectionStatus.value = 4;
      callback(4);
    };

    sdk.on(SdkEvent.CONNECTING, handleConnecting);
    sdk.on(SdkEvent.CONNECTED, handleConnected);
    sdk.on(SdkEvent.DISCONNECTED, handleDisconnected);
    sdk.on(SdkEvent.RECONNECTING, handleReconnecting);

    // 返回清理函数
    return () => {
      sdk.off(SdkEvent.CONNECTING, handleConnecting);
      sdk.off(SdkEvent.CONNECTED, handleConnected);
      sdk.off(SdkEvent.DISCONNECTED, handleDisconnected);
      sdk.off(SdkEvent.RECONNECTING, handleReconnecting);
    };
  };

  const isConnected = computed(() => connectionStatus.value === 2);

  return {
    connectionStatus,
    isConnecting,
    isConnected,
    currentUserID,
    connect,
    disconnect,
    refreshToken,
    onConnectionStatusChange,
  };
};

/**
 * IM消息管理
 */
export const useIMMessages = (conversationID: string) => {
  const sdk = getOpenIMSDK();
  const messages = ref<ImMessage[]>([]);
  const isLoading = ref(false);
  const hasMore = ref(true);

  /**
   * 加载历史消息
   */
  const loadHistory = async (count = 20): Promise<void> => {
    if (isLoading.value || !hasMore.value) {
      return;
    }

    try {
      isLoading.value = true;

      const startClientMsgID = messages.value.length > 0 ? messages.value[0].clientMsgID : '';
      const historyMessages = await sdk.getHistoryMessages(conversationID, count, startClientMsgID);

      if (historyMessages.length === 0) {
        hasMore.value = false;
      } else {
        // 历史消息添加到列表前面
        messages.value = [...historyMessages.reverse(), ...messages.value];
      }
    } catch (error) {
      console.error('❌ [IM服务] 加载历史消息失败:', error);
      antMessage.error('加载历史消息失败');
    } finally {
      isLoading.value = false;
    }
  };

  /**
   * 发送文本消息
   */
  const sendText = async (text: string): Promise<boolean> => {
    if (!text.trim()) {
      return false;
    }

    const message = await sdk.sendTextMessage(conversationID, text.trim());
    if (message) {
      messages.value.push(message);
      return true;
    }
    return false;
  };

  /**
   * 发送图片消息
   */
  const sendImage = async (imageFile: File): Promise<boolean> => {
    const message = await sdk.sendImageMessage(conversationID, imageFile);
    if (message) {
      messages.value.push(message);
      return true;
    }
    return false;
  };

  /**
   * 发送文件消息
   */
  const sendFile = async (file: File): Promise<boolean> => {
    const message = await sdk.sendFileMessage(conversationID, file);
    if (message) {
      messages.value.push(message);
      return true;
    }
    return false;
  };

  /**
   * 标记消息已读
   */
  const markAsRead = async (): Promise<void> => {
    if (messages.value.length === 0) {
      return;
    }

    const unreadMessages = messages.value.filter(msg => !msg.isRead);
    if (unreadMessages.length === 0) {
      return;
    }

    const msgIDs = unreadMessages.map(msg => msg.clientMsgID);
    await sdk.markMessageAsRead(conversationID, msgIDs);

    // 更新本地状态
    unreadMessages.forEach(msg => {
      msg.isRead = true;
    });
  };

  /**
   * 监听新消息
   */
  const onNewMessage = (callback: (message: ImMessage) => void) => {
    const handleNewMessage = (message: ImMessage) => {
      // 只处理当前会话的消息
      if (message.groupID === conversationID || message.sendID === conversationID || message.recvID === conversationID) {
        messages.value.push(message);
        callback(message);
      }
    };

    sdk.on(SdkEvent.NEW_MESSAGE, handleNewMessage);

    // 返回清理函数
    return () => {
      sdk.off(SdkEvent.NEW_MESSAGE, handleNewMessage);
    };
  };

  /**
   * 监听消息撤回
   */
  const onMessageRevoked = (callback: (data: any) => void) => {
    const handleRevoked = (data: any) => {
      // 从列表中移除被撤回的消息
      const index = messages.value.findIndex(msg => msg.clientMsgID === data.clientMsgID);
      if (index > -1) {
        messages.value.splice(index, 1);
      }
      callback(data);
    };

    sdk.on(SdkEvent.MESSAGE_REVOKED, handleRevoked);

    return () => {
      sdk.off(SdkEvent.MESSAGE_REVOKED, handleRevoked);
    };
  };

  return {
    messages,
    isLoading,
    hasMore,
    loadHistory,
    sendText,
    sendImage,
    sendFile,
    markAsRead,
    onNewMessage,
    onMessageRevoked,
  };
};

/**
 * IM会话管理
 */
export const useIMConversations = () => {
  const sdk = getOpenIMSDK();
  const conversations = ref<ImConversation[]>([]);
  const totalUnreadCount = ref(0);

  /**
   * 加载会话列表
   */
  const loadConversations = async (): Promise<void> => {
    try {
      const list = await sdk.getConversationList();
      conversations.value = list;
    } catch (error) {
      console.error('❌ [IM服务] 加载会话列表失败:', error);
    }
  };

  /**
   * 监听会话变更
   */
  const onConversationChange = (callback: (conversations: ImConversation[]) => void) => {
    const handleChange = (changedConversations: ImConversation[]) => {
      // 更新会话列表
      changedConversations.forEach(changed => {
        const index = conversations.value.findIndex(conv => conv.conversationID === changed.conversationID);
        if (index > -1) {
          conversations.value[index] = changed;
        }
      });
      callback(changedConversations);
    };

    sdk.on(SdkEvent.CONVERSATION_CHANGED, handleChange);

    return () => {
      sdk.off(SdkEvent.CONVERSATION_CHANGED, handleChange);
    };
  };

  /**
   * 监听新会话
   */
  const onNewConversation = (callback: (conversations: ImConversation[]) => void) => {
    const handleNew = (newConversations: ImConversation[]) => {
      conversations.value.push(...newConversations);
      callback(newConversations);
    };

    sdk.on(SdkEvent.NEW_CONVERSATION, handleNew);

    return () => {
      sdk.off(SdkEvent.NEW_CONVERSATION, handleNew);
    };
  };

  /**
   * 监听未读数变更
   */
  const onUnreadCountChange = (callback: (count: number) => void) => {
    const handleChange = (count: number) => {
      totalUnreadCount.value = count;
      callback(count);
    };

    sdk.on(SdkEvent.TOTAL_UNREAD_CHANGED, handleChange);

    return () => {
      sdk.off(SdkEvent.TOTAL_UNREAD_CHANGED, handleChange);
    };
  };

  return {
    conversations,
    totalUnreadCount,
    loadConversations,
    onConversationChange,
    onNewConversation,
    onUnreadCountChange,
  };
};

/**
 * IM群组管理
 */
export const useIMGroup = () => {
  const sdk = getOpenIMSDK();

  /**
   * 获取群组信息
   */
  const getGroupInfo = async (groupID: string): Promise<ImGroupInfo | null> => {
    return await sdk.getGroupInfo(groupID);
  };

  /**
   * 获取警情群组信息
   */
  const getPoliceGroup = async (reportId: number): Promise<PoliceGroupMapping | null> => {
    try {
      const result = await getPoliceGroupApi(reportId);
      return result.data || null;
    } catch (error: any) {
      // 404表示群组不存在，这是正常情况，不输出错误
      if (error?.response?.status !== 404) {
        console.error('❌ [IM服务] 获取警情群组失败:', error?.message || error);
      }
      return null;
    }
  };

  /**
   * 邀请成员加入警情群组
   */
  const inviteMembers = async (data: InviteMembersForm): Promise<boolean> => {
    try {
      await inviteMembersApi(data);
      antMessage.success('成员邀请成功');
      return true;
    } catch (error) {
      console.error('❌ [IM服务] 邀请成员失败:', error);
      antMessage.error('邀请成员失败');
      return false;
    }
  };

  return {
    getGroupInfo,
    getPoliceGroup,
    inviteMembers,
  };
};

/**
 * IM通用Composable - 自动管理连接和清理
 */
export const useIM = () => {
  const connection = useIMConnection();
  const conversations = useIMConversations();
  const group = useIMGroup();

  // 自动清理
  onUnmounted(() => {
    console.log('📱 [IM服务] 组件卸载,清理IM资源');
  });

  return {
    // 连接管理
    ...connection,

    // 会话管理
    conversations: conversations.conversations,
    totalUnreadCount: conversations.totalUnreadCount,
    loadConversations: conversations.loadConversations,
    onConversationChange: conversations.onConversationChange,
    onNewConversation: conversations.onNewConversation,
    onUnreadCountChange: conversations.onUnreadCountChange,

    // 群组管理
    getGroupInfo: group.getGroupInfo,
    getPoliceGroup: group.getPoliceGroup,
    inviteMembers: group.inviteMembers,
  };
};
