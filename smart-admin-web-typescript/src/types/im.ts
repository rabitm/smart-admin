/**
 * OpenIM 类型定义
 *
 * @Author: Claude Code Assistant
 * @Date: 2025-10-08
 * @Copyright 1024创新实验室
 */

/**
 * IM Token信息
 */
export interface ImTokenInfo {
  userToken: string;
  userID: string;
  expireTime: number;
  apiUrl: string;
  wsUrl: string;
  platformID: number;
}

/**
 * IM用户信息
 */
export interface ImUserInfo {
  userID: string;
  nickname: string;
  faceURL: string;
  ex?: string;
}

/**
 * IM群组信息
 */
export interface ImGroupInfo {
  groupID: string;
  groupName: string;
  notification: string;
  introduction: string;
  faceURL: string;
  ownerUserID: string;
  createTime: number;
  memberCount: number;
  status: number;
  creatorUserID: string;
  groupType: number;
  needVerification: number;
  lookMemberInfo: number;
  applyMemberFriend: number;
  notificationUpdateTime: number;
  notificationUserID: string;
  ex?: string;
}

/**
 * IM群组成员信息
 */
export interface ImGroupMemberInfo {
  groupID: string;
  userID: string;
  nickname: string;
  faceURL: string;
  roleLevel: number;  // 1=普通成员, 2=管理员, 3=群主
  joinTime: number;
  joinSource: number;
  muteEndTime: number;
  inviterUserID: string;
  operatorUserID: string;
  ex?: string;
}

/**
 * IM消息类型
 */
export enum ImMessageType {
  TEXT = 101,           // 文本消息
  IMAGE = 102,          // 图片消息
  VOICE = 103,          // 语音消息
  VIDEO = 104,          // 视频消息
  FILE = 105,           // 文件消息
  AT_TEXT = 106,        // @消息
  MERGE = 107,          // 合并消息
  CARD = 108,           // 名片消息
  LOCATION = 109,       // 位置消息
  CUSTOM = 110,         // 自定义消息
  REVOKE = 111,         // 撤回消息通知
  TYPING = 113,         // 正在输入
  QUOTE = 114,          // 引用消息
}

/**
 * IM消息内容
 */
export interface ImMessageContent {
  content: string;
  [key: string]: any;
}

/**
 * IM文本消息元素
 */
export interface ImTextElem {
  content: string;
}

/**
 * IM图片消息元素
 */
export interface ImPictureElem {
  sourcePath?: string;
  sourcePicture?: {
    uuid?: string;
    type?: string;
    size?: number;
    width?: number;
    height?: number;
    url?: string;
  };
  bigPicture?: any;
  snapshotPicture?: any;
}

/**
 * IM文件消息元素
 */
export interface ImFileElem {
  filePath?: string;
  uuid?: string;
  sourceUrl?: string;
  fileName?: string;
  fileSize?: number;
}

/**
 * IM消息
 */
export interface ImMessage {
  clientMsgID: string;
  serverMsgID: string;
  createTime: number;
  sendTime: number;
  sessionType: number;
  sendID: string;
  recvID: string;
  msgFrom: number;
  contentType: number;
  platformID: number;
  senderNickname: string;
  senderFaceURL: string;
  groupID: string;
  content: string;
  seq: number;
  isRead: boolean;
  status: number;
  offlinePush: any;
  attachedInfo: string;
  ex: string;
  localEx: string;
  textElem?: ImTextElem;
  pictureElem?: ImPictureElem;
  soundElem?: any;
  videoElem?: any;
  fileElem?: ImFileElem;
  atTextElem?: any;
  locationElem?: any;
  customElem?: any;
  quoteElem?: any;
  notificationElem?: any;
  attachedInfoElem?: any;
}

/**
 * IM会话类型
 */
export enum ImSessionType {
  SINGLE = 1,   // 单聊
  GROUP = 2,    // 群聊
  NOTIFICATION = 4, // 通知
}

/**
 * IM会话
 */
export interface ImConversation {
  conversationID: string;
  conversationType: number;
  userID: string;
  groupID: string;
  showName: string;
  faceURL: string;
  recvMsgOpt: number;
  unreadCount: number;
  groupAtType: number;
  latestMsg: string;
  latestMsgSendTime: number;
  draftText: string;
  draftTextTime: number;
  isPinned: boolean;
  isPrivateChat: boolean;
  burnDuration: number;
  isNotInGroup: boolean;
  attachedInfo: string;
  ex: string;
}

/**
 * IM连接状态
 */
export enum ImConnectStatus {
  CONNECTING = 1,   // 连接中
  CONNECTED = 2,    // 已连接
  DISCONNECTED = 3, // 断开连接
  RECONNECTING = 4, // 重连中
}

/**
 * IM配置
 */
export interface ImConfig {
  apiUrl: string;
  wsUrl: string;
  platformID: number;
  userID: string;
  token: string;
  dataDir?: string;
  logLevel?: number;
  isLogStandardOutput?: boolean;
}

/**
 * 警情群组映射信息
 */
export interface PoliceGroupMapping {
  id: number;
  groupId: string;
  businessType: string;
  businessId: number;
  groupName: string;
  ownerUserId: string;
  ownerEmployeeId: number;
  memberCount: number;
  createTime: string;
}

/**
 * 邀请成员表单
 */
export interface InviteMembersForm {
  reportId: number;
  employeeIds: number[];
}
