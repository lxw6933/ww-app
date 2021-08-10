package com.ww.mall.mvc.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.mvc.entity.SysJobDetailLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务记录日志
 * 
 * @author ww
 * @date 2021-05-14 09:30:37
 */
@Mapper
public interface SysJobDetailLogDao extends BaseMapper<SysJobDetailLogEntity> {
	
}
