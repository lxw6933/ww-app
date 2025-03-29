package com.ww.mall.coupon.view.bo;

import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.eunms.CouponType;
import lombok.Data;

/**
 * @author ww
 * @create 2025-03-11- 14:07
 * @description:
 */
@Data
public class MemberCouponCenterBO {

    /**
     * 最后一条数据id的游标值
     */
    private String endIdCursorValue;

    /**
     * 状态
     */
    private CouponConstant.Status status;

    /**
     * 优惠券类型
     */
    private CouponType couponType;

    /**
     * 积分类型
     */
    private CouponConstant.Type type;

    /**
     * 展示数量
     */
    private Integer size;

    /**
     * activityCode
     */
    private String activityCode;

    public Integer getSize() {
        return this.size == null ? 10 : this.size;
    }
}
