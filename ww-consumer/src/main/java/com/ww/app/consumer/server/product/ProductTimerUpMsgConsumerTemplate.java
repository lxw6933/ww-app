package com.ww.app.consumer.server.product;

import com.ww.app.rabbitmq.template.MsgConsumerTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ww
 * @create 2023-07-28- 08:58
 * @description:
 */
@Slf4j
public class ProductTimerUpMsgConsumerTemplate extends MsgConsumerTemplate<Long> {
    @Override
    public boolean doProcess(Long msg) {
        // TODO: 2023/7/28 定时上架商品
        return true;
    }
}
