package com.ww.app.im.core.api.common;

import com.ww.app.proto.im.ImMsgBodyRequest;
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
     * 用户id[业务消息：接收方用户id，非业务消息发送方用户id]
     */
    private Long userId;

    /**
     * 消息发送者token
     */
    private String token;

    /**
     * 消息所属业务code
     */
    private int bizCode;

    /**
     * 业务消息内容
     */
    private String bizMsg;

    public static ImMsgBody build(ImMsgBodyRequest request) {
        ImMsgBody imMsgBody = new ImMsgBody();
        imMsgBody.setSeqId(request.getSeqId());
        imMsgBody.setAppId(request.getAppId());
        imMsgBody.setUserId(request.getUserId());
        imMsgBody.setToken(request.getToken());
        imMsgBody.setBizCode(request.getBizCode());
        imMsgBody.setBizMsg(request.getBizMsg());
        return imMsgBody;
    }

}
