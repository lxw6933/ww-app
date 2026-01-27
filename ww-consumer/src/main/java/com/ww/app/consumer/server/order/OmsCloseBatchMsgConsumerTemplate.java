package com.ww.app.consumer.server.order;

import com.ww.app.rabbitmq.template.BatchMsgConsumerTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @description:
 */
@Slf4j
@Component
public class OmsCloseBatchMsgConsumerTemplate extends BatchMsgConsumerTemplate<String> {
    @Override
    protected boolean doProcess(String msg) {
        // TODO: 关单逻辑处理
        log.info("关单消息处理: {}", msg);
        return true;
    }
}
