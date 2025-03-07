package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.eunms.CouponStatus;
import lombok.Data;

import java.util.Date;

/**
 * @author ww
 * @create 2023-07-26- 09:16
 * @description:
 */
@Data
public class SmsCouponCodeListVO {

    /**
     * 优惠券券码
     */
    private String code;

    /**
     * 优惠券券码状态
     */
    private CouponStatus couponStatus;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 领取时间
     */
    private Date receiveTime;

    /**
     * 领取用户id
     */
    private Long memberId;

}
