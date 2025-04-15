package com.ww.app.auth.serivce;

import com.ww.app.admin.user.rpc.AdminUserApi;
import com.ww.app.auth.config.JwtProperties;
import com.ww.app.member.member.rpc.MemberApi;
import com.ww.app.third.sms.rpc.SmsApi;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-09-22 13:51
 * @description:
 */
public class BaseService {

    @Resource
    protected JwtProperties jwtProperties;

    @Resource
    protected StringRedisTemplate stringRedisTemplate;

    @Resource
    protected MongoTemplate mongoTemplate;

    @Resource
    protected AdminUserApi adminUserApi;

    @Resource
    protected MemberApi memberApi;

    @Resource
    protected SmsApi smsApi;

}
