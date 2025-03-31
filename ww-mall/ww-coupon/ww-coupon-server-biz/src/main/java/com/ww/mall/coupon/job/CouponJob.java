package com.ww.mall.coupon.job;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.ww.app.redis.AppRedisTemplate;
import com.ww.mall.coupon.component.key.CouponRedisKeyBuilder;
import com.ww.mall.coupon.entity.MerchantCouponActivity;
import com.ww.mall.coupon.entity.SmsCouponActivity;
import com.ww.mall.coupon.entity.base.BaseCouponInfo;
import com.ww.mall.coupon.eunms.CouponType;
import com.ww.mall.coupon.utils.CouponUtils;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author ww
 * @create 2025-03-31- 09:17
 * @description: 优惠券定时任务
 */
@Slf4j
@Component
public class CouponJob {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private AppRedisTemplate appRedisTemplate;

    @Resource
    private CouponRedisKeyBuilder couponRedisKeyBuilder;

    @XxlJob("ExpireCouponRedisDataHandleJobHandler")
    public void expireCouponRedisDataHandleJobHandler() {
        log.info("优惠券过期redis数据处理任务开始");
        List<String> removeKeyList = new ArrayList<>();
        String numberPrefixKey = couponRedisKeyBuilder.buildCouponNumberPrefixKey();
        Set<String> numberKeys = appRedisTemplate.scanKeys(numberPrefixKey + "*");
        if (CollectionUtil.isNotEmpty(numberKeys)) {
            numberKeys.forEach(numberKey -> {
                String activityCode = couponRedisKeyBuilder.extractActivityCode(numberKey, numberPrefixKey);
                handleExpireCouponActivity(numberKey, activityCode, removeKeyList);
            });
        }
        String codePrefixKey = couponRedisKeyBuilder.buildCouponCodePrefixKey();
        Set<String> codeKeys = appRedisTemplate.scanKeys(codePrefixKey + "*");
        if (CollectionUtil.isNotEmpty(codeKeys)) {
            codeKeys.forEach(codeKey -> {
                String activityCode = couponRedisKeyBuilder.extractActivityCode(codeKey, codePrefixKey);
                handleExpireCouponActivity(codeKey, activityCode, removeKeyList);
            });
        }
        if (!removeKeyList.isEmpty()) {
            log.info("活动已过期需要清理的redisKey:[{}]", removeKeyList);
            appRedisTemplate.batchRemoveKeys(removeKeyList, true);
        }
        log.info("优惠券过期redis数据处理任务结束");
    }

    private void handleExpireCouponActivity(String key, String activityCode, List<String> removeKeyList) {
        if (StrUtil.isBlank(activityCode)) {
            return;
        }
        Date now = new Date();
        // 查询活动是否过期
        CouponType couponType = CouponUtils.getCouponType(activityCode);
        switch (couponType) {
            case PLATFORM:
                SmsCouponActivity smsCouponActivity = mongoTemplate.findOne(BaseCouponInfo.buildActivityCodeQuery(activityCode), SmsCouponActivity.class);
                if (smsCouponActivity != null && smsCouponActivity.getReceiveEndTime().before(now)) {
                    log.info("平台优惠券活动[{}]已过期", activityCode);
                    removeKeyList.add(key);
                }
                break;
            case MERCHANT:
                MerchantCouponActivity merchantCouponActivity = mongoTemplate.findOne(BaseCouponInfo.buildActivityCodeQuery(activityCode), MerchantCouponActivity.class);
                if (merchantCouponActivity != null && merchantCouponActivity.getReceiveEndTime().before(now)) {
                    log.info("商家优惠券活动[{}]已过期", activityCode);
                    removeKeyList.add(key);
                }
                break;
            default:
        }
    }

}
