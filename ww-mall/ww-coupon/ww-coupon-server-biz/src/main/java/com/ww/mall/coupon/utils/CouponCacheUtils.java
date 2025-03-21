package com.ww.mall.coupon.utils;

import cn.hutool.extra.spring.SpringUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.ww.app.common.utils.CaffeineUtil;
import com.ww.mall.coupon.entity.SmsCouponActivity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2025-03-21- 16:51
 * @description:
 */
@Slf4j
public class CouponCacheUtils {

    private CouponCacheUtils() {}

    private static MongoTemplate mongoTemplate;

    private static MongoTemplate getMongoTemplate() {
        if (mongoTemplate == null) {
            log.info("初始化MongodbTemplate引用");
            mongoTemplate = SpringUtil.getBean(MongoTemplate.class);
        }
        return mongoTemplate;
    }

    private static final Cache<String, SmsCouponActivity> smsCouponActivityCache = CaffeineUtil.initCaffeine(100, 200, 30, TimeUnit.MINUTES);

    public static SmsCouponActivity getSmsCouponActivityCache(String activityCode) {
        return smsCouponActivityCache.get(activityCode, code -> getMongoTemplate().findOne(SmsCouponActivity.buildActivityCodeQuery(code), SmsCouponActivity.class));
    }

}
