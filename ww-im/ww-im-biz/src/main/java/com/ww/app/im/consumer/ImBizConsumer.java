package com.ww.app.im.consumer;

import com.google.common.collect.Lists;
import com.mongodb.bulk.BulkWriteResult;
import com.ww.app.im.api.common.ImBizMqConstant;
import com.ww.app.im.core.api.common.ImMsgBody;
import com.ww.app.im.entity.SingleChatMessage;
import com.ww.app.im.service.MsgService;
import com.ww.app.im.utils.DocShardUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ww
 * @create 2025-01-14- 18:05
 * @description:
 *     DirectRabbitListenerContainerFactory：适合高实时性、低延迟的 IM 消息场景（如私聊消息）[不支持批量消费]。
 *     SimpleRabbitListenerContainerFactory：适合高并发、动态扩展的 IM 消息场景（如群聊消息）。
 */
@Slf4j
@Component
public class ImBizConsumer {

    @Resource
    private MsgService msgService;

    @Resource
    private MongoTemplate mongoTemplate;

    private static final int BATCH_SIZE = 500;

    @RabbitListener(queues = {ImBizMqConstant.IM_BIZ_MSG_QUEUE}, containerFactory = "appDirectContainerFactory")
    public void imBizMsg(Message message, ImMsgBody imMsgBody) {
        msgService.handleImMsg(imMsgBody);
    }

    @RabbitListener(queues = {ImBizMqConstant.IM_BIZ_MSG_HANDLE_QUEUE}, containerFactory = "appBatchContainerFactory")
    public void imBizMsgHandle(List<SingleChatMessage> msgList) {
        log.info("消息持久化批次大小: {}", msgList.size());
        Map<String, List<SingleChatMessage>> msgMap = msgList.stream()
                .collect(Collectors.groupingBy(msgKey ->
                        DocShardUtils.getSingleChatDocName(msgKey.getSenderId(), msgKey.getSendTime())));

        msgMap.forEach((collectionName, targetMsgList) -> {
            // 分批写入，避免单次批量过大
            Lists.partition(targetMsgList, BATCH_SIZE).forEach(batch -> {
                BulkOperations bulkOps = mongoTemplate.bulkOps(
                        BulkOperations.BulkMode.UNORDERED,
                        SingleChatMessage.class,
                        collectionName
                );

                batch.forEach(bulkOps::insert);

                try {
                    BulkWriteResult result = bulkOps.execute();
                    log.debug("MongoDB批量写入成功: {}", result.getInsertedCount());
                } catch (Exception e) {
                    log.error("MongoDB批量写入失败, collection: {}", collectionName, e);
                    // 失败重试或记录死信队列
                }
            });
        });
    }

}
