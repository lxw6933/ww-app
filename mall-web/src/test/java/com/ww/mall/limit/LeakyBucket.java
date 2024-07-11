package com.ww.mall.limit;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ww
 * @create 2024-06-07 22:24
 * @description:
 */
@Slf4j
public class LeakyBucket {

    /**
     * 漏桶最后计算时间
     */
    private long lastTimeStamp = System.currentTimeMillis();

    /**
     * 漏桶容量
     */
    private final int capacity = 20;

    /**
     * 漏桶消耗速率
     */
    private final int rate = 5;

    /**
     * 漏桶当前存放数量
     */
    private final AtomicLong water = new AtomicLong(0);

    public void req() {
        // 获取当前请求时间
        long currentTimeMillis = System.currentTimeMillis();
        // 获取截止当前，漏桶消耗water,剩余多少water
        long decr = ((currentTimeMillis - this.lastTimeStamp) / 1000) * this.rate;
        this.water.getAndSet(Math.max(0, this.water.addAndGet(-decr)));
        if (decr > 0) {
            this.lastTimeStamp = currentTimeMillis;
        }
        if (this.water.get() < this.capacity) {
            log.info("thread[{}] water:{}", Thread.currentThread().getId(), this.water.addAndGet(1));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        LeakyBucket leakyBucket = new LeakyBucket();
        A a = new A(leakyBucket);
        B b = new B(leakyBucket);
        a.start();
        b.start();
        while (true) {

        }
    }

    static class A extends Thread {
        private final LeakyBucket leakyBucket;

        public A(LeakyBucket leakyBucket) {
            this.leakyBucket = leakyBucket;
        }

        @Override
        public void run() {
            for (int i = 0; i < 30; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                leakyBucket.req();
            }
        }
    }

    static class B extends Thread {
        private LeakyBucket leakyBucket;

        public B(LeakyBucket leakyBucket) {
            this.leakyBucket = leakyBucket;
        }

        @Override
        public void run() {
            for (int i = 0; i < 30; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                leakyBucket.req();
            }
        }
    }

}
