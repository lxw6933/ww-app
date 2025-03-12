package com.ww.mall.coupon.entity;

import com.ww.mall.coupon.entity.base.BaseCouponInfo;
import com.ww.mall.coupon.eunms.ApplyProductRangeType;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
    private Long channelId;

    public static Query buildSpuQuery(Long channelId, Boolean integralType, Long smsId) {
        Query query = buildCouponCenterQuery(channelId, integralType);
        Criteria criteria = new Criteria().orOperator(
                Criteria.where("applyProductRangeType").is(ApplyProductRangeType.ALL),
                Criteria.where("applyProductRangeType").is(ApplyProductRangeType.SPECIFY_PRODUCT).and("idList").in(smsId),
                Criteria.where("applyProductRangeType").is(ApplyProductRangeType.EXCLUDE_PRODUCT).and("idList").nin(smsId)
        );
        query.addCriteria(criteria);
        return query;
    }

}
