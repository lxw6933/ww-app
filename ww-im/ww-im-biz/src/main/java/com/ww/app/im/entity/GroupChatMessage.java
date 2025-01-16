package com.ww.app.im.entity;

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
public class GroupChatMessage extends BaseDoc {

    /**
     * 发送者id
     */
    private Long senderId;

    /**
     * 群组id
     */
    private Long groupId;

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
     * 是否删除（0：未删除  1：已删除）
     */
    private short delete;

}
