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
@Schema(description = "平台优惠券活动批次号查询参数")
public class SmsCouponActivityBatchNoBO {

    @Schema(description = "渠道ID", example = "1")
    @NotNull(message = "渠道id不能为空")
    private Long channelId;

    @Schema(description = "活动编码", example = "SC1234567890")
    @NotBlank(message = "活动编码不能为空")
    private String activityCode;

}
