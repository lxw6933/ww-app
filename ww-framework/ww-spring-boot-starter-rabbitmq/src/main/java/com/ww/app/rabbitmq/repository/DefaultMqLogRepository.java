package com.ww.app.rabbitmq.repository;

import com.ww.app.rabbitmq.common.BaseMqLog;
import com.ww.app.rabbitmq.common.MyCorrelationData;
import com.ww.app.common.enums.MqMsgStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ww
 * @create 2024-11-12- 14:11
 * @description:
 */
@Slf4j
public class DefaultMqLogRepository implements MqLogRepository<String, BaseMqLog> {

    @Override
    public <E> boolean save(MyCorrelationData<E> myCorrelationData, MqMsgStatus status) {
        log.info("save mq log: {} status: {}", myCorrelationData, status);
        return true;
    }

    @Override
    public boolean update(String correlationId, MqMsgStatus status) {
        log.info("update mq log correlationId: {} status: {}", correlationId, status);
        return true;
    }

    @Override
    public BaseMqLog get(String correlationId) {
        return null;
    }
}
