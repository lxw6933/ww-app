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
@Schema(description = "API发放优惠券券码参数")
public class IssueCouponCodeBO {

    @Schema(description = "渠道ID", example = "1")
    @NotNull(message = "渠道id不能为空")
    private Long channelId;

    @Schema(description = "活动编码", example = "SC1234567890")
    @NotBlank(message = "活动编码不能为空")
    private String activityCode;

    @Schema(description = "券码批次号", example = "20250101-1")
    @NotBlank(message = "券码批次号不能为空")
    private String batchNo;

    @Schema(description = "发放单号", example = "ORDER123456")
    @NotBlank(message = "发放单号")
    private String issueOrderCode;

}
