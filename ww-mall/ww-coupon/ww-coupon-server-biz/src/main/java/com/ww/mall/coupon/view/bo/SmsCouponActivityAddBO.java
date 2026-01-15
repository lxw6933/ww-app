package com.ww.mall.coupon.view.bo;

import com.ww.mall.coupon.entity.SmsCouponActivity;
import com.ww.mall.coupon.enums.CouponType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2025-03-05- 15:07
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "平台优惠券活动新增参数")
public class SmsCouponActivityAddBO extends CouponActivityBaseAddBO {

    @Schema(description = "渠道ID", example = "1")
    @NotNull(message = "渠道id不能为空")
    private Long channelId;

    /**
     * 将SmsCouponActivityAddBO转换为SmsCouponActivity
     *
     * @return SmsCouponActivity
     */
    public SmsCouponActivity convertSmsCouponActivity() {
        SmsCouponActivity activity = new SmsCouponActivity();
        initCouponActivity(activity, CouponType.PLATFORM);
        activity.setChannelId(this.getChannelId());
        return activity;
    }

}
