package com.ww.mall.product.controller.admin.brand.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ww
 * @create 2025-09-06 16:36
 * @description:
 */
@Data
public class ProductBrandVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "品牌名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "品牌图片地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String img;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sort;

    @Schema(description = "开启状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean status;

    @Schema(description = "品牌描述")
    private String description;

}
