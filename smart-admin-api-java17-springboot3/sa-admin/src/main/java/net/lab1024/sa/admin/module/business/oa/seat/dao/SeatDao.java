package net.lab1024.sa.admin.module.business.oa.seat.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.lab1024.sa.admin.module.business.oa.seat.domain.entity.SeatEntity;
import net.lab1024.sa.admin.module.business.oa.seat.domain.form.SeatQueryForm;
import net.lab1024.sa.admin.module.business.oa.seat.domain.vo.SeatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 席位管理DAO
 *
 * @Author Claude Code Assistant
 * @Date 2025-09-25
 * @Copyright 1024创新实验室
 */
@Mapper
public interface SeatDao extends BaseMapper<SeatEntity> {

    /**
     * 分页查询席位列表
     */
    List<SeatVO> queryPage(Page page, @Param("query") SeatQueryForm queryForm);

    /**
     * 根据席位编码查询席位
     */
    SeatEntity selectBySeatCode(@Param("seatCode") String seatCode);

    /**
     * 查询区域内的所有席位
     */
    List<SeatVO> selectByArea(@Param("area") String area);

    /**
     * 查询指定楼层的席位
     */
    List<SeatVO> selectByFloorNumber(@Param("floorNumber") Integer floorNumber);

    /**
     * 根据IP地址查询席位
     */
    SeatEntity selectByIpAddress(@Param("ipAddress") String ipAddress);

    /**
     * 更新席位状态
     */
    int updateSeatStatus(@Param("seatId") Long seatId, @Param("status") Integer status);

    /**
     * 批量更新席位状态
     */
    int batchUpdateSeatStatus(@Param("seatIds") List<Long> seatIds, @Param("status") Integer status);
}