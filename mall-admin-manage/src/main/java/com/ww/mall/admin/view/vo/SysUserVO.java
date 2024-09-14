package com.ww.mall.admin.view.vo;

import lombok.Data;

import java.util.Date;

/**
 * @author ww
 * @create 2024-05-21- 10:39
 * @description:
 */
@Data
public class SysUserVO {

    private Long id;

    /**
     * 用户名（账号名称）
     */
    private String username;

    /**
     * 用户昵称（姓名）
     */
    private String realName;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 电话号码
     */
    private String phone;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态
     */
    private Boolean status;

    /**
     * 是否有效
     */
    private Boolean valid;

    /**
     * 创建时间
     */
    private Date createTime;

}
