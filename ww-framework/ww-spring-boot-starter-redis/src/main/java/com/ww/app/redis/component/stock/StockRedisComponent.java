package com.ww.app.redis.component.stock;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.ww.app.redis.component.lua.RedisScriptComponent;
import com.ww.app.redis.component.stock.constant.StockLuaConstant;
import com.ww.app.redis.component.stock.entity.ActivityStockVO;
import com.ww.app.redis.component.stock.entity.StockResult;
import com.ww.app.redis.component.stock.entity.StockResultCode;
import com.ww.app.redis.component.stock.handler.RedisStockHandlerManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

/**
 * @author ww
 * @create 2024-12-21 11:04
 * @description: 库存组件
 *
 * 注意事项：
 * - 操作不具备幂等性，调用方需处理重复执行与重试。
 * - Redis 重启后可能出现 NOSCRIPT，组件会重新加载脚本。
 * - 集群环境批量脚本要求 key 在同一哈希槽。
 */
@Slf4j
@Component
public class StockRedisComponent {

    private static final int INIT_STOCK = 0;

    private static final String FIELD_TOTAL_STOCK = "totalStock";
    private static final String FIELD_LOCK_STOCK = "lockStock";
    private static final String FIELD_USE_STOCK = "useStock";
    private static final List<String> STOCK_FIELDS = Collections.unmodifiableList(
            Arrays.asList(FIELD_TOTAL_STOCK, FIELD_LOCK_STOCK, FIELD_USE_STOCK));

    private static final String SCRIPT_DECREMENT_STOCK = "stock_decrement";
    private static final String SCRIPT_LOCK_HASH_STOCK = "stock_lock_hash";
    private static final String SCRIPT_USE_HASH_STOCK = "stock_use_hash";
    private static final String SCRIPT_ROLLBACK_HASH_STOCK = "stock_rollback_hash";
    private static final String SCRIPT_ROLLBACK_AFTER_HASH_STOCK = "stock_rollback_after_hash";
    private static final String SCRIPT_BATCH_LOCK_HASH_STOCK = "stock_batch_lock_hash";
    private static final String SCRIPT_BATCH_USE_HASH_STOCK = "stock_batch_use_hash";
    private static final String SCRIPT_BATCH_ROLLBACK_HASH_STOCK = "stock_batch_rollback_hash";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisScriptComponent redisScriptComponent;

    @Resource
    private RedisStockHandlerManager redisStockHandlerManager;

    @Resource
    private DefaultRedisScript<Long> decrementStockScript;

    @Resource
    private DefaultRedisScript<Long> lockHashStockScript;

    @Resource
    private DefaultRedisScript<Long> useHashStockScript;

    @PostConstruct
    public void init() {
        // 预加载的 stock lua脚本
        preloadLuaScript();
    }

    /**
     * 预加载 Lua 脚本，避免首次执行出现 NOSCRIPT。
     */
    private void preloadLuaScript() {
        redisScriptComponent.preLoadLuaScript(SCRIPT_DECREMENT_STOCK, StockLuaConstant.DECREMENT_STOCK_LUA);
        redisScriptComponent.preLoadLuaScript(SCRIPT_LOCK_HASH_STOCK, StockLuaConstant.LOCK_STOCK_HASH_LUA);
        redisScriptComponent.preLoadLuaScript(SCRIPT_USE_HASH_STOCK, StockLuaConstant.USE_STOCK_HASH_LUA);
        redisScriptComponent.preLoadLuaScript(SCRIPT_ROLLBACK_HASH_STOCK, StockLuaConstant.ROLLBACK_STOCK_HASH_LUA);
        redisScriptComponent.preLoadLuaScript(SCRIPT_ROLLBACK_AFTER_HASH_STOCK, StockLuaConstant.ROLLBACK_AFTER_STOCK_HASH_LUA);
        redisScriptComponent.preLoadLuaScript(SCRIPT_BATCH_LOCK_HASH_STOCK, StockLuaConstant.BATCH_LOCK_STOCK_HASH_LUA);
        redisScriptComponent.preLoadLuaScript(SCRIPT_BATCH_USE_HASH_STOCK, StockLuaConstant.BATCH_USE_STOCK_HASH_LUA);
        redisScriptComponent.preLoadLuaScript(SCRIPT_BATCH_ROLLBACK_HASH_STOCK, StockLuaConstant.BATCH_ROLLBACK_STOCK_HASH_LUA);
    }

    /**
     * 初始化字符串库存值。
     *
     * @param strKey 库存键
     * @param totalStock 总库存
     */
    public void initStrStock(String strKey, int totalStock) {
        if (StrUtil.isBlank(strKey)) {
            log.warn("initStrStock skipped because key is blank.");
            return;
        }
        if (totalStock < 0) {
            log.warn("initStrStock skipped because totalStock is negative. key: {}, totalStock: {}", strKey, totalStock);
            return;
        }
        stringRedisTemplate.opsForValue().set(strKey, String.valueOf(totalStock));
    }

    /**
     * 获取字符串库存值。
     *
     * @param strKey 库存键
     * @return 库存值，缺失或非法时返回 0
     */
    public int getStrStock(String strKey) {
        if (StrUtil.isBlank(strKey)) {
            return INIT_STOCK;
        }
        String stockStr = stringRedisTemplate.opsForValue().get(strKey);
        if (StrUtil.isBlank(stockStr)) {
            return INIT_STOCK;
        }
        try {
            return Integer.parseInt(stockStr);
        } catch (NumberFormatException ex) {
            log.warn("invalid stock value. key: {}, value: {}", strKey, stockStr);
            return INIT_STOCK;
        }
    }

    /**
     * Lua 扣减字符串库存。
     *
     * @param key 库存键
     * @param number 扣减数量
     * @return 扣减成功且剩余库存不为负时返回 true
     */
    public boolean decrementStock(String key, int number) {
        return decrementStockResult(key, number).isSuccess();
    }

    /**
     * Lua 扣减字符串库存并返回详细结果。
     *
     * @param key 库存键
     * @param number 扣减数量
     * @return 库存结果
     */
    public StockResult decrementStockResult(String key, int number) {
        StockResult invalid = validateKeyAndNumber(key, number);
        if (invalid != null) {
            return invalid;
        }
        Long result = executeSingleKeyScript(SCRIPT_DECREMENT_STOCK, key, number);
        if (result == null) {
            return StockResult.failure(StockResultCode.EXECUTION_ERROR, "redis lua execution failed");
        }
        if (result >= 0) {
            return StockResult.success(result);
        }
        return StockResult.failure(StockResultCode.INSUFFICIENT_STOCK, result, "insufficient stock");
    }

    /**
     * 初始化 Hash 库存。
     *
     * @param hashKey 哈希键
     * @param totalStock 总库存
     */
    public void initHashStock(String hashKey, int totalStock) {
        if (StrUtil.isBlank(hashKey)) {
            log.warn("initHashStock skipped because key is blank.");
            return;
        }
        if (totalStock < 0) {
            log.warn("initHashStock skipped because totalStock is negative. key: {}, totalStock: {}", hashKey, totalStock);
            return;
        }
        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(hashKey);
        hashOps.put(FIELD_TOTAL_STOCK, String.valueOf(totalStock));
        hashOps.put(FIELD_LOCK_STOCK, String.valueOf(INIT_STOCK));
        hashOps.put(FIELD_USE_STOCK, String.valueOf(INIT_STOCK));
    }

    /**
     * 设置 Hash 库存值。
     *
     * @param hashKey 哈希键
     * @param totalStock 总库存
     * @param lockStock 已锁库存
     * @param useStock 已用库存
     */
    public void setHashStock(String hashKey, int totalStock, int lockStock, int useStock) {
        if (StrUtil.isBlank(hashKey)) {
            log.warn("setHashStock skipped because key is blank.");
            return;
        }
        Map<String, String> param = new HashMap<>();
        param.put(FIELD_TOTAL_STOCK, String.valueOf(totalStock));
        param.put(FIELD_LOCK_STOCK, String.valueOf(lockStock));
        param.put(FIELD_USE_STOCK, String.valueOf(useStock));
        stringRedisTemplate.opsForHash().putAll(hashKey, param);
    }

    /**
     * 获取 Hash 库存值。
     *
     * @param hashKey 哈希键
     * @return 库存值，缺失或非法时返回 null
     */
    public ActivityStockVO getHashStock(String hashKey) {
        if (StrUtil.isBlank(hashKey)) {
            return null;
        }
        HashOperations<String, String, String> hashOps = stringRedisTemplate.opsForHash();
        List<String> values = hashOps.multiGet(hashKey, STOCK_FIELDS);
        if (values.size() != STOCK_FIELDS.size()) {
            return null;
        }
        Integer totalStock = parseStockValue(values.get(0), hashKey, FIELD_TOTAL_STOCK);
        Integer lockStock = parseStockValue(values.get(1), hashKey, FIELD_LOCK_STOCK);
        Integer useStock = parseStockValue(values.get(2), hashKey, FIELD_USE_STOCK);
        if (totalStock == null || lockStock == null || useStock == null) {
            return null;
        }
        ActivityStockVO vo = new ActivityStockVO();
        vo.setTotalStock(totalStock);
        vo.setLockStock(lockStock);
        vo.setUseStock(useStock);
        return vo;
    }

    /**
     * 锁定 Hash 库存。
     *
     * @param hashKey 哈希键
     * @param number 锁定数量
     * @return 锁定成功返回 true
     */
    public boolean lockHashStock(String hashKey, int number) {
        return lockHashStockResult(hashKey, number).isSuccess();
    }

    /**
     * 锁定 Hash 库存并返回详细结果。
     *
     * @param hashKey 哈希键
     * @param number 锁定数量
     * @return 库存结果
     */
    public StockResult lockHashStockResult(String hashKey, int number) {
        Map<Long, StockError> errorMap = new HashMap<>();
        errorMap.put(-1L, new StockError(StockResultCode.STOCK_NOT_FOUND, "stock not found"));
        errorMap.put(-2L, new StockError(StockResultCode.INSUFFICIENT_STOCK, "insufficient stock"));
        return executeHashStockResult(SCRIPT_LOCK_HASH_STOCK, hashKey, number, errorMap);
    }

    /**
     * 使用已锁定库存。
     *
     * @param hashKey 哈希键
     * @param number 使用数量
     * @return 使用成功返回 true
     */
    public boolean useHashStock(String hashKey, int number) {
        return useHashStockResult(hashKey, number).isSuccess();
    }

    /**
     * 使用已锁定库存并返回详细结果。
     *
     * @param hashKey 哈希键
     * @param number 使用数量
     * @return 库存结果
     */
    public StockResult useHashStockResult(String hashKey, int number) {
        Map<Long, StockError> errorMap = new HashMap<>();
        errorMap.put(-1L, new StockError(StockResultCode.INSUFFICIENT_STOCK, "insufficient locked stock"));
        return executeHashStockResult(SCRIPT_USE_HASH_STOCK, hashKey, number, errorMap);
    }

    /**
     * 回滚锁定库存。
     *
     * @param hashKey 哈希键
     * @param number 回滚数量
     * @return 回滚成功返回 true
     */
    public boolean rollbackHashStock(String hashKey, int number) {
        return rollbackHashStockResult(hashKey, number).isSuccess();
    }

    /**
     * 回滚锁定库存并返回详细结果。
     *
     * @param hashKey 哈希键
     * @param number 回滚数量
     * @return 库存结果
     */
    public StockResult rollbackHashStockResult(String hashKey, int number) {
        StockResult invalid = validateKeyAndNumber(hashKey, number);
        if (invalid != null) {
            return invalid;
        }
        Long result = executeSingleKeyScript(SCRIPT_ROLLBACK_HASH_STOCK, hashKey, number);
        if (result == null) {
            return StockResult.failure(StockResultCode.EXECUTION_ERROR, "redis lua execution failed");
        }
        if (result > 0) {
            return StockResult.success(result);
        }
        return StockResult.failure(StockResultCode.INSUFFICIENT_STOCK, result, "insufficient locked stock");
    }

    /**
     * 回滚已使用库存。
     *
     * @param hashKey 哈希键
     * @param number 回滚数量
     * @return 回滚成功返回 true
     */
    public boolean rollbackHashAfterStock(String hashKey, int number) {
        return rollbackHashAfterStockResult(hashKey, number).isSuccess();
    }

    /**
     * 回滚已使用库存并返回详细结果。
     *
     * @param hashKey 哈希键
     * @param number 回滚数量
     * @return 库存结果
     */
    public StockResult rollbackHashAfterStockResult(String hashKey, int number) {
        StockResult invalid = validateKeyAndNumber(hashKey, number);
        if (invalid != null) {
            return invalid;
        }
        Long result = executeSingleKeyScript(SCRIPT_ROLLBACK_AFTER_HASH_STOCK, hashKey, number);
        if (result == null) {
            return StockResult.failure(StockResultCode.EXECUTION_ERROR, "redis lua execution failed");
        }
        if (result > 0) {
            return StockResult.success(result);
        }
        return StockResult.failure(StockResultCode.INSUFFICIENT_STOCK, result, "insufficient used stock");
    }

    /**
     * @deprecated 请使用 {@link #decrementStockResult(String, int)}。
     */
    @Deprecated
    public boolean a(String key, int number) {
        StockResult invalid = validateKeyAndNumber(key, number);
        if (invalid != null) {
            return false;
        }
        Long res = stringRedisTemplate.execute(decrementStockScript, Collections.singletonList(key), String.valueOf(number));
        return res >= 0;
    }

    /**
     * @deprecated 请使用 {@link #lockHashStockResult(String, int)}。
     */
    @Deprecated
    public boolean b(String key, int number) {
        StockResult invalid = validateKeyAndNumber(key, number);
        if (invalid != null) {
            return false;
        }
        Long res = stringRedisTemplate.execute(lockHashStockScript, Collections.singletonList(key), String.valueOf(number));
        return res >= 0;
    }

    /**
     * @deprecated 请使用 {@link #useHashStockResult(String, int)}。
     */
    @Deprecated
    public boolean c(String key, int number) {
        StockResult invalid = validateKeyAndNumber(key, number);
        if (invalid != null) {
            return false;
        }
        Long res = stringRedisTemplate.execute(useHashStockScript, Collections.singletonList(key), String.valueOf(number));
        return res >= 0;
    }

    /**
     * Lua 批量锁定库存。
     *
     * @param stockMap 键: hashKey, 值: 数量
     * @return 全部锁定成功返回 true
     * @deprecated 非事务；Redis Cluster 下批量 key 需位于同一哈希槽。
     */
    @Deprecated
    public boolean batchLockHashStock(Map<String, Integer> stockMap) {
        return batchHashStockHandler(stockMap, SCRIPT_BATCH_LOCK_HASH_STOCK);
    }

    /**
     * 支付流程按顺序使用库存。
     *
     * @param stockMap 键: hashKey, 值: 数量
     * @return 全部更新成功返回 true
     */
    public boolean multiplePayHashStock(Map<String, Integer> stockMap) {
        if (MapUtil.isEmpty(stockMap)) {
            return true;
        }
        Map<String, Integer> successMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : stockMap.entrySet()) {
            String hashKey = entry.getKey();
            Integer number = entry.getValue();
            if (number == null) {
                log.error("pay stock update failed due to null number. key: {}", hashKey);
                break;
            }
            StockResult result = useHashStockResult(hashKey, number);
            if (result.isSuccess()) {
                successMap.put(hashKey, number);
                log.info("pay stock updated. key: {}, number: {}, result: {}", hashKey, number, result.getScriptResult());
            } else {
                log.error("pay stock update failed. key: {}, number: {}, code: {}, message: {}",
                        hashKey, number, result.getCode(), result.getMessage());
                redisStockHandlerManager.handleFailRollbackStock(hashKey, number, 2);
                break;
            }
        }
        return successMap.size() == stockMap.size();
    }

    /**
     * 按顺序锁定库存。
     *
     * @param stockMap 键: hashKey, 值: 数量
     * @return 全部锁定成功返回 true
     */
    public boolean multipleLockHashStock(Map<String, Integer> stockMap) {
        if (MapUtil.isEmpty(stockMap)) {
            return true;
        }
        Map<String, Integer> successMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : stockMap.entrySet()) {
            String hashKey = entry.getKey();
            Integer number = entry.getValue();
            if (number == null) {
                log.error("lock stock failed due to null number. key: {}", hashKey);
                break;
            }
            StockResult result = lockHashStockResult(hashKey, number);
            if (result.isSuccess()) {
                successMap.put(hashKey, number);
                log.info("stock locked. key: {}, number: {}", hashKey, number);
            } else {
                log.error("stock lock failed. key: {}, number: {}, code: {}, message: {}",
                        hashKey, number, result.getCode(), result.getMessage());
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

    /**
     * 按顺序回滚库存。
     *
     * @param rollbackStockMap 键: hashKey, 值: 数量
     * @param isCancel 为 true 回滚锁定库存；为 false 回滚已使用库存
     */
    public void multipleRollbackHashStock(Map<String, Integer> rollbackStockMap, boolean isCancel) {
        if (MapUtil.isEmpty(rollbackStockMap)) {
            return;
        }
        rollbackStockMap.forEach((hashKey, number) -> {
            if (number == null) {
                log.error("stock rollback failed due to null number. key: {}", hashKey);
                redisStockHandlerManager.handleFailRollbackStock(hashKey, 0, 0);
                return;
            }
            StockResult result = isCancel
                    ? rollbackHashStockResult(hashKey, number)
                    : rollbackHashAfterStockResult(hashKey, number);
            if (result.isSuccess()) {
                log.info("stock rollback succeeded. cancel: {}, key: {}, number: {}", isCancel, hashKey, number);
            } else {
                log.error("stock rollback failed. cancel: {}, key: {}, number: {}, code: {}, message: {}",
                        isCancel, hashKey, number, result.getCode(), result.getMessage());
                redisStockHandlerManager.handleFailRollbackStock(hashKey, number, 0);
            }
        });
    }

    /**
     * Lua 批量使用库存。
     *
     * @param stockMap 键: hashKey, 值: 数量
     * @return 全部使用成功返回 true
     * @deprecated 非事务；Redis Cluster 下批量 key 需位于同一哈希槽。
     */
    @Deprecated
    public boolean batchUseHashStock(Map<String, Integer> stockMap) {
        return batchHashStockHandler(stockMap, SCRIPT_BATCH_USE_HASH_STOCK);
    }

    /**
     * Lua 批量回滚锁定库存。
     *
     * @param stockMap 键: hashKey, 值: 数量
     * @return 全部回滚成功返回 true
     * @deprecated 非事务；Redis Cluster 下批量 key 需位于同一哈希槽。
     */
    @Deprecated
    public boolean batchRollbackHashStock(Map<String, Integer> stockMap) {
        return batchHashStockHandler(stockMap, SCRIPT_BATCH_ROLLBACK_HASH_STOCK);
    }

    private boolean batchHashStockHandler(Map<String, Integer> hashStockMap, String scriptName) {
        if (MapUtil.isEmpty(hashStockMap)) {
            return false;
        }
        Long result = executeBatchScript(scriptName, hashStockMap);
        return result != null && result > 0;
    }

    private Long executeSingleKeyScript(String scriptName, String key, int number) {
        try {
            return redisScriptComponent.executeLuaScript(
                    scriptName,
                    ReturnType.INTEGER,
                    Collections.singletonList(key),
                    Collections.singletonList(String.valueOf(number)));
        } catch (Exception ex) {
            log.error("库存脚本执行异常: name={}, key={}, number={}", scriptName, key, number, ex);
            return null;
        }
    }

    private Long executeBatchScript(String scriptName, Map<String, Integer> hashStockMap) {
        List<String> keys = new ArrayList<>(hashStockMap.size());
        List<String> values = new ArrayList<>(hashStockMap.size());
        for (Map.Entry<String, Integer> entry : hashStockMap.entrySet()) {
            String key = entry.getKey();
            Integer number = entry.getValue();
            if (StrUtil.isBlank(key) || number == null || number < 0) {
                log.warn("batch stock skipped due to invalid entry. key: {}, number: {}", key, number);
                return null;
            }
            keys.add(key);
            values.add(String.valueOf(number));
        }
        try {
            return redisScriptComponent.executeLuaScript(scriptName, ReturnType.INTEGER, keys, values);
        } catch (Exception ex) {
            log.error("库存批量脚本执行异常: name={}, size={}", scriptName, hashStockMap.size(), ex);
            return null;
        }
    }

    private StockResult validateKeyAndNumber(String key, int number) {
        if (StrUtil.isBlank(key)) {
            return StockResult.failure(StockResultCode.INVALID_ARGUMENT, "key is blank");
        }
        if (number < 0) {
            return StockResult.failure(StockResultCode.INVALID_ARGUMENT, "number must be >= 0");
        }
        return null;
    }

    private StockResult executeHashStockResult(String scriptName, String hashKey, int number, Map<Long, StockError> errorMap) {
        StockResult invalid = validateKeyAndNumber(hashKey, number);
        if (invalid != null) {
            return invalid;
        }
        Long result = executeSingleKeyScript(scriptName, hashKey, number);
        if (result == null) {
            return StockResult.failure(StockResultCode.EXECUTION_ERROR, "redis lua execution failed");
        }
        if (result > 0) {
            return StockResult.success(result);
        }
        StockError error = errorMap.get(result);
        if (error != null) {
            return StockResult.failure(error.getCode(), result, error.getMessage());
        }
        return StockResult.failure(StockResultCode.EXECUTION_ERROR, result, "unexpected lua result");
    }

    private Integer parseStockValue(String value, String hashKey, String field) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            log.warn("invalid stock field value. key: {}, field: {}, value: {}", hashKey, field, value);
            return null;
        }
    }

    private static final class StockError {
        private final StockResultCode code;
        private final String message;

        private StockError(StockResultCode code, String message) {
            this.code = code;
            this.message = message;
        }

        private StockResultCode getCode() {
            return code;
        }

        private String getMessage() {
            return message;
        }
    }

}
