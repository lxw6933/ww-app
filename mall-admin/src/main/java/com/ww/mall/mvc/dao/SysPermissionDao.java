package com.ww.mall.mvc.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.mvc.entity.SysPermissionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后台权限表
 * 
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@Mapper
public interface SysPermissionDao extends BaseMapper<SysPermissionEntity> {
	
}
