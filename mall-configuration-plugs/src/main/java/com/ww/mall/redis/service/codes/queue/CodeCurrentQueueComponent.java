package com.ww.mall.redis.service.codes.queue;

import com.ww.mall.redis.service.codes.AbstractCodeQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * @author ww
 * @create 2024-08-31 11:55
 * @description:
 */
@Slf4j
public class CodeCurrentQueueComponent extends AbstractCodeQueue {

    public CodeCurrentQueueComponent(MongoTemplate mongoTemplate) {
        super(mongoTemplate);
    }

}
