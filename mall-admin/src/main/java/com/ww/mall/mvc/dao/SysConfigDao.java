package com.ww.mall.mvc.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ww.mall.mvc.entity.SysConfigEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-18 19:51
 */
@Mapper
public interface SysConfigDao extends BaseMapper<SysConfigEntity> {

    /**
     * 根据key获取value
     *
     * @param key key
     * @return value
     */
    String getConfigValue(String key);

}
