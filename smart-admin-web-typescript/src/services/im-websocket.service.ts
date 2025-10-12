/**
 * IM WebSocket服务
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-09
 */

import { ref, onUnmounted } from 'vue';
import { getWebSocketClient } from '/@/utils/websocket-manager';
import type { IWebSocketClient, WebSocketMessage } from '/@/types/websocket';

export interface IMMessage {
  reportId: number;
  messageId: string;
  senderId: string;
  senderName: string;
  content: string;
  contentType: number;
  sendTime: number;
  isSelf: boolean;
}

export interface IMMessageHandler {
  (message: IMMessage): void;
}

export interface IMErrorHandler {
  (error: string): void;
}

/**
 * IM WebSocket服务类
 */
class IMWebSocketService {
  private messageHandlers: Map<number, Set<IMMessageHandler>> = new Map();
  private messageSentHandlers: Set<IMMessageHandler> = new Set();
  private errorHandlers: Set<IMErrorHandler> = new Set();
  private initialized = false;

  /**
   * 获取WebSocket客户端
   */
  private getClient(): IWebSocketClient | null {
    return getWebSocketClient();
  }

  /**
   * 初始化IM WebSocket服务
   */
  initialize() {
    if (this.initialized) {
      return;
    }

    console.log('📡 [IM WebSocket] 初始化服务');

    const client = this.getClient();
    if (!client) {
      console.warn('⚠️ [IM WebSocket] WebSocket客户端尚未初始化');
      return;
    }

    // 监听新消息
    client.onModuleMessage('im', 'NEW_MESSAGE', (message: WebSocketMessage) => {
      console.log('📨 [IM WebSocket] 收到新消息:', message);
      this.handleNewMessage(message);
    });

    // 监听消息发送成功
    client.onModuleMessage('im', 'MESSAGE_SENT', (message: WebSocketMessage) => {
      console.log('✅ [IM WebSocket] 消息发送成功:', message);
      this.handleMessageSent(message);
    });

    // 监听消息发送失败
    client.onModuleMessage('im', 'MESSAGE_ERROR', (message: WebSocketMessage) => {
      console.log('❌ [IM WebSocket] 消息发送失败:', message);
      this.handleMessageError(message);
    });

    this.initialized = true;
  }

  /**
   * 订阅警情消息
   */
  subscribeReport(reportId: number, handler: IMMessageHandler) {
    console.log(`📥 [IM WebSocket] 订阅警情${reportId}`);

    if (!this.messageHandlers.has(reportId)) {
      this.messageHandlers.set(reportId, new Set());
    }

    this.messageHandlers.get(reportId)!.add(handler);
  }

  /**
   * 取消订阅警情消息
   */
  unsubscribeReport(reportId: number, handler?: IMMessageHandler) {
    console.log(`📤 [IM WebSocket] 取消订阅警情${reportId}`);

    if (handler) {
      const handlers = this.messageHandlers.get(reportId);
      if (handlers) {
        handlers.delete(handler);
        if (handlers.size === 0) {
          this.messageHandlers.delete(reportId);
        }
      }
    } else {
      this.messageHandlers.delete(reportId);
    }
  }

  /**
   * 监听消息发送成功事件
   */
  onMessageSent(handler: IMMessageHandler) {
    this.messageSentHandlers.add(handler);
  }

  /**
   * 取消监听消息发送成功事件
   */
  offMessageSent(handler: IMMessageHandler) {
    this.messageSentHandlers.delete(handler);
  }

  /**
   * 监听错误事件
   */
  onError(handler: IMErrorHandler) {
    this.errorHandlers.add(handler);
  }

  /**
   * 取消监听错误事件
   */
  offError(handler: IMErrorHandler) {
    this.errorHandlers.delete(handler);
  }

  /**
   * 处理新消息
   */
  private handleNewMessage(message: WebSocketMessage) {
    const data = message.data as any;
    const reportId = data.reportId;

    if (!reportId) {
      console.warn('⚠️ [IM WebSocket] 消息缺少reportId');
      return;
    }

    const imMessage: IMMessage = {
      reportId: data.reportId,
      messageId: data.messageId,
      senderId: data.senderId,
      senderName: data.senderName,
      content: data.content,
      contentType: data.contentType,
      sendTime: data.sendTime,
      isSelf: data.isSelf || false,
    };

    // 调用对应警情的所有处理器
    const handlers = this.messageHandlers.get(reportId);
    if (handlers && handlers.size > 0) {
      console.log(`📨 [IM WebSocket] 分发消息给${handlers.size}个处理器`);
      handlers.forEach((handler) => {
        try {
          handler(imMessage);
        } catch (error) {
          console.error('❌ [IM WebSocket] 消息处理器执行失败:', error);
        }
      });
    } else {
      console.log(`⚠️ [IM WebSocket] 警情${reportId}没有注册处理器`);
    }
  }

  /**
   * 处理消息发送成功
   */
  private handleMessageSent(message: WebSocketMessage) {
    const data = message.data as any;

    const imMessage: IMMessage = {
      reportId: data.reportId,
      messageId: data.messageId,
      senderId: data.senderId,
      senderName: data.senderName,
      content: data.content,
      contentType: data.contentType,
      sendTime: data.sendTime,
      isSelf: true, // 自己发送的消息
    };

    this.messageSentHandlers.forEach((handler) => {
      try {
        handler(imMessage);
      } catch (error) {
        console.error('❌ [IM WebSocket] 消息发送成功处理器执行失败:', error);
      }
    });
  }

  /**
   * 处理消息发送失败
   */
  private handleMessageError(message: WebSocketMessage) {
    const data = message.data as any;
    const errorMessage = data.error || '消息发送失败';

    this.errorHandlers.forEach((handler) => {
      try {
        handler(errorMessage);
      } catch (error) {
        console.error('❌ [IM WebSocket] 错误处理器执行失败:', error);
      }
    });
  }

  /**
   * 清理所有订阅
   */
  cleanup() {
    console.log('🧹 [IM WebSocket] 清理所有订阅');
    this.messageHandlers.clear();
    this.messageSentHandlers.clear();
    this.errorHandlers.clear();
  }
}

// 导出单例
export const imWebSocketService = new IMWebSocketService();

/**
 * Vue组件中使用的Hook
 */
export function useIMWebSocket(reportId: number) {
  const messages = ref<IMMessage[]>([]);
  const isConnected = ref(false);

  // 初始化服务
  imWebSocketService.initialize();

  // 消息处理器
  const handleNewMessage = (message: IMMessage) => {
    console.log('📨 [useIMWebSocket] 收到新消息:', message);
    messages.value.push(message);
  };

  // 消息发送成功处理器
  const handleMessageSent = (message: IMMessage) => {
    console.log('✅ [useIMWebSocket] 消息发送成功:', message);
    // 检查是否已存在（避免重复添加）
    const exists = messages.value.some((m) => m.messageId === message.messageId);
    if (!exists) {
      messages.value.push(message);
    }
  };

  // 错误处理器
  const handleError = (error: string) => {
    console.error('❌ [useIMWebSocket] 发生错误:', error);
    // 可以在这里显示错误提示
  };

  // 订阅
  imWebSocketService.subscribeReport(reportId, handleNewMessage);
  imWebSocketService.onMessageSent(handleMessageSent);
  imWebSocketService.onError(handleError);

  // 监听连接状态
  const client = getWebSocketClient();
  isConnected.value = client?.isConnected ?? false;

  // 组件卸载时清理
  onUnmounted(() => {
    console.log('🧹 [useIMWebSocket] 组件卸载，清理订阅');
    imWebSocketService.unsubscribeReport(reportId, handleNewMessage);
    imWebSocketService.offMessageSent(handleMessageSent);
    imWebSocketService.offError(handleError);
  });

  return {
    messages,
    isConnected,
  };
}
