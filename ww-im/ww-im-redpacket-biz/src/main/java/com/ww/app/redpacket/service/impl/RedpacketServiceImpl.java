package com.ww.app.redpacket.service.impl;

import cn.hutool.core.lang.Assert;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.AuthorizationContext;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.app.redpacket.common.RedpacketConstant;
import com.ww.app.redpacket.common.RedpacketMQConstant;
import com.ww.app.redpacket.component.RedpacketComponent;
import com.ww.app.redpacket.dto.RedpacketReceiveDTO;
import com.ww.app.redpacket.entity.RedPacket;
import com.ww.app.redpacket.service.RedpacketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * @author ww
 * @create 2024-12-30- 13:48
 * @description:
 */
@Slf4j
@Service
public class RedpacketServiceImpl implements RedpacketService {

    @Resource
    private RedpacketComponent redPacketComponent;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private RabbitMqPublisher rabbitMqPublisher;

    @Override
    public String generateRedPacket(BigDecimal totalAmount, int totalCount) {
        ClientUser clientUser = AuthorizationContext.getClientUser();
        // TODO 扣减用户金额
        RedPacket redPacket = RedPacket.build(clientUser.getId(), totalAmount, totalCount);
        try {
            // 生成红包记录
            redPacket = mongoTemplate.save(redPacket);
            // 拆分红包
            if (!redPacketComponent.generateRedpacket(redPacket.getId(), totalAmount, totalCount)) {
                log.error("红包个数生成异常");
                throw new ApiException("红包生成失败");
            }
        } catch (Exception e) {
            log.error("创建红包失败", e);
            // TODO 回滚用户金额
            throw new ApiException("红包生成失败");
        }
        // 发送24小时延时消息回滚红包未领取金额
        rabbitMqPublisher.sendDelayMsg(RedpacketMQConstant.REDPACKET_DELAY_EXCHANGE, RedpacketMQConstant.REDPACKET_ROLLBACK_KEY, redPacket.getId(), RedpacketConstant.REDPACKET_EXPIRE_TIME);
        return redPacket.getId();
    }

    @Override
    public String receiveRedPacket(String redPacketId) {
        ClientUser clientUser = AuthorizationContext.getClientUser();
        String redPacketAmount = redPacketComponent.receiveRedpacket(redPacketId);
        Assert.notNull(redPacketAmount, () -> new ApiException("来晚一步，红包已抢完"));
        log.info("用户[{}]领取红包[{}]金额[{}]", clientUser.getId(), redPacketId, redPacketAmount);
        // 发送消息 新增用户金额
        RedpacketReceiveDTO redpacketReceiveDTO = new RedpacketReceiveDTO(redPacketId, clientUser.getId(), redPacketAmount);
        rabbitMqPublisher.sendMsg(RedpacketMQConstant.REDPACKET_EXCHANGE, RedpacketMQConstant.REDPACKET_RECORD_KEY, redpacketReceiveDTO);
        return redPacketAmount;
    }
}
