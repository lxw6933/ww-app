package com.ww.app.search.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

/**
 * @author ww
 * @create 2023-08-14- 10:53
 * @description:
 */
@Data
@Document(indexName = "jh_v2_product")
public class Product {

    @Id
    private String id;

    @Field(type = FieldType.Keyword, name = "bcmProductId")
    private String bcmProductId;

    @Field(type = FieldType.Keyword, name = "bcmBrandCategoryCode")
    private String bcmBrandCategoryCode;

    @Field(type = FieldType.Long, name = "merchantId")
    private Long merchantId;

    @Field(type = FieldType.Long, name = "categoryId")
    private Long categoryId;

    @Field(type = FieldType.Long, name = "brandId")
    private Long brandId;

    @Field(type = FieldType.Long, name = "invoiceRuleId")
    private Long invoiceRuleId;

    @Field(type = FieldType.Text, name = "baseSpuAttrs")
    private String baseSpuAttrs;

    @Field(type = FieldType.Text, name = "customSpuAttrs")
    private String customSpuAttrs;

    @Field(type = FieldType.Text, name = "productSkuAttrs", analyzer = "ik_smart")
    private String productSkuAttrs;

    @Field(type = FieldType.Keyword, name = "rechargeType")
    private String rechargeType;

    @Field(type = FieldType.Keyword, name = "productType")
    private String productType;

    @Field(type = FieldType.Keyword, name = "accessType")
    private String accessType;

    @Field(type = FieldType.Text, name = "imageUrl")
    private String imageUrl;

    @Field(type = FieldType.Keyword, name = "productCode")
    private String productCode;

    @Field(type = FieldType.Text, name = "title", analyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, name = "secondTitle", analyzer = "ik_smart")
    private String secondTitle;

    @Field(type = FieldType.Boolean, name = "pass")
    private Boolean pass;

    @Field(type = FieldType.Text, name = "priceCompare")
    private String priceCompare;

    @Field(type = FieldType.Text, name = "enclosureName")
    private String enclosureName;

    @Field(type = FieldType.Text, name = "enclosureUrl")
    private String enclosureUrl;

    @Field(type = FieldType.Text, name = "videoUrl")
    private String videoUrl;

    @Field(type = FieldType.Keyword, name = "backgroundColor")
    private String backgroundColor;

    @Field(type = FieldType.Text, name = "productDetail")
    private String productDetail;

    @Field(type = FieldType.Boolean, name = "valid")
    private Boolean valid;

    @Field(type = FieldType.Keyword, name = "status")
    private String status;

    @Field(type = FieldType.Keyword, name = "auditStatus")
    private String auditStatus;

    @Field(type = FieldType.Boolean, name = "hasWorkFlow")
    private Boolean hasWorkFlow;

    @Field(type = FieldType.Long, name = "lastProductDocId")
    private Long lastProductDocId;

    @Field(type = FieldType.Long, name = "unit")
    private Long unit;

    @Field(type = FieldType.Long, name = "fareTmpId")
    private Long fareTmpId;

    @Field(type = FieldType.Keyword, name = "tagIds")
    private String tagIds;

    @Field(type = FieldType.Boolean, name = "openSearch")
    private Boolean openSearch;

    @Field(type = FieldType.Integer, name = "limitedNum")
    private Integer limitedNum;

    @Field(type = FieldType.Boolean, name = "isLimitPayType")
    private Boolean isLimitPayType;

    @Field(type = FieldType.Keyword, name = "payCardsType")
    private String payCardsType;

    @Field(type = FieldType.Keyword, name = "payCardsNo")
    private String payCardsNo;

    @Field(type = FieldType.Keyword, name = "invoiceTypes")
    private String invoiceTypes;

    @Field(type = FieldType.Boolean, name = "refundable")
    private Boolean refundable;

    @Field(type = FieldType.Boolean, name = "replaceable")
    private Boolean replaceable;

    @Field(type = FieldType.Boolean, name = "refundOnly")
    private Boolean refundOnly;

    @Field(type = FieldType.Keyword, name = "channelIds")
    private String channelIds;

    @Field(type = FieldType.Keyword, name = "upConfig")
    private String upConfig;

    @Field(type = FieldType.Date, name = "upTime")
    private Date upTime;

    @Field(type = FieldType.Keyword, name = "stages")
    private String stages;

    @Field(type = FieldType.Boolean, name = "isSpuDimension")
    private Boolean isSpuDimension;

    @Field(type = FieldType.Date, name = "downDate")
    private Date downDate;

    @Field(type = FieldType.Date, name = "putDate")
    private Date putDate;

    @Field(type = FieldType.Double, name = "minSalesPrice")
    private Double minSalesPrice;

    @Field(type = FieldType.Text, name = "brandAuthChannel")
    private String brandAuthChannel;

    @Field(type = FieldType.Keyword, name = "productServiceType")
    private String productServiceType;

    @Field(type = FieldType.Date, name = "createTime")
    private Date createTime;

}
