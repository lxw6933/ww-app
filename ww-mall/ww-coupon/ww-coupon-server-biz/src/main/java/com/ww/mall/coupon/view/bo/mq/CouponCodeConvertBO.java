package com.ww.mall.coupon.view.bo.mq;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author ww
 * @create 2025-03-20- 20:30
 * @description: 券码兑换消息BO（MQ消息体）
 */
@Data
@AllArgsConstructor
@Schema(description = "券码兑换消息参数（MQ消息体）")
public class CouponCodeConvertBO {

    @Schema(description = "用户ID", example = "10001")
    private Long userId;

    @Schema(description = "渠道ID", example = "1")
    private Long channelId;

    @Schema(description = "优惠券券码", example = "ABCD1234EFGH5678")
    private String couponCode;

}
