package com.ww.app.cart.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 添加购物车请求
 *
 * @author ww
 * @date 2025-11-05
 */
@Data
@Schema(description = "添加购物车请求")
public class AddCartRequest {

    @Schema(description = "商品SKU ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @Schema(description = "添加数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @Max(value = 100, message = "数量不能超过100")
    private Integer num;
}
