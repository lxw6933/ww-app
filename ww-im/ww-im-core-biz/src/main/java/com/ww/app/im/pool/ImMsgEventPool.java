package com.ww.app.im.pool;

import com.ww.app.im.event.ImMsgEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ImMsgEvent 对象池
 * 用于减少对象创建和GC压力
 * 
 * @author ww
 */
@Slf4j
@Component
public class ImMsgEventPool {
    
    /**
     * 对象池大小
     */
    private static final int POOL_SIZE = 10000;
    
    /**
     * 获取超时时间(ms)
     */
    private static final long ACQUIRE_TIMEOUT_MS = 100;
    
    /**
     * 对象池
     */
    private BlockingQueue<ImMsgEvent> pool;
    
    /**
     * 统计信息
     */
    private final AtomicInteger createCount = new AtomicInteger(0);
    private final AtomicInteger recycleCount = new AtomicInteger(0);
    private final AtomicInteger hitCount = new AtomicInteger(0);
    private final AtomicInteger missCount = new AtomicInteger(0);
    
    @PostConstruct
    public void init() {
        pool = new ArrayBlockingQueue<>(POOL_SIZE);
        
        // 预创建一半的对象
        int preCreateSize = POOL_SIZE / 2;
        for (int i = 0; i < preCreateSize; i++) {
            pool.offer(new ImMsgEvent());
        }
        
        createCount.set(preCreateSize);
        log.info("ImMsgEventPool 初始化完成, 预创建对象数: {}", preCreateSize);
    }
    
    /**
     * 从池中获取对象
     * 如果池为空，则创建新对象
     */
    public ImMsgEvent acquire() {
        try {
            ImMsgEvent event = pool.poll(ACQUIRE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            if (event != null) {
                hitCount.incrementAndGet();
                return event;
            }
            
            // 池中没有可用对象，创建新对象
            missCount.incrementAndGet();
            createCount.incrementAndGet();
            
            if (createCount.get() % 1000 == 0) {
                log.warn("对象池未命中率较高, 创建数: {}, 命中数: {}, 未命中数: {}", 
                        createCount.get(), hitCount.get(), missCount.get());
            }
            
            return new ImMsgEvent();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            missCount.incrementAndGet();
            createCount.incrementAndGet();
            return new ImMsgEvent();
        }
    }
    
    /**
     * 归还对象到池中
     * 如果池已满，则丢弃对象
     */
    public void recycle(ImMsgEvent event) {
        if (event == null) {
            return;
        }
        
        // 清理对象状态
        event.setCtx(null);
        event.setImMsg(null);
        event.setReceiveTime(0);
        
        // 尝试归还到池中
        boolean offered = pool.offer(event);
        
        if (offered) {
            recycleCount.incrementAndGet();
        } else {
            // 池已满，对象将被GC回收
            if (recycleCount.get() % 1000 == 0) {
                log.debug("对象池已满，对象被丢弃. 回收数: {}", recycleCount.get());
            }
        }
    }
    
    /**
     * 获取池中可用对象数量
     */
    public int getAvailableCount() {
        return pool.size();
    }
    
    /**
     * 获取统计信息
     */
    public PoolStats getStats() {
        return new PoolStats(
                createCount.get(),
                recycleCount.get(),
                hitCount.get(),
                missCount.get(),
                pool.size(),
                calculateHitRate()
        );
    }
    
    /**
     * 计算命中率
     */
    private double calculateHitRate() {
        int total = hitCount.get() + missCount.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) hitCount.get() / total * 100;
    }
    
    /**
     * 打印统计信息
     */
    public void printStats() {
        PoolStats stats = getStats();
        log.info("对象池统计: 创建={}, 回收={}, 命中={}, 未命中={}, 可用={}, 命中率={}%",
                stats.createCount, stats.recycleCount, stats.hitCount, 
                stats.missCount, stats.availableCount, stats.hitRate);
    }
    
    /**
     * 重置统计信息
     */
    public void resetStats() {
        createCount.set(pool.size());
        recycleCount.set(0);
        hitCount.set(0);
        missCount.set(0);
        log.info("对象池统计信息已重置");
    }
    
    @PreDestroy
    public void destroy() {
        printStats();
        pool.clear();
        log.info("ImMsgEventPool 已销毁");
    }
    
    /**
     * 统计信息
     */
    public static class PoolStats {
        public final int createCount;
        public final int recycleCount;
        public final int hitCount;
        public final int missCount;
        public final int availableCount;
        public final double hitRate;
        
        public PoolStats(int createCount, int recycleCount, int hitCount, 
                        int missCount, int availableCount, double hitRate) {
            this.createCount = createCount;
            this.recycleCount = recycleCount;
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.availableCount = availableCount;
            this.hitRate = hitRate;
        }
    }
}
