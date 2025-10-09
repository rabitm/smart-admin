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

        <!-- IM群聊Tab -->
        <a-tab-pane key="im-chat" tab="群组沟通">
          <template #tab>
            <message-outlined style="margin-right: 4px" />
            群组沟通
          </template>
          <div class="im-chat-container">
            <EmbeddedImChat v-if="detailData.reportId" :report-id="Number(detailData.reportId)" />
          </div>
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
  import { MessageOutlined } from '@ant-design/icons-vue';
  import { SmartLoading } from '/@/components/framework/smart-loading';
  import { policeReportApi } from '/@/api/business/oa/police-report-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import PoliceReportOperate from './components/police-report-operate-modal.vue';
  import PoliceOperationTimeline from './components/police-operation-timeline.vue';
  import CompactOperationHistory from './components/compact-operation-history.vue';
  import EmbeddedImChat from '/@/components/business/im/embedded-im-chat.vue';

  const route = useRoute();
  const router = useRouter();

  const loading = ref(false);
  const detailData = ref({});
  const activeTab = ref('basic');
  const operateRef = ref();
  const useCompactTimeline = ref(true); // 默认使用紧凑模式

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
    } catch (e) {
      smartSentry.captureError(e);
    } finally {
      loading.value = false;
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

.im-chat-container {
  height: 600px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}
</style>