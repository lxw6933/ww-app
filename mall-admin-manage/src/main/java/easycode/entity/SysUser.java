package easycode.entity;

import java.util.Date;

import com.baomidou.mybatisplus.extension.activerecord.Model;

import java.io.Serializable;

import com.ww.mall.web.cmmon.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * @author ww
 * @create 2024-05-20 14:02:20
 * @description:
 */
@Data
@TableName("sys_user")
@EqualsAndHashCode(callSuper = true)
public class SysUser extends BaseEntity {
    /**
     * 物理主键
     */
    private Long id;

    /**
     * 平台
     */
    private String platform;

    /**
     * 用户名（账号名称）
     */
    private String username;

    /**
     * 用户昵称（姓名）
     */
    private String nickname;

    /**
     * 密码
     */
    private String password;

    /**
     * 密码盐
     */
    private String salt;

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
    private Integer status;

    /**
     * 是否有效
     */
    private Integer valid;

    /**
     * 平台id
     */
    private Long platformId;

    private Long version;

    private Long creatorId;

    private Long updaterId;

    private Date createTime;

    private Date updateTime;

}

