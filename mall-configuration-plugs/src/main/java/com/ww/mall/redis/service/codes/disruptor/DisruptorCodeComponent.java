package com.ww.mall.redis.service.codes.disruptor;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.ww.mall.redis.service.codes.IssueCodeRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.concurrent.Executors;

/**
 * @author ww
 * @create 2024-08-31 10:03
 * @description:
 */
@Slf4j
public class DisruptorCodeComponent {

    private final Disruptor<IssueCodeRecord> disruptor;
    private final CodeEventHandler codeEventHandler;

    private static final int DEFAULT_SIZE = 4;

    public DisruptorCodeComponent(MongoTemplate mongoTemplate) {
        EventFactory<IssueCodeRecord> eventFactory = new CodeEventFactory();
        disruptor = new Disruptor<>(eventFactory, DEFAULT_SIZE, Executors.defaultThreadFactory());

        codeEventHandler = new CodeEventHandler(mongoTemplate);
        disruptor.handleEventsWith(codeEventHandler);
        disruptor.start();
    }

    public void publishEvent(IssueCodeRecord issueCodeRecord) {
        RingBuffer<IssueCodeRecord> ringBuffer = disruptor.getRingBuffer();
        long sequence = ringBuffer.next();
        try {
            IssueCodeRecord recordEvent = ringBuffer.get(sequence);
            recordEvent.setCodes(issueCodeRecord.getCodes());
            recordEvent.setIssueTime(issueCodeRecord.getIssueTime());
            recordEvent.setOutOrderCode(issueCodeRecord.getOutOrderCode());
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    public void destroy() {
        try {
            disruptor.shutdown();
        } finally {
            // 确保所有未处理的数据都已保存
            codeEventHandler.destroy();
        }
    }
}
