package com.ww.mall.seckill.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.digest.MD5;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wf.captcha.ArithmeticCaptcha;
import com.ww.mall.common.common.MallClientUser;
import com.ww.mall.common.constant.RedisKeyConstant;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.common.utils.IdUtil;
import com.ww.mall.rabbitmq.MallPublisher;
import com.ww.mall.rabbitmq.exchange.ExchangeConstant;
import com.ww.mall.rabbitmq.routekey.RouteKeyConstant;
import com.ww.mall.redis.MallRedisTemplate;
import com.ww.mall.seckill.service.SeckillService;
import com.ww.mall.seckill.view.bo.SecKillOrderReqBO;
import com.ww.mall.web.utils.AuthorizationContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2024-02-06- 14:55
 * @description:
 */
@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private MallRedisTemplate mallRedisTemplate;

    @Autowired
    private MallPublisher mallPublisher;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @PostConstruct
    public void init() {
        mallRedisTemplate.initStock("skuStock", 1000);
        // 初始化活动数据信息
        activityCache.get("activityRedisCacheKey", key -> redisTemplate.opsForValue().get(key));
    }

    private static final Cache<String, String> activityCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(10000)
            .build();

    @Override
    public void captcha(HttpServletResponse response, String activityCode, Long skuId) {
        // 获取用户
        MallClientUser clientUser = AuthorizationContext.getClientUser();
        // 算术类型
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 48);
        captcha.getArithmeticString();
//        // gif类型
//        GifCaptcha captcha = new GifCaptcha(130, 48);
//        // 中文类型
//        ChineseCaptcha captcha = new ChineseCaptcha(130, 48);
//        // 中文gif类型
//        ChineseGifCaptcha captcha = new ChineseGifCaptcha(130, 48);
        captcha.setLen(3);
        // 验证码结果存入redis
        String key = getSecKillVerCodeKey(clientUser, activityCode, skuId);
        redisTemplate.opsForValue().set(key, captcha.text(), 1, TimeUnit.MINUTES);
        // 输出图片流
        try {
            captcha.out(response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getSecKillPath(String activityCode, Long skuId) {
        // 获取用户
        MallClientUser clientUser = AuthorizationContext.getClientUser();

        String key = getSecKillPathKey(clientUser, activityCode, skuId);
        String userSecKillPath = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotEmpty(userSecKillPath)) {
            return userSecKillPath;
        }
        // 生成secKillPath
        userSecKillPath = MD5.create().digestHex(UUID.randomUUID() + activityCode + clientUser.getMemberId().toString(), StandardCharsets.UTF_8.name());
        // 加密后地址存入redis
        redisTemplate.opsForValue().set(key, userSecKillPath, 1, TimeUnit.MINUTES);
        return userSecKillPath;
    }

    @Override
    public Boolean doSecKillOrder(String userSecKillPath, SecKillOrderReqBO reqBO) {
        // 校验用户是否存在秒杀资格
        MallClientUser clientUser = AuthorizationContext.getClientUser();
        // 校验地址是否正确
        Assert.isFalse(checkSecKillPath(clientUser, userSecKillPath, reqBO.getActivityCode(), reqBO.getSkuId()), () -> new ApiException("秒杀路径异常"));
        // 校验图形验证码是否正确
        Assert.isFalse(checkSecKillVerCode(clientUser, reqBO.getActivityCode(), reqBO.getSkuId(), reqBO.getCaptcha()), () -> new ApiException("验证码错误"));
        // 本地缓存存储活动信息，校验活动信息

        // 本地缓存商品信息，校验商品信息
        if (mallRedisTemplate.decrementStock("skuStock", 1)) {
            String orderDate = DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN);
            String orderNo = IdUtil.generatorIdStr();
            mallPublisher.publishMsg(ExchangeConstant.MALL_OMS_EXCHANGE, RouteKeyConstant.MALL_CREATE_ORDER_KEY, orderNo);
            log.info("订单【{}】下单成功【{}】", orderNo, orderDate);
            return true;
        } else {
            // TODO 标记秒杀结束，无库存
            return false;
        }
    }

    private boolean checkSecKillPath(MallClientUser clientUser, String userSecKillPath, String activityCode, Long skuId) {
        if (StringUtils.isEmpty(userSecKillPath)) {
            return false;
        }
        String key = getSecKillPathKey(clientUser, activityCode, skuId);
        String userSecKillPathCache = redisTemplate.opsForValue().get(key);
        return userSecKillPath.equals(userSecKillPathCache);
    }

    private boolean checkSecKillVerCode(MallClientUser clientUser, String activityCode, Long skuId, String userVerCode) {
        String key = getSecKillVerCodeKey(clientUser, activityCode, skuId);
        String verCodeCache = redisTemplate.opsForValue().get(key);
        return userVerCode.equals(verCodeCache);
    }

    private String getSecKillPathKey(MallClientUser clientUser, String activityCode, Long skuId) {
        return RedisKeyConstant.SECKILL_PATH_PREFIX + clientUser.getMemberId() + RedisKeyConstant.SPLIT_KEY + activityCode + RedisKeyConstant.SPLIT_KEY + skuId;
    }

    private String getSecKillVerCodeKey(MallClientUser clientUser, String activityCode, Long skuId) {
        return RedisKeyConstant.SECKILL_CODE_PREFIX + clientUser.getMemberId() + RedisKeyConstant.SPLIT_KEY + activityCode + RedisKeyConstant.SPLIT_KEY + skuId;
    }

}
