package com.ww.mall.coupon.view.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2025-03-07 19:43
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SmsAddCouponCodeBO extends AddCouponCodeBO {

    @NotNull(message = "渠道id不能为空")
    private Long channelId;

}
