package com.ww.app.cart.controller;

import com.ww.app.cart.entity.Cart;
import com.ww.app.cart.service.HashCartService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/17 20:43
 **/
@Validated
@RestController
@RequestMapping("/cart")
public class CartController {

    @Resource
    private HashCartService cartService;

    @GetMapping("/userCart")
    public Cart userCart() {
        return cartService.userCartList();
    }

    @GetMapping("/clearUserCart")
    public boolean clearUserCart() {
        return cartService.clearUserCart();
    }

    @GetMapping("/addToCart")
    public boolean addCart(@RequestParam("skuId") Long skuId) {
        return cartService.addToCart(skuId, 1);
    }

    @GetMapping("/checkItem")
    public boolean checkItem(@RequestParam("skuId") Long skuId) {
        return cartService.checkItem(skuId);
    }

    @GetMapping("/modifyItemCount")
    public boolean modifyItemCount(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num) {
        return cartService.modifyItemCount(skuId, num);
    }

    @GetMapping("/deleteItem")
    public boolean deleteItem(@RequestParam("skuId") Long skuId) {
        return cartService.deleteItem(skuId);
    }

}
