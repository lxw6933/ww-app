package com.ww.mall.im.service;

import com.ww.mall.im.common.ImMsgBody;

/**
 * @author ww
 * @create 2024-12-24 16:39
 * @description:
 */
public interface MsgAckService {

    /**
     * 客户端发送ack包到服务端，通知已确认收到消息
     */
    void doMsgAck(ImMsgBody imMsgBody);

    /**
     * 记录消息的ack和times
     */
    void recordMsgAck(ImMsgBody imMsgBody, int times);

    /**
     * 发送延迟消息，用于进行消息重试功能
     */
    void sendDelayMsg(ImMsgBody imMsgBody);

    /**
     * 获取ack消息的重试次数
     */
    int getMsgAckTimes(String msgId,long userId,int appId);

}
