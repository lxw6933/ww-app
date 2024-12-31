package com.ww.app.redpacket.service;

import java.math.BigDecimal;

/**
 * @author ww
 * @create 2024-12-30- 13:47
 * @description:
 */
public interface RedpacketService {

    /**
     * 生成红包
     *
     * @param totalAmount 红包总金额
     * @param totalCount 红包个数
     * @return 红包id
     */
    String generateRedPacket(BigDecimal totalAmount, int totalCount);

    /**
     * 领取红包
     *
     * @param redPacketId 红包id
     * @return 领取红包金额
     */
    String receiveRedPacket(String redPacketId);
}
