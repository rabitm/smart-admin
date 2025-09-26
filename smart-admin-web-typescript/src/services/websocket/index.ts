/*
 * WebSocket服务统一导出
 *
 * @Author:    Claude Code Assistant
 * @Date:      2025-09-25
 * @Copyright  1024创新实验室
 */

// 导出业务WebSocket服务
export { policeWebSocketService, PoliceWebSocketService } from './police-websocket.service';
export { collaborationWebSocketService, CollaborationWebSocketService } from './collaboration-websocket.service';

// 导入服务实例
import { collaborationWebSocketService } from './collaboration-websocket.service';

// 导出相关类型定义
export type {
  PoliceReportCollaboration,
  PoliceEditLockInfo,
  PoliceFieldUpdate
} from './police-websocket.service';

export type {
  CollaborationUser,
  CollaborationEvent,
  CursorPosition
} from './collaboration-websocket.service';

// 导出WebSocket管理器
export {
  getWebSocketClient,
  initWebSocket,
  reconnectWebSocketAfterLogin,
  disconnectWebSocket,
  webSocketManager
} from '/@/utils/websocket-manager';

// 座位WebSocket服务（使用统一架构）
import { policeWebSocketService } from './police-websocket.service';

// 座位服务暂时使用警务服务的实现，后续可以独立实现
export const webSocketSeatService = policeWebSocketService;

/**
 * 初始化所有WebSocket业务服务
 */
export async function initializeAllWebSocketServices(): Promise<void> {
  try {
    // 确保全局WebSocket管理器已初始化
    await initWebSocket();
    console.log('🚀 全局WebSocket管理器初始化完成');

    // 初始化业务服务（延迟初始化，在需要时再初始化）
    console.log('📋 WebSocket业务服务准备就绪，将在需要时初始化');

    // 不在这里直接初始化所有服务，而是在各个页面/组件需要时按需初始化
    // 这样可以避免不必要的资源消耗和模块订阅

  } catch (error) {
    console.error('❌ WebSocket服务初始化失败:', error);
    throw error;
  }
}

/**
 * 按需初始化指定业务WebSocket服务
 */
export async function initializeWebSocketService(serviceType: 'police' | 'collaboration' | 'seat'): Promise<void> {
  switch (serviceType) {
    case 'police':
      await policeWebSocketService.initialize();
      break;
    case 'collaboration':
      await collaborationWebSocketService.initialize();
      break;
    case 'seat':
      await webSocketSeatService.initialize();
      break;
    default:
      throw new Error(`未知的WebSocket服务类型: ${serviceType}`);
  }
}

/**
 * 销毁所有WebSocket业务服务
 */
export function destroyAllWebSocketServices(): void {
  try {
    policeWebSocketService.destroy();
    collaborationWebSocketService.destroy();
    webSocketSeatService.destroy();

    console.log('🗑️ 所有WebSocket业务服务已销毁');
  } catch (error) {
    console.error('❌ 销毁WebSocket服务时出错:', error);
  }
}