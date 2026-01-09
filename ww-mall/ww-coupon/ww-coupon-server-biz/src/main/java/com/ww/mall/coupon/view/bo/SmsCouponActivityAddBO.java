package com.ww.mall.coupon.view.bo;

import com.ww.mall.coupon.entity.SmsCouponActivity;
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
public class SmsCouponActivityAddBO extends CouponActivityBaseAddBO {

    @NotNull(message = "渠道id不能为空")
    private Long channelId;

    /**
     * 将SmsCouponActivityAddBO转换为SmsCouponActivity
     *
     * @return SmsCouponActivity
     */
    public SmsCouponActivity convertSmsCouponActivity() {
        SmsCouponActivity activity = new SmsCouponActivity();
        initCouponActivity(activity);
        activity.setChannelId(this.getChannelId());
        return activity;
    }

}
