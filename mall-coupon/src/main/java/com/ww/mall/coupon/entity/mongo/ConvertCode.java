package com.ww.mall.coupon.entity.mongo;

import com.ww.mall.coupon.eunms.CodeStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author ww
 * @create 2024-03-12- 13:56
 * @description:
 */
@Data
@Document("t_convert_code")
public class ConvertCode {

    @Id
    private String id;

    /**
     * 活动编码
     */
    private String activityCode;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 兑换码
     */
    private String convertCode;

    /**
     * 兑换码状态
     */
    private CodeStatus codeStatus;

    /**
     * 开始时间【有效】
     */
    private String startTime;

    /**
     * 结束时间【有效】
     */
    private String endTime;

    /**
     * 核销时间
     */
    private String consumeTime;

    /**
     * 兑换单号
     */
    private String convertOrderNo;

    /**
     * 核销用户
     */
    private Long userId;

}
