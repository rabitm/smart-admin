<!--
  * 警情录入详情页面
  *
  * @Author:    Claude Code Assistant
  * @Date:      2025-09-18
  * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
-->
<template>
  <a-card title="警情详情" class="smart-card-container">
    <template #extra>
      <a-button @click="goBack">
        <template #icon>
          <ArrowLeftOutlined />
        </template>
        返回
      </a-button>
    </template>

    <div v-if="loading" class="loading-container">
      <a-spin size="large" />
    </div>

    <div v-else>
      <!-- Tab页面 -->
      <a-tabs v-model:activeKey="activeTab" type="card">
        <a-tab-pane key="basic" tab="基本信息">
          <a-descriptions :column="2" bordered>
        <a-descriptions-item label="警情编号">
          <a-tag color="blue">{{ detailData.reportNumber }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="警情类型">
          {{ $smartEnumPlugin.getDescByValue('POLICE_REPORT_TYPE_ENUM', detailData.reportType) }}
        </a-descriptions-item>
        <a-descriptions-item label="警情等级">
          <a-tag :color="getLevelColor(detailData.reportLevel)">
            {{ $smartEnumPlugin.getDescByValue('POLICE_REPORT_LEVEL_ENUM', detailData.reportLevel) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="处理状态">
          <a-tag :color="getStatusColor(detailData.status)">
            {{ $smartEnumPlugin.getDescByValue('POLICE_REPORT_STATUS_ENUM', detailData.status) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="报警人姓名">{{ detailData.reporterName }}</a-descriptions-item>
        <a-descriptions-item label="报警人电话">{{ detailData.reporterPhone }}</a-descriptions-item>
        <a-descriptions-item label="报警人身份证" :span="2">{{ detailData.reporterIdCard || '未提供' }}</a-descriptions-item>
        <a-descriptions-item label="报警时间" :span="2">{{ detailData.reportTime }}</a-descriptions-item>
        <a-descriptions-item label="事发地点" :span="2">{{ detailData.incidentLocation }}</a-descriptions-item>
        <a-descriptions-item label="警情描述" :span="2">
          <div class="description-content">{{ detailData.description }}</div>
        </a-descriptions-item>
        <a-descriptions-item label="处理人员" v-if="detailData.handlerName">{{ detailData.handlerName }}</a-descriptions-item>
        <a-descriptions-item label="处理完成时间" v-if="detailData.handleTime">{{ detailData.handleTime }}</a-descriptions-item>
        <a-descriptions-item label="处理结果" :span="2" v-if="detailData.handleResult">
          <div class="description-content">{{ detailData.handleResult }}</div>
        </a-descriptions-item>
        <a-descriptions-item label="附件信息" :span="2" v-if="detailData.attachments">{{ detailData.attachments }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2" v-if="detailData.remark">
          <div class="description-content">{{ detailData.remark }}</div>
        </a-descriptions-item>
        <a-descriptions-item label="创建人">{{ detailData.createUserName }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ detailData.createTime }}</a-descriptions-item>
            <a-descriptions-item label="更新时间" :span="2">{{ detailData.updateTime }}</a-descriptions-item>
          </a-descriptions>

          <!-- 操作按钮区域 -->
          <div class="operation-buttons" v-if="detailData.reportId">
            <a-space>
              <a-button @click="handleEdit" v-privilege="'oa:police:update'" type="primary">
                <template #icon>
                  <EditOutlined />
                </template>
                编辑警情
              </a-button>
              <a-button @click="handleDelete" v-privilege="'oa:police:delete'" danger>
                <template #icon>
                  <DeleteOutlined />
                </template>
                删除警情
              </a-button>
            </a-space>
          </div>
        </a-tab-pane>

        <!-- 操作时间轴Tab -->
        <a-tab-pane key="timeline" tab="操作历史">
          <template #tab>
            操作历史
            <a-tooltip title="切换显示模式">
              <a-switch
                v-model:checked="useCompactTimeline"
                size="small"
                style="margin-left: 8px;"
                checked-children="紧凑"
                un-checked-children="详细"
              />
            </a-tooltip>
          </template>

          <!-- 紧凑型操作历史 -->
          <CompactOperationHistory
            v-if="useCompactTimeline && detailData.reportId"
            :reportId="detailData.reportId"
            :reportNumber="detailData.reportNumber"
          />

          <!-- 原始操作时间轴 -->
          <PoliceOperationTimeline
            v-else-if="detailData.reportId"
            :reportId="detailData.reportId"
            :reportNumber="detailData.reportNumber"
          />
        </a-tab-pane>

        <!-- 即时聊天Tab -->
        <a-tab-pane key="chat" tab="即时聊天">
          <ChatPanel
            v-if="detailData.reportId"
            :report-id="Number(detailData.reportId)"
            :group-id="detailData.imGroupId"
            :group-name="`警情-${detailData.reportNumber || '未命名'}`"
          />
          <a-empty v-else description="加载中..." />
        </a-tab-pane>
      </a-tabs>
    </div>

    <!-- 编辑弹窗 -->
    <PoliceReportOperate ref="operateRef" @refresh="getDetail" />
  </a-card>
</template>

<script setup lang="ts">
  import { ref, onMounted } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { message, Modal } from 'ant-design-vue';
  import { SmartLoading } from '/@/components/framework/smart-loading';
  import { policeReportApi } from '/@/api/business/oa/police-report-api';
  import { imBusinessApi } from '/@/api/business/oa/im-business-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import PoliceReportOperate from './components/police-report-operate-modal.vue';
  import PoliceOperationTimeline from './components/police-operation-timeline.vue';
  import CompactOperationHistory from './components/compact-operation-history.vue';
  import ChatPanel from './components/ChatPanel.vue';

  const route = useRoute();
  const router = useRouter();

  const loading = ref(false);
  const detailData = ref({});
  const activeTab = ref('basic');
  const operateRef = ref();
  const useCompactTimeline = ref(true); // 默认使用紧凑模式
  const imGroupId = ref<string | undefined>(undefined);

  const reportId = route.query.reportId;

  async function getDetail() {
    if (!reportId) {
      message.error('缺少警情ID参数');
      return;
    }

    try {
      loading.value = true;
      let responseModel = await policeReportApi.getDetail(reportId);
      detailData.value = responseModel.data;

      // 🆕 加载或创建 IM 群组
      await ensureIMGroupExists(Number(reportId));
    } catch (e) {
      smartSentry.captureError(e);
    } finally {
      loading.value = false;
    }
  }

  /**
   * 确保 IM 群组存在
   *
   * 策略: 直接调用创建接口，后端会自动检测并修复同步问题
   */
  async function ensureIMGroupExists(reportId: number) {
    try {
      console.log('📡 [警情详情-IM群组] 开始创建/获取群组, reportId:', reportId);

      // 直接调用创建接口 - 后端会自动处理以下情况：
      // 1. 群组不存在 → 创建新群组
      // 2. 群组存在且验证通过 → 返回现有 groupId
      // 3. 数据库有映射但服务器无群组 → 自动删除旧映射并重新创建
      await createNewIMGroup(reportId);

    } catch (error) {
      console.error('❌ [警情详情-IM群组] 创建/获取群组失败:', error);
      // 不显示错误提示，让用户可以继续查看其他内容
    }
  }

  /**
   * 创建新的 IM 群组
   */
  async function createNewIMGroup(reportId: number) {
    try {
      console.log('🆕 [警情详情-IM群组] 开始创建新群组, reportId:', reportId);

      const groupIdResponse = await imBusinessApi.createGroupForReport(reportId);
      const groupId = groupIdResponse.data; // 后端直接返回 String (groupId)

      if (groupId) {
        imGroupId.value = groupId;
        detailData.value.imGroupId = groupId; // 同步到 detailData
        console.log('✅ [警情详情-IM群组] 群组创建成功, groupId:', imGroupId.value);
      } else {
        console.error('❌ [警情详情-IM群组] 群组创建返回数据异常:', groupId);
        throw new Error('群组创建返回数据异常');
      }
    } catch (error) {
      console.error('❌ [警情详情-IM群组] 创建群组失败:', error);
      throw error;
    }
  }

  function goBack() {
    router.go(-1);
  }

  function handleEdit() {
    operateRef.value.showModal(reportId);
  }

  function handleDelete() {
    Modal.confirm({
      title: '确定要删除这条警情记录吗？',
      content: '删除后，该警情信息将不可恢复',
      okText: '删除',
      okType: 'danger',
      onOk() {
        deletePoliceReport();
      },
      cancelText: '取消',
      onCancel() {},
    });
  }

  async function deletePoliceReport() {
    try {
      SmartLoading.show();
      await policeReportApi.deletePoliceReport(reportId);
      message.success('删除成功');
      router.push('/oa/police/report-list');
    } catch (e) {
      smartSentry.captureError(e);
    } finally {
      SmartLoading.hide();
    }
  }

  function getStatusColor(status) {
    const colorMap = {
      1: 'orange',    // 待处理
      2: 'blue',      // 处理中
      3: 'green',     // 已完成
      4: 'default',   // 已关闭
    };
    return colorMap[status] || 'default';
  }

  function getLevelColor(level) {
    const colorMap = {
      1: 'red',       // 紧急
      2: 'orange',    // 高
      3: 'blue',      // 中
      4: 'default',   // 低
    };
    return colorMap[level] || 'default';
  }

  onMounted(() => {
    getDetail();
  });
</script>

<style scoped>
.smart-card-container {
  margin: 20px;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.description-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}

.operation-buttons {
  margin-top: 30px;
  text-align: center;
  padding: 20px 0;
  border-top: 1px solid #f0f0f0;
}
</style>