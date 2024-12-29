package com.ww.mall.search.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author ww
 * @create 2024-07-23- 16:34
 * @description: 商品搜索表
 */
@Data
@Document("v2_product_search")
public class ProductSearch {

    @Id
    private String id;

    private Long spuId;

    private Long skuId;

    private Long smsId;

    private Long channelId;

    /**
     * 品牌id
     */
    private Long brandId;

    /**
     * 类目id
     */
    private Long categoryId;

    /**
     * 商家id
     */
    private Long merchantId;

    /**
     * spu主标题
     */
    private String spuTitle;

    /**
     * spu子标题
     */
    private String spuSubTitle;

    /**
     * spu销量
     */
    private Integer spuSaleNumber;

    /**
     * sku销售价
     */
    private Double salePrice;

    /**
     * 建议销售价
     */
    private Double suggestSalesPrice;

    /**
     * 最低固定积分【多个价格积分，取最低积分】【固定积分+现金商城使用】【纯鸡粉维护】
     */
    private Integer minFixIntegral;

    /**
     * 最低固定金额【多个价格积分，取最低金额】【固定积分+现金商城使用】【纯鸡粉维护】
     */
    private Double minFixPrice;

    /**
     * spu上架时间
     */
    private Long upTime;

    /**
     * 是否允许搜索【1: 可以搜索  0：不能搜索】
     */
    private Integer openSearch;

    /**
     * 是否主推sku【1: 主推  0：非主推】
     */
    private Integer recommendSku;

    /**
     * sku是否禁用状态【1: 正常  0：禁用】
     */
    private Integer skuStatus;

    /**
     * spu是否上架状态【1: 上架  0：非上架】
     */
    private Integer upStatus;

    /**
     * 品牌是否到期【1: yes  0：no】
     */
    private Integer brandAuthExpire;

}
