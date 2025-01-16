package com.ww.app.im.entity;

import com.ww.app.im.api.dto.MessageDTO;
import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * @author ww
 * @create 2025-01-16- 16:05
 * @description:
 */
@Data
@Document
@EqualsAndHashCode(callSuper = true)
public class SingleChatMessage extends BaseDoc {

    /**
     * 发送者id
     */
    private Long senderId;

    /**
     * 接受者id
     */
    private Long receiverId;

    /**
     * 消息类型（1：文本 2：图片 3：语言）
     */
    private int messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送时间
     */
    private Date sendTime;

    /**
     * 消息状态（1：已发送 2：已送达 3：已读）
     */
    private int status;

    /**
     * 是否删除（0：未删除  1：已删除）
     */
    private int delete;

    public static SingleChatMessage build(Long receiverId, MessageDTO messageDTO) {
        SingleChatMessage message = new SingleChatMessage();
        message.setSenderId(messageDTO.getUserId());
        message.setReceiverId(receiverId);
        message.setMessageType(messageDTO.getType());
        message.setContent(messageDTO.getContent());
        message.setSendTime(messageDTO.getCreateTime());
        message.setStatus(1);
        message.setDelete(0);
        return message;
    }

}
