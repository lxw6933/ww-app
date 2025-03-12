package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.eunms.EffectTimeType;
import com.ww.mall.coupon.view.vo.base.BaseCouponInfoVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author ww
 * @create 2025-03-12- 10:56
 * @description: 商品详情优惠券活动信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductCouponActivityVO extends BaseCouponInfoVO {

    /**
     * 优惠券领取后有效期计算类型
     * [固定有效期]
     * [根据领取时间计算]
     */
    private EffectTimeType effectTimeType;

    /**
     * 优惠券有效开始时间【固定有效期】
     */
    private Date useStartTime;

    /**
     * 优惠券有效结束时间【固定有效期】
     */
    private Date useEndTime;

    /**
     * 领取多少天后生效【根据领取时间计算】
     */
    private int receiveAfterEffectDay;

    /**
     * 多少天的有效期【根据领取时间计算】
     */
    private int effectDay;

}
