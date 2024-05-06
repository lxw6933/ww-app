package com.ww.mall.netty.entity;

import com.alibaba.fastjson.JSON;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WSMessageDTO {
    /**
     * 消息发送者
     */
    private Long senderId;
    /**
     * 消息接收者/群聊id
     */
    private Long chatId;
    /**
     * 消息类型 0文本 1图片 2文件 3视频 4语音 5位置 6名片 7链接 8系统消息
     *
     * @see com.vector.netty.enums.EnumMessage
     */
    private byte messageType;
    /**
     * 业务类型 chat单聊 group群聊 onlineCount在线人数
     *
     * @see com.vector.netty.enums.EnumBusiness
     */
    private String businessType;

    /**
     * 记录每条消息的id
     */
    private Long messageId;
    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息发送时间
     */
    private LocalDateTime sendTime;
    /**
     * 消息接收时间
     */
    private LocalDateTime receiveTime;

    /**
     * 最后一条消息内容
     */
    private String lastMessage;

    /**
     * 消息状态 0失败 1成功
     */
    private byte code;


    /**
     * 封装统一返回格式
     */
    public static TextWebSocketFrame ok() {
        WSMessageDTO data = new WSMessageDTO();
        data.setCode((byte) 1);
        return new TextWebSocketFrame(JSON.toJSONString(data)).retain();
    }

    public static TextWebSocketFrame ok(WSMessageDTO data) {
        data.setCode((byte) 1);
        return new TextWebSocketFrame(JSON.toJSONString(data)).retain();
    }

    public static TextWebSocketFrame error(String message) {
        WSMessageDTO data = new WSMessageDTO();
        data.setCode((byte) 0);
        data.setMessage(message);
        return new TextWebSocketFrame(JSON.toJSONString(data)).retain();
    }
}

