package com.ww.mall.coupon.view.bo;

import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.enums.ApplyProductRangeType;
import com.ww.mall.coupon.enums.EffectTimeType;
import com.ww.mall.coupon.enums.LimitReceiveTimeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Update;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2025-03-05- 15:07
 * @description:
 */
@Slf4j
@Data
@Schema(description = "优惠券活动基础编辑参数")
public class CouponActivityBaseEditBO {

    @Schema(description = "活动编码", example = "SC1234567890")
    @NotBlank(message = "活动编码不能为空")
    private String activityCode;

    /**
     * 活动名称
     */
    @Schema(description = "活动名称", example = "新用户专享优惠券")
    @NotBlank(message = "活动名词不能为空")
    private String name;

    // =============活动未开始可以编辑字段==============

    /**
     * 优惠券需满X金额
     */
    @Schema(description = "优惠券需满金额（活动未开始时可编辑）", example = "100.00")
    @NotNull(message = "满减金额不能为空")
    private BigDecimal achieveAmount;

    /**
     * 优惠券扣减金额【折扣】
     */
    @Schema(description = "优惠券扣减金额或折扣率（活动未开始时可编辑）", example = "20.00")
    @NotNull(message = "抵扣金额不能为空")
    private BigDecimal deductionAmount;

    /**
     * 领取开始时间
     */
    @Schema(description = "领取开始时间（活动未开始时可编辑）", example = "2025-01-01 00:00:00")
    @NotNull(message = "领取开始时间不能为空")
    private Date receiveStartTime;

    /**
     * 领取结束时间
     */
    @Schema(description = "领取结束时间（活动未开始时可编辑）", example = "2025-12-31 23:59:59")
    @NotNull(message = "领取结束时间不能为空")
    private Date receiveEndTime;

    /**
     * 优惠券领取后有效期计算类型
     * [固定有效期]
     * [根据领取时间计算]
     */
    @Schema(description = "优惠券生效类型（活动未开始时可编辑）", example = "FIXED")
    @NotNull(message = "优惠券生效类型不能为空")
    private EffectTimeType effectTimeType;

    /**
     * 优惠券有效开始时间【固定有效期】
     */
    @Schema(description = "优惠券有效开始时间（固定有效期，活动未开始时可编辑）", example = "2025-01-01 00:00:00")
    private Date useStartTime;

    /**
     * 优惠券有效结束时间【固定有效期】
     */
    @Schema(description = "优惠券有效结束时间（固定有效期，活动未开始时可编辑）", example = "2025-12-31 23:59:59")
    private Date useEndTime;

    /**
     * 领取多少天后可用【根据领取时间计算】
     */
    @Schema(description = "领取多少天后可用（根据领取时间计算，活动未开始时可编辑）", example = "0")
    private int receiveDay;

    /**
     * 有效单位【天，分】【根据领取时间计算】
     */
    @Schema(description = "有效单位（根据领取时间计算，活动未开始时可编辑）", example = "DAY")
    private CouponConstant.EffectTimeUnit effectTimeUnit;

    /**
     * 有效期【根据领取时间计算】
     */
    @Schema(description = "有效期数值（根据领取时间计算，活动未开始时可编辑）", example = "30")
    private int effectNumber;

    // ===========================

    /**
     * 适用范围
     */
    @Schema(description = "适用范围", example = "ALL")
    @NotNull(message = "适用范围不能为空")
    private ApplyProductRangeType applyProductRangeType;

    /**
     * 适用范围id集合
     */
    @Schema(description = "适用范围ID集合", example = "[1001, 1002]")
    private List<Long> idList;

    /**
     * 领取限制类型
     */
    @Schema(description = "领取限制类型", example = "ALL_TIME")
    @NotNull(message = "领取限制类型不能为空")
    private LimitReceiveTimeType limitReceiveTimeType;

    /**
     * 领取限制数量
     */
    @Schema(description = "领取限制数量", example = "1", minimum = "1")
    @Min(value = 1, message = "最小数量不能小于1")
    private int limitReceiveNumber;

    /**
     * 活动说明描述
     */
    @Schema(description = "活动说明描述", example = "限新用户使用，满100减20")
    @NotBlank(message = "活动说明不能为空")
    private String desc;

    public Update buildInfoUpdate() {
        return new Update().set("name", this.name)
                .set("applyProductRangeType", this.applyProductRangeType)
                .set("idList", this.idList)
                .set("limitReceiveTimeType", this.limitReceiveTimeType)
                .set("limitReceiveNumber", this.limitReceiveNumber)
                .set("desc", this.desc);
    }

    public Update buildWaitStartInfoUpdate() {
        Update update = buildInfoUpdate();
        update.set("achieveAmount", this.achieveAmount)
                .set("deductionAmount", this.deductionAmount)
                .set("receiveStartTime", this.receiveStartTime)
                .set("receiveEndTime", this.receiveEndTime)
                .set("effectTimeType", this.effectTimeType)
                .set("useStartTime", this.useStartTime)
                .set("useEndTime", this.useEndTime)
                .set("receiveDay", this.receiveDay)
                .set("effectTimeUnit", this.effectTimeUnit)
                .set("effectNumber", this.effectNumber);
        return update;
    }

}
