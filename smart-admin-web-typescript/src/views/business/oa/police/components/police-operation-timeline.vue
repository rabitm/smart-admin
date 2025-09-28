<!--
  * 警情操作时间轴组件
  *
  * @Author:    Claude Code Assistant
  * @Date:      2025-09-28
  * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
-->
<template>
  <div class="police-operation-timeline">
    <!-- 筛选区域 -->
    <div class="filter-section">
      <a-row :gutter="16">
        <a-col :span="8">
          <a-select
            v-model:value="queryForm.operationType"
            placeholder="请选择操作类型"
            allowClear
            @change="handleSearch"
          >
            <a-select-option value="create">创建</a-select-option>
            <a-select-option value="update">修改</a-select-option>
            <a-select-option value="delete">删除</a-select-option>
            <a-select-option value="view">查看</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="8">
          <a-input
            v-model:value="queryForm.userName"
            placeholder="请输入操作人员"
            allowClear
            @change="handleSearch"
          />
        </a-col>
        <a-col :span="8">
          <a-button type="primary" @click="handleSearch">
            <template #icon>
              <SearchOutlined />
            </template>
            搜索
          </a-button>
        </a-col>
      </a-row>
    </div>

    <!-- 时间轴内容 -->
    <div class="timeline-content">
      <a-spin :spinning="loading" tip="加载中...">
      <a-timeline v-if="timelineData.length > 0" class="compact-timeline">
        <a-timeline-item
          v-for="item in timelineData"
          :key="item.id"
          :color="getTimelineColor(item.type)"
        >
          <template #dot>
            <component :is="getTimelineIcon(item.type)" class="timeline-dot-icon" />
          </template>

          <div class="timeline-item-content">
            <div class="timeline-header">
              <div class="header-left">
                <span class="operation-type">{{ item.operationType }}</span>
                <a-avatar size="small" :src="item.userAvatar" class="user-avatar">
                  {{ item.userName?.charAt(0) }}
                </a-avatar>
                <span class="operator-name">{{ item.userName }}</span>
              </div>
              <span class="timestamp">{{ formatTime(item.createTime) }}</span>
            </div>

            <!-- 字段变更详情 -->
            <div class="field-changes-compact" v-if="item.fieldChanges && Object.keys(item.fieldChanges).length > 0">
              <div class="field-changes-summary">
                <span class="changes-icon">🔄</span>
                <span class="changes-text">涉及 {{ Object.keys(item.fieldChanges).length }} 个字段的修改</span>
              </div>
              <div class="field-changes-list-compact">
                <div
                  v-for="(change, fieldKey) in item.fieldChanges"
                  :key="fieldKey"
                  class="field-change-item-compact"
                >
                  <span class="field-name">{{ change.field }}</span>

                  <!-- 如果有旧值，显示对比 -->
                  <template v-if="change.hasOldValue">
                    <span class="field-old-value-compact">{{ formatFieldValue(change.oldValue) }}</span>
                    <span class="field-arrow">→</span>
                    <span class="field-new-value-compact highlight">{{ formatFieldValue(change.newValue) }}</span>
                  </template>

                  <!-- 如果没有旧值，只显示新值 -->
                  <template v-else>
                    <span class="field-arrow">→</span>
                    <span class="field-new-value-compact">{{ formatFieldValue(change.newValue) }}</span>
                  </template>
                </div>
              </div>
            </div>

            <!-- 操作详情（仅在没有字段变更时显示） -->
            <div class="operation-details-compact" v-else-if="item.content">
              {{ item.content }}
            </div>
          </div>
        </a-timeline-item>
      </a-timeline>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <a-empty
          :image="Empty.PRESENTED_IMAGE_SIMPLE"
          description="暂无操作记录"
        />
      </div>
      </a-spin>
    </div>

    <!-- 分页 -->
    <div class="pagination-section" v-if="total > 0">
      <a-pagination
        v-model:current="queryForm.pageNum"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        :show-size-changer="true"
        :show-quick-jumper="true"
        :show-total="(total, range) => `共 ${total} 条记录`"
        @change="handlePageChange"
        @showSizeChange="handlePageSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, onMounted, reactive, watch } from 'vue';
  import { Empty } from 'ant-design-vue';
  import {
    SearchOutlined,
    PlusCircleOutlined,
    EditOutlined,
    DeleteOutlined,
    EyeOutlined,
    ClockCircleOutlined
  } from '@ant-design/icons-vue';
  import { operateLogApi } from '/@/api/support/operate-log-api';
  import { collaborationHistoryApi } from '/@/api/business/oa/collaboration-history-api';
  import { policeFormConfigApi, type PoliceFormConfigVO } from '/@/api/business/oa/police-form-config-api';
  import { policeReportApi } from '/@/api/business/oa/police-report-api';
  import { smartSentry } from '/@/lib/smart-sentry';

  // 组件属性
  const props = defineProps<{
    reportId: string | number;
    reportNumber?: string;
  }>();

  // 响应式数据
  const loading = ref(false);
  const timelineData = ref([]);
  const total = ref(0);
  const fieldDisplayNames = ref<Record<string, string>>({});
  const reportInfo = ref<any>(null);

  const queryForm = reactive({
    pageNum: 1,
    pageSize: 20,
    operationType: undefined,
    userName: undefined,
  });

  // 初始化字段显示名称
  async function initFieldDisplayNames() {
    try {
      // 1. 获取警情详情信息（包含报告类型）
      if (!reportInfo.value) {
        const reportResponse = await policeReportApi.getDetail(props.reportId);
        if (!reportResponse.data) {
          console.warn('无法获取警情详情信息');
          return;
        }
        reportInfo.value = reportResponse.data;
      }

      // 2. 根据报告类型获取表单配置
      const reportType = reportInfo.value.reportType;
      if (reportType) {
        const configResponse = await policeFormConfigApi.getFormConfig(reportType);
        if (configResponse.data) {
          const config: PoliceFormConfigVO = configResponse.data;

          // 3. 从模板配置中构建字段显示名称映射
          const fieldMap: Record<string, string> = {};

          // 4. 从模板配置中添加所有字段映射
          [...config.step2Fields, ...config.step3Fields].forEach(field => {
            fieldMap[field.key] = field.label;
          });

          // 5. 如果模板中没有配置基础字段，再添加基础字段映射
          const baseFieldsMap = {
            'reportNumber': '警情编号',
            'reportType': '警情类型',
            'reportLevel': '警情等级',
            'status': '处理状态',
            'reporterName': '报警人姓名',
            'reporterPhone': '报警人电话',
            'reporterIdCard': '报警人身份证',
            'incidentLocation': '事发地点',
            'description': '警情描述',
            'handlerName': '处理人员',
            'handleResult': '处理结果',
            'attachments': '附件信息',
            'remark': '备注',
            'createTime': '创建时间',
            'updateTime': '更新时间',
          };

          // 6. 只在模板中没有定义的情况下才添加基础字段
          Object.entries(baseFieldsMap).forEach(([key, label]) => {
            if (!fieldMap[key]) {
              fieldMap[key] = label;
            }
          });

          fieldDisplayNames.value = fieldMap;

          console.log('✅ 字段显示名称映射已初始化:', fieldDisplayNames.value);
        }
      }
    } catch (error) {
      console.error('❌ 初始化字段显示名称失败:', error);
    }
  }

  // 获取时间轴数据
  async function fetchTimelineData() {
    if (!props.reportId) {
      console.warn('PoliceOperationTimeline: reportId is required');
      return;
    }

    console.log('🔍 获取时间轴数据 - reportId:', props.reportId, 'reportNumber:', props.reportNumber);

    try {
      loading.value = true;

      // 确保字段显示名称已初始化
      if (Object.keys(fieldDisplayNames.value).length === 0) {
        await initFieldDisplayNames();
      }

      // 优先尝试协作历史API
      try {
        const historyParams = {
          entityType: 'POLICE_REPORT',
          entityId: props.reportId,
          pageNum: queryForm.pageNum,
          pageSize: queryForm.pageSize,
          operationType: queryForm.operationType,
        };
        console.log('📡 调用协作历史API - 参数:', historyParams);

        const historyResponse = await collaborationHistoryApi.queryHistory(historyParams);
        console.log('✅ 协作历史API响应:', historyResponse);

        if (historyResponse.data?.records && historyResponse.data.records.length > 0) {
          timelineData.value = formatTimelineData(historyResponse.data.records, 'collaboration');
          total.value = historyResponse.data.total || 0;
          console.log('📊 使用协作历史数据，条数:', historyResponse.data.records.length);
          return;
        } else {
          console.log('⚠️ 协作历史API返回空数据');
        }
      } catch (collaborationError) {
        console.log('❌ 协作历史API调用失败，使用操作日志API作为备选:', collaborationError);
      }

      // 备选：直接获取所有操作日志数据，在前端进行筛选
      try {
        const allLogsParams = {
          pageNum: 1,
          pageSize: 100, // 获取更多数据用于筛选
        };
        console.log('📡 获取所有操作日志进行前端筛选 - 参数:', allLogsParams);
        const allLogsResponse = await operateLogApi.queryList(allLogsParams);
        console.log('✅ 所有操作日志响应:', allLogsResponse);

        if (allLogsResponse.data?.list && allLogsResponse.data.list.length > 0) {
          console.log('📋 系统中有操作日志，总数量:', allLogsResponse.data.list.length);

          // 严格筛选：只显示与当前警情ID相关的操作日志
          let filteredLogs = allLogsResponse.data.list.filter(log => {
            if (!log.url) return false;

            // 检查URL路径中是否包含当前警情ID
            const urlContainsReportId = log.url.includes(`/${props.reportId}`) ||
                                      log.url.includes(`reportId=${props.reportId}`) ||
                                      log.url.includes(`id=${props.reportId}`);

            // 检查请求参数中是否包含当前警情ID
            const requestContainsReportId = log.requestParam &&
                                          log.requestParam.includes(`"reportId":"${props.reportId}"`) ||
                                          log.requestParam?.includes(`"id":"${props.reportId}"`) ||
                                          log.requestParam?.includes(`reportId=${props.reportId}`);

            // 检查响应参数中是否包含当前警情ID
            const responseContainsReportId = log.responseParam &&
                                           log.responseParam.includes(`"reportId":"${props.reportId}"`) ||
                                           log.responseParam?.includes(`"id":"${props.reportId}"`);

            // 只有明确包含当前警情ID的操作才显示
            return urlContainsReportId || requestContainsReportId || responseContainsReportId;
          });

          console.log('🎯 筛选后的当前警情操作日志数量:', filteredLogs.length);
          if (filteredLogs.length > 0) {
            console.log('🎯 当前警情操作日志样例:', filteredLogs.slice(0, 3));
          } else {
            console.log('⚠️ 未找到当前警情ID相关的操作记录');
            // 输出调试信息以帮助排查问题
            console.log('🔍 当前查找的警情ID:', props.reportId);
            console.log('🔍 所有操作日志中的URL样例:',
              allLogsResponse.data.list.slice(0, 5).map(log => log.url)
            );
          }

          // 应用用户的筛选条件
          if (queryForm.userName) {
            filteredLogs = filteredLogs.filter(log =>
              log.operateUserName && log.operateUserName.includes(queryForm.userName)
            );
          }

          if (queryForm.operationType) {
            filteredLogs = filteredLogs.filter(log => {
              const operationType = getOperationTypeFromUrl(log.url || '');
              return operationType === queryForm.operationType;
            });
          }

          // 分页处理
          const startIndex = (queryForm.pageNum - 1) * queryForm.pageSize;
          const endIndex = startIndex + queryForm.pageSize;
          const paginatedLogs = filteredLogs.slice(startIndex, endIndex);

          timelineData.value = formatTimelineData(paginatedLogs, 'operateLog');
          total.value = filteredLogs.length;
          console.log('📊 最终显示数据条数:', paginatedLogs.length, '总条数:', filteredLogs.length);

        } else {
          console.log('⚠️ 系统中没有任何操作日志记录');
          timelineData.value = [];
          total.value = 0;
        }
      } catch (allLogsError) {
        console.log('❌ 获取操作日志失败:', allLogsError);
        timelineData.value = [];
        total.value = 0;
      }

    } catch (error) {
      console.error('❌ 获取时间轴数据失败:', error);
      smartSentry.captureError(error);
      timelineData.value = [];
      total.value = 0;
    } finally {
      loading.value = false;
    }
  }

  // 格式化时间轴数据
  function formatTimelineData(records: any[], source: 'collaboration' | 'operateLog') {
    return records.map((record, index) => {
      if (source === 'collaboration') {
        return {
          id: record.id || index,
          operationType: record.operationType || '操作',
          content: record.description || record.content || '',
          userName: record.userName || record.operator || '系统',
          userRole: record.userRole || '',
          userAvatar: record.userAvatar || '',
          createTime: record.createTime || record.timestamp,
          type: getOperationType(record.operationType),
          extraInfo: record.extraInfo || {},
        };
      } else {
        const operationType = getOperationTypeFromUrl(record.url);
        const operationDesc = getOperationDescription(record.url, record.content);
        const fieldChanges = parseFieldChanges(record.param, record.response);

        return {
          id: record.operateLogId || index,
          operationType: operationDesc,
          content: buildDetailedContent(record, fieldChanges),
          userName: record.operateUserName || record.userName || '系统',
          userRole: record.userRole || '',
          userAvatar: '',
          createTime: record.operateTime || record.createTime,
          type: operationType,
          extraInfo: buildExtraInfo(record, fieldChanges),
          fieldChanges: fieldChanges, // 新增字段变更信息
        };
      }
    });
  }

  // 解析字段变更信息
  function parseFieldChanges(param: string, response: string) {
    const changes = {};

    try {
      if (!param) return changes;

      const paramData = JSON.parse(param);
      if (!Array.isArray(paramData)) {
        // 尝试解析新格式：包含旧值和新值的JSON格式
        try {
          const changeData = JSON.parse(param);
          if (typeof changeData === 'object' && changeData !== null) {
            // 新格式: {"fieldName":{"old":"oldValue","new":"newValue"}}
            Object.entries(changeData).forEach(([fieldName, changeInfo]: [string, any]) => {
              if (changeInfo && typeof changeInfo === 'object' && changeInfo.hasOwnProperty('old') && changeInfo.hasOwnProperty('new')) {
                const displayName = getFieldDisplayName(fieldName);
                changes[fieldName] = {
                  field: displayName,
                  oldValue: changeInfo.old,
                  newValue: changeInfo.new,
                  hasOldValue: true
                };

                console.log('✅ parseFieldChanges: 成功解析新格式字段变更', {
                  fieldName,
                  displayName,
                  oldValue: changeInfo.old,
                  newValue: changeInfo.new
                });
              }
            });
          }
        } catch (nestedError) {
          console.log('❌ parseFieldChanges: 新格式解析失败:', nestedError);
        }
        return changes;
      }

      // 数据格式分析：
      // [5] - 只有reportId，通常是查询操作
      // [5, "fieldName", "newValue"] - 字段更新操作（旧格式）
      // [5, "__FORM_CONFIG_UPDATE__", "[object Object]"] - 表单配置更新

      if (paramData.length === 3) {
        const [reportId, fieldName, newValue] = paramData;

        // 跳过内部配置字段
        if (fieldName === '__FORM_CONFIG_UPDATE__') {
          return changes;
        }

        // 解析字段变更（旧格式，只有新值）
        if (typeof fieldName === 'string' && fieldName) {
          const displayName = getFieldDisplayName(fieldName);
          changes[fieldName] = {
            field: displayName,
            newValue: newValue,
            hasOldValue: false
          };

          console.log('✅ parseFieldChanges: 成功解析旧格式字段变更', {
            fieldName,
            displayName,
            newValue
          });
        }
      }

    } catch (e) {
      console.log('❌ parseFieldChanges: 解析参数失败:', e);
    }

    return changes;
  }

  // 解析详细变更日志
  function parseDetailedChangeLog(param: string) {
    const changes = {};

    try {
      if (!param) {
        console.log('🔍 parseDetailedChangeLog: param为空');
        return changes;
      }

      console.log('🔍 parseDetailedChangeLog: 原始param数据:', param);
      console.log('🔍 parseDetailedChangeLog: param类型:', typeof param);

      const paramData = JSON.parse(param);
      console.log('🔍 parseDetailedChangeLog: 解析后的paramData:', paramData);
      console.log('🔍 parseDetailedChangeLog: paramData类型:', typeof paramData, 'isArray:', Array.isArray(paramData));

      if (!Array.isArray(paramData) || paramData.length === 0) {
        console.log('🔍 parseDetailedChangeLog: paramData不是数组或为空数组');
        return changes;
      }

      console.log('🔍 parseDetailedChangeLog: paramData.length:', paramData.length);

      for (let i = 0; i < paramData.length; i++) {
        console.log(`🔍 parseDetailedChangeLog: paramData[${i}]:`, paramData[i]);
        console.log(`🔍 parseDetailedChangeLog: paramData[${i}]类型:`, typeof paramData[i]);
      }

      const logContent = paramData[0];
      if (typeof logContent !== 'string') {
        console.log('🔍 parseDetailedChangeLog: logContent不是字符串，类型:', typeof logContent);
        console.log('🔍 parseDetailedChangeLog: logContent内容:', logContent);
        return changes;
      }

      console.log('🔍 parseDetailedChangeLog: logContent字符串内容:', logContent);

      // 解析格式："  • 字段名：原值 → 新值"
      const changePattern = /\s*•\s*([^：]+)：([^→]+)\s*→\s*(.+)/g;
      let match;
      let matchCount = 0;

      while ((match = changePattern.exec(logContent)) !== null) {
        matchCount++;
        console.log(`🔍 parseDetailedChangeLog: 匹配${matchCount}:`, match);

        const fieldName = match[1].trim();
        const oldValue = match[2].trim();
        const newValue = match[3].trim();

        console.log('🔍 parseDetailedChangeLog: 解析字段 -', {
          fieldName,
          oldValue,
          newValue
        });

        changes[fieldName] = {
          field: fieldName,
          oldValue: oldValue === '未填写' || oldValue === '未设置' ? null : oldValue,
          newValue: newValue,
          hasOldValue: true
        };
      }

      console.log('🔍 parseDetailedChangeLog: 正则匹配次数:', matchCount);
      console.log('🔍 parseDetailedChangeLog: 最终解析到的详细变更:', changes);
    } catch (e) {
      console.log('🔍 parseDetailedChangeLog: 解析详细变更日志失败:', e);
    }

    return changes;
  }

  // 构建详细的操作内容描述
  function buildDetailedContent(record: any, fieldChanges: any) {
    let content = record.content || getOperationDescription(record.url, record.content);

    // 如果有字段变更，添加详细信息
    const changeCount = Object.keys(fieldChanges).length;
    if (changeCount > 0) {
      content += `，涉及 ${changeCount} 个字段的修改`;

      // 显示主要字段变更（限制显示数量避免过长）
      const mainChanges = Object.entries(fieldChanges).slice(0, 3);
      const changeDesc = mainChanges.map(([key, change]: [string, any]) => {
        return `${change.field}: ${formatFieldValue(change.newValue)}`;
      }).join('；');

      if (changeDesc) {
        content += `：${changeDesc}`;
        if (changeCount > 3) {
          content += ` 等${changeCount}项`;
        }
      }
    }

    return content;
  }

  // 构建扩展信息
  function buildExtraInfo(record: any, fieldChanges: any) {
    const extraInfo = {
      '操作时间': record.operateTime || record.createTime,
      '请求URL': record.url,
      '请求方法': record.method,
      '操作IP': record.operateUserIp || record.ip,
    };

    // 添加字段变更详情
    if (Object.keys(fieldChanges).length > 0) {
      Object.entries(fieldChanges).forEach(([key, change]: [string, any]) => {
        extraInfo[`${change.field}`] = formatFieldValue(change.newValue);
      });
    }

    return extraInfo;
  }

  // 获取字段显示名称
  function getFieldDisplayName(fieldKey: string): string {
    // 优先使用从数据库动态获取的字段映射
    if (fieldDisplayNames.value[fieldKey]) {
      return fieldDisplayNames.value[fieldKey];
    }

    // 如果动态映射中没有找到，记录日志并返回原始字段名
    console.warn('⚠️ 未找到字段显示名称：', {
      fieldKey,
      reportType: reportInfo.value?.reportType,
      availableFields: Object.keys(fieldDisplayNames.value)
    });

    return fieldKey;
  }

  // 格式化字段值显示
  function formatFieldValue(value: any): string {
    if (value === null || value === undefined) {
      return '空';
    }

    if (typeof value === 'string') {
      return value.length > 30 ? value.substring(0, 30) + '...' : value;
    }

    if (typeof value === 'object') {
      return JSON.stringify(value).length > 30 ?
        JSON.stringify(value).substring(0, 30) + '...' :
        JSON.stringify(value);
    }

    return String(value);
  }

  // 格式化时间显示
  function formatTime(timeString: string): string {
    if (!timeString) return '';

    try {
      const date = new Date(timeString);
      const now = new Date();
      const diff = now.getTime() - date.getTime();

      // 小于1分钟
      if (diff < 60 * 1000) {
        return '刚刚';
      }

      // 小于1小时
      if (diff < 60 * 60 * 1000) {
        const minutes = Math.floor(diff / (60 * 1000));
        return `${minutes}分钟前`;
      }

      // 小于24小时
      if (diff < 24 * 60 * 60 * 1000) {
        const hours = Math.floor(diff / (60 * 60 * 1000));
        return `${hours}小时前`;
      }

      // 超过24小时，显示具体日期
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      if (date >= today) {
        return date.toLocaleTimeString('zh-CN', {
          hour: '2-digit',
          minute: '2-digit'
        });
      } else {
        return date.toLocaleDateString('zh-CN', {
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit'
        });
      }
    } catch (e) {
      return timeString;
    }
  }

  // 从URL推断操作类型
  function getOperationTypeFromUrl(url: string): string {
    if (!url) return 'other';
    if (url.includes('add') || url.includes('create')) return 'create';
    if (url.includes('update') || url.includes('edit')) return 'update';
    if (url.includes('delete')) return 'delete';
    if (url.includes('query') || url.includes('detail') || url.includes('page')) return 'view';
    return 'other';
  }

  // 获取操作描述
  function getOperationDescription(url: string, content: string): string {
    if (content) return content;

    if (!url) return '未知操作';

    if (url.includes('police') || url.includes('oa')) {
      if (url.includes('add') || url.includes('create')) return '创建警情';
      if (url.includes('update') || url.includes('edit')) return '修改警情';
      if (url.includes('delete')) return '删除警情';
      if (url.includes('query') || url.includes('page')) return '查询警情';
      if (url.includes('detail')) return '查看警情详情';
      return '警情相关操作';
    }

    if (url.includes('add') || url.includes('create')) return '创建操作';
    if (url.includes('update') || url.includes('edit')) return '修改操作';
    if (url.includes('delete')) return '删除操作';
    if (url.includes('query') || url.includes('detail')) return '查看操作';

    return '系统操作';
  }

  // 获取操作类型
  function getOperationType(operationType: string): string {
    const typeMap = {
      '创建': 'create',
      '修改': 'update',
      '更新': 'update',
      '删除': 'delete',
      '查看': 'view',
      '详情': 'view',
    };
    return typeMap[operationType] || 'other';
  }

  // 获取时间轴颜色
  function getTimelineColor(type: string): string {
    const colorMap = {
      create: 'green',
      update: 'blue',
      delete: 'red',
      view: 'gray',
      other: 'default',
    };
    return colorMap[type] || 'default';
  }

  // 获取时间轴图标
  function getTimelineIcon(type: string) {
    const iconMap = {
      create: PlusCircleOutlined,
      update: EditOutlined,
      delete: DeleteOutlined,
      view: EyeOutlined,
      other: ClockCircleOutlined,
    };
    return iconMap[type] || ClockCircleOutlined;
  }

  // 搜索处理
  function handleSearch() {
    queryForm.pageNum = 1;
    fetchTimelineData();
  }

  // 分页处理
  function handlePageChange(page: number, pageSize: number) {
    queryForm.pageNum = page;
    queryForm.pageSize = pageSize;
    fetchTimelineData();
  }

  function handlePageSizeChange(current: number, size: number) {
    queryForm.pageNum = 1;
    queryForm.pageSize = size;
    fetchTimelineData();
  }

  // 监听reportId变化
  watch(() => props.reportId, async (newReportId) => {
    if (newReportId) {
      // 重置状态
      fieldDisplayNames.value = {};
      reportInfo.value = null;
      queryForm.pageNum = 1;

      // 重新获取数据
      await fetchTimelineData();
    }
  }, { immediate: true });

  // 组件挂载时获取数据
  onMounted(() => {
    if (props.reportId) {
      fetchTimelineData();
    }
  });
</script>

<style scoped>
.police-operation-timeline {
  padding: 16px;
}

.filter-section {
  margin-bottom: 24px;
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
}

.timeline-content {
  min-height: 200px;
  margin-bottom: 24px;
}

.timeline-item-content {
  padding-left: 12px;
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.operation-type {
  font-weight: 600;
  color: #1890ff;
  font-size: 14px;
}

.timestamp {
  color: #999;
  font-size: 12px;
}

.timeline-body {
  background: #f9f9f9;
  padding: 12px;
  border-radius: 6px;
  border-left: 3px solid #1890ff;
}

.operator-info {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  gap: 8px;
}

.operator-name {
  font-weight: 500;
  color: #333;
}

.operator-role {
  color: #666;
  font-size: 12px;
  background: #e6f7ff;
  padding: 2px 6px;
  border-radius: 4px;
}

.operation-details {
  color: #666;
  line-height: 1.6;
  margin-bottom: 8px;
  word-break: break-word;
}

.field-changes {
  margin-top: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
  border-left: 3px solid #52c41a;
}

.field-changes-title {
  display: flex;
  align-items: center;
  font-weight: 500;
  color: #52c41a;
  margin-bottom: 8px;
  font-size: 13px;
}

.changes-icon {
  margin-right: 6px;
}

.field-changes-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-change-item {
  padding: 8px 0;
  font-size: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.field-change-item:last-child {
  border-bottom: none;
}

.field-name {
  font-weight: 600;
  color: #262626;
  margin-bottom: 6px;
  display: block;
}

.field-comparison {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.field-simple-change {
  display: flex;
  align-items: center;
  gap: 8px;
}

.field-old-value,
.field-new-value {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.value-label {
  font-size: 11px;
  color: #8c8c8c;
  font-weight: 500;
}

.value-content {
  padding: 4px 8px;
  border-radius: 4px;
  word-break: break-word;
  font-size: 12px;
}

.field-old-value .value-content {
  background: #fff2e8;
  color: #fa8c16;
  border: 1px solid #ffd591;
}

.field-new-value .value-content {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.field-simple-change .field-new-value {
  background: #f6ffed;
  padding: 4px 8px;
  border-radius: 4px;
  color: #52c41a;
  font-weight: 500;
}

.field-arrow {
  color: #1890ff;
  font-weight: bold;
  font-size: 14px;
  align-self: center;
}

.operation-extra {
  margin-top: 12px;
}

.extra-info-value {
  word-break: break-word;
  max-width: 200px;
  display: inline-block;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

.pagination-section {
  text-align: center;
  padding: 16px 0;
  border-top: 1px solid #f0f0f0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .timeline-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .operator-info {
    flex-wrap: wrap;
  }

  .filter-section .ant-col {
    margin-bottom: 8px;
  }
}

/* 紧凑型时间轴样式 */
.compact-timeline {
  :deep(.ant-timeline-item) {
    padding-bottom: 12px;
  }

  :deep(.ant-timeline-item-content) {
    margin-left: 30px;
    margin-top: -2px;
  }

  .timeline-dot-icon {
    font-size: 12px;
  }
}

/* 时间轴项目内容 */
.timeline-item-content {
  .timeline-header {
    .header-left {
      display: flex;
      align-items: center;
      gap: 8px;

      .operation-type {
        font-weight: 600;
        color: #1890ff;
        font-size: 13px;
      }

      .user-avatar {
        width: 20px;
        height: 20px;
        font-size: 10px;
      }

      .operator-name {
        color: #666;
        font-size: 12px;
      }
    }

    .timestamp {
      color: #999;
      font-size: 11px;
    }
  }
}

/* 紧凑型字段变更 */
.field-changes-compact {
  .field-changes-summary {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 6px;
    font-size: 12px;
    color: #666;

    .changes-icon {
      font-size: 12px;
    }
  }

  .field-changes-list-compact {
    background: #f8f9fa;
    border-radius: 4px;
    padding: 8px;

    .field-change-item-compact {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-bottom: 4px;
      font-size: 12px;

      &:last-child {
        margin-bottom: 0;
      }

      .field-name {
        color: #1890ff;
        font-weight: 500;
        min-width: 80px;
        flex-shrink: 0;
      }

      .field-arrow {
        color: #999;
        font-size: 11px;
      }

      .field-old-value-compact {
        color: #999;
        text-decoration: line-through;
        background: #f5f5f5;
        padding: 1px 4px;
        border-radius: 2px;
        font-size: 11px;
        max-width: 100px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .field-new-value-compact {
        color: #333;
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;

        &.highlight {
          background: #e6f7ff;
          padding: 1px 4px;
          border-radius: 2px;
          color: #1890ff;
          font-weight: 500;
        }
      }

      /* 兼容旧样式 */
      .field-new-value {
        color: #333;
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
}

/* 紧凑型操作详情 */
.operation-details-compact {
  font-size: 12px;
  color: #666;
  background: #f8f9fa;
  padding: 6px 8px;
  border-radius: 4px;
  margin-top: 4px;
}
</style>