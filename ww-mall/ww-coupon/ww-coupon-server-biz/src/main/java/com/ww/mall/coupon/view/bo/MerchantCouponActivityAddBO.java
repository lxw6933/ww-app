package com.ww.mall.coupon.view.bo;

import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.entity.MerchantCouponActivity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author ww
 * @create 2025-03-05- 15:07
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantCouponActivityAddBO extends CouponActivityBaseAddBO {

    @NotNull(message = "商户ID不能为空")
    private Long merchantId;

    @NotNull(message = "分发渠道不能为空")
    private List<Long> channelIds;

    public MerchantCouponActivity convertMerchantCouponActivity() {
        MerchantCouponActivity activity = new MerchantCouponActivity();
        initCouponActivity(activity);
        activity.setMerchantId(this.getMerchantId());
        activity.setChannelIds(this.getChannelIds());
        activity.setAuditStatus(CouponConstant.AuditStatus.WAIT_AUDIT);
        return activity;
    }

}
