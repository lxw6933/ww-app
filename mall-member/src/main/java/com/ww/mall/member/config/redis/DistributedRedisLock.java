package com.ww.mall.member.config.redis;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @description: 分布式锁
 * @author: ww
 * @create: 2021/5/15 下午3:31
 **/
@Slf4j
@Component
public class DistributedRedisLock {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 加锁
     * @param lockName 锁名
     * @return boolean
     */
    public Boolean lock(String lockName) {
        try {
            RLock lock = redissonClient.getLock(lockName);
            // 锁10秒后自动释放，防止死锁
            lock.lock(10, TimeUnit.SECONDS);
            log.info("Thread [{}] DistributedRedisLock lock [{}] success", Thread.currentThread().getName(), lockName);
            // 加锁成功
            return true;
        } catch (Exception e) {
            log.error("DistributedRedisLock lock [{}] Exception:", lockName, e);
            return false;
        }
    }

    /**
     * 释放锁
     * @param lockName 锁名
     * @return boolean
     */
    public Boolean unlock(String lockName) {
        try {
            RLock lock = redissonClient.getLock(lockName);
            lock.unlock();
            log.info("Thread [{}] DistributedRedisLock unlock [{}] success", Thread.currentThread().getName(), lockName);
            // 释放锁成功
            return true;
        } catch (Exception e) {
            log.error("DistributedRedisLock unlock [{}] Exception:", lockName, e);
            return false;
        }
    }

}
