package com.ww.mall.coupon.component;

import com.mongodb.client.result.UpdateResult;
import com.ww.app.common.exception.ApiException;
import com.ww.app.mongodb.common.BaseDoc;
import com.ww.mall.coupon.component.key.CouponRedisKeyBuilder;
import com.ww.mall.coupon.entity.SmsCouponRecord;
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

    /**
     * 冻结优惠券
     *
     * @param userId         用户id
     * @param couponRecordId 用户优惠券id
     */
    public void freezeMemberCoupon(Long userId, String couponRecordId) {
        String freezeKey = couponRedisKeyBuilder.buildCouponFreezeKey(couponRecordId);
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(freezeKey, Boolean.TRUE.toString(), 1, TimeUnit.DAYS);
        if (Boolean.TRUE.equals(result)) {
            log.info("冻结用户[{}]优惠券[{}]成功", userId, couponRecordId);
        } else {
            log.error("用户[{}]优惠券[{}]已经被占用", userId, couponRecordId);
            throw new ApiException("优惠券已被占用");
        }
    }

    /**
     * 判断下单优惠券是否被冻结
     *
     * @param couponRecordId 用户优惠券id
     */
    public void isFreezeCoupon(String couponRecordId) {
        String freezeKey = couponRedisKeyBuilder.buildCouponFreezeKey(couponRecordId);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(freezeKey))) {
            throw new ApiException("优惠券已被占用");
        }
    }

    /**
     * 解冻优惠券
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
     * 支付更新优惠券状态
     *
     * @param couponRecordId 优惠券id
     */
    public void updateMemberCouponUseStatus(String couponRecordId) {
        UpdateResult updateResult = mongoTemplate.updateFirst(BaseDoc.buildIdQuery(couponRecordId), SmsCouponRecord.buildStatusUpdate(CouponStatus.USED), SmsCouponRecord.class);
        if (updateResult.getModifiedCount() == 1) {
            log.info("优惠券[{}]状态更新已使用状态成功", couponRecordId);
        } else {
            log.error("优惠券[{}]状态更新已使用状态失败", couponRecordId);
        }
    }

    /**
     * 售后回滚优惠券状态
     *
     * @param couponRecordId 优惠券id
     */
    public void updateMemberCouponRollbackStatus(String couponRecordId) {
        UpdateResult updateResult = mongoTemplate.updateFirst(BaseDoc.buildIdQuery(couponRecordId), SmsCouponRecord.buildStatusUpdate(CouponStatus.IN_EFFECT), SmsCouponRecord.class);
        if (updateResult.getModifiedCount() == 1) {
            log.info("回滚优惠券[{}]状态成功", couponRecordId);
        } else {
            log.error("回滚优惠券[{}]状态失败", couponRecordId);
        }
    }

}
