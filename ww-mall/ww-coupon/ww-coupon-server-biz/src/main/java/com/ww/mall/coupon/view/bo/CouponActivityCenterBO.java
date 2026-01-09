package com.ww.mall.coupon.view.bo;

import com.ww.mall.coupon.constant.CouponConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ww
 * @create 2025-03-11- 14:07
 * @description:
 */
@Data
@Schema(description = "平台领券中心查询参数")
public class CouponActivityCenterBO {

    /**
     * 是否积分券
     */
    @Schema(description = "优惠券类型", example = "CASH", allowableValues = {"ALL", "CASH", "INTEGRAL"})
    private CouponConstant.Type type;

    /**
     * 最后一条数据id的游标值
     */
    @Schema(description = "最后一条数据ID的游标值（分页用）", example = "507f1f77bcf86cd799439011")
    private String endIdCursorValue;

}
