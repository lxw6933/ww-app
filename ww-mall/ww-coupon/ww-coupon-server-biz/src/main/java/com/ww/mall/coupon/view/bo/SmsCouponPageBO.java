package com.ww.mall.coupon.view.bo;

import com.ww.app.mongodb.common.AbstractMongoPage;
import com.ww.mall.coupon.entity.SmsCouponActivity;
import com.ww.mall.coupon.eunms.CouponDiscountType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Criteria;

/**
 * @author ww
 * @create 2023-07-26- 09:25
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SmsCouponPageBO extends AbstractMongoPage<SmsCouponActivity> {

    /**
     * 优惠券名称
     */
    private String title;

    /**
     * 上下架
     */
    private Boolean status;

    /**
     * 优惠券优惠类型
     */
    private CouponDiscountType couponDiscountType;

    @Override
    public Criteria buildQuery() {
        Criteria criteria = new Criteria();
        if (StringUtils.isNotBlank(this.title)) {
            criteria.and("title").is(this.title);
        }
        if (this.couponDiscountType != null) {
            criteria.and("couponDiscountType").is(this.couponDiscountType);
        }
        if (this.status != null) {
            criteria.and("status").is(this.status);
        }
        return criteria;
    }

}
