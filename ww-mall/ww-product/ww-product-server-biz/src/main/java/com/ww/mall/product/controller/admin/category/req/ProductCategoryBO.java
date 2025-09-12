package com.ww.mall.product.controller.admin.category.req;

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
public class ProductCategoryBO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分类名称不能为空")
    private String name;

    @Schema(description = "父分类id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "父分类id不能为空")
    private Long parentId;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "图标地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "移动端分类图不能为空")
    private String icon;

    @Schema(description = "开启状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开启状态不能为空")
    private Boolean status;

}
