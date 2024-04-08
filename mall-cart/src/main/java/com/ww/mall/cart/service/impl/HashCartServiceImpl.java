package com.ww.mall.cart.service.impl;

import cn.hutool.core.lang.Assert;
import com.ww.mall.cart.entity.Cart;
import com.ww.mall.cart.entity.CartItem;
import com.ww.mall.cart.interceptor.CartInterceptor;
import com.ww.mall.cart.service.HashCartService;
import com.ww.mall.cart.to.UserInfoTo;
import org.apache.commons.collections4.CollectionUtils;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.ww.mall.cart.constant.CartConstant.CART_PREFIX;

/**
 * @author ww
 * @create 2024-04-08- 09:35
 * @description:
 */
@Service
public class HashCartServiceImpl implements HashCartService {

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public CartItem addToCart(Long skuId, Integer num) {
        RMap<String, CartItem> userCart = getUserCart();
        // 判断购物车是否存在当前商品
        CartItem cartItem = userCart.get(skuId.toString());
        if (cartItem == null) {
            cartItem = new CartItem();
            // TODO: 2023/7/17 远程查询商品信息
            cartItem.setSkuId(skuId);
            cartItem.setCount(num);
            cartItem.setChecked(true);
            cartItem.setPrice(BigDecimal.ONE);
        } else {
            cartItem.setCount(cartItem.getCount() + num);
        }
        userCart.put(skuId.toString(), cartItem);
        return cartItem;
    }

    @Override
    public Cart userCartList() {
        Cart cart = new Cart();
        UserInfoTo userInfoTo = CartInterceptor.cartThreadLocal.get();
        if (userInfoTo.getUserId() != null) {
            // 获取临时用户购物车数据
            String tempUserCartKey = CART_PREFIX + userInfoTo.getTempUserKey();
            List<CartItem> tempUserCartList = getUserCartItemList(tempUserCartKey);
            if (CollectionUtils.isNotEmpty(tempUserCartList)) {
                // 合并到当前登录用户购物车
                tempUserCartList.forEach(tempCartItem -> this.addToCart(tempCartItem.getSkuId(), tempCartItem.getCount()));
                // 清空临时用户购物车数据
                redissonClient.getMap(tempUserCartKey).clear();
            }
            // 获取用户购物车商品数据
            cart.setCartItems(getUserCartItemList());
        } else {
            // 临时购物车商品数据
            cart.setCartItems(getUserCartItemList());
        }
        return cart;
    }

    @Override
    public boolean clearUserCart() {
        RMap<String, CartItem> userCart = getUserCart();
        userCart.clear();
        return true;
    }

    @Override
    public boolean checkItem(Long skuId) {
        RMap<String, CartItem> userCart = getUserCart();
        CartItem userCartItem = userCart.get(skuId.toString());
        Assert.isNull(userCartItem);
        userCartItem.setChecked(!userCartItem.getChecked());
        userCart.put(skuId.toString(), userCartItem);
        return true;
    }

    @Override
    public boolean modifyItemCount(Long skuId, Integer num) {
        RMap<String, CartItem> userCart = getUserCart();
        CartItem userCartItem = userCart.get(skuId.toString());
        Assert.isNull(userCartItem);
        if (num < 1) {
            return false;
        }
        userCartItem.setCount(num);
        userCart.put(skuId.toString(), userCartItem);
        return true;
    }

    @Override
    public boolean deleteItem(Long skuId) {
        RMap<String, CartItem> userCart = getUserCart();
        userCart.remove(skuId.toString());
        return true;
    }

    @Override
    public boolean batchDeleteItem(List<Long> skuIdList) {
        RMap<String, CartItem> userCart = getUserCart();
        skuIdList.forEach(skuId -> userCart.remove(skuId.toString()));
        return true;
    }

    private RMap<String, CartItem> getUserCart() {
        UserInfoTo userInfoTo = CartInterceptor.cartThreadLocal.get();
        String userCartKey;
        if (userInfoTo.getUserId() != null) {
            userCartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            userCartKey = CART_PREFIX + userInfoTo.getTempUserKey();
        }
        return redissonClient.getMap(userCartKey);
    }

    private List<CartItem> getUserCartItemList() {
        List<CartItem> userCartList = new ArrayList<>();
        RMap<String, CartItem> userCart = getUserCart();
        if (CollectionUtils.isNotEmpty(userCart.values())) {
            userCartList = new ArrayList<>(userCart.values());
        }
        return userCartList;
    }

    private List<CartItem> getUserCartItemList(String userCartKey) {
        List<CartItem> userCartList = new ArrayList<>();
        RMap<String, CartItem> userCart = redissonClient.getMap(userCartKey);
        if (CollectionUtils.isNotEmpty(userCart.values())) {
            userCartList = new ArrayList<>(userCart.values());
        }
        return userCartList;
    }

    private CartItem getUserCartItem(Long skuId) {
        RMap<String, CartItem> userCart = getUserCart();
        return userCart.get(skuId.toString());
    }
}
