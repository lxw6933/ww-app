package com.ww.app.consumer.server.canal;

import com.ww.app.rabbitmq.template.MsgConsumerTemplate;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2023-07-27- 10:11
 * @description:
 */
@Component
public class CanalMsgConsumerTemplate extends MsgConsumerTemplate<CanalMessage<?>> {
    @Override
    public boolean doProcess(CanalMessage<?> msg) {
        // TODO: 2023/7/27 根据表名处理后续逻辑
        return true;
    }
}
