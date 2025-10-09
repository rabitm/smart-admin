<template>
  <div class="embedded-im-chat">
    <!-- 加载状态 -->
    <div v-if="isLoadingGroup" class="loading-wrapper">
      <a-spin size="large" />
      <div class="loading-text">正在加载群聊...</div>
    </div>

    <!-- 群组未创建 -->
    <div v-else-if="!groupID && reportId" class="empty-wrapper">
      <a-empty description="该警情尚未创建IM群组">
        <template #image>
          <team-outlined style="font-size: 64px; color: #d9d9d9" />
        </template>
        <a-button type="primary" @click="handleCreateGroup">创建群组</a-button>
      </a-empty>
    </div>

    <!-- 聊天面板 -->
    <im-chat-panel v-else-if="groupID" :groupID="groupID" :reportId="reportId" />

    <!-- 错误状态 -->
    <div v-else class="error-wrapper">
      <a-result status="error" title="加载失败" sub-title="无法加载IM群聊,请刷新页面重试">
        <template #extra>
          <a-button type="primary" @click="handleRetry">重试</a-button>
        </template>
      </a-result>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue';
import { message as antMessage } from 'ant-design-vue';
import { TeamOutlined } from '@ant-design/icons-vue';
import ImChatPanel from './im-chat-panel.vue';
import { useIMGroup } from '/@/services/im.service';
import { createPoliceGroupApi } from '/@/api/im/im-group-api';

interface Props {
  reportId?: number;
  groupId?: string;
}

const props = defineProps<Props>();

const { getPoliceGroup } = useIMGroup();

const isLoadingGroup = ref(false);
const groupID = ref<string>(props.groupId || '');

/**
 * 加载群组信息
 */
const loadGroupInfo = async () => {
  if (!props.reportId) {
    console.warn('⚠️ [嵌入式IM] reportId为空,无法加载群组');
    return;
  }

  try {
    isLoadingGroup.value = true;
    console.log('📱 [嵌入式IM] 开始加载警情群组...', { reportId: props.reportId });

    const policeGroup = await getPoliceGroup(props.reportId);

    if (policeGroup && policeGroup.groupId) {
      groupID.value = policeGroup.groupId;
      console.log('✅ [嵌入式IM] 群组加载成功:', policeGroup);
    } else {
      console.log('📝 [嵌入式IM] 该警情暂无IM群组');
      groupID.value = '';
    }
  } catch (error) {
    console.error('❌ [嵌入式IM] 加载群组信息失败:', error);
    antMessage.error('加载IM群组失败');
  } finally {
    isLoadingGroup.value = false;
  }
};

/**
 * 创建群组
 */
const handleCreateGroup = async () => {
  if (!props.reportId) {
    antMessage.error('警情ID不存在,无法创建群组');
    return;
  }

  try {
    isLoadingGroup.value = true;
    console.log('📱 [嵌入式IM] 开始手动创建群组...', { reportId: props.reportId });

    const result = await createPoliceGroupApi(props.reportId);

    if (result.data && result.data.groupId) {
      groupID.value = result.data.groupId;
      console.log('✅ [嵌入式IM] 群组创建成功:', result.data);
      antMessage.success('群组创建成功');
    } else {
      console.error('❌ [嵌入式IM] 群组创建失败: 返回数据为空');
      antMessage.error('群组创建失败');
    }
  } catch (error: any) {
    console.error('❌ [嵌入式IM] 创建群组失败:', error);
    antMessage.error(error?.message || '创建群组失败,请重试');
  } finally {
    isLoadingGroup.value = false;
  }
};

/**
 * 重试加载
 */
const handleRetry = () => {
  loadGroupInfo();
};

/**
 * 组件挂载
 */
onMounted(() => {
  // 如果没有传入groupId但有reportId,则尝试加载
  if (!props.groupId && props.reportId) {
    loadGroupInfo();
  }
});

/**
 * 监听reportId变化
 */
watch(
  () => props.reportId,
  (newReportId) => {
    if (newReportId && !props.groupId) {
      loadGroupInfo();
    }
  }
);

/**
 * 监听groupId变化
 */
watch(
  () => props.groupId,
  (newGroupId) => {
    if (newGroupId) {
      groupID.value = newGroupId;
    }
  }
);
</script>

<style scoped lang="less">
.embedded-im-chat {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.loading-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 300px;

  .loading-text {
    margin-top: 16px;
    font-size: 14px;
    color: #00000073;
  }
}

.empty-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 300px;
}

.error-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 300px;
}
</style>
