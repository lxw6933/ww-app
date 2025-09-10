package com.ww.mall.product.dto.spu;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2025-09-10 16:17
 * @description:
 */
@Data
public class ProductSpuDTO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "商品编码")
    private String spuCode;

    @Schema(description = "商品名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

//    @Schema(description = "商品类型【虚拟、实物】", requiredMode = Schema.RequiredMode.REQUIRED)
//    private SpuType spuType;

    @Schema(description = "商品分类", requiredMode = Schema.RequiredMode.REQUIRED)
    private String categoryName;

    @Schema(description = "商品品牌", requiredMode = Schema.RequiredMode.REQUIRED)
    private String brandName;

    @Schema(description = "商品分类编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryId;

    @Schema(description = "商品品牌编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long brandId;

    @Schema(description = "商品封面图", requiredMode = Schema.RequiredMode.REQUIRED)
    private String img;

    @Schema(description = "商品轮播图", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> sliderImgList;

    @Schema(description = "商品简介", requiredMode = Schema.RequiredMode.REQUIRED)
    private String introduction;

    @Schema(description = "规格类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean specType;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private Date createTime;

}
