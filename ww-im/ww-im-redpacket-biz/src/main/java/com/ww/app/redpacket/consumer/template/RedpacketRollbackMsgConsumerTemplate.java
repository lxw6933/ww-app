package com.ww.app.redpacket.consumer.template;

import com.ww.app.rabbitmq.template.MsgConsumerTemplate;
import com.ww.app.redpacket.component.RedpacketComponent;
import com.ww.app.redpacket.entity.RedPacket;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * @author ww
 * @create 2024-12-30- 18:28
 * @description:
 */
@Slf4j
@Component
public class RedpacketRollbackMsgConsumerTemplate extends MsgConsumerTemplate<String> {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private RedpacketComponent redpacketComponent;

    @Override
    public boolean doProcess(String redpacketId) {
        log.info("[红包回滚]收到消息：{}", redpacketId);
        RedPacket redPacket = mongoTemplate.findOne(RedPacket.buildIdQuery(redpacketId), RedPacket.class);
        if (redPacket == null) {
            log.error("[红包回滚]id[{}]找不到", redpacketId);
            return true;
        }
        // 获取红包是否领取完
        List<String> remainRedpacketList = redpacketComponent.getRemainRedpacket(redpacketId);
        if (CollectionUtils.isNotEmpty(remainRedpacketList)) {
            // 需要回滚金额remainAmount
            Optional<BigDecimal> remainAmount = remainRedpacketList.stream().map(BigDecimal::new).reduce(BigDecimal::add);
            // TODO 回滚用户金额
        }
        // 变更红包状态
//        mongoTemplate.updateFirst();
        // 删除红包redis记录
        redpacketComponent.removeRedpacket(redpacketId);
        return true;
    }

}
