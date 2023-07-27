package com.ww.mall.consumer.template;

import com.ww.mall.consumer.server.canal.CanalMessage;

/**
 * @author ww
 * @create 2023-07-27- 10:11
 * @description:
 */
public class CanalMsgConsumer extends MsgConsumerTemplate<CanalMessage<?>> {
    @Override
    boolean serverHandler(CanalMessage<?> msg) {
        // TODO: 2023/7/27 根据表名处理后续逻辑
        return true;
    }
}
