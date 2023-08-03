package com.ww.mall.web.concurrent;

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

    ExecutorService executor = new ThreadPoolExecutor(20, 20,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()) {
        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            int count = finishTaskCount.incrementAndGet();
            Integer taskCount = taskCountQueue.peekFirst();
            log.info("current compare taskCount: {}", taskCount);
            if (taskCount == null) taskCount = 0;
            if (count >= taskCount) {
                log.info("完成了一批任务");
                Integer i = taskCountQueue.pollFirst();
                if (i != null) {
                    log.info("移除已达成小目标:{}", i);
                }
            }
        }
    };

    volatile LinkedBlockingDeque<Integer> taskCountQueue = new LinkedBlockingDeque<>(1000);

    SecureRandom random = new SecureRandom();

    @Test
    void test1() {
        Thread producer = new Thread(() -> {
            int taskBatchCount = random.nextInt(20);
            log.info("task batch count: {}", taskBatchCount);
            for (int i = 0; i < taskBatchCount; i++) {
                int taskCount = random.nextInt(20);
                log.info("第{}批次任务数: {}", (i + 1), taskCount);
                Integer count = taskCountQueue.peekLast();
                log.info("获取目前最新任务数:{}", count);
                if (count == null) count = 0;
                int taskCounter = count + taskCount;
                log.info("计算新的任务总数: {}", taskCounter);
                taskCountQueue.addLast(taskCounter);
                for (int j = 0; j < taskCount; j++) {
                    executor.submit(() -> {
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
