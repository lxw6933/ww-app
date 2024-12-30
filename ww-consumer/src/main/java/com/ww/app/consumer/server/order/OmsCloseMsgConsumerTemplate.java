package com.ww.app.consumer.server.order;

import com.ww.app.rabbitmq.template.MsgConsumerTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ww
 * @create 2023-07-28- 09:10
 * @description:
 */
@Slf4j
public class OmsCloseMsgConsumerTemplate extends MsgConsumerTemplate<Long> {
    @Override
    public boolean serverHandler(Long mainOrderId) {
        // TODO: 2023/7/28 关单逻辑处理
        return true;
    }
}
