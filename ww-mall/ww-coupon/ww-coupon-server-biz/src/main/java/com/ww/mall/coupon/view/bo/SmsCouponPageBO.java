package com.ww.mall.coupon.view.bo;

import com.ww.app.common.utils.SpecialCharacterUtil;
import com.ww.app.mongodb.common.AbstractMongoPage;
import com.ww.mall.coupon.constant.CouponConstant;
import com.ww.mall.coupon.entity.SmsCouponActivity;
import com.ww.mall.coupon.entity.base.BaseCouponInfo;
import com.ww.mall.coupon.enums.CouponDiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "平台优惠券分页查询参数")
public class SmsCouponPageBO extends AbstractMongoPage<SmsCouponActivity> {

    /**
     * 渠道id
     */
    @Schema(description = "渠道ID", example = "1")
    private Long channelId;

    /**
     * id
     */
    @Schema(description = "活动ID", example = "507f1f77bcf86cd799439011")
    private String id;

    /**
     * 优惠券名称
     */
    @Schema(description = "优惠券名称（支持模糊查询）", example = "新用户")
    private String name;

    /**
     * 活动编码
     */
    @Schema(description = "活动编码", example = "SC1234567890")
    private String activityCode;

    /**
     * 类型【积分/现金】
     */
    @Schema(description = "类型", example = "CASH", allowableValues = {"ALL", "CASH", "INTEGRAL"})
    private CouponConstant.Type type;

    /**
     * 上下架
     */
    @Schema(description = "上下架状态", example = "true")
    private Boolean status;

    /**
     * 优惠券优惠类型
     */
    @Schema(description = "优惠券优惠类型", example = "DIRECT_REDUCTION")
    private CouponDiscountType couponDiscountType;

    /**
     * 活动状态
     */
    @Schema(description = "活动状态", example = "EFFECTIVE", allowableValues = {"ALL", "WAIT_EFFECTIVE", "EFFECTIVE", "EXPIRED"})
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
