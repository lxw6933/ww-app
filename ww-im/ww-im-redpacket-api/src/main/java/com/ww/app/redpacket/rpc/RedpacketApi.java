package com.ww.app.redpacket.rpc;

import feign.Param;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;

/**
 * @author ww
 * @create 2024-12-30- 15:50
 * @description:
 */
@FeignClient(value = "ww-im-redpacket")
public interface RedpacketApi {

    /**
     * 生成红包
     *
     * @param totalAmount 红包总金额
     * @param totalCount 红包个数
     * @return 红包id
     */
    @PostMapping("/ww-im-redpacket/redpacket/inner/generateRedPacket")
    String generateRedPacket(@Param BigDecimal totalAmount, @Param int totalCount);

    /**
     * 领取红包
     *
     * @param redPacketId 红包id
     * @return 领取红包金额
     */
    @PostMapping("/ww-im-redpacket/redpacket/inner/receiveRedPacket")
    String receiveRedPacket(@Param String redPacketId);

}
