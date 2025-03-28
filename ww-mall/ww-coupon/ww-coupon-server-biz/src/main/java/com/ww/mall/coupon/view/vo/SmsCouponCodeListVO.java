package com.ww.mall.coupon.view.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.ww.mall.coupon.eunms.CouponStatus;
import lombok.Data;

import java.util.Date;

/**
 * @author ww
 * @create 2023-07-26- 09:16
 * @description: 优惠券券码列表信息
 */
@Data
public class SmsCouponCodeListVO {

    /**
     * 优惠券券码
     */
    @ExcelProperty(value = "优惠券券码", index = 0)
    private String code;

    /**
     * 优惠券券码状态
     */
    @ExcelProperty(value = "优惠券券码状态", index = 1)
    private CouponStatus couponStatus;

    /**
     * 批次号
     */
    @ExcelProperty(value = "批次号", index = 2)
    private String batchNo;

    /**
     * 领取时间
     */
    @ExcelProperty(value = "领取时间", index = 3)
    private Date receiveTime;

    /**
     * 核销时间
     */
    @ExcelProperty(value = "核销时间", index = 4)
    private Date verificationTime;

    /**
     * 领取用户id
     */
    @ExcelProperty(value = "领取用户id", index = 5)
    private Long memberId;

}
