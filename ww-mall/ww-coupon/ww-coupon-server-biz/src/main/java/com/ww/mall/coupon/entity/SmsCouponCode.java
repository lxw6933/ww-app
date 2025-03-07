package com.ww.mall.coupon.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author ww
 * @create 2024-10-15- 15:00
 * @description: 平台优惠券券码
 */
@Data
@Document
public class SmsCouponCode {

    @Id
    private String id;

    /**
     * 优惠券活动编码
     */
    private String activityCode;

    /**
     * 渠道id
     */
    private Long channelId;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 券码
     */
    private String code;

    public SmsCouponCode(String activityCode, Long channelId, String batchNo, String code) {
        this.activityCode = activityCode;
        this.channelId = channelId;
        this.batchNo = batchNo;
        this.code = code;
    }

    /**
     * 券码查询条件
     *
     * @param channelId 渠道id
     * @param couponCode 券码
     * @return Query
     */
    public static Query buildCodeQuery(Long channelId, String couponCode) {
        return new Query().addCriteria(
                Criteria.where("channelId").is(channelId)
                        .and("code").is(couponCode)
        );
    }

}
