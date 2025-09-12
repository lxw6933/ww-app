package com.ww.mall.product.controller.admin.property.req;

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
public class ProductPropertyValueBO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "属性名id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "属性名id不能为空")
    private Long propertyId;

    @Schema(description = "属性值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "属性值不能为空")
    private String name;

}
