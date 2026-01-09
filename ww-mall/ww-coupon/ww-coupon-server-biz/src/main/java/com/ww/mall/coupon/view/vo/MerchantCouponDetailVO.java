package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.constant.CouponConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author ww
 * @create 2023-07-26- 09:16
 * @description: 商家优惠券活动详情
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商家优惠券活动详情")
public class MerchantCouponDetailVO extends BaseCouponDetailVO {

    /**
     * 分发渠道
     */
    @Schema(description = "分发渠道ID列表", example = "[1, 2, 3]")
    private List<Long> channelIds;

    /**
     * 审核状态
     */
    @Schema(description = "审核状态", example = "WAIT_AUDIT", allowableValues = {"WAIT_AUDIT", "AUDIT_PASS", "AUDIT_NOT_PASS"})
    private CouponConstant.AuditStatus auditStatus;

}
