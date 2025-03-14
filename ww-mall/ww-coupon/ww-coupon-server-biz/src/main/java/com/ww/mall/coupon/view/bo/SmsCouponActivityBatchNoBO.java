package com.ww.mall.coupon.view.bo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2025-03-12- 18:01
 * @description:
 */
@Data
public class SmsCouponActivityBatchNoBO {

    @NotNull(message = "渠道id不能为空")
    private Long channelId;

    @NotBlank(message = "活动编码不能为空")
    private String activityCode;

}
