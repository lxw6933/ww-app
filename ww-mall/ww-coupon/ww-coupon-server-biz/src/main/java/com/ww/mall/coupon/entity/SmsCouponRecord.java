package com.ww.mall.coupon.entity;

import cn.hutool.core.date.DateUtil;
import com.ww.app.mongodb.common.BaseDoc;
import com.ww.mall.coupon.eunms.CouponDiscountType;
import com.ww.mall.coupon.eunms.CouponStatus;
import com.ww.mall.coupon.eunms.CouponType;
import com.ww.mall.coupon.eunms.LimitReceiveTimeType;
import com.ww.mall.coupon.utils.CouponUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author ww
 * @create 2023-07-25- 09:54
 * @description: 用户领取优惠券记录
 */
@Data
@Document
@EqualsAndHashCode(callSuper = true)
public class SmsCouponRecord extends BaseDoc {

    /**
     * 用户id
     */
    private Long memberId;

    /**
     * 渠道id
     */
    private Long channelId;

    /**
     * 优惠券券码【手动发放才有】
     */
    private String couponCode;

    /**
     * 优惠券唯一活动编码
     */
    private String activityCode;

    /**
     * 优惠券类型【店铺、渠道】
     */
    private CouponType couponType;

    /**
     * 优惠券优惠类型【满减券、代金券、满折券】
     */
    private CouponDiscountType couponDiscountType;

    /**
     * 优惠券需满X金额
     */
    private BigDecimal achieveAmount;

    /**
     * 优惠券扣减金额【折扣】
     */
    private BigDecimal deductionAmount;

    /**
     * 开始使用时间
     */
    private Date useStartTime;

    /**
     * 过期时间
     */
    private Date useEndTime;

    /**
     * 优惠券券码状态
     */
    private CouponStatus couponStatus;

    public static String buildCollectionName(Long channelId) {
        return CouponUtils.getSmsCouponRecordCollectionName(channelId);
    }

    public static Query buildMemberReceiveRecordQuery(Long memberId, String activityCode, LimitReceiveTimeType limitReceiveTimeType) {
        Query query = new Query().addCriteria(Criteria.where("activityCode").is(activityCode).and("memberId").is(memberId));
        Date now;
        switch (limitReceiveTimeType) {
            case DAY:
                now = new Date();
                Date dayBeginDay = DateUtil.beginOfDay(now);
                Date dayEndDay = DateUtil.endOfDay(now);
                query.addCriteria(Criteria.where("createTime").gte(dayBeginDay).lte(dayEndDay));
                break;
            case WEEK:
                now = new Date();
                Date weekBeginDay = DateUtil.beginOfWeek(now);
                Date weekEndDay = DateUtil.endOfWeek(now);
                query.addCriteria(Criteria.where("createTime").gte(weekBeginDay).lte(weekEndDay));
                break;
            case MONTH:
                now = new Date();
                Date monthBeginDay = DateUtil.beginOfMonth(now);
                Date monthEndDay = DateUtil.endOfMonth(now);
                query.addCriteria(Criteria.where("createTime").gte(monthBeginDay).lte(monthEndDay));
                break;
            case FOREVER:
            default:
        }
        return query;
    }

    public static Query buildMemberEffectCouponRecordQuery(Long memberId) {
        return new Query().addCriteria(
                Criteria.where("memberId").is(memberId).orOperator(
                                Criteria.where("couponStatus").is(CouponStatus.TO_TAKE_EFFECT),
                                Criteria.where("couponStatus").is(CouponStatus.IN_EFFECT)
                        ));
    }

    public static Query buildStatusQuery(String activityCode, CouponStatus couponStatus) {
        return new Query().addCriteria(Criteria.where("activityCode").is(activityCode).and("couponStatus").is(couponStatus));
    }

    public static Query buildCodeQuery(String couponCode) {
        return new Query().addCriteria(Criteria.where("couponCode").is(couponCode));
    }

    public static Update buildStatusUpdate(CouponStatus couponStatus) {
        return new Update().set("couponStatus", couponStatus);
    }

}
