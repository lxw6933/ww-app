package com.ww.mall.redis.service.codes;

import com.ww.mall.common.queue.AbstractRecordQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

/**
 * @author ww
 * @create 2024-08-31 11:55
 * @description:
 */
@Slf4j
public class RecordCurrentQueueComponent extends AbstractRecordQueue<IssueCodeRecord> {

    private final MongoTemplate mongoTemplate;

    public RecordCurrentQueueComponent(MongoTemplate mongoTemplate) {
        super();
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void recordDBHandler(List<IssueCodeRecord> batchCodeRecordList) {
        mongoTemplate.insert(batchCodeRecordList, IssueCodeRecord.class);
    }

}
