package com.ww.mall.coupon.view.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.ww.app.excel.convert.CommonEnumConverter;
import com.ww.mall.coupon.enums.CouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * @author ww
 * @create 2023-07-26- 09:16
 * @description: 优惠券券码列表信息
 */
@Data
@Schema(description = "优惠券券码列表信息")
public class SmsCouponCodeListVO {

    /**
     * 优惠券券码
     */
    @Schema(description = "优惠券券码", example = "ABCD1234EFGH5678")
    @ExcelProperty(value = "优惠券券码", index = 0)
    private String code;

    /**
     * 优惠券券码状态
     */
    @Schema(description = "优惠券券码状态", example = "WAIT")
    @ExcelProperty(value = "优惠券券码状态", converter = CommonEnumConverter.class, index = 1)
    private CouponStatus couponStatus;

    /**
     * 批次号
     */
    @Schema(description = "批次号", example = "20250101-1")
    @ExcelProperty(value = "批次号", index = 2)
    private String batchNo;

    /**
     * 领取时间
     */
    @Schema(description = "领取时间", example = "2025-01-15 10:30:00")
    @ExcelProperty(value = "领取时间", index = 3)
    private Date receiveTime;

    /**
     * 核销时间
     */
    @Schema(description = "核销时间", example = "2025-01-20 14:20:00")
    @ExcelProperty(value = "核销时间", index = 4)
    private Date verificationTime;

    /**
     * 领取用户id
     */
    @Schema(description = "领取用户ID", example = "10001")
    @ExcelProperty(value = "领取用户id", index = 5)
    private Long memberId;

}
