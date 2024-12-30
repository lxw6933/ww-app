package com.ww.app.cart.service;

import com.ww.app.cart.entity.Cart;
import com.ww.app.cart.entity.CartItem;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/17 20:18
 **/
public interface CartService {

    /**
     * 添加购物车
     *
     * @param skuId 规格id
     * @param num 数量
     * @return cartItem
     */
    CartItem addToCart(Long skuId, Integer num);

    /**
     * 获取用户购物车列表
     *
     * @return Cart
     */
    Cart userCartList();

    /**
     * 清空用户购物车
     *
     * @return boolean
     */
    boolean clearUserCart();

    /**
     * 勾选购物项
     *
     * @param skuId skuId
     * @return boolean
     */
    boolean checkItem(Long skuId);

    /**
     * 修改购物车商品数量
     *
     * @param skuId 规格id
     * @param num 数量
     * @return boolean
     */
    boolean modifyItemCount(Long skuId, Integer num);

    /**
     * 删除购物项
     *
     * @param skuId skuId
     * @return boolean
     */
    boolean deleteItem(Long skuId);
}
