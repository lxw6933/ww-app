package com.ww.app.cart.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 批量删除购物车商品请求
 *
 * @author ww
 * @date 2025-11-05
 */
@Data
@Schema(description = "批量删除请求")
public class BatchDeleteRequest {

    @Schema(description = "SKU ID列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1001, 1002, 1003]")
    @NotNull(message = "SKU ID列表不能为空")
    @Size(min = 1, max = 50, message = "一次最多删除50个商品")
    private List<Long> skuIdList;
}
