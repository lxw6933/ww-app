package com.ww.mall.coupon.view.bo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "平台优惠券新增券码参数")
public class SmsAddCouponCodeBO extends AddCouponCodeBO {

    @Schema(description = "渠道ID", example = "1")
    @NotNull(message = "渠道id不能为空")
    private Long channelId;

}
