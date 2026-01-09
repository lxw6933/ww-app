package com.ww.app.cart.component;

import cn.hutool.core.collection.CollUtil;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.cart.component.key.CartRedisKeyBuilder;
import com.ww.app.cart.entity.Cart;
import com.ww.app.cart.entity.CartItem;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.utils.CaffeineUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 购物车缓存组件
 * 使用 Caffeine 本地缓存 + Redis 二级缓存架构
 *
 * @author ww
 * @date 2025-11-06
 */
@Slf4j
@Component
public class CartCacheComponent {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private CartRedisKeyBuilder cartRedisKeyBuilder;

    /**
     * 本地缓存: userId -> Cart
     * 缓存热点用户的购物车数据
     */
    private LoadingCache<Long, Cart> cartLocalCache;

    @PostConstruct
    public void init() {
        // 初始化购物车本地缓存
        // 容量: 3000个活跃用户
        // 过期时间: 5分钟(随机5-10分钟防止雪崩)
        // 刷新时间: 3分钟(自动刷新热点数据)
        this.cartLocalCache = CaffeineUtil.createRandomExpireAutoRefreshCache(
                500,        // 初始容量
                3000,                   // 最大容量
                5,                      // 最小过期时间(分钟)
                10,                     // 最大过期时间(分钟)
                TimeUnit.MINUTES,       // 时间单位
                3,                      // 刷新时间
                TimeUnit.MINUTES,       // 刷新时间单位
                this::loadCartFromRedis // 刷新函数
        );

        log.info("购物车本地缓存初始化完成 - 最大容量: 3000, 过期时间: 5-10分钟, 刷新时间: 3分钟");
    }

    /**
     * 使本地缓存失效
     * 在购物车变更后调用,确保数据一致性
     *
     * @param userId 用户ID
     */
    public void invalidateCache(Long userId) {
        try {
            if (cartLocalCache != null) {
                cartLocalCache.invalidate(userId);
            }
            if (log.isDebugEnabled()) {
                log.debug("购物车本地缓存失效: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("购物车缓存失效操作失败: userId={}", userId, e);
        }
    }

    /**
     * 批量使缓存失效
     *
     * @param userIds 用户ID集合
     */
    public void invalidateBatchCache(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        try {
            CaffeineUtil.invalidateAll(cartLocalCache, userIds);
            log.info("批量失效购物车缓存: userCount={}", userIds.size());
        } catch (Exception e) {
            log.error("批量失效购物车缓存失败: userCount={}", userIds.size(), e);
        }
    }

    /**
     * 预热购物车缓存
     * 在系统启动或高峰期前调用
     *
     * @param userIds 用户ID列表
     */
    public void warmUpCache(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        try {
            log.info("开始预热购物车缓存: userCount={}", userIds.size());
            int successCount = 0;

            for (Long userId : userIds) {
                try {
                    Cart cart = loadCartFromRedis(userId);
                    if (cart.getCountType() > 0) {
                        cartLocalCache.put(userId, cart);
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("预热单个用户购物车失败: userId={}", userId, e);
                }
            }

            log.info("购物车缓存预热完成: 成功={}/{}", successCount, userIds.size());
        } catch (Exception e) {
            log.error("购物车缓存预热失败: userCount={}", userIds.size(), e);
        }
    }

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息字符串
     */
    public String getCacheStats() {
        try {
            return "购物车本地缓存统计:\n" +
                    "  - 大小: " + CaffeineUtil.size(cartLocalCache) + "\n" +
                    "  - 命中率: " + String.format("%.2f%%", CaffeineUtil.getHitRate(cartLocalCache) * 100) + "\n";
        } catch (Exception e) {
            log.error("获取缓存统计信息失败", e);
            return "获取统计信息失败";
        }
    }

    /**
     * 清空本地缓存
     * 仅用于特殊场景(如系统维护)
     */
    public void clearLocalCache() {
        try {
            CaffeineUtil.clear(cartLocalCache);
            log.info("购物车本地缓存已清空");
        } catch (Exception e) {
            log.error("清空购物车本地缓存失败", e);
        }
    }

    /**
     * 获取购物车(优先从本地缓存)
     *
     * @param userId 用户ID
     * @return 购物车信息
     */
    public Cart getUserCartCache(Long userId) {
        try {
            Cart cart = cartLocalCache.get(userId);
            if (log.isDebugEnabled()) {
                log.debug("从本地缓存获取购物车成功: userId={}, itemCount={}",
                        userId, cart != null ? cart.getCountType() : 0);
            }
            return cart != null ? cart : buildEmptyCart();
        } catch (Exception e) {
            log.error("从本地缓存获取购物车失败，降级到Redis: userId={}", userId, e);
            return loadCartFromRedis(userId);
        }
    }

    /**
     * 从 Redis 加载购物车数据
     *
     * @param userId 用户ID
     * @return 购物车信息
     */
    private Cart loadCartFromRedis(Long userId) {
        try {

            Cart cart = new Cart();
            cart.setCartItems(getUserCartItemList(userId));
            cart.recalcTotals();

            if (log.isDebugEnabled()) {
                log.debug("从Redis加载购物车: userId={}, itemCount={}, totalAmount={}",
                        userId, cart.getCountType(), cart.getTotalAmount());
            }

            return cart;
        } catch (Exception e) {
            log.error("从Redis加载购物车失败: userId={}", userId, e);
            return buildEmptyCart();
        }
    }

    /**
     * 构建空购物车
     *
     * @return 空购物车对象
     */
    private Cart buildEmptyCart() {
        Cart cart = new Cart();
        cart.setCartItems(Collections.emptyList());
        cart.setCountNum(0);
        cart.setCountType(0);
        cart.setTotalAmount(0L);
        cart.setCheckedCount(0);
        cart.setInvalidCount(0);
        return cart;
    }

    /**
     * 获取用户购物车 Map
     */
    public RMap<String, CartItem> getUserCart() {
        Long userId = AuthorizationContext.getClientUser().getId();
        return getUserCart(userId);
    }

    public RMap<String, CartItem> getUserCart(Long userId) {
        String userCartKey = cartRedisKeyBuilder.buildUserCartKey(userId);
        return redissonClient.getMap(userCartKey);
    }

    /**
     * 获取用户购物车列表
     */
    public List<CartItem> getUserCartItemList(Long userId) {
        RMap<String, CartItem> userCart = getUserCart(userId);

        // 性能优化：使用 readAllValues() 批量读取
        Collection<CartItem> values = userCart.readAllValues();

        if (CollUtil.isEmpty(values)) {
            return Collections.emptyList();
        }

        return new ArrayList<>(values);
    }

}
