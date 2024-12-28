package com.ww.mall.admin.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.mall.admin.dao.DaoFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-21- 13:39
 * @description:
 */
@Slf4j
public abstract class BaseService <M extends BaseMapper<T>, T> extends ServiceImpl<M, T> {

    @Resource
    protected DaoFactory df;

    @Resource
    protected ServiceFactory sf;

    @Resource
    protected RedisTemplate<String, Object> redisTemplate;

}
