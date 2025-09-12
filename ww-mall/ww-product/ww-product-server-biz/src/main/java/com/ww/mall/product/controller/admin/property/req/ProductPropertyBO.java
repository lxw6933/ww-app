package com.ww.mall.product.controller.admin.property.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author ww
 * @create 2023-07-29- 11:24
 * @description:
 */
@Data
public class ProductPropertyBO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "属性名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "属性名不能为空")
    private String name;

}
