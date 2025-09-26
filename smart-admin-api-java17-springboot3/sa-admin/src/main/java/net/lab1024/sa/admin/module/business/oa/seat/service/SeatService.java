package net.lab1024.sa.admin.module.business.oa.seat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.lab1024.sa.admin.module.business.oa.seat.dao.SeatDao;
import net.lab1024.sa.admin.module.business.oa.seat.domain.entity.SeatEntity;
import net.lab1024.sa.admin.module.business.oa.seat.domain.form.SeatQueryForm;
import net.lab1024.sa.admin.module.business.oa.seat.domain.vo.SeatVO;
import net.lab1024.sa.base.common.domain.PageResult;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.common.util.SmartBeanUtil;
import net.lab1024.sa.base.common.util.SmartPageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 座位管理服务
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-25
 * @Copyright 1024创新实验室
 */
@Service
public class SeatService {

    @Autowired
    private SeatDao seatDao;

    @Autowired
    private SeatSyncService seatSyncService;

    /**
     * 分页查询座位列表
     */
    public PageResult<SeatVO> queryPage(SeatQueryForm queryForm) {
        Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
        List<SeatVO> list = seatDao.queryPage(page, queryForm);
        PageResult<SeatVO> pageResult = SmartPageUtil.convert2PageResult(page, list);
        return pageResult;
    }

    /**
     * 获取座位详情
     */
    public ResponseDTO<SeatVO> getDetail(Long seatId) {
        SeatEntity entity = seatDao.selectById(seatId);
        if (entity == null) {
            return ResponseDTO.userErrorParam("座位不存在");
        }
        SeatVO vo = SmartBeanUtil.copy(entity, SeatVO.class);
        return ResponseDTO.ok(vo);
    }

    /**
     * 添加座位
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> add(SeatEntity seatEntity) {
        // 检查席位编码是否重复
        SeatEntity existSeat = seatDao.selectBySeatCode(seatEntity.getSeatCode());
        if (existSeat != null) {
            return ResponseDTO.userErrorParam("席位编码已存在");
        }

        seatEntity.setCreateTime(LocalDateTime.now());
        seatEntity.setDeletedFlag(false);
        seatEntity.setEnabledFlag(true);
        seatEntity.setSeatStatus(0); // 默认空闲状态
        seatDao.insert(seatEntity);
        return ResponseDTO.ok();
    }

    /**
     * 更新座位
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> update(SeatEntity seatEntity) {
        SeatEntity existSeat = seatDao.selectById(seatEntity.getSeatId());
        if (existSeat == null) {
            return ResponseDTO.userErrorParam("座位不存在");
        }

        // 检查席位编码是否重复（排除当前席位）
        SeatEntity duplicateSeat = seatDao.selectBySeatCode(seatEntity.getSeatCode());
        if (duplicateSeat != null && !duplicateSeat.getSeatId().equals(seatEntity.getSeatId())) {
            return ResponseDTO.userErrorParam("席位编码已存在");
        }

        seatEntity.setUpdateTime(LocalDateTime.now());
        seatDao.updateById(seatEntity);
        return ResponseDTO.ok();
    }

    /**
     * 删除座位
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> delete(Long seatId) {
        SeatEntity existSeat = seatDao.selectById(seatId);
        if (existSeat == null) {
            return ResponseDTO.userErrorParam("座位不存在");
        }

        // 检查座位是否在使用中
        if (existSeat.getSeatStatus() == 1) {
            return ResponseDTO.userErrorParam("席位使用中，无法删除");
        }

        existSeat.setDeletedFlag(true);
        existSeat.setUpdateTime(LocalDateTime.now());
        seatDao.updateById(existSeat);
        return ResponseDTO.ok();
    }

    /**
     * 席位登录/占用
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> reserve(Long seatId, Long userId, String userName,
                                       LocalDateTime startTime, LocalDateTime endTime) {
        SeatEntity seat = seatDao.selectById(seatId);
        if (seat == null) {
            return ResponseDTO.userErrorParam("席位不存在");
        }

        if (!seat.getEnabledFlag()) {
            return ResponseDTO.userErrorParam("席位已禁用");
        }

        if (seat.getSeatStatus() != 0) {
            return ResponseDTO.userErrorParam("席位不可用");
        }

        String oldStatus = getSeatStatusName(seat.getSeatStatus());
        // 席位状态改为忙碌
        seat.setSeatStatus(1);
        seat.setUpdateTime(LocalDateTime.now());
        seatDao.updateById(seat);

        // 发送WebSocket通知
        seatSyncService.notifySeatStatusChange(seatId, oldStatus, "忙碌", userId, userName);
        seatSyncService.notifySeatReservationChange(seatId, "login", userId, userName, startTime, endTime);

        return ResponseDTO.ok();
    }

    /**
     * 设置席位为忙碌状态
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> occupy(Long seatId, Long userId, String userName) {
        SeatEntity seat = seatDao.selectById(seatId);
        if (seat == null) {
            return ResponseDTO.userErrorParam("席位不存在");
        }

        if (!seat.getEnabledFlag()) {
            return ResponseDTO.userErrorParam("席位已禁用");
        }

        if (seat.getSeatStatus() != 0) {
            return ResponseDTO.userErrorParam("席位不可使用");
        }

        String oldStatus = getSeatStatusName(seat.getSeatStatus());
        seat.setSeatStatus(1); // 忙碌
        seat.setUpdateTime(LocalDateTime.now());
        seatDao.updateById(seat);

        // 发送WebSocket通知
        seatSyncService.notifySeatStatusChange(seatId, oldStatus, "忙碌", userId, userName);

        return ResponseDTO.ok();
    }

    /**
     * 释放席位（设置为空闲状态）
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO<String> release(Long seatId) {
        SeatEntity seat = seatDao.selectById(seatId);
        if (seat == null) {
            return ResponseDTO.userErrorParam("席位不存在");
        }

        String oldStatus = getSeatStatusName(seat.getSeatStatus());

        seat.setSeatStatus(0); // 空闲
        seat.setUpdateTime(LocalDateTime.now());
        seatDao.updateById(seat);

        // 发送WebSocket通知
        seatSyncService.notifySeatStatusChange(seatId, oldStatus, "空闲", null, null);

        return ResponseDTO.ok();
    }

    /**
     * 获取席位状态名称
     */
    private String getSeatStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "空闲";
            case 1:
                return "忙碌";
            case 2:
                return "离线";
            case 3:
                return "维护";
            default:
                return "未知";
        }
    }
}