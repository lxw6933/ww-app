package com.ww.app.cart.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.ww.app.cart.component.CartCacheComponent;
import com.ww.app.cart.component.key.CartRedisKeyBuilder;
import com.ww.app.cart.config.CartProperties;
import com.ww.app.cart.entity.Cart;
import com.ww.app.cart.entity.CartItem;
import com.ww.app.cart.service.HashCartService;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.constant.RedisChannelConstant;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.exception.ApiException;
import com.ww.app.redis.annotation.RedisPublishMsg;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

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

    @Resource
    private CartCacheComponent cartCacheComponent;

    @Override
    @RedisPublishMsg(value = RedisChannelConstant.USER_CART_CHANNEL, userMsgFlag = true)
    public boolean addToCart(Long skuId, Integer num) {
        // 参数校验
        validateSkuId(skuId);
        validateQuantity(num);

        Long userId = getCurrentUserId();
        log.info("添加购物车: userId={}, skuId={}, num={}", userId, skuId, num);

        RMap<String, CartItem> userCart = cartCacheComponent.getUserCart();

        // 使用原子标识判断是否为新增商品
        AtomicBoolean isNewItem = new AtomicBoolean(false);

        // 使用 compute 方法确保原子性操作
        userCart.compute(skuId.toString(), (key, oldItem) -> {
            if (oldItem == null) {
                // 检查购物车是否已满（只在新增时检查）
                if (userCart.size() >= cartProperties.getMaxCartNumber()) {
                    log.warn("购物车已满: userId={}, size={}, maxSize={}", 
                            userId, userCart.size(), cartProperties.getMaxCartNumber());
                    throw new ApiException("超出购物车最大容量");
                }

                // 新商品：创建购物车项
                isNewItem.set(true);
                log.info("新增商品到购物车: userId={}, skuId={}, num={}", userId, skuId, num);
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
                    log.warn("商品数量超限: userId={}, skuId={}, oldCount={}, addNum={}, maxNum={}",
                            userId, skuId, oldItem.getCount(), num, cartProperties.getMaxAddNumber());
                    throw new ApiException("商品数量不能超过" + cartProperties.getMaxAddNumber());
                }
                log.info("累加商品数量: userId={}, skuId={}, oldCount={}, newCount={}", 
                        userId, skuId, oldItem.getCount(), newCount);
                oldItem.setCount(newCount);
                oldItem.setUpdateTime(LocalDateTime.now());
                return oldItem;
            }
        });

        // 设置或重置过期时间
        resetExpireTime(userCart, isNewItem.get());

        log.info("添加购物车成功: userId={}, skuId={}, num={}, isNew={}", 
                userId, skuId, num, isNewItem.get());
        return true;
    }

    @Override
    public Cart userCartList() {
        Long userId = getCurrentUserId();
        log.info("查询购物车: userId={}", userId);

        // 优化: 优先从本地缓存获取
        Cart cart = cartCacheComponent.getUserCartCache(userId);

        log.info("购物车查询成功(含缓存): userId={}, itemCount={}, totalAmount={}", 
                userId, cart.getCountType(), cart.getTotalAmount());
        return cart;
    }

    @Override
    @RedisPublishMsg(value = RedisChannelConstant.USER_CART_CHANNEL, userMsgFlag = true)
    public boolean clearUserCart() {
        Long userId = getCurrentUserId();
        log.info("清空购物车: userId={}", userId);

        String userCartKey = cartRedisKeyBuilder.buildUserCartKey(userId);
        boolean deleted = redissonClient.getKeys().delete(userCartKey) > 0;

        if (deleted) {
            log.info("清空购物车成功: userId={}", userId);
        } else {
            log.warn("清空购物车失败或购物车本身为空: userId={}", userId);
        }

        return true;
    }

    @Override
    @RedisPublishMsg(value = RedisChannelConstant.USER_CART_CHANNEL, userMsgFlag = true)
    public boolean checkItem(Long skuId) {
        // 参数校验
        validateSkuId(skuId);

        Long userId = getCurrentUserId();
        log.info("勾选商品: userId={}, skuId={}", userId, skuId);

        RMap<String, CartItem> userCart = cartCacheComponent.getUserCart();

        // 使用 compute 方法确保原子性操作（修复并发安全问题）
        CartItem updatedItem = userCart.compute(skuId.toString(), (key, item) -> {
            if (item == null) {
                log.warn("商品不存在: userId={}, skuId={}", userId, skuId);
                throw new ApiException("商品不存在");
            }
            boolean newChecked = !item.isChecked();
            log.info("切换商品选中状态: userId={}, skuId={}, oldChecked={}, newChecked={}",
                    userId, skuId, item.isChecked(), newChecked);
            item.setChecked(newChecked);
            item.setUpdateTime(LocalDateTime.now());
            return item;
        });

        resetExpireTime(userCart, false);

        log.info("勾选商品成功: userId={}, skuId={}, checked={}", 
                userId, skuId, updatedItem.isChecked());
        return true;
    }

    @Override
    @RedisPublishMsg(value = RedisChannelConstant.USER_CART_CHANNEL, userMsgFlag = true)
    public boolean modifyItemCount(Long skuId, Integer num) {
        // 参数校验
        validateSkuId(skuId);
        validateQuantity(num);

        Long userId = getCurrentUserId();
        log.info("修改商品数量: userId={}, skuId={}, num={}", userId, skuId, num);

        RMap<String, CartItem> userCart = cartCacheComponent.getUserCart();

        // 使用 computeIfPresent 确保原子性
        CartItem updatedItem = userCart.computeIfPresent(skuId.toString(), (k, item) -> {
            log.info("修改数量: userId={}, skuId={}, oldCount={}, newCount={}", 
                    userId, skuId, item.getCount(), num);
            item.setCount(num);
            item.setUpdateTime(LocalDateTime.now());
            return item;
        });

        if (updatedItem == null) {
            log.warn("商品不存在: userId={}, skuId={}", userId, skuId);
            throw new ApiException("商品不存在");
        }

        resetExpireTime(userCart, false);

        log.info("修改商品数量成功: userId={}, skuId={}, num={}", userId, skuId, num);
        return true;
    }

    @Override
    @RedisPublishMsg(value = RedisChannelConstant.USER_CART_CHANNEL, userMsgFlag = true)
    public boolean deleteItem(Long skuId) {
        // 参数校验
        validateSkuId(skuId);

        Long userId = getCurrentUserId();
        log.info("删除商品: userId={}, skuId={}", userId, skuId);

        RMap<String, CartItem> userCart = cartCacheComponent.getUserCart();
        CartItem removed = userCart.remove(skuId.toString());

        if (removed == null) {
            log.warn("删除失败，商品不存在: userId={}, skuId={}", userId, skuId);
        } else {
            log.info("删除商品成功: userId={}, skuId={}", userId, skuId);
        }

        resetExpireTime(userCart, false);
        return true;
    }

    @Override
    @RedisPublishMsg(value = RedisChannelConstant.USER_CART_CHANNEL, userMsgFlag = true)
    public boolean batchDeleteItem(List<Long> skuIdList) {
        // 参数校验
        validateSkuIdList(skuIdList);

        Long userId = getCurrentUserId();
        log.info("批量删除商品: userId={}, skuIds={}, count={}", 
                userId, skuIdList, skuIdList.size());

        RMap<String, CartItem> userCart = cartCacheComponent.getUserCart();

        // 优化：直接传入 String 数组，避免重复转换
        String[] keys = skuIdList.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toArray(String[]::new);

        long removeCount = userCart.fastRemove(keys);

        resetExpireTime(userCart, false);

        log.info("批量删除商品成功: userId={}, requestCount={}, actualRemoved={}", 
                userId, skuIdList.size(), removeCount);
        return true;
    }

    @Override
    public String getCacheStats() {
        return cartCacheComponent.getCacheStats();
    }

    /**
     * 校验商品 ID
     *
     * @param skuId 商品 ID
     */
    private void validateSkuId(Long skuId) {
        if (skuId == null || skuId <= 0) {
            log.warn("商品ID参数非法: skuId={}", skuId);
            throw new ApiException("商品ID不能为空");
        }
    }

    /**
     * 校验商品数量
     *
     * @param num 商品数量
     */
    private void validateQuantity(Integer num) {
        if (num == null || num <= 0) {
            log.warn("商品数量参数非法: num={}", num);
            throw new ApiException("商品数量必须大于0");
        }
        if (num > cartProperties.getMaxAddNumber()) {
            log.warn("商品数量超限: num={}, maxNum={}", num, cartProperties.getMaxAddNumber());
            throw new ApiException("商品数量不能超过" + cartProperties.getMaxAddNumber());
        }
    }

    /**
     * 校验商品 ID 列表
     *
     * @param skuIdList 商品 ID 列表
     */
    private void validateSkuIdList(List<Long> skuIdList) {
        if (CollUtil.isEmpty(skuIdList)) {
            log.warn("商品ID列表为空");
            throw new ApiException("商品ID列表不能为空");
        }
        
        // 验证列表中的每个 skuId
        for (Long skuId : skuIdList) {
            if (skuId == null || skuId <= 0) {
                log.warn("商品ID列表包含无效ID: skuId={}", skuId);
                throw new ApiException("商品ID不能为空或小于等于0");
            }
        }
    }

    /**
     * 设置或重置购物车过期时间（性能优化版）
     * 
     * @param userCart 用户购物车
     * @param isNewCart 是否为新购物车
     */
    private void resetExpireTime(RMap<String, CartItem> userCart, boolean isNewCart) {
        try {
            // 如果是新购物车，直接设置过期时间
            if (isNewCart) {
                userCart.expire(Duration.ofDays(cartProperties.getTtlDays()));
                log.debug("首次设置购物车过期时间: {}天", cartProperties.getTtlDays());
                return;
            }

            // 性能优化：检查剩余过期时间
            long remainTimeMs = userCart.remainTimeToLive();

            // remainTimeToLive 返回 -1 表示没有设置过期时间，-2 表示 key 不存在
            if (remainTimeMs == -1) {
                // 首次设置过期时间
                userCart.expire(Duration.ofDays(cartProperties.getTtlDays()));
                log.info("补充设置购物车过期时间: {}天", cartProperties.getTtlDays());
            } else if (remainTimeMs > 0 && remainTimeMs < cartProperties.getExpireRefreshThreshold() * 1000) {
                // 剩余时间小于阈值时才重置（性能优化）
                userCart.expire(Duration.ofDays(cartProperties.getTtlDays()));
                log.info("重置购物车过期时间: remainTimeMs={}, threshold={}ms",
                        remainTimeMs, cartProperties.getExpireRefreshThreshold() * 1000);
            } else {
                log.debug("购物车过期时间充足，无需重置: remainTimeMs={}", remainTimeMs);
            }
        } catch (Exception e) {
            // 重置过期时间失败不应影响主流程
            log.error("重置购物车过期时间失败: userId={}, error={}", 
                    getCurrentUserId(), e.getMessage(), e);
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
