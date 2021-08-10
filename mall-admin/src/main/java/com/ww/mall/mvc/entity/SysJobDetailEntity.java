package com.ww.mall.mvc.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ww.mall.mvc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 任务详情
 *
 * @author ww
 * @date 2021-05-14 09:30:37
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job_detail")
public class SysJobDetailEntity extends BaseEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Long id;

	/**
	 * md5（任务名称+任务组+cron表达式）的16位小写字母
	 */
	private String md5;

	/**
	 * 任务处理ID
	 */
	private Long jobId;

	/**
	 * 任务组ID
	 */
	private Long jobGroupId;

	/**
	 * corn表达式
	 */
	private String cronExpression;

	/**
	 * 下次执行时间
	 */
	private Date nextExecutionTime;

	/**
	 * 状态， 0:关闭，1：启用
	 */
	private Boolean enabled;

	/**
	 * 上次执行状态，0：失败，1：成功
	 */
	private Boolean lastExecutionStatus;

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
