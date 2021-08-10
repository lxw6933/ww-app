package com.ww.mall.mvc.view.query.admin;

import lombok.Data;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-13 09:22
 */
@Data
public class SysLogQuery {

    /**
     * 日志类型  0:总台 1:云医院
     */
    private Integer type;

    /**
     * 中心端、用户名
     */
    private String keywords;

    /**
     * 中心端
     */
    private Long centerName;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户操作
     */
    private String operation;

    /**
     * 开始日期
     */
//    @DateFormat(format = "yyyy-MM-dd HH:mm", message = "startTime格式不符合[yyyy-MM-dd]")
    private String startTime;

    /**
     * 结束日期
     */
//    @DateFormat(format = "yyyy-MM-dd HH:mm", message = "endTime格式不符合[yyyy-MM-dd]")
    private String endTime;
}
