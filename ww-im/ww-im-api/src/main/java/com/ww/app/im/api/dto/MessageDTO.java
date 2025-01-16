package com.ww.app.im.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ww
 * @create 2024-12-25 21:58
 * @description:
 */
@Data
public class MessageDTO implements Serializable {

    /**
     * 发送人id
     */
    private Long userId;

    /**
     * 直播间id
     */
    private Integer roomId;

    /**
     * 发送人名称
     */
    private String senderName;

    /**
     * 发送人头像
     */
    private String senderAvtar;

    /**
     * 消息类型
     */
    private Integer type;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送时间
     */
    private Date sendTime;

}
