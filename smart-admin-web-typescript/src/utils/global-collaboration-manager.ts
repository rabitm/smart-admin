/**
 * 全局协作管理器 - 统一WebSocket连接和状态管理
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-09-25
 * @Copyright 1024创新实验室
 */

import { ref, reactive } from 'vue';
import { message } from 'ant-design-vue';
import { postRequest, getRequest } from '/@/lib/axios';

interface CollaborationUser {
  id: string;
  name: string;
  avatar?: string;
  color: string;
  sessionId: string;
  lastActivity: number;
  isOnline: boolean;
}

interface UserCursor {
  userId: string;
  fieldName: string;
  position: {
    x: number;
    y: number;
    width: number;
    height: number;
    element: HTMLElement;
  };
  caretPosition?: number;
  isActive: boolean;
  timestamp: number;
}

interface CollaborationState {
  isConnected: boolean;
  connectionStatus: 'connected' | 'disconnected' | 'connecting';
  activeUsers: CollaborationUser[];
  userCursors: UserCursor[];
  currentRoom: string | null;
  currentUser: CollaborationUser | null;
}

class GlobalCollaborationManager {
  private websocket: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectTimer: NodeJS.Timeout | null = null;
  private heartbeatTimer: NodeJS.Timeout | null = null;

  // 全局协作状态
  public state = reactive<CollaborationState>({
    isConnected: false,
    connectionStatus: 'disconnected',
    activeUsers: [],
    userCursors: [],
    currentRoom: null,
    currentUser: null
  });

  // 事件监听器
  private eventListeners: {
    [event: string]: Array<(data: any) => void>;
  } = {};

  /**
   * 初始化协作管理器
   */
  public initialize(currentUser: CollaborationUser) {
    this.state.currentUser = currentUser;
    console.log('🔧 [Global Collaboration] 初始化协作管理器', currentUser);
  }

  /**
   * 加入协作房间（暂时使用REST接口）
   */
  public async joinRoom(roomId: string, wsUrl?: string) {
    if (this.state.currentRoom === roomId) {
      console.log('✅ [Global Collaboration] 已在房间中:', roomId);
      return;
    }

    this.state.currentRoom = roomId;
    this.state.connectionStatus = 'connecting';

    try {
      // 使用REST API加入房间
      await this.joinRoomViaRest(roomId);
      this.state.connectionStatus = 'connected';
      this.state.isConnected = true;

      // 启动定时获取用户列表
      this.startPollingUsers(roomId);

      console.log('✅ [Global Collaboration] 成功加入房间 (REST模式):', roomId);
      this.emit('connected', { room: roomId });
    } catch (error) {
      console.error('❌ [Global Collaboration] 加入房间失败:', error);
      this.state.connectionStatus = 'disconnected';
      this.state.isConnected = false;
    }
  }

  /**
   * 离开协作房间
   */
  public async leaveRoom() {
    const currentRoom = this.state.currentRoom;

    if (this.websocket) {
      this.websocket.close();
      this.websocket = null;
    }

    this.stopPollingUsers();
    this.stopHeartbeat();
    this.clearReconnectTimer();

    // 调用 REST API 通知后端用户离开
    if (currentRoom) {
      await this.leaveRoomViaRest(currentRoom);
    }

    this.state.currentRoom = null;
    this.state.isConnected = false;
    this.state.connectionStatus = 'disconnected';
    this.state.activeUsers = [];
    this.state.userCursors = [];

    console.log('👋 [Global Collaboration] 离开协作房间');
  }

  /**
   * REST方式加入房间
   */
  private async joinRoomViaRest(roomId: string) {
    // 不再传递用户信息，后端会自动从当前登录会话中获取真实用户信息
    const result = await postRequest(`/api/collaboration/join/${roomId}`, {});
    console.log('🚀 [Global Collaboration] 加入房间响应:', result);
  }

  /**
   * 离开房间REST接口
   */
  private async leaveRoomViaRest(roomId: string) {
    try {
      // 不再传递用户信息，后端会自动从当前登录会话中获取真实用户信息
      const result = await postRequest(`/api/collaboration/leave/${roomId}`, {});
      console.log('👋 [Global Collaboration] 离开房间成功:', result);
    } catch (error) {
      console.error('❌ [Global Collaboration] 离开房间错误:', error);
    }
  }

  /**
   * 定时轮询用户列表
   */
  private pollingTimer: NodeJS.Timeout | null = null;

  private startPollingUsers(roomId: string) {
    if (this.pollingTimer) return;

    this.pollingTimer = setInterval(async () => {
      try {
        // 获取用户列表
        const result = await getRequest(`/api/collaboration/users/${roomId}`);
        console.log('👥 [Global Collaboration] 获取用户列表响应:', result);
        if (result.ok && Array.isArray(result.data)) {
          // 过滤掉当前用户，只显示其他协作用户
          const otherUsers = result.data.filter(user => user.id !== this.state.currentUser?.id);
          this.state.activeUsers = otherUsers;
          console.log('👥 [Global Collaboration] 更新活跃用户列表:', otherUsers);
          this.emit('users_updated', otherUsers);
        } else {
          console.warn('⚠️ [Global Collaboration] API响应格式不正确:', result);
        }

        // 发送心跳 - 不再传递用户信息，后端会自动从当前登录会话中获取真实用户信息
        await postRequest(`/api/collaboration/heartbeat/${roomId}`, {});

      } catch (error) {
        console.error('❌ [Global Collaboration] 轮询用户列表失败:', error);
      }
    }, 3000); // 每3秒轮询一次
  }

  private stopPollingUsers() {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
      this.pollingTimer = null;
    }
  }

  /**
   * 建立WebSocket连接（暂时禁用）
   */
  private async connectWebSocket(url: string) {
    if (this.websocket?.readyState === WebSocket.OPEN) {
      return;
    }

    this.state.connectionStatus = 'connecting';
    console.log('🔌 [Global Collaboration] 尝试连接:', url);

    try {
      this.websocket = new WebSocket(url);

      this.websocket.onopen = () => {
        console.log('✅ [Global Collaboration] WebSocket连接成功');
        this.state.isConnected = true;
        this.state.connectionStatus = 'connected';
        this.reconnectAttempts = 0;

        this.startHeartbeat();
        this.emit('connected', { room: this.state.currentRoom });

        // 发送加入房间消息
        this.sendMessage({
          type: 'join_room',
          room: this.state.currentRoom,
          user: this.state.currentUser
        });
      };

      this.websocket.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          this.handleMessage(data);
        } catch (error) {
          console.error('❌ [Global Collaboration] 解析消息失败:', error);
        }
      };

      this.websocket.onclose = (event) => {
        console.log('🔌 [Global Collaboration] WebSocket连接关闭', event.code, event.reason);
        this.state.isConnected = false;
        this.state.connectionStatus = 'disconnected';

        this.stopHeartbeat();
        this.emit('disconnected', { code: event.code, reason: event.reason });

        // 自动重连
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.scheduleReconnect(url);
        }
      };

      this.websocket.onerror = (error) => {
        console.error('❌ [Global Collaboration] WebSocket连接错误:', error);
        this.state.connectionStatus = 'disconnected';
        this.emit('error', error);
      };

    } catch (error) {
      console.error('❌ [Global Collaboration] WebSocket初始化失败:', error);
      this.state.connectionStatus = 'disconnected';
    }
  }

  /**
   * 处理WebSocket消息
   */
  private handleMessage(data: any) {
    switch (data.type) {
      case 'user_joined':
        this.handleUserJoined(data.user);
        break;

      case 'user_left':
        this.handleUserLeft(data.user);
        break;

      case 'users_list':
        this.state.activeUsers = data.users || [];
        this.emit('users_updated', this.state.activeUsers);
        break;

      case 'cursor_update':
        this.handleCursorUpdate(data);
        break;

      case 'pong':
        // 心跳响应
        break;

      default:
        this.emit('message', data);
    }
  }

  /**
   * 处理用户加入
   */
  private handleUserJoined(user: CollaborationUser) {
    const existingIndex = this.state.activeUsers.findIndex(u => u.id === user.id);
    if (existingIndex === -1) {
      this.state.activeUsers.push(user);
      message.info(`${user.name} 加入了协作`);
    }
    this.emit('user_joined', user);
  }

  /**
   * 处理用户离开
   */
  private handleUserLeft(user: CollaborationUser) {
    this.state.activeUsers = this.state.activeUsers.filter(u => u.id !== user.id);
    this.state.userCursors = this.state.userCursors.filter(c => c.userId !== user.id);
    message.info(`${user.name} 离开了协作`);
    this.emit('user_left', user);
  }

  /**
   * 处理光标更新
   */
  private handleCursorUpdate(data: any) {
    const existingIndex = this.state.userCursors.findIndex(
      c => c.userId === data.userId && c.fieldName === data.fieldName
    );

    if (existingIndex !== -1) {
      this.state.userCursors[existingIndex] = { ...this.state.userCursors[existingIndex], ...data };
    } else {
      this.state.userCursors.push(data);
    }

    this.emit('cursor_updated', data);
  }

  /**
   * 发送消息
   */
  public sendMessage(data: any) {
    if (this.websocket?.readyState === WebSocket.OPEN) {
      this.websocket.send(JSON.stringify(data));
    } else {
      console.warn('⚠️ [Global Collaboration] WebSocket未连接，消息未发送:', data);
    }
  }

  /**
   * 发送光标位置
   */
  public sendCursorPosition(fieldName: string, position: any, caretPosition?: number) {
    this.sendMessage({
      type: 'cursor_update',
      room: this.state.currentRoom,
      userId: this.state.currentUser?.id,
      fieldName,
      position,
      caretPosition,
      timestamp: Date.now()
    });
  }

  /**
   * 心跳机制
   */
  private startHeartbeat() {
    if (this.heartbeatTimer) return;

    this.heartbeatTimer = setInterval(() => {
      if (this.websocket?.readyState === WebSocket.OPEN) {
        this.sendMessage({ type: 'ping', timestamp: Date.now() });
      }
    }, 30000);
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  /**
   * 重连机制
   */
  private scheduleReconnect(url: string) {
    if (this.reconnectTimer) return;

    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
    this.reconnectAttempts++;

    console.log(`🔄 [Global Collaboration] ${delay}ms后尝试重连 (第${this.reconnectAttempts}次)`);

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connectWebSocket(url);
    }, delay);
  }

  private clearReconnectTimer() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  /**
   * 构建默认WebSocket URL
   */
  private buildDefaultWsUrl(roomId: string): string {
    const userId = this.state.currentUser?.id || 'anonymous';
    return `ws://localhost:1024/api/collaboration/ws?roomId=${roomId}&userId=${userId}`;
  }

  /**
   * 事件监听
   */
  public on(event: string, callback: (data: any) => void) {
    if (!this.eventListeners[event]) {
      this.eventListeners[event] = [];
    }
    this.eventListeners[event].push(callback);
  }

  public off(event: string, callback: (data: any) => void) {
    if (this.eventListeners[event]) {
      this.eventListeners[event] = this.eventListeners[event].filter(cb => cb !== callback);
    }
  }

  private emit(event: string, data: any) {
    if (this.eventListeners[event]) {
      this.eventListeners[event].forEach(callback => callback(data));
    }
  }

  /**
   * 获取连接状态
   */
  public getConnectionStatus() {
    return this.state.connectionStatus;
  }

  /**
   * 获取当前房间活跃用户
   */
  public getActiveUsers() {
    return this.state.activeUsers;
  }

  /**
   * 获取用户光标
   */
  public getUserCursors() {
    return this.state.userCursors;
  }
}

// 导出单例实例
export const globalCollaborationManager = new GlobalCollaborationManager();
export default globalCollaborationManager;