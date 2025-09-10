package com.ww.mall.product.view.bo;

import com.ww.mall.product.enums.SpuStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2025-09-10 15:44
 * @description:
 */
@Data
public class ProductSpuStatusBO {

    @Schema(description = "商品编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品编号不能为空")
    private Long id;

    @Schema(description = "商品状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品状态不能为空")
    private SpuStatus status;

}
