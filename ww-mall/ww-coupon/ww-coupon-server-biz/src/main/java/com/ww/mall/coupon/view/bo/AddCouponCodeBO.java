package com.ww.mall.coupon.view.bo;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * @author ww
 * @create 2025-03-07 19:43
 * @description:
 */
@Data
public class AddCouponCodeBO {

    @NotBlank(message = "活动编码")
    private String activityCode;

    @Min(value = 1, message = "最少生成1数量")
    @Max(value = 10000, message = "最多生成10000数量")
    private Integer number;

}
