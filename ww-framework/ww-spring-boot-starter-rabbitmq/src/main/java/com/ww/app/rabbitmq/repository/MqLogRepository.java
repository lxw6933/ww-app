package com.ww.app.rabbitmq.repository;

import com.ww.app.rabbitmq.common.BaseMqLog;
import com.ww.app.rabbitmq.common.MyCorrelationData;
import com.ww.app.common.enums.MqMsgStatus;

/**
 * @author ww
 * @create 2024-05-28- 15:41
 * @description:
 */
public interface MqLogRepository<ID, T extends BaseMqLog> {

    /**
     * 保存mq日志
     *
     * @param myCorrelationData 消息
     * @return boolean
     */
    boolean save(MyCorrelationData<?> myCorrelationData, MqMsgStatus status);

    /**
     * 更新mq日志状态和重试次数
     *
     * @return boolean
     */
    boolean update(ID correlationId, MqMsgStatus status);

    /**
     * 根据消息唯一标识获取消息日志
     *
     * @param correlationId 唯一标识
     * @return T
     */
    T get(ID correlationId);

}
