package com.ww.app.coupon.entity.mongo;

import com.ww.app.coupon.eunms.CouponDiscountType;
import com.ww.app.coupon.eunms.CouponStatus;
import com.ww.app.coupon.eunms.CouponType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

/**
 * @author ww
 * @create 2023-07-25- 09:54
 * @description: 用户领取优惠券记录
 */
@Data
@Document(collection = "t_member_coupon_record")
public class MemberCoupon {

    @Id
    private String id;

    /**
     * 用户id
     */
    private Long memberId;

    /**
     * 领取优惠券编码
     */
    private String couponTicketCode;

    /**
     * 优惠券唯一活动编码
     */
    private String activityCode;

    /**
     * 优惠券类型
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
    private String useStartTime;

    /**
     * 过期时间
     */
    private String useEndTime;

    /**
     * 优惠券状态
     */
    private CouponStatus couponStatus;

    /**
     * 用户领取时间
     */
    private String receiveTime;

    /**
     * 更新时间
     */
    private String updateTime;

}
