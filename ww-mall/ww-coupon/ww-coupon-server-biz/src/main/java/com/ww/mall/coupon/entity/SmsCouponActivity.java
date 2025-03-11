package com.ww.mall.coupon.entity;

import com.ww.mall.coupon.entity.base.BaseCouponInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

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

}
