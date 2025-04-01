package com.ww.app.seckill.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.digest.MD5;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wf.captcha.ArithmeticCaptcha;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.utils.IdUtil;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.app.rabbitmq.exchange.ExchangeConstant;
import com.ww.app.rabbitmq.routekey.RouteKeyConstant;
import com.ww.app.redis.component.StockRedisComponent;
import com.ww.app.redis.component.key.StockRedisKeyBuilder;
import com.ww.app.seckill.component.key.SeckillRedisKeyBuilder;
import com.ww.app.seckill.service.SeckillService;
import com.ww.app.seckill.view.bo.SecKillOrderReqBO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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

    @Resource
    private RabbitMqPublisher rabbitMqPublisher;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private StockRedisComponent stockRedisComponent;

    @Resource
    private StockRedisKeyBuilder stockRedisKeyBuilder;

    @Resource
    private SeckillRedisKeyBuilder seckillRedisKeyBuilder;

    @PostConstruct
    public void init() {
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
        ClientUser clientUser = AuthorizationContext.getClientUser();
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
        String key = seckillRedisKeyBuilder.buildSeckillCodeKey(activityCode, clientUser.getId(), skuId);
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
        ClientUser clientUser = AuthorizationContext.getClientUser();

        String key = seckillRedisKeyBuilder.buildSeckillPathKey(activityCode, clientUser.getId(), skuId);
        String userSecKillPath = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotEmpty(userSecKillPath)) {
            return userSecKillPath;
        }
        // 生成secKillPath
        userSecKillPath = MD5.create().digestHex(UUID.randomUUID() + activityCode + clientUser.getId().toString(), StandardCharsets.UTF_8.name());
        // 加密后地址存入redis
        redisTemplate.opsForValue().set(key, userSecKillPath, 1, TimeUnit.MINUTES);
        return userSecKillPath;
    }

    @Override
    public Boolean doSecKillOrder(String userSecKillPath, SecKillOrderReqBO reqBO) {
        // 校验用户是否存在秒杀资格
        ClientUser clientUser = AuthorizationContext.getClientUser();
        // 校验地址是否正确
        Assert.isFalse(checkSecKillPath(clientUser, userSecKillPath, reqBO.getActivityCode(), reqBO.getSkuId()), () -> new ApiException("秒杀路径异常"));
        // 校验图形验证码是否正确
        Assert.isFalse(checkSecKillVerCode(clientUser, reqBO.getActivityCode(), reqBO.getSkuId(), reqBO.getCaptcha()), () -> new ApiException("验证码错误"));
        // 本地缓存存储活动信息，校验活动信息

        // 本地缓存商品信息，校验商品信息
        String skuStockRedisKey = stockRedisKeyBuilder.buildStockKey(reqBO.getActivityCode(), null, null, reqBO.getSkuId());
        if (stockRedisComponent.decrementStock(skuStockRedisKey, 1)) {
            String orderDate = DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN);
            String orderNo = IdUtil.nextIdStr();
            rabbitMqPublisher.sendMsg(ExchangeConstant.OMS_EXCHANGE, RouteKeyConstant.CREATE_ORDER_KEY, orderNo);
            log.info("订单[{}]下单成功[{}]", orderNo, orderDate);
            return true;
        } else {
            // TODO 标记秒杀结束，无库存
            return false;
        }
    }

    private boolean checkSecKillPath(ClientUser clientUser, String userSecKillPath, String activityCode, Long skuId) {
        if (StringUtils.isEmpty(userSecKillPath)) {
            return false;
        }
        String key = seckillRedisKeyBuilder.buildSeckillPathKey(activityCode, clientUser.getId(), skuId);
        String userSecKillPathCache = redisTemplate.opsForValue().get(key);
        return userSecKillPath.equals(userSecKillPathCache);
    }

    private boolean checkSecKillVerCode(ClientUser clientUser, String activityCode, Long skuId, String userVerCode) {
        String key = seckillRedisKeyBuilder.buildSeckillCodeKey(activityCode, clientUser.getId(), skuId);
        String verCodeCache = redisTemplate.opsForValue().get(key);
        return userVerCode.equals(verCodeCache);
    }

}
