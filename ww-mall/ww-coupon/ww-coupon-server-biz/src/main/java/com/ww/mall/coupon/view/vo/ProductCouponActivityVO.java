package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.entity.SmsCouponActivity;
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
    private int receiveDay;

    /**
     * 有效单位【天，分】【根据领取时间计算】
     */
    private CouponConstant.EffectTimeUnit effectTimeUnit;

    /**
     * 多少天的有效期【根据领取时间计算】
     */
    private int effectNumber;
    
    /**
     * 将SmsCouponActivity对象转换为ProductCouponActivityVO对象
     * 
     * @param smsCouponActivity 优惠券活动
     * @return ProductCouponActivityVO
     */
    public static ProductCouponActivityVO convertFrom(SmsCouponActivity smsCouponActivity) {
        ProductCouponActivityVO vo = new ProductCouponActivityVO();
        // 从BaseCouponInfoVO继承的属性
        vo.setId(smsCouponActivity.getId());
        vo.setActivityCode(smsCouponActivity.getActivityCode());
        vo.setName(smsCouponActivity.getName());
        vo.setDesc(smsCouponActivity.getDesc());
        vo.setCouponDiscountType(smsCouponActivity.getCouponDiscountType());
        vo.setAchieveAmount(smsCouponActivity.getAchieveAmount());
        vo.setDeductionAmount(smsCouponActivity.getDeductionAmount());
        
        // 本类属性
        vo.setEffectTimeType(smsCouponActivity.getEffectTimeType());
        vo.setUseStartTime(smsCouponActivity.getUseStartTime());
        vo.setUseEndTime(smsCouponActivity.getUseEndTime());
        vo.setReceiveDay(smsCouponActivity.getReceiveDay());
        vo.setEffectTimeUnit(smsCouponActivity.getEffectTimeUnit());
        vo.setEffectNumber(smsCouponActivity.getEffectNumber());
        
        return vo;
    }
}
