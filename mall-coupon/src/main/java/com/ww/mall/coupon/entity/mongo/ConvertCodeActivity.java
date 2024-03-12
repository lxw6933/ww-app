package com.ww.mall.coupon.entity.mongo;

import com.ww.mall.coupon.eunms.CodeLength;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author ww
 * @create 2024-03-12- 13:51
 * @description:
 */
@Data
@Document("t_convert_code_activity")
public class ConvertCodeActivity {

    @Id
    private String id;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 更新时间
     */
    private String updateTime;

    /**
     * 活动名称
     */
    private String title;

    /**
     * 活动编码
     */
    private String activityCode;

    /**
     * 活动开始时间
     */
    private String startTime;

    /**
     * 活动结束时间
     */
    private String endTime;

    /**
     * 兑换码位数
     */
    private CodeLength codeLength;

    /**
     * 上下架
     */
    private Boolean status;

}
