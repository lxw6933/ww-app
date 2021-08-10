package com.ww.mall.mvc.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.mvc.entity.MqMsgLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * MQ消费日志
 * 
 * @author ww
 * @date 2021-06-30 09:30:37
 */
@Mapper
public interface MqMsgLogDao extends BaseMapper<MqMsgLogEntity> {
	
}
