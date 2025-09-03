package com.ww.mall.product.entity.spu;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ww.app.mybatis.common.BaseEntity;
import com.ww.mall.product.entity.brand.ProductBrand;
import com.ww.mall.product.entity.category.ProductCategory;
import com.ww.mall.product.enums.SpuStatus;
import com.ww.mall.product.enums.SpuType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author ww
 * @create 2025-09-03- 09:26
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("product_spu")
public class ProductSpu extends BaseEntity {

    /**
     * 商品名称
     */
    private String name;

    /**
     * 关键字
     */
    private String keyword;

    /**
     * 商品编码
     */
    private String spuCode;

    /**
     * 商品类型【虚拟、实物】
     */
    private SpuType spuType;

    /**
     * 商品分类编号
     * </p>
     * 关联 {@link ProductCategory#getId()}
     */
    private Long categoryId;

    /**
     * 商品品牌编号
     * </p>
     * 关联 {@link ProductBrand#getId()}
     */
    private Long brandId;

    /**
     * 商品封面图
     */
    private String img;

    /**
     * 商品轮播图
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> sliderImgList;

    /**
     * 商品简介
     */
    private String introduction;

    /**
     * 商品详情
     */
    private String description;

    /**
     * 商品状态
     * </p>
     * 枚举 {@link SpuStatus}
     */
    private SpuStatus status;

    /**
     * 规格类型
     * </p>
     * false - 单规格
     * true - 多规格
     */
    private Boolean specType;

    /**
     * 运费模板【默认：1 包邮】
     */
    private Long deliveryTemplateId;

    /**
     * 商品销量
     */
    private Integer salesCount;

    /**
     * 浏览量
     */
    private Integer browseCount;

}
