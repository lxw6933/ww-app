package com.ww.mall.redis.constant;

/**
 * @author ww
 * @create 2024-06-14- 13:34
 * @description:
 */
public class LuaConstant {

    public static final String DECREMENT_STOCK_LUA = "local current_stock = tonumber(redis.call('get', KEYS[1]) or 0);\n" +
            "if current_stock >= tonumber(ARGV[1]) then\n" +
            "    redis.call('DECRBY', KEYS[1], tonumber(ARGV[1]));\n" +
            "    return current_stock - tonumber(ARGV[1]);\n" +
            "else\n" +
            "    return -1;\n" +
            "end";
    public static final byte[] DECREMENT_STOCK_LUA_BYTE = DECREMENT_STOCK_LUA.getBytes();

    public static final String LOCK_STOCK_HASH_LUA = "local hashKey = KEYS[1]\n" +
            "local decrementStock = tonumber(ARGV[1])\n" +
            "local totalStock = tonumber(redis.call('HGET', hashKey, 'totalStock'))\n" +
            "local lockStock = tonumber(redis.call('HGET', hashKey, 'lockStock'))\n" +
            "local useStock = tonumber(redis.call('HGET', hashKey, 'useStock'))\n" +
            "if totalStock == nil or lockStock == nil or useStock == nil then\n" +
            "   return -1\n" +
            "end\n" +
            "local availableStock = totalStock - lockStock - useStock\n" +
            "if availableStock < decrementStock then\n" +
            "   return -2\n" +
            "end\n" +
            "local newLockStock = lockStock + decrementStock\n" +
            "redis.call('HSET', hashKey, 'lockStock', newLockStock)\n" +
            "return 1";
    public static final byte[] LOCK_STOCK_HASH_LUA_BYTE = LOCK_STOCK_HASH_LUA.getBytes();

    public static final String BATCH_LOCK_STOCK_HASH_LUA = "for i = 1, #KEYS do\n" +
            "local hashKey = KEYS[i]\n" +
            "local decrementStock = tonumber(ARGV[i])\n" +
            "local totalStock = tonumber(redis.call('HGET', hashKey, 'totalStock'))\n" +
            "local lockStock = tonumber(redis.call('HGET', hashKey, 'lockStock'))\n" +
            "local useStock = tonumber(redis.call('HGET', hashKey, 'useStock'))\n" +
            "if totalStock == nil or lockStock == nil or useStock == nil then\n" +
            "    return -1\n" +
            "end\n" +
            "local availableStock = totalStock - lockStock - useStock\n" +
            "if availableStock < decrementStock then\n" +
            "    return -2\n" +
            "end\n" +
            "local newLockStock = lockStock + decrementStock\n" +
            "redis.call('HSET', hashKey, 'lockStock', newLockStock)\n" +
            "end\n" +
            "return 1";
    public static final byte[] BATCH_LOCK_STOCK_HASH_LUA_BYTE = BATCH_LOCK_STOCK_HASH_LUA.getBytes();

    public static final String USE_STOCK_HASH_LUA = "local hashKey = KEYS[1]\n" +
            "local number = tonumber(ARGV[1])\n" +
            "local oldLockValue = redis.call('HGET', hashKey, 'lockStock')\n" +
            "local oldUseValue = redis.call('HGET', hashKey, 'useStock')\n" +
            "local lockStock = tonumber(oldLockValue)\n" +
            "if lockStock < number then\n" +
            "    return -1\n" +
            "end\n" +
            "local lockResult = redis.call('HINCRBYFLOAT', hashKey, 'lockStock', -number)\n" +
            "local useResult = redis.call('HINCRBYFLOAT', hashKey, 'useStock', number)\n" +
            "return 1";
    public static final byte[] USE_STOCK_HASH_LUA_BYTE = USE_STOCK_HASH_LUA.getBytes();

    public static final String BATCH_USE_STOCK_HASH_LUA = "for i = 1, #KEYS do\n" +
            "local hashKey = KEYS[i]\n" +
            "local number = tonumber(ARGV[i])\n" +
            "local lockStock = tonumber(redis.call('HGET', hashKey, 'lockStock'))\n" +
            "if lockStock < number then\n" +
            "    return -1\n" +
            "end\n" +
            "redis.call('HINCRBYFLOAT', hashKey, 'lockStock', -number)\n" +
            "redis.call('HINCRBYFLOAT', hashKey, 'useStock', number)\n" +
            "end\n" +
            "return 1";
    public static final byte[] BATCH_USE_STOCK_HASH_LUA_BYTE = BATCH_USE_STOCK_HASH_LUA.getBytes();

    public static final String ROLLBACK_STOCK_HASH_LUA = "local hashKey = KEYS[1]\n" +
            "local number = tonumber(ARGV[1])\n" +
            "local lockStock = tonumber(redis.call('HGET', hashKey, 'lockStock'))\n" +
            "if lockStock < number then\n" +
            "    return -1\n" +
            "end\n" +
            "redis.call('HINCRBYFLOAT', hashKey, 'lockStock', -number)\n" +
            "return 1";
    public static final byte[] ROLLBACK_STOCK_HASH_LUA_BYTE = ROLLBACK_STOCK_HASH_LUA.getBytes();

    public static final String ROLLBACK_AFTER_STOCK_HASH_LUA = "local hashKey = KEYS[1]\n" +
            "local number = tonumber(ARGV[1])\n" +
            "local useStock = tonumber(redis.call('HGET', hashKey, 'useStock'))\n" +
            "if useStock < number then\n" +
            "    return -1\n" +
            "end\n" +
            "redis.call('HINCRBYFLOAT', hashKey, 'useStock', -number)\n" +
            "return 1";
    public static final byte[] ROLLBACK_AFTER_STOCK_HASH_LUA_BYTE = ROLLBACK_AFTER_STOCK_HASH_LUA.getBytes();

    public static final String BATCH_ROLLBACK_STOCK_HASH_LUA = "for i = 1, #KEYS do\n" +
            "local hashKey = KEYS[i]\n" +
            "local number = tonumber(ARGV[i])\n" +
            "local lockStock = tonumber(redis.call('HGET', hashKey, 'lockStock'))\n" +
            "if lockStock < number then\n" +
            "    return -1\n" +
            "end\n" +
            "redis.call('HINCRBYFLOAT', hashKey, 'lockStock', -number)\n" +
            "end\n" +
            "return 1";
    public static final byte[] BATCH_ROLLBACK_STOCK_HASH_LUA_BYTE = BATCH_ROLLBACK_STOCK_HASH_LUA.getBytes();

}
