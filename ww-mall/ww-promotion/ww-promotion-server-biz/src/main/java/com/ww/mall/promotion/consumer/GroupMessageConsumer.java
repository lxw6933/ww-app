package com.ww.mall.promotion.consumer;

import com.ww.mall.promotion.mq.GroupFailedMessage;
import com.ww.mall.promotion.mq.GroupMqConstant;
import com.ww.mall.promotion.mq.GroupRefundMessage;
import com.ww.mall.promotion.mq.GroupSuccessMessage;
import com.ww.mall.promotion.service.group.support.GroupFlowLogSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 拼团消息消费者。
 * <p>
 * 当前服务内的拼团状态流转已经通过 Disruptor + Redis/Mongo 完成闭环，
 * 因此成功/失败通知消息只作为对外通知或排障记录使用，不能再反向回调本地
 * `handleGroupSuccess/handleGroupFailed`，否则会形成消息自激回流。
 *
 * @author ww
 * @create 2025-12-08 18:15
 * @description: 拼团消息消费者
 */
@Slf4j
@Component
public class GroupMessageConsumer {

    private static final String MQ_CONSUMER_SOURCE = "GROUP_MQ_CONSUMER";

    @Resource
    private GroupFlowLogSupport groupFlowLogSupport;

    /**
     * 消费拼团成功通知消息。
     * <p>
     * 当前服务不再基于该消息回调内部状态机，仅记录消费日志，避免“处理成功 -> 发 MQ ->
     * 再次消费 -> 再次处理成功”的循环。
     *
     * @param message 原始消息
     * @param msg 业务消息体
     */
    @RabbitListener(queues = GroupMqConstant.GROUP_SUCCESS_QUEUE)
    public void handleGroupSuccess(Message message, GroupSuccessMessage msg) {
        log.info("收到拼团成功通知消息，跳过本地状态回调: groupId={}, traceId={}",
                msg.getGroupId(), msg.getTraceId());
        groupFlowLogSupport.record(msg.getTraceId(), msg.getGroupId(), msg.getActivityId(), null, null,
                "GROUP_SUCCESS_MQ", MQ_CONSUMER_SOURCE, "SKIPPED", null,
                "当前服务不再消费成功通知回调内部状态，避免消息自激回流", msg);
    }

    /**
     * 消费拼团失败通知消息。
     * <p>
     * 当前服务不再基于该消息回调内部状态机，仅记录消费日志，避免失败消息循环触发补偿。
     *
     * @param message 原始消息
     * @param msg 业务消息体
     */
    @RabbitListener(queues = GroupMqConstant.GROUP_FAILED_QUEUE)
    public void handleGroupFailed(Message message, GroupFailedMessage msg) {
        log.info("收到拼团失败通知消息，跳过本地状态回调: groupId={}, traceId={}, reason={}",
                msg.getGroupId(), msg.getTraceId(), msg.getReason());
        groupFlowLogSupport.record(msg.getTraceId(), msg.getGroupId(), msg.getActivityId(), null, null,
                "GROUP_FAILED_MQ", MQ_CONSUMER_SOURCE, "SKIPPED", null,
                "当前服务不再消费失败通知回调内部状态，避免消息自激回流", msg);
    }

    /**
     * 消费拼团退款消息。
     * <p>
     * 当前仓库尚未接入真实支付退款能力，因此这里只保留消费入口与链路日志，
     * 后续接入支付服务后，可在此补充正式退款逻辑与重试策略。
     *
     * @param message 原始消息
     * @param msg 业务消息体
     */
    @RabbitListener(queues = GroupMqConstant.GROUP_REFUND_QUEUE)
    public void handleGroupRefund(Message message, GroupRefundMessage msg) {
        int orderCount = msg.getRefundOrders() != null ? msg.getRefundOrders().size() : 0;
        log.info("收到拼团退款消息: groupId={}, traceId={}, reason={}, orderCount={}",
                msg.getGroupId(), msg.getTraceId(), msg.getReason(), orderCount);
        try {
            groupFlowLogSupport.record(msg.getTraceId(), msg.getGroupId(), msg.getActivityId(), null, null,
                    "GROUP_REFUND_MQ", MQ_CONSUMER_SOURCE, "PROCESSING", null, null, msg);
            log.info("拼团退款消息已记录，待后续接入支付退款能力: groupId={}, traceId={}",
                    msg.getGroupId(), msg.getTraceId());
            groupFlowLogSupport.record(msg.getTraceId(), msg.getGroupId(), msg.getActivityId(), null, null,
                    "GROUP_REFUND_MQ", MQ_CONSUMER_SOURCE, "SUCCESS", null,
                    "当前仅记录退款待办，尚未接入真实支付退款", msg);
        } catch (Exception e) {
            log.error("处理拼团退款消息异常: groupId={}, traceId={}", msg.getGroupId(), msg.getTraceId(), e);
            groupFlowLogSupport.record(msg.getTraceId(), msg.getGroupId(), msg.getActivityId(), null, null,
                    "GROUP_REFUND_MQ", MQ_CONSUMER_SOURCE, "FAILED", null, e.getMessage(), msg);
        }
    }
}
