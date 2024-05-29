package com.ww.mall.mongodb.repository;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.ww.mall.rabbitmq.MallCorrelationData;
import com.ww.mall.rabbitmq.enums.MqMsgStatus;
import com.ww.mall.rabbitmq.repository.MqLogRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author ww
 * @create 2024-05-28- 16:05
 * @description:
 */
@Component
public class MongoMqLogRepository implements MqLogRepository<String, MqMsgLogEntity> {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public boolean save(MallCorrelationData<?> mallCorrelationData, MqMsgStatus status) {
        MqMsgLogEntity mqLog = new MqMsgLogEntity();
        mqLog.setRoutingKey(mallCorrelationData.getRoutingKey());
        mqLog.setExchange(mallCorrelationData.getExchange());
        mqLog.setMessage(JSON.toJSONString(mallCorrelationData.getMessage()));
        mqLog.setMsgId(mallCorrelationData.getId());
        mqLog.setTryCount(0);
        mqLog.setCreateTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
        mqLog.setUpdateTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
        mqLog.setStatus(status);
        mongoTemplate.save(mqLog);
        return true;
    }

    @Override
    public boolean update(String correlationId, MqMsgStatus status) {
        Criteria criteria = Criteria.where("msgId").is(correlationId);
        Update update = new Update();
        if (status == null) {
            update.inc("tryCount", 1);
        } else {
            update.set("status", status);
        }
        update.set("updateTime", DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
        mongoTemplate.updateFirst(new Query().addCriteria(criteria), update, MqMsgLogEntity.class);
        return true;
    }

    @Override
    public MqMsgLogEntity get(String correlationId) {
        Criteria criteria = Criteria.where("msgId").is(correlationId);
        return mongoTemplate.findOne(new Query().addCriteria(criteria), MqMsgLogEntity.class);
    }

}
