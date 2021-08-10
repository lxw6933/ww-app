package com.ww.mall.mvc.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ww.mall.mvc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台角色表
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_role")
public class SysRoleEntity extends BaseEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * 角色ID
	 */
	@TableId
	private Long id;

	/**
	 * 角色名称
	 */
	private String roleName;

	/**
	 * 角色标识码 格式：ADMIN
	 */
	private String roleNo;

	/**
	 * 角色描述
	 */
	private String roleDesc;

	/**
	 * 排序（默认10）
	 */
	private Integer roleSort;

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
