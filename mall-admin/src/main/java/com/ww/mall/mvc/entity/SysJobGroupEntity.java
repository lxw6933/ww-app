package com.ww.mall.mvc.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ww.mall.mvc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务组
 *
 * @author ww
 * @date 2021-05-14 09:30:37
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job_group")
public class SysJobGroupEntity extends BaseEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Long id;

	/**
	 * 任务组名称
	 */
	private String name;

	/**
	 * 任务组说明
	 */
	private String remarks;

	/**
	 * 乐观锁
	 */
	@Version
	private Integer version;
	/**
	 * 逻辑删除 0：已删除；1：正常
	 */
	@TableLogic
	private Integer deleted;

}
