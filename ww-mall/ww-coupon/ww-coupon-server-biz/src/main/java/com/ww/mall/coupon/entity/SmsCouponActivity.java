package com.ww.mall.coupon.entity;

import com.ww.app.mongodb.common.BaseDoc;
import com.ww.mall.coupon.eunms.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2025-03-05- 15:07
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "sms_coupon_activity")
public class SmsCouponActivity extends BaseDoc {

    /**
     * 渠道id
     */
    private Long channelId;

    /**
     * 活动编码
     */
    private String activityCode;

    /**
     * 活动名词
     */
    private String name;

    /**
     * 优惠券优惠类型
     */
    private CouponDiscountType couponDiscountType;

    /**
     * 优惠券需满X金额
     */
    private BigDecimal achieveAmount;

    /**
     * 优惠券扣减金额【折扣】
     */
    private BigDecimal deductionAmount;

    /**
     * 领取开始时间
     */
    private Date receiveStartDate;

    /**
     * 领取结束时间
     */
    private Date receiveEndDate;

    /**
     * 优惠券领取后有效期计算类型
     * [固定有效期]
     * [根据领取时间计算]
     */
    private EffectTimeType effectTimeType;

    /**
     * 优惠券有效开始时间【固定有效期】
     */
    private Date useStartTime;

    /**
     * 优惠券有效结束时间【固定有效期】
     */
    private Date useEndTime;

    /**
     * 领取多少天后生效【根据领取时间计算】
     */
    private int receiveAfterEffectDay;

    /**
     * 多少天的有效期【根据领取时间计算】
     */
    private int effectDay;

    /**
     * 发放类型
     */
    private IssueType issueType;

    /**
     * 适用用户范围
     */
    private ApplyMemberType applyMemberType;

    /**
     * 适用范围
     */
    private ApplyProductRangeType applyProductRangeType;

    /**
     * 适用范围id集合
     */
    private List<Long> idList;

    /**
     * 领取限制类型
     */
    private LimitReceiveTimeType limitReceiveTimeType;

    /**
     * 领取限制数量
     */
    private int limitReceiveNumber;

    /**
     * 优惠券数量
     */
    private int number;

    /**
     * 领取数量
     */
    private int receiveNumber;

    /**
     * 使用数量
     */
    private int useNumber;

    /**
     * 活动说明描述
     */
    private String desc;

    /**
     * 上下架
     */
    private Boolean status;

    public static Query buildActivityCodeQuery(String activityCode) {
        return new Query().addCriteria(Criteria.where("activityCode").is(activityCode));
    }

    public static Update buildActivityNumberUpdate(int number) {
        return new Update().inc("number", number);
    }

}
