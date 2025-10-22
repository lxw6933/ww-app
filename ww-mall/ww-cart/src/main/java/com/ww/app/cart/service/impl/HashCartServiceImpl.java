package com.ww.app.cart.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import com.ww.app.cart.component.key.CartRedisKeyBuilder;
import com.ww.app.cart.entity.Cart;
import com.ww.app.cart.entity.CartItem;
import com.ww.app.cart.service.HashCartService;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.exception.ApiException;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.ww.app.cart.constant.CartConstant.*;

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
    public boolean addToCart(Long skuId, Integer num) {
        RMap<String, CartItem> userCart = getUserCart();
        // 是否达到购物车最大容量
        Assert.isTrue(userCart.size() < MAX_CART_NUMBER, () -> new ApiException("超出购物车最大容量"));
        // 判断购物车是否存在当前商品
        userCart.compute(skuId.toString(), (key, oldItem) -> {
            if (oldItem == null) {
                CartItem newItem = new CartItem();
                newItem.setSkuId(skuId);
                newItem.setCount(num);
                newItem.setChecked(true);
                // 模拟远程商品信息
                newItem.setPrice(100L); // 1元
                newItem.setTitle("测试商品");
                return newItem;
            } else {
                oldItem.setCount(oldItem.getCount() + num);
                return oldItem;
            }
        });
        resetExpire(userCart);
        return true;
    }

    @Override
    public Cart userCartList() {
        Cart cart = new Cart();
        cart.setCartItems(getUserCartItemList());
        cart.recalcTotals();
        return cart;
    }

    @Override
    public boolean clearUserCart() {
        ClientUser clientUser = AuthorizationContext.getClientUser();
        String userCartKey = cartRedisKeyBuilder.buildUserCartKey(clientUser.getId());
        redissonClient.getKeys().delete(userCartKey);
        return true;
    }

    @Override
    public boolean checkItem(Long skuId) {
        RMap<String, CartItem> userCart = getUserCart();
        CartItem userCartItem = userCart.get(skuId.toString());
        Assert.notNull(userCartItem, () -> new ApiException("商品不存在"));
        userCartItem.setChecked(!userCartItem.isChecked());
        userCart.fastPut(skuId.toString(), userCartItem);
        resetExpire(userCart);
        return true;
    }

    @Override
    public boolean modifyItemCount(Long skuId, Integer num) {
        Assert.isTrue(num > 0 && num < MAX_ADD_NUMBER, () -> new ApiException("数量必须大于0且不能超过" + MAX_ADD_NUMBER));
        RMap<String, CartItem> userCart = getUserCart();
        userCart.computeIfPresent(skuId.toString(), (k, cartItem) -> {
            cartItem.setCount(num);
            return cartItem;
        });
        resetExpire(userCart);
        return true;
    }

    @Override
    public boolean deleteItem(Long skuId) {
        RMap<String, CartItem> userCart = getUserCart();
        userCart.fastRemove(skuId.toString());
        resetExpire(userCart);
        return true;
    }

    @Override
    public boolean batchDeleteItem(List<Long> skuIdList) {
        if (CollUtil.isEmpty(skuIdList)) {
            return false;
        }
        RMap<String, CartItem> userCart = getUserCart();
        userCart.fastRemove(skuIdList.stream().map(String::valueOf).toArray(String[]::new));
        resetExpire(userCart);
        return true;
    }

    /**
     * 获取用户购物车 Map
     */
    private RMap<String, CartItem> getUserCart() {
        ClientUser clientUser = AuthorizationContext.getClientUser();
        String userCartKey = cartRedisKeyBuilder.buildUserCartKey(clientUser.getId());
        RMap<String, CartItem> userCart = redissonClient.getMap(userCartKey);
        resetExpire(userCart);
        return userCart;
    }

    /**
     * 获取用户购物车列表
     */
    private List<CartItem> getUserCartItemList() {
        RMap<String, CartItem> userCart = getUserCart();
        Collection<CartItem> values = userCart.readAllValues();
        if (CollUtil.isEmpty(values)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(values);
    }

    /**
     * 重置购物车过期时间（防止活跃用户被清空）
     */
    private void resetExpire(RMap<String, CartItem> userCart) {
        if (userCart.remainTimeToLive() < TimeUnit.DAYS.toMillis(CART_TTL_DAYS / 2)) {
            userCart.expire(Duration.ofDays(CART_TTL_DAYS));
        }
    }

}
