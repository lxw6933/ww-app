package com.ww.mall.coupon.job;

import com.ww.mall.coupon.service.SmsCouponService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2025-03-31- 09:17
 * @description: 优惠券定时任务
 */
@Slf4j
@Component
public class CouponJob {

    @Resource
    private SmsCouponService smsCouponService;

    @XxlJob("ExpireCouponRedisDataHandleJobHandler")
    public void expireCouponRedisDataHandleJobHandler() {
        log.info("优惠券过期redis数据处理任务开始");
        smsCouponService.expireActivityRedisDataHandle();
        log.info("优惠券过期redis数据处理任务结束");
    }

}
