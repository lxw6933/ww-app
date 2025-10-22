package com.ww.app.cart.service.impl;

import cn.hutool.core.lang.Assert;
import com.ww.app.cart.component.key.CartRedisKeyBuilder;
import com.ww.app.cart.entity.Cart;
import com.ww.app.cart.entity.CartItem;
import com.ww.app.cart.service.HashCartService;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.exception.ApiException;
import org.apache.commons.collections4.CollectionUtils;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static com.ww.app.cart.constant.CartConstant.MAX_CART_NUMBER;

/**
 * @author ww
 * @create 2024-04-08- 09:35
 * @description:
 */
@Service
public class HashCartServiceImpl implements HashCartService {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private CartRedisKeyBuilder cartRedisKeyBuilder;

    @Override
    public CartItem addToCart(Long skuId, Integer num) {
        RMap<String, CartItem> userCart = getUserCart();
        // 是否达到购物车最大容量
        Assert.isTrue(userCart.size() < MAX_CART_NUMBER, () -> new ApiException("超出购物车最大容量"));
        // 判断购物车是否存在当前商品
        CartItem cartItem = userCart.get(skuId.toString());
        if (cartItem == null) {
            cartItem = new CartItem();
            // TODO: 2023/7/17 远程查询商品信息
            cartItem.setSkuId(skuId);
            cartItem.setCount(num);
            cartItem.setChecked(true);
            cartItem.setPrice(100L); // 设置为1元（100分）
        } else {
            cartItem.setCount(cartItem.getCount() + num);
        }
        userCart.put(skuId.toString(), cartItem);
        return cartItem;
    }

    @Override
    public Cart userCartList() {
        Cart cart = new Cart();
        cart.setCartItems(getUserCartItemList());
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
        Assert.notNull(userCartItem, () -> new ApiException("商品不存在"));
        userCartItem.setChecked(!userCartItem.isChecked());
        userCart.put(skuId.toString(), userCartItem);
        return true;
    }

    @Override
    public boolean modifyItemCount(Long skuId, Integer num) {
        RMap<String, CartItem> userCart = getUserCart();
        CartItem userCartItem = userCart.get(skuId.toString());
        Assert.notNull(userCartItem, () -> new ApiException("商品不存在"));
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
        ClientUser clientUser = AuthorizationContext.getClientUser();
        String userCartKey = cartRedisKeyBuilder.buildUserCartKey(clientUser.getId());
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

}
