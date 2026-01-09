package com.ww.mall.coupon.view.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2023-07-26- 09:16
 * @description: 平台优惠券信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "平台优惠券活动详情")
public class SmsCouponDetailVO extends BaseCouponDetailVO {

}
