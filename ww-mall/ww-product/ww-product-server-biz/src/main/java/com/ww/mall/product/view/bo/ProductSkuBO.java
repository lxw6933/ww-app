package com.ww.mall.product.view.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author ww
 * @create 2025-09-09 16:06
 * @description:
 */
@Data
public class ProductSkuBO {

    @Schema(description = "商品 SKU 名字", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "商品 SKU 名字不能为空")
    private String name;

    @Schema(description = "销售价格，单位：分", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "销售价格，单位：分不能为空")
    private Integer price;

    @Schema(description = "市场价")
    private Integer marketPrice;

    @Schema(description = "成本价")
    private Integer costPrice;

    @Schema(description = "条形码")
    private String barCode;

    @Schema(description = "图片地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "图片地址不能为空")
    private String img;

    @Schema(description = "库存", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "库存不能为空")
    private Integer stock;

    @Schema(description = "属性数组")
    private List<Property> properties;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "商品属性")
    public static class Property {

        @Schema(description = "属性名id")
        private Long propertyId;

        @Schema(description = "属性名")
        private String propertyName;

        @Schema(description = "属性值id")
        private Long valueId;

        @Schema(description = "属性值")
        private String valueName;

    }

}
