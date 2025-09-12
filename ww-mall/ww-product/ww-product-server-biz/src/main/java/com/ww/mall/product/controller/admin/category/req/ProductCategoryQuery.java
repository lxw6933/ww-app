package com.ww.mall.product.controller.admin.category.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ww
 * @create 2023-07-29- 11:24
 * @description:
 */
@Data
public class ProductCategoryQuery {

    @Schema(description = "类目名称")
    private String name;

}
