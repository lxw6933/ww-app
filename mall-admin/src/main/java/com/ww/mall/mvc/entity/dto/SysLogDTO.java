package com.ww.mall.mvc.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-13 09:29
 */
@Data
public class SysLogDTO {

    /**
     * 日志id
     */
    private Long id;

    /**
     * 中心端名称
     */
    private String centerName;

    /**
     * 日志类型， 0：总体，1：中心端
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

}
