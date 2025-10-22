package com.ww.mall.product.controller.app.spu.res;

import com.ww.mall.product.entity.sku.ProductSku;
import com.ww.mall.product.enums.SpuType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author ww
 * @create 2025-09-12 16:12
 * @description:
 */
@Data
public class AppProductSpuDetailVO {

    @Schema(description = "商品 SPU 编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    // ========== 基本信息 =========

    @Schema(description = "商品名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "商品类型【虚拟、实物】", requiredMode = Schema.RequiredMode.REQUIRED)
    private SpuType spuType;

    @Schema(description = "商品简介", requiredMode = Schema.RequiredMode.REQUIRED)
    private String introduction;

    @Schema(description = "商品详情", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @Schema(description = "商品分类编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryId;

    @Schema(description = "商品品牌编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long brandId;

    @Schema(description = "商品封面图", requiredMode = Schema.RequiredMode.REQUIRED)
    private String img;

    @Schema(description = "商品轮播图", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> sliderImgList;

    // ========== 营销相关字段 =========

    // ========== 统计相关字段 =========

    @Schema(description = "商品销量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer salesCount;

    // ========== SKU 相关字段 =========
    /**
     * SKU 数组
     */
    private List<Sku> skus;

    @Data
    @Schema(description = "用户 App - 商品 SPU 明细的 SKU 信息")
    public static class Sku {

        @Schema(description = "商品 SKU 编号")
        private Long id;

        @Schema(description = "商品属性数组", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<ProductSku.Property> properties;

        @Schema(description = "销售价格，单位：分", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long price;

        @Schema(description = "市场价，单位使用：分", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long marketPrice;

        @Schema(description = "图片地址", requiredMode = Schema.RequiredMode.REQUIRED)
        private String img;

        @Schema(description = "库存", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer stock;

    }

}
