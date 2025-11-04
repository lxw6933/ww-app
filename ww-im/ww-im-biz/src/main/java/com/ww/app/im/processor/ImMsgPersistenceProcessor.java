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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * IM消息持久化处理器
 * @author ww
 */
@Slf4j
@Component
public class ImMsgPersistenceProcessor implements BatchEventProcessor<SingleChatMessage> {

    @Resource
    private MongoTemplate mongoTemplate;
    
    private static final int MAX_BATCH_SIZE = 1000;

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
        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        
        for (Map.Entry<String, List<SingleChatMessage>> entry : collectionMap.entrySet()) {
            String collectionName = entry.getKey();
            List<SingleChatMessage> messages = entry.getValue();
            
            // 异步批量写入
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() ->
                    batchInsert(collectionName, messages)
            );
            futures.add(future);
        }
        
        // 等待所有写入完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(5, TimeUnit.SECONDS);
            
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
            log.info("批量持久化完成: 总数={}, 集合数={}, 耗时={}ms", 
                    totalInserted, collectionMap.size(), duration);
            
            return ProcessResult.success();
            
        } catch (Exception e) {
            log.error("批量持久化失败", e);
            return ProcessResult.failure(e.getMessage());
        }
    }
    
    /**
     * 批量插入消息到指定集合
     */
    private int batchInsert(String collectionName, List<SingleChatMessage> messages) {
        if (messages.isEmpty()) {
            return 0;
        }
        
        try {
            // 分批写入，避免单次过大
            int insertedCount = 0;
            for (List<SingleChatMessage> partition : Lists.partition(messages, MAX_BATCH_SIZE)) {
                BulkOperations bulkOps = mongoTemplate.bulkOps(
                        BulkOperations.BulkMode.UNORDERED,
                        SingleChatMessage.class,
                        collectionName
                );
                
                partition.forEach(bulkOps::insert);
                BulkWriteResult result = bulkOps.execute();
                insertedCount += result.getInsertedCount();
            }
            
            log.debug("集合 {} 写入成功: {} 条", collectionName, insertedCount);
            return insertedCount;
            
        } catch (Exception e) {
            log.error("MongoDB 批量写入失败, collection: {}, size: {}", 
                    collectionName, messages.size(), e);
            // TODO: 写入死信队列或重试队列
            return 0;
        }
    }
}
