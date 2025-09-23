<!--
  * 警情表单模板管理页面
  *
  * @Author:    Claude Code Assistant
  * @Date:      2025-09-22
  * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
-->
<template>
  <div class="form-template-list">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">🛠️ 警情表单配置</h1>
        <p class="page-subtitle">管理不同警情类型的录入表单模板，支持个性化字段配置</p>
      </div>
      <div class="header-right">
        <a-button type="primary" size="large" @click="showCreateModal" class="create-btn">
          <template #icon><PlusOutlined /></template>
          新建模板
        </a-button>
      </div>
    </div>

    <!-- 模板列表 -->
    <div class="template-grid">
      <div
        v-for="template in templateList"
        :key="template.id"
        class="template-card"
        :class="{ 'default-template': template.isDefault }"
      >
        <!-- 卡片头部 -->
        <div class="card-header">
          <div class="type-info">
            <span class="type-icon">{{ getTypeIcon(template.reportType) }}</span>
            <div class="type-text">
              <h3 class="type-name">{{ template.reportTypeName }}</h3>
              <p class="template-name">{{ template.templateName }}</p>
            </div>
          </div>
          <div class="card-actions">
            <a-dropdown>
              <a-button type="text" class="action-btn">
                <MoreOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="editTemplate(template)">
                    <EditOutlined />
                    编辑配置
                  </a-menu-item>
                  <a-menu-item @click="copyTemplate(template)">
                    <CopyOutlined />
                    复制模板
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item @click="deleteTemplate(template)" class="danger-item">
                    <DeleteOutlined />
                    删除模板
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
        </div>

        <!-- 卡片内容 -->
        <div class="card-content">
          <div class="template-stats">
            <div class="stat-item">
              <span class="stat-label">字段数量</span>
              <span class="stat-value">{{ template.fieldCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">创建时间</span>
              <span class="stat-value">{{ formatTime(template.createTime) }}</span>
            </div>
          </div>

          <div class="template-description" v-if="template.description">
            <p>{{ template.description }}</p>
          </div>

          <!-- 默认模板标识 -->
          <div v-if="template.isDefault" class="default-badge">
            <CheckCircleOutlined />
            默认模板
          </div>
        </div>

        <!-- 卡片底部 -->
        <div class="card-footer">
          <a-button type="primary" @click="editTemplate(template)" block>
            <SettingOutlined />
            配置字段
          </a-button>
        </div>
      </div>

      <!-- 空状态卡片 -->
      <div v-if="templateList.length === 0" class="empty-card">
        <div class="empty-content">
          <div class="empty-icon">📝</div>
          <h3 class="empty-title">暂无模板配置</h3>
          <p class="empty-desc">点击右上角"新建模板"创建第一个表单模板</p>
        </div>
      </div>
    </div>

    <!-- 创建模板弹窗 -->
    <a-modal
      v-model:open="createModalVisible"
      title="新建表单模板"
      width="600px"
      @ok="handleCreateTemplate"
      @cancel="cancelCreateModal"
    >
      <a-form
        ref="createFormRef"
        :model="createForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item
          label="警情类型"
          name="reportType"
          :rules="[{ required: true, message: '请选择警情类型' }]"
        >
          <a-select v-model:value="createForm.reportType" placeholder="选择警情类型">
            <a-select-option
              v-for="(type, key) in POLICE_REPORT_TYPE_ENUM"
              :key="key"
              :value="type.value"
            >
              {{ type.icon }} {{ type.desc }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          label="模板名称"
          name="templateName"
          :rules="[{ required: true, message: '请输入模板名称' }]"
        >
          <a-input v-model:value="createForm.templateName" placeholder="输入模板名称" />
        </a-form-item>

        <a-form-item label="模板描述" name="description">
          <a-textarea
            v-model:value="createForm.description"
            placeholder="描述模板的用途和特点"
            :rows="3"
            :maxlength="200"
            show-count
          />
        </a-form-item>

        <a-form-item label="设置选项" name="options">
          <a-checkbox v-model:checked="createForm.isDefault">设为默认模板</a-checkbox>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { message, Modal } from 'ant-design-vue';
  import {
    PlusOutlined,
    MoreOutlined,
    EditOutlined,
    CopyOutlined,
    DeleteOutlined,
    SettingOutlined,
    CheckCircleOutlined,
  } from '@ant-design/icons-vue';
  import { POLICE_REPORT_TYPE_ENUM } from '/@/constants/business/oa/police-report-const';
  import { policeFormTemplateApi, type PoliceFormTemplateVO, type PoliceFormTemplateAddForm } from '/@/api/business/oa/police-form-template-api';
  import { smartSentry } from '/@/lib/smart-sentry';
  import dayjs from 'dayjs';

  const router = useRouter();

  // 数据状态
  const templateList = ref<PoliceFormTemplateVO[]>([]);
  const loading = ref(false);

  // 创建模板相关
  const createModalVisible = ref(false);
  const createFormRef = ref();
  const createForm = reactive<PoliceFormTemplateAddForm>({
    reportType: undefined as any,
    templateName: '',
    description: '',
    isDefault: false,
  });

  // 页面初始化
  onMounted(() => {
    loadTemplateList();
  });

  // 加载模板列表
  async function loadTemplateList() {
    try {
      loading.value = true;
      const response = await policeFormTemplateApi.getTemplateList();
      if (response.data) {
        templateList.value = response.data;
      }
    } catch (error) {
      console.error('加载模板列表失败:', error);
      smartSentry.captureError(error);
      message.error('加载模板列表失败');
    } finally {
      loading.value = false;
    }
  }

  // 获取警情类型图标
  function getTypeIcon(reportType: number): string {
    const type = Object.values(POLICE_REPORT_TYPE_ENUM).find(t => t.value === reportType);
    return type ? type.icon : '❓';
  }

  // 格式化时间
  function formatTime(time: string): string {
    return dayjs(time).format('MM-DD HH:mm');
  }

  // 显示创建模板弹窗
  function showCreateModal() {
    createModalVisible.value = true;
    resetCreateForm();
  }

  // 重置创建表单
  function resetCreateForm() {
    Object.assign(createForm, {
      reportType: undefined,
      templateName: '',
      description: '',
      isDefault: false,
    });
    createFormRef.value?.resetFields();
  }

  // 取消创建
  function cancelCreateModal() {
    createModalVisible.value = false;
    resetCreateForm();
  }

  // 创建模板
  async function handleCreateTemplate() {
    try {
      await createFormRef.value.validateFields();

      const response = await policeFormTemplateApi.createTemplate(createForm);
      if (response.code === 0 || response.code === 1) {
        message.success('模板创建成功');
        createModalVisible.value = false;
        resetCreateForm();
        loadTemplateList();
      } else {
        message.error(response.msg || '创建失败');
      }
    } catch (error) {
      console.error('创建模板失败:', error);
      smartSentry.captureError(error);
    }
  }

  // 编辑模板
  function editTemplate(template: PoliceFormTemplateVO) {
    router.push({
      path: '/oa/police/form-template-config',
      query: { templateId: template.id }
    });
  }

  // 复制模板
  async function copyTemplate(template: PoliceFormTemplateVO) {
    const newTemplate: PoliceFormTemplateAddForm = {
      reportType: template.reportType,
      templateName: `${template.templateName}_副本`,
      description: template.description,
      isDefault: false,
    };

    try {
      const response = await policeFormTemplateApi.createTemplate(newTemplate);
      if (response.code === 0 || response.code === 1) {
        message.success('模板复制成功');
        loadTemplateList();
      } else {
        message.error(response.msg || '复制失败');
      }
    } catch (error) {
      console.error('复制模板失败:', error);
      smartSentry.captureError(error);
      message.error('复制模板失败');
    }
  }

  // 删除模板
  function deleteTemplate(template: PoliceFormTemplateVO) {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除模板"${template.templateName}"吗？此操作不可撤销。`,
      okText: '确定删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const response = await policeFormTemplateApi.deleteTemplate(template.id);
          if (response.code === 0 || response.code === 1) {
            message.success('模板删除成功');
            loadTemplateList();
          } else {
            message.error(response.msg || '删除失败');
          }
        } catch (error) {
          console.error('删除模板失败:', error);
          smartSentry.captureError(error);
          message.error('删除模板失败');
        }
      },
    });
  }
</script>

<style scoped lang="scss">
.form-template-list {
  padding: 24px;
  min-height: calc(100vh - 120px);
  background: #f5f5f5;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 32px;
  padding: 0 8px;
}

.header-left {
  .page-title {
    font-size: 28px;
    font-weight: 700;
    color: #1f2937;
    margin: 0 0 8px 0;
  }

  .page-subtitle {
    font-size: 16px;
    color: #6b7280;
    margin: 0;
  }
}

.create-btn {
  height: 44px;
  padding: 0 24px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
}

/* 模板网格 */
.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 24px;
}

/* 模板卡片 */
.template-card {
  background: white;
  border-radius: 12px;
  border: 2px solid #e5e7eb;
  transition: all 0.3s ease;
  overflow: hidden;

  &:hover {
    border-color: #3b82f6;
    box-shadow: 0 8px 25px rgba(59, 130, 246, 0.15);
    transform: translateY(-2px);
  }

  &.default-template {
    border-color: #10b981;
    background: linear-gradient(135deg, #ecfdf5 0%, #ffffff 100%);

    .card-header {
      background: linear-gradient(135deg, #10b981 0%, #059669 100%);
      color: white;

      .type-name {
        color: white;
      }

      .template-name {
        color: #d1fae5;
      }
    }
  }
}

.card-header {
  padding: 20px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;

  .type-info {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    flex: 1;

    .type-icon {
      font-size: 32px;
      line-height: 1;
    }

    .type-text {
      .type-name {
        font-size: 18px;
        font-weight: 700;
        color: white;
        margin: 0 0 4px 0;
      }

      .template-name {
        font-size: 14px;
        color: #dbeafe;
        margin: 0;
      }
    }
  }

  .card-actions {
    .action-btn {
      color: white;
      border: none;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 6px;

      &:hover {
        background: rgba(255, 255, 255, 0.2);
        color: white;
      }
    }
  }
}

.card-content {
  padding: 20px;
  position: relative;

  .template-stats {
    display: flex;
    gap: 32px;
    margin-bottom: 16px;

    .stat-item {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .stat-label {
        font-size: 12px;
        color: #6b7280;
        font-weight: 500;
      }

      .stat-value {
        font-size: 16px;
        font-weight: 600;
        color: #1f2937;
      }
    }
  }

  .template-description {
    margin-bottom: 16px;

    p {
      font-size: 14px;
      color: #6b7280;
      line-height: 1.5;
      margin: 0;
    }
  }

  .default-badge {
    position: absolute;
    top: 20px;
    right: 20px;
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px 8px;
    background: #d1fae5;
    color: #065f46;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
  }
}

.card-footer {
  padding: 16px 20px 20px;
  border-top: 1px solid #f3f4f6;

  .ant-btn {
    height: 40px;
    font-weight: 600;
    border-radius: 8px;
  }
}

/* 空状态卡片 */
.empty-card {
  grid-column: 1 / -1;
  background: white;
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  padding: 60px 40px;
  text-align: center;

  .empty-content {
    .empty-icon {
      font-size: 64px;
      margin-bottom: 16px;
    }

    .empty-title {
      font-size: 20px;
      font-weight: 600;
      color: #374151;
      margin: 0 0 8px 0;
    }

    .empty-desc {
      font-size: 16px;
      color: #6b7280;
      margin: 0;
    }
  }
}

/* 下拉菜单危险项 */
:deep(.danger-item) {
  color: #dc2626 !important;

  &:hover {
    background-color: #fee2e2 !important;
  }
}
</style>