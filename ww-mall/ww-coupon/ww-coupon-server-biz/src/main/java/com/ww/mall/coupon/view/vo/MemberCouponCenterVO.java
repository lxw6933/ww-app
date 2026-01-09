package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.entity.SmsCouponRecord;
import com.ww.mall.coupon.enums.ApplyProductRangeType;
import com.ww.mall.coupon.enums.CouponStatus;
import com.ww.mall.coupon.enums.CouponType;
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
    
    /**
     * 将SmsCouponRecord对象转换为MemberCouponCenterVO对象
     * 
     * @param smsCouponRecord 用户优惠券记录
     * @return MemberCouponCenterVO
     */
    public static MemberCouponCenterVO convertFrom(SmsCouponRecord smsCouponRecord) {
        MemberCouponCenterVO vo = new MemberCouponCenterVO();
        // 从BaseCouponInfoVO继承的属性
        vo.setId(smsCouponRecord.getId());
        vo.setActivityCode(smsCouponRecord.getActivityCode());
        vo.setCouponDiscountType(smsCouponRecord.getCouponDiscountType());
        vo.setAchieveAmount(smsCouponRecord.getAchieveAmount());
        vo.setDeductionAmount(smsCouponRecord.getDeductionAmount());
        
        // 本类属性
        vo.setCouponType(smsCouponRecord.getCouponType());
        vo.setUseStartTime(smsCouponRecord.getUseStartTime());
        vo.setUseEndTime(smsCouponRecord.getUseEndTime());
        vo.setCouponStatus(smsCouponRecord.getCouponStatus());
        // 适用范围和适用范围id集合需要从活动中获取，这里先不设置
        
        return vo;
    }
}
