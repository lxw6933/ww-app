package com.ww.app.juc;

import java.util.concurrent.Semaphore;

/**
 * @author ww
 * @create 2025-10-10 11:35
 * @description: 限流、连接池、停车场等 [限制有多少人能同时干]
 */
public class SemaphoreDemo {

    public static void main(String[] args) {
        // 同时允许3个线程执行
        Semaphore semaphore = new Semaphore(3);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    semaphore.acquire(); // 获取许可
                    System.out.println(Thread.currentThread().getName() + " 获取到许可，开始执行任务");
                    Thread.sleep(2000);
                    System.out.println(Thread.currentThread().getName() + " 执行完任务，释放许可");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    // 释放许可
                    semaphore.release();
                }
            }, "Thread-" + i).start();
        }

    }

}
