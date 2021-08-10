package com.ww.mall.mvc.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ww.mall.mvc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台权限表
 *
 * @author ww
 * @date 2021-06-26 11:36:58
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_permission")
public class SysPermissionEntity extends BaseEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * 权限ID
	 */
	@TableId
	private Long id;

	/**
	 * 父级权限 关联 sys_permission 
	 */
	private Long parentId;

	/**
	 * 权限名称
	 */
	private String name;

	/**
	 * 权限类型（0：目录；1：菜单；2：权限）
	 */
	private Integer type;

	/**
	 * 权限标识码 格式：xx:xx
	 */
	private String code;

	/**
	 * 权限url
	 */
	private String url;

	/**
	 * 权限api(多个url用,分隔)
	 */
	private String permUrl;

	/**
	 * 菜单图标
	 */
	private String icon;

	/**
	 * 排序(默认10)
	 */
	private Integer sort;

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
