package com.ww.mall.coupon.view.bo;

import com.ww.app.common.utils.SpecialCharacterUtil;
import com.ww.app.mongodb.common.AbstractMongoPage;
import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.entity.SmsCouponActivity;
import com.ww.mall.coupon.entity.base.BaseCouponInfo;
import com.ww.mall.coupon.eunms.CouponDiscountType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Date;

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

    /**
     * 活动状态
     */
    private CouponConstant.ActivityStatus activityStatus;

    @Override
    public Criteria buildQuery() {
        Date now = new Date();
        Criteria criteria = new Criteria();
        criteria.and("channelId").is(this.channelId);
        if (StringUtils.isNotBlank(this.id)) {
            criteria.and(MONGO_PRIMARY_KEY).is(this.id);
        }
        if (StringUtils.isNotBlank(this.name)) {
            String escapedKeyword = SpecialCharacterUtil.escapeSpecialCharacters(this.name);
            String pattern = ".*" + escapedKeyword + ".*";
            criteria.and("name").regex(pattern, "i");
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
        BaseCouponInfo.setCouponTypeCriteria(type, criteria);
        switch (activityStatus) {
            case WAIT_EFFECTIVE:
                criteria.and("receiveStartTime").lt(now);
                break;
            case EFFECTIVE:
                criteria.and("receiveStartTime").lte(now)
                        .and("receiveEndTime").gte(now);
                break;
            case EXPIRED:
                criteria.and("receiveEndTime").lt(now);
                break;
            case ALL:
            default:
        }
        return criteria;
    }

}
