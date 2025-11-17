package com.ww.mall.product.entity.sku;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ww.app.mybatis.common.BaseEntity;
import com.ww.mall.product.entity.property.ProductProperty;
import com.ww.mall.product.entity.property.ProductPropertyValue;
import com.ww.mall.product.entity.spu.ProductSpu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author ww
 * @create 2025-09-03- 09:26
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "product_sku", autoResultMap = true)
public class ProductSku extends BaseEntity {

    /**
     * SPU 编号
     * <p>
     * 关联 {@link ProductSpu#getId()}
     */
    private Long spuId;

    /**
     * 属性数组，JSON 格式
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Property> properties;

    /**
     * 商品价格，单位：分
     */
    private Long price;

    /**
     * 市场价，单位：分
     */
    private Long marketPrice;

    /**
     * 成本价，单位：分
     */
    private Long costPrice;

    /**
     * 图片地址
     */
    private String img;

    /**
     * 条形码
     */
    private String barCode;

    /**
     * 库存
     */
    private Integer stock;

    // ========== 营销相关字段 =========

    // ========== 统计相关字段 =========

    /**
     * 商品属性
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "商品SKU属性")
    public static class Property {

        /**
         * 属性编号
         * 关联 {@link ProductProperty#getId()}
         */
        @Schema(description = "属性的编号", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long propertyId;

        /**
         * 属性名字
         * 冗余 {@link ProductProperty#getName()}
         * <p>
         * 注意：每次属性名字发生变化时，需要更新该冗余
         */
        @Schema(description = "属性的名称", requiredMode = Schema.RequiredMode.REQUIRED)
        private String propertyName;

        /**
         * 属性值编号
         * 关联 {@link ProductPropertyValue#getId()}
         */
        @Schema(description = "属性值的编号", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long valueId;

        /**
         * 属性值名字
         * 冗余 {@link ProductPropertyValue#getName()}
         * <p>
         * 注意：每次属性值名字发生变化时，需要更新该冗余
         */
        @Schema(description = "属性值的名称", requiredMode = Schema.RequiredMode.REQUIRED)
        private String valueName;

    }

}
