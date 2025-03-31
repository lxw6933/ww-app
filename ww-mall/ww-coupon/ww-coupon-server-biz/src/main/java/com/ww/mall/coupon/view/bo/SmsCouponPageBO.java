package com.ww.mall.coupon.view.bo;

import com.ww.app.mongodb.common.AbstractMongoPage;
import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.entity.SmsCouponActivity;
import com.ww.mall.coupon.eunms.CouponDiscountType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Criteria;

import static com.ww.app.common.constant.Constant.MONGO_PRIMARY_KEY;

/**
 * @author ww
 * @create 2023-07-26- 09:25
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SmsCouponPageBO extends AbstractMongoPage<SmsCouponActivity> {

    /**
     * 渠道id
     */
    private Long channelId;

    /**
     * id
     */
    private String id;

    /**
     * 优惠券名称
     */
    private String name;

    /**
     * 活动编码
     */
    private String activityCode;

    /**
     * 类型【积分/现金】
     */
    private CouponConstant.Type type;

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
        criteria.and("channelId").is(this.channelId);
        if (StringUtils.isNotBlank(this.id)) {
            criteria.and(MONGO_PRIMARY_KEY).is(this.id);
        }
        if (StringUtils.isNotBlank(this.name)) {
            criteria.and("name").is(this.name);
        }
        if (StringUtils.isNotBlank(this.activityCode)) {
            criteria.and("activityCode").is(this.activityCode);
        }
        if (this.couponDiscountType != null) {
            criteria.and("couponDiscountType").is(this.couponDiscountType);
        }
        if (this.status != null) {
            criteria.and("status").is(this.status);
        }
        switch (type) {
            case ALL:
                break;
            case INTEGRAL:
                criteria.and("couponDiscountType").is(CouponDiscountType.INTEGRAL_DISCOUNT);
                break;
            case CASH:
                criteria.and("couponDiscountType").in(CouponDiscountType.FULL_DISCOUNT, CouponDiscountType.FULL_REDUCTION, CouponDiscountType.DIRECT_REDUCTION);
                break;
        }
        return criteria;
    }

}
