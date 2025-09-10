package com.ww.mall.product.rpc;

import com.ww.app.common.common.Result;
import com.ww.mall.product.constants.ApiConstants;
import com.ww.mall.product.dto.spu.ProductSpuDTO;
import com.ww.mall.product.fallback.ProductSpuApiFallBack;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author ww
 * @create 2025-09-10 16:05
 * @description:
 */
@Tag(name = "RPC 服务 - 商品 SPU")
@FeignClient(value = ApiConstants.NAME, fallbackFactory = ProductSpuApiFallBack.class)
public interface ProductSpuApi {

    String PREFIX = ApiConstants.PREFIX + "/spu";

    @GetMapping(PREFIX + "/get")
    @Schema(description = "获得 SPU")
    @Parameter(name = "id", description = "SPU 编号", required = true)
    Result<ProductSpuDTO> getSpu(@RequestParam("id") Long id);

}
