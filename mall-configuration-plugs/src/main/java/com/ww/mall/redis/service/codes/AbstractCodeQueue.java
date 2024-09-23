package com.ww.mall.redis.service.codes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ww
 * @create 2024-08-31 12:43
 * @description:
 */
@Slf4j
public abstract class AbstractCodeQueue {

    public final AtomicBoolean running = new AtomicBoolean(false);

    public static final int BATCH_NUMBER = 1000;

    private static final int CODE_NUMBER_THRESHOLD = 100;

    public static final int CODE_RESULT_THREAD_POOL_SIZE = 10;

    public static final ExecutorService codeResultExecutor = Executors.newFixedThreadPool(CODE_RESULT_THREAD_POOL_SIZE);

    public static final ConcurrentLinkedQueue<IssueCodeRecord> recordQueue = new ConcurrentLinkedQueue<>();

    private static final ScheduledExecutorService issueCodeScheduler = Executors.newScheduledThreadPool(1);

    public final MongoTemplate mongoTemplate;

    public AbstractCodeQueue(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        issueCodeScheduler.scheduleAtFixedRate(this::handleRemainingRecord, 0, 5, TimeUnit.MINUTES);
    }

    public void addRecordToQueue(IssueCodeRecord issueCodeRecord) {
        recordQueue.offer(issueCodeRecord);
        log.info("【入队】outOrderCode[{}]codes[{}]", issueCodeRecord.getOutOrderCode(), issueCodeRecord.getCodes());
        if (!running.get() && recordQueue.size() > BATCH_NUMBER) {
            recordQueueHandler();
        }
    }

    /**
     * 处理队列中的发放结果进行落库
     */
    public void recordQueueHandler() {
        boolean flag = this.running.compareAndSet(false, true);
        if (!flag) {
            return;
        }
        log.info("【处理队列中的发放结果进行落库】开始");
        try {
            List<IssueCodeRecord> codeRecordList = new ArrayList<>();
            // 队列不为空 and 批处理数据<批处理数量
            while (!recordQueue.isEmpty() && codeRecordList.size() < BATCH_NUMBER) {
                IssueCodeRecord record = recordQueue.poll();
                if (record != null) {
                    codeRecordList.add(record);
                }
            }
            codeResultExecutor.submit(() -> batchSaveIssueResult(codeRecordList));
        } finally {
            running.set(false);
            log.info("【处理队列中的发放结果进行落库】结束");
        }
    }

    /**
     * 将队列中取出的发放结果集合进行落库
     *
     * @param batchCodeRecordList 发放结果落库集合
     */
    public void batchSaveIssueResult(List<IssueCodeRecord> batchCodeRecordList) {
        if (batchCodeRecordList.isEmpty()) {
            return;
        }
        try {
            mongoTemplate.insert(batchCodeRecordList, IssueCodeRecord.class);
            log.info("【发放结果批量入库 数量: {}】", batchCodeRecordList.size());
        } catch (Exception e) {
            log.error("【发放结果批量入库异常】", e);
            batchCodeRecordList.forEach(errorRecord -> log.error("【发放结果批量入库异常】outOrderCode[{}]codes[{}]", errorRecord.getOutOrderCode(), errorRecord.getCodes()));
        }
    }

    /**
     * 处理队列中所有剩余未落库的发放结果
     */
    public void handleRemainingRecord() {
        List<IssueCodeRecord> remainingRecordList = new ArrayList<>();
        log.info("【服务关闭处理队列中未入库的发放结果：{}】", recordQueue.size());
        while (!recordQueue.isEmpty()) {
            IssueCodeRecord record = recordQueue.poll();
            log.info("【未入库的发放结果】{}", record);
            if (record != null) {
                remainingRecordList.add(record);
                if (remainingRecordList.size() > BATCH_NUMBER) {
                    codeResultExecutor.submit(() -> batchSaveIssueResult(new ArrayList<>(remainingRecordList)));
                    remainingRecordList.clear();
                }
            }
        }
        if (!remainingRecordList.isEmpty()) {
            batchSaveIssueResult(remainingRecordList);
        }
    }

    public void destroy() {
        handleRemainingRecord();
        codeResultExecutor.shutdown();
    }

}
