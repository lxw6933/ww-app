package com.ww.app.redpacket.rpc;

import com.ww.app.redpacket.service.RedpacketService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * @author ww
 * @create 2024-12-30- 15:54
 * @description:
 */
@RestController
@RequestMapping(RedpacketApi.PREFIX)
public class RedpacketApiRpc implements RedpacketApi {

    @Resource
    private RedpacketService redpacketService;

    @Override
    @PostMapping("/generateRedPacket")
    public String generateRedPacket(BigDecimal totalAmount, int totalCount) {
        return redpacketService.generateRedPacket(totalAmount, totalCount);
    }

    @Override
    @PostMapping("/receiveRedPacket")
    public String receiveRedPacket(String redPacketId) {
        return redpacketService.receiveRedPacket(redPacketId);
    }
}
