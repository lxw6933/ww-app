package com.ww.mall.coupon.view.bo;

import com.ww.mall.coupon.eunms.ApplyProductRangeType;
import com.ww.mall.coupon.eunms.LimitReceiveTimeType;
import lombok.Data;
import org.springframework.data.mongodb.core.query.Update;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author ww
 * @create 2025-03-05- 15:07
 * @description:
 */
@Data
public class SmsCouponActivityEditBO {

    @NotNull(message = "渠道id不能为空")
    private Long channelId;

    @NotBlank(message = "活动编码不能为空")
    private String activityCode;

    /**
     * 活动名称
     */
    @NotBlank(message = "活动名词不能为空")
    private String name;

    /**
     * 适用范围
     */
    @NotNull(message = "适用范围不能为空")
    private ApplyProductRangeType applyProductRangeType;

    /**
     * 适用范围id集合
     */
    private List<Long> idList;

    /**
     * 领取限制类型
     */
    @NotNull(message = "领取限制类型不能为空")
    private LimitReceiveTimeType limitReceiveTimeType;

    /**
     * 领取限制数量
     */
    @Min(value = 1, message = "最小数量不能小于1")
    private int limitReceiveNumber;

    /**
     * 活动说明描述
     */
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

}
