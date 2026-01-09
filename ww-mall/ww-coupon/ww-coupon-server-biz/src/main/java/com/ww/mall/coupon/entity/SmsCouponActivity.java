package com.ww.mall.coupon.entity;

import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.entity.base.BaseCouponInfo;
import com.ww.mall.coupon.eunms.ApplyProductRangeType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author ww
 * @create 2025-03-05- 15:07
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "sms_coupon_activity")
public class SmsCouponActivity extends BaseCouponInfo {

    /**
     * 渠道id
     */
    @Indexed(name = "idx_channelId")
    private Long channelId;

    public static Query buildSmsCouponCenterQuery(Long channelId, CouponConstant.Type type) {
        Query query = buildCouponCenterQuery(type);
        query.addCriteria(new Criteria().and("channelId").is(channelId));
        return query;
    }

    public static Query buildSpuQuery(Long channelId, CouponConstant.Type type, Long smsId) {
        Query query = buildSmsCouponCenterQuery(channelId, type);
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("applyProductRangeType").is(ApplyProductRangeType.ALL),
                Criteria.where("applyProductRangeType").is(ApplyProductRangeType.SPECIFY_PRODUCT).and("idList").in(smsId),
                Criteria.where("applyProductRangeType").is(ApplyProductRangeType.EXCLUDE_PRODUCT).and("idList").nin(smsId)
        );
        query.addCriteria(criteria);
        return query;
    }

}
