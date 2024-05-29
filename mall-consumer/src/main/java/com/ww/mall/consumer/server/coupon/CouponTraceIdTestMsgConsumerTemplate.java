package com.ww.mall.consumer.server.coupon;

import com.ww.mall.rabbitmq.template.MsgConsumerTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ww
 * @create 2023-12-12- 18:02
 * @description:
 */
@Slf4j
public class CouponTraceIdTestMsgConsumerTemplate extends MsgConsumerTemplate<Integer>  {
    @Override
    public boolean serverHandler(Integer msg) {
        log.info("traceId 测试 消息【{}】消费处理逻辑", msg);
        int a = 1 / msg;
        return true;
    }
}
