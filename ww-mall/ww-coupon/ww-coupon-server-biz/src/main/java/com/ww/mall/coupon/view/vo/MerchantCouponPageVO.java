package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.constant.CouponConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author ww
 * @create 2026-01-07 14:56
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantCouponPageVO extends SmsCouponPageVO {

    /**
     * 分发渠道
     */
    private List<Long> channelIds;

    /**
     * 审核状态
     */
    private CouponConstant.AuditStatus auditStatus;

}
