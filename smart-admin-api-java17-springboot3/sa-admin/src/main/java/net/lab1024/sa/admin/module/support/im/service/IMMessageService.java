package net.lab1024.sa.admin.module.support.im.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.im.dao.IMMessageDao;
import net.lab1024.sa.admin.module.support.im.dao.IMUserMappingDao;
import net.lab1024.sa.admin.module.support.im.domain.entity.IMMessageEntity;
import net.lab1024.sa.admin.module.support.im.domain.entity.IMUserMappingEntity;
import net.lab1024.sa.admin.module.support.im.domain.vo.MessageVO;
import net.lab1024.sa.base.common.util.SmartBeanUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * IM消息服务
 *
 * 功能: 消息的保存和查询
 * 用途: 解决无痕模式首次登录无法加载历史消息的问题
 *
 * @Author Claude Code Assistant
 * @Date 2025-10-11
 * @Copyright 1024创新实验室
 */
@Slf4j
@Service
public class IMMessageService {

    @Resource
    private IMMessageDao imMessageDao;

    @Resource
    private IMUserMappingDao imUserMappingDao;

    /**
     * 保存消息到数据库
     *
     * @param messageVO 消息VO
     * @param reportId 警情ID
     * @param groupId 群组ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveMessage(MessageVO messageVO, Long reportId, String groupId) {
        if (messageVO == null || messageVO.getMessageId() == null) {
            log.warn("⚠️ [消息保存] 消息为空或消息ID为空,跳过保存");
            return;
        }

        try {
            // 检查消息是否已存在
            IMMessageEntity existingMessage = imMessageDao.selectByMessageId(messageVO.getMessageId());
            if (existingMessage != null) {
                log.debug("🔄 [消息保存] 消息已存在,跳过保存: {}", messageVO.getMessageId());
                return;
            }

            // 构建消息实体
            IMMessageEntity entity = new IMMessageEntity();
            entity.setReportId(reportId);
            entity.setGroupId(groupId);
            entity.setMessageId(messageVO.getMessageId());
            entity.setServerMessageId(messageVO.getServerMessageId());
            entity.setConversationId(messageVO.getConversationId());

            // 发送者信息
            entity.setSenderId(messageVO.getSenderId());
            entity.setSenderName(messageVO.getSenderName());
            entity.setSenderAvatar(messageVO.getSenderAvatar());

            // 尝试解析发送者员工ID
            if (messageVO.getSenderId() != null && messageVO.getSenderId().startsWith("emp_")) {
                try {
                    Long employeeId = Long.parseLong(messageVO.getSenderId().substring(4));
                    entity.setSenderEmployeeId(employeeId);
                } catch (NumberFormatException e) {
                    log.warn("⚠️ [消息保存] 无法解析员工ID: {}", messageVO.getSenderId());
                }
            }

            // 消息内容
            entity.setContentType(messageVO.getContentType());
            entity.setContent(messageVO.getContent());

            // 如果有复杂内容,保存完整JSON
            if (messageVO.getContentJson() != null) {
                entity.setContentJson(messageVO.getContentJson());
            }

            // 时间信息
            entity.setSendTime(messageVO.getSendTime());
            entity.setSeq(messageVO.getSeq());

            // 消息状态
            entity.setStatus(1); // 1-正常
            entity.setIsRead(0); // 0-未读
            entity.setDeletedFlag(0); // 0-未删除
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());

            // 保存到数据库
            imMessageDao.insert(entity);

            log.info("✅ [消息保存] 消息保存成功: reportId={}, messageId={}, content={}",
                    reportId, messageVO.getMessageId(), messageVO.getContent());

        } catch (Exception e) {
            log.error("❌ [消息保存] 保存失败: reportId={}, messageId={}",
                    reportId, messageVO.getMessageId(), e);
            // 不抛出异常,避免影响主流程
        }
    }

    /**
     * 根据警情ID查询历史消息
     *
     * @param reportId 警情ID
     * @param limit 数量限制
     * @return 消息列表
     */
    public List<MessageVO> getMessageHistory(Long reportId, Integer limit) {
        if (reportId == null) {
            log.warn("⚠️ [消息查询] 警情ID为空");
            return Collections.emptyList();
        }

        if (limit == null || limit <= 0) {
            limit = 50; // 默认50条
        }

        try {
            log.info("📥 [消息查询] 开始查询历史消息: reportId={}, limit={}", reportId, limit);

            List<IMMessageEntity> entityList = imMessageDao.selectByReportId(reportId, limit);

            if (entityList == null || entityList.isEmpty()) {
                log.info("📭 [消息查询] 暂无历史消息");
                return Collections.emptyList();
            }

            // 转换为VO
            List<MessageVO> messageList = entityList.stream()
                    .map(this::entityToVO)
                    .collect(Collectors.toList());

            // 反转列表,使其按时间正序(旧消息在前,新消息在后)
            Collections.reverse(messageList);

            log.info("✅ [消息查询] 查询成功, 数量: {}", messageList.size());

            return messageList;

        } catch (Exception e) {
            log.error("❌ [消息查询] 查询失败: reportId={}", reportId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据警情ID和时间范围查询历史消息
     *
     * @param reportId 警情ID
     * @param beforeTime 时间戳(毫秒),查询此时间之前的消息
     * @param limit 数量限制
     * @return 消息列表
     */
    public List<MessageVO> getMessageHistoryByTime(Long reportId, Long beforeTime, Integer limit) {
        if (reportId == null) {
            log.warn("⚠️ [消息查询] 警情ID为空");
            return Collections.emptyList();
        }

        if (limit == null || limit <= 0) {
            limit = 50;
        }

        try {
            log.info("📥 [消息查询] 查询历史消息: reportId={}, beforeTime={}, limit={}",
                    reportId, beforeTime, limit);

            List<IMMessageEntity> entityList = imMessageDao.selectByReportIdAndTime(reportId, beforeTime, limit);

            if (entityList == null || entityList.isEmpty()) {
                return Collections.emptyList();
            }

            List<MessageVO> messageList = entityList.stream()
                    .map(this::entityToVO)
                    .collect(Collectors.toList());

            Collections.reverse(messageList);

            log.info("✅ [消息查询] 查询成功, 数量: {}", messageList.size());

            return messageList;

        } catch (Exception e) {
            log.error("❌ [消息查询] 查询失败: reportId={}, beforeTime={}", reportId, beforeTime, e);
            return Collections.emptyList();
        }
    }

    /**
     * 统计警情的消息总数
     *
     * @param reportId 警情ID
     * @return 消息总数
     */
    public Integer countMessages(Long reportId) {
        if (reportId == null) {
            return 0;
        }

        try {
            return imMessageDao.countByReportId(reportId);
        } catch (Exception e) {
            log.error("❌ [消息统计] 统计失败: reportId={}", reportId, e);
            return 0;
        }
    }

    /**
     * 批量保存消息(用于历史数据导入)
     *
     * @param messageList 消息列表
     * @return 保存数量
     */
    @Transactional(rollbackFor = Exception.class)
    public Integer batchSaveMessages(List<IMMessageEntity> messageList) {
        if (messageList == null || messageList.isEmpty()) {
            return 0;
        }

        try {
            log.info("📦 [批量保存] 开始批量保存消息, 数量: {}", messageList.size());

            Integer count = imMessageDao.batchInsert(messageList);

            log.info("✅ [批量保存] 保存成功, 数量: {}", count);

            return count;

        } catch (Exception e) {
            log.error("❌ [批量保存] 保存失败", e);
            throw e;
        }
    }

    /**
     * 实体转VO
     *
     * @param entity 实体
     * @return VO
     */
    private MessageVO entityToVO(IMMessageEntity entity) {
        MessageVO vo = new MessageVO();

        vo.setMessageId(entity.getMessageId());
        vo.setServerMessageId(entity.getServerMessageId());
        vo.setConversationId(entity.getConversationId());

        vo.setSenderId(entity.getSenderId());
        vo.setSenderName(entity.getSenderName());
        vo.setSenderAvatar(entity.getSenderAvatar());

        vo.setContentType(entity.getContentType());
        vo.setContent(entity.getContent());

        // 如果有完整JSON,设置到VO
        if (entity.getContentJson() != null && !entity.getContentJson().isEmpty()) {
            vo.setContentJson(entity.getContentJson());
        }

        vo.setSendTime(entity.getSendTime());
        vo.setSeq(entity.getSeq());

        // TODO: 判断是否是当前用户发送的消息(需要当前用户信息)
        vo.setIsSelf(false);

        return vo;
    }

    /**
     * 从OpenIM消息JSON解析为MessageVO
     *
     * @param messageJson OpenIM消息JSON
     * @return MessageVO
     */
    public MessageVO parseOpenIMMessage(JSONObject messageJson) {
        if (messageJson == null) {
            return null;
        }

        try {
            MessageVO vo = new MessageVO();

            vo.setMessageId(messageJson.getString("clientMsgID"));
            vo.setServerMessageId(messageJson.getString("serverMsgID"));
            vo.setConversationId(messageJson.getString("conversationID"));

            vo.setSenderId(messageJson.getString("sendID"));
            vo.setSenderName(messageJson.getString("senderNickname"));
            vo.setSenderAvatar(messageJson.getString("senderFaceURL"));

            vo.setContentType(messageJson.getInteger("contentType"));
            vo.setSendTime(messageJson.getLong("sendTime"));
            vo.setSeq(messageJson.getLong("seq"));

            // 解析消息内容
            String contentStr = messageJson.getString("content");
            if (contentStr != null && !contentStr.isEmpty()) {
                try {
                    JSONObject contentJson = JSON.parseObject(contentStr);
                    // 文本消息的内容在 "content" 或 "text" 字段
                    String text = contentJson.getString("content");
                    if (text == null) {
                        text = contentJson.getString("text");
                    }
                    vo.setContent(text);
                    vo.setContentJson(contentStr);
                } catch (Exception e) {
                    // 如果解析失败,直接使用原始字符串
                    vo.setContent(contentStr);
                }
            }

            return vo;

        } catch (Exception e) {
            log.error("❌ [消息解析] 解析OpenIM消息失败", e);
            return null;
        }
    }
}
