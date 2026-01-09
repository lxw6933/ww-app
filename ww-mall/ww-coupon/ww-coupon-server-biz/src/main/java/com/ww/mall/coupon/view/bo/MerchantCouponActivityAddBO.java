package com.ww.mall.coupon.view.bo;

import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.entity.MerchantCouponActivity;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "商家优惠券活动新增参数")
public class MerchantCouponActivityAddBO extends CouponActivityBaseAddBO {

    @Schema(description = "商户ID", example = "1001")
    @NotNull(message = "商户ID不能为空")
    private Long merchantId;

    @Schema(description = "分发渠道ID列表", example = "[1, 2, 3]")
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
