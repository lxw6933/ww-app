package com.ww.app.consumer.server.canal;

import com.ww.app.rabbitmq.template.MsgConsumerTemplate;

/**
 * @author ww
 * @create 2023-07-27- 10:11
 * @description:
 */
public class CanalMsgConsumerTemplate extends MsgConsumerTemplate<CanalMessage<?>> {
    @Override
    public boolean serverHandler(CanalMessage<?> msg) {
        // TODO: 2023/7/27 根据表名处理后续逻辑
        return true;
    }
}
