<!--
  统一消息客户端演示组件
  展示WebSocket和RocketMQ双传输模式的使用方式
-->
<template>
  <div class="message-client-demo">
    <a-card title="统一消息客户端演示" size="small">
      <!-- 连接状态 -->
      <div class="connection-status mb-4">
        <a-space>
          <a-badge :status="getConnectionBadgeStatus()" :text="getConnectionStatusText()" />
          <a-tag :color="getTransportColor()">{{ connectionStats.currentTransport || 'Unknown' }}</a-tag>
          <a-button
            v-if="connectionStats.transportType === 'hybrid'"
            size="small"
            @click="switchTransport"
            :loading="switching"
          >
            切换传输方式
          </a-button>
        </a-space>
      </div>

      <!-- 连接统计 -->
      <div class="connection-stats mb-4">
        <a-descriptions size="small" :column="4">
          <a-descriptions-item label="传输模式">{{ connectionStats.transportType }}</a-descriptions-item>
          <a-descriptions-item label="当前传输">{{ connectionStats.currentTransport }}</a-descriptions-item>
          <a-descriptions-item label="连接状态">{{ connectionStats.state }}</a-descriptions-item>
          <a-descriptions-item label="已初始化">{{ connectionStats.initialized ? '是' : '否' }}</a-descriptions-item>
        </a-descriptions>
      </div>

      <!-- 消息发送测试 -->
      <div class="message-send mb-4">
        <a-space direction="vertical" style="width: 100%">
          <a-input v-model:value="testMessage" placeholder="输入测试消息内容" />
          <a-space>
            <a-select v-model:value="targetType" style="width: 120px">
              <a-select-option value="broadcast">广播</a-select-option>
              <a-select-option value="room">房间</a-select-option>
              <a-select-option value="user">用户</a-select-option>
            </a-select>
            <a-input
              v-if="targetType !== 'broadcast'"
              v-model:value="targetValue"
              :placeholder="targetType === 'room' ? '房间名称' : '用户ID'"
              style="width: 120px"
            />
            <a-button type="primary" @click="sendTestMessage" :loading="sending">发送消息</a-button>
          </a-space>
        </a-space>
      </div>

      <!-- 消息日志 -->
      <div class="message-logs">
        <div class="logs-header">
          <span>消息日志</span>
          <a-button size="small" @click="clearLogs">清空</a-button>
        </div>
        <div class="logs-content">
          <div
            v-for="(log, index) in messageLogs"
            :key="index"
            :class="['log-item', `log-${log.type}`]"
          >
            <span class="log-time">{{ formatTime(log.timestamp) }}</span>
            <span class="log-type">{{ log.type.toUpperCase() }}</span>
            <span class="log-content">{{ log.content }}</span>
          </div>
          <div v-if="messageLogs.length === 0" class="empty-logs">
            暂无消息日志
          </div>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue';
import { useMessageClient } from '/@/services/message-client-manager';
import { WebSocketMessage } from '/@/types/websocket';

// 使用统一消息客户端
const {
  isReady,
  sendMessage,
  onModuleMessage,
  offModuleMessage,
  getConnectionState,
  getConnectionStats,
  switchTransport
} = useMessageClient();

// 响应式数据
const testMessage = ref('Hello, this is a test message!');
const targetType = ref<'broadcast' | 'room' | 'user'>('broadcast');
const targetValue = ref('');
const sending = ref(false);
const switching = ref(false);
const connectionStats = reactive<any>({});
const messageLogs = ref<Array<{
  type: 'send' | 'receive' | 'event';
  content: string;
  timestamp: number;
}>>([]);

// 更新连接统计
const updateConnectionStats = () => {
  Object.assign(connectionStats, getConnectionStats());
};

// 获取连接状态徽章
const getConnectionBadgeStatus = () => {
  if (!connectionStats.isConnected) return 'error';
  return 'success';
};

// 获取连接状态文本
const getConnectionStatusText = () => {
  return connectionStats.isConnected ? '已连接' : '未连接';
};

// 获取传输方式颜色
const getTransportColor = () => {
  switch (connectionStats.currentTransport) {
    case 'websocket': return 'blue';
    case 'rocketmq': return 'green';
    default: return 'default';
  }
};

// 发送测试消息
const sendTestMessage = async () => {
  if (!testMessage.value.trim()) return;

  sending.value = true;
  try {
    const message: WebSocketMessage = {
      messageId: `test-${Date.now()}`,
      type: 'TEST_MESSAGE',
      module: 'demo',
      content: testMessage.value,
      data: { test: true },
      timestamp: new Date().toISOString()
    };

    // 根据目标类型设置消息
    if (targetType.value === 'room' && targetValue.value) {
      message.room = targetValue.value;
    } else if (targetType.value === 'user' && targetValue.value) {
      message.toUserId = parseInt(targetValue.value);
    }

    const success = await sendMessage(message);

    addLog('send', `发送${targetType.value}消息: ${success ? '成功' : '失败'} - ${testMessage.value}`);

    if (success) {
      testMessage.value = '';
    }
  } catch (error) {
    addLog('send', `发送消息失败: ${error}`);
  } finally {
    sending.value = false;
  }
};

// 切换传输方式
const handleSwitchTransport = async () => {
  switching.value = true;
  try {
    await switchTransport();
    updateConnectionStats();
    addLog('event', `传输方式已切换到: ${connectionStats.currentTransport}`);
  } catch (error) {
    addLog('event', `传输方式切换失败: ${error}`);
  } finally {
    switching.value = false;
  }
};

// 添加日志
const addLog = (type: 'send' | 'receive' | 'event', content: string) => {
  messageLogs.value.unshift({
    type,
    content,
    timestamp: Date.now()
  });

  // 保持最多100条日志
  if (messageLogs.value.length > 100) {
    messageLogs.value = messageLogs.value.slice(0, 100);
  }
};

// 清空日志
const clearLogs = () => {
  messageLogs.value = [];
};

// 格式化时间
const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleTimeString();
};

// 消息处理器
const handleDemoMessage = (message: WebSocketMessage) => {
  addLog('receive', `收到消息: ${message.content || JSON.stringify(message.data)}`);
};

// 组件挂载
onMounted(() => {
  // 更新连接统计
  updateConnectionStats();

  // 定时更新连接状态
  const updateTimer = setInterval(updateConnectionStats, 2000);

  // 订阅演示消息
  onModuleMessage('demo', 'TEST_MESSAGE', handleDemoMessage);
  onModuleMessage('system', 'CONNECTED', () => addLog('event', '系统连接已建立'));
  onModuleMessage('system', 'DISCONNECTED', () => addLog('event', '系统连接已断开'));

  // 清理定时器
  onUnmounted(() => {
    clearInterval(updateTimer);
    offModuleMessage('demo', 'TEST_MESSAGE', handleDemoMessage);
  });
});
</script>

<style scoped>
.message-client-demo {
  padding: 16px;
}

.mb-4 {
  margin-bottom: 16px;
}

.message-logs {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  height: 300px;
  display: flex;
  flex-direction: column;
}

.logs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid #d9d9d9;
  background-color: #fafafa;
  font-weight: 500;
}

.logs-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.log-item {
  display: flex;
  align-items: center;
  padding: 4px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 12px;
}

.log-item:last-child {
  border-bottom: none;
}

.log-time {
  color: #999;
  margin-right: 8px;
  min-width: 70px;
}

.log-type {
  font-weight: 500;
  margin-right: 8px;
  min-width: 60px;
}

.log-send .log-type {
  color: #1890ff;
}

.log-receive .log-type {
  color: #52c41a;
}

.log-event .log-type {
  color: #faad14;
}

.log-content {
  flex: 1;
  word-break: break-all;
}

.empty-logs {
  text-align: center;
  color: #999;
  padding: 40px 0;
}
</style>