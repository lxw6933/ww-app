package com.ww.mall.coupon.view.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2025-03-07 19:43
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商家优惠券新增券码参数")
public class MerchantAddCouponCodeBO extends AddCouponCodeBO {

}
