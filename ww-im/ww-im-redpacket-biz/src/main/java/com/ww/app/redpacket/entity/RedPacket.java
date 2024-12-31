package com.ww.app.redpacket.entity;

import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author ww
 * @create 2024-12-30- 13:49
 * @description: 红包记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document("red_packet")
public class RedPacket extends BaseDoc {

    /**
     * 红包创建人id
     */
    private Long userId;

    /**
     * 红包总金额
     */
    private BigDecimal totalAmount;

    /**
     * 红包总个数
     */
    private int totalCount;

    /**
     * 红包剩余金额
     */
    private BigDecimal remainAmount;

    /**
     * 红包领取金额
     */
    private BigDecimal receiveAmount;

    /**
     * 领取人数
     */
    private int receiveCount;

    /**
     * 红包过期时间【创建时间 + 24小时】
     */
    private Date expireDate;

    /**
     * 红包状态（0：未领完，1：已领完，2：已回滚）
     */
    private int status;

    public static RedPacket build(Long userId, BigDecimal totalAmount, int totalCount) {
        RedPacket redPacket = new RedPacket();
        redPacket.setUserId(userId);
        redPacket.setTotalAmount(totalAmount);
        redPacket.setTotalCount(totalCount);
        redPacket.setReceiveAmount(BigDecimal.ZERO);
        redPacket.setReceiveCount(0);
        redPacket.setRemainAmount(totalAmount);
        redPacket.setStatus(0);
        return redPacket;
    }

    public static Update buildReceiveUpdate(BigDecimal receiveAmount) {
        return new Update()
                .inc("receiveCount", 1)
                .inc("receiveAmount", receiveAmount);
    }

}
