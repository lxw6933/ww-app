package com.ww.mall.coupon.view.bo;

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
public class MerchantCouponActivityCenterBO extends CouponActivityCenterBO {

    /**
     * 商家id列表
     */
    private List<Long> merchantIdList;

}
