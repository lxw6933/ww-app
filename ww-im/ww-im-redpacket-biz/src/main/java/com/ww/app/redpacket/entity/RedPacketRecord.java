package com.ww.app.redpacket.entity;

import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author ww
 * @create 2024-12-30- 13:50
 * @description: 红包领取记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document("red_packet_record")
public class RedPacketRecord extends BaseDoc {

    /**
     * 红包id
     */
    private String redpacketId;

    /**
     * 领取红包用户id
     */
    private Long userId;

    /**
     * 领取红包金额
     */
    private String amount;

    public static RedPacketRecord build(String redPacketId, Long userId, String amount) {
        RedPacketRecord redPacketRecord = new RedPacketRecord();
        redPacketRecord.setRedpacketId(redPacketId);
        redPacketRecord.setUserId(userId);
        redPacketRecord.setAmount(amount);
        return redPacketRecord;
    }

}
