package com.ww.mall.consumer.utils;

import com.ww.mall.rabbitmq.MqMsgLogEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/22 14:13
 **/
@Component
public class MqMsgUtil {

    @Autowired
    private MongoTemplate mongoTemplate;

    public MqMsgLogEntity getMqMsgById(String msgId) {
        Criteria criteria = Criteria.where("msgId").is(msgId);
        return mongoTemplate.findOne(new Query().addCriteria(criteria), MqMsgLogEntity.class);
    }


}
