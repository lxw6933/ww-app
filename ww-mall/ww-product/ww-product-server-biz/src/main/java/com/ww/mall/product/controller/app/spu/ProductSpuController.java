package com.ww.mall.product.controller.app.spu;

import com.ww.mall.product.controller.app.spu.res.AppProductSpuDetailVO;
import com.ww.mall.product.service.spu.ProductSpuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2023-07-29- 11:21
 * @description:
 */
@RestController
@Tag(name = "APP - 商品")
@RequestMapping("/product/spu")
public class ProductSpuController {

    @Resource
    private ProductSpuService productSpuService;

    @GetMapping("/detail")
    @Operation(summary = "商品详情")
    @Parameter(name = "id", description = "编号", required = true)
    public AppProductSpuDetailVO detail(@RequestParam("id") Long id) {
        return productSpuService.detail(id);
    }

}
