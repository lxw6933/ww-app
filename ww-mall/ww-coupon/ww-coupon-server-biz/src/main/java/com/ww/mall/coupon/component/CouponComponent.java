package com.ww.mall.coupon.component;

import com.mongodb.client.result.UpdateResult;
import com.ww.app.common.exception.ApiException;
import com.ww.app.mongodb.common.BaseDoc;
import com.ww.mall.coupon.component.key.CouponRedisKeyBuilder;
import com.ww.mall.coupon.entity.SmsCouponRecord;
import com.ww.mall.coupon.eunms.ErrorCodeConstants;
import com.ww.mall.coupon.eunms.CouponStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2025-03-12- 15:49
 * @description: 优惠券组件
 */
@Slf4j
@Component
public class CouponComponent {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CouponRedisKeyBuilder couponRedisKeyBuilder;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private SmsCouponStatisticsComponent smsCouponStatisticsComponent;

    /**
     * 冻结优惠券
     *
     * @param userId         用户id
     * @param couponRecordId 用户优惠券id
     */
    public void freezeMemberCoupon(Long userId, String couponRecordId) {
        String freezeKey = couponRedisKeyBuilder.buildCouponFreezeKey(couponRecordId);
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(freezeKey, Boolean.TRUE.toString(), 1, TimeUnit.HOURS);
        if (Boolean.TRUE.equals(result)) {
            log.info("冻结用户[{}]优惠券[{}]成功", userId, couponRecordId);
        } else {
            log.error("用户[{}]优惠券[{}]已经被占用", userId, couponRecordId);
            throw new ApiException(ErrorCodeConstants.COUPON_USED_EXCEPTION);
        }
    }

    /**
     * 判断下单优惠券是否被冻结
     *
     * @param couponRecordId 用户优惠券id
     */
    public boolean isFreezeCoupon(String couponRecordId) {
        String freezeKey = couponRedisKeyBuilder.buildCouponFreezeKey(couponRecordId);
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(freezeKey));
    }

    /**
     * 解冻优惠券redis
     *
     * @param couponRecordId 用户优惠券id
     */
    public void unFreezeMemberCoupon(String couponRecordId) {
        String freezeKey = couponRedisKeyBuilder.buildCouponFreezeKey(couponRecordId);
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(freezeKey))) {
            log.info("优惠券[{}]无需解冻", couponRecordId);
            return;
        }
        Boolean result = stringRedisTemplate.delete(freezeKey);
        if (Boolean.TRUE.equals(result)) {
            log.info("解冻优惠券[{}]成功", couponRecordId);
        } else {
            log.error("解冻优惠券[{}]失败", couponRecordId);
        }
    }

    /**
     * 更新优惠券状态
     *
     * @param channelId 渠道id
     * @param couponRecordId 优惠券id
     * @param status 状态
     */
    public boolean updateMemberCouponStatus(Long channelId, String couponRecordId, CouponStatus status) {
        UpdateResult updateResult = mongoTemplate.updateFirst(BaseDoc.buildIdQuery(couponRecordId), SmsCouponRecord.buildStatusUpdate(status), SmsCouponRecord.class, SmsCouponRecord.buildCollectionName(channelId));
        if (updateResult.getModifiedCount() == 1) {
            log.info("优惠券[{}]状态更新[{}]状态成功", couponRecordId, status);
            return true;
        } else {
            log.error("优惠券[{}]状态更新[{}]状态失败", couponRecordId, status);
            return false;
        }
    }

    /**
     * 取消订单回滚优惠券状态
     *
     * @param channelId 渠道id
     * @param couponRecordId 优惠券id
     */
    public void updateMemberCouponCancelStatus(Long channelId, String couponRecordId) {
        updateMemberCouponRollbackStatus(channelId, couponRecordId, false);
        // 清除redis数据
        unFreezeMemberCoupon(couponRecordId);
    }

    /**
     * 支付更新优惠券状态
     *
     * @param channelId 渠道id
     * @param couponRecordId 优惠券id
     */
    public void updateMemberCouponUseStatus(Long channelId, String couponRecordId) {
        if (updateMemberCouponStatus(channelId, couponRecordId, CouponStatus.USED)) {
            SmsCouponRecord couponRecord = mongoTemplate.findOne(BaseDoc.buildIdQuery(couponRecordId), SmsCouponRecord.class, SmsCouponRecord.buildCollectionName(channelId));
            smsCouponStatisticsComponent.statisticsCouponUse(couponRecord.getActivityCode());
            // 清除redis数据
            unFreezeMemberCoupon(couponRecordId);
        }
    }

    /**
     * 售后回滚优惠券状态
     *
     * @param channelId 渠道id
     * @param couponRecordId 优惠券id
     * @param isAfterSale 是否售后
     */
    public void updateMemberCouponRollbackStatus(Long channelId, String couponRecordId, boolean isAfterSale) {
        if (updateMemberCouponStatus(channelId, couponRecordId, CouponStatus.IN_EFFECT)) {
            if (isAfterSale) {
                SmsCouponRecord couponRecord = mongoTemplate.findOne(BaseDoc.buildIdQuery(couponRecordId), SmsCouponRecord.class, SmsCouponRecord.buildCollectionName(channelId));
                smsCouponStatisticsComponent.statisticsCouponRollback(couponRecord.getActivityCode());
            }
        }
    }

}
