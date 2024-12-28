package com.ww.mall.member.view.vo;

import lombok.Data;

import java.util.Date;

/**
 * @author ww
 * @create 2023-07-17- 15:31
 * @description:
 */
@Data
public class MemberVO {

    private Long id;

    private Date createTime;

    /**
     * OpenId
     */
    private String openId;

    /**
     * 渠道ID
     */
    private Long channelId;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 头像
     */
    private String headImg;

    /**
     * 锁定积分
     */
    private Integer occupyIntegral;

    /**
     * 可用积分
     */
    private Integer availableIntegral;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 出生日期
     */
    private Date birthday;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 是否黑名单用户
     */
    private Boolean blacklist;

}
