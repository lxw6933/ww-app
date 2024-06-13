package com.ww.mall.limit;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author ww
 * @create 2024-06-07 22:24
 * @description:
 */
public class LeakyBucket {

    /**
     * 漏桶最后计算时间
     */
    private long timeStamp = System.currentTimeMillis();

    /**
     * 漏桶容量
     */
    private final int capacity = 100;

    /**
     * 漏桶消耗速率
     */
    private int rate;

    /**
     * 漏桶当前存放数量
     */
    private long water;

    private boolean limit() {
        // 获取当前请求时间
        long currentTimeMillis = System.currentTimeMillis();
        // 获取截止当前，漏桶消耗water,剩余多少water
        water = Math.max(0, water - (((currentTimeMillis - timeStamp) / 1000) * rate));
        timeStamp = currentTimeMillis;
        return ++water <= capacity;
    }

}
