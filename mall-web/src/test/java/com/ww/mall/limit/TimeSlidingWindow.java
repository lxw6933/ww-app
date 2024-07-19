package com.ww.mall.limit;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ww
 * @create 2024-07-11- 16:36
 * @description:
 */
@Slf4j
public class TimeSlidingWindow {
    /**
     * 限流上线次数
     */
    private final Integer limitMaxNum;

    /**
     * 滑动窗口格子数
     * 根据时间来，60秒分成60格
     */
    private final Integer windowNum;

    /**
     * 上次统计时间，单位秒
     */
    private final ConcurrentHashMap<String, AtomicLong> lastTimeMap;

    /**
     * 记录key值与时间窗口映射
     */
    private ConcurrentHashMap<String, AtomicIntegerArray> timeWindowsMap;

    /**
     * 记录key值与窗口内请求总数映射
     */
    private ConcurrentHashMap<String, AtomicInteger> timeCountMap;

    public TimeSlidingWindow(int limitMaxNum, int windowNum) {
        this.windowNum = windowNum;
        this.limitMaxNum = limitMaxNum;
        this.timeWindowsMap = new ConcurrentHashMap<>();
        this.timeCountMap = new ConcurrentHashMap<>();
        this.lastTimeMap = new ConcurrentHashMap<>();
    }

    /**
     * 限流方法入口
     */
    public Boolean limit(String key) {
        // 获取当前资源的窗口
        AtomicIntegerArray windows = this.timeWindowsMap.computeIfAbsent(key, k -> new AtomicIntegerArray(this.windowNum));
        // 获取当前资源窗口总的请求次数
        AtomicInteger count = this.timeCountMap.computeIfAbsent(key, k -> new AtomicInteger(0));
        // 获取当前资源最后一次请求时间【单位：秒】
        AtomicLong lastTime = this.lastTimeMap.computeIfAbsent(key, k -> new AtomicLong(System.currentTimeMillis() / 1000));

        // 获取当前时间【秒】
        Long now = System.currentTimeMillis() / 1000;
        // 根据请求时间与窗口取模，定位到具体单位窗口下标
        int temp = (int) (now % this.windowNum);
        // 计算当前请求时间与最近一次请求时间差，用于刷新窗口
        long diffTime = now - lastTime.get();
        // 以窗口最小单位为锁资源
        synchronized (windows) {
            if (diffTime >= 0) {
                if (now.equals(lastTime.get())) {
                    // 1.同一单位时间内的请求
                    // 将单位窗口内的请求次数累加1
                    windows.getAndAdd(temp, 1);
                    // 整个窗口请求次数累加1
                    count.addAndGet(1);
                } else if (diffTime < this.windowNum) {
                    // 2.当前时间和上次请求记录时间在同一个周期中，环形数组的同一个周期中，没有超过一个周期。
                    //   该情况意味着，从当前时间now计算时间窗口内请求数，只需要保留并计算 (now -> last) 之间的单位窗口；
                    //   而从(last -> now) 之间的单位窗口都已失效，需要将其重置为0；
                    //   并且将now 当前的格子数置为1，重新开始计数当前单位窗口。
                    clearExpireWindows(windows, (int) (lastTime.get() % this.windowNum), temp, count);
                    // 将单位窗口内的请求次数设置为1
                    windows.set(temp, 1);
                    // 整个窗口请求次数累加1
                    count.addAndGet(1);
                } else if (diffTime >= this.windowNum) {
                    // 3.当前时间和上次请求记录时间不在同一个周期中，之前统计的数据都已失效
                    // 重置整个时间窗口
                    windows = new AtomicIntegerArray(this.windowNum);
                    // 将单位窗口内的请求次数设置为1
                    windows.set(temp, 1);
                    // 整个窗口请求次数设置为1
                    count.set(1);
                }
            } else {
                // 4.异常情况，时钟回拨
                this.timeWindowsMap = new ConcurrentHashMap<>();
                this.timeCountMap = new ConcurrentHashMap<>();
                return true;
            }
            lastTime.set(now);
            // 判断是否需要限流
            if (count.get() > this.limitMaxNum) {
                windows.getAndAdd(temp, -1);
                count.addAndGet(-1);
                return false;
            }
        }
        return true;
    }

    /**
     * 清除过期数据
     *
     * @param windows 需要清除的窗口
     * @param from    开始位置
     * @param to      结束位置
     * @param count   当前周期计算总和
     */
    private void clearExpireWindows(AtomicIntegerArray windows, int from, int to, AtomicInteger count) {
        if (to == from) {
            count.addAndGet(1);
            return;
        }
        // 调整下标值，若结束位置小于开始位置，则说明当前格子位于下一个周期中。
        if (to < from) {
            to = this.windowNum + to;
        }
        while (++from <= to) {
            int index = from % this.windowNum;
            int window = windows.get(index);
            count.addAndGet(-window);
            windows.set(index, 0);
        }
    }

    public static void main(String[] args) {
        TimeSlidingWindow timeSlidingWindow = new TimeSlidingWindow(5, 10);

        new Thread(() -> {
            int i = 0;
            while (i < 600) {
                Boolean limit = timeSlidingWindow.limit("/hello");
                System.out.println("/hello" + i + ":" + limit + ", 时间:" + (i * 300.0) / 1000.0);
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
            }
        }).start();

        new Thread(() -> {
            int i = 0;
            while (i < 600) {
                Boolean limit1 = timeSlidingWindow.limit("/world");
                System.out.println("/world" + i + ":" + limit1 + ", 时间:" + (i * 500.0) / 1000.0);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
            }
        }).start();
    }

}
