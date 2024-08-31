package com.ww.mall.redis.service.codes.disruptor;

import com.lmax.disruptor.EventHandler;
import com.ww.mall.redis.service.codes.AbstractCodeQueue;
import com.ww.mall.redis.service.codes.IssueCodeRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * @author ww
 * @create 2024-08-31 10:02
 * @description:
 */
@Slf4j
public class CodeEventHandler extends AbstractCodeQueue implements EventHandler<IssueCodeRecord> {

    public CodeEventHandler(MongoTemplate mongoTemplate) {
        super(mongoTemplate);
    }

    @Override
    public void onEvent(IssueCodeRecord event, long sequence, boolean endOfBatch) {
        log.info("【收到事件】:{}", event);
        addRecordToQueue(event);
    }

}
