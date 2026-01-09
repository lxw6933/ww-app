package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.constant.CouponConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author ww
 * @create 2023-07-26- 09:16
 * @description: 平台优惠券信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantCouponDetailVO extends BaseCouponDetailVO {

    /**
     * 分发渠道
     */
    private List<Long> channelIds;

    /**
     * 审核状态
     */
    private CouponConstant.AuditStatus auditStatus;

}
