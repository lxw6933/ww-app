package com.ww.app.web.concurrent;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author NineSu
 */
@Slf4j
class LinkedBlockingDequeTest {

    AtomicInteger finishTaskCount = new AtomicInteger();
    AtomicInteger num = new AtomicInteger();

    private final Object lock = new Object();

    ExecutorService executor = new ThreadPoolExecutor(20, 20,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()) {
        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            synchronized (lock) {
                // 执行完的总任务数
                int finishTaskTotalCount = finishTaskCount.incrementAndGet();
                // 获取任务数队列数量【get from header】
                Integer taskCount = taskCountQueue.peekFirst();
                if (taskCount == null) taskCount = 0;
                log.info("threadName:[{}] current compare taskCount: {}", Thread.currentThread().getId(), taskCount);
                if (finishTaskTotalCount >= taskCount) {
                    log.info("threadName:[{}] 完成了一批任务，当前批次任务总数：{}", Thread.currentThread().getId(), taskCount);
                    // 移除已完成的任务批次任务
                    Integer i = taskCountQueue.pollFirst();
                    if (i != null) {
                        log.info("threadName:[{}] 已移除已达成小目标:{}", Thread.currentThread().getId(), i);
                    }
                }
            }
        }
    };

    volatile LinkedBlockingDeque<Integer> taskCountQueue = new LinkedBlockingDeque<>(1000);

    SecureRandom random = new SecureRandom();

    @Test
    void test1() {
        Thread producer = new Thread(() -> {
            // 生产任务批次数量
            int taskBatchCount = random.nextInt(100);
            log.info("task batch count: {}", taskBatchCount);
            for (int i = 0; i < taskBatchCount; i++) {
                // 每批任务批次总任务数量
                int taskCount = random.nextInt(20);
                log.info("第{}批次任务数: {}", (i + 1), taskCount);
                Integer count = taskCountQueue.peekLast();
                if (count == null) count = 0;
                log.info("获取目前最新任务数:{}", count);
                int taskCounter = count + taskCount;
                log.info("计算新的任务总数: {}", taskCounter);
                taskCountQueue.addLast(taskCounter);
                for (int j = 0; j < taskCount; j++) {
                    executor.submit(() -> {
                        num.incrementAndGet();
                        // try {
                        //     int sleep = random.nextInt(10);
                        //     TimeUnit.SECONDS.sleep(sleep);
                        // } catch (InterruptedException e) {
                        //     Thread.currentThread().interrupt();
                        // }
                    });
                }
            }
            executor.shutdown();
        });
        producer.start();
        waitFinish(executor);
        log.info("执行次数：{}, 执行总任务数：{}", num, finishTaskCount);
        Assertions.assertTrue(true);
    }

    private void waitFinish(ExecutorService executor) {
        try {
            while (!executor.isTerminated()) {
                TimeUnit.SECONDS.sleep(5);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
