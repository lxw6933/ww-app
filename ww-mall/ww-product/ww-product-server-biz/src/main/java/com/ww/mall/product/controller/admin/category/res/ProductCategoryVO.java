package com.ww.mall.product.controller.admin.category.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2023-07-29- 11:24
 * @description:
 */
@Data
public class ProductCategoryVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "父分类id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long parentId;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sort;

    @Schema(description = "图标地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String icon;

    @Schema(description = "开启状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private Date createTime;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Schema(description = "子类集合", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ProductCategoryVO> children;

}
