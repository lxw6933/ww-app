package com.ww.mall.mvc.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.mvc.entity.SysLogEntity;
import com.ww.mall.mvc.entity.dto.SysLogDTO;
import com.ww.mall.mvc.view.query.admin.SysLogQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-13 09:26
 */
@Mapper
public interface SysLogDao extends BaseMapper<SysLogEntity> {

    /**
     * 获取系统日志列表
     * @param query 系统日志查询条件参数
     * @return List<SysLogDTO>
     */
    List<SysLogDTO> page(SysLogQuery query);

    /**
     * 获取指定系统日志
     * @param id 系统日志id
     * @return SysLogDTO
     */
    SysLogDTO info(@Param("id") Long id);

    /**
     * 系统日志下载
     * @param query 系统日志查询条件参数
     * @return List<SysLogDTO>
     */
    List<SysLogDTO> exportExcel(SysLogQuery query);
}
