package com.ww.mall.coupon.view.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2025-03-05- 15:07
 * @description:
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class MerchantCouponActivityEditBO extends CouponActivityBaseEditBO {

    @NotNull(message = "商户ID不能为空")
    private Long merchantId;

}
