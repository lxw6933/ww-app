package com.ww.app.member.entity.mongo;

import com.ww.app.member.enums.IntegralSource;
import com.ww.app.member.enums.IntegralType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author ww
 * @create 2023-07-21- 15:42
 * @description:
 */
@Data
@Document(collection = "member_integral_record")
public class MemberIntegralRecord {

    @Id
    private String id;

    /**
     * 会员id
     */
    private Long memberId;

    /**
     * 积分来源
     */
    private IntegralSource integralSource;

    /**
     * 积分类型【新增、减少】
     */
    private IntegralType integralType;

    /**
     * 积分数量
     */
    private Integer integralNum;

    /**
     * 订单号
     */
    private String orderCode;

    /**
     * 支付流水
     */
    private String payJrn;

    /**
     * 是否已读
     */
    private Boolean read;

    /**
     * 创建时间
     */
    private String createTime;

}
