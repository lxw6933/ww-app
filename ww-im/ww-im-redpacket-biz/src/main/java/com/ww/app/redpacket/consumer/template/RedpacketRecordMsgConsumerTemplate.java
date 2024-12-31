package com.ww.app.redpacket.consumer.template;

import com.ww.app.rabbitmq.template.MsgConsumerTemplate;
import com.ww.app.redpacket.dto.RedpacketReceiveDTO;
import com.ww.app.redpacket.entity.RedPacket;
import com.ww.app.redpacket.entity.RedPacketRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * @author ww
 * @create 2024-12-30- 18:28
 * @description:
 */
@Slf4j
@Component
public class RedpacketRecordMsgConsumerTemplate extends MsgConsumerTemplate<RedpacketReceiveDTO> {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public boolean serverHandler(RedpacketReceiveDTO redpacketReceiveDTO) {
        log.info("[红包领取]收到消息：{}", redpacketReceiveDTO);
        // 保存领取记录
        RedPacketRecord redPacketRecord = RedPacketRecord.build(redpacketReceiveDTO.getRedpacketId(), redpacketReceiveDTO.getUserId(), redpacketReceiveDTO.getAmount());
        mongoTemplate.save(redPacketRecord);
        // 更新红包领取数据
        mongoTemplate.updateFirst(RedPacket.buildIdQuery(redpacketReceiveDTO.getRedpacketId()), RedPacket.buildReceiveUpdate(new BigDecimal(redpacketReceiveDTO.getAmount())), RedPacket.class);
        // TODO 新增用户金额
        return true;
    }

}
