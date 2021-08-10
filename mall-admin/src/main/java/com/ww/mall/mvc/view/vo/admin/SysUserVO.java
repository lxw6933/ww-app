package com.ww.mall.mvc.view.vo.admin;

import lombok.Data;

import java.util.Date;

/**
 * 后台用户表 - VO
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@Data
public class SysUserVO {
    /**
     * 用户ID
     */
    private Long id;

    /**
     * 中心端ID
     */
    private Long centerId;

    /**
     * 用户账号
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 账号状态（1：正常；0：冻结）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
