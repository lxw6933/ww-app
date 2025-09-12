package com.ww.mall.product.controller.app.property.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ww
 * @create 2025-09-12 16:15
 * @description:
 */
@Data
public class AppProductPropertyDetailVO {

    @Schema(description = "属性的编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long propertyId;

    @Schema(description = "属性的名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String propertyName;

    @Schema(description = "属性值的编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long valueId;

    @Schema(description = "属性值的名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String valueName;

}
