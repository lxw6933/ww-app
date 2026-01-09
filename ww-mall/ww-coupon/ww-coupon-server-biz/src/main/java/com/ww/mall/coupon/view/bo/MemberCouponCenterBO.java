package com.ww.mall.coupon.view.bo;

import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.enums.CouponType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ww
 * @create 2025-03-11- 14:07
 * @description:
 */
@Data
@Schema(description = "会员卡券中心查询参数")
public class MemberCouponCenterBO {

    /**
     * 最后一条数据id的游标值
     */
    @Schema(description = "最后一条数据ID的游标值（分页用）", example = "507f1f77bcf86cd799439011")
    private String endIdCursorValue;

    /**
     * 状态
     */
    @Schema(description = "优惠券状态", example = "USE", allowableValues = {"ALL", "USE", "USED", "EXPIRE"})
    private CouponConstant.Status status;

    /**
     * 优惠券类型
     */
    @Schema(description = "优惠券类型", example = "PLATFORM", allowableValues = {"PLATFORM", "MERCHANT"})
    private CouponType couponType;

    /**
     * 积分类型
     */
    @Schema(description = "优惠券价值类型", example = "CASH", allowableValues = {"ALL", "CASH", "INTEGRAL"})
    private CouponConstant.Type type;

    /**
     * 展示数量
     */
    @Schema(description = "每页展示数量", example = "10")
    private Integer size;

    /**
     * activityCode
     */
    @Schema(description = "活动编码", example = "SC1234567890")
    private String activityCode;

    public Integer getSize() {
        return this.size == null ? 10 : this.size;
    }
}
