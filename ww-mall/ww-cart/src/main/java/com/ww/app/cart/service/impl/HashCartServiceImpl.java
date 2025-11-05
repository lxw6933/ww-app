package com.ww.app.cart.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.ww.app.cart.component.key.CartRedisKeyBuilder;
import com.ww.app.cart.config.CartProperties;
import com.ww.app.cart.entity.Cart;
import com.ww.app.cart.entity.CartItem;
import com.ww.app.cart.service.HashCartService;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 购物车服务实现（优化版）
 *
 * @author ww
 * @date 2024-04-08
 */
@Slf4j
@Service
public class HashCartServiceImpl implements HashCartService {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private CartRedisKeyBuilder cartRedisKeyBuilder;

    @Resource
    private CartProperties cartProperties;

    @Override
    public boolean addToCart(Long skuId, Integer num) {
        log.info("添加购物车: skuId={}, num={}", skuId, num);
        
        RMap<String, CartItem> userCart = getUserCart();
        
        // 检查购物车是否已满
        if (userCart.size() >= cartProperties.getMaxCartNumber()) {
            log.warn("购物车已满: userId={}, size={}", getCurrentUserId(), userCart.size());
            throw new ApiException("超出购物车最大容量");
        }
        
        // 使用 compute 方法确保原子性操作
        userCart.compute(skuId.toString(), (key, oldItem) -> {
            if (oldItem == null) {
                // 新商品：创建购物车项
                log.debug("新增商品到购物车: skuId={}", skuId);
                return CartItem.builder()
                        .skuId(skuId)
                        .count(num)
                        .checked(true)
                        .invalid(false)
                        .addTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        // TODO: 后续集成商品服务获取真实商品信息
                        .price(100L)  // 临时测试数据
                        .title("测试商品")
                        .build();
            } else {
                // 已存在：累加数量
                int newCount = oldItem.getCount() + num;
                if (newCount > cartProperties.getMaxAddNumber()) {
                    log.warn("商品数量超限: skuId={}, oldCount={}, addNum={}", 
                            skuId, oldItem.getCount(), num);
                    throw new ApiException("商品数量不能超过" + cartProperties.getMaxAddNumber());
                }
                log.debug("累加商品数量: skuId={}, oldCount={}, newCount={}", 
                        skuId, oldItem.getCount(), newCount);
                oldItem.setCount(newCount);
                oldItem.setUpdateTime(LocalDateTime.now());
                return oldItem;
            }
        });

        // 按需重置过期时间（性能优化）
        resetExpireIfNeeded(userCart);

        log.info("添加购物车成功: skuId={}, num={}", skuId, num);
        return true;
    }

    @Override
    public Cart userCartList() {
        log.debug("查询购物车: userId={}", getCurrentUserId());
        
        Cart cart = new Cart();
        cart.setCartItems(getUserCartItemList());
        cart.recalcTotals();
        
        log.debug("购物车查询成功: userId={}, itemCount={}", getCurrentUserId(), cart.getCountType());
        return cart;
    }

    @Override
    public boolean clearUserCart() {
        Long userId = getCurrentUserId();
        log.info("清空购物车: userId={}", userId);
        
        String userCartKey = cartRedisKeyBuilder.buildUserCartKey(userId);
        redissonClient.getKeys().delete(userCartKey);
        
        log.info("清空购物车成功: userId={}", userId);
        return true;
    }

    @Override
    public boolean checkItem(Long skuId) {
        log.info("勾选商品: skuId={}", skuId);
        
        RMap<String, CartItem> userCart = getUserCart();
        
        // 使用 compute 方法确保原子性操作（修复并发安全问题）
        userCart.compute(skuId.toString(), (key, item) -> {
            if (item == null) {
                log.warn("商品不存在: skuId={}", skuId);
                throw new ApiException("商品不存在");
            }
            boolean newChecked = !item.isChecked();
            log.debug("切换商品选中状态: skuId={}, oldChecked={}, newChecked={}", 
                    skuId, item.isChecked(), newChecked);
            item.setChecked(newChecked);
            item.setUpdateTime(LocalDateTime.now());
            return item;
        });
        
        resetExpireIfNeeded(userCart);
        
        log.info("勾选商品成功: skuId={}", skuId);
        return true;
    }

    @Override
    public boolean modifyItemCount(Long skuId, Integer num) {
        log.info("修改商品数量: skuId={}, num={}", skuId, num);
        
        // 参数校验
        if (num <= 0 || num > cartProperties.getMaxAddNumber()) {
            log.warn("商品数量不合法: skuId={}, num={}", skuId, num);
            throw new ApiException("数量必须大于0且不能超过" + cartProperties.getMaxAddNumber());
        }
        
        RMap<String, CartItem> userCart = getUserCart();
        
        // 使用 computeIfPresent 确保原子性
        CartItem updatedItem = userCart.computeIfPresent(skuId.toString(), (k, item) -> {
            log.debug("修改数量: skuId={}, oldCount={}, newCount={}", skuId, item.getCount(), num);
            item.setCount(num);
            item.setUpdateTime(LocalDateTime.now());
            return item;
        });
        
        if (updatedItem == null) {
            log.warn("商品不存在: skuId={}", skuId);
            throw new ApiException("商品不存在");
        }
        
        resetExpireIfNeeded(userCart);
        
        log.info("修改商品数量成功: skuId={}, num={}", skuId, num);
        return true;
    }

    @Override
    public boolean deleteItem(Long skuId) {
        log.info("删除商品: skuId={}", skuId);
        
        RMap<String, CartItem> userCart = getUserCart();
        CartItem removed = userCart.remove(skuId.toString());
        
        if (removed == null) {
            log.warn("删除失败，商品不存在: skuId={}", skuId);
        } else {
            log.info("删除商品成功: skuId={}", skuId);
        }
        
        resetExpireIfNeeded(userCart);
        return true;
    }

    @Override
    public boolean batchDeleteItem(List<Long> skuIdList) {
        if (CollUtil.isEmpty(skuIdList)) {
            log.warn("批量删除参数为空");
            return false;
        }
        
        log.info("批量删除商品: skuIds={}, count={}", skuIdList, skuIdList.size());
        
        RMap<String, CartItem> userCart = getUserCart();
        
        // 优化：直接传入 String 数组，避免重复转换
        String[] keys = skuIdList.stream()
                .map(String::valueOf)
                .toArray(String[]::new);
        
        long removeCount = userCart.fastRemove(keys);
        
        resetExpireIfNeeded(userCart);
        
        log.info("批量删除商品成功: 删除数量={}", removeCount);
        return true;
    }

    /**
     * 获取用户购物车 Map
     */
    private RMap<String, CartItem> getUserCart() {
        Long userId = getCurrentUserId();
        String userCartKey = cartRedisKeyBuilder.buildUserCartKey(userId);
        return redissonClient.getMap(userCartKey);
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
     * 按需重置购物车过期时间（性能优化）
     */
    private void resetExpireIfNeeded(RMap<String, CartItem> userCart) {
        try {
            long remainTimeMs = userCart.remainTimeToLive();
            
            // remainTimeToLive 返回 -1 表示没有设置过期时间，-2 表示 key 不存在
            if (remainTimeMs == -1) {
                // 首次设置过期时间
                userCart.expire(Duration.ofDays(cartProperties.getTtlDays()));
                log.debug("首次设置购物车过期时间: {}天", cartProperties.getTtlDays());
            } else if (remainTimeMs > 0 && remainTimeMs < cartProperties.getExpireRefreshThreshold() * 1000) {
                // 剩余时间小于阈值时才重置（性能优化）
                userCart.expire(Duration.ofDays(cartProperties.getTtlDays()));
                log.debug("重置购物车过期时间: remainTimeMs={}, threshold={}ms", 
                        remainTimeMs, cartProperties.getExpireRefreshThreshold() * 1000);
            }
        } catch (Exception e) {
            // 重置过期时间失败不应影响主流程
            log.error("重置购物车过期时间失败", e);
        }
    }

    /**
     * 获取当前用户 ID
     */
    private Long getCurrentUserId() {
        ClientUser clientUser = AuthorizationContext.getClientUser();
        return clientUser.getId();
    }
}
