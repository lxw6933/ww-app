package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.view.vo.base.BaseCouponInfoVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * @author ww
 * @create 2025-03-12- 11:09
 * @description: 确认下单用户优惠券vo
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderMemberCouponVO extends BaseCouponInfoVO {

    /**
     * 开始使用时间
     */
    private Date useStartTime;

    /**
     * 过期时间
     */
    private Date useEndTime;

    /**
     * 最高优惠总金额/积分
     */
    private BigDecimal discountTotalAmount = BigDecimal.ZERO;

    /**
     * 还差多少金额或者积分可用优惠券
     */
    private BigDecimal lackAmount = BigDecimal.ZERO;

    /**
     * 不可用原因
     */
    private CouponConstant.Disabled disabled;

    /**
     * 商品均摊金额
     */
    private Map<Long, BigDecimal> allocateResultMap;
    
    /**
     * 将MemberCouponCenterVO对象转换为OrderMemberCouponVO对象
     * 
     * @param memberCouponCenterVO 用户优惠券中心VO
     * @return OrderMemberCouponVO
     */
    public static OrderMemberCouponVO convertFrom(MemberCouponCenterVO memberCouponCenterVO) {
        OrderMemberCouponVO vo = new OrderMemberCouponVO();
        // 从BaseCouponInfoVO继承的属性
        vo.setId(memberCouponCenterVO.getId());
        vo.setActivityCode(memberCouponCenterVO.getActivityCode());
        vo.setName(memberCouponCenterVO.getName());
        vo.setDesc(memberCouponCenterVO.getDesc());
        vo.setCouponDiscountType(memberCouponCenterVO.getCouponDiscountType());
        vo.setAchieveAmount(memberCouponCenterVO.getAchieveAmount());
        vo.setDeductionAmount(memberCouponCenterVO.getDeductionAmount());
        
        // 本类属性
        vo.setUseStartTime(memberCouponCenterVO.getUseStartTime());
        vo.setUseEndTime(memberCouponCenterVO.getUseEndTime());
        // discountTotalAmount默认为0
        // lackAmount默认为0
        // 其他属性由调用者设置
        
        return vo;
    }
}
