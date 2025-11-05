package com.ww.app.redis.component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
import org.redisson.api.listener.MessageListener;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 高性能组件类
 * 提供常用的分布式操作封装，优化性能和易用性
 *
 * @author ww
 * @create 2025-11-05 14:59
 * @description: Redisson 通用组件，提供高性能的分布式操作
 */
@Getter
@Slf4j
@Component
public class RedissonComponent {

    private final RedissonClient redissonClient;

    public RedissonComponent(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    // ==================== 批量操作 ====================

    /**
     * 批量获取多个 key 的值
     * 使用 Pipeline 批量获取，提升性能[一对一的键值关系]
     *
     * @param keys key 集合
     * @return Map<String, Object> key-value 映射
     */
    public <V> Map<String, V> batchGet(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            RBatch batch = redissonClient.createBatch();
            Map<String, RFuture<V>> futureMap = new LinkedHashMap<>(keys.size());

            // 批量添加获取操作
            for (String key : keys) {
                RBucketAsync<V> bucket = batch.getBucket(key);
                futureMap.put(key, bucket.getAsync());
            }

            // 执行批量操作
            batch.execute();

            // 组装结果
            Map<String, V> result = new LinkedHashMap<>(keys.size());
            for (Map.Entry<String, RFuture<V>> entry : futureMap.entrySet()) {
                try {
                    V value = entry.getValue().get();
                    if (value != null) {
                        result.put(entry.getKey(), value);
                    }
                } catch (Exception e) {
                    log.error("批量获取 key={} 失败", entry.getKey(), e);
                }
            }

            return result;
        } catch (Exception e) {
            log.error("批量获取操作失败，keys={}", keys, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 批量设置多个 key-value
     * 使用 Pipeline 批量设置，提升性能
     *
     * @param keyValues key-value 映射
     * @return 成功设置的数量
     */
    public <V> int batchSet(Map<String, V> keyValues) {
        if (keyValues == null || keyValues.isEmpty()) {
            return 0;
        }

        try {
            RBatch batch = redissonClient.createBatch();
            List<RFuture<Void>> futures = new ArrayList<>(keyValues.size());

            // 批量添加设置操作
            for (Map.Entry<String, V> entry : keyValues.entrySet()) {
                RBucketAsync<V> bucket = batch.getBucket(entry.getKey());
                futures.add(bucket.setAsync(entry.getValue()));
            }

            // 执行批量操作
            batch.execute();

            // 统计成功数量
            int successCount = 0;
            for (RFuture<Void> future : futures) {
                try {
                    future.get();
                    successCount++;
                } catch (Exception e) {
                    log.error("批量设置单个 key 失败", e);
                }
            }

            return successCount;
        } catch (Exception e) {
            log.error("批量设置操作失败", e);
            return 0;
        }
    }

    /**
     * 批量设置带过期时间的 key-value
     *
     * @param keyValues key-value 映射
     * @param ttl       过期时间
     * @param timeUnit  时间单位
     * @return 成功设置的数量
     */
    public <V> int batchSetWithTTL(Map<String, V> keyValues, long ttl, TimeUnit timeUnit) {
        if (keyValues == null || keyValues.isEmpty()) {
            return 0;
        }

        try {
            RBatch batch = redissonClient.createBatch();
            List<RFuture<Void>> futures = new ArrayList<>(keyValues.size());

            // 批量添加设置操作
            for (Map.Entry<String, V> entry : keyValues.entrySet()) {
                RBucketAsync<V> bucket = batch.getBucket(entry.getKey());
                futures.add(bucket.setAsync(entry.getValue(), ttl, timeUnit));
            }

            // 执行批量操作
            batch.execute();

            // 统计成功数量
            int successCount = 0;
            for (RFuture<Void> future : futures) {
                try {
                    future.get();
                    successCount++;
                } catch (Exception e) {
                    log.error("批量设置带 TTL 的单个 key 失败", e);
                }
            }

            return successCount;
        } catch (Exception e) {
            log.error("批量设置带 TTL 操作失败", e);
            return 0;
        }
    }

    /**
     * 批量删除 key
     *
     * @param keys key 集合
     * @return 成功删除的数量
     */
    public long batchDelete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }

        try {
            String[] keyArray = keys.toArray(new String[0]);
            return redissonClient.getKeys().delete(keyArray);
        } catch (Exception e) {
            log.error("批量删除操作失败，keys={}", keys, e);
            return 0;
        }
    }

    /**
     * 批量检查 key 是否存在
     *
     * @param keys key 集合
     * @return Map<String, Boolean> key-存在状态映射
     */
    public Map<String, Boolean> batchExists(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            RBatch batch = redissonClient.createBatch();
            Map<String, RFuture<Boolean>> futureMap = new LinkedHashMap<>(keys.size());

            // 批量添加检查操作
            for (String key : keys) {
                RBucketAsync<Object> bucket = batch.getBucket(key);
                futureMap.put(key, bucket.isExistsAsync());
            }

            // 执行批量操作
            batch.execute();

            // 组装结果
            Map<String, Boolean> result = new LinkedHashMap<>(keys.size());
            for (Map.Entry<String, RFuture<Boolean>> entry : futureMap.entrySet()) {
                try {
                    result.put(entry.getKey(), entry.getValue().get());
                } catch (Exception e) {
                    log.error("批量检查 key={} 存在性失败", entry.getKey(), e);
                    result.put(entry.getKey(), false);
                }
            }

            return result;
        } catch (Exception e) {
            log.error("批量检查存在性操作失败，keys={}", keys, e);
            return Collections.emptyMap();
        }
    }

    // ==================== 分布式锁操作 ====================

    /**
     * 尝试获取锁并执行业务逻辑
     * 自动释放锁，避免死锁
     *
     * @param lockKey  锁 key
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param timeUnit 时间单位
     * @param supplier 业务逻辑
     * @param <T>      返回值类型
     * @return 业务逻辑执行结果，获取锁失败返回 null
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime,
                                   TimeUnit timeUnit, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(waitTime, leaseTime, timeUnit)) {
                try {
                    return supplier.get();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                log.warn("获取锁失败，lockKey={}", lockKey);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取锁被中断，lockKey={}", lockKey, e);
            return null;
        } catch (Exception e) {
            log.error("执行锁内业务逻辑失败，lockKey={}", lockKey, e);
            return null;
        }
    }

    /**
     * 尝试获取公平锁并执行业务逻辑
     * 保证锁的获取顺序
     *
     * @param lockKey  锁 key
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param timeUnit 时间单位
     * @param supplier 业务逻辑
     * @param <T>      返回值类型
     * @return 业务逻辑执行结果
     */
    public <T> T executeWithFairLock(String lockKey, long waitTime, long leaseTime,
                                       TimeUnit timeUnit, Supplier<T> supplier) {
        RLock fairLock = redissonClient.getFairLock(lockKey);
        try {
            if (fairLock.tryLock(waitTime, leaseTime, timeUnit)) {
                try {
                    return supplier.get();
                } finally {
                    if (fairLock.isHeldByCurrentThread()) {
                        fairLock.unlock();
                    }
                }
            } else {
                log.warn("获取公平锁失败，lockKey={}", lockKey);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取公平锁被中断，lockKey={}", lockKey, e);
            return null;
        } catch (Exception e) {
            log.error("执行公平锁内业务逻辑失败，lockKey={}", lockKey, e);
            return null;
        }
    }

    /**
     * 获取读锁并执行业务逻辑（读写锁）
     *
     * @param lockKey  锁 key
     * @param supplier 业务逻辑
     * @param <T>      返回值类型
     * @return 业务逻辑执行结果
     */
    public <T> T executeWithReadLock(String lockKey, Supplier<T> supplier) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(lockKey);
        RLock readLock = rwLock.readLock();
        
        try {
            readLock.lock();
            return supplier.get();
        } finally {
            if (readLock.isHeldByCurrentThread()) {
                readLock.unlock();
            }
        }
    }

    /**
     * 获取写锁并执行业务逻辑（读写锁）
     *
     * @param lockKey  锁 key
     * @param supplier 业务逻辑
     * @param <T>      返回值类型
     * @return 业务逻辑执行结果
     */
    public <T> T executeWithWriteLock(String lockKey, Supplier<T> supplier) {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(lockKey);
        RLock writeLock = rwLock.writeLock();
        
        try {
            writeLock.lock();
            return supplier.get();
        } finally {
            if (writeLock.isHeldByCurrentThread()) {
                writeLock.unlock();
            }
        }
    }

    // ==================== 限流器操作 ====================

    /**
     * 尝试获取限流令牌
     * 使用令牌桶算法
     *
     * @param rateLimiterKey 限流器 key
     * @param rate           速率（每秒令牌数）
     * @param rateInterval   速率时间间隔
     * @param permits        需要的令牌数
     * @return true 获取成功，false 获取失败
     */
    public boolean tryAcquire(String rateLimiterKey, long rate, long rateInterval, int permits) {
        try {
            RRateLimiter rateLimiter = redissonClient.getRateLimiter(rateLimiterKey);
            
            // 尝试初始化限流器配置
            if (!rateLimiter.isExists()) {
                rateLimiter.trySetRate(RateType.OVERALL, rate, rateInterval, RateIntervalUnit.SECONDS);
            }
            
            return rateLimiter.tryAcquire(permits);
        } catch (Exception e) {
            log.error("限流器获取令牌失败，rateLimiterKey={}", rateLimiterKey, e);
            return false;
        }
    }

    /**
     * 尝试获取限流令牌（单个令牌）
     *
     * @param rateLimiterKey 限流器 key
     * @param rate           速率（每秒令牌数）
     * @param rateInterval   速率时间间隔
     * @return true 获取成功，false 获取失败
     */
    public boolean tryAcquire(String rateLimiterKey, long rate, long rateInterval) {
        return tryAcquire(rateLimiterKey, rate, rateInterval, 1);
    }

    // ==================== 布隆过滤器操作 ====================

    /**
     * 初始化布隆过滤器
     *
     * @param bloomFilterKey     布隆过滤器 key
     * @param expectedInsertions 预期插入数量
     * @param falseProbability   误判率
     * @return true 初始化成功，false 已存在
     */
    public boolean initBloomFilter(String bloomFilterKey, long expectedInsertions, double falseProbability) {
        try {
            RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(bloomFilterKey);
            return bloomFilter.tryInit(expectedInsertions, falseProbability);
        } catch (Exception e) {
            log.error("初始化布隆过滤器失败，bloomFilterKey={}", bloomFilterKey, e);
            return false;
        }
    }

    /**
     * 向布隆过滤器添加元素
     *
     * @param bloomFilterKey 布隆过滤器 key
     * @param value          值
     * @return true 添加成功，false 添加失败
     */
    public boolean bloomFilterAdd(String bloomFilterKey, Object value) {
        try {
            RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(bloomFilterKey);
            return bloomFilter.add(value);
        } catch (Exception e) {
            log.error("布隆过滤器添加元素失败，bloomFilterKey={}, value={}", bloomFilterKey, value, e);
            return false;
        }
    }

    /**
     * 检查布隆过滤器是否包含元素
     *
     * @param bloomFilterKey 布隆过滤器 key
     * @param value          值
     * @return true 可能存在，false 一定不存在
     */
    public boolean bloomFilterContains(String bloomFilterKey, Object value) {
        try {
            RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(bloomFilterKey);
            return bloomFilter.contains(value);
        } catch (Exception e) {
            log.error("布隆过滤器检查元素失败，bloomFilterKey={}, value={}", bloomFilterKey, value, e);
            return false;
        }
    }

    /**
     * 批量添加到布隆过滤器
     *
     * @param bloomFilterKey 布隆过滤器 key
     * @param values         值集合
     * @return 成功添加的数量
     */
    public int bloomFilterBatchAdd(String bloomFilterKey, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }

        try {
            RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(bloomFilterKey);
            int count = 0;
            for (Object value : values) {
                if (bloomFilter.add(value)) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            log.error("布隆过滤器批量添加失败，bloomFilterKey={}", bloomFilterKey, e);
            return 0;
        }
    }

    // ==================== 分布式计数器操作 ====================

    /**
     * 原子递增
     *
     * @param key 计数器 key
     * @return 递增后的值
     */
    public long increment(String key) {
        try {
            RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
            return atomicLong.incrementAndGet();
        } catch (Exception e) {
            log.error("原子递增失败，key={}", key, e);
            return -1;
        }
    }

    /**
     * 原子递增指定值
     *
     * @param key   计数器 key
     * @param delta 递增值
     * @return 递增后的值
     */
    public long incrementBy(String key, long delta) {
        try {
            RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
            return atomicLong.addAndGet(delta);
        } catch (Exception e) {
            log.error("原子递增指定值失败，key={}, delta={}", key, delta, e);
            return -1;
        }
    }

    /**
     * 原子递减
     *
     * @param key 计数器 key
     * @return 递减后的值
     */
    public long decrement(String key) {
        try {
            RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
            return atomicLong.decrementAndGet();
        } catch (Exception e) {
            log.error("原子递减失败，key={}", key, e);
            return -1;
        }
    }

    /**
     * 获取计数器值
     *
     * @param key 计数器 key
     * @return 当前值
     */
    public long getCounter(String key) {
        try {
            RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
            return atomicLong.get();
        } catch (Exception e) {
            log.error("获取计数器值失败，key={}", key, e);
            return 0;
        }
    }

    // ==================== Lua 脚本操作 ====================

    /**
     * 预加载 Lua 脚本到 Redis
     *
     * @param luaScript Lua 脚本内容
     * @return 脚本 SHA1 值
     */
    public String loadLuaScript(String luaScript) {
        try {
            RScript rScript = redissonClient.getScript(StringCodec.INSTANCE);
            return rScript.scriptLoad(luaScript);
        } catch (Exception e) {
            log.error("加载 Lua 脚本失败", e);
            return null;
        }
    }

    /**
     * 执行 Lua 脚本
     *
     * @param script     Lua 脚本
     * @param returnType 返回值类型
     * @param keys       key 列表
     * @param values     参数列表
     * @param <T>        返回值类型
     * @return 脚本执行结果
     */
    public <T> T evalLua(String script, RScript.ReturnType returnType, List<Object> keys, Object... values) {
        try {
            RScript rScript = redissonClient.getScript(StringCodec.INSTANCE);
            return rScript.eval(RScript.Mode.READ_WRITE, script, returnType, keys, values);
        } catch (Exception e) {
            log.error("执行 Lua 脚本失败", e);
            return null;
        }
    }

    /**
     * 执行已加载的 Lua 脚本（通过 SHA1）
     *
     * @param sha1       脚本 SHA1 值
     * @param returnType 返回值类型
     * @param keys       key 列表
     * @param values     参数列表
     * @param <T>        返回值类型
     * @return 脚本执行结果
     */
    public <T> T evalLuaBySha(String sha1, RScript.ReturnType returnType, List<Object> keys, Object... values) {
        try {
            RScript rScript = redissonClient.getScript(StringCodec.INSTANCE);
            return rScript.evalSha(RScript.Mode.READ_WRITE, sha1, returnType, keys, values);
        } catch (Exception e) {
            log.error("执行已加载 Lua 脚本失败，sha1={}", sha1, e);
            return null;
        }
    }

    // ==================== 缓存穿透防护 ====================

    /**
     * 带缓存穿透防护的获取方法
     * 使用空值缓存和布隆过滤器双重防护
     *
     * @param key            缓存 key
     * @param bloomFilterKey 布隆过滤器 key（可选，为 null 则不使用）
     * @param dbLoader       数据库加载函数
     * @param ttl            缓存过期时间
     * @param timeUnit       时间单位
     * @param <V>            值类型
     * @return 缓存值或数据库加载值
     */
    public <V> V getWithBloomFilter(String key, String bloomFilterKey, 
                                     Supplier<V> dbLoader, long ttl, TimeUnit timeUnit) {
        try {
            // 1. 先检查布隆过滤器（如果提供）
            if (bloomFilterKey != null && !bloomFilterContains(bloomFilterKey, key)) {
                log.debug("布隆过滤器判断 key={} 不存在", key);
                return null;
            }

            // 2. 查询缓存
            RBucket<V> bucket = redissonClient.getBucket(key);
            V value = bucket.get();
            
            if (value != null) {
                return value;
            }

            // 3. 缓存未命中，查询数据库
            value = dbLoader.get();
            
            if (value != null) {
                // 4. 数据存在，写入缓存
                bucket.set(value, ttl, timeUnit);
                
                // 5. 添加到布隆过滤器
                if (bloomFilterKey != null) {
                    bloomFilterAdd(bloomFilterKey, key);
                }
            } else {
                // 6. 数据不存在，缓存空值防止穿透（短时间）
                bucket.set(null, Math.min(ttl, 60), TimeUnit.SECONDS);
            }
            
            return value;
        } catch (Exception e) {
            log.error("带布隆过滤器的缓存获取失败，key={}", key, e);
            return null;
        }
    }

    // ==================== HyperLogLog 操作 ====================

    /**
     * HyperLogLog 添加元素
     *
     * @param key   HyperLogLog key
     * @param value 值
     * @return true 添加成功
     */
    public boolean hyperLogLogAdd(String key, Object value) {
        try {
            RHyperLogLog<Object> hyperLogLog = redissonClient.getHyperLogLog(key);
            return hyperLogLog.add(value);
        } catch (Exception e) {
            log.error("HyperLogLog 添加失败，key={}", key, e);
            return false;
        }
    }

    /**
     * HyperLogLog 批量添加元素
     *
     * @param key    HyperLogLog key
     * @param values 值集合
     * @return true 添加成功
     */
    public <T> boolean hyperLogLogAddAll(String key, Collection<T> values) {
        try {
            RHyperLogLog<T> hyperLogLog = redissonClient.getHyperLogLog(key);
            return hyperLogLog.addAll(values);
        } catch (Exception e) {
            log.error("HyperLogLog 批量添加失败，key={}", key, e);
            return false;
        }
    }

    /**
     * HyperLogLog 获取基数（去重计数）
     *
     * @param key HyperLogLog key
     * @return 基数估算值
     */
    public long hyperLogLogCount(String key) {
        try {
            RHyperLogLog<Object> hyperLogLog = redissonClient.getHyperLogLog(key);
            return hyperLogLog.count();
        } catch (Exception e) {
            log.error("HyperLogLog 获取基数失败，key={}", key, e);
            return 0;
        }
    }

    // ==================== 批量获取 RSet 操作 ====================

    /**
     * 批量获取 RSet 对象
     * 注意：此方法返回的是 RSet 对象引用，不是 Set 中的值
     *
     * @param keys key 集合
     * @param <V>  Set 元素类型
     * @return Map<String, RSet<V>> key-RSet 映射
     */
    public <V> Map<String, RSet<V>> batchGetSets(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            Map<String, RSet<V>> result = new LinkedHashMap<>(keys.size());
            for (String key : keys) {
                RSet<V> set = redissonClient.getSet(key);
                result.put(key, set);
            }
            return result;
        } catch (Exception e) {
            log.error("批量获取 RSet 失败，keys={}", keys, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 批量获取 RSet 中的所有元素值
     * 使用 Pipeline 批量读取，提升性能
     *
     * @param keys key 集合
     * @param <V>  Set 元素类型
     * @return Map<String, Set<V>> key-元素集合映射
     */
    public <V> Map<String, Set<V>> batchGetSetValues(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            RBatch batch = redissonClient.createBatch();
            Map<String, RFuture<Set<V>>> futureMap = new LinkedHashMap<>(keys.size());

            // 批量添加读取操作
            for (String key : keys) {
                RSetAsync<V> set = batch.getSet(key);
                futureMap.put(key, set.readAllAsync());
            }

            // 执行批量操作
            batch.execute();

            // 组装结果
            Map<String, Set<V>> result = new LinkedHashMap<>(keys.size());
            for (Map.Entry<String, RFuture<Set<V>>> entry : futureMap.entrySet()) {
                try {
                    Set<V> values = entry.getValue().get();
                    if (values != null && !values.isEmpty()) {
                        result.put(entry.getKey(), values);
                    }
                } catch (Exception e) {
                    log.error("批量获取 RSet 值 key={} 失败", entry.getKey(), e);
                }
            }

            return result;
        } catch (Exception e) {
            log.error("批量获取 RSet 值操作失败，keys={}", keys, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 批量获取 RList 对象
     *
     * @param keys key 集合
     * @param <V>  List 元素类型
     * @return Map<String, RList<V>> key-RList 映射
     */
    public <V> Map<String, RList<V>> batchGetLists(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            Map<String, RList<V>> result = new LinkedHashMap<>(keys.size());
            for (String key : keys) {
                RList<V> list = redissonClient.getList(key);
                result.put(key, list);
            }
            return result;
        } catch (Exception e) {
            log.error("批量获取 RList 失败，keys={}", keys, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 批量获取 RMap 对象
     *
     * @param keys key 集合
     * @param <K>  Map 键类型
     * @param <V>  Map 值类型
     * @return Map<String, RMap<K, V>> key-RMap 映射
     */
    public <K, V> Map<String, RMap<K, V>> batchGetMaps(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            Map<String, RMap<K, V>> result = new LinkedHashMap<>(keys.size());
            for (String key : keys) {
                RMap<K, V> map = redissonClient.getMap(key);
                result.put(key, map);
            }
            return result;
        } catch (Exception e) {
            log.error("批量获取 RMap 失败，keys={}", keys, e);
            return Collections.emptyMap();
        }
    }

    // ==================== 分布式集合操作 ====================

    /**
     * Set 批量添加元素
     *
     * @param key    Set key
     * @param values 值集合
     * @param <V>    值类型
     * @return 成功添加的数量
     */
    public <V> int setAddAll(String key, Collection<V> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }

        try {
            RSet<V> set = redissonClient.getSet(key);
            int count = 0;
            for (V value : values) {
                if (set.add(value)) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            log.error("Set 批量添加失败，key={}", key, e);
            return 0;
        }
    }

    /**
     * Set 获取所有元素
     *
     * @param key Set key
     * @param <V> 值类型
     * @return 元素集合
     */
    public <V> Set<V> setGetAll(String key) {
        try {
            RSet<V> set = redissonClient.getSet(key);
            return set.readAll();
        } catch (Exception e) {
            log.error("Set 获取所有元素失败，key={}", key, e);
            return Collections.emptySet();
        }
    }

    /**
     * List 批量添加元素到尾部
     *
     * @param key    List key
     * @param values 值集合
     * @param <V>    值类型
     * @return 成功添加的数量
     */
    public <V> int listAddAll(String key, Collection<V> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }

        try {
            RList<V> list = redissonClient.getList(key);
            return list.addAll(values) ? values.size() : 0;
        } catch (Exception e) {
            log.error("List 批量添加失败，key={}", key, e);
            return 0;
        }
    }

    /**
     * List 范围获取元素
     *
     * @param key   List key
     * @param start 起始索引
     * @param end   结束索引
     * @param <V>   值类型
     * @return 元素列表
     */
    public <V> List<V> listRange(String key, int start, int end) {
        try {
            RList<V> list = redissonClient.getList(key);
            return list.range(start, end);
        } catch (Exception e) {
            log.error("List 范围获取失败，key={}, start={}, end={}", key, start, end, e);
            return Collections.emptyList();
        }
    }

    // ==================== 发布订阅操作 ====================

    /**
     * 发布消息到主题
     *
     * @param topic   主题名称
     * @param message 消息内容
     * @return 接收到消息的订阅者数量
     */
    public long publish(String topic, Object message) {
        try {
            RTopic topic1 = redissonClient.getTopic(topic);
            return topic1.publish(message);
        } catch (Exception e) {
            log.error("发布消息失败，topic={}", topic, e);
            return 0;
        }
    }

    /**
     * 订阅主题消息
     *
     * @param topic        主题名称
     * @param messageClass 消息类型 Class
     * @param listener     消息监听器
     * @param <M>          消息类型
     * @return 监听器ID
     */
    public <M> int subscribe(String topic, Class<M> messageClass, MessageListener<M> listener) {
        try {
            RTopic topic1 = redissonClient.getTopic(topic);
            return topic1.addListener(messageClass, listener);
        } catch (Exception e) {
            log.error("订阅主题失败，topic={}", topic, e);
            return -1;
        }
    }

    // ==================== 过期时间操作 ====================

    /**
     * 设置 key 的过期时间
     *
     * @param key      key
     * @param ttl      过期时间
     * @param timeUnit 时间单位
     * @return true 设置成功
     */
    public boolean expire(String key, long ttl, TimeUnit timeUnit) {
        try {
            RBucket<Object> bucket = redissonClient.getBucket(key);
            return bucket.expire(Duration.ofMillis(timeUnit.toMillis(ttl)));
        } catch (Exception e) {
            log.error("设置过期时间失败，key={}", key, e);
            return false;
        }
    }

    /**
     * 获取 key 的剩余过期时间
     *
     * @param key key
     * @return 剩余毫秒数，-1 表示永不过期，-2 表示 key 不存在
     */
    public long getExpireTime(String key) {
        try {
            RBucket<Object> bucket = redissonClient.getBucket(key);
            return bucket.remainTimeToLive();
        } catch (Exception e) {
            log.error("获取过期时间失败，key={}", key, e);
            return -2;
        }
    }

    // ==================== 分布式Map操作 ====================

    /**
     * Map 批量设置
     *
     * @param mapKey Map key
     * @param data   数据映射
     * @param <K>    键类型
     * @param <V>    值类型
     */
    public <K, V> void mapPutAll(String mapKey, Map<K, V> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        try {
            RMap<K, V> map = redissonClient.getMap(mapKey);
            map.putAll(data);
        } catch (Exception e) {
            log.error("Map 批量设置失败，mapKey={}", mapKey, e);
        }
    }

    /**
     * Map 批量获取
     *
     * @param mapKey Map key
     * @param keys   键集合
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 键值映射
     */
    public <K, V> Map<K, V> mapGetAll(String mapKey, Set<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            RMap<K, V> map = redissonClient.getMap(mapKey);
            return map.getAll(keys);
        } catch (Exception e) {
            log.error("Map 批量获取失败，mapKey={}", mapKey, e);
            return Collections.emptyMap();
        }
    }

    /**
     * Map 获取所有键值对
     *
     * @param mapKey Map key
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 所有键值对
     */
    public <K, V> Map<K, V> mapGetAllEntries(String mapKey) {
        try {
            RMap<K, V> map = redissonClient.getMap(mapKey);
            return map.readAllMap();
        } catch (Exception e) {
            log.error("Map 获取所有键值对失败，mapKey={}", mapKey, e);
            return Collections.emptyMap();
        }
    }

    // ==================== 分布式信号量操作 ====================

    /**
     * 尝试获取信号量许可
     *
     * @param semaphoreKey 信号量 key
     * @param permits      许可数量
     * @param waitTime     等待时间
     * @param timeUnit     时间单位
     * @return true 获取成功
     */
    public boolean tryAcquireSemaphore(String semaphoreKey, int permits, long waitTime, TimeUnit timeUnit) {
        try {
            RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);
            return semaphore.tryAcquire(permits, waitTime, timeUnit);
        } catch (Exception e) {
            log.error("获取信号量失败，semaphoreKey={}", semaphoreKey, e);
            return false;
        }
    }

    /**
     * 释放信号量许可
     *
     * @param semaphoreKey 信号量 key
     * @param permits      许可数量
     */
    public void releaseSemaphore(String semaphoreKey, int permits) {
        try {
            RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);
            semaphore.release(permits);
        } catch (Exception e) {
            log.error("释放信号量失败，semaphoreKey={}", semaphoreKey, e);
        }
    }

    /**
     * 设置信号量许可总数
     *
     * @param semaphoreKey 信号量 key
     * @param permits      许可总数
     * @return true 设置成功
     */
    public boolean trySetSemaphorePermits(String semaphoreKey, int permits) {
        try {
            RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);
            return semaphore.trySetPermits(permits);
        } catch (Exception e) {
            log.error("设置信号量许可总数失败，semaphoreKey={}", semaphoreKey, e);
            return false;
        }
    }

    // ==================== 分布式延迟队列操作 ====================

    /**
     * 添加元素到延迟队列
     *
     * @param queueKey 队列 key
     * @param value    值
     * @param delay    延迟时间
     * @param timeUnit 时间单位
     * @param <V>      值类型
     */
    public <V> void offerDelayed(String queueKey, V value, long delay, TimeUnit timeUnit) {
        try {
            RDelayedQueue<V> delayedQueue = redissonClient.getDelayedQueue(redissonClient.getBlockingQueue(queueKey));
            delayedQueue.offer(value, delay, timeUnit);
        } catch (Exception e) {
            log.error("添加延迟队列元素失败，queueKey={}", queueKey, e);
        }
    }

    /**
     * 从阻塞队列获取元素
     *
     * @param queueKey 队列 key
     * @param timeout  超时时间
     * @param timeUnit 时间单位
     * @param <V>      值类型
     * @return 队列元素
     */
    public <V> V pollBlocking(String queueKey, long timeout, TimeUnit timeUnit) {
        try {
            RBlockingQueue<V> blockingQueue = redissonClient.getBlockingQueue(queueKey);
            return blockingQueue.poll(timeout, timeUnit);
        } catch (Exception e) {
            log.error("从阻塞队列获取元素失败，queueKey={}", queueKey, e);
            return null;
        }
    }

    // ==================== 工具方法 ====================

}
