package com.ww.mall.mvc.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ww.mall.mvc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台用户表
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_user")
public class SysUserEntity extends BaseEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * 用户ID
	 */
	@TableId
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
	 * 用户密码
	 */
	private String password;

	/**
	 * 用户昵称
	 */
	private String nickname;

	/**
	 * 账号状态（1：正常；0：冻结）
	 */
	private Integer status;

	/**
	 * 是否删除（0：否，1：是）
	 */
	@TableLogic
	private Boolean isDel;

	/**
	 * 乐观锁
	 */
	@Version
	private Integer version;


}
