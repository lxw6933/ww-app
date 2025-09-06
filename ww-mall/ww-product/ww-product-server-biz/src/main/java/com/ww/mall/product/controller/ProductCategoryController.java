package com.ww.mall.product.controller;

import com.ww.mall.product.service.category.ProductCategoryService;
import com.ww.mall.product.view.bo.ProductCategoryBO;
import com.ww.mall.product.view.query.ProductCategoryQuery;
import com.ww.mall.product.view.vo.ProductCategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ww
 * @create 2023-07-29- 11:21
 * @description:
 */
@RestController
@Tag(name = "管理后台 - 商品分类")
@RequestMapping("/product/category")
public class ProductCategoryController {

    @Resource
    private ProductCategoryService productCategoryService;

    @GetMapping("/tree")
    @Operation(summary = "获得商品分类列表")
    public List<ProductCategoryVO> tree(ProductCategoryQuery productCategoryQuery) {
        return productCategoryService.listCategoryTree(productCategoryQuery);
    }

    @PostMapping
    @Operation(summary = "新增商品分类")
    public boolean add(@RequestBody @Validated ProductCategoryBO productCategoryBO) {
        return productCategoryService.add(productCategoryBO);
    }

    @PutMapping
    @Operation(summary = "编辑商品分类")
    public boolean update(@RequestBody @Validated ProductCategoryBO productCategoryBO) {
        return productCategoryService.update(productCategoryBO);
    }

}
