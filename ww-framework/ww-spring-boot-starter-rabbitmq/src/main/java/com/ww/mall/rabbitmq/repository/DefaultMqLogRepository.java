package com.ww.mall.rabbitmq.repository;

import com.ww.mall.rabbitmq.common.BaseMqLog;
import com.ww.mall.rabbitmq.common.MyCorrelationData;
import com.ww.mall.common.enums.MqMsgStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ww
 * @create 2024-11-12- 14:11
 * @description:
 */
@Slf4j
public class DefaultMqLogRepository implements MqLogRepository<String, BaseMqLog> {

    @Override
    public boolean save(MyCorrelationData<?> myCorrelationData, MqMsgStatus status) {
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
