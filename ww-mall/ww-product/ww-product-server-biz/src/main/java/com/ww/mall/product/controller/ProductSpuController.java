package com.ww.mall.product.controller;

import com.ww.app.common.common.AppPageResult;
import com.ww.mall.product.service.spu.ProductSpuService;
import com.ww.mall.product.view.bo.ProductSpuBO;
import com.ww.mall.product.view.bo.ProductSpuStatusBO;
import com.ww.mall.product.view.query.ProductSpuPageQuery;
import com.ww.mall.product.view.vo.ProductSpuPageAdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2023-07-29- 11:21
 * @description:
 */
@RestController
@Tag(name = "管理后台 - 商品")
@RequestMapping("/product/spu")
public class ProductSpuController {

    @Resource
    private ProductSpuService productSpuService;

    @GetMapping("/page")
    @Operation(summary = "获得商品分页列表[后台]")
    public AppPageResult<ProductSpuPageAdminVO> page(ProductSpuPageQuery productSpuPageQuery) {
        return productSpuService.page(productSpuPageQuery);
    }

    @PostMapping
    @Operation(summary = "新增商品")
    public boolean add(@RequestBody @Validated ProductSpuBO productSpuBO) {
        return productSpuService.add(productSpuBO);
    }

    @PutMapping
    @Operation(summary = "编辑商品")
    public boolean update(@RequestBody @Validated ProductSpuBO productSpuBO) {
        return productSpuService.update(productSpuBO);
    }

    @PutMapping("/status")
    @Operation(summary = "更新商品状态")
    public boolean updateStatus(@RequestBody @Validated ProductSpuStatusBO productSpuStatusBO) {
        productSpuService.updateSpuStatus(productSpuStatusBO);
        return true;
    }

}
