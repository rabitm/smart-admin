/*
 * OA办公业务路由
 *
 * @Author:    Claude Code Assistant
 * @Date:      2025-09-18
 * @Copyright  1024创新实验室 （ https://1024lab.net ），Since 2012
 */

import { MENU_TYPE_ENUM } from '/@/constants/system/menu-const';
import SmartLayout from '/@/layout/index.vue';

export const oaRouters: Array<RouteRecordRaw> = [
  {
    path: '/oa',
    name: '_oa',
    component: SmartLayout,
    meta: {
      title: 'OA办公',
      menuType: MENU_TYPE_ENUM.CATALOG.value,
      icon: 'PartitionOutlined',
    },
    children: [
      // 警情管理
      {
        path: '/oa/police',
        name: '_oa_police',
        meta: {
          title: '警情管理',
          menuType: MENU_TYPE_ENUM.CATALOG.value,
          icon: 'AlertOutlined',
          parentMenuList: [{ name: '_oa', title: 'OA办公' }],
        },
        children: [
          {
            path: '/oa/police/report-list',
            name: 'PoliceReportList',
            component: () => import('/@/views/business/oa/police/police-report-list.vue'),
            meta: {
              title: '警情录入',
              menuType: MENU_TYPE_ENUM.MENU.value,
              icon: 'FileTextOutlined',
              parentMenuList: [
                { name: '_oa', title: 'OA办公' },
                { name: '_oa_police', title: '警情管理' }
              ],
            },
          },
          {
            path: '/oa/police/report-detail',
            name: 'PoliceReportDetail',
            component: () => import('/@/views/business/oa/police/police-report-detail.vue'),
            meta: {
              title: '警情详情',
              hideInMenu: true,
              parentMenuList: [
                { name: '_oa', title: 'OA办公' },
                { name: '_oa_police', title: '警情管理' }
              ],
            },
          },
          {
            path: '/oa/police/report-edit',
            name: 'PoliceReportEdit',
            component: () => import('/@/views/business/oa/police/police-report-edit.vue'),
            meta: {
              title: '编辑警情',
              hideInMenu: true,
              parentMenuList: [
                { name: '_oa', title: 'OA办公' },
                { name: '_oa_police', title: '警情管理' }
              ],
            },
          },
          {
            path: '/oa/police/emergency-intake',
            name: 'EmergencyIntake',
            component: () => import('/@/views/business/oa/police/emergency-intake.vue'),
            meta: {
              title: '智能接警',
              hideInMenu: true,
              parentMenuList: [
                { name: '_oa', title: 'OA办公' },
                { name: '_oa_police', title: '警情管理' }
              ],
            },
          },
          {
            path: '/oa/police/form-template-list',
            name: 'PoliceFormTemplateList',
            component: () => import('/@/views/business/oa/police/form-template-list.vue'),
            meta: {
              title: '表单配置',
              menuType: MENU_TYPE_ENUM.MENU.value,
              icon: 'SettingOutlined',
              parentMenuList: [
                { name: '_oa', title: 'OA办公' },
                { name: '_oa_police', title: '警情管理' }
              ],
            },
          },
          {
            path: '/oa/police/form-template-config',
            name: 'PoliceFormTemplateConfig',
            component: () => import('/@/views/business/oa/police/form-template-config.vue'),
            meta: {
              title: '字段配置',
              hideInMenu: true,
              parentMenuList: [
                { name: '_oa', title: 'OA办公' },
                { name: '_oa_police', title: '警情管理' }
              ],
            },
          },
        ],
      },
      // 企业管理
      {
        path: '/oa/enterprise',
        name: '_oa_enterprise',
        meta: {
          title: '企业管理',
          menuType: MENU_TYPE_ENUM.CATALOG.value,
          icon: 'BankOutlined',
          parentMenuList: [{ name: '_oa', title: 'OA办公' }],
        },
        children: [
          {
            path: '/oa/enterprise/enterprise-list',
            name: 'EnterpriseList',
            component: () => import('/@/views/business/oa/enterprise/enterprise-list.vue'),
            meta: {
              title: '企业信息',
              menuType: MENU_TYPE_ENUM.MENU.value,
              icon: 'TeamOutlined',
              parentMenuList: [
                { name: '_oa', title: 'OA办公' },
                { name: '_oa_enterprise', title: '企业管理' }
              ],
            },
          },
        ],
      },
      // 通知公告
      {
        path: '/oa/notice',
        name: '_oa_notice',
        meta: {
          title: '通知公告',
          menuType: MENU_TYPE_ENUM.CATALOG.value,
          icon: 'SoundOutlined',
          parentMenuList: [{ name: '_oa', title: 'OA办公' }],
        },
        children: [
          {
            path: '/oa/notice/notice-list',
            name: 'NoticeList',
            component: () => import('/@/views/business/oa/notice/notice-list.vue'),
            meta: {
              title: '通知公告',
              menuType: MENU_TYPE_ENUM.MENU.value,
              icon: 'NotificationOutlined',
              parentMenuList: [
                { name: '_oa', title: 'OA办公' },
                { name: '_oa_notice', title: '通知公告' }
              ],
            },
          },
        ],
      },
    ],
  },
];