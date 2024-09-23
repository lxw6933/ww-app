package com.ww.mall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.ww.mall.cart.entity.Cart;
import com.ww.mall.cart.entity.CartItem;
import com.ww.mall.cart.interceptor.CartInterceptor;
import com.ww.mall.cart.service.CartService;
import com.ww.mall.cart.to.UserInfoTo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static com.ww.mall.cart.constant.CartConstant.CART_PREFIX;
import static com.ww.mall.common.utils.CollectionUtils.convertList;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/17 20:18
 **/
@Service
public class CartServiceImpl implements CartService {

//    @Autowired
//    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, String> stringRedisTemplate;

    @Autowired
    private ThreadPoolExecutor defaultThreadPoolExecutor;

    @Override
    public CartItem addToCart(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> userCart = getUserCart();
        // 判断购物车是否存在当前商品
        String res = (String) userCart.get(skuId.toString());
        if (StringUtils.isEmpty(res)) {
            CartItem cartItem = new CartItem();
            // TODO: 2023/7/17 远程查询商品信息
            cartItem.setSkuId(skuId);
            cartItem.setCount(num);
            cartItem.setChecked(true);
            cartItem.setPrice(BigDecimal.ONE);
            userCart.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        } else {
            CartItem cartItem = JSON.parseObject(res, CartItem.class);
            cartItem.setCount(cartItem.getCount() + num);
            userCart.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        }
    }

    @Override
    public Cart userCartList() {
        Cart cart = new Cart();
        UserInfoTo userInfoTo = CartInterceptor.cartThreadLocal.get();
        String tempUserCartKey = CART_PREFIX + userInfoTo.getTempUserKey();
        if (userInfoTo.getUserId() != null) {
            String userCartKey = CART_PREFIX + userInfoTo.getUserId();
            List<CartItem> tempUserCartList = getUserCartItemList(tempUserCartKey);
            if (CollectionUtils.isNotEmpty(tempUserCartList)) {
                // 合并到当前登录用户购物车
                tempUserCartList.forEach(tempCartItem -> this.addToCart(tempCartItem.getSkuId(), tempCartItem.getCount()));
                // 清空临时用户购物车数据
                clearCart(tempUserCartKey);
            }
            // 获取用户购物车商品数据
            cart.setCartItems(getUserCartItemList(userCartKey));
            return cart;
        } else {
            // 临时购物车商品数据
            cart.setCartItems(getUserCartItemList(tempUserCartKey));
            return cart;
        }
    }

    @Override
    public boolean clearUserCart() {
        UserInfoTo userInfoTo = CartInterceptor.cartThreadLocal.get();
        String userCartKey;
        if (userInfoTo.getUserId() != null) {
            userCartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            userCartKey = CART_PREFIX + userInfoTo.getTempUserKey();
        }
        clearCart(userCartKey);
        return true;
    }

    @Override
    public boolean checkItem(Long skuId) {
        BoundHashOperations<String, Object, Object> userCart = getUserCart();
        CartItem cartItem = getUserCartItem(skuId);
        cartItem.setChecked(!cartItem.getChecked());
        userCart.put(skuId.toString(), JSON.toJSONString(cartItem));
        return true;
    }

    @Override
    public boolean modifyItemCount(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> userCart = getUserCart();
        CartItem cartItem = getUserCartItem(skuId);
        cartItem.setCount(num);
        userCart.put(skuId.toString(), JSON.toJSONString(cartItem));
        return true;
    }

    @Override
    public boolean deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> userCart = getUserCart();
        userCart.delete(skuId.toString());
        return true;
    }

    private void clearCart(String userCartKey) {
        stringRedisTemplate.delete(userCartKey);
    }

    /**
     * 获取用户购物车商品明细
     * @param userCartKey key
     * @return List<CartItem>
     */
    private List<CartItem> getUserCartItemList(String userCartKey) {
        List<CartItem> userCartList = new ArrayList<>();
        BoundHashOperations<String, Object, Object> userCart = getUserCart();
        if (CollectionUtils.isNotEmpty(userCart.values())) {
            userCartList = convertList(userCart.values(), res -> JSON.parseObject(res.toString(), CartItem.class));
        }
        return userCartList;
    }

    private CartItem getUserCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> userCart = getUserCart();
        String item = (String) userCart.get(skuId.toString());
        return JSON.parseObject(item, CartItem.class);
    }

    /**
     * 获取用户购物车
     */
    private BoundHashOperations<String, Object, Object> getUserCart() {
        UserInfoTo userInfoTo = CartInterceptor.cartThreadLocal.get();
        String userCartKey;
        if (userInfoTo.getUserId() != null) {
            userCartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            userCartKey = CART_PREFIX + userInfoTo.getTempUserKey();
        }
        return stringRedisTemplate.boundHashOps(userCartKey);
    }

}
