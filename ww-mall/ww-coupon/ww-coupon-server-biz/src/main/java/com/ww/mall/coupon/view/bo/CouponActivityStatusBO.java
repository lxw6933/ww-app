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
@Schema(description = "优惠券活动状态更新参数")
public class CouponActivityStatusBO {

    @Schema(description = "活动编码", example = "SC1234567890")
    @NotBlank(message = "活动编码不能为空")
    private String activityCode;

    @Schema(description = "上下架状态：true-上架，false-下架", example = "true")
    @NotNull(message = "状态不能为空")
    private Boolean status;

}
