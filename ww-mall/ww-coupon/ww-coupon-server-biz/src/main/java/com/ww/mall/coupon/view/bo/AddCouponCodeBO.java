package com.ww.mall.coupon.view.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2025-03-07 19:43
 * @description:
 */
@Data
@Schema(description = "新增券码基础参数")
public class AddCouponCodeBO {

    @Schema(description = "渠道ID", example = "1")
    @NotNull(message = "渠道id不能为空")
    private Long channelId;

    @Schema(description = "活动编码", example = "SC1234567890")
    @NotBlank(message = "活动编码")
    private String activityCode;

    @Schema(description = "新增券码数量", example = "100", minimum = "1", maximum = "10000")
    @Min(value = 1, message = "最少生成1数量")
    @Max(value = 10000, message = "最多生成10000数量")
    private Integer number;

}
