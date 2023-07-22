package com.ww.mall.consumer.server.member;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.rabbitmq.client.Channel;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.consumer.utils.MqMsgUtil;
import com.ww.mall.enums.MqMsgStatus;
import com.ww.mall.rabbitmq.MqMsgLogEntity;
import com.ww.mall.rabbitmq.queue.QueueConstant;
import com.ww.mall.web.feign.MemberFeignService;
import com.ww.mall.web.view.bo.AddMemberIntegralBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/22 11:07
 **/
@Slf4j
@Component
public class MallMemberConsumer {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MqMsgUtil mqMsgUtil;

    @Autowired
    private MemberFeignService memberFeignService;

    @RabbitListener(queues = {QueueConstant.MALL_MEMBER_REGISTER_QUEUE_NAME})
    public void memberRegisterMessage(Message message, Long memberId, Channel channel) throws IOException {
        log.info("收到mall_member服务发送新用户注册的消息：{}", memberId);
        MessageProperties properties = message.getMessageProperties();
        long tag = properties.getDeliveryTag();
        // 获取消息的id
        String correlationId = properties.getCorrelationId();
        // 消费幂等性, 防止消息被重复消费
        if (isConsumed(correlationId)) {
            log.warn("重复消费, correlationId: {}", correlationId);
            // 将消息从队列移除
            channel.basicNack(tag, false, false);
            return;
        }
        Criteria criteria = Criteria.where("msgId").is(correlationId);
        Update update = new Update();
        update.set("updateTime", DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
        try {
            // 真正消费的业务逻辑
            AddMemberIntegralBO addMemberIntegralBO = new AddMemberIntegralBO();
            addMemberIntegralBO.setMemberId(memberId);
            addMemberIntegralBO.setIntegralType(true);
            addMemberIntegralBO.setIntegralNum(100);
            Result<Boolean> booleanResult = memberFeignService.addMemberIntegral(addMemberIntegralBO);
            if (Boolean.TRUE.equals(booleanResult.isSuccess())) {
                // 消费确认
                channel.basicAck(tag, false);
                update.set("status", MqMsgStatus.DELIVER_SUCCESS);
                log.info("消费【{}】消息完毕", correlationId);
                mongoTemplate.updateFirst(new Query().addCriteria(criteria), update, MqMsgLogEntity.class);
            } else {
                // 远程调用失败
                log.error("消费者远程调用失败：{}", booleanResult.getMessage());
                throw new ApiException("消费者远程调用失败");
            }
        } catch (Exception e) {
            log.error("执行消费业务异常！！！", e);
            MqMsgLogEntity mqMsg = mqMsgUtil.getMqMsgById(correlationId);
            if (mqMsg.getTryCount() > 3) {
                // 重试三次，如果还未消费成功，则改变状态
                channel.basicNack(tag, false, false);
                update.set("status", MqMsgStatus.DELIVER_FAIL);
            } else {
                channel.basicNack(tag, false, true);
                update.set("tryCount", mqMsg.getTryCount() + 1);
            }
            mongoTemplate.updateFirst(new Query().addCriteria(criteria), update, MqMsgLogEntity.class);
        }
    }

    /**
     * 消息是否已被消费(幂等性)
     *
     * @param correlationId ack消息id
     * @return boolean
     */
    private boolean isConsumed(String correlationId) {
        // 查看消息是否存在
        MqMsgLogEntity mqMsg = mqMsgUtil.getMqMsgById(correlationId);
        // 存在记录，且状态为已消费
        return mqMsg != null && MqMsgStatus.CONSUMED_SUCCESS.equals(mqMsg.getStatus());
    }

}
