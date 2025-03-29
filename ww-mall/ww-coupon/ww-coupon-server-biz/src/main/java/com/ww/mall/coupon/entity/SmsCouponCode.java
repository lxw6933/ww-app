package com.ww.mall.coupon.entity;

import com.ww.app.mongodb.common.BaseDoc;
import com.ww.mall.coupon.utils.CouponUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * @author ww
 * @create 2024-10-15- 15:00
 * @description: 平台优惠券券码
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document
public class SmsCouponCode extends BaseDoc {

    /**
     * 优惠券活动编码
     */
    private String activityCode;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 券码
     */
    private String code;

    /**
     * 领取用户id
     */
    private Long userId;

    public SmsCouponCode(String activityCode, String batchNo, String code) {
        this.activityCode = activityCode;
        this.batchNo = batchNo;
        this.code = code;
    }

    public static String buildCollectionName(Long channelId) {
        return CouponUtils.getSmsCouponCodeCollectionName(channelId);
    }

    /**
     * 券码查询条件
     *
     * @param couponCode 券码
     * @return Query
     */
    public static Query buildCodeQuery(String couponCode) {
        return new Query().addCriteria(Criteria.where("code").is(couponCode));
    }

    public static Update buildCodeUserIdUpdate(Long userId) {
        return new Update().set("userId", userId);
    }

}
