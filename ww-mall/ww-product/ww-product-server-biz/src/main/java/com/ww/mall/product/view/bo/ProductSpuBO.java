package com.ww.mall.product.view.bo;

import com.ww.mall.product.enums.SpuType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author ww
 * @create 2025-09-09 16:06
 * @description:
 */
@Data
public class ProductSpuBO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "商品名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "商品名称不能为空")
    private String name;

    @Schema(description = "关键字", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "关键字不能为空")
    private String keyword;

    @Schema(description = "商品类型【虚拟、实物】", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品类型不能为空")
    private SpuType spuType;

    @Schema(description = "商品分类编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品分类不能为空")
    private Long categoryId;

    @Schema(description = "商品品牌编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品品牌不能为空")
    private Long brandId;

    @Schema(description = "商品封面图", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "商品封面图不能为空")
    private String img;

    @Schema(description = "商品轮播图", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> sliderImgList;

    @Schema(description = "商品简介", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "商品简介不能为空")
    private String introduction;

    @Schema(description = "商品详情", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "商品详情不能为空")
    private String description;

    // ========== SKU 相关字段 =========

    @Schema(description = "规格类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品规格类型不能为空")
    private Boolean specType;

    // ========== 物流相关字段 =========

    @Schema(description = "运费模板【默认：1 包邮】", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long deliveryTemplateId;

    // ========== SKU 相关字段 =========

    @Valid
    @Schema(description = "SKU 数组")
    private List<ProductSkuBO> skus;

}
