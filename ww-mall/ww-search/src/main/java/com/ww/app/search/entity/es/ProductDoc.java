package com.ww.app.search.entity.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2025-01-02- 10:37
 * @description: es商品搜索文档
 */
@Data
@Document(indexName = "product")
public class ProductDoc {

    @Id
    private String smsId; // 运营商品id

    @Field(type = FieldType.Keyword)
    private String productId; // 商品ID

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String name; // 商品名称

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String desc; // 商品描述

    @Field(type = FieldType.Keyword)
    private String categoryId; // 分类ID

    @Field(type = FieldType.Keyword)
    private String categoryName; // 分类名称

    @Field(type = FieldType.Keyword)
    private String brandId; // 品牌ID

    @Field(type = FieldType.Keyword)
    private String brandName; // 品牌名称

    @Field(type = FieldType.Keyword)
    private String status; // 商品状态

    @Field(type = FieldType.Nested)
    private List<Attr> attrs; // 商品属性

    @Field(type = FieldType.Nested)
    private List<Sku> skus; // sku规格

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Date createdAt; // 创建时间

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Date updatedAt; // 更新时间

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String searchKeywords; // 搜索关键词

    @Field(type = FieldType.Integer)
    private Integer salesCount; // 销量

    @Field(type = FieldType.Float)
    private Float rating; // 评分

    // 嵌套属性类
    @Data
    public static class Attr {

        @Field(type = FieldType.Keyword)
        private String key; // 商品属性名

        @Field(type = FieldType.Keyword)
        private String value; // 商品属性值

    }

    // 嵌套规格类
    @Data
    public static class Sku {
        @Field(type = FieldType.Integer)
        private Integer skuId; // SKU ID

        @Field(type = FieldType.Float)
        private Double price; // SKU 价格

        @Field(type = FieldType.Integer)
        private Integer integral;  // SKU 积分

        @Field(type = FieldType.Float)
        private Double originalPrice; // SKU 原价

        @Field(type = FieldType.Integer)
        private Integer stock; // SKU 库存

        @Field(type = FieldType.Boolean)
        private Boolean hasStock; // SKU 是否有库存

        @Field(type = FieldType.Nested)
        private List<Attr> attrs; // SKU 属性

    }


}
