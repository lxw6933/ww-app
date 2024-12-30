package com.ww.app.limit;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

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
    private final int capacity;

    /**
     * 漏桶消耗速率
     */
    private final int rate;

    /**
     * 漏桶当前存放数量
     */
    private final AtomicInteger water = new AtomicInteger(0);

    public LeakyBucket(int capacity, int rate) {
        this.capacity = capacity;
        this.rate = rate;
    }

    public void req() {
        // 获取当前请求时间
        long currentTimeMillis = System.currentTimeMillis();
        // 获取截止当前，漏桶消耗water,剩余多少water
        long decr = ((currentTimeMillis - this.lastTimeStamp) / 1000) * this.rate;
        if (decr > 0) {
            this.lastTimeStamp = currentTimeMillis;
        }
        this.water.getAndSet(Math.max(0, this.water.addAndGet((int) -decr)));
        if (this.water.get() < this.capacity) {
            log.info("thread[{}] water:{}", Thread.currentThread().getId(), this.water.addAndGet(1));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        LeakyBucket leakyBucket = new LeakyBucket(20, 5);
        A a = new A(leakyBucket);
        B b = new B(leakyBucket);
        a.start();
        b.start();
        Thread.sleep(5000);
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
        private final LeakyBucket leakyBucket;

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
