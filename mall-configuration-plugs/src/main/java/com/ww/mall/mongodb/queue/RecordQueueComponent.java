package com.ww.mall.mongodb.queue;

import com.mongodb.bulk.BulkWriteResult;
import com.ww.mall.common.queue.AbstractRecordQueue;
import com.ww.mall.redis.service.codes.IssueCodeRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

/**
 * @author ww
 * @create 2024-08-31 11:55
 * @description:
 */
@Slf4j
public class RecordQueueComponent extends AbstractRecordQueue<IssueCodeRecord> {

    private final MongoTemplate mongoTemplate;

    public RecordQueueComponent(MongoTemplate mongoTemplate) {
        super();
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public int recordDBHandler(List<IssueCodeRecord> batchCodeRecordList) {
        // 初始化 BulkOperations
        // UNORDERED: 条记录插入失败，其他数据仍然会继续插入
        // ORDERED: 遇到错误时停止后续操作
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, IssueCodeRecord.class);
        // 将所有数据添加到 bulk 操作中
        bulkOps.insert(batchCodeRecordList);
        // 提交批量操作
        BulkWriteResult bulkWriteResult = bulkOps.execute();
        return bulkWriteResult.getInsertedCount();
    }

}
