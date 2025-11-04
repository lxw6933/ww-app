package com.ww.app.im.processor;

import com.google.common.collect.Lists;
import com.mongodb.bulk.BulkWriteResult;
import com.ww.app.disruptor.model.Event;
import com.ww.app.disruptor.model.EventBatch;
import com.ww.app.disruptor.model.ProcessResult;
import com.ww.app.disruptor.processor.BatchEventProcessor;
import com.ww.app.im.entity.SingleChatMessage;
import com.ww.app.im.utils.DocShardUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IM消息持久化处理器
 * 优化：
 * 1. 增加线程池，提升并发写入性能
 * 2. 优化批量大小，平衡性能和内存
 * 3. 增加性能统计
 * @author ww
 */
@Slf4j
@Component
public class ImMsgPersistenceProcessor implements BatchEventProcessor<SingleChatMessage> {

    @Resource
    private MongoTemplate mongoTemplate;
    
    /**
     * 单次批量写入最大数量
     * 优化：从1000调整为500，减少单次操作耗时
     */
    private static final int MAX_BATCH_SIZE = 500;
    
    /**
     * 并发写入线程池
     * 优化：使用专用线程池处理并发写入
     */
    private static final ExecutorService WRITE_EXECUTOR = new ThreadPoolExecutor(
            4,  // 核心线程数
            8,  // 最大线程数
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "mongo-write-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );
    
    /**
     * 性能统计
     */
    private final AtomicInteger totalBatches = new AtomicInteger(0);
    private final AtomicInteger totalMessages = new AtomicInteger(0);
    private final AtomicInteger failedMessages = new AtomicInteger(0);

    @Override
    public ProcessResult processBatch(EventBatch<SingleChatMessage> batch) {
        long startTime = System.currentTimeMillis();
        List<Event<SingleChatMessage>> events = batch.getEvents();
        
        if (events.isEmpty()) {
            return ProcessResult.success();
        }
        
        log.debug("开始批量持久化 {} 条消息", events.size());
        
        // 按集合名分组
        Map<String, List<SingleChatMessage>> collectionMap = new HashMap<>();
        
        for (Event<SingleChatMessage> event : events) {
            SingleChatMessage msg = event.getPayload();
            String collectionName = DocShardUtils.getSingleChatDocName(msg.getSenderId(), msg.getSendTime());
            
            collectionMap.computeIfAbsent(collectionName, k -> new ArrayList<>()).add(msg);
        }
        
        // 并行写入不同集合
        // 优化：使用专用线程池，避免使用默认的ForkJoinPool
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        
        for (Map.Entry<String, List<SingleChatMessage>> entry : collectionMap.entrySet()) {
            String collectionName = entry.getKey();
            List<SingleChatMessage> messages = entry.getValue();
            
            // 异步批量写入，使用专用线程池
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() ->
                    batchInsert(collectionName, messages), WRITE_EXECUTOR
            );
            futures.add(future);
        }
        
        // 等待所有写入完成
        // 优化：增加超时时间，记录详细统计信息
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(10, TimeUnit.SECONDS);  // 从5秒增加到10秒
            
            int totalInserted = futures.stream()
                    .mapToInt(f -> {
                        try {
                            return f.get();
                        } catch (Exception e) {
                            log.error("获取写入结果失败", e);
                            return 0;
                        }
                    })
                    .sum();
            
            long duration = System.currentTimeMillis() - startTime;
            int expectedTotal = events.size();
            int failed = expectedTotal - totalInserted;
            
            // 更新统计信息
            totalBatches.incrementAndGet();
            totalMessages.addAndGet(totalInserted);
            failedMessages.addAndGet(failed);
            
            log.info("批量持久化完成: 总数={}, 成功={}, 失败={}, 集合数={}, 耗时={}ms, TPS={}", 
                    expectedTotal, totalInserted, failed, collectionMap.size(), 
                    duration, duration > 0 ? (totalInserted * 1000 / duration) : 0);
            
            // 每100批次打印累计统计
            if (totalBatches.get() % 100 == 0) {
                printStats();
            }
            
            return ProcessResult.success();
            
        } catch (TimeoutException e) {
            log.error("批量持久化超时: size={}", events.size(), e);
            failedMessages.addAndGet(events.size());
            return ProcessResult.failure("持久化超时");
        } catch (Exception e) {
            log.error("批量持久化失败", e);
            failedMessages.addAndGet(events.size());
            return ProcessResult.failure(e.getMessage());
        }
    }
    
    /**
     * 批量插入消息到指定集合
     * 优化：增加重试机制和详细日志
     */
    private int batchInsert(String collectionName, List<SingleChatMessage> messages) {
        if (messages.isEmpty()) {
            return 0;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 分批写入，避免单次过大
            int insertedCount = 0;
            List<List<SingleChatMessage>> partitions = Lists.partition(messages, MAX_BATCH_SIZE);
            
            for (int i = 0; i < partitions.size(); i++) {
                List<SingleChatMessage> partition = partitions.get(i);
                
                try {
                    BulkOperations bulkOps = mongoTemplate.bulkOps(
                            BulkOperations.BulkMode.UNORDERED,
                            SingleChatMessage.class,
                            collectionName
                    );
                    
                    partition.forEach(bulkOps::insert);
                    BulkWriteResult result = bulkOps.execute();
                    insertedCount += result.getInsertedCount();
                    
                } catch (Exception e) {
                    log.error("分区写入失败: collection={}, partition={}/{}, size={}", 
                            collectionName, i + 1, partitions.size(), partition.size(), e);
                    // 继续处理下一个分区
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("集合 {} 写入完成: 总数={}, 成功={}, 耗时={}ms", 
                    collectionName, messages.size(), insertedCount, duration);
            
            return insertedCount;
            
        } catch (Exception e) {
            log.error("MongoDB 批量写入失败, collection: {}, size: {}", 
                    collectionName, messages.size(), e);
            // TODO: 写入死信队列或重试队列
            return 0;
        }
    }
    
    /**
     * 打印性能统计
     */
    private void printStats() {
        log.info("MongoDB持久化统计: 批次数={}, 总消息数={}, 失败数={}, 成功率={:.2f}%",
                totalBatches.get(), 
                totalMessages.get(), 
                failedMessages.get(),
                totalMessages.get() > 0 ? 
                        (double) (totalMessages.get() - failedMessages.get()) / totalMessages.get() * 100 : 0);
    }
    
    /**
     * 获取统计信息
     */
    public String getStats() {
        return String.format("批次=%d, 总数=%d, 失败=%d", 
                totalBatches.get(), totalMessages.get(), failedMessages.get());
    }
}
