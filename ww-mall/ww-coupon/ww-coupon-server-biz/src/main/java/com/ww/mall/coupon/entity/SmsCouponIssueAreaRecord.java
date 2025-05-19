package com.ww.mall.coupon.entity;

import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author ww
 * @create 2025-05-16- 14:17
 * @description: 加入发放区记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "v2_sms_coupon_issue_area_record")
public class SmsCouponIssueAreaRecord extends BaseDoc {

    private String activityCode;

    private String batchNo;

    private int codeNumber;

    private int issueAreaNumber;

    public static Query buildActivityCodeBatchNoQuery(String activityCode, String batchNo) {
        return new Query().addCriteria(Criteria.where("activityCode").is(activityCode).and("batchNo").is(batchNo));
    }

}
