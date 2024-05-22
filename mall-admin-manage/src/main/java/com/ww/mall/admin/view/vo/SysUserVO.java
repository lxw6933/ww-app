package com.ww.mall.admin.view.vo;

import com.ww.mall.common.enums.SysPlatformType;
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
     * 平台
     */
    private SysPlatformType platform;

    /**
     * 用户名（账号名称）
     */
    private String username;

    /**
     * 用户昵称（姓名）
     */
    private String nickname;

    /**
     * 头像
     */
    private String headPicture;

    /**
     * 性别
     */
    private Integer sex;

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
     * 平台id
     */
    private Long roleId;

    /**
     * 创建时间
     */
    private Date createTime;

}
