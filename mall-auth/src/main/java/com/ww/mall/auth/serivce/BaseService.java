package com.ww.mall.auth.serivce;

import com.ww.mall.admin.user.AdminUserApi;
import com.ww.mall.auth.config.JwtProperties;
import com.ww.mall.web.feign.MemberFeignService;
import com.ww.mall.web.feign.ThirdServerFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-09-22 13:51
 * @description:
 */
public class BaseService {

    @Autowired
    protected JwtProperties jwtProperties;

    @Autowired
    protected RedisTemplate<String, String> redisTemplate;

    @Autowired
    protected MongoTemplate mongoTemplate;

    @Autowired
    protected ThirdServerFeignService thirdServerFeignService;

    @Autowired
    protected MemberFeignService memberFeignService;

    @Resource
    protected AdminUserApi adminUserApi;
    
}
