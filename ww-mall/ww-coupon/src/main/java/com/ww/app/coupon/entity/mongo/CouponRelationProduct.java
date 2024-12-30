package com.ww.app.coupon.entity.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author ww
 * @create 2023-07-27- 13:58
 * @description: 优惠券绑定能使用的商品
 */
@Data
@Document(collection = "t_coupon_relation_product")
public class CouponRelationProduct {

    @Id
    private String id;

    /**
     * 优惠券活动编码
     */
    private String activityCode;

    /**
     * 商品id
     */
    private Long spuId;

    /**
     * 规格id
     */
    private Long skuId;

    /**
     * 渠道id
     */
    private Long channelId;

    /**
     * 品牌id
     */
    private Long brandId;

    /**
     * 类目id【三级类目】
     */
    private Long categoryId;

    /**
     * 分组id【三级分组】
     */
    private Long groupId;

}
