package com.ww.mall.promotion.consumer;

import com.ww.mall.promotion.mq.GroupAfterSaleSuccessMessage;
import com.ww.mall.promotion.mq.GroupMqConstant;
import com.ww.mall.promotion.mq.GroupOrderPaidMessage;
import com.ww.mall.promotion.service.group.GroupTradeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 拼团消息消费者。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 支付成功和售后成功消息统一驱动拼团状态机
 */
@Slf4j
@Component
public class GroupMessageConsumer {

    @Resource
    private GroupTradeService groupTradeService;

    /**
     * 消费支付成功消息。
     *
     * @param message 支付成功消息
     */
    @RabbitListener(queues = GroupMqConstant.GROUP_ORDER_PAID_QUEUE)
    public void handleOrderPaid(GroupOrderPaidMessage message) {
        log.info("消费拼团支付成功消息: orderId={}, tradeType={}", message.getOrderId(), message.getTradeType());
        groupTradeService.handleOrderPaid(message);
    }

    /**
     * 消费售后成功消息。
     *
     * @param message 售后成功消息
     */
    @RabbitListener(queues = GroupMqConstant.GROUP_AFTER_SALE_QUEUE)
    public void handleAfterSaleSuccess(GroupAfterSaleSuccessMessage message) {
        log.info("消费拼团售后成功消息: orderId={}, afterSaleId={}", message.getOrderId(), message.getAfterSaleId());
        groupTradeService.handleAfterSaleSuccess(message);
    }
}
