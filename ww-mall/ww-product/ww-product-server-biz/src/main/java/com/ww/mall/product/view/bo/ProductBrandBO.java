package com.ww.mall.product.view.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2023-07-29- 11:24
 * @description:
 */
@Data
public class ProductBrandBO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "品牌名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "品牌名称不能为空")
    private String name;

    @Schema(description = "品牌图片地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "品牌图片地址不能为空")
    private String img;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "开启状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开启状态不能为空")
    private Boolean status;

    @Schema(description = "品牌描述")
    private String description;

}
