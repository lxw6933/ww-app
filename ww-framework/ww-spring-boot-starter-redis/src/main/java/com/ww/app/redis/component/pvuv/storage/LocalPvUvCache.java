package com.ww.app.redis.component.pvuv.storage;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * 本地PV/UV缓存
 * 提供线程安全的本地缓存机制，用于暂存PV/UV数据
 * 采用LongAdder和双缓冲设计，优化高并发性能
 */
@Slf4j
public class LocalPvUvCache {

    /**
     * PV数据缓存：key -> LongAdder
     * 使用LongAdder替代AtomicLong，提高高并发性能
     */
    private final ConcurrentHashMap<String, LongAdder> pvCache = new ConcurrentHashMap<>();

    /**
     * UV用户缓存 - 当前缓冲区
     * 存储实时写入的UV用户数据
     */
    private ConcurrentHashMap<String, Set<String>> currentUvBuffer = new ConcurrentHashMap<>();
    
    /**
     * UV用户缓存 - 同步缓冲区
     * 在同步操作时，与currentUvBuffer交换，用于同步到Redis
     */
    private ConcurrentHashMap<String, Set<String>> syncUvBuffer = new ConcurrentHashMap<>();
    
    /**
     * 同步状态标记
     */
    private final AtomicBoolean syncing = new AtomicBoolean(false);

    /**
     * 构造函数
     */
    public LocalPvUvCache() {
        log.debug("本地PV/UV缓存初始化成功");
    }

    /**
     * 增加PV计数
     *
     * @param key 键
     */
    public void incrementPv(String key) {
        // 使用computeIfAbsent确保线程安全地创建LongAdder
        LongAdder adder = pvCache.computeIfAbsent(key, k -> new LongAdder());
        adder.increment();
        
        // 返回当前值（注意：这不是原子操作，仅用于近似展示）
        adder.sum();
    }

    /**
     * 添加用户到UV
     *
     * @param key    键
     * @param userId 用户ID
     */
    public void addUserToUv(String key, String userId) {
        // 处理无效用户ID
        if (userId == null || userId.isEmpty()) {
            // 默认不记录异常用户ID
            return;
        }
        
        // 从当前缓冲区获取用户集合，如果不存在则创建
        Set<String> userSet = currentUvBuffer.computeIfAbsent(key, k -> 
            Collections.newSetFromMap(new ConcurrentHashMap<>()));
        
        // 添加用户并返回是否是新用户
        userSet.add(userId);
    }

    /**
     * 获取UV计数
     *
     * @param key 键
     * @return UV计数
     */
    public long getUvCount(String key) {
        // 直接从用户集合获取大小
        Set<String> userSet = currentUvBuffer.get(key);
        return userSet != null ? userSet.size() : 0;
    }

    /**
     * 获取缓存的所有PV数据
     * 注意：此方法会重置PV计数器，用于同步操作
     *
     * @return PV数据映射
     */
    public Map<String, Long> getAllPv() {
        Map<String, Long> result = new HashMap<>(pvCache.size());
        
        // 遍历所有PV计数器，调用sumThenReset原子地获取值并重置
        for (Map.Entry<String, LongAdder> entry : pvCache.entrySet()) {
            String key = entry.getKey();
            LongAdder adder = entry.getValue();
            long value = adder.sumThenReset();
            
            // 只添加非零值
            if (value > 0) {
                result.put(key, value);
            }
        }
        
        return result;
    }

    /**
     * 获取并清空UV用户数据（双缓冲交换）
     * 此方法通过双缓冲设计，原子地交换当前缓冲区和同步缓冲区
     *
     * @return UV用户数据映射
     */
    public Map<String, Set<String>> getAllUvUsers() {
        // CAS操作确保同时只有一个线程执行此方法
        if (!syncing.compareAndSet(false, true)) {
            return Collections.emptyMap();
        }
        
        try {
            // 快速检查：如果当前缓冲区为空，直接返回空结果
            if (currentUvBuffer.isEmpty()) {
                return Collections.emptyMap();
            }
            
            // 正确实现双缓冲交换：交换currentUvBuffer和syncUvBuffer
            ConcurrentHashMap<String, Set<String>> dataToSync = syncUvBuffer;
            syncUvBuffer = currentUvBuffer;
            currentUvBuffer = dataToSync;
            
            // 清空新的当前缓冲区（原同步缓冲区）
            currentUvBuffer.clear();
            
            // 返回需要同步的数据（现在已经在syncUvBuffer中）
            return syncUvBuffer;
        } finally {
            syncing.set(false);
        }
    }

    /**
     * 获取PV缓存大小
     *
     * @return 缓存大小
     */
    public int getPvCacheSize() {
        return pvCache.size();
    }

    /**
     * 获取UV缓存大小
     *
     * @return 缓存大小
     */
    public int getUvCacheSize() {
        return currentUvBuffer.size() + syncUvBuffer.size();
    }
} 