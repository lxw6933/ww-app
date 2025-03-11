package com.ww.mall.coupon.component;

import com.mongodb.client.result.UpdateResult;
import com.ww.mall.coupon.entity.MerchantCouponActivity;
import com.ww.mall.coupon.entity.SmsCouponActivity;
import com.ww.mall.coupon.entity.base.BaseCouponInfo;
import com.ww.mall.coupon.utils.CouponUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author ww
 * @create 2025-03-10 14:37
 * @description: 优惠券领取统计
 */
@Slf4j
@Component
public class SmsCouponStatisticsComponent {

    // 本地领取统计
    @Getter
    private final Map<String, LongAdder> statisticsReceiveMap = new ConcurrentHashMap<>();
    // 本地使用统计
    @Getter
    private final Map<String, LongAdder> statisticsUseMap = new ConcurrentHashMap<>();

    private final ScheduledExecutorService statisticsDataSyncScheduler = Executors.newScheduledThreadPool(1);

    @Resource
    private MongoTemplate mongoTemplate;

    @PostConstruct
    public void init() {
        // 开启定时任务
        statisticsDataSyncScheduler.scheduleAtFixedRate(this::syncStatisticsDataToDB, 0, 30, TimeUnit.MINUTES);
        // 新增停服钩子
        Runtime.getRuntime().addShutdownHook(new Thread(this::syncStatisticsDataToDB));
    }

    /**
     * 统计领取数据
     *
     * @param activityCode 活动编码
     */
    public void statisticsCouponReceive(String activityCode) {
        statisticsReceiveMap.computeIfAbsent(activityCode, k -> new LongAdder()).add(1);
    }

    /**
     * 统计使用数据
     *
     * @param activityCode 活动编码
     */
    public void statisticsCouponUse(String activityCode) {
        statisticsUseMap.computeIfAbsent(activityCode, k -> new LongAdder()).add(1);
    }

    /**
     * 统计回滚数据
     *
     * @param activityCode 活动编码
     */
    public void statisticsCouponRollback(String activityCode) {
        statisticsUseMap.computeIfAbsent(activityCode, k -> new LongAdder()).decrement();
    }

    /**
     * 将本地优惠券领取统计数据同步到DB
     */
    private void syncStatisticsDataToDB() {
        statisticsReceiveMap.forEach((key, longAddr) -> {
            int statisticsNumber = (int) longAddr.sumThenReset();
            log.info("[{}]优惠券领取数据同步到DB, 领取数量[{}] 开始同步", key, statisticsNumber);
            UpdateResult updateResult = null;
            switch (CouponUtils.getCouponType(key)) {
                case MERCHANT:
                    updateResult = mongoTemplate.updateFirst(BaseCouponInfo.buildActivityCodeQuery(key), BaseCouponInfo.buildActivityReceiveNumberUpdate(statisticsNumber), MerchantCouponActivity.class);
                    break;
                case PLATFORM:
                    updateResult = mongoTemplate.updateFirst(BaseCouponInfo.buildActivityCodeQuery(key), BaseCouponInfo.buildActivityReceiveNumberUpdate(statisticsNumber), SmsCouponActivity.class);
                    break;
                default:
            }
            log.info("[{}]优惠券领取数据同步到DB, 领取数量[{}] 同步结果[{}]", key, statisticsNumber, updateResult.getModifiedCount() == 1);
        });
        statisticsUseMap.forEach((key, longAddr) -> {
            int statisticsNumber = (int) longAddr.sumThenReset();
            log.info("[{}]优惠券使用数据同步到DB, 使用数量[{}] 开始同步", key, statisticsNumber);
            UpdateResult updateResult = null;
            switch (CouponUtils.getCouponType(key)) {
                case MERCHANT:
                    updateResult = mongoTemplate.updateFirst(BaseCouponInfo.buildActivityCodeQuery(key), BaseCouponInfo.buildActivityUseNumberUpdate(statisticsNumber), MerchantCouponActivity.class);
                    break;
                case PLATFORM:
                    updateResult = mongoTemplate.updateFirst(BaseCouponInfo.buildActivityCodeQuery(key), BaseCouponInfo.buildActivityUseNumberUpdate(statisticsNumber), SmsCouponActivity.class);
                    break;
                default:
            }
            log.info("[{}]优惠券使用数据同步到DB, 使用数量[{}] 同步结果[{}]", key, statisticsNumber, updateResult.getModifiedCount() == 1);
        });
    }

    @PreDestroy
    public void destroy() {
        statisticsDataSyncScheduler.shutdown();
        syncStatisticsDataToDB();
    }

}
