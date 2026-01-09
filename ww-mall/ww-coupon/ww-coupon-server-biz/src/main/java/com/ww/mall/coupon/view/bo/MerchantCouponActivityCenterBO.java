package com.ww.mall.coupon.view.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author ww
 * @create 2025-03-11- 14:07
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商家优惠券领券中心查询参数")
public class MerchantCouponActivityCenterBO extends CouponActivityCenterBO {

    /**
     * 商家id列表
     */
    @Schema(description = "商家ID列表", example = "[1001, 1002]")
    private List<Long> merchantIdList;

}
