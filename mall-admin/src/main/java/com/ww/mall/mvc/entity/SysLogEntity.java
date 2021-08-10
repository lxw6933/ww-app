package com.ww.mall.mvc.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @description: 系统日志
 * @author: ww
 * @create: 2021-05-13 09:12
 */
@Data
@TableName("sys_log")
public class SysLogEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总台日志
     */
    public static final int LOG_TYPE_PLATFORM = 0;

    /**
     * 中心端日志
     */
    public static final int LOG_TYPE_TENANT = 1;

    @TableId
    private Long id;

    /**
     * 操作员ID
     */
    private Long userId;

    /**
     * 中心端ID
     */
    private Long centerId;

    /**
     * 日志类型， 0：总台，1：中心端
     */
    private Integer type;

    /**
     * 用户名
     */
    private String username;
    /**
     * 用户操作
     */
    private String operation;
    /**
     * 请求方法
     */
    private String method;
    /**
     * 请求参数
     */
    private String params;
    /**
     * 执行时长(毫秒)
     */
    private Long time;
    /**
     * IP地址
     */
    private String ip;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createDate;
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
