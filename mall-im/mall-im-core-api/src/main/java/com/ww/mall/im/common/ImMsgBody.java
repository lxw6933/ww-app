package com.ww.mall.im.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @author ww
 * @create 2024-12-24 9:22
 * @description:
 */
@Data
public class ImMsgBody implements Serializable {

    /**
     * 消息序列号
     */
    private String seqId;

    /**
     *
     */
    private int appId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 消息发送者token
     */
    private String token;

    /**
     * 消息所属业务code
     */
    private String bizCode;

    /**
     * 业务消息内容
     */
    private String msg;
}
