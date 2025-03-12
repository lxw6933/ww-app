package com.ww.mall.coupon.view.vo;

import com.ww.mall.coupon.eunms.ApplyProductRangeType;
import com.ww.mall.coupon.view.vo.base.BaseCouponInfoVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2025-03-11- 13:57
 * @description: 领券中心优惠券活动信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CouponActivityCenterVO extends BaseCouponInfoVO {

    /**
     * 开始领取时间
     */
    private Date receiveStartTime;

    /**
     * 结束领取时间
     */
    private Date receiveEndTime;

    /**
     * 适用范围
     */
    private ApplyProductRangeType applyProductRangeType;

    /**
     * 适用范围id集合
     */
    private List<Long> idList;

    /**
     * 领取比例
     */
    private BigDecimal ratio;

}
