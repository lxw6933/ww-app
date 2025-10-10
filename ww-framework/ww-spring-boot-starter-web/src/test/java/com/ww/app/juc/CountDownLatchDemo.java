package com.ww.app.juc;

import java.util.concurrent.CountDownLatch;

/**
 * @author ww
 * @create 2025-10-10 11:30
 * @description: 一个或多个线程等待一组事件完成	一次性闭锁（只能用一次）[等别人干完事我再继续]
 */
public class CountDownLatchDemo {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " 开始执行任务");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        System.out.println("所有任务执行完，主线程继续");

    }

}
