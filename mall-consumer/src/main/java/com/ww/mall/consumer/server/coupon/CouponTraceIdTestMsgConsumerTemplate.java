package com.ww.mall.consumer.server.coupon;

import com.ww.mall.consumer.template.MsgConsumerTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ww
 * @create 2023-12-12- 18:02
 * @description:
 */
@Slf4j
public class CouponTraceIdTestMsgConsumerTemplate extends MsgConsumerTemplate<String>  {
    @Override
    public boolean serverHandler(String msg) {
        log.info("traceId 测试 消息【{}】消费处理逻辑", msg);
        return true;
    }
}
