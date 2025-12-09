package com.ww.mall.promotion.consumer;

import com.ww.mall.promotion.mq.GroupFailedMessage;
import com.ww.mall.promotion.mq.GroupMqConstant;
import com.ww.mall.promotion.mq.GroupRefundMessage;
import com.ww.mall.promotion.mq.GroupSuccessMessage;
import com.ww.mall.promotion.service.group.GroupInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2025-12-08 18:15
 * @description: 拼团消息消费者
 */
@Slf4j
@Component
public class GroupMessageConsumer {

    @Resource
    private GroupInstanceService instanceService;

    /**
     * 消费拼团成功消息
     */
    @RabbitListener(queues = GroupMqConstant.GROUP_SUCCESS_QUEUE)
    public void handleGroupSuccess(Message message, GroupSuccessMessage msg) {
        log.info("收到拼团成功消息: groupId={}", msg.getGroupId());
        try {
            instanceService.handleGroupSuccess(msg.getGroupId());
            log.info("处理拼团成功消息完成: groupId={}", msg.getGroupId());
        } catch (Exception e) {
            log.error("处理拼团成功消息异常: groupId={}", msg.getGroupId(), e);
            // 可以发送到死信队列或重试
        }
    }

    /**
     * 消费拼团失败消息
     */
    @RabbitListener(queues = GroupMqConstant.GROUP_FAILED_QUEUE)
    public void handleGroupFailed(Message message, GroupFailedMessage msg) {
        log.info("收到拼团失败消息: groupId={}, reason={}", msg.getGroupId(), msg.getReason());
        try {
            instanceService.handleGroupFailed(msg.getGroupId());
            log.info("处理拼团失败消息完成: groupId={}", msg.getGroupId());
        } catch (Exception e) {
            log.error("处理拼团失败消息异常: groupId={}", msg.getGroupId(), e);
            // 可以发送到死信队列或重试
        }
    }

    /**
     * 消费拼团退款消息
     */
    @RabbitListener(queues = GroupMqConstant.GROUP_REFUND_QUEUE)
    public void handleGroupRefund(Message message, GroupRefundMessage msg) {
        log.info("收到拼团退款消息: groupId={}, reason={}, orderCount={}", 
                msg.getGroupId(), msg.getReason(), 
                msg.getRefundOrders() != null ? msg.getRefundOrders().size() : 0);
        try {
            // 这里可以调用支付系统的退款接口
            // paymentService.refund(msg.getRefundOrders());
            log.info("处理拼团退款消息完成: groupId={}", msg.getGroupId());
        } catch (Exception e) {
            log.error("处理拼团退款消息异常: groupId={}", msg.getGroupId(), e);
            // 可以发送到死信队列或重试
        }
    }

}
