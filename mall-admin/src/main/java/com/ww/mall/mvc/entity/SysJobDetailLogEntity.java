package com.ww.mall.mvc.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ww.mall.mvc.base.BaseNoRecorderEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 任务记录日志
 *
 * @author ww
 * @date 2021-05-14 09:30:37
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job_detail_log")
public class SysJobDetailLogEntity extends BaseNoRecorderEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Long id;

	/**
	 * 任务名
	 */
	private String jobName;

	/**
	 * 任务组
	 */
	private String jobGroup;

	/**
	 * className
	 */
	private String className;

	/**
	 * 执行状态，0：失败，1：成功
	 */
	private Boolean status;

	/**
	 * 开始时间
	 */
	private Date startTime;

	/**
	 * 结束时间
	 */
	private Date endTime;

	/**
	 * 耗时
	 */
	private Long times;

	/**
	 * 执行参数
	 */
	private String params;

	/**
	 * 异常情况
	 */
	private String exception;

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
