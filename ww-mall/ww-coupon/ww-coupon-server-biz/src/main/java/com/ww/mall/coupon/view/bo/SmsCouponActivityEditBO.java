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
@Schema(description = "平台优惠券活动编辑参数")
public class SmsCouponActivityEditBO extends CouponActivityBaseEditBO {

    @Schema(description = "渠道ID", example = "1")
    @NotNull(message = "渠道id不能为空")
    private Long channelId;

}
