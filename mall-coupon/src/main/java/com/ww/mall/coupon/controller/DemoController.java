package com.ww.mall.coupon.controller;
import com.ww.mall.coupon.eunms.AllowProductRangeType;
import java.math.BigDecimal;
import com.ww.mall.coupon.eunms.CouponDiscountType;
import com.ww.mall.coupon.eunms.CouponUseTimeType;
import com.ww.mall.coupon.eunms.CouponLimitReceiveTimeType;
import com.ww.mall.coupon.eunms.AllowMemberRangeType;
import com.ww.mall.coupon.eunms.CouponType;
import java.util.Date;
import com.ww.mall.coupon.eunms.CouponDistributeType;

import com.ww.mall.common.exception.ApiException;
import com.ww.mall.coupon.config.CouponProperties;
import com.ww.mall.coupon.dao.CouponMapper;
import com.ww.mall.coupon.entity.Coupon;
import com.ww.mall.coupon.service.CouponService;
import com.ww.mall.coupon.view.bo.CouponPageBO;
import com.ww.mall.rabbitmq.MallPublisher;
import com.ww.mall.rabbitmq.exchange.ExchangeConstant;
import com.ww.mall.rabbitmq.routekey.RouteKeyConstant;
import com.ww.mall.web.config.SecretProperties;
import com.ww.mall.ip2region.Ip2regionSearcher;
import com.ww.mall.web.config.thread.DefaultThreadPoolProperties;
import com.ww.mall.web.utils.VerificationCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author:         ww
 * Datetime:       2021\3\4 0004
 * Description:
 */
@Slf4j
@RestController
@RequestMapping("/coupon")
public class DemoController {

    @Autowired
    private DefaultThreadPoolProperties defaultThreadPoolProperties;

    @Autowired
    private SecretProperties secretProperties;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CouponProperties couponProperties;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CouponService couponService;

    @Autowired
    private MallPublisher mallPublisher;

    @Autowired
    private CouponMapper couponMapper;

    @GetMapping("saveMysql")
    public void saveMysql() {
        Coupon coupon = new Coupon();
        coupon.setActivityCode("");
        coupon.setChannelId(0L);
        coupon.setMerchantId(0L);
        coupon.setTitle("");
        coupon.setCouponType(CouponType.PLATFORM);
        coupon.setCouponDiscountType(CouponDiscountType.FULL_REDUCTION);
        coupon.setAchieveAmount(new BigDecimal("0"));
        coupon.setDeductionAmount(new BigDecimal("0"));
        coupon.setReceiveStartTime(new Date());
        coupon.setReceiveEndTime(new Date());
        coupon.setCouponUseTimeType(CouponUseTimeType.FIXED);
        coupon.setUseStartTime(new Date());
        coupon.setUseEndTime(new Date());
        coupon.setReceiveAfterDayEffect(0);
        coupon.setReceiveAfterEffectDay(0);
        coupon.setInitTotalCouponNumber(0);
        coupon.setInitSuccess(false);
        coupon.setCouponLimitReceiveTimeType(CouponLimitReceiveTimeType.FOREVER);
        coupon.setCouponLimitReceiveNumber(0);
        coupon.setAllowMemberRangeType(AllowMemberRangeType.ALL);
        coupon.setCouponDistributeType(CouponDistributeType.RECEIVE);
        coupon.setAllowProductRangeType(AllowProductRangeType.ALL);
        coupon.setState(false);
        coupon.setRemark("");
        coupon.setVersion(0L);
        coupon.setCreatorId(0L);
        coupon.setUpdaterId(0L);
        coupon.setCreateTime(new Date());
        coupon.setUpdateTime(new Date());
        couponService.saveBatch(Collections.singletonList(coupon));
    }

    @GetMapping("/testMsg")
    public void testMsg(String msg) {
        mallPublisher.publishMsg(ExchangeConstant.MALL_COUPON_EXCHANGE, RouteKeyConstant.MALL_COUPON_TEST_KEY, msg);
    }

    private final AtomicInteger num = new AtomicInteger(0);

    @GetMapping("/lineLock")
    public void lineLock(@RequestParam("activityCode") String activityCode) {
        int total = num.getAndIncrement();
        if (total > 500000) {
            throw new ApiException("库存不足");
        }
//        RLock lock = redissonClient.getLock(activityCode);
//        try {
//            lock.lock(10, TimeUnit.SECONDS);
//
//        } catch (Exception e) {
//            throw new ApiException("服务异常");
//        } finally {
//            lock.unlock();
//        }


    }

    @GetMapping("/lock")
    public void getLock(CouponPageBO couponPageBO) {
        couponService.demo(couponPageBO);
    }

    @GetMapping("/demo/add/{id}")
    public void add(@PathVariable("id") String id) {
        stringRedisTemplate.opsForHyperLogLog().add("ww", id);
    }

    @GetMapping("/demo/size")
    public int add() {
        return stringRedisTemplate.opsForHyperLogLog().size("ww").intValue();
    }

    @RequestMapping("/demo")
    public String demo(HttpServletRequest request){
//        CompletableFuture.runAsync(() -> {
//            log.info("子线程打印");
//        }, defaultThreadPoolExecutor);
        log.info("main线程执行");
        return "coupon active is opening！！！" + couponProperties;
    }

    @RequestMapping("/test")
    public String test(){
        String code = VerificationCodeUtil.generateVerificationCode(4);
        String key = "test_redis_rate:" + code;
        stringRedisTemplate.opsForValue().set(key, code, 3, TimeUnit.MINUTES);
        return "success";
    }


}

