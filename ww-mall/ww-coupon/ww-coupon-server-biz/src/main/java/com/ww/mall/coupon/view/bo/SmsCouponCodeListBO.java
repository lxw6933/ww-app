package com.ww.mall.coupon.view.bo;

import cn.hutool.extra.spring.SpringUtil;
import com.ww.app.mongodb.common.AbstractMongoPage;
import com.ww.mall.coupon.entity.SmsCouponCode;
import com.ww.mall.coupon.entity.SmsCouponRecord;
import com.ww.mall.coupon.eunms.CouponStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static com.ww.app.common.utils.CollectionUtils.convertList;

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
        if (this.couponStatus != null) {
            List<String> codes = new ArrayList<>();
            if (StringUtils.isNotBlank(this.code)) {
                codes.add(this.code);
            }
            MongoTemplate mongoTemplate = SpringUtil.getBean(MongoTemplate.class);
            String smsCouponRecordCollectionName = SmsCouponRecord.buildCollectionName(this.channelId);
            if (CouponStatus.WAIT.equals(this.couponStatus)) {
                criteria.and("userId").isNull();
            } else {
                Query query = SmsCouponRecord.buildStatusQuery(this.activityCode, this.couponStatus);
                query.fields().include("couponCode");
                List<SmsCouponRecord> couponRecordList = mongoTemplate.find(query, SmsCouponRecord.class, smsCouponRecordCollectionName);
                if (CollectionUtils.isEmpty(couponRecordList)) {
                    criteria.and("code").is("-");
                } else {
                    List<String> codesCondition = convertList(couponRecordList, SmsCouponRecord::getCouponCode);
                    codesCondition.retainAll(codes);
                    if (!codesCondition.isEmpty()) {
                        criteria.and("code").in(codesCondition);
                    } else {
                        criteria.and("code").is("-");
                    }
                }
            }
        } else {
            if (StringUtils.isNotBlank(this.code)) {
                criteria.and("code").is(this.code);
            }
        }
        return criteria;
    }
}
