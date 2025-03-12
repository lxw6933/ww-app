package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.eunms.ApplyProductRangeType;
import com.ww.mall.coupon.eunms.CouponStatus;
import com.ww.mall.coupon.eunms.CouponType;
import com.ww.mall.coupon.view.vo.base.BaseCouponInfoVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2025-03-11- 13:57
 * @description: 用户个人中心优惠券中心信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberCouponCenterVO extends BaseCouponInfoVO {

    /**
     * 优惠券类型【店铺、渠道】
     */
    private CouponType couponType;

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

    /**
     * 适用范围
     */
    private ApplyProductRangeType applyProductRangeType;

    /**
     * 适用范围id集合
     */
    private List<Long> idList;

}
