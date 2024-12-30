package com.ww.app.rabbitmq.repository;

import com.alibaba.fastjson.JSON;
import com.ww.app.common.enums.MqMsgStatus;
import com.ww.app.rabbitmq.common.MyCorrelationData;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-28- 16:05
 * @description:
 */
public class MongoMqLogRepository implements MqLogRepository<String, MqMsgLogEntity> {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public boolean save(MyCorrelationData<?> myCorrelationData, MqMsgStatus status) {
        MqMsgLogEntity mqLog = new MqMsgLogEntity();
        mqLog.setRoutingKey(myCorrelationData.getRoutingKey());
        mqLog.setExchange(myCorrelationData.getExchange());
        mqLog.setMessage(JSON.toJSONString(myCorrelationData.getMessage()));
        mqLog.setMsgId(myCorrelationData.getId());
        mqLog.setTryCount(0);
        mqLog.setStatus(status);
        mongoTemplate.save(mqLog);
        return true;
    }

    @Override
    public boolean update(String correlationId, MqMsgStatus status) {
        Criteria criteria = Criteria.where("msgId").is(correlationId);
        Update update = new Update();
        update.set("status", status);
        if (MqMsgStatus.CONSUMED_FAIL == status) {
            update.inc("tryCount", 1);
        }
        mongoTemplate.updateFirst(new Query().addCriteria(criteria), update, MqMsgLogEntity.class);
        return true;
    }

    @Override
    public MqMsgLogEntity get(String correlationId) {
        Criteria criteria = Criteria.where("msgId").is(correlationId);
        return mongoTemplate.findOne(new Query().addCriteria(criteria), MqMsgLogEntity.class);
    }

}
