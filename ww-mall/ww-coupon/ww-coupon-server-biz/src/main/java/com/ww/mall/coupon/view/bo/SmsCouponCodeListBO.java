package com.ww.mall.coupon.view.bo;

import cn.hutool.extra.spring.SpringUtil;
import com.ww.app.mongodb.common.AbstractMongoPage;
import com.ww.mall.coupon.entity.SmsCouponCode;
import com.ww.mall.coupon.entity.SmsCouponRecord;
import com.ww.mall.coupon.eunms.CouponStatus;
import com.ww.mall.coupon.utils.CouponUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ww
 * @create 2023-07-26- 09:25
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SmsCouponCodeListBO extends AbstractMongoPage<SmsCouponCode> {

    @NotNull(message = "渠道不能为空")
    private Long channelId;

    /**
     * 活动编码
     */
    @NotBlank(message = "活动编码不能为空")
    private String activityCode;

    /**
     * 优惠券券码状态
     */
    private CouponStatus couponStatus;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 优惠券券码
     */
    private String code;

    @Override
    public Criteria buildQuery() {
        Criteria criteria = new Criteria();
        criteria.and("activityCode").is(this.activityCode);
        if (StringUtils.isNotBlank(this.batchNo)) {
            criteria.and("batchNo").is(this.batchNo);
        }
        if (StringUtils.isNotBlank(this.code)) {
            criteria.and("code").is(this.code);
        }
        if (this.couponStatus != null) {
            MongoTemplate mongoTemplate = SpringUtil.getBean(MongoTemplate.class);
            String smsCouponRecordCollectionName = CouponUtils.getSmsCouponRecordCollectionName(this.channelId);
            List<SmsCouponRecord> couponRecordList = mongoTemplate.find(SmsCouponRecord.buildStatusQuery(this.activityCode, this.couponStatus).limit(getPageSize()), SmsCouponRecord.class, smsCouponRecordCollectionName);
            if (CollectionUtils.isEmpty(couponRecordList)) {
                criteria.and("code").is("-");
            } else {
                List<String> codesCondition = couponRecordList.stream().map(SmsCouponRecord::getCouponCode).collect(Collectors.toList());
                criteria.and("code").in(codesCondition);
            }
        }
        return criteria;
    }
}
