<template>
  <div class="seat-detail">
    <a-card title="席位详情" :bordered="false">
      <template #extra>
        <a-button @click="goBack">返回</a-button>
      </template>

      <a-descriptions v-if="seatDetail" :column="2" bordered>
        <a-descriptions-item label="席位ID">
          {{ seatDetail.seatId }}
        </a-descriptions-item>
        <a-descriptions-item label="席位编码">
          {{ seatDetail.seatCode }}
        </a-descriptions-item>
        <a-descriptions-item label="席位名称">
          {{ seatDetail.seatName }}
        </a-descriptions-item>
        <a-descriptions-item label="席位类型">
          <a-tag :color="getSeatTypeColor(seatDetail.seatType)">
            {{ getSeatTypeName(seatDetail.seatType) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="席位状态">
          <a-tag :color="getSeatStatusColor(seatDetail.seatStatus)">
            {{ getSeatStatusName(seatDetail.seatStatus) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="启用状态">
          <a-tag :color="seatDetail.enabledFlag ? 'green' : 'red'">
            {{ seatDetail.enabledFlag ? '启用' : '禁用' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="位置坐标">
          X: {{ seatDetail.positionX || '-' }}, Y: {{ seatDetail.positionY || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="楼层">
          {{ seatDetail.floorNumber || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="区域">
          {{ seatDetail.area || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="IP地址">
          {{ seatDetail.ipAddress || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="MAC地址">
          {{ seatDetail.macAddress || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">
          {{ seatDetail.remark || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="创建人">
          {{ seatDetail.createUserName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">
          {{ seatDetail.createTime ? dayjs(seatDetail.createTime).format('YYYY-MM-DD HH:mm:ss') : '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="更新时间" :span="2">
          {{ seatDetail.updateTime ? dayjs(seatDetail.updateTime).format('YYYY-MM-DD HH:mm:ss') : '-' }}
        </a-descriptions-item>
      </a-descriptions>

      <a-skeleton v-else :loading="loading" active />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { seatApi, SeatVO } from '/@/api/business/oa/seat-api';
import { SEAT_STATUS_ENUM, SEAT_TYPE_ENUM } from '/@/constants/business/oa/seat-const';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const seatDetail = ref<SeatVO | null>(null);

const getSeatTypeName = (type: number) => {
  const typeItem = Object.values(SEAT_TYPE_ENUM).find(item => item.value === type);
  return typeItem?.desc || '未知';
};

const getSeatTypeColor = (type: number) => {
  const typeItem = Object.values(SEAT_TYPE_ENUM).find(item => item.value === type);
  return typeItem?.color || '#666666';
};

const getSeatStatusName = (status: number) => {
  const statusItem = Object.values(SEAT_STATUS_ENUM).find(item => item.value === status);
  return statusItem?.desc || '未知';
};

const getSeatStatusColor = (status: number) => {
  const statusItem = Object.values(SEAT_STATUS_ENUM).find(item => item.value === status);
  return statusItem?.color || '#666666';
};

const goBack = () => {
  router.back();
};

const loadSeatDetail = async () => {
  const seatId = route.params.id as string;
  if (!seatId) {
    message.error('席位ID不能为空');
    return;
  }

  loading.value = true;
  try {
    const res = await seatApi.getDetail(Number(seatId));
    if (res.data) {
      seatDetail.value = res.data;
    }
  } catch (error) {
    message.error('加载席位详情失败');
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadSeatDetail();
});
</script>

<style scoped lang="less">
.seat-detail {
  .ant-descriptions {
    margin-top: 16px;
  }
}
</style>