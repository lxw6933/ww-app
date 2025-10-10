package com.ww.app.juc;

import java.util.concurrent.CyclicBarrier;

/**
 * @author ww
 * @create 2025-10-10 11:28
 * @description: 一组线程相互等待，直到都到达某个共同点再继续执行 可循环使用的屏障 [大家都到齐了再一起干]
 */
public class CyclicBarrierDemo {

    public static void main(String[] args) {
        int workerCount = 3;
        CyclicBarrier barrier = new CyclicBarrier(workerCount, () -> System.out.println("全部线程到达屏障，开始下一阶段！"));

        for (int i = 0; i < workerCount; i++) {
            new Thread(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " 到达第一阶段");
                    barrier.await();

                    System.out.println(Thread.currentThread().getName() + " 进入第二阶段");
                    barrier.await();

                    System.out.println(Thread.currentThread().getName() + " 完成任务");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

    }

}
