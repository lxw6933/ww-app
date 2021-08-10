package com.ww.mall.mvc.entity.dto;

import lombok.Data;

import java.util.Date;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-14 09:48
 */
@Data
public class SysJobDetailDTO {

    private Long id;

    /**
     * md5
     */
    private String md5;

    /**
     * 任务处理ID
     */
    private Long jobId;

    /**
     * 任务名
     */
    private String jobName;

    /**
     * 类
     */
    private String className;

    /**
     * 任务组ID
     */
    private Long jobGroupId;

    /**
     * 任务组名
     */
    private String jobGroupName;

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
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date updateTime;
}
