package com.ww.app.redpacket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ww
 * @create 2024-12-30- 16:30
 * @description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RedpacketReceiveDTO {

    /**
     * 红包id
     */
    private String redpacketId;

    /**
     * 领取红包用户id
     */
    private Long userId;

    /**
     * 领取金额
     */
    private String amount;

}
