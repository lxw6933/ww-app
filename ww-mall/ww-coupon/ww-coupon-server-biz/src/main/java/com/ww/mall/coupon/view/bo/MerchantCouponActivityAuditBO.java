package com.ww.mall.coupon.view.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2025-03-12- 18:01
 * @description:
 */
@Data
@Schema(description = "商家优惠券活动审核参数")
public class MerchantCouponActivityAuditBO {

    @Schema(description = "活动编码", example = "MC1234567890")
    @NotBlank(message = "活动编码不能为空")
    private String activityCode;

    @Schema(description = "是否审核通过：true-通过，false-不通过", example = "true")
    @NotNull(message = "是否审核通过")
    private Boolean auditPass;

}
