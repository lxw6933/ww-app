package com.ww.mall.mvc.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.mvc.entity.SysJobDetailEntity;
import com.ww.mall.mvc.entity.dto.SysJobDetailDTO;
import com.ww.mall.mvc.view.query.admin.SysJobDetailQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 任务详情
 * 
 * @author ww
 * @date 2021-05-14 09:30:37
 */
@Mapper
public interface SysJobDetailDao extends BaseMapper<SysJobDetailEntity> {

    /**
     * 查询任务详情
     * @param query 查询条件
     * @return List<SysJobDetailDTO>
     */
    List<SysJobDetailDTO> selectJobDetailDTO(SysJobDetailQuery query);

}
