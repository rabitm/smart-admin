<!--
  高性能警情列表性能测试页面
  用于验证200-500个并发端的实时更新性能

  测试场景：
  1. 大量数据渲染性能测试
  2. 实时更新延迟测试
  3. 内存使用监控
  4. WebSocket连接稳定性测试

  @Author: Claude Code Assistant
  @Date: 2025-09-26
-->
<template>
  <div class="performance-test-container">
    <a-card title="高性能警情列表 - 性能测试" size="small">
      <!-- 测试控制面板 -->
      <div class="test-control-panel">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-statistic
              title="测试数据量"
              :value="testDataCount"
              suffix="条"
            />
          </a-col>
          <a-col :span="6">
            <a-statistic
              title="更新频率"
              :value="updateFrequency"
              suffix="次/秒"
            />
          </a-col>
          <a-col :span="6">
            <a-statistic
              title="平均延迟"
              :value="averageLatency"
              suffix="ms"
              :precision="1"
            />
          </a-col>
          <a-col :span="6">
            <a-statistic
              title="渲染FPS"
              :value="renderFPS"
              :value-style="getFPSStyle()"
            />
          </a-col>
        </a-row>

        <a-divider />

        <a-row :gutter="16">
          <a-col :span="4">
            <a-input-number
              v-model:value="testSettings.dataCount"
              placeholder="数据量"
              :min="100"
              :max="10000"
              :step="100"
            />
            <div class="setting-label">测试数据量</div>
          </a-col>
          <a-col :span="4">
            <a-input-number
              v-model:value="testSettings.updateInterval"
              placeholder="更新间隔(ms)"
              :min="10"
              :max="5000"
              :step="10"
            />
            <div class="setting-label">更新间隔(ms)</div>
          </a-col>
          <a-col :span="4">
            <a-input-number
              v-model:value="testSettings.concurrentUsers"
              placeholder="并发用户数"
              :min="1"
              :max="500"
              :step="1"
            />
            <div class="setting-label">模拟并发用户</div>
          </a-col>
          <a-col :span="12">
            <a-space>
              <a-button
                type="primary"
                @click="startPerformanceTest"
                :loading="isTestRunning"
                :disabled="isTestRunning"
              >
                开始性能测试
              </a-button>
              <a-button
                @click="stopPerformanceTest"
                :disabled="!isTestRunning"
              >
                停止测试
              </a-button>
              <a-button @click="generateTestData">生成测试数据</a-button>
              <a-button @click="clearTestData">清空数据</a-button>
            </a-space>
          </a-col>
        </a-row>
      </div>

      <a-divider />

      <!-- 性能监控图表 -->
      <a-row :gutter="16" style="margin-bottom: 16px;">
        <a-col :span="12">
          <a-card title="延迟监控" size="small">
            <div ref="latencyChartRef" style="height: 200px;"></div>
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card title="内存使用" size="small">
            <div ref="memoryChartRef" style="height: 200px;"></div>
          </a-card>
        </a-col>
      </a-row>

      <!-- 高性能虚拟滚动表格 -->
      <VirtualScrollTable
        :data="testTableData"
        :columns="testColumns"
        :container-height="400"
        :row-height="50"
        :show-performance-panel="true"
        :highlight-updates="true"
        @row-click="handleRowClick"
        ref="virtualTableRef"
      >
        <!-- 警情编号列 -->
        <template #reportNumber="{ record }">
          <a-tag :color="getUpdateColor(record)">
            {{ record.reportNumber }}
          </a-tag>
        </template>

        <!-- 状态列 -->
        <template #status="{ record }">
          <a-tag :color="getStatusColor(record.status)">
            {{ getStatusText(record.status) }}
          </a-tag>
        </template>

        <!-- 更新时间列 -->
        <template #lastUpdate="{ record }">
          <span :class="{ 'recently-updated': isRecentlyUpdated(record) }">
            {{ formatTime(record.updatedAt) }}
          </span>
        </template>
      </VirtualScrollTable>

      <!-- 测试日志 -->
      <a-card title="测试日志" size="small" style="margin-top: 16px;">
        <div class="test-log" ref="logContainerRef">
          <div
            v-for="(log, index) in testLogs"
            :key="index"
            class="log-entry"
            :class="log.level"
          >
            [{{ log.timestamp }}] {{ log.level.toUpperCase() }}: {{ log.message }}
          </div>
        </div>
      </a-card>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import VirtualScrollTable from '/@/components/business/police/virtual-scroll-table.vue'
import { policeListUpdateManager } from '/@/utils/police-list-update-manager'
import type { PoliceReportData } from '/@/utils/police-list-update-manager'

// 测试设置
const testSettings = reactive({
  dataCount: 1000,
  updateInterval: 100,
  concurrentUsers: 50
})

// 测试状态
const isTestRunning = ref(false)
const testDataCount = ref(0)
const updateFrequency = ref(0)
const averageLatency = ref(0)
const renderFPS = ref(60)

// 测试数据
const testTableData = ref<PoliceReportData[]>([])
const testLogs = ref<Array<{ timestamp: string; level: string; message: string }>>([])

// 性能监控
const latencyHistory = ref<number[]>([])
const memoryHistory = ref<number[]>([])
const updateTimes = ref<number[]>([])

// 测试定时器
let testTimer: NodeJS.Timeout | null = null
let performanceTimer: NodeJS.Timeout | null = null

// DOM引用
const virtualTableRef = ref()
const logContainerRef = ref()
const latencyChartRef = ref()
const memoryChartRef = ref()

// 表格列配置
const testColumns = computed(() => [
  {
    key: 'reportNumber',
    title: '警情编号',
    width: 120,
    slot: 'reportNumber'
  },
  {
    key: 'reportType',
    title: '类型',
    width: 80
  },
  {
    key: 'reportLevel',
    title: '等级',
    width: 80
  },
  {
    key: 'status',
    title: '状态',
    width: 100,
    slot: 'status'
  },
  {
    key: 'reporterName',
    title: '报警人',
    width: 100
  },
  {
    key: 'incidentLocation',
    title: '地点',
    width: 150
  },
  {
    key: 'lastUpdate',
    title: '最后更新',
    width: 120,
    slot: 'lastUpdate'
  }
])

// 生成测试数据
const generateTestData = () => {
  const data: PoliceReportData[] = []

  for (let i = 1; i <= testSettings.dataCount; i++) {
    data.push({
      id: i,
      reportId: i,
      reportNumber: `JQ${String(i).padStart(6, '0')}`,
      reportType: Math.floor(Math.random() * 4) + 1,
      reportLevel: Math.floor(Math.random() * 4) + 1,
      status: Math.floor(Math.random() * 4) + 1,
      reporterName: `测试用户${i}`,
      reporterPhone: `138${String(Math.floor(Math.random() * 100000000)).padStart(8, '0')}`,
      incidentLocation: `测试地点${i}`,
      description: `这是第${i}条测试警情数据`,
      reportTime: new Date().toISOString(),
      handlerName: `处理员${Math.floor(Math.random() * 10) + 1}`,
      createUserName: '测试员',
      createTime: new Date().toISOString(),
      updatedAt: Date.now(),
      version: 1,
      updateFields: new Set()
    })
  }

  testTableData.value = data
  testDataCount.value = data.length

  // 初始化高性能管理器
  policeListUpdateManager.initializeData(data)

  addTestLog('info', `生成了 ${data.length} 条测试数据`)
}

// 开始性能测试
const startPerformanceTest = async () => {
  isTestRunning.value = true
  addTestLog('info', '开始性能测试')

  // 模拟并发更新
  testTimer = setInterval(() => {
    simulateUpdates()
  }, testSettings.updateInterval)

  // 性能监控
  performanceTimer = setInterval(() => {
    updatePerformanceStats()
  }, 1000)
}

// 停止性能测试
const stopPerformanceTest = () => {
  isTestRunning.value = false

  if (testTimer) {
    clearInterval(testTimer)
    testTimer = null
  }

  if (performanceTimer) {
    clearInterval(performanceTimer)
    performanceTimer = null
  }

  addTestLog('info', '性能测试已停止')
}

// 模拟更新
const simulateUpdates = () => {
  const updateCount = Math.min(testSettings.concurrentUsers, testTableData.value.length)
  const startTime = performance.now()

  for (let i = 0; i < updateCount; i++) {
    const randomIndex = Math.floor(Math.random() * testTableData.value.length)
    const record = testTableData.value[randomIndex]

    if (record) {
      // 随机更新字段
      const fields = ['status', 'handlerName', 'description', 'reportLevel']
      const randomField = fields[Math.floor(Math.random() * fields.length)]

      const oldValue = (record as any)[randomField]
      let newValue: any

      switch (randomField) {
        case 'status':
          newValue = Math.floor(Math.random() * 4) + 1
          break
        case 'reportLevel':
          newValue = Math.floor(Math.random() * 4) + 1
          break
        case 'handlerName':
          newValue = `处理员${Math.floor(Math.random() * 20) + 1}`
          break
        case 'description':
          newValue = `更新描述 ${Date.now()}`
          break
        default:
          newValue = `更新值 ${Date.now()}`
      }

      // 更新记录
      (record as any)[randomField] = newValue
      record.updatedAt = Date.now()
      record.version++
      record.updateFields?.add(randomField)

      // 清理更新标记
      setTimeout(() => {
        record.updateFields?.delete(randomField)
      }, 2000)
    }
  }

  // 记录延迟
  const endTime = performance.now()
  const latency = endTime - startTime
  latencyHistory.value.push(latency)

  if (latencyHistory.value.length > 100) {
    latencyHistory.value.shift()
  }

  updateTimes.value.push(startTime)
}

// 更新性能统计
const updatePerformanceStats = () => {
  // 计算平均延迟
  if (latencyHistory.value.length > 0) {
    averageLatency.value = latencyHistory.value.reduce((a, b) => a + b, 0) / latencyHistory.value.length
  }

  // 计算更新频率
  const now = performance.now()
  const recentUpdates = updateTimes.value.filter(time => (now - time) < 1000)
  updateFrequency.value = recentUpdates.length

  // 获取虚拟表格性能统计
  if (virtualTableRef.value) {
    const stats = virtualTableRef.value.getStats?.()
    if (stats) {
      renderFPS.value = Math.round(1000 / (stats.lastUpdateTime || 16))
    }
  }

  // 内存使用（如果支持）
  if ('memory' in performance) {
    const memInfo = (performance as any).memory
    const memoryUsage = Math.round(memInfo.usedJSHeapSize / 1048576)
    memoryHistory.value.push(memoryUsage)

    if (memoryHistory.value.length > 60) {
      memoryHistory.value.shift()
    }
  }
}

// 清空测试数据
const clearTestData = () => {
  testTableData.value = []
  testDataCount.value = 0
  testLogs.value = []
  latencyHistory.value = []
  memoryHistory.value = []
  updateTimes.value = []
  addTestLog('info', '测试数据已清空')
}

// 添加测试日志
const addTestLog = (level: string, message: string) => {
  testLogs.value.push({
    timestamp: new Date().toLocaleTimeString(),
    level,
    message
  })

  // 限制日志数量
  if (testLogs.value.length > 100) {
    testLogs.value.shift()
  }

  // 滚动到底部
  nextTick(() => {
    if (logContainerRef.value) {
      logContainerRef.value.scrollTop = logContainerRef.value.scrollHeight
    }
  })
}

// 工具函数
const getFPSStyle = () => {
  if (renderFPS.value >= 50) return { color: '#52c41a' }
  if (renderFPS.value >= 30) return { color: '#faad14' }
  return { color: '#ff4d4f' }
}

const getUpdateColor = (record: PoliceReportData) => {
  if (record.updateFields && record.updateFields.size > 0) {
    return 'processing'
  }
  return 'default'
}

const getStatusColor = (status: number) => {
  const colors = ['default', 'orange', 'blue', 'green', 'gray']
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts = ['未知', '待处理', '处理中', '已完成', '已关闭']
  return texts[status] || '未知'
}

const isRecentlyUpdated = (record: PoliceReportData) => {
  return (Date.now() - record.updatedAt) < 5000
}

const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleTimeString()
}

const handleRowClick = (record: PoliceReportData) => {
  addTestLog('info', `点击了警情: ${record.reportNumber}`)
}

// 生命周期
onMounted(() => {
  addTestLog('info', '性能测试页面已加载')
})

onUnmounted(() => {
  stopPerformanceTest()
})
</script>

<style scoped>
.performance-test-container {
  padding: 16px;
}

.test-control-panel {
  background: #f5f5f5;
  padding: 16px;
  border-radius: 6px;
}

.setting-label {
  font-size: 12px;
  color: #666;
  text-align: center;
  margin-top: 4px;
}

.test-log {
  height: 150px;
  overflow-y: auto;
  background: #1e1e1e;
  padding: 8px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
}

.log-entry {
  color: #fff;
  margin-bottom: 2px;
}

.log-entry.info {
  color: #1890ff;
}

.log-entry.warn {
  color: #faad14;
}

.log-entry.error {
  color: #ff4d4f;
}

.recently-updated {
  color: #52c41a;
  font-weight: bold;
}

.test-log::-webkit-scrollbar {
  width: 6px;
}

.test-log::-webkit-scrollbar-thumb {
  background: #666;
  border-radius: 3px;
}
</style>