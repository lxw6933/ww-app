package com.ww.mall.coupon.view.bo;

import com.ww.mall.coupon.constant.CouponConstant;
import lombok.Data;

/**
 * @author ww
 * @create 2025-03-11- 14:07
 * @description:
 */
@Data
public class CouponActivityCenterBO {

    /**
     * 是否积分券
     */
    private CouponConstant.Type type;

    /**
     * 最后一条数据id的游标值
     */
    private String endIdCursorValue;

}
