package com.ww.app.web.concurrent;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author NineSu
 */
@Slf4j
class CopyOnWriteArrayListLockTest {

    AtomicInteger finishTaskCount = new AtomicInteger();

    ReentrantLock lock = new ReentrantLock();

    ExecutorService executor = new ThreadPoolExecutor(20, 20,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()) {
        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            int count = finishTaskCount.incrementAndGet();
            lock.lock();
            try {
                Integer taskCount = null;
                if (!taskCountQueue.isEmpty()) {
                    taskCount = taskCountQueue.get(0);
                }
                log.info("current compare taskCount: {}", taskCount);
                if (taskCount == null) taskCount = 0;
                if (count >= taskCount) {
                    log.info("完成了一批任务");
                    Integer i = null;
                    if (!taskCountQueue.isEmpty()) {
                        i = taskCountQueue.remove(0);
                    }
                    if (i != null) {
                        log.info("移除已达成小目标:{}", i);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    };

    CopyOnWriteArrayList<Integer> taskCountQueue = new CopyOnWriteArrayList<>();

    SecureRandom random = new SecureRandom();

    @Test
    void test1() {
        Thread producer = new Thread(() -> {
            int taskBatchCount = random.nextInt(20);
            log.info("task batch count: {}", taskBatchCount);
            for (int i = 0; i < taskBatchCount; i++) {
                int taskCount = random.nextInt(20);
                log.info("第{}批次任务数: {}", (i + 1), taskCount);
                Integer count = null;
                if (!taskCountQueue.isEmpty()) {
                    count = taskCountQueue.get(0);
                }
                log.info("获取目前最新任务数:{}", count);
                if (count == null) count = 0;
                int taskCounter = count + taskCount;
                log.info("计算新的任务总数: {}", taskCounter);
                taskCountQueue.add(taskCounter);
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
