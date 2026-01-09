package com.ww.mall.coupon.view.bo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "商家优惠券活动编辑参数")
public class MerchantCouponActivityEditBO extends CouponActivityBaseEditBO {

    @Schema(description = "商户ID", example = "1001")
    @NotNull(message = "商户ID不能为空")
    private Long merchantId;

}
