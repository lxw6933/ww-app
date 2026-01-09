package com.ww.mall.coupon.entity;

import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.entity.base.BaseCouponInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

/**
 * @author ww
 * @create 2025-03-05- 15:07
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "merchant_coupon_activity")
public class MerchantCouponActivity extends BaseCouponInfo {

    /**
     * 商家id
     */
    @Indexed(name = "idx_merchantId")
    private Long merchantId;

    /**
     * 分发渠道
     */
    private List<Long> channelIds;

    /**
     * 审核状态
     */
    private CouponConstant.AuditStatus auditStatus;

    public static Query buildMerchantCouponCenterQuery(List<Long> merchantIds, Long channelId, CouponConstant.Type type) {
        Query query = buildCouponCenterQuery(type);
        query.addCriteria(new Criteria().and("auditStatus").is(CouponConstant.AuditStatus.AUDIT_PASS)
                .and("channelIds").in(channelId)
                .and("merchantId").in(merchantIds)
        );
        return query;
    }

    public static Query buildMerchantCouponAuditQuery(String activityCode) {
        Query query = buildActivityCodeQuery(activityCode);
        query.addCriteria(new Criteria().and("auditStatus").is(CouponConstant.AuditStatus.WAIT_AUDIT));
        return query;
    }

    public static Update buildMerchantActivityAuditUpdate(boolean status) {
        return new Update().set("auditStatus", status ? CouponConstant.AuditStatus.AUDIT_PASS : CouponConstant.AuditStatus.AUDIT_NOT_PASS);
    }

}
