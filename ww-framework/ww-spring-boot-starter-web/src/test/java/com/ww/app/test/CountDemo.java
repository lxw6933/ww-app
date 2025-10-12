package com.ww.app.test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ww
 * @create 2025-10-12 14:30
 * @description:
 */
public class CountDemo {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        for (int i = 0; i < 100; i++) {
            long res = testCount();
            if (res != 1000 * 10000) {
                System.out.println("异常：" + res);
            }
        }
    }

    private static long testCount() throws InterruptedException, ExecutionException {
        AtomicLong source = new AtomicLong(0);
        AtomicLong target = new AtomicLong(0);
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
            // 模拟定时任务重置
            for (int i = 0; i < 3; i++) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {}
                long sum = source.getAndSet(0);
                target.addAndGet(sum);
            }
        });
        for (int i = 0; i < 10000; i++) {
//            executorService.execute(longAdder::increment);
            executorService.execute(() -> {
                for (int j = 0; j < 1000; j++) {
                    source.incrementAndGet();
                }
            });
        }
        task.get();
        executorService.shutdown();
        // 2. 等待所有任务完成，最多等待1分钟
        boolean terminated = executorService.awaitTermination(5, TimeUnit.SECONDS);
        long sum = source.getAndSet(0);
        if (terminated) {
            return target.addAndGet(sum);
        } else {
            System.out.println("等待超时，但任务还未完全结束。当前结果: " + sum);
            return 0;
        }
    }

}
