package com.ww.app.seckill.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author ww
 * @create 2024-06-13 23:55
 * @description:
 */
@Data
@Document("stock_jtn_log")
public class StockJrnLog {

    @Id
    private String id;

    /**
     * spuId
     */
    private Long spuId;

    /**
     * skuId
     */
    private Long skuId;

    /**
     * 渠道id
     */
    private Long channelId;

    /**
     * 活动编码【非活动为null】
     */
    private String activityCode;

    /**
     * 活动场次编码【非活动为null】
     */
    private String subActivityCode;

    /**
     * 变动类型【如入库、出库、调整】
     */
    private Integer type;

    /**
     * 变动数量【正数增加、负数减少】
     */
    private Integer number;

    /**
     * 变动时间
     */
    private Long time;

    /**
     * 来源编码【订单编码、手功补单】
     */
    private String SourceCode;

    /**
     * 备注
     */
    private String remark;

    /**
     * 变动前库存【区分活动库存和商品库存】
     */
    private Integer beforeStock;

    /**
     * 变动后库存【区分活动库存和商品库存】
     */
    private Integer afterStock;

}
