package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.entity.SmsCouponActivity;
import com.ww.mall.coupon.eunms.*;
import com.ww.mall.coupon.view.vo.base.BaseCouponInfoVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2023-07-26- 09:16
 * @description: 平台优惠券信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SmsCouponDetailVO extends BaseCouponInfoVO {

    /**
     * 开始领取时间
     */
    private Date receiveStartTime;

    /**
     * 结束领取时间
     */
    private Date receiveEndTime;

    /**
     * 优惠券领取后有效期计算类型
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
     * 领取多少天后可用【根据领取时间计算】
     */
    private int receiveDay;

    /**
     * 有效单位【天，分】【根据领取时间计算】
     */
    private CouponConstant.EffectTimeUnit effectTimeUnit;

    /**
     * 有效期【根据领取时间计算】
     */
    private int effectNumber;

    /**
     * 发放类型
     */
    private IssueType issueType;

    /**
     * 适用用户范围
     */
    private ApplyMemberType applyMemberType;

    /**
     * 适用范围
     */
    private ApplyProductRangeType applyProductRangeType;

    /**
     * 适用范围id集合
     */
    private List<Long> idList;

    /**
     * 领取限制类型
     */
    private LimitReceiveTimeType limitReceiveTimeType;

    /**
     * 领取限制数量
     */
    private int limitReceiveNumber;

    /**
     * 优惠券数量
     */
    private Integer number;

    /**
     * 将SmsCouponActivity对象转换为SmsCouponDetailVO对象
     * 
     * @param smsCouponActivity 优惠券活动
     * @return SmsCouponDetailVO
     */
    public static SmsCouponDetailVO convertFrom(SmsCouponActivity smsCouponActivity) {
        SmsCouponDetailVO vo = new SmsCouponDetailVO();
        // 从BaseCouponInfoVO继承的属性
        vo.setId(smsCouponActivity.getId());
        vo.setActivityCode(smsCouponActivity.getActivityCode());
        vo.setName(smsCouponActivity.getName());
        vo.setDesc(smsCouponActivity.getDesc());
        vo.setCouponDiscountType(smsCouponActivity.getCouponDiscountType());
        vo.setAchieveAmount(smsCouponActivity.getAchieveAmount());
        vo.setDeductionAmount(smsCouponActivity.getDeductionAmount());
        
        // 本类属性
        vo.setReceiveStartTime(smsCouponActivity.getReceiveStartTime());
        vo.setReceiveEndTime(smsCouponActivity.getReceiveEndTime());
        vo.setEffectTimeType(smsCouponActivity.getEffectTimeType());
        vo.setUseStartTime(smsCouponActivity.getUseStartTime());
        vo.setUseEndTime(smsCouponActivity.getUseEndTime());
        vo.setReceiveDay(smsCouponActivity.getReceiveDay());
        vo.setEffectTimeUnit(smsCouponActivity.getEffectTimeUnit());
        vo.setEffectNumber(smsCouponActivity.getEffectNumber());
        vo.setIssueType(smsCouponActivity.getIssueType());
        vo.setApplyMemberType(smsCouponActivity.getApplyMemberType());
        vo.setApplyProductRangeType(smsCouponActivity.getApplyProductRangeType());
        vo.setIdList(smsCouponActivity.getIdList());
        vo.setLimitReceiveTimeType(smsCouponActivity.getLimitReceiveTimeType());
        vo.setLimitReceiveNumber(smsCouponActivity.getLimitReceiveNumber());
        vo.setNumber(smsCouponActivity.getNumber());
        
        return vo;
    }
}
