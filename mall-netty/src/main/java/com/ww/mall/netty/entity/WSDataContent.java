package com.ww.mall.netty.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class WSDataContent<T> implements Serializable {

    /**
     * 消息类型
     */
    private Integer action;

    /**
     * msgId
     */
    private String msgId;

    /**
     * 发起连接用户id
     */
    private String uid;

    /**
     * 登陆标识【同一个浏览器，多个label连接，认为是同一个】
     */
    private String loginLabel;

    /**
     * data
     */
    private T data;

}
