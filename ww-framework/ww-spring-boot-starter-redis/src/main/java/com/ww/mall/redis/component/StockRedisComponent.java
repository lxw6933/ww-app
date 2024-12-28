package com.ww.mall.redis.component;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.ww.mall.redis.constant.LuaConstant;
import com.ww.mall.redis.handler.RedisStockHandlerManager;
import com.ww.mall.redis.vo.ActivityStockVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

/**
 * @author ww
 * @create 2024-12-21 11:04
 * @description: 库存组件
 */
@Slf4j
@Component
public class StockRedisComponent {

    private static final int INIT_STOCK = 0;

    private static final List<Object> stockFieldList = Arrays.asList("totalStock", "lockStock", "useStock");

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisStockHandlerManager redisStockHandlerManager;

    @Resource
    private DefaultRedisScript<Long> decrementStockScript;

    @Resource
    private DefaultRedisScript<Long> lockHashStockScript;

    @Resource
    private DefaultRedisScript<Long> useHashStockScript;

    private String decrementStockSha1;

    private String lockHashStockSha1;

    private String useHashStockSha1;

    private String rollbackHashStockSha1;
    private String rollbackHashAfterStockSha1;

    private String batchLockHashStockSha1;
    private String batchUseHashStockSha1;
    private String batchRollbackHashStockSha1;

    @PostConstruct
    public void init() {
        // 预加载的lua脚本
        preloadLuaScript();
    }

    /**
     * 预加载lua script，redis服务重启，需重新加载脚本，否则报错NOSCRIPT
     */
    private void preloadLuaScript() {
        decrementStockSha1 = stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.scriptLoad(LuaConstant.DECREMENT_STOCK_LUA_BYTE));
        lockHashStockSha1 = stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.scriptLoad(LuaConstant.LOCK_STOCK_HASH_LUA_BYTE));
        useHashStockSha1 = stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.scriptLoad(LuaConstant.USE_STOCK_HASH_LUA_BYTE));
        rollbackHashStockSha1 = stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.scriptLoad(LuaConstant.ROLLBACK_STOCK_HASH_LUA_BYTE));
        rollbackHashAfterStockSha1 = stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.scriptLoad(LuaConstant.ROLLBACK_AFTER_STOCK_HASH_LUA_BYTE));
        // 批量处理lua脚本
        batchLockHashStockSha1 = stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.scriptLoad(LuaConstant.BATCH_LOCK_STOCK_HASH_LUA_BYTE));
        batchUseHashStockSha1 = stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.scriptLoad(LuaConstant.BATCH_USE_STOCK_HASH_LUA_BYTE));
        batchRollbackHashStockSha1 = stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.scriptLoad(LuaConstant.BATCH_ROLLBACK_STOCK_HASH_LUA_BYTE));
    }

    /**
     * 初始化库存数据
     *
     * @param strKey stringKey
     * @param totalStock 总库存
     */
    public void initStrStock(String strKey, int totalStock) {
        stringRedisTemplate.opsForValue().set(strKey, String.valueOf(totalStock));
    }

    public int getStrStock(String strKey) {
        String stockStr = stringRedisTemplate.opsForValue().get(strKey);
        return StrUtil.isBlank(stockStr) ? INIT_STOCK : Integer.parseInt(stockStr);
    }

    /**
     * lua扣减库存
     *
     * @param key    key
     * @param number 扣减数量
     * @return long
     */
    public boolean decrementStock(String key, int number) {
        return stockRedisHandler(decrementStockSha1, key, number);
    }

    /**
     * 初始化hash库存数据
     *
     * @param hashKey hashKey
     * @param totalStock 总库存
     */
    public void initHashStock(String hashKey, int totalStock) {
        BoundHashOperations<String, String, String> skuHashStock = stringRedisTemplate.boundHashOps(hashKey);
        skuHashStock.put(stockFieldList.get(0).toString(), String.valueOf(totalStock));
        skuHashStock.put(stockFieldList.get(1).toString(), String.valueOf(INIT_STOCK));
        skuHashStock.put(stockFieldList.get(2).toString(), String.valueOf(INIT_STOCK));
    }

    /**
     * hash库存数据
     *
     * @param hashKey hashKey
     * @param totalStock 总库存
     */
    public void setHashStock(String hashKey, int totalStock, int lockStock, int useStock) {
        Map<String, String> param = new HashMap<>();
        param.put(stockFieldList.get(0).toString(), String.valueOf(totalStock));
        param.put(stockFieldList.get(1).toString(), String.valueOf(lockStock));
        param.put(stockFieldList.get(2).toString(), String.valueOf(useStock));
        stringRedisTemplate.opsForHash().putAll(hashKey, param);
    }

    /**
     * 获取活动库存
     *
     * @param hashKey hashKey
     * @return ActivityStockVO
     */
    public ActivityStockVO getHashStock(String hashKey) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(hashKey))) {
            List<Object> values = stringRedisTemplate.opsForHash().multiGet(hashKey, stockFieldList);
            ActivityStockVO vo = new ActivityStockVO();
            vo.setTotalStock(Integer.parseInt(values.get(0).toString()));
            vo.setLockStock(Integer.parseInt(values.get(1).toString()));
            vo.setUseStock(Integer.parseInt(values.get(2).toString()));
            return vo;
        } else {
            return null;
        }
    }

    /**
     * 锁定库存
     *
     * @param hashKey hashKey
     * @param number 数量
     * @return success > 0
     */
    public boolean lockHashStock(String hashKey, int number) {
        return stockRedisHandler(lockHashStockSha1, hashKey, number);
    }

    /**
     * 使用库存
     *
     * @param hashKey hashKey
     * @param number 数量
     * @return success > 0
     */
    public boolean useHashStock(String hashKey, int number) {
        return stockRedisHandler(useHashStockSha1, hashKey, number);
    }

    /**
     * 回滚库存
     *
     * @param hashKey hashKey
     * @param number 数量
     * @return boolean
     */
    public boolean rollbackHashStock(String hashKey, int number) {
        return stockRedisHandler(rollbackHashStockSha1, hashKey, number);
    }

    /**
     * 回滚售后库存
     *
     * @param hashKey hashKey
     * @param number 数量
     * @return boolean
     */
    public boolean rollbackHashAfterStock(String hashKey, int number) {
        return stockRedisHandler(rollbackHashAfterStockSha1, hashKey, number);
    }

    @SuppressWarnings("all")
    private boolean stockRedisHandler(String lua, String hashKey, int number) {
        Long res = stringRedisTemplate.execute((RedisCallback<Long>) connection -> {
            RedisSerializer keySerializer = stringRedisTemplate.getKeySerializer();
            byte[] keyBytes = keySerializer.serialize(hashKey);
            // 执行lua脚本
            Object result = connection.evalSha(lua, ReturnType.INTEGER, 1, keyBytes, String.valueOf(number).getBytes());
            return (Long) result;
        });
        return res != null && res >= 0;
    }

    public boolean a(String key, int number) {
        Long res = stringRedisTemplate.execute(decrementStockScript, Collections.singletonList(key), String.valueOf(number));
        return res != null && res >= 0;
    }

    public boolean b(String key, int number) {
        Long res = stringRedisTemplate.execute(lockHashStockScript, Collections.singletonList(key), String.valueOf(number));
        return res != null && res >= 0;
    }

    public boolean c(String key, int number) {
        Long res = stringRedisTemplate.execute(useHashStockScript, Collections.singletonList(key), String.valueOf(number));
        return res != null && res >= 0;
    }

    /**
     * 批量原子锁定库存
     *
     * @param stockMap key: hashKey value: number
     * @return success > 0
     * @deprecated 不能保证事务，存在事务影响性能
     */
    @Deprecated
    public boolean batchLockHashStock(Map<String, Integer> stockMap) {
        return batchHashStockHandler(stockMap, batchLockHashStockSha1);
    }

    public boolean multiplePayHashStock(Map<String, Integer> stockMap) {
        if (MapUtil.isEmpty(stockMap)) {
            return true;
        }
        Map<String, Integer> successMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : stockMap.entrySet()) {
            String hashKey = entry.getKey();
            Integer number = entry.getValue();
            if (this.useHashStock(hashKey, number)) {
                successMap.put(hashKey, number);
                log.info("【支付】库存更新成功：key：{} number: {}", hashKey, number);
            } else {
                log.error("【支付】库存更新失败：key：{} number: {}", hashKey, number);
                redisStockHandlerManager.handleFailRollbackStock(hashKey, number, 2);
                break;
            }
        }
        return successMap.size() == stockMap.size();
    }

    public boolean multipleLockHashStock(Map<String, Integer> stockMap) {
        if (MapUtil.isEmpty(stockMap)) {
            return true;
        }
        Map<String, Integer> successMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : stockMap.entrySet()) {
            String hashKey = entry.getKey();
            Integer number = entry.getValue();
            if (this.lockHashStock(hashKey, number)) {
                successMap.put(hashKey, number);
                log.info("库存锁定成功：key：{} number: {}", hashKey, number);
            } else {
                log.error("库存锁定失败：key：{} number: {}", hashKey, number);
                break;
            }
        }
        if (successMap.size() != stockMap.size()) {
            // 回滚库存
            multipleRollbackHashStock(successMap, true);
            return false;
        }
        return true;
    }

    public void multipleRollbackHashStock(Map<String, Integer> rollbckStockMap, boolean isCancel) {
        if (MapUtil.isEmpty(rollbckStockMap)) {
            return;
        }
        // 回滚库存
        rollbckStockMap.forEach((hashKey, number) -> {
            if (isCancel ? this.rollbackHashStock(hashKey, number) : this.rollbackHashAfterStock(hashKey, number)) {
                log.info("【{}】库存回滚成功：key：{} number: {}", isCancel, hashKey, number);
            } else {
                log.error("【{}】库存回滚失败：key：{} number: {}", isCancel, hashKey, number);
                redisStockHandlerManager.handleFailRollbackStock(hashKey, number, 0);
            }
        });
    }

    /**
     * 批量原子使用库存
     *
     * @param stockMap key: hashKey value: number
     * @return success > 0
     * @deprecated 不能保证事务，存在事务影响性能
     */
    @Deprecated
    public boolean batchUseHashStock(Map<String, Integer> stockMap) {
        return batchHashStockHandler(stockMap, batchUseHashStockSha1);
    }

    /**
     * 批量原子回滚库存
     *
     * @param stockMap key: hashKey value: number
     * @return success > 0
     * @deprecated 不能保证事务，存在事务影响性能
     */
    @Deprecated
    public boolean batchRollbackHashStock(Map<String, Integer> stockMap) {
        return batchHashStockHandler(stockMap, batchRollbackHashStockSha1);
    }

    private boolean batchHashStockHandler(Map<String, Integer> hashStockMap, String batchLua) {
        if (hashStockMap.isEmpty()) {
            return false;
        }
        List<byte[]> keys = new ArrayList<>();
        List<byte[]> values = new ArrayList<>();
        hashStockMap.forEach((key, stock) -> {
            keys.add(key.getBytes());
            values.add(String.valueOf(stock).getBytes());
        });
        List<byte[]> args = new ArrayList<>(keys);
        args.addAll(values);
        Long res = stringRedisTemplate.execute((RedisCallback<Long>) connection -> {
            // 执行lua脚本
            Object result = connection.evalSha(batchLua,
                    ReturnType.INTEGER,
                    keys.size(),
                    args.toArray(new byte[0][0]));
            return (Long) result;
        });
        return res != null && res >= 0;
    }

}
