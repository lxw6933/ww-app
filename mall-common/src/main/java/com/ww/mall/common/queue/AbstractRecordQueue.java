package com.ww.mall.common.queue;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ww
 * @create 2024-08-31 12:43
 * @description:
 */
@Slf4j
public abstract class AbstractRecordQueue<T> {

    public static final int BATCH_NUMBER = 1000;

    public static final int RECORD_RESULT_THREAD_POOL_SIZE = 10;

    public final AtomicBoolean running = new AtomicBoolean(false);

    public final ExecutorService recordResultExecutor;

    public final ConcurrentLinkedQueue<T> recordQueue = new ConcurrentLinkedQueue<>();

    public AbstractRecordQueue() {
        initRecordScheduled();
        // 初始化record处理线程池
        this.recordResultExecutor = Executors.newFixedThreadPool(RECORD_RESULT_THREAD_POOL_SIZE);
    }

    public AbstractRecordQueue(ExecutorService recordResultExecutor) {
        initRecordScheduled();
        // 初始化record处理线程池
        this.recordResultExecutor = recordResultExecutor;
    }

    public void initRecordScheduled() {
        // 初始化定时任务处理record
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(this::recordQueueHandler, 0, 10, TimeUnit.SECONDS);
    }

    public void addRecordToQueue(T record) {
        recordQueue.offer(record);
        log.info("[入队]record[{}]", record);
        if (!running.get() && recordQueue.size() > BATCH_NUMBER) {
            recordQueueHandler();
        }
    }

    /**
     * 处理队列中的record进行落库
     */
    public void recordQueueHandler() {
        if (!this.running.compareAndSet(false, true)) {
            return;
        }
        log.info("[record进行落库]开始");
        try {
            List<T> recordList = new ArrayList<>();
            // 队列不为空 and 批处理数据<批处理数量
            while (!recordQueue.isEmpty() && recordList.size() < BATCH_NUMBER) {
                T record = recordQueue.poll();
                if (record != null) {
                    recordList.add(record);
                }
            }
            recordResultExecutor.submit(() -> batchSaveRecordResult(recordList));
        } finally {
            running.set(false);
            log.info("[record进行落库]结束");
        }
    }

    /**
     * 将队列中取出的record集合进行落库
     *
     * @param batchRecordList record落库集合
     */
    public void batchSaveRecordResult(List<T> batchRecordList) {
        if (batchRecordList.isEmpty()) {
            return;
        }
        try {
            int successSize = recordDBHandler(batchRecordList);
            log.info("[批量处理record数量: {}]", successSize);
        } catch (Exception e) {
            log.error("[批量处理异常]", e);
            batchRecordList.forEach(errorRecord -> log.error("[批量处理异常]record[{}]", errorRecord));
        }
    }

    /**
     * 队列记录批量处理
     *
     * @param batchCodeRecordList 批量数据
     * @return 成功数量
     */
    public abstract int recordDBHandler(List<T> batchCodeRecordList);

    /**
     * 处理队列中所有剩余未落库的record
     */
    public void handleRemainingRecord() {
        List<T> remainingRecordList = new ArrayList<>();
        log.info("[服务关闭处理队列中未入库的record：{}]", recordQueue.size());
        while (!recordQueue.isEmpty()) {
            T record = recordQueue.poll();
            log.info("[未入库的record]{}", record);
            if (record != null) {
                remainingRecordList.add(record);
                if (remainingRecordList.size() > BATCH_NUMBER) {
                    recordResultExecutor.submit(() -> batchSaveRecordResult(new ArrayList<>(remainingRecordList)));
                    remainingRecordList.clear();
                }
            }
        }
        if (!remainingRecordList.isEmpty()) {
            batchSaveRecordResult(remainingRecordList);
        }
    }

    public void destroy() {
        handleRemainingRecord();
        recordResultExecutor.shutdown();
    }

}
