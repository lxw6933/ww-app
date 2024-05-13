package com.ww.mall.netty.entity.chat;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author ww
 * @create 2024-05-13 20:57
 * @description:
 */
@Data
@Document("user")
public class User {

    @Id
    private String id;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户名称
     */
    private String nickname;

    /**
     * -1：不祥
     * 0：男
     * 1：女
     */
    private Integer sex;

    /**
     * 密码
     */
    private String password;

    /**
     * 0：直接加好友
     * 1：同意后加好友
     */
    private Integer joinType;

    /**
     * 个性签名
     */
    private String personSignature;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 最后登录时间
     */
    private String lastLoginTime;

    /**
     * 最后离线时间
     */
    private String lastOffTime;

    /**
     * 区域名称
     */
    private String areaName;

    /**
     * 区域编号
     */
    private String areaCode;

}
