package com.ww.app.cart.controller;

import com.ww.app.cart.dto.request.AddCartRequest;
import com.ww.app.cart.dto.request.BatchDeleteRequest;
import com.ww.app.cart.dto.request.UpdateCountRequest;
import com.ww.app.cart.entity.Cart;
import com.ww.app.cart.service.HashCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 购物车控制器
 *
 * @author ww
 * @date 2023-07-17
 */
@Validated
@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "购物车管理", description = "购物车相关接口")
public class CartController {

    @Resource
    private HashCartService cartService;

    @Operation(summary = "获取购物车", description = "查询当前用户的购物车信息")
    @GetMapping
    public Cart getCart() {
        return cartService.userCartList();
    }

    @Operation(summary = "添加商品到购物车", description = "将指定商品添加到购物车，如果已存在则累加数量")
    @PostMapping("/items")
    public Boolean addToCart(@Valid @RequestBody AddCartRequest request) {
        return cartService.addToCart(request.getSkuId(), request.getNum());
    }

    @Operation(summary = "修改商品数量", description = "修改购物车中指定商品的数量")
    @PutMapping("/items/{skuId}/count")
    public Boolean updateItemCount(
            @Parameter(description = "SKU ID", required = true)
            @PathVariable @NotNull(message = "SKU ID不能为空") Long skuId,
            @Valid @RequestBody UpdateCountRequest request) {
        return cartService.modifyItemCount(skuId, request.getNum());
    }

    @Operation(summary = "勾选/取消勾选商品", description = "切换购物车商品的选中状态")
    @PutMapping("/items/{skuId}/check")
    public Boolean checkItem(
            @Parameter(description = "SKU ID", required = true)
            @PathVariable @NotNull(message = "SKU ID不能为空") Long skuId) {
        return cartService.checkItem(skuId);
    }

    @Operation(summary = "删除单个商品", description = "从购物车中删除指定商品")
    @DeleteMapping("/items/{skuId}")
    public Boolean deleteItem(
            @Parameter(description = "SKU ID", required = true)
            @PathVariable @NotNull(message = "SKU ID不能为空") Long skuId) {
        return cartService.deleteItem(skuId);
    }

    @Operation(summary = "批量删除商品", description = "从购物车中批量删除指定的多个商品")
    @DeleteMapping("/items/batch")
    public Boolean batchDeleteItems(@Valid @RequestBody BatchDeleteRequest request) {
        return cartService.batchDeleteItem(request.getSkuIdList());
    }

    @Operation(summary = "清空购物车", description = "清空当前用户的购物车所有商品")
    @DeleteMapping
    public Boolean clearCart() {
        return cartService.clearUserCart();
    }

    @Operation(summary = "获取缓存统计", description = "获取购物车缓存命中率等统计信息")
    @GetMapping("/cache/stats")
    public String getCacheStats() {
        return cartService.getCacheStats();
    }
}
