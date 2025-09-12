package com.ww.mall.product.controller.admin.brand;

import com.ww.app.common.common.AppPageResult;
import com.ww.mall.product.service.brand.ProductBrandService;
import com.ww.mall.product.controller.admin.brand.req.ProductBrandBO;
import com.ww.mall.product.controller.admin.brand.req.ProductBrandPageQuery;
import com.ww.mall.product.controller.admin.brand.res.ProductBrandVO;
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
@Tag(name = "管理后台 - 商品品牌")
@RequestMapping("/product/brand")
public class ProductBrandController {

    @Resource
    private ProductBrandService productBrandService;

    @GetMapping("/page")
    @Operation(summary = "获得商品品牌分页列表")
    public AppPageResult<ProductBrandVO> page(ProductBrandPageQuery productCategoryQuery) {
        return productBrandService.page(productCategoryQuery);
    }

    @PostMapping
    @Operation(summary = "新增商品品牌")
    public boolean add(@RequestBody @Validated ProductBrandBO productBrandBO) {
        return productBrandService.add(productBrandBO);
    }

    @PutMapping
    @Operation(summary = "编辑商品品牌")
    public boolean update(@RequestBody @Validated ProductBrandBO productBrandBO) {
        return productBrandService.update(productBrandBO);
    }

}
