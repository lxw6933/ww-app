package com.ww.mall.consumer.server.product;

import com.ww.mall.consumer.template.MsgConsumerTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ww
 * @create 2023-07-28- 08:58
 * @description:
 */
@Slf4j
public class ProductTimerUpMsgConsumerTemplate extends MsgConsumerTemplate<Long> {
    @Override
    public boolean serverHandler(Long msg) {
        // TODO: 2023/7/28 定时上架商品
        return true;
    }
}
