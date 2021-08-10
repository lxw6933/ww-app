package com.ww.mall.mvc.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.mvc.entity.SysJobEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 定时任务
 * 
 * @author ww
 * @date 2021-05-14 09:30:37
 */
@Mapper
public interface SysJobDao extends BaseMapper<SysJobEntity> {
	
}
