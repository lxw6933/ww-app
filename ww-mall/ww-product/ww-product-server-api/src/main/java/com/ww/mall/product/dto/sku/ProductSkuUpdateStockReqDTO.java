package com.ww.mall.product.dto.sku;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author ww
 * @create 2025-09-10 11:35
 * @description:
 */
@Data
public class ProductSkuUpdateStockReqDTO {

    @Schema(description = "商品 SKU 数组", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品 SKU 不能为空")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "商品 SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "商品 SKU 编号不能为空")
        private Long id;

        @Schema(description = "库存变化数量[正数：增加库存；负数：扣减库存]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "库存变化数量不能为空")
        private Integer incrCount;

    }

}
