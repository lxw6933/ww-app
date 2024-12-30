package com.ww.app.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.app.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author ww
 * @create 2023-07-17- 10:48
 * @description:
 */
@Data
@TableName("t_member")
@EqualsAndHashCode(callSuper = true)
public class Member extends BaseEntity {

    /**
     * OpenId
     */
    private String openId;

    /**
     * 渠道ID
     */
    private Long channelId;

    /**
     * 密码
     */
    private String password;

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
     * 性别(-1：未知 0：女 1：男)
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
