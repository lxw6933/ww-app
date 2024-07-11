package com.ww.mall.limit;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ww
 * @create 2024-07-11- 09:35
 * @description:
 */
@Slf4j
public class TokenBucket {

    private final int capacity;

    private final int tokenRate;

    private final AtomicInteger bucket = new AtomicInteger(0);

    public TokenBucket(int capacity, int tokenRate) {
        this.capacity = capacity;
        this.tokenRate = tokenRate;
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            synchronized (this) {
                int res = Math.min(this.capacity, this.bucket.addAndGet(this.tokenRate));
                log.info("生成token, 当前bucket容量为【{}】个", res);
                this.bucket.set(res);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public synchronized void req() {
        if (this.bucket.get() > 0) {
            log.info("bucket: {}", this.bucket.addAndGet(-1));
        }
    }

    public static void main(String[] args) throws InterruptedException {
        TokenBucket tokenBucket = new TokenBucket(20, 2);
        for (int i = 0; i < 30; i++) {
            Thread.sleep(100);
            tokenBucket.req();
        }
    }

}
