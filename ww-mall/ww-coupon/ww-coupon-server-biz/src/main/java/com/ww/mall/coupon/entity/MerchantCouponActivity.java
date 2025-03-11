package com.ww.mall.coupon.entity;

import com.ww.mall.coupon.entity.base.BaseCouponInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

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
    private Long merchantId;

    /**
     * 分发渠道
     */
    private List<Long> channelIds;

    /**
     * 是否审核通过
     */
    private boolean auditPass;

}
